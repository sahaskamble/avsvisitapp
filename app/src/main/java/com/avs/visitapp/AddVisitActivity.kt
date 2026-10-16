package com.avs.visitapp

import android.Manifest
import android.R
import android.text.TextWatcher
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.util.Base64
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.avs.visitapp.databinding.AddVisitActivityBinding
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddVisitActivity : AppCompatActivity() {

    private lateinit var binding: AddVisitActivityBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null

    private val CAMERA_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private var loadingDialog: AlertDialog? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var fullAddress = ""
    private var currentDateTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddVisitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val documentTypes = listOf("Aadhaar ", "PAN Card", "Voter ID", "Driving License")

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            documentTypes
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.black))
                view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                return view
            }
        }

        binding.etDocumentType.setAdapter(adapter)
        binding.etDocumentType.setDropDownBackgroundResource(android.R.color.white)
        binding.etDocumentType.setTextColor(ContextCompat.getColor(this, R.color.black))

        binding.etDocumentType.setOnClickListener {
            binding.etDocumentType.showDropDown()
        }


        val HasExistingLoan = listOf("Yes", "No")

        val adapterr = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            HasExistingLoan
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.black))
                view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                return view
            }
        }

    // Set adapter to AutoCompleteTextView
        binding.etHasExistingLoan.setAdapter(adapterr)
        binding.etHasExistingLoan.setDropDownBackgroundResource(android.R.color.white)
        binding.etHasExistingLoan.setTextColor(ContextCompat.getColor(this, R.color.black))


        binding.etHasExistingLoan.setOnClickListener {
            binding.etHasExistingLoan.showDropDown()
        }


        // Show dropdown on click
        binding.etHasExistingLoan.setOnItemClickListener { _, _, position, _ ->
            val selectedValue = binding.etHasExistingLoan.text.toString()
            if (selectedValue.equals("Yes", ignoreCase = true)) {
                binding.etLoanAmount.visibility = View.VISIBLE
                binding.etLoanBankName.visibility = View.VISIBLE
            } else {
                binding.etLoanAmount.visibility = View.GONE
                binding.etLoanBankName.visibility = View.GONE
                // Optional: Clear the fields when hidden
                binding.etLoanBankName.text?.clear()
                binding.etLoanAmount.text?.clear()
            }
        }



        val mortgagedList = listOf("Yes", "No")

        val adapters = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mortgagedList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                return view
            }
        }

        binding.etIsMortgaged.setAdapter(adapters)
        binding.etIsMortgaged.setDropDownBackgroundResource(android.R.color.white)
        binding.etIsMortgaged.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        binding.etIsMortgaged.setOnClickListener {
            binding.etIsMortgaged.showDropDown()
        }

        binding.etIsMortgaged.setOnItemClickListener { parent, view, position, id ->
            val selected = mortgagedList[position]

            if (selected == "Yes") {
                val input = EditText(this).apply {
                    hint = "Enter mortgage details"
                    setPadding(40, 20, 40, 20)
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    background = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        setStroke(3, Color.BLACK)
                        cornerRadius = 0f
                    }
                }

                val dialog = AlertDialog.Builder(this)
                    .setTitle("Enter Details")
                    .setView(input)
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        val detail = input.text.toString().trim()
                        binding.etIsMortgaged.setText("Yes - $detail", false) // false to prevent triggering listeners
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialogInterface, _ ->
                        binding.etIsMortgaged.setText("", false)
                        dialogInterface.dismiss()
                    }
                    .create()

                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
                }

                dialog.show()
            } else {
                binding.etIsMortgaged.setText("No", false)
            }
        }


        binding.etMobileNo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString().trim()

                if (mobile.length == 10 && mobile.all { it.isDigit() }) {
                    binding.etMobileNo.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.black))
                    binding.tilOMobile.error = null
                } else {
                    binding.etMobileNo.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.holo_red_dark))
                    binding.tilOMobile.error = "Enter valid 10-digit mobile number"
                }
            }
        })

        binding.etVMobileNo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString().trim()

                if (mobile.length == 10 && mobile.all { it.isDigit() }) {
                    binding.etVMobileNo.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.black))
                    binding.tilVMobile.error = null
                } else {
                    binding.etVMobileNo.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.holo_red_dark))
                    binding.tilVMobile.error = "Enter valid 10-digit mobile number"
                }
            }
        })

        binding.etMobile.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString().trim()

                if (mobile.length == 10 && mobile.all { it.isDigit() }) {
                    binding.etMobile.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.black))
                    binding.tilMobile.error = null
                } else {
                    binding.etMobile.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.holo_red_dark))
                    binding.tilMobile.error = "Enter valid 10-digit mobile number"
                }
            }
        })


        binding.etRequestedAmount.addTextChangedListener(object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()

                if (input == currentText) return

                // Remove commas
                val plainText = input.replace(",", "")

                val amount = plainText.toIntOrNull()

                // Format as per Indian locale if valid
                if (amount != null) {
                    val formatted = formatIndianAmount(amount)
                    currentText = formatted

                    // Temporarily remove listener to avoid infinite loop
                    binding.etRequestedAmount.removeTextChangedListener(this)
                    binding.etRequestedAmount.setText(formatted)
                    binding.etRequestedAmount.setSelection(formatted.length)
                    binding.etRequestedAmount.addTextChangedListener(this)

                    // Validation
                    if (amount > 500) {
                        binding.etRequestedAmount.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.black))
                        binding.tilLoanAmount.error = null
                    } else {
                        binding.etRequestedAmount.setTextColor(ContextCompat.getColor(this@AddVisitActivity, android.R.color.holo_red_dark))
                        binding.tilLoanAmount.error = "Amount must be greater than 500"
                    }
                } else {
                    binding.tilLoanAmount.error = "Enter a valid amount"
                }
            }

            private fun formatIndianAmount(amount: Int): String {
                val formatter = java.text.NumberFormat.getInstance(java.util.Locale("en", "IN"))
                return formatter.format(amount)
            }
        })


        val incomeWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalIncome()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.etSelfIncome.addTextChangedListener(incomeWatcher)
        binding.etSpouseIncome.addTextChangedListener(incomeWatcher)
        binding.etOtherIncome.addTextChangedListener(incomeWatcher)


        val expensesWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateNetSurplusFromExpenses()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.etMonthlyExpenses.addTextChangedListener(expensesWatcher)


        binding.profilePhoto.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        binding.etSelfIncome.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etSelfIncome))
        binding.etSpouseIncome.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etSpouseIncome))
        binding.etOtherIncome.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etOtherIncome))
        binding.etMonthlyExpenses.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etMonthlyExpenses))
        binding.etDepositAmount.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etDepositAmount))
        binding.etMonthlyRent.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etMonthlyRent))
        binding.etLoanAmount.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etLoanAmount))
        binding.etMonthlyIncome.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etMonthlyIncome))
        binding.etApproxCost.addTextChangedListener(getIndianCurrencyTextWatcher(binding.etApproxCost))

        binding.submit.setOnClickListener {
            showLoadingDialog()
            submitStaticApplicationData()
        }




        binding.etRentFrom.setOnClickListener {
            showDatePickerDialog(binding.etRentFrom)
        }

        binding.etRentTo.setOnClickListener {
            showDatePickerDialog(binding.etRentTo)
        }

    }


    private fun formatIndianAmount(value: Double): String {
        return try {
            val formatter = NumberFormat.getInstance(Locale("en", "IN"))
            formatter.maximumFractionDigits = 2
            formatter.format(value)
        } catch (e: Exception) {
            value.toString()
        }
    }

    private fun getIndianCurrencyTextWatcher(editText: TextInputEditText): TextWatcher {
        return object : TextWatcher {
            private var current = ""

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().replace(",", "")
                if (input == current || input.isEmpty()) return

                val number = input.toDoubleOrNull() ?: return
                current = input

                val formatted = formatIndianAmount(number)
                editText.removeTextChangedListener(this)
                editText.setText(formatted)
                editText.setSelection(formatted.length)
                editText.addTextChangedListener(this)

                calculateTotalIncome()
                calculateNetSurplusFromExpenses()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }



    private fun calculateTotalIncome() {
        val self = parseAmount(binding.etSelfIncome.text.toString())
        val spouse = parseAmount(binding.etSpouseIncome.text.toString())
        val other = parseAmount(binding.etOtherIncome.text.toString())

        val total = self + spouse + other
        binding.etTotalIncome.setText(formatIndianAmount(total))
    }

    private fun calculateNetSurplusFromExpenses() {
        val total = parseAmount(binding.etTotalIncome.text.toString())
        val expenses = parseAmount(binding.etMonthlyExpenses.text.toString())

        val surplus = total - expenses
        binding.etNetSurplus.setText(formatIndianAmount(surplus))
    }

    private fun parseAmount(amount: String): Double {
        return amount.replace(",", "").toDoubleOrNull() ?: 0.0
    }





    private fun showLoadingDialog() {
        val dialogBinding = DialogLoadingBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        dialogBinding.tvLoading.text = "Please wait..."
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

//    private fun validateAndSubmitThenSubmitApplication() {
//        val lat = binding.etLat.text.toString().trim()
//        val lon = binding.etLong.text.toString().trim()
//        val date = binding.etDate.text.toString().trim()
//        val customerName = binding.etCustomer.text.toString().trim()
//        val mobile = binding.etMobile.text.toString().trim()
//        val loanAccount = binding.etLoanAmount.text.toString().trim()
//        val documentType = binding.etDocumentType.text.toString().trim()
//        val documentNo = binding.etDocumentNo.text.toString().trim()
//
//        val photoAvailable = photoFile != null && photoFile!!.exists()
//
//        if (!photoAvailable || lat.isEmpty() || lon.isEmpty() || date.isEmpty() ||
//            customerName.isEmpty() || mobile.length != 10 ||
//            documentType.isEmpty() || documentNo.isEmpty()) {
//            showToast("Please fill all required fields")
//            dismissLoadingDialog()
//            return
//        }
//
//        val formattedDate = try {
//            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//            outputFormat.format(inputFormat.parse(date)!!)
//        } catch (e: Exception) {
//            showToast("Invalid date format")
//            dismissLoadingDialog()
//            return
//        }
//
//        val photoBase64 = encodeImageToBase64(photoFile!!)
//        val emp_id = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")
//
//        val visitRequest = VisitRequest(
//            emp_id = emp_id,
//            customer_name = customerName,
//            mobile_number = mobile,
//            loan_account = loanAccount,
//            visit_date = formattedDate,
//            location = Location(
//                latitude = lat,
//                longitude = lon
//            ),
//            photo_base64 = photoBase64,
//            document_type = documentType,
//            document_number = documentNo
//        )
//
//        ApiClient.apiService.submitVisit(visitRequest).enqueue(object : Callback<VisitResponse> {
//            override fun onResponse(call: Call<VisitResponse>, response: Response<VisitResponse>) {
//                if (response.isSuccessful && response.body()?.status == "success") {
//                    // Call second API now
//                    submitStaticApplicationData(
//                        lat = lat,
//                        lon = lon,
//                        date = formattedDate,
//                        customerName = customerName,
//                        mobile = mobile,
//                        loanAccount = loanAccount,
//                        documentType = documentType,
//                        documentNo = documentNo,
//                        photoBase64 = photoBase64
//                    ) {
//                        dismissLoadingDialog()
//                        showToast("Visit and Application submitted successfully!")
//                        clearFormFields()
//
//                        val intent = Intent(this@AddVisitActivity, Dashboard::class.java)
//                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                        startActivity(intent)
//                        finish()
//                    }
//
//
//                } else {
//                    showToast("Visit submission failed")
//                    dismissLoadingDialog()
//                }
//            }
//
//            override fun onFailure(call: Call<VisitResponse>, t: Throwable) {
//                showToast("Visit API failed: ${t.message}")
//                dismissLoadingDialog()
//            }
//        })
//    }



    private fun encodeImageToBase64(file: File): String {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }



//    private fun submitVisitData(request: VisitRequest) {
//
//        showLoadingDialog()
//
//        ApiClient.apiService.submitVisit(request).enqueue(object : Callback<VisitResponse> {
//            override fun onResponse(call: Call<VisitResponse>, response: Response<VisitResponse>) {
//                dismissLoadingDialog()
//
//                if (response.isSuccessful) {
//                    val res = response.body()
//
//                    if (res?.status == "success") {
//                        showToast("Visit submitted successfully!")
//                        clearFields()
//                    } else {
//                        showToast("Failed: ${res?.message}")
//                        dismissLoadingDialog()
//                    }
//                } else {
//                    showToast("Server error: ${response.code()}")
//                    dismissLoadingDialog()
//                }
//            }
//
//            override fun onFailure(call: Call<VisitResponse>, t: Throwable) {
//                dismissLoadingDialog()
//            }
//        })
//    }

    private fun clearFields() {
        binding.etLat.setText("")
        binding.etLong.setText("")
        binding.etDate.setText("")
        binding.etCustomer.setText("")
        binding.etMobile.setText("")
        binding.etLoanAmount.setText("")
        binding.etDocumentType.setText("")
        binding.etDocumentNo.setText("")

        // Reset image view to default
        binding.profilePhoto.setImageResource(R.drawable.ic_menu_camera)

        // Delete existing photo file
        photoFile?.let {
            if (it.exists()) it.delete()
        }

        photoFile = null
    }




    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    private fun checkPermissionsAndOpenCamera() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (cameraPermission && locationPermission) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            photoFile = createImageFile()
            val authority = "$packageName.fileprovider"
            val photoUri: Uri = FileProvider.getUriForFile(this, authority, photoFile!!)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
            fetchLocationAndOverlayImage()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchLocationAndOverlayImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                fullAddress = getAddressFromLocation(latitude, longitude)

                // 🔽 Set values to fields
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) // ⏰ Time format

                currentDateTime = currentDate

                binding.etLat.setText(latitude.toString())
                binding.etLong.setText(longitude.toString())
                binding.etDate.setText(currentDate)
                binding.etVisitTime.setText(currentTime)

                overlayImage(photoFile!!.absolutePath)
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses != null && addresses.isNotEmpty()) addresses[0].getAddressLine(0) else "Address not found"
        } catch (e: Exception) {
            e.printStackTrace()
            "Address not found"
        }
    }

    private fun overlayImage(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            isAntiAlias = true
            setShadowLayer(10f, 5f, 5f, Color.BLACK)
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#88000000")
            style = Paint.Style.FILL
        }

        val lines = listOf(
            "📍 $fullAddress",
            "📅 $currentDateTime",
            "🌐 Lat: $latitude",
            "🌐 Long: $longitude"
        )

        val x = 30f
        val yStart = mutableBitmap.height - lines.size * 80f - 30f

        canvas.drawRect(0f, yStart - 40f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)

        var y = yStart
        lines.forEach {
            canvas.drawText(it, x, y, paint)
            y += 80f
        }

        saveBitmapToGallery(mutableBitmap)

        binding.profilePhoto.setImageBitmap(mutableBitmap)
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VisitApp")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VisitApp")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            }
            Toast.makeText(this, "Image saved with location", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            openCamera()
        } else {
            Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
        }
    }


    private fun submitStaticApplicationData() {
        val missingFields = mutableListOf<String>()

        val applicantName = binding.etCustomer.text.toString().trim()
        val mobileNumber = binding.etMobile.text.toString().trim()
        val visitDate = binding.etDate.text.toString().trim()
        val visitTime = binding.etVisitTime.text.toString().trim()
        val presentPersonName = binding.PresentPersonName.text.toString().trim()
        val residentialAddress = binding.ResidentialAddress.text.toString().trim()
        val vMobileNo = binding.etVMobileNo.text.toString().trim()

        val bankName = binding.etBankName.text.toString().trim()
        val branchName = binding.etBranchName.text.toString().trim()
        val bankAddress = binding.etBankAddress.text.toString().trim()
        val accountNumber = binding.etAccountNumber.text.toString().trim()
        val hasExistingLoan = binding.etHasExistingLoan.text.toString().trim().equals("Yes", ignoreCase = true)
        val loanBankName = binding.etLoanBankName.text.toString().trim()
        val loanPurposeType = binding.etLoanPurposeType.text.toString().trim()

        val totalFamilyMembers = binding.etTotalFamilyMembers.text.toString().trim()
        val otherMemberName = binding.etOtherMemberName.text.toString().trim()
        val stayDuration = binding.etStayDuration.text.toString().trim()
        val rentFrom = binding.etRentFrom.text.toString().trim()
        val rentTo = binding.etRentTo.text.toString().trim()
        val isMortgaged = binding.etIsMortgaged.text.toString().trim()

        val occupationPersonName = binding.etPersonName.text.toString().trim()
        val occupationTitle = binding.etOccupation.text.toString().trim()
        val occupationMobile = binding.etMobileNo.text.toString().trim()
        val agriculturalLand = binding.etAgriculturalLand.text.toString().trim()
        val otherAssets = binding.etOtherAssets.text.toString().trim()

        // Validate required fields
        if (bankName.isEmpty()) missingFields.add("Bank Name")
        if (branchName.isEmpty()) missingFields.add("Branch Name")
        if (bankAddress.isEmpty()) missingFields.add("Bank Address")
        if (accountNumber.isEmpty()) missingFields.add("Account Number")
        if (binding.etHasExistingLoan.text.toString().trim().isEmpty()) {
            missingFields.add("Has Existing Loan")
        }
        if (loanPurposeType.isEmpty()) missingFields.add("Loan Purpose Type")

        if (totalFamilyMembers.isEmpty()) missingFields.add("Total Family Members")
        if (binding.etSelfIncome.text.isNullOrBlank()) missingFields.add("Self Income")
        if (binding.etSpouseIncome.text.isNullOrBlank()) missingFields.add("Spouse Income")
        if (binding.etOtherIncome.text.isNullOrBlank()) missingFields.add("Other Income")
        if (binding.etTotalIncome.text.isNullOrBlank()) missingFields.add("Total Income")
        if (binding.etMonthlyExpenses.text.isNullOrBlank()) missingFields.add("Monthly Expenses")
        if (binding.etNetSurplus.text.isNullOrBlank()) missingFields.add("Net Surplus")

        if (otherMemberName.isEmpty()) missingFields.add("Other Member Name")
        if (binding.etApproxCost.text.isNullOrBlank()) missingFields.add("Approx Cost")
        if (stayDuration.isEmpty()) missingFields.add("Stay Duration")
        if (isMortgaged.isEmpty()) missingFields.add("Is Mortgaged")

        if (occupationPersonName.isEmpty()) missingFields.add("Occupation Person Name")
        if (occupationTitle.isEmpty()) missingFields.add("Occupation Title")
        if (occupationMobile.isEmpty()) missingFields.add("Occupation Mobile")
        if (binding.etMonthlyIncome.text.isNullOrBlank()) missingFields.add("Monthly Income")
        if (agriculturalLand.isEmpty()) missingFields.add("Agricultural Land")
        if (otherAssets.isEmpty()) missingFields.add("Other Assets")

        if (missingFields.isNotEmpty()) {
            showToast("Please fill the following fields:\n• " + missingFields.joinToString("\n• "))
            dismissLoadingDialog()
            onComplete()
            return
        }

        val formattedDate = try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = inputFormat.parse(visitDate)
            outputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            showToast("Invalid date format")
            dismissLoadingDialog()
            onComplete()
            return
        }

        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")?.toIntOrNull() ?: 0

        val photoBase64 = encodeImageToBase64(photoFile!!)

        val request = ApplicationRequest(
            Applicant = Applicant(
                ApplicantID = 0,
                Name = applicantName,
                ApplicationDate = formattedDate,
                ApplicationSerialNo = "APP${formattedDate.replace("-", "")}-${System.currentTimeMillis() % 10000}",
                LoanPurposeType = loanPurposeType,
                RequestedAmount = getCleanDouble(binding.etRequestedAmount),
                EmployeeId = empId,
                MobileNo = mobileNumber
            ),
            BankDetails = BankDetails(
                BankName = bankName,
                BranchName = branchName,
                BankAddress = bankAddress,
                AccountNumber = accountNumber,
                HasExistingLoan = hasExistingLoan,
                LoanBankName = loanBankName,
                LoanAmount = getCleanDouble(binding.etLoanAmount)?.toFloat() ?: 0f
            ),
            FamilyIncome = FamilyIncome(
                TotalFamilyMembers = totalFamilyMembers.toIntOrNull() ?: 0,
                SelfIncome = getCleanDouble(binding.etSelfIncome),
                SpouseIncome = getCleanDouble(binding.etSpouseIncome),
                OtherIncome = getCleanDouble(binding.etOtherIncome),
                TotalIncome = getCleanDouble(binding.etTotalIncome),
                MonthlyExpenses = getCleanDouble(binding.etMonthlyExpenses),
                NetSurplus = getCleanDouble(binding.etNetSurplus)
            ),
            HousingDetails = HousingDetails(
                OtherMemberName = otherMemberName,
                ApproxCost = getCleanDouble(binding.etApproxCost),
                StayDuration = stayDuration,
                RentFrom = rentFrom,
                RentTo = rentTo,
                DepositAmount = getCleanDouble(binding.etDepositAmount),
                MonthlyRent = getCleanDouble(binding.etMonthlyRent),
                IsMortgaged = isMortgaged
            ),
            OccupationDetails = OccupationDetails(
                PersonName = occupationPersonName,
                Occupation = occupationTitle,
                MobileNo = occupationMobile,
                MonthlyIncome = getCleanDouble(binding.etMonthlyIncome),
                AgriculturalLand = agriculturalLand,
                OtherAssets = otherAssets
            ),
            visit = visitDetails(
                VisitDate = formattedDate,
                VisitTime = visitTime,
                PresentPersonName = presentPersonName,
                ResidentialAddress = residentialAddress,
                VMobileNo = vMobileNo,
                EmployeeId = empId
            ),
            avsVisit = AvsVisit(
                emp_id = empId,
                customer_name = applicantName,
                mobile_number = mobileNumber,
                loan_account = binding.etLoanAmount.text.toString().replace(",", ""),
                visit_date = visitDate,
                location = Location(
                    latitude = binding.etLat.text.toString().trim(),
                    longitude = binding.etLong.text.toString().trim()
                ),
                document_type = binding.etDocumentType.text.toString().trim(),
                document_number = binding.etDocumentNo.text.toString().trim(),
                photo_base64 = photoBase64
            )
        )

        ApiClient.apiService.submitApplication(request).enqueue(object : Callback<ApplicationResponse> {
            override fun onResponse(call: Call<ApplicationResponse>, response: Response<ApplicationResponse>) {
                dismissLoadingDialog()
                if (response.isSuccessful && response.body() != null) {
                    showToast("Application submitted successfully")
                    clearFormFields()
                    val intent = Intent(this@AddVisitActivity, Dashboard::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    showToast("Submission failed. Try again.")
                }
                onComplete()
            }

            override fun onFailure(call: Call<ApplicationResponse>, t: Throwable) {
                showToast("Application API failed: ${t.message}")
                dismissLoadingDialog()
                onComplete()
            }
        })
    }


    private fun getCleanDouble(text: TextInputEditText): Double? {
        return text.text.toString().replace(",", "").toDoubleOrNull()
    }



    private fun onComplete() {
        dismissLoadingDialog()
    }



    private fun showDatePickerDialog(targetEditText: EditText) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val backendDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)  // yyyy-MM-dd (backend)
                val displayDate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)  // dd-MM-yyyy (user)

                targetEditText.setText(displayDate)
                targetEditText.tag = backendDate  // optional: store backend format if needed later
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }


    private fun clearFormFields() {

        clearFields()
        // Personal & visit details
        binding.etCustomer.text?.clear()
        binding.etMobile.text?.clear()
        binding.etLoanAmount.text?.clear()
        binding.etDocumentType.text?.clear()
        binding.etDocumentNo.text?.clear()
        binding.etDate.text?.clear()
        binding.etVisitTime.text?.clear()
        binding.PresentPersonName.text?.clear()
        binding.ResidentialAddress.text?.clear()
        binding.etVMobileNo.text?.clear()

        // Bank Details
        binding.etBankName.text?.clear()
        binding.etBranchName.text?.clear()
        binding.etBankAddress.text?.clear()
        binding.etAccountNumber.text?.clear()
        binding.etHasExistingLoan.text?.clear()
        binding.etLoanBankName.text?.clear()

        // Family Income
        binding.etTotalFamilyMembers.text?.clear()
        binding.etSelfIncome.text?.clear()
        binding.etSpouseIncome.text?.clear()
        binding.etOtherIncome.text?.clear()
        binding.etTotalIncome.text?.clear()
        binding.etMonthlyExpenses.text?.clear()
        binding.etNetSurplus.text?.clear()

        // Housing Details
        binding.etOtherMemberName.text?.clear()
        binding.etApproxCost.text?.clear()
        binding.etStayDuration.text?.clear()
        binding.etRentFrom.text?.clear()
        binding.etRentTo.text?.clear()
        binding.etDepositAmount.text?.clear()
        binding.etMonthlyRent.text?.clear()
        binding.etIsMortgaged.text?.clear()

        // Occupation
        binding.etPersonName.text?.clear()
        binding.etOccupation.text?.clear()
        binding.etMobileNo.text?.clear()
        binding.etMonthlyIncome.text?.clear()
        binding.etAgriculturalLand.text?.clear()
        binding.etOtherAssets.text?.clear()
    }


}
