package com.nexus.launcher.ui

import android.Manifest
import android.content.Intent
import android.provider.AlarmClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.ai.NexusAction
import com.nexus.launcher.system.Contact
import com.nexus.launcher.system.PhoneActions
import com.nexus.launcher.ui.screens.CommandSession
import com.nexus.launcher.ui.screens.Countdown
import com.nexus.launcher.ui.screens.Exchange
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Turns NEXUS's action tags into things that happen on the phone. Calls and texts go through
 * a 3-second countdown the user can cancel; everything else runs immediately. Asks for any
 * missing permission first, then carries on with the action once it's granted.
 */
@Composable
fun rememberActionRunner(
    graph: NexusGraph,
    session: CommandSession,
    onScreen: (Screen) -> Unit,
): (NexusAction) -> Unit {
    val context = LocalContext.current
    val phone = remember { PhoneActions(context.applicationContext) }
    val navigate by rememberUpdatedState(onScreen)
    val pending = remember { arrayOfNulls<() -> Unit>(1) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val next = pending[0]
        pending[0] = null
        if (result.values.all { it }) next?.invoke()
        else session.log += Exchange(false, "I need that permission to do it. You can allow it any time in Android settings.", isError = true)
    }

    fun say(text: String, error: Boolean = false) {
        session.log += Exchange(false, text, isError = error)
    }

    fun withPermissions(vararg perms: String, block: () -> Unit) {
        val missing = perms.filterNot(phone::has)
        if (missing.isEmpty()) block() else {
            pending[0] = block
            permissions.launch(missing.toTypedArray())
        }
    }

    fun withContact(who: String, extra: Array<String>, block: (Contact) -> Unit) {
        val isNumber = who.count(Char::isDigit) >= 3
        val perms = if (isNumber) extra else extra + Manifest.permission.READ_CONTACTS
        withPermissions(*perms) {
            val contact = phone.findContact(who)
            if (contact == null) say("I couldn't find \"$who\" in your contacts.", error = true) else block(contact)
        }
    }

    fun fail(what: String) = say("No app on this phone can $what.", error = true)

    return remember(graph, session) {
        { action: NexusAction ->
            when (action) {
                is NexusAction.Call -> withContact(action.who, arrayOf(Manifest.permission.CALL_PHONE)) { c ->
                    session.startCountdown(graph.scope, Countdown("Calling", c.name, c.number) {
                        if (phone.call(c.number)) null else "Couldn't start the call."
                    })
                }
                is NexusAction.Text -> withContact(action.who, arrayOf(Manifest.permission.SEND_SMS)) { c ->
                    session.startCountdown(graph.scope, Countdown("Texting", c.name, "“${action.message}”") {
                        if (phone.sendSms(c.number, action.message)) "Sent to ${c.name}." else "The text to ${c.name} didn't send."
                    })
                }
                is NexusAction.WhatsApp -> withContact(action.who, emptyArray()) { c ->
                    if (!phone.whatsApp(c.number, action.message)) fail("open WhatsApp")
                }
                is NexusAction.Email -> if (!phone.email(action.to, action.subject, action.body)) fail("send email")
                is NexusAction.Directions -> if (!phone.directions(action.place)) fail("give directions")
                is NexusAction.Play -> if (!phone.playMusic(action.query)) fail("play music by search")
                is NexusAction.Camera -> if (!phone.camera(action.video)) fail("open the camera")
                is NexusAction.Event -> {
                    val start = runCatching {
                        LocalDateTime.parse(action.start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
                    if (!phone.calendarEvent(action.title, start, action.minutes)) fail("add calendar events")
                }
                is NexusAction.Torch -> if (!phone.torch(action.on)) say("This phone has no flashlight I can control.", error = true)
                is NexusAction.Volume -> phone.volume(action.percent)
                is NexusAction.Panel -> phone.settingsPanel(action.which)
                is NexusAction.OpenApp -> {
                    val app = graph.apps.find(action.name)
                    when {
                        app == null -> say("${action.name} isn't installed.", error = true)
                        !graph.focus.allows(app.packageName) -> say("${app.label} is held by Focus Mode.", error = true)
                        else -> graph.apps.launch(app)
                    }
                }
                is NexusAction.StartFocus -> { graph.focus.start(action.minutes); navigate(Screen.FOCUS) }
                NexusAction.StopFocus -> graph.focus.stop()
                is NexusAction.Navigate -> navigate(
                    when (action.screen) {
                        "apps", "orbit" -> Screen.ORBIT
                        "alerts", "notifications" -> Screen.ALERTS
                        "focus" -> Screen.FOCUS
                        "settings" -> Screen.SETTINGS
                        "command" -> Screen.COMMAND
                        else -> Screen.HOME
                    },
                )
                is NexusAction.SetAlarm -> start(
                    context,
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                        .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
                ) || run { fail("set alarms"); true }
                is NexusAction.SetTimer -> start(
                    context,
                    Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
                ) || run { fail("set timers"); true }
                is NexusAction.WebSearch -> start(
                    context,
                    Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, action.query),
                ) || run { fail("search the web"); true }
            }
            Unit
        }
    }
}

private fun start(context: android.content.Context, intent: Intent): Boolean =
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
