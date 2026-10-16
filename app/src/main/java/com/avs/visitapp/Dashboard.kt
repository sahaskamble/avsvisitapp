package com.avs.visitapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.app.AlertDialog
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.avs.visitapp.databinding.DashboardActivityBinding

class Dashboard : AppCompatActivity() {

    // Data model
    data class DashboardItem(
        val iconRes: Int,
        val label: String,
        val targetActivity: Class<out AppCompatActivity>
    )

    private lateinit var binding: DashboardActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get full name from SharedPreferences
        val empName = Constant.getSharedPreference(this, Constant.SHARE_EMP_NAME, "User") ?: "User"
        val nameParts = empName.trim().split(" ")
        val firstName = nameParts.firstOrNull() ?: ""
        val lastName = if (nameParts.size > 1) nameParts.lastOrNull() ?: "" else ""

        binding.tvName.text = "Welcome, $firstName $lastName"

        val dashboardItems = listOf(
            DashboardItem(R.drawable.ic_emp_profile, "EMPLOYEE\nPROFILE", ProfileActivity::class.java),
            DashboardItem(R.drawable.ic_attendance, "DAILY\nATTENDANCE", AttendanceActivity::class.java),
            DashboardItem(R.drawable.video, "TRAINING\nVIDEOS", TrainingVideoActivity::class.java),
            DashboardItem(R.drawable.ic_leave_management, "LEAVE\nMANAGEMENT", LeaveActivity::class.java),
            DashboardItem(R.drawable.ic_visit, "VISITS", VisitActivity::class.java),
            DashboardItem(R.drawable.ic_notice, "NOTICE\nANNOUNCEMENT", NoticeActivity::class.java),
            DashboardItem(R.drawable.ic_report, "REPORTS", ReportsActivity::class.java),
            DashboardItem(R.drawable.ic_payslip, "Salary\nSLIP", PayslipActivity::class.java),
            DashboardItem(R.drawable.ic_helpdesk, "HELPDESK", HelpdeskActivity::class.java)
        )

        binding.dashboardRecycler.layoutManager = GridLayoutManager(this, 3)
//        binding.dashboardRecycler.adapter = DashboardAdapter(dashboardItems) { item, _ ->
//            /*when (item.targetActivity) {
//                VisitActivity::class.java,
//                AttendanceActivity::class.java , ProfileActivity::class.java
//                    -> {
//                    // Allow only VisitActivity or AttendanceActivity
//                    startActivity(Intent(this, item.targetActivity))
//                }
//                else -> {
//                    // Show licensed copy message
//                    showCustomToast("Use Licensed Copy")
//
//                }
//            }*/
//            item.targetActivity?.let {
//                startActivity(Intent(this, it))
//            }
//        }

        binding.dashboardRecycler.adapter = DashboardAdapter(dashboardItems) { item, position ->

            // Allow only item 2 (index 1) and item 5 (index 4)
            if (position == 1 || position == 4) {
                startActivity(Intent(this, item.targetActivity))
            } else {
                showLockedPopup()   // Show popup for all other options
            }
        }



        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

//        binding.btnNotification.setOnClickListener {
//            val intent = Intent(this@Dashboard, NoticeActivity::class.java)
//            startActivity(intent)
//        }

    }

    private fun showLockedPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Feature Locked")
        builder.setMessage("This feature is locked.\nPlease buy a license to access it.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.setOnShowListener {

            // White background
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            // Set text color to black
            val textColor = Color.BLACK
            val buttonColor = Color.parseColor("#1A5D88") // blue button text

            // Message color
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)

            // Title color
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<TextView>(titleId)?.setTextColor(textColor)

            // Button colors
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(buttonColor)
        }

        dialog.show()
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


    private fun showLogoutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout and exit?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            val prefs = getSharedPreferences(Constant.SHARE_PREF_NAME, MODE_PRIVATE)
            Log.d("PrefsBeforeClear", prefs.all.toString())

            prefs.edit().clear().commit()

            Log.d("PrefsAfterClear", prefs.all.toString())

            Handler(Looper.getMainLooper()).postDelayed({
                finishAffinity()
                System.exit(0)
            }, 500)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            val buttonColor = Color.parseColor("#1A5D88") // Button text color
            val textColor = Color.BLACK // Title and message text color

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(buttonColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(buttonColor)
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)

            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<TextView>(titleId)?.setTextColor(textColor)
        }

        dialog.show()
    }

    override fun onBackPressed() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Exit")
        builder.setMessage("Are you sure you want to exit the app?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            finishAffinity()
            System.exit(0)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            val buttonColor = Color.parseColor("#1A5D88") // Button text color
            val textColor = Color.BLACK // Title and message text color

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(buttonColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(buttonColor)
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)

            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<TextView>(titleId)?.setTextColor(textColor)
        }

        dialog.show()
    }


}
