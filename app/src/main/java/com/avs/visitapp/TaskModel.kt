package com.avs.visitapp.model
import java.io.Serializable

data class TaskModel(
    val TaskID: Int,
    val EmpNo: Int,
    val AssignedToEmpNo: Int,
    val TaskDescription: String,
    val TaskDate: String,
    val CompletionDescription: String?,
    val OtherRemarks: String?,
    val Stage: String,
    val Status: String,
    val CreatedDate: String,
    val BRCD: Int,
    var Zone: String,
    var AssignedToName: String? = null,
    var BranchName: String? = null
) : Serializable





data class TaskInsertRequest(
    val EmpNo: Int,
    val AssignedToEmpNo: Int,
    val TaskDescription: String,
    val TaskDate: String,
    val CompletionDescription: String,
    val OtherRemarks: String,
    val Stage: String,   // must be "1001"
    val Status: String,
    val BRCD: Int,
    val Zone: String
)



data class MyTaskRequest(
    val TaskID: Int = 0,
    val EmpNo: Int,
    val AssignedToEmpNo: Int = 0,
    val TaskDescription: String,
    val TaskDate: String,
    val CompletionDescription: String = "",
    val OtherRemarks: String = "",
    val BRCD: Int,
    val Zone: String,
    val Status: String,
    val CreatedDate: String
)
