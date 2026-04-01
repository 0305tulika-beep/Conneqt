package com.example.conneqt

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.conneqt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // ViewBinding lets you access XML views without findViewById
    // binding.rvClasses = your RecyclerView
    // binding.fabNewClass = your FAB button
    private lateinit var binding: ActivityMainBinding

    // This is your data list — later you'll load this from a database
    private val classList = mutableListOf<ClassModel>()
    private lateinit var classAdapter: ClassAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Inflate the XML layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Step 2: Setup RecyclerView
        setupRecyclerView()

        // Step 3: Setup FAB button
        setupFab()

        // Step 4: Load data (dummy for now, replace with DB later)
        loadClasses()
    }

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
                classList.removeAt(position)               // remove from list
                classAdapter.notifyItemRemoved(position)   // animate card out
                updateUI()                                 // update counts
            }
        )

        binding.rvClasses.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = classAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewClass.setOnClickListener {
            // TODO: Open file picker to select Excel sheet
            // For now just add a dummy class to test UI
            addDummyClass()
        }
    }

    private fun loadClasses() {
        // TODO: Replace this with real data from your database
        // For now, dummy data so you can see the UI working
        classList.addAll(
            listOf(
                ClassModel(id = "1", name = "Data Structures\nCS-301", studentCount = 42, uploadDate = "Jan 2025"),
                ClassModel(id = "2", name = "Mathematics\nMATH-201", studentCount = 38, uploadDate = "Feb 2025"),
                ClassModel(id = "3", name = "Physics\nPHY-101", studentCount = 55, uploadDate = "Mar 2025"),
            )
        )
        updateUI()
    }

    private fun addDummyClass() {
        classList.add(
            ClassModel(
                id = classList.size.toString(),
                name = "New Class ${classList.size + 1}",
                studentCount = 0,
                uploadDate = "Mar 2025"
            )
        )
        // Tell adapter new item was added at the end
        classAdapter.notifyItemInserted(classList.size - 1)
        updateUI()
    }

    private fun updateUI() {
        // Update stat boxes at the top
        binding.tvClassCount.text = classList.size.toString()
        binding.tvStudentCount.text = classList.sumOf { it.studentCount }.toString()

        // Show/hide empty state
        if (classList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvClasses.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvClasses.visibility = View.VISIBLE
        }
    }
}