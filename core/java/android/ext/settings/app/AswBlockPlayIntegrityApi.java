package android.ext.settings.app;

import android.content.pm.GosPackageState;
import android.content.pm.GosPackageStateFlag;

/** @hide */
public final class AswBlockPlayIntegrityApi {
    public static final AswBlockPlayIntegrityApi I = new AswBlockPlayIntegrityApi();

    private AswBlockPlayIntegrityApi() {}

    public boolean isNotificationEnabled(GosPackageState ps) {
        return !ps.hasFlag(GosPackageStateFlag.SUPPRESS_PLAY_INTEGRITY_API_NOTIF);
    }
}
