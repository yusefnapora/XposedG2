package org.napora.Xposed.G2;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class Settings {
    // FIXME: This whole class needs to be based on SharedPreferences, not hardcoded crap

    private static XSharedPreferences sSharedPrefs;
    private static XSharedPreferences getSharedPrefs() {
        if (sSharedPrefs == null) {
            sSharedPrefs = new XSharedPreferences("org.napora.Xposed.G2");
        }
        return sSharedPrefs;
    }

    public static boolean recentAppsKeyMod() {
        return !recentAppsButtonReplacement().equals("default");
    }

    // Which button does the recent apps key replace?
    // //Returns the name of a resource id as referenced in navigation_bar.xml
    public static String recentAppsButtonReplacement() {
        return getSharedPrefs().getString("pref_key_recent_apps_mod", "default");
    }

    public static boolean enableActionBarOverflowMenu() {
        return getSharedPrefs().getBoolean("pref_key_action_bar_overflow", false);
    }

    public static String longPressHomeBehavior() {
        return getSharedPrefs().getString("pref_key_long_press_home_behavior", "default");
    }

    public static boolean longPressHomeForMenu() {
        return longPressHomeBehavior().equals("menu");
    }

    public static boolean longPressHomeForNotification() {
        return longPressHomeBehavior().equals("notification");
    }

    public static boolean hideNFCIcon() {
        return getSharedPrefs().getBoolean("pref_key_hide_nfc_icon", false);
    }

    public static boolean hideHeadsetIcon() {
        return getSharedPrefs().getBoolean("pref_key_hide_headset_icon", false);
    }

    public static boolean hideGPSIcon() {
        return getSharedPrefs().getBoolean("pref_key_hide_gps_icon", false);
    }

    public static boolean hideMobileDataWhenWifiConnected() {
        return getSharedPrefs().getBoolean("pref_key_hide_mobile_data_icon", false);
    }

    public static boolean hideCarrierTextInNavigationPanel() {
        return getSharedPrefs().getBoolean("pref_key_hide_carrier_in_panel", false);
    }

    public static boolean hideCarrierTextInLockScreen() {
        return getSharedPrefs().getBoolean("pref_key_hide_carrier_in_lockscreen", false);
    }

    public static boolean changeNavBarSize() {
        return !navBarSize().equals("default");
    }

    public static String navBarSize() {
        return getSharedPrefs().getString("pref_key_nav_bar_size", "default");
    }

    public static boolean hideClockAMPM() {
        return getSharedPrefs().getBoolean("pref_key_hide_am_pm", false);
    }

    public static boolean blackNavBarTheme() {
        return getSharedPrefs().getBoolean("pref_key_black_status_bar", false);
    }

    public static boolean blackStatusBarTheme() {
        return getSharedPrefs().getBoolean("pref_key_black_nav_bar", false);
    }
}
