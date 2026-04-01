package com.saya.noscreenshotflash;

import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Constructor;
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

            // Hook 所有构造函数，不需要知道参数类型
            for (Constructor<?> ctor : controllerClz.getDeclaredConstructors()) {
                XposedBridge.log(TAG + ": hooking constructor " + ctor);
                XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        hideFlashView(param.thisObject);
                    }
                });
            }

            // Hook getEntranceAnimation，截图开始动画时再次确保隐藏
            for (Method m : controllerClz.getDeclaredMethods()) {
                if (m.getName().equals("getEntranceAnimation")
                        || m.getName().contains("Entrance")
                        || m.getName().contains("flash")
                        || m.getName().contains("Flash")) {
                    XposedBridge.log(TAG + ": hooking method " + m.getName());
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            hideFlashView(param.thisObject);
                        }
                    });
                }
            }

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
        } catch (Throwable ignored) {
            // flashView 尚未赋值时忽略
        }
    }
}
