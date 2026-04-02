package com.saya.noscreenshotflash;

import android.animation.ObjectAnimator;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "NoScreenshotFlash";
    private static final String TARGET_PKG = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;
        XposedBridge.log(TAG + ": loaded into SystemUI");

        // 方案1：拦截 ObjectAnimator.ofFloat，如果是对 screenshot_flash view 的 alpha 动画就取消
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
                        if (resName != null && resName.contains("screenshot_flash")) {
                            XposedBridge.log(TAG + ": blocked flash alpha anim on " + resName);
                            param.setResult(null); // 阻止动画启动
                            v.setAlpha(0f);
                            v.setVisibility(View.GONE);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        );

        // 方案2：拦截 View.setVisibility，如果是 screenshot_flash 被设为 VISIBLE 则拦截
        XposedHelpers.findAndHookMethod(
            View.class, "setVisibility", int.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        int visibility = (int) param.args[0];
                        if (visibility != View.VISIBLE) return;
                        View v = (View) param.thisObject;
                        String resName = getResName(v);
                        if (resName != null && resName.contains("screenshot_flash")) {
                            XposedBridge.log(TAG + ": blocked setVisibility(VISIBLE) on " + resName);
                            param.setResult(null);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        );

        XposedBridge.log(TAG + ": hooks installed");
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
