# 商品サービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `ProductService` | `service.product` | 商品検索・新着・ランキング・詳細取得 |
| `ProductFilterOptionService` | `service.product` | フィルターオプション（色・形状・素材等）取得 |

---

## 2. 主要メソッド

### 2.1 findTopNewArrivals()

- **機能:** トップ画面用新着商品取得（4件）
- **ロジック:** `sale_start_at <= now` かつ `sale_end_at IS NULL OR sale_end_at > now` の商品を `sale_start_at DESC` で上位4件
- **戻り値:** `List<ProductCardView>`

### 2.2 findTopRankedProducts()

- **機能:** 売れ筋ランキング商品取得
- **ロジック:** `popular_product_rankings` と `products` をJOINし、最新日付の上位10件を取得
- **戻り値:** `List<ProductCardView>`

### 2.3 search(condition)

- **機能:** 商品カタログ検索（カテゴリ・フィルター・ソート・ページング）
- **受入値:** `ProductSearchCondition`

| フィールド | 内容 |
|---|---|
| `category` | `desk` / `chair` / `storage` |
| `colorIds` | 色フィルター |
| `topShapeId` | 天板形状（デスクのみ） |
| `functionId` | 機能（チェアのみ） |
| `materialId` | 素材（チェアのみ） |
| `usageId` | 用途（収納家具のみ） |
| `tasteId` | テイスト |
| `sort` | `recommended` / `newest` / `price_asc` / `price_desc` |
| `page`, `size` | ページング |

> **補足:** 検索結果画面（`categoryId` 未指定）では、`ProductCategoryFilter` の `deskTasteIds` / `chairTasteIds` / `storageTasteIds` を3カテゴリ分同時に適用する。カテゴリ別一覧では従来通り該当カテゴリのテイストのみ適用。

> **キーワード制限:** `keyword` が100文字を超える場合、先頭100文字に切り詰める。切り詰め処理は `CatalogController#searchResults()` で実施する。

- **戻り値:** `Page<ProductCardView>`

### 2.4 findDetail(productId)

- **機能:** 商品詳細取得
- **ロジック:**
  1. products + product_variants + colors を取得
  2. カテゴリに応じた属性テーブルもJOIN
  3. `recommended_related_products` から関連商品上位4件を取得
  4. 会員ログイン中はお気に入り登録済フラグをビューにセット
- **戻り値:** `ProductDetailView`
- **エラー:** 商品が存在しないまたは販売期間外 -> `ProductNotFoundException`

---

## 3. 販売期間のフィルタリング

```
公開条件:
sale_start_at <= 現在日時
AND (sale_end_at IS NULL OR sale_end_at > 現在日時)

日時取得: AppClockConfig の Clock Bean を使用（テストで固定時刻に切替可能）
```

---

## 4. 表記ゆれ正規化（キーワード検索）

キーワード検索時、入力値とDB値の両方を正規化して比較することで、全角半角・大文字小文字を区別しない検索を実現する。

### 4.1 Java側（アプリケーション層）

- `common/` パッケージの正規化ユーティリティを使用
  - 全角英字（Ａ-Ｚ, ａ-ｚ）→ 半角（A-Z, a-z）
  - 全角数字（０-９）→ 半角（0-9）
  - 半角カタカナ → 全角カタカナ（ｱ→ア, ｶﾞ→ガ 等。濁音・半濁音の2文字結合も全角1文字に変換）
- `ProductRepository#toKeywordLike()` 内でユーティリティを呼び出し、正規化済みキーワードでLIKE文字列を生成
- `toKeywordPrefix()` メソッドを新規追加し、商品コード検索用の前方一致文字列（`keyword%`）を生成

### 4.2 特殊文字エスケープ（FR-043）

LIKE/ILIKE のワイルドカードとして解釈される特殊文字をエスケープする。エスケープ文字は `\`。

| 入力文字 | エスケープ後 |
|---|---|
| `\` | `\\` |
| `%` | `\%` |
| `_` | `\_` |

- 処理順序: **正規化（全角→半角）→ エスケープ → LIKE文字列生成**（`%` + escaped + `%`）
- `toKeywordLike()` と `toKeywordPrefix()` の両方で同一のエスケープ処理を適用
- SQL側で `ESCAPE '\'` を各 ILIKE 句に付与する
- 現行の `toKeywordLike()` にはエスケープ処理が未実装のため、本改修で新規に追加する

### 4.3 SQL側

- `BaseProductWhere` fragment内で `translate()` を使用し、カラム値を正規化して比較
  - 英数字: 全角→半角に変換（Ａ→A 等）
  - カタカナ: 半角清音→全角に変換（ｱ→ア 等、1:1変換可能な清音＋長音のみ）
  - `translate()` は1文字対1文字の置換のため、半角濁音・半濁音の結合文字（ｶﾞ等の2文字）は変換対象外。DBデータは通常全角カタカナで格納されるため実用上問題なし
- 大文字小文字の同一視は既存の `ILIKE` で対応
- 変換テーブル文字列はSQL fragment（`<sql id="NormalizeFrom">` / `<sql id="NormalizeTo">`）として1箇所に定義し、重複を排除

---

## 5. テイストフィルター構築（検索結果画面）

### 5.1 buildSearchFilter()（ProductFilterOptionService）

検索結果画面（`categoryId` 未指定）でテイスト入力を受け取り、`ProductCategoryFilter` を構築するメソッドを新規追加する。

```java
public ProductCategoryFilter buildSearchFilter(
        List<Integer> rawDeskTasteIds,
        List<Integer> rawChairTasteIds,
        List<Integer> rawStorageTasteIds,
        ProductFilterOptionsBundle optionsBundle)
```

- 入力のテイストIDを `optionsBundle` のマスタ候補で正規化（存在しないIDを除外）
- テイスト以外のフィルター項目（天板形状、幅、素材等）はすべて空リストをセット
- 戻り値: `ProductCategoryFilter`（テイスト3種のみ有効、他はempty list）

### 5.2 hasTasteFilter フラグ計算（ProductRepository）

`buildSearchParams()` に `hasTasteFilter` の計算ロジックを追加する。

```java
boolean hasTasteFilter = (category == null)
        && (!deskTasteIds.isEmpty()
            || !chairTasteIds.isEmpty()
            || !storageTasteIds.isEmpty());
```

- `category == null`（検索結果画面）の場合のみ `true` になり得る
- カテゴリ別一覧では従来通り `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` でテイストを制御（排他関係）
