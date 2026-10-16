package com.avs.visitapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.avs.visitapp.databinding.TrainingVideoActivityBinding

class TrainingVideoActivity : AppCompatActivity() {

    private lateinit var binding: TrainingVideoActivityBinding
    private val isAdmin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TrainingVideoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}