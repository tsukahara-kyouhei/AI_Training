package jp.co.skig.officeorder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * `app.*` 配下の独自設定を束ねる型付き設定クラス。
 *
 * <p>アプリ固有の設定値を {@code @Value} で点在させず、
 * ここへ集約して型安全に参照できるようにする。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** アプリ標準のタイムゾーン。 */
    private String timeZone = "Asia/Tokyo";
    /** セキュリティ関連設定。 */
    private final Security security = new Security();
    /** Cookie関連設定。 */
    private final Cookie cookie = new Cookie();
    /** バッチ関連設定。 */
    private final Batch batch = new Batch();
    /** メール関連設定。 */
    private final Mail mail = new Mail();

    /**
     * アプリ標準のタイムゾーンを返す。
     *
     * @return タイムゾーンID
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * アプリ標準のタイムゾーンを設定する。
     *
     * @param timeZone タイムゾーンID
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * セキュリティ関連設定を返す。
     *
     * @return セキュリティ関連設定
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * Cookie関連設定を返す。
     *
     * @return Cookie関連設定
     */
    public Cookie getCookie() {
        return cookie;
    }

    /**
     * バッチ関連設定を返す。
     *
     * @return バッチ関連設定
     */
    public Batch getBatch() {
        return batch;
    }

    /**
     * メール関連設定を返す。
     *
     * @return メール関連設定
     */
    public Mail getMail() {
        return mail;
    }

    /**
     * セキュリティ関連の独自設定。
     */
    public static class Security {

        /** remember-me Cookie署名キー。 */
        private String rememberMeKey = "office-order-remember-me-key-v1";

        /**
         * remember-me Cookie署名キーを返す。
         *
         * @return remember-me署名キー
         */
        public String getRememberMeKey() {
            return rememberMeKey;
        }

        /**
         * remember-me Cookie署名キーを設定する。
         *
         * @param rememberMeKey remember-me署名キー
         */
        public void setRememberMeKey(String rememberMeKey) {
            this.rememberMeKey = rememberMeKey;
        }
    }

    /**
     * Cookie関連の独自設定。
     */
    public static class Cookie {

        /** カートCookie関連設定。 */
        private final Cart cart = new Cart();

        /**
         * カートCookie関連設定を返す。
         *
         * @return カートCookie関連設定
         */
        public Cart getCart() {
            return cart;
        }

        /**
         * カートCookie関連の設定。
         */
        public static class Cart {

            /** Secure属性の付与方針。 */
            private String secureMode = "auto";

            /**
             * Secure属性の付与方針を返す。
             *
             * @return Secure属性の付与方針
             */
            public String getSecureMode() {
                return secureMode;
            }

            /**
             * Secure属性の付与方針を設定する。
             *
             * @param secureMode Secure属性の付与方針
             */
            public void setSecureMode(String secureMode) {
                this.secureMode = secureMode;
            }
        }
    }

    /**
     * バッチ関連の独自設定。
     */
    public static class Batch {

        /** 毎時実行の cron 式。 */
        private String hourlyCron = "0 0 * * * *";

        /**
         * 毎時実行の cron 式を返す。
         *
         * @return cron式
         */
        public String getHourlyCron() {
            return hourlyCron;
        }

        /**
         * 毎時実行の cron 式を設定する。
         *
         * @param hourlyCron cron式
         */
        public void setHourlyCron(String hourlyCron) {
            this.hourlyCron = hourlyCron;
        }
    }

    /**
     * メール関連の独自設定。
     */
    public static class Mail {

        /** 送信元メールアドレス。 */
        private String from = "no-reply@office-order.local";
        /** 返信先メールアドレス。 */
        private String replyTo = "support@office-order.local";
        /** サイトURL。 */
        private String siteUrl = "http://localhost:8080/";
        /** 問い合わせ先メールアドレス。 */
        private String contactEmail = "support@office-order.local";

        /**
         * 送信元メールアドレスを返す。
         *
         * @return 送信元メールアドレス
         */
        public String getFrom() {
            return from;
        }

        /**
         * 送信元メールアドレスを設定する。
         *
         * @param from 送信元メールアドレス
         */
        public void setFrom(String from) {
            this.from = from;
        }

        /**
         * 返信先メールアドレスを返す。
         *
         * @return 返信先メールアドレス
         */
        public String getReplyTo() {
            return replyTo;
        }

        /**
         * 返信先メールアドレスを設定する。
         *
         * @param replyTo 返信先メールアドレス
         */
        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        /**
         * サイトURLを返す。
         *
         * @return サイトURL
         */
        public String getSiteUrl() {
            return siteUrl;
        }

        /**
         * サイトURLを設定する。
         *
         * @param siteUrl サイトURL
         */
        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        /**
         * 問い合わせ先メールアドレスを返す。
         *
         * @return 問い合わせ先メールアドレス
         */
        public String getContactEmail() {
            return contactEmail;
        }

        /**
         * 問い合わせ先メールアドレスを設定する。
         *
         * @param contactEmail 問い合わせ先メールアドレス
         */
        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }
    }
}
