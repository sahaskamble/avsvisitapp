//package com.avs.visitapp
//
//import android.Manifest
//import android.app.AlertDialog
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
//import android.view.Gravity
//import android.view.MotionEvent
//import android.view.View
//import android.widget.TextView
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.annotation.RequiresPermission
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.avs.visitapp.Adapter.BankVisitAdapter
//import com.avs.visitapp.databinding.ActivityBankVisitBinding
//import com.avs.visitapp.databinding.DialogLoadingBinding
//import com.avs.visitapp.model.BankVisitData
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.ResponseBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//@RequiresApi(Build.VERSION_CODES.M)
//class BankVisitActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityBankVisitBinding
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var adapter: BankVisitAdapter
//
//    private var photoFile: File? = null
//    private var loadingDialog: AlertDialog? = null
//
//    private val CAMERA_REQUEST_CODE = 101
//    private val PERMISSION_REQUEST_CODE = 102
//
//    private var latitude = 0.0
//    private var longitude = 0.0
//    private var fullAddress = ""
//    private var currentDateTime = ""
//    private var isImageClicked = false
//    private var capturedInTime = ""
//    private var capturedInDate = ""
//
//    // Tracks if we are Inserting a new visit or Updating an existing one
//    private var isUpdateMode = false
//    private var selectedVisitIdForUpdate = 0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityBankVisitBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.toolbarBack.setOnClickListener { finish() }
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        setupFieldWatchers()
//
//        // Camera Triggers
//        binding.profilePhoto.setOnClickListener { checkPermissionsAndOpenCamera() }
//        binding.buttonUpload.setOnClickListener { checkPermissionsAndOpenCamera() }
//
//        // Setup RecyclerView & Adapter
//        adapter = BankVisitAdapter(emptyList()) { selectedVisit ->
//            openUpdateForm(selectedVisit)
//        }
//        binding.rvBankVisits.layoutManager = LinearLayoutManager(this)
//        binding.rvBankVisits.adapter = adapter
//
//        binding.rbPending.setOnClickListener {
//            binding.rbDone.isChecked = false
//            adapter.setCurrentTab("Pending")
//            fetchBankVisits(1001)
//        }
//
//        binding.rbDone.setOnClickListener {
//            binding.rbPending.isChecked = false
//            adapter.setCurrentTab("Done")
//            fetchBankVisits(1002)
//        }
//
//        // Open Add Form
//        binding.fabAdd.setOnClickListener {
//            resetFormForAdd()
//            binding.radioGroupContainer.visibility = View.GONE
//            binding.fabAdd.visibility = View.GONE
//            binding.rvBankVisits.visibility = View.GONE
//            binding.scrollContent.visibility = View.VISIBLE
//        }
//
//        // Handle Form Submission
//        binding.btnSubmitBankVisit.setOnClickListener {
//            if (isUpdateMode) {
//                if (binding.Comments.text.isNullOrBlank()) {
//                    binding.CommentsLayout.error = "Comments are mandatory to complete visit"
//                } else {
//                    submitUpdateBankVisit()
//                }
//            } else {
//                if (validateAndSubmitAdd()) {
//                    submitBankVisit()
//                }
//            }
//        }
//
//        // Load initially
//        fetchBankVisits(1001)
//        binding.rbPending.isChecked = true
//
//        // Fetch background count for done
//        fetchDoneCountInBackground()
//    }
//
////    private fun resetFormForAdd() {
////        isUpdateMode = false
////        selectedVisitIdForUpdate = 0
////        photoFile = null
////        capturedInTime = ""
////        capturedInDate = ""
////        isImageClicked = false
////        latitude = 0.0
////        longitude = 0.0
////        fullAddress = ""
////
////        binding.profilePhoto.setImageResource(R.drawable.user_profile)
////        binding.etDate.setText("")
////        binding.etVisitTime.setText("")
////        binding.etLat.setText("")
////        binding.etLong.setText("")
////        binding.etAddress.setText("")
////        binding.BankName.setText("")
////        binding.BranchName.setText("")
////
////        // Form states for Add mode
////        binding.BankName.isEnabled = false // Unlocks after photo
////        binding.BranchName.isEnabled = false // Unlocks after photo
////        binding.profilePhoto.isEnabled = true
////        binding.buttonUpload.visibility = View.VISIBLE
////
////        // Hide Comments in Add mode based on Postman insert screenshot
////        binding.CommentsLayout.visibility = View.GONE
////        binding.btnSubmitBankVisit.text = "Submit Bank Visit"
////    }
//
////    private fun resetFormForAdd() {
////        isUpdateMode = false
////        selectedVisitIdForUpdate = 0
////        photoFile = null
////        capturedInTime = ""
////        capturedInDate = ""
////        isImageClicked = false
////        latitude = 0.0
////        longitude = 0.0
////        fullAddress = ""
////
////        binding.profilePhoto.setImageResource(R.drawable.user_profile)
////        binding.etDate.setText("")
////        binding.etVisitTime.setText("")
////        binding.etLat.setText("")
////        binding.etLong.setText("")
////        binding.etAddress.setText("")
////        binding.BankName.setText("")
////        binding.BranchName.setText("")
////
////        // Remove the dull/gray effect from all fields
////        val allFields = listOf(
////            binding.etDate, binding.etVisitTime, binding.etLat, binding.etLong,
////            binding.etAddress, binding.BankName, binding.BranchName, binding.StaffName
////        )
////        allFields.forEach { setFieldLockedVisuals(it, false) }
////
////        // Form states for Add mode (Bank/Branch unlock after photo)
////        binding.BankName.isEnabled = false
////        binding.BranchName.isEnabled = false
////
////        // Restore Photo UI
////        binding.profilePhoto.isEnabled = true
////        binding.profilePhoto.alpha = 1.0f
////        binding.buttonUpload.visibility = View.VISIBLE
////
////        // Hide Comments in Add mode based on Postman insert screenshot
////        binding.CommentsLayout.visibility = View.GONE
////        binding.btnSubmitBankVisit.text = "Submit Bank Visit"
////    }
//
//    private fun resetFormForAdd() {
//        isUpdateMode = false
//        selectedVisitIdForUpdate = 0
//        photoFile = null
//        capturedInTime = ""
//        capturedInDate = ""
//        isImageClicked = false
//        latitude = 0.0
//        longitude = 0.0
//        fullAddress = ""
//
//        binding.profilePhoto.setImageResource(R.drawable.user_profile)
//        binding.etDate.setText("")
//        binding.etVisitTime.setText("")
//        binding.etLat.setText("")
//        binding.etLong.setText("")
//        binding.etAddress.setText("")
//        binding.BankName.setText("")
//        binding.BranchName.setText("")
//
//        // 1. System fields are ALWAYS locked and gray
//        val systemFields = listOf(
//            binding.etDate, binding.etVisitTime, binding.etLat, binding.etLong,
//            binding.etAddress, binding.StaffName
//        )
//        systemFields.forEach { setFieldLockedVisuals(it, true) }
//
//        // 2. Bank and Branch start locked and gray (until photo is clicked)
//        setFieldLockedVisuals(binding.BankName, true)
//        setFieldLockedVisuals(binding.BranchName, true)
//
//        // 3. Hide Comments completely in Add Mode
//        binding.CommentsLayout.visibility = View.GONE
//
//        // 4. Restore Photo UI
//        binding.profilePhoto.isEnabled = true
//        binding.profilePhoto.alpha = 1.0f
//        binding.buttonUpload.visibility = View.VISIBLE
//
//        binding.btnSubmitBankVisit.text = "Submit Bank Visit"
//    }
//
////    private fun openUpdateForm(visit: BankVisitData) {
////        isUpdateMode = true
////        selectedVisitIdForUpdate = visit.ID ?: 0
////
////        // Hide list, show form
////        binding.radioGroupContainer.visibility = View.GONE
////        binding.fabAdd.visibility = View.GONE
////        binding.rvBankVisits.visibility = View.GONE
////        binding.scrollContent.visibility = View.VISIBLE
////
////        // Populate fields with API data
////        binding.etDate.setText(visit.VisitDate?.substringBefore("T"))
////        binding.etVisitTime.setText(visit.VisitInTime)
////        binding.etLat.setText(visit.Latitude)
////        binding.etLong.setText(visit.Longitude)
////        binding.etAddress.setText(visit.FullAddress)
////        binding.BankName.setText(visit.BankName)
////        binding.BranchName.setText(visit.BranchName)
////        binding.StaffName.setText(visit.StaffName)
////
////        // Lock existing fields
////        binding.BankName.isEnabled = false
////        binding.BranchName.isEnabled = false
////        binding.profilePhoto.isEnabled = false
////        binding.buttonUpload.visibility = View.GONE // Hide capture button for update
////
////        // Display pre-filled placeholder image (if you have an image URL, use Glide here)
////        binding.profilePhoto.setImageResource(R.drawable.user_profile)
////
////        // Show Comments for Update mode
////        binding.CommentsLayout.visibility = View.VISIBLE
////        binding.CommentsLayout.error = null
////        binding.Comments.isEnabled = true
////        binding.Comments.setText("")
////
////        binding.btnSubmitBankVisit.text = "Mark Done"
////    }
//
//    private fun openUpdateForm(visit: BankVisitData) {
//        isUpdateMode = true
//        selectedVisitIdForUpdate = visit.ID ?: 0
//
//        binding.radioGroupContainer.visibility = View.GONE
//        binding.fabAdd.visibility = View.GONE
//        binding.rvBankVisits.visibility = View.GONE
//        binding.scrollContent.visibility = View.VISIBLE
//
//        binding.etDate.setText(visit.VisitDate?.substringBefore("T") ?: "")
//        binding.etVisitTime.setText(visit.VisitInTime ?: "N/A")
//        binding.etLat.setText(visit.Latitude ?: "")
//        binding.etLong.setText(visit.Longitude ?: "")
//        binding.etAddress.setText(visit.FullAddress ?: "")
//        binding.BankName.setText(visit.BankName ?: "")
//        binding.BranchName.setText(visit.BranchName ?: "")
//        binding.StaffName.setText(visit.StaffName ?: "")
//
//        // Lock EVERY existing field (makes them gray)
//        val readOnlyFields = listOf(
//            binding.etDate, binding.etVisitTime, binding.etLat, binding.etLong,
//            binding.etAddress, binding.BankName, binding.BranchName, binding.StaffName
//        )
//        readOnlyFields.forEach { setFieldLockedVisuals(it, true) }
//
//        // Lock profile photo
//        binding.profilePhoto.isEnabled = false
//        binding.profilePhoto.alpha = 0.6f
//        binding.buttonUpload.visibility = View.GONE
//        binding.profilePhoto.setImageResource(R.drawable.user_profile)
//
//        // Show Comments, UNLOCK it (turns it white with border)
//        binding.CommentsLayout.visibility = View.VISIBLE
//        binding.CommentsLayout.error = null
//        setFieldLockedVisuals(binding.Comments, false)
//        binding.Comments.setText("")
//
//        binding.btnSubmitBankVisit.text = "Mark Done"
//    }
//
//    private fun fetchBankVisits(stage: Int) {
//        showLoadingDialog()
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getBankVisits(stage = stage, flag = "FETCH")
//                withContext(Dispatchers.Main) {
//                    dismissLoadingDialog()
//                    if (response.isSuccessful) {
//                        val bankVisitResponse = response.body()
//                        val count = bankVisitResponse?.Count ?: 0
//                        val visitsList = bankVisitResponse?.Data ?: emptyList()
//
//                        if (stage == 1001) binding.tvPendingCount.text = count.toString()
//                        else if (stage == 1002) binding.tvDoneCount.text = count.toString()
//
//                        adapter.updateList(visitsList)
//
//                        if (visitsList.isEmpty()) {
//                            showCustomToast(if (stage == 1001) "No pending visits found" else "No completed visits found")
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    dismissLoadingDialog()
//                    showCustomToast("Error fetching visits: ${e.message}")
//                }
//            }
//        }
//    }
//
//    private fun fetchDoneCountInBackground() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val doneResponse = ApiClient.apiService.getBankVisits(1002, "FETCH")
//                withContext(Dispatchers.Main) {
//                    if (doneResponse.isSuccessful) {
//                        binding.tvDoneCount.text = (doneResponse.body()?.Count ?: 0).toString()
//                    }
//                }
//            } catch (e: Exception) { e.printStackTrace() }
//        }
//    }
//
//    private fun checkPermissionsAndOpenCamera() {
//        if(isUpdateMode) return // Prevent camera open in update mode
//        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//
//        if (cameraPermission && locationPermission) {
//            openCamera()
//        } else {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
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
////    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////
////        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
////            capturedInTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
////            capturedInDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
////
////            fetchLocationAndOverlayImage()
////
////            binding.BankName.isEnabled = true
////            binding.BranchName.isEnabled = true
////            isImageClicked = true
////
////            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
////            binding.StaffName.setText(staffName)
////        }
////    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
//            capturedInTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//            capturedInDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
//
//            fetchLocationAndOverlayImage()
//
//            // Unlock Bank & Branch fields (turns them white with border)
//            setFieldLockedVisuals(binding.BankName, false)
//            setFieldLockedVisuals(binding.BranchName, false)
//
//            isImageClicked = true
//
//            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
//            binding.StaffName.setText(staffName)
//        }
//    }
//
//    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    private fun fetchLocationAndOverlayImage() {
//        showLoadingDialog()
//        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
//            .addOnSuccessListener { location ->
//                if (location != null) {
//                    latitude = location.latitude
//                    longitude = location.longitude
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        fullAddress = getAddressFromLocation(latitude, longitude)
//                        withContext(Dispatchers.Main) {
//                            currentDateTime = "$capturedInDate $capturedInTime"
//
//                            binding.etDate.setText(capturedInDate)
//                            binding.etVisitTime.setText(capturedInTime)
//                            binding.etLat.setText(latitude.toString())
//                            binding.etLong.setText(longitude.toString())
//                            binding.etAddress.setText(fullAddress)
//
//                            overlayImage(photoFile!!.absolutePath)
//                            dismissLoadingDialog()
//                        }
//                    }
//                } else {
//                    dismissLoadingDialog()
//                    showCustomToast("Unable to fetch location. Please ensure location is enabled.")
//                }
//            }
//            .addOnFailureListener { e ->
//                dismissLoadingDialog()
//                showCustomToast("Failed to get location: ${e.message}")
//            }
//    }
//
//    private fun getAddressFromLocation(lat: Double, lon: Double): String {
//        try {
//            val geocoder = Geocoder(this@BankVisitActivity, Locale.getDefault())
//            val addresses = geocoder.getFromLocation(lat, lon, 1)
//            if (!addresses.isNullOrEmpty()) {
//                val address = addresses[0].getAddressLine(0)
//                if (address != null) return address
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        return try {
//            val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1"
//            val url = java.net.URL(urlString)
//            val connection = url.openConnection() as java.net.HttpURLConnection
//            connection.requestMethod = "GET"
//            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
//            connection.connectTimeout = 5000
//            connection.readTimeout = 5000
//
//            if (connection.responseCode == 200) {
//                val inputStream = connection.inputStream
//                val response = inputStream.bufferedReader().use { it.readText() }
//                val jsonObject = org.json.JSONObject(response)
//                if (jsonObject.has("display_name")) jsonObject.getString("display_name") else "Address not found"
//            } else "Address not found (API Error ${connection.responseCode})"
//        } catch (e: Exception) {
//            "Address not found (Check Internet Connection)"
//        }
//    }
//
//    private fun overlayImage(imagePath: String) {
//        val bitmap = BitmapFactory.decodeFile(imagePath)
//        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(mutableBitmap)
//        val paint = Paint().apply {
//            color = Color.WHITE
//            textSize = 50f
//            isAntiAlias = true
//            setShadowLayer(10f, 5f, 5f, Color.BLACK)
//        }
//        val bgPaint = Paint().apply {
//            color = Color.parseColor("#88000000")
//            style = Paint.Style.FILL
//        }
//
//        val lines = listOf("📍 $fullAddress", "📅 $currentDateTime", "🌐 Lat: $latitude", "🌐 Long: $longitude")
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
//        binding.profilePhoto.setImageBitmap(mutableBitmap)
//    }
//
////    private fun validateAndSubmitAdd(): Boolean {
////        var isValid = true
////        if (!isImageClicked || photoFile == null) {
////            showCustomToast("Please capture an image first.")
////            return false
////        }
////        if (binding.BankName.text.isNullOrBlank()) {
////            binding.BankNameLayout.error = "Bank Name is required"
////            isValid = false
////        } else binding.BankNameLayout.error = null
////
////        if (binding.BranchName.text.isNullOrBlank()) {
////            binding.BranchNameLayout.error = "Branch Name is required"
////            isValid = false
////        } else binding.BranchNameLayout.error = null
////
////        return isValid
////    }
//
//    private fun validateAndSubmitAdd(): Boolean {
//        var isValid = true
//
//        // 1. Check if Image is captured
//        if (!isImageClicked || photoFile == null) {
//            showCustomToast("Please capture an image first.")
//            return false // Early return because location/time depends on the image
//        }
//
//        // 2. Check System Auto-Filled Fields (Location, Time, Date, Staff Name)
//        if (binding.etLat.text.isNullOrBlank() ||
//            binding.etLong.text.isNullOrBlank() ||
//            binding.etAddress.text.isNullOrBlank() ||
//            binding.etDate.text.isNullOrBlank() ||
//            binding.etVisitTime.text.isNullOrBlank() ||
//            binding.StaffName.text.isNullOrBlank()
//        ) {
//            showCustomToast("System data (Location/Time/Staff) is missing. Please check your connection or recapture the image.")
//            isValid = false
//        }
//
//        // 3. Check Manual Field: Bank Name
//        if (binding.BankName.text.isNullOrBlank()) {
//            binding.BankNameLayout.error = "Bank Name is required"
//            isValid = false
//        } else {
//            binding.BankNameLayout.error = null
//        }
//
//        // 4. Check Manual Field: Branch Name
//        if (binding.BranchName.text.isNullOrBlank()) {
//            binding.BranchNameLayout.error = "Branch Name is required"
//            isValid = false
//        } else {
//            binding.BranchNameLayout.error = null
//        }
//
//        return isValid
//    }
//
//    // ----------------------------------------------------
//    // API Call: ADD New Visit
//    // ----------------------------------------------------
//    private fun submitBankVisit() {
//        showLoadingDialog()
//        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "") ?: ""
//
//        lifecycleScope.launch {
//            val visitDatePart = createPart(binding.etDate.text?.toString() ?: "")
//            val visitTimePart = createPart(binding.etVisitTime.text?.toString() ?: "")
//            val staffIdPart = createPart(staffId)
//            val staffNamePart = createPart(binding.StaffName.text?.toString() ?: "")
//            val bankNamePart = createPart(binding.BankName.text?.toString() ?: "")
//            val branchNamePart = createPart(binding.BranchName.text?.toString() ?: "")
//            val fullAddressPart = createPart(binding.etAddress.text?.toString() ?: "")
//            val latPart = createPart(binding.etLat.text?.toString() ?: "")
//            val longPart = createPart(binding.etLong.text?.toString() ?: "")
//
//            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile, "Image") }
//
//            ApiClient.apiService.insertBankVisit(
//                visitDate = visitDatePart,
//                visitTime = visitTimePart,
//                staffID = staffIdPart,
//                staffName = staffNamePart,
//                bankName = bankNamePart,
//                branchName = branchNamePart,
//                fullAddress = fullAddressPart,
//                latitude = latPart,
//                longitude = longPart,
//                image = imagePart
//            ).enqueue(object : Callback<ResponseBody> {
//                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                    dismissLoadingDialog()
//                    if (response.isSuccessful) {
//                        showCustomToast("Bank Visit Added Successfully!")
//                        showListAndFetch()
//                    } else showCustomToast("API Error: ${response.code()}")
//                }
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    dismissLoadingDialog()
//                    showCustomToast("Network Error: ${t.localizedMessage}")
//                }
//            })
//        }
//    }
//
//    // ----------------------------------------------------
//    // API Call: UPDATE Existing Visit (Mark Done)
//    // ----------------------------------------------------
//    private fun submitUpdateBankVisit() {
//        showLoadingDialog()
//        val outTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//        val commentsText = binding.Comments.text.toString()
//
//        val idPart = createPart(selectedVisitIdForUpdate.toString())
//        val outTimePart = createPart(outTime)
//        val commentsPart = createPart(commentsText)
//
//        ApiClient.apiService.updateBankVisitOutTime(
//            id = idPart,
//            visitOutTime = outTimePart,
//            comments = commentsPart
//        ).enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                dismissLoadingDialog()
//                if (response.isSuccessful) {
//                    showCustomToast("Visit Marked as Done!")
//                    fetchDoneCountInBackground() // update done count
//                    showListAndFetch()
//                } else {
//                    showCustomToast("Failed to complete visit: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                dismissLoadingDialog()
//                showCustomToast("Network Error: ${t.localizedMessage}")
//            }
//        })
//    }
//
//    private fun showListAndFetch() {
//        binding.radioGroupContainer.visibility = View.VISIBLE
//        binding.fabAdd.visibility = View.VISIBLE
//        binding.rvBankVisits.visibility = View.VISIBLE
//        binding.scrollContent.visibility = View.GONE
//
//        // Return to pending list
//        binding.rbPending.isChecked = true
//        adapter.setCurrentTab("Pending")
//        fetchBankVisits(1001)
//    }
//
//    private fun showCustomToast(message: String) {
//        val layout = layoutInflater.inflate(R.layout.toast_custom, binding.root, false)
//        layout.findViewById<TextView>(R.id.toast_text).text = message
//        Toast(applicationContext).apply {
//            duration = Toast.LENGTH_SHORT
//            view = layout
//            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
//            show()
//        }
//    }
//
//    private fun setupFieldWatchers() {
//        val fields = listOf(binding.BankName, binding.BranchName)
//        fields.forEach { field ->
//            field.setOnTouchListener { v, event ->
//                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked && !isUpdateMode) {
//                    showCustomToast("Please capture an image first.")
//                    v.clearFocus()
//                    true
//                } else false
//            }
//        }
//    }
//
//    private fun createPart(value: String): RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), value)
//
//    private fun createImagePart(file: File?, partName: String): MultipartBody.Part? {
//        val compressedFile = file?.let { compressImage(it, 50) }
//        return compressedFile?.let {
//            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
//            MultipartBody.Part.createFormData(partName, it.name, requestFile)
//        }
//    }
//
//    private fun showLoadingDialog() {
//        val dialogBinding = DialogLoadingBinding.inflate(layoutInflater)
//        loadingDialog = AlertDialog.Builder(this).setView(dialogBinding.root).setCancelable(false).create()
//        dialogBinding.tvLoading.text = "Submitting..."
//        loadingDialog?.show()
//    }
//
//    private fun dismissLoadingDialog() { loadingDialog?.dismiss() }
//
//    private fun compressImage(file: File, maxKb: Int = 50): File? {
//        return try {
//            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//            val outFile = File(cacheDir, "compressed_${file.name}")
//            var compressQuality = 100
//            var streamLength: Int
//            do {
//                val stream = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
//                streamLength = stream.toByteArray().size / 1024
//                compressQuality -= 5
//                stream.close()
//            } while (streamLength > maxKb && compressQuality > 5)
//            val fos = FileOutputStream(outFile)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
//            fos.flush()
//            fos.close()
//            outFile
//        } catch (e: Exception) { null }
//    }
//
////    private fun setFieldLockedVisuals(editText: com.google.android.material.textfield.TextInputEditText, isLocked: Boolean) {
////        if (isLocked) {
////            editText.isEnabled = false
////            editText.setTextColor(android.graphics.Color.GRAY)
////            editText.alpha = 0.6f // Makes the box look dull/grayish
////        } else {
////            // Restore to normal for "Add" mode
////            editText.setTextColor(android.graphics.Color.BLACK)
////            editText.alpha = 1.0f
////        }
////    }
//
////    private fun setFieldLockedVisuals(editText: com.google.android.material.textfield.TextInputEditText, isLocked: Boolean) {
////        // Find the parent TextInputLayout automatically
////        var parentView = editText.parent
////        var textInputLayout: com.google.android.material.textfield.TextInputLayout? = null
////
////        while (parentView is View) {
////            if (parentView is com.google.android.material.textfield.TextInputLayout) {
////                textInputLayout = parentView
////                break
////            }
////            parentView = parentView.parent
////        }
////
////        if (isLocked) {
////            editText.isEnabled = false
////            editText.setTextColor(android.graphics.Color.GRAY)
////            // Dulling the ENTIRE layout makes the hint, outline, and text look perfectly read-only
////            textInputLayout?.alpha = 0.5f
////        } else {
////            editText.setTextColor(android.graphics.Color.BLACK)
////            textInputLayout?.alpha = 1.0f
////        }
////    }
//
//    private fun setFieldLockedVisuals(editText: com.google.android.material.textfield.TextInputEditText, isLocked: Boolean) {
//        var parentView = editText.parent
//        var textInputLayout: com.google.android.material.textfield.TextInputLayout? = null
//
//        while (parentView is View) {
//            if (parentView is com.google.android.material.textfield.TextInputLayout) {
//                textInputLayout = parentView
//                break
//            }
//            parentView = parentView.parent
//        }
//
//        if (isLocked) {
//            editText.isEnabled = false
//            editText.setTextColor(android.graphics.Color.BLACK)
//            textInputLayout?.alpha = 1.0f
//
//            // Force background to be grayish for locked fields
//            editText.backgroundTintList = android.content.res.ColorStateList.valueOf(
//                androidx.core.content.ContextCompat.getColor(this, R.color.auto_fill_bg)
//            )
//        } else {
//            editText.isEnabled = true
//            editText.setTextColor(android.graphics.Color.BLACK)
//            textInputLayout?.alpha = 1.0f
//
//            // Restore default white background & border by removing the tint entirely
//            editText.backgroundTintList = null
//        }
//    }
//}
//
////package com.avs.visitapp
////
////import android.Manifest
////import android.app.AlertDialog
////import android.content.Intent
////import android.content.pm.PackageManager
////import android.graphics.Bitmap
////import android.graphics.BitmapFactory
////import android.graphics.Canvas
////import android.graphics.Color
////import android.graphics.Paint
////import android.location.Geocoder
////import android.net.Uri
////import android.os.Build
////import android.os.Bundle
////import android.os.Environment
////import android.provider.MediaStore
////import android.view.Gravity
////import android.view.MotionEvent
////import android.view.View
////import android.widget.TextView
////import android.widget.Toast
////import androidx.annotation.RequiresApi
////import androidx.annotation.RequiresPermission
////import androidx.appcompat.app.AppCompatActivity
////import androidx.core.app.ActivityCompat
////import androidx.core.content.ContextCompat
////import androidx.core.content.FileProvider
////import androidx.lifecycle.lifecycleScope
////import androidx.recyclerview.widget.LinearLayoutManager
////import com.avs.visitapp.databinding.ActivityBankVisitBinding
////import com.avs.visitapp.databinding.DialogLoadingBinding
////import com.google.android.gms.location.FusedLocationProviderClient
////import com.google.android.gms.location.LocationServices
////import com.google.android.gms.location.Priority
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import kotlinx.coroutines.withContext
////import okhttp3.MediaType.Companion.toMediaTypeOrNull
////import okhttp3.MultipartBody
////import okhttp3.RequestBody
////// IMPORTANT: This fixes the 'asRequestBody' unresolved reference error
////import okhttp3.RequestBody.Companion.asRequestBody
////import okhttp3.ResponseBody
////import retrofit2.Call
////import retrofit2.Callback
////import retrofit2.Response
////import java.io.ByteArrayOutputStream
////import java.io.File
////import java.io.FileOutputStream
////import java.text.SimpleDateFormat
////import java.util.Date
////import java.util.Locale
////
////@RequiresApi(Build.VERSION_CODES.M)
////class BankVisitActivity : AppCompatActivity() {
////
////    private lateinit var binding: ActivityBankVisitBinding
////    private lateinit var fusedLocationClient: FusedLocationProviderClient
////
////    private var photoFile: File? = null
////    private var loadingDialog: AlertDialog? = null
////
////    private val CAMERA_REQUEST_CODE = 101
////    private val PERMISSION_REQUEST_CODE = 102
////
////    private var latitude = 0.0
////    private var longitude = 0.0
////    private var fullAddress = ""
////    private var currentDateTime = ""
////    private var isImageClicked = false
////    private var capturedInTime = ""
////    private var capturedInDate = ""
////
////    // Add this with your other variables near the top:
////    private lateinit var adapter: com.avs.visitapp.Adapter.BankVisitAdapter
////
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////        binding = ActivityBankVisitBinding.inflate(layoutInflater)
////        setContentView(binding.root)
////
////        binding.toolbarBack.setOnClickListener { finish() }
////
////        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
////
////        setupFieldWatchers()
////
////        // Camera Triggers
////        binding.profilePhoto.setOnClickListener { checkPermissionsAndOpenCamera() }
////        binding.buttonUpload.setOnClickListener { checkPermissionsAndOpenCamera() }
////
////        // Setup RecyclerView for Pending/Done
////        adapter = com.avs.visitapp.Adapter.BankVisitAdapter(emptyList()) { selectedVisit ->
////            // Trigger the Update API when "Complete Visit" is clicked
////            selectedVisit.ID?.let { visitId ->
////                markVisitDone(visitId)
////            }
////        }
////        binding.rvBankVisits.layoutManager = LinearLayoutManager(this)
////        binding.rvBankVisits.adapter = adapter
////
////        // Put this in onCreate() right above `fetchBankVisits(1001)`
////        lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                // Fetch Done count silently in background
////                val doneResponse = ApiClient.apiService.getBankVisits(1002, "FETCH")
////                withContext(Dispatchers.Main) {
////                    if (doneResponse.isSuccessful) {
////                        binding.tvDoneCount.text = (doneResponse.body()?.Count ?: 0).toString()
////                    }
////                }
////            } catch (e: Exception) { e.printStackTrace() }
////        }
////
////        // Setup RecyclerView for Pending/Done (Adapter needs to be configured by you)
////        binding.rvBankVisits.layoutManager = LinearLayoutManager(this)
////
////        binding.rbPending.setOnClickListener {
////            binding.rbDone.isChecked = false
////            adapter.setCurrentTab("Pending") // Tell adapter to show button
////            fetchBankVisits(1001)
////        }
////
////        binding.rbDone.setOnClickListener {
////            binding.rbPending.isChecked = false
////            adapter.setCurrentTab("Done") // Tell adapter to hide button
////            fetchBankVisits(1002)
////        }
////
////        // Open Add Form
////        binding.fabAdd.setOnClickListener {
////            binding.radioGroupContainer.visibility = View.GONE
////            binding.fabAdd.visibility = View.GONE
////            binding.rvBankVisits.visibility = View.GONE
////            binding.scrollContent.visibility = View.VISIBLE
////            resetForm()
////        }
////
////        // Submit Form
////        binding.btnSubmitBankVisit.setOnClickListener {
////            if (validateAndSubmit()) {
////                submitBankVisit()
////            }
////        }
////
////        // Load initially
////        fetchBankVisits(1001)
////        binding.rbPending.isChecked = true
////    }
////
////    private fun resetForm() {
////        photoFile = null
////        capturedInTime = ""
////        capturedInDate = ""
////        isImageClicked = false
////        latitude = 0.0
////        longitude = 0.0
////        fullAddress = ""
////        binding.profilePhoto.setImageResource(R.drawable.user_profile)
////        binding.etDate.setText("")
////        binding.etVisitTime.setText("")
////        binding.etLat.setText("")
////        binding.etLong.setText("")
////        binding.etAddress.setText("")
////        binding.BankName.setText("")
////        binding.BranchName.setText("")
////        binding.BankName.isEnabled = false
////        binding.BranchName.isEnabled = false
////    }
////
////    private fun fetchBankVisits(stage: Int) {
////        showLoadingDialog() // Optional: shows loader while fetching
////        lifecycleScope.launch(Dispatchers.IO) {
////            try {
////                // Call the API
////                val response = ApiClient.apiService.getBankVisits(stage = stage, flag = "FETCH")
////
////                withContext(Dispatchers.Main) {
////                    dismissLoadingDialog()
////                    if (response.isSuccessful) {
////                        val bankVisitResponse = response.body()
////                        val count = bankVisitResponse?.Count ?: 0
////                        val visitsList = bankVisitResponse?.Data ?: emptyList()
////
////                        // Update the Count UI
////                        if (stage == 1001) {
////                            binding.tvPendingCount.text = count.toString()
////                        } else if (stage == 1002) {
////                            binding.tvDoneCount.text = count.toString()
////                        }
////
////                        // Update your RecyclerView Adapter
////                         adapter.updateList(visitsList)
////
////                        if (visitsList.isEmpty()) {
////                            showCustomToast(if (stage == 1001) "No pending visits found" else "No completed visits found")
////                        }
////                    } else {
////                        showCustomToast("API Error: ${response.code()}")
////                    }
////                }
////            } catch (e: Exception) {
////                withContext(Dispatchers.Main) {
////                    dismissLoadingDialog()
////                    showCustomToast("Error fetching visits: ${e.message}")
////                }
////            }
////        }
////    }
////
////    private fun checkPermissionsAndOpenCamera() {
////        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
////        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
////
////        if (cameraPermission && locationPermission) {
////            openCamera()
////        } else {
////            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
////        }
////    }
////
////    private fun openCamera() {
////        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
////        try {
////            photoFile = createImageFile()
////            val authority = "$packageName.fileprovider"
////            val photoUri: Uri = FileProvider.getUriForFile(this, authority, photoFile!!)
////            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
////            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
////            startActivityForResult(intent, CAMERA_REQUEST_CODE)
////        } catch (e: Exception) {
////            Toast.makeText(this, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    @Throws(Exception::class)
////    private fun createImageFile(): File {
////        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
////        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
////        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
////    }
////
////    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////
////        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
////            capturedInTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
////            capturedInDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
////
////            fetchLocationAndOverlayImage()
////
////            binding.BankName.isEnabled = true
////            binding.BranchName.isEnabled = true
////            isImageClicked = true
////
////            // Set Staff Name from SharedPrefs automatically
////            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
////            binding.StaffName.setText(staffName)
////        }
////    }
////
////
////    private fun markVisitDone(visitId: Int) {
////        showLoadingDialog()
////        // Capture the exact time the user clicked "Complete"
////        val outTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
////
////        ApiClient.apiService.updateBankVisitOutTime(visitId, outTime).enqueue(object : Callback<okhttp3.ResponseBody> {
////            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
////                dismissLoadingDialog()
////                if (response.isSuccessful) {
////                    showCustomToast("Visit Marked as Done at $outTime!")
////                    // Refresh the pending list to remove the completed item
////                    fetchBankVisits(1001)
////                } else {
////                    showCustomToast("Failed to complete visit: ${response.code()}")
////                }
////            }
////
////            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
////                dismissLoadingDialog()
////                showCustomToast("Network Error: ${t.localizedMessage}")
////            }
////        })
////    }
////
//////    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//////    private fun fetchLocationAndOverlayImage() {
//////        showLoadingDialog()
//////        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
//////            .addOnSuccessListener { location ->
//////                dismissLoadingDialog()
//////                if (location != null) {
//////                    latitude = location.latitude
//////                    longitude = location.longitude
//////                    fullAddress = getAddressFromLocation(latitude, longitude)
//////                    currentDateTime = "$capturedInDate $capturedInTime"
//////
//////                    binding.etDate.setText(capturedInDate)
//////                    binding.etVisitTime.setText(capturedInTime)
//////                    binding.etLat.setText(latitude.toString())
//////                    binding.etLong.setText(longitude.toString())
//////                    binding.etAddress.setText(fullAddress)
//////
//////                    overlayImage(photoFile!!.absolutePath)
//////                } else {
//////                    showCustomToast("Unable to fetch location. Please ensure location is enabled.")
//////                }
//////            }
//////            .addOnFailureListener { e ->
//////                dismissLoadingDialog()
//////                showCustomToast("Failed to get location: ${e.message}")
//////            }
//////    }
//////
//////    private fun getAddressFromLocation(lat: Double, lon: Double): String {
//////        return try {
//////            val geocoder = Geocoder(this, Locale.getDefault())
//////            val addresses = geocoder.getFromLocation(lat, lon, 1)
//////            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Address not found"
//////        } catch (e: Exception) {
//////            "Address not found"
//////        }
//////    }
////
////    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
////    private fun fetchLocationAndOverlayImage() {
////        showLoadingDialog()
////        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
////            .addOnSuccessListener { location ->
////                if (location != null) {
////                    latitude = location.latitude
////                    longitude = location.longitude
////
////                    // Fetch Address in a Background Thread (Dispatchers.IO)
////                    lifecycleScope.launch(Dispatchers.IO) {
////                        fullAddress = getAddressFromLocation(latitude, longitude)
////
////                        // Switch back to Main Thread to update UI
////                        withContext(Dispatchers.Main) {
////                            currentDateTime = "$capturedInDate $capturedInTime"
////
////                            binding.etDate.setText(capturedInDate)
////                            binding.etVisitTime.setText(capturedInTime)
////                            binding.etLat.setText(latitude.toString())
////                            binding.etLong.setText(longitude.toString())
////
////                            // 🔹 This will now safely populate your address field
////                            binding.etAddress.setText(fullAddress)
////
////                            overlayImage(photoFile!!.absolutePath)
////                            dismissLoadingDialog()
////                        }
////                    }
////                } else {
////                    dismissLoadingDialog()
////                    showCustomToast("Unable to fetch location. Please ensure location is enabled.")
////                }
////            }
////            .addOnFailureListener { e ->
////                dismissLoadingDialog()
////                showCustomToast("Failed to get location: ${e.message}")
////            }
////    }
////
////    private fun getAddressFromLocation(lat: Double, lon: Double): String {
////        // ATTEMPT 1: Try Android's built-in Geocoder first
////        try {
////            val geocoder = Geocoder(this@BankVisitActivity, Locale.getDefault())
////            val addresses = geocoder.getFromLocation(lat, lon, 1)
////            if (!addresses.isNullOrEmpty()) {
////                val address = addresses[0].getAddressLine(0)
////                if (address != null) return address
////            }
////        } catch (e: Exception) {
////            e.printStackTrace()
////            android.util.Log.e("BankVisitActivity", "Built-in Geocoder failed, trying fallback API...")
////        }
////
////        // ATTEMPT 2: Fallback to a Free Public API (OpenStreetMap Nominatim)
////        // This works on Emulators and devices without Google Play Services
////        return try {
////            val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1"
////            val url = java.net.URL(urlString)
////            val connection = url.openConnection() as java.net.HttpURLConnection
////            connection.requestMethod = "GET"
////
////            // Nominatim requires a User-Agent header to fulfill requests
////            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
////            connection.connectTimeout = 5000
////            connection.readTimeout = 5000
////
////            if (connection.responseCode == 200) {
////                val inputStream = connection.inputStream
////                val response = inputStream.bufferedReader().use { it.readText() }
////
////                // Parse the JSON response
////                val jsonObject = org.json.JSONObject(response)
////                if (jsonObject.has("display_name")) {
////                    jsonObject.getString("display_name")
////                } else {
////                    "Address not found"
////                }
////            } else {
////                "Address not found (API Error ${connection.responseCode})"
////            }
////        } catch (e: Exception) {
////            e.printStackTrace()
////            "Address not found (Check Internet Connection)"
////        }
////    }
////
////    private fun overlayImage(imagePath: String) {
////        val bitmap = BitmapFactory.decodeFile(imagePath)
////        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
////        val canvas = Canvas(mutableBitmap)
////        val paint = Paint().apply {
////            color = Color.WHITE
////            textSize = 50f
////            isAntiAlias = true
////            setShadowLayer(10f, 5f, 5f, Color.BLACK)
////        }
////        val bgPaint = Paint().apply {
////            color = Color.parseColor("#88000000")
////            style = Paint.Style.FILL
////        }
////
////        val lines = listOf("📍 $fullAddress", "📅 $currentDateTime", "🌐 Lat: $latitude", "🌐 Long: $longitude")
////        val x = 30f
////        val yStart = mutableBitmap.height - lines.size * 80f - 30f
////
////        canvas.drawRect(0f, yStart - 40f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)
////
////        var y = yStart
////        lines.forEach {
////            canvas.drawText(it, x, y, paint)
////            y += 80f
////        }
////        binding.profilePhoto.setImageBitmap(mutableBitmap)
////    }
////
////    private fun validateAndSubmit(): Boolean {
////        var isValid = true
////        if (!isImageClicked || photoFile == null) {
////            showCustomToast("Please capture an image first.")
////            return false
////        }
////        if (binding.BankName.text.isNullOrBlank()) {
////            binding.BankNameLayout.error = "Bank Name is required"
////            isValid = false
////        } else binding.BankNameLayout.error = null
////
////        if (binding.BranchName.text.isNullOrBlank()) {
////            binding.BranchNameLayout.error = "Branch Name is required"
////            isValid = false
////        } else binding.BranchNameLayout.error = null
////
////        return isValid
////    }
////
////    private fun submitBankVisit() {
////        showLoadingDialog()
////        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "") ?: ""
////
////        lifecycleScope.launch {
////            // Mapping exactly to your Postman Keys
////            val visitDatePart = createPart(binding.etDate.text?.toString() ?: "")
////            val visitTimePart = createPart(binding.etVisitTime.text?.toString() ?: "")
////            val staffIdPart = createPart(staffId)
////            val staffNamePart = createPart(binding.StaffName.text?.toString() ?: "")
////            val bankNamePart = createPart(binding.BankName.text?.toString() ?: "")
////            val branchNamePart = createPart(binding.BranchName.text?.toString() ?: "")
////            val fullAddressPart = createPart(binding.etAddress.text?.toString() ?: "")
////            val latPart = createPart(binding.etLat.text?.toString() ?: "")
////            val longPart = createPart(binding.etLong.text?.toString() ?: "")
////
////            // Key must be "Image" as per Postman screenshot
////            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile, "Image") }
////
////            ApiClient.apiService.insertBankVisit(
////                visitDate = visitDatePart,
////                visitTime = visitTimePart,
////                staffID = staffIdPart,
////                staffName = staffNamePart,
////                bankName = bankNamePart,
////                branchName = branchNamePart,
////                fullAddress = fullAddressPart,
////                latitude = latPart,
////                longitude = longPart,
////                image = imagePart
////            ).enqueue(object : Callback<ResponseBody> {
////                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
////                    dismissLoadingDialog()
////                    if (response.isSuccessful) {
////                        showCustomToast("Bank Visit submitted successfully!")
////                        binding.radioGroupContainer.visibility = View.VISIBLE
////                        binding.fabAdd.visibility = View.VISIBLE
////                        binding.rvBankVisits.visibility = View.VISIBLE
////                        binding.scrollContent.visibility = View.GONE
////                        fetchBankVisits(1001)
////                    } else {
////                        showCustomToast("API Error: ${response.code()}")
////                    }
////                }
////                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
////                    dismissLoadingDialog()
////                    showCustomToast("Network Error: ${t.localizedMessage}")
////                }
////            })
////        }
////    }
////
////    private fun showCustomToast(message: String) {
////        val layout = layoutInflater.inflate(R.layout.toast_custom, binding.root, false)
////        layout.findViewById<TextView>(R.id.toast_text).text = message
////        Toast(applicationContext).apply {
////            duration = Toast.LENGTH_SHORT
////            view = layout
////            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
////            show()
////        }
////    }
////
////    private fun setupFieldWatchers() {
////        val fields = listOf(binding.BankName, binding.BranchName)
////        fields.forEach { field ->
////            field.setOnTouchListener { v, event ->
////                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
////                    showCustomToast("Please capture an image first.")
////                    v.clearFocus()
////                    true
////                } else false
////            }
////        }
////    }
////
////    // --- Helper Functions to Fix Build Errors ---
////    private fun createPart(value: String): RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), value)
////
////    private fun createImagePart(file: File?, partName: String): MultipartBody.Part? {
////        val compressedFile = file?.let { compressImage(it, 50) }
////        return compressedFile?.let {
////            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
////            MultipartBody.Part.createFormData(partName, it.name, requestFile)
////        }
////    }
////
////    private fun showLoadingDialog() {
////        val dialogBinding = DialogLoadingBinding.inflate(layoutInflater)
////        loadingDialog = AlertDialog.Builder(this).setView(dialogBinding.root).setCancelable(false).create()
////        dialogBinding.tvLoading.text = "Submitting..."
////        loadingDialog?.show()
////    }
////
////    private fun dismissLoadingDialog() { loadingDialog?.dismiss() }
////
////    private fun compressImage(file: File, maxKb: Int = 50): File? {
////        return try {
////            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
////            val outFile = File(cacheDir, "compressed_${file.name}")
////            var compressQuality = 100
////            var streamLength: Int
////            do {
////                val stream = ByteArrayOutputStream()
////                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
////                streamLength = stream.toByteArray().size / 1024
////                compressQuality -= 5
////                stream.close()
////            } while (streamLength > maxKb && compressQuality > 5)
////            val fos = FileOutputStream(outFile)
////            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
////            fos.flush()
////            fos.close()
////            outFile
////        } catch (e: Exception) { null }
////    }
////}
