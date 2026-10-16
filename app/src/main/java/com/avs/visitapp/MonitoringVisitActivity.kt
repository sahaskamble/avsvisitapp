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
import com.google.android.gms.location.Priority
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
import com.avs.visitapp.Adapter.MonitoringCustomerAdapter
import com.avs.visitapp.Adapter.SimpleTextWatcher
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.MonitoringVisitActivityBinding
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
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@RequiresApi(Build.VERSION_CODES.M)
class MonitoringVisitActivity : AppCompatActivity() {

    private lateinit var binding: MonitoringVisitActivityBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null
    private var isImageClicked = false
    private val CAMERA_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private var latitude = 0.0
    private var longitude = 0.0
    private var fullAddress = ""
    private var currentDateTime = ""
    private var loadingDialog: AlertDialog? = null
    private lateinit var adapter: MonitoringCustomerAdapter


    private val loanAcCustName = mutableListOf<String>()

    private val customerMap = mutableMapOf<String, CustomerNameResponse>()


    private val loanCategoryOptions = listOf("Standard", "Sub Standard", "D-1", "D-2", "D-3", "Loss")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MonitoringVisitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.LoanDisbDate.setOnClickListener {
            openDatePicker()
        }


        setupLoanAmountFormatting()

        val MonitoringVisitmobileLayout = binding.CustMobNoLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val MonitoringVisitmobileEditText = binding.CustMobNo.findViewById<TextInputEditText>(R.id.CustMobNo)

        MonitoringVisitmobileEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    MonitoringVisitmobileLayout.boxStrokeColor = ContextCompat.getColor(this@MonitoringVisitActivity, R.color.red)
                    MonitoringVisitmobileLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    MonitoringVisitmobileLayout.boxStrokeColor = ContextCompat.getColor(this@MonitoringVisitActivity, R.color.colorPrimaryDark)
                    MonitoringVisitmobileLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        binding.submit.setOnClickListener {
            if (validateAndSubmit()) {
                submitMonitoringVisit()
            }
        }


        binding.fabAdd.setOnClickListener {
            // Hide RadioGroup
            binding.radioGroup.visibility = View.GONE
            binding.fabAdd.visibility = View.GONE
            binding.rvvisit.visibility = View.GONE

            // Show ScrollView
            binding.scrollContent.visibility = View.VISIBLE
        }


        adapter = MonitoringCustomerAdapter(emptyList())
        binding.rvvisit.layoutManager = LinearLayoutManager(this)
        binding.rvvisit.adapter = adapter

        val StaffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toInt() ?: 0

        fetchDoneCount(StaffID, "MQ")
        fetchPendingCount(StaffID, "MQ")


        binding.rbDone.setOnClickListener {
            binding.rbPending.isChecked = false
            fetchDoneVisits(StaffID = StaffID, flag = "MQ")
        }

        binding.rbPending.setOnClickListener {
            binding.rbDone.isChecked = false
            fetchPendingVisits(StaffID = StaffID, flag = "MQ")
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        setupFieldWatchers()
    }


    private fun fetchDoneCount(StaffID: Int, flag: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllDoneVisits(StaffID, flag)
                if (response.isSuccessful) {
                    val doneVisitResponse = response.body()
                    withContext(Dispatchers.Main) {
                        binding.tvDoneCount.text = doneVisitResponse?.TotalCount?.toString() ?: "0"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvDoneCount.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvDoneCount.text = "0"
                }
            }
        }
    }

    private fun fetchPendingCount(StaffID: Int, flag: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllPendingVisits(StaffID, flag)
                if (response.isSuccessful) {
                    val pendingVisitResponse = response.body()
                    withContext(Dispatchers.Main) {
                        binding.tvPendingCount.text = pendingVisitResponse?.TotalCount?.toString() ?: "0"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvPendingCount.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvPendingCount.text = "0"
                }
            }
        }
    }


    private fun fetchDoneVisits(StaffID: Int, flag: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllDoneVisits(StaffID, flag)
                if (response.isSuccessful) {
                    val doneVisitResponse = response.body()
                    val visits = doneVisitResponse?.Visits
                    withContext(Dispatchers.Main) {
                        binding.tvDoneCount.text = doneVisitResponse?.TotalCount?.toString() ?: "0"

                        if (!visits.isNullOrEmpty()) {
                            adapter.updateList(visits)
                        } else {
                            showCustomToast("No visits found")
                            adapter.updateList(emptyList())
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showCustomToast("API Error: ${response.code()}")
                        binding.tvDoneCount.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showCustomToast("Exception: ${e.message}")
                    binding.tvDoneCount.text = "0"
                }
            }
        }
    }

    private fun fetchPendingVisits(StaffID: Int, flag: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllPendingVisits(StaffID, flag)
                if (response.isSuccessful) {
                    val pendingVisitResponse = response.body()
                    val visits = pendingVisitResponse?.Visits
                    withContext(Dispatchers.Main) {
                        binding.tvPendingCount.text = pendingVisitResponse?.TotalCount?.toString() ?: "0"

                        if (!visits.isNullOrEmpty()) {
                            adapter.updateList(visits)
                        } else {
                            showCustomToast("No pending visits found")
                            adapter.updateList(emptyList())
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showCustomToast("API Error: ${response.code()}")
                        binding.tvPendingCount.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showCustomToast("Exception: ${e.message}")
                    binding.tvPendingCount.text = "0"
                }
            }
        }
    }

    private fun showCustomToast(message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.toast_custom, binding.root, false)

        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()
    }



    private fun setupBranchDropdown() {
        setupDropdowns()
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

                            // Show branch code automatically
                            binding.branchCode.setText(selectedCode, false)

                            // Fetch loan follow-ups
                            selectedBank.BRCD?.let { brcd ->
                                fetchLoanFollowUps(brcd)
                            }
                        } else {
                            Log.e("BranchSelection", "No matching bank found for name: $selectedName")
                        }
                    }

                } else {
                    Log.e("BranchAPI", "Error response: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@MonitoringVisitActivity,
                        "Failed to fetch branches",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MonitoringVisitActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


//    private fun fetchLoanFollowUps(branchCode: Int) {
//        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        lifecycleScope.launch {
//            try {
//                val customers = ApiClient.apiService.getmonitoringLoanFollowUps(branchCode, Empid)
//
//                loanAcCustName.clear()
//                customerMap.clear()
//
//                if (customers.isNullOrEmpty()) {
//                    // No data returned, clear fields and show toast once
//                    clearCustomerFields()
//                    findViewById<AutoCompleteTextView>(R.id.Custname).isEnabled = false
//                    Toast.makeText(
//                        this@MonitoringVisitActivity,
//                        "No customer assigned for selected branch",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@launch
//                }
//
//                customers.forEach { customer ->
//                    val name = customer.Name.trim()
//                    loanAcCustName.add(name)
//                    customerMap[name] = customer
//                }
//
//                // Update customer name dropdown
//                setupCustNameDropdown()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                clearCustomerFields()
//                findViewById<AutoCompleteTextView>(R.id.Custname).isEnabled = false
//                Toast.makeText(this@MonitoringVisitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }


    private fun fetchLoanFollowUps(branchCode: Int) {
        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")

        // 1. START LOADING STATE
        // Clear any old text immediately
        binding.Custname.setText(null)
        // Change the floating label (hint) to show loading status
        binding.CustnameLayout.hint = "Loading Customers..."
        // Disable the field to prevent clicks during fetch
        binding.Custname.isEnabled = false

        lifecycleScope.launch {
            try {
                val customers = ApiClient.apiService.getmonitoringLoanFollowUps(branchCode, Empid)

                loanAcCustName.clear()
                customerMap.clear()

                if (customers.isNullOrEmpty()) {
                    clearCustomerFields()

                    // 2. NO DATA STATE
                    binding.CustnameLayout.hint = "No Customers Found"
                    binding.Custname.isEnabled = false

                    showCustomToast("No customer assigned for selected branch")
                    return@launch
                }

//                customers.forEach { customer ->
//                    val name = customer.Name.trim()
//                    loanAcCustName.add(name)
//                    customerMap[name] = customer
//                }

                customers.forEach { customer ->
                    // OLD CODE (Causes Crash):
                    // val name = customer.Name.trim()

                    // NEW CODE (Safe):
                    // If Name is null, use empty string "" instead of crashing
                    val name = customer.Name?.trim() ?: ""

                    if (name.isNotEmpty()) { // Optional: Skip empty names if you want
                        loanAcCustName.add(name)
                        customerMap[name] = customer
                    }
                }

                // 3. SUCCESS STATE
                // Restore the original hint
                binding.CustnameLayout.hint = "Customer Name with A/c. No"
                // Enable the field for interaction
                binding.Custname.isEnabled = true

                // Attach the data to the dropdown
                setupCustNameDropdown()

            } catch (e: Exception) {
                e.printStackTrace()
                clearCustomerFields()

                // 4. ERROR STATE
                binding.CustnameLayout.hint = "Error Loading Data"
                binding.Custname.isEnabled = false

                showCustomToast("Error: ${e.message}")
            }
        }
    }

    private fun setupCustNameDropdown() {
        val custName = findViewById<AutoCompleteTextView>(R.id.Custname)

        val uniqueCustomerNames = loanAcCustName.distinct()

        if (uniqueCustomerNames.isEmpty()) {
            clearCustomerFields()
            custName.isEnabled = false
            return
        } else {
            custName.isEnabled = true
            setupDropdown(custName, uniqueCustomerNames)
        }

        custName.setOnItemClickListener { _, _, _, _ ->
            val selectedName = custName.text.toString().trim()
            val customer = customerMap[selectedName]

            customer?.let {
                binding.LoanType.setText(it.LOANGLCODE?.toString() ?: "")


                binding.CustMobNo.setText(it.MobileNo ?: "")

                val accountNumber = try {
                    it.CUSTACCNO?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                } catch (e: Exception) {
                    0L
                }
                binding.LoanAcNo.setText(accountNumber.toString())
            }
        }
    }



    private fun clearCustomerFields() {
        binding.LoanType.setText("")
        binding.CustMobNo.setText("")
        binding.LoanAcNo.setText("")
        findViewById<AutoCompleteTextView>(R.id.Custname).setText("")
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

    private fun setupDropdowns() {
        val customLayout = R.layout.dropdown_item

        // Helper function to setup AutoCompleteTextView
        fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
            val adapter = ArrayAdapter(this, customLayout, data)
            autoComplete.setAdapter(adapter)

            // Show all items on first click
            autoComplete.setOnClickListener {
                if (!autoComplete.isPopupShowing) {
                    autoComplete.showDropDown() // show all initially
                }
            }

            // Show dropdown as user types (filters automatically)
            autoComplete.threshold = 1

            // Ensure keyboard opens
            autoComplete.isFocusable = true
            autoComplete.isFocusableInTouchMode = true
        }

        // Loan Category
        val loanCategory = findViewById<AutoCompleteTextView>(R.id.LoanCategory)
        setupDropdown(loanCategory, loanCategoryOptions)


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

//    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    private fun fetchLocationAndOverlayImage() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                latitude = location.latitude
//                longitude = location.longitude
//                fullAddress = getAddressFromLocation(latitude, longitude)
//
//                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) // 争 dash format
//                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//                currentDateTime = currentDate
//
//                binding.etLat.setText(latitude.toString())
//                binding.etLong.setText(longitude.toString())
//                binding.etDate.setText(currentDate) // 争 dd-MM-yyyy
//                binding.etVisitTime.setText(currentTime)
//                binding.etAddress.setText(fullAddress)
//
//                overlayImage(photoFile!!.absolutePath)
//            } else {
//                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }


    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchLocationAndOverlayImage() {
        // 1. Check Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showCustomToast("Location permission not granted")
            return
        }

        // 2. Show Loading Dialog (Fetching live location takes time)
        showLoadingDialog()

        // 3. Fetch LIVE location (High Accuracy)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                dismissLoadingDialog() // Hide loader

                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    fullAddress = getAddressFromLocation(latitude, longitude)

                    val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    currentDateTime = currentDate

                    binding.etLat.setText(latitude.toString())
                    binding.etLong.setText(longitude.toString())
                    binding.etDate.setText(currentDate)
                    binding.etVisitTime.setText(currentTime)
                    binding.etAddress.setText(fullAddress)

                    // Overlay text on image
                    overlayImage(photoFile!!.absolutePath)
                } else {
                    showCustomToast("Unable to fetch location. Please ensure location is enabled.")
                }
            }
            .addOnFailureListener { e ->
                dismissLoadingDialog()
                showCustomToast("Failed to get location: ${e.message}")
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
            "桃 $fullAddress",
            "套 $currentDateTime",
            "倹 Lat: $latitude",
            "倹 Long: $longitude"
        )

        val x = 30f
        val yStart = mutableBitmap.height - lines.size * 80f - 30f

        canvas.drawRect(0f, yStart - 40f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)

        var y = yStart
        lines.forEach {
            canvas.drawText(it, x, y, paint)
            y += 80f
        }

//        saveBitmapToGallery(mutableBitmap)
        binding.profilePhoto.setImageBitmap(mutableBitmap)
    }



    private fun enableFieldsAfterPhoto() {
        binding.branchName.isEnabled = true
        binding.Custname.isEnabled = true
        binding.LoanAcNo.isEnabled = true
        binding.LoanType.isEnabled = true
        binding.CustMobNo.isEnabled = true
        binding.LoanCategory.isEnabled = true
        binding.LoanAmount.isEnabled = true
        binding.LoanDisbDate.isEnabled = true
        binding.EndUseFunds.isEnabled = true
        binding.OtherRemarks.isEnabled = true // Added enable for new field
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // Format date and set to EditText
                val selectedDate = String.format(
                    "%02d-%02d-%04d",
                    selectedDayOfMonth,
                    selectedMonth + 1,
                    selectedYear
                )
                binding.LoanDisbDate.setText(selectedDate)
            },
            year,
            month,
            day
        )

        // Prevent future dates
        datePicker.datePicker.maxDate = calendar.timeInMillis

        datePicker.show()
    }


    private fun setupLoanAmountFormatting() {
        binding.LoanAmount.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.LoanAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace(",", "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toLong()
                            // Use Indian Number Format
                            val formatter = NumberFormat.getInstance(Locale("en", "IN")) as DecimalFormat
                            formatter.applyPattern("#,##,##0")
                            val formatted = formatter.format(parsed)
                            current = formatted
                            binding.LoanAmount.setText(formatted)
                            binding.LoanAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }

                    binding.LoanAmount.addTextChangedListener(this)
                }
            }
        })
    }

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

        if (binding.LoanAcNo.text.isNullOrBlank()) {
            missingFields.add("Loan Account No")
            setFieldError(binding.LoanAcNoLayout, true)
        } else {
            setFieldError(binding.LoanAcNoLayout, false)
        }

        if (binding.LoanType.text.isNullOrBlank()) {
            missingFields.add("Loan Type")
            setFieldError(binding.LoanTypeLayout, true)
        } else {
            setFieldError(binding.LoanTypeLayout, false)
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

        if (binding.LoanCategory.text.isNullOrBlank()) {
            missingFields.add("Loan Category")
            setFieldError(binding.LoanCategoryLayout, true)
        } else {
            setFieldError(binding.LoanCategoryLayout, false)
        }

        if (binding.LoanAmount.text.isNullOrBlank()) {
            missingFields.add("Loan Amount")
            setFieldError(binding.LoanAmountLayout, true)
        } else {
            setFieldError(binding.LoanAmountLayout, false)
        }

        if (binding.LoanDisbDate.text.isNullOrBlank()) {
            missingFields.add("Loan Disbursed Date")
            setFieldError(binding.LoanDisbDateLayout, true)
        } else {
            setFieldError(binding.LoanDisbDateLayout, false)
        }

        if (binding.EndUseFunds.text.isNullOrBlank()) {
            missingFields.add("End Use Funds")
            setFieldError(binding.EndUseFundsLayout, true)
        } else {
            setFieldError(binding.EndUseFundsLayout, false)
        }

        // Added validation for Other Remarks
        if (binding.OtherRemarks.text.isNullOrBlank()) {
            missingFields.add("Other Remarks")
            setFieldError(binding.OtherRemarksLayout, true)
        } else {
            setFieldError(binding.OtherRemarksLayout, false)
        }


        return missingFields
    }


    private fun setFieldError(layout: TextInputLayout, isError: Boolean) {
        if (isError) {
            layout.boxStrokeColor = resources.getColor(R.color.red, null)
        }
        else {
            layout.boxStrokeColor = resources.getColor(R.color.colorPrimaryDark, null)
        }
    }

    private fun setupFieldWatchers() {
        // Map each EditText to its corresponding TextInputLayout
        val fieldMap = mapOf(
            binding.etDate to binding.etDateLayout,
            binding.etVisitTime to binding.etVisitTimeLayout,
            binding.etLat to binding.etLatLayout,
            binding.etLong to binding.etLongLayout,
            binding.etAddress to binding.etAddressLayout,
            binding.StaffName to binding.StaffNameLayout,
            binding.branchName to binding.branchNameLayout,
            binding.branchCode to binding.branchCodeLayout,
            binding.Custname to binding.CustnameLayout,
            binding.LoanAcNo to binding.LoanAcNoLayout,
            binding.LoanType to binding.LoanTypeLayout,
            binding.CustMobNo to binding.CustMobNoLayout,
            binding.LoanCategory to binding.LoanCategoryLayout,
            binding.LoanAmount to binding.LoanAmountLayout,
            binding.LoanDisbDate to binding.LoanDisbDateLayout,
            binding.EndUseFunds to binding.EndUseFundsLayout,
            binding.OtherRemarks to binding.OtherRemarksLayout // Added watcher for new field
        )

        // Attach TextChangedListener and touch listener to each field
        fieldMap.forEach { (editText, layout) ->
            // Reset error on text change
            editText.addTextChangedListener(SimpleTextWatcher { setFieldError(layout, false) })

            // Block interaction until image is clicked
            editText.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                    showCustomToast("Please first click image")
                    v.clearFocus()
                    true // consume event
                } else {
                    false // allow interaction
                }
            }
        }
    }


    private fun submitMonitoringVisit() {
        showLoadingDialog() // show loader immediately

        val StaffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toIntOrNull() ?: 0

        lifecycleScope.launch {
            // Compress image off the main thread
            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile) }

            // Convert text fields to RequestBody
            val visitDate = createPart(convertDateForServer(binding.etDate.text?.toString() ?: ""))
            val visitTime = createPart(binding.etVisitTime.text?.toString() ?: "")
            val StaffID = createPart(StaffID.toString())
            val staffName = createPart(binding.StaffName.text?.toString() ?: "")
            val branchName = createPart(binding.branchName.text?.toString() ?: "")
            val branchCode = createPart(binding.branchCode.text?.toString() ?: "")
            val customerNameWithAccountNo = createPart(binding.Custname.text?.toString() ?: "")
            val loanAccountNo = createPart(binding.LoanAcNo.text?.toString() ?: "")
            val loanType = createPart(binding.LoanType.text?.toString() ?: "")
            val customerMobileNo = createPart(binding.CustMobNo.text?.toString() ?: "")
            val loanCategory = createPart(binding.LoanCategory.text?.toString() ?: "")
            val loanAmount = createPart(parseAmountForServer(binding.LoanAmount.text?.toString() ?: ""))
            val loanDisbursedDate = createPart(convertDateForServer(binding.LoanDisbDate.text?.toString() ?: ""))
            val endUseFundsComments = createPart(binding.EndUseFunds.text?.toString() ?: "")
            val otherRemarks = createPart(binding.OtherRemarks.text?.toString() ?: "") // Added new field
            val latitude = createPart(binding.etLat.text?.toString() ?: "")
            val longitude = createPart(binding.etLong.text?.toString() ?: "")
            val fullAddress = createPart(binding.etAddress.text?.toString() ?: "")

            // API call
            ApiClient.apiService.InsertMonitoringVisit(
                VisitDate = visitDate,
                VisitTime = visitTime,
                StaffID = StaffID,
                StaffName = staffName,
                BranchName = branchName,
                BranchCode = branchCode,
                CustomerNameWithAccountNo = customerNameWithAccountNo,
                LoanAccountNo = loanAccountNo,
                LoanType = loanType,
                CustomerMobileNo = customerMobileNo,
                LoanCategory = loanCategory,
                LoanAmount = loanAmount,
                LoanDisbursedDate = loanDisbursedDate,
                EndUseFundsComments = endUseFundsComments,
                OtherRemarks = otherRemarks, // Pass to API
                Latitude = latitude,
                Longitude = longitude,
                FullAddress = fullAddress,
                image = imagePart
            ).enqueue(object : Callback<MonitoringVisitResponse> {
                override fun onResponse(call: Call<MonitoringVisitResponse>, response: Response<MonitoringVisitResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@MonitoringVisitActivity, "Monitoring visit recorded successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MonitoringVisitActivity, VisitActivity::class.java))
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
                        Toast.makeText(this@MonitoringVisitActivity, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<MonitoringVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    Toast.makeText(this@MonitoringVisitActivity, "Network Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
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

    private fun parseAmountForServer(amountText: String?): String = try {
        val cleaned = amountText?.replace(",", "") ?: "0"
        String.format("%.2f", cleaned.toDouble())
    } catch (e: Exception) {
        "0.00"
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

//package com.avs.visitapp
//
//import android.Manifest
//import android.app.AlertDialog
//import android.app.DatePickerDialog
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.location.Geocoder
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.text.Editable
//import android.text.TextWatcher
//import android.util.Base64
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.MotionEvent
//import android.view.View
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.annotation.RequiresApi
//import androidx.annotation.RequiresPermission
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.avs.visitapp.Adapter.MonitoringCustomerAdapter
//import com.avs.visitapp.Adapter.SimpleTextWatcher
//import com.avs.visitapp.databinding.DialogLoadingBinding
//import com.avs.visitapp.databinding.MonitoringVisitActivityBinding
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.google.android.material.textfield.TextInputEditText
//import com.google.android.material.textfield.TextInputLayout
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileOutputStream
//import java.text.DecimalFormat
//import java.text.NumberFormat
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Date
//import java.util.Locale
//@RequiresApi(Build.VERSION_CODES.M)
//class MonitoringVisitActivity : AppCompatActivity() {
//
//    private lateinit var binding: MonitoringVisitActivityBinding
//
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private var photoFile: File? = null
//    private var isImageClicked = false
//    private val CAMERA_REQUEST_CODE = 101
//    private val PERMISSION_REQUEST_CODE = 102
//    private var latitude = 0.0
//    private var longitude = 0.0
//    private var fullAddress = ""
//    private var currentDateTime = ""
//    private var loadingDialog: AlertDialog? = null
//    private lateinit var adapter: MonitoringCustomerAdapter
//
//
//    private val loanAcCustName = mutableListOf<String>()
//
//    private val customerMap = mutableMapOf<String, CustomerNameResponse>()
//
//
//    private val loanCategoryOptions = listOf("Standard", "Sub Standard", "D-1", "D-2", "D-3", "Loss")
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = MonitoringVisitActivityBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
////        setSupportActionBar(binding.toolbar)
////        supportActionBar?.setDisplayHomeAsUpEnabled(true)
////        binding.toolbar.setNavigationOnClickListener { finish() }
//
//
//        binding.toolbarBack.setOnClickListener {
//            val intent = Intent(this, VisitActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        setupBranchDropdown()
//
//        binding.profilePhoto.setOnClickListener {
//            checkPermissionsAndOpenCamera()
//        }
//
//        binding.LoanDisbDate.setOnClickListener {
//            openDatePicker()
//        }
//
//
//        setupLoanAmountFormatting()
//
//        val MonitoringVisitmobileLayout = binding.CustMobNoLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
//        val MonitoringVisitmobileEditText = binding.CustMobNo.findViewById<TextInputEditText>(R.id.CustMobNo)
//
//        MonitoringVisitmobileEditText.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                val mobile = s?.toString()?.trim() ?: ""
//
//                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
//                    // Invalid: red outline and error message
//                    MonitoringVisitmobileLayout.boxStrokeColor = ContextCompat.getColor(this@MonitoringVisitActivity, R.color.red)
//                    MonitoringVisitmobileLayout.error = "Enter 10 digit number"
//                } else {
//                    // Valid: reset to default
//                    MonitoringVisitmobileLayout.boxStrokeColor = ContextCompat.getColor(this@MonitoringVisitActivity, R.color.colorPrimaryDark)
//                    MonitoringVisitmobileLayout.error = null
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//
//
//        binding.submit.setOnClickListener {
//            if (validateAndSubmit()) {
//                submitMonitoringVisit()
//            }
//        }
//
//
//        binding.fabAdd.setOnClickListener {
//            // Hide RadioGroup
//            binding.radioGroup.visibility = View.GONE
//            binding.fabAdd.visibility = View.GONE
//            binding.rvvisit.visibility = View.GONE
//
//            // Hide only the calendar icon
////            binding.toolbarCalendar.visibility = View.GONE
//
//            // Show ScrollView
//            binding.scrollContent.visibility = View.VISIBLE
//        }
//
//
//        adapter = MonitoringCustomerAdapter(emptyList())
//        binding.rvvisit.layoutManager = LinearLayoutManager(this)
//        binding.rvvisit.adapter = adapter
//
//        val StaffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toInt() ?: 0
//
//        fetchDoneCount(StaffID, "MQ")
//        fetchPendingCount(StaffID, "MQ")
//
//
//        binding.rbDone.setOnClickListener {
//            binding.rbPending.isChecked = false
//            fetchDoneVisits(StaffID = StaffID, flag = "MQ")
//        }
//
//        binding.rbPending.setOnClickListener {
//            binding.rbDone.isChecked = false
//            fetchPendingVisits(StaffID = StaffID, flag = "MQ")
//        }
//
//
////        binding.toolbarCalendar.setOnClickListener {
////            val calendar = Calendar.getInstance()
////            val year = calendar.get(Calendar.YEAR)
////            val month = calendar.get(Calendar.MONTH)
////            val day = calendar.get(Calendar.DAY_OF_MONTH)
////
////            val datePickerDialog = DatePickerDialog(
////                this,
////                { _, selectedYear, selectedMonth, selectedDay ->
////                },
////                year,
////                month,
////                day
////            )
////            datePickerDialog.show()
////        }
//
//        setupFieldWatchers()
//
//
//    }
//
//
//    private fun fetchDoneCount(StaffID: Int, flag: String) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getAllDoneVisits(StaffID, flag)
//                if (response.isSuccessful) {
//                    val doneVisitResponse = response.body()
//                    withContext(Dispatchers.Main) {
//                        binding.tvDoneCount.text = doneVisitResponse?.TotalCount?.toString() ?: "0"
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        binding.tvDoneCount.text = "0"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    binding.tvDoneCount.text = "0"
//                }
//            }
//        }
//    }
//
//    private fun fetchPendingCount(StaffID: Int, flag: String) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getAllPendingVisits(StaffID, flag)
//                if (response.isSuccessful) {
//                    val pendingVisitResponse = response.body()
//                    withContext(Dispatchers.Main) {
//                        binding.tvPendingCount.text = pendingVisitResponse?.TotalCount?.toString() ?: "0"
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        binding.tvPendingCount.text = "0"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    binding.tvPendingCount.text = "0"
//                }
//            }
//        }
//    }
//
//
//    private fun fetchDoneVisits(StaffID: Int, flag: String) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getAllDoneVisits(StaffID, flag)
//                if (response.isSuccessful) {
//                    val doneVisitResponse = response.body()
//                    val visits = doneVisitResponse?.Visits
//                    withContext(Dispatchers.Main) {
//                        binding.tvDoneCount.text = doneVisitResponse?.TotalCount?.toString() ?: "0"
//
//                        if (!visits.isNullOrEmpty()) {
//                            adapter.updateList(visits)
//                        } else {
//                            showCustomToast("No visits found")
//                            adapter.updateList(emptyList())
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        showCustomToast("API Error: ${response.code()}")
//                        binding.tvDoneCount.text = "0"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    showCustomToast("Exception: ${e.message}")
//                    binding.tvDoneCount.text = "0"
//                }
//            }
//        }
//    }
//
//    private fun fetchPendingVisits(StaffID: Int, flag: String) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getAllPendingVisits(StaffID, flag)
//                if (response.isSuccessful) {
//                    val pendingVisitResponse = response.body()
//                    val visits = pendingVisitResponse?.Visits
//                    withContext(Dispatchers.Main) {
//                        binding.tvPendingCount.text = pendingVisitResponse?.TotalCount?.toString() ?: "0"
//
//                        if (!visits.isNullOrEmpty()) {
//                            adapter.updateList(visits)
//                        } else {
//                            showCustomToast("No pending visits found")
//                            adapter.updateList(emptyList())
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        showCustomToast("API Error: ${response.code()}")
//                        binding.tvPendingCount.text = "0"
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    showCustomToast("Exception: ${e.message}")
//                    binding.tvPendingCount.text = "0"
//                }
//            }
//        }
//    }
//
//    private fun showCustomToast(message: String) {
//        val inflater = layoutInflater
//        val layout = inflater.inflate(R.layout.toast_custom, binding.root, false)
//
//        val textView = layout.findViewById<TextView>(R.id.toast_text)
//        textView.text = message
//
//        val toast = Toast(applicationContext)
//        toast.duration = Toast.LENGTH_SHORT
//        toast.view = layout
//        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
//        toast.show()
//    }
//
//
//
//    private fun setupBranchDropdown() {
//        setupDropdowns()
//        lifecycleScope.launch {
//            try {
//                val response = ApiClient.apiService.getAllBanks()
//                if (response.isSuccessful) {
//                    val banks = response.body() ?: emptyList()
//
//                    // Extract only names for dropdown
//                    val branchNames = banks.mapNotNull { it.MIDNAME }
//
//                    setupDropdown(binding.branchName, branchNames)
//
//                    // On branch select
//                    binding.branchName.setOnItemClickListener { _, _, _, _ ->
//                        val selectedName = binding.branchName.text.toString().trim()
//
//                        // Find matching bank by name
//                        val selectedBank = banks.find { it.MIDNAME?.trim() == selectedName }
//
//                        if (selectedBank != null) {
//                            val selectedCode = selectedBank.BRCD?.toString() ?: "0"
//
//                            Log.d(
//                                "BranchSelection",
//                                "Selected: $selectedName, Code: $selectedCode"
//                            )
//
//                            // Show branch code automatically
//                            binding.branchCode.setText(selectedCode, false)
//
//                            // Fetch loan follow-ups
//                            selectedBank.BRCD?.let { brcd ->
//                                fetchLoanFollowUps(brcd)
//                            }
//                        } else {
//                            Log.e("BranchSelection", "No matching bank found for name: $selectedName")
//                        }
//                    }
//
//                } else {
//                    Log.e("BranchAPI", "Error response: ${response.errorBody()?.string()}")
//                    Toast.makeText(
//                        this@MonitoringVisitActivity,
//                        "Failed to fetch branches",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(
//                    this@MonitoringVisitActivity,
//                    "Error: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }
//
//
//    private fun fetchLoanFollowUps(branchCode: Int) {
//        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        lifecycleScope.launch {
//            try {
//                val customers = ApiClient.apiService.getmonitoringLoanFollowUps(branchCode, Empid)
//
//                loanAcCustName.clear()
//                customerMap.clear()
//
//                if (customers.isNullOrEmpty()) {
//                    // No data returned, clear fields and show toast once
//                    clearCustomerFields()
//                    findViewById<AutoCompleteTextView>(R.id.Custname).isEnabled = false
//                    Toast.makeText(
//                        this@MonitoringVisitActivity,
//                        "No customer assigned for selected branch",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@launch
//                }
//
//                customers.forEach { customer ->
//                    val name = customer.Name.trim()
//                    loanAcCustName.add(name)
//                    customerMap[name] = customer
//                }
//
//                // Update customer name dropdown
//                setupCustNameDropdown()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                clearCustomerFields()
//                findViewById<AutoCompleteTextView>(R.id.Custname).isEnabled = false
//                Toast.makeText(this@MonitoringVisitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }
//
//    private fun setupCustNameDropdown() {
//        val custName = findViewById<AutoCompleteTextView>(R.id.Custname)
//
//        val uniqueCustomerNames = loanAcCustName.distinct()
//
//        if (uniqueCustomerNames.isEmpty()) {
//            clearCustomerFields()
//            custName.isEnabled = false
//            return
//        } else {
//            custName.isEnabled = true
//            setupDropdown(custName, uniqueCustomerNames)
//        }
//
//        custName.setOnItemClickListener { _, _, _, _ ->
//            val selectedName = custName.text.toString().trim()
//            val customer = customerMap[selectedName]
//
//            customer?.let {
//                binding.LoanType.setText(it.LOANGLCODE?.toString() ?: "")
//
//
//                binding.CustMobNo.setText(it.MobileNo ?: "")
//
//                val accountNumber = try {
//                    it.CUSTACCNO?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
//                } catch (e: Exception) {
//                    0L
//                }
//                binding.LoanAcNo.setText(accountNumber.toString())
//            }
//        }
//    }
//
//
//
//    private fun clearCustomerFields() {
//        binding.LoanType.setText("")
//        binding.CustMobNo.setText("")
//        binding.LoanAcNo.setText("")
//        findViewById<AutoCompleteTextView>(R.id.Custname).setText("")
//    }
//
//    private val customLayout = R.layout.dropdown_item
//
//    fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
//        val adapter = ArrayAdapter(this, customLayout, data)
//        autoComplete.setAdapter(adapter)
//
//        // Show all items on first click
//        autoComplete.setOnClickListener {
//            if (!autoComplete.isPopupShowing) {
//                autoComplete.showDropDown()
//            }
//        }
//
//        // Filter as user types
//        autoComplete.threshold = 1
//        autoComplete.isFocusable = true
//        autoComplete.isFocusableInTouchMode = true
//    }
//
//    private fun setupDropdowns() {
//        val customLayout = R.layout.dropdown_item
//
//        // Helper function to setup AutoCompleteTextView
//        fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
//            val adapter = ArrayAdapter(this, customLayout, data)
//            autoComplete.setAdapter(adapter)
//
//            // Show all items on first click
//            autoComplete.setOnClickListener {
//                if (!autoComplete.isPopupShowing) {
//                    autoComplete.showDropDown() // show all initially
//                }
//            }
//
//            // Show dropdown as user types (filters automatically)
//            autoComplete.threshold = 1
//
//            // Ensure keyboard opens
//            autoComplete.isFocusable = true
//            autoComplete.isFocusableInTouchMode = true
//        }
//
//        // Loan Category
//        val loanCategory = findViewById<AutoCompleteTextView>(R.id.LoanCategory)
//        setupDropdown(loanCategory, loanCategoryOptions)
//
//
//    }
//
//
//    private fun checkPermissionsAndOpenCamera() {
//        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//
//        if (cameraPermission && fineLocationPermission && coarseLocationPermission) {
//            openCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(
//                    Manifest.permission.CAMERA,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ),
//                PERMISSION_REQUEST_CODE
//            )
//        }
//    }
//
//    // Handle permission result
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            val perms = permissions.zip(grantResults.toTypedArray()).toMap()
//            val cameraGranted = perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED
//            val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED
//            val coarseLocationGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == PackageManager.PERMISSION_GRANTED
//
//            if (cameraGranted && fineLocationGranted && coarseLocationGranted) {
//                openCamera()
//            } else {
//                Toast.makeText(this, "Camera and Location permissions are required", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun openCamera() {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        try {
//            photoFile = createImageFile()
//            val authority = "$packageName.fileprovider"
//            val photoUri: Uri = FileProvider.getUriForFile(this, authority, photoFile!!)
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
//            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivityForResult(intent, CAMERA_REQUEST_CODE)
//        } catch (e: Exception) {
//            Toast.makeText(this, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    @Throws(Exception::class)
//    private fun createImageFile(): File {
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
//    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
//            fetchLocationAndOverlayImage()
//            enableFieldsAfterPhoto()
//            isImageClicked = true
//
//            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
//            binding.StaffName.setText(staffName)
//
//        }
//    }
//
//    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    private fun fetchLocationAndOverlayImage() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                latitude = location.latitude
//                longitude = location.longitude
//                fullAddress = getAddressFromLocation(latitude, longitude)
//
//                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) // 👈 dash format
//                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//                currentDateTime = currentDate
//
//                binding.etLat.setText(latitude.toString())
//                binding.etLong.setText(longitude.toString())
//                binding.etDate.setText(currentDate) // 👈 dd-MM-yyyy
//                binding.etVisitTime.setText(currentTime)
//                binding.etAddress.setText(fullAddress)
//
//                overlayImage(photoFile!!.absolutePath)
//            } else {
//                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//
//    private fun getAddressFromLocation(lat: Double, lon: Double): String {
//        return try {
//            val geocoder = Geocoder(this, Locale.getDefault())
//            val addresses = geocoder.getFromLocation(lat, lon, 1)
//            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Address not found"
//        } catch (e: Exception) {
//            e.printStackTrace()
//            "Address not found"
//        }
//    }
//
//    private fun overlayImage(imagePath: String) {
//        val bitmap = BitmapFactory.decodeFile(imagePath)
//        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(mutableBitmap)
//
//        val paint = Paint().apply {
//            color = Color.WHITE
//            textSize = 50f
//            isAntiAlias = true
//            setShadowLayer(10f, 5f, 5f, Color.BLACK)
//        }
//
//        val bgPaint = Paint().apply {
//            color = Color.parseColor("#88000000")
//            style = Paint.Style.FILL
//        }
//
//        val lines = listOf(
//            "📍 $fullAddress",
//            "📅 $currentDateTime",
//            "🌐 Lat: $latitude",
//            "🌐 Long: $longitude"
//        )
//
//        val x = 30f
//        val yStart = mutableBitmap.height - lines.size * 80f - 30f
//
//        canvas.drawRect(0f, yStart - 40f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)
//
//        var y = yStart
//        lines.forEach {
//            canvas.drawText(it, x, y, paint)
//            y += 80f
//        }
//
////        saveBitmapToGallery(mutableBitmap)
//        binding.profilePhoto.setImageBitmap(mutableBitmap)
//    }
//
//
//
//    private fun enableFieldsAfterPhoto() {
//        binding.branchName.isEnabled = true
//        binding.Custname.isEnabled = true
//        binding.LoanAcNo.isEnabled = true
//        binding.LoanType.isEnabled = true
//        binding.CustMobNo.isEnabled = true
//        binding.LoanCategory.isEnabled = true
//        binding.LoanAmount.isEnabled = true
//        binding.LoanDisbDate.isEnabled = true
//        binding.EndUseFunds.isEnabled = true
//    }
//
//    private fun openDatePicker() {
//        val calendar = Calendar.getInstance()
//        val year = calendar.get(Calendar.YEAR)
//        val month = calendar.get(Calendar.MONTH)
//        val day = calendar.get(Calendar.DAY_OF_MONTH)
//
//        val datePicker = DatePickerDialog(
//            this,
//            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
//                // Format date and set to EditText
//                val selectedDate = String.format(
//                    "%02d-%02d-%04d",
//                    selectedDayOfMonth,
//                    selectedMonth + 1,
//                    selectedYear
//                )
//                binding.LoanDisbDate.setText(selectedDate)
//            },
//            year,
//            month,
//            day
//        )
//
//        // Prevent future dates
//        datePicker.datePicker.maxDate = calendar.timeInMillis
//
//        datePicker.show()
//    }
//
//
//    private fun setupLoanAmountFormatting() {
//        binding.LoanAmount.addTextChangedListener(object : TextWatcher {
//            private var current = ""
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (s.toString() != current) {
//                    binding.LoanAmount.removeTextChangedListener(this)
//
//                    val cleanString = s.toString().replace(",", "")
//                    if (cleanString.isNotEmpty()) {
//                        try {
//                            val parsed = cleanString.toLong()
//                            // Use Indian Number Format
//                            val formatter = NumberFormat.getInstance(Locale("en", "IN")) as DecimalFormat
//                            formatter.applyPattern("#,##,##0")
//                            val formatted = formatter.format(parsed)
//                            current = formatted
//                            binding.LoanAmount.setText(formatted)
//                            binding.LoanAmount.setSelection(formatted.length)
//                        } catch (e: NumberFormatException) {
//                            e.printStackTrace()
//                        }
//                    }
//
//                    binding.LoanAmount.addTextChangedListener(this)
//                }
//            }
//        })
//    }
//
//    private fun validateAndSubmit(): Boolean {
//        val missingFields = getMissingFields()
//
//        return if (missingFields.isEmpty()) {
//
//            true
//        } else {
//            val message = "Please fill: ${missingFields.joinToString(", ")}"
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
//            false
//        }
//    }
//
//    private fun getMissingFields(): List<String> {
//        val missingFields = mutableListOf<String>()
//
//        if (binding.etDate.text.isNullOrBlank()) {
//            missingFields.add("Visit Date")
//            setFieldError(binding.etDateLayout, true)
//        } else {
//            setFieldError(binding.etDateLayout, false)
//        }
//
//        if (binding.etVisitTime.text.isNullOrBlank()) {
//            missingFields.add("Visit Time")
//            setFieldError(binding.etVisitTimeLayout, true)
//        } else {
//            setFieldError(binding.etVisitTimeLayout, false)
//        }
//
//        if (binding.etLat.text.isNullOrBlank()) {
//            missingFields.add("Latitude")
//            setFieldError(binding.etLatLayout, true)
//        } else {
//            setFieldError(binding.etLatLayout, false)
//        }
//
//        if (binding.etLong.text.isNullOrBlank()) {
//            missingFields.add("Longitude")
//            setFieldError(binding.etLongLayout, true)
//        } else {
//            setFieldError(binding.etLongLayout, false)
//        }
//
//        if (binding.etAddress.text.isNullOrBlank()) {
//            missingFields.add("Address")
//            setFieldError(binding.etAddressLayout, true)
//        } else {
//            setFieldError(binding.etAddressLayout, false)
//        }
//
//        if (binding.StaffName.text.isNullOrBlank()) {
//            missingFields.add("Staff Name")
//            setFieldError(binding.StaffNameLayout, true)
//        } else {
//            setFieldError(binding.StaffNameLayout, false)
//        }
//
//        if (binding.branchName.text.isNullOrBlank()) {
//            missingFields.add("Branch Name")
//            setFieldError(binding.branchNameLayout, true)
//        } else {
//            setFieldError(binding.branchNameLayout, false)
//        }
//
//        if (binding.branchCode.text.isNullOrBlank()) {
//            missingFields.add("Branch Code")
//            setFieldError(binding.branchCodeLayout, true)
//        } else {
//            setFieldError(binding.branchCodeLayout, false)
//        }
//
//        if (binding.Custname.text.isNullOrBlank()) {
//            missingFields.add("Customer Name with A/c. No")
//            setFieldError(binding.CustnameLayout, true)
//        } else {
//            setFieldError(binding.CustnameLayout, false)
//        }
//
//        if (binding.LoanAcNo.text.isNullOrBlank()) {
//            missingFields.add("Loan Account No")
//            setFieldError(binding.LoanAcNoLayout, true)
//        } else {
//            setFieldError(binding.LoanAcNoLayout, false)
//        }
//
//        if (binding.LoanType.text.isNullOrBlank()) {
//            missingFields.add("Loan Type")
//            setFieldError(binding.LoanTypeLayout, true)
//        } else {
//            setFieldError(binding.LoanTypeLayout, false)
//        }
//
//        if (binding.CustMobNo.text.isNullOrBlank()) {
//            missingFields.add("Customer Mobile No")
//            setFieldError(binding.CustMobNoLayout, true)
//        } else {
//            val mobile = binding.CustMobNo.text.toString().trim()
//            if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
//                missingFields.add("Valid 10-digit Customer Mobile No")
//                setFieldError(binding.CustMobNoLayout, true)
//            } else {
//                setFieldError(binding.CustMobNoLayout, false)
//            }
//        }
//
//        if (binding.LoanCategory.text.isNullOrBlank()) {
//            missingFields.add("Loan Category")
//            setFieldError(binding.LoanCategoryLayout, true)
//        } else {
//            setFieldError(binding.LoanCategoryLayout, false)
//        }
//
//        if (binding.LoanAmount.text.isNullOrBlank()) {
//            missingFields.add("Loan Amount")
//            setFieldError(binding.LoanAmountLayout, true)
//        } else {
//            setFieldError(binding.LoanAmountLayout, false)
//        }
//
//        if (binding.LoanDisbDate.text.isNullOrBlank()) {
//            missingFields.add("Loan Disbursed Date")
//            setFieldError(binding.LoanDisbDateLayout, true)
//        } else {
//            setFieldError(binding.LoanDisbDateLayout, false)
//        }
//
//        if (binding.EndUseFunds.text.isNullOrBlank()) {
//            missingFields.add("End Use Funds")
//            setFieldError(binding.EndUseFundsLayout, true)
//        } else {
//            setFieldError(binding.EndUseFundsLayout, false)
//        }
//
//
//        return missingFields
//    }
//
//
//    private fun setFieldError(layout: TextInputLayout, isError: Boolean) {
//        if (isError) {
//            layout.boxStrokeColor = resources.getColor(R.color.red, null)
//        }
//        else {
//            layout.boxStrokeColor = resources.getColor(R.color.colorPrimaryDark, null)
//        }
//    }
//
//    private fun setupFieldWatchers() {
//        // Map each EditText to its corresponding TextInputLayout
//        val fieldMap = mapOf(
//            binding.etDate to binding.etDateLayout,
//            binding.etVisitTime to binding.etVisitTimeLayout,
//            binding.etLat to binding.etLatLayout,
//            binding.etLong to binding.etLongLayout,
//            binding.etAddress to binding.etAddressLayout,
//            binding.StaffName to binding.StaffNameLayout,
//            binding.branchName to binding.branchNameLayout,
//            binding.branchCode to binding.branchCodeLayout,
//            binding.Custname to binding.CustnameLayout,
//            binding.LoanAcNo to binding.LoanAcNoLayout,
//            binding.LoanType to binding.LoanTypeLayout,
//            binding.CustMobNo to binding.CustMobNoLayout,
//            binding.LoanCategory to binding.LoanCategoryLayout,
//            binding.LoanAmount to binding.LoanAmountLayout,
//            binding.LoanDisbDate to binding.LoanDisbDateLayout,
//            binding.EndUseFunds to binding.EndUseFundsLayout
//        )
//
//        // Attach TextChangedListener and touch listener to each field
//        fieldMap.forEach { (editText, layout) ->
//            // Reset error on text change
//            editText.addTextChangedListener(SimpleTextWatcher { setFieldError(layout, false) })
//
//            // Block interaction until image is clicked
//            editText.setOnTouchListener { v, event ->
//                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
//                    showCustomToast("Please first click image")
//                    v.clearFocus()
//                    true // consume event
//                } else {
//                    false // allow interaction
//                }
//            }
//        }
//    }
//
//
//    private fun submitMonitoringVisit() {
//        showLoadingDialog() // show loader immediately
//
//        val StaffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toIntOrNull() ?: 0
//
//        lifecycleScope.launch {
//            // Compress image off the main thread
//            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile) }
//
//            // Convert text fields to RequestBody
//            val visitDate = createPart(convertDateForServer(binding.etDate.text?.toString() ?: ""))
//            val visitTime = createPart(binding.etVisitTime.text?.toString() ?: "")
//            val StaffID = createPart(StaffID.toString())
//            val staffName = createPart(binding.StaffName.text?.toString() ?: "")
//            val branchName = createPart(binding.branchName.text?.toString() ?: "")
//            val branchCode = createPart(binding.branchCode.text?.toString() ?: "")
//            val customerNameWithAccountNo = createPart(binding.Custname.text?.toString() ?: "")
//            val loanAccountNo = createPart(binding.LoanAcNo.text?.toString() ?: "")
//            val loanType = createPart(binding.LoanType.text?.toString() ?: "")
//            val customerMobileNo = createPart(binding.CustMobNo.text?.toString() ?: "")
//            val loanCategory = createPart(binding.LoanCategory.text?.toString() ?: "")
//            val loanAmount = createPart(parseAmountForServer(binding.LoanAmount.text?.toString() ?: ""))
//            val loanDisbursedDate = createPart(convertDateForServer(binding.LoanDisbDate.text?.toString() ?: ""))
//            val endUseFundsComments = createPart(binding.EndUseFunds.text?.toString() ?: "")
//            val latitude = createPart(binding.etLat.text?.toString() ?: "")
//            val longitude = createPart(binding.etLong.text?.toString() ?: "")
//            val fullAddress = createPart(binding.etAddress.text?.toString() ?: "")
//
//            // API call
//            ApiClient.apiService.InsertMonitoringVisit(
//                VisitDate = visitDate,
//                VisitTime = visitTime,
//                StaffID = StaffID,
//                StaffName = staffName,
//                BranchName = branchName,
//                BranchCode = branchCode,
//                CustomerNameWithAccountNo = customerNameWithAccountNo,
//                LoanAccountNo = loanAccountNo,
//                LoanType = loanType,
//                CustomerMobileNo = customerMobileNo,
//                LoanCategory = loanCategory,
//                LoanAmount = loanAmount,
//                LoanDisbursedDate = loanDisbursedDate,
//                EndUseFundsComments = endUseFundsComments,
//                Latitude = latitude,
//                Longitude = longitude,
//                FullAddress = fullAddress,
//                image = imagePart
//            ).enqueue(object : Callback<MonitoringVisitResponse> {
//                override fun onResponse(call: Call<MonitoringVisitResponse>, response: Response<MonitoringVisitResponse>) {
//                    dismissLoadingDialog()
//                    if (response.isSuccessful && response.body() != null) {
//                        Toast.makeText(this@MonitoringVisitActivity, "Monitoring visit recorded successfully!", Toast.LENGTH_SHORT).show()
//                        startActivity(Intent(this@MonitoringVisitActivity, VisitActivity::class.java))
//                        finish()
//                    } else {
//                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
//                        Toast.makeText(this@MonitoringVisitActivity, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<MonitoringVisitResponse>, t: Throwable) {
//                    dismissLoadingDialog()
//                    Toast.makeText(this@MonitoringVisitActivity, "Network Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
//                }
//            })
//        }
//    }
//
//
//
//    private fun createPart(value: String): RequestBody =
//        RequestBody.create("text/plain".toMediaTypeOrNull(), value)
//
//    private fun createImagePart(file: File?, partName: String = "Image"): MultipartBody.Part? {
//        val compressedFile = file?.let { compressImage(it, 50) } // compress to ~50KB
//        return compressedFile?.let {
//            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
//            Log.d("ImageCompression", "Sending Image: ${it.name}, Size: ${it.length() / 1024} KB")
//            MultipartBody.Part.createFormData(partName, it.name, requestFile)
//        }
//    }
//
//    private fun compressImage(file: File, maxKb: Int = 50): File? {
//        return try {
//            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//            val outFile = File(cacheDir, "compressed_${file.name}")
//
//            var compressQuality = 100
//            var streamLength: Int
//
//            do {
//                val stream = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
//                streamLength = stream.toByteArray().size / 1024
//                compressQuality -= 5
//                stream.close()
//            } while (streamLength > maxKb && compressQuality > 5)
//
//            val fos = FileOutputStream(outFile)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
//            fos.flush()
//            fos.close()
//
//            Log.d("ImageCompression", "Compressed Image: ${outFile.name}, Size: ${outFile.length() / 1024} KB")
//            outFile
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    private fun convertDateForServer(displayDate: String): String = try {
//        val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//        val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val date: Date = displayFormat.parse(displayDate)!!
//        serverFormat.format(date)
//    } catch (e: Exception) {
//        ""
//    }
//
//    private fun parseAmountForServer(amountText: String?): String = try {
//        val cleaned = amountText?.replace(",", "") ?: "0"
//        String.format("%.2f", cleaned.toDouble())
//    } catch (e: Exception) {
//        "0.00"
//    }
//
//    private fun showLoadingDialog() {
//        val dialogBinding = DialogLoadingBinding.inflate(LayoutInflater.from(this))
//        val builder = AlertDialog.Builder(this)
//        builder.setView(dialogBinding.root)
//        builder.setCancelable(false)
//        dialogBinding.tvLoading.text = "Please wait..."
//        loadingDialog = builder.create()
//        loadingDialog?.show()
//    }
//
//    private fun dismissLoadingDialog() {
//        loadingDialog?.dismiss()
//    }
//
//}