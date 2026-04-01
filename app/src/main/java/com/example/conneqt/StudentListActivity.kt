package com.example.conneqt

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conneqt.databinding.ActivityStudentListBinding

class StudentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentListBinding
    private lateinit var studentAdapter: StudentAdapter

    private val originalList = mutableListOf<StudentModel>()
    private val displayList  = mutableListOf<StudentModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val className = intent.getStringExtra("CLASS_NAME") ?: "Class"
        val classId   = intent.getStringExtra("CLASS_ID")   ?: ""

        setupToolbar(className)
        setupRecyclerView()
        setupSearch()
        loadStudents(classId)
    }

    private fun setupToolbar(className: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(displayList)
        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(this@StudentListActivity)
            adapter = studentAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            studentAdapter.filter(text.toString(), originalList)
            if (displayList.isEmpty()) {
                binding.layoutEmptyStudents.visibility = View.VISIBLE
                binding.rvStudents.visibility = View.GONE
            } else {
                binding.layoutEmptyStudents.visibility = View.GONE
                binding.rvStudents.visibility = View.VISIBLE
            }
        }
    }

    private fun loadStudents(classId: String) {
        val dummyStudents = listOf(
            StudentModel("1", "Aryan Mehta", "2021-CS-001", "+91 98765 43210", "aryan@email.com"),
            StudentModel("2", "Riya Sharma", "2021-CS-002", "+91 91234 56789", "riya@email.com"),
            StudentModel("3", "Karan Singh", "2021-CS-003", "+91 99887 76655", "karan@email.com"),
            StudentModel("4", "Priya Patel", "2021-CS-004", "+91 88776 65544", "priya@email.com"),
            StudentModel("5", "Rohit Kumar", "2021-CS-005", "+91 77665 54433", "rohit@email.com"),
            StudentModel("6", "Sneha Gupta", "2021-CS-006", "+91 66554 43322", "sneha@email.com"),
            StudentModel("7", "Amit Verma",  "2021-CS-007", "+91 55443 32211", "amit@email.com"),
            StudentModel("8", "Pooja Reddy", "2021-CS-008", "+91 44332 21100", "pooja@email.com"),
        )

        originalList.addAll(dummyStudents)
        displayList.addAll(dummyStudents)
        studentAdapter.notifyDataSetChanged()

        updateHeaderStats()
        updateEmptyState()
    }

    private fun updateHeaderStats() {
        val total = originalList.size
        binding.tvHeaderStudentCount.text = "$total students"
    }

    private fun updateEmptyState() {
        if (originalList.isEmpty()) {
            binding.layoutEmptyStudents.visibility = View.VISIBLE
            binding.rvStudents.visibility          = View.GONE
        } else {
            binding.layoutEmptyStudents.visibility = View.GONE
            binding.rvStudents.visibility          = View.VISIBLE
        }
    }
}