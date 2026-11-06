/*
 * Copyright (C) 2026 The ValkyOS Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on GrapheneOS AppBindArgs.
 */
package android.app;

/**
 * Keys of the per-app arguments that system_server passes to an app process when
 * it is bound (IApplicationThread.bindApplication).
 *
 * @hide
 */
public interface AppBindArgs {
    String KEY_GOS_PACKAGE_STATE = "gosPs";
}
