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
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.visitapp.Adapter.CustomerAdapter
import com.avs.visitapp.Adapter.SimpleTextWatcher
import com.avs.visitapp.Adapter.VisitAgainAdapter
import com.avs.visitapp.databinding.DelinquencyVisitActivityBinding
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.google.android.gms.common.api.Api
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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.avs.visitapp.VisitAgainModel


@RequiresApi(Build.VERSION_CODES.M)
class DelinquencyVisitActivity : AppCompatActivity() {

    private lateinit var binding : DelinquencyVisitActivityBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var visitAgainAdapter: VisitAgainAdapter

    private var photoFile: File? = null
    private var loadingDialog: AlertDialog? = null
    private lateinit var adapter: CustomerAdapter
    private var lastDialedVisit: Visit? = null
    private val CAMERA_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private var latitude = 0.0
    private var longitude = 0.0
    private var fullAddress = ""
    private var currentDateTime = ""
    private var isImageClicked = false

    private val loanAcCustName = mutableListOf<String>()

    private val customerMap = mutableMapOf<String, CustomerNameResponse>()
    private val caseOptions = listOf("101", "91", "NA")
    private val promiseOptions = listOf("Yes", "No")

    private val SPEECH_REQUEST_CODE = 200


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DelinquencyVisitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarBack.setOnClickListener {
            val intent = Intent(this, VisitActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnSpeakComment.setOnClickListener {
            startSpeechToText()
        }
//        binding.CommentsLayout.setEndIconOnClickListener {
//            startSpeechToText()
//        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupBranchDropdown()

        binding.profilePhoto.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        binding.PTPDate.setOnClickListener {
            openDatePicker()
        }


        setupOverdueAmountFormatting()

//        // ✅ Add this block to open Add Visit section by default
//        binding.radioGroup.visibility = View.GONE
//        binding.fabAdd.visibility = View.GONE
//        binding.rvvisit.visibility = View.GONE
//        binding.scrollContent.visibility = View.VISIBLE
//        // ------------------------------

        val delinquencymobileLayout = binding.CustMobNoLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val delinquencymobileEditText = binding.CustMobNo.findViewById<TextInputEditText>(R.id.CustMobNo)
        val StaffID = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toInt() ?: 0

        delinquencymobileEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    delinquencymobileLayout.boxStrokeColor = ContextCompat.getColor(this@DelinquencyVisitActivity, R.color.red)
                    delinquencymobileLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    delinquencymobileLayout.boxStrokeColor = ContextCompat.getColor(this@DelinquencyVisitActivity, R.color.colorPrimaryDark)
                    delinquencymobileLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })



//        binding.submit.setOnClickListener {
//            if (validateAndSubmit()) {
//                submitDelinquencyVisit()
//            }
//        }

        binding.btnVisitAgain.setOnClickListener {
            if (validateAndSubmit()) {
                submitDelinquencyVisit(isVisitAgain = true)
            }
        }




        binding.btnVisitDone.setOnClickListener {
            if (validateAndSubmit()) {
                submitDelinquencyVisit(isVisitAgain = false)
            }
        }


        binding.fabAdd.setOnClickListener {

            binding.radioGroup.visibility = View.GONE
            binding.fabAdd.visibility = View.GONE
            binding.rvvisit.visibility = View.GONE

            binding.scrollContent.visibility = View.VISIBLE
        }


        adapter = CustomerAdapter(emptyList()) { selectedVisit ->
            dialCustomer(selectedVisit)
        }

        binding.rvvisit.layoutManager = LinearLayoutManager(this)
        binding.rvvisit.adapter = adapter


        visitAgainAdapter = VisitAgainAdapter(emptyList()) { selectedVisitAgain ->
            openVisitAgainUpdate(selectedVisitAgain)
        }

        binding.rvvisit.layoutManager = LinearLayoutManager(this)



        fetchDoneCount(StaffID, "DQ")
        fetchPendingCount(StaffID, "DQ")
        fetchVisitAgainCount(StaffID)   // ✅ ADD THIS
        fetchPendingVisits(StaffID = StaffID, flag = "DQ")
        binding.rbPending.isChecked = true


        binding.rbPending.setOnClickListener {
            binding.rbDone.isChecked = false
            binding.rbVisitAgain.isChecked = false

            binding.rvvisit.adapter = adapter        // ✔ use normal adapter
            adapter.setCurrentTab("Pending")

            fetchPendingVisits(StaffID, "DQ")
        }

        binding.rbDone.setOnClickListener {
            binding.rbPending.isChecked = false
            binding.rbVisitAgain.isChecked = false

            binding.rvvisit.adapter = adapter        // ✔ same CustomerAdapter
            adapter.setCurrentTab("Done")

            fetchDoneVisits(StaffID, "DQ")
        }

        binding.rbVisitAgain.setOnClickListener {
            binding.rbPending.isChecked = false
            binding.rbDone.isChecked = false

            binding.rvvisit.adapter = visitAgainAdapter   // ✔ custom adapter
            fetchVisitAgainList(StaffID)
        }


        val promiseDropdown = findViewById<AutoCompleteTextView>(R.id.PromisetoPay)
        promiseDropdown.keyListener = null  // Prevent manual typing
        promiseDropdown.setOnClickListener {
            promiseDropdown.showDropDown()  // Show options on tap
        }
        promiseDropdown.setOnTouchListener { _, _ ->
            promiseDropdown.showDropDown()
            false
        }
//
//        // toggle search icon functionality
//        val searchIcon = findViewById<ImageView>(R.id.toolbarSearch)
//        val title = findViewById<TextView>(R.id.toolbarTitle)
//        val searchEdit = findViewById<EditText>(R.id.etSearchBar)
//
//        searchIcon.setOnClickListener {
//            if (searchEdit.visibility == View.GONE) {
//                // Show search bar inside toolbar
//                searchEdit.visibility = View.VISIBLE
//                title.visibility = View.GONE
//                searchEdit.requestFocus()
//            } else {
//                // Hide search bar
//                searchEdit.visibility = View.GONE
//                title.visibility = View.VISIBLE
//                searchEdit.text.clear()
//            }
//        }





        setupFieldWatchers()
        setupPromisetoPayListener()
    }


    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")   // Marathi
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "कृपया बोला…") // Show Marathi prompt

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetchDoneCount(staffId: Int, flag: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllDoneVisits(staffId, flag)
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


//    private fun fetchVisitAgainList(staffId: Int) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = ApiClient.apiService.getAllVisitAgainList(staffId)
//                if (response.isSuccessful) {
//                    val visitAgainResponse = response.body()
//                    val visits = visitAgainResponse?.Visits
//
//                    withContext(Dispatchers.Main) {
//                        binding.tvVisitAgainCount.text = visitAgainResponse?.TotalCount?.toString() ?: "0"
//
//                        if (!visits.isNullOrEmpty()) {
//                            adapter.updateList(visits)
//                        } else {
//                            showCustomToast("No Visit Again entries found")
//                            adapter.updateList(emptyList())
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        binding.tvVisitAgainCount.text = "0"
//                        showCustomToast("API Error: ${response.code()}")
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    binding.tvVisitAgainCount.text = "0"
//                    showCustomToast("Error: ${e.message}")
//                }
//            }
//        }
//    }


    private fun fetchVisitAgainList(staffId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllVisitAgainList(staffId)
                if (response.isSuccessful) {

                    val visitAgainResponse = response.body()
                    val visits = visitAgainResponse?.Visits ?: emptyList()

                    withContext(Dispatchers.Main) {
                        binding.tvVisitAgainCount.text = visitAgainResponse?.TotalCount?.toString() ?: "0"

                        visitAgainAdapter.updateList(visits)   // ← FIXED

                        if (visits.isEmpty()) {
                            showCustomToast("No Visit Again entries found")
                        }
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvVisitAgainCount.text = "0"
                        showCustomToast("API Error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvVisitAgainCount.text = "0"
                    showCustomToast("Error: ${e.message}")
                }
            }
        }
    }



    private fun fetchVisitAgainCount(staffId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllVisitAgainList(staffId)
                if (response.isSuccessful) {
                    val data = response.body()
                    withContext(Dispatchers.Main) {
                        binding.tvVisitAgainCount.text = data?.TotalCount?.toString() ?: "0"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvVisitAgainCount.text = "0"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvVisitAgainCount.text = "0"
                }
            }
        }
    }




    override fun onResume() {
        super.onResume()
        lastDialedVisit?.let { visit ->
            showRemarkDialog(visit)
            lastDialedVisit = null
        }
    }


    private fun showRemarkDialog(selectedVisit: Visit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_remark, null)

        val etMobile = dialogView.findViewById<EditText>(R.id.mobile)
        val etRemark = dialogView.findViewById<EditText>(R.id.etRemark)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        etMobile.setText(selectedVisit.CustMobileNo)
        etMobile.isEnabled = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val remarkText = etRemark.text.toString().trim()
            if (remarkText.isNotEmpty()) {
                btnSave.isEnabled = false
                val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val request = CallDelinquencyVisitRequest(
                            StaffID = staffId,
                            CustomerName = selectedVisit.CustomerName,
                            CustomerMobileNo = selectedVisit.CustMobileNo,
                            AccountNo = selectedVisit.LoanAccNo,
                            LoanAccountNo = selectedVisit.LoanAccNo,
                            CallDate = currentDate,
                            CallTime = currentTime,
                            BranchCode = selectedVisit.BRCD.toString(),
                            OverdueAmount = selectedVisit.ODAmt.toDoubleOrNull() ?: 0.0,
                            CallRemarks = remarkText
                        )


                        val response = ApiClient.apiService.insertCallDelinquencyVisit(request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(
                                    this@DelinquencyVisitActivity,
                                    "Remark saved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()

                                val staffId = Constant.getSharedPreference(this@DelinquencyVisitActivity, Constant.SHARE_EMP_CD, "")?.toInt() ?: 0
                                fetchPendingVisits(StaffID = staffId, flag = "DQ")
                                binding.rbPending.isChecked = true

                            } else {
                                Toast.makeText(
                                    this@DelinquencyVisitActivity,
                                    "Failed to save remark: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                btnSave.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@DelinquencyVisitActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            btnSave.isEnabled = true
                        }
                    }
                }

            } else {
                etRemark.error = "Please enter a remark"
            }
        }

        dialog.show()
    }



    private fun dialCustomer(visit: Visit) {
        lastDialedVisit = visit
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.CustMobileNo}"))
        startActivity(intent)
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
                        this@DelinquencyVisitActivity,
                        "Failed to fetch branches",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@DelinquencyVisitActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


//    private fun fetchLoanFollowUps(branchCode: Int) {
//        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        Log.d("BranchSelection", "Fetching loan follow-ups for branch: $branchCode")
//        Log.d("Empid", "Empid: $branchCode")
//
//        lifecycleScope.launch {
//            try {
//                val customers = ApiClient.apiService.getLoanFollowUps(branchCode, Empid)
//
//                loanAcCustName.clear()
//                customerMap.clear()
//
//                if (customers.isNullOrEmpty()) {
//                    // No data returned, clear fields and show toast once
//                    clearCustomerFields()
//                    findViewById<AutoCompleteTextView>(R.id.Custname).isEnabled = false
//                    showCustomToast("No customer assigned for selected branch")
//                    return@launch
//                }
//
//                customers.forEach { customer ->
////                    val name = customer.Name.trim()
//                    val name = customer.Name?.trim() ?: ""
//
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
//                showCustomToast("Error: ${e.message}")
//            }
//        }
//    }


    private fun fetchLoanFollowUps(branchCode: Int) {
        val Empid = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")

        // 1. RESET STATE: Clear the text immediately so no "black name" is visible
        binding.Custname.setText(null)

        // Change the LABEL on the TextInputLayout to show status
        binding.CustnameLayout.hint = "Loading Customers..."

        // Disable the actual text view to prevent clicks
        binding.Custname.isEnabled = false

        lifecycleScope.launch {
            try {
                val customers = ApiClient.apiService.getLoanFollowUps(branchCode, Empid)

                loanAcCustName.clear()
                customerMap.clear()

                if (customers.isNullOrEmpty()) {
                    clearCustomerFields()

                    // No Data Found State
                    binding.CustnameLayout.hint = "No Customers Found"
                    binding.Custname.isEnabled = false

                    showCustomToast("No customer assigned for selected branch")
                    return@launch
                }

//                customers.forEach { customer ->
//                    val name = customer.Name?.trim() ?: ""
//                    loanAcCustName.add(name)
//                    customerMap[name] = customer
//                }

                customers.forEach { customer ->
                    // OLD RISK: This crashes if Name is null
                    // val name = customer.Name.trim()

                    // NEW FIX: Safely handle nulls
                    val name = customer.Name?.trim() ?: ""

                    if (name.isNotEmpty()) {
                        loanAcCustName.add(name)
                        customerMap[name] = customer
                    }
                }

                // 2. DATA LOADED: Restore the original label and enable clicking
                binding.CustnameLayout.hint = "Customer Name"
                binding.Custname.isEnabled = true

                // Re-apply the dropdown logic (using the Fix from step 1)
                setupCustNameDropdown()

            } catch (e: Exception) {
                e.printStackTrace()
                clearCustomerFields()

                // Error State
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
                // ✅ Set values from API response
                binding.LoanType.setText(it.LOANGLCODE ?: "")
                binding.LoanCategory.setText(it.AssetCode ?: "N/A")

                val overdue = it.OverDueAMT?.toDoubleOrNull() ?: 0.0
                binding.OverdueAmount.setText(String.format("%.2f", overdue))

                // ---------------------------------------------------------
                // START CHANGE: Conditional Mobile Number Editable Logic
                val mobileFromApi = it.MobileNo?.trim()

                if (!mobileFromApi.isNullOrEmpty()) {
                    // API returned a number: Fill it and Disable editing
                    binding.CustMobNo.setText(mobileFromApi)
                    binding.CustMobNo.isEnabled = false
                } else {
                    // API returned null/empty: Clear it and Enable editing
                    binding.CustMobNo.setText("")
                    binding.CustMobNo.isEnabled = true
                }
                // ---------------------------------------------------------

//                binding.CustMobNo.setText(it.MobileNo ?: "9999999999")
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
        binding.OverdueAmount.setText("")
        binding.LoanAcNo.setText("")
        binding.LoanCategory.setText("")
        findViewById<AutoCompleteTextView>(R.id.Custname).setText("")

        // ---------------------------------------------------------
        // START CHANGE: Reset Mobile field to empty and editable
        // ---------------------------------------------------------
        binding.CustMobNo.setText("")
        binding.CustMobNo.isEnabled = true
        // ---------------------------------------------------------
        // END CHANGE
        // ---------------------------------------------------------
    }



    private val customLayout = R.layout.dropdown_item


    // customer name click issue 27-12-2025
    fun setupDropdown(autoComplete: AutoCompleteTextView, data: List<String>) {
        val adapter = ArrayAdapter(this, customLayout, data)
        autoComplete.setAdapter(adapter)

        // We use setOnTouchListener instead of setOnClickListener.
        // This bypasses the "First Tap Focus" issue where the click listener gets ignored.
        autoComplete.setOnTouchListener { v, event ->

            // 1. SECURITY: Block interaction if image is not clicked
            // We must repeat this check here because this listener overwrites the one from setupFieldWatchers()
            if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                showCustomToast("Please first click image")
                v.clearFocus()
                return@setOnTouchListener true // Stop here, don't allow typing or clicking
            }

            // 2. LOGIC: Show dropdown immediately on Finger UP
            if (event.action == MotionEvent.ACTION_UP) {
                if (!autoComplete.isPopupShowing) {
                    autoComplete.showDropDown()
                }
                v.performClick() // Standard accessibility call
            }

            // 3. TYPING: Return 'false' to allow the event to pass through
            // This ensures the keyboard still opens and you can still search/type
            false
        }

        // Configuration to ensure typing still works for searching
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

        // Case Options
        val case = findViewById<AutoCompleteTextView>(R.id.Case)
        case.keyListener = null  // Prevent manual typing
        setupDropdown(case, caseOptions)

        // promise Options
        val promise = findViewById<AutoCompleteTextView>(R.id.PromisetoPay)
        setupDropdown(promise, promiseOptions)


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


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // -------------------------
        // 📸 CAMERA RESULT
        // -------------------------
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && photoFile != null) {
            fetchLocationAndOverlayImage()
            enableFieldsAfterPhoto()
            isImageClicked = true

            val staffName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "")
            binding.StaffName.setText(staffName)
            return
        }

        // -------------------------
        // 🎤 SPEECH-TO-TEXT RESULT
        // -------------------------
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""

            val currentText = binding.Comments.text?.toString() ?: ""
            binding.Comments.setText("$currentText $spokenText")
            binding.Comments.setSelection(binding.Comments.text!!.length)
        }
    }


    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchLocationAndOverlayImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showCustomToast("Location permission not granted")
            return
        }

        showLoadingDialog()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                dismissLoadingDialog()
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

//        saveBitmapToGallery(mutableBitmap)
        binding.profilePhoto.setImageBitmap(mutableBitmap)
    }


    private fun enableFieldsAfterPhoto() {
        binding.branchName.isEnabled = true
        binding.Custname.isEnabled = true
        binding.Case.isEnabled = true
        binding.Comments.isEnabled = true
        binding.PromisetoPay.isEnabled = true
        binding.PTPDate.isEnabled = true
        binding.VisitPlace.isEnabled = true
        binding.OtherRemarks.isEnabled = true
    }


    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // Format date and set to EditText
                val selectedDate = String.format(
                    "%02d-%02d-%04d",
                    selectedDayOfMonth,
                    selectedMonth + 1,
                    selectedYear
                )
                binding.PTPDate.setText(selectedDate)
            }, year, month, day)

        // Set minimum date to today
        datePicker.datePicker.minDate = calendar.timeInMillis

        datePicker.show()
    }


    private fun setupOverdueAmountFormatting() {
        binding.OverdueAmount.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.OverdueAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace(",", "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toLong()
                            // Use Indian Number Format
                            val formatter = NumberFormat.getInstance(Locale("en", "IN")) as DecimalFormat
                            formatter.applyPattern("#,##,##0")
                            val formatted = formatter.format(parsed)
                            current = formatted
                            binding.OverdueAmount.setText(formatted)
                            binding.OverdueAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }

                    binding.OverdueAmount.addTextChangedListener(this)
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
            showCustomToast(message)
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
            missingFields.add("Customer Branch Name")
            setFieldError(binding.branchNameLayout, true)
        } else {
            setFieldError(binding.branchNameLayout, false)
        }

        if (binding.branchCode.text.isNullOrBlank()) {
            missingFields.add("Customer Branch Code")
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

        if (binding.Case.text.isNullOrBlank()) {
            missingFields.add("Case 101/91")
            setFieldError(binding.CaseLayout, true)
        } else {
            setFieldError(binding.CaseLayout, false)
        }

        if (binding.OverdueAmount.text.isNullOrBlank()) {
            missingFields.add("Overdue Amount")
            setFieldError(binding.OverdueAmountLayout, true)
        } else {
            setFieldError(binding.OverdueAmountLayout, false)
        }

        if (binding.Comments.text.isNullOrBlank()) {
            missingFields.add("Comments")
            setFieldError(binding.CommentsLayout, true)
        } else {
            setFieldError(binding.CommentsLayout, false)
        }

        if (binding.PromisetoPay.text.isNullOrBlank()) {
            missingFields.add("Promise to Pay")
            setFieldError(binding.PromisetoPayLayout, true)
        } else {
            setFieldError(binding.PromisetoPayLayout, false)
            if (binding.PromisetoPay.text.toString().trim().equals("Yes", ignoreCase = true)) {
                if (binding.PTPDate.text.isNullOrBlank()) {
                    missingFields.add("PTP Date")
                    setFieldError(binding.PTPDateLayout, true)
                } else {
                    setFieldError(binding.PTPDateLayout, false)
                }
            } else {
                setFieldError(binding.PTPDateLayout, false)
            }
        }



        if (binding.VisitPlace.text.isNullOrBlank()) {
            missingFields.add("Visited Place Name")
            setFieldError(binding.VisitPlaceLayout, true)
        } else {
            setFieldError(binding.VisitPlaceLayout, false)
        }

        /*if (binding.OtherRemarks.text.isNullOrBlank()) {
            missingFields.add("Other Remarks")
            setFieldError(binding.OtherRemarksLayout, true)
        } else {
            setFieldError(binding.OtherRemarksLayout, false)
        }*/

        return missingFields
    }

    private fun setupPromisetoPayListener() {
        binding.PromisetoPay.doAfterTextChanged { text ->
            val value = text.toString().trim()
            if (value.equals("Yes", ignoreCase = true)) {
                // Show PTP Date field
                binding.PTPDateLayout.visibility = View.VISIBLE
                binding.PTPDateLayout.isEnabled = true
                binding.PTPDate.isEnabled = true
                binding.PTPDate.isFocusable = false  // still clickable
            } else {
                // Hide PTP Date field
                binding.PTPDateLayout.visibility = View.GONE
                binding.PTPDateLayout.isEnabled = false
                binding.PTPDate.setText("") // clear previous value
            }
        }
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
        // --- TextChangedListeners for error reset ---
        binding.etDate.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etDateLayout, false) })
        binding.etVisitTime.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etVisitTimeLayout, false) })
        binding.etLat.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etLatLayout, false) })
        binding.etLong.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etLongLayout, false) })
        binding.etAddress.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.etAddressLayout, false) })
        binding.StaffName.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.StaffNameLayout, false) })
        binding.branchName.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.branchNameLayout, false) })
        binding.branchCode.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.branchCodeLayout, false) })
        binding.Custname.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CustnameLayout, false) })
        binding.LoanAcNo.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.LoanAcNoLayout, false) })
        binding.LoanType.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.LoanTypeLayout, false) })
        binding.CustMobNo.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CustMobNoLayout, false) })
        binding.LoanCategory.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.LoanCategoryLayout, false) })
        binding.Case.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CaseLayout, false) })
        binding.OverdueAmount.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.OverdueAmountLayout, false) })
        binding.Comments.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.CommentsLayout, false) })
        binding.PromisetoPay.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.PromisetoPayLayout, false) })
        binding.PTPDate.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.PTPDateLayout, false) })
        binding.VisitPlace.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.VisitPlaceLayout, false) })
        binding.OtherRemarks.addTextChangedListener(SimpleTextWatcher { setFieldError(binding.OtherRemarksLayout, false) })

        // --- Fields to block interaction until image is clicked ---
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
            binding.LoanAcNo,
            binding.LoanType,
            binding.CustMobNo,
            binding.LoanCategory,
            binding.Case,
            binding.OverdueAmount,
            binding.Comments,
            binding.PromisetoPay,
            binding.PTPDate,
            binding.VisitPlace,
            binding.OtherRemarks
        )

        fields.forEach { field ->
            field.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !isImageClicked) {
                    showCustomToast("Please first click image")
                    v.clearFocus()
                    true // consume event to block interaction
                } else {
                    false // allow interaction
                }
            }
        }
    }



    private fun submitDelinquencyVisit(isVisitAgain: Boolean = false)
    {
        showLoadingDialog()

        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toIntOrNull() ?: 0

        lifecycleScope.launch {
            val imagePart = withContext(Dispatchers.Default) { createImagePart(photoFile) }

            val visitDate = createPart(convertDateForServer(binding.etDate.text?.toString() ?: ""))
            val visitTime = createPart(binding.etVisitTime.text?.toString() ?: "")
            val staffID = createPart(staffId.toString())
            val staffName = createPart(binding.StaffName.text?.toString() ?: "")
            val branchName = createPart(binding.branchName.text?.toString() ?: "")
            val branchCode = createPart(binding.branchCode.text?.toString() ?: "")
            val customerName = createPart(binding.Custname.text?.toString() ?: "")
            val accountNo = createPart(binding.LoanAcNo.text?.toString() ?: "")
            val loanAccountNo = createPart(binding.LoanAcNo.text?.toString() ?: "")
            val loanType = createPart(binding.LoanType.text?.toString() ?: "")
            val customerMobileNo = createPart(binding.CustMobNo.text?.toString() ?: "")
            val loanCategory = createPart(binding.LoanCategory.text?.toString() ?: "")
            val loan101_91 = createPart(binding.Case.text?.toString() ?: "")
            val overdueAmount = createPart(parseAmountForServer(binding.OverdueAmount.text?.toString() ?: ""))
            val comments = createPart(binding.Comments.text?.toString() ?: "")
            val promiseToPay = createPart(binding.PromisetoPay.text?.toString() ?: "")
            val ptpDate = createPart(convertDateForServer(binding.PTPDate.text?.toString() ?: ""))
            val visitedPlaceName = createPart(binding.VisitPlace.text?.toString() ?: "")
            val otherRemarks = createPart(binding.OtherRemarks.text?.toString() ?: "")
            val latitude = createPart(binding.etLat.text?.toString() ?: "")
            val longitude = createPart(binding.etLong.text?.toString() ?: "")
            val fullAddress = createPart(binding.etAddress.text?.toString() ?: "")

//            ApiClient.apiService.InsertDelinquencyVisit(
//                VisitDate = visitDate,
//                VisitTime = visitTime,
//                StaffID = staffID,
//                StaffName = staffName,
//                BranchName = branchName,
//                BranchCode = branchCode,
//                CustomerName = customerName,
//                AccountNo = accountNo,
//                LoanAccountNo = loanAccountNo,
//                LoanType = loanType,
//                CustomerMobileNo = customerMobileNo,
//                LoanCategory = loanCategory,
//                Loan101_91 = loan101_91,
//                OverdueAmount = overdueAmount,
//                Comments = comments,
//                PromiseToPay = promiseToPay,
//                PTPDate = ptpDate,
//                VisitedPlaceName = visitedPlaceName,
//                OtherRemarks = otherRemarks,
//                Latitude = latitude,
//                Longitude = longitude,
//                FullAddress = fullAddress,
//                image = imagePart
//            ).enqueue(object : Callback<DelinquencyVisitResponse> {
//                override fun onResponse(
//                    call: Call<DelinquencyVisitResponse>,
//                    response: Response<DelinquencyVisitResponse>
//                ) {
//                    dismissLoadingDialog()
//                    if (response.isSuccessful && response.body() != null) {
//                        showCustomToast("Delinquency visit recorded successfully!")
//
//                        // 🔹 Call SMS API after successful visit submission
//                        val mobile = binding.CustMobNo.text?.toString()?.trim() ?: ""
//                        if (mobile.isNotEmpty()) {
//                        }
//
//                        startActivity(Intent(this@DelinquencyVisitActivity, Dashboard::class.java))
//                        finish()
//                    } else {
//                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
//                        showCustomToast("API Error: $errorMsg")
//                    }
//                }
//
//                override fun onFailure(call: Call<DelinquencyVisitResponse>, t: Throwable) {
//                    dismissLoadingDialog()
//                    showCustomToast("Network Error: ${t.localizedMessage}")
//                }
//            })

            val apiCall = if (isVisitAgain) {
                ApiClient.apiService.InsertVisitAgainDelinquencyVisit(
                    VisitDate = visitDate,
                    VisitTime = visitTime,
                    StaffID = staffID,
                    StaffName = staffName,
                    BranchName = branchName,
                    BranchCode = branchCode,
                    CustomerName = customerName,
                    AccountNo = accountNo,
                    LoanAccountNo = loanAccountNo,
                    LoanType = loanType,
                    CustomerMobileNo = customerMobileNo,
                    LoanCategory = loanCategory,
                    Loan101_91 = loan101_91,
                    OverdueAmount = overdueAmount,
                    Comments = comments,
                    PromiseToPay = promiseToPay,
                    PTPDate = ptpDate,
                    VisitedPlaceName = visitedPlaceName,
                    OtherRemarks = otherRemarks,
                    Latitude = latitude,
                    Longitude = longitude,
                    FullAddress = fullAddress,
                    image = imagePart
                )
            } else {
                ApiClient.apiService.InsertDelinquencyVisit(
                    VisitDate = visitDate,
                    VisitTime = visitTime,
                    StaffID = staffID,
                    StaffName = staffName,
                    BranchName = branchName,
                    BranchCode = branchCode,
                    CustomerName = customerName,
                    AccountNo = accountNo,
                    LoanAccountNo = loanAccountNo,
                    LoanType = loanType,
                    CustomerMobileNo = customerMobileNo,
                    LoanCategory = loanCategory,
                    Loan101_91 = loan101_91,
                    OverdueAmount = overdueAmount,
                    Comments = comments,
                    PromiseToPay = promiseToPay,
                    PTPDate = ptpDate,
                    VisitedPlaceName = visitedPlaceName,
                    OtherRemarks = otherRemarks,
                    Latitude = latitude,
                    Longitude = longitude,
                    FullAddress = fullAddress,
                    image = imagePart
                )
            }

            apiCall.enqueue(object : Callback<DelinquencyVisitResponse> {
                override fun onResponse(call: Call<DelinquencyVisitResponse>, response: Response<DelinquencyVisitResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        val message = if (isVisitAgain)
                            "Visit Again submitted successfully!"
                        else
                            "Visit Done recorded successfully!"

                        showCustomToast(message)
                        startActivity(Intent(this@DelinquencyVisitActivity, Dashboard::class.java))
                        finish()
                    } else {
                        showCustomToast("API Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<DelinquencyVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showCustomToast("Network Error: ${t.localizedMessage}")
                }
            })

        }
    }


    private fun sendSMS() {
        try {
            val message = "Welcome The SMS Banking Services With SHREE KALIKAMATA NAGARI SAHAKARI PATHSANSTHA,Thank You"
            val encodedMessage = Uri.encode(message)

            val smsUrl = "https://webpostservice.com/sendsms_v2.0/sendsms.php" +
                    "?username=GYEkal" +
                    "&password=sms2021" +
                    "&type=text" +
                    "&sender=SKMSPP" +
                    "&mobile='7021623344" +
                    "&message=$encodedMessage"

            // 🔹 Use coroutine for network call
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(smsUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {

                    } else {
                        Log.e("SMS_API", "Failed to send SMS. Response code: $responseCode")
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("SMS_API", "Error: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SMS_API", "Error preparing SMS API: ${e.message}", e)
        }
    }

    private fun createPart(value: String): RequestBody =
        RequestBody.create("text/plain".toMediaTypeOrNull(), value)

    private fun createImagePart(file: File?, partName: String = "Image"): MultipartBody.Part? {
        val compressedFile = file?.let { compressImage(it, 50) }
        return compressedFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, it.name, requestFile)
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

    private fun compressImage(file: File, maxKb: Int = 50): File? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val outFile = File(cacheDir, "compressed_${file.name}")

            var compressQuality = 100
            var streamLength: Int

            do {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
                val bmpPicByteArray = stream.toByteArray()
                streamLength = bmpPicByteArray.size / 1024 // KB
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


    private fun openVisitAgainUpdate(visit: VisitAgainModel) {
        val intent = Intent(this, VisitAgainUpdateActivity::class.java)
        intent.putExtra("visitAgainData", visit)
        startActivity(intent)
    }

}
