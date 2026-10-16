package com.avs.visitapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.visitapp.Adapter.TaskAdapter
import com.avs.visitapp.databinding.ActivityTaskListBinding
import com.avs.visitapp.model.TaskModel
import com.avs.visitapp.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections.list

class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private lateinit var apiService: ApiService

    private var taskAdapter: TaskAdapter? = null
    private var userList: List<User> = emptyList()

    private var assignedByYouList: List<TaskModel> = emptyList()
    private var assignedToYouList: List<TaskModel> = emptyList()

    private var myTaskList: List<TaskModel> = emptyList()



    // Refresh after Add/Update Operation
    private val refreshLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            // Reload data for whichever radio tab is active
            when (binding.radioGroupTaskType.checkedRadioButtonId) {
                binding.radioMyTask.id -> fetchMyTasks()
                binding.radioAssignedByYou.id -> fetchAssignedByYou()
                binding.radioAssignedToYou.id -> fetchAssignedToYou()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = ApiClient.apiService

        // Setup RecyclerView
        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)

        // Add Task Button
//        binding.fabAddTask.setOnClickListener {
//            val intent = Intent(this, AddTaskActivity::class.java)
//            refreshLauncher.launch(intent)
//        }


        binding.fabAddTask.setOnClickListener {

            val intent = Intent(this, AddTaskActivity::class.java)

            when (binding.radioGroupTaskType.checkedRadioButtonId) {

                binding.radioMyTask.id -> {
                    intent.putExtra("mode", "MY_TASK")
                    refreshLauncher.launch(intent)
                }

                binding.radioAssignedByYou.id -> {
                    intent.putExtra("mode", "ASSIGN_TO_OTHERS")
                    refreshLauncher.launch(intent)
                }

                binding.radioAssignedToYou.id -> {
                    Toast.makeText(this, "You cannot add tasks here", Toast.LENGTH_SHORT).show()
                }
            }
        }



        binding.toolbarSearch.setOnClickListener {
            showSearchBar()
        }
        binding.toolbarBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchFilter(s.toString())
            }
        })


        binding.toolbarBack.setOnClickListener {
            val intent = Intent(this, VisitActivity::class.java)
            startActivity(intent)
            finish()
        }



        setupTaskTypeSelector()

        // FIRST: fetch users → THEN load tasks
        fetchUsersThenLoadTasks()
    }


    private fun showSearchBar() {
        binding.toolbarTitle.visibility = View.GONE
        binding.toolbarSearch.visibility = View.GONE
        binding.etSearchBar.visibility = View.VISIBLE
        binding.etSearchBar.requestFocus()

        // OPEN KEYBOARD
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearchBar, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSearchBar() {
        binding.toolbarTitle.visibility = View.VISIBLE
        binding.toolbarSearch.visibility = View.VISIBLE
        binding.etSearchBar.visibility = View.GONE
        binding.etSearchBar.setText("")

        applySearchFilter("")  // reset filtered list

        // HIDE KEYBOARD
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchBar.windowToken, 0)
    }


    private fun applySearchFilter(query: String) {

        val search = query.trim().lowercase()
        val filteredList: List<TaskModel>
        val mode: String

        when (binding.radioGroupTaskType.checkedRadioButtonId) {

            binding.radioAssignedByYou.id -> {
                mode = "ASSIGNED_BY_YOU"
                filteredList = assignedByYouList.filter { task ->
                    task.AssignedToName?.lowercase()?.contains(search) == true ||
                            task.BranchName?.lowercase()?.contains(search) == true ||
                            task.TaskDescription.lowercase().contains(search) ||
                            task.OtherRemarks?.lowercase()?.contains(search) == true ||
                            task.CompletionDescription?.lowercase()?.contains(search) == true
                }
            }

            binding.radioMyTask.id -> {
                mode = "MY_TASK"
                filteredList = myTaskList.filter { task ->
                    task.TaskDescription.lowercase().contains(search)
                            || task.OtherRemarks?.lowercase()?.contains(search) == true
                }
            }

            binding.radioAssignedToYou.id -> {
                mode = "ASSIGNED_TO_YOU"
                filteredList = assignedToYouList.filter { task ->
                    task.AssignedToName?.lowercase()?.contains(search) == true ||
                            task.BranchName?.lowercase()?.contains(search) == true ||
                            task.TaskDescription.lowercase().contains(search) ||
                            task.OtherRemarks?.lowercase()?.contains(search) == true ||
                            task.CompletionDescription?.lowercase()?.contains(search) == true
                }
            }

            else -> {
                mode = "ASSIGNED_BY_YOU"
                filteredList = assignedByYouList
            }
        }

        if (filteredList.isEmpty()) showNoTaskMessage()
        else hideNoTaskMessage()

        updateRecycler(filteredList, mode)
    }



    // -------------------------------------------------------------------
    // 1️⃣ FETCH USERS FIRST
    // -------------------------------------------------------------------
    private fun fetchUsersThenLoadTasks() {
        apiService.getUsersWithBankDetails().enqueue(object : Callback<GetUsersResponse> {
            override fun onResponse(
                call: Call<GetUsersResponse>,
                response: Response<GetUsersResponse>
            ) {
                if (response.isSuccessful) {
                    userList = response.body()?.Data ?: emptyList()
                } else {
                    userList = emptyList()
                }

                // After loading users, load the selected tab
                when (binding.radioGroupTaskType.checkedRadioButtonId) {
                    binding.radioAssignedByYou.id -> fetchAssignedByYou()
                    binding.radioAssignedToYou.id -> fetchAssignedToYou()
                }
            }

            override fun onFailure(call: Call<GetUsersResponse>, t: Throwable) {
                userList = emptyList()

                when (binding.radioGroupTaskType.checkedRadioButtonId) {
                    binding.radioAssignedByYou.id -> fetchAssignedByYou()
                    binding.radioAssignedToYou.id -> fetchAssignedToYou()
                }
            }
        })
    }

    // -------------------------------------------------------------------
    // 2️⃣ FETCH TASKS ASSIGNED BY YOU  (Current API)
    // -------------------------------------------------------------------
    private fun fetchAssignedByYou() {
        val staffId =
            Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0

        apiService.getTasks(staffId).enqueue(object : Callback<List<TaskModel>> {
            override fun onResponse(
                call: Call<List<TaskModel>>,
                response: Response<List<TaskModel>>
            ) {
                if (response.isSuccessful) {
                    val tasks = response.body() ?: emptyList()

                    // Merge username + branch
                    for (task in tasks) {
                        val user = userList.find { it.PermissionNo == task.AssignedToEmpNo }
                        task.AssignedToName = user?.USERNAME ?: "Unknown"
                        task.BranchName = user?.BRANCHNAME ?: "Unknown"
                    }

                    assignedByYouList = tasks
                    applySearchFilter(binding.etSearchBar.text.toString())


                }
            }

            override fun onFailure(call: Call<List<TaskModel>>, t: Throwable) {
                Toast.makeText(this@TaskListActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // -------------------------------------------------------------------
    // 3️⃣ FETCH TASKS ASSIGNED TO YOU (API will be added later)
    // -------------------------------------------------------------------
    private fun fetchAssignedToYou() {

        val empNo =
            Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0

        apiService.getAssignedTasks(empNo).enqueue(object : Callback<List<TaskModel>> {
            override fun onResponse(
                call: Call<List<TaskModel>>,
                response: Response<List<TaskModel>>
            ) {

                if (response.isSuccessful) {

                    val list = response.body() ?: emptyList()

                    if (list.isEmpty()) {
                        showNoTaskMessage()
                        return
                    }

                    // Merge username + branch
                    for (task in list) {
                        val user = userList.find { it.PermissionNo == task.AssignedToEmpNo }
                        task.AssignedToName = user?.USERNAME ?: "Unknown"
                        task.BranchName = user?.BRANCHNAME ?: "Unknown"
                    }

                    hideNoTaskMessage()
                    assignedToYouList = list
                    applySearchFilter(binding.etSearchBar.text.toString())


                } else {
                    showNoTaskMessage()
                }
            }

            override fun onFailure(call: Call<List<TaskModel>>, t: Throwable) {
                showNoTaskMessage()
            }
        })
    }


    private fun showNoTaskMessage() {
        binding.recyclerTasks.visibility = View.GONE
        binding.tvNoTasks.visibility = View.VISIBLE
    }

    private fun hideNoTaskMessage() {
        binding.recyclerTasks.visibility = View.VISIBLE
        binding.tvNoTasks.visibility = View.GONE
    }



    // -------------------------------------------------------------------
    // RADIO BUTTON SELECTOR
    // -------------------------------------------------------------------
    private fun setupTaskTypeSelector() {
//        binding.radioGroupTaskType.setOnCheckedChangeListener { _, checkedId ->
//            when (checkedId) {
//                binding.radioAssignedByYou.id -> fetchAssignedByYou()
//                binding.radioAssignedToYou.id -> fetchAssignedToYou()
//            }
//        }

        binding.radioGroupTaskType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioAssignedByYou.id -> fetchAssignedByYou()
                binding.radioMyTask.id -> fetchMyTasks()
                binding.radioAssignedToYou.id -> fetchAssignedToYou()
            }
        }

    }

    private fun fetchMyTasks() {

        val empNo =
            Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0

        apiService.getMyTasks(empNo).enqueue(object : Callback<List<TaskModel>> {
            override fun onResponse(
                call: Call<List<TaskModel>>,
                response: Response<List<TaskModel>>
            ) {
                if (response.isSuccessful) {

                    myTaskList = response.body() ?: emptyList()

                    val currentUser = userList.find { it.PermissionNo == empNo }

                    for (task in myTaskList) {
                        task.AssignedToName = currentUser?.USERNAME ?: "You"
                        task.BranchName = currentUser?.BRANCHNAME ?: "Unknown"
                        task.Zone = currentUser?.Zone ?: "Unknown"
                    }

                    if (myTaskList.isEmpty()) showNoTaskMessage()
                    else hideNoTaskMessage()

                    updateRecycler(myTaskList, "MY_TASK")


                } else {
                    showNoTaskMessage()
                }
            }

            override fun onFailure(call: Call<List<TaskModel>>, t: Throwable) {
                showNoTaskMessage()
            }
        })
    }




    // -------------------------------------------------------------------
    // UPDATE RECYCLER ADAPTER
    // -------------------------------------------------------------------
    private fun updateRecycler(tasks: List<TaskModel>, mode: String) {

        if (taskAdapter == null) {

            taskAdapter = TaskAdapter(tasks, mode) { selectedTask ->
                openTaskInEdit(selectedTask)
            }

            binding.recyclerTasks.adapter = taskAdapter

        } else {
            taskAdapter!!.updateTasks(tasks)
        }
    }


    // -------------------------------------------------------------------
    // OPEN TASK IN EDIT MODE
    // -------------------------------------------------------------------
    private fun openTaskInEdit(task: TaskModel) {
        val intent = Intent(this, AddTaskActivity::class.java)
        intent.putExtra("task", task)

        // 🔥 Also send user list so update mode shows LoginCode
        intent.putExtra("user_list_loaded", true)

        refreshLauncher.launch(intent)
    }





}





//package com.avs.visitapp
//
//import android.app.Activity
//import android.content.Intent
//import android.os.Bundle
//import android.widget.RadioGroup
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.avs.visitapp.Adapter.TaskAdapter
//import com.avs.visitapp.databinding.ActivityTaskListBinding
//import com.avs.visitapp.model.TaskModel
//import retrofit2.*
//import retrofit2.converter.gson.GsonConverterFactory
//
//class TaskListActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityTaskListBinding
//    private lateinit var apiService: ApiService
//    private var taskAdapter: TaskAdapter? = null
//
//    // Refresh launcher after Add/Edit Task
//    private val refreshLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//
//            val selectedStatus = when (binding.radioGroupTaskStatus.checkedRadioButtonId) {
//                binding.radioPending.id -> "Pending"
//                binding.radioInProgress.id -> "In Progress"
//                binding.radioCompleted.id -> "Completed"
//                else -> "Pending"
//            }
//
//            fetchTasksByStatus(selectedStatus)
//        }
//    }
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityTaskListBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // FAB - Add Task
//        binding.fabAddTask.setOnClickListener {
//            val intent = Intent(this, AddTaskActivity::class.java)
//            refreshLauncher.launch(intent)
//        }
//
//        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
//
//        // Retrofit setup
//        val retrofit = Retrofit.Builder()
//            .baseUrl("http://43.239.110.100:80/Trackme_payroll/api/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        apiService = retrofit.create(ApiService::class.java)
//
//        setupRadioButtonListener()
//
//        // Load default (Pending)
//        fetchTasksByStatus("Pending")
//    }
//
//    private fun setupRadioButtonListener() {
//        binding.radioGroupTaskStatus.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
//            val status = when (checkedId) {
//                binding.radioPending.id -> "Pending"
//                binding.radioInProgress.id -> "In Progress"
//                binding.radioCompleted.id -> "Completed"
//                else -> "Pending"
//            }
//            fetchTasksByStatus(status)
//        }
//    }
//
//    private fun getStageCode(status: String): String {
//        return when (status.lowercase()) {
//            "pending" -> "1001"
//            "in progress" -> "1002"
//            "completed" -> "1003"
//            else -> "1001"
//        }
//    }
//
//    private fun fetchTasksByStatus(status: String) {
//
//        val stageCode = getStageCode(status)
//        val staffId = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD) ?: "0"
//        val brcd = Constant.getSharedPreferenceValue(this, Constant.SHARE_BRANCH_Code) ?: "0"
//
//        apiService.getSpecificTasks(staffId, stageCode, brcd)
//            .enqueue(object : Callback<TaskResponse> {
//                override fun onResponse(call: Call<TaskResponse>, response: Response<TaskResponse>) {
//                    if (response.isSuccessful) {
//                        val body = response.body()
//                        val tasks = body?.Tasks ?: emptyList()
//                        updateRecycler(tasks)
//                    } else {
//                        Toast.makeText(this@TaskListActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<TaskResponse>, t: Throwable) {
//                    Toast.makeText(this@TaskListActivity, "Failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
//                }
//            })
//
//    }
//
//    private fun updateRecycler(tasks: List<TaskModel>) {
//        if (taskAdapter == null) {
//            taskAdapter = TaskAdapter(tasks) { selectedTask ->
//                openEditTask(selectedTask)
//            }
//            binding.recyclerTasks.adapter = taskAdapter
//        } else {
//            taskAdapter!!.updateTasks(tasks)
//        }
//    }
//
//    private fun openEditTask(task: TaskModel) {
//        val intent = Intent(this, AddTaskActivity::class.java)
//        intent.putExtra("editTask", task)
//        refreshLauncher.launch(intent)
//    }
//}
//


//package com.avs.visitapp
//
//import android.app.Activity
//import android.content.Intent
//import android.os.Bundle
//import android.widget.ArrayAdapter
//import android.widget.Button
//import android.widget.RadioGroup
//import android.widget.Spinner
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.avs.visitapp.Adapter.TaskAdapter
//import com.avs.visitapp.databinding.ActivityTaskListBinding
//import com.avs.visitapp.model.TaskModel
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import retrofit2.*
//import retrofit2.converter.gson.GsonConverterFactory
//
//class TaskListActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityTaskListBinding
//    private lateinit var apiService: ApiService
//    private var staffId: Int = 0
//    private var allTasks: List<TaskModel> = emptyList()
//    private var taskAdapter: TaskAdapter? = null
//
//    // ---- START: NEW CODE FOR REFRESHING ----
//    // 1. Create a launcher that will handle the result from AddTaskActivity
//    private val addTaskLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        // This block is executed when AddTaskActivity finishes
//        if (result.resultCode == Activity.RESULT_OK) {
//            // The task was added successfully. Refresh the list.
//            Toast.makeText(this, "Refreshing task list...", Toast.LENGTH_SHORT).show()
//            fetchTasks(staffId)
//        }
//    }
//    // ---- END: NEW CODE FOR REFRESHING ----
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityTaskListBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Access the FAB using the binding object
//        binding.fabAddTask.setOnClickListener {
//            val intent = Intent(this, AddTaskActivity::class.java)
//            // 2. Use the new launcher to start the activity
//            addTaskLauncher.launch(intent)
//        }
//
//        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
//
//        // Setup Retrofit
//        val retrofit = Retrofit.Builder()
//            .baseUrl("http://43.239.110.100:80/Trackme_payroll/api/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//        apiService = retrofit.create(ApiService::class.java)
//
//        // Get staffId from login/session
//        staffId = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//
//        setupRadioGroupListener()
//        fetchTasks(staffId)
//    }
//
//    private fun setupRadioGroupListener() {
//        binding.radioGroupTaskStatus.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
//            val status = when (checkedId) {
//                binding.radioPending.id -> "Pending"
//                binding.radioInProgress.id -> "In Progress"
//                binding.radioCompleted.id -> "Completed"
//                else -> "Pending" // Default to Pending if no other is selected
//            }
//            fetchTasksByStatus(status)
//
//        }
//    }
//
//    private fun fetchTasks(staffId: Int) {
//        apiService.getTasks(staffId).enqueue(object : Callback<List<TaskModel>> {
//            override fun onResponse(call: Call<List<TaskModel>>, response: Response<List<TaskModel>>) {
//                if (response.isSuccessful) {
//                    allTasks = response.body() ?: emptyList()
//                    // Initially filter tasks based on the default selected radio button (Pending)
//                    fetchTasksByStatus("Pending")
//
//                } else {
//                    Toast.makeText(this@TaskListActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<List<TaskModel>>, t: Throwable) {
//                Toast.makeText(this@TaskListActivity, "Failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
//            }
//        })
//    }
//
////    private fun filterTasksByStatus(status: String) {
////        val filteredList = allTasks.filter { it.Status == status }
////        if (taskAdapter == null) {
////            taskAdapter = TaskAdapter(filteredList)
////            binding.recyclerTasks.adapter = taskAdapter
////        } else {
////            taskAdapter?.updateTasks(filteredList)
////        }
////    }
//
//    private fun getStageCode(status: String): String {
//        return when (status.lowercase()) {
//            "pending" -> "1001"
//            "in progress" -> "1002"
//            "completed" -> "1003"
//            else -> "1001"
//        }
//    }
//
//
//    private fun fetchTasksByStatus(status: String) {
//
//        val stageCode = getStageCode(status)
//
//        val staffId = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD) ?: "0"
//        val brcd = Constant.getSharedPreferenceValue(this, Constant.SHARE_BRANCH_Code) ?: "0"
//
//        apiService.getSpecificTasks(staffId, stageCode, brcd)
//            .enqueue(object : Callback<List<TaskModel>> {
//                override fun onResponse(call: Call<List<TaskModel>>, response: Response<List<TaskModel>>) {
//                    if (response.isSuccessful) {
//                        val tasks = response.body() ?: emptyList()
//                        updateRecycler(tasks)
//                    } else {
//                        Toast.makeText(this@TaskListActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<List<TaskModel>>, t: Throwable) {
//                    Toast.makeText(this@TaskListActivity, "Failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
//                }
//            })
//    }
//
//    private fun updateRecycler(tasks: List<TaskModel>) {
//        if (taskAdapter == null) {
//            taskAdapter = TaskAdapter(tasks) { selectedTask ->
//                openEditTask(selectedTask)
//            }
//            binding.recyclerTasks.adapter = taskAdapter
//        } else {
//            taskAdapter!!.updateTasks(tasks)
//        }
//    }
//
//    private fun openEditTask(task: TaskModel) {
//        val intent = Intent(this, AddTaskActivity::class.java)
//        intent.putExtra("editTask", task)
//        startActivity(intent)
//    }
//
//
//
//
//
//    private fun showEditTaskDialog(task: TaskModel) {
//        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_edit_task, null)
//        val bottomSheetDialog = BottomSheetDialog(this)
//        bottomSheetDialog.setContentView(dialogView)
//
//        val prioritySpinner = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
//        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinnerStatus)
//        val saveBtn = dialogView.findViewById<Button>(R.id.btnSave)
//
//        // Preselect current values
//        val priorityAdapter = ArrayAdapter.createFromResource(
//            this, R.array.task_priority_list, android.R.layout.simple_spinner_item
//        )
//        prioritySpinner.adapter = priorityAdapter
//        prioritySpinner.setSelection(priorityAdapter.getPosition(task.Priority))
//
//        val statusAdapter = ArrayAdapter.createFromResource(
//            this, R.array.task_status_list, android.R.layout.simple_spinner_item
//        )
//        statusSpinner.adapter = statusAdapter
//        statusSpinner.setSelection(statusAdapter.getPosition(task.Status))
//
//        saveBtn.setOnClickListener {
//            val newPriority = prioritySpinner.selectedItem.toString()
//            val newStatus = statusSpinner.selectedItem.toString()
//
//            // Example API call to update task
////            apiService.updateTask(task.TaskId, newPriority, newStatus).enqueue(object : Callback<Void> {
////                override fun onResponse(call: Call<Void>, response: Response<Void>) {
////                    Toast.makeText(this@TaskListActivity, "Task updated", Toast.LENGTH_SHORT).show()
////                    fetchTasks(staffId)
////                    bottomSheetDialog.dismiss()
////                }
////
////                override fun onFailure(call: Call<Void>, t: Throwable) {
////                    Toast.makeText(this@TaskListActivity, "Failed: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
////                }
////            })
//        }
//
//        bottomSheetDialog.show()
//    }
//
//
//}