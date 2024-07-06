package com.finwise.finwise_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class History : Fragment() {

    private lateinit var financialDataAdapter: FinancialDataAdapter
    private val financialDataList = mutableListOf<FinancialData>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        financialDataAdapter = FinancialDataAdapter(financialDataList)
        recyclerView.adapter = financialDataAdapter

        fetchFinancialData()

        return view
    }

    override fun onResume() {
        super.onResume()
        //fetchFinancialData()
    }
    private fun fetchFinancialData() {
        financialDataList.clear()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userFinanceRef = db.collection("users").document(userId)
                .collection("finance")

            userFinanceRef.get()
                .addOnSuccessListener { financeDocuments ->
                    for (financeDocument in financeDocuments) {
                        val year = financeDocument.id

                        for (month in 1..12) {
                            val monthName = getMonthName(month)
                            val finDocumentRef = userFinanceRef.document(year.toString())
                                .collection(monthName).document("fin")

                            finDocumentRef.get()
                                .addOnSuccessListener { finDocument ->
                                    Log.d(TAG, "Iterating for $year - $monthName")
                                    if (monthName == "April") {
                                        val finData = finDocument.data
                                        if (finData != null) {
                                            val income = finData["income"]
                                            val budget = finData["budget"]
                                            Log.d(TAG, "April Income: $income, Budget: $budget")
                                        }
                                    }

                                        val finData = finDocument.data
                                        if (finData != null) {
                                            val income = finData["income"] as? Double ?: 0.0
                                            val budget = finData["budget"] as? Double ?: 0.0
                                            val totalSpending =
                                                finData["spending"] as? Double ?: 0.0
                                            val savings = finData["savings"] as? Double ?: 0.0

                                            if (income > 0 || budget > 0 || totalSpending > 0 || savings > 0) {
                                                val financialData = FinancialData(
                                                    year = year.toString(),
                                                    month = getMonthName(month),
                                                    income = income,
                                                    budget = budget,
                                                    totalSpending = totalSpending,
                                                    savings = savings
                                                )
                                                financialDataList.add(financialData)
                                                financialDataAdapter.notifyItemInserted(financialDataList.size - 1)
                                            }
                                        }

                                }
                        }

                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting finance documents: ", exception)
                }
        }
    }
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }
    }


    companion object {
        private const val TAG = "HistoryFragment"
    }
}
