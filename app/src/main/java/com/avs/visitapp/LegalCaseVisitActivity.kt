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
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.visitapp.Adapter.CustomerAdapter
import com.avs.visitapp.Adapter.SimpleTextWatcher
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.ActivityLegalCaseVisitBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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


// Data Classes for the 101 & 138 API Response
data class LegalTypeResponse(
    val TotalCount: Int?,
    val Data: List<LegalTypeData>?
)

data class LegalTypeData(
    val LNO: Int?,
    val LTYPE: String?,
    val DESCRIPTION: String?,
    val SRNO: Int?,
    val Format: String? // Added Format property
)

data class LocationData(
    val LNO: Int?,
    val DESCRIPTION: String?,
    val LTYPE: String?,
    val SRNO: Int?,
    val REFNO: Int?
)


@RequiresApi(Build.VERSION_CODES.M)
class LegalCaseVisitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalCaseVisitBinding

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

    private lateinit var adapter: CustomerAdapter
    private var lastDialedVisit: Visit? = null

    private val loanAcCustName = mutableListOf<String>()
    private val customerMap = mutableMapOf<String, CustomerNameResponse>()

    private var currentLegalTypeDataList = listOf<LegalTypeData>()
    private var currentDataFormat = ""
    private var currentSrNo = ""

    private var districtList = listOf<LocationData>()
    private var talukaList = listOf<LocationData>()

    // NEW: List of Main Legal Categories
//    private val legalCaseCategories = listOf("91 Cases", "101 Cases", "138 Cases", "Other Cases")
    private var visitFetchJob: kotlinx.coroutines.Job? = null
    // NEW: List of Main Legal Categories
    private val legalCaseCategories = listOf("91 Cases", "101 Cases", "138 Cases", "OTS Cases", "Other Cases")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLegalCaseVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.toolbarBack.setOnClickListener {
//            val intent = Intent(this, VisitActivity::class.java)
//            startActivity(intent)
//            finish()
//        }

        binding.toolbarBack.setOnClickListener {
            // FIX: Just finish this activity to pop it off the stack
            finish()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupBranchDropdown()
        fetchDistricts()

        // 1. Set up the new Merged Dropdown immediately
        setupStrictDropdown(binding.LegalCaseCategory, legalCaseCategories)

//        binding.LegalCaseCategory.setOnItemClickListener { _, _, position, _ ->
//            val selectedCategory = legalCaseCategories[position]
//
//            // Hide dynamically generated views and comments by default on switch
//            binding.DynamicFieldsContainer.visibility = View.GONE
//            binding.CommentsLayout.visibility = View.GONE
//            binding.Comments.setText("")
//
//            when (selectedCategory) {
//                "91 Cases", "101 Cases" -> {
//                    // TODO: Call API for 91/101 and populate DynamicFieldsContainer
//                    showCustomToast("Logic to fetch 91/101 API data will apply here")
//                }
//                "138 Cases" -> {
//                    // TODO: Call API for 138 and populate DynamicFieldsContainer
//                    showCustomToast("Logic to fetch 138 API data will apply here")
//                }
//                "Other Cases" -> {
//                    // For "Other Cases", just display the Comments Box
//                    binding.CommentsLayout.visibility = View.VISIBLE
//                }
//            }
//        }

        binding.LegalCaseCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = legalCaseCategories[position]

            // Hide everything on new selection
            binding.LegalStatusLayout.visibility = View.GONE
            binding.DynamicInputContainer.visibility = View.GONE
            binding.CommentsLayout.visibility = View.GONE

            // HIDE DISTRICT & TALUKA
            binding.DistrictLayout.visibility = View.GONE
            binding.TalukaLayout.visibility = View.GONE

            // Clear text
            binding.LegalStatus.setText("", false)
            binding.DynamicInput.setText("")
            binding.Comments.setText("")
            binding.District.setText("", false)
            binding.Taluka.setText("", false)



//            // Route logic based on category
//            when (selectedCategory) {
//                "91 Cases", "101 Cases" -> fetchLegalStatusAPI("101")
//                "138 Cases" -> fetchLegalStatusAPI("138")
//                "Other Cases" -> {
//                    // For 'Other', just show Comments box
//                    binding.CommentsLayout.visibility = View.VISIBLE
//                }
//            }
            // Route logic based on category
            when (selectedCategory) {
                "91 Cases", "101 Cases" -> fetchLegalStatusAPI("101")
                "138 Cases" -> fetchLegalStatusAPI("138")
                "OTS Cases" -> fetchLegalStatusAPI("139")
                "Other Cases" -> {
                    // For 'Other', just show Comments box
                    binding.CommentsLayout.visibility = View.VISIBLE
                }
            }
        }

        binding.profilePhoto.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        val legalVisitMobileLayout = binding.CustMobNoLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val legalVisitMobileEditText = binding.CustMobNo.findViewById<TextInputEditText>(R.id.CustMobNo)

        legalVisitMobileEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""
                if (mobile.isNotEmpty() && (mobile.length != 10 || !mobile.all { it.isDigit() })) {
                    legalVisitMobileLayout.boxStrokeColor = ContextCompat.getColor(this@LegalCaseVisitActivity, R.color.red)
                    legalVisitMobileLayout.error = "Enter 10 digit number"
                } else {
                    legalVisitMobileLayout.boxStrokeColor = ContextCompat.getColor(this@LegalCaseVisitActivity, R.color.colorPrimaryDark)
                    legalVisitMobileLayout.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.submit.setOnClickListener {
            if (validateAndSubmit()) {
                submitLegalCaseVisit()
            }
        }

        binding.fabAdd.setOnClickListener {
            binding.radioGroup.visibility = View.GONE
            binding.fabAdd.visibility = View.GONE
            binding.rvvisit.visibility = View.GONE
            binding.countLayout.visibility = View.GONE
            binding.scrollContent.visibility = View.VISIBLE
        }

        adapter = CustomerAdapter(emptyList()) { selectedVisit ->
            dialCustomer(selectedVisit)
        }
        binding.rvvisit.layoutManager = LinearLayoutManager(this)
        binding.rvvisit.adapter = adapter

        val staffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toInt() ?: 0
        val flag = "LV"

        fetchDoneCount(staffID, flag)
        fetchPendingCount(staffID, "DQ")

        fetchPendingVisits(staffID, "DQ")
        binding.rbPending.isChecked = true

        binding.rbPending.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.rbDone.isChecked = false
                adapter.setCurrentTab("Pending")
                adapter.updateList(emptyList())
                fetchPendingVisits(StaffID = staffID, flag = "DQ")
            }
        }

        binding.rbDone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.rbPending.isChecked = false
                adapter.setCurrentTab("Done")
                adapter.updateList(emptyList())
                fetchDoneVisits(StaffID = staffID, flag = flag)
            }
        }

//        binding.LoanDisbDate.setOnClickListener {
//            showDatePickerFor(binding.LoanDisbDate)
//        }

        setupFieldWatchers()
    }

    private fun fetchDistricts() {
        // 1. Visible proof on your phone screen that this function is running
        Toast.makeText(this, "Fetching Districts...", Toast.LENGTH_SHORT).show()
        Log.d("LegalApi", "fetchDistricts() started...")

        binding.DistrictLayout.hint = "Loading Districts..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("LegalApi", "Making API Call to GetDistrict...")
                val response = ApiClient.apiService.getDistricts()
                Log.d("LegalApi", "fetchDistricts HTTP Code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    binding.DistrictLayout.hint = "District"
                    if (response.isSuccessful && response.body() != null) {
                        districtList = response.body()!!
                        Log.d("LegalApi", "Success! Fetched ${districtList.size} districts.")

                        val districtNames = districtList.mapNotNull { it.DESCRIPTION }
                        setupDropdown(binding.District, districtNames)

                        // Search by name so filtering works smoothly
                        binding.District.setOnItemClickListener { _, _, _, _ ->
                            val selectedName = binding.District.text.toString().trim()
                            val selectedDistrict = districtList.find { it.DESCRIPTION?.trim() == selectedName }

                            // Reset Taluka field
                            binding.Taluka.setText("", false)
                            setupDropdown(binding.Taluka, emptyList())

                            // Fetch new Talukas using the District's SRNO
                            selectedDistrict?.SRNO?.let {
                                Log.d("LegalApi", "District Selected. Fetching Talukas for SRNO: $it")
                                fetchTalukas(it)
                            }
                        }
                    } else {
                        val err = response.errorBody()?.string()
                        Log.e("LegalApi", "fetchDistricts Failed! Error: $err")
                        showCustomToast("Failed to load Districts")
                    }
                }
            } catch (e: Exception) {
                Log.e("LegalApi", "Exception in fetchDistricts: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.DistrictLayout.hint = "District"
                    showCustomToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun fetchTalukas(refNo: Int) {
        Log.d("LegalApi", "fetchTalukas() started for RefNo: $refNo...")
        binding.TalukaLayout.hint = "Loading Talukas..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getTalukas(refNo)
                Log.d("LegalApi", "fetchTalukas API HTTP Code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    binding.TalukaLayout.hint = "Taluka"
                    if (response.isSuccessful && response.body() != null) {
                        talukaList = response.body()!!
                        Log.d("LegalApi", "Success! Fetched ${talukaList.size} talukas.")

                        val talukaNames = talukaList.mapNotNull { it.DESCRIPTION }
                        setupDropdown(binding.Taluka, talukaNames)
                    } else {
                        Log.e("LegalApi", "fetchTalukas Failed! Error Body: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("LegalApi", "Exception in fetchTalukas: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.TalukaLayout.hint = "Taluka"
                    showCustomToast("Error fetching talukas")
                }
            }
        }
    }

    private fun fetchLegalStatusAPI(flag: String) {
        binding.LegalStatusLayout.visibility = View.VISIBLE
        binding.LegalStatusLayout.hint = "Loading Status..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getLegalTypes(flag)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        currentLegalTypeDataList = response.body()?.Data ?: emptyList()
                        val descriptions = currentLegalTypeDataList.mapNotNull { it.DESCRIPTION }

                        binding.LegalStatusLayout.hint = "Select Status"
                        setupStrictDropdown(binding.LegalStatus, descriptions)

                        // Set up SubItem Click Listener
                        binding.LegalStatus.setOnItemClickListener { _, _, position, _ ->
                            val selectedItem = currentLegalTypeDataList[position]
                            currentDataFormat = selectedItem.Format ?: "EDF"
                            currentSrNo = selectedItem.SRNO?.toString() ?: ""

                            binding.DynamicInputContainer.visibility = View.VISIBLE
                            binding.DynamicInput.setText("")

                            // ==========================================
                            // NEW CONDITIONAL LOGIC FOR DISTRICT/TALUKA
                            // ==========================================
                            val desc = selectedItem.DESCRIPTION?.trim() ?: ""
                            if (desc == "तारण मालमत्ता तपशील" || desc == "इतर तारण मालमत्ता तपशील") {
                                binding.DistrictLayout.visibility = View.VISIBLE
                                binding.TalukaLayout.visibility = View.VISIBLE
                            } else {
                                binding.DistrictLayout.visibility = View.GONE
                                binding.TalukaLayout.visibility = View.GONE
                                binding.District.setText("", false)
                                binding.Taluka.setText("", false)
                            }
                            // ==========================================
                            // Change Input Type Dynamically Based on Format
                            when (currentDataFormat) {
                                "DT" -> {
                                    binding.DynamicInputLayout.hint = "Select Date"
                                    binding.DynamicInput.isFocusable = false
                                    binding.DynamicInput.isClickable = true
                                    binding.DynamicInput.inputType = android.text.InputType.TYPE_NULL
                                    binding.btnDynamicDatePicker.visibility = View.VISIBLE

                                    binding.DynamicInput.setOnClickListener { showDatePickerFor(binding.DynamicInput) }
                                    binding.btnDynamicDatePicker.setOnClickListener { showDatePickerFor(binding.DynamicInput) }
                                }
                                "AMT" -> {
                                    binding.DynamicInputLayout.hint = "Enter Amount"
                                    binding.DynamicInput.isFocusable = true
                                    binding.DynamicInput.isFocusableInTouchMode = true
                                    binding.DynamicInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                                    binding.btnDynamicDatePicker.visibility = View.GONE
                                    binding.DynamicInput.setOnClickListener(null)
                                }
                                "NUM" -> {
                                    binding.DynamicInputLayout.hint = "Enter Number"
                                    binding.DynamicInput.isFocusable = true
                                    binding.DynamicInput.isFocusableInTouchMode = true
                                    binding.DynamicInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                    binding.btnDynamicDatePicker.visibility = View.GONE
                                    binding.DynamicInput.setOnClickListener(null)
                                }
                                "EDF" -> {
                                    binding.DynamicInputLayout.hint = "Enter Details"
                                    binding.DynamicInput.isFocusable = true
                                    binding.DynamicInput.isFocusableInTouchMode = true
                                    binding.DynamicInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
                                    binding.btnDynamicDatePicker.visibility = View.GONE
                                    binding.DynamicInput.setOnClickListener(null)
                                }
                            }
                        }

                    } else {
                        binding.LegalStatusLayout.hint = "Error Loading Status"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showCustomToast("Failed to fetch legal types") }
            }
        }
    }

    // Helper to read EXIF data and rotate image upright
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: String): Bitmap {
        val ei = android.media.ExifInterface(selectedImage)
        val orientation = ei.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle() // free up memory
        return rotatedImg
    }

    private fun dialCustomer(visit: Visit) {
        lastDialedVisit = visit
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.CustMobileNo}"))
        startActivity(intent)
    }

    private fun showDatePickerFor(targetField: TextInputEditText) {
        if (!isImageClicked) {
            showCustomToast("Please first click image")
            return
        }

        val calendar = Calendar.getInstance()
        val existingText = targetField.text?.toString()
        if (!existingText.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = sdf.parse(existingText)
                if (date != null) calendar.time = date
            } catch (e: Exception) { /* use today */ }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val formatted = String.format(Locale.getDefault(), "%02d-%02d-%04d", day, month + 1, year)
                targetField.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            show()
        }
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
                    withContext(Dispatchers.Main) { binding.tvDoneCount.text = "0" }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { binding.tvDoneCount.text = "0" }
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
                    withContext(Dispatchers.Main) { binding.tvPendingCount.text = "0" }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { binding.tvPendingCount.text = "0" }
            }
        }
    }
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
//                        if (!visits.isNullOrEmpty()) {
//                            adapter.updateList(visits)
//                        } else {
//                            showCustomToast("No visits found")
//                            adapter.updateList(emptyList())
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) { showCustomToast("API Error: ${response.code()}") }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) { showCustomToast("Exception: ${e.message}") }
//            }
//        }
//    }


    private fun fetchDoneVisits(StaffID: Int, flag: String) {
        visitFetchJob?.cancel() // Cancel any ongoing fetch instantly

        visitFetchJob = lifecycleScope.launch(Dispatchers.IO) {
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
                    withContext(Dispatchers.Main) { showCustomToast("API Error: ${response.code()}") }
                }
            } catch (e: Exception) {
                // Ignore CancellationExceptions caused by switching tabs
                if (e !is kotlinx.coroutines.CancellationException) {
                    withContext(Dispatchers.Main) { showCustomToast("Exception: ${e.message}") }
                }
            }
        }
    }


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
//                        if (!visits.isNullOrEmpty()) {
//                            adapter.updateList(visits)
//                        } else {
//                            showCustomToast("No pending visits found")
//                            adapter.updateList(emptyList())
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) { showCustomToast("API Error: ${response.code()}") }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) { showCustomToast("Exception: ${e.message}") }
//            }
//        }
//    }

    private fun fetchPendingVisits(StaffID: Int, flag: String) {
        visitFetchJob?.cancel() // Cancel any ongoing fetch instantly

        visitFetchJob = lifecycleScope.launch(Dispatchers.IO) {
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
                    withContext(Dispatchers.Main) { showCustomToast("API Error: ${response.code()}") }
                }
            } catch (e: Exception) {
                // Ignore CancellationExceptions caused by switching tabs
                if (e !is kotlinx.coroutines.CancellationException) {
                    withContext(Dispatchers.Main) { showCustomToast("Exception: ${e.message}") }
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
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllBanks()
                if (response.isSuccessful) {
                    val banks = response.body() ?: emptyList()
                    val branchNames = banks.mapNotNull { it.MIDNAME }

                    setupDropdown(binding.branchName, branchNames)

                    binding.branchName.setOnItemClickListener { _, _, _, _ ->
                        val selectedName = binding.branchName.text.toString().trim()
                        val selectedBank = banks.find { it.MIDNAME?.trim() == selectedName }

                        if (selectedBank != null) {
                            val selectedCode = selectedBank.BRCD?.toString() ?: "0"
                            binding.branchCode.setText(selectedCode, false)

                            selectedBank.BRCD?.let { brcd ->
                                fetchLoanFollowUps(brcd)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@LegalCaseVisitActivity, "Failed to fetch branches", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

//    private fun setupStrictDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
//        val adapter = ArrayAdapter(this, R.layout.dropdown_item, data)
//        autoComplete.setAdapter(adapter)
//
//        autoComplete.keyListener = null
//
//        autoComplete.setOnClickListener {
//            if (!isImageClicked) {
//                showCustomToast("Please first click image")
//            } else {
//                autoComplete.showDropDown()
//            }
//        }
//
//        autoComplete.setOnTouchListener { v, event ->
//            if (event.action == MotionEvent.ACTION_UP) {
//                if (!isImageClicked) {
//                    showCustomToast("Please first click image")
//                } else {
//                    autoComplete.showDropDown()
//                }
//            }
//            false
//        }
//    }


    private fun setupStrictDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, data)
        autoComplete.setAdapter(adapter)

        // 1. Completely disable typing
        autoComplete.keyListener = null

        // 2. FIX DOUBLE TAP: Prevent the field from absorbing the first tap just for "focus"
        autoComplete.isFocusableInTouchMode = false

        // 3. Open dropdown instantly on click
        autoComplete.setOnClickListener {
            if (!isImageClicked) {
                showCustomToast("Please first click image")
            } else {
                autoComplete.showDropDown()
            }
        }

        // 4. (Optional) Auto-open if the user tabs into this field using the keyboard's "Next" button
        autoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isImageClicked) {
                autoComplete.showDropDown()
            }
        }
    }


    private fun fetchLoanFollowUps(branchCode: Int) {
        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")

        binding.Custname.setText(null)
        binding.CustnameLayout.hint = "Loading Customers..."
        binding.Custname.isEnabled = false

        lifecycleScope.launch {
            try {
                val customers = ApiClient.apiService.getmonitoringLoanFollowUps(branchCode, Empid)
                loanAcCustName.clear()
                customerMap.clear()

                if (customers.isNullOrEmpty()) {
                    clearCustomerFields()
                    binding.CustnameLayout.hint = "No Customers Found"
                    binding.Custname.isEnabled = false
                    showCustomToast("No customer assigned for selected branch")
                    return@launch
                }

                customers.forEach { customer ->
                    val name = customer.Name?.trim() ?: ""
                    if (name.isNotEmpty()) {
                        loanAcCustName.add(name)
                        customerMap[name] = customer
                    }
                }

                binding.CustnameLayout.hint = "Customer Name with A/c No."
                binding.Custname.isEnabled = true
                setupCustNameDropdown()

            } catch (e: Exception) {
                clearCustomerFields()
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

                val mobileStr = it.MobileNo?.trim() ?: ""
                binding.CustMobNo.setText(mobileStr)
                binding.CustMobNo.isEnabled = mobileStr.isEmpty()

                val accountNumber = try {
                    it.CUSTACCNO?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                } catch (e: Exception) {
                    0L
                }
                binding.LoanAcNo.setText(accountNumber.toString())

                binding.LoanCategory.setText(it.AssetCode ?: "")

                val amount = it.OverDueAMT?.toString()?.replace(",", "")
                if (!amount.isNullOrEmpty()) {
                    try {
                        val parsed = amount.toDouble()
                        val formatter = NumberFormat.getInstance(Locale("en", "IN")) as DecimalFormat
                        formatter.applyPattern("#,##,##0")
                        binding.LoanAmount.setText(formatter.format(parsed))
                    } catch (e: Exception) {
                        binding.LoanAmount.setText(amount)
                    }
                } else {
                    binding.LoanAmount.setText("")
                }

//                binding.LoanDisbDate.setText(it.DisbursedDate ?: "")
            }
        }
    }

    private fun clearCustomerFields() {
        binding.LoanType.setText("")
        binding.CustMobNo.setText("")
        binding.CustMobNo.isEnabled = true
        binding.LoanAcNo.setText("")
        binding.LoanCategory.setText("")
        binding.LoanAmount.setText("")
//        binding.LoanDisbDate.setText("")
        findViewById<AutoCompleteTextView>(R.id.Custname).setText("")
    }

    fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, data)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                showCustomToast("Please first click image")
                v.clearFocus()
                return@setOnTouchListener true
            }

            if (event.action == MotionEvent.ACTION_UP) {
                if (!autoComplete.isPopupShowing) {
                    autoComplete.showDropDown()
                }
                v.performClick()
            }
            false
        }

        autoComplete.threshold = 1
        autoComplete.isFocusable = true
        autoComplete.isFocusableInTouchMode = true
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
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showCustomToast("Location permission not granted")
            return
        }

        showLoadingDialog()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                processLocationInBackground(location)
            } else {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { freshLocation ->
                        if (freshLocation != null) {
                            processLocationInBackground(freshLocation)
                        } else {
                            dismissLoadingDialog()
                            showCustomToast("Unable to fetch location. Please ensure location is enabled.")
                        }
                    }
                    .addOnFailureListener { e ->
                        dismissLoadingDialog()
                        showCustomToast("Failed to get location: ${e.message}")
                    }
            }
        }.addOnFailureListener {
            dismissLoadingDialog()
            showCustomToast("Failed to access location services.")
        }
    }

    private fun processLocationInBackground(location: android.location.Location) {
        latitude = location.latitude
        longitude = location.longitude

        lifecycleScope.launch(Dispatchers.IO) {
            fullAddress = getAddressFromLocation(latitude, longitude)

            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            currentDateTime = currentDate

//            val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
//            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            val rawBitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            val uprightBitmap = rotateImageIfRequired(rawBitmap, photoFile!!.absolutePath)
            val mutableBitmap = uprightBitmap.copy(Bitmap.Config.ARGB_8888, true)

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

            val lines = listOf("📍 $fullAddress", "📅 $currentDateTime", "📌 Lat: $latitude", "📌 Long: $longitude")
            val x = 30f
            val yStart = mutableBitmap.height - lines.size * 80f - 30f

            canvas.drawRect(0f, yStart - 40f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)

            var y = yStart
            lines.forEach {
                canvas.drawText(it, x, y, paint)
                y += 80f
            }

            withContext(Dispatchers.Main) {
                binding.etLat.setText(latitude.toString())
                binding.etLong.setText(longitude.toString())
                binding.etDate.setText(currentDate)
                binding.etVisitTime.setText(currentTime)
                binding.etAddress.setText(fullAddress)

                binding.profilePhoto.setImageBitmap(mutableBitmap)
                dismissLoadingDialog()
            }
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Address not found"
        } catch (e: Exception) {
            "Address not found"
        }
    }

    private fun enableFieldsAfterPhoto() {
        binding.branchName.isEnabled = true
        binding.Custname.isEnabled = true
        binding.Comments.isEnabled = true
        binding.CommentsNew.isEnabled = true // <-- ADD THIS

        if(binding.CustMobNo.text.isNullOrEmpty()) binding.CustMobNo.isEnabled = true

        binding.LoanAmountLayout.isEnabled = true
        binding.LoanAmount.isEnabled = true
//        binding.LoanDisbDateLayout.isEnabled = true
//        binding.LoanDisbDate.isEnabled = true

        binding.LegalCaseCategoryLayout.isEnabled = true
        binding.LegalCaseCategory.isEnabled = true

        binding.Comments.isEnabled = true
        binding.District.isEnabled = true
        binding.Taluka.isEnabled = true
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

        if (binding.etDate.text.isNullOrBlank()) { missingFields.add("Visit Date"); setFieldError(binding.etDateLayout, true) } else setFieldError(binding.etDateLayout, false)
        if (binding.etVisitTime.text.isNullOrBlank()) { missingFields.add("Visit Time"); setFieldError(binding.etVisitTimeLayout, true) } else setFieldError(binding.etVisitTimeLayout, false)
        if (binding.etLat.text.isNullOrBlank()) { missingFields.add("Location"); setFieldError(binding.etLatLayout, true) } else setFieldError(binding.etLatLayout, false)
        if (binding.branchName.text.isNullOrBlank()) { missingFields.add("Branch Name"); setFieldError(binding.branchNameLayout, true) } else setFieldError(binding.branchNameLayout, false)
        if (binding.Custname.text.isNullOrBlank()) { missingFields.add("Customer Name"); setFieldError(binding.CustnameLayout, true) } else setFieldError(binding.CustnameLayout, false)

        val mobile = binding.CustMobNo.text.toString().trim()
        if (mobile.length != 10 || !mobile.all { it.isDigit() }) { missingFields.add("Valid Mobile No"); setFieldError(binding.CustMobNoLayout, true) } else setFieldError(binding.CustMobNoLayout, false)

        // Ensure category is selected
        if (binding.LegalCaseCategory.text.isNullOrBlank()) { missingFields.add("Legal Case Category"); setFieldError(binding.LegalCaseCategoryLayout, true) } else setFieldError(binding.LegalCaseCategoryLayout, false)

        // Require comments only if "Other Cases" is selected
        if (binding.LegalCaseCategory.text.toString() == "Other Cases") {
            if (binding.Comments.text.isNullOrBlank()) { missingFields.add("Comments"); setFieldError(binding.CommentsLayout, true) } else setFieldError(binding.CommentsLayout, false)
        }

        // Only validate District & Taluka if they are visible on the screen
        if (binding.DistrictLayout.visibility == View.VISIBLE) {
            if (binding.District.text.isNullOrBlank()) { missingFields.add("District"); setFieldError(binding.DistrictLayout, true) } else setFieldError(binding.DistrictLayout, false)
            if (binding.Taluka.text.isNullOrBlank()) { missingFields.add("Taluka"); setFieldError(binding.TalukaLayout, true) } else setFieldError(binding.TalukaLayout, false)
        }
        return missingFields
    }

    private fun setFieldError(layout: TextInputLayout, isError: Boolean) {
        if (isError) {
            layout.boxStrokeColor = resources.getColor(R.color.red, null)
        } else {
            layout.boxStrokeColor = resources.getColor(R.color.colorPrimaryDark, null)
        }
    }

//    private fun setupFieldWatchers() {
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
////            binding.LoanDisbDate to binding.LoanDisbDateLayout,
//            binding.Comments to binding.CommentsLayout,
//            binding.Comments to binding.CommentsLayout,
//            binding.CommentsNew to binding.CommentsNewLayout, // <-- ADD THIS
//            binding.District to binding.DistrictLayout,
//            binding.Taluka to binding.TalukaLayout
//        )
//
//        fieldMap.forEach { (editText, layout) ->
//            editText.addTextChangedListener(SimpleTextWatcher { setFieldError(layout, false) })
//            editText.setOnTouchListener { v, event ->
//                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
//                    showCustomToast("Please first click image")
//                    v.clearFocus()
//                    true
//                } else {
//                    false
//                }
//            }
//        }
//
//        // LegalCategory touch listener blocking until image capture
//        binding.LegalCaseCategory.setOnTouchListener { v, event ->
//            if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
//                showCustomToast("Please first click image")
//                v.clearFocus()
//                true
//            } else {
//                false
//            }
//        }
//    }

    private fun setupFieldWatchers() {
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
            binding.Comments to binding.CommentsLayout,
            binding.CommentsNew to binding.CommentsNewLayout,
            binding.District to binding.DistrictLayout,
            binding.Taluka to binding.TalukaLayout
        )

        fieldMap.forEach { (editText, layout) ->
            editText.addTextChangedListener(SimpleTextWatcher { setFieldError(layout, false) })

            // FIX: Only apply this generic touch listener to normal TextInputs.
            // AutoCompleteTextViews (Dropdowns) already have their own click listeners set up!
            if (editText !is AutoCompleteTextView) {
                editText.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                        showCustomToast("Please first click image")
                        v.clearFocus()
                        true
                    } else {
                        false
                    }
                }
            }
        }

        // (Removed the manual LegalCaseCategory touch listener here because
        // setupStrictDropdown already handles the image-clicked protection natively)
    }

    private fun submitLegalCaseVisit() {
        showLoadingDialog()

        val staffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toIntOrNull() ?: 0

        lifecycleScope.launch {
            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile) }

            val visitDate = createPart(convertDateForServer(binding.etDate.text?.toString() ?: ""))
            val visitTime = createPart(binding.etVisitTime.text?.toString() ?: "")
            val sID = createPart(staffID.toString())
            val staffName = createPart(binding.StaffName.text?.toString() ?: "")
            val branchName = createPart(binding.branchName.text?.toString() ?: "")
            val branchCode = createPart(binding.branchCode.text?.toString() ?: "")

            val custAccNo = createPart(binding.Custname.text?.toString() ?: "")
            val loanAccountNo = createPart(binding.LoanAcNo.text?.toString() ?: "")
            val loanType = createPart(binding.LoanType.text?.toString() ?: "")
            val customerMobileNo = createPart(binding.CustMobNo.text?.toString() ?: "")
            val loanCategory = createPart(binding.LoanCategory.text?.toString() ?: "")
            val loanAmount = createPart(parseAmountForServer(binding.LoanAmount.text?.toString() ?: ""))
//            val loanDisbursedDate = createPart(convertDateForServer(binding.LoanDisbDate.text?.toString() ?: ""))
            val loanDisbursedDate = createPart(convertDateForServer(""))

            val fullAddress = createPart(binding.etAddress.text?.toString() ?: "")

            val endUseFundsComments = createPart("")
            val otherRemarks = createPart(binding.Comments.text?.toString() ?: "")

            val latitude = createPart(binding.etLat.text?.toString() ?: "")
            val longitude = createPart(binding.etLong.text?.toString() ?: "")

            // Passing empty parameters for old API fields to avoid compilation break while retaining payload structure
            val case1 = createPart("")
            val case1_DT = createPart(convertDateForServer(""))

            val case2 = createPart("")
            val case2_DT = createPart(convertDateForServer(""))

//            // Get Category and convert to number code
//            val selectedCategory = binding.LegalCaseCategory.text.toString()
//            val caseTypeStr = when (selectedCategory) {
//                "91 Cases" -> "91"
//                "101 Cases" -> "101"
//                "138 Cases" -> "138"
//                else -> "Other"
//            }
            // Get Category and convert to number code
            val selectedCategory = binding.LegalCaseCategory.text.toString()
            val caseTypeStr = when (selectedCategory) {
                "91 Cases" -> "91"
                "101 Cases" -> "101"
                "138 Cases" -> "138"
                "OTS Cases" -> "139"
                else -> "Other"
            }

            // Update InputStatus to use Format, and extract SRNO
            val inputStatusStr = if (caseTypeStr == "Other") "Other Remarks" else currentDataFormat
            val srnoStr = if (caseTypeStr == "Other") "0" else currentSrNo

            var rawInput = if (caseTypeStr == "Other") binding.Comments.text.toString() else binding.DynamicInput.text.toString()
            if (currentDataFormat == "DT" && caseTypeStr != "Other") {
                rawInput = convertDateForServer(rawInput) // Format Date if DT
            }

            // Create the new RequestBody parts
            val inputReq = createPart(rawInput)
            val inputStatusReq = createPart(inputStatusStr)
            val caseTypeReq = createPart(caseTypeStr)
            val srnoReq = createPart(srnoStr) // <-- CREATE SRNO PART
            val commentsNewReq = createPart(binding.CommentsNew.text?.toString() ?: "") // <-- EXTRACT TEXT
            val districtReq = createPart(binding.District.text?.toString() ?: "")
            val talukaReq = createPart(binding.Taluka.text?.toString() ?: "")

            // >>> Make sure to pass all 4 of these variables into your ApiClient call below! <<<
            // Input = inputReq,
            // InputStatus = inputStatusReq,
            // Casetype = caseTypeReq,
            // Srno = srnoReq,



            Log.d("LegalSubmitAPI", "Submitting Visit...")

            ApiClient.apiService.InsertLegalCasesVisit(
                VisitDate = visitDate,
                VisitTime = visitTime,
                StaffID = sID,
                StaffName = staffName,
                BranchName = branchName,
                BranchCode = branchCode,
                CustAccNo = custAccNo,
                LoanAccNo = loanAccountNo,
                LoanType = loanType,
                CustMobileNo = customerMobileNo,
                LoanCategory = loanCategory,
                LoanAmount = loanAmount,
                LoanDisbursedDate = loanDisbursedDate,
                FullAddress = fullAddress,
                EndUseFundsComments = endUseFundsComments,
                OtherRemarks = otherRemarks,
                Latitude = latitude,
                Longitude = longitude,
                Case1 = case1,
                Case2 = case2,
                Case1_DT = case1_DT,
                Case2_DT = case2_DT,
                 Input = inputReq,
                 InputStatus = inputStatusReq,
                 Casetype = caseTypeReq,
                Srno = srnoReq,
                CommentsNew = commentsNewReq,
                District = districtReq, // <-- ADD THIS
                Taluka = talukaReq,     // <-- ADD THIS
                image = imagePart
            ).enqueue(object : Callback<LegalCaseVisitResponse> {
//                override fun onResponse(call: Call<LegalCaseVisitResponse>, response: Response<LegalCaseVisitResponse>) {
//                    dismissLoadingDialog()
//                    if (response.isSuccessful && response.body() != null) {
//                        Log.d("LegalSubmitAPI", "Success Response: ${response.body().toString()}")
//                        Toast.makeText(this@LegalCaseVisitActivity, "Legal case visit recorded successfully!", Toast.LENGTH_SHORT).show()
//                        startActivity(Intent(this@LegalCaseVisitActivity, VisitActivity::class.java))
//                        finish()
//                    } else {
//                        val errorBodyStr = response.errorBody()?.string()
//                        Log.e("LegalSubmitAPI", "Failed Response Code: ${response.code()}, Error: $errorBodyStr")
//                        Toast.makeText(this@LegalCaseVisitActivity, "API Error: $errorBodyStr", Toast.LENGTH_LONG).show()
//                    }
//                }

                override fun onResponse(call: Call<LegalCaseVisitResponse>, response: Response<LegalCaseVisitResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        Log.d("LegalSubmitAPI", "Success Response: ${response.body().toString()}")
                        Toast.makeText(this@LegalCaseVisitActivity, "Legal case visit recorded successfully!", Toast.LENGTH_SHORT).show()

                        // FIX: Clear the back stack so we don't pile up activities
                        val intent = Intent(this@LegalCaseVisitActivity, VisitActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()

                    } else {
                        val errorBodyStr = response.errorBody()?.string()
                        Log.e("LegalSubmitAPI", "Failed Response Code: ${response.code()}, Error: $errorBodyStr")
                        Toast.makeText(this@LegalCaseVisitActivity, "API Error: $errorBodyStr", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<LegalCaseVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    Log.e("LegalSubmitAPI", "Network Failure: ${t.localizedMessage}")
                    Toast.makeText(this@LegalCaseVisitActivity, "Network Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun createPart(value: String): RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), value)

    private fun createImagePart(file: File?, partName: String = "Image"): MultipartBody.Part? {
        val compressedFile = file?.let { compressImage(it, 50) }
        return compressedFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, it.name, requestFile)
        }
    }

//    private fun compressImage(file: File, maxKb: Int = 50): File? {
//        return try {
//            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//            val outFile = File(cacheDir, "compressed_${file.name}")

    private fun compressImage(file: File, maxKb: Int = 50): File? {
        return try {
            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
            val bitmap = rotateImageIfRequired(rawBitmap, file.absolutePath) // Fix rotation before compress

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
            outFile
        } catch (e: Exception) { null }
    }

    private fun convertDateForServer(displayDate: String): String = try {
        val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date: Date = displayFormat.parse(displayDate)!!
        serverFormat.format(date)
    } catch (e: Exception) { "" }

    private fun parseAmountForServer(amountText: String?): String = try {
        val cleaned = amountText?.replace(",", "") ?: "0"
        String.format("%.2f", cleaned.toDouble())
    } catch (e: Exception) { "0.00" }

    private fun showLoadingDialog() {
        val dialogBinding = DialogLoadingBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        dialogBinding.tvLoading.text = "Please wait..."
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() { loadingDialog?.dismiss() }
}

