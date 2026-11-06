/*
 * Copyright (C) 2026 The ValkyOS Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on the DVY Project automatic security reboot.
 */
package com.android.server;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

/**
 * Reboots the device after eight continuous hours really locked (screen off alone
 * does not start the timer). After a reboot it waits for a successful unlock in
 * that boot before counting again, so it never loops.
 */
public final class ValkyAutoRebootService extends SystemService {
    private static final String TAG = "ValkyAutoReboot";
    private static final String SETTING_ENABLED = "valkyos_auto_reboot_enabled";
    private static final String LAST_UNLOCK_BOOT_COUNT =
            "valkyos_auto_reboot_last_unlock_boot_count";
    private static final long REBOOT_DELAY_MILLIS = 8L * 60L * 60L * 1000L;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AlarmManager.OnAlarmListener mAlarmListener = this::handleAlarm;

    private AlarmManager mAlarmManager;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private int mBootCount;
    private boolean mArmed;
    private boolean mInitialized;

    public ValkyAutoRebootService(android.content.Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        // No Binder is published: everything stays inside system_server.
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED && !mInitialized) {
            initialize();
        }
    }

    private void initialize() {
        mInitialized = true;
        mAlarmManager = getContext().getSystemService(AlarmManager.class);
        mKeyguardManager = getContext().getSystemService(KeyguardManager.class);
        mPowerManager = getContext().getSystemService(PowerManager.class);
        if (mAlarmManager == null || mKeyguardManager == null || mPowerManager == null) {
            Slog.e(TAG, "Required services are not available");
            return;
        }

        mBootCount = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.BOOT_COUNT, 0);
        mArmed = Settings.Global.getInt(getContext().getContentResolver(),
                LAST_UNLOCK_BOOT_COUNT, -1) == mBootCount;

        getContext().getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(SETTING_ENABLED), false,
                new ContentObserver(mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateForCurrentState();
                    }
                }, UserHandle.USER_ALL);

        mKeyguardManager.addKeyguardLockedStateListener(
                command -> mHandler.post(command), this::onKeyguardStateChanged);
        updateForCurrentState();
    }

    private boolean isEnabled() {
        return Settings.Global.getInt(getContext().getContentResolver(),
                SETTING_ENABLED, 1) != 0;
    }

    private void updateForCurrentState() {
        if (!isEnabled()) {
            cancelAlarm();
            return;
        }
        onKeyguardStateChanged(mKeyguardManager.isKeyguardLocked());
    }

    private void onKeyguardStateChanged(boolean locked) {
        if (!locked) {
            mArmed = true;
            Settings.Global.putInt(getContext().getContentResolver(),
                    LAST_UNLOCK_BOOT_COUNT, mBootCount);
            cancelAlarm();
            return;
        }
        if (isEnabled() && mArmed) {
            scheduleAlarm();
        }
    }

    private void scheduleAlarm() {
        cancelAlarm();
        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + REBOOT_DELAY_MILLIS,
                TAG, command -> mHandler.post(command), null, mAlarmListener);
        Slog.i(TAG, "Reboot scheduled after eight hours locked");
    }

    private void cancelAlarm() {
        if (mAlarmManager != null) {
            mAlarmManager.cancel(mAlarmListener);
        }
    }

    private void handleAlarm() {
        if (!isEnabled() || !mArmed || !mKeyguardManager.isKeyguardLocked()) {
            updateForCurrentState();
            return;
        }

        // Disarm first: if nobody unlocks after the reboot, it will not reboot again.
        mArmed = false;
        Settings.Global.putInt(getContext().getContentResolver(),
                LAST_UNLOCK_BOOT_COUNT, -1);
        Slog.i(TAG, "Security reboot after eight hours locked");
        mPowerManager.reboot("valkyos_auto_reboot");
    }
}
