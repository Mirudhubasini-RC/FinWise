package com.finwise.finwise_app

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.*

class SmsUtils {

    companion object {

        // Function to fetch debit transactions from SMS inbox
        fun fetchDebitTransactions(context: Context): List<Transaction> {
            val transactions = mutableListOf<Transaction>()

            // Check if app has permission to read SMS
            if (!hasReadSmsPermission(context)) {
                // Handle lack of permission (request permission, show explanation, etc.)
                return transactions
            }

            // Uri for SMS content provider (inbox)
            val uri = Uri.parse("content://sms/inbox")

            // Projection for querying SMS database
            val projection = arrayOf(
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE
            )

            // Sort order to retrieve most recent SMS first
            val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC"

            // Query SMS content provider
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val bodyColumnIndex = cursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateColumnIndex = cursor.getColumnIndex(Telephony.Sms.Inbox.DATE)

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyColumnIndex)
                    val dateMillis = cursor.getLong(dateColumnIndex)

                    // Check if SMS is a debit transaction
                    if (isDebitTransaction(body)) {
                        val transaction = parseTransaction(body, dateMillis)
                        transaction?.let {
                            transactions.add(it)
                        }
                    }
                }
            }

            return transactions
        }

        // Function to check if app has READ_SMS permission
        private fun hasReadSmsPermission(context: Context): Boolean {
            return context.checkSelfPermission(android.Manifest.permission.READ_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        // Function to check if SMS is a debit transaction based on title
        private fun isDebitTransaction(body: String): Boolean {
            val debitPattern = Regex("^[A-Z]{2}-KVB")
            return debitPattern.find(body) != null
        }

        // Function to parse transaction details from SMS body
        private fun parseTransaction(body: String, dateMillis: Long): Transaction? {
            // Regex pattern to extract amount, date, and time from the SMS body
            val regex = Regex("""Rs\. (\d+\.\d+) on (\d{1,2}/\d{1,2}/\d{4}) (\d{1,2}:\d{2}:\d{2} [AP]M) to .+""")
            val matchResult = regex.find(body)

            // Check if the regex found a match and captured the necessary groups
            if (matchResult != null && matchResult.groupValues.size == 4) {
                val amountStr = matchResult.groupValues[1]
                val dateStr = matchResult.groupValues[2]
                val timeStr = matchResult.groupValues[3]

                // Convert amount string to Double
                val amount = amountStr.toDoubleOrNull()

                // Format date and time into desired formats using the provided dateMillis
                val formattedDate = formatDate(dateStr)
                val formattedTime = formatTime(timeStr)

                // Check if amount was successfully parsed and return Transaction object
                if (amount != null) {
                    return Transaction(amount, formattedDate, formattedTime)
                }
            }

            return null
        }

        // Helper function to format date from string (e.g., "MM/dd/yyyy") to another format
        private fun formatDate(dateStr: String): String {
            val inputDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputDateFormat.parse(dateStr)
            return outputDateFormat.format(date)
        }

        // Helper function to format time from string (12-hour format) to another format
        private fun formatTime(timeStr: String): String {
            val inputTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val outputTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val time = inputTimeFormat.parse(timeStr)
            return outputTimeFormat.format(time)
        }
    }

    // Data class representing a transaction
    data class Transaction(
        val amount: Double,
        val date: String,
        val beneficiary: String
    )
}
