package org.napora.Xposed.G2;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/31/13
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntryPoint implements IXposedHookInitPackageResources, IXposedHookZygoteInit, IXposedHookLoadPackage
{

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (resparam.packageName.equals("com.android.systemui")) {
            if (Settings.recentAppsKeyMod()) {
                RecentAppsKey.hookSystemUIResources(resparam);
            }

            if (Settings.hideCarrierTextInNavigationPanel()) {
                XposedBridge.log("Hiding carrier text in navigation panel");
                Declutter.hideCarrierTextInNavigationPanel(resparam);
            }
        }

        if (resparam.packageName.equals("com.lge.lockscreen")
            && Settings.hideCarrierTextInLockScreen()) {
            XposedBridge.log("Hiding Carrier text in lock screen");
            Declutter.hideCarrierTextInLockscreen(resparam);
        }
    }

    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Settings.hideGPSIcon()) {
            XposedBridge.log("Hiding GPS status bar icon");
            Declutter.hideGPSIcon(lpparam);
        }

        if (Settings.hideHeadsetIcon()) {
            XposedBridge.log("Hiding headset status bar icon");
            Declutter.hideHeadsetIcon(lpparam);
        }

        if (Settings.hideMobileDataWhenWifiConnected()) {
            XposedBridge.log("Hiding mobile data status bar icon when wifi is connected");
            Declutter.hideMobileDataIconWhenWifiConnected(lpparam);
        }

        if (Settings.recentAppsKeyMod()) {
            RecentAppsKey.hookSystemUICode(lpparam);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            hookSystemUI(lpparam);
        } else if (lpparam.packageName.equals("com.android.nfc")) {
            if (Settings.hideNFCIcon()) {
                XposedBridge.log("Hiding NFC status bar icon");
                Declutter.hideNFCIcon(lpparam);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (Settings.enableActionBarOverflowMenu()) {
            ActionBarOverflowMenu.hookZygote(startupParam);
        }

        if (!Settings.longPressHomeBehavior().equals("default")) {
            LongPressHomeBehavior.hookZygote(startupParam);
        }

        if (Settings.hideClockAMPM()) {
            Declutter.hideClockAMPMText(startupParam);
        }
        if (Settings.changeNavBarSize()) {
            NavBarSize.changeNavBarSize(startupParam);
        }
    }
}
