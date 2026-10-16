////BNSPF_VISIT
package com.avs.visitapp

import android.view.View


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.avs.visitapp.databinding.AttendanceActivityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: AttendanceActivityBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null

    private val REQUEST_IMAGE_CHECK_IN = 101
    private val REQUEST_IMAGE_CHECK_OUT = 102
    private val REQUEST_PERMISSION = 200

    private var currentLat = 0.0
    private var currentLong = 0.0


    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private var isCheckIn = true
    private val allowedDistanceMeters = 1000f // (GPS logic skipped for now)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AttendanceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "").toString()

        fetchLatestAttendance()

        binding.textInTime.text = "--"
        binding.textOutTime.text = "--"

        binding.toolbarBack.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finish()
        }

        binding.checkIN.setOnClickListener {
            isCheckIn = true
            checkPermissionsAndOpenCamera()
        }

        binding.checkOut.setOnClickListener {
            if (binding.textInTime.text.isNullOrBlank() || binding.textInTime.text == "--") {
                Toast.makeText(this, "Please Check-In before Check-Out", Toast.LENGTH_SHORT).show()
            } else {
                isCheckIn = false
                checkPermissionsAndOpenCamera()
            }
        }

        // 🔹 Disable all calendar date selections permanently
        binding.calendarView.selectionMode =
            com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_NONE
        binding.calendarView.setOnDateChangedListener { _, _, _ -> /* Do nothing */ }

        val initialDate = binding.calendarView.currentDate.date
        val calendar = Calendar.getInstance()
        calendar.time = initialDate
        binding.textMonthYear.text = monthYearFormat.format(initialDate)
        fetchAndShowMonthlyAttendance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)

        binding.calendarView.setOnMonthChangedListener { _, date ->
            binding.textMonthYear.text = monthYearFormat.format(date.date)
            val cal = Calendar.getInstance()
            cal.time = date.date
            fetchAndShowMonthlyAttendance(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }

        // 🔹 Hardcode and save office coordinates once
        val officeLat = 19.1107254
        val officeLong = 73.0153968



        Constant.saveSharedPreferenceValues(this, Constant.SHARE_OFFICE_LATITUDE, officeLat.toString())
        Constant.saveSharedPreferenceValues(this, Constant.SHARE_OFFICE_LONGITUDE, officeLong.toString())

        Log.d("OfficeLocation", "Office coordinates saved → Lat=$officeLat, Long=$officeLong")
    }


    // ---------------- CAMERA PERMISSIONS ----------------
    private fun checkPermissionsAndOpenCamera() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (cameraPermission && locationPermission) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION
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
            startActivityForResult(intent, if (isCheckIn) REQUEST_IMAGE_CHECK_IN else REQUEST_IMAGE_CHECK_OUT)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera/Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- CAMERA RESULT ----------------
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && photoFile != null) {
            val currentTime = timeFormat.format(Date()).uppercase(Locale.getDefault())
            val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD).toString()

            // Save selfie temporarily
            Constant.saveSharedPreferenceValues(
                this,
                if (isCheckIn) Constant.SHARE_EMP_SELFIE_IN else Constant.SHARE_EMP_SELFIE_OUT,
                photoFile!!.absolutePath
            )

            // 🔹 Fetch GPS first, then check distance
            fetchCurrentLocation { success ->
                if (success) {
                    if (isWithinOfficeArea()) {
                        Log.d("AttendanceDebug", "✅ Within office area. Proceeding with attendance.")
                        if (isCheckIn) markAttendance(true) else markAttendance(false)
                    } else {
                        Log.d("AttendanceDebug", "❌ Outside office area. Attendance blocked.")
                        Toast.makeText(
                            this,
                            "You must be inside the office area (≤ 50 m) to mark attendance",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Unable to fetch GPS location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }






    // ---------------- API CALL ----------------
//    private fun markAttendanceApiCall(empId: String) {
//
//        lifecycleScope.launch {
//            try {
//                val response = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.markAttendance(empId)
//                }
//
//                if (response.isSuccessful) {
//                    val message = response.body() ?: "Attendance recorded successfully."
//                    Toast.makeText(this@AttendanceActivity, message, Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@AttendanceActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
//                }
//                Log.d("AttendanceAPI", "Calling MarkAttendance for empId=$response")
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(this@AttendanceActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//
//            Log.d("AttendanceAPI", "Calling MarkAttendance for empId=$empId")
//
//        }
//    }

    /**
     * Unified Attendance API call – handles both Check-In and Check-Out.
     * Shows proper toast message and logs result.
     */
    private fun markAttendance(isCheckIn: Boolean) {
        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
        if (empCode == 0) {
            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Fetch saved office coordinates
        val officeLat = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LATITUDE)?.toDoubleOrNull()
        val officeLong = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LONGITUDE)?.toDoubleOrNull()

        // 🔹 Ensure they exist
        if (officeLat == null || officeLong == null) {
            Log.e("AttendanceDebug", "❌ Office coordinates not available in SharedPreferences!")
            Toast.makeText(this, "Office coordinates not available. Please set them first.", Toast.LENGTH_LONG).show()
            return
        }

        // 🔹 Check if location was fetched
        if (currentLat == 0.0 || currentLong == 0.0) {
            Toast.makeText(this, "Live location not available, please enable GPS", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Calculate distance from office
        val result = FloatArray(1)
        Location.distanceBetween(currentLat, currentLong, officeLat, officeLong, result)
        val distanceInMeters = result[0]

        Log.d("LocationCheck", "📍 Distance from office = ${"%.2f".format(distanceInMeters)} m")

//        // 🔹 Reject if outside office radius
//        if (distanceInMeters > allowedDistanceMeters) {
//            Toast.makeText(this, "You are outside the office area (>${allowedDistanceMeters.toInt()}m)", Toast.LENGTH_LONG).show()
//            return
//        }

        // ✅ Passed all checks → proceed with attendance
        val label = if (isCheckIn) "Check-In" else "Check-Out"
        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())

//        lifecycleScope.launch {
//            try {
//                Log.d("AttendanceAPI", "Calling MarkAttendance (POST) for empId=$empCode")
//                val response = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.markAttendance(empCode.toString())
//                }
//
//                if (response.isSuccessful) {
//                    val msg = response.body() ?: "$label recorded successfully at $currentTime"
//                    Toast.makeText(this@AttendanceActivity, msg, Toast.LENGTH_SHORT).show()
//                    if (isCheckIn) {
//                        binding.textInTime.text = currentTime
//                        binding.textOutTime.text = "--"
//                        binding.checkIN.isEnabled = false
//                    } else {
//                        binding.textOutTime.text = currentTime
//                    }
//                } else {
//                    Toast.makeText(this@AttendanceActivity, "Server error ${response.code()} during $label", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(this@AttendanceActivity, "$label failed: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val address = getAddressFromLocation(currentLat, currentLong)
                    Log.d("AttendanceAPI", "MarkAttendance → empId=$empCode, Latt=$currentLat, Long=$currentLong, Add=$address")
                    ApiClient.apiService.markAttendance(
                        empCode.toString(),
                        currentLat.toString(),
                        currentLong.toString(),
                        address
                    )
                }

                if (response.isSuccessful) {
                    val msg = response.body() ?: "$label recorded successfully"
                    Toast.makeText(this@AttendanceActivity, msg, Toast.LENGTH_SHORT).show()

                    // === NEW: SAVE TODAY'S DATE LOCALLY ON SUCCESS ===
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    Constant.saveSharedPreferenceValues(this@AttendanceActivity, "LAST_ATTENDANCE_DATE", todayDate)

                    if (isCheckIn) {
                        binding.textInTime.text = currentTime
                        binding.textOutTime.text = "--"
                        binding.checkIN.isEnabled = false
                        binding.checkIN.alpha = 0.5f
                    } else {
                        binding.textOutTime.text = currentTime
                        binding.checkOut.isEnabled = false
                        binding.checkOut.alpha = 0.5f
                    }
                } else {
                    Toast.makeText(this@AttendanceActivity, "Server error ${response.code()} during $label", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AttendanceActivity, "$label failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // ---------------- MONTHLY CALENDAR ----------------
//    private fun fetchAndShowMonthlyAttendance(year: Int, month: Int) {
//        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//        if (empCode == 0) {
//            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        lifecycleScope.launch {
//            try {
//                val response: Response<List<Attendance>> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getMonthlyAttendance(empCode, year, month)
//                }
//
//                if (response.isSuccessful) {
//                    val attendanceList = response.body() ?: emptyList()
//                    val presentDates = hashSetOf<CalendarDay>()
//                    val absentDates = hashSetOf<CalendarDay>()
//
//                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//                    val today = Calendar.getInstance()
//
//                    attendanceList.forEach { att ->
//                        val cal = Calendar.getInstance()
//                        try {
//                            cal.time = dateFormat.parse(att.ATDATE) ?: return@forEach
//                            // Skip future dates
//                            if (cal.after(today)) return@forEach
//
//                            val day = CalendarDay.from(cal)
//                            when (att.Status) {
//                                "Present" -> presentDates.add(day)
//                                "Absent" -> absentDates.add(day)
//                            }
//                        } catch (_: Exception) { }
//                    }
//
//                    // Remove old decorators and apply new ones
//                    withContext(Dispatchers.Main) {
//                        binding.calendarView.removeDecorators()
//
//// Present - green circle + white text
//                        binding.calendarView.addDecorator(
//                            AttendanceDecorator(this@AttendanceActivity, presentDates, R.drawable.bg_present_circle)
//                        )
//                        binding.calendarView.addDecorator(
//                            AttendanceTextColorDecorator(presentDates)
//                        )
//
//// Absent - red circle + white text
//                        binding.calendarView.addDecorator(
//                            AttendanceDecorator(this@AttendanceActivity, absentDates, R.drawable.bg_absent_circle)
//                        )
//                        binding.calendarView.addDecorator(
//                            AttendanceTextColorDecorator(absentDates)
//                        )
//
//
//                    }
//
//                } else {
//                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance data", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun fetchAndShowMonthlyAttendance(year: Int, month: Int) {
        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
        if (empCode == 0) {
            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Show loading overlay & disable calendar
        binding.calendarLoadingSpinner.visibility = View.VISIBLE
        binding.calendarView.isEnabled = false

        lifecycleScope.launch {
            try {
                val response: Response<List<Attendance>> = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getMonthlyAttendance(empCode, year, month)
                }

                if (response.isSuccessful) {
                    val attendanceList = response.body() ?: emptyList()
                    val presentDates = hashSetOf<CalendarDay>()
                    val absentDates = hashSetOf<CalendarDay>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val today = Calendar.getInstance()

                    attendanceList.forEach { att ->
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = dateFormat.parse(att.ATDATE) ?: return@forEach
                            if (cal.after(today)) return@forEach

                            val day = CalendarDay.from(cal)
                            when (att.Status) {
                                "Present" -> presentDates.add(day)
                                "Absent" -> absentDates.add(day)
                            }
                        } catch (_: Exception) { }
                    }

                    withContext(Dispatchers.Main) {
                        binding.calendarView.removeDecorators()

                        binding.calendarView.addDecorator(
                            AttendanceDecorator(this@AttendanceActivity, presentDates, R.drawable.bg_present_circle)
                        )
                        binding.calendarView.addDecorator(
                            AttendanceTextColorDecorator(presentDates)
                        )
                        binding.calendarView.addDecorator(
                            AttendanceDecorator(this@AttendanceActivity, absentDates, R.drawable.bg_absent_circle)
                        )
                        binding.calendarView.addDecorator(
                            AttendanceTextColorDecorator(absentDates)
                        )
                    }
                } else {
                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // After data is fetched:
                binding.calendarLoadingSpinner.visibility = View.GONE
                binding.calendarView.isEnabled = true
            }
        }
    }




//    private fun fetchLatestAttendance() {
//        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        lifecycleScope.launch {
//            try {
//                val response: Response<LatestAttendanceResponse> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getLatestAttendance(empId)
//                }
//
//                if (response.isSuccessful) {
//                    val attendance = response.body()
//                    if (attendance != null) {
//                        val apiTimeFormat = SimpleDateFormat("HH:mm:ss.SSSSSSS", Locale.getDefault())
//                        val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
//
//                        val inTimeText = attendance.INTIME?.let { inTime ->
//                            try {
//                                val date = apiTimeFormat.parse(inTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) {
//                                "--"
//                            }
//                        } ?: "--"
//
//                        val outTimeText = attendance.OUTTIME?.let { outTime ->
//                            try {
//                                val date = apiTimeFormat.parse(outTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) {
//                                "--"
//                            }
//                        } ?: "--"
//
//                        binding.textInTime.text = inTimeText
//                        binding.textOutTime.text = outTimeText
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

//    private fun fetchLatestAttendance() {
//        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        lifecycleScope.launch {
//            try {
//                val response: Response<LatestAttendanceResponse> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getLatestAttendance(empId)
//                }
//
//                if (response.isSuccessful) {
//                    val attendance = response.body()
//                    if (attendance != null) {
//                        val apiTimeFormat = SimpleDateFormat("HH:mm:ss.SSSSSSS", Locale.getDefault())
//                        val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
//
//                        // --- 1. HANDLE IN-TIME ---
//                        val inTimeText = attendance.INTIME?.let { inTime ->
//                            try {
//                                val date = apiTimeFormat.parse(inTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) { "--" }
//                        } ?: "--"
//
//                        binding.textInTime.text = inTimeText
//
//                        // SAFETY CHECK: If In-Time exists, disable Check-In button
//                        if (inTimeText != "--") {
//                            binding.checkIN.isEnabled = false
//                            binding.checkIN.alpha = 0.5f // Optional: Dim the button
//                        }
//
//                        // --- 2. HANDLE OUT-TIME ---
//                        val outTimeText = attendance.OUTTIME?.let { outTime ->
//                            try {
//                                val date = apiTimeFormat.parse(outTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) { "--" }
//                        } ?: "--"
//
//                        binding.textOutTime.text = outTimeText
//
//                        // SAFETY CHECK: If Out-Time exists, disable Check-Out button
//                        if (outTimeText != "--") {
//                            binding.checkOut.isEnabled = false
//                            binding.checkOut.alpha = 0.5f // Optional: Dim the button
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

    private fun fetchLatestAttendance() {
        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")

        // 1. Get Today's Date and the Saved Date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val lastRecordedDate = Constant.getSharedPreferenceValue(this, "LAST_ATTENDANCE_DATE") ?: ""

        // 2. CHECK: Is it a new day?
        if (lastRecordedDate != todayDate) {
            // === NEW DAY DETECTED (RESET UI) ===
            Log.d("AttendanceDebug", "Date mismatch: Saved=$lastRecordedDate vs Today=$todayDate. Resetting.")

            binding.textInTime.text = "--"
            binding.textOutTime.text = "--"

            // Enable buttons for the new day
            binding.checkIN.isEnabled = true
            binding.checkIN.alpha = 1.0f

            binding.checkOut.isEnabled = true
            binding.checkOut.alpha = 1.0f

            // Stop here! Do not load old API data because it belongs to yesterday.
            return
        }

        // 3. SAME DAY: Fetch API data to restore state (in case app was restarted)
        lifecycleScope.launch {
            try {
                val response: Response<LatestAttendanceResponse> = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLatestAttendance(empId)
                }

                if (response.isSuccessful) {
                    val attendance = response.body()
                    if (attendance != null) {
                        val apiTimeFormat = SimpleDateFormat("HH:mm:ss.SSSSSSS", Locale.getDefault())
                        val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

                        // --- Restore In-Time ---
                        val inTimeText = attendance.INTIME?.let { inTime ->
                            try {
                                val date = apiTimeFormat.parse(inTime)
                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
                            } catch (e: Exception) { "--" }
                        } ?: "--"

                        binding.textInTime.text = inTimeText

                        // Since we know it is the SAME DAY (checked above), lock button if time exists
                        if (inTimeText != "--") {
                            binding.checkIN.isEnabled = false
                            binding.checkIN.alpha = 0.5f
                        }

                        // --- Restore Out-Time ---
                        val outTimeText = attendance.OUTTIME?.let { outTime ->
                            try {
                                val date = apiTimeFormat.parse(outTime)
                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
                            } catch (e: Exception) { "--" }
                        } ?: "--"

                        // SAFETY: If OUTTIME is same as INTIME (or blank), user has NOT checked out yet.
                        // Show "--" and keep Check-Out button enabled instead of duplicating In-Time.
                        val isOutTimeValid = outTimeText != "--" &&
                                !attendance.OUTTIME.isNullOrBlank() &&
                                attendance.OUTTIME != attendance.INTIME

                        binding.textOutTime.text = if (isOutTimeValid) outTimeText else "--"

                        if (isOutTimeValid) {
                            binding.checkOut.isEnabled = false
                            binding.checkOut.alpha = 0.5f
                        } else {
                            binding.checkOut.isEnabled = true
                            binding.checkOut.alpha = 1.0f
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

//    private fun fetchLatLong(staffId: String) {
//        lifecycleScope.launch {
//            try {
//                val response: Response<LatLongResponse> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getLatLong(staffId)
//                }
//
//                if (response.isSuccessful) {
//                    val latLongResponse = response.body()
//                    if (latLongResponse != null && latLongResponse.data.isNotEmpty()) {
//                        val responseData = latLongResponse.data[0]
//                        val lat = responseData.Latitude.toDoubleOrNull()
//                        val lng = responseData.Longitude.toDoubleOrNull()
//
//                        if (lat != null && lng != null) {
//                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LATITUDE, lat.toString())
//                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LONGITUDE, lng.toString())
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
////    @SuppressLint("MissingPermission")
//    private fun fetchCurrentLocation(onLocationReady: (Boolean) -> Unit) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                REQUEST_PERMISSION
//            )
//            onLocationReady(false)
//            return
//        }
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            if (location != null) {
//                currentLat = location.latitude
//                currentLong = location.longitude
//                Log.d("LiveLocationDebug", "Fetched live GPS → Lat=$currentLat, Long=$currentLong")
//                onLocationReady(true)
//            } else {
//                Log.d("LiveLocationDebug", "⚠ Location = null (GPS off or signal weak)")
//                Toast.makeText(this, "Unable to fetch live GPS location", Toast.LENGTH_SHORT).show()
//                onLocationReady(false)
//            }
//        }
//    }


    //    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(onLocationReady: (Boolean) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION
            )
            onLocationReady(false)
            return
        }

        // 🔹 Check if Location (GPS) service is ON. If OFF, show dialog to enable it.
        if (!isLocationServiceEnabled()) {
            Log.e("LiveLocationDebug", "⚠ Location service is OFF")
            showEnableLocationDialog()
            onLocationReady(false)
            return
        }

        // 🔹 CHANGE: Use CancellationTokenSource for fresh request
        val cancellationTokenSource = CancellationTokenSource()

        // 🔹 CHANGE: Request PRIORITY_HIGH_ACCURACY to get live GPS data immediately
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLong = location.longitude
                Log.d("LiveLocationDebug", "✅ Fetched LIVE High-Accuracy GPS → Lat=$currentLat, Long=$currentLong")
                onLocationReady(true)
            } else {
                Log.e("LiveLocationDebug", "⚠ Location is null. GPS might be disabled or view obstructed.")
                Toast.makeText(this, "Unable to fetch live GPS location. Please ensure GPS is On.", Toast.LENGTH_SHORT).show()
                onLocationReady(false)
            }
        }.addOnFailureListener { e ->
            Log.e("LiveLocationDebug", "❌ Failed to get location: ${e.message}")
            Toast.makeText(this, "Error fetching location: ${e.message}", Toast.LENGTH_SHORT).show()
            onLocationReady(false)
        }
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Required")
            .setMessage("Location (GPS) is turned OFF. Please turn ON location to mark attendance.")
            .setCancelable(false)
            .setPositiveButton("Turn On Location") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Address not found"
        } catch (e: Exception) {
            Log.e("AttendanceDebug", "Geocoder failed: ${e.message}")
            "Address not found"
        }
    }


    private fun isWithinOfficeArea(): Boolean {
        val officeLat = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LATITUDE)?.toDoubleOrNull() ?: 0.0
        val officeLng = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LONGITUDE)?.toDoubleOrNull() ?: 0.0

        if (officeLat == 0.0 || officeLng == 0.0) {
            Log.d("LiveLocationDebug", "⚠ Office coordinates not available")
            Toast.makeText(this, "Office location not configured", Toast.LENGTH_SHORT).show()
            return false
        }

        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLong, officeLat, officeLng, results)
        val distance = results[0]

        Log.d(
            "LiveLocationDebug",
            """
        -------------------------------
        📍 LIVE LOCATION
        Lat=$currentLat, Long=$currentLong
        🏢 OFFICE LOCATION
        Lat=$officeLat, Long=$officeLng
        📏 Distance = ${"%.2f".format(distance)} m
        -------------------------------
        """.trimIndent()
        )

//        if (distance > 50f) {
//            Toast.makeText(this, "You must be inside the office area (≤ 50 m) to mark attendance", Toast.LENGTH_LONG).show()
//            return false
//        }

        return true
    }




}





//
////vishal junnar
//package com.avs.visitapp
//
//import android.view.View
//
//
//import android.Manifest
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.location.Location
//import android.net.Uri
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.util.Log
//import android.widget.Toast
//import androidx.annotation.RequiresPermission
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import androidx.lifecycle.lifecycleScope
//import com.avs.visitapp.databinding.AttendanceActivityBinding
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.prolificinteractive.materialcalendarview.CalendarDay
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import retrofit2.Response
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.*
//
//
//class AttendanceActivity : AppCompatActivity() {
//
//    private lateinit var binding: AttendanceActivityBinding
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private var photoFile: File? = null
//
//    private val REQUEST_IMAGE_CHECK_IN = 101
//    private val REQUEST_IMAGE_CHECK_OUT = 102
//    private val REQUEST_PERMISSION = 200
//
//    private var currentLat = 0.0
//    private var currentLong = 0.0
//
//
//    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
//    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
//
//    private var isCheckIn = true
//    private val allowedDistanceMeters = 1000f // (GPS logic skipped for now)
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = AttendanceActivityBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "").toString()
//
//        fetchLatestAttendance()
//
//        binding.textInTime.text = "--"
//        binding.textOutTime.text = "--"
//
//        binding.toolbarBack.setOnClickListener {
//            val intent = Intent(this, Dashboard::class.java)
//            startActivity(intent)
//            finish()
//        }
//
//        binding.checkIN.setOnClickListener {
//            isCheckIn = true
//            checkPermissionsAndOpenCamera()
//        }
//
//        binding.checkOut.setOnClickListener {
//            if (binding.textInTime.text.isNullOrBlank() || binding.textInTime.text == "--") {
//                Toast.makeText(this, "Please Check-In before Check-Out", Toast.LENGTH_SHORT).show()
//            } else {
//                isCheckIn = false
//                checkPermissionsAndOpenCamera()
//            }
//        }
//
//        // 🔹 Disable all calendar date selections permanently
//        binding.calendarView.selectionMode =
//            com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_NONE
//        binding.calendarView.setOnDateChangedListener { _, _, _ -> /* Do nothing */ }
//
//        val initialDate = binding.calendarView.currentDate.date
//        val calendar = Calendar.getInstance()
//        calendar.time = initialDate
//        binding.textMonthYear.text = monthYearFormat.format(initialDate)
//        fetchAndShowMonthlyAttendance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
//
//        binding.calendarView.setOnMonthChangedListener { _, date ->
//            binding.textMonthYear.text = monthYearFormat.format(date.date)
//            val cal = Calendar.getInstance()
//            cal.time = date.date
//            fetchAndShowMonthlyAttendance(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
//        }
//
//        // 🔹 Hardcode and save office coordinates once
//        val officeLat = 19.1107254
//        val officeLong = 73.0153968
//
////        // 🔹 Hardcode and save home coordinates once
////        val officeLat = 19.1107254
////        val officeLong = 73.0153968
//
//        Constant.saveSharedPreferenceValues(this, Constant.SHARE_OFFICE_LATITUDE, officeLat.toString())
//        Constant.saveSharedPreferenceValues(this, Constant.SHARE_OFFICE_LONGITUDE, officeLong.toString())
//
//        Log.d("OfficeLocation", "Office coordinates saved → Lat=$officeLat, Long=$officeLong")
//    }
//
//
//    // ---------------- CAMERA PERMISSIONS ----------------
//    private fun checkPermissionsAndOpenCamera() {
//        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//
//        if (cameraPermission && locationPermission) {
//            openCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
//                REQUEST_PERMISSION
//            )
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
//            startActivityForResult(intent, if (isCheckIn) REQUEST_IMAGE_CHECK_IN else REQUEST_IMAGE_CHECK_OUT)
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
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_PERMISSION) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                openCamera()
//            } else {
//                Toast.makeText(this, "Camera/Location permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    // ---------------- CAMERA RESULT ----------------
////    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (resultCode == Activity.RESULT_OK && photoFile != null) {
//            val currentTime = timeFormat.format(Date()).uppercase(Locale.getDefault())
//            val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD).toString()
//
//            // Save selfie temporarily
//            Constant.saveSharedPreferenceValues(
//                this,
//                if (isCheckIn) Constant.SHARE_EMP_SELFIE_IN else Constant.SHARE_EMP_SELFIE_OUT,
//                photoFile!!.absolutePath
//            )
//
//            // 🔹 Fetch GPS first, then check distance
//            fetchCurrentLocation { success ->
//                if (success) {
//                    if (isWithinOfficeArea()) {
//                        Log.d("AttendanceDebug", "✅ Within office area. Proceeding with attendance.")
//                        if (isCheckIn) markAttendance(true) else markAttendance(false)
//                    } else {
//                        Log.d("AttendanceDebug", "❌ Outside office area. Attendance blocked.")
//                        Toast.makeText(
//                            this,
//                            "You must be inside the office area (≤ 50 m) to mark attendance",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Unable to fetch GPS location", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//
//
//
//
//
//    // ---------------- API CALL ----------------
////    private fun markAttendanceApiCall(empId: String) {
////
////        lifecycleScope.launch {
////            try {
////                val response = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.markAttendance(empId)
////                }
////
////                if (response.isSuccessful) {
////                    val message = response.body() ?: "Attendance recorded successfully."
////                    Toast.makeText(this@AttendanceActivity, message, Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(this@AttendanceActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
////                }
////                Log.d("AttendanceAPI", "Calling MarkAttendance for empId=$response")
////
////            } catch (e: Exception) {
////                e.printStackTrace()
////                Toast.makeText(this@AttendanceActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
////            }
////
////            Log.d("AttendanceAPI", "Calling MarkAttendance for empId=$empId")
////
////        }
////    }
//
//    /**
//     * Unified Attendance API call – handles both Check-In and Check-Out.
//     * Shows proper toast message and logs result.
//     */
//    private fun markAttendance(isCheckIn: Boolean) {
//        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//        if (empCode == 0) {
//            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 🔹 Fetch saved office coordinates
//        val officeLat = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LATITUDE)?.toDoubleOrNull()
//        val officeLong = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LONGITUDE)?.toDoubleOrNull()
//
//        // 🔹 Ensure they exist
//        if (officeLat == null || officeLong == null) {
//            Log.e("AttendanceDebug", "❌ Office coordinates not available in SharedPreferences!")
//            Toast.makeText(this, "Office coordinates not available. Please set them first.", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        // 🔹 Check if location was fetched
//        if (currentLat == 0.0 || currentLong == 0.0) {
//            Toast.makeText(this, "Live location not available, please enable GPS", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 🔹 Calculate distance from office
//        val result = FloatArray(1)
//        Location.distanceBetween(currentLat, currentLong, officeLat, officeLong, result)
//        val distanceInMeters = result[0]
//
//        Log.d("LocationCheck", "📍 Distance from office = ${"%.2f".format(distanceInMeters)} m")
//
//        // 🔹 Reject if outside office radius
//        if (distanceInMeters > allowedDistanceMeters) {
//            Toast.makeText(this, "You are outside the office area (>${allowedDistanceMeters.toInt()}m)", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        // ✅ Passed all checks → proceed with attendance
//        val label = if (isCheckIn) "Check-In" else "Check-Out"
//        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
//
//        lifecycleScope.launch {
//            try {
//                Log.d("AttendanceAPI", "Calling MarkAttendance (POST) for empId=$empCode")
//                val response = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.markAttendance(empCode.toString())
//                }
//
//                if (response.isSuccessful) {
//                    val msg = response.body() ?: "$label recorded successfully at $currentTime"
//                    Toast.makeText(this@AttendanceActivity, msg, Toast.LENGTH_SHORT).show()
//                    if (isCheckIn) {
//                        binding.textInTime.text = currentTime
//                        binding.textOutTime.text = "--"
//                        binding.checkIN.isEnabled = false
//                    } else {
//                        binding.textOutTime.text = currentTime
//                    }
//                } else {
//                    Toast.makeText(this@AttendanceActivity, "Server error ${response.code()} during $label", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(this@AttendanceActivity, "$label failed: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//
//
//    // ---------------- MONTHLY CALENDAR ----------------
////    private fun fetchAndShowMonthlyAttendance(year: Int, month: Int) {
////        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
////        if (empCode == 0) {
////            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
////            return
////        }
////
////        lifecycleScope.launch {
////            try {
////                val response: Response<List<Attendance>> = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.getMonthlyAttendance(empCode, year, month)
////                }
////
////                if (response.isSuccessful) {
////                    val attendanceList = response.body() ?: emptyList()
////                    val presentDates = hashSetOf<CalendarDay>()
////                    val absentDates = hashSetOf<CalendarDay>()
////
////                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
////                    val today = Calendar.getInstance()
////
////                    attendanceList.forEach { att ->
////                        val cal = Calendar.getInstance()
////                        try {
////                            cal.time = dateFormat.parse(att.ATDATE) ?: return@forEach
////                            // Skip future dates
////                            if (cal.after(today)) return@forEach
////
////                            val day = CalendarDay.from(cal)
////                            when (att.Status) {
////                                "Present" -> presentDates.add(day)
////                                "Absent" -> absentDates.add(day)
////                            }
////                        } catch (_: Exception) { }
////                    }
////
////                    // Remove old decorators and apply new ones
////                    withContext(Dispatchers.Main) {
////                        binding.calendarView.removeDecorators()
////
////// Present - green circle + white text
////                        binding.calendarView.addDecorator(
////                            AttendanceDecorator(this@AttendanceActivity, presentDates, R.drawable.bg_present_circle)
////                        )
////                        binding.calendarView.addDecorator(
////                            AttendanceTextColorDecorator(presentDates)
////                        )
////
////// Absent - red circle + white text
////                        binding.calendarView.addDecorator(
////                            AttendanceDecorator(this@AttendanceActivity, absentDates, R.drawable.bg_absent_circle)
////                        )
////                        binding.calendarView.addDecorator(
////                            AttendanceTextColorDecorator(absentDates)
////                        )
////
////
////                    }
////
////                } else {
////                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance data", Toast.LENGTH_SHORT).show()
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
////            }
////        }
////    }
//
//    private fun fetchAndShowMonthlyAttendance(year: Int, month: Int) {
//        val empCode = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//        if (empCode == 0) {
//            Toast.makeText(this, "Invalid employee code", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 🔹 Show loading overlay & disable calendar
//        binding.calendarLoadingSpinner.visibility = View.VISIBLE
//        binding.calendarView.isEnabled = false
//
//        lifecycleScope.launch {
//            try {
//                val response: Response<List<Attendance>> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getMonthlyAttendance(empCode, year, month)
//                }
//
//                if (response.isSuccessful) {
//                    val attendanceList = response.body() ?: emptyList()
//                    val presentDates = hashSetOf<CalendarDay>()
//                    val absentDates = hashSetOf<CalendarDay>()
//                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//                    val today = Calendar.getInstance()
//
//                    attendanceList.forEach { att ->
//                        val cal = Calendar.getInstance()
//                        try {
//                            cal.time = dateFormat.parse(att.ATDATE) ?: return@forEach
//                            if (cal.after(today)) return@forEach
//
//                            val day = CalendarDay.from(cal)
//                            when (att.Status) {
//                                "Present" -> presentDates.add(day)
//                                "Absent" -> absentDates.add(day)
//                            }
//                        } catch (_: Exception) { }
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        binding.calendarView.removeDecorators()
//
//                        binding.calendarView.addDecorator(
//                            AttendanceDecorator(this@AttendanceActivity, presentDates, R.drawable.bg_present_circle)
//                        )
//                        binding.calendarView.addDecorator(
//                            AttendanceTextColorDecorator(presentDates)
//                        )
//                        binding.calendarView.addDecorator(
//                            AttendanceDecorator(this@AttendanceActivity, absentDates, R.drawable.bg_absent_circle)
//                        )
//                        binding.calendarView.addDecorator(
//                            AttendanceTextColorDecorator(absentDates)
//                        )
//                    }
//                } else {
//                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance data", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//            } finally {
//                // After data is fetched:
//                binding.calendarLoadingSpinner.visibility = View.GONE
//                binding.calendarView.isEnabled = true
//            }
//        }
//    }
//
//
//
//
//    private fun fetchLatestAttendance() {
//        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
//
//        lifecycleScope.launch {
//            try {
//                val response: Response<LatestAttendanceResponse> = withContext(Dispatchers.IO) {
//                    ApiClient.apiService.getLatestAttendance(empId)
//                }
//
//                if (response.isSuccessful) {
//                    val attendance = response.body()
//                    if (attendance != null) {
//                        val apiTimeFormat = SimpleDateFormat("HH:mm:ss.SSSSSSS", Locale.getDefault())
//                        val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
//
//                        val inTimeText = attendance.INTIME?.let { inTime ->
//                            try {
//                                val date = apiTimeFormat.parse(inTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) {
//                                "--"
//                            }
//                        } ?: "--"
//
//                        val outTimeText = attendance.OUTTIME?.let { outTime ->
//                            try {
//                                val date = apiTimeFormat.parse(outTime)
//                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
//                            } catch (e: Exception) {
//                                "--"
//                            }
//                        } ?: "--"
//
//                        binding.textInTime.text = inTimeText
//                        binding.textOutTime.text = outTimeText
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
////    private fun fetchLatLong(staffId: String) {
////        lifecycleScope.launch {
////            try {
////                val response: Response<LatLongResponse> = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.getLatLong(staffId)
////                }
////
////                if (response.isSuccessful) {
////                    val latLongResponse = response.body()
////                    if (latLongResponse != null && latLongResponse.data.isNotEmpty()) {
////                        val responseData = latLongResponse.data[0]
////                        val lat = responseData.Latitude.toDoubleOrNull()
////                        val lng = responseData.Longitude.toDoubleOrNull()
////
////                        if (lat != null && lng != null) {
////                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LATITUDE, lat.toString())
////                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LONGITUDE, lng.toString())
////                        }
////                    }
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////            }
////        }
////    }
//
////    @SuppressLint("MissingPermission")
//    private fun fetchCurrentLocation(onLocationReady: (Boolean) -> Unit) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                REQUEST_PERMISSION
//            )
//            onLocationReady(false)
//            return
//        }
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            if (location != null) {
//                currentLat = location.latitude
//                currentLong = location.longitude
//                Log.d("LiveLocationDebug", "Fetched live GPS → Lat=$currentLat, Long=$currentLong")
//                onLocationReady(true)
//            } else {
//                Log.d("LiveLocationDebug", "⚠ Location = null (GPS off or signal weak)")
//                Toast.makeText(this, "Unable to fetch live GPS location", Toast.LENGTH_SHORT).show()
//                onLocationReady(false)
//            }
//        }
//    }
//
//
//    private fun isWithinOfficeArea(): Boolean {
//        val officeLat = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LATITUDE)?.toDoubleOrNull() ?: 0.0
//        val officeLng = Constant.getSharedPreferenceValue(this, Constant.SHARE_OFFICE_LONGITUDE)?.toDoubleOrNull() ?: 0.0
//
//        if (officeLat == 0.0 || officeLng == 0.0) {
//            Log.d("LiveLocationDebug", "⚠ Office coordinates not available")
//            Toast.makeText(this, "Office location not configured", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        val results = FloatArray(1)
//        Location.distanceBetween(currentLat, currentLong, officeLat, officeLng, results)
//        val distance = results[0]
//
//        Log.d(
//            "LiveLocationDebug",
//            """
//        -------------------------------
//        📍 LIVE LOCATION
//        Lat=$currentLat, Long=$currentLong
//        🏢 OFFICE LOCATION
//        Lat=$officeLat, Long=$officeLng
//        📏 Distance = ${"%.2f".format(distance)} m
//        -------------------------------
//        """.trimIndent()
//        )
//
//        if (distance > 50f) {
//            Toast.makeText(this, "You must be inside the office area (≤ 50 m) to mark attendance", Toast.LENGTH_LONG).show()
//            return false
//        }
//
//        return true
//    }
//
//
//
//
//}
//
//
//
//
////package com.avs.visitapp
////
////import android.Manifest
////import android.app.Activity
////import android.content.Intent
////import android.content.pm.PackageManager
////import android.graphics.Bitmap
////import android.location.Location
////import android.os.Bundle
////import android.provider.MediaStore
////import android.widget.Toast
////import androidx.annotation.RequiresPermission
////import androidx.appcompat.app.AppCompatActivity
////import androidx.core.app.ActivityCompat
////import androidx.core.content.ContextCompat
////import androidx.lifecycle.lifecycleScope
////import com.avs.visitapp.databinding.AttendanceActivityBinding
////import com.google.android.gms.location.FusedLocationProviderClient
////import com.google.android.gms.location.LocationServices
////import com.prolificinteractive.materialcalendarview.CalendarDay
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import kotlinx.coroutines.withContext
////import retrofit2.Response
////import java.text.SimpleDateFormat
////import java.util.*
////
////class AttendanceActivity : AppCompatActivity() {
////
////    private lateinit var binding: AttendanceActivityBinding
////    private lateinit var fusedLocationClient: FusedLocationProviderClient
////
////    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
////
////    private val REQUEST_IMAGE_CHECK_IN = 101
////    private val REQUEST_IMAGE_CHECK_OUT = 102
////    private val REQUEST_LOCATION = 200
////
////    private var isCheckIn = true
////    private val allowedDistanceMeters = 1000f
////    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////        binding = AttendanceActivityBinding.inflate(layoutInflater)
////        setContentView(binding.root)
////
////        val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "").toString()
////
////        // Fetch office lat/long
////        fetchLatLong(staffId)
////
////        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
////
////        binding.textInTime.text = "--"
////        binding.textOutTime.text = "--"
////
////        // Fetch latest attendance data from API
////        fetchLatestAttendance()
////
////        binding.toolbarBack.setOnClickListener {
////            val intent = Intent(this, Dashboard::class.java)
////            startActivity(intent)
////            finish()
////        }
////
////        binding.checkIN.setOnClickListener {
////            isCheckIn = true
////            checkLocationPermissionAndProceed()
////        }
////
////        binding.checkOut.setOnClickListener {
////            if (binding.textInTime.text.isNullOrBlank() || binding.textInTime.text == "--") {
////                Toast.makeText(this, "Please Check-In before Check-Out", Toast.LENGTH_SHORT).show()
////            } else {
////                isCheckIn = false
////                checkLocationPermissionAndProceed()
////            }
////        }
////
////        // Initial month display and API call
////        val initialDate = binding.calendarView.currentDate.date
////        val calendar = Calendar.getInstance()
////        calendar.time = initialDate
////        binding.textMonthYear.text = monthYearFormat.format(initialDate)
////        fetchAndShowMonthlyAttendance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
////
////
////        binding.calendarView.setOnMonthChangedListener { _, date ->
////            binding.textMonthYear.text = monthYearFormat.format(date.date)
////            val cal = Calendar.getInstance()
////            cal.time = date.date
////            fetchAndShowMonthlyAttendance(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
////        }
////
////    }
////
////    private fun fetchAndShowMonthlyAttendance(year: Int, month: Int) {
////        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")?.toIntOrNull() ?: return
////
////        lifecycleScope.launch {
////            try {
////                val response: Response<List<Attendance>> = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.getMonthlyAttendance(empId, year, month)
////                }
////
////                if (response.isSuccessful) {
////                    val attendanceList = response.body() ?: emptyList()
////
////                    val presentDates = hashSetOf<CalendarDay>()
////                    val absentDates = hashSetOf<CalendarDay>()
////                    val today = Calendar.getInstance()
////
////                    attendanceList.forEach { att ->
////                        val cal = Calendar.getInstance()
////                        cal.time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(att.ATDATE)!!
////
////                        // Skip future dates
////                        if (cal.after(today)) return@forEach
////
////                        val day = CalendarDay.from(cal)
////                        when (att.Status) {
////                            "Present" -> presentDates.add(day)
////                            "Absent" -> absentDates.add(day)
////                        }
////                    }
////
////                    // Remove previous decorators
////                    binding.calendarView.removeDecorators()
//////
//////                    binding.calendarView.addDecorator(
//////                        AttendanceDecorator(ContextCompat.getColor(this@AttendanceActivity, R.color.present_day), presentDates)
//////                    )
//////                    binding.calendarView.addDecorator(
//////                        AttendanceDecorator(ContextCompat.getColor(this@AttendanceActivity, R.color.absent_day), absentDates)
//////                    )
////
////                } else {
////                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance", Toast.LENGTH_SHORT).show()
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
//////                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
////            }
////        }
////    }
////
////
////    private fun fetchLatestAttendance() {
////        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
////
////        lifecycleScope.launch {
////            try {
////                val response: Response<LatestAttendanceResponse> = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.getLatestAttendance(empId)
////                }
////
////                if (response.isSuccessful) {
////                    val attendance = response.body()
////                    if (attendance != null) {
////                        val apiTimeFormat = SimpleDateFormat("HH:mm:ss.SSSSSSS", Locale.getDefault())
////                        val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
////
////                        val inTimeText = attendance.INTIME?.let { inTime ->
////                            try {
////                                val date = apiTimeFormat.parse(inTime)
////                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
////                            } catch (e: Exception) { "--" }
////                        } ?: "--"
////
////                        val outTimeText = attendance.OUTTIME?.let { outTime ->
////                            try {
////                                val date = apiTimeFormat.parse(outTime)
////                                displayTimeFormat.format(date).uppercase(Locale.getDefault())
////                            } catch (e: Exception) { "--" }
////                        } ?: "--"
////
////                        binding.textInTime.text = inTimeText
////                        binding.textOutTime.text = outTimeText
////
////                        // Enable/disable Check-In button based on INTIME
////                        binding.checkIN.isEnabled = attendance.INTIME.isNullOrEmpty() || !attendance.OUTTIME.isNullOrEmpty()
////
////                    } else {
////                        Toast.makeText(this@AttendanceActivity, "No attendance data found.", Toast.LENGTH_SHORT).show()
////                    }
////                } else {
////                    Toast.makeText(this@AttendanceActivity, "Failed to fetch attendance: ${response.code()}", Toast.LENGTH_SHORT).show()
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                Toast.makeText(this@AttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
////            }
////        }
////    }
////
////    // ----------------- Fetch Lat/Long -----------------
////    private fun fetchLatLong(staffId: String) {
////        lifecycleScope.launch {
////            try {
////                val response: Response<LatLongResponse> = withContext(Dispatchers.IO) {
////                    ApiClient.apiService.getLatLong(staffId)
////                }
////
////                if (response.isSuccessful) {
////                    val latLongResponse = response.body()
////                    if (latLongResponse != null && latLongResponse.data.isNotEmpty()) {
////                        val responseData = latLongResponse.data[0]
////                        val lat = responseData.Latitude.toDoubleOrNull()
////                        val lng = responseData.Longitude.toDoubleOrNull()
////
////                        if (lat != null && lng != null) {
////                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LATITUDE, lat.toString())
////                            Constant.saveSharedPreferenceValues(this@AttendanceActivity, Constant.SHARE_EMP_LONGITUDE, lng.toString())
////                        }
////                    }
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////            }
////        }
////    }
////
////    // ----------------- Location & Distance -----------------
////    private fun checkLocationPermissionAndProceed() {
////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
////            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
////        } else {
////            getLocationAndVerifyDistance()
////        }
////    }
////
////    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
////    private fun getLocationAndVerifyDistance() {
////        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
////            if (location != null) {
////                val savedLatLng = getSavedLatLng()
////                if (savedLatLng != null) {
////                    val distance = FloatArray(1)
////                    Location.distanceBetween(
////                        location.latitude, location.longitude,
////                        savedLatLng.first, savedLatLng.second,
////                        distance
////                    )
////
////                    if (distance[0] <= allowedDistanceMeters) {
////                        checkCameraPermissionAndOpenCamera(if (isCheckIn) REQUEST_IMAGE_CHECK_IN else REQUEST_IMAGE_CHECK_OUT)
////                    } else {
////                        Toast.makeText(this, "You are not in office premises", Toast.LENGTH_SHORT).show()
////                    }
////                } else {
////                    Toast.makeText(this, "Location not found. Please login again.", Toast.LENGTH_SHORT).show()
////                }
////            } else {
////                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
////            }
////        }.addOnFailureListener {
////            Toast.makeText(this, "Location fetch failed: ${it.message}", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    private fun getSavedLatLng(): Pair<Double, Double>? {
////        val latStr = Constant.getSharedPreference(this, Constant.SHARE_EMP_LATITUDE, "")
////        val lngStr = Constant.getSharedPreference(this, Constant.SHARE_EMP_LONGITUDE, "")
////
////        return if (!latStr.isNullOrEmpty() && !lngStr.isNullOrEmpty()) {
////            val lat = latStr.toDoubleOrNull()
////            val lng = lngStr.toDoubleOrNull()
////            if (lat != null && lng != null) Pair(lat, lng) else null
////        } else null
////    }
////
////    // ----------------- Camera -----------------
////    private fun checkCameraPermissionAndOpenCamera(requestCode: Int) {
////        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
////            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), requestCode)
////        } else {
////            openCamera(requestCode)
////        }
////    }
////
////    private fun openCamera(requestCode: Int) {
////        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
////        if (cameraIntent.resolveActivity(packageManager) != null) {
////            startActivityForResult(cameraIntent, requestCode)
////        } else {
////            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////
////        if (resultCode == Activity.RESULT_OK && data != null) {
////            val bitmap = data.extras?.get("data") as? Bitmap
////            val currentTime = timeFormat.format(Date()).uppercase(Locale.getDefault())
////            val staffId = Constant.getSharedPreference(this, Constant.SHARE_EMP_ID, "").toString()
////
////            when (requestCode) {
////                REQUEST_IMAGE_CHECK_IN -> {
////                    if (isCheckIn) {
////                        binding.textInTime.text = currentTime
////                        binding.textOutTime.text = "--"
////                        Toast.makeText(this, "Check-In at $currentTime", Toast.LENGTH_SHORT).show()
////                        markAttendanceApiCall()
////                        binding.checkIN.isEnabled = false
////                    }
////                }
////                REQUEST_IMAGE_CHECK_OUT -> {
////                    if (!isCheckIn) {
////                        binding.textOutTime.text = currentTime
////                        Toast.makeText(this, "Check-Out at $currentTime", Toast.LENGTH_SHORT).show()
////                        markAttendanceApiCall()
////                    }
////                }
////            }
////        }
////    }
////
////    private fun markAttendanceApiCall() {
////        val empId = Constant.getSharedPreference(this, Constant.SHARE_EMP_CD, "")
////        lifecycleScope.launch {
////            try {
////                val response = withContext(Dispatchers.IO) { ApiClient.apiService.markAttendance(empId).execute() }
////
////                if (response.isSuccessful) {
////                    val message = response.body() ?: "Attendance recorded"
////                    Toast.makeText(this@AttendanceActivity, message, Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(this@AttendanceActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                Toast.makeText(this@AttendanceActivity, "Failed to record attendance: ${e.message}", Toast.LENGTH_SHORT).show()
////            }
////        }
////    }
////
////    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
////    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
////        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
////        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////            when (requestCode) {
////                REQUEST_LOCATION -> getLocationAndVerifyDistance()
////                REQUEST_IMAGE_CHECK_IN -> openCamera(REQUEST_IMAGE_CHECK_IN)
////                REQUEST_IMAGE_CHECK_OUT -> openCamera(REQUEST_IMAGE_CHECK_OUT)
////            }
////        } else {
////            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
////        }
////    }
////}
