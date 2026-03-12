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
    context: Context,
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

    val intent = Intent(Intent.ACTION_SENDTO).apply {

        data = Uri.parse("smsto:$phone")

        putExtra("sms_body", message)
    }

    context.startActivity(intent)
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