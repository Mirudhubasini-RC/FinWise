package com.finwise.finwise_app

data class FinancialData(
    val year: String,
    val month: String,
    val income: Double,
    val budget: Double,
    val totalSpending: Double,
    val savings: Double
)
