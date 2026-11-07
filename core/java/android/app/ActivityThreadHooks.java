/*
 * Copyright (C) 2026 The ValkyOS Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on GrapheneOS ActivityThreadHooks.
 */
package android.app;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.GosPackageState;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

/**
 * App-side hooks called by ActivityThread. They deliver the GosPackageState of the app
 * (per-app privacy settings) to the features that need it, both when the app starts and
 * whenever the state changes.
 *
 * @hide
 */
class ActivityThreadHooks {
    private static final String TAG = "ValkyAppBindArgs";

    @Nullable // null during the early part of app process init
    private static volatile Context appContext;

    private static final Object gosPackageStateChangeLock = new Object();
    private static boolean hadInitialGosPsChangeCallback;
    private static boolean hasPendingGosPsChangeCallback;

    // called after the initial app context is constructed
    // ActivityThread.handleBindApplication
    @Nullable
    static Bundle onBind(ActivityThread.AppBindData appBindData) {
        return appBindData.extraArgs;
    }

    // called after ActivityThread instrumentation is inited, which happens before execution of
    // any of app's code
    // ActivityThread.handleBindApplication
    static void onBind2(Context appContext, @Nullable Bundle appBindArgs) {
        ActivityThreadHooks.appContext = appContext;
        if (!Process.isIsolated()) { // isolated processes don't have access to GosPackageState
            handleGosPackageStateChange(true, appBindArgs);
        }
    }

    // called from a binder thread (IApplicationThread.onGosPackageStateChanged)
    static void onGosPackageStateChanged(@Nullable Bundle unused) {
        handleGosPackageStateChange(false, null);
    }

    private static void handleGosPackageStateChange(boolean initial, @Nullable Bundle appBindArgs) {
        final Context ctx = appContext;
        final GosPackageState state;
        synchronized (gosPackageStateChangeLock) {
            if (initial) {
                hadInitialGosPsChangeCallback = true;
                GosPackageState fromArgs = appBindArgs == null ? null :
                        appBindArgs.getParcelable(AppBindArgs.KEY_GOS_PACKAGE_STATE,
                                GosPackageState.class);
                // GosPackageState from AppBindArgs is outdated if a change arrived meanwhile
                state = (hasPendingGosPsChangeCallback || fromArgs == null)
                        ? getForSelf(ctx) : fromArgs;
                hasPendingGosPsChangeCallback = false;
            } else {
                if (ctx == null || !hadInitialGosPsChangeCallback) {
                    hasPendingGosPsChangeCallback = true;
                    return;
                }
                state = getForSelf(ctx);
            }
        }
        if (state == null) {
            return;
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, (initial ? "initial" : "updated") + " GosPackageState for "
                    + ctx.getPackageName() + ": " + state);
        }
        // Features that depend on GosPackageState are enabled here
        // (Storage Scopes is added in a later ValkyOS batch).
        com.android.internal.app.ContactScopes.maybeEnable(ctx, state);
    }

    @Nullable
    private static GosPackageState getForSelf(@Nullable Context ctx) {
        if (ctx == null) {
            return null;
        }
        try {
            return GosPackageState.getForSelf(ctx);
        } catch (RuntimeException e) {
            Log.e(TAG, "unable to obtain GosPackageState", e);
            return null;
        }
    }
}
