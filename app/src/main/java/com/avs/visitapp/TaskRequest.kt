package com.avs.visitapp

data class TaskRequest(
    val TaskTitle: String,
    val Description: String,
    val AssignBy: Int,      // Use PermissionNo
    val Priority: String,
    val AproxTime: String,
    val Status: String,
    val StaffId: Int,
    val Brcd: String,
    val Remarks: String
)
