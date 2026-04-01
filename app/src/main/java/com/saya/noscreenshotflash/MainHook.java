package com.saya.noscreenshotflash;

import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "NoScreenshotFlash";
    private static final String TARGET_PKG = "com.android.systemui";
    private static final String CONTROLLER_CLASS =
            "com.android.systemui.screenshot.ui.ScreenshotAnimationController";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded into SystemUI");

        try {
            Class<?> controllerClz = XposedHelpers.findClass(CONTROLLER_CLASS, lpparam.classLoader);

            // Hook all methods named "getEntranceAnimation" regardless of params
            for (Method m : controllerClz.getDeclaredMethods()) {
                if (m.getName().equals("getEntranceAnimation")) {
                    XposedBridge.log(TAG + ": hooking " + m);
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            hideFlashView(param.thisObject);
                        }
                    });
                }
            }

            // 兜底：hook 构造函数，flashView 赋值后立刻隐藏
            XposedHelpers.findAndHookConstructor(controllerClz, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    hideFlashView(param.thisObject);
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook failed: " + t);
        }
    }

    private void hideFlashView(Object controller) {
        try {
            View flashView = (View) XposedHelpers.getObjectField(controller, "flashView");
            if (flashView != null) {
                flashView.setVisibility(View.GONE);
                flashView.setAlpha(0f);
                XposedBridge.log(TAG + ": flashView hidden");
            }
        } catch (Throwable t) {
            // flashView 还未赋值时会抛，忽略即可
        }
    }
}
