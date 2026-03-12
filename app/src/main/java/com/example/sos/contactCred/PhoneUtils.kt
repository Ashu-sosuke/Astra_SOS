package com.example.sos.contactCred

object PhoneUtils {

    fun normalize(phone: String, defaultCountryCode: String = "+91"): String {
        var cleaned = phone.replace("\\s".toRegex(), "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("0")) {
                cleaned = cleaned.drop(1)
            }
            cleaned = defaultCountryCode + cleaned
        }

        return cleaned
    }
}
