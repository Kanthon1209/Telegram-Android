package org.telegram.messenger.kgram;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.LaunchActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On an expired or invalid IM token, clear the local session and return to the
 * login screen. Idempotent so a burst of 1501/401 responses does not relaunch
 * login repeatedly.
 */
public final class KgramSessionExpiry {
    private static final AtomicBoolean handling = new AtomicBoolean(false);

    private KgramSessionExpiry() {}

    public static void resetAfterLogin() {
        handling.set(false);
    }

    public static void handleFromBackground(int errCode) {
        if (!KgramTokenPolicy.requiresReLogin(errCode)) {
            return;
        }
        try {
            AndroidUtilities.runOnUIThread(() -> forceReLogin(errCode));
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static void forceReLogin(int errCode) {
        if (!KgramTokenPolicy.requiresReLogin(errCode)) {
            return;
        }
        if (!handling.compareAndSet(false, true)) {
            return;
        }
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) {
            handling.set(false);
            return;
        }
        try {
            KgramSession.clear(ctx);
            KgramTgBridge.clearActivation();
            try {
                Toast.makeText(ctx, "登录已过期，请重新登录", Toast.LENGTH_LONG).show();
            } catch (Throwable ignore) {
            }
            Intent intent = new Intent(ctx, LaunchActivity.class);
            intent.putExtra("kgram_force_login", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(intent);
        } catch (Throwable t) {
            FileLog.e(t);
            handling.set(false);
        }
    }
}
