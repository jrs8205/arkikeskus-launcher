package org.arkikeskus.launcher.feature.backup.drive

import android.app.Activity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Scope for the hidden per-app Drive folder (non-sensitive — no Google verification needed). */
private const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

class DriveAuth(private val activity: Activity) {

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DRIVE_APPDATA))).build()

    /** Returns a usable access token, launching the consent UI if authorization is pending. */
    suspend fun authorizeOrNull(): AuthorizationResult = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(activity).authorize(request())
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}
