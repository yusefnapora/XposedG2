package org.napora.Xposed.G2;


import static de.robv.android.xposed.XposedHelpers.*;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class Declutter implements IXposedHookLoadPackage {

    private void hideGPSIcon(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.lge.systemui.LGPhoneStatusBarPolicy", lpparam.classLoader,
                "updateGPSPrivacySetting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object barService = getObjectField(param.thisObject, "mService");
                callMethod(barService, "setIconVisibility", "gps_privacy", false);
            }
        });
    }

    private void hideHeadsetIcon(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.lge.systemui.LGPhoneStatusBarPolicy", lpparam.classLoader,
                "updateHeadset", Intent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object barService = getObjectField(param.thisObject, "mService");
                callMethod(barService, "setIconVisibility", "headset", false);
            }
        });
    }


    private void hideMobileDataIconWhenWifiConnected(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.android.systemui.statusbar.policy.NetworkController", lpparam.classLoader,
                "updateDataNetType", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Boolean wifiEnabled = getBooleanField(param.thisObject, "mWifiEnabled");
                Boolean wifiConnected = getBooleanField(param.thisObject, "mWifiConnected");
                if (wifiEnabled && wifiConnected) {
                    setIntField(param.thisObject, "mThirdTypeIconId", 0);
                }
            }
        });
    }

    private void hideCarrierTextInNavigationPanel(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader,
                "makeStatusBarView",  new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View carrierLabel = (View) getObjectField(param.thisObject, "mCarrierLabel");
                carrierLabel.setVisibility(View.GONE);
                setBooleanField(param.thisObject, "mShowCarrierInPanel", false);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader,
                "updateCarrierLabelVisibility", Boolean.TYPE, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, "mShowCarrierInPanel", false);
            }
        });
    }

    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (Settings.hideGPSIcon()) {
            XposedBridge.log("Hiding GPS status bar icon");
            hideGPSIcon(lpparam);
        }

        if (Settings.hideHeadsetIcon()) {
            XposedBridge.log("Hiding headset status bar icon");
            hideHeadsetIcon(lpparam);
        }

        if (Settings.hideMobileDataWhenWifiConnected()) {
            XposedBridge.log("Hiding mobile data status bar icon when wifi is connected");
            hideMobileDataIconWhenWifiConnected(lpparam);
        }

        if (Settings.hideCarrierTextInNavigationPanel()) {
            XposedBridge.log("Hiding carrier text in navigation panel");
            hideCarrierTextInNavigationPanel(lpparam);
        }
    }

    private void hookNFC(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.android.nfc.LNfcCommonMethod$LNfcServiceIconHandler", lpparam.classLoader,
                "handleMessage", Message.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Message msg = (Message) param.args[0];
                Context context = (Context) msg.obj;
                Object statusBarManager = context.getSystemService("statusbar");
                callMethod(statusBarManager, "removeIcon", "lge_nfc");
                param.setResult(null);
            }
        });
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            hookSystemUI(lpparam);
        } else if (lpparam.packageName.equals("com.android.nfc")) {
            if (Settings.hideNFCIcon()) {
                XposedBridge.log("Hiding NFC status bar icon");
                hookNFC(lpparam);
            }
        }
    }

}
