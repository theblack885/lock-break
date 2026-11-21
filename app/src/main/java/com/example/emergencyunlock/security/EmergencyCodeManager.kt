package com.example.emergencyunlock.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AuditEntry(
    val timestamp: String,
    val event: String,
    val details: String
)

sealed class VerifyResult {
    data object Unlocked : VerifyResult()
    data class InvalidCode(val remainingAttempts: Int) : VerifyResult()
    data class LockedOut(val remainingSeconds: Long) : VerifyResult()
}

class EmergencyCodeManager(context: Context) {

    private val prefs = context.getSharedPreferences("emergency_sec_prefs", Context.MODE_PRIVATE)
    private val maxAttempts = 3
    private val lockoutDurationMs = 30_000L

    init {
        if (!hasCodeConfigured()) {
            setEmergencyCode("9112")
        }
    }

    fun hasCodeConfigured(): Boolean {
        return prefs.getString(KEY_HASH, null) != null && prefs.getString(KEY_SALT, null) != null
    }

    fun setEmergencyCode(newCode: String) {
        val trimmed = newCode.trim()
        require(trimmed.length >= 4) { "Code kam se kam 4 characters ka hona chahiye." }

        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = saltBytes.toHex()
        val hashHex = hashWithSalt(trimmed, saltHex)

        prefs.edit()
            .putString(KEY_SALT, saltHex)
            .putString(KEY_HASH, hashHex)
            .putInt(KEY_FAILED_COUNT, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()

        appendAuditLog("CODE_UPDATED", "Emergency code securely updated.")
    }

    fun verifyEmergencyCode(inputCode: String): VerifyResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)

        if (now < lockoutUntil) {
            val secondsLeft = ((lockoutUntil - now) / 1000L).coerceAtLeast(1L)
            appendAuditLog("BLOCKED_COOLDOWN", "Attempt blocked during cooldown ($secondsLeft s left).")
            return VerifyResult.LockedOut(secondsLeft)
        }

        val storedSalt = prefs.getString(KEY_SALT, null) ?: return VerifyResult.InvalidCode(maxAttempts)
        val storedHash = prefs.getString(KEY_HASH, null) ?: return VerifyResult.InvalidCode(maxAttempts)

        val candidateHash = hashWithSalt(inputCode.trim(), storedSalt)

        return if (MessageDigest.isEqual(storedHash.toByteArray(), candidateHash.toByteArray())) {
            prefs.edit()
                .putInt(KEY_FAILED_COUNT, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()

            appendAuditLog("EMERGENCY_AUTHORIZED", "Valid emergency code verified.")
            VerifyResult.Unlocked
        } else {
            val nextFailed = prefs.getInt(KEY_FAILED_COUNT, 0) + 1
            if (nextFailed >= maxAttempts) {
                val until = now + lockoutDurationMs
                prefs.edit()
                    .putInt(KEY_FAILED_COUNT, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, until)
                    .apply()

                appendAuditLog("LOCKOUT_TRIGGERED", "Too many failed attempts. 30s lockout active.")
                VerifyResult.LockedOut(lockoutDurationMs / 1000L)
            } else {
                prefs.edit().putInt(KEY_FAILED_COUNT, nextFailed).apply()
                val left = maxAttempts - nextFailed
                appendAuditLog("INVALID_CODE", "Wrong code entered. Attempts left: $left")
                VerifyResult.InvalidCode(left)
            }
        }
    }

    fun getAuditLogs(): List<AuditEntry> {
        val raw = prefs.getString(KEY_AUDIT_LOG, "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(" | ", limit = 3)
                if (parts.size == 3) {
                    AuditEntry(parts[0], parts[1], parts[2])
                } else null
            }
            .reversed()
    }

    private fun appendAuditLog(event: String, details: String) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val existing = prefs.getString(KEY_AUDIT_LOG, "") ?: ""
        val newLine = "$timeStr | $event | $details"
        val updated = if (existing.isBlank()) newLine else "$existing\n$newLine"
        val trimmedLines = updated.split("\n").takeLast(50).joinToString("\n")
        prefs.edit().putString(KEY_AUDIT_LOG, trimmedLines).apply()
    }

    private fun hashWithSalt(code: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltHex.toByteArray(Charsets.UTF_8))
        val digest = md.digest(code.toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_SALT = "code_salt"
        private const val KEY_HASH = "code_hash"
        private const val KEY_FAILED_COUNT = "failed_count"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_AUDIT_LOG = "audit_log"
    }
}
