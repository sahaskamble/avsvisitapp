package com.avs.visitapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.visitapp.Adapter.VisitAdapter
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.VisitRecordActivityBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class VisitRecordActivity : AppCompatActivity() {

    private lateinit var binding: VisitRecordActivityBinding
    private var loadingDialog: AlertDialog? = null

    private val fromCalendar = Calendar.getInstance()
    private val toCalendar = Calendar.getInstance()

    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var fromDateApi: String? = null
    private var toDateApi: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VisitRecordActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.visitRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.etFromDate.setOnClickListener { showDatePicker(isFrom = true) }
//        binding.etToDate.setOnClickListener { showDatePicker(isFrom = false) }

        binding.etMobile.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString()
                if (mobile.length == 10 && mobile.all { it.isDigit() }) {
                    binding.etMobile.setTextColor(ContextCompat.getColor(this@VisitRecordActivity, android.R.color.black))
                    binding.tilMobile.error = null
                } else {
                    binding.etMobile.setTextColor(ContextCompat.getColor(this@VisitRecordActivity, android.R.color.holo_red_dark))
                    binding.tilMobile.error = "Enter valid 10-digit mobile number"
                }
            }
        })

        binding.btnSubmit.setOnClickListener {
            val mobile = binding.etMobile.text.toString().trim()
            val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")

            when {
                mobile.isEmpty() -> {
                    Toast.makeText(this, "Enter mobile number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                fromDateApi.isNullOrEmpty() -> {
                    Toast.makeText(this, "Select From Date", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val request = VisitRecordRequest(
                EmployeeId = empId,
                FromDate = fromDateApi!!,
//                ToDate = toDateApi,
                MobileNo = mobile
            )

            fetchVisitData(request)
        }
    }

    private fun showDatePicker(isFrom: Boolean) {
        val calendar = if (isFrom) fromCalendar else toCalendar

        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)

            val formattedDisplayDate = displayDateFormat.format(calendar.time)
            val formattedApiDate = apiDateFormat.format(calendar.time)

            if (isFrom) {
                binding.etFromDate.setText(formattedDisplayDate)
                fromDateApi = formattedApiDate
            }
//            else {
//                binding.etToDate.setText(formattedDisplayDate)
//                toDateApi = formattedApiDate
//            }
        }

        DatePickerDialog(
            this, listener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchVisitData(request: VisitRecordRequest) {
        showLoadingDialog()

        ApiClient.apiService.getVisitsByEmployeeId(request)
            .enqueue(object : Callback<VisitRetrieveResponse> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<VisitRetrieveResponse>,
                    response: Response<VisitRetrieveResponse>
                ) {
                    dismissLoadingDialog()
                    val visitList = response.body()?.data
                    if (response.isSuccessful && visitList?.isNotEmpty() == true) {
                        val reversedList = visitList.reversed()
                        val adapter = VisitAdapter(reversedList) { selectedVisit ->
                            showVisitDetailsDialog(selectedVisit)
                        }
                        binding.visitRecyclerView.adapter = adapter
                        binding.filterLayout.visibility = View.GONE
                    } else {
                        Toast.makeText(this@VisitRecordActivity, "No visit records found", Toast.LENGTH_SHORT).show()
                        binding.visitRecyclerView.adapter = null
                    }
                }

                override fun onFailure(call: Call<VisitRetrieveResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    val errorMsg = if (t.message?.contains("Unable to resolve host") == true)
                        "No internet connection"
                    else
                        "Error: ${t.localizedMessage}"
                    Toast.makeText(this@VisitRecordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(dateStr: String?): String {
        return try {
            val date = LocalDate.parse(dateStr?.substring(0, 10))
            date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        } catch (e: Exception) {
            "N/A"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatTime(timeStr: String?): String {
        return try {
            val time = LocalTime.parse(timeStr)
            time.format(DateTimeFormatter.ofPattern("hh:mm:ss a"))
        } catch (e: Exception) {
            "N/A"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    private fun showVisitDetailsDialog(data: VisitData) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_visit_details, null)
        val buttonView = inflater.inflate(R.layout.dialog_visit_buttons, null)

        val formattedAppDate = formatDate(data.Applicant?.ApplicationDate)
        val formattedVisitDate = formatDate(data.Visit?.VisitDate)
        val formattedVisitTime = formatTime(data.Visit?.VisitTime)

        dialogView.findViewById<TextView>(R.id.tvName).text = data.Applicant?.Name ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvMobile).text = data.OccupationDetails?.MobileNo ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvAppDate).text = formattedAppDate
        dialogView.findViewById<TextView>(R.id.tvLoanPurpose).text = data.Applicant?.LoanPurposeType ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvRequestedAmount).text = data.Applicant?.RequestedAmount?.toString() ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvSerialNo).text = data.Applicant?.ApplicationSerialNo ?: "N/A"

        dialogView.findViewById<TextView>(R.id.tvBankName).text = data.BankDetails?.BankName ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvBranchName).text = data.BankDetails?.BranchName ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvAccountNo).text = data.BankDetails?.AccountNumber ?: "N/A"

        dialogView.findViewById<TextView>(R.id.tvFamilyIncome).text = "₹ ${data.FamilyIncome?.TotalIncome ?: "0"}"
        dialogView.findViewById<TextView>(R.id.tvMonthlyIncome).text = "₹ ${data.OccupationDetails?.MonthlyIncome ?: "0"}"
        dialogView.findViewById<TextView>(R.id.tvAgriLand).text = data.OccupationDetails?.AgriculturalLand ?: "N/A"

        dialogView.findViewById<TextView>(R.id.tvVisitDate).text = formattedVisitDate
        dialogView.findViewById<TextView>(R.id.tvVisitTime).text = formattedVisitTime
        dialogView.findViewById<TextView>(R.id.tvPersonName).text = data.Visit?.PresentPersonName ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvVisitMobile).text = data.Visit?.VMobileNo ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvAddress).text = data.Visit?.ResidentialAddress ?: "N/A"

        val customTitle = TextView(this).apply {
            text = "Visit Details"
            setPadding(40, 30, 40, 30)
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4fc1c2"))
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(dialogView)
            addView(buttonView)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(contentLayout)
        }

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(customTitle)
            .setView(scrollView)
            .create()

        dialog.setOnShowListener {
            val btnDownload = buttonView.findViewById<Button>(R.id.btnDownload)
            val btnClose = buttonView.findViewById<Button>(R.id.btnClose)

            btnDownload.setOnClickListener {
//                generateVisitPDF(this@VisitRecordActivity, data)
                dialog.dismiss()
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showLoadingDialog() {
        val dialogBinding = DialogLoadingBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        dialogBinding.tvLoading.text = "Loading visit records..."
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
