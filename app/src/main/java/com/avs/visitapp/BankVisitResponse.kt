package com.avs.visitapp.model // Adjust package name as needed

data class BankVisitResponse(
    val Status: String?,
    val Count: Int?,
    val Data: List<BankVisitData>?
)

data class BankVisitData(
    val ID: Int?,
    val FLAG: String?,
    val VisitDate: String?,
    val VisitInTime: String?,
    val VisitOutTime: String?,
    val StaffID: Int?,
    val StaffName: String?,
    val BankName: String?,
    val BranchName: String?,
    val Comments: String?,
    val FullAddress: String?,
    val Latitude: String?,
    val Longitude: String?,
    val Image: String?
)