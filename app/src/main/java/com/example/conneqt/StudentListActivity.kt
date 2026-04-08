package com.example.conneqt

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conneqt.databinding.ActivityStudentListBinding

class StudentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private val studentList = mutableListOf<StudentModel>()
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val classId   = intent.getStringExtra("CLASS_ID") ?: ""
        val className = intent.getStringExtra("CLASS_NAME") ?: "Students"

        // ── Toolbar ────────────────────────────────────────────────
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className.replace("\n", " — ")
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Header ─────────────────────────────────────────────────
        binding.tvHeaderClassName.text = className.replace("\n", " — ")

        // ── Adapter ────────────────────────────────────────────────
        studentAdapter = StudentAdapter(studentList)
        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(this@StudentListActivity)
            adapter       = studentAdapter
        }

        // ── Search ─────────────────────────────────────────────────
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                studentAdapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        if (classId.isEmpty()) {
            binding.tvHeaderStudentCount.text = "Error: no class ID"
            return
        }

        loadStudents(classId)
    }

    private fun loadStudents(classId: String) {
        // Load directly from local storage — instant, no network needed
        val students = LocalStorage.getStudents(this, classId)

        studentList.clear()

        // Sort by student number
        val sorted = students.sortedWith(compareBy {
            it.studentNo.filter { c -> c.isDigit() }.toLongOrNull() ?: Long.MAX_VALUE
        })

        studentList.addAll(sorted)
        binding.tvHeaderStudentCount.text = "${studentList.size} students"
        studentAdapter.notifyDataSetChanged()

        if (studentList.isEmpty()) {
            binding.layoutEmptyStudents.visibility = View.VISIBLE
            binding.rvStudents.visibility          = View.GONE
        } else {
            binding.layoutEmptyStudents.visibility = View.GONE
            binding.rvStudents.visibility          = View.VISIBLE
        }
    }
}