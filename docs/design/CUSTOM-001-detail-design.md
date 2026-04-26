# CUSTOM-001 詳細設計書 — お薦め機能

| 項目 | 内容 |
|------|------|
| 課題ID | CUSTOM-001 |
| 作成日 | 2026-04-18 |
| ステータス | Draft |
| 関連要件 | [CUSTOM-001-requirements.md](../issues/custome/001/CUSTOM-001-requirements.md) |
| 関連基本設計 | [CUSTOM-001-basic-design.md](CUSTOM-001-basic-design.md) |

---

## 1. `RecommendationService`（新規クラス）

### 1.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/service/product/RecommendationService.java`

### 1.2 設計

```
クラス: RecommendationService
種別: @Service（Spring Bean）
役割: トップページ向けパーソナライズ推薦の3段階補完ロジックを担う

フィールド:
  - ProductRepository productRepository
  - OrderRepository orderRepository
  - AppTimeProvider appTimeProvider

メソッド:
  + findTopPersonalizedRecommendations(long memberId, List<Long> recentlyViewedProductIds): List<ProductCardView>
      最大8件のパーソナライズ推薦商品リストを返す。
      処理手順:
        1. orderRepository.findPurchasedProductIds(memberId, 直近6ヶ月) で購入商品IDを取得
        2. productRepository.findRecommendedBySourceIds(
               purchasedProductIds,
               excludeProductIds=null,     // 後述の除外リストは SQL 内で処理
               purchasedWithin3MonthsIds,  // 直近3ヶ月購入済みIDリスト（除外対象）
               currentDateTime,
               limit=8
           ) で推薦候補を取得
        3. 候補が8件未満かつ recentlyViewedProductIds が空でない場合:
           productRepository.findRecommendedBySourceIds(
               recentlyViewedProductIds,
               alreadyCollected=現在の候補IDリスト,
               purchasedWithin3MonthsIds,
               currentDateTime,
               limit=8-現在件数
           ) で補完
        4. まだ8件未満の場合:
           productRepository.findTopRankedAsRecommendation(
               alreadyCollected=現在の候補IDリスト,
               purchasedWithin3MonthsIds,
               currentDateTime,
               limit=8-現在件数
           ) で売れ筋から補完
        5. 重複排除・最大8件にトリム後、List<ProductCardView> を返す
        6. 推薦候補が0件の場合は空リストを返す（呼び出し元でセクション非表示）
```

---

## 2. `ProductRepository` — 追加メソッド（セクションA）

### 2.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

### 2.2 メソッド: `findRelatedProducts`

```java
/**
 * 商品詳細ページ向けの関連商品（お薦め）を取得する。
 *
 * @param sourceProductId 起点となる商品ID
 * @param now             現在日時（販売期間チェック用）
 * @return 関連商品リスト（最大4件、rank 昇順）
 */
List<ProductCardView> findRelatedProducts(long sourceProductId, OffsetDateTime now);
```

**SQL 概要（ProductMapper.xml）:**

```sql
SELECT
    p.product_id, p.product_name, p.variation_name,
    MIN(pv.unit_price) AS min_price,
    MAX(pv.unit_price) AS max_price,
    BOOL_OR(pv.stock_quantity > 0) AS in_stock,
    rrp.rank
FROM recommended_related_products rrp
JOIN products p ON p.product_id = rrp.recommended_product_id
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE rrp.source_product_id = #{sourceProductId}
  AND rrp.recommendation_date = (
      SELECT MAX(recommendation_date) FROM recommended_related_products
      WHERE source_product_id = #{sourceProductId}
  )
  AND (p.sale_start_at IS NULL OR p.sale_start_at <= #{now})
  AND (p.sale_end_at   IS NULL OR p.sale_end_at   >  #{now})
GROUP BY p.product_id, p.product_name, p.variation_name, rrp.rank
ORDER BY rrp.rank ASC
LIMIT 4
```

---

## 3. `ProductRepository` — 追加メソッド（セクションB）

### 3.1 メソッド: `findCrossSellProducts`

```java
/**
 * カート画面向けのクロスセル商品を取得する。
 *
 * @param sourceProductId  起点商品ID（最近見た商品の先頭）
 * @param excludeProductIds カート内既存商品IDリスト（除外対象）
 * @param now              現在日時
 * @return クロスセル商品リスト（最大4件、rank 昇順）
 */
List<ProductCardView> findCrossSellProducts(
    long sourceProductId,
    List<Long> excludeProductIds,
    OffsetDateTime now
);
```

**SQL 概要（ProductMapper.xml）:**

```sql
SELECT
    p.product_id, p.product_name, p.variation_name,
    MIN(pv.unit_price) AS min_price,
    MAX(pv.unit_price) AS max_price,
    BOOL_OR(pv.stock_quantity > 0) AS in_stock,
    rrp.rank
FROM recommended_related_products rrp
JOIN products p ON p.product_id = rrp.recommended_product_id
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE rrp.source_product_id = #{sourceProductId}
  AND rrp.recommendation_date = (
      SELECT MAX(recommendation_date) FROM recommended_related_products
      WHERE source_product_id = #{sourceProductId}
  )
  AND (p.sale_start_at IS NULL OR p.sale_start_at <= #{now})
  AND (p.sale_end_at   IS NULL OR p.sale_end_at   >  #{now})
  <if test="excludeProductIds != null and !excludeProductIds.isEmpty()">
  AND p.product_id NOT IN
      <foreach item="id" collection="excludeProductIds" open="(" separator="," close=")">
          #{id}
      </foreach>
  </if>
GROUP BY p.product_id, p.product_name, p.variation_name, rrp.rank
ORDER BY rrp.rank ASC
LIMIT 4
```

---

## 4. `ProductRepository` — 追加メソッド（セクションC）

### 4.1 メソッド: `findRecommendedBySourceIds`

```java
/**
 * 複数の起点商品IDから推薦商品を取得する（セクションC 補完ロジック共通）。
 *
 * @param sourceProductIds          起点商品IDリスト
 * @param alreadyCollectedIds       すでに収集済みのIDリスト（除外）
 * @param purchasedWithin3MonthsIds 直近3ヶ月購入済みIDリスト（除外）
 * @param now                       現在日時
 * @param limit                     最大取得件数
 * @return 推薦商品リスト
 */
List<ProductCardView> findRecommendedBySourceIds(
    List<Long> sourceProductIds,
    List<Long> alreadyCollectedIds,
    List<Long> purchasedWithin3MonthsIds,
    OffsetDateTime now,
    int limit
);
```

### 4.2 メソッド: `findTopRankedAsRecommendation`

```java
/**
 * 売れ筋ランキングから推薦補完用の商品を取得する。
 *
 * @param alreadyCollectedIds       すでに収集済みのIDリスト（除外）
 * @param purchasedWithin3MonthsIds 直近3ヶ月購入済みIDリスト（除外）
 * @param now                       現在日時
 * @param limit                     最大取得件数
 * @return 売れ筋商品リスト
 */
List<ProductCardView> findTopRankedAsRecommendation(
    List<Long> alreadyCollectedIds,
    List<Long> purchasedWithin3MonthsIds,
    OffsetDateTime now,
    int limit
);
```

---

## 5. `OrderRepository` — 追加メソッド

### 5.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/repository/OrderRepository.java`

### 5.2 メソッド: `findPurchasedProductIds`

```java
/**
 * 指定会員の購入履歴から商品IDリストを取得する。
 *
 * @param memberId  会員ID
 * @param from      集計開始日時（直近6ヶ月 = 現在 - 180日）
 * @return 購入済み商品IDリスト（重複あり）
 */
List<Long> findPurchasedProductIds(long memberId, OffsetDateTime from);
```

**SQL 概要（OrderMapper.xml）:**

```sql
SELECT DISTINCT p.product_id
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
JOIN product_variants pv ON pv.product_code = oi.product_code
JOIN products p ON p.product_id = pv.product_id
WHERE o.member_id = #{memberId}
  AND o.order_datetime >= #{from}
```

> **注意:** `order_items` は商品コードのスナップショットを保持する。`product_code` → `product_variants.product_code` → `products.product_id` と辿って product_id を解決する。

### 5.3 メソッド: `findPurchasedProductIdsWithin`

直近3ヶ月の除外リスト取得で同メソッドを流用する。`from = now - 90日` で呼び出す。

---

## 6. `ProductService` — 追加メソッド

### 6.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`

### 6.2 追加メソッド

```java
/**
 * 商品詳細ページ向けの関連商品を取得する。
 *
 * @param productId 表示中の商品ID
 * @return 関連商品リスト（最大4件。推薦なしの場合は空リスト）
 */
public List<ProductCardView> findRelatedProducts(long productId) {
    return repository.findRelatedProducts(productId, appTimeProvider.now());
}

/**
 * カート画面向けのクロスセル商品を取得する。
 *
 * @param sourceProductId  起点商品ID（最近見た商品の先頭）
 * @param cartProductIds   カート内既存商品IDリスト
 * @return クロスセル商品リスト（最大4件）
 */
public List<ProductCardView> findCrossSellProducts(long sourceProductId, List<Long> cartProductIds) {
    return repository.findCrossSellProducts(sourceProductId, cartProductIds, appTimeProvider.now());
}
```

---

## 7. `HomeController` の変更

### 7.1 DI 追加

`MemberSessionService` および `RecommendationService` を新たに注入する。

```java
// 既存
private final ProductService productService;
private final AnnouncementService announcementService;

// ★追加
private final MemberSessionService memberSessionService;
private final RecommendationService recommendationService;
```

### 7.2 `top()` メソッドの変更

```java
@GetMapping("/")
public String top(HttpSession session, HttpServletRequest request, Model model) {
    // 既存処理
    var topNewArrivals = productService.findTopNewArrivals();
    model.addAttribute("topNewArrivals", topNewArrivals);
    model.addAttribute("hasTopNewArrivals", !topNewArrivals.isEmpty());
    model.addAttribute("topRankedProducts", productService.findTopRankedProducts());

    // ★CUSTOM-001 追加
    memberSessionService.currentMember(session).ifPresent(member -> {
        List<Long> recentlyViewedIds = resolveRecentlyViewedIds(request); // 確認事項 D-01 参照
        List<ProductCardView> personalizedRecs =
            recommendationService.findTopPersonalizedRecommendations(member.memberId(), recentlyViewedIds);
        model.addAttribute("topPersonalizedRecommendations", personalizedRecs);
        model.addAttribute("hasTopPersonalizedRecommendations", !personalizedRecs.isEmpty());
    });

    return "pages/top";
}
```

---

## 8. `CatalogController` の変更

### 8.1 `productDetail()` メソッドへの追加

既存の `productDetail()` メソッドに以下を追加する。

```java
// ★CUSTOM-001 追加（お気に入り判定の後）
List<ProductCardView> relatedProducts = productService.findRelatedProducts(productId);
model.addAttribute("relatedProducts", relatedProducts);
model.addAttribute("hasRelatedProducts", !relatedProducts.isEmpty());
```

---

## 9. `CartController` の変更

### 9.1 `cart()` メソッドへの追加

既存の `cart()` メソッドに `ProductService` の DI を追加し、以下を追加する。

```java
// ★CUSTOM-001 追加（CartView 取得後）
List<Long> recentlyViewedIds = resolveRecentlyViewedIds(request); // 確認事項 D-01 参照
if (!recentlyViewedIds.isEmpty()) {
    List<Long> cartProductIds = cartView.items().stream()
        .map(item -> item.productId())
        .toList();
    List<ProductCardView> crossSellProducts =
        productService.findCrossSellProducts(recentlyViewedIds.get(0), cartProductIds);
    model.addAttribute("crossSellProducts", crossSellProducts);
    model.addAttribute("hasCrossSellProducts", !crossSellProducts.isEmpty());
} else {
    model.addAttribute("hasCrossSellProducts", false);
}
```

---

## 10. 最近見た商品IDの取得方法（設計確認事項 D-01）

> **本セクションは確認事項です。** 実装方式は `CUSTOM-001-design-kadaiList.md` の D-01 への回答後に確定する。

| 候補 | 概要 |
|-----|------|
| **案1: HTTP Cookie から直接読み取り** | JS が書き込んだ Cookie（例: `recently_viewed`）を `HttpServletRequest.getCookies()` で読み取り。サーバーサイドのみで完結するが、Cookie 書き込み実装が必要または既存実装の確認が必要 |
| **案2: セッション（HttpSession）経由** | 商品詳細ページ表示時に `CatalogController` がセッションへ保存済みのIDリストを読み取る。現行シーケンス図にも「最近見た商品リストを更新」の記述がある |

現行コードの `CatalogController.productDetail()` には Session への書き込みコメントがあるため、**案2（HttpSession 経由）** が現状の動作に近い可能性が高い。D-01 回答後に確定する。
