現状のコントローラ／サービス構成とテーブル設計候補を把握します。

調査して、既存の構造に沿った設計をまとめます。

既存の ProductDetailView と会員/注文関連の設計を確認します。

1. データベース設計
   追加テーブル
   product_review
   review_id : BIGSERIAL / BIGINT PK
   member_id : BIGINT NOT NULL
   product_id : BIGINT NOT NULL
   rating : SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5)
   title : VARCHAR(255) NULL
   body : TEXT NOT NULL
   published : BOOLEAN NOT NULL DEFAULT TRUE
   created_at : TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
   updated_at : TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
   制約
   UNIQUE(member_id, product_id) : 1会員1商品1レビュー
   FOREIGN KEY(member_id) → member 系テーブル
   FOREIGN KEY(product_id) → product 系テーブル
   INDEX(product_id)
   INDEX(member_id)
   追加参照用集計
   AVG(rating) : 平均評価
   COUNT(\*) : 件数
   WHERE published = TRUE で公開レビューのみカウント（要件に公開状態を含めるため）
2. バックエンド実装方針
   新規追加
   src/main/java/jp/co/skig/officeorder/mapper/ReviewMapper.java
   List<ProductReviewView> selectProductReviews(long productId, int limit, int offset)
   long countProductReviews(long productId)
   ReviewSummaryRow selectProductReviewSummary(long productId)
   Optional<ProductReviewView> selectMemberReview(long memberId, long productId)
   int insertProductReview(Map<String,Object> params)
   int updateProductReview(Map<String,Object> params)
   src/main/resources/mappers/ReviewMapper.xml
   SQL 定義
   selectProductReviews
   selectProductReviewSummary
   selectMemberReview
   insertProductReview
   updateProductReview
   src/main/java/jp/co/skig/officeorder/repository/ReviewRepository.java
   商品レビュー参照・保存の窓口
   List<ProductReviewView> findReviewsByProductId(long productId, int page, int size)
   long countReviewsByProductId(long productId)
   ReviewSummaryView findReviewSummary(long productId)
   Optional<ProductReviewView> findMemberReview(long memberId, long productId)
   void saveReview(ProductReviewForm form)
   void updateReview(ProductReviewForm form)
   src/main/java/jp/co/skig/officeorder/service/review/ReviewService.java
   レビュー表示・投稿・更新ロジック
   ProductReviewPage getProductReviews(long productId, int page, int size)
   ReviewSummaryView getReviewSummary(long productId)
   Optional<ProductReviewView> getMemberReview(long memberId, long productId)
   boolean canMemberReview(long memberId, long productId)
   void createOrUpdateReview(long memberId, long productId, ReviewForm form)
   既存修正
   OrderService.java
   または OrderRepository.java
   購入履歴判定の公開メソッドを追加
   例: boolean hasPurchasedProduct(long memberId, long productId)
   OrderMapper.java
   Long countMemberPurchasedProduct(@Param("memberId") long memberId, @Param("productId") long productId)
   List<OrderItemRow> selectMemberPurchasedProducts(...) など
   CatalogController.java
   productDetail() にレビュー情報を追加reviewSummary
   reviewPage
   memberReview
   isReviewEligible
   productDetail() のモデルへ追加して product-detail.html で表示可能にする
   新規コントローラ案
   src/main/java/jp/co/skig/officeorder/web/ReviewController.java
   @GetMapping("/products/{productId}/reviews/new")
   投稿フォーム表示
   @GetMapping("/products/{productId}/reviews/edit")
   既存レビュー編集フォーム表示
   @PostMapping("/products/{productId}/reviews")
   新規作成 / 更新処理
   @PostMapping("/products/{productId}/reviews/preview")
   必要ならプレビュー用（任意）
   モデル・フォーム
   src/main/java/jp/co/skig/officeorder/model/review/ReviewForm.java
   productId
   rating
   title
   body
   src/main/java/jp/co/skig/officeorder/model/review/ProductReviewView.java
   reviewId
   memberId
   memberName
   rating
   title
   body
   published
   createdAt
   updatedAt
   src/main/java/jp/co/skig/officeorder/model/review/ReviewSummaryView.java
   averageRating
   reviewCount
   src/main/java/jp/co/skig/officeorder/model/review/ProductReviewPage.java
   List<ProductReviewView> items
   long totalCount
   int page
   int size
3. フロントエンド実装方針
   修正するHTMLテンプレート
   product-detail.html
   商品詳細下にレビュー表示領域を追加平均評価
   レビュー件数
   評価スター表示
   レビュー一覧ブロックth:each="review : ${reviewPage.items}"
   review.title, review.body, review.rating, review.memberName, review.createdAt
   投稿・編集導線未ログイン時：/login?redirect=/products/{productId}
   ログイン済み且つ購入履歴あり且つ未投稿：レビューを書く
   投稿済み：レビューを編集する
   購入履歴なし：購入履歴のある商品でのみ投稿できます 表示
   新規テンプレート案
   src/main/resources/templates/pages/product-review-form.html
   レビュー入力フォーム
   rating：1〜5 のラジオ or セレクト
   title：任意
   body：必須 textarea
   hidden productId
   バリデーションエラー表示
   追加UI要素
   review summary平均評価 4.2
   レビュー 12 件
   review listタイトル / 本文 / 評価スター / 投稿者 / 投稿日
   action buttonレビューを書く
   編集する
   未ログイン時CTAログインしてレビューを書く
   購入履歴なしメッセージこの商品は購入済みの会員のみレビューできます
4. 実装を進める上での注意点
   購入履歴判定
   購入済み 判定は「注文確定済みの購入履歴」ベース
   未確定カートや注文入力中は対象外
   member_id / product_id の組み合わせで注文明細を検索する
   delivery 状態や order status が必要なら、order 側の既存ステータス判定ロジックに沿う
   1会員1商品1レビュー
   DB の UNIQUE(member_id, product_id) で強制
   サービス層も既存レビュー有無をチェックして create/update を分岐
   createOrUpdateReview() などで同一ロジックにまとめる
   認証と遷移
   未ログイン時の投稿ボタンは redirect 付きで login へ遷移
   ReviewController では MemberSessionService.currentMember(session) で会員取得
   member == null の場合は redirect:/login?redirect=...
   バリデーション
   rating : 必須、1〜5
   body : 必須、空文字禁止
   title : 任意、文字数上限
   フォーム送信時にエラーメッセージを表示
   公開状態
   published カラムを追加することで、将来的に管理者が非公開にできる
   現状は通常 TRUE で保存し、一覧や統計は公開レビューのみ集計
   既存構造との整合
   ProductDetailView を直接拡張するか、コントローラから別属性として reviewSummary, reviewPage, memberReview を追加するかはどちらでも可能
   既存 CatalogController.productDetail() にレビュー読み込み処理を追加するのが自然
   SQL / MyBatis のマッパー実装は既存の OrderMapper / ProductMapper と同じ設計方針に合わせる
   画面UX
   投稿済み会員には「レビューを編集」導線を明確に
   レビュー一覧は最新順またはスター順をユーザーに判断できるように
   平均評価は小数1桁程度で表示し、星評価 UI を併用
   まとめ
   最小構成
   product_review テーブル追加
   ReviewMapper, ReviewRepository, ReviewService の新規追加
   OrderMapper / OrderRepository に購入履歴判定メソッド追加
   CatalogController または新規 ReviewController にレビュー表示・投稿処理追加
   product-detail.html にレビュー UI を追加
   必要なら product-review-form.html を追加
   この設計なら、既存の商品詳細表示の延長としてレビュー機能を自然に実装できます。
