package com.finwise.finwise_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.finwise.finwise_app.SmsUtils.Transaction

class UpiAdapter : RecyclerView.Adapter<UpiAdapter.UpiViewHolder>() {

    private var transactions: List<Transaction> = emptyList()

    fun setData(transactions: List<Transaction>) {
        this.transactions = transactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return UpiViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpiViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    class UpiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val transactionIcon: ImageView = itemView.findViewById(R.id.transactionIcon)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)

        fun bind(transaction: Transaction) {
            // Example: Set transaction icon based on type or default icon
            // transactionIcon.setImageResource(R.drawable.your_transaction_icon)

            // Set amount and date text
            amountTextView.text = "Rs. ${transaction.amount}"
            dateTextView.text = transaction.date
        }
    }
}
