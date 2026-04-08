package com.example.conneqt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.conneqt.databinding.ActivityNewClassBinding
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.UUID

class NewClassActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewClassBinding
    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedFileUri = uri
                val fileName = getFileName(uri)
                binding.tvFileName.text = "✓  $fileName"
                binding.tvFileName.visibility = View.VISIBLE
                binding.tvFileHint.text = "File selected"
                binding.tvFileHint.setTextColor(
                    android.graphics.Color.parseColor("#3DDC84")
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityNewClassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUploadArea()
        setupCreateButton()   // ← now implemented
        setupBackButton()
    }

    private fun setupUploadArea() {
        binding.layoutUploadArea.setOnClickListener { openFilePicker() }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Excel File"))
    }

    private fun getFileName(uri: Uri): String {
        var name = "selected_file.xlsx"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun setupCreateButton() {
        binding.btnCreateClass.setOnClickListener {
            val className = binding.etClassName.text.toString().trim()
            val classCode = binding.etClassCode.text.toString().trim()
            val section   = binding.etSection.text.toString().trim()

            if (!validateInputs(className, classCode, section)) return@setOnClickListener

            binding.progressBar.visibility   = View.VISIBLE
            binding.btnCreateClass.isEnabled = false

            Thread {
                val students = parseExcel(selectedFileUri!!)
                runOnUiThread {
                    if (students.isEmpty()) {
                        binding.progressBar.visibility   = View.GONE
                        binding.btnCreateClass.isEnabled = true
                        Toast.makeText(
                            this,
                            "No students found. Check Excel headers: Name, Student No, Phone, Email",
                            Toast.LENGTH_LONG
                        ).show()
                        return@runOnUiThread
                    }
                    saveLocally(className, classCode, section, students)
                }
            }.start()
        }
    }

    private fun parseExcel(uri: Uri): List<StudentModel> {
        val students = mutableListOf<StudentModel>()
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return students
            val workbook    = WorkbookFactory.create(inputStream)
            val sheet       = workbook.getSheetAt(0)

            val headerRow = sheet.getRow(0) ?: return students
            var nameCol      = -1
            var studentNoCol = -1
            var phoneCol     = -1
            var emailCol     = -1

            for (cell in headerRow) {
                val header = cell.toString().trim().lowercase()
                when {
                    header.contains("name")                              -> nameCol      = cell.columnIndex
                    header.contains("student") && header.contains("no") -> studentNoCol = cell.columnIndex
                    header.contains("phone") || header.contains("mobile") -> phoneCol   = cell.columnIndex
                    header.contains("email")                             -> emailCol     = cell.columnIndex
                }
            }

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                fun cellVal(col: Int): String {
                    if (col < 0) return ""
                    val cell = row.getCell(col) ?: return ""
                    return when (cell.cellType) {
                        CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
                        else             -> cell.toString().trim()
                    }
                }

                val name = cellVal(nameCol)
                if (name.isEmpty()) continue

                students.add(
                    StudentModel(
                        id        = UUID.randomUUID().toString(),
                        name      = name,
                        studentNo = cellVal(studentNoCol),
                        phone     = cellVal(phoneCol),
                        email     = cellVal(emailCol)
                    )
                )
            }
            workbook.close()

        } catch (e: Exception) {
            android.util.Log.e("EXCEL", "Parse error: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error reading Excel: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return students
    }

    private fun saveLocally(
        className: String,
        classCode: String,
        section: String,
        students: List<StudentModel>
    ) {
        val classId    = UUID.randomUUID().toString()
        val uploadDate = java.text.SimpleDateFormat(
            "MMM yyyy", java.util.Locale.getDefault()
        ).format(java.util.Date())

        val classModel = ClassModel(
            id           = classId,
            name         = "$className\n$classCode - Sec $section",
            studentCount = students.size,
            uploadDate   = uploadDate,
            emoji        = "📘",
            isActive     = true
        )

        LocalStorage.saveClass(this, classModel, students)

        binding.progressBar.visibility   = View.GONE
        binding.btnCreateClass.isEnabled = true

        Toast.makeText(this, "Class created with ${students.size} students!", Toast.LENGTH_SHORT).show()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun validateInputs(className: String, classCode: String, section: String): Boolean {
        if (className.isEmpty()) {
            binding.etClassName.error = "Required"
            binding.etClassName.requestFocus()
            return false
        }
        if (classCode.isEmpty()) {
            binding.etClassCode.error = "Required"
            binding.etClassCode.requestFocus()
            return false
        }
        if (section.isEmpty()) {
            binding.etSection.error = "Required"
            binding.etSection.requestFocus()
            return false
        }
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please upload an Excel sheet", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setupBackButton() {
        binding.tvGoBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}