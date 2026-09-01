package jp.co.skig.officeorder.model.product;

/**
 * 商品一覧で選択できる並び順。
 */
public enum ProductSort {
    RECOMMENDED("recommended"),
    NEWEST("newest"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc");

    private final String value;

    ProductSort(String value) {
        this.value = value;
    }

    /**
     * リクエストパラメータや画面選択値として扱う並び順コードを返す。
     */
    public String value() {
        return value;
    }

    /**
     * リクエスト値を並び順列挙値へ変換し、不正値時は既定値を返す。
     */
    public static ProductSort fromValue(String value, ProductSort defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        for (ProductSort sort : values()) {
            if (sort.value.equalsIgnoreCase(value)) {
                return sort;
            }
        }
        return defaultValue;
    }
}
