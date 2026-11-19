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

    /** OpenIM Chat demo super verify code (verifyCode.superCode). */
    public static final String SUPER_VERIFY_CODE = "666666";

    /** Android platform id in OpenIM Chat. */
    public static final int PLATFORM_ANDROID = 2;

    public static final String PREFS = "kgram_openim";

    /**
     * @deprecated Polling removed — realtime is OpenIM WebSocket only.
     */
    @Deprecated
    public static final int CHAT_POLL_MS = 0;
}
