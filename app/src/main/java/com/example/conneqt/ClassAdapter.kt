package com.example.conneqt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conneqt.databinding.ItemClassCardBinding

class ClassAdapter(
    private val classList: MutableList<ClassModel>,  // MutableList so we can remove items
    private val onClick: (ClassModel) -> Unit,
    private val onDelete: (ClassModel, Int) -> Unit  // new delete callback
) : RecyclerView.Adapter<ClassAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemClassCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val classModel = classList[position]

        holder.binding.apply {
            tvClassName.text    = classModel.name
            tvStudentCount.text = "${classModel.studentCount} students"
            tvUploadDate.text   = classModel.uploadDate
            tvClassIcon.text    = classModel.emoji

            dotActive.visibility = if (classModel.isActive)
                android.view.View.VISIBLE else android.view.View.GONE

            root.setOnClickListener { onClick(classModel) }

            // Delete button — passes the model and current position back
            btnDeleteClass.setOnClickListener {
                val currentPosition = holder.bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_ID.toInt()) {
                    onDelete(classModel, currentPosition)
                }
            }
        }
    }

    override fun getItemCount() = classList.size
}