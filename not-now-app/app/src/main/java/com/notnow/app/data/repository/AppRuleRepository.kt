package com.notnow.app.data.repository

import com.notnow.app.data.dao.AppRuleDao
import com.notnow.app.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRuleRepository(private val dao: AppRuleDao) {

    val allRules: Flow<List<AppRule>> = dao.observeAll()

    suspend fun getRuleForPackage(packageName: String): AppRule? =
        dao.findByPackage(packageName)

    suspend fun upsert(rule: AppRule) = dao.upsert(rule)

    suspend fun delete(rule: AppRule) = dao.delete(rule)

    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        dao.setEnabled(packageName, enabled)

    suspend fun resetToDefaults() {
        seedDefaults()
        dao.enableAll()
    }

    suspend fun seedDefaults() {
        val defaults = listOf(
            AppRule("com.google.android.youtube",     "YouTube",    AppCategory.ENTERTAINMENT, FrictionLevel.LEVEL_1_MINOR,    blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.instagram.android",          "Instagram",  AppCategory.SOCIAL,        FrictionLevel.LEVEL_2_ATTENTION, blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.facebook.katana",            "Facebook",   AppCategory.SOCIAL,        FrictionLevel.LEVEL_2_ATTENTION, blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.twitter.android",            "X / Twitter",AppCategory.SOCIAL,        FrictionLevel.LEVEL_2_ATTENTION, blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.reddit.frontpage",           "Reddit",     AppCategory.SOCIAL,        FrictionLevel.LEVEL_2_ATTENTION, blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("in.amazon.mShop.android.shopping","Amazon",   AppCategory.SHOPPING,      FrictionLevel.LEVEL_3_SPENDING,  blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.myntra.android",             "Myntra",     AppCategory.SHOPPING,      FrictionLevel.LEVEL_3_SPENDING,  blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.flipkart.android",           "Flipkart",   AppCategory.SHOPPING,      FrictionLevel.LEVEL_3_SPENDING,  blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.ajio.ajio",                  "Ajio",       AppCategory.SHOPPING,      FrictionLevel.LEVEL_3_SPENDING,  blockedInFocusMode = true,  blockedAtNight = true),
            AppRule("com.snapchat.android",           "Snapchat",   AppCategory.SOCIAL,        FrictionLevel.LEVEL_2_ATTENTION, blockedInFocusMode = true,  blockedAtNight = true),
        )
        dao.upsertAll(defaults)
    }
}
