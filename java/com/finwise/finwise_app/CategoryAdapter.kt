package com.finwise.finwise_app

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class CategoryAdapter(private val context: Context, private val categories: MutableList<Category>, private val listener: OnItemClickListener,private val auth: FirebaseAuth) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    interface OnItemClickListener {
        fun onItemClick(category: Category)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)

        if (position == categories.size - 1 && category.name == "+ Add") {
            holder.itemView.setOnClickListener {
                showAddCategoryDialog()
            }
        } else {
            holder.itemView.setOnClickListener {
                listener.onItemClick(category)
            }
        }
    }

    private fun showAddCategoryDialog() {
        val inputField = EditText(context)
        inputField.hint = "Enter category or sub-category name"

        AlertDialog.Builder(context)
            .setTitle("Add New Category")
            .setView(inputField)
            .setPositiveButton("Add") { dialog, _ ->
                val categoryName = inputField.text.toString()
                if (categoryName.isNotEmpty()) {
                    // Create a new Category object and add it to Firestore
                    val newCategory = Category(categoryName, R.drawable.coin, auth.currentUser?.uid ?: "")
                    addNewCategory(newCategory)
                } else {
                    Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun addNewCategory(newCategory: Category) {
        categories.add(categories.size - 1, newCategory) // Add the newCategory directly
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        val categoryImage: ImageView = itemView.findViewById(R.id.categoryImage)

        fun bind(category: Category) {
            categoryName.text = category.name
            categoryImage.setImageResource(category.imageResource)
        }
    }
}