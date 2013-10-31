package org.napora.Xposed.G2;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class Settings {
    // FIXME: This whole class needs to be based on SharedPreferences, not hardcoded crap

    public static boolean recentAppsKeyMod() {
        return true;
    }

    // Which button does the recent apps key replace?
    // //Returns the name of a resource id as referenced in navigation_bar.xml
    public static String recentAppsButtonReplacement() {
        return "menu";
    }

    public static boolean enableActionBarOverflowMenu() {
        return true;
    }

    public static boolean longPressHomeForMenu() {
        return true;
    }

    public static boolean hideNFCIcon() {
        return true;
    }

    public static boolean hideHeadsetIcon() {
        return true;
    }

    public static boolean hideGPSIcon() {
        return true;
    }

    public static boolean hideMobileDataWhenWifiConnected() {
        return true;
    }

    public static boolean hideCarrierTextInNavigationPanel() {
        return false;
    }
}
