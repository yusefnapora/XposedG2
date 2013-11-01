package org.napora.Xposed.G2;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 11/1/13
 * Time: 9:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class Themes {

    public static void blackNavBar(InitPackageResourcesParam resparam, XModuleResources modRes) throws Throwable {
        String packageName = "com.lge.systemui.theme.lovelypink";
        if (!resparam.packageName.equals(packageName)) return;

        resparam.res.setReplacement(packageName, "string", "theme_name", "Black");

        XResources.DrawableLoader blackLoader = new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(Color.BLACK);
            }
        };
        resparam.res.setReplacement(packageName, "drawable", "bg_sysbar_land", blackLoader);
        resparam.res.setReplacement(packageName, "drawable", "bg_sysbar_normal", blackLoader);
        resparam.res.setReplacement(packageName, "drawable", "preview", modRes.fwd(R.drawable.black_statusbar_preview));

    }

    public static void blackStatusBar(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui")) return;

        XResources.DrawableLoader blackLoader = new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(Color.BLACK);
            }
        };
        resparam.res.setReplacement("com.android.systemui", "drawable", "stat_sys_indi_bg", blackLoader);
    }
}
