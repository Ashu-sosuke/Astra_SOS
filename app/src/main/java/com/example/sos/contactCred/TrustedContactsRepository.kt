package com.example.sos.contactCred


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TrustedContactsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun uid() = auth.currentUser?.uid

    fun listenContacts(onChange: (List<TrustedContact>) -> Unit) {
        val userId = uid() ?: return

        Log.d("TrustedContacts", "listenContacts uid = $userId")


        db.collection("users")
            .document(userId)
            .collection("trusted_contacts")
            .addSnapshotListener { snapshot, _ ->
                val contacts =
                    snapshot?.toObjects(TrustedContact::class.java)
                        ?: emptyList()
                onChange(contacts)
            }
    }

    fun addContact(contact: TrustedContact) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("trusted_contacts")
            .document(contact.id)
            .set(contact)
    }

    fun updateContact(contact: TrustedContact) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("trusted_contacts")
            .document(contact.id)
            .set(contact)
    }

    fun deleteContact(contactId: String) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("trusted_contacts")
            .document(contactId)
            .delete()
    }
}
