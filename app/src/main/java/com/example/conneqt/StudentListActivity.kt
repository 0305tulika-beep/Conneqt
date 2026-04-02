package com.example.conneqt

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conneqt.databinding.ActivityStudentListBinding
import com.google.firebase.firestore.FirebaseFirestore

class StudentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private lateinit var db: FirebaseFirestore
    private val studentList = mutableListOf<StudentModel>()
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        val classId   = intent.getStringExtra("CLASS_ID") ?: ""
        val className = intent.getStringExtra("CLASS_NAME") ?: "Students"

        Log.d("STUDENTS", "Opening class: '$className' with id: '$classId'")

        // ── Toolbar ────────────────────────────────────────────────
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className.replace("\n", " — ")
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Header card ────────────────────────────────────────────
        binding.tvHeaderClassName.text = className.replace("\n", " — ")

        // ── Adapter + RecyclerView ─────────────────────────────────
        studentAdapter = StudentAdapter(studentList)
        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(this@StudentListActivity)
            adapter = studentAdapter
        }

        // ── Search filter ──────────────────────────────────────────
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                studentAdapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ── Load students ──────────────────────────────────────────
        if (classId.isEmpty()) {
            Log.e("STUDENTS", "Class ID is empty! Cannot load students.")
            binding.tvHeaderStudentCount.text = "Error: No class ID"
            return
        }

        loadStudents(classId)
    }

    private fun loadStudents(classId: String) {
        Log.d("STUDENTS", "Fetching from Firestore: classes/$classId/students")

        db.collection("classes")
            .document(classId)
            .collection("students")
            .get()
            .addOnSuccessListener { result ->
                Log.d("STUDENTS", "Firestore returned ${result.size()} documents")

                studentList.clear()

                // ── Correctly map each document to StudentModel ────────
                val students = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: ""
                    if (name.isEmpty()) {
                        Log.w("STUDENTS", "Skipping doc ${doc.id} — empty name")
                        return@mapNotNull null
                    }
                    val student = StudentModel(
                        id        = doc.getString("id") ?: doc.id,
                        name      = name,
                        studentNo = doc.getString("studentNo") ?: "",
                        phone     = doc.getString("phone") ?: "",
                        email     = doc.getString("email") ?: ""
                    )
                    Log.d("STUDENTS", "Loaded: ${student.name} | ${student.studentNo}")
                    student  // ← this is the return value of mapNotNull
                }

                // ── Sort by student number numerically ─────────────────
                val sorted = students.sortedWith(compareBy {
                    it.studentNo.filter { c -> c.isDigit() }
                        .toLongOrNull() ?: Long.MAX_VALUE
                })

                studentList.addAll(sorted)

                Log.d("STUDENTS", "Total students loaded: ${studentList.size}")

                // ── Update header ──────────────────────────────────────
                binding.tvHeaderStudentCount.text = "${studentList.size} students"

                // ── Refresh adapter ────────────────────────────────────
                studentAdapter.notifyDataSetChanged()

                // ── Show/hide empty state ──────────────────────────────
                if (studentList.isEmpty()) {
                    binding.layoutEmptyStudents.visibility = View.VISIBLE
                    binding.rvStudents.visibility          = View.GONE
                } else {
                    binding.layoutEmptyStudents.visibility = View.GONE
                    binding.rvStudents.visibility          = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("STUDENTS", "Firestore fetch failed: ${e.message}")
                binding.tvHeaderStudentCount.text = "Failed to load"
            }
    }
}