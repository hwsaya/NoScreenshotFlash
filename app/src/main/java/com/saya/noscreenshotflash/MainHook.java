package com.saya.noscreenshotflash;

import android.animation.ObjectAnimator;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;
        if (!lpparam.processName.contains("screenshot")) return;

        XposedHelpers.findAndHookMethod(
            ObjectAnimator.class, "start",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        ObjectAnimator anim = (ObjectAnimator) param.thisObject;
                        if (!"alpha".equals(anim.getPropertyName())) return;
                        Object target = anim.getTarget();
                        if (!(target instanceof View)) return;
                        View v = (View) target;
                        String resName = getResName(v);
                        if (resName != null && resName.contains("flash")) {
                            param.setResult(null);
                            v.setAlpha(0f);
                            v.setVisibility(View.GONE);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        );
    }

    private String getResName(View v) {
        try {
            int id = v.getId();
            if (id == View.NO_ID) return null;
            return v.getResources().getResourceEntryName(id);
        } catch (Throwable e) {
            return null;
        }
    }
}
