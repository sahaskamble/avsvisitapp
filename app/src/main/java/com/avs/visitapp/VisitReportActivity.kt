package com.avs.visitapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.visitapp.Adapter.VisitAdapter
import com.avs.visitapp.Adapter.VisitReportAdapter
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.VisitReportActivityBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class VisitReportActivity : AppCompatActivity() {

    private lateinit var binding: VisitReportActivityBinding
    private lateinit var adapter: VisitReportAdapter
    private var loadingDialog: AlertDialog? = null
    private var fromDate: String? = null
    private var toDate: String? = null
    private var isFromDate = true
    private var fullVisitList: List<VisitData> = listOf()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VisitReportActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }



        binding.visitRecyclerView.layoutManager = LinearLayoutManager(this)

//        fetchVisitRecords()

        binding.btnFrom.setOnClickListener {
            isFromDate = true
            showDatePickerDialog()
        }

        binding.btnTo.setOnClickListener {
            isFromDate = false
            showDatePickerDialog()
        }

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
        loadingDialog?.dismiss()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchVisitRecords() {
        val emp_id = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "")

        lifecycleScope.launch {
            showLoadingDialog()
            try {
                val response = ApiClient.apiService.getVisitsByEmployeeId(id = emp_id)
                if (response.isSuccessful && response.body() != null) {

                    // ✅ Reverse the list to show latest visits first
                    fullVisitList = response.body()!!.data.reversed()


                    val nullVisitCount = fullVisitList.count { it.Visit == null }

                    showTodayVisits()
                } else {
                    Toast.makeText(this@VisitReportActivity, "No data found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {

                e.printStackTrace()
                Toast.makeText(this@VisitReportActivity, "Failed to fetch visits", Toast.LENGTH_SHORT).show()
            } finally {
                dismissLoadingDialog()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTodayVisits() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todayList = fullVisitList.filter {
            val date = it.Visit?.VisitDate?.substring(0, 10)
            Log.d("VisitFilter", "Checking date: $date vs $today")
            date == today
        }

        adapter = VisitReportAdapter(todayList) { visitData ->
            showVisitDetailsDialog(visitData)
        }
        binding.visitRecyclerView.adapter = adapter
    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun showAllVisits() {
//        adapter = VisitReportAdapter(fullVisitList) { visitData ->
//            showVisitDetailsDialog(visitData)
//        }
//        binding.visitRecyclerView.adapter = adapter
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun showVisitsByDate(date: String) {
//        val filteredList = fullVisitList.filter {
//            // Safely extract just the date part (first 10 chars)
//            val visitDate = it.Visit?.VisitDate?.substring(0, 10)
//            visitDate == date
//        }
//
//        adapter = VisitReportAdapter(filteredList) { visitData ->
//            showVisitDetailsDialog(visitData)
//        }
//        binding.visitRecyclerView.adapter = adapter
//    }




    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Internally store in yyyy-MM-dd (for filtering)
                val internalDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                // Display in dd-MM-yyyy
                val displayDate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)

                if (isFromDate) {
                    fromDate = internalDate
                    binding.btnFrom.text = displayDate
                } else {
                    toDate = internalDate
                    binding.btnTo.text = displayDate
                }
                filterVisitsByDateRange()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterVisitsByDateRange() {
        if (fromDate.isNullOrEmpty() || toDate.isNullOrEmpty()) return

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        val filteredList = fullVisitList.filter {
            val visitDateStr = it.Visit?.VisitDate?.substring(0, 10)
            visitDateStr != null && run {
                val visitDate = LocalDate.parse(visitDateStr, formatter)
                (visitDate >= from && visitDate <= to)
            }
        }

        adapter = VisitReportAdapter(filteredList) { visitData ->
            showVisitDetailsDialog(visitData)
        }
        binding.visitRecyclerView.adapter = adapter
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    private fun showVisitDetailsDialog(data: VisitData) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_visit_details, null)
        val buttonView = inflater.inflate(R.layout.dialog_visit_buttons, null)

        val formattedAppDate = formatDate(data.Applicant.ApplicationDate)
        val formattedVisitDate = formatDate(data.Visit?.VisitDate)
        val formattedVisitTime = formatTime(data.Visit?.VisitTime)

        dialogView.findViewById<TextView>(R.id.tvName).text = data.Applicant.Name
        dialogView.findViewById<TextView>(R.id.tvMobile).text = data.OccupationDetails.MobileNo
        dialogView.findViewById<TextView>(R.id.tvAppDate).text = formattedAppDate
        dialogView.findViewById<TextView>(R.id.tvLoanPurpose).text = data.Applicant.LoanPurposeType
        dialogView.findViewById<TextView>(R.id.tvRequestedAmount).text = data.Applicant.RequestedAmount.toString()
        dialogView.findViewById<TextView>(R.id.tvSerialNo).text = data.Applicant.ApplicationSerialNo

        dialogView.findViewById<TextView>(R.id.tvBankName).text = data.BankDetails.BankName
        dialogView.findViewById<TextView>(R.id.tvBranchName).text = data.BankDetails.BranchName
        dialogView.findViewById<TextView>(R.id.tvAccountNo).text = data.BankDetails.AccountNumber

        dialogView.findViewById<TextView>(R.id.tvFamilyIncome).text = "₹ ${data.FamilyIncome.TotalIncome}"
        dialogView.findViewById<TextView>(R.id.tvMonthlyIncome).text = "₹ ${data.OccupationDetails.MonthlyIncome}"
        dialogView.findViewById<TextView>(R.id.tvAgriLand).text = data.OccupationDetails.AgriculturalLand

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
                dialog.dismiss()
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun formatDate(dateString: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString ?: "")
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString ?: "N/A"
        }
    }

    private fun formatTime(timeString: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val time = inputFormat.parse(timeString ?: "")
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            outputFormat.format(time ?: Date())
        } catch (e: Exception) {
            timeString ?: "N/A"
        }
    }


}
