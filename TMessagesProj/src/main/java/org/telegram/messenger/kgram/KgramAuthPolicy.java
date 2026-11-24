package org.telegram.messenger.kgram;

/**
 * Login / registration policy for kgram OpenIM Chat auth.
 * Production defaults: password login stays available; hardcoded demo
 * super-codes are off; registration is closed until a real verify-code
 * (SMS or equivalent) path is deployed. This class has no Android UI
 * dependency so it can be unit-tested on a plain JVM.
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

    /** Real SMS /account/code/send is not wired; default remains closed. */
    public boolean isProductionRegistrationOpen() {
        return config.allowProductionRegistration;
    }

    public boolean isRegistrationAvailable() {
        return isProductionRegistrationOpen() || isDemoSuperVerifyEnabled();
    }

    public String phonePageTitle() {
        return isRegistrationAvailable() ? "手机号登录 / 注册" : "手机号登录";
    }

    /**
     * v0.1.11 subtitle: after confirming the number, go to the password page.
     * Never includes a demo verification code.
     */
    public String phonePageSubtitle() {
        if (!isRegistrationAvailable()) {
            return "确认手机号后输入密码登录。当前未接入短信验证码，暂不能在此注册新账号。";
        }
        return "确认手机号后输入密码登录；没有账号可在下一步注册。";
    }

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

    public String accountNotFoundMessage() {
        if (!isRegistrationAvailable()) {
            return "该手机号还没有账号。当前未接入短信验证码，暂不能在此注册，请使用已有账号登录。";
        }
        return "该手机号还没有账号。";
    }

    public boolean shouldOfferRegisterFromLogin() {
        return isRegistrationAvailable();
    }

    /**
     * Returns the verify code to send to Chat /account/register, or null
     * if registration must not proceed. Production never injects a
     * hardcoded super code. Demo acceptance requires the explicit flag.
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
        // Production path (only when explicitly opened): use the user-entered
        // code from a real send-code flow. Do not substitute the demo value.
        return code;
    }

    /** True if this policy would ever put the given code into a register request. */
    public boolean wouldSendVerifyCode(String code) {
        String resolved = resolveRegisterVerifyCode(code);
        return resolved != null && resolved.equals(code);
    }
}
