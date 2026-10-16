package com.avs.visitapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.avs.visitapp.databinding.ActivityVisitAgainUpdateBinding
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import java.util.*

class VisitAgainUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitAgainUpdateBinding
    private lateinit var apiService: ApiService
    private var visitData: VisitAgainModel? = null

    private var latitude = ""
    private var longitude = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitAgainUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = ApiClient.apiService

        visitData = intent.getSerializableExtra("visitAgainData") as? VisitAgainModel
        if (visitData == null) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

//        setupToolbar()
        prefillData()
        setupListeners()
//        getLocation()
    }

//    private fun setupToolbar() {
//        binding.topAppBar.setNavigationOnClickListener { finish() }
//    }

    private fun prefillData() {
        val v = visitData!!

        // Auto-filled (read-only)
        binding.Custname.setText(v.CustomerName)
        binding.LoanAcNo.setText(v.LoanAccNo)
        binding.CustMobNo.setText(v.CustMobileNo)
        binding.Branch.setText(v.BranchName)
        binding.OverdueAmount.setText(v.ODAmt)
        binding.Case101.setText(v.Loan101_91 ?: "")
        binding.PromiseToPay.setText(v.PromiseToPay ?: "")
        binding.VisitPlace.setText(v.VisitedPlaceName ?: "")
        binding.OtherRemarks.setText(v.OtherRemarks ?: "")

        // Editable fields only
        binding.Comments.setText(v.Comments ?: "")
        binding.PTPDate.setText(v.PTPDate ?: "")

        // Disable all non-editable fields
        binding.Custname.isEnabled = false
        binding.LoanAcNo.isEnabled = false
        binding.CustMobNo.isEnabled = false
        binding.Branch.isEnabled = false
        binding.OverdueAmount.isEnabled = false
        binding.Case101.isEnabled = false
        binding.PromiseToPay.isEnabled = false
        binding.VisitPlace.isEnabled = false
        binding.OtherRemarks.isEnabled = false
    }

    private fun setupListeners() {
        binding.PTPDate.setOnClickListener { openDatePicker() }

        binding.btnUpdate.setOnClickListener {
            submitUpdateVisitAgain()
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val dp = DatePickerDialog(
            this,
            { _, year, month, day ->
                val formatted = "$year-${month + 1}-$day"
                binding.PTPDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dp.show()
    }

//    private fun getLocation() {
//        val fused = LocationServices.getFusedLocationProviderClient(this)
//        fused.lastLocation.addOnSuccessListener {
//            if (it != null) {
//                latitude = it.latitude.toString()
//                longitude = it.longitude.toString()
//            }
//        }
//    }

//    private fun submitUpdateVisitAgain() {
//
//        val comments = binding.Comments.text.toString().trim()
//        val ptpDate = binding.PTPDate.text.toString().trim()
//
//        if (comments.isEmpty()) {
//            Toast.makeText(this, "Please enter comments", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val v = visitData!!
//        val empNo = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//
//
//        // API request body
//        val request = VisitAgainUpdateRequest(
//            AccountNo = v.CustMobileNo ?: "",
//            LoanAccountNo = v.LoanAccNo ?: "",
//            BranchCode = v.BRCD.toString(),
//            StaffID = empNo,
//            LoanType = "DQ",
//            Comments = comments,
//            PTPDate = if (ptpDate.isEmpty()) null else ptpDate,
//            Latitude = latitude.ifEmpty { "0" },
//            Longitude = longitude.ifEmpty { "0" }
//
//        )
//
//        apiService.updateVisitAgain(request).enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                if (response.isSuccessful) {
//                    Toast.makeText(
//                        this@VisitAgainUpdateActivity,
//                        "Visit Again Updated Successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    setResult(RESULT_OK)
//                    finish()
//                } else {
//                    Toast.makeText(
//                        this@VisitAgainUpdateActivity,
//                        "Server Error: ${response.code()}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Toast.makeText(
//                    this@VisitAgainUpdateActivity,
//                    "Failed: ${t.localizedMessage}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        })
//    }

    private fun submitUpdateVisitAgain() {



        val comments = binding.Comments.text.toString().trim()
        val ptpDate = binding.PTPDate.text.toString().trim()

        if (comments.isEmpty()) {
            Toast.makeText(this, "Add comments", Toast.LENGTH_SHORT).show()
            return
        }

        val empNo = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
        val v = visitData!!

        val request = VisitAgainUpdateRequest(
            AccountNo = v.LoanAccNo ?: "",
            LoanAccountNo = v.LoanAccNo ?: "",
            BranchCode = v.BRCD.toString(),
            StaffID = 0,
            LoanType = v.ProdctCD ?: "",
            Comments = comments,
//            PTPDate = if (ptpDate.isEmpty()) null else ptpDate,   // FIX
            PTPDate = v.PTPDate ?: "",
            Latitude = if (latitude.isEmpty()) "0" else latitude, // FIX


            Longitude = if (longitude.isEmpty()) "0" else longitude // FIX
        )

        apiService.updateVisitAgain(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VisitAgainUpdateActivity, "Visit Again Updated", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@VisitAgainUpdateActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@VisitAgainUpdateActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

}
