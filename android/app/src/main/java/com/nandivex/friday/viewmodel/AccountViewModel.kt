package com.nandivex.friday.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nandivex.friday.data.AccountProfile
import com.nandivex.friday.data.AccountRepository
import com.nandivex.friday.data.AccountResult
import com.nandivex.friday.data.AgeCheck
import com.nandivex.friday.data.AgeRules
import com.nandivex.friday.data.SetupLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year

data class AccountUiState(
    /** False in a build without google-services.json, where sign-in cannot work. */
    val available: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    /** True while the profile is being read for the first time after sign-in. */
    val loadingProfile: Boolean = false,
    val profile: AccountProfile? = null,
    /** The form for the name and year of birth is open (first save, or an edit). */
    val editing: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
) {
    /** Signed in, but no profile yet: the form is shown instead of the profile. */
    val needsProfile: Boolean get() = signedIn && !loadingProfile && profile == null
}

/** The account on the Profile page: sign in, save the profile, sign out, delete the account. */
class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AccountRepository(application)
    private val _state = MutableStateFlow(AccountUiState(available = repo.isAvailable))
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Reads who is signed in and loads their profile. */
    fun refresh() {
        val user = repo.currentUser
        if (user == null) {
            _state.update { it.copy(signedIn = false, email = null, profile = null, loadingProfile = false, editing = false) }
            return
        }
        _state.update { it.copy(signedIn = true, email = user.email, loadingProfile = true, error = null) }
        viewModelScope.launch {
            when (val result = repo.loadProfile()) {
                is AccountResult.Success -> _state.update { it.copy(profile = result.value, loadingProfile = false) }
                is AccountResult.Failure -> _state.update { it.copy(loadingProfile = false, error = result.message) }
                AccountResult.Cancelled -> _state.update { it.copy(loadingProfile = false) }
            }
        }
    }

    fun signIn(activity: Activity) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repo.signInWithGoogle(activity)) {
                is AccountResult.Success -> {
                    _state.update { it.copy(busy = false) }
                    refresh()
                }
                is AccountResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
                AccountResult.Cancelled -> _state.update { it.copy(busy = false) }
            }
        }
    }

    fun startEditing() = _state.update { it.copy(editing = true, error = null) }

    fun cancelEditing() = _state.update { it.copy(editing = false, error = null) }

    /** Checks the name and year of birth, then saves them. The error is shown on the form when a check fails. */
    fun saveProfile(name: String, yearText: String) {
        val state = _state.value
        if (state.busy) return
        if (!SetupLogic.isNameValid(name)) {
            _state.update { it.copy(error = "Enter your name.") }
            return
        }
        val year = AgeRules.parseYear(yearText)
        val check = AgeRules.check(year, Year.now().value)
        AgeRules.message(check)?.let { message ->
            _state.update { it.copy(error = message) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repo.saveProfile(name, year!!, state.profile)) {
                is AccountResult.Success -> {
                    _state.update { it.copy(busy = false, editing = false) }
                    refresh()
                }
                is AccountResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
                AccountResult.Cancelled -> _state.update { it.copy(busy = false) }
            }
        }
    }

    fun signOut() {
        if (_state.value.busy) return
        viewModelScope.launch {
            repo.signOut()
            refresh()
        }
    }

    fun deleteAccount(activity: Activity) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repo.deleteAccount(activity)) {
                is AccountResult.Success -> {
                    _state.update { it.copy(busy = false) }
                    refresh()
                }
                is AccountResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
                AccountResult.Cancelled -> _state.update { it.copy(busy = false) }
            }
        }
    }

    /** The age band of the saved profile, for display. Null when there is no profile. */
    fun bandLabel(): String? {
        val profile = _state.value.profile ?: return null
        return (AgeRules.check(profile.yearOfBirth, Year.now().value) as? AgeCheck.Adult)?.band?.label
    }
}
