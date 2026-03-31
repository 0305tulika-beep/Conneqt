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

    // Keep TWO lists:
    // originalList → never changes, used for resetting search
    // displayList  → what adapter actually shows (filtered)
    private val originalList = mutableListOf<StudentModel>()
    private val displayList  = mutableListOf<StudentModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get class info passed from MainActivity when card was tapped
        val className = intent.getStringExtra("CLASS_NAME") ?: "Class"
        val classId   = intent.getStringExtra("CLASS_ID")   ?: ""

        setupToolbar(className)
        setupRecyclerView()
        setupSearch()
        loadStudents(classId)
    }

    private fun setupToolbar(className: String) {
        setSupportActionBar(binding.toolbar)
        // Shows back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className
        // Back arrow click → close this screen
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(displayList)

        binding.rvStudents.apply {
            // LinearLayoutManager = one student per row (vertical list)
            layoutManager = LinearLayoutManager(this@StudentListActivity)
            adapter = studentAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            studentAdapter.filter(text.toString(), originalList)

            // Show empty state if search returns nothing
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
        // TODO: Replace with real Excel data loaded from your database
        // For now, dummy data so you can see the UI working
        val dummyStudents = listOf(
            StudentModel("1", "Aryan Mehta",   "2021-CS-001", "+91 98765 43210", "aryan@email.com",   SmsStatus.DELIVERED),
            StudentModel("2", "Riya Sharma",   "2021-CS-002", "+91 91234 56789", "riya@email.com",    SmsStatus.READ),
            StudentModel("3", "Karan Singh",   "2021-CS-003", "+91 99887 76655", "karan@email.com",   SmsStatus.SENT),
            StudentModel("4", "Priya Patel",   "2021-CS-004", "+91 88776 65544", "priya@email.com",   SmsStatus.REPLIED),
            StudentModel("5", "Rohit Kumar",   "2021-CS-005", "+91 77665 54433", "rohit@email.com",   SmsStatus.NONE),
            StudentModel("6", "Sneha Gupta",   "2021-CS-006", "+91 66554 43322", "sneha@email.com",   SmsStatus.DELIVERED),
            StudentModel("7", "Amit Verma",    "2021-CS-007", "+91 55443 32211", "amit@email.com",    SmsStatus.NONE),
            StudentModel("8", "Pooja Reddy",   "2021-CS-008", "+91 44332 21100", "pooja@email.com",   SmsStatus.READ),
        )

        originalList.addAll(dummyStudents)
        displayList.addAll(dummyStudents)
        studentAdapter.notifyDataSetChanged()

        updateHeaderStats()
        updateEmptyState()
    }

    private fun updateHeaderStats() {
        val total = originalList.size
        val sent  = originalList.count { it.smsStatus != SmsStatus.NONE }

        binding.tvHeaderStudentCount.text = "$total students"
        binding.tvMsgSentCount.text       = "$sent/$total"
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