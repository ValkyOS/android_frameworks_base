/*
 * Copyright (C) 2026 The ValkyOS Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on GrapheneOS PackageManagerHooks.getExtraAppBindArgs().
 */
package com.android.server.ext;

import android.annotation.Nullable;
import android.app.AppBindArgs;
import android.content.pm.GosPackageState;
import android.content.pm.GosPackageStateFlag;
import android.content.pm.PackageManagerInternal;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.app.ContactScopes;
import com.android.server.pm.Computer;
import com.android.server.pm.GosPackageStatePmHooks;
import com.android.server.pm.pkg.PackageStateInternal;

/** @hide */
public final class PackageManagerHooks {
    private static final String TAG = "AppBindArgs";

    private PackageManagerHooks() {}

    /**
     * Contact Scopes: the real contacts permissions must not be granted while the app uses
     * Contact Scopes (the app sees them as granted through permission check spoofing).
     */
    public static boolean shouldBlockGrantRuntimePermission(
            PackageManagerInternal pm, String permName, String packageName, int userId)
    {
        if (ContactScopes.getSpoofablePermissionDflag(permName) != 0) {
            GosPackageState gosPs = pm.getGosPackageState(packageName, userId);
            if (gosPs.hasFlag(GosPackageStateFlag.CONTACT_SCOPES_ENABLED)) {
                String msg = "refusing to grant " + permName + " to " + packageName +
                        ": Contact Scopes is enabled";
                Slog.d("PermissionManager", msg);
                return true;
            }
        }

        return false;
    }

    /**
     * Per-app arguments passed to the app process in IApplicationThread.bindApplication().
     * Never throws: a failure here must not prevent the app from starting.
     */
    @Nullable
    public static Bundle getExtraAppBindArgs(PackageManagerInternal pm,
                                             String packageName, int appUid, int pid) {
        try {
            // Note that app UID differs from process UID for isolated processes, and that for
            // android:externalService processes the app UID and package name are the client's.
            final int appId = UserHandle.getAppId(appUid);
            final int userId = UserHandle.getUserId(appUid);

            Computer pmComputer = (Computer) pm.snapshot();
            PackageStateInternal pkgState = pmComputer.getPackageStateInternal(packageName);
            if (pkgState == null || pkgState.getAppId() != appId || pkgState.getPkg() == null) {
                return null;
            }

            GosPackageState unfilteredGosPs =
                    pkgState.getUserStateOrDefault(userId).getGosPackageState();
            // GosPackageState that is filtered for the target app
            GosPackageState gosPs = GosPackageStatePmHooks.getFiltered(pmComputer, pkgState,
                    unfilteredGosPs, appUid, pid, userId);

            var b = new Bundle();
            b.putParcelable(AppBindArgs.KEY_GOS_PACKAGE_STATE, gosPs);
            return b;
        } catch (RuntimeException e) {
            Slog.e(TAG, "unable to obtain AppBindArgs for " + packageName, e);
            return null;
        }
    }
}
