# CUSTOM-001 設計変更リスト

| 項目 | 内容 |
|------|------|
| 課題ID | CUSTOM-001 |
| 作成日 | 2026-04-18 |
| ステータス | Draft |
| 関連要件 | [CUSTOM-001-requirements.md](../../issues/custome/001/CUSTOM-001-requirements.md) |

---

## 1. 既存設計書の更新一覧

| # | ファイル | 変更区分 | 変更内容 |
|---|---------|--------|---------|
| 1 | [screen-list.md](../../design/screen-list.md) | 更新 | ヘッダ最終更新日更新。1-1（トップ）・2-6（商品詳細）・4-1（カート）の機能説明に★CUSTOM-001 追記 |
| 2 | [crud-matrix.md](../../design/crud-matrix.md) | 更新 | 機能一覧に F17〜F19 追加。CRUDテーブルに3列追加（`F17 お薦め 商品詳細` / `F18 お薦め カート` / `F19 お薦め トップ`）。`products`・`product_variants`・`orders`・`order_items`・`popular_product_rankings`・`recommended_related_products`・`members` の各行に R を追加 |
| 3 | [sequence-diagram.md](../../design/sequence-diagram.md) | 更新 | ヘッダ最終更新日更新。1-1（トップ）のシーケンスに「超マジお薦め」取得フロー（ログイン時のみ）を追加。2-6（商品詳細）のシーケンスに関連商品取得フローを追加。4-1（カート）のシーケンスにクロスセル取得フローを追加 |
| 4 | [screen-flow-diagram.md](../../design/screen-flow-diagram.md) | 更新 | ヘッダ最終更新日更新。商品詳細「関連商品クリック」→商品詳細、カート「クロスセル商品クリック」→商品詳細、トップ「超マジお薦めクリック（会員のみ）」→商品詳細 の遷移矢印を追加 |

---

## 2. 新規作成設計書

| # | ファイル | 内容 |
|---|---------|------|
| 5 | [CUSTOM-001-basic-design.md](../../design/CUSTOM-001-basic-design.md) | 基本設計書（変更概要・アーキテクチャ・変更コンポーネント一覧・処理フロー概要・性能設計・セキュリティ設計・フェーズ区分） |
| 6 | [CUSTOM-001-detail-design.md](../../design/CUSTOM-001-detail-design.md) | 詳細設計書（`RecommendationService` 新規クラス・`ProductRepository` / `OrderRepository` 追加メソッド・SQL 概要・`HomeController` / `CatalogController` / `CartController` 変更箇所） |
| 7 | [CUSTOM-001-screen-design.md](../../design/CUSTOM-001-screen-design.md) | 画面設計書（変更対象テンプレート・各セクション表示位置・表示条件・Thymeleaf テンプレート追加箇所・モデル属性一覧） |

---

## 3. 変更サマリー（機能別）

### セクション A — 商品詳細「この商品と合わせてよく見られています」

| 変更点 | 詳細 |
|-------|------|
| Controller | `CatalogController.productDetail()` に `findRelatedProducts()` 呼び出しを追加。モデルに `relatedProducts`・`hasRelatedProducts` を追加 |
| Service | `ProductService` に `findRelatedProducts(productId)` を追加 |
| Repository | `ProductRepository` に `findRelatedProducts(sourceProductId, now)` を追加 |
| Mapper | `ProductMapper.xml` に推薦商品 SELECT SQL を追加 |
| テンプレート | `product-detail.html` に関連商品セクションを追加 |

### セクション B — カート「こちらもいかがですか？」

| 変更点 | 詳細 |
|-------|------|
| Controller | `CartController.cart()` に `findCrossSellProducts()` 呼び出しを追加。最近見た商品IDの取得処理を追加（確認事項 D-01 確定後） |
| Service | `ProductService` に `findCrossSellProducts(sourceProductId, cartProductIds)` を追加 |
| Repository | `ProductRepository` に `findCrossSellProducts(sourceProductId, excludeProductIds, now)` を追加 |
| Mapper | `ProductMapper.xml` にクロスセル商品 SELECT SQL を追加 |
| テンプレート | `cart.html` にクロスセルセクションを追加 |

### セクション C — トップ「超マジお薦め」

| 変更点 | 詳細 |
|-------|------|
| Controller | `HomeController.top()` に `MemberSessionService`・`RecommendationService` を DI 追加。ログイン判定・パーソナライズ推薦取得処理を追加 |
| Service（新規） | `RecommendationService` を新規作成。3段階補完ロジック（購入履歴→最近見た商品→売れ筋）を実装 |
| Repository | `ProductRepository` に `findRecommendedBySourceIds()` / `findTopRankedAsRecommendation()` を追加 |
| Repository | `OrderRepository` に `findPurchasedProductIds(memberId, from)` を追加 |
| Mapper | `ProductMapper.xml`・`OrderMapper.xml` に SQL を追加 |
| テンプレート | `top.html` に「超マジお薦め」セクション（ログイン中会員のみ）を追加 |

---

## 4. 未解決事項

設計上の確認事項は `CUSTOM-001-design-kadaiList.md` を参照。  
現時点で解答待ちの項目は以下。

| ID | 内容 |
|----|------|
| D-01 | 最近見た商品IDの取得方式（Cookie 直接読み取り vs HttpSession） |
| D-02 | `recommended_related_products` のインデックス追加要否 |
| D-03 | `order_items.product_code` → `product_variants.product_code` の外部結合可否 |
| D-04 | `ProductCardView` への `inStock` フラグの追加要否 |
