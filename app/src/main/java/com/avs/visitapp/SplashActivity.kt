package com.avs.visitapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import com.avs.visitapp.databinding.SplashActivityBinding
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import java.io.IOException


class SplashActivity : AppCompatActivity() {

    private lateinit var binding: SplashActivityBinding
    private lateinit var mContext: Context

    private val splashTimeOut: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        Handler(Looper.getMainLooper()).postDelayed({
            checkUpdate { goNext() }
        }, splashTimeOut)
    }

    private fun goNext() {
        val userId = Constant.getSharedPreference(mContext, "SHARE_EMP_ID", "")

        if (userId.isNullOrEmpty()) {
            // Not logged in
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Already logged in
            Log.d("USER_ID", userId)
            startActivity(Intent(this, Dashboard::class.java))
        }
        finish()
    }

    /**
     * Silently checks the latest release for a newer version.
     * Always invokes [onDone] on the UI thread — on success (after the user
     * taps Update or not) and on any failure/network error. Never blocks or crashes.
     */
    private fun checkUpdate(onDone: () -> Unit) {
        val url = "https://github.com/OWNER/visitapp-releases/releases/latest/download/version.json"

        val request = Request.Builder().url(url).build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Offline / any network failure → continue silently
                runOnUiThread { onDone() }
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                try {
                    if (!response.isSuccessful) {
                        runOnUiThread { onDone() }
                        return
                    }

                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        runOnUiThread { onDone() }
                        return
                    }

                    val json = JsonParser.parseString(body).asJsonObject
                    val remoteBuild = json.get("build").asLong
                    val version = json.get("version").asString
                    val notes = json.get("notes").asString
                    val updateUrl = json.get("url").asString

                    val pInfo = packageManager.getPackageInfo(packageName, 0)
                    val currentBuild = PackageInfoCompat.getLongVersionCode(pInfo)

                    if (remoteBuild > currentBuild) {
                        runOnUiThread {
                            AlertDialog.Builder(this@SplashActivity)
                                .setTitle("Update $version available")
                                .setMessage(notes)
                                .setCancelable(false)
                                .setPositiveButton("Update") { _, _ ->
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                                    } catch (_: Exception) {
                                    }
                                    finish()
                                }
                                .show()
                        }
                    } else {
                        // Already up to date
                        runOnUiThread { onDone() }
                    }
                } catch (e: Exception) {
                    // Bad JSON / any parse error → continue silently
                    Log.e("UpdateCheck", "Update check failed: ${e.message}")
                    runOnUiThread { onDone() }
                }
            }
        })
    }
}
