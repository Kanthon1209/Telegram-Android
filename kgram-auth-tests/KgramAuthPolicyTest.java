import org.telegram.messenger.kgram.KgramAuthPolicy;
import org.telegram.messenger.kgram.KgramServerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JVM unit + static regression for issue 4. No Android runtime required.
 * Run via kgram-auth-tests/run.sh
 */
public final class KgramAuthPolicyTest {
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path repoRoot = args.length > 0 ? Paths.get(args[0]) : Paths.get("..");
        testDefaultServerConfigIsSafe();
        testDefaultPolicyClosesRegistrationAndHidesDemoCode();
        testPhoneValidation();
        testPasswordLoginRouting();
        testDemoFlagAllowsOnlyMatchingCodeAndStillHidesItFromCopy();
        testProductionRegistrationUsesEnteredCodeNeverInjectsDemo();
        testStaticSources(repoRoot.toAbsolutePath().normalize());
        if (failures > 0) {
            System.err.println("FAILED: " + failures + " assertion(s)");
            System.exit(1);
        }
        System.out.println("OK: all KgramAuthPolicy checks passed");
    }

    private static void testDefaultServerConfigIsSafe() {
        assertFalse("demo super-verify must be off by default", KgramServerConfig.ALLOW_DEMO_SUPER_VERIFY);
        assertFalse("production registration must be off until SMS is deployed",
                KgramServerConfig.ALLOW_PRODUCTION_REGISTRATION);
    }

    private static void testDefaultPolicyClosesRegistrationAndHidesDemoCode() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertFalse("registration unavailable by default", p.isRegistrationAvailable());
        assertFalse("must not offer register from login", p.shouldOfferRegisterFromLogin());
        String subtitle = p.phonePageSubtitle();
        assertTrue("subtitle must mention password login", subtitle.contains("密码登录"));
        assertFalse("subtitle must not contain demo code digits", subtitle.contains("666666"));
        assertFalse("subtitle must not say 演示验证码", subtitle.contains("演示验证码"));
        assertFalse("title should not promise registration when closed", p.phonePageTitle().contains("注册"));
        assertEquals("account-not-found explains SMS gap",
                "该手机号还没有账号。当前未接入短信验证码，暂不能在此注册，请使用已有账号登录。",
                p.accountNotFoundMessage());
        assertNull("empty code rejected", p.resolveRegisterVerifyCode(""));
        assertNull("666666 rejected when demo is off", p.resolveRegisterVerifyCode("666666"));
        assertNull("any code rejected when registration closed", p.resolveRegisterVerifyCode("123456"));
        assertFalse("must not send hardcoded demo code on default policy", p.wouldSendVerifyCode("666666"));
    }

    private static void testPhoneValidation() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertTrue("+86 13800138000", p.isValidPhone("+86", "13800138000"));
        assertTrue("strips spaces", p.isValidPhone("86", "138 0013 8000"));
        assertFalse("short phone", p.isValidPhone("+86", "123"));
        assertFalse("missing area", p.isValidPhone("", "13800138000"));
        assertFalse("nulls", p.isValidPhone(null, null));
    }

    private static void testPasswordLoginRouting() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertTrue("v0.1.11: confirm phone goes to password login",
                p.shouldOpenPasswordLoginAfterPhoneConfirm());
        assertFalse("must not show login/register dialog", p.shouldShowLoginRegisterDialog());
    }

    private static void testDemoFlagAllowsOnlyMatchingCodeAndStillHidesItFromCopy() {
        KgramAuthPolicy p = new KgramAuthPolicy(new KgramAuthPolicy.Config(true, "666666", false));
        assertTrue("demo flag opens registration", p.isRegistrationAvailable());
        assertEquals("666666", p.resolveRegisterVerifyCode("666666"));
        assertNull("wrong demo code rejected", p.resolveRegisterVerifyCode("000000"));
        assertFalse("phone subtitle still must not display the code",
                p.phonePageSubtitle().contains("666666"));
        assertFalse(p.phonePageSubtitle().contains("演示验证码"));
        assertEquals("验证码不正确", p.invalidVerifyCodeMessage());
        assertFalse("error copy must not leak the demo code", p.invalidVerifyCodeMessage().contains("666666"));
    }

    private static void testProductionRegistrationUsesEnteredCodeNeverInjectsDemo() {
        KgramAuthPolicy p = new KgramAuthPolicy(new KgramAuthPolicy.Config(false, "666666", true));
        assertTrue(p.isRegistrationAvailable());
        assertEquals("user-entered SMS code is forwarded", "482913", p.resolveRegisterVerifyCode("482913"));
        assertNull("empty not injected with demo value", p.resolveRegisterVerifyCode(""));
        assertNull("whitespace only", p.resolveRegisterVerifyCode("   "));
        // Typing 666666 when demo is off is still just a user-entered string; the
        // policy must not auto-substitute it, and default production has demo off
        // so the app never pre-fills it. We do not special-case the digits here.
        assertEquals("666666", p.resolveRegisterVerifyCode("666666"));
    }

    private static void testStaticSources(Path root) throws IOException {
        String login = read(root, "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java");
        assertFalse("LoginActivity must not display SUPER/DEMO verify code",
                login.contains("SUPER_VERIFY_CODE") || login.contains("DEMO_SUPER_VERIFY_CODE") || login.contains("演示验证码固定"));
        assertFalse("LoginActivity must not use the degraded login/register dialog",
                login.contains("showOpenIMAuthDialog"));
        assertTrue("LoginActivity must open KgramLoginActivity after phone confirm",
                login.contains("KgramLoginActivity") && login.contains("shouldOpenPasswordLoginAfterPhoneConfirm"));

        String registerUi = read(root, "TMessagesProj/src/main/java/org/telegram/ui/kgram/KgramRegisterActivity.java");
        assertFalse("Register UI must not prefill the demo code",
                registerUi.contains("setText(KgramServerConfig.SUPER_VERIFY_CODE")
                        || registerUi.contains("setText(KgramServerConfig.DEMO_SUPER_VERIFY_CODE")
                        || registerUi.contains("演示环境已预填验证码"));
        assertTrue("Register UI must consult auth policy",
                registerUi.contains("KgramAuthPolicy") && registerUi.contains("resolveRegisterVerifyCode"));

        String client = read(root, "TMessagesProj/src/main/java/org/telegram/messenger/kgram/OpenIMChatClient.java");
        assertFalse("register() must not hardcode/auto-insert the demo super code",
                client.contains("body.put(\"verifyCode\", KgramServerConfig.SUPER_VERIFY_CODE")
                        || client.contains("body.put(\"verifyCode\", KgramServerConfig.DEMO_SUPER_VERIFY_CODE"));
        assertTrue("register() must take a caller-supplied verify code",
                client.contains("register(String areaCode, String phoneNumber, String password, String nickname, String verifyCode)"));
        assertFalse("friendlyMessage must not leak the demo code",
                client.contains("演示请用"));

        String serverCfg = read(root, "TMessagesProj/src/main/java/org/telegram/messenger/kgram/KgramServerConfig.java");
        assertTrue("ALLOW_DEMO_SUPER_VERIFY default false is in source",
                serverCfg.contains("ALLOW_DEMO_SUPER_VERIFY = false"));
        assertTrue("ALLOW_PRODUCTION_REGISTRATION default false is in source",
                serverCfg.contains("ALLOW_PRODUCTION_REGISTRATION = false"));
        assertFalse("old SUPER_VERIFY_CODE constant should be gone",
                serverCfg.contains("String SUPER_VERIFY_CODE") || serverCfg.contains(" SUPER_VERIFY_CODE"));
    }

    private static String read(Path root, String rel) throws IOException {
        Path p = root.resolve(rel);
        if (!Files.exists(p)) {
            fail("missing file " + p);
            return "";
        }
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static void assertTrue(boolean cond) {
        assertTrue("expected true", cond);
    }

    private static void assertFalse(boolean cond) {
        assertFalse("expected false", cond);
    }

    private static void assertEquals(Object expected, Object actual) {
        assertEquals("values differ", expected, actual);
    }

    private static void assertTrue(String msg, boolean cond) {
        if (!cond) {
            fail(msg);
        }
    }

    private static void assertFalse(String msg, boolean cond) {
        if (cond) {
            fail(msg);
        }
    }

    private static void assertEquals(String msg, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(msg + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void assertNull(String msg, Object actual) {
        if (actual != null) {
            fail(msg + " expected=null actual=[" + actual + "]");
        }
    }

    private static void fail(String msg) {
        failures++;
        System.err.println("FAIL: " + msg);
    }
}
