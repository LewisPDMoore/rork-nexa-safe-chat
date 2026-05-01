package com.rork.nexa.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.data.auth.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val email: String = "",
    val identifier: String = "",
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository.get(app)

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    val status: StateFlow<SessionStatus> = repo.status

    fun setEmail(value: String) = _form.update { it.copy(email = value, error = null) }
    fun setIdentifier(value: String) = _form.update { it.copy(identifier = value, error = null) }
    fun setUsername(value: String) = _form.update {
        it.copy(
            username = value.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20),
            error = null,
        )
    }
    fun setDisplayName(value: String) = _form.update { it.copy(displayName = value.take(40), error = null) }
    fun setPassword(value: String) = _form.update { it.copy(password = value, error = null) }
    fun toggleShowPassword() = _form.update { it.copy(showPassword = !it.showPassword) }
    fun setRememberMe(value: Boolean) = _form.update { it.copy(rememberMe = value) }
    fun clearError() = _form.update { it.copy(error = null) }
    fun resetForm() = _form.value.let {
        _form.value = AuthFormState(rememberMe = it.rememberMe)
    }

    fun signUp(onSuccess: () -> Unit) {
        val s = _form.value
        val username = s.username.trim()
        val displayName = s.displayName.trim()
        val email = s.email.trim()
        val password = s.password

        if (displayName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _form.update { it.copy(error = "All fields are required.") }
            return
        }
        if (!isValidEmail(email)) {
            _form.update { it.copy(error = "That email doesn't look right.") }
            return
        }
        if (username.length < 3) {
            _form.update { it.copy(error = "Username must be at least 3 characters.") }
            return
        }
        if (password.length < 6) {
            _form.update { it.copy(error = "Password must be at least 6 characters.") }
            return
        }

        _form.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repo.signUp(
                email = email,
                username = username,
                displayName = displayName,
                password = password,
            )
            result
                .onSuccess {
                    _form.update { AuthFormState(rememberMe = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    _form.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong.") }
                }
            if (result.isSuccess) _form.update { it.copy(isLoading = false) }
        }
    }

    fun signIn(onSuccess: () -> Unit) {
        val s = _form.value
        val identifier = s.identifier.trim().ifBlank { s.email.trim() }
        val password = s.password
        if (identifier.isBlank() || password.isBlank()) {
            _form.update { it.copy(error = "All fields are required.") }
            return
        }

        _form.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repo.signIn(identifier = identifier, password = password, rememberMe = s.rememberMe)
            result
                .onSuccess {
                    _form.update { AuthFormState(rememberMe = s.rememberMe) }
                    onSuccess()
                }
                .onFailure { e ->
                    _form.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong.") }
                }
            if (result.isSuccess) _form.update { it.copy(isLoading = false) }
        }
    }

    fun acknowledgeBan() {
        repo.acknowledgeBan()
    }

    fun createChildAccount(
        username: String,
        password: String,
        onDone: (String?) -> Unit,
    ) {
        val u = username.trim().lowercase()
        if (u.length < 3) { onDone("Username must be at least 3 characters."); return }
        if (password.length < 6) { onDone("Password must be at least 6 characters."); return }
        viewModelScope.launch {
            val r = repo.createChildAccount(u, password)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.signOut()
            onDone()
        }
    }

    fun saveAvatar(emoji: String, gradient: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveAvatar(emoji, gradient)
            onDone()
        }
    }

    fun updateDisplayName(name: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.updateDisplayName(name)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun updateUsername(username: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.updateUsername(username)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun updateEmail(email: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.updateEmail(email)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun updatePassword(current: String, new: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.updatePassword(current, new)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun uploadProfilePhoto(bytes: ByteArray, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.uploadProfilePhoto(bytes)
            onDone(r.exceptionOrNull()?.message)
        }
    }

    fun setMainPhoto(url: String) {
        viewModelScope.launch { repo.setMainPhoto(url) }
    }

    fun deletePhoto(url: String) {
        viewModelScope.launch { repo.deletePhoto(url) }
    }

    private fun isValidEmail(value: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(value)
    }
}
