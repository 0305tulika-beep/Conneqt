package com.example.conneqt

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.conneqt.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val classList = mutableListOf<ClassModel>()
    private lateinit var classAdapter: ClassAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupGreeting()
        setupProfessorName()
        setupRecyclerView()
        setupFab()
        setupProfileIcon()
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good Morning ☀️"
            hour < 17 -> "Good Afternoon 🌤️"
            else      -> "Good Evening 🌙"
        }
    }

    private fun setupProfessorName() {
        val name = auth.currentUser?.displayName
        binding.tvProfName.text = if (!name.isNullOrEmpty()) name else "Professor"
    }

    private fun setupRecyclerView() {
        classAdapter = ClassAdapter(
            classList = classList,
            onClick   = { classModel ->
                val intent = Intent(this, StudentListActivity::class.java)
                intent.putExtra("CLASS_NAME", classModel.name)
                intent.putExtra("CLASS_ID",   classModel.id)
                startActivity(intent)
            },
            onDelete  = { classModel, position ->
                showDeleteConfirmation(classModel, position)
            }
        )

        binding.rvClasses.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter       = classAdapter
        }
    }

    private fun showDeleteConfirmation(classModel: ClassModel, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete \"${classModel.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                LocalStorage.deleteClass(this, classModel.id)
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

    // ── Plain startActivity — onResume handles the refresh ────────────
    private fun setupFab() {
        binding.fabNewClass.setOnClickListener {
            startActivity(Intent(this, NewClassActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupProfileIcon() {
        binding.cardAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    // ── Fires every time you return to this screen ────────────────────
    override fun onResume() {
        super.onResume()
        setupGreeting()
        setupProfessorName()
        android.util.Log.d("MAIN", "onResume fired, loading classes...")
        loadClasses()
    }

    private fun loadClasses() {
        classList.clear()
        val loaded = LocalStorage.getAllClasses(this)
        android.util.Log.d("MAIN", "Loaded ${loaded.size} classes from storage")
        classList.addAll(loaded)
        classAdapter.notifyDataSetChanged()
        updateUI()
    }

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