package com.avs.visitapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.avs.visitapp.databinding.VisitActivityBinding

class VisitActivity : AppCompatActivity() {

    private lateinit var binding: VisitActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VisitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        // Toolbar
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        binding.toolbar.setNavigationOnClickListener { finish() }


        binding.toolbarBack.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finish()
        }

//        binding.addvisit.setOnClickListener {
//            val intent = Intent(this, AddVisitActivity::class.java)
//            startActivity(intent)
//        }

//        binding.Checkvisit.setOnClickListener {
//            val intent = Intent(this, VisitReportActivity::class.java)
//            startActivity(intent)
//        }

        binding.DelinquencyVisit.setOnClickListener {
            val intent = Intent(this, DelinquencyVisitActivity::class.java)
            startActivity(intent)
        }

        binding.MarketingVisit.setOnClickListener {
            val intent = Intent(this, MarketingVisitActivity::class.java)
            startActivity(intent)
        }

        binding.MonitoringVisit.setOnClickListener {
            val intent = Intent(this, MonitoringVisitActivity::class.java)
            startActivity(intent)
        }

        binding.LegalCaseVisit.setOnClickListener {
            val intent = Intent(this, LegalCaseVisitActivity::class.java)
            startActivity(intent)
        }

//        binding.BankVisit.setOnClickListener {
//            val intent = Intent(this, BankVisitActivity::class.java)
//            startActivity(intent)
//        }

//        binding.addTask.setOnClickListener { showAddTaskDialog() }
//        binding.addTask.setOnClickListener {
//            val intent = Intent(this, AddTaskActivity::class.java)
//            startActivity(intent)
//        }

        binding.seeTasks.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            startActivity(intent)
        }

    }
}