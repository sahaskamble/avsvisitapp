package com.avs.visitapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.avs.visitapp.databinding.HelpdeskActivityBinding

class HelpdeskActivity : AppCompatActivity() {

    private lateinit var binding: HelpdeskActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HelpdeskActivityBinding.inflate(layoutInflater)
    }
}
