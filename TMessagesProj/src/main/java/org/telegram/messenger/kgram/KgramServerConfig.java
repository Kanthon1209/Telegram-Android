package org.telegram.messenger.kgram;

/**
 * kgram endpoints — traffic goes through the kgram gateway over HTTPS / WSS.
 * The APK must not contain an OpenIM admin secret or call admin APIs.
 *
 * Demo TLS is not live on the ECS until a cert is installed and port 8443
 * (or 443) is opened; see the gateway deploy README. Self-signed certs need
 * a public certificate in res/raw plus a trust-anchor in the network
 * security config.
 */
public final class KgramServerConfig {
    private KgramServerConfig() {}

    /**
     * When true: LoginActivity uses OpenIM Chat only; ConnectionsManager drops MTProto
     * requests and keeps TG DCs paused (no official Telegram network / API id flood).
     */
    public static final boolean USE_OPENIM_AUTH = true;

    public static final String GATEWAY_HOST = "8.136.150.149";
    public static final int GATEWAY_PORT = 8443;

    /** Gateway origin (HTTPS). */
    public static final String GATEWAY_API = "https://" + GATEWAY_HOST + ":" + GATEWAY_PORT;

    public static final String CHAT_API = GATEWAY_API + "/chat";
    public static final String IM_API = GATEWAY_API + "/im";
    public static final String IM_WS = "wss://" + GATEWAY_HOST + ":" + GATEWAY_PORT + "/v1/ws";
    public static final String MINIO = GATEWAY_API + "/objects";

    /**
     * Demo unified verification code is enabled: registration accepts
     * {@link #DEMO_SUPER_VERIFY_CODE} (666666). This is a kgram demo policy,
     * not Telegram official "Test Backend".
     */
    public static final boolean ALLOW_DEMO_SUPER_VERIFY = true;

    /**
     * Unified demo phone verification code, used only when
     * {@link #ALLOW_DEMO_SUPER_VERIFY} is true. Shown as a register-page hint;
     * never auto-injected if the field is left blank, and never used on login.
     */
    public static final String DEMO_SUPER_VERIFY_CODE = "666666";

    /**
     * Set true only when a real Chat /account/code/send (SMS or equivalent)
     * path is deployed. Default false — do not pretend SMS is available.
     */
    public static final boolean ALLOW_PRODUCTION_REGISTRATION = false;

    /** Android platform id in OpenIM Chat. */
    public static final int PLATFORM_ANDROID = 2;

    public static final String PREFS = "kgram_openim";

    /**
     * @deprecated Polling removed — realtime is OpenIM WebSocket only.
     */
    @Deprecated
    public static final int CHAT_POLL_MS = 0;
}
