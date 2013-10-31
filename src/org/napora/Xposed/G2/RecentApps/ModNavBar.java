package org.napora.Xposed.G2.RecentApps;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.napora.Xposed.G2.Settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 7:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class ModNavBar implements IXposedHookLoadPackage {
    private int getResourceId(String resName, Context context) {
        Resources res = context.getResources();
        return res.getIdentifier(resName, "id", "com.android.systemui");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui")) return;
        if (!Settings.recentAppsKeyMod()) return;

        final String keyToReplace = Settings.recentAppsButtonReplacement();
        final int keycodeToReplace;
        final String getReplacementButtonMethod;
        if (keyToReplace.equals("menu")) {
            keycodeToReplace = KeyEvent.KEYCODE_MENU;
            getReplacementButtonMethod = "getMenuButton";
        } else if (keyToReplace.equals("notification")) {
            keycodeToReplace = KeyEvent.KEYCODE_NOTIFICATION;
            getReplacementButtonMethod = "getNotificationButton";
        } else {
            XposedBridge.log("Unknown value for recent apps button replacement: " + keyToReplace);
            return;
        }

        Class<?> navigationBarViewClass =
                findClass("com.android.systemui.statusbar.phone.NavigationBarView", lpparam.classLoader);
        Constructor<?> navigationBarViewConstructor =
                findConstructorExact(navigationBarViewClass, Context.class, AttributeSet.class);

        // After hook the constructor to modify the ID, KEYCODE, and CONTENT_DESCRIPTION arrays
        XposedBridge.hookMethod(navigationBarViewConstructor, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int ids[] = (int[])getObjectField(param.thisObject, "ID");
                int keycodes[] = (int[])getObjectField(param.thisObject, "KEYCODE");
                int content_descriptions[] = (int[])getObjectField(param.thisObject, "CONTENT_DESCRIPTION");

                // loop through the keycodes to find the index of the button we want to replace.  The index will
                // be the same for all three arrays
                int idx = -1;
                for (int i = 0; i < keycodes.length; i++) {
                    if (keycodes[i] == keycodeToReplace) {
                        idx = i;
                        break;
                    }
                }
                if (idx == -1) {
                    XposedBridge.log("Unable to find key with keycode 0x" + Integer.toHexString(keycodeToReplace));
                    return;
                }
                XposedBridge.log("Replacing button with index " + idx +
                        ", keycode 0x" + Integer.toHexString(keycodes[idx]) +
                        ", id 0x" + Integer.toHexString(ids[idx]));

                // get the id of the recent apps key
                Context c = (Context)param.args[0];
                int recentAppsID = getResourceId("recent_apps", c);

                // and the id of the content description string resource
                int recentAppsContentDescId = c.getResources().getIdentifier("accessibility_recent", "string", "com.android.systemui");

                ids[idx] = recentAppsID;
                content_descriptions[idx] = recentAppsContentDescId;
                // The keycode gets set to zero, because there's a custom touch handler on the recent apps key
                keycodes[idx] = 0;

                setObjectField(param.thisObject, "ID", ids);
                setObjectField(param.thisObject, "KEYCODE", keycodes);
                setObjectField(param.thisObject, "CONTENT_DESCRIPTION", content_descriptions);
            }
        });

        // After hook setDisabledFlags(int, boolean) which gets called whenever the nav bar is updated
        // force the visibility of the key we're replacing to GONE
        findAndHookMethod(navigationBarViewClass, "setDisabledFlags", Integer.TYPE, Boolean.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View buttonView = (View)callMethod(param.thisObject, getReplacementButtonMethod);
                        buttonView.setVisibility(View.GONE);
                        if (keyToReplace.equals("notification")) {
                            View notificationDummyView = (View)getObjectField(param.thisObject, "mNotificationDummyView");
                            notificationDummyView.setVisibility(View.GONE);
                        }
                    }
                });

        // After hook to update the button resources using the resources from the current theme
        // On Verizon models, this is done in the updateButtonsUsingTheme() method of NavigationBarView
        // On AT&T (and possibly international versions) it's done in com.lge.systemui.ThemeChanger/updateButtons()
        Method updateUsingThemeMethod = findMethodExact(navigationBarViewClass, "updateButtonsUsingTheme");
        XC_MethodHook updateThemeHook;

        // We need to call Utils.isPortraitMode() in the hook
        final Class<?> utilsClass = findClass("com.lge.systemui.Utils", lpparam.classLoader);
        final Method isPortraitMethod = findMethodExact(utilsClass, "isPortraitMode", Context.class);

        if (updateUsingThemeMethod == null) {
            // AT&T version
            Class<?> themeChangerClass = findClass("com.lge.systemui.Utils", lpparam.classLoader);
            updateUsingThemeMethod = findMethodExact(themeChangerClass, "updateButtons");
            final Method setButtonImageMethod =
                    findMethodExact(themeChangerClass, "setButtonImage", View.class, Integer.TYPE, Integer.TYPE);

            updateThemeHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context)getObjectField(param.thisObject, "mContext");
                    Boolean isPortrait = (Boolean)isPortraitMethod.invoke(null, context);
                    Object navBarView = getObjectField(param.thisObject, "mNavigationBarView");
                    View recentsButtonView = (View)callMethod(navBarView, "getRecentsButton");

                    int defaultButtonId = getResourceId("ic_sysbar_recent_button", context);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> tagMap = (Map<String,Integer>)callMethod(param.thisObject, "getTagMap");
                    int tag;
                    if (isPortrait) {
                        tag = tagMap.get("ButtonRecent");
                    } else {
                        tag = tagMap.get("ButtonRecentLand");
                    }
                    setButtonImageMethod.invoke(param.thisObject, recentsButtonView, tag, defaultButtonId);
                }
            };
        } else {
            // Verizon version
            updateThemeHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context)getObjectField(param.thisObject, "mContext");
                    Boolean isPortrait = (Boolean)isPortraitMethod.invoke(null, context);
                    int recentButtonId = getResourceId("recent_apps", context);
                    int defaultButtonId = getResourceId("ic_sysbar_recent_button", context);

                    Object themeChanger = getObjectField(param.thisObject, "mThemeChanger");
                    @SuppressWarnings("unchecked")
                    Map<String,Integer> tagMap = (Map<String,Integer>)callMethod(themeChanger, "getTagMap");
                    int tag;
                    if (isPortrait) {
                        tag = tagMap.get("ButtonRecent");
                    } else {
                        tag = tagMap.get("ButtonRecentLand");
                    }
                    callMethod(param.thisObject, "setButtonImageUsingTheme", recentButtonId, tag, defaultButtonId);
                }
            };
        }
        XposedBridge.hookMethod(updateUsingThemeMethod, updateThemeHook);

        // After hook NavigationBarView/updateNavigationBar() to do some reflection hacks on
        // PhoneStatusBar.  Basically, when the navigation bar layout changes, we need to re-add
        // the onClickListener contained in the field PhoneStatusBar/mRecentsClickListener, as well
        // as the touch listener in PhoneStatusBar/mRecentsPreloadOnTouchListener

        // First we have to hook the setBar(BaseStatusBar) method to keep a reference around to the status bar
        Class<?> baseStatusBarClass = findClass("com.android.systemui.statusbar.BaseStatusBar", lpparam.classLoader);
        findAndHookMethod(navigationBarViewClass, "setBar", baseStatusBarClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                setAdditionalInstanceField(param.thisObject, "mPhoneStatusBarHack", param.args[0]);
            }
        });

        // Now we hook the updateNavigationBar() method to pull the click and touch listeners from the
        // bar we saved earlier and add them to the recents button.
        findAndHookMethod(navigationBarViewClass, "updateNavigationBar", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object phoneStatusBar = getAdditionalInstanceField(param.thisObject, "mPhoneStatusBarHack");
                if (phoneStatusBar == null) return;

                View.OnClickListener clickListener = (View.OnClickListener) getObjectField(phoneStatusBar, "mRecentsClickListener");
                View.OnTouchListener touchListener = (View.OnTouchListener) getObjectField(phoneStatusBar, "mRecentsPreloadOnTouchListener");
                View recentsButton = (View)callMethod(param.thisObject, "getRecentsButton");
                recentsButton.setOnClickListener(clickListener);
                recentsButton.setOnTouchListener(touchListener);
            }
        });
    }
}
