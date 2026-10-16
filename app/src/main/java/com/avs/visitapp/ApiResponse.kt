package com.avs.visitapp

import com.avs.visitapp.model.User
import java.io.Serializable

data class OtpResponse(
    val status: String?,
    val message: String?,
    val emp_id: String,
    val otp: String,
    val name: String,
    val phoneNumber: String?
)

data class VerifyOtpResponse(
    val status: String?,
    val message: String?,
    val emp_id: String,
    val name: String,
    val branch_name: String,
    val emp_CD: String,
    val branch_code: String,
    val token: String?,
    val Latitude: String?,
    val Longitude: String?,
    val emp_status: String?
)

data class VisitResponse(
    val status: String,
    val message: String
)

data class VisitRetrieveResponse(
    val status: String,
    val data: List<VisitData>
)

data class VisitData(
    val EmployeeName: String,
    val EmployeeId: String,
    val Applicant: Applicant,
    val BankDetails: BankresponseDetails,
    val FamilyIncome: FamilyIncome,
    val HousingDetails: HousingDetails,
    val OccupationDetails: OccupationDetails,
    val Visit: visitDetails,
    val VisitResponses: List<VisitsResponse>
)


data class VisitReportResponse(
    val status: String,
    val download_url: String
)



data class ApplicationResponse(
    val status: String,
    val applicantId: Int
)


data class VisitsResponse(
    val visit_id: Int,
    val customer_name: String,
    val visit_date: String,
    val document_type: String,
    val document_number: String,
    val location: Location,
    val photo_url: String
)

data class LoanVisitResponse(
    val Message: String,
    val Id: Int
)

data class VisitTypeResponse(
    val status: String,
    val data: List<VisitType>
)

data class VisitType(
    val id: Int,
    val visit_type_name: String
)

data class DelinquencyVisitResponse(
    val Message: String,
    val Id: Int
)

data class MarketingVisitResponse(
    val Success: Boolean,
    val Message: String,
    val Id: Int
)

data class MonitoringVisitResponse(
    val Message: String,
    val Id: Int
)

data class LegalCaseVisitResponse(
    val Message: String,
    val Id: Int
)

data class BankResponse(
    val BRCD: Int,
    val MIDNAME: String?,
)
data class CustomerNameResponse(
    val CUSTNO: String,
    val CUSTACCNO: String,
    val AssetCode: String,
    val LOANGLCODE: String,
    val Name: String,
    val OverDueAMT: String,
    val Int_PenalInt: String,
    val MobileNo: String,
    val accountno: String,
    val LoanAmount: String?,
    val DisbursedDate: String?
)

data class DoneVisitResponse(
    val TotalCount: Int,
    val Visits: List<Visit>
)

data class Visit(
    val CustMobileNo: String,
    val CallRemarks: String,
    val Count: Int,
    val CustomerName: String,
    val BRCD: Int,
    val BranchName: String,
    val LoanAccNo: String,
    val ODAmt: String,
    val status: String,
    val VisitDate: String
)



data class VisitAgainModel(
    val Count: Int,
    val CustomerName: String,
    val BRCD: Int,
    val CustMobileNo: String,
    val ProdctCD: String?,
    val LoanAccNo: String,
    val ODAmt: String,
    val Status: String,
    val BranchName: String,
    val VisitDate: String?,
    val OtherRemarks: String?,
    val Comments: String?,
    val Loan101_91: String?,
    val PTPDate: String?,
    val PromiseToPay: String?,
    val VisitedPlaceName: String?
) : Serializable


data class VisitAgainUpdateRequest(
    val AccountNo: String,
    val LoanAccountNo: String,
    val BranchCode: String,
    val StaffID: Int,
    val LoanType: String = "DQ",
    val Comments: String,
    val PTPDate: String?,
    val Latitude: String,
    val Longitude: String
)



data class PendingVisitResponse(
    val TotalCount: Int,
    val Visits: List<Visit>
)


data class LatLongResponse(
    val status: Boolean,
    val data: List<LatLongData>
)

data class LatLongData(
    val Latitude: String,
    val Longitude: String
)

data class AttendanceResponse(
    val message: String
)

data class LatestAttendanceResponse(
    val EMPID: String,
    val INTIME: String?,
    val OUTTIME: String?
)

data class Attendance(
    val ATDATE: String,
    val EMPID: String,
    val INTIME: String?,
    val OUTTIME: String?,
    val Status: String
)


data class CallDelinquencyVisitResponse(
    val message: String,
    val id: Int
)



data class ApiResponse(
    val status: String?,
    val message: String?
)


data class VisitAgainResponse(
    val TotalCount: Int,
    val Visits: List<VisitAgainModel>
)




data class GetUsersResponse(
    val Status: String,
    val Count: Int,
    val Data: List<User>
)

//data class LegalTypeResponse(
//    val Status: String,
//    val Data: List<LegalType>
//)
//
//data class LegalType(
//    val ID: Int,
//    val DESCRIPTION: String
//)
