package com.example.sos.loginCred

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit


class AuthViewModel : ViewModel() {


    var profile by mutableStateOf(UserData())
        private set

    var profileSaved by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val storage = FirebaseStorage.getInstance()

    var verificationId by mutableStateOf<String?>(null)
    var isNewUser by mutableStateOf(false)
    var phoneNumber by mutableStateOf("")

    private fun updateFirestore(
        uid: String,
        userData: UserData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading = true

        db.collection("users")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                profile = userData
                profileSaved = true
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to update profile")
            }
        }

    fun loadUserProfile(onError: (String) -> Unit = {}) {

        val user = auth.currentUser ?: return

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {
                    profile = UserData(
                        name = doc.getString("name") ?: "User",
                        phone = doc.getString("phone")
                            ?: user.phoneNumber,
                        email = doc.getString("email")
                            ?: user.email,
                        photoUrl = doc.getString("photoUrl")
                    )
                } else {
                    onError("Profile not found")
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to load profile")
            }
    }
    fun sendOtp(
        phone: String,
        activity: Activity,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {

        phoneNumber = phone

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnFailureListener { e ->
                            val message = when (e) {
                                is FirebaseAuthInvalidCredentialsException -> "Invalid OTP"
                                is FirebaseAuthInvalidUserException -> "User disabled"
                                else -> e.message ?: "Verification failed"
                            }
                            onError(message)
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.message ?: "OTP failed")
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    onCodeSent()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        otp: String,
        onNewUser: (String) -> Unit,
        onExistingUser: () -> Unit,
        onError: (String) -> Unit
    ) {

        val id = verificationId ?: return onError("OTP not sent")

        val credential = PhoneAuthProvider.getCredential(
            verificationId ?: return onError("Verification failed"),
            otp
        )

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->

                isNewUser = result.additionalUserInfo?.isNewUser == true

                if (isNewUser) {
                    onNewUser(phoneNumber)
                } else {
                    onExistingUser()
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Invalid OTP")
            }
    }

    /* ---------------- GOOGLE ---------------- */

    fun googleLogin(
        idToken: String,
        name: String,
        email: String,
        onComplete: (Boolean) -> Unit
    ) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid ?: return@addOnSuccessListener
                val isNew = result.additionalUserInfo?.isNewUser == true

                if (isNew) {
                    val user = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to result.user?.phoneNumber,
                        "photoUrl" to result.user?.photoUrl?.toString()
                    )

                    db.collection("users").document(uid).set(user)
                }

                onComplete(true)
            }
            .addOnFailureListener { e ->
                val msg = when (e) {
                    is FirebaseAuthException -> e.message
                    else -> "Google sign-in failed"
                }
                onComplete(false)
            }
    }
    fun logout(onLoggedOut: () -> Unit) {

        FirebaseAuth.getInstance().signOut()

        // Clear local profile state
        profile = UserData()
        profileSaved = false

        onLoggedOut()
    }
    fun clearProfileSavedFlag() {
        profileSaved = false
    }
    fun saveNewUser(
        name: String,
        phone: String,
        email: String?,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError("User not logged in")

        isLoading = true

        if (imageUri != null) {

            val ref = storage.reference.child("profile_images/$uid.jpg")

            ref.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Upload failed")
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->

                    val user = hashMapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to email,
                        "photoUrl" to downloadUrl.toString()
                    )

                    db.collection("users")
                        .document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            loadUserProfile()
                            profileSaved = true
                            isLoading = false
                            onSuccess()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            onError("Failed saving user data")
                        }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    onError(e.message ?: "Image upload failed")
                }

        } else {

            val user = hashMapOf(
                "name" to name,
                "phone" to phone,
                "email" to email
            )

            db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener {
                    loadUserProfile()
                    profileSaved = true
                    isLoading = false
                    onSuccess()
                }
                .addOnFailureListener {
                    isLoading = false
                    onError("Failed saving user data")
                }
        }
    }


    fun saveProfile(
        updated: UserData,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError("User not logged in")

        isLoading = true

        if (imageUri != null) {

            val ref = storage.reference.child("users/$uid/profile.jpg")

            ref.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Upload failed")
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->

                    val newData = updated.copy(
                        photoUrl = downloadUrl.toString()
                    )

                    updateFirestore(uid, newData, onSuccess, onError)
                }
                .addOnFailureListener {
                    isLoading = false
                    onError("Image upload failed")
                }

        } else {
            updateFirestore(uid, updated, onSuccess, onError)
        }
    }


    fun isLoggedIn(): Boolean = auth.currentUser != null
}