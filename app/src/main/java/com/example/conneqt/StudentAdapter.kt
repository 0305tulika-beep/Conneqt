package com.example.conneqt

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conneqt.databinding.ItemStudentRowBinding

class StudentAdapter(
    private val studentList: MutableList<StudentModel>
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    // Avatar background colors — cycles through for each student
    private val avatarColors = listOf(
        "#1A3DDC84",  // green tint
        "#1A2979FF",  // blue tint
        "#1AFF6D00",  // orange tint
        "#1AD500F9"   // purple tint
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

            // ── AVATAR: first letter + cycling color ──
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

            // ── SMS STATUS BADGE ──
            // Shows different text + color based on where the message is
            when (student.smsStatus) {
                SmsStatus.NONE -> {
                    tvSmsStatus.visibility = View.GONE
                }
                SmsStatus.SENT -> {
                    tvSmsStatus.visibility = View.VISIBLE
                    tvSmsStatus.text = "✓ Sent"
                    tvSmsStatus.setTextColor(android.graphics.Color.parseColor("#8892A4"))
                }
                SmsStatus.DELIVERED -> {
                    tvSmsStatus.visibility = View.VISIBLE
                    tvSmsStatus.text = "✓✓ Delivered"
                    tvSmsStatus.setTextColor(android.graphics.Color.parseColor("#3DDC84"))
                }
                SmsStatus.READ -> {
                    tvSmsStatus.visibility = View.VISIBLE
                    tvSmsStatus.text = "✓✓ Read"
                    tvSmsStatus.setTextColor(android.graphics.Color.parseColor("#2979FF"))
                }
                SmsStatus.REPLIED -> {
                    tvSmsStatus.visibility = View.VISIBLE
                    tvSmsStatus.text = "↩ Replied"
                    tvSmsStatus.setTextColor(android.graphics.Color.parseColor("#FF6D00"))
                }
            }

            // ── SMS BUTTON ──
            // Opens the phone's native SMS app with student number pre-filled
            btnSms.setOnClickListener {
                val smsUri = Uri.parse("smsto:${student.phone}")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri)
                intent.putExtra("sms_body", "")
                context.startActivity(intent)
            }

            // ── CALL BUTTON ──
            // Opens the phone dialer with student number pre-filled
            btnCall.setOnClickListener {
                val callUri = Uri.parse("tel:${student.phone}")
                val intent = Intent(Intent.ACTION_DIAL, callUri)
                context.startActivity(intent)
            }
        }
    }

    // Search/filter function — called when professor types in search bar
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