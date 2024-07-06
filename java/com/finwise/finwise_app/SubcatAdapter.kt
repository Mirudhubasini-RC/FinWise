package com.finwise.finwise_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubcatAdapter(
    private val subcategories: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SubcatAdapter.SubcategoryViewHolder>() {

    inner class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subcategoryTextView: TextView = itemView.findViewById(R.id.subcategoryTextView)

        fun bind(subcategory: String) {
            subcategoryTextView.text = subcategory
            itemView.setOnClickListener {
                onItemClick(subcategory)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subcat, parent, false)
        return SubcategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        holder.bind(subcategories[position])
    }

    override fun getItemCount(): Int {
        return subcategories.size
    }
}
