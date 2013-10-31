package org.napora.Xposed.G2;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 8:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class LongPressHomeForMenu implements IXposedHookZygoteInit {
    private void injectMenuKey() {
        long now = SystemClock.uptimeMillis();
        InputManager im = (InputManager)callStaticMethod(InputManager.class, "getInstance");
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU,
                0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU,
                0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        callMethod(im, "injectInputEvent", down, 0);
        callMethod(im, "injectInputEvent", up, 0);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!Settings.longPressHomeForMenu()) return;
        XposedBridge.log("Setting Long-press Home -> Menu");

        findAndHookMethod("com.android.internal.policy.impl.PhoneWindowManager", null,
                "handleLongPressOnHome",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        injectMenuKey();
                        setBooleanField(param.thisObject, "mHomeLongPressed", true);
                        param.setResult(null);
                    }
                });
    }
}
