# FEAT-003 商品レビュー機能 実装計画 TODO リスト

> 要件定義書: `docs/issues/FEAT-003-requirements.md`
> 設計書: `docs/design/FEAT-003-*.md`
> 実装ブランチ: `ebihara.toshikazu`

---

## TODO リスト

### Phase 1: DB スキーマ

- [x] **1-1. `sql/schema/reviews.sql` を新規作成する**
  - `reviews` テーブルの DDL（DB 設計書 §1.1）
  - カラム: `review_id`, `member_id`, `product_id`, `rating`, `title`, `body`, `is_published`, `is_blocked`, `created_at`, `updated_at`
  - 制約: PK, FK(members), FK(products), UNIQUE(member_id, product_id), CHECK(rating BETWEEN 1 AND 5)
  - インデックス: `idx_reviews_product_created` ON `(product_id, created_at DESC)`

---

### Phase 2: MapperRow（マッピング用 record）

- [x] **2-1. `ReviewMapperRow` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/mapper/row/ReviewMapperRow.java`（新規）
  - record: `reviewId`, `memberId`, `productId`, `rating`, `title`, `body`, `isPublished`, `isBlocked`, `createdAt`, `updatedAt`

- [x] **2-2. `ReviewSummaryMapperRow` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/mapper/row/ReviewSummaryMapperRow.java`（新規）
  - record: `productId`, `reviewCount`, `averageRating`

---

### Phase 3: Mapper インターフェース＋ XML

- [x] **3-1. `ReviewMapper` インターフェースを新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/mapper/ReviewMapper.java`（新規）
  - メソッド（クラス設計書 §3.5）:
    - `selectReviewSummary(@Param productId)`
    - `selectReviewsByProduct(@Param productId, @Param limit, @Param offset)`
    - `selectByMemberAndProduct(@Param memberId, @Param productId)`
    - `selectById(@Param reviewId)`
    - `existsPurchase(@Param memberId, @Param productId)`
    - `insertReview(...)`
    - `updateReview(...)`
    - `deleteReview(@Param reviewId)`
    - `selectReviewSummaries(@Param productIds)`
    - `countByProduct(@Param productId)`

- [x] **3-2. `ReviewMapper.xml` を新規作成する**
  - 対象ファイル: `src/main/resources/mappers/ReviewMapper.xml`（新規）
  - resultMap: `ReviewRowMap`, `ReviewSummaryRowMap`
  - SQL 定義（DB 設計書 §2, §3 参照）:
    - `existsPurchase`: orders + order_items + products JOIN（`order_status != 'cancelled'`）
    - `selectReviewSummary`: AVG(rating), COUNT(*) WHERE is_published = true
    - `selectReviewsByProduct`: 最新順・公開のみ・LIMIT/OFFSET
    - `selectByMemberAndProduct`: UNIQUE キーで 1 件（公開・非公開問わず）
    - `selectById`: PK で 1 件
    - `insertReview`: INSERT INTO reviews
    - `updateReview`: UPDATE reviews SET ... WHERE review_id = #{reviewId}、updated_at = now()
    - `deleteReview`: DELETE FROM reviews WHERE review_id = #{reviewId}
    - `selectReviewSummaries`: IN 句で一括集計
    - `countByProduct`: 公開レビュー件数

---

### Phase 4: Model 層

- [x] **4-1. `ReviewForm` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/review/ReviewForm.java`（新規）
  - JavaBean（Serializable）、既存 ContactForm と同パターン
  - フィールド: `rating`(@NotNull @Min(1) @Max(5)), `title`(@Size(max=100)), `body`(@NotBlank @Size(max=1000)), `published`(boolean, default true)
  - getter/setter

- [x] **4-2. `ReviewView` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/review/ReviewView.java`（新規）
  - record: `reviewId`, `memberId`, `productId`, `rating`, `title`, `body`, `published`, `blocked`, `createdAt`, `updatedAt`

- [x] **4-3. `ReviewSummaryView` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/review/ReviewSummaryView.java`（新規）
  - record: `productId`, `reviewCount`, `averageRating`（BigDecimal, 小数点 1 桁）

- [x] **4-4. `ReviewListResponse` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/review/ReviewListResponse.java`（新規）
  - record: `items`(List\<ReviewView\>), `hasNext`(boolean)

- [x] **4-5. `ProductCardView` に `reviewCount`, `averageRating` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductCardView.java`
  - record にフィールド追加 → 生成箇所（ProductRepository 内）をすべて更新

---

### Phase 5: Repository 層

- [x] **5-1. `ReviewRepository` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ReviewRepository.java`（新規）
  - ReviewMapper に依存
  - メソッド（クラス設計書 §3.4）: `findSummary`, `findByProduct`, `findByMemberAndProduct`, `findById`, `existsPurchase`, `insert`, `update`, `delete`, `findSummaries`, `countByProduct`
  - MapperRow → View 変換ロジックを実装

- [x] **5-2. `ProductRepository` の商品一覧取得にレビュー集計を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
  - ProductCardView 生成時に reviewCount, averageRating をセット
  - ProductMapper.xml の商品一覧クエリにレビュー集計サブクエリを LEFT JOIN で追加

---

### Phase 6: Service 層

- [x] **6-1. `ReviewService` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/review/ReviewService.java`（新規）
  - @Service、メソッド単位で @Transactional
  - メソッド（クラス設計書 §3.3）:
    - `findReviewSummary(productId)` → ReviewSummaryView
    - `findReviews(productId, page, size)` → ReviewListResponse（limit+1 取得で hasNext 判定）
    - `findMyReview(memberId, productId)` → Optional\<ReviewView\>
    - `hasPurchased(memberId, productId)` → boolean
    - `postReview(memberId, productId, form)` → void（既存レビュー有無で INSERT/UPDATE 分岐）
    - `updateReview(memberId, reviewId, form)` → void（本人確認＋ブロック確認）
    - `deleteReview(memberId, reviewId)` → void（本人確認＋ブロック確認）
    - `findReviewSummaries(productIds)` → Map\<Long, ReviewSummaryView\>

---

### Phase 7: Controller 層

- [x] **7-1. `ReviewController` を新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/ReviewController.java`（新規）
  - @Controller、依存: ReviewService, MemberSessionService
  - エンドポイント（API 設計書 §1 参照）:
    - `POST /products/{productId}/reviews` → 投稿（@Valid + BindingResult → PRG）
    - `POST /products/{productId}/reviews/{reviewId}` → 更新
    - `POST /products/{productId}/reviews/{reviewId}/delete` → 削除
    - `GET /products/{productId}/reviews` → もっと見る（JSON: @ResponseBody）
  - 未ログイン時: `/login?redirect=...` へリダイレクト（お気に入りトグルと同パターン）

- [x] **7-2. `CatalogController.productDetail()` を拡張する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - ReviewService を DI に追加
  - モデル属性追加（API 設計書 §1.5）: `reviewSummary`, `reviews`, `hasMoreReviews`, `canPostReview`, `hasPurchased`, `myReview`, `reviewForm`
  - ログイン状態に応じて購入履歴判定・投稿済みレビュー取得を分岐

---

### Phase 8: Mapper XML（商品一覧クエリ拡張）

- [x] **8-1. `ProductMapper.xml` の商品一覧クエリにレビュー集計を追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 商品一覧の SELECT 句にレビュー集計サブクエリを LEFT JOIN
  - `review_count`, `average_rating` カラムを追加
  - `ProductListMapperRow` に対応フィールドを追加

---

### Phase 9: テンプレート

- [x] **9-1. `fragments/review/review-list.html` を新規作成する**
  - レビュー一覧フラグメント（星表示、タイトル、本文、投稿日時）
  - タイトル未入力時はタイトル行を非表示
  - 「もっと見る」ボタン（hasMoreReviews で表示制御）

- [x] **9-2. `fragments/review/review-form.html` を新規作成する**
  - インライン投稿/編集フォームフラグメント
  - 評価（星クリック選択 + hidden input）、タイトル、本文（文字数カウンター）、公開状態チェックボックス
  - 新規投稿 / 編集（既存値プリセット）/ 削除の action 分岐
  - バリデーションエラー表示

- [x] **9-3. `pages/product-detail.html` にレビューセクションを追加する**
  - 対象ファイル: `src/main/resources/templates/pages/product-detail.html`
  - 関連商品セクションの直前に挿入（画面設計書 §1.1）
  - サマリー表示（平均評価・件数）
  - 投稿/編集ボタン表示条件分岐（画面設計書 §1.3）
  - review-list / review-form フラグメントの include

- [x] **9-4. `fragments/common/product-card.html` に平均評価・件数を追加する**
  - 対象ファイル: `src/main/resources/templates/fragments/common/product-card.html`
  - 価格行の下に「★ 4.2 (12)」行を追加
  - レビュー 0 件時は非表示

---

### Phase 10: JavaScript / CSS

- [x] **10-1. `js/review.js` を新規作成する**
  - 「もっと見る」の非同期取得（fetch → DOM 追加）
  - 星評価クリック選択（hidden input 連動）
  - 本文の文字数カウンター
  - 削除確認ダイアログ（confirm）
  - フォーム展開/非表示切替

- [x] **10-2. レビュー関連の CSS を追加する**
  - 星評価表示（塗りつぶし表現）
  - レビュー一覧・フォームのスタイリング

---

### Phase 11: 単体テスト

- [x] **11-1. `ReviewServiceTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/review/ReviewServiceTest.java`（新規）
  - Mockito で ReviewRepository をモック
  - テスト対象:
    - `findReviewSummary`: レビューあり/なしの場合
    - `findReviews`: hasNext 判定（limit+1 パターン）
    - `hasPurchased`: true/false
    - `postReview`: 新規投稿（INSERT）/既存更新（UPDATE）の分岐
    - `updateReview`: 本人確認、ブロック確認、正常更新
    - `deleteReview`: 本人確認、ブロック確認、正常削除

- [x] **11-2. `ReviewRepositoryTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/repository/ReviewRepositoryTest.java`（新規）
  - Mockito で ReviewMapper をモック
  - MapperRow → View 変換ロジックの検証
  - findSummary: レビューなし時のデフォルト値検証

- [x] **11-3. `ReviewControllerTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/web/ReviewControllerTest.java`（新規）
  - @WebMvcTest + MockMvc
  - テスト対象:
    - 未ログイン時のリダイレクト
    - バリデーションエラー時のフォーム再表示
    - 投稿成功時のリダイレクト
    - もっと見るの JSON レスポンス

- [x] **11-4. `ReviewFormTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/model/review/ReviewFormTest.java`（新規）
  - Bean Validation のテスト
  - rating: null/0/6 → エラー、1〜5 → OK
  - body: null/空/1001 文字 → エラー、1〜1000 文字 → OK
  - title: 101 文字 → エラー、null/100 文字 → OK

---

## 実装順序（依存関係に基づく推奨順）

```
Phase 1 (DB)
    ↓
Phase 2 (MapperRow)
    ↓
Phase 3 (Mapper IF + XML)
    ↓
Phase 4 (Model)
    ↓
Phase 5 (Repository)
    ↓
Phase 6 (Service)
    ↓
Phase 7 (Controller) + Phase 8 (ProductMapper 拡張)
    ↓
Phase 9 (Template) + Phase 10 (JS/CSS)
    ↓
Phase 11 (Unit Tests) ←── 各 Phase 完了後に随時追加可
```

---

## 変更ファイル一覧

| Phase | ファイルパス | 種別 |
|-------|------------|------|
| 1 | `sql/schema/reviews.sql` | **新規** |
| 2 | `src/main/java/.../mapper/row/ReviewMapperRow.java` | **新規** |
| 2 | `src/main/java/.../mapper/row/ReviewSummaryMapperRow.java` | **新規** |
| 3 | `src/main/java/.../mapper/ReviewMapper.java` | **新規** |
| 3 | `src/main/resources/mappers/ReviewMapper.xml` | **新規** |
| 4 | `src/main/java/.../model/review/ReviewForm.java` | **新規** |
| 4 | `src/main/java/.../model/review/ReviewView.java` | **新規** |
| 4 | `src/main/java/.../model/review/ReviewSummaryView.java` | **新規** |
| 4 | `src/main/java/.../model/review/ReviewListResponse.java` | **新規** |
| 4 | `src/main/java/.../model/product/ProductCardView.java` | 変更 |
| 5 | `src/main/java/.../repository/ReviewRepository.java` | **新規** |
| 5 | `src/main/java/.../repository/ProductRepository.java` | 変更 |
| 6 | `src/main/java/.../service/review/ReviewService.java` | **新規** |
| 7 | `src/main/java/.../web/ReviewController.java` | **新規** |
| 7 | `src/main/java/.../web/CatalogController.java` | 変更 |
| 8 | `src/main/resources/mappers/ProductMapper.xml` | 変更 |
| 8 | `src/main/java/.../mapper/row/ProductListMapperRow.java` | 変更 |
| 9 | `src/main/resources/templates/fragments/review/review-list.html` | **新規** |
| 9 | `src/main/resources/templates/fragments/review/review-form.html` | **新規** |
| 9 | `src/main/resources/templates/pages/product-detail.html` | 変更 |
| 9 | `src/main/resources/templates/fragments/common/product-card.html` | 変更 |
| 10 | `src/main/resources/static/js/review.js` | **新規** |
| 10 | `src/main/resources/static/css/` (該当ファイル) | 変更 |
| 11 | `src/test/java/.../service/review/ReviewServiceTest.java` | **新規** |
| 11 | `src/test/java/.../repository/ReviewRepositoryTest.java` | **新規** |
| 11 | `src/test/java/.../web/ReviewControllerTest.java` | **新規** |
| 11 | `src/test/java/.../model/review/ReviewFormTest.java` | **新規** |
