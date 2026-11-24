import org.telegram.messenger.kgram.KgramAuthPolicy;
import org.telegram.messenger.kgram.KgramServerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JVM unit + static regression for account-password login, register-with-phone,
 * demo verify code 666666, and hiding official Telegram Test Backend.
 * Run via kgram-auth-tests/run.sh
 */
public final class KgramAuthPolicyTest {
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path repoRoot = args.length > 0 ? Paths.get(args[0]) : Paths.get("..");
        testDefaultServerConfigEnablesDemoVerify();
        testDefaultLoginIsAccountAndPasswordOnly();
        testDefaultRegistrationRequiresPhoneAndAccepts666666();
        testPhoneValidation();
        testParseLoginAccount();
        testOfficialTestBackendIsHidden();
        testDemoOffStillRejectsSuperCodeUnlessProductionOpen();
        testProductionRegistrationUsesEnteredCodeNeverInjectsDemo();
        testProductionOpenDoesNotSubstituteDemoWhenCodeMissing();
        testStaticSources(repoRoot.toAbsolutePath().normalize());
        if (failures > 0) {
            System.err.println("FAILED: " + failures + " assertion(s)");
            System.exit(1);
        }
        System.out.println("OK: all KgramAuthPolicy checks passed");
    }

    private static void testDefaultServerConfigEnablesDemoVerify() {
        assertTrue("demo unified verify code is on", KgramServerConfig.ALLOW_DEMO_SUPER_VERIFY);
        assertEquals("unified demo code is 666666", "666666", KgramServerConfig.DEMO_SUPER_VERIFY_CODE);
        assertFalse("production SMS registration stays closed",
                KgramServerConfig.ALLOW_PRODUCTION_REGISTRATION);
        assertTrue("OpenIM auth is on (no official Telegram DC)", KgramServerConfig.USE_OPENIM_AUTH);
    }

    private static void testDefaultLoginIsAccountAndPasswordOnly() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertTrue(p.loginShowsAccountField());
        assertTrue(p.loginShowsPasswordField());
        assertFalse("login must not use a phone picker", p.loginShowsPhonePicker());
        assertFalse("login must not ask for a verify code", p.loginShowsVerifyCode());
        assertFalse("login must not show Test Backend", p.loginShowsTestBackend());
        assertTrue("official phone/SMS page is skipped", p.shouldSkipOfficialPhoneLogin());
        assertFalse("must not show login/register dialog", p.shouldShowLoginRegisterDialog());
        String subtitle = p.loginPageSubtitle();
        assertTrue("subtitle mentions 账号 and 密码", subtitle.contains("账号") && subtitle.contains("密码"));
        assertFalse("login subtitle must not require 验证码", subtitle.contains("验证码"));
        assertFalse("login subtitle must not mention Test Backend",
                subtitle.toLowerCase().contains("test backend") || subtitle.contains("测试后端"));
        assertTrue("register is offered from login in demo", p.shouldOfferRegisterFromLogin());
        assertEquals("该账号还没有注册，可以去注册。", p.accountNotFoundMessage());
    }

    private static void testDefaultRegistrationRequiresPhoneAndAccepts666666() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertTrue("registration available via demo verify", p.isRegistrationAvailable());
        assertTrue("register requires a phone number", p.registerRequiresPhone());
        assertTrue(p.registerShowsVerifyCode());
        assertTrue("demo super-verify enabled", p.isDemoSuperVerifyEnabled());
        assertEquals("666666", p.resolveRegisterVerifyCode("666666"));
        assertEquals("666666", p.resolveRegisterVerifyCode(" 666666 "));
        assertNull("wrong code rejected", p.resolveRegisterVerifyCode("000000"));
        assertNull("empty code not auto-injected", p.resolveRegisterVerifyCode(""));
        assertNull("null code not auto-injected", p.resolveRegisterVerifyCode(null));
        assertTrue(p.wouldSendVerifyCode("666666"));
        assertFalse(p.wouldSendVerifyCode("000000"));
        assertTrue("register hint tells the user the unified demo code",
                p.registerVerifyCodeHint().contains("666666"));
        assertEquals("验证码不正确", p.invalidVerifyCodeMessage());
    }

    private static void testPhoneValidation() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertTrue("+86 13800138000", p.isValidPhone("+86", "13800138000"));
        assertTrue("strips spaces", p.isValidPhone("86", "138 0013 8000"));
        assertFalse("short phone", p.isValidPhone("+86", "123"));
        assertFalse("missing area", p.isValidPhone("", "13800138000"));
        assertFalse("nulls", p.isValidPhone(null, null));
    }

    private static void testParseLoginAccount() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        KgramAuthPolicy.ParsedAccount a = p.parseLoginAccount("13800138000");
        assertTrue("11-digit CN mobile", a != null);
        assertEquals("+86", a.areaCode);
        assertEquals("13800138000", a.phone);

        a = p.parseLoginAccount("+86 138 0013 8000");
        assertTrue("plus 86 with spaces", a != null);
        assertEquals("+86", a.areaCode);
        assertEquals("13800138000", a.phone);

        a = p.parseLoginAccount("8613800138000");
        assertTrue("86 prefix without plus", a != null);
        assertEquals("+86", a.areaCode);
        assertEquals("13800138000", a.phone);

        a = p.parseLoginAccount("008613800138000");
        assertTrue("00 international prefix", a != null);
        assertEquals("+86", a.areaCode);
        assertEquals("13800138000", a.phone);

        assertTrue(p.isValidLoginAccount("13800138000"));
        assertFalse("empty account", p.isValidLoginAccount(""));
        assertFalse("null account", p.isValidLoginAccount(null));
        assertFalse("too short", p.isValidLoginAccount("123"));
        assertEquals("请输入有效账号（手机号）", p.invalidLoginAccountMessage());
    }

    private static void testOfficialTestBackendIsHidden() {
        KgramAuthPolicy p = KgramAuthPolicy.app();
        assertFalse("OpenIM builds must not show official Test Backend",
                p.shouldShowOfficialTestBackendSwitch());
        assertFalse(p.loginShowsTestBackend());
    }

    private static void testDemoOffStillRejectsSuperCodeUnlessProductionOpen() {
        KgramAuthPolicy p = new KgramAuthPolicy(new KgramAuthPolicy.Config(false, "666666", false));
        assertFalse(p.isRegistrationAvailable());
        assertNull("666666 rejected when demo is off and production closed",
                p.resolveRegisterVerifyCode("666666"));
        assertFalse(p.wouldSendVerifyCode("666666"));
        assertFalse(p.registerShowsVerifyCode());
        assertFalse(p.shouldOfferRegisterFromLogin());
        assertFalse("hint must not advertise the code when registration is closed",
                p.registerVerifyCodeHint().contains("666666"));
    }

    private static void testProductionRegistrationUsesEnteredCodeNeverInjectsDemo() {
        KgramAuthPolicy p = new KgramAuthPolicy(new KgramAuthPolicy.Config(false, "666666", true));
        assertTrue(p.isRegistrationAvailable());
        assertEquals("user-entered SMS code is forwarded", "482913", p.resolveRegisterVerifyCode("482913"));
        assertNull("empty not injected with demo value", p.resolveRegisterVerifyCode(""));
        assertNull("whitespace only", p.resolveRegisterVerifyCode("   "));
        // Typing 666666 when demo is off is still just a user-entered string.
        assertEquals("666666", p.resolveRegisterVerifyCode("666666"));
        assertFalse("production hint must not print the demo code",
                p.registerVerifyCodeHint().contains("666666"));
    }

    private static void testProductionOpenDoesNotSubstituteDemoWhenCodeMissing() {
        KgramAuthPolicy p = new KgramAuthPolicy(new KgramAuthPolicy.Config(false, "666666", true));
        assertTrue(p.isProductionRegistrationOpen());
        assertFalse(p.isDemoSuperVerifyEnabled());
        assertNull("null must not become demo default", p.resolveRegisterVerifyCode(null));
        assertNull("empty must not become demo default", p.resolveRegisterVerifyCode(""));
        assertNull("whitespace must not become demo default", p.resolveRegisterVerifyCode("   "));
        String resolvedEmpty = p.resolveRegisterVerifyCode("");
        assertFalse("empty must not equal demo code", "666666".equals(resolvedEmpty));
    }

    private static void testStaticSources(Path root) throws IOException {
        String login = read(root, "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java");
        assertTrue("LoginActivity must skip official phone login for kgram",
                login.contains("shouldOpenKgramAccountPasswordLogin")
                        && login.contains("openKgramAccountPasswordLogin"));
        assertTrue("LoginActivity must hide Test Backend when OpenIM is on",
                login.contains("!KgramServerConfig.USE_OPENIM_AUTH")
                        && login.contains("allowTestBackend"));
        assertFalse("LoginActivity must not display SUPER/DEMO verify code",
                login.contains("SUPER_VERIFY_CODE") || login.contains("DEMO_SUPER_VERIFY_CODE")
                        || login.contains("演示验证码固定"));
        assertFalse("LoginActivity must not use the degraded login/register dialog",
                login.contains("showOpenIMAuthDialog"));

        String loginUi = read(root, "TMessagesProj/src/main/java/org/telegram/ui/kgram/KgramLoginActivity.java");
        assertTrue("login page labels 账号", loginUi.contains("\"账号\""));
        assertTrue("login page labels 密码", loginUi.contains("\"密码\""));
        assertTrue("login parses the account field", loginUi.contains("parseLoginAccount"));
        assertFalse("login page must not show Test Backend",
                loginUi.contains("Test Backend") || loginUi.contains("DebugTestBackend")
                        || loginUi.contains("testBackendCheckBox") || loginUi.contains("switchBackend"));
        assertFalse("login page must not collect a verify code",
                loginUi.contains("codeField") || loginUi.contains("验证码")
                        || loginUi.contains("resolveRegisterVerifyCode"));
        assertFalse("login must stay open without phone extras",
                loginUi.contains("if (TextUtils.isEmpty(areaCode) || TextUtils.isEmpty(phone))")
                        && loginUi.contains("finish();"));

        String registerUi = read(root, "TMessagesProj/src/main/java/org/telegram/ui/kgram/KgramRegisterActivity.java");
        assertTrue("register UI must require a phone field",
                registerUi.contains("\"手机号\"") && registerUi.contains("phoneField")
                        && registerUi.contains("isValidPhone"));
        assertTrue("register UI must consult auth policy for the verify code",
                registerUi.contains("KgramAuthPolicy") && registerUi.contains("resolveRegisterVerifyCode"));
        assertTrue("register hint comes from policy (demo code 666666)",
                registerUi.contains("registerVerifyCodeHint"));
        assertFalse("Register UI must not prefill the demo code",
                registerUi.contains("setText(KgramServerConfig.SUPER_VERIFY_CODE")
                        || registerUi.contains("setText(KgramServerConfig.DEMO_SUPER_VERIFY_CODE")
                        || registerUi.contains("演示环境已预填验证码"));
        assertFalse("register must stay open without phone extras",
                registerUi.contains("if (TextUtils.isEmpty(areaCode) || TextUtils.isEmpty(phone))")
                        && registerUi.contains("finish();"));

        String client = read(root, "TMessagesProj/src/main/java/org/telegram/messenger/kgram/OpenIMChatClient.java");
        assertFalse("register() must not hardcode/auto-insert the demo super code",
                client.contains("body.put(\"verifyCode\", KgramServerConfig.SUPER_VERIFY_CODE")
                        || client.contains("body.put(\"verifyCode\", KgramServerConfig.DEMO_SUPER_VERIFY_CODE"));
        assertTrue("register() must take a caller-supplied verify code",
                client.contains("register(String areaCode, String phoneNumber, String password, String nickname, String verifyCode)"));
        assertFalse("friendlyMessage must not leak the demo code",
                client.contains("演示请用"));

        String serverCfg = read(root, "TMessagesProj/src/main/java/org/telegram/messenger/kgram/KgramServerConfig.java");
        assertTrue("ALLOW_DEMO_SUPER_VERIFY default true is in source",
                serverCfg.contains("ALLOW_DEMO_SUPER_VERIFY = true"));
        assertTrue("ALLOW_PRODUCTION_REGISTRATION default false is in source",
                serverCfg.contains("ALLOW_PRODUCTION_REGISTRATION = false"));
        assertTrue("unified demo code constant remains 666666",
                serverCfg.contains("DEMO_SUPER_VERIFY_CODE = \"666666\""));
        assertFalse("old SUPER_VERIFY_CODE constant should be gone",
                serverCfg.contains("String SUPER_VERIFY_CODE") || serverCfg.contains(" SUPER_VERIFY_CODE"));

        String connections = read(root, "TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java");
        int switchIdx = connections.indexOf("public void switchBackend(boolean restart)");
        assertTrue("switchBackend method exists", switchIdx >= 0);
        String switchBody = connections.substring(switchIdx, switchIdx + 600);
        assertTrue("switchBackend must no-op when kgram blocks official DCs",
                switchBody.contains("kgramBlocksTelegramDc()") && switchBody.contains("return;"));

        String profile = read(root, "TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java");
        assertTrue("profile debug Test Backend row hidden for OpenIM",
                profile.contains("switchBackendRow = rowCount++")
                        && profile.contains("!org.telegram.messenger.kgram.KgramServerConfig.USE_OPENIM_AUTH"));
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
