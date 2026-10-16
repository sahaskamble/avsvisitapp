package com.avs.visitapp.model

data class User(
    val LOGINCODE: String?,
    val PermissionNo: Int,
    val USERNAME: String?,
    val USERMOBILENO: Long?,
    val BRCD: Int?,
    val Zone: String?,
    val BRANCHNAME: String?,
    val USERGROUP: Int?,
    val USERSTATUS: Int?,
    val Latitude: String?,
    val Longitude: String?
)
