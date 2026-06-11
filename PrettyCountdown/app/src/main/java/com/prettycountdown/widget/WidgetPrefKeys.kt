package com.prettycountdown.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.action.ActionParameters

/** Per-widget-instance configuration, stored via [androidx.glance.state.PreferencesGlanceStateDefinition]. */
object WidgetPrefKeys {
    val EVENT_ID = longPreferencesKey("event_id")
    val STYLE_OVERRIDE = stringPreferencesKey("style_override")
    val FORMAT_OVERRIDE = stringPreferencesKey("format_override")
}

/** Parameters attached to the tap action that opens an event from a widget. */
object WidgetActionParams {
    val EVENT_ID = ActionParameters.Key<Long>("event_id")
}
