package com.example.sos.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.example.sos.contactCred.CryptoManager.decrypt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun openSmsMessenger(
    phone: String,
    latitude: Double,
    longitude: Double,
    link: String
) {

    val message = """
🚨 SOS ALERT

I may be in danger.

Live tracking:
$link

Google Maps:
https://maps.google.com/?q=$latitude,$longitude
    """.trimIndent()

    try {

        val smsManager = SmsManager.getDefault()

        smsManager.sendTextMessage(
            phone,
            null,
            message,
            null,
            null
        )

        Log.d("SOS message", "SMS sent to $phone")

    } catch (e: Exception) {

        Log.e("SOS message", "SMS failed: ${e.message}")

    }
}

fun getTrustedContacts(onResult: (List<String>) -> Unit) {

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        onResult(emptyList())
        return
    }

    FirebaseFirestore.getInstance()
        
        .collection("users")
        .document(userId)
        .collection("trusted_contacts")
        .get()
        .addOnSuccessListener { result ->

            val phones = result.documents.mapNotNull { doc ->
                val encrypted = doc.getString("phone")

                encrypted?.let {
                    decrypt(it)
                }
            }

            onResult(phones)
        }
}