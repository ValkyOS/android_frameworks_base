/*
 * Copyright (C) 2022 GrapheneOS
 * Copyright (C) 2026 The ValkyOS Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on GrapheneOS SpecialRuntimePermUtils. ValkyOS ports only the OTHER_SENSORS
 * ("Sensors") special runtime permission; the INTERNET ("Network") one is not ported.
 */

package com.android.server.pm.permission;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.EmptyArray;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;

public class SpecialRuntimePermUtils {
    private static final String TAG = SpecialRuntimePermUtils.class.getSimpleName();

    // Settings.Secure, per user. 1 (default) = new apps get the Sensors permission at install,
    // like on GrapheneOS. Same key name as GrapheneOS.
    public static final String AUTO_GRANT_OTHER_SENSORS_PERMISSION = "auto_grant_OTHER_SENSORS_perm";

    private static final ArraySet<String> specialRuntimePermissions = new ArraySet<>(new String[] {
            Manifest.permission.OTHER_SENSORS,
    });

    public static boolean isSpecialRuntimePermission(String permission) {
        return specialRuntimePermissions.contains(permission);
    }

    public static String[] getAll() {
        return specialRuntimePermissions.toArray(EmptyArray.STRING);
    }

    public static boolean shouldAutoGrant(Context ctx, String packageName, int userId, String perm) {
        if (!isSpecialRuntimePermission(perm)) {
            return false;
        }

        if (Manifest.permission.OTHER_SENSORS.equals(perm)) {
            if (ActivityManager.getService() == null) {
                // a failsafe: should never happen
                Slog.d(TAG, "AMS is null");
                if (Build.isDebuggable()) {
                    throw new IllegalStateException();
                }
                return false;
            }

            var um = LocalServices.getService(UserManagerInternal.class);
            // use parent profile settings for work profile
            int userIdForSettings = um.getProfileParentId(userId);

            return Settings.Secure.getIntForUser(ctx.getContentResolver(),
                    AUTO_GRANT_OTHER_SENSORS_PERMISSION, 1, userIdForSettings) != 0;
        }

        return true;
    }

    private SpecialRuntimePermUtils() {}
}
