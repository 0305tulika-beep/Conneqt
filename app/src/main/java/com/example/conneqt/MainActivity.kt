package com.example.conneqt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.conneqt.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val classList = mutableListOf<ClassModel>()
    private lateinit var classAdapter: ClassAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        db = FirebaseFirestore.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupGreeting()
        setupProfessorName()
        setupRecyclerView()
        setupFab()
        setupProfileIcon()
        loadClasses()
    }

    // ── Greeting changes based on time of day ──────────────────────────
    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning ☀️"
            hour < 17 -> "Good Afternoon 🌤️"
            else      -> "Good Evening 🌙"
        }
        binding.tvGreeting.text = greeting
    }

    // ── Load professor name from Firebase Auth ─────────────────────────
    private fun setupProfessorName() {
        val user = auth.currentUser
        val name = user?.displayName
        if (!name.isNullOrEmpty()) {
            // Show first name only — e.g. "Dr. Sharma" → "Dr. Sharma"
            binding.tvProfName.text = name
        } else {
            binding.tvProfName.text = "Professor"
        }
    }

    // ── RecyclerView with delete confirmation dialog ───────────────────
    private fun setupRecyclerView() {
        classAdapter = ClassAdapter(
            classList = classList,
            onClick = { classModel ->
                val intent = Intent(this, StudentListActivity::class.java)
                intent.putExtra("CLASS_NAME", classModel.name)
                intent.putExtra("CLASS_ID", classModel.id)
                startActivity(intent)
            },
            onDelete = { classModel, position ->
                // Show confirmation dialog before deleting
                showDeleteConfirmation(classModel, position)
            }
        )

        binding.rvClasses.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = classAdapter
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────
    private fun showDeleteConfirmation(classModel: ClassModel, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete \"${classModel.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                classList.removeAt(position)
                classAdapter.notifyItemRemoved(position)
                updateUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ── FAB opens NewClassActivity ─────────────────────────────────────
    // Add this at the top of the class (like you did filePickerLauncher in NewClassActivity)
    // Add this at the top of the class (like you did filePickerLauncher in NewClassActivity)
    private val newClassLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val newClass = ClassModel(
                id           = data.getStringExtra("CLASS_ID") ?: "",
                name         = data.getStringExtra("CLASS_NAME") ?: "",
                studentCount = data.getIntExtra("STUDENT_COUNT", 0),
                uploadDate   = data.getStringExtra("UPLOAD_DATE") ?: ""
            )
            classList.add(newClass)
            classAdapter.notifyItemInserted(classList.size - 1)
            updateUI()
        }
    }

    // Then update setupFab() to use the launcher
    private fun setupFab() {
        binding.fabNewClass.setOnClickListener {
            newClassLauncher.launch(Intent(this, NewClassActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    // ── Profile avatar opens ProfileActivity ───────────────────────────
    private fun setupProfileIcon() {
        binding.cardAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    // ── Reload data when returning from NewClassActivity ──────────────
    override fun onResume() {
        super.onResume()
        setupGreeting()     // refresh greeting in case time changed
        setupProfessorName() // refresh name in case profile was updated
    }

    // ── Dummy data — replace with Firestore later ──────────────────────
    private fun loadClasses() {
        db.collection("classes")
            .get()
            .addOnSuccessListener { result ->
                classList.clear()
                for (doc in result) {
                    classList.add(ClassModel(
                        id           = doc.getString("id") ?: doc.id,
                        name         = doc.getString("name") ?: "",
                        studentCount = (doc.getLong("studentCount") ?: 0L).toInt(),
                        uploadDate   = doc.getString("uploadDate") ?: ""
                    ))
                }
                classAdapter.notifyDataSetChanged()
                updateUI()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Update stat counters + empty state visibility ──────────────────
    private fun updateUI() {
        binding.tvClassCount.text   = classList.size.toString()
        binding.tvStudentCount.text = classList.sumOf { it.studentCount }.toString()

        if (classList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvClasses.visibility        = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvClasses.visibility        = View.VISIBLE
        }
    }
}