package com.example.expensetracker

object UpiParser {

    // ✅ Detect transaction type (UPI / CARD / BANK)
    fun detectSource(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("upi") -> "UPI"

            // 🔥 Card detection
            lower.contains("spent using") ||
                    lower.contains("using card") ||
                    (lower.contains("card") && lower.contains("xx")) -> "CREDIT_CARD"

            // Bank debit fallback
            lower.contains("debited") -> "BANK"

            else -> "UNKNOWN"
        }
    }

    // ✅ Only debit check (ignore credit)
    fun isDebit(message: String): Boolean {
        val lower = message.lowercase()

        return lower.contains("debit") ||
                lower.contains("debited") ||
                lower.contains("spent") ||
                lower.contains("paid") ||
                lower.contains("sent")
    }

    // ✅ Extract Amount (handles commas)
    fun extractAmount(message: String): String? {
        val regex = Regex("(rs\\.?|inr)\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)

        return regex.find(message)
            ?.groups?.get(2)?.value
            ?.replace(",", "")
    }

    // ✅ Extract Payee (handles multiple formats)
    fun extractPayee(message: String): String {

        // 🔥 1. CARD (fix: last "on" use karo)
        val cardRegex = Regex("on \\d{2}-[A-Za-z]{3}-\\d{2} on ([A-Za-z0-9 .&]+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
        val cardMatch = cardRegex.find(message)
        if (cardMatch != null) {
            return cardMatch.groups[1]?.value?.trim() ?: "Unknown"
        }

        // 🔥 2. UPI "credited to NAME via"
        val creditedRegex = Regex("credited to ([A-Za-z ]+?) via", RegexOption.IGNORE_CASE)
        val creditedMatch = creditedRegex.find(message)
        if (creditedMatch != null) {
            return creditedMatch.groups[1]?.value?.trim() ?: "Unknown"
        }

        // 🔥 3. paid/sent
        val simplePatterns = listOf(
            Regex("paid to ([A-Za-z0-9 .&]+)", RegexOption.IGNORE_CASE),
            Regex("sent to ([A-Za-z0-9 .&]+)", RegexOption.IGNORE_CASE),
            Regex("to ([A-Za-z0-9 .&]+) via", RegexOption.IGNORE_CASE)
        )

        for (pattern in simplePatterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groups[1]?.value?.trim() ?: "Unknown"
            }
        }

        // 🔥 4. UPI structured fallback
        val upiRegex = Regex("UPI/.*/([A-Za-z0-9 .&]+)", RegexOption.IGNORE_CASE)
        val upiMatch = upiRegex.find(message)
        if (upiMatch != null) {
            return upiMatch.groups[1]?.value?.trim() ?: "Unknown"
        }

        return "Unknown"
    }
}