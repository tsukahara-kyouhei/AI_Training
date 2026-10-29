# FEAT-001 パフォーマンス設計

## 1. 目的

本設計は、検索機能の性能要件を実装観点で定義する。性能試験設計と分離し、実装者が SQL / DB / 画面設計の観点で改善方針を判断できるようにする。

## 2. 既存確認済み事項

- `ProductRepository.search()` は `countProducts()` と `selectProducts()` を2回実行している
- `ProductMapper.xml` では `WHERE` に `EXISTS` 条件と `IN (...)` 条件が使われている
- `product_variants` には `stock_quantity` と `unit_price` にインデックスが存在する見込みのテーブル定義がある
- `products` には `category_id`, `sale_start_at`, `sale_end_at` に対するインデックスがある

## 3. 設計方針

### 3.1 既存前提

- 要件: 1000 件程度の検索結果を想定し、95% のケースで 2 秒以内を目標とする
- 現状実装: 画面は検索結果一覧で `count` + `select` を行い、画面検索/フィルタの制御は最小限である
- 改修は既存の SQL パターンを維持し、追加の検索基盤導入を行わない

### 3.2 今回設計

- `countProducts()` と `selectProducts()` を最小限に保つ
- `LIKE` による部分一致条件は、キーワードが空でない場合のみ実行する
- `EXISTS` 条件はカテゴリ属性で必要な場合にのみ実行する
- `LIMIT/OFFSET` を使い、1ページあたりの件数を制限する
- `ORDER BY` は `sort` に応じたカラムに限定する

## 4. 想定データ量

- 商品件数: 1,000 件前後を想定
- 商品バリエーション件数: 1〜数十万件まで増加を想定
- フィルタ条件の選択数: 通常 1〜3 種類の OR 条件を想定
- 画面表示件数: 15 / 30 / 60

## 5. SQL 実行構造と性能観点

### 5.1 count 実行

```sql
SELECT COUNT(*)
FROM products p
WHERE ...
```

性能観点:
- `count(*)` は WHERE 条件によりレコード数が増えるため、`EXISTS` 条件の数をできるだけ減らす
- 価格帯、カラー、在庫条件は `product_variants` と join/exists に整理する
- キーワード未入力時は `keyword` 条件をスキップする

### 5.2 list 実行

```sql
SELECT
  p.product_id,
  p.product_name,
  MIN(pv.unit_price) AS min_price,
  MAX(pv.stock_quantity) AS max_stock,
  MIN(pv.product_code) AS product_code
FROM products p
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE ...
GROUP BY p.product_id, p.product_name
ORDER BY ...
LIMIT :limit OFFSET :offset
```

性能観点:
- `GROUP BY` は product_id、product_name で抑える
- 1画面につき最大 60 件までなので `LIMIT/OFFSET` を必ず使う
- 価格帯やカラーの複数選択時は `IN (...)` を最大数に抑える

## 6. N+1 と重複

### N+1

- `ProductRepository.search()` は `findColorCodes()` をバッチ取得しているため、商品ごとの color 取得を 1 回にまとめる
- これを維持する

### 重複行

- `product_variants` には product_id に対して複数レコードが存在するため、一覧 SQL では `GROUP BY` と `MIN/MAX` を使って集約する
- `color` 取得でも `selectColorCodes` の `ORDER BY pv.product_id ASC, c.sort_order ASC` を採用する

## 7. LIKE / ILIKE の性能

- `product_name`, `description` などの部分一致条件には `ILIKE`/`LIKE` を使う
- `product_code` は完全一致/前方一致に限定し、`LIKE` 範囲を厳格に絞る
- キーワードが空 or blank の場合は条件を省く
- 悪い検索文字列（`%`、`_`、`\` など）が入る場合はエスケープを行う

### 7.1 設計として確定済みの仕様

- エスケープ方式: Java で `\` を `\\`, `%` を `\%`, `_` を `\_` に置換した後、SQL では `ESCAPE '\'` を付与する
- Java 側: `escaped = input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")`
- SQL 側: `LOWER(product_code) LIKE :keywordPrefixPattern ESCAPE '\'` または `p.product_name ILIKE :keywordContainsPattern ESCAPE '\'`
- 対象文字: `%`, `_`, `\`
- prefix の定義: `P0001` は `P0001-` を境界として `P0001-C01` / `P0001-C02` をヒットさせ、`P00010-C01` はヒットしない

### 7.2 性能試験で確認する事項

- 実際の応答時間
- 実データでの `ILIKE` 性能
- 実際の DB 負荷
- 本番データ量での検索性能

## 8. フィルタ条件の性能

### 8.1 価格帯

- `priceRanges` は OR 条件でまとめる
- `product_variants.unit_price` を対象にする
- `List<RangeValue>` を `IN` ではなく `BETWEEN` 風の OR 条件で表現する

### 8.2 カラー

- `colorIds` は `IN (...)` で実行する
- `SELECT` 時は `colors` と join して候補を確定する

### 8.3 テイスト

- `EXISTS (...)` で `product_desk_attributes` / `product_chair_attributes` / `product_storage_attributes` を参照する
- 同一カテゴリの複数選択は OR, 異なるカテゴリ属性は AND で扱う

## 9. インデックスと DB 設計

### 9.1 確認済み

- `products`: `category_id`, `sale_start_at`, `sale_end_at`
- `product_variants`: `product_id`, `color_id`, `unit_price`
- `product_desk_attributes`: `product_id`, `taste_id`, `width_mm`, `depth_mm`, `height_mm`
- `product_chair_attributes`: `product_id`, `function_id`, `material_id`, `taste_id`
- `product_storage_attributes`: `product_id`, `usage_id`, `taste_id`

### 9.2 今回設計で追加すべき観点

- `product_name` / `description` に対する全文検索は未対応であり、今回の設計では対象外とする
- 1,000 件前後の応答で大きく問題が出る場合、部分一致検索の条件を絞る設計は性能試験で評価する
- ここに列挙したものは設計の未確定事項ではなく、性能試験で確認する項目とする

## 10. セキュリティと負荷対策

- SQL インジェクション防止: `#{}` のみ使用
- パラメータ数異常: `List` 入力は `normalize*` で許可フィルタに制限する
- 大量検索: `size` は 15/30/60 に制限される
- `LIKE` 特殊文字: Java で `\` / `%` / `_` をエスケープし、SQL は `ESCAPE '\'` を付与する

## 11. 性能改善の優先順位

1. キーワード未入力時に `keyword` 条件を省略する
2. 一覧取得時に `count` と `list` の条件を一致させる
3. `EXISTS` 条件を必要なカテゴリに限定する
4. `GROUP BY` を最小化する
5. 価格帯 / カラー / テイストの許容数を抑える

## 12. 実装後の確認観点

- 本番データ量で `ILIKE` が許容できるかは性能試験で確認する
- `LIKE` / `ILIKE` の特殊文字エスケープは PostgreSQL の `ESCAPE '\'` を基準に実装する
- `product_name`, `variation_name`, `description` の検索性能はデータ量で変わるため、性能試験で継続監視する
