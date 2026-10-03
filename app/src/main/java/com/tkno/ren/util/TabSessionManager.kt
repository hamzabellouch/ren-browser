package com.tkno.ren.util

import android.content.Context
import android.content.SharedPreferences
import com.tkno.ren.model.WebTab
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Data representation of a WebTab for serialization, session restoration,
 * and closed tab history tracking.
 */
data class SavedTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Blank page",
    val url: String = "about:blank",
    val isIncognito: Boolean = false,
    val isDesktopSite: Boolean = false,
    val isReaderMode: Boolean = false,
    val originalUrlBeforeReader: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
            put("isIncognito", isIncognito)
            put("isDesktopSite", isDesktopSite)
            put("isReaderMode", isReaderMode)
            if (originalUrlBeforeReader != null) {
                put("originalUrlBeforeReader", originalUrlBeforeReader)
            }
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SavedTab {
            return SavedTab(
                id = obj.optString("id", UUID.randomUUID().toString()),
                title = obj.optString("title", "Blank page"),
                url = obj.optString("url", "about:blank"),
                isIncognito = obj.optBoolean("isIncognito", false),
                isDesktopSite = obj.optBoolean("isDesktopSite", false),
                isReaderMode = obj.optBoolean("isReaderMode", false),
                originalUrlBeforeReader = if (obj.has("originalUrlBeforeReader") && !obj.isNull("originalUrlBeforeReader")) {
                    obj.optString("originalUrlBeforeReader")
                } else {
                    null
                },
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }

        fun fromWebTab(webTab: WebTab): SavedTab {
            return SavedTab(
                id = webTab.id,
                title = webTab.title,
                url = webTab.url,
                isIncognito = webTab.isIncognito,
                isDesktopSite = webTab.isDesktopSite,
                isReaderMode = webTab.isReaderMode,
                originalUrlBeforeReader = webTab.originalUrlBeforeReader,
                timestamp = System.currentTimeMillis()
            )
        }

        fun listToJson(list: List<SavedTab>): String {
            val jsonArray = JSONArray()
            for (tab in list) {
                jsonArray.put(tab.toJson())
            }
            return jsonArray.toString()
        }

        fun listFromJson(jsonStr: String?): List<SavedTab> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val list = mutableListOf<SavedTab>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    list.add(fromJson(obj))
                }
            } catch (e: Exception) {
                // Safeguard against malformed JSON
            }
            return list
        }
    }
}

/**
 * Extension helper to easily convert a [WebTab] to a [SavedTab].
 */
fun WebTab.toSavedTab(): SavedTab = SavedTab.fromWebTab(this)

/**
 * Manages tab session persistence, startup tab restoration,
 * closed tabs history (Undo Close Tab stack), and session clearing.
 */
object TabSessionManager {

    private const val PREFS_NAME = "ren_tab_session_prefs"
    private const val KEY_SAVED_OPEN_TABS = "saved_open_tabs_json"
    private const val KEY_SAVED_ACTIVE_TAB_ID = "saved_active_tab_id"
    private const val KEY_SAVED_ACTIVE_TAB_INDEX = "saved_active_tab_index"
    private const val KEY_CLOSED_TABS = "closed_tabs_json"
    private const val KEY_RESTORE_TABS_ENABLED = "restore_tabs_on_startup_enabled"

    // Settings preferences file used by SettingsPages
    private const val SETTINGS_PREFS_NAME = "links_prefs"
    private const val KEY_RESTORE_TABS_OPTION = "restore_tabs_on_startup"

    const val OPTION_ALWAYS_RESTORE = "Always restore"
    const val OPTION_DONT_RESTORE = "Don't restore"
    const val OPTION_ASK_FIRST = "Ask first"

    const val MAX_CLOSED_TABS = 20

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getSettingsPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =========================================================================
    // Startup Restore Configuration
    // =========================================================================

    /**
     * Returns true if tabs should be automatically restored on app launch.
     */
    fun isRestoreTabsOnStartupEnabled(context: Context): Boolean {
        val settingsOption = getSettingsPrefs(context).getString(KEY_RESTORE_TABS_OPTION, null)
        if (settingsOption != null) {
            return settingsOption.equals(OPTION_ALWAYS_RESTORE, ignoreCase = true) ||
                   settingsOption.equals("true", ignoreCase = true) ||
                   settingsOption.equals("1", ignoreCase = true)
        }
        return getPrefs(context).getBoolean(KEY_RESTORE_TABS_ENABLED, false)
    }

    /**
     * Enables or disables automatic tab restoration on app startup.
     */
    fun setRestoreTabsOnStartupEnabled(context: Context, enabled: Boolean) {
        val option = if (enabled) OPTION_ALWAYS_RESTORE else OPTION_DONT_RESTORE
        getSettingsPrefs(context).edit().putString(KEY_RESTORE_TABS_OPTION, option).apply()
        getPrefs(context).edit().putBoolean(KEY_RESTORE_TABS_ENABLED, enabled).apply()
    }

    /**
     * Gets the startup restore option ("Always restore", "Don't restore", "Ask first").
     */
    fun getRestoreTabsStartupOption(context: Context): String {
        return getSettingsPrefs(context).getString(KEY_RESTORE_TABS_OPTION, OPTION_DONT_RESTORE) ?: OPTION_DONT_RESTORE
    }

    /**
     * Sets the startup restore option string.
     */
    fun setRestoreTabsStartupOption(context: Context, option: String) {
        getSettingsPrefs(context).edit().putString(KEY_RESTORE_TABS_OPTION, option).apply()
        val isEnabled = option.equals(OPTION_ALWAYS_RESTORE, ignoreCase = true)
        getPrefs(context).edit().putBoolean(KEY_RESTORE_TABS_ENABLED, isEnabled).apply()
    }

    // =========================================================================
    // Open Tabs Persistence (Session Save / Restore)
    // =========================================================================

    /**
     * Saves the current list of open non-incognito tabs to persistent storage.
     * Incognito tabs are strictly ignored for privacy.
     */
    fun saveOpenTabs(context: Context, tabs: List<WebTab>, activeTabId: String? = null) {
        val nonIncognitoSaved = tabs
            .filter { !it.isIncognito }
            .map { it.toSavedTab() }

        val activeIndex = if (activeTabId != null) {
            tabs.indexOfFirst { it.id == activeTabId }
        } else {
            -1
        }

        saveSavedTabs(context, nonIncognitoSaved, activeTabId, activeIndex)
    }

    /**
     * Saves a list of [SavedTab] objects to persistent storage.
     */
    fun saveSavedTabs(
        context: Context,
        tabs: List<SavedTab>,
        activeTabId: String? = null,
        activeTabIndex: Int = -1
    ) {
        val nonIncognito = tabs.filter { !it.isIncognito }
        val jsonStr = SavedTab.listToJson(nonIncognito)

        getPrefs(context).edit()
            .putString(KEY_SAVED_OPEN_TABS, jsonStr)
            .putString(KEY_SAVED_ACTIVE_TAB_ID, activeTabId ?: "")
            .putInt(KEY_SAVED_ACTIVE_TAB_INDEX, activeTabIndex)
            .apply()
    }

    /**
     * Returns the saved list of open non-incognito tabs from the previous session.
     */
    fun getSavedOpenTabs(context: Context): List<SavedTab> {
        val jsonStr = getPrefs(context).getString(KEY_SAVED_OPEN_TABS, null)
        return SavedTab.listFromJson(jsonStr).filter { !it.isIncognito }
    }

    /**
     * Returns the ID of the tab that was active when the session was saved.
     */
    fun getSavedActiveTabId(context: Context): String? {
        val id = getPrefs(context).getString(KEY_SAVED_ACTIVE_TAB_ID, null)
        return if (!id.isNullOrBlank()) id else null
    }

    /**
     * Returns the index of the tab that was active when the session was saved.
     */
    fun getSavedActiveTabIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_SAVED_ACTIVE_TAB_INDEX, -1)
    }

    /**
     * Checks if there are any saved open tabs available to restore.
     */
    fun hasSavedOpenTabs(context: Context): Boolean {
        return getSavedOpenTabs(context).isNotEmpty()
    }

    // =========================================================================
    // Closed Tabs History / "Undo Close Tab" Stack
    // =========================================================================

    /**
     * Pushes a closed tab onto the Undo stack (max 20 tabs).
     * Incognito tabs are never saved for privacy.
     */
    @Synchronized
    fun pushClosedTab(context: Context, tab: WebTab) {
        if (tab.isIncognito) return
        pushClosedTab(context, tab.toSavedTab())
    }

    /**
     * Pushes a [SavedTab] onto the Undo stack (max 20 tabs).
     * Incognito tabs are never saved for privacy.
     */
    @Synchronized
    fun pushClosedTab(context: Context, tab: SavedTab) {
        if (tab.isIncognito) return

        val currentList = getClosedTabs(context).toMutableList()
        // Remove existing entry with same ID if present to avoid duplication
        currentList.removeAll { it.id == tab.id }

        // Add to top of stack
        currentList.add(0, tab)

        // Trim to MAX_CLOSED_TABS (20)
        val trimmed = if (currentList.size > MAX_CLOSED_TABS) {
            currentList.subList(0, MAX_CLOSED_TABS)
        } else {
            currentList
        }

        saveClosedTabsList(context, trimmed)
    }

    /**
     * Pops and removes the most recently closed tab from the Undo stack.
     * Returns null if no closed tabs are available.
     */
    @Synchronized
    fun popClosedTab(context: Context): SavedTab? {
        val currentList = getClosedTabs(context).toMutableList()
        if (currentList.isEmpty()) return null

        val topTab = currentList.removeAt(0)
        saveClosedTabsList(context, currentList)
        return topTab
    }

    /**
     * Peeks at the most recently closed tab without removing it from the stack.
     */
    @Synchronized
    fun peekClosedTab(context: Context): SavedTab? {
        val currentList = getClosedTabs(context)
        return currentList.firstOrNull()
    }

    /**
     * Retrieves the list of recently closed tabs (up to 20, newest first).
     */
    @Synchronized
    fun getClosedTabs(context: Context): List<SavedTab> {
        val jsonStr = getPrefs(context).getString(KEY_CLOSED_TABS, null)
        return SavedTab.listFromJson(jsonStr).filter { !it.isIncognito }
    }

    /**
     * Removes a specific tab by ID from the closed tabs history.
     */
    @Synchronized
    fun removeClosedTab(context: Context, tabId: String): Boolean {
        val currentList = getClosedTabs(context).toMutableList()
        val removed = currentList.removeAll { it.id == tabId }
        if (removed) {
            saveClosedTabsList(context, currentList)
        }
        return removed
    }

    /**
     * Checks if there are any closed tabs stored in the Undo stack.
     */
    fun hasClosedTabs(context: Context): Boolean {
        return getClosedTabs(context).isNotEmpty()
    }

    /**
     * Clears all closed tabs from the Undo stack.
     */
    @Synchronized
    fun clearClosedTabs(context: Context) {
        getPrefs(context).edit().remove(KEY_CLOSED_TABS).apply()
    }

    private fun saveClosedTabsList(context: Context, list: List<SavedTab>) {
        val jsonStr = SavedTab.listToJson(list)
        getPrefs(context).edit().putString(KEY_CLOSED_TABS, jsonStr).apply()
    }

    // =========================================================================
    // Clear Session Data Support
    // =========================================================================

    /**
     * Clears only the saved open tabs session data.
     */
    fun clearOpenTabs(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_SAVED_OPEN_TABS)
            .remove(KEY_SAVED_ACTIVE_TAB_ID)
            .remove(KEY_SAVED_ACTIVE_TAB_INDEX)
            .apply()
    }

    /**
     * Clears both saved open tabs and closed tabs history stack.
     */
    fun clearSession(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_SAVED_OPEN_TABS)
            .remove(KEY_SAVED_ACTIVE_TAB_ID)
            .remove(KEY_SAVED_ACTIVE_TAB_INDEX)
            .remove(KEY_CLOSED_TABS)
            .apply()
    }

    /**
     * Clears all data managed by [TabSessionManager].
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
