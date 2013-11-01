package org.napora.Xposed.G2;


import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class Declutter  {

    public static void hideGPSIcon(LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.lge.systemui.LGPhoneStatusBarPolicy", lpparam.classLoader,
                "updateGPSPrivacySetting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object barService = getObjectField(param.thisObject, "mService");
                callMethod(barService, "setIconVisibility", "gps_privacy", false);
            }
        });
    }

    public static void hideHeadsetIcon(LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.lge.systemui.LGPhoneStatusBarPolicy", lpparam.classLoader,
                "updateHeadset", Intent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object barService = getObjectField(param.thisObject, "mService");
                callMethod(barService, "setIconVisibility", "headset", false);
            }
        });
    }


    public static void hideMobileDataIconWhenWifiConnected(LoadPackageParam lpparam) throws Throwable {
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


    public static void hideNFCIcon(LoadPackageParam lpparam) throws Throwable {
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

    public static void hideCarrierTextInNavigationPanel(InitPackageResourcesParam resparam) throws Throwable {
        resparam.res.hookLayout("com.android.systemui", "layout", "super_status_bar",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        View carrierLabel = liparam.view.findViewById(
                                liparam.res.getIdentifier("carrier_label", "id", "com.android.systemui"));
                        ViewGroup parent = (ViewGroup)carrierLabel.getParent();
                        parent.removeView(carrierLabel);
                    }
                });
    }

    public static void hideCarrierTextInLockscreen(InitPackageResourcesParam resparam) throws Throwable {
        resparam.res.setReplacement("com.lge.lockscreen", "bool", "config_feature_display_carrier_string", false);
    }

}
