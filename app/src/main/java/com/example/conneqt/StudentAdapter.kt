package com.example.conneqt

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.conneqt.databinding.ItemStudentRowBinding

class StudentAdapter(
    private val fullList: MutableList<StudentModel>
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    private val displayList = mutableListOf<StudentModel>().apply { addAll(fullList) }

    private val avatarColors = listOf(
        "#3DDC84", "#2979FF", "#FF6D00", "#D500F9", "#FF1744", "#00BCD4"
    )

    inner class ViewHolder(val binding: ItemStudentRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemStudentRowBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun getItemCount() = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = displayList[position]
        holder.binding.apply {

            // ── Avatar letter + color ──────────────────────────────
            val color = avatarColors[position % avatarColors.size]
            tvAvatar.text = if (student.name.isNotEmpty())
                student.name.first().uppercaseChar().toString() else "?"
            tvAvatar.setBackgroundColor(
                android.graphics.Color.parseColor(color)
            )

            // ── Student info ───────────────────────────────────────
            tvStudentName.text  = student.name
            tvStudentNo.text    = student.studentNo
            tvStudentPhone.text = student.phone
            tvStudentEmail.text = student.email

            // ── WhatsApp button ────────────────────────────────────
            btnSms.setOnClickListener {
                val phone = student.phone
                    .replace(" ", "")
                    .replace("-", "")
                    .replace("+", "")

                // Add country code 91 (India) if not already present
                val fullPhone = if (phone.startsWith("91") && phone.length > 10)
                    phone else "91$phone"

                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/$fullPhone")
                        setPackage("com.whatsapp")
                    }
                    it.context.startActivity(intent)
                } catch (e: Exception) {
                    // WhatsApp not installed — open in browser
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$fullPhone"))
                    it.context.startActivity(intent)
                }
            }

            // ── Call button ────────────────────────────────────────
            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:${student.phone}"))
                it.context.startActivity(intent)
            }

            // ── Email button ───────────────────────────────────────
            btnEmail.setOnClickListener {
                if (student.email.isEmpty()) {
                    Toast.makeText(it.context,
                        "No email address for this student", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${student.email}")
                    putExtra(Intent.EXTRA_SUBJECT, "Regarding your studies")
                    putExtra(Intent.EXTRA_TEXT, "Dear ${student.name},\n\n")
                }
                try {
                    it.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(it.context,
                        "No email app found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Filter by name or student number ──────────────────────────────
    fun filter(query: String) {
        displayList.clear()
        if (query.isEmpty()) {
            displayList.addAll(fullList)
        } else {
            val q = query.lowercase()
            displayList.addAll(fullList.filter {
                it.name.lowercase().contains(q) ||
                        it.studentNo.lowercase().contains(q)
            })
        }
        notifyDataSetChanged()
    }
}