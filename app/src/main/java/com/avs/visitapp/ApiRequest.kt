package com.avs.visitapp

import com.avs.visitapp.model.TaskModel


data class OtpRequest(
    val emp_code: String
)

data class VerifyOtpRequest(
    val emp_code: String,
    val inputotp: String,
)

data class VisitModel(
    val visit_id: Int,
    val customer_name: String,
    val visit_date: String,
    val document_type: String,
    val document_number: String,
    val location: Location,
    val photo_url: String
) {
    data class Location(
        val latitude: String,
        val longitude: String
    )
}

data class VisitRequest(
    val emp_id: String?,
    val customer_name: String,
    val mobile_number: String,
    val loan_account: String,
    val visit_date: String,
    val location: Location,
    val photo_base64: String,
    val document_type: String,
    val document_number: String
)



data class VisitReportRequest(
    val emp_id: String?,
    val from: String,
    val to: String,
    val mobile: String
)

data class VisitRecordRequest(
    val EmployeeId: String?,
    val FromDate: String,
    val MobileNo: String
)


data class ApplicationRequest(
    val Applicant: Applicant,
    val BankDetails: BankDetails,
    val FamilyIncome: FamilyIncome,
    val HousingDetails: HousingDetails,
    val OccupationDetails: OccupationDetails,
    val visit: visitDetails,
    val avsVisit: AvsVisit
)


data class Applicant(
    val ApplicantID: Int,
    val Name: String,
    val ApplicationDate: String,
    val ApplicationSerialNo: String,
    val LoanPurposeType: String,
    val RequestedAmount: Double?,
    val EmployeeId: Int,
    val MobileNo: String
)

data class BankDetails(
    val BankName: String,
    val BranchName: String,
    val BankAddress: String,
    val AccountNumber: String,
    val HasExistingLoan: Boolean,
    val LoanBankName: String,
    val LoanAmount: Float
)
data class BankresponseDetails(
    val BankName: String,
    val BranchName: String,
    val BankAddress: String,
    val AccountNumber: String,
    val HasExistingLoan: Boolean,
    val LoanBankName: String,
    val LoanAmount:  Double?
)

data class FamilyIncome(
    val TotalFamilyMembers: Int,
    val SelfIncome: Double?,
    val SpouseIncome:  Double?,
    val OtherIncome:  Double?,
    val TotalIncome:  Double?,
    val MonthlyExpenses:  Double?,
    val NetSurplus:  Double?
)

data class HousingDetails(
    val OtherMemberName: String,
    val ApproxCost:  Double?,
    val StayDuration: String,
    val RentFrom: String,
    val RentTo: String,
    val DepositAmount:  Double?,
    val MonthlyRent:  Double?,
    val IsMortgaged: String
)

data class OccupationDetails(
    val PersonName: String,
    val Occupation: String,
    val MobileNo: String,
    val MonthlyIncome:  Double?,
    val AgriculturalLand: String,
    val OtherAssets: String
)
data class visitDetails(
    val VisitDate: String,
    val VisitTime: String,
    val PresentPersonName: String,
    val ResidentialAddress: String,
    val VMobileNo: String,
    val EmployeeId: Int
)

data class AvsVisit(
    val emp_id: Int,
    val customer_name: String,
    val mobile_number: String,
    val loan_account: String,
    val visit_date: String,
    val location: Location,
    val document_type: String,
    val document_number: String,
    val photo_base64: String
)

data class Location(
    val latitude: String,
    val longitude: String
)

data class LoanVisitRequest(
    val Staff_id: Int,
    val Image: String,
    val CustomerNameAccount: String,
    val LoanAccountNo: String,
    val LoanType: String,
    val CustomerMobileNo: String,
    val LoanCategory: String,
    val LoanDisbursedDate: String,
    val Case_101_91: String,
    val OverdueAmount: Double,
    val Comments: String,
    val PromiseToPay: String,
    val PtpDate: String,
    val VisitTime: String,
    val OtherRemarks: String,
    val Latitude: String,
    val Longitude: String,
    val BankName: String,
    val BranchName: String,
    val VisitDate: String
)


data class MarkAttendance(
    val empId: String
)

data class CallDelinquencyVisitRequest(
    val StaffID: String?,
    val CustomerName: String,
    val CustomerMobileNo: String,
    val AccountNo: String,
    val LoanAccountNo: String,
    val CallDate: String,
    val CallTime: String,
    val BranchCode: String,
    val OverdueAmount: Double,
    val CallRemarks: String
)


data class UpdateStageResponse(
    val Message: String,
    val Status: String
)

data class TaskResponse(
    val TotalCount: Int,
    val Tasks: List<TaskModel>
)


