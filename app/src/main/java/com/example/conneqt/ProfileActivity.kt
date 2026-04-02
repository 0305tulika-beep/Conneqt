package com.example.conneqt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.conneqt.databinding.ProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding  = ProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadProfileData()
        loadStats()
        setupLogout()
    }

    // ── Load name + email from Firebase Auth ───────────────────────────
    private fun loadProfileData() {
        val user = auth.currentUser ?: return

        val name  = user.displayName ?: "Professor"
        val email = user.email ?: ""

        binding.tvProfName.text  = name
        binding.tvProfEmail.text = email

        // Show first letter of name as avatar initial
        binding.tvInitial.text = if (name.isNotEmpty()) name[0].uppercaseChar().toString() else "P"
    }

    // ── Load class + student counts from Firestore ─────────────────────
    private fun loadStats() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("professors")
            .document(uid)
            .collection("classes")
            .get()
            .addOnSuccessListener { classSnapshot ->
//                val classCount = classSnapshot.size()
//                binding.tvTotalClasses.text = classCount.toString()

                // Count total students across all classes
                var totalStudents = 0
                var classesChecked = 0

//                if (classCount == 0) {
//                    binding.tvTotalStudents.text = "0"
//                    return@addOnSuccessListener
//                }

                for (classDoc in classSnapshot.documents) {
                    firestore.collection("professors")
                        .document(uid)
                        .collection("classes")
                        .document(classDoc.id)
                        .collection("students")
                        .get()
                        .addOnSuccessListener { studentSnapshot ->
                            totalStudents += studentSnapshot.size()
                            classesChecked++
//                            if (classesChecked == classCount) {
//                                // All classes checked — update UI
//                                binding.tvTotalStudents.text = totalStudents.toString()
//                            }
                        }
                }
            }
            .addOnFailureListener {
                // Firestore failed — show zeros, not a crash
//                binding.tvTotalClasses.text  = "0"
//                binding.tvTotalStudents.text = "0"
            }
    }

    // ── Logout with confirmation dialog ───────────────────────────────
    private fun setupLogout() {
        binding.rowLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { dialog, _ ->
                    auth.signOut()
                    // Clear back stack — can't press Back to get back in
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}