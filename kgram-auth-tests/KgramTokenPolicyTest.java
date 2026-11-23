import java.util.Objects;

import org.telegram.messenger.kgram.KgramTokenPolicy;

/**
 * Targeted checks for issue 5: expired/invalid IM tokens must force a
 * re-login, and must not be silently refreshed (no stored password / refresh
 * token, no client-side admin get_user_token).
 */
public final class KgramTokenPolicyTest {
    public static void main(String[] args) {
        testExpiredAndInvalidCodesRequireReLogin();
        testNonAuthErrorsDoNotRequireReLogin();
        testSilentRefreshUnavailable();
        System.out.println("OK: all KgramTokenPolicy checks passed");
    }

    private static void testExpiredAndInvalidCodesRequireReLogin() {
        int[] codes = {401, 1501, 1502, 1503, 1504, 1505, 1506, 1507};
        for (int code : codes) {
            assertTrue("errCode " + code + " must require re-login",
                    KgramTokenPolicy.requiresReLogin(code));
        }
    }

    private static void testNonAuthErrorsDoNotRequireReLogin() {
        int[] codes = {0, -1, 1001, 1303, 403, 404, 500, 1499, 1508};
        for (int code : codes) {
            assertFalse("errCode " + code + " must not be treated as token expiry",
                    KgramTokenPolicy.requiresReLogin(code));
        }
    }

    private static void testSilentRefreshUnavailable() {
        assertFalse("do not silently refresh without a stored credential",
                KgramTokenPolicy.canSilentRefresh());
    }

    private static void assertTrue(String msg, boolean v) {
        if (!v) {
            throw new AssertionError(msg);
        }
    }

    private static void assertFalse(String msg, boolean v) {
        if (v) {
            throw new AssertionError(msg);
        }
    }
}
