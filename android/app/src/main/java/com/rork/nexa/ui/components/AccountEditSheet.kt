package com.rork.nexa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AccountField { DisplayName, Username, Email, Password }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AccountEditSheet(
    field: AccountField,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String, String?, (String?) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var value by remember { mutableStateOf(initialValue) }
    var current by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val (title, subtitle, placeholder, icon, keyboard) = when (field) {
        AccountField.DisplayName -> Pent("Display name", "How others see you in chats.", "Your name", Icons.Outlined.Person, KeyboardType.Text)
        AccountField.Username -> Pent("Username", "Letters, numbers, underscores. Must be unique.", "username", Icons.Outlined.AlternateEmail, KeyboardType.Text)
        AccountField.Email -> Pent("Email", "We'll send a confirmation to your new address.", "you@example.com", Icons.Outlined.Mail, KeyboardType.Email)
        AccountField.Password -> Pent("Password", "Use at least 6 characters.", "New password", Icons.Outlined.Lock, KeyboardType.Password)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 6.dp),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))

            if (field == AccountField.Password) {
                AuthInputField(
                    value = current,
                    onChange = { current = it; error = null },
                    placeholder = "Current password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(10.dp))
                AuthInputField(
                    value = value,
                    onChange = { value = it; error = null },
                    placeholder = "New password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(10.dp))
                AuthInputField(
                    value = confirm,
                    onChange = { confirm = it; error = null },
                    placeholder = "Confirm new password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                )
            } else {
                AuthInputField(
                    value = value,
                    onChange = { value = it; error = null },
                    placeholder = placeholder,
                    leadingIcon = icon,
                    keyboardType = keyboard,
                    imeAction = ImeAction.Done,
                )
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(20.dp))
            AuthPrimaryButton(
                label = "Save",
                loading = loading,
                onClick = {
                    if (field == AccountField.Password) {
                        if (current.isBlank() || value.isBlank() || confirm.isBlank()) {
                            error = "All fields are required."
                            return@AuthPrimaryButton
                        }
                        if (value != confirm) {
                            error = "New passwords don't match."
                            return@AuthPrimaryButton
                        }
                    } else if (value.trim().isBlank()) {
                        error = "Field can't be empty."
                        return@AuthPrimaryButton
                    }
                    loading = true
                    onSave(value.trim(), current.takeIf { field == AccountField.Password }) { err ->
                        loading = false
                        if (err != null) error = err else onDismiss()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class Pent(
    val title: String,
    val subtitle: String,
    val placeholder: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val keyboard: KeyboardType,
)
