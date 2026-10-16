package com.avs.visitapp

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.Adapter.CustomerAdapter
import com.avs.visitapp.Adapter.SimpleTextWatcher
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.MarketingVisitActivityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MarketingVisitActivity : AppCompatActivity() {

    private lateinit var binding: MarketingVisitActivityBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null
    private var loadingDialog: AlertDialog? = null
    private var isImageClicked = false

    private val CAMERA_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private var latitude = 0.0
    private var longitude = 0.0
    private var fullAddress = ""
    private var currentDateTime = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MarketingVisitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

//
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.toolbarBack.setOnClickListener {
            val intent = Intent(this, VisitActivity::class.java)
            startActivity(intent)
            finish()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupBranchDropdown()

        binding.profilePhoto.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }


        val MarketingVisitmobileLayout =
            binding.CustMobNoLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val MarketingVisitmobileEditText =
            binding.CustMobNo.findViewById<TextInputEditText>(R.id.CustMobNo)

        MarketingVisitmobileEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    MarketingVisitmobileLayout.boxStrokeColor =
                        ContextCompat.getColor(this@MarketingVisitActivity, R.color.red)
                    MarketingVisitmobileLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    MarketingVisitmobileLayout.boxStrokeColor = ContextCompat.getColor(
                        this@MarketingVisitActivity,
                        R.color.colorPrimaryDark
                    )
                    MarketingVisitmobileLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.submit.setOnClickListener {
            if (validateAndSubmit()) {
                submitMarketingVisit()
            }
        }

        setupFieldWatchers()

    }

    private val customLayout = R.layout.dropdown_item

    fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
        val adapter = ArrayAdapter(this, customLayout, data)
        autoComplete.setAdapter(adapter)

        // Show all items on first click
        autoComplete.setOnClickListener {
            if (!autoComplete.isPopupShowing) {
                autoComplete.showDropDown()
            }
        }


        // Filter as user types
        autoComplete.threshold = 1
        autoComplete.isFocusable = true
        autoComplete.isFocusableInTouchMode = true
    }


    private fun setupBranchDropdown() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllBanks()
                if (response.isSuccessful) {
                    val banks = response.body() ?: emptyList()

                    // Extract only names for dropdown
                    val branchNames = banks.mapNotNull { it.MIDNAME }

                    setupDropdown(binding.branchName, branchNames)

                    // On branch select
                    binding.branchName.setOnItemClickListener { _, _, _, _ ->
                        val selectedName = binding.branchName.text.toString().trim()

                        // Find matching bank by name
                        val selectedBank = banks.find { it.MIDNAME?.trim() == selectedName }

                        if (selectedBank != null) {
                            val selectedCode = selectedBank.BRCD?.toString() ?: "0"

                            Log.d(
                                "BranchSelection",
                                "Selected: $selectedName, Code: $selectedCode"
                            )

                            binding.branchCode.setText(selectedCode, false)


                            selectedBank.BRCD?.let { brcd ->

                            }
                        } else {
                            Log.e("BranchSelection", "No matching bank found for name: $selectedName")
                        }
                    }

                } else {
                    Log.e("BranchAPI", "Error response: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@MarketingVisitActivity,
                        "Failed to fetch branches",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MarketingVisitActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (cameraPermission && fineLocationPermission && coarseLocationPermission) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val perms = permissions.zip(grantResults.toTypedArray()).toMap()
            val cameraGranted = perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED
            val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED
            val coarseLocationGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == PackageManager.PERMISSION_GRANTED

            if (cameraGranted && fineLocationGranted && coarseLocationGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera and Location permissions are required", Toast.LENGTH_LONG).show()
            }
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
            enableFieldsAfterPhoto()
            isImageClicked = true

            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
            binding.StaffName.setText(staffName)

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

                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) // 👈 dash format
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                currentDateTime = currentDate

                binding.etLat.setText(latitude.toString())
                binding.etLong.setText(longitude.toString())
                binding.etDate.setText(currentDate) // 👈 dd-MM-yyyy
                binding.etVisitTime.setText(currentTime)
                binding.etAddress.setText(fullAddress)

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
            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Address not found"
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

        binding.profilePhoto.setImageBitmap(mutableBitmap)
    }

    private fun enableFieldsAfterPhoto() {
        binding.branchName.isEnabled = true
        binding.Custname.isEnabled = true
        binding.CustMobNo.isEnabled = true
        binding.CustomerRequirement.isEnabled = true
        binding.Comments.isEnabled = true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun validateAndSubmit(): Boolean {
        val missingFields = getMissingFields()

        return if (missingFields.isEmpty()) {

            true
        } else {
            val message = "Please fill: ${missingFields.joinToString(", ")}"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            false
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun getMissingFields(): List<String> {
        val missingFields = mutableListOf<String>()

        if (binding.etDate.text.isNullOrBlank()) {
            missingFields.add("Visit Date")
            setFieldError(binding.etDateLayout, true)
        } else {
            setFieldError(binding.etDateLayout, false)
        }

        if (binding.etVisitTime.text.isNullOrBlank()) {
            missingFields.add("Visit Time")
            setFieldError(binding.etVisitTimeLayout, true)
        } else {
            setFieldError(binding.etVisitTimeLayout, false)
        }

        if (binding.etLat.text.isNullOrBlank()) {
            missingFields.add("Latitude")
            setFieldError(binding.etLatLayout, true)
        } else {
            setFieldError(binding.etLatLayout, false)
        }

        if (binding.etLong.text.isNullOrBlank()) {
            missingFields.add("Longitude")
            setFieldError(binding.etLongLayout, true)
        } else {
            setFieldError(binding.etLongLayout, false)
        }

        if (binding.etAddress.text.isNullOrBlank()) {
            missingFields.add("Address")
            setFieldError(binding.etAddressLayout, true)
        } else {
            setFieldError(binding.etAddressLayout, false)
        }

        if (binding.StaffName.text.isNullOrBlank()) {
            missingFields.add("Staff Name")
            setFieldError(binding.StaffNameLayout, true)
        } else {
            setFieldError(binding.StaffNameLayout, false)
        }

        if (binding.branchName.text.isNullOrBlank()) {
            missingFields.add("Branch Name")
            setFieldError(binding.branchNameLayout, true)
        } else {
            setFieldError(binding.branchNameLayout, false)
        }

        if (binding.branchCode.text.isNullOrBlank()) {
            missingFields.add("Branch Code")
            setFieldError(binding.branchCodeLayout, true)
        } else {
            setFieldError(binding.branchCodeLayout, false)
        }

        if (binding.Custname.text.isNullOrBlank()) {
            missingFields.add("Customer Name with A/c. No")
            setFieldError(binding.CustnameLayout, true)
        } else {
            setFieldError(binding.CustnameLayout, false)
        }


        if (binding.CustomerRequirement.text.isNullOrBlank()) {
            missingFields.add("Customer Requirement")
            setFieldError(binding.CustomerRequirementLayout, true)
        } else {
            setFieldError(binding.CustomerRequirementLayout, false)
        }

        if (binding.CustMobNo.text.isNullOrBlank()) {
            missingFields.add("Customer Mobile No")
            setFieldError(binding.CustMobNoLayout, true)
        } else {
            val mobile = binding.CustMobNo.text.toString().trim()
            if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                missingFields.add("Valid 10-digit Customer Mobile No")
                setFieldError(binding.CustMobNoLayout, true)
            } else {
                setFieldError(binding.CustMobNoLayout, false)
            }
        }

        if (binding.Comments.text.isNullOrBlank()) {
            missingFields.add("Comments")
            setFieldError(binding.CommentsLayout, true)
        } else {
            setFieldError(binding.CommentsLayout, false)
        }

        return missingFields
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setFieldError(layout: TextInputLayout, isError: Boolean) {
        if (isError) {
            layout.boxStrokeColor = resources.getColor(R.color.red, null)
        }
        else {
            layout.boxStrokeColor = resources.getColor(R.color.colorPrimaryDark, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupFieldWatchers() {
        binding.etDate.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etDateLayout, false) })
        binding.etVisitTime.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etVisitTimeLayout, false) })
        binding.etLat.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etLatLayout, false) })
        binding.etLong.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etLongLayout, false) })
        binding.etAddress.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etAddressLayout, false) })
        binding.StaffName.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.StaffNameLayout, false) })
        binding.branchName.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.branchNameLayout, false) })
        binding.branchCode.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.branchCodeLayout, false) })
        binding.Custname.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CustnameLayout, false) })
        binding.CustMobNo.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CustMobNoLayout, false) })
        binding.Comments.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CommentsLayout, false) })

        // Fields to block interaction until image is clicked
        val fields = listOf(
            binding.etDate,
            binding.etVisitTime,
            binding.etLat,
            binding.etLong,
            binding.etAddress,
            binding.StaffName,
            binding.branchName,
            binding.branchCode,
            binding.Custname,
            binding.CustMobNo,
            binding.Comments
        )

        fields.forEach { field ->
            field.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                    showCustomToast("Please first click image")
                    v.clearFocus()
                    true // consume the event so further processing is blocked
                } else {
                    false // allow other events or interactions
                }
            }
        }

    }

    private fun showCustomToast(message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.toast_custom, findViewById(android.R.id.content), false)

        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()
    }

    private fun submitMarketingVisit() {
        showLoadingDialog() // Show immediately

        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")?.toIntOrNull() ?: 0

        lifecycleScope.launch {
            // Compress image in background
            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile) }

            // Convert text fields to RequestBody
            val visitDate = createPart(convertDateForServer(binding.etDate.text?.toString() ?: ""))
            val visitTime = createPart(binding.etVisitTime.text?.toString() ?: "")
            val staffID = createPart(staffId.toString())
            val staffName = createPart(binding.StaffName.text?.toString() ?: "")
            val branchName = createPart(binding.branchName.text?.toString() ?: "")
            val branchCode = createPart(binding.branchCode.text?.toString() ?: "")
            val customerName = createPart(binding.Custname.text?.toString() ?: "")
            val customerMobileNo = createPart(binding.CustMobNo.text?.toString() ?: "")
            val customerRequirement = createPart(binding.CustomerRequirement.text?.toString() ?: "")
            val comments = createPart(binding.Comments.text?.toString() ?: "")
            val latitude = createPart(binding.etLat.text?.toString() ?: "")
            val longitude = createPart(binding.etLong.text?.toString() ?: "")
            val fullAddress = createPart(binding.etAddress.text?.toString() ?: "")

            // API call
            ApiClient.apiService.InsertMarketingVisit(
                VisitDate = visitDate,
                VisitTime = visitTime,
                StaffID = staffID,
                StaffName = staffName,
                BranchName = branchName,
                BranchCode = branchCode,
                CustomerName = customerName,
                CustomerMobileNo = customerMobileNo,
                CustomerRequirement = customerRequirement,
                Comments = comments,
                Latitude = latitude,
                Longitude = longitude,
                FullAddress = fullAddress,
                image = imagePart
            ).enqueue(object : Callback<MarketingVisitResponse> {
                override fun onResponse(
                    call: Call<MarketingVisitResponse>,
                    response: Response<MarketingVisitResponse>
                ) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        showCustomToast("Marketing visit recorded successfully!")
                        startActivity(Intent(this@MarketingVisitActivity, VisitActivity::class.java))
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
                        showCustomToast("API Error: $errorMsg")
                    }
                }

                override fun onFailure(call: Call<MarketingVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showCustomToast("Network Error: ${t.localizedMessage}")
                }
            })
        }
    }



    private fun createPart(value: String): RequestBody =
        RequestBody.create("text/plain".toMediaTypeOrNull(), value)

    private fun createImagePart(file: File?, partName: String = "Image"): MultipartBody.Part? {
        val compressedFile = file?.let { compressImage(it, 50) } // compress to ~50KB
        return compressedFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
            Log.d("ImageCompression", "Sending Image: ${it.name}, Size: ${it.length() / 1024} KB")
            MultipartBody.Part.createFormData(partName, it.name, requestFile)
        }
    }

    private fun compressImage(file: File, maxKb: Int = 50): File? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val outFile = File(cacheDir, "compressed_${file.name}")

            var compressQuality = 100
            var streamLength: Int

            do {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
                streamLength = stream.toByteArray().size / 1024
                compressQuality -= 5
                stream.close()
            } while (streamLength > maxKb && compressQuality > 5)

            val fos = FileOutputStream(outFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
            fos.flush()
            fos.close()

            Log.d("ImageCompression", "Compressed Image: ${outFile.name}, Size: ${outFile.length() / 1024} KB")
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun convertDateForServer(displayDate: String): String = try {
        val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date: Date = displayFormat.parse(displayDate)!!
        serverFormat.format(date)
    } catch (e: Exception) {
        ""
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

}