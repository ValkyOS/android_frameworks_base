/*
 * ValkyOS: minimal ContentProviderRedirector, API-compatible with GrapheneOS.
 *
 * In GrapheneOS this class redirects content provider authorities for Contact Scopes and the
 * Gservices flags redirect. ValkyOS does not have those features yet, so the redirector stays
 * disabled and behaves exactly like the GrapheneOS version when it is not enabled.
 */

package com.android.internal.app;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;

/** @hide */
public class ContentProviderRedirector {

    private static volatile boolean isEnabled;

    public static void enable() {
        isEnabled = true;
    }

    public static String translateContentProviderAuthority(String auth) {
        // no redirect sources are available yet
        return auth;
    }

    public static boolean shouldSkipRegisterContentObserver(Uri uri, boolean notifyForDescendants,
                                                            ContentObserver observer, int userId) {
        return false;
    }

    public static boolean shouldSkipUnregisterContentObserver(ContentObserver observer) {
        return false;
    }

    public static boolean shouldSkipNotifyChange(Uri uri, @Nullable ContentObserver observer,
                                                 @ContentResolver.NotifyFlags int flags) {
        return false;
    }
}
