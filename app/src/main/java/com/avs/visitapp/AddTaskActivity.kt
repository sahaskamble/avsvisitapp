package com.avs.visitapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.avs.visitapp.databinding.ActivityAddTaskBinding
import com.avs.visitapp.model.MyTaskRequest
import com.avs.visitapp.model.TaskInsertRequest
import com.avs.visitapp.model.TaskModel
import com.avs.visitapp.model.User
import okhttp3.ResponseBody
import retrofit2.*
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private lateinit var apiService: ApiService

    private var selectedEmpNo: Int = 0           // AssignedToEmpNo
    private var selectedBrcd: Int = 0            // Bank Code
    private var selectedZone: String = ""        // Zone

    private var isUpdateMode = false
    private var taskToUpdate: TaskModel? = null

    private var assignUserList: List<User> = emptyList()

    private var mode: String = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        apiService = ApiClient.apiService

        mode = intent.getStringExtra("mode") ?: "ASSIGN_TO_OTHERS"

        if (mode == "MY_TASK") {
            enterMyTaskMode()
        }


        // CHECK IF TASK CLICKED FROM LIST
        taskToUpdate = intent.getSerializableExtra("task") as? TaskModel
        isUpdateMode = taskToUpdate != null

        if (isUpdateMode) {
            fetchUsers()   // load user list first
        } else {
            enterInsertMode()
        }


        val empNo = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
        binding.empNo.setText(empNo.toString())

        if (!isUpdateMode) {
            fetchUsers() // only for insertion
        }

        binding.taskDate.setOnClickListener { openDatePicker() }

        binding.btnSaveTask.setOnClickListener {
            if (isUpdateMode) {
                updateTask()
            } else {
                saveTask(empNo)
            }
        }




    }

    private fun enterInsertMode() {

        isUpdateMode = false

        // hide update-only fields
        binding.dailyWrkCompDetailsLayout.visibility = View.GONE
        binding.remarkLayout.visibility = View.GONE

        // editable fields
        binding.workAllocation.isEnabled = true
        binding.taskDate.isEnabled = true

        // button text
        binding.btnSaveTask.text = "Save Task"
    }

    private fun enterUpdateMode(task: TaskModel) {

        fetchUsersForName(task.AssignedToEmpNo)

        val currentUser = assignUserList.find { it.PermissionNo == task.EmpNo }




        isUpdateMode = true

        // SHOW update-only fields
        binding.dailyWrkCompDetailsLayout.visibility = View.VISIBLE
        binding.remarkLayout.visibility = View.VISIBLE

        // Prefill read-only fields
//        binding.empNo.setText(task.EmpNo.toString())
        binding.empNo.setText(currentUser?.LOGINCODE ?: task.EmpNo.toString())
        binding.userName.setText(task.AssignedToEmpNo.toString()) // or resolved name if needed
        binding.branch.setText(task.BRCD.toString())
        binding.zone.setText(task.Zone ?: "")
        binding.workAllocation.setText(task.TaskDescription ?: "")
        binding.taskDate.setText(task.TaskDate.take(10))

        // Make all 6 fields read-only
        binding.empNo.isEnabled = false
        binding.userName.isEnabled = false
        binding.branch.isEnabled = false
        binding.zone.isEnabled = false
        binding.workAllocation.isEnabled = false
        binding.taskDate.isEnabled = false

        // Change button
        binding.btnSaveTask.text = "Update Task"
    }

    private fun fetchUsersForName(permissionNo: Int) {
        apiService.getUsersWithBankDetails().enqueue(object : Callback<GetUsersResponse> {
            override fun onResponse(call: Call<GetUsersResponse>, response: Response<GetUsersResponse>) {
                val users = response.body()?.Data ?: return
                val user = users.find { it.PermissionNo == permissionNo }
                binding.userName.setText(user?.USERNAME ?: "Unknown")
            }

            override fun onFailure(call: Call<GetUsersResponse>, t: Throwable) {}
        })
    }



    private fun updateTask() {

        val task = taskToUpdate ?: return

        val completion = binding.dailyWrkCompDetails.text.toString().trim()
        val remarks = binding.remarks.text.toString().trim()

        if (completion.isEmpty()) {
            Toast.makeText(this, "Enter work completion details", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.updateTask(task.TaskID, completion, remarks)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddTaskActivity, "Task Updated", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddTaskActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@AddTaskActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            })

    }

//    private fun fetchUsers() {
//
//        val assignDropdown = binding.userName as AutoCompleteTextView
//
//        apiService.getUsersWithBankDetails().enqueue(object : Callback<GetUsersResponse> {
//            override fun onResponse(call: Call<GetUsersResponse>, response: Response<GetUsersResponse>) {
//
//                if (response.isSuccessful && response.body()?.Status == "Success") {
//
//                    assignUserList = response.body()?.Data ?: emptyList()
//
//                    setupUserDropdown(assignUserList)
//                }
//            }
//
//            override fun onFailure(call: Call<GetUsersResponse>, t: Throwable) {}
//        })
//    }

    private fun fetchUsers() {

        apiService.getUsersWithBankDetails().enqueue(object : Callback<GetUsersResponse> {

            override fun onResponse(
                call: Call<GetUsersResponse>,
                response: Response<GetUsersResponse>
            ) {
                if (response.isSuccessful && response.body()?.Status == "Success") {

                    assignUserList = response.body()?.Data ?: emptyList()

                    // Setup dropdown (for Assign-To mode)
                    setupUserDropdown(assignUserList)

                    // 🔥 Show LOGINCODE instead of EmpNo for Assign-By-You mode
                    if (mode == "ASSIGN_TO_OTHERS" && !isUpdateMode) {
                        val empNo = Constant.getSharedPreferenceValue(this@AddTaskActivity, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
                        val currentUser = assignUserList.find { it.PermissionNo == empNo }
                        binding.empNo.setText(currentUser?.LOGINCODE ?: empNo.toString())
                    }


                    // 🔥 If My Task mode → autofill after user list loads
                    if (mode == "MY_TASK") {
                        autoFillMyTaskUser()
                    }

                    // 🔥 VERY IMPORTANT: Load Update UI AFTER users are loaded
                    if (isUpdateMode && taskToUpdate != null) {
                        enterUpdateMode(taskToUpdate!!)
                    }
                }
            }

            override fun onFailure(call: Call<GetUsersResponse>, t: Throwable) {}
        })
    }

    private fun autoFillMyTaskUser() {

        val empNo = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0

        val user = assignUserList.find { it.PermissionNo == empNo }

        if (user != null) {
//            binding.empNo.setText(empNo.toString())
            binding.empNo.setText(user.LOGINCODE ?: empNo.toString())
            binding.branch.setText(user.BRANCHNAME ?: "")
            binding.zone.setText(user.Zone ?: "")
        } else {
            Log.e("MY_TASK", "User not found in list for PermissionNo = $empNo")
        }
    }




    private fun setupUserDropdown(list: List<User>) {

        val assignDropdown = binding.userName

        val userNames = list.map { it.USERNAME ?: "" }

        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown,
            userNames
        )

        assignDropdown.setAdapter(adapter)

        // WHITE DROPDOWN BACKGROUND
        assignDropdown.setDropDownBackgroundDrawable(
            ContextCompat.getDrawable(this, R.color.white)
        )

        // SHOW DROPDOWN IMMEDIATELY ON CLICK
        assignDropdown.setOnClickListener { assignDropdown.showDropDown() }
        assignDropdown.setOnTouchListener { _, _ ->
            assignDropdown.showDropDown()
            false
        }

        // REAL-TIME SEARCH
        assignDropdown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val search = s.toString().trim()

                val filtered = list.filter {
                    it.USERNAME?.contains(search, ignoreCase = true) == true
                }

                val filteredNames = filtered.map { it.USERNAME ?: "" }

                val newAdapter = ArrayAdapter(
                    this@AddTaskActivity,
                    R.layout.item_dropdown,
                    filteredNames
                )

                assignDropdown.setAdapter(newAdapter)

                // White background again
                assignDropdown.setDropDownBackgroundDrawable(
                    ContextCompat.getDrawable(this@AddTaskActivity, R.color.white)
                )

                assignDropdown.showDropDown()
            }
        })

//        // ON ITEM SELECT
//        assignDropdown.setOnItemClickListener { _, _, position, _ ->
//
//            val selectedName = assignDropdown.text.toString()
//            val user = list.find { it.USERNAME == selectedName }
//
//            user?.let {
//                selectedEmpNo = it.PermissionNo
//                selectedBrcd = it.BRCD ?: 0
//                selectedZone = it.Zone ?: ""
//
//                binding.branch.setText(user.BRANCHNAME ?: "")
//                binding.zone.setText(user.Zone ?: "")
//                binding.empNo.setText(user.LOGINCODE ?: user.PermissionNo.toString())
//
//            }
//        }


        assignDropdown.setOnItemClickListener { _, _, position, _ ->

            val selectedName = assignDropdown.text.toString()
            val user = list.find { it.USERNAME == selectedName }

            user?.let {
                selectedEmpNo = it.PermissionNo
                selectedBrcd = it.BRCD ?: 0
                selectedZone = it.Zone ?: ""

                binding.branch.setText(it.BRANCHNAME ?: "")
                binding.zone.setText(it.Zone ?: "")

                // ❌ DO NOT TOUCH EmpNo here
                // EmpNo must remain logged-in user's number
            }
        }

    }




    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)

                val formatted = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.getDefault()
                ).format(selectedDate.time)

                binding.taskDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun saveTask(empNo: Int) {
        val taskDescription = binding.workAllocation.text.toString().trim()
        val taskDate = binding.taskDate.text.toString().trim()

        // MY TASK MODE
        if (mode == "MY_TASK") {
            saveMyTask(empNo)
            return
        }

        // NORMAL TASK MODE
        if (taskDescription.isEmpty() || selectedEmpNo == 0 || taskDate.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault()
        ).format(Date())

        val request = TaskInsertRequest(
            EmpNo = empNo,
            AssignedToEmpNo = selectedEmpNo,
            TaskDescription = taskDescription,
            TaskDate = taskDate,
            CompletionDescription = "",
            OtherRemarks = "",
            Stage = "1001",
            Status = "Pending",
            BRCD = selectedBrcd,
            Zone = selectedZone
        )

        val call = apiService.insertTaskNew(request)

        println("🔥 FULL API URL → " + call.request().url)
        println("🔥 REQUEST BODY → " + request)

        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddTaskActivity, "Task Saved Successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddTaskActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@AddTaskActivity, "Failed: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun enterMyTaskMode() {

        // hide user dropdown
        binding.userNameLayout.visibility = View.GONE

        // disable fields
        binding.empNo.isEnabled = false
        binding.branch.isEnabled = false
        binding.zone.isEnabled = false

        // enable only these two
        binding.workAllocation.isEnabled = true
        binding.taskDate.isEnabled = true

        // Change label only for My Task
        binding.workAllocationLayout.hint = "Work Description"

    }

    private fun saveMyTask(empNo: Int) {

        val taskDescription = binding.workAllocation.text.toString().trim()
        val taskDate = binding.taskDate.text.toString().trim()
        val zone = binding.zone.text.toString()
        val branch = binding.branch.text.toString()

        if (taskDescription.isEmpty() || taskDate.isEmpty()) {
            Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

//        val request = MyTaskRequest(
//            EmpNo = empNo,
//            TaskDescription = taskDescription,
//            TaskDate = taskDate,
//            BRCD = 0,
//            Zone = zone,
//            Status = "Pending",
//            CreatedDate = currentDate
//        )

        val request = MyTaskRequest(
            EmpNo = empNo,
            AssignedToEmpNo = empNo,       // 🔥 SAME AS EMPLOYEE'S PERMISSION NO
            TaskDescription = taskDescription,
            TaskDate = taskDate,
            CompletionDescription = "",
            OtherRemarks = "",
            BRCD = 0,
            Zone = zone,
            Status = "Pending",
            CreatedDate = currentDate
        )


        apiService.insertMyTask(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddTaskActivity, "My Task Saved", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)        // 🔥 Add this
                    finish()
                }
            else {
                    Toast.makeText(this@AddTaskActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@AddTaskActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }




}





//package com.avs.visitapp
//
//import android.app.Activity
//import android.os.Bundle
//import android.util.Log
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.avs.visitapp.databinding.ActivityAddTaskBinding
//import com.avs.visitapp.model.TaskModel
//import retrofit2.*
//import retrofit2.converter.gson.GsonConverterFactory
//
//class AddTaskActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityAddTaskBinding
//    private lateinit var apiService: ApiService
//    private var selectedPermissionNo: Int = 0
//
//    // add near other private vars
//    private var allUsers: List<User> = emptyList()
//    private var editTask: TaskModel? = null
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAddTaskBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//
//        // in onCreate()
//        editTask = intent.getSerializableExtra("editTask") as? TaskModel
//        if (editTask != null) {
//            prefillTaskData(editTask!!)
//            binding.btnSaveTask.text = "Update Task"   // UI clarity
//        }
//
//
//
//        // Initialize Retrofit
//        val retrofit = Retrofit.Builder()
//            .baseUrl("http://43.239.110.100:80/Trackme_payroll/api/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//        apiService = retrofit.create(ApiService::class.java)
//
//        // Toolbar back (optional)
//        // binding.toolbarBack.setOnClickListener { finish() }
//
//        // Load dropdowns
//        setupDropdowns()
//        fetchUsers()
//
//        // Save Task
//        binding.btnSaveTask.setOnClickListener {
//            if (editTask != null) {
//                updateTaskStatus(editTask!!)
//            } else {
//                saveTask()
//            }
//        }
//
//    }
//
//
//    private fun prefillTaskData(task: TaskModel) {
//        // Map PermissionNo to username (safe if allUsers is empty -> fallback to empty string)
//        val assignedUser = allUsers.find { it.PermissionNo.toString() == task.AssignBy }
//        val assignByName = assignedUser?.USERNAME ?: ""
//
//        binding.etAssignBy.setText(assignByName)
//        binding.taskTitle.setText(task.TaskTitle)
//        binding.description.setText(task.Description)
//        binding.priority.setText(task.Priority, false)
//        binding.status.setText(task.Status, false)
//        binding.approxTime.setText(task.AproxTime ?: "")
//
//
//        // Disable everything except Status
//        binding.taskTitle.isEnabled = false
//        binding.description.isEnabled = false
//        binding.priority.isEnabled = false
//        binding.etAssignBy.isEnabled = false
//        binding.status.isEnabled = true
//        binding.approxTime.isEnabled = false
//
//        // Update button text to clarify edit mode
//        binding.btnSaveTask.text = "Update Task"
//    }
//
//
//
//
//
//
//    private fun setupDropdowns() {
//        // Priority dropdown setup
//        val priorities = listOf("High", "Medium", "Low")
//        val priorityAdapter = ArrayAdapter(this, R.layout.item_dropdown, priorities)
//        val priorityDropdown = binding.priority as AutoCompleteTextView
//        priorityDropdown.setAdapter(priorityAdapter)
//        priorityDropdown.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.color.white))
//        priorityDropdown.setOnClickListener { priorityDropdown.showDropDown() }
//
//        // Status dropdown setup
//        val statuses = listOf("Pending", "In Progress", "Completed")
//        val statusAdapter = ArrayAdapter(this, R.layout.item_dropdown, statuses)
//        val statusDropdown = binding.status as AutoCompleteTextView
//        statusDropdown.setAdapter(statusAdapter)
//        statusDropdown.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.color.white))
//        statusDropdown.setOnClickListener { statusDropdown.showDropDown() }
//    }
//
//    private fun saveTask() {
//        val staffId = Constant.getSharedPreferenceValue(this, Constant.SHARE_EMP_CD)?.toIntOrNull() ?: 0
//        val brcd = Constant.getSharedPreferenceValue(this, Constant.SHARE_BRANCH_Code) ?: ""
//
//        val taskTitle = binding.taskTitle.text.toString().trim()
//        val description = binding.description.text.toString().trim()
//        val priority = binding.priority.text.toString().trim()
//        val approxTime = binding.approxTime.text.toString().trim()
//        val status = binding.status.text.toString().trim()
//
//        if (taskTitle.isEmpty() || description.isEmpty() || selectedPermissionNo == 0) {
//            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val remarks = "Created from mobile app"
//
//        Log.d("StaffId",staffId.toString())
//
//        val taskRequest = TaskRequest(
//            TaskTitle = taskTitle,
//            Description = description,
//            AssignBy = selectedPermissionNo,
//            Priority = priority,
//            AproxTime = approxTime,
//            Status = status,
//            StaffId = staffId,
//            Brcd = brcd,
//            Remarks = remarks
//        )
//
//        Log.d("AddTask", "📤 Sending Task: $taskRequest")
//
//        val call = apiService.insertTask(taskRequest)
//        call.enqueue(object : Callback<String> {
//            override fun onResponse(call: Call<String>, response: Response<String>) {
//                if (response.isSuccessful) {
//                    val serverMsg = response.body() ?: "No response from server"
//                    Log.d("InsertTask", "✅ Response: $serverMsg")
//                    Toast.makeText(this@AddTaskActivity, serverMsg, Toast.LENGTH_LONG).show()
//
//                    // Clear fields
////                    binding.taskTitle.text?.clear()
////                    binding.description.text?.clear()
////                    binding.etAssignBy.text?.clear()
////                    binding.priority.text?.clear()
////                    binding.approxTime.text?.clear()
////                    binding.status.text?.clear()
////                    selectedPermissionNo = 0
//
//                    // Return success to TaskListActivity
//                    setResult(Activity.RESULT_OK)
//                    finish()
//
//                } else {
//                    val err = response.errorBody()?.string()
//                    Log.e("InsertTask", "❌ Error: $err")
//                    Toast.makeText(this@AddTaskActivity, "Error: $err", Toast.LENGTH_SHORT).show()
//                }
//                Log.d("TaskAPI", "StaffId=$staffId, Branch=$brcd, Permission=$selectedPermissionNo")
//            }
//
//            override fun onFailure(call: Call<String>, t: Throwable) {
//                Log.e("InsertTask", "🚨 Failure: ${t.message}")
//                Toast.makeText(this@AddTaskActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    private fun onSaveTaskSuccess() {
//        // This is the function you would call after your API call to add a task succeeds.
//
//        // 1. Set the result to RESULT_OK to signal success
//        setResult(Activity.RESULT_OK)
//
//        // 2. Finish this activity and return to TaskListActivity
////        finish()
//    }
//
//    private fun fetchUsers() {
//        // cast once to AutoCompleteTextView and keep a local reference
//        val assignByDropdown = binding.etAssignBy as AutoCompleteTextView
//
//        val call = apiService.getAllUsers()
//        call.enqueue(object : Callback<List<User>> {
//            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
//                if (!response.isSuccessful) {
//                    Toast.makeText(this@AddTaskActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//                allUsers = response.body() ?: emptyList()
//
//                val userNames: List<String> = allUsers.map { it.USERNAME ?: "" }
//
//                // Explicit generic to avoid "Cannot infer type parameter" errors
//                val adapter = ArrayAdapter<String>(
//                    this@AddTaskActivity,
//                    R.layout.item_dropdown,
//                    userNames
//                )
//
//                assignByDropdown.setAdapter(adapter)
//                assignByDropdown.setDropDownBackgroundDrawable(
//                    ContextCompat.getDrawable(this@AddTaskActivity, R.color.white)
//                )
//
//                assignByDropdown.setOnClickListener { assignByDropdown.showDropDown() }
//                assignByDropdown.setOnTouchListener { _, _ ->
//                    assignByDropdown.showDropDown()
//                    false
//                }
//
//                // Correct parameter list for AdapterView.OnItemClickListener
//                assignByDropdown.setOnItemClickListener { parent, view, position, id ->
//                    val selectedUser = allUsers[position]
//                    selectedPermissionNo = selectedUser.PermissionNo
//                    // Optionally show username in the field (it will, because AutoCompleteTextView sets it)
//                    assignByDropdown.setText(selectedUser.USERNAME ?: "", false)
//
//                    Toast.makeText(
//                        this@AddTaskActivity,
//                        "Selected: ${selectedUser.USERNAME} (PermissionNo=${selectedUser.PermissionNo})",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//                Toast.makeText(this@AddTaskActivity, "Loaded ${allUsers.size} users", Toast.LENGTH_SHORT).show()
//
//                // If we came here from edit mode, now we can prefill fields that require user-name lookup
//                editTask?.let { prefillTaskData(it) }
//            }
//
//            override fun onFailure(call: Call<List<User>>, t: Throwable) {
//                Toast.makeText(this@AddTaskActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    private fun getStageCode(status: String): Int {
//        return when (status.lowercase()) {
//            "pending" -> 1001
//            "in progress" -> 1002
//            "completed" -> 1003
//            else -> 1001
//        }
//    }
//
//
//
//
//
//
//
//    private fun updateTaskStatus(task: TaskModel) {
//
//        val newStatus = binding.status.text.toString().trim()
//        val stageCode = getStageCode(newStatus)
//
//        apiService.updateStage(
//            task.TaskId,
//            stageCode.toString(),
//            newStatus,
//            ""
//        ).enqueue(object : Callback<UpdateStageResponse> {
//
//            override fun onResponse(
//                call: Call<UpdateStageResponse>,
//                response: Response<UpdateStageResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val body = response.body()
//
//                    Toast.makeText(
//                        this@AddTaskActivity,
//                        body?.Message ?: "Updated successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    setResult(Activity.RESULT_OK)
//                    finish()
//                } else {
//                    Toast.makeText(
//                        this@AddTaskActivity,
//                        "Server error: ${response.errorBody()?.string()}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//
//            override fun onFailure(call: Call<UpdateStageResponse>, t: Throwable) {
//                Toast.makeText(
//                    this@AddTaskActivity,
//                    "Failed: ${t.localizedMessage}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        })
//    }
//
//
//}
//
