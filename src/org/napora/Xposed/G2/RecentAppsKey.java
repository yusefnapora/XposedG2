package org.napora.Xposed.G2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XResources;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import static de.robv.android.xposed.XposedHelpers.*;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Replace either the Menu key or Notification key with Recent Apps
 */
public class RecentAppsKey {
    public static void hookSystemUIResources(InitPackageResourcesParam resparam)
            throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui")) return;

        // Replace either the menu key or the notification key with recent apps
        final String replacementButtonName = Settings.recentAppsButtonReplacement();
        XposedBridge.log("Replacing " + replacementButtonName + " with recent_apps");

        XC_LayoutInflated inflateHook = new XC_LayoutInflated() {
            private void swapButtons(View navbar, XResources resources, boolean isLandscape) {
                View buttonToRemove = navbar.findViewById(
                        resources.getIdentifier(replacementButtonName, "id", "com.android.systemui"));

                View recentsButton = navbar.findViewById(
                        resources.getIdentifier("recent_apps", "id", "com.android.systemui"));

                //  we need to pull the layout_gravity, layout_width and tag from the button we're replacing
                // and add them to the recents button
                recentsButton.setLayoutParams(buttonToRemove.getLayoutParams());
                recentsButton.setTag(buttonToRemove.getTag());
                recentsButton.setVisibility(View.VISIBLE);
                XposedBridge.log("Set recent_apps button tag to " + recentsButton.getTag());

                // Got to remove the tag from the button we're replacing and set its visibility to GONE
                buttonToRemove.setTag(null);
                buttonToRemove.setVisibility(View.GONE);

                // If we're replacing the notification view, force the dummy view visibility to GONE
                if (replacementButtonName.equals("notification")) {
                    View dummyView = navbar.findViewById(resources.getIdentifier("notification_dummy", "id", "com.android.systemui"));
                    dummyView.setVisibility(View.GONE);
                }

                // If we're in landscape, we also want to muck with the layout a bit.
                // There's an invisible spacer view that comes immediately before the recent apps button
                // It throws off the spacing, and should come immediately after the recent apps button
                // Make it so.
                if (isLandscape) {
                    ViewGroup parent = (ViewGroup)recentsButton.getParent();
                    int recentsIndex = parent.indexOfChild(recentsButton);
                    View spacer = parent.getChildAt(recentsIndex-1);
                    parent.removeView(spacer);
                    // since we removed a view, recentsIndex is now *after* the recents button, as all following views
                    // had their indices shifted down by one
                    parent.addView(spacer, recentsIndex);
                }
            }

            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                View portrait = liparam.view.findViewById(
                        liparam.res.getIdentifier("rot0", "id", "com.android.systemui"));
                View landscape = liparam.view.findViewById(
                        liparam.res.getIdentifier("rot90", "id", "com.android.systemui"));

                swapButtons(portrait, liparam.res, false);
                swapButtons(landscape, liparam.res, true);
            }
        };


        resparam.res.hookLayout("com.android.systemui", "layout", "navigation_bar", inflateHook);
        try {
            // navigation_bar_with_cursor only exists on AT&T models (possibly international too)
            resparam.res.hookLayout("com.android.systemui", "layout", "navigation_bar_with_cursor", inflateHook);
        } catch (Exception e) {
            XposedBridge.log("Didn't find navigation_bar_with_cursor layout.  Probably a verizon device");
        }
    }


    public static void hookSystemUICode(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui")) return;

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
                Resources res = c.getResources();
                int recentAppsID = res.getIdentifier("recent_apps", "id", "com.android.systemui");

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

                    int defaultButtonId = context.getResources()
                            .getIdentifier("ic_sysbar_recent_button", "id", "com.android.systemui");
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
                    Resources res = context.getResources();
                    Boolean isPortrait = (Boolean)isPortraitMethod.invoke(null, context);
                    int recentButtonId = res.getIdentifier("recent_apps", "id", "com.android.systemui");
                    int defaultButtonId = res.getIdentifier("ic_sysbar_recent_button", "id", "com.android.systemui");

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
