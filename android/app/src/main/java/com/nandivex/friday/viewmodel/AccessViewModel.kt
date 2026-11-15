package com.nandivex.friday.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nandivex.friday.data.AccessGate
import com.nandivex.friday.data.LicenseStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the user stands before the assistant opens. Until accounts and the free trial arrive (Phase 4) nothing is locked. */
sealed interface AccessState {
    data object Checking : AccessState
    data class NeedsKey(val busy: Boolean = false, val error: String? = null) : AccessState
    data class Active(val key: String, val status: LicenseStatus) : AccessState
}

/** The licence server on AWS is gone, so every build opens straight to the assistant. */
class AccessViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<AccessState>(
        AccessState.Active(OPEN_KEY, LicenseStatus(active = true, plan = "open", expiresAt = 0, reason = ""))
    )
    val state: StateFlow<AccessState> = _state.asStateFlow()

    init {
        AccessGate.unlocked = true
    }

    /** Nothing to re-check any more; kept so callers need no change. */
    fun refresh(quiet: Boolean = false) = Unit

    /** There is no activation key any more. */
    fun activate(input: String) = Unit

    fun removeKey() = Unit

    private companion object {
        const val OPEN_KEY = "FRIDAY-OPEN"
    }
}
