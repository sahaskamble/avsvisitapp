package com.avs.visitapp.Adapter

import android.text.Editable
import android.text.TextWatcher


class SimpleTextWatcher(private val onTextChanged: () -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
        onTextChanged()
    }
}