package jp.co.skig.officeorder.repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * カートCookieの読込・保存を担当するコンポーネント。
 *
 * <p>
 * Cookie値は `variantId:quantity:assembly` の独自形式で保持し、
 * ここでパースとバリデーションを一元化する。
 */
@Component
public class CartCookieStore {

    /** カートCookie名。 */
    public static final String COOKIE_NAME = "office_order_cart";
    /** カートに保持できる最大明細数。 */
    public static final int MAX_LINE_ITEMS = 100;
    /** Secure属性の付与方針。 */
    private final SecureMode secureMode;

    /**
     * カートCookieストアを生成する。
     *
     * @param appProperties 独自アプリ設定
     */
    public CartCookieStore(AppProperties appProperties) {
        this.secureMode = SecureMode.from(appProperties.getCookie().getCart().getSecureMode());
    }

    /**
     * カートCookieを読み込み、妥当な明細だけを返す。
     *
     * @param request 現在のHTTPリクエスト
     * @return カート明細
     */
    public List<CartCookieItem> load(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return List.of();
        }
        String raw = null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                raw = cookie.getValue();
                break;
            }
        }
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String decoded = UriUtils.decode(raw, StandardCharsets.UTF_8);
        String[] tokens = decoded.split("\\|");
        Map<Long, CartCookieItem> deduplicated = new LinkedHashMap<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String[] parts = token.split(":");
            if (parts.length != 3) {
                continue;
            }
            Long variantId = toLong(parts[0]);
            Integer quantity = toInt(parts[1]);
            if (variantId == null || variantId <= 0 || quantity == null || quantity < 1 || quantity > 99) {
                continue;
            }
            Boolean assemblyRequested = parseAssembly(parts[2]);
            deduplicated.put(variantId, new CartCookieItem(variantId, quantity, assemblyRequested));
            if (deduplicated.size() >= MAX_LINE_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    /**
     * カート明細をCookieへ保存する。
     *
     * @param request  現在のHTTPリクエスト
     * @param response 現在のHTTPレスポンス
     * @param items    保存対象明細
     */
    public void save(HttpServletRequest request,
            HttpServletResponse response,
            List<CartCookieItem> items) {
        if (response == null) {
            return;
        }
        if (items == null || items.isEmpty()) {
            clear(request, response);
            return;
        }
        StringBuilder builder = new StringBuilder();
        int written = 0;
        for (CartCookieItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.productVariantId() <= 0 || item.quantity() < 1 || item.quantity() > 99) {
                continue;
            }
            if (written > 0) {
                builder.append('|');
            }
            builder.append(item.productVariantId())
                    .append(':')
                    .append(item.quantity())
                    .append(':')
                    .append(formatAssembly(item.assemblyRequested()));
            written++;
            if (written >= MAX_LINE_ITEMS) {
                break;
            }
        }
        if (written == 0) {
            clear(request, response);
            return;
        }
        String encoded = UriUtils.encodePathSegment(builder.toString(), StandardCharsets.UTF_8);
        Cookie cookie = new Cookie(COOKIE_NAME, encoded);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(resolveSecureFlag(request));
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    /**
     * カートCookieを削除する。
     *
     * @param request  現在のHTTPリクエスト
     * @param response 現在のHTTPレスポンス
     */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        if (response == null) {
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(resolveSecureFlag(request));
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * 設定値と現在のリクエスト状態から Secure 属性を決定する。
     *
     * @param request 現在のHTTPリクエスト
     * @return Secure属性を付与する場合は {@code true}
     */
    private boolean resolveSecureFlag(HttpServletRequest request) {
        return switch (secureMode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> request != null && request.isSecure();
        };
    }

    /**
     * 組立指定をCookie保存用の1文字表現へ変換する。
     *
     * @param value 組立指定
     * @return Cookie保存値
     */
    private String formatAssembly(Boolean value) {
        if (value == null) {
            return "n";
        }
        return Boolean.TRUE.equals(value) ? "1" : "0";
    }

    /**
     * Cookie保存値を組立指定へ戻す。
     *
     * @param token Cookie保存値
     * @return 組立指定
     */
    private Boolean parseAssembly(String token) {
        if ("1".equals(token)) {
            return true;
        }
        if ("0".equals(token)) {
            return false;
        }
        return null;
    }

    /**
     * 文字列をLongへ変換する。
     *
     * @param raw 変換対象
     * @return 変換結果。失敗時は {@code null}
     */
    private Long toLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 文字列をIntegerへ変換する。
     *
     * @param raw 変換対象
     * @return 変換結果。失敗時は {@code null}
     */
    private Integer toInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Cookie Secure属性の付与モード。
     */
    private enum SecureMode {
        AUTO,
        ALWAYS,
        NEVER;

        /**
         * 設定文字列を SecureMode へ変換する。
         *
         * @param raw 設定値
         * @return 変換結果。解釈不能時は {@link #AUTO}
         */
        private static SecureMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return AUTO;
            }
            try {
                return SecureMode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return AUTO;
            }
        }
    }
}
