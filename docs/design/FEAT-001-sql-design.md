# FEAT-001 商品検索機能強化 SQL 詳細設計書

対象ファイル: `src/main/resources/mappers/ProductMapper.xml`

---

## 1. パラメータ設計

`ProductRepository.buildSearchParams()` が生成するパラメータのうち、キーワード検索に関わるものを以下の通り変更・追加する。

### 1.1 キーワード関連パラメータ（変更）

| パラメータ名 | 型 | 現状の値 | 変更後の値 | 用途 |
|------------|-----|---------|-----------|------|
| `keywordLike` | `String` | `%keyword%` | `%正規化済みkeyword%` | 部分一致（商品名・バリエーション名・商品紹介文） |
| `keywordExact` | `String` | ─（新規追加） | `正規化済みkeyword` | 商品コード完全一致 |
| `keywordPrefix` | `String` | ─（新規追加） | `正規化済みkeyword%` | 商品コード前方一致 |

> **注意：** 正規化ルールは「半角数字・英字・カタカナ → 全角」。正規化は Java で実施し、正規化済みの値を SQL に渡す。

### 1.2 テイスト絞り込み関連パラメータ（新規）

| パラメータ名 | 型 | 値 | 用途 |
|------------|-----|---|------|
| `searchTasteNames` | `List<String>` | 選択された display_name のリスト | 検索結果テイスト絞り込み（名称ベース） |
| `hasSearchTasteFilter` | `boolean` | `searchTasteNames` が非空のとき `true` | `<if>` 制御フラグ |

---

## 2. `BaseProductWhere` フラグメントの変更

### 2.1 変更前（現状）

```xml
<if test="keywordLike != null">
  AND (
    p.product_name ILIKE #{keywordLike}
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE #{keywordLike}
    )
  )
</if>
```

### 2.2 変更後

```xml
<if test="keywordLike != null">
  AND (
    normalize_fullwidth(p.product_name)    ILIKE #{keywordLike}
    OR (p.variation_name IS NOT NULL
        AND normalize_fullwidth(p.variation_name) ILIKE #{keywordLike})
    OR normalize_fullwidth(p.description)  ILIKE #{keywordLike}
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND (
          normalize_fullwidth(pvk.product_code) = #{keywordExact}
          OR normalize_fullwidth(pvk.product_code) ILIKE #{keywordPrefix}
        )
    )
  )
</if>
```

#### 変更のポイント

| 項目 | 内容 |
|------|------|
| `products.variation_name` | 新規追加。`IS NOT NULL` チェックにより NULL 商品を除外（`ILIKE` の NULL 評価でも除外されるが明示的に記述） |
| `products.description` | 新規追加。部分一致 |
| 商品コード | `ILIKE #{keywordLike}`（部分一致）→ `= #{keywordExact} OR ILIKE #{keywordPrefix}`（完全一致 or 前方一致）に変更 |
| `normalize_fullwidth()` | DB カラム値を全角へ正規化する関数（後述） |

---

## 3. 全角・半角正規化方式（`normalize_fullwidth` 関数）

### 3.1 実装方式の決定（未決事項 #1 の結論方針）

DB カラム値の正規化は **PostgreSQL カスタム関数 `normalize_fullwidth(text) RETURNS text`** で実装する方向を推奨とし、最終決定は実装担当者に委ねる。

#### 推奨理由

| 観点 | PostgreSQL カスタム関数 | Java 生成の OR 条件拡張 |
|------|----------------------|----------------------|
| SQL の可読性 | 高い（1 行で記述） | 低い（AND OR の組み合わせが複雑化） |
| 保守性 | 関数だけ変更すれば全 SQL に適用 | 全 SQL 変更が必要 |
| パフォーマンス | インデックス不使用（関数適用のため） | インデックス不使用（ILIKE のため） |
| Migrationコスト | スキーマ移行 SQL が必要 | 不要 |

> 商品数が最大 1,000 件程度（要件定義 §5）の規模であり、パフォーマンス差は許容範囲内と判断する。

### 3.2 カスタム関数の仕様（参考）

```sql
CREATE OR REPLACE FUNCTION normalize_fullwidth(input text) RETURNS text AS $$
DECLARE
  result text := input;
BEGIN
  -- 半角数字 → 全角 (0-9 → ０-９)
  result := translate(result,
    '0123456789',
    '０１２３４５６７８９');
  -- 半角英字大文字 → 全角 (A-Z → Ａ-Ｚ)
  result := translate(result,
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ');
  -- 半角英字小文字 → 全角 (a-z → ａ-ｚ)
  result := translate(result,
    'abcdefghijklmnopqrstuvwxyz',
    'ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ');
  -- 半角カタカナ → 全角はより複雑なマッピングが必要（濁点・半濁点の合成を含む）
  -- → Java ライブラリ（ICU4J 等）で変換したほうが確実。
  --   実装担当者と相談の上、Java 側正規化のみで賄うか DB 関数で対応するかを決定する。
  RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
```

> **タイミング：** この関数は DB 移行スクリプト（Flyway または手動 DDL）として追加する。

---

## 4. `SearchTasteFilter` フラグメント（新規追加）

検索結果画面用のテイスト絞り込みを行う新規フラグメント。
`BaseProductWhere` の末尾（カテゴリ固有フィルタの後）に `<include>` して呼び出す。

```xml
<sql id="SearchTasteFilter">
  <if test="hasSearchTasteFilter">
    AND (
      EXISTS (
        SELECT 1
        FROM product_desk_attributes da
        JOIN desk_tastes dt ON da.taste_id = dt.taste_id
        WHERE da.product_id = p.product_id
          AND dt.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1
        FROM product_chair_attributes ca
        JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
        WHERE ca.product_id = p.product_id
          AND ct.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1
        FROM product_storage_attributes sa
        JOIN storage_tastes st ON sa.taste_id = st.taste_id
        WHERE sa.product_id = p.product_id
          AND st.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
    )
  </if>
</sql>
```

### 設計判断の根拠

| 観点 | 方針 |
|------|------|
| 絞り込みキー | `display_name`（文字列）を使用。3 テーブルを名称統合して表示するため、ID は各カテゴリで独立しており使えない。 |
| NULL の扱い | デスク・チェア・収納に属さない商品（属性テーブルにレコードなし）はすべて除外される。テイストを選択した場合は意図した動作。 |
| OR 結合 | category をまたいでいずれかのテイストにマッチすれば表示対象とする（要件定義 §4.5.3）。 |

---

## 5. テイスト選択肢取得クエリ（新規追加）

検索結果画面のテイストチェックボックスに使う選択肢を取得するクエリを追加する。

```xml
<select id="selectUnifiedSearchTasteOptions" resultMap="ProductFilterOptionRowMap">
  SELECT DISTINCT display_name AS display_name
  FROM (
    SELECT display_name, sort_order FROM desk_tastes    WHERE is_active = TRUE
    UNION ALL
    SELECT display_name, sort_order FROM chair_tastes   WHERE is_active = TRUE
    UNION ALL
    SELECT display_name, sort_order FROM storage_tastes WHERE is_active = TRUE
  ) t
  GROUP BY display_name
  ORDER BY MIN(sort_order) ASC
</select>
```

#### 設計判断

| 観点 | 内容 |
|------|------|
| 重複排除 | `GROUP BY display_name` で名称の重複を排除する |
| 並び順 | 同一名称が複数カテゴリに存在する場合、最も小さい `sort_order` を優先する |
| `ResultMap` | 既存の `ProductFilterOptionRowMap`（`option_id`, `display_name` 列）を流用する。<br>ただし `option_id` に相当する ID カラムは不要のため、`display_name` のみ使用する形に整理する（詳細はクラス設計書参照）。 |

---

## 6. 変更対象のまとめ（ProductMapper.xml）

| 変更箇所 | 種別 | 内容 |
|---------|------|------|
| `BaseProductWhere` 内のキーワード条件 | 変更 | 検索対象フィールド追加・商品コード一致方式変更・全角正規化 |
| `SearchTasteFilter` フラグメント | 新規追加 | 検索結果用テイスト絞り込み |
| `BaseProductWhere` への `SearchTasteFilter` include | 変更 | カテゴリ固有フィルタの後に追記 |
| `selectUnifiedSearchTasteOptions` | 新規追加 | 統合テイスト選択肢取得 |
