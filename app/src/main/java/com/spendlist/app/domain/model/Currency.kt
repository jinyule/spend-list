package com.spendlist.app.domain.model

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    CNY("CNY", "¥", "Chinese Yuan"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen"),
    HKD("HKD", "HK$", "Hong Kong Dollar"),
    TWD("TWD", "NT$", "Taiwan Dollar"),
    KRW("KRW", "₩", "Korean Won"),
    SGD("SGD", "S$", "Singapore Dollar"),
    CAD("CAD", "C$", "Canadian Dollar"),
    AUD("AUD", "A$", "Australian Dollar");

    companion object {
        fun fromCode(code: String): Currency? {
            return entries.find { it.code == code }
        }
    }
}
