package com.education.brcmeducorn.activites

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.education.brcmeducorn.R
import com.education.brcmeducorn.api.apiModels.LoginResponse
import com.education.brcmeducorn.api.apiModels.RegisterRequest
import com.education.brcmeducorn.fragments.admin_dashboard_fragments.AddOrRemoveMembersFragment
import com.education.brcmeducorn.utils.ApiUtils
import com.education.brcmeducorn.utils.RealPathUtil
import com.education.brcmeducorn.utils.SharedPrefs
import com.education.brcmeducorn.utils.ValidRegistration
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


class RegisterActivity : AppCompatActivity() {
    private lateinit var txtBranch: Spinner
    private lateinit var txtSemester: Spinner
    private lateinit var imgUploadBtn: Button
    private lateinit var imgStudent: ImageView
    private lateinit var txtName: EditText
    private lateinit var txtbatch: EditText
    private lateinit var txtRegistrationNo: EditText
    private lateinit var txtUserMail: EditText
    private lateinit var txtPhoneNo: EditText
    private lateinit var countryCode: CountryCodePicker
    private lateinit var txtAddress: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtRollNo: EditText
    private lateinit var txtFather: EditText
    private lateinit var txtDOB: EditText
    private lateinit var btnUpdateDetails: Button

    private var editTextDOB: EditText? = null
    private var branchArray = arrayOf("Branch", "Cse", "Civil", "Mechanical", "Electrical")
    private var semesterArray = arrayOf(
        "Semester", "Sem1", "Sem2", "Sem3", "Sem4", "Sem5", "Sem6", "Sem7", "Sem8"
    )
    lateinit var prefs: SharedPrefs
    private lateinit var selectedImageUri: Uri
    private lateinit var imagePicker: ActivityResultLauncher<Intent>

    private lateinit var imagePart: MultipartBody.Part
    private lateinit var imagePath: String

    companion object {
        var student_user = 1
        var faculty_user = 0
        var admin_user = 0
        var roll: String = "student"
        var selectedBranch: String = "branch"
        var selectedSemester: String = "sem"
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        imgUploadBtn = findViewById(R.id.imgUploadBtn)
        imgStudent = findViewById(R.id.imgStudent)
        txtName = findViewById(R.id.txtName)
        txtBranch = findViewById(R.id.txtBranch)
        txtSemester = findViewById(R.id.txtSemester)
        txtbatch = findViewById(R.id.txtbatch)
        txtRegistrationNo = findViewById(R.id.txtRegistrationNo)
        txtUserMail = findViewById(R.id.txtUserMail)
        txtPhoneNo = findViewById(R.id.txtPhoneNo)
        countryCode = findViewById(R.id.countryCode)
        txtAddress = findViewById(R.id.txtAddress)
        txtFather = findViewById(R.id.txtFather)
        txtDOB = findViewById(R.id.txtDOB)
        txtPassword = findViewById(R.id.txtUserPass)
        txtRollNo = findViewById(R.id.txtRollNo)
        btnUpdateDetails = findViewById(R.id.btnUpdateDetails)
        val branchAdapter = ArrayAdapter(this, R.layout.spinner_item, branchArray)
        val semAdapter = ArrayAdapter(this, R.layout.spinner_item, semesterArray)
        getItemFromSpinner(branchArray, semesterArray)
        txtBranch.adapter = branchAdapter
        txtSemester.adapter = semAdapter

        imagePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val imageUri = data?.data
                    if (imageUri != null) {
                        selectedImageUri = imageUri
                        imagePath = RealPathUtil.getRealPath(this, selectedImageUri).toString()
                        val bitmap = BitmapFactory.decodeFile(imagePath)
                        imgStudent.setImageBitmap(bitmap)

                    }


                }
            }
        txtDOB.setOnClickListener {
            ValidRegistration.showDatePickerDialog(this, txtDOB)
        }
        btnUpdateDetails.setOnClickListener {
            if (::selectedImageUri.isInitialized) {
                registerRequest(this)
            } else {
                Toast.makeText(this, "please select an image", Toast.LENGTH_SHORT).show()
            }
        }
        imgUploadBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                imagePicker.launch(intent)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE

                )
            }

        }
    }


    private fun registerRequest(context: Context) {
        prefs = SharedPrefs(context)

        val email = txtUserMail.text?.toString()?.trim() ?: ""
        val phone = txtPhoneNo.text?.toString() ?: ""
        val countryCode = countryCode.selectedCountryCodeAsInt
        val pass = txtPassword.text?.toString()?.trim() ?: ""
        val role = "student"
        val rollno = txtRollNo.text?.toString()?.trim()?.uppercase() ?: ""
        val name = txtName.text?.toString()?.trim() ?: ""
        val semester = selectedSemester.trim()
        val branch = selectedBranch.trim()
        val address = txtAddress.text?.toString() ?: ""
        val batchYear = txtbatch.text?.toString()?.toIntOrNull() ?: 0
        val fathername = txtFather.text?.toString() ?: ""
        val registrationNo = txtRegistrationNo.text?.toString() ?: ""
        val dateOfBirth = txtDOB.text?.toString() ?: ""
        val age = 20

//        val filesDir = applicationContext.filesDir
//        val file = File(filesDir, "image.png")
//        val inputStream = contentResolver.openInputStream(selectedImageUri)
//        val outputStream = FileOutputStream(file)
//        inputStream!!.copyTo(outputStream)
//        val requestBody = file.asRequestBody("image/*".toMediaType())
//        val photo = MultipartBody.Part.createFormData("image", file.name, requestBody)

        val isValid = ValidRegistration.isUserValid(
            email, phone, countryCode, pass, role,
            rollno, name, "$branch|$semester",
            "photo", address, batchYear, fathername, registrationNo,
            dateOfBirth, age, this
        )
        if (isValid) {
            val emailRequestBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneRequestBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val countryCodeRequestBody =
                countryCode.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val passRequestBody = pass.toRequestBody("text/plain".toMediaTypeOrNull())
            val roleRequestBody = role.toRequestBody("text/plain".toMediaTypeOrNull())
            val rollnoRequestBody = rollno.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val semesterRequestBody = semester.toRequestBody("text/plain".toMediaTypeOrNull())
            val branchRequestBody =
                branch + semester.toRequestBody("text/plain".toMediaTypeOrNull())
            val addressRequestBody = address.toRequestBody("text/plain".toMediaTypeOrNull())
            val batchYearRequestBody =
                batchYear.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val fathernameRequestBody = fathername.toRequestBody("text/plain".toMediaTypeOrNull())
            val registrationNoRequestBody =
                registrationNo.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateOfBirthRequestBody = dateOfBirth.toRequestBody("text/plain".toMediaTypeOrNull())
            val ageRequestBody = age.toString().toRequestBody("text/plain".toMediaTypeOrNull())




            CoroutineScope(Dispatchers.Main).launch {
                val endpoint = "register"
                val method = "REGISTER"
                val userRequest = RegisterRequest(
                    emailRequestBody, phoneRequestBody, countryCodeRequestBody,
                    passRequestBody, roleRequestBody, rollnoRequestBody, nameRequestBody,
                    semesterRequestBody, "photo",
                    addressRequestBody, batchYearRequestBody, fathernameRequestBody,
                    registrationNoRequestBody, dateOfBirthRequestBody, ageRequestBody
                )
//                Log.d("hloo", userRequest.toString())

                val result = ApiUtils.register(endpoint, method, userRequest, imagePath)
                Log.d("hlooo", result.toString())

                if (result is LoginResponse) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "your register request has been sent successfully please wait until verify",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("hlooo", result.toString())
                }

            }

        } else {
            Toast.makeText(
                this@RegisterActivity,
                "Invalid input. Please check your details.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun savePrefs(response: LoginResponse) {

        prefs.saveString("token", response.token)
        prefs.saveString("name", response.member.name)
        prefs.saveString("rollNo", response.member.rollno)
        prefs.saveString("roll", response.member.role)

    }

    private fun checkRoll(response: LoginResponse): Boolean {
        return if (response.member.role == "admin" && response.member.role == "admin") {
            Log.d("hii", true.toString())
            true
        } else if (response.member.role == "student" && response.member.role == "student") {
            true
        } else response.member.role == "faculty" && response.member.role == "faculty"
    }

    private fun navigateDashboard(context: Context, msg: String, isTrue: Boolean) {
        if (isTrue) {
            if (student_user == 1 && faculty_user == 0 && admin_user == 0) {
                val intent = Intent(context, StudentDashboardActivity::class.java)
                ContextCompat.startActivity(context, intent, null)
            } else if (faculty_user == 1 && student_user == 0 && admin_user == 0) {
                val intent = Intent(context, FacultyDashboardActivity::class.java)
                ContextCompat.startActivity(context, intent, null)
            } else if (admin_user == 1 && faculty_user == 0 && student_user == 0) {
                val intent = Intent(context, AdminDashboardActivity::class.java)
                ContextCompat.startActivity(context, intent, null)
            } else {
                Toast.makeText(
                    context, msg, Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "please select your correct roll", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getItemFromSpinner(branchArray: Array<String>, semesterArray: Array<String>) {
        txtBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                AddOrRemoveMembersFragment.selectedBranch = branchArray[position]
                Toast.makeText(
                    this@RegisterActivity,
                    AddOrRemoveMembersFragment.selectedBranch,
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        txtSemester.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                AddOrRemoveMembersFragment.selectedSemester = semesterArray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Kuch nahi karna
            }
        }
    }


    private fun getPathFromURI(contentUri: Uri?): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri!!, projection, null, null, null)
        val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val filePath = cursor.getString(columnIndex)
        cursor.close()
        return filePath
    }


}