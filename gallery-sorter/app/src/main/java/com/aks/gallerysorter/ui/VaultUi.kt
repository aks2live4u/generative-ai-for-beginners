package com.aks.gallerysorter.ui

import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.HideResult

/** Shown wherever a high-security choice is made (setup, hide, move tier, settings). */
const val HIGH_SECURITY_UNINSTALL_WARNING = "High-security items are deleted for good if the app is uninstalled."

/** Confirmation before archiving an earlier install's standard vault ("Start a new vault"). */
const val NEW_VAULT_WARNING =
    "Your previously hidden items stay encrypted in a backup folder and can only be opened with the old PIN."

/** Confirmation before "Forgot passcode? Start a new vault" for the high-security vault. */
const val NEW_HIGH_SECURITY_VAULT_WARNING =
    "The high-security vault is moved aside into a backup folder inside the app, and its items disappear from the " +
        "Vault screen. They stay encrypted and can only be opened with the old passcode, and they are deleted for good " +
        "if the app is uninstalled. A new, empty high-security vault is then set up."

/** One-line, name-free summary of a hide for a snackbar, e.g. "Hid 3 in the standard vault; 1 skipped". */
fun HideResult.summary(tier: VaultTier, verb: String = "Hid"): String = buildString {
    append("$verb $hidden in the ${tier.label.lowercase()} vault")
    if (failed.isNotEmpty()) append("; ${failed.size} failed")
    if (skipped.isNotEmpty()) append("; ${skipped.size} skipped")
    if (missing.isNotEmpty()) append("; ${missing.size} no longer on the phone")
    aborted?.let { append("; stopped: $it") }
}
