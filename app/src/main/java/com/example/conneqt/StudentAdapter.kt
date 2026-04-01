package com.example.conneqt

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conneqt.databinding.ItemStudentRowBinding

class StudentAdapter(
    private val studentList: MutableList<StudentModel>
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    private val avatarColors = listOf(
        "#1A3DDC84",
        "#1A2979FF",
        "#1AFF6D00",
        "#1AD500F9"
    )
    private val avatarTextColors = listOf(
        "#3DDC84", "#2979FF", "#FF6D00", "#D500F9"
    )

    inner class ViewHolder(val binding: ItemStudentRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = studentList[position]
        val context = holder.itemView.context

        holder.binding.apply {

            // ── AVATAR ──
            tvAvatar.text = student.name.first().uppercaseChar().toString()
            tvAvatar.setTextColor(
                android.graphics.Color.parseColor(avatarTextColors[position % avatarTextColors.size])
            )
            (tvAvatar.parent as androidx.cardview.widget.CardView)
                .setCardBackgroundColor(
                    android.graphics.Color.parseColor(avatarColors[position % avatarColors.size])
                )

            // ── STUDENT INFO ──
            tvStudentName.text = student.name
            tvStudentNo.text   = student.studentNo

            // ── SMS BUTTON ──
            btnSms.setOnClickListener {
                val smsUri = Uri.parse("smsto:${student.phone}")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri)
                intent.putExtra("sms_body", "")
                context.startActivity(intent)
            }

            // ── CALL BUTTON ──
            btnCall.setOnClickListener {
                val callUri = Uri.parse("tel:${student.phone}")
                val intent = Intent(Intent.ACTION_DIAL, callUri)
                context.startActivity(intent)
            }
        }
    }

    fun filter(query: String, originalList: List<StudentModel>) {
        studentList.clear()
        if (query.isEmpty()) {
            studentList.addAll(originalList)
        } else {
            val q = query.lowercase()
            studentList.addAll(originalList.filter {
                it.name.lowercase().contains(q) ||
                        it.studentNo.lowercase().contains(q) ||
                        it.phone.contains(q)
            })
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = studentList.size
}