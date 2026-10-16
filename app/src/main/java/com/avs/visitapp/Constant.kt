package com.avs.visitapp

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import java.util.Arrays



object Constant {
    // SharedPreference file name
    const val SHARE_PREF_NAME: String = "MyAppPrefs"

    // Keys
    const val IS_ACTIVE: String = "IS_ACTIVE"
    const val SHARE_EMP_ID: String = "SHARE_EMP_ID"
    const val SHARE_EMP_NAME: String = "SHARE_EMP_NAME"
    const val SHARE_BRANCH_NAME: String = "SHARE_BRANCH_NAME"
    const val SHARE_EMP_CD: String = "SHARE_EMP_CD"
    const val SHARE_BRANCH_Code: String = "SHARE_BRANCH_CODE"
    const val SHARE_EMP_TOKEN: String = "SHARE_EMP_TOKEN"
    const val SHARE_EMP_LATITUDE: String = "SHARE_EMP_LATITUDE"
    const val SHARE_EMP_LONGITUDE: String = "SHARE_EMP_LONGITUDE"


    private val gson = Gson()


    // Save string
    fun saveSharedPreferenceValues(context: Context, key: String?, value: String?) {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    // Get string
    fun getSharedPreference(context: Context, key: String?, defaultValue: String?): String? {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        return if (prefs.getString(key, defaultValue) != null) prefs.getString(
            key,
            defaultValue
        ) else defaultValue
    }

    // Save object as JSON
    fun saveSharedPreferenceValues(context: Context, key: String?, value: Any?) {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(value)
        editor.putString(key, json)
        editor.apply()
    }

    // Save List<String> as JSON
    fun saveSharedPreferenceValues(context: Context, key: String?, values: List<String?>?) {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(values)
        editor.putString(key, json)
        editor.apply()
    }

    // Get generic object from JSON
    fun <T> getSharedPreferenceObject(context: Context, key: String?, clazz: Class<T>): T? {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null)
        return if (json != null) gson.fromJson(json, clazz) else null
    }

    // Get List<String> from JSON
    fun getStringListFromSharedPreference(context: Context, key: String?): List<String>? {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null)
        if (json != null) {
            val stringArray = gson.fromJson(
                json,
                Array<String>::class.java
            )
            return Arrays.asList(*stringArray)
        }
        return null
    }

    // Get App version code
    fun getAppVersionCode(context: Context): Int {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    // Simple getter for String value (shortcut)
    fun getSharedPreferenceValue(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    //save the in & out selfie for attendance
    const val SHARE_EMP_SELFIE_IN = "SHARE_EMP_SELFIE_IN"
    const val SHARE_EMP_SELFIE_OUT = "SHARE_EMP_SELFIE_OUT"

    const val SHARE_OFFICE_LATITUDE = "share_office_latitude"
    const val SHARE_OFFICE_LONGITUDE = "share_office_longitude"


}
