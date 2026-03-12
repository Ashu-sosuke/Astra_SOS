package com.example.sos.contactCred

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class TrustedContactsViewModel : ViewModel() {

    private val repo = TrustedContactsRepository()

    private val _contacts = MutableStateFlow<List<TrustedContact>>(emptyList())
    val contacts: StateFlow<List<TrustedContact>> = _contacts

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.let {
                repo.listenContacts { list ->
                    _contacts.value = list
                }
            }
        }
    }

    fun addContact(name: String, phonePlain: String, relation: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("TrustedContacts", "User is null")
            return
        }

        val normalized = PhoneUtils.normalize(phonePlain)
        val phoneHash = CryptoManager.hash(normalized)

        Log.d("TrustedContacts", "Normalized: $normalized")
        Log.d("TrustedContacts", "Hash: $phoneHash")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("trusted_contacts")
            .whereEqualTo("phoneHash", phoneHash)
            .get()
            .addOnSuccessListener { snapshot ->

                Log.d("TrustedContacts", "Duplicate check size: ${snapshot.size()}")

                if (!snapshot.isEmpty) {
                    Log.d("TrustedContacts", "Duplicate detected")
                    return@addOnSuccessListener
                }

                val contact = TrustedContact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phone = CryptoManager.encrypt(normalized),
                    phoneHash = phoneHash,
                    relation = relation
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("trusted_contacts")
                    .document(contact.id)
                    .set(contact)
                    .addOnSuccessListener {
                        Log.d("TrustedContacts", "Contact successfully written!")
                    }
                    .addOnFailureListener {
                        Log.e("TrustedContacts", "Write failed: ${it.message}")
                    }
            }
            .addOnFailureListener {
                Log.e("TrustedContacts", "Duplicate check failed: ${it.message}")
            }
    }


    fun updateContact(contact: TrustedContact) {
        repo.updateContact(contact)
    }

    fun deleteContact(contact: TrustedContact) {
        repo.deleteContact(contact.id)
    }

    fun existsByPhone(plainPhone: String): Boolean {
        return _contacts.value.any {
            CryptoManager.decrypt(it.phone) == plainPhone
        }
    }
}
