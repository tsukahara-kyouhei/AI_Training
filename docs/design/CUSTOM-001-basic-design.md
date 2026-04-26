# CUSTOM-001 基本設計書 — お薦め機能

| 項目 | 内容 |
|------|------|
| 課題ID | CUSTOM-001 |
| 作成日 | 2026-04-18 |
| ステータス | Draft |
| 関連要件 | [CUSTOM-001-requirements.md](../issues/custome/001/CUSTOM-001-requirements.md) |

---

## 1. 変更概要

閲覧・購入データを活用したお薦め機能を以下3箇所に追加する。

| # | 対象画面 | セクションタイトル | 主なデータソース | フェーズ |
|---|---------|-----------------|---------------|--------|
| A | 商品詳細ページ（`/products/{productId}`） | 「この商品と合わせてよく見られています」 | `recommended_related_products` | フェーズ1 |
| B | カート画面（`/cart`） | 「こちらもいかがですか？」 | `recommended_related_products`（最近見た商品起点） | フェーズ1 |
| C | トップページ（`/`）会員限定 | 「超マジお薦め」 | `orders`/`recommended_related_products`/`popular_product_rankings` | フェーズ1 |

---

## 2. アーキテクチャ概要

本システムの既存多層アーキテクチャを踏襲する。今回の変更が及ぶ層を示す。

```
[Browser]
   │  HTTP GET /  /products/{id}  /cart
   ▼
[Controller層]      HomeController          ★変更あり（セクションC）
                    CatalogController       ★変更あり（セクションA）
                    CartController          ★変更あり（セクションB）
   │
   ▼
[Service層]         ProductService          ★変更あり（findRelatedProducts / findCrossSellProducts 追加）
                    RecommendationService   ★新規追加（セクションCのパーソナライズロジック）
   │
   ▼
[Repository層]      ProductRepository       ★変更あり（推薦取得メソッド追加）
                    OrderRepository         ★変更あり（購入履歴取得メソッド追加）
   │
   ▼
[Mapper層]          ProductMapper (XML)     ★変更あり（推薦取得 SQL 追加）
                    OrderMapper (XML)       ★変更あり（購入履歴取得 SQL 追加）
   │
   ▼
[DB]                recommended_related_products   変更なし
                    popular_product_rankings        変更なし
                    orders / order_items            変更なし（参照のみ）
                    products / product_variants     変更なし
```

DB スキーマの変更はなし。インデックス追加のみ検討（確認事項 D-02 参照）。

---

## 3. 変更コンポーネント一覧

### 3.1 新規追加

| コンポーネント | パッケージ | 役割 |
|-------------|---------|------|
| `RecommendationService` | `jp.co.skig.officeorder.service.product` | トップページ向けパーソナライズ推薦ロジック（3段階補完）を担うサービス |

### 3.2 変更（既存クラスへの追加・修正）

| コンポーネント | ファイルパス | 変更内容の概要 |
|-------------|-----------|-------------|
| `HomeController` | `web/HomeController.java` | `MemberSessionService`・`RecommendationService` を DI。セクションCの条件分岐と `recentlyViewedProductIds` 取得を追加 |
| `CatalogController` | `web/CatalogController.java` | `productDetail()` メソッドに `findRelatedProducts()` 呼び出しを追加 |
| `CartController` | `web/CartController.java` | `cart()` メソッドに `findCrossSellProducts()` 呼び出しを追加 |
| `ProductService` | `service/product/ProductService.java` | `findRelatedProducts(productId)` / `findCrossSellProducts(sourceId, excludeIds)` メソッドを追加 |
| `ProductRepository` | `repository/ProductRepository.java` | 推薦取得3メソッドを追加（詳細設計書 §2〜§4 参照） |
| `OrderRepository` | `repository/OrderRepository.java` | 購入履歴商品ID取得メソッドを追加（詳細設計書 §5 参照） |
| `ProductMapper.xml` | `resources/mappers/ProductMapper.xml` | 推薦取得 SQL フラグメントを追加 |
| `OrderMapper.xml` | `resources/mappers/OrderMapper.xml` | 購入履歴取得 SQL を追加 |
| `product-detail.html` | `templates/pages/product-detail.html` | 関連商品セクション追加 |
| `cart.html` | `templates/pages/cart.html` | クロスセルセクション追加 |
| `top.html` | `templates/pages/top.html` | 「超マジお薦め」セクション追加（会員限定、ログインチェック） |

### 3.3 変更なし

- `ProductCardView`・`ProductDetailView` など既存表示モデル — 変更なし
- `ProductListSearchService`・`ProductFilterOptionService` — 変更なし
- DB スキーマ全般 — 変更なし
- カテゴリ別一覧ページ、検索結果ページ — 変更なし

---

## 4. 処理フロー概要

### 4-A. 商品詳細ページ（セクションA）

```
1. GET /products/{productId} を受信
2. 既存の商品詳細取得・お気に入り判定（現行どおり）
3. ProductService.findRelatedProducts(productId) を呼び出す
   - 最新の recommendation_date を持つ recommended_related_products を参照
   - 販売期間外の商品を除外
   - rank 昇順で最大4件返却
   - 0件の場合はセクション非表示（空リストを渡す）
4. モデルに relatedProducts を追加
5. HTML レンダリング
```

### 4-B. カート画面（セクションB）

```
1. GET /cart を受信
2. 既存のカート表示処理（現行どおり）
3. 最近見た商品IDリストを取得（設計確認事項 D-01 参照）
4. リストが空の場合はセクション非表示
5. ProductService.findCrossSellProducts(recentlyViewedIds[0], cartProductIds) を呼び出す
   - 最新 recommendation_date の recommended_related_products を参照
   - カート内既存商品を除外
   - 販売期間外を除外
   - rank 昇順で最大4件返却
6. モデルに crossSellProducts を追加
7. HTML レンダリング
```

### 4-C. トップページ（セクションC）

```
1. GET / を受信
2. 既存の新着商品・ランキング取得（現行どおり）
3. MemberSessionService.currentMember() でログイン状態を確認
4. 未ログイン → セクション非表示
5. ログイン中 → RecommendationService.findTopPersonalizedRecommendations(memberId, recentlyViewedIds) を呼び出す
   【優先順位1】 直近6ヶ月の購入商品を起点に recommended_related_products を参照
                 直近3ヶ月購入済み商品・販売期間外商品は除外
   【優先順位2】 候補8件未満の場合、最近見た商品を起点に補完
   【優先順位3】 まだ8件未満の場合、popular_product_rankings の上位商品で補完
   重複排除後、最大8件を返却
6. モデルに topPersonalizedRecommendations を追加
7. HTML レンダリング
```

---

## 5. 性能設計

| ポイント | 方針 |
|---------|------|
| `recommended_related_products` の参照 | `(source_product_id, recommendation_date)` の複合インデックスが有効かを確認（確認事項 D-02）|
| セクションC の DB アクセス回数 | 最大3クエリ（購入履歴→推薦→売れ筋）。すべて単純な SELECT で結合は推薦テーブルと商品テーブルのみ |
| 目標レスポンス | 95 パーセンタイルで 2 秒以内（既存 FEAT-001 基準と同一） |

---

## 6. セキュリティ設計

| 観点 | 対応 |
|-----|------|
| セクションC の会員認証確認 | `MemberSessionService.currentMember()` が空の場合は推薦取得を行わずセクション非表示 |
| 最近見た商品IDの検証 | Cookie/Session から取得した ID は数値（`Long`）として厳密に Parse し、不正値は無視する |
| SQL インジェクション | MyBatis `#{}` バインドパラメータを使用する（`${}` は使用しない） |

---

## 7. フェーズ区分

| フェーズ | 実装スコープ |
|---------|------------|
| フェーズ1（本設計書対象） | セクションA・B・C 全実装。カートBは詳細リンクのみ。Cは会員限定 |
| フェーズ2（次回設計） | 未ログイン時トップにも売れ筋起点で推薦表示。カートBに直接追加ボタン |
