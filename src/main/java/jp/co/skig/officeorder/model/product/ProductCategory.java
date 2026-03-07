package jp.co.skig.officeorder.model.product;

import java.util.Arrays;
import java.util.Optional;

/**
 * 商品カテゴリの識別子と表示名を表す列挙型。
 */
public enum ProductCategory {
    DESK("desk", "デスク"),
    CHAIR("chair", "チェア"),
    STORAGE("storage", "収納家具");

    private final String id;
    private final String label;

    ProductCategory(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * URL や検索条件で使用するカテゴリIDを返す。
     */
    public String id() {
        return id;
    }

    /**
     * 画面表示に使用するカテゴリ名を返す。
     */
    public String label() {
        return label;
    }

    /**
     * URL やフォームから渡されたカテゴリIDを列挙値へ変換する。
     */
    public static Optional<ProductCategory> fromId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(category -> category.id.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
