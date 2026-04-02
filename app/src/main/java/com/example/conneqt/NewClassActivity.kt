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
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.UUID

class NewClassActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewClassBinding
    private lateinit var db: FirebaseFirestore
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
        db = FirebaseFirestore.getInstance()

        setupUploadArea()
        setupCreateButton()
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

    // ── Parse Excel ───────────────────────────────────────────────────
    private fun parseExcel(uri: Uri): List<StudentModel> {
        val students = mutableListOf<StudentModel>()
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return students
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            android.util.Log.d("EXCEL", "Total rows: ${sheet.lastRowNum}")

            val headerRow = sheet.getRow(0) ?: return students
            var nameCol = -1
            var studentNoCol = -1
            var phoneCol = -1
            var emailCol = -1

            for (cell in headerRow) {
                val header = cell.toString().trim().lowercase()
                android.util.Log.d("EXCEL", "Header col ${cell.columnIndex}: '$header'")
                when {
                    header.contains("name") -> nameCol = cell.columnIndex
                    header.contains("student") && header.contains("no")
                        -> studentNoCol = cell.columnIndex
                    header.contains("phone") || header.contains("mobile")
                        -> phoneCol = cell.columnIndex
                    header.contains("email") -> emailCol = cell.columnIndex
                }
            }

            android.util.Log.d(
                "EXCEL",
                "Columns → name:$nameCol, no:$studentNoCol, phone:$phoneCol, email:$emailCol"
            )

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                fun cellVal(col: Int): String {
                    if (col < 0) return ""
                    val cell = row.getCell(col) ?: return ""
                    return when (cell.cellType) {
                        CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
                        else -> cell.toString().trim()
                    }
                }

                val name = cellVal(nameCol)
                android.util.Log.d("EXCEL", "Row $i → name='$name'")
                if (name.isEmpty()) continue

                val student = StudentModel(
                    id        = UUID.randomUUID().toString(),
                    name      = name,
                    studentNo = cellVal(studentNoCol),
                    phone     = cellVal(phoneCol),
                    email     = cellVal(emailCol)
                )
                android.util.Log.d(
                    "EXCEL",
                    "Parsed: ${student.name} | ${student.studentNo} | ${student.phone} | ${student.email}"
                )
                students.add(student)
            }
            workbook.close()
            android.util.Log.d("EXCEL", "Total parsed: ${students.size}")

        } catch (e: Exception) {
            android.util.Log.e("EXCEL", "Parse error: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(
                    this, "Error reading Excel: ${e.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
        return students
    }

    private fun setupCreateButton() {
        binding.btnCreateClass.setOnClickListener {
            val className = binding.etClassName.text.toString().trim()
            val classCode = binding.etClassCode.text.toString().trim()
            val section   = binding.etSection.text.toString().trim()

            if (!validateInputs(className, classCode, section)) return@setOnClickListener

            binding.progressBar.visibility   = View.VISIBLE
            binding.btnCreateClass.isEnabled = false

            // Parse on background thread
            Thread {
                val students = parseExcel(selectedFileUri!!)
                android.util.Log.d("SAVE", "Parsed ${students.size} students, saving...")
                runOnUiThread {
                    saveToFirestore(className, classCode, section, students)
                }
            }.start()
        }
    }

    // ── Save to Firestore ─────────────────────────────────────────────
    private fun saveToFirestore(
        className: String,
        classCode: String,
        section: String,
        students: List<StudentModel>
    ) {
        val classId = UUID.randomUUID().toString()

        // Explicitly typed as <String, Any> to avoid type mismatch
        val classData = hashMapOf<String, Any>(
            "id"           to classId,
            "name"         to "$className\n$classCode - Sec $section",
            "studentCount" to students.size,
            "uploadDate"   to java.text.SimpleDateFormat(
                "MMM yyyy", java.util.Locale.getDefault()
            ).format(java.util.Date())
        )

        android.util.Log.d("SAVE", "Saving class '$className' with ${students.size} students")

        db.collection("classes").document(classId)
            .set(classData)
            .addOnSuccessListener {
                android.util.Log.d("SAVE", "Class doc saved. Saving ${students.size} students...")

                if (students.isEmpty()) {
                    android.util.Log.w("SAVE", "No students found in Excel!")
                    onSaveComplete(classId, classData, students)
                    return@addOnSuccessListener
                }

                var savedCount = 0
                var failCount  = 0

                students.forEach { student ->
                    // Explicitly typed map to avoid type issues
                    val studentData = hashMapOf<String, Any>(
                        "id"        to student.id,
                        "name"      to student.name,
                        "studentNo" to student.studentNo,
                        "phone"     to student.phone,
                        "email"     to student.email
                    )

                    db.collection("classes")
                        .document(classId)
                        .collection("students")
                        .document(student.id)
                        .set(studentData)
                        .addOnSuccessListener {
                            savedCount++
                            android.util.Log.d(
                                "SAVE",
                                "✓ Saved $savedCount/${students.size}: ${student.name}"
                            )
                            if (savedCount + failCount == students.size) {
                                onSaveComplete(classId, classData, students)
                            }
                        }
                        .addOnFailureListener { e ->
                            failCount++
                            android.util.Log.e(
                                "SAVE",
                                "✗ Failed ${student.name}: ${e.message}"
                            )
                            if (savedCount + failCount == students.size) {
                                onSaveComplete(classId, classData, students)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility   = View.GONE
                binding.btnCreateClass.isEnabled = true
                android.util.Log.e("SAVE", "Class save failed: ${e.message}")
                Toast.makeText(
                    this, "Failed to save class: ${e.message}", Toast.LENGTH_LONG
                ).show()
            }
    }

    // ── Called when all students are saved ────────────────────────────
    private fun onSaveComplete(
        classId: String,
        classData: HashMap<String, Any>,   // ← now correctly typed
        students: List<StudentModel>
    ) {
        runOnUiThread {
            binding.progressBar.visibility   = View.GONE
            binding.btnCreateClass.isEnabled = true

            val resultIntent = Intent().apply {
                putExtra("CLASS_ID",      classId)
                putExtra("CLASS_NAME",    classData["name"].toString())
                putExtra("STUDENT_COUNT", students.size)
                putExtra("UPLOAD_DATE",   classData["uploadDate"].toString())
            }
            setResult(Activity.RESULT_OK, resultIntent)

            Toast.makeText(
                this,
                "Class created with ${students.size} students!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun validateInputs(
        className: String,
        classCode: String,
        section: String
    ): Boolean {
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