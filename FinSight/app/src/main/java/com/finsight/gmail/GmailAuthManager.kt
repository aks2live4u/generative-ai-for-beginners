package com.finsight.gmail

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles Google Sign-In for Gmail's READ-ONLY scope only. The app never requests
 * gmail.modify/gmail.send - see [gmailReadonlyScope] below. Uses native Android Google Sign-In,
 * which Google validates via this app's package name + signing certificate fingerprint rather
 * than a client ID embedded in code - you must register an Android OAuth client (matching
 * package name and SHA-1) and add yourself as an OAuth consent screen test user in Google Cloud
 * Console before sign-in will succeed. See README.md "Gmail setup" for the exact steps.
 */
class GmailAuthManager(private val context: Context) {

    private val gmailReadonlyScope = Scope("https://www.googleapis.com/auth/gmail.readonly")

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(gmailReadonlyScope)
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    fun signOut() {
        signInClient.signOut()
    }

    /**
     * Blocking OAuth token fetch (must be called off the main thread). The returned token is a
     * short-lived bearer token used only to call the read-only Gmail REST endpoints in
     * [GmailScanner] - it is never persisted to disk.
     */
    suspend fun getAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val androidAccount = Account(account.email, "com.google")
        GoogleAuthUtil.getToken(context, androidAccount, "oauth2:${gmailReadonlyScope.scopeUri}")
    }
}
