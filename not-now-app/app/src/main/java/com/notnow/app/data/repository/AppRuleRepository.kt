package com.notnow.app.data.repository

import com.notnow.app.data.dao.AppRuleDao
import com.notnow.app.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRuleRepository(private val dao: AppRuleDao) {

    fun getAllRules(): Flow<List<AppRule>> = dao.getAllRules()

    suspend fun getRuleForPackage(packageName: String): AppRule? = dao.getRuleForPackage(packageName)

    suspend fun upsert(rule: AppRule) = dao.upsert(rule)

    suspend fun delete(rule: AppRule) = dao.delete(rule)

    suspend fun toggleRule(packageName: String, enabled: Boolean) {
        dao.getRuleForPackage(packageName)?.let { dao.upsert(it.copy(isEnabled = enabled)) }
    }

    suspend fun seedDefaultRules() {
        val defaults = buildDefaultRules()
        dao.upsertAll(defaults)
    }

    private fun buildDefaultRules(): List<AppRule> = listOf(
        // Level 1 — Minor Distractions (30 sec)
        AppRule("com.google.android.youtube", "YouTube", AppCategory.ENTERTAINMENT, FrictionLevel.LEVEL_1_DISTRACTION),
        AppRule("com.google.android.apps.youtube.music", "YouTube Music", AppCategory.ENTERTAINMENT, FrictionLevel.LEVEL_1_DISTRACTION, blockedDuringFocusMode = false),

        // Level 2 — Attention Traps (10 min)
        AppRule("com.instagram.android", "Instagram", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("com.facebook.katana", "Facebook", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("com.twitter.android", "X (Twitter)", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("com.reddit.frontpage", "Reddit", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("com.snapchat.android", "Snapchat", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("com.zhiliaoapp.musically", "TikTok", AppCategory.SOCIAL_MEDIA, FrictionLevel.LEVEL_2_ATTENTION_TRAP),

        // Level 3 — Spending Triggers (60 min)
        AppRule("in.amazon.mShop.android.shopping", "Amazon", AppCategory.SHOPPING, FrictionLevel.LEVEL_3_SPENDING),
        AppRule("com.myntra.android", "Myntra", AppCategory.SHOPPING, FrictionLevel.LEVEL_3_SPENDING),
        AppRule("com.flipkart.android", "Flipkart", AppCategory.SHOPPING, FrictionLevel.LEVEL_3_SPENDING),
        AppRule("com.ajio.app", "Ajio", AppCategory.SHOPPING, FrictionLevel.LEVEL_3_SPENDING),
        AppRule("com.meesho.supply", "Meesho", AppCategory.SHOPPING, FrictionLevel.LEVEL_3_SPENDING),
        AppRule("com.zomato.android", "Zomato", AppCategory.ENTERTAINMENT, FrictionLevel.LEVEL_2_ATTENTION_TRAP),
        AppRule("in.swiggy.android", "Swiggy", AppCategory.ENTERTAINMENT, FrictionLevel.LEVEL_2_ATTENTION_TRAP),

        // News
        AppRule("com.inshorts.android", "Inshorts", AppCategory.NEWS, FrictionLevel.LEVEL_1_DISTRACTION),
        AppRule("com.google.android.apps.magazines", "Google News", AppCategory.NEWS, FrictionLevel.LEVEL_1_DISTRACTION),
    )
}
