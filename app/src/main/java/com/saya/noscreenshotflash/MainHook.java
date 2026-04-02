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


        // DEBUG: 打印所有 alpha ObjectAnimator，找到真实的 flash view 名称
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
                        // 打印所有 alpha 动画目标，方便找到 flash view
                        XposedBridge.log(TAG + ": alpha anim on view id=" + resName
                                + " class=" + v.getClass().getSimpleName());
                        // 拦截包含 flash 的
                        if (resName != null && resName.contains("flash")) {
                            XposedBridge.log(TAG + ": BLOCKING flash anim on " + resName);
                            param.setResult(null);
                            v.setAlpha(0f);
                            v.setVisibility(View.GONE);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": anim hook err: " + t);
                    }
                }
            }
        );

        // DEBUG: 打印所有 setVisibility(VISIBLE) 调用，找 flash 相关
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
                        if (resName == null) return;
                        // 只打印 screenshot 相关的 view
                        if (resName.contains("screenshot") || resName.contains("flash")) {
                            XposedBridge.log(TAG + ": setVisible on " + resName);
                        }
                        if (resName.contains("flash")) {
                            XposedBridge.log(TAG + ": BLOCKING setVisible on " + resName);
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
