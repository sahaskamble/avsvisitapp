package com.avs.visitapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.avs.visitapp.databinding.DialogLoadingBinding
import com.avs.visitapp.databinding.LoginActivityBinding
import com.google.android.gms.auth.api.phone.SmsRetriever
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginActivityBinding
    private var loadingDialog: AlertDialog? = null
    private lateinit var otpFields: List<EditText>
    private val PERMISSION_REQUEST_CODE = 1001
    private val SMS_CONSENT_REQUEST = 200

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPhonePermission()

        otpFields = listOf(
            findViewById(R.id.editText1),
            findViewById(R.id.editText2),
            findViewById(R.id.editText3),
            findViewById(R.id.editText4),
            findViewById(R.id.editText5),
            findViewById(R.id.editText6)
        )
//        setupOtpManualInput()
        otpFields.forEach { it.isEnabled = false }

        binding.etEmpCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textLength = s?.length ?: 0

                if (textLength >= 3) {
                    binding.ivSendOtp.visibility = View.VISIBLE


                    if (!binding.ivSendOtp.isActivated) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Click on arrow to get OTP",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.ivSendOtp.isActivated = true
                    }
                } else {
                    binding.ivSendOtp.visibility = View.GONE
                    binding.ivSendOtp.isActivated = false
                }

                // Clear OTP fields and disable
                otpFields.forEach {
                    it.setText("")
                    it.isEnabled = false
                }

                binding.etEmpCode.isEnabled = true
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        binding.ivSendOtp.setOnClickListener {
            val empCode = binding.etEmpCode.text.toString().trim()
            if (empCode.isEmpty()) {
                Toast.makeText(this, "Please enter employee code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtpToEmployee(empCode)
        }

        binding.login.setOnClickListener {
            val empCode = binding.etEmpCode.text.toString().trim()
            val otp = otpFields.joinToString("") { it.text.toString().trim() }

            if (empCode.isEmpty()) {
                Toast.makeText(this, "Please enter employee code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otp.length != 6) {
                Toast.makeText(this, "Please enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(empCode, otp)
        }
        setupOtpManualInput()

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
//        registerReceiver(smsConsentReceiver, intentFilter, Context.RECEIVER_EXPORTED)

    }

    private fun sendOtpToEmployee(empCode: String) {
        showLoadingDialog()

        val request = OtpRequest(emp_code = empCode)

        ApiClient.apiService.sendOtp(request).enqueue(object : Callback<OtpResponse> {
            override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                dismissLoadingDialog()

                // Read the raw error payload ONCE (a ResponseBody can only be consumed once)
                val httpCode = response.code()
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }

                // Full detail of what actually came back (see logcat tag below)
                Log.d(
                    TAG,
                    "sendOtp -> url=${call.request().url} emp_code=$empCode" +
                        " | HTTP $httpCode ${response.message()}" +
                        " | body=${response.body()} errorBody=$errorBody"
                )

                if (response.isSuccessful) {
                    val otpResponse = response.body()
                    if (otpResponse?.status == "success") {
                        Toast.makeText(
                            this@LoginActivity,
                            "OTP sent successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        // Enable OTP input fields
                        otpFields.forEach { it.isEnabled = true }

                        // Disable employee code input
                        binding.etEmpCode.isEnabled = false

                        // Start listening for incoming OTP via SMS Consent
//                        startSmsUserConsent()
                    } else {
                        // Server answered 2xx but rejected the request - show ITS reason, not a guess
                        val reason = otpResponse?.message
                            ?: "status=${otpResponse?.status ?: "null"}"
                        Toast.makeText(
                            this@LoginActivity,
                            "OTP request failed: $reason",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Non-2xx (404 / 403 / 500 ...) - usually a wrong URL or a server/proxy error
                    val detail = errorBody?.take(200)?.takeIf { it.isNotBlank() }
                        ?: response.message()
                    Toast.makeText(
                        this@LoginActivity,
                        "OTP request failed (HTTP $httpCode): $detail",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                dismissLoadingDialog()
                Log.e(TAG, "sendOtp network failure -> url=${call.request().url}", t)
                Toast.makeText(
                    this@LoginActivity,
                    "Network issue. Please check your internet connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun verifyOtp(empCode: String, otp: String) {
        showLoadingDialog()

        val request = VerifyOtpRequest(emp_code = empCode, inputotp = otp)

        ApiClient.apiService.verifyOtp(request).enqueue(object : Callback<VerifyOtpResponse> {
            override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                dismissLoadingDialog()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {

                        if (responseBody.emp_status == "2") {
                            Toast.makeText(
                                this@LoginActivity,
                                "Your account is inactive. Please contact admin.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }

                        // Save basic info
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_EMP_ID, responseBody.emp_id)
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_EMP_NAME, responseBody.name)
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_BRANCH_Code, responseBody.branch_code)
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_BRANCH_NAME, responseBody.branch_name)
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_EMP_CD, responseBody.emp_CD)
                        Constant.saveSharedPreferenceValues(this@LoginActivity, Constant.SHARE_EMP_TOKEN, responseBody.token ?: "")


                        Toast.makeText(this@LoginActivity, "Welcome! ${responseBody.name}", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@LoginActivity, Dashboard::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@LoginActivity, responseBody?.message ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                dismissLoadingDialog()
                Toast.makeText(this@LoginActivity, "Server error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "verifyOtp failed", t)
            }
        })
    }


//    private fun startSmsUserConsent() {
//        val client = SmsRetriever.getClient(this)
//        client.startSmsUserConsent(null) // listen for any sender
//    }
//
//    private val smsConsentReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
//                val extras = intent.extras
//                val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? com.google.android.gms.common.api.Status
//                if (status?.statusCode == com.google.android.gms.common.api.CommonStatusCodes.SUCCESS) {
//                    val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
//                    try {
//                        startActivityForResult(consentIntent!!, SMS_CONSENT_REQUEST)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SMS_CONSENT_REQUEST && resultCode == RESULT_OK && data != null) {
            val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return


            val otp = Regex("\\b\\d{6}\\b").find(message)?.value
            if (!otp.isNullOrEmpty()) {
                otpFields.forEachIndexed { index, editText ->
                    editText.setText(otp[index].toString())
                    editText.isEnabled = true
                }
            }
        }
    }

//    private fun setupOtpManualInput() {
//        for (i in otpFields.indices) {
//            otpFields[i].addTextChangedListener(object : TextWatcher {
//                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                    if (!s.isNullOrEmpty()) {
//                        if (i < otpFields.size - 1) {
//                            otpFields[i + 1].requestFocus()
//                        }
//                    }
//                }
//
//                override fun afterTextChanged(s: Editable?) {}
//            })
//
//            otpFields[i].setOnKeyListener { v, keyCode, event ->
//                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
//                    if (otpFields[i].text.isEmpty() && i > 0) {
//                        otpFields[i - 1].requestFocus()
//                    }
//                }
//                false
//            }
//        }
//    }


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

    private fun checkAndRequestPhonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun setupOtpManualInput() {
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!s.isNullOrEmpty()) {
                        // Move to next field automatically
                        if (i < otpFields.size - 1) {
                            otpFields[i + 1].requestFocus()
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            otpFields[i].setOnKeyListener { _, keyCode, event ->
                // Detect backspace on empty field
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpFields[i].text.isEmpty() && i > 0) {
                        otpFields[i - 1].requestFocus()
                        otpFields[i - 1].setSelection(otpFields[i - 1].text.length)
                    }
                }
                false
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
//            unregisterReceiver(smsConsentReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
