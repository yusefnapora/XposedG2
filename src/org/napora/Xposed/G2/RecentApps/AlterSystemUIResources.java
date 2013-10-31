package org.napora.Xposed.G2.RecentApps;

import android.content.res.XResources;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import org.napora.Xposed.G2.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 6:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class AlterSystemUIResources implements IXposedHookInitPackageResources {
    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui")) return;
        if (!Settings.recentAppsKeyMod()) return;



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
}
