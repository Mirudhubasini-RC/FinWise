package com.finwise.finwise_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FinancialDataAdapter(private val financialDataList: List<FinancialData>) :
    RecyclerView.Adapter<FinancialDataAdapter.FinancialDataViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FinancialDataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_financial_data, parent, false)
        return FinancialDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: FinancialDataViewHolder, position: Int) {
        val data = financialDataList[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int {
        return financialDataList.size
    }

    class FinancialDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val yearTextView: TextView = itemView.findViewById(R.id.textYear)
        private val monthTextView: TextView = itemView.findViewById(R.id.textMonth)
        private val incomeTextView: TextView = itemView.findViewById(R.id.textIncome)
        private val budgetTextView: TextView = itemView.findViewById(R.id.textBudget)
        private val totalSpendingTextView: TextView = itemView.findViewById(R.id.textTotalSpending)
        private val totalSavingsTextView: TextView = itemView.findViewById(R.id.textTotalSaving)

        fun bind(data: FinancialData) {
            yearTextView.text = data.year
            monthTextView.text = data.month
            incomeTextView.text = data.income.toString()
            budgetTextView.text = data.budget.toString()
            totalSpendingTextView.text = data.totalSpending.toString()
            totalSavingsTextView.text = data.savings.toString()

        }
    }
}
