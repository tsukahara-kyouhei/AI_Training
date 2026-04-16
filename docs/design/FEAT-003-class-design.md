# FEAT-003: クラス設計書

## 1. レイヤー構成

既存アーキテクチャ（Controller → Service → Repository → Mapper）に準拠する。

```
CatalogController (既存拡張)
ReviewController (新規)
    ↓
ReviewService (新規)
    ↓
ReviewRepository (新規)
    ↓
ReviewMapper (新規: Interface + XML)
```

## 2. 新規クラス一覧

### 2.1 Web 層

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `ReviewController` | `web` | レビュー投稿/更新/削除/一覧取得エンドポイント |

### 2.2 Service 層

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `ReviewService` | `service.review` | レビューの CRUD ビジネスロジック、購入履歴判定、権限チェック |

### 2.3 Repository 層

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `ReviewRepository` | `repository` | ReviewMapper を呼び出し、MapperRow → View 変換 |

### 2.4 Mapper 層

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `ReviewMapper` | `mapper` | MyBatis マッパーインターフェース |
| `ReviewMapper.xml` | `resources/mappers` | SQL 定義 |
| `ReviewMapperRow` | `mapper.row` | レビュー取得結果のマッピング行 |
| `ReviewSummaryMapperRow` | `mapper.row` | 集計結果（平均評価・件数）のマッピング行 |

### 2.5 Model 層

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `ReviewForm` | `model.review` | 投稿/編集フォーム（Bean Validation 付き） |
| `ReviewView` | `model.review` | レビュー表示用ビューモデル |
| `ReviewSummaryView` | `model.review` | 平均評価・件数の表示用ビューモデル |
| `ReviewListResponse` | `model.review` | もっと見る API のレスポンス |

## 3. クラス詳細

### 3.1 ReviewController

```
@Controller
場所: jp.co.skig.officeorder.web.ReviewController
依存: ReviewService, MemberSessionService
```

| メソッド | エンドポイント | 処理概要 |
|----------|---------------|----------|
| `postReview(productId, reviewForm, bindingResult, session, redirectAttributes)` | `POST /products/{productId}/reviews` | バリデーション → 投稿/更新 → リダイレクト |
| `updateReview(productId, reviewId, reviewForm, bindingResult, session, redirectAttributes)` | `POST /products/{productId}/reviews/{reviewId}` | 本人＋未ブロック確認 → 更新 → リダイレクト |
| `deleteReview(productId, reviewId, session, redirectAttributes)` | `POST /products/{productId}/reviews/{reviewId}/delete` | 本人＋未ブロック確認 → 削除 → リダイレクト |
| `moreReviews(productId, page, size)` | `GET /products/{productId}/reviews` | JSON で追加レビュー返却 |

未ログイン時: `memberSessionService.currentMember(session)` が空なら `/login?redirect=...` へリダイレクト（既存のお気に入りトグルと同パターン）。

### 3.2 CatalogController 拡張

既存の `productDetail()` メソッドに以下を追加。

```
- ReviewService.findReviewSummary(productId) → model: reviewSummary
- ReviewService.findReviews(productId, page=1, size=5) → model: reviews, hasMoreReviews
- ReviewService.hasPurchased(memberId, productId) → model: hasPurchased, canPostReview
- ReviewService.findMyReview(memberId, productId) → model: myReview
- new ReviewForm() or 既存レビューから復元 → model: reviewForm
```

### 3.3 ReviewService

```
@Service
場所: jp.co.skig.officeorder.service.review.ReviewService
依存: ReviewRepository
アノテーション: メソッド単位で @Transactional
```

| メソッド | 引数 | 戻り値 | 処理概要 |
|----------|------|--------|----------|
| `findReviewSummary(productId)` | long | `ReviewSummaryView` | 公開レビューの平均評価・件数を返却 |
| `findReviews(productId, page, size)` | long, int, int | `ReviewListResponse` | 公開レビュー一覧（最新順）＋ hasNext 判定 |
| `findMyReview(memberId, productId)` | long, long | `Optional<ReviewView>` | 会員の投稿済みレビューを返却 |
| `hasPurchased(memberId, productId)` | long, long | `boolean` | 購入履歴の有無を判定 |
| `postReview(memberId, productId, form)` | long, long, ReviewForm | `void` | UPSERT（UNIQUE 制約活用） |
| `updateReview(memberId, reviewId, form)` | long, long, ReviewForm | `void` | 本人＋未ブロック確認後に更新 |
| `deleteReview(memberId, reviewId)` | long, long | `void` | 本人＋未ブロック確認後に削除 |
| `findReviewSummaries(productIds)` | List\<Long\> | `Map<Long, ReviewSummaryView>` | 商品一覧用の一括集計取得 |

### 3.4 ReviewRepository

```
場所: jp.co.skig.officeorder.repository.ReviewRepository
依存: ReviewMapper
```

| メソッド | 処理概要 |
|----------|----------|
| `findSummary(productId)` | `reviewMapper.selectReviewSummary` → `ReviewSummaryView` 変換 |
| `findByProduct(productId, limit, offset)` | `reviewMapper.selectReviewsByProduct` → `List<ReviewView>` 変換 |
| `findByMemberAndProduct(memberId, productId)` | 会員の投稿済みレビュー取得 |
| `findById(reviewId)` | ID でレビュー 1 件取得 |
| `existsPurchase(memberId, productId)` | 購入履歴判定 |
| `insert(memberId, productId, rating, title, body, isPublished)` | レビュー挿入 |
| `update(reviewId, rating, title, body, isPublished)` | レビュー更新 |
| `delete(reviewId)` | レビュー削除 |
| `findSummaries(productIds)` | 商品 ID リストに対する一括集計 |
| `countByProduct(productId)` | 公開レビュー件数 |

### 3.5 ReviewMapper

```
場所: jp.co.skig.officeorder.mapper.ReviewMapper
SQL: src/main/resources/mappers/ReviewMapper.xml
```

| メソッド | SQL 概要 |
|----------|----------|
| `selectReviewSummary(@Param productId)` | `AVG(rating)`, `COUNT(*)` WHERE `is_published = true` |
| `selectReviewsByProduct(@Param productId, @Param limit, @Param offset)` | 最新順・公開のみ |
| `selectByMemberAndProduct(@Param memberId, @Param productId)` | UNIQUE キーで 1 件取得 |
| `selectById(@Param reviewId)` | PK で 1 件取得 |
| `existsPurchase(@Param memberId, @Param productId)` | orders + order_items + products JOIN |
| `insertReview(...)` | INSERT |
| `updateReview(...)` | UPDATE（updated_at = now()） |
| `deleteReview(@Param reviewId)` | DELETE |
| `selectReviewSummaries(@Param productIds)` | IN 句で一括集計 |
| `countByProduct(@Param productId)` | 公開件数 |

### 3.6 Model クラス

#### ReviewForm

```java
// jp.co.skig.officeorder.model.review.ReviewForm
public class ReviewForm implements Serializable {
    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 100)
    private String title;

    @NotBlank @Size(max = 1000)
    private String body;

    private boolean published = true;  // デフォルト公開
}
```

#### ReviewView (record)

```java
record ReviewView(
    long reviewId,
    long memberId,
    long productId,
    int rating,
    String title,
    String body,
    boolean published,
    boolean blocked,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
)
```

#### ReviewSummaryView (record)

```java
record ReviewSummaryView(
    long productId,
    int reviewCount,
    BigDecimal averageRating  // 小数点1桁で丸め
)
```

#### ReviewListResponse (record)

```java
record ReviewListResponse(
    List<ReviewView> items,
    boolean hasNext
)
```

### 3.7 MapperRow クラス

#### ReviewMapperRow (record)

```java
record ReviewMapperRow(
    long reviewId, long memberId, long productId,
    int rating, String title, String body,
    boolean isPublished, boolean isBlocked,
    LocalDateTime createdAt, LocalDateTime updatedAt
)
```

#### ReviewSummaryMapperRow (record)

```java
record ReviewSummaryMapperRow(
    long productId, int reviewCount, BigDecimal averageRating
)
```

## 4. 既存クラスの変更

| クラス | 変更内容 |
|--------|----------|
| `CatalogController` | `productDetail()` にレビュー関連のモデル属性追加 |
| `ProductCardView` | `reviewCount`, `averageRating` フィールド追加 |
| `ProductRepository` | 商品一覧取得時にレビュー集計を JOIN する処理追加 |
| `ProductMapper.xml` | 商品一覧クエリにレビュー集計サブクエリ追加 |
| `SecurityConfig` | 変更なし（管理者画面はスコープ外） |
