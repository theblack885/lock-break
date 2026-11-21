package com.example.emergencyunlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.emergencyunlock.action.EmergencyActionHandler
import com.example.emergencyunlock.security.AuditEntry
import com.example.emergencyunlock.security.EmergencyCodeManager
import com.example.emergencyunlock.security.VerifyResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val codeManager = EmergencyCodeManager(this)
        val actionHandler = EmergencyActionHandler(this)

        setContent {
            val darkColors = darkColorScheme(
                primary = Color(0xFF4F8EF7),
                background = Color(0xFF0B0E14),
                surface = Color(0xFF141923),
                onBackground = Color(0xFFF3F4F6),
                onSurface = Color(0xFFF3F4F6)
            )

            MaterialTheme(colorScheme = darkColors) {
                EmergencyControlScreen(
                    codeManager = codeManager,
                    onAuthorizedSuccess = { actionHandler.executeAuthorizedEmergencyAction() }
                )
            }
        }
    }
}

@Composable
fun EmergencyControlScreen(
    codeManager: EmergencyCodeManager,
    onAuthorizedSuccess: () -> String
) {
    var enteredCode by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Default test code: 9112 (ise badal lein)") }
    var logs by remember { mutableStateOf<List<AuditEntry>>(codeManager.getAuditLogs()) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Emergency Code Controller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Verify Emergency Code", fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = { enteredCode = it },
                        label = { Text("Enter Code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            when (val res = codeManager.verifyEmergencyCode(enteredCode)) {
                                is VerifyResult.Unlocked -> {
                                    statusMessage = onAuthorizedSuccess()
                                    enteredCode = ""
                                }
                                is VerifyResult.InvalidCode -> {
                                    statusMessage = "Galat code! Bachi hui koshishein: ${res.remainingAttempts}"
                                }
                                is VerifyResult.LockedOut -> {
                                    statusMessage = "Zyada galat tries! ${res.remainingSeconds}s ke liye lock."
                                }
                            }
                            logs = codeManager.getAuditLogs()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Trigger Emergency Action")
                    }

                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Set / Change Code (Min 4 chars)", fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("New Emergency Code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                codeManager.setEmergencyCode(newCode)
                                statusMessage = "Naya emergency code save ho gaya."
                                newCode = ""
                            } catch (e: IllegalArgumentException) {
                                statusMessage = e.message ?: "Invalid code."
                            }
                            logs = codeManager.getAuditLogs()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save New Code")
                    }
                }
            }

            Text("Audit Log", fontWeight = FontWeight.SemiBold)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${item.timestamp} • ${item.event}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
