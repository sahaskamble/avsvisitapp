package com.avs.visitapp

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
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
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.avs.visitapp.databinding.VisitApplicationActivityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.widget.addTextChangedListener
import retrofit2.Callback
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.avs.visitapp.databinding.DialogLoadingBinding
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


class VisitApplicationActivity : AppCompatActivity() {

    private lateinit var binding: VisitApplicationActivityBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null
    private var loadingDialog: AlertDialog? = null
    private var currentVisitType: String? = null
    private val TAG = "VisitApplicationn"


    private val CAMERA_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private var latitude = 0.0
    private var longitude = 0.0
    private var fullAddress = ""
    private var currentDateTime = ""

    // Mock branch data
    private val BranchName = listOf("Mumbai", "Navi Mumbai", "Pune")
    private val BranchCode = listOf("1", "2", "3")
    // Loan Recovery dropdown options
    private val loanCategoryOptions = listOf("Standard", "Sub Standard", "D-1", "D-2", "D-3", "Loss")
    private val loanAcNumbers = listOf("123456789", "789456123", "7569514789", "895446552")
    private val loanAcCustName = listOf("Sushant/123456789", "Omkar/789456123", "Rupali/7569514789", "Swarali/895446552")
    private val caseOptions = listOf("Yes", "No", "NA")
    private val eligibility = listOf("Yes", "No")
    private val promiseOptions = listOf("Yes", "No")
    private val LoanTypeOptions = listOf("Personal Loan", "Vehical Loan" ,"Housing Loan" )
    private val CustomerRequirement = listOf("Deposit", "Loan" ,"Other  Selection","NA" )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VisitApplicationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupBranchDropdown()
        setupVisitTypeDropdownUnified()

        binding.profilePhoto.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        val delinquencymobileLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val delinquencymobileEditText = binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        delinquencymobileEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    delinquencymobileLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.red)
                    delinquencymobileLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    delinquencymobileLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.colorPrimaryDark)
                    delinquencymobileLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val MarketingmobileeLayout = binding.MarketingVisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val MarketingmobileeEditText = binding.MarketingVisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        MarketingmobileeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    MarketingmobileeLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.red)
                    MarketingmobileeLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    MarketingmobileeLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.colorPrimaryDark)
                    MarketingmobileeLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val MonitoringmobileeLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val MonitoringmobileeEditText = binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        MonitoringmobileeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s?.toString()?.trim() ?: ""

                if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                    // Invalid: red outline and error message
                    MonitoringmobileeLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.red)
                    MonitoringmobileeLayout.error = "Enter 10 digit number"
                } else {
                    // Valid: reset to default
                    MonitoringmobileeLayout.boxStrokeColor = ContextCompat.getColor(this@VisitApplicationActivity, R.color.colorPrimaryDark)
                    MonitoringmobileeLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.VisitType.setOnItemClickListener { parent, _, position, _ ->
            val selectedType = parent.getItemAtPosition(position).toString().trim()
            currentVisitType = selectedType
            binding.VisitType.setText(currentVisitType, false) // show selected value

            Log.d(TAG, "Item clicked: $currentVisitType")
            Toast.makeText(this, "Selected: '$currentVisitType'", Toast.LENGTH_SHORT).show()

            // Hide all layouts first
            binding.loanRecoveryLayout.visibility = View.GONE
            binding.loanEnquriyLayout.visibility = View.GONE
            binding.delinquencyvisitLayout.visibility = View.GONE
            binding.MarketingVisitLayout.visibility = View.GONE
            binding.MonitoringVisitLayout.visibility = View.GONE

            when (currentVisitType?.lowercase()) {
                "loan recovery" -> binding.loanRecoveryLayout.visibility = View.VISIBLE
                "loan enquiry" -> binding.loanEnquriyLayout.visibility = View.VISIBLE
                "delinquency visit" -> binding.delinquencyvisitLayout.visibility = View.VISIBLE
                "marketing visit" -> binding.MarketingVisitLayout.visibility = View.VISIBLE
                "monitoring visit" -> binding.MonitoringVisitLayout.visibility = View.VISIBLE
                else -> Log.d(TAG, "Unknown visit type: $currentVisitType")
            }
        }


        binding.submit.setOnClickListener {
            Log.d(TAG, "Submit clicked. Current visit type: $currentVisitType")

            if (currentVisitType.isNullOrBlank()) {
                Toast.makeText(this, "Please select a visit type", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "No visit type selected")
                return@setOnClickListener
            }

            when (currentVisitType!!.lowercase()) {
                "loan recovery" -> {
                    Log.d(TAG, "Validating Loan Recovery fields")
                    if (validateLoanRecoveryFields()) {
                        Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Loan Recovery submission successful")
                    }
                }
                "loan enquiry" -> {
                    Log.d(TAG, "Validating Loan Enquiry fields")
                    if (validateLoanEnquiryFields()) {
                        Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Loan Enquiry submission successful")
                    }
                }
                "delinquency visit" -> {
                    Log.d(TAG, "Validating Delinquency Visit fields")
                    if (validateDelinquencyVisitFields()) {
                        Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Delinquency Visit submission successful")
                    }
                }
                "marketing visit" -> {
                    Log.d(TAG, "Validating Marketing Visit fields")
                    if (validateMarketingVisitFields()) {
                        Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Marketing Visit submission successful")
                    }
                }
                "monitoring visit" -> {
                    Log.d(TAG, "Validating Monitoring Visit fields")
                    if (validateMonitoringVisitFields()) {
                        Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Monitoring Visit submission successful")
                    }
                }
                else -> {
                    Toast.makeText(this, "Please select a valid visit type", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Unknown or invalid visit type: $currentVisitType")
                }
            }
        }

    }

    private fun showAddTaskDialog() {
        // This function will show the popup
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
    }


    private fun enableFieldsAfterPhoto() {
        binding.StaffName.isEnabled = true
        binding.branchName.isEnabled = true
        binding.branchCode.isEnabled = true
        binding.VisitType.isEnabled = true
    }



    private fun validateDelinquencyVisitFields(): Boolean {
        val custnameLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.CustnameLayout)
        val custnameEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.Custname)

        val loanAcNoLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.LoanAcNoLayout)
        val loanAcNoEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanAcNo)

        val loanTypeLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.LoanTypeLayout)
        val loanTypeEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanType)

        val custMobLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val custMobEdit = binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        val loanCategoryLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.LoanCategoryLayout)
        val loanCategoryEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)

        val caseLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.CaseLayout)
        val caseEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.Case)

        val overdueAmountLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.OverdueAmountLayout)
        val overdueAmountEdit = binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.OverdueAmount)

        val promisetoPayLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.PromisetoPayLayout)
        val promisetoPayEdit = binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay)

        val ptpDateLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.PTPDateLayout)
        val ptpDateEdit = binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.PTPDate)

        val visitPlaceLayout = binding.delinquencyvisitLayout.findViewById<TextInputLayout>(R.id.VisitPlaceLayout)
        val visitPlaceEdit = binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.VisitPlace)

        // Customer Name
        val custname = custnameEdit.text?.toString()?.trim().orEmpty()
        if (custname.isEmpty()) {
            custnameLayout.error = "Enter Customer Name"
            custnameLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Name", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custnameLayout.error = null
        }

        // Loan Account Number
        val loanAcNo = loanAcNoEdit.text?.toString()?.trim().orEmpty()
        if (loanAcNo.isEmpty()) {
            loanAcNoLayout.error = "Enter Loan Account Number"
            loanAcNoLayout.requestFocus()
            Toast.makeText(this, "Enter Loan Account Number", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanAcNoLayout.error = null
        }

        // Loan Type
        val loanType = loanTypeEdit.text?.toString()?.trim().orEmpty()
        if (loanType.isEmpty()) {
            loanTypeLayout.error = "Select Loan Type"
            loanTypeLayout.requestFocus()
            Toast.makeText(this, "Select Loan Type", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanTypeLayout.error = null
        }

        // Customer Mobile Number
        val custMob = custMobEdit.text?.toString()?.trim().orEmpty()
        if (custMob.isEmpty()) {
            custMobLayout.error = "Enter Customer Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else if (custMob.length != 10 || !custMob.all { it.isDigit() }) {
            custMobLayout.error = "Enter valid 10-digit Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custMobLayout.error = null
        }

        // Loan Category
        val loanCategory = loanCategoryEdit.text?.toString()?.trim().orEmpty()
        if (loanCategory.isEmpty()) {
            loanCategoryLayout.error = "Select Loan Category"
            loanCategoryLayout.requestFocus()
            Toast.makeText(this, "Select Loan Category", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanCategoryLayout.error = null
        }

        // Case
        val caseVal = caseEdit.text?.toString()?.trim().orEmpty()
        if (caseVal.isEmpty()) {
            caseLayout.error = "Enter Case"
            caseLayout.requestFocus()
            Toast.makeText(this, "Enter Case", Toast.LENGTH_SHORT).show()
            return false
        } else {
            caseLayout.error = null
        }

        // Overdue Amount
        val overdueAmount = overdueAmountEdit.text?.toString()?.trim().orEmpty()

        if (overdueAmount.isEmpty()) {
            overdueAmountLayout.error = "Enter Overdue Amount"
            overdueAmountLayout.requestFocus()
            Toast.makeText(this, "Enter Overdue Amount", Toast.LENGTH_SHORT).show()
            return false
        } else {
            try {
                // Remove commas from user input
                val cleanAmount = overdueAmount.replace(",", "")
                val amount = cleanAmount.toDouble()

                if (amount <= 0) {
                    overdueAmountLayout.error = "Enter valid amount greater than 0"
                    overdueAmountLayout.requestFocus()
                    Toast.makeText(this, "Enter valid amount greater than 0", Toast.LENGTH_SHORT).show()
                    return false
                }

                // Clear error
                overdueAmountLayout.error = null

                // Optional: if you want to display the formatted plain number
                overdueAmountEdit.setText(amount.toString()) // converts "1,000" -> "1000"

            } catch (e: NumberFormatException) {
                overdueAmountLayout.error = "Enter valid number"
                overdueAmountLayout.requestFocus()
                Toast.makeText(this, "Enter valid number for Overdue Amount", Toast.LENGTH_SHORT).show()
                return false
            }
        }


        // Promise to Pay
        val promisetoPay = promisetoPayEdit.text?.toString()?.trim().orEmpty()
        if (promisetoPay.isEmpty()) {
            promisetoPayLayout.error = "Select Promise to Pay"
            promisetoPayLayout.requestFocus()
            Toast.makeText(this, "Select Promise to Pay", Toast.LENGTH_SHORT).show()
            return false
        } else {
            promisetoPayLayout.error = null
        }

        // PTP Date
        val ptpDate = ptpDateEdit.text?.toString()?.trim().orEmpty()
        if (ptpDate.isEmpty()) {
            ptpDateLayout.error = "Select PTP Date"
            ptpDateLayout.requestFocus()
            Toast.makeText(this, "Select PTP Date", Toast.LENGTH_SHORT).show()
            return false
        } else {
            ptpDateLayout.error = null
        }

        // Visit Place
        val visitPlace = visitPlaceEdit.text?.toString()?.trim().orEmpty()
        if (visitPlace.isEmpty()) {
            visitPlaceLayout.error = "Enter Visit Place"
            visitPlaceLayout.requestFocus()
            Toast.makeText(this, "Enter Visit Place", Toast.LENGTH_SHORT).show()
            return false
        } else {
            visitPlaceLayout.error = null
        }

        // All validations passed
        return true
    }

    private fun validateMarketingVisitFields(): Boolean {
        val custnameLayout = binding.MarketingVisitLayout.findViewById<TextInputLayout>(R.id.CustnameLayout)
        val custnameEdit = binding.MarketingVisitLayout.findViewById<TextInputEditText>(R.id.Custname)

        val custMobLayout = binding.MarketingVisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val custMobEdit = binding.MarketingVisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        val customerRequirementLayout = binding.MarketingVisitLayout.findViewById<TextInputLayout>(R.id.CustomerRequirementLayout)
        val customerRequirementEdit = binding.MarketingVisitLayout.findViewById<AutoCompleteTextView>(R.id.CustomerRequirement)

        // Comments is optional, no need to validate
        // val commentsLayout = binding.MarketingVisitLayout.findViewById<TextInputLayout>(R.id.CommentsLayout)
        // val commentsEdit = binding.MarketingVisitLayout.findViewById<TextInputEditText>(R.id.Comments)

        // Customer Name
        val custname = custnameEdit.text?.toString()?.trim().orEmpty()
        if (custname.isEmpty()) {
            custnameLayout.error = "Enter Customer Name"
            custnameLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Name", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custnameLayout.error = null
        }

        // Customer Mobile Number
        val custMob = custMobEdit.text?.toString()?.trim().orEmpty()
        if (custMob.isEmpty()) {
            custMobLayout.error = "Enter Customer Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else if (custMob.length != 10 || !custMob.all { it.isDigit() }) {
            custMobLayout.error = "Enter valid 10-digit Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custMobLayout.error = null
        }

        // Customer Requirement
        val customerRequirement = customerRequirementEdit.text?.toString()?.trim().orEmpty()
        if (customerRequirement.isEmpty()) {
            customerRequirementLayout.error = "Select Customer Requirement"
            customerRequirementLayout.requestFocus()
            Toast.makeText(this, "Select Customer Requirement", Toast.LENGTH_SHORT).show()
            return false
        } else {
            customerRequirementLayout.error = null
        }

        // All validations passed
        return true
    }

    private fun validateMonitoringVisitFields(): Boolean {
        val custnameLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.CustnameLayout)
        val custnameEdit = binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.Custname)

        val loanAcNoLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.LoanAcNoLayout)
        val loanAcNoEdit = binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanAcNo)

        val loanTypeLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.LoanTypeLayout)
        val loanTypeEdit = binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanType)

        val custMobLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.CustMobNoLayout)
        val custMobEdit = binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.CustMobNo)

        val loanCategoryLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.LoanCategoryLayout)
        val loanCategoryEdit = binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)

        val loanAmountLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.LoanAmountLayout)
        val loanAmountEdit = binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.LoanAmount)

        val loanDisbDateLayout = binding.MonitoringVisitLayout.findViewById<TextInputLayout>(R.id.LoanDisbDateLayout)
        val loanDisbDateEdit = binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.LoanDisbDate)

        // Customer Name
        val custname = custnameEdit.text?.toString()?.trim().orEmpty()
        if (custname.isEmpty()) {
            custnameLayout.error = "Enter Customer Name"
            custnameLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Name", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custnameLayout.error = null
        }

        // Loan Account No
        val loanAcNo = loanAcNoEdit.text?.toString()?.trim().orEmpty()
        if (loanAcNo.isEmpty()) {
            loanAcNoLayout.error = "Enter Loan Account No"
            loanAcNoLayout.requestFocus()
            Toast.makeText(this, "Enter Loan Account No", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanAcNoLayout.error = null
        }

        // Loan Type
        val loanType = loanTypeEdit.text?.toString()?.trim().orEmpty()
        if (loanType.isEmpty()) {
            loanTypeLayout.error = "Select Loan Type"
            loanTypeLayout.requestFocus()
            Toast.makeText(this, "Select Loan Type", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanTypeLayout.error = null
        }

        // Customer Mobile Number
        val custMob = custMobEdit.text?.toString()?.trim().orEmpty()
        if (custMob.isEmpty()) {
            custMobLayout.error = "Enter Customer Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter Customer Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else if (custMob.length != 10 || !custMob.all { it.isDigit() }) {
            custMobLayout.error = "Enter valid 10-digit Mobile Number"
            custMobLayout.requestFocus()
            Toast.makeText(this, "Enter valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show()
            return false
        } else {
            custMobLayout.error = null
        }

        // Loan Category
        val loanCategory = loanCategoryEdit.text?.toString()?.trim().orEmpty()
        if (loanCategory.isEmpty()) {
            loanCategoryLayout.error = "Select Loan Category"
            loanCategoryLayout.requestFocus()
            Toast.makeText(this, "Select Loan Category", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanCategoryLayout.error = null
        }

        // Loan Amount
        val loanAmountStr = loanAmountEdit.text?.toString()?.trim().orEmpty()
        if (loanAmountStr.isEmpty()) {
            loanAmountLayout.error = "Enter Loan Amount"
            loanAmountLayout.requestFocus()
            Toast.makeText(this, "Enter Loan Amount", Toast.LENGTH_SHORT).show()
            return false
        } else {
            try {
                // Remove commas
                val cleanAmountStr = loanAmountStr.replace(",", "")
                val amount = cleanAmountStr.toDouble()

                if (amount <= 0) {
                    loanAmountLayout.error = "Enter valid Loan Amount"
                    loanAmountLayout.requestFocus()
                    Toast.makeText(this, "Enter valid Loan Amount", Toast.LENGTH_SHORT).show()
                    return false
                }

                // Clear error
                loanAmountLayout.error = null

                // Optional: update EditText to show plain number
                loanAmountEdit.setText(amount.toString())

            } catch (e: NumberFormatException) {
                loanAmountLayout.error = "Enter valid number"
                loanAmountLayout.requestFocus()
                Toast.makeText(this, "Enter valid number for Loan Amount", Toast.LENGTH_SHORT).show()
                return false
            }
        }


        // Loan Disbursed Date
        val loanDisbDate = loanDisbDateEdit.text?.toString()?.trim().orEmpty()
        if (loanDisbDate.isEmpty()) {
            loanDisbDateLayout.error = "Select Loan Disbursed Date"
            loanDisbDateLayout.requestFocus()
            Toast.makeText(this, "Select Loan Disbursed Date", Toast.LENGTH_SHORT).show()
            return false
        } else {
            loanDisbDateLayout.error = null
        }

        // All validations passed
        return true
    }


    private fun setupBranchDropdown() {
        // Adapter for branch names
        val branchNameAdapter = ArrayAdapter(this, R.layout.dropdown_item, BranchName)
        binding.branchName.setAdapter(branchNameAdapter)
        binding.branchName.threshold = 0

        // Adapter for branch codes
        val branchCodeAdapter = ArrayAdapter(this, R.layout.dropdown_item, BranchCode)
        binding.branchCode.setAdapter(branchCodeAdapter)
        binding.branchCode.threshold = 0


        // Show dropdowns on click
        binding.branchName.setOnClickListener { binding.branchName.showDropDown() }
        binding.branchCode.setOnClickListener { binding.branchCode.showDropDown() }
    }

    private fun setupVisitTypeDropdownUnified() {
        binding.VisitType.setOnClickListener {
            fetchVisitTypes()
        }
    }

    private fun fetchVisitTypes() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getLoanVisitTypes()
                if (response.isSuccessful && response.body()?.status == "success") {
                    val visitTypeList = response.body()?.data?.map { it.visit_type_name } ?: emptyList()
                    setupDropdownAdapter(visitTypeList) // 🔹 call here
                } else {
                    Toast.makeText(this@VisitApplicationActivity, "Failed to load visit types", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@VisitApplicationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupDropdownAdapter(visitTypes: List<String>) {
        val visitAdapter = ArrayAdapter(this, R.layout.dropdown_item, visitTypes)
        binding.VisitType.setAdapter(visitAdapter)
        binding.VisitType.threshold = 0

        // Show dropdown when user clicks
        binding.VisitType.setOnClickListener {
            binding.VisitType.showDropDown()
        }

        binding.VisitType.setOnItemClickListener { parent, _, position, _ ->
            val selectedType = parent.getItemAtPosition(position) as String

            // 1️⃣ Store the selected value for Submit
            currentVisitType = selectedType

            // 2️⃣ Show the selected value in your separate TextView
            binding.VisitTypeName.text = selectedType

            Log.d(TAG, "Selected visit type: $currentVisitType")

            // Clear all previous visit type fields before showing the new layout
            clearAllVisitTypeFields()

            // Hide all conditional layouts first
            binding.loanRecoveryLayout.visibility = View.GONE
            binding.loanEnquriyLayout.visibility = View.GONE
            binding.delinquencyvisitLayout.visibility = View.GONE
            binding.MarketingVisitLayout.visibility = View.GONE
            binding.MonitoringVisitLayout.visibility = View.GONE

            // Show layout based on selected visit type
            when (selectedType) {
//                "Loan Recovery" -> {
//                    binding.loanRecoveryLayout.visibility = View.VISIBLE
//                    setupLoanRecoveryDropdowns()
//                }
//                "Loan Enquiry" -> {
//                    binding.loanEnquriyLayout.visibility = View.VISIBLE
//                    setupLoanEnquiryDropdowns()
//                }
                "Delinquency Visit" -> {
                    binding.delinquencyvisitLayout.visibility = View.VISIBLE
                    setupDelinquencyVisitDropdowns()
                }
                "Marketing Visit" -> {
                    binding.MarketingVisitLayout.visibility = View.VISIBLE
                    setupMarketingVisitDropdowns()
                }
                "Monitoring Visit" -> {
                    binding.MonitoringVisitLayout.visibility = View.VISIBLE
                    setupMonitoringVisitDropdowns()
                }
            }
        }
    }


    private fun clearAllVisitTypeFields() {
        fun clearViews(view: View) {
            when (view) {
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        clearViews(view.getChildAt(i))
                    }
                }
                is TextInputEditText -> {
                    view.text?.clear()
                    // Also clear the error/red border on parent TextInputLayout
                    val parentLayout = view.parent.parent
                    if (parentLayout is TextInputLayout) {
                        parentLayout.error = null
                        parentLayout.isErrorEnabled = false
                    }
                }
                is AutoCompleteTextView -> {
                    view.setText("", false)
                    // Clear red border if inside TextInputLayout
                    val parentLayout = view.parent.parent
                    if (parentLayout is TextInputLayout) {
                        parentLayout.error = null
                        parentLayout.isErrorEnabled = false
                    }
                }
                // Add other input types if needed
            }
        }

        // Call for all visit type layouts
        clearViews(binding.loanRecoveryLayout)
        clearViews(binding.loanEnquriyLayout)
        clearViews(binding.delinquencyvisitLayout)
        clearViews(binding.MarketingVisitLayout)
        clearViews(binding.MonitoringVisitLayout)
    }




    private fun setupLoanRecoveryDropdowns() {
        // Loan Category
        val loanCategoryAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanCategoryOptions)
        binding.loanRecoveryLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory).apply {
            setAdapter(loanCategoryAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // Case dropdown
        val caseAdapter = ArrayAdapter(this, R.layout.dropdown_item, caseOptions)
        binding.loanRecoveryLayout.findViewById<AutoCompleteTextView>(R.id.Case).apply {
            setAdapter(caseAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // Promise to Pay dropdown
        val promiseAdapter = ArrayAdapter(this, R.layout.dropdown_item, promiseOptions)
        binding.loanRecoveryLayout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay).apply {
            setAdapter(promiseAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // Loan Type dropdown
        val LoanTypeAdapter = ArrayAdapter(this, R.layout.dropdown_item, LoanTypeOptions)
        binding.loanRecoveryLayout.findViewById<AutoCompleteTextView>(R.id.LoanType).apply {
            setAdapter(LoanTypeAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // LoanDisbDate: date picker
        binding.loanRecoveryLayout.findViewById<TextInputEditText>(R.id.LoanDisbDate).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker(this, restrictFutureDates = true)
            }
        }

        binding.loanRecoveryLayout.findViewById<TextInputEditText>(R.id.PTPDate).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker(this, restrictFutureDates = false)
            }
        }

        binding.loanRecoveryLayout.findViewById<TextInputEditText>(R.id.OverdueAmount).apply {
            addTextChangedListener(getIndianCurrencyTextWatcher(this))
        }

    }

    private fun setupDelinquencyVisitDropdowns() {
        // Loan Category
        val loanCategoryAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanCategoryOptions)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory).apply {
            setAdapter(loanCategoryAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }
        // Loan AC number With customer name
        val loannumberwithcustAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanAcCustName)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.Custname).apply {
            setAdapter(loannumberwithcustAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }
        // Loan AC number
        val loannumberAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanAcNumbers)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanAcNo).apply {
            setAdapter(loannumberAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // Case dropdown
        val caseAdapter = ArrayAdapter(this, R.layout.dropdown_item, caseOptions)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.Case).apply {
            setAdapter(caseAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        // Promise to Pay dropdown
        val promiseAdapter = ArrayAdapter(this, R.layout.dropdown_item, promiseOptions)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay).apply {
            setAdapter(promiseAdapter)
            threshold = 0
            keyListener = null // Explicitly make it non-editable
            setOnClickListener { showDropDown() }
        }

        // Loan Type dropdown
        val LoanTypeAdapter = ArrayAdapter(this, R.layout.dropdown_item, LoanTypeOptions)
        binding.delinquencyvisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanType).apply {
            setAdapter(LoanTypeAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.PTPDate).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker(this, restrictFutureDates = false)
            }
        }

        binding.delinquencyvisitLayout.findViewById<TextInputEditText>(R.id.OverdueAmount).apply {
            addTextChangedListener(getIndianCurrencyTextWatcher(this))
        }


    }

    private fun setupMarketingVisitDropdowns() {
        val CustomerRequirementAdapter = ArrayAdapter(this, R.layout.dropdown_item, CustomerRequirement)

        // Use the correct layout where the AutoCompleteTextView actually exists
        binding.MarketingVisitLayout.findViewById<AutoCompleteTextView>(R.id.CustomerRequirement)?.apply {
            setAdapter(CustomerRequirementAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        } ?: run {
            Log.e("VisitApp", "CustomerRequirement AutoCompleteTextView not found!")
        }
    }

    private fun setupMonitoringVisitDropdowns() {
        // Loan Category
        val loanCategoryAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanCategoryOptions)
        binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory).apply {
            setAdapter(loanCategoryAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }
        // Loan AC number With customer name
        val loannumberwithcustAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanAcCustName)
        binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.Custname).apply {
            setAdapter(loannumberwithcustAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }
        // Loan AC number
        val loannumberAdapter = ArrayAdapter(this, R.layout.dropdown_item, loanAcNumbers)
        binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanAcNo).apply {
            setAdapter(loannumberAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }


        // Loan Type dropdown
        val LoanTypeAdapter = ArrayAdapter(this, R.layout.dropdown_item, LoanTypeOptions)
        binding.MonitoringVisitLayout.findViewById<AutoCompleteTextView>(R.id.LoanType).apply {
            setAdapter(LoanTypeAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }


        binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.LoanAmount).apply {
            addTextChangedListener(getIndianCurrencyTextWatcher(this))
        }

        binding.MonitoringVisitLayout.findViewById<TextInputEditText>(R.id.LoanDisbDate).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker(this, restrictFutureDates = true)
            }
        }

    }

    private fun submitLoanRecoveryVisit() {
        val layout = binding.loanRecoveryLayout

        // 1️⃣ Validate all fields first
        if (!validateLoanRecoveryFields()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return  // Stop API call if validation fails
        }

        // 2️⃣ Show loading dialog after validation passes
        showLoadingDialog()

        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")?.toIntOrNull() ?: 0

        fun convertDateForServer(displayDate: String): String {
            return try {
                val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date: Date = displayFormat.parse(displayDate)!!
                serverFormat.format(date)
            } catch (e: Exception) {
                ""
            }
        }

        fun parseAmountForServer(amountText: String?): Double {
            return try {
                val cleaned = amountText?.replace(",", "") ?: "0"
                String.format("%.2f", cleaned.toDouble()).toDouble()
            } catch (e: Exception) {
                0.0
            }
        }

        val photoBase64 = photoFile?.let { encodeImageToBase64(it) } ?: ""

        val request = LoanVisitRequest(
            Staff_id = staffId,
            Image = photoBase64,
            CustomerNameAccount = layout.findViewById<TextInputEditText>(R.id.Custname)?.text?.toString() ?: "",
            LoanAccountNo = layout.findViewById<TextInputEditText>(R.id.AcNo)?.text?.toString() ?: "",
            LoanType = layout.findViewById<AutoCompleteTextView>(R.id.LoanType)?.text?.toString() ?: "",
            CustomerMobileNo = layout.findViewById<TextInputEditText>(R.id.CustMobNo)?.text?.toString() ?: "",
            LoanCategory = layout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)?.text?.toString() ?: "",
            LoanDisbursedDate = convertDateForServer(layout.findViewById<TextInputEditText>(R.id.LoanDisbDate)?.text?.toString() ?: ""),
            Case_101_91 = layout.findViewById<AutoCompleteTextView>(R.id.Case)?.text?.toString() ?: "",
            OverdueAmount = parseAmountForServer(layout.findViewById<TextInputEditText>(R.id.OverdueAmount)?.text?.toString()),
            Comments = layout.findViewById<TextInputEditText>(R.id.Comments)?.text?.toString() ?: "",
            PromiseToPay = layout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay)?.text?.toString() ?: "",
            PtpDate = convertDateForServer(layout.findViewById<TextInputEditText>(R.id.PTPDate)?.text?.toString() ?: ""),
            VisitTime = binding.etVisitTime.text?.toString() ?: "",
            OtherRemarks = layout.findViewById<TextInputEditText>(R.id.OtherRemarks)?.text?.toString() ?: "",
            Latitude = binding.etLat.text?.toString() ?: "",
            Longitude = binding.etLong.text?.toString() ?: "",
            BankName = binding.branchName.text?.toString() ?: "",
            BranchName = binding.branchCode.text?.toString() ?: "",
            VisitDate = convertDateForServer(binding.etDate.text?.toString() ?: "")
        )

        // 3️⃣ API call
        ApiClient.apiService.insertLoanVisit(request)
            .enqueue(object : Callback<LoanVisitResponse> {
                override fun onResponse(call: Call<LoanVisitResponse>, response: Response<LoanVisitResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@VisitApplicationActivity, "Success: ${response.body()!!.Message}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@VisitApplicationActivity, Dashboard::class.java))
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
                        Toast.makeText(this@VisitApplicationActivity, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoanVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    Toast.makeText(this@VisitApplicationActivity, "Network Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
    }


    private fun submitDelinquencyVisit() {
        val layout = binding.delinquencyvisitLayout


        // 2️⃣ Show loading dialog after validation passes
        showLoadingDialog()

        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")?.toIntOrNull() ?: 0

        fun convertDateForServer(displayDate: String): String {
            return try {
                val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date: Date = displayFormat.parse(displayDate)!!
                serverFormat.format(date)
            } catch (e: Exception) {
                ""
            }
        }

        fun parseAmountForServer(amountText: String?): Double {
            return try {
                val cleaned = amountText?.replace(",", "") ?: "0"
                String.format("%.2f", cleaned.toDouble()).toDouble()
            } catch (e: Exception) {
                0.0
            }
        }

        val photoBase64 = photoFile?.let { encodeImageToBase64(it) } ?: ""

        val request = LoanVisitRequest(
            Staff_id = staffId,
            Image = photoBase64,
            CustomerNameAccount = layout.findViewById<TextInputEditText>(R.id.Custname)?.text?.toString() ?: "",
            LoanAccountNo = layout.findViewById<TextInputEditText>(R.id.AcNo)?.text?.toString() ?: "",
            LoanType = layout.findViewById<AutoCompleteTextView>(R.id.LoanType)?.text?.toString() ?: "",
            CustomerMobileNo = layout.findViewById<TextInputEditText>(R.id.CustMobNo)?.text?.toString() ?: "",
            LoanCategory = layout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)?.text?.toString() ?: "",
            LoanDisbursedDate = convertDateForServer(layout.findViewById<TextInputEditText>(R.id.LoanDisbDate)?.text?.toString() ?: ""),
            Case_101_91 = layout.findViewById<AutoCompleteTextView>(R.id.Case)?.text?.toString() ?: "",
            OverdueAmount = parseAmountForServer(layout.findViewById<TextInputEditText>(R.id.OverdueAmount)?.text?.toString()),
            Comments = layout.findViewById<TextInputEditText>(R.id.Comments)?.text?.toString() ?: "",
            PromiseToPay = layout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay)?.text?.toString() ?: "",
            PtpDate = convertDateForServer(layout.findViewById<TextInputEditText>(R.id.PTPDate)?.text?.toString() ?: ""),
            VisitTime = binding.etVisitTime.text?.toString() ?: "",
            OtherRemarks = layout.findViewById<TextInputEditText>(R.id.OtherRemarks)?.text?.toString() ?: "",
            Latitude = binding.etLat.text?.toString() ?: "",
            Longitude = binding.etLong.text?.toString() ?: "",
            BankName = binding.branchName.text?.toString() ?: "",
            BranchName = binding.branchCode.text?.toString() ?: "",
            VisitDate = convertDateForServer(binding.etDate.text?.toString() ?: "")
        )

        // 3️⃣ API call
        ApiClient.apiService.insertLoanVisit(request)
            .enqueue(object : Callback<LoanVisitResponse> {
                override fun onResponse(call: Call<LoanVisitResponse>, response: Response<LoanVisitResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@VisitApplicationActivity, "Success: ${response.body()!!.Message}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@VisitApplicationActivity, Dashboard::class.java))
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown API error"
                        Toast.makeText(this@VisitApplicationActivity, "API Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoanVisitResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    Toast.makeText(this@VisitApplicationActivity, "Network Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
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

    private fun encodeImageToBase64(file: File): String {
        // Decode bitmap
        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

        // Resize the image to a smaller width while keeping aspect ratio
        val maxWidth = 800
        val scale = maxWidth.toFloat() / originalBitmap.width
        val newHeight = (originalBitmap.height * scale).toInt()
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)

        // Compress bitmap to JPEG with lower quality (50%)
        val stream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        val byteArray = stream.toByteArray()

        // Optional: log the size for debugging
        Log.d("ImageCompression", "Compressed image size: ${byteArray.size / 1024} KB")

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }




    private fun setupLoanEnquiryDropdowns() {
        // Loan Type dropdown
        val loanTypeAdapter = ArrayAdapter(this, R.layout.dropdown_item, LoanTypeOptions)
        binding.loanEnquriyLayout.findViewById<AutoCompleteTextView>(R.id.LoanType).apply {
            setAdapter(loanTypeAdapter)
            setOnClickListener { showDropDown() }
        }

        // Eligibility dropdown
        val eligibilityAdapter = ArrayAdapter(this, R.layout.dropdown_item, eligibility)
        binding.loanEnquriyLayout.findViewById<AutoCompleteTextView>(R.id.Eligibility).apply {
            setAdapter(eligibilityAdapter)
            threshold = 0
            setOnClickListener { showDropDown() }
        }

        binding.loanEnquriyLayout.findViewById<TextInputEditText>(R.id.ExpireryDate).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker(this, restrictFutureDates = false)
            }
        }
    }


    private fun validateLoanRecoveryFields(): Boolean {
        val loanRecoveryLayout = binding.loanRecoveryLayout
        var isValid = true

        // Helper to validate EditText fields
        fun checkField(
            textInputLayout: TextInputLayout,
            field: TextInputEditText,
            message: String,
            extraValidation: ((TextInputLayout, String) -> Unit)? = null
        ) {
            val text = field.text?.toString().orEmpty()

            if (text.isBlank()) {
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = message
                isValid = false
            } else {
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
                extraValidation?.invoke(textInputLayout, text)
            }

            // Clear error when user types
            field.addTextChangedListener {
                if (!it.isNullOrBlank()) {
                    textInputLayout.isErrorEnabled = false
                    textInputLayout.error = null
                }
            }
        }

        // Helper to validate dropdowns (AutoCompleteTextView)
        fun checkDropdown(
            textInputLayout: TextInputLayout,
            field: AutoCompleteTextView,
            message: String
        ) {
            val text = field.text?.toString().orEmpty()

            if (text.isBlank()) {
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = message
                isValid = false
            } else {
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }

            // Clear error when an item is selected
            field.setOnItemClickListener { _, _, _, _ ->
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }
        }

        // ----- Validation calls for all fields -----
        checkField(
            loanRecoveryLayout.findViewById(R.id.CustnameLayout),
            loanRecoveryLayout.findViewById(R.id.Custname),
            "Customer name is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.CustBranchLayout),
            loanRecoveryLayout.findViewById(R.id.CustBranch),
            "Branch is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.AcNoLayout),
            loanRecoveryLayout.findViewById(R.id.AcNo),
            "Account number is required"
        )

        checkDropdown(
            loanRecoveryLayout.findViewById(R.id.LoanTypeLayout),
            loanRecoveryLayout.findViewById(R.id.LoanType),
            "Loan type is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.CustMobNoLayout),
            loanRecoveryLayout.findViewById(R.id.CustMobNo),
            "Mobile number is required"
        ) { layout, text ->
            if (!text.matches(Regex("\\d{10}"))) {
                layout.isErrorEnabled = true
                layout.error = "Enter a valid 10-digit mobile number"
                isValid = false
            }
        }

        checkDropdown(
            loanRecoveryLayout.findViewById(R.id.LoanCategoryLayout),
            loanRecoveryLayout.findViewById(R.id.LoanCategory),
            "Loan category is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.LoanDisbDateLayout),
            loanRecoveryLayout.findViewById(R.id.LoanDisbDate),
            "Disbursed date is required"
        )

        checkDropdown(
            loanRecoveryLayout.findViewById(R.id.CaseLayout),
            loanRecoveryLayout.findViewById(R.id.Case),
            "Case is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.OverdueAmountLayout),
            loanRecoveryLayout.findViewById(R.id.OverdueAmount),
            "Overdue amount is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.CommentsLayout),
            loanRecoveryLayout.findViewById(R.id.Comments),
            "Comments are required"
        )

        checkDropdown(
            loanRecoveryLayout.findViewById(R.id.PromisetoPayLayout),
            loanRecoveryLayout.findViewById(R.id.PromisetoPay),
            "Promise to pay is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.PTPDateLayout),
            loanRecoveryLayout.findViewById(R.id.PTPDate),
            "PTP date is required"
        )

        checkField(
            loanRecoveryLayout.findViewById(R.id.OtherRemarksLayout),
            loanRecoveryLayout.findViewById(R.id.OtherRemarks),
            "Other remarks are required"
        )

        return isValid
    }

    private fun validateLoanEnquiryFields(): Boolean {
        val loanEnquiryLayout = binding.loanEnquriyLayout
        var isValid = true

        fun checkField(textInputLayout: TextInputLayout, field: TextInputEditText, message: String) {
            // Remove error on text change
            field.addTextChangedListener {
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }

            if (field.text.isNullOrBlank()) {
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = message
                isValid = false
            } else {
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }
        }

        fun checkDropdown(textInputLayout: TextInputLayout, field: AutoCompleteTextView, message: String) {
            // Remove error on item select
            field.setOnItemClickListener { _, _, _, _ ->
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }

            if (field.text.isNullOrBlank()) {
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = message
                isValid = false
            } else {
                textInputLayout.isErrorEnabled = false
                textInputLayout.error = null
            }
        }

        checkDropdown(
            loanEnquiryLayout.findViewById(R.id.LoanTypeLayout),
            loanEnquiryLayout.findViewById(R.id.LoanType),
            "Loan type is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.AmountLayout),
            loanEnquiryLayout.findViewById(R.id.Amount),
            "Amount is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.CibilScoreLayout),
            loanEnquiryLayout.findViewById(R.id.cibilscore),
            "Cibil score is required"
        )

        checkDropdown(
            loanEnquiryLayout.findViewById(R.id.EligibilityLayout),
            loanEnquiryLayout.findViewById(R.id.Eligibility),
            "Eligibility is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.salaryLayout),
            loanEnquiryLayout.findViewById(R.id.salaryBussiness),
            "Salary/Business is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.PeriodLayout),
            loanEnquiryLayout.findViewById(R.id.Period),
            "Period is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.RateLayout),
            loanEnquiryLayout.findViewById(R.id.Rate),
            "Rate is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.InstallAmountLayout),
            loanEnquiryLayout.findViewById(R.id.InstallAmount),
            "Installment amount is required"
        )

        checkField(
            loanEnquiryLayout.findViewById(R.id.ExpireryDateLayout),
            loanEnquiryLayout.findViewById(R.id.ExpireryDate),
            "Expiry date is required"
        )

        return isValid
    }


    private fun clearLoanRecoveryErrors() {
        val layout = binding.loanRecoveryLayout
        val textInputLayouts = listOf(
            R.id.CustnameLayout, R.id.CustBranchLayout, R.id.AcNoLayout,
            R.id.LoanTypeLayout, R.id.CustMobNoLayout, R.id.LoanCategoryLayout,
            R.id.LoanDisbDateLayout, R.id.CaseLayout, R.id.OverdueAmountLayout,
            R.id.CommentsLayout, R.id.PromisetoPayLayout, R.id.PTPDateLayout,
            R.id.OtherRemarksLayout
        )
        textInputLayouts.forEach { id ->
            val til = layout.findViewById<TextInputLayout>(id)
            til.isErrorEnabled = false
            til.error = null
        }
    }

    private fun clearLoanEnquiryErrors() {
        val layout = binding.loanEnquriyLayout
        val textInputLayouts = listOf(
            R.id.LoanTypeLayout, R.id.AmountLayout, R.id.CibilScoreLayout,
            R.id.EligibilityLayout, R.id.salaryLayout, R.id.PeriodLayout,
            R.id.RateLayout, R.id.InstallAmountLayout, R.id.ExpireryDateLayout
        )
        textInputLayouts.forEach { id ->
            val til = layout.findViewById<TextInputLayout>(id)
            til.isErrorEnabled = false
            til.error = null
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

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }


    private fun showDatePicker(editText: TextInputEditText, restrictFutureDates: Boolean) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                editText.setText(sdf.format(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        if (restrictFutureDates) {
            datePicker.datePicker.maxDate = System.currentTimeMillis()
        }

        datePicker.show()
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
            // Process captured image with location
            fetchLocationAndOverlayImage()

            // Enable previously disabled fields after photo is stored
            enableFieldsAfterPhoto()
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




}


// This Code is For Generate Report Into Pdf And Excel Format


//binding.submit.setOnClickListener {
//    val options = arrayOf("PDF", "Excel")
//
//    // First, check if required fields are filled
//    fun isValid(): Boolean {
//        val requiredFields = listOf(
//            binding.branchName,
//            binding.branchCode,
//            binding.VisitTypeName,
//            binding.StaffName,
//            binding.etDate,
//            binding.etVisitTime,
//            binding.etLat,
//            binding.etLong,
//            binding.etAddress
//        )
//
//        for (field in requiredFields) {
//            if (field.text.toString().isBlank()) {
//                field.error = "Required"
//                field.requestFocus()
//                return false
//            }
//        }
//
//        //Check loan recovery fields if visible
//        if (binding.loanRecoveryLayout.visibility == View.VISIBLE) {
//            val loanLayout = binding.loanRecoveryLayout
//            val loanRequired = listOf(
//                loanLayout.findViewById<TextInputEditText>(R.id.Custname),
//                loanLayout.findViewById<TextInputEditText>(R.id.AcNo),
//                loanLayout.findViewById<TextInputEditText>(R.id.CustMobNo),
//            )
//            for (field in loanRequired) {
//                if (field?.text.toString().isBlank()) {
//                    field?.error = "Required"
//                    field?.requestFocus()
//                    return false
//                }
//            }
//        }
//
//        return true
//    }
//
//    if (!isValid()) {
//        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
//        return@setOnClickListener
//    }
//
//    val builder = AlertDialog.Builder(this)
//
//    // Create custom title
//    val titleTextView = TextView(this).apply {
//        text = "Select File Format"
//        setTextColor(Color.BLACK)
//        textSize = 18f
//        setPadding(50, 50, 50, 20)
//        setTypeface(null, Typeface.BOLD)
//    }
//    builder.setCustomTitle(titleTextView)
//
//    builder.setItems(options) { dialog, which ->
//        when (which) {
//            0 -> generateVisitAppPDF(this, binding)
//            1 -> generateVisitAppExcel(this, binding)
//        }
//        dialog.dismiss()
//    }
//
//    val dialog = builder.create()
//    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
//
//    dialog.setOnShowListener {
//        val listView = dialog.listView
//        if (listView != null) {
//            for (i in 0 until listView.count) {
//                val tv = listView.getChildAt(i) as? TextView
//                tv?.setTextColor(Color.BLACK)
//                tv?.textSize = 16f
//                tv?.setPadding(50, 20, 50, 20)
//            }
//        }
//    }
//
//    dialog.show()
//}




//@SuppressLint("DefaultLocale")
//fun generateVisitAppPDF(context: Context, binding: VisitApplicationActivityBinding) {
//    val pdfDocument = PdfDocument()
//    val paint = Paint()
//    val textPaint = Paint().apply {
//        textSize = 12f
//        color = Color.BLACK
//    }
//
//    val pageWidth = 595
//    val pageHeight = 842
//    val margin = 40
//    val labelWidth = 130
//    var y: Float = 60F
//    var pageNumber = 1
//
//    var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
//    var canvas = page.canvas
//
//    // ------------------- Helper Functions -------------------
//    fun newPage() {
//        pdfDocument.finishPage(page)
//        pageNumber++
//        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
//        canvas = page.canvas
//        y = 60F
//    }
//
//    fun checkSpace(extraSpace: Int = 0) {
//        if (y + extraSpace > pageHeight - margin) newPage()
//    }
//
//    fun drawHeader(title: String) {
//        checkSpace(37)
//        paint.color = Color.LTGRAY
//        canvas.drawRect(margin.toFloat(), y, (pageWidth - margin).toFloat(), (y + 25), paint)
//        paint.color = Color.BLACK
//        paint.isFakeBoldText = true
//        paint.textSize = 14f
//        canvas.drawText(title, (margin + 10).toFloat(), y + 18, paint)
//        y += 37
//    }
//
//    fun drawLine(label: String, value: String) {
//        val fieldStartX = margin + 8
//        paint.textSize = 12f
//        paint.isFakeBoldText = true
//        val colonX = fieldStartX + labelWidth
//        val maxWidth = pageWidth - margin - (colonX + 10)
//        val text = value.ifBlank { "N/A" }
//        val words = text.split(" ")
//        var line = ""
//        var lineY = y
//        val lines: MutableList<String> = mutableListOf()
//        for (word in words) {
//            val testLine = if (line.isEmpty()) word else "$line $word"
//            if (paint.measureText(testLine) > maxWidth) {
//                lines.add(line)
//                line = word
//            } else {
//                line = testLine
//            }
//        }
//        lines.add(line)
//
//        checkSpace(lines.size * 18 + 10)
//
//        canvas.drawText(label, fieldStartX.toFloat(), y, paint)
//        paint.isFakeBoldText = false
//        canvas.drawText(":", colonX.toFloat(), y, paint)
//
//        var textY = y
//        for (l in lines) {
//            canvas.drawText(l, (colonX + 10).toFloat(), textY, paint)
//            textY += 18f
//        }
//        y = textY + 10
//    }
//
//    fun drawMultilineText(text: String) {
//        val lines = text.split("\n")
//        for (line in lines) {
//            checkSpace(20)
//            canvas.drawText(line, margin.toFloat(), y, textPaint)
//            y += 20
//        }
//        y += 10
//    }
//
//    fun formatIndianAmount(amount: String): String {
//        return try {
//            val num = amount.replace(",", "").toDouble()
//            val formatter = java.text.NumberFormat.getInstance(Locale("en", "IN"))
//            formatter.maximumFractionDigits = 2
//            formatter.format(num)
//        } catch (e: Exception) { amount }
//    }
//
//    // ---------------------- PDF HEADER ----------------------
//    paint.textSize = 18f
//    paint.isFakeBoldText = true
//    val companyName = "AVS in-so-Tech Private Limited"
//    canvas.drawText(companyName, (pageWidth - paint.measureText(companyName)) / 2, y, paint)
//    y += 25
//
//    paint.textSize = 12f
//    paint.isFakeBoldText = false
//    val addressLines = listOf(
//        "Railway Station, 409, 4th Floor A1 Wing, Rupa Soliatre, MBP, near Ghansoli, Millenium",
//        "Business Park, Sector 2, Kopar Khairane, Navi Mumbai, Maharashtra 400701,",
//        "Phone: 099698 36021"
//    )
//    for (line in addressLines) {
//        canvas.drawText(line, (pageWidth - paint.measureText(line)) / 2, y, paint)
//        y += 20
//    }
//    y -= 5
//    canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, paint)
//    y += 20
//
//    paint.isFakeBoldText = true
//    val reportTitle = "-: Loan Recovery Visit Report :-"
//    canvas.drawText(reportTitle, (pageWidth - paint.measureText(reportTitle)) / 2, y, paint)
//    y += 25
//
//    // ---------------------- PERSONAL INFORMATION ----------------------
//    drawHeader("PERSONAL INFORMATION")
//    y += 10
//
//    val photoBitmap = (binding.profilePhoto.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
//    val photoSize = 100
//    val photoX = pageWidth - margin - photoSize
//    val photoY = y
//    var photoBottom = y
//    if (photoBitmap != null) {
//        val scaled = Bitmap.createScaledBitmap(photoBitmap, photoSize, photoSize, false)
//        canvas.drawBitmap(scaled, photoX.toFloat(), photoY, paint)
//        photoBottom = photoY + photoSize + 10
//    }
//
//    drawLine("Branch Name", binding.branchName.text.toString())
//    drawLine("Branch Code", binding.branchCode.text.toString())
//    drawLine("Visit Type", binding.VisitTypeName.text.toString())
//    drawLine("Staff Name", binding.StaffName.text.toString())
//    drawLine("Visit Date", binding.etDate.text.toString())
//    drawLine("Visit Time", binding.etVisitTime.text.toString())
//    drawLine("Latitude", binding.etLat.text.toString())
//    drawLine("Longitude", binding.etLong.text.toString())
//    drawLine("Address", binding.etAddress.text.toString())
//
//    y = maxOf(y, photoBottom) + 10
//
//    // ---------------------- LOAN RECOVERY ----------------------
//    if (binding.loanRecoveryLayout.visibility == View.VISIBLE) {
//        drawHeader("LOAN RECOVERY DETAILS")
//        y += 10
//
//        val loanLayout = binding.loanRecoveryLayout
//
//        val loanCategory = loanLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)?.text?.toString().orEmpty()
//        val caseValue = loanLayout.findViewById<AutoCompleteTextView>(R.id.Case)?.text?.toString().orEmpty()
//        val promiseToPay = loanLayout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay)?.text?.toString().orEmpty()
//        val loanType = loanLayout.findViewById<AutoCompleteTextView>(R.id.LoanType)?.text?.toString().orEmpty()
//
//        val custName = loanLayout.findViewById<TextInputEditText>(R.id.Custname)?.text?.toString().orEmpty()
//        val acNo = loanLayout.findViewById<TextInputEditText>(R.id.AcNo)?.text?.toString().orEmpty()
//        val custMobNo = loanLayout.findViewById<TextInputEditText>(R.id.CustMobNo)?.text?.toString().orEmpty()
//        val loanDisbDate = loanLayout.findViewById<TextInputEditText>(R.id.LoanDisbDate)?.text?.toString().orEmpty()
//        val ptpDate = loanLayout.findViewById<TextInputEditText>(R.id.PTPDate)?.text?.toString().orEmpty()
//        val CustBranch = loanLayout.findViewById<TextInputEditText>(R.id.CustBranch)?.text?.toString().orEmpty()
//        val comments = loanLayout.findViewById<TextInputEditText>(R.id.Comments)?.text?.toString().orEmpty()
//        val otherRemarks = loanLayout.findViewById<TextInputEditText>(R.id.OtherRemarks)?.text?.toString().orEmpty()
//        val overdueAmount = loanLayout.findViewById<TextInputEditText>(R.id.OverdueAmount)?.text?.toString().orEmpty()
//
//        drawLine("Customer Name", custName)
//        drawLine("Branch Of Customer", CustBranch)
//        drawLine("A/c No", acNo)
//        drawLine("Loan Type", loanType)
//        drawLine("Loan Category", loanCategory)
//        drawLine("Customer Mobile No", custMobNo)
//        drawLine("Loan Disbursed Date", loanDisbDate)
//        drawLine("Overdue Amount", "₹ ${formatIndianAmount(overdueAmount)}")
//        drawLine("Comments", comments)
//        drawLine("Case 101/91", caseValue)
//        drawLine("Promise To Pay", promiseToPay)
//        drawLine("PTP Date", ptpDate)
//        drawLine("Other Remarks", otherRemarks)
//    }
//
//  // ---------------------- RECOMMENDATION ----------------------
//
//    y += 10
//
//    drawMultilineText("""
//        Based on the physical visit, the purpose of the loan appears to be Valid / Invalid.
//        Considering the net surplus monthly income of INR ___________________, the recommendation is to
//        Approve / Reject the loan amount of INR _______________.
//    """.trimIndent())
//
//    drawLine("Name Of The Officer/Clerk", "_________________________")
//    drawLine("Signature Of Officer/Clerk", "_________________________")
//
//    pdfDocument.finishPage(page)
//
//    // ---------------------- SAVE PDF ----------------------
//    try {
//        val fileName = "Visit_Report_${System.currentTimeMillis()}.pdf"
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val pdfFile = File(downloadsDir, fileName)
//        if (!downloadsDir.exists()) downloadsDir.mkdirs()
//        FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
//        pdfDocument.close()
//
//        MediaScannerConnection.scanFile(context, arrayOf(pdfFile.absolutePath), arrayOf("application/pdf")) { _, _ -> }
//
//        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
//    } catch (e: Exception) {
//        e.printStackTrace()
//        Toast.makeText(context, "PDF generation failed: ${e.message}", Toast.LENGTH_LONG).show()
//        pdfDocument.close()
//    }
//}
//
//
//@SuppressLint("DefaultLocale")
//fun generateVisitAppExcel(context: Context, binding: VisitApplicationActivityBinding) {
//    try {
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        if (!downloadsDir.exists()) downloadsDir.mkdirs()
//        val file = File(downloadsDir, "Visit_Report_${System.currentTimeMillis()}.xls")
//
//        val workbook = jxl.Workbook.createWorkbook(file)
//        val sheet = workbook.createSheet("Loan Visit Report", 0)
//
//        // -------------------- Styles --------------------
//        val headerFont = WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD)
//        val headerFormat = WritableCellFormat(headerFont).apply {
//            alignment = Alignment.CENTRE
//            verticalAlignment = VerticalAlignment.CENTRE
//            setBorder(Border.ALL, BorderLineStyle.THIN)
//        }
//
//        val labelFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)
//        val labelFormat = WritableCellFormat(labelFont).apply {
//            alignment = Alignment.LEFT
//            verticalAlignment = VerticalAlignment.CENTRE
//            setBorder(Border.ALL, BorderLineStyle.THIN)
//        }
//
//        val valueFont = WritableFont(WritableFont.ARIAL, 10)
//        val valueFormat = WritableCellFormat(valueFont).apply {
//            alignment = Alignment.LEFT
//            verticalAlignment = VerticalAlignment.CENTRE
//            setBorder(Border.ALL, BorderLineStyle.THIN)
//        }
//
//
//        // -------------------- Helper Functions --------------------
//        fun addRow(rowNum: Int, label: String, value: String) {
//            val safeValue = if (value.isBlank()) "N/A" else value
//            sheet.addCell(jxl.write.Label(0, rowNum, label, labelFormat))
//            sheet.addCell(jxl.write.Label(1, rowNum, safeValue, valueFormat))
//        }
//
//        fun addSectionTitle(rowNum: Int, title: String) {
//            val sectionCell = jxl.write.Label(0, rowNum, title, headerFormat)
//            sheet.addCell(sectionCell)
//            // Merge title across two columns
//            sheet.mergeCells(0, rowNum, 1, rowNum)
//        }
//
//        // -------------------- Start Writing --------------------
//        var row = 0
//
//        // Document Title
//        addSectionTitle(row++, "LOAN RECOVERY VISIT REPORT")
//        row++
//
//        // Personal Information Section
//        addSectionTitle(row++, "PERSONAL INFORMATION")
//        addRow(row++, "Branch Name", binding.branchName.text.toString())
//        addRow(row++, "Branch Code", binding.branchCode.text.toString())
//        addRow(row++, "Visit Type", binding.VisitTypeName.text.toString())
//        addRow(row++, "Staff Name", binding.StaffName.text.toString())
//        addRow(row++, "Visit Date", binding.etDate.text.toString())
//        addRow(row++, "Visit Time", binding.etVisitTime.text.toString())
//        addRow(row++, "Latitude", binding.etLat.text.toString())
//        addRow(row++, "Longitude", binding.etLong.text.toString())
//        addRow(row++, "Address", binding.etAddress.text.toString())
//        row++
//
//        // Loan Recovery Section
//        if (binding.loanRecoveryLayout.visibility == View.VISIBLE) {
//            val loanLayout = binding.loanRecoveryLayout
//            addSectionTitle(row++, "LOAN RECOVERY DETAILS")
//            addRow(row++, "Customer Name", loanLayout.findViewById<TextInputEditText>(R.id.Custname)?.text?.toString().orEmpty())
//            addRow(row++, "Branch Of Customer", loanLayout.findViewById<TextInputEditText>(R.id.CustBranch)?.text?.toString().orEmpty())
//            addRow(row++, "A/c No", loanLayout.findViewById<TextInputEditText>(R.id.AcNo)?.text?.toString().orEmpty())
//            addRow(row++, "Loan Type", loanLayout.findViewById<AutoCompleteTextView>(R.id.LoanType)?.text?.toString().orEmpty())
//            addRow(row++, "Loan Category", loanLayout.findViewById<AutoCompleteTextView>(R.id.LoanCategory)?.text?.toString().orEmpty())
//            addRow(row++, "Customer Mobile No", loanLayout.findViewById<TextInputEditText>(R.id.CustMobNo)?.text?.toString().orEmpty())
//            addRow(row++, "Loan Disbursed Date", loanLayout.findViewById<TextInputEditText>(R.id.LoanDisbDate)?.text?.toString().orEmpty())
//            addRow(row++, "Overdue Amount", loanLayout.findViewById<TextInputEditText>(R.id.OverdueAmount)?.text?.toString().orEmpty())
//            addRow(row++, "Comments", loanLayout.findViewById<TextInputEditText>(R.id.Comments)?.text?.toString().orEmpty())
//            addRow(row++, "Case 101/91", loanLayout.findViewById<AutoCompleteTextView>(R.id.Case)?.text?.toString().orEmpty())
//            addRow(row++, "Promise To Pay", loanLayout.findViewById<AutoCompleteTextView>(R.id.PromisetoPay)?.text?.toString().orEmpty())
//            addRow(row++, "PTP Date", loanLayout.findViewById<TextInputEditText>(R.id.PTPDate)?.text?.toString().orEmpty())
//            addRow(row++, "Other Remarks", loanLayout.findViewById<TextInputEditText>(R.id.OtherRemarks)?.text?.toString().orEmpty())
//        }
//
//        // Recommendation Section
//        row++
//        addSectionTitle(row++, "RECOMMENDATION")
//        addRow(row++, "Recommendation Details", "Based on the physical visit, the purpose of the loan appears Valid/Invalid. Approve/Reject based on net surplus income.")
//        addRow(row++, "Officer Name", "_________________________")
//        addRow(row++, "Officer Signature", "_________________________")
//
//        // -------------------- Autosize Columns --------------------
//        sheet.setColumnView(0, 25)
//        sheet.setColumnView(1, 50)
//
//        // Write and close workbook
//        workbook.write()
//        workbook.close()
//
//        // Refresh media library
//        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("application/vnd.ms-excel")) { path, uri -> }
//
//        Toast.makeText(context, "Excel saved to Downloads", Toast.LENGTH_SHORT).show()
//    } catch (e: Exception) {
//        e.printStackTrace()
//        Toast.makeText(context, "Excel generation failed: ${e.message}", Toast.LENGTH_LONG).show()
//    }
//}
