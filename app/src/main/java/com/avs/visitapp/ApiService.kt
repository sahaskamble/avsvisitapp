package com.avs.visitapp

import com.avs.visitapp.model.MyTaskRequest
import com.avs.visitapp.model.TaskInsertRequest
import com.avs.visitapp.model.TaskModel
import com.avs.visitapp.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import com.avs.visitapp.LegalCaseVisitActivity

interface ApiService {

    //  Send OTP
    @POST("api/Login/SendOtp")
    fun sendOtp(@Body request: OtpRequest): Call<OtpResponse>

    //  Verify OTP
    @POST("api/Login/VerifyOtp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>

    // Add Visit
    @POST("api/Visit/Add")
    fun submitVisit(@Body request: VisitRequest): Call<VisitResponse>

    @POST("api/Applicants/Submit")
    fun submitApplication(@Body request: ApplicationRequest): Call<ApplicationResponse>

    // Retrieve All Visit Record
    @POST("api/Applicants/GetApplicantsByCriteria")
    fun getVisitsByEmployeeId(@Body request: VisitRecordRequest): Call<VisitRetrieveResponse>

    // Download pdf
    @POST("api/Report/GeneratePdfLink")
    fun getVisitReportPdf(@Body request: VisitReportRequest): Call<VisitReportResponse>


    // Retrieve All Visit Record
    @GET("api/Applicants/GetALL/")
    suspend fun getVisitsByEmployeeId(
        @Query("id") id: String?): Response<VisitRetrieveResponse>





    @POST("api/LoanVisitRecovery/InsertLoanVisi")
    fun insertLoanVisit(@Body request: LoanVisitRequest): Call<LoanVisitResponse>


    @GET("api/LoanVisitRecovery/Getloantype")
    suspend fun getLoanVisitTypes(): Response<VisitTypeResponse>


    // Create Delinquency Visit

    @Multipart
    @POST("api/LoanVisitRecovery/InsertDelinquencyVisit")
    fun InsertDelinquencyVisit(
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("VisitTime") VisitTime: RequestBody,
        @Part("StaffID") StaffID: RequestBody,
        @Part("StaffName") StaffName: RequestBody,
        @Part("BranchName") BranchName: RequestBody,
        @Part("BranchCode") BranchCode: RequestBody,
        @Part("CustomerName") CustomerName: RequestBody,
        @Part("AccountNo") AccountNo: RequestBody,
        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
        @Part("LoanType") LoanType: RequestBody,
        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
        @Part("LoanCategory") LoanCategory: RequestBody,
        @Part("Loan101_91") Loan101_91: RequestBody,
        @Part("OverdueAmount") OverdueAmount: RequestBody,
        @Part("Comments") Comments: RequestBody,
        @Part("PromiseToPay") PromiseToPay: RequestBody,
        @Part("PTPDate") PTPDate: RequestBody,
        @Part("VisitedPlaceName") VisitedPlaceName: RequestBody,
        @Part("OtherRemarks") OtherRemarks: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("FullAddress") FullAddress: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<DelinquencyVisitResponse>

    @Multipart
    @POST("api/LoanVisitRecovery/InsertVisitAgainDelinquencyVisit")
    fun InsertVisitAgainDelinquencyVisit(
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("VisitTime") VisitTime: RequestBody,
        @Part("StaffID") StaffID: RequestBody,
        @Part("StaffName") StaffName: RequestBody,
        @Part("BranchName") BranchName: RequestBody,
        @Part("BranchCode") BranchCode: RequestBody,
        @Part("CustomerName") CustomerName: RequestBody,
        @Part("AccountNo") AccountNo: RequestBody,
        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
        @Part("LoanType") LoanType: RequestBody,
        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
        @Part("LoanCategory") LoanCategory: RequestBody,
        @Part("Loan101_91") Loan101_91: RequestBody,
        @Part("OverdueAmount") OverdueAmount: RequestBody,
        @Part("Comments") Comments: RequestBody,
        @Part("PromiseToPay") PromiseToPay: RequestBody,
        @Part("PTPDate") PTPDate: RequestBody,
        @Part("VisitedPlaceName") VisitedPlaceName: RequestBody,
        @Part("OtherRemarks") OtherRemarks: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("FullAddress") FullAddress: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<DelinquencyVisitResponse>


    // Create Marketing Visit
    @Multipart
    @POST("api/LoanVisitRecovery/InsertMarketingVisit")
    fun InsertMarketingVisit(
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("VisitTime") VisitTime: RequestBody,
        @Part("StaffID") StaffID: RequestBody,
        @Part("StaffName") StaffName: RequestBody,
        @Part("BranchName") BranchName: RequestBody,
        @Part("BranchCode") BranchCode: RequestBody,
        @Part("CustomerName") CustomerName: RequestBody,
        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
        @Part("CustomerRequirement") CustomerRequirement: RequestBody,
        @Part("Comments") Comments: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("FullAddress") FullAddress: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<MarketingVisitResponse>

    //5. monitoring
    @Multipart
    @POST("api/LoanVisitRecovery/InsertMonitoringVisit")
    fun InsertMonitoringVisit(
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("VisitTime") VisitTime: RequestBody,
        @Part("StaffID") StaffID: RequestBody,
        @Part("StaffName") StaffName: RequestBody,
        @Part("BranchName") BranchName: RequestBody,
        @Part("BranchCode") BranchCode: RequestBody,
        @Part("CustomerNameWithAccountNo") CustomerNameWithAccountNo: RequestBody,
        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
        @Part("LoanType") LoanType: RequestBody,
        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
        @Part("LoanCategory") LoanCategory: RequestBody,
        @Part("LoanAmount") LoanAmount: RequestBody,
        @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
        @Part("EndUseFundsComments") EndUseFundsComments: RequestBody,
        @Part("OtherRemarks") OtherRemarks: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("FullAddress") FullAddress: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<MonitoringVisitResponse>

//    @Multipart
//    @POST("api/LoanVisitRecovery/InsertLegalCaseVisit")
//    fun InsertLegalCaseVisit(
//        @Part("VisitDate") VisitDate: RequestBody,
//        @Part("VisitTime") VisitTime: RequestBody,
//        @Part("StaffID") StaffID: RequestBody,
//        @Part("StaffName") StaffName: RequestBody,
//        @Part("BranchName") BranchName: RequestBody,
//        @Part("BranchCode") BranchCode: RequestBody,
//        @Part("CustomerNameWithAccountNo") CustomerNameWithAccountNo: RequestBody,
//        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
//        @Part("LoanType") LoanType: RequestBody,
//        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
//        @Part("LoanCategory") LoanCategory: RequestBody,
//        @Part("LoanAmount") LoanAmount: RequestBody,
//        @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
//        @Part("Cases91And101") Cases91And101: RequestBody,
//        @Part("Date91And101") Date91And101: RequestBody,
//        @Part("Cases138AndOther") Cases138AndOther: RequestBody,
//        @Part("Date138AndOther") Date138AndOther: RequestBody,
//        @Part("Comments") Comments: RequestBody,
//        @Part("Latitude") Latitude: RequestBody,
//        @Part("Longitude") Longitude: RequestBody,
//        @Part("FullAddress") FullAddress: RequestBody,
//        @Part image: MultipartBody.Part?
//    ): Call<LegalCaseVisitResponse>


    // Add these to your interface:
    @GET("api/LoanVisitRecovery/GetLegalTypes")
    suspend fun getLegalTypes(@Query("flag") flag: String): Response<LegalTypeResponse>

    @Multipart
    @POST("api/LoanVisitRecovery/InsertLegalCasesVisit")
    fun InsertLegalCasesVisit(
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("VisitTime") VisitTime: RequestBody,
        @Part("StaffID") StaffID: RequestBody,
        @Part("StaffName") StaffName: RequestBody,
        @Part("BranchName") BranchName: RequestBody,
        @Part("BranchCode") BranchCode: RequestBody,
        @Part("CustAccNo") CustAccNo: RequestBody,
        @Part("LoanAccNo") LoanAccNo: RequestBody,
        @Part("LoanType") LoanType: RequestBody,
        @Part("CustMobileNo") CustMobileNo: RequestBody,
        @Part("LoanCategory") LoanCategory: RequestBody,
        @Part("LoanAmount") LoanAmount: RequestBody,
        @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
        @Part("FullAddress") FullAddress: RequestBody,
        @Part("EndUseFundsComments") EndUseFundsComments: RequestBody,
        @Part("OtherRemarks") OtherRemarks: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("Case1") Case1: RequestBody,
        @Part("Case2") Case2: RequestBody,
        @Part("Case1_DT") Case1_DT: RequestBody,
        @Part("Case2_DT") Case2_DT: RequestBody,
        @Part("Input") Input: RequestBody,
        @Part("InputStatus") InputStatus: RequestBody,
        @Part("Casetype") Casetype: RequestBody,
        @Part("Srno") Srno: RequestBody,
        @Part("CommentsNew") CommentsNew: RequestBody,
        @Part("District") District: RequestBody, // <-- ADD THIS
        @Part("Taluka") Taluka: RequestBody,     // <-- ADD THIS
        @Part image: MultipartBody.Part?
    ): Call<LegalCaseVisitResponse> // Ensure you have a LegalCaseVisitResponse class for the result


    // New Location APIs
    @GET("api/LoanVisitRecovery/GetDistrict")
    suspend fun getDistricts(): Response<List<LocationData>>

    @GET("api/LoanVisitRecovery/GetTaluka")
    suspend fun getTalukas(@Query("RefNo") refNo: Int): Response<List<LocationData>>


    //3. monitoring
    @GET("api/BankName/GetALL")
    suspend fun getAllBanks(): Response<List<BankResponse>>


    @GET("api/LoanFollowUp/GetLoanFollowUps")
    suspend fun getLoanFollowUps(
        @Query("brcd") branchCode: Int,
        @Query("Empid") Empid: String?,
    ): List<CustomerNameResponse>

//    //4. monitoring old
//     @GET("api/LoanFollowUp/GetMonitoringVisits")
//     suspend fun getmonitoringLoanFollowUps(
//          @Query("brcd") branchCode: Int,
//          @Query("Empid") Empid: String?,
//     ): List<CustomerNameResponse>

    //4. monitoring new
    @GET("api/LoanFollowUp/GetLoanFollowUps")
    suspend fun getmonitoringLoanFollowUps(
        @Query("brcd") branchCode: Int,
        @Query("Empid") Empid: String?,
    ): List<CustomerNameResponse>

    //1. monitoring
    @GET("api/LoanVisitRecovery/GetAllDoneVisits")
    suspend fun getAllDoneVisits(
        @Query("StaffID") staffId: Int?,
        @Query("flag") flag: String
    ): Response<DoneVisitResponse>

    //2. monitoring
    @GET("api/LoanVisitRecovery/GetALLPendingVisit")
    suspend fun getAllPendingVisits(
        @Query("StaffID") staffId: Int?,
        @Query("flag") flag: String
    ): Response<PendingVisitResponse>


    @GET("api/LoanVisitRecovery/GetAllVisitAgainList")
    suspend fun getAllVisitAgainList(
        @Query("staffId") staffId: Int
    ): Response<VisitAgainResponse>


    @POST("api/LoanVisitRecovery/UpdateVisitAgainDelinquencyVisit")
    fun updateVisitAgain(
        @Body body: VisitAgainUpdateRequest
    ): Call<ResponseBody>



    @GET("api/Applicants/GetLat_Log")
    suspend fun getLatLong(
        @Query("empId") staffId: String?
    ): Response<LatLongResponse>


//     @POST("api/Attendence/MarkAttendance")
//     fun markAttendance(
//          @Query("empId") empId: String?
//     ): Call<String>

    @POST("api/Attendence/MarkAttendance")
    suspend fun markAttendance(
        @Query("empId") empId: String,
        @Query("Latt") latt: String,
        @Query("Long") lng: String,
        @Query("Add") add: String
    ): Response<String>


    @GET("api/Attendence/GetLatestAttendance")
    suspend fun getLatestAttendance(
        @Query("empId") empId: String?
    ): Response<LatestAttendanceResponse>

//     @GET("api/Attendence/GetMonthlyAttendance")
//     suspend fun getMonthlyAttendance(
//          @Query("empId") empId: Int,
//          @Query("year") year: Int,
//          @Query("month") month: Int
//     ): Response<List<Attendance>>

    @GET("api/Attendence/GetMonthlyAttendance")
    suspend fun getMonthlyAttendance(
        @Query("empId") empId: Int,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<List<Attendance>>


    @POST("api/LoanVisitRecovery/InsertCallDelinquencyVisit")
    suspend fun insertCallDelinquencyVisit(
        @Body request: CallDelinquencyVisitRequest
    ): Response<CallDelinquencyVisitResponse>


    // fetch all users
//     @GET("User/GetAllUsers")
//     fun getAllUsers(): Call<List<User>>

    @GET("api/TaskManagement/GetUserWithBankDetails")
    fun getUsersWithBankDetails(): Call<GetUsersResponse>


//     @POST("TaskManagement/InsertTask")
//     fun insertTask(@Body task: TaskRequest): Call<ApiResponse>

//     //insert task
//     @POST("api/TaskManagement/InsertTask")
//     fun insertTask(@Body task: TaskRequest): Call<String>

    @POST("api/TaskManagement/InsertTask")
    fun insertTaskNew(@Body request: TaskInsertRequest): Call<String>


    //get all task assign by you
    @GET("api/TaskManagement/GetTasks")
    fun getTasks(@Query("staffId") staffId: Int): Call<List<TaskModel>>

    //get all task assign to you
    @GET("api/TaskManagement/GetAssignedTasks")
    fun getAssignedTasks(
        @Query("staffId") empNo: Int
    ): Call<List<TaskModel>>



    @POST("api/TaskManagement/UpdateTask")
    fun updateTask(
        @Query("taskId") taskId: Int,
        @Query("completionDescription") completion: String,
        @Query("otherRemarks") remarks: String?
    ): Call<ResponseBody>


    @POST("api/TaskManagement/InsertMYTasks")
    fun insertMyTask(
        @Body request: MyTaskRequest
    ): Call<ResponseBody>

    @GET("api/TaskManagement/GetMYTasks")
    fun getMyTasks(
        @Query("staffId") empNo: Int
    ): Call<List<TaskModel>>

}



//package com.avs.visitapp
//
//import com.avs.visitapp.model.MyTaskRequest
//import com.avs.visitapp.model.TaskInsertRequest
//import com.avs.visitapp.model.TaskModel
//import com.avs.visitapp.model.User
//import okhttp3.MultipartBody
//import okhttp3.RequestBody
//import okhttp3.ResponseBody
//import retrofit2.Call
//import retrofit2.Response
//import retrofit2.http.Body
//import retrofit2.http.Field
//import retrofit2.http.FormUrlEncoded
//import retrofit2.http.GET
//import retrofit2.http.Multipart
//import retrofit2.http.POST
//import retrofit2.http.Part
//import retrofit2.http.Path
//import retrofit2.http.Query
//import com.avs.visitapp.LegalCaseVisitActivity
//
//interface ApiService {
//
//     //  Send OTP
//     @POST("Trackme_payroll/api/Login/SendOtp")
//     fun sendOtp(@Body request: OtpRequest): Call<OtpResponse>
//
//     //  Verify OTP
//     @POST("Trackme_payroll/api/Login/VerifyOtp")
//     fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>
//
//     // Add Visit
//     @POST("Trackme_payroll/api/Visit/Add")
//     fun submitVisit(@Body request: VisitRequest): Call<VisitResponse>
//
//     @POST("Trackme_payroll/api/Applicants/Submit")
//     fun submitApplication(@Body request: ApplicationRequest): Call<ApplicationResponse>
//
//     // Retrieve All Visit Record
//     @POST("Trackme_payroll/api/Applicants/GetApplicantsByCriteria")
//     fun getVisitsByEmployeeId(@Body request: VisitRecordRequest): Call<VisitRetrieveResponse>
//
//     // Download pdf
//     @POST("Trackme_payroll/api/Report/GeneratePdfLink")
//     fun getVisitReportPdf(@Body request: VisitReportRequest): Call<VisitReportResponse>
//
//
//     // Retrieve All Visit Record
//     @GET("Trackme_payroll/api/Applicants/GetALL/")
//     suspend fun getVisitsByEmployeeId(
//          @Query("id") id: String?): Response<VisitRetrieveResponse>
//
//
//
//
//
//     @POST("Trackme_payroll/api/LoanVisitRecovery/InsertLoanVisi")
//     fun insertLoanVisit(@Body request: LoanVisitRequest): Call<LoanVisitResponse>
//
//
//     @GET("Trackme_payroll/api/LoanVisitRecovery/Getloantype")
//     suspend fun getLoanVisitTypes(): Response<VisitTypeResponse>
//
//
//     // Create Delinquency Visit
//
//     @Multipart
//     @POST("Trackme_payroll/api/LoanVisitRecovery/InsertDelinquencyVisit")
//     fun InsertDelinquencyVisit(
//          @Part("VisitDate") VisitDate: RequestBody,
//          @Part("VisitTime") VisitTime: RequestBody,
//          @Part("StaffID") StaffID: RequestBody,
//          @Part("StaffName") StaffName: RequestBody,
//          @Part("BranchName") BranchName: RequestBody,
//          @Part("BranchCode") BranchCode: RequestBody,
//          @Part("CustomerName") CustomerName: RequestBody,
//          @Part("AccountNo") AccountNo: RequestBody,
//          @Part("LoanAccountNo") LoanAccountNo: RequestBody,
//          @Part("LoanType") LoanType: RequestBody,
//          @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
//          @Part("LoanCategory") LoanCategory: RequestBody,
//          @Part("Loan101_91") Loan101_91: RequestBody,
//          @Part("OverdueAmount") OverdueAmount: RequestBody,
//          @Part("Comments") Comments: RequestBody,
//          @Part("PromiseToPay") PromiseToPay: RequestBody,
//          @Part("PTPDate") PTPDate: RequestBody,
//          @Part("VisitedPlaceName") VisitedPlaceName: RequestBody,
//          @Part("OtherRemarks") OtherRemarks: RequestBody,
//          @Part("Latitude") Latitude: RequestBody,
//          @Part("Longitude") Longitude: RequestBody,
//          @Part("FullAddress") FullAddress: RequestBody,
//          @Part image: MultipartBody.Part?
//     ): Call<DelinquencyVisitResponse>
//
//    @Multipart
//    @POST("Trackme_payroll/api/LoanVisitRecovery/InsertVisitAgainDelinquencyVisit")
//    fun InsertVisitAgainDelinquencyVisit(
//        @Part("VisitDate") VisitDate: RequestBody,
//        @Part("VisitTime") VisitTime: RequestBody,
//        @Part("StaffID") StaffID: RequestBody,
//        @Part("StaffName") StaffName: RequestBody,
//        @Part("BranchName") BranchName: RequestBody,
//        @Part("BranchCode") BranchCode: RequestBody,
//        @Part("CustomerName") CustomerName: RequestBody,
//        @Part("AccountNo") AccountNo: RequestBody,
//        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
//        @Part("LoanType") LoanType: RequestBody,
//        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
//        @Part("LoanCategory") LoanCategory: RequestBody,
//        @Part("Loan101_91") Loan101_91: RequestBody,
//        @Part("OverdueAmount") OverdueAmount: RequestBody,
//        @Part("Comments") Comments: RequestBody,
//        @Part("PromiseToPay") PromiseToPay: RequestBody,
//        @Part("PTPDate") PTPDate: RequestBody,
//        @Part("VisitedPlaceName") VisitedPlaceName: RequestBody,
//        @Part("OtherRemarks") OtherRemarks: RequestBody,
//        @Part("Latitude") Latitude: RequestBody,
//        @Part("Longitude") Longitude: RequestBody,
//        @Part("FullAddress") FullAddress: RequestBody,
//        @Part image: MultipartBody.Part?
//    ): Call<DelinquencyVisitResponse>
//
//
//    // Create Marketing Visit
//     @Multipart
//     @POST("Trackme_payroll/api/LoanVisitRecovery/InsertMarketingVisit")
//     fun InsertMarketingVisit(
//          @Part("VisitDate") VisitDate: RequestBody,
//          @Part("VisitTime") VisitTime: RequestBody,
//          @Part("StaffID") StaffID: RequestBody,
//          @Part("StaffName") StaffName: RequestBody,
//          @Part("BranchName") BranchName: RequestBody,
//          @Part("BranchCode") BranchCode: RequestBody,
//          @Part("CustomerName") CustomerName: RequestBody,
//          @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
//          @Part("CustomerRequirement") CustomerRequirement: RequestBody,
//          @Part("Comments") Comments: RequestBody,
//          @Part("Latitude") Latitude: RequestBody,
//          @Part("Longitude") Longitude: RequestBody,
//          @Part("FullAddress") FullAddress: RequestBody,
//          @Part image: MultipartBody.Part?
//     ): Call<MarketingVisitResponse>
//
//    //5. monitoring
//     @Multipart
//     @POST("Trackme_payroll/api/LoanVisitRecovery/InsertMonitoringVisit")
//     fun InsertMonitoringVisit(
//          @Part("VisitDate") VisitDate: RequestBody,
//          @Part("VisitTime") VisitTime: RequestBody,
//          @Part("StaffID") StaffID: RequestBody,
//          @Part("StaffName") StaffName: RequestBody,
//          @Part("BranchName") BranchName: RequestBody,
//          @Part("BranchCode") BranchCode: RequestBody,
//          @Part("CustomerNameWithAccountNo") CustomerNameWithAccountNo: RequestBody,
//          @Part("LoanAccountNo") LoanAccountNo: RequestBody,
//          @Part("LoanType") LoanType: RequestBody,
//          @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
//          @Part("LoanCategory") LoanCategory: RequestBody,
//          @Part("LoanAmount") LoanAmount: RequestBody,
//          @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
//          @Part("EndUseFundsComments") EndUseFundsComments: RequestBody,
//          @Part("OtherRemarks") OtherRemarks: RequestBody,
//          @Part("Latitude") Latitude: RequestBody,
//          @Part("Longitude") Longitude: RequestBody,
//          @Part("FullAddress") FullAddress: RequestBody,
//          @Part image: MultipartBody.Part?
//     ): Call<MonitoringVisitResponse>
//
////    @Multipart
////    @POST("Trackme_payroll/api/LoanVisitRecovery/InsertLegalCaseVisit")
////    fun InsertLegalCaseVisit(
////        @Part("VisitDate") VisitDate: RequestBody,
////        @Part("VisitTime") VisitTime: RequestBody,
////        @Part("StaffID") StaffID: RequestBody,
////        @Part("StaffName") StaffName: RequestBody,
////        @Part("BranchName") BranchName: RequestBody,
////        @Part("BranchCode") BranchCode: RequestBody,
////        @Part("CustomerNameWithAccountNo") CustomerNameWithAccountNo: RequestBody,
////        @Part("LoanAccountNo") LoanAccountNo: RequestBody,
////        @Part("LoanType") LoanType: RequestBody,
////        @Part("CustomerMobileNo") CustomerMobileNo: RequestBody,
////        @Part("LoanCategory") LoanCategory: RequestBody,
////        @Part("LoanAmount") LoanAmount: RequestBody,
////        @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
////        @Part("Cases91And101") Cases91And101: RequestBody,
////        @Part("Date91And101") Date91And101: RequestBody,
////        @Part("Cases138AndOther") Cases138AndOther: RequestBody,
////        @Part("Date138AndOther") Date138AndOther: RequestBody,
////        @Part("Comments") Comments: RequestBody,
////        @Part("Latitude") Latitude: RequestBody,
////        @Part("Longitude") Longitude: RequestBody,
////        @Part("FullAddress") FullAddress: RequestBody,
////        @Part image: MultipartBody.Part?
////    ): Call<LegalCaseVisitResponse>
//
//
//    // Add these to your interface:
//    @GET("Trackme_payroll/api/LoanVisitRecovery/GetLegalTypes")
//    suspend fun getLegalTypes(@Query("flag") flag: String): Response<LegalTypeResponse>
//
//    @Multipart
//    @POST("Trackme_payroll/api/LoanVisitRecovery/InsertLegalCasesVisit")
//    fun InsertLegalCasesVisit(
//        @Part("VisitDate") VisitDate: RequestBody,
//        @Part("VisitTime") VisitTime: RequestBody,
//        @Part("StaffID") StaffID: RequestBody,
//        @Part("StaffName") StaffName: RequestBody,
//        @Part("BranchName") BranchName: RequestBody,
//        @Part("BranchCode") BranchCode: RequestBody,
//        @Part("CustAccNo") CustAccNo: RequestBody,
//        @Part("LoanAccNo") LoanAccNo: RequestBody,
//        @Part("LoanType") LoanType: RequestBody,
//        @Part("CustMobileNo") CustMobileNo: RequestBody,
//        @Part("LoanCategory") LoanCategory: RequestBody,
//        @Part("LoanAmount") LoanAmount: RequestBody,
//        @Part("LoanDisbursedDate") LoanDisbursedDate: RequestBody,
//        @Part("FullAddress") FullAddress: RequestBody,
//        @Part("EndUseFundsComments") EndUseFundsComments: RequestBody,
//        @Part("OtherRemarks") OtherRemarks: RequestBody,
//        @Part("Latitude") Latitude: RequestBody,
//        @Part("Longitude") Longitude: RequestBody,
//        @Part("Case1") Case1: RequestBody,
//        @Part("Case2") Case2: RequestBody,
//        @Part("Case1_DT") Case1_DT: RequestBody,
//        @Part("Case2_DT") Case2_DT: RequestBody,
//        @Part("Input") Input: RequestBody,
//        @Part("InputStatus") InputStatus: RequestBody,
//        @Part("Casetype") Casetype: RequestBody,
//        @Part("Srno") Srno: RequestBody,
//        @Part image: MultipartBody.Part?
//    ): Call<LegalCaseVisitResponse> // Ensure you have a LegalCaseVisitResponse class for the result
//
//    //3. monitoring
//     @GET("Trackme_payroll/api/BankName/GetALL")
//     suspend fun getAllBanks(): Response<List<BankResponse>>
//
//
//     @GET("Trackme_payroll/api/LoanFollowUp/GetLoanFollowUps")
//     suspend fun getLoanFollowUps(
//          @Query("brcd") branchCode: Int,
//          @Query("Empid") Empid: String?,
//     ): List<CustomerNameResponse>
//
////    //4. monitoring old
////     @GET("Trackme_payroll/api/LoanFollowUp/GetMonitoringVisits")
////     suspend fun getmonitoringLoanFollowUps(
////          @Query("brcd") branchCode: Int,
////          @Query("Empid") Empid: String?,
////     ): List<CustomerNameResponse>
//
//    //4. monitoring new
//    @GET("Trackme_payroll/api/LoanFollowUp/GetLoanFollowUps")
//    suspend fun getmonitoringLoanFollowUps(
//        @Query("brcd") branchCode: Int,
//        @Query("Empid") Empid: String?,
//    ): List<CustomerNameResponse>
//
//    //1. monitoring
//     @GET("Trackme_payroll/api/LoanVisitRecovery/GetAllDoneVisits")
//     suspend fun getAllDoneVisits(
//          @Query("StaffID") staffId: Int?,
//          @Query("flag") flag: String
//     ): Response<DoneVisitResponse>
//
//    //2. monitoring
//     @GET("Trackme_payroll/api/LoanVisitRecovery/GetALLPendingVisit")
//     suspend fun getAllPendingVisits(
//          @Query("StaffID") staffId: Int?,
//          @Query("flag") flag: String
//     ): Response<PendingVisitResponse>
//
//
//    @GET("Trackme_payroll/api/LoanVisitRecovery/GetAllVisitAgainList")
//    suspend fun getAllVisitAgainList(
//        @Query("staffId") staffId: Int
//    ): Response<VisitAgainResponse>
//
//
//    @POST("Trackme_payroll/api/LoanVisitRecovery/UpdateVisitAgainDelinquencyVisit")
//    fun updateVisitAgain(
//        @Body body: VisitAgainUpdateRequest
//    ): Call<ResponseBody>
//
//
//
//    @GET("Trackme_payroll/api/Applicants/GetLat_Log")
//     suspend fun getLatLong(
//          @Query("empId") staffId: String?
//     ): Response<LatLongResponse>
//
//
////     @POST("Trackme_payroll/api/Attendence/MarkAttendance")
////     fun markAttendance(
////          @Query("empId") empId: String?
////     ): Call<String>
//
//     @POST("Trackme_payroll/api/Attendence/MarkAttendance")
//     suspend fun markAttendance(@Query("empId") empId: String): Response<String>
//
//
//     @GET("Trackme_payroll/api/Attendence/GetLatestAttendance")
//     suspend fun getLatestAttendance(
//          @Query("empId") empId: String?
//     ): Response<LatestAttendanceResponse>
//
////     @GET("Trackme_payroll/api/Attendence/GetMonthlyAttendance")
////     suspend fun getMonthlyAttendance(
////          @Query("empId") empId: Int,
////          @Query("year") year: Int,
////          @Query("month") month: Int
////     ): Response<List<Attendance>>
//
//     @GET("Trackme_payroll/api/Attendence/GetMonthlyAttendance")
//     suspend fun getMonthlyAttendance(
//          @Query("empId") empId: Int,
//          @Query("year") year: Int,
//          @Query("month") month: Int
//     ): Response<List<Attendance>>
//
//
//     @POST("Trackme_payroll/api/LoanVisitRecovery/InsertCallDelinquencyVisit")
//     suspend fun insertCallDelinquencyVisit(
//          @Body request: CallDelinquencyVisitRequest
//     ): Response<CallDelinquencyVisitResponse>
//
//
//     // fetch all users
////     @GET("User/GetAllUsers")
////     fun getAllUsers(): Call<List<User>>
//
//    @GET("Trackme_payroll/api/TaskManagement/GetUserWithBankDetails")
//    fun getUsersWithBankDetails(): Call<GetUsersResponse>
//
//
////     @POST("TaskManagement/InsertTask")
////     fun insertTask(@Body task: TaskRequest): Call<ApiResponse>
//
////     //insert task
////     @POST("Trackme_payroll/api/TaskManagement/InsertTask")
////     fun insertTask(@Body task: TaskRequest): Call<String>
//
//    @POST("Trackme_payroll/api/TaskManagement/InsertTask")
//    fun insertTaskNew(@Body request: TaskInsertRequest): Call<String>
//
//
//    //get all task assign by you
//    @GET("Trackme_payroll/api/TaskManagement/GetTasks")
//    fun getTasks(@Query("staffId") staffId: Int): Call<List<TaskModel>>
//
//    //get all task assign to you
//    @GET("Trackme_payroll/api/TaskManagement/GetAssignedTasks")
//    fun getAssignedTasks(
//        @Query("staffId") empNo: Int
//    ): Call<List<TaskModel>>
//
//
//
//    @POST("Trackme_payroll/api/TaskManagement/UpdateTask")
//    fun updateTask(
//        @Query("taskId") taskId: Int,
//        @Query("completionDescription") completion: String,
//        @Query("otherRemarks") remarks: String?
//    ): Call<ResponseBody>
//
//
//    @POST("Trackme_payroll/api/TaskManagement/InsertMYTasks")
//    fun insertMyTask(
//        @Body request: MyTaskRequest
//    ): Call<ResponseBody>
//
//    @GET("Trackme_payroll/api/TaskManagement/GetMYTasks")
//    fun getMyTasks(
//        @Query("staffId") empNo: Int
//    ): Call<List<TaskModel>>
//
//}
