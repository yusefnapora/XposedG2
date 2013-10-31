package org.napora.Xposed.G2;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnableActionBarOverflowMenu implements IXposedHookZygoteInit {
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!Settings.enableActionBarOverflowMenu()) return;
        XposedBridge.log("Enabling ActionBar overflow menu");

        findAndHookMethod("android.view.ViewConfiguration", null, "hasPermanentMenuKey", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
