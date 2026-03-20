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
