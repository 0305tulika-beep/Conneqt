package com.example.conneqt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conneqt.databinding.ItemClassCardBinding

class ClassAdapter(
    private val classList: List<ClassModel>,
    private val onClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ViewHolder>() {

    // ViewHolder holds references to views inside ONE card
    // Created ~8 times total, then recycled as you scroll
    inner class ViewHolder(val binding: ItemClassCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Called when RecyclerView needs a brand new card view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Called every time a card scrolls into view
    // Bind your ClassModel data to the views here
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val classModel = classList[position]

        holder.binding.apply {
            tvClassName.text    = classModel.name
            tvStudentCount.text = "${classModel.studentCount} students"
            tvUploadDate.text   = classModel.uploadDate
            tvClassIcon.text    = classModel.emoji

            // Green dot — visible if class is active
            dotActive.visibility = if (classModel.isActive)
                android.view.View.VISIBLE else android.view.View.GONE

            // Tap on the whole card
            root.setOnClickListener { onClick(classModel) }
        }
    }

    // How many items total
    override fun getItemCount() = classList.size
}
