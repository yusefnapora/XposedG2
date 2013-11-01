package org.napora.Xposed.G2;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/31/13
 * Time: 11:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavBarSize {
    public static void changeNavBarSize(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        int heightId, widthId;
        if (Settings.navBarSize().equals("mid")) {
            heightId = R.dimen.navbar_height_mid;
            widthId = R.dimen.navbar_width_mid;
        } else if (Settings.navBarSize().equals("tiny")) {
            heightId = R.dimen.navbar_height_tiny;
            widthId = R.dimen.navbar_width_tiny;
        } else {
            XposedBridge.log("Unknown navbar size value: " + Settings.navBarSize());
            return;
        }


        XposedBridge.log("Setting nav bar size to " + Settings.navBarSize());

        XModuleResources modRes = XModuleResources.createInstance(startupParam.modulePath, null);
        XResources.setSystemWideReplacement("android", "dimen", "navigation_bar_height", modRes.fwd(heightId));
        XResources.setSystemWideReplacement("android", "dimen", "navigation_bar_height_landscape", modRes.fwd(heightId));
        XResources.setSystemWideReplacement("android", "dimen", "navigation_bar_width", modRes.fwd(widthId));
    }
}
