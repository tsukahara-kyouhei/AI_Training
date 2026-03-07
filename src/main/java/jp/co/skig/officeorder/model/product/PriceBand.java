package jp.co.skig.officeorder.model.product;

/**
 * 商品一覧の価格帯絞り込みで使用する帯定義。
 */
public enum PriceBand {
    BAND_1(1, null, 20000L),
    BAND_2(2, 20000L, 40000L),
    BAND_3(3, 40000L, 60000L),
    BAND_4(4, 60000L, 80000L),
    BAND_5(5, 80000L, 100000L),
    BAND_6(6, 100000L, null);

    private final int id;
    private final Long min;
    private final Long max;

    PriceBand(int id, Long min, Long max) {
        this.id = id;
        this.min = min;
        this.max = max;
    }

    /**
     * 画面や検索条件で扱う価格帯IDを返す。
     */
    public int id() {
        return id;
    }

    /**
     * 価格帯の下限金額を返す。下限なしの場合は {@code null}。
     */
    public Long min() {
        return min;
    }

    /**
     * 価格帯の上限金額を返す。上限なしの場合は {@code null}。
     */
    public Long max() {
        return max;
    }

    /**
     * 画面から渡された価格帯IDを列挙値へ変換する。
     */
    public static PriceBand fromId(int id) {
        for (PriceBand band : values()) {
            if (band.id == id) {
                return band;
            }
        }
        return null;
    }
}

