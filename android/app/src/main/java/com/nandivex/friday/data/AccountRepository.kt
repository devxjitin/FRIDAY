package com.nandivex.friday.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** What is stored in `users/{uid}` (see `firestore.rules`). */
data class AccountProfile(
    val name: String,
    val yearOfBirth: Int,
    val email: String,
    /** Milliseconds since 1970 when the profile was first saved. */
    val memberSinceMillis: Long?,
    val language: String? = null
)

/** Why an account action did not work, in words the user can act on. */
sealed interface AccountResult<out T> {
    data class Success<T>(val value: T) : AccountResult<T>
    /** The user closed the Google account sheet. Not an error to show. */
    data object Cancelled : AccountResult<Nothing>
    data class Failure(val message: String) : AccountResult<Nothing>
}

/**
 * The signed-in user and their profile: Google sign-in through Credential Manager, Firebase Authentication, and the
 * user's own document in Firestore. Nothing here runs unless the user taps Sign in.
 */
class AccountRepository(private val context: Context) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = runCatching { auth.currentUser }.getOrNull()

    /** The web client id the Firebase plugin generates from google-services.json; 0 when that file is not in the build. */
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    val isAvailable: Boolean get() = webClientId() != null

    /** Shows the Google account sheet and signs in to Firebase with the chosen account. */
    suspend fun signInWithGoogle(activity: Activity): AccountResult<Unit> {
        val clientId = webClientId() ?: return AccountResult.Failure("Sign-in is not available in this build.")
        return try {
            val credential = requestGoogleCredential(activity, clientId)
            auth.signInWithCredential(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
            AccountResult.Success(Unit)
        } catch (e: GetCredentialCancellationException) {
            AccountResult.Cancelled
        } catch (e: GetCredentialException) {
            AccountResult.Failure("Google sign-in did not complete. Check that this phone has a Google account and an internet connection, then try again.")
        } catch (e: Exception) {
            AccountResult.Failure("Sign-in failed. Check your internet connection and try again.")
        }
    }

    private suspend fun requestGoogleCredential(activity: Activity, clientId: String): GoogleIdTokenCredential {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val result = CredentialManager.create(activity)
            .getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data)
        }
        throw IllegalStateException("Unexpected credential type")
    }

    /** The profile of the signed-in user, or null when none is saved yet. */
    suspend fun loadProfile(): AccountResult<AccountProfile?> {
        val user = currentUser ?: return AccountResult.Failure("You are not signed in.")
        return try {
            val snap = db.collection("users").document(user.uid).get().await()
            if (!snap.exists()) {
                AccountResult.Success(null)
            } else {
                AccountResult.Success(
                    AccountProfile(
                        name = snap.getString("name").orEmpty(),
                        yearOfBirth = (snap.getLong("yearOfBirth") ?: 0L).toInt(),
                        email = snap.getString("email") ?: user.email.orEmpty(),
                        memberSinceMillis = snap.getTimestamp("createdAt")?.toDate()?.time,
                        language = snap.getString("language")
                    )
                )
            }
        } catch (e: Exception) {
            AccountResult.Failure("Could not load your profile. Check your internet connection and try again.")
        }
    }

    /** Creates the profile, or updates the name and year of birth of the one that exists. */
    suspend fun saveProfile(name: String, yearOfBirth: Int, existing: AccountProfile?): AccountResult<Unit> {
        val user = currentUser ?: return AccountResult.Failure("You are not signed in.")
        val email = user.email ?: return AccountResult.Failure("Your Google account has no email address.")
        val data = hashMapOf<String, Any>(
            "name" to name.trim(), "yearOfBirth" to yearOfBirth, "email" to email, "emailVerified" to true
        )
        existing?.language?.let { data["language"] = it }
        if (existing?.memberSinceMillis != null) {
            data["createdAt"] = Timestamp(existing.memberSinceMillis / 1000, ((existing.memberSinceMillis % 1000) * 1_000_000).toInt())
            data["updatedAt"] = FieldValue.serverTimestamp()
        } else {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        return try {
            db.collection("users").document(user.uid).set(data).await()
            AccountResult.Success(Unit)
        } catch (e: Exception) {
            AccountResult.Failure("Could not save your profile. Check your internet connection and try again.")
        }
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    /**
     * Deletes the profile document and then the sign-in. Firebase only deletes an account that signed in recently, so
     * the user is asked to pick their Google account again first. The memories and conversations on this phone are
     * not touched; they are removed from Storage.
     */
    suspend fun deleteAccount(activity: Activity): AccountResult<Unit> {
        val user = currentUser ?: return AccountResult.Failure("You are not signed in.")
        val clientId = webClientId() ?: return AccountResult.Failure("Sign-in is not available in this build.")
        return try {
            val credential = requestGoogleCredential(activity, clientId)
            user.reauthenticate(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
            db.collection("users").document(user.uid).delete().await()
            user.delete().await()
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
            AccountResult.Success(Unit)
        } catch (e: GetCredentialCancellationException) {
            AccountResult.Cancelled
        } catch (e: Exception) {
            AccountResult.Failure("Could not delete the account. Check your internet connection, pick the same Google account when asked, and try again.")
        }
    }
}
