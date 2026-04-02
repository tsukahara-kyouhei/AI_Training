# FEAT-001 商品検索機能強化 SQL設計

## 1. 対象

- `ProductRepository#buildSearchParams`
- `ProductMapper.xml`
- 検索結果画面向けテイスト絞込 SQL

---

## 2. SQL パラメータ設計

### 2.1 変更前

| パラメータ | 内容 |
|-----------|------|
| `keywordLike` | `%keyword%` |

### 2.2 変更後

| パラメータ | 型 | 内容 |
|-----------|----|------|
| `keywordWords` | `List<String>` | 正規化・分割済みの検索ワード |
| `keywordPrefixWords` | `List<String>` | `keyword + "%"` の一覧 |
| `hasSearchTasteFilter` | `boolean` | テイスト横断絞込の有無 |
| `deskTasteIds` | `List<Integer>` | デスクテイスト ID 一覧 |
| `chairTasteIds` | `List<Integer>` | チェアテイスト ID 一覧 |
| `storageTasteIds` | `List<Integer>` | 収納テイスト ID 一覧 |

---

## 3. BaseProductWhere 変更設計

### 3.1 キーワード条件

#### 変更前

```sql
AND (
  p.product_name ILIKE #{keywordLike}
  OR EXISTS (
    SELECT 1
    FROM product_variants pvk
    WHERE pvk.product_id = p.product_id
      AND pvk.product_code ILIKE #{keywordLike}
  )
)
```

#### 変更後

```xml
<if test="keywordWords != null and keywordWords.size() &gt; 0">
  <foreach collection="keywordWords" item="word" index="index">
    AND (
      p.product_name ILIKE CONCAT('%', #{word}, '%')
      OR p.variation_name ILIKE CONCAT('%', #{word}, '%')
      OR p.description ILIKE CONCAT('%', #{word}, '%')
      OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND (
            pvk.product_code = #{word}
            OR pvk.product_code ILIKE #{keywordPrefixWords[index]}
          )
      )
    )
  </foreach>
</if>
```

### 3.2 意味

- 各ワードブロックは AND で結合する。
- 各ワードブロック内では商品名・バリエーション名・説明文・商品コード条件を OR で結合する。
- `variation_name` が null の行は `ILIKE` で自然にヒットしない。

---

## 4. SearchTasteFilter 設計

### 4.1 追加位置

- `BaseProductWhere` の末尾付近に追加する。
- `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` とは独立した条件にする。

### 4.2 SQL フラグメント

```xml
<if test="hasSearchTasteFilter">
  AND (
    <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">
      EXISTS (
        SELECT 1
        FROM product_desk_attributes da
        WHERE da.product_id = p.product_id
          AND da.taste_id IN
          <foreach collection="deskTasteIds" item="value" open="(" close=")" separator=",">
            #{value}
          </foreach>
      )
    </if>
    <if test="chairTasteIds != null and chairTasteIds.size() &gt; 0">
      <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">OR</if>
      EXISTS (
        SELECT 1
        FROM product_chair_attributes ca
        WHERE ca.product_id = p.product_id
          AND ca.taste_id IN
          <foreach collection="chairTasteIds" item="value" open="(" close=")" separator=",">
            #{value}
          </foreach>
      )
    </if>
    <if test="storageTasteIds != null and storageTasteIds.size() &gt; 0">
      <if test="(deskTasteIds != null and deskTasteIds.size() &gt; 0) or (chairTasteIds != null and chairTasteIds.size() &gt; 0)">OR</if>
      EXISTS (
        SELECT 1
        FROM product_storage_attributes sa
        WHERE sa.product_id = p.product_id
          AND sa.taste_id IN
          <foreach collection="storageTasteIds" item="value" open="(" close=")" separator=",">
            #{value}
          </foreach>
      )
    </if>
  )
</if>
```

### 4.3 条件分岐ルール

- 各カテゴリの taste_id 一覧が空なら、そのカテゴリの `EXISTS` は出力しない。
- `IN ()` は生成しない。
- テイスト複数選択は同一カテゴリ内では `IN (...)`、カテゴリ間では `OR` とする。

---

## 5. Repository 側パラメータ構築詳細

### 5.1 `hasSearchTasteFilter`

```java
boolean hasSearchTasteFilter = !deskTasteIds.isEmpty()
        || !chairTasteIds.isEmpty()
        || !storageTasteIds.isEmpty();
```

### 5.2 `keywordPrefixWords`

```java
List<String> keywordPrefixWords = keywordWords.stream()
        .map(word -> word + "%")
        .toList();
```

### 5.3 並存するフラグ

| フラグ | 用途 |
|-------|------|
| `hasDeskFilter` | デスクカテゴリ一覧の形状・寸法・テイスト条件 |
| `hasChairFilter` | チェアカテゴリ一覧の機能・素材・テイスト条件 |
| `hasStorageFilter` | 収納カテゴリ一覧の用途・テイスト条件 |
| `hasSearchTasteFilter` | 検索結果画面のカテゴリ横断テイスト条件 |

---

## 6. 索引設計方針

### 6.1 前提

- 対象件数は最大 1,000 件程度
- DB 側正規化インデックスは追加しない

### 6.2 評価対象

| 対象 | 評価観点 |
|------|----------|
| `product_variants.product_code` | 完全一致・前方一致での応答性能 |
| `products.product_name` | 部分一致時のレスポンス |
| `products.variation_name` | 部分一致時のレスポンス |
| `products.description` | 部分一致時のレスポンス |

### 6.3 方針

- まずは既存索引と件数規模で性能評価を行う。
- 追加索引の検討優先度は `product_variants.product_code` を最優先とする。
- `product_name` / `variation_name` / `description` は性能測定結果に応じて追加対応を判断する。

---

## 7. SQL 変更箇所一覧

| ファイル | 変更箇所 |
|---------|---------|
| `ProductMapper.xml` | `BaseProductWhere` |
| `ProductMapper.xml` | 新規 `SearchTasteFilter` |
| `ProductMapper.xml` | `countProducts` / `selectProducts` が参照する共通条件 |
