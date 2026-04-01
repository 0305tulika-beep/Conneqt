package com.example.conneqt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.signup_screen)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etName            = findViewById(R.id.etName)
        etEmail           = findViewById(R.id.etEmail)
        etPassword        = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignup         = findViewById(R.id.btnSignup)
        progressBar       = findViewById(R.id.progressBar)
        tvGoToLogin       = findViewById(R.id.tvGoToLogin)

        btnSignup.setOnClickListener {
            val name            = etName.text.toString().trim()
            val email           = etEmail.text.toString().trim()
            val password        = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validateInputs(name, email, password, confirmPassword)) return@setOnClickListener
            createAccount(name, email, password)
        }

        // "Already have an account? Sign in" → go back to LoginActivity
        tvGoToLogin.setOnClickListener {
            finish() // Simply pop SignupActivity — LoginActivity is already underneath
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            etName.error = "Name is required"
            etName.requestFocus()
            return false
        }
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            etEmail.requestFocus()
            return false
        }
        if (password.isEmpty() || password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }
        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }
        return true
    }

    private fun createAccount(name: String, email: String, password: String) {
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser!!

                    // Step 1: Set display name on the Firebase Auth profile
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user.updateProfile(profileUpdate)
                        .addOnCompleteListener {
                            // Step 2: Save professor data to Firestore
                            saveProfessorToFirestore(user.uid, name, email)
                        }
                } else {
                    setLoading(false)
                    val errorMsg = when {
                        task.exception?.message?.contains("email address is already") == true ->
                            "An account with this email already exists"
                        task.exception?.message?.contains("network") == true ->
                            "Network error. Check your connection."
                        else -> task.exception?.message ?: "Sign up failed"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveProfessorToFirestore(uid: String, name: String, email: String) {
        val professorData = hashMapOf(
            "uid"       to uid,
            "name"      to name,
            "email"     to email,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("professors")
            .document(uid)
            .set(professorData)
            .addOnSuccessListener {
                setLoading(false)
                navigateToDashboard()
            }
            .addOnFailureListener {
                setLoading(false)
                // Firestore write failed but Auth account exists — still proceed
                // The Firestore doc can be recreated later from Auth data
                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear entire back stack — professor cannot press Back into signup
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility    = if (isLoading) View.VISIBLE else View.GONE
        btnSignup.isEnabled       = !isLoading
        btnSignup.text            = if (isLoading) "" else "CREATE ACCOUNT"
    }
}