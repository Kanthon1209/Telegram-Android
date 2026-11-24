package org.telegram.messenger.kgram;

/**
 * Login / registration policy for kgram OpenIM Chat auth.
 *
 * Product rules (demo):
 * - Login is account + password only. No SMS/verify-code step, and no
 *   official Telegram "Test Backend" (test-DC) switch on the login path.
 * - Registration requires a phone number. The unified demo verify code is
 *   {@link KgramServerConfig#DEMO_SUPER_VERIFY_CODE} when the demo flag is on.
 *
 * This class has no Android UI dependency so it can be unit-tested on a
 * plain JVM.
 */
public final class KgramAuthPolicy {

    public static final class Config {
        public final boolean allowDemoSuperVerify;
        public final String demoSuperVerifyCode;
        public final boolean allowProductionRegistration;

        public Config(boolean allowDemoSuperVerify, String demoSuperVerifyCode,
                      boolean allowProductionRegistration) {
            this.allowDemoSuperVerify = allowDemoSuperVerify;
            this.demoSuperVerifyCode = demoSuperVerifyCode != null ? demoSuperVerifyCode : "";
            this.allowProductionRegistration = allowProductionRegistration;
        }
    }

    public static final class ParsedAccount {
        public final String areaCode;
        public final String phone;

        public ParsedAccount(String areaCode, String phone) {
            this.areaCode = areaCode;
            this.phone = phone;
        }

        public String display() {
            if (areaCode == null || areaCode.length() == 0) {
                return phone != null ? phone : "";
            }
            if (phone == null || phone.length() == 0) {
                return areaCode;
            }
            return areaCode + " " + phone;
        }
    }

    public static Config configFromServer() {
        return new Config(
                KgramServerConfig.ALLOW_DEMO_SUPER_VERIFY,
                KgramServerConfig.DEMO_SUPER_VERIFY_CODE,
                KgramServerConfig.ALLOW_PRODUCTION_REGISTRATION);
    }

    public static KgramAuthPolicy app() {
        return new KgramAuthPolicy(configFromServer());
    }

    private final Config config;

    public KgramAuthPolicy(Config config) {
        this.config = config != null ? config : configFromServer();
    }

    public boolean isDemoSuperVerifyEnabled() {
        return config.allowDemoSuperVerify && config.demoSuperVerifyCode.length() > 0;
    }

    public String demoSuperVerifyCode() {
        return config.demoSuperVerifyCode;
    }

    /** Real SMS /account/code/send is not wired; default remains closed. */
    public boolean isProductionRegistrationOpen() {
        return config.allowProductionRegistration;
    }

    public boolean isRegistrationAvailable() {
        return isProductionRegistrationOpen() || isDemoSuperVerifyEnabled();
    }

    /**
     * Official Telegram login checkbox labeled "Test Backend".
     * That control calls {@code ConnectionsManager.switchBackend()} and flips
     * native MTProto between production DCs and official test DCs
     * ({@code tapv3.stel.com}). It is not a kgram demo switch. kgram never
     * talks to official Telegram DCs, so the control must stay hidden.
     */
    public boolean shouldShowOfficialTestBackendSwitch() {
        return !KgramServerConfig.USE_OPENIM_AUTH;
    }

    /** Main unauthenticated entry skips the official phone/SMS LoginActivity UI. */
    public boolean shouldSkipOfficialPhoneLogin() {
        return KgramServerConfig.USE_OPENIM_AUTH;
    }

    public boolean loginShowsAccountField() {
        return true;
    }

    public boolean loginShowsPasswordField() {
        return true;
    }

    /** Login does not use a dedicated country-code + phone picker. */
    public boolean loginShowsPhonePicker() {
        return false;
    }

    public boolean loginShowsVerifyCode() {
        return false;
    }

    public boolean loginShowsTestBackend() {
        return false;
    }

    public boolean registerRequiresPhone() {
        return true;
    }

    public boolean registerShowsVerifyCode() {
        return isRegistrationAvailable();
    }

    public String defaultAreaCode() {
        return "+86";
    }

    public String loginPageTitle() {
        return "登录";
    }

    public String loginPageSubtitle() {
        if (isRegistrationAvailable()) {
            return "请输入账号和密码。没有账号可去注册。";
        }
        return "请输入账号和密码登录。";
    }

    public String loginAccountHint() {
        return "请输入账号（手机号）";
    }

    public String loginPasswordHint() {
        return "请输入密码";
    }

    public String registerPageTitle() {
        return "注册";
    }

    public String registerPhoneHint() {
        return "请输入手机号";
    }

    public String registerVerifyCodeHint() {
        if (isDemoSuperVerifyEnabled()) {
            return "演示环境统一验证码为 " + config.demoSuperVerifyCode;
        }
        if (isProductionRegistrationOpen()) {
            return "请使用已发送到该手机号的验证码。";
        }
        return registrationUnavailableMessage();
    }

    /** Official phone page is no longer the main login path. */
    public String phonePageTitle() {
        return loginPageTitle();
    }

    /** Official phone page is no longer the main login path. */
    public String phonePageSubtitle() {
        return loginPageSubtitle();
    }

    /**
     * Fallback only: if the official phone form is still confirmed, open the
     * account-password login page (prefilling the number). Main path skips
     * that form entirely.
     */
    public boolean shouldOpenPasswordLoginAfterPhoneConfirm() {
        return true;
    }

    public boolean shouldShowLoginRegisterDialog() {
        return false;
    }

    public boolean isValidPhone(String areaCode, String phoneNumber) {
        String area = normalizeAreaCode(areaCode);
        String phone = normalizePhone(phoneNumber);
        return area.length() >= 2 && phone.length() >= 5;
    }

    public boolean isValidLoginAccount(String raw) {
        return parseLoginAccount(raw) != null;
    }

    /**
     * Parse a login "账号" into area code + national number for Chat
     * {@code /account/login}. Accepts {@code 13800138000}, {@code +86 13800138000},
     * {@code 8613800138000}, and similar forms. Default area is +86.
     */
    public ParsedAccount parseLoginAccount(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("[\\s\\-()]", "");
        if (s.length() == 0) {
            return null;
        }
        boolean hasIntlPrefix = s.startsWith("+") || s.startsWith("00");
        if (s.startsWith("00")) {
            s = s.substring(2);
        } else if (s.startsWith("+")) {
            s = s.substring(1);
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.length() < 5) {
            return null;
        }

        if (digits.length() == 11 && digits.charAt(0) == '1') {
            return validAccount("+86", digits);
        }
        if (digits.length() == 13 && digits.startsWith("86") && digits.charAt(2) == '1') {
            return validAccount("+86", digits.substring(2));
        }
        if (hasIntlPrefix) {
            if (digits.startsWith("86") && digits.length() >= 7) {
                return validAccount("+86", digits.substring(2));
            }
            if (digits.startsWith("1") && digits.length() == 11) {
                return validAccount("+1", digits.substring(1));
            }
            if (digits.length() >= 7 && digits.length() <= 15) {
                return validAccount("+" + digits.substring(0, 2), digits.substring(2));
            }
            if (digits.length() == 6) {
                return validAccount("+" + digits.substring(0, 1), digits.substring(1));
            }
        }
        if (digits.length() >= 5 && digits.length() <= 12) {
            return validAccount(defaultAreaCode(), digits);
        }
        return null;
    }

    private ParsedAccount validAccount(String area, String phone) {
        if (!isValidPhone(area, phone)) {
            return null;
        }
        return new ParsedAccount(normalizeAreaCode(area), normalizePhone(phone));
    }

    static String normalizeAreaCode(String areaCode) {
        if (areaCode == null) {
            return "";
        }
        String digits = areaCode.replaceAll("[^0-9]", "");
        return digits.length() == 0 ? "" : "+" + digits;
    }

    static String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    public String registrationUnavailableMessage() {
        return "当前未接入短信验证码，无法在此注册新账号。请使用已有账号和密码登录。";
    }

    public String invalidVerifyCodeMessage() {
        return "验证码不正确";
    }

    public String invalidLoginAccountMessage() {
        return "请输入有效账号（手机号）";
    }

    public String accountNotFoundMessage() {
        if (!isRegistrationAvailable()) {
            return "该账号还没有注册。当前未接入短信验证码，暂不能在此注册，请使用已有账号登录。";
        }
        return "该账号还没有注册，可以去注册。";
    }

    public boolean shouldOfferRegisterFromLogin() {
        return isRegistrationAvailable();
    }

    /**
     * Returns the verify code to send to Chat /account/register, or null
     * if registration must not proceed. When demo super-verify is on, only
     * the unified demo code is accepted. Production (only when explicitly
     * opened) forwards the user-entered code and never substitutes the demo
     * value for a blank field.
     */
    public String resolveRegisterVerifyCode(String userEntered) {
        if (!isRegistrationAvailable()) {
            return null;
        }
        String code = userEntered != null ? userEntered.trim() : "";
        if (code.length() == 0) {
            return null;
        }
        if (isDemoSuperVerifyEnabled()) {
            return config.demoSuperVerifyCode.equals(code) ? code : null;
        }
        return code;
    }

    /** True if this policy would ever put the given code into a register request. */
    public boolean wouldSendVerifyCode(String code) {
        String resolved = resolveRegisterVerifyCode(code);
        return resolved != null && resolved.equals(code);
    }
}
