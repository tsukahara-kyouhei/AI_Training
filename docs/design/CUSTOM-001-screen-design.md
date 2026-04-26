# CUSTOM-001 画面設計書 — お薦め機能

| 項目 | 内容 |
|------|------|
| 課題ID | CUSTOM-001 |
| 作成日 | 2026-04-18 |
| ステータス | Draft |
| 関連要件 | [CUSTOM-001-requirements.md](../issues/custome/001/CUSTOM-001-requirements.md) |
| 関連詳細設計 | [CUSTOM-001-detail-design.md](CUSTOM-001-detail-design.md) |

---

## 1. 変更対象画面

| 画面名 | テンプレートファイル | 変更区分 |
|------|----------------|--------|
| 商品詳細 | `templates/pages/product-detail.html` | 変更（関連商品セクション追加） |
| カート | `templates/pages/cart.html` | 変更（クロスセルセクション追加） |
| トップ | `templates/pages/top.html` | 変更（「超マジお薦め」セクション追加） |

---

## 2. セクション A — 商品詳細「この商品と合わせてよく見られています」

### 2.1 表示位置

既存の商品詳細コンテンツ（スペック・バリアント選択・カート追加ボタン）の **下部**、シリーズ商品リンク（存在する場合）の下に追加する。

### 2.2 表示条件

| 条件 | 動作 |
|-----|------|
| `hasRelatedProducts == true` | セクション全体を表示 |
| `hasRelatedProducts == false` | セクション全体を非表示（HTML 要素ごと出力しない） |

### 2.3 テンプレート追加箇所（`product-detail.html`）

```html
<!-- ★CUSTOM-001 関連商品セクション（A） -->
<section th:if="${hasRelatedProducts}" class="related-products-section">
  <h2 class="section-title">この商品と合わせてよく見られています</h2>
  <div class="product-card-row">
    <th:block th:each="product : ${relatedProducts}">
      <!-- 既存の商品カード部品（_product-card.html フラグメント）を流用 -->
      <div th:replace="~{fragments/product-card :: card(product=${product})}"></div>
    </th:block>
  </div>
</section>
```

### 2.4 商品カードの仕様

- **既存の `ProductCardView` 仕様に準拠**（商品名・最低価格〜最高価格・サムネイル・お気に入りトグル）。
- 在庫切れ（`!product.inStock`）の場合は在庫切れバッジを表示する（既存カード仕様と同様）。
- カードクリックで `/products/{productId}` へ遷移する。

---

## 3. セクション B — カート「こちらもいかがですか？」

### 3.1 表示位置

カート内商品一覧・合計金額・購入手続きボタンの **下部** に追加する。

### 3.2 表示条件

| 条件 | 動作 |
|-----|------|
| `hasCrossSellProducts == true` | セクション全体を表示 |
| `hasCrossSellProducts == false` または Cookie なし | セクション全体を非表示 |

### 3.3 テンプレート追加箇所（`cart.html`）

```html
<!-- ★CUSTOM-001 クロスセルセクション（B） -->
<section th:if="${hasCrossSellProducts}" class="cross-sell-section">
  <h2 class="section-title">こちらもいかがですか？</h2>
  <div class="product-card-row">
    <th:block th:each="product : ${crossSellProducts}">
      <div th:replace="~{fragments/product-card :: card(product=${product})}"></div>
    </th:block>
  </div>
</section>
```

### 3.4 カードのアクション（フェーズ区分）

| フェーズ | 仕様 |
|---------|-----|
| フェーズ1 | 既存 `ProductCardView` と同じ動線（カードクリックで商品詳細ページへ遷移）。カート直接追加ボタンは表示しない |
| フェーズ2 | 商品詳細を経由しないカート直接追加ボタンを追加（バリアント選択が必要な場合は商品詳細ページへ誘導） |

---

## 4. セクション C — トップ「超マジお薦め」

### 4.1 表示位置

トップページの「最近見た商品」セクションの **下部** に追加する。

### 4.2 表示条件

| 条件 | 動作 |
|-----|------|
| ログイン中かつ `hasTopPersonalizedRecommendations == true` | セクション全体を表示 |
| 未ログイン | セクション全体を非表示（`th:if` でログイン判定） |
| ログイン中かつ推薦商品0件 | セクション全体を非表示 |

### 4.3 テンプレート追加箇所（`top.html`）

```html
<!-- ★CUSTOM-001 超マジお薦めセクション（C・フェーズ1） -->
<section th:if="${hasTopPersonalizedRecommendations}" class="personalized-recommendation-section">
  <h2 class="section-title">超マジお薦め</h2>
  <div class="product-card-grid">
    <th:block th:each="product : ${topPersonalizedRecommendations}">
      <div th:replace="~{fragments/product-card :: card(product=${product})}"></div>
    </th:block>
  </div>
</section>
```

### 4.4 フェーズ2（未ログイン時の対応）

フェーズ2では以下を追加する（本設計書のスコープ外）。
- 未ログイン時にも `popular_product_rankings` を起点としたお薦め商品をセクション C 相当の位置に表示する。
- セクションタイトル・ロジックはフェーズ2設計時に確定する。

---

## 5. 在庫切れの表示仕様

在庫切れ商品（すべてのバリアントで `stock_quantity <= 0`）は推薦セクションに表示するが、商品カード上で在庫状態を示す。

| 表示方法 | 仕様 |
|---------|-----|
| 在庫切れバッジ | 既存の在庫切れバッジ（`in-stock-badge--out`）をカード左上に重ねて表示する |
| カード不活性化 | カードを若干グレーアウトし、在庫切れであることを視覚的に示す（既存スタイルに準拠） |

---

## 6. モデル属性一覧

| 画面 | 属性名 | 型 | 説明 |
|-----|-------|---|------|
| product-detail | `relatedProducts` | `List<ProductCardView>` | 関連商品リスト（0〜4件） |
| product-detail | `hasRelatedProducts` | `boolean` | セクション表示フラグ |
| cart | `crossSellProducts` | `List<ProductCardView>` | クロスセル商品リスト（0〜4件） |
| cart | `hasCrossSellProducts` | `boolean` | セクション表示フラグ |
| top | `topPersonalizedRecommendations` | `List<ProductCardView>` | パーソナライズ推薦リスト（0〜8件） |
| top | `hasTopPersonalizedRecommendations` | `boolean` | セクション表示フラグ |
