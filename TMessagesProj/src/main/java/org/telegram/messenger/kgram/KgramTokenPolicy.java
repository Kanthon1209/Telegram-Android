package org.telegram.messenger.kgram;

/**
 * Decides when an OpenIM / kgram-gateway auth error means the stored IM token
 * is no longer usable. Silent refresh is not available: the session does not
 * store a password or refresh token, and admin {@code get_user_token} must not
 * be called from the client.
 */
public final class KgramTokenPolicy {
    private KgramTokenPolicy() {}

    /** Gateway {@code requireUser} maps parse_token failures to this code. */
    public static final int ERR_GATEWAY_UNAUTHORIZED = 401;

    // Official OpenIM token error family.
    public static final int ERR_TOKEN_EXPIRED = 1501;
    public static final int ERR_TOKEN_INVALID = 1502;
    public static final int ERR_TOKEN_MALFORMED = 1503;
    public static final int ERR_TOKEN_NOT_VALID_YET = 1504;
    public static final int ERR_TOKEN_UNKNOWN = 1505;
    public static final int ERR_TOKEN_KICKED = 1506;
    public static final int ERR_TOKEN_NOT_EXIST = 1507;

    public static boolean requiresReLogin(int errCode) {
        if (errCode == ERR_GATEWAY_UNAUTHORIZED) {
            return true;
        }
        return errCode >= ERR_TOKEN_EXPIRED && errCode <= ERR_TOKEN_NOT_EXIST;
    }

    /** No stored password or refresh token; do not invent a silent re-login. */
    public static boolean canSilentRefresh() {
        return false;
    }
}
