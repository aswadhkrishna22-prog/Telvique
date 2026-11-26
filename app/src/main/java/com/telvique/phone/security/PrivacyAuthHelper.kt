package com.telvique.phone.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Configures OS-level app switcher preview privacy for Activities.
 */
fun Activity.applyRecentsPrivacy(hideInRecents: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setRecentsScreenshotEnabled(!hideInRecents)
    } else {
        if (hideInRecents) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

object BiometricAuthHelper {

    /**
     * API 30+ allows combining BIOMETRIC_STRONG and DEVICE_CREDENTIAL.
     * API 26-29 requires BIOMETRIC_WEAK or DEVICE_CREDENTIAL (STRONG + CREDENTIAL throws IllegalArgumentException).
     */
    fun preferredAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        if (manager.canAuthenticate(preferredAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS) {
            return true
        }
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return keyguard?.isDeviceSecure == true &&
            manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onCancelOrError: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onCancelOrError()
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val primaryAuthenticators = preferredAuthenticators()
        val launched = runCatching {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(primaryAuthenticators)
                .build()
            prompt.authenticate(info)
        }.isSuccess

        if (!launched) {
            val fallbackLaunched = runCatching {
                val fallbackInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(fallbackInfo)
            }.isSuccess
            if (!fallbackLaunched) {
                onCancelOrError()
            }
        }
    }
}
