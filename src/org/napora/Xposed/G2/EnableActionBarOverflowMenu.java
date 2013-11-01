package org.napora.Xposed.G2;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnableActionBarOverflowMenu {
    public static void hookZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XposedBridge.log("Enabling ActionBar overflow menu");

        findAndHookMethod("android.view.ViewConfiguration", null, "hasPermanentMenuKey", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
