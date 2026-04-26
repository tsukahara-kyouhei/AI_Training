# シーケンス図

> 最終更新: 2026-04-18（★CUSTOM-001 お薦め機能対応）

## 凡例

| 略称 | 対応コンポーネント |
|------|-----------------|
| Browser | ブラウザ（ユーザー操作） |
| Controller | Spring MVC Controller 層 |
| Service | ビジネスロジック層（Service クラス） |
| Repository | データアクセス層（Repository / MyBatis Mapper） |
| DB | PostgreSQL |
| Session | HTTP セッション / Cookie |
| SpringSec | Spring Security フィルタチェーン |

---

## 1. 共通・静的コンテンツ

### 1-1. トップ画面（GET /）

```mermaid
sequenceDiagram
    participant Browser
    participant HomeController
    participant ProductService
    participant RecommendationService
    participant MemberSessionService
    participant Repository
    participant DB
    participant Session

    Browser->>HomeController: GET /
    HomeController->>ProductService: findTopNewArrivals()
    ProductService->>Repository: SELECT 新着商品（上位N件）
    Repository->>DB: SQL実行
    DB-->>Repository: 商品リスト
    Repository-->>ProductService: 商品リスト
    ProductService-->>HomeController: 新着商品リスト

    HomeController->>ProductService: findTopRankedProducts()
    ProductService->>Repository: SELECT ランキング商品
    Repository->>DB: SQL実行
    DB-->>Repository: 商品リスト
    Repository-->>ProductService: 商品リスト
    ProductService-->>HomeController: ランキング商品リスト

    Note over HomeController,MemberSessionService: ★CUSTOM-001 「超マジお薦め」セクション（フェーズ1）
    HomeController->>MemberSessionService: currentMember(session)
    MemberSessionService-->>HomeController: MemberSessionUser または empty

    alt ログイン中の会員
        HomeController->>Session: 最近見た商品IDリストを取得（設計確認事項–D-01参照）
        Session-->>HomeController: recentlyViewedProductIds
        HomeController->>RecommendationService: findTopPersonalizedRecommendations(memberId, recentlyViewedProductIds)
        RecommendationService->>Repository: SELECT orders/order_items（直近6ヶ月の購入商品）
        Repository->>DB: SQL実行
        DB-->>Repository: 購入商品IDリスト
        Repository-->>RecommendationService: 購入商品IDリスト
        RecommendationService->>Repository: SELECT recommended_related_products（購入商品起点・直近3ヶ月購入済を除外）
        Repository->>DB: SQL実行
        DB-->>Repository: 推薦候補リスト
        Repository-->>RecommendationService: 推薦候補リスト
        opt 候補〇8件未満
            RecommendationService->>Repository: SELECT recommended_related_products（最近見た商品起点）
            Repository->>DB: SQL実行
            DB-->>Repository: 追加推薦候補
            Repository-->>RecommendationService: 追加推薦候補
        end
        opt まだ〇8件未満
            RecommendationService->>Repository: SELECT popular_product_rankings（売れ筋上位）
            Repository->>DB: SQL実行
            DB-->>Repository: 売れ筋商品リスト
            Repository-->>RecommendationService: 売れ筋商品リスト
        end
        RecommendationService-->>HomeController: 推薦商品リスト（重複排除・販売期間外除外・最大8件）
    end

    HomeController-->>Browser: HTML（pages/top）
```

---

### 1-2. お知らせ一覧（GET /announcements）

```mermaid
sequenceDiagram
    participant Browser
    participant HomeController
    participant AnnouncementService
    participant Repository
    participant DB

    Browser->>HomeController: GET /announcements
    HomeController->>AnnouncementService: findAnnouncementList()
    AnnouncementService->>Repository: SELECT announcements
    Repository->>DB: SQL実行
    DB-->>Repository: お知らせリスト
    Repository-->>AnnouncementService: お知らせリスト
    AnnouncementService-->>HomeController: お知らせリスト
    HomeController-->>Browser: HTML（pages/announcements）
```

---

### 1-8. お問い合わせフォーム表示（GET /contact）

```mermaid
sequenceDiagram
    participant Browser
    participant ContactController
    participant MemberSessionService
    participant ContactService

    Browser->>ContactController: GET /contact
    ContactController->>MemberSessionService: currentMember(session)
    MemberSessionService-->>ContactController: MemberSessionUser または empty
    ContactController->>ContactService: createInitialForm(member)
    Note right of ContactService: ログイン中は名前・メールを自動補完
    ContactService-->>ContactController: ContactForm
    ContactController-->>Browser: HTML（pages/contact）
```

---

### 1-8b. お問い合わせ送信（POST /contact）

```mermaid
sequenceDiagram
    participant Browser
    participant ContactController
    participant MemberSessionService
    participant ContactService
    participant Repository
    participant DB

    Browser->>ContactController: POST /contact（入力フォーム）
    ContactController->>ContactController: Bean Validation
    alt バリデーション NG
        ContactController-->>Browser: HTML（pages/contact）エラー表示
    else バリデーション OK
        ContactController->>MemberSessionService: currentMember(session)
        MemberSessionService-->>ContactController: memberId または null
        ContactController->>ContactService: submit(memberId, form)
        ContactService->>Repository: INSERT inquiry
        Repository->>DB: SQL実行
        DB-->>Repository: inquiryId
        Repository-->>ContactService: inquiryId
        ContactService-->>ContactController: inquiryId
        ContactController-->>Browser: redirect:/contact（フラッシュ：受付完了）
    end
```

---

## 2. 商品カタログ

### 2-1. 新着商品一覧（GET /products/new-arrivals）

```mermaid
sequenceDiagram
    participant Browser
    participant CatalogController
    participant FilterOptionSvc as ProductFilterOptionService
    participant SearchSvc as ProductListSearchService
    participant Repository
    participant DB

    Browser->>CatalogController: GET /products/new-arrivals
    CatalogController->>FilterOptionSvc: loadOptionsBundle()
    FilterOptionSvc->>Repository: SELECT 絞り込み選択肢（色・価格帯等）
    Repository->>DB: SQL実行
    DB-->>Repository: 選択肢リスト
    Repository-->>FilterOptionSvc: 選択肢リスト
    FilterOptionSvc-->>CatalogController: ProductFilterOptionsBundle

    CatalogController->>SearchSvc: buildNewArrivalCondition(..., optionsBundle)
    SearchSvc-->>CatalogController: ProductSearchCondition

    CatalogController->>SearchSvc: searchWithPageCorrection(condition)
    SearchSvc->>Repository: countProducts / selectProducts
    Repository->>DB: SQL実行
    DB-->>Repository: 件数・商品リスト
    Repository-->>SearchSvc: 件数・商品リスト
    SearchSvc-->>CatalogController: ProductListSearchResult

    CatalogController-->>Browser: HTML（pages/product-list-new-arrivals）
```

---

### 2-2. キーワード検索結果（GET /products/search）★FEAT-001

```mermaid
sequenceDiagram
    participant Browser
    participant CatalogController
    participant FilterOptionSvc as ProductFilterOptionService
    participant SearchSvc as ProductListSearchService
    participant Normalizer as SearchKeywordNormalizer
    participant Repository
    participant DB

    Browser->>CatalogController: GET /products/search?q=...&taste=...
    CatalogController->>FilterOptionSvc: loadOptionsBundle()
    FilterOptionSvc->>Repository: SELECT テイスト・色・価格帯マスタ（全カテゴリ）
    Repository->>DB: SQL実行
    DB-->>Repository: 選択肢リスト
    Repository-->>FilterOptionSvc: 選択肢リスト
    FilterOptionSvc-->>CatalogController: ProductFilterOptionsBundle

    CatalogController->>SearchSvc: buildCondition(keyword, tasteNames, ...)
    SearchSvc->>Normalizer: normalize(keyword)
    Note right of Normalizer: 全角→半角・大文字→小文字変換
    Normalizer-->>SearchSvc: 正規化済みキーワード
    Note right of SearchSvc: 桁数で完全一致/前方一致を自動判定
    SearchSvc-->>CatalogController: ProductSearchCondition（tasteNames含む）

    CatalogController->>SearchSvc: searchWithPageCorrection(condition)
    SearchSvc->>Repository: countProducts / selectProducts
    Repository->>DB: SQL実行（キーワード・テイスト・絞り込み条件適用）
    DB-->>Repository: 件数・商品リスト
    Repository-->>SearchSvc: 件数・商品リスト
    SearchSvc-->>CatalogController: ProductListSearchResult

    CatalogController-->>Browser: HTML（pages/product-list-search-results）
```

---

### 2-3〜2-5. カテゴリ一覧（GET /categories/{desks|chairs|storages}）

> デスクを例として記載。チェア・収納家具も同構造。

```mermaid
sequenceDiagram
    participant Browser
    participant CatalogController
    participant FilterOptionSvc as ProductFilterOptionService
    participant SearchSvc as ProductListSearchService
    participant Repository
    participant DB

    Browser->>CatalogController: GET /categories/desks（絞り込みパラメータ含む）
    CatalogController->>FilterOptionSvc: loadOptionsBundle()
    FilterOptionSvc->>Repository: SELECT 絞り込み選択肢
    Repository->>DB: SQL実行
    DB-->>Repository: 選択肢リスト
    Repository-->>FilterOptionSvc: 選択肢リスト
    FilterOptionSvc-->>CatalogController: ProductFilterOptionsBundle

    CatalogController->>FilterOptionSvc: buildDeskFilter(rawParams, optionsBundle)
    FilterOptionSvc-->>CatalogController: ProductCategoryFilter

    CatalogController->>SearchSvc: buildCondition(categoryId, deskFilter, ...)
    SearchSvc-->>CatalogController: ProductSearchCondition

    CatalogController->>SearchSvc: searchWithPageCorrection(condition)
    SearchSvc->>Repository: countProducts / selectProducts
    Repository->>DB: SQL実行（デスク属性・テイスト・絞り込み適用）
    DB-->>Repository: 件数・商品リスト
    Repository-->>SearchSvc: 件数・商品リスト
    SearchSvc-->>CatalogController: ProductListSearchResult

    CatalogController-->>Browser: HTML（pages/product-list-category-desk）
```

---

### 2-6. 商品詳細（GET /products/{productId}）

```mermaid
sequenceDiagram
    participant Browser
    participant CatalogController
    participant ProductService
    participant MemberService
    participant MemberSessionService
    participant Session
    participant Repository
    participant DB

    Browser->>CatalogController: GET /products/{productId}
    CatalogController->>ProductService: findDetail(productId, forceOutOfStock)
    ProductService->>Repository: SELECT 商品・バリアント・属性
    Repository->>DB: SQL実行
    DB-->>Repository: 商品詳細
    Repository-->>ProductService: 商品詳細
    ProductService-->>CatalogController: ProductDetailView（存在しない場合は404）

    CatalogController->>Session: 最近見た商品リストを更新
    CatalogController->>MemberSessionService: currentMember(session)
    MemberSessionService-->>CatalogController: MemberSessionUser または empty

    alt ログイン中
        CatalogController->>MemberService: isFavorite(memberId, productId)
        MemberService->>Repository: SELECT お気に入り有無
        Repository->>DB: SQL実行
        DB-->>Repository: boolean
        Repository-->>MemberService: boolean
        MemberService-->>CatalogController: isFavorite
    end

    Note over CatalogController,Repository: ★CUSTOM-001 関連商品セクション（フェーズ1）
    CatalogController->>ProductService: findRelatedProducts(productId)
    ProductService->>Repository: SELECT recommended_related_products（最新推薦日・販売期間チェック）
    Repository->>DB: SQL実行
    DB-->>Repository: 関連商品リスト（0〜4件）
    Repository-->>ProductService: 関連商品リスト
    ProductService-->>CatalogController: 関連商品リスト（0件の場合はセクション非表示）

    CatalogController-->>Browser: HTML（pages/product-detail）
```

---

### 2-7. お気に入りトグル（POST /products/{productId}/favorite）

```mermaid
sequenceDiagram
    participant Browser
    participant CatalogController
    participant ProductService
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>CatalogController: POST /products/{productId}/favorite
    CatalogController->>ProductService: findDetail(productId, forceOutOfStock)
    ProductService->>Repository: SELECT 商品（存在確認）
    Repository->>DB: SQL実行
    DB-->>Repository: 商品（存在しない場合は404）
    Repository-->>ProductService: 商品
    ProductService-->>CatalogController: ProductDetailView

    CatalogController->>MemberSessionService: currentMember(session)
    MemberSessionService-->>CatalogController: MemberSessionUser または empty

    alt 未ログイン
        CatalogController-->>Browser: redirect:/login?redirect=元ページ
    else ログイン中
        CatalogController->>MemberService: toggleFavorite / removeFavorite
        MemberService->>Repository: INSERT/DELETE member_favorites
        Repository->>DB: SQL実行
        DB-->>Repository: 結果
        Repository-->>MemberService: 結果
        MemberService-->>CatalogController: 完了（上限超過時は例外）
        CatalogController-->>Browser: redirect:元ページ（上限超過時はフラッシュエラー付き）
    end
```

---

## 3. 認証・会員登録

### 3-1. ログイン画面表示（GET /login）

```mermaid
sequenceDiagram
    participant Browser
    participant AuthController
    participant MemberSessionService
    participant LoginEmailCookieService

    Browser->>AuthController: GET /login(?redirect=...&expired=false)
    AuthController->>MemberSessionService: currentMember(session)
    MemberSessionService-->>AuthController: MemberSessionUser または empty

    alt 既ログイン
        AuthController-->>Browser: redirect:/mypage/orders
    else 未ログイン
        AuthController->>LoginEmailCookieService: findRememberedEmail(request)
        LoginEmailCookieService-->>AuthController: 記憶済みメールアドレス または empty
        AuthController-->>Browser: HTML（pages/login）
    end
```

---

### 3-1b. ログイン処理（POST /login）

```mermaid
sequenceDiagram
    participant Browser
    participant SpringSec as Spring Security
    participant MemberDetailsService
    participant MemberSessionService
    participant Repository
    participant DB

    Browser->>SpringSec: POST /login（email, password）
    SpringSec->>MemberDetailsService: loadUserByUsername(email)
    MemberDetailsService->>Repository: SELECT 会員（email検索）
    Repository->>DB: SQL実行
    DB-->>Repository: 会員情報
    Repository-->>MemberDetailsService: 会員情報
    MemberDetailsService-->>SpringSec: UserDetails
    Note right of SpringSec: パスワード照合（BCrypt）

    alt 認証失敗
        SpringSec-->>Browser: redirect:/login?error=true
    else 認証成功
        SpringSec->>MemberSessionService: セッションへ会員情報を格納
        SpringSec-->>Browser: redirect:/mypage/orders または redirectパラメータ先
    end
```

---

### 3-2. ログアウト処理（POST /logout）

```mermaid
sequenceDiagram
    participant Browser
    participant SpringSec as Spring Security
    participant Session

    Browser->>SpringSec: POST /logout
    SpringSec->>Session: セッション無効化
    SpringSec-->>Browser: redirect:/login
```

---

### 3-3. 会員登録入力（GET /members/register）

```mermaid
sequenceDiagram
    participant Browser
    participant MemberRegCtrl as MemberRegistrationController
    participant Session

    Browser->>MemberRegCtrl: GET /members/register
    MemberRegCtrl->>Session: PENDING_REGISTER_FORM を取得
    Session-->>MemberRegCtrl: 保留フォーム または null
    MemberRegCtrl-->>Browser: HTML（pages/member-register）
```

---

### 3-3b. 会員登録バリデーション・確認画面遷移（POST /members/register/confirm）

```mermaid
sequenceDiagram
    participant Browser
    participant MemberRegCtrl as MemberRegistrationController
    participant MemberService
    participant Repository
    participant DB
    participant Session

    Browser->>MemberRegCtrl: POST /members/register/confirm（入力フォーム）
    MemberRegCtrl->>MemberRegCtrl: Bean Validation・フォーム正規化
    MemberRegCtrl->>MemberService: existsByEmail(email)
    MemberService->>Repository: SELECT 会員（email）
    Repository->>DB: SQL実行
    DB-->>Repository: 件数
    Repository-->>MemberService: 件数
    MemberService-->>MemberRegCtrl: boolean（重複有無）

    alt バリデーション NG / メール重複
        MemberRegCtrl-->>Browser: HTML（pages/member-register）エラー表示
    else バリデーション OK
        MemberRegCtrl->>Session: PENDING_REGISTER_FORM に保存
        MemberRegCtrl-->>Browser: HTML（pages/member-register-confirm）確認画面
    end
```

---

### 3-4b. 会員登録確定（POST /members/register）

```mermaid
sequenceDiagram
    participant Browser
    participant MemberRegCtrl as MemberRegistrationController
    participant MemberService
    participant MemberSessionService
    participant NotificationMailService
    participant Repository
    participant DB
    participant Session

    Browser->>MemberRegCtrl: POST /members/register
    MemberRegCtrl->>Session: PENDING_REGISTER_FORM を取得
    Session-->>MemberRegCtrl: フォーム または null

    alt フォームなし
        MemberRegCtrl-->>Browser: redirect:/members/register
    else フォームあり
        MemberRegCtrl->>MemberService: register(form)
        MemberService->>Repository: INSERT members
        Repository->>DB: SQL実行
        DB-->>Repository: 登録済み会員ID
        Repository-->>MemberService: MemberSessionUser
        MemberService-->>MemberRegCtrl: MemberSessionUser

        alt メール重複エラー
            MemberRegCtrl-->>Browser: HTML（pages/member-register）エラー表示
        else 登録成功
            MemberRegCtrl->>Session: PENDING_REGISTER_FORM を削除
            MemberRegCtrl->>MemberSessionService: login(request, created)
            MemberRegCtrl->>NotificationMailService: sendMemberRegistrationCompleteMail(created)
            MemberRegCtrl-->>Browser: redirect:/members/register/complete
        end
    end
```

---

### 3-5. 会員登録完了（GET /members/register/complete）

```mermaid
sequenceDiagram
    participant Browser
    participant MemberRegCtrl as MemberRegistrationController

    Browser->>MemberRegCtrl: GET /members/register/complete
    MemberRegCtrl->>MemberRegCtrl: フラッシュ属性（registeredMemberCode）の有無を確認

    alt フラッシュなし（直アクセス）
        MemberRegCtrl-->>Browser: 404 エラー
    else フラッシュあり
        MemberRegCtrl-->>Browser: HTML（pages/member-register-complete）
    end
```

---

## 4. カート・購入フロー

### 4-1. カート表示（GET /cart）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant CartCookieStore
    participant CartRepository
    participant ProductService
    participant Repository
    participant DB
    participant Session

    Browser->>CartController: GET /cart
    CartController->>CartService: getCart(request, response)
    CartService->>CartCookieStore: Cookieからカートアイテムを取得
    CartCookieStore-->>CartService: List[CartCookieItem]
    CartService->>CartRepository: SELECT 商品スナップショット（バリアントID一覧）
    CartRepository->>DB: SQL実行
    DB-->>CartRepository: 商品情報・税率
    CartRepository-->>CartService: CartProductSnapshotリスト
    Note right of CartService: 数量正規化・小計・送料・消費税・合計を計算
    CartService-->>CartController: CartView

    Note over CartController,Repository: ★CUSTOM-001 クロスセルセクション（フェーズ1）
    CartController->>Session: 最近見た商品IDリストを取得（設計確認事項–D-01参照）
    Session-->>CartController: recentlyViewedProductIds

    alt 最近見た商品が存在する
        CartController->>ProductService: findCrossSellProducts(recentlyViewedProductIds[0], cartProductIds)
        ProductService->>Repository: SELECT recommended_related_products（最新推薦日・販売期間チェック・カート内商品除外）
        Repository->>DB: SQL実行
        DB-->>Repository: クロスセル商品リスト（0〜4件）
        Repository-->>ProductService: クロスセル商品リスト
        ProductService-->>CartController: クロスセル商品リスト（0件の場合はセクション非表示）
    end

    CartController-->>Browser: HTML（pages/cart）
```

---

### 4-1b. カート追加（POST /cart/items）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant CartCookieStore

    Browser->>CartController: POST /cart/items（productVariantId, quantity, redirect）
    CartController->>CartService: addItem(request, response, productVariantId, quantity, assemblyRequested)
    CartService->>CartCookieStore: 現在のカートを取得
    Note right of CartService: 数量上限・組立可否チェック
    CartService->>CartCookieStore: Cookieを更新（追加後のアイテムリスト）

    alt 不正パラメータ
        CartController-->>Browser: redirect:元ページ（フラッシュ：エラー）
    else 正常
        CartController-->>Browser: redirect:元ページ（フラッシュ：追加完了）
    end
```

---

### 4-1c. カート数量更新（POST /cart/items/{productVariantId}/update）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant CartCookieStore

    Browser->>CartController: POST /cart/items/{productVariantId}/update（quantity）
    CartController->>CartService: updateItem(request, response, productVariantId, quantity)
    CartService->>CartCookieStore: Cookieを更新（数量変更後のアイテムリスト）

    alt バリデーションエラー
        CartController-->>Browser: redirect:/cart（フラッシュ：エラー）
    else 正常
        CartController-->>Browser: redirect:/cart
    end
```

---

### 4-1d. カートアイテム削除（POST /cart/items/{productVariantId}/delete）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant CartCookieStore

    Browser->>CartController: POST /cart/items/{productVariantId}/delete
    CartController->>CartService: removeItem(request, response, productVariantId)
    CartService->>CartCookieStore: Cookieを更新（アイテム削除後のリスト）
    CartController-->>Browser: redirect:/cart
```

---

### 4-1e. カートクリア（POST /cart/clear）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant CartCookieStore

    Browser->>CartController: POST /cart/clear
    CartController->>CartService: clear(request, response)
    CartService->>CartCookieStore: Cookieを空にする
    CartController-->>Browser: redirect:/cart
```

---

### 4-2. 購入方法選択（GET /checkout/method）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant LoginEmailCookieService

    Browser->>CartController: GET /checkout/method
    CartController->>CartService: getCart(request, response)
    CartService-->>CartController: CartView

    alt カートが空
        CartController-->>Browser: redirect:/cart
    else カートあり
        CartController->>LoginEmailCookieService: findRememberedEmail(request)
        LoginEmailCookieService-->>CartController: 記憶済みメールアドレス または empty
        CartController-->>Browser: HTML（pages/checkout-method）
    end
```

---

### 4-3. 注文情報入力（GET /checkout/input）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant MemberSessionService
    participant OrderService
    participant MemberService
    participant Repository
    participant DB
    participant Session

    Browser->>CartController: GET /checkout/input
    CartController->>CartService: getCart(request, response)
    CartService-->>CartController: CartView

    alt カートが空
        CartController-->>Browser: redirect:/cart
    else カートあり
        CartController->>Session: CHECKOUT_FORM_SESSION_KEY を取得
        CartController->>MemberSessionService: currentMember(session)
        MemberSessionService-->>CartController: MemberSessionUser または empty

        alt ログイン中かつセッションフォームなし
            CartController->>OrderService: createInitialForm(member)
            OrderService->>MemberService: 会員情報・追加お届け先を取得
            MemberService->>Repository: SELECT 会員・追加お届け先
            Repository->>DB: SQL実行
            DB-->>Repository: 会員情報
            Repository-->>MemberService: 会員情報
            MemberService-->>OrderService: 会員情報
            OrderService-->>CartController: CheckoutInputForm（初期値補完済み）
        end

        CartController->>MemberService: findAdditionalAddresses(memberId)
        MemberService->>Repository: SELECT 追加お届け先
        Repository->>DB: SQL実行
        DB-->>Repository: 追加お届け先
        Repository-->>MemberService: 追加お届け先リスト
        MemberService-->>CartController: 追加お届け先リスト

        CartController-->>Browser: HTML（pages/checkout-input）
    end
```

---

### 4-3b. 注文情報入力送信（POST /checkout/input）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant Session

    Browser->>CartController: POST /checkout/input（入力フォーム）
    CartController->>CartService: getCart(request, response)
    CartService-->>CartController: CartView

    alt カートが空
        CartController-->>Browser: redirect:/cart
    else カートあり
        CartController->>CartController: Bean Validation・業務ルール検証・フォーム正規化

        alt バリデーション NG
            CartController-->>Browser: HTML（pages/checkout-input）エラー表示
        else バリデーション OK
            CartController->>Session: CHECKOUT_FORM_SESSION_KEY に保存
            CartController->>Session: CHECKOUT_CONFIRM_TOKEN_SESSION_KEY にトークンを生成・保存
            CartController-->>Browser: redirect:/checkout/confirm
        end
    end
```

---

### 4-4. 注文確認（GET /checkout/confirm）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant Session

    Browser->>CartController: GET /checkout/confirm
    CartController->>CartService: getCart(request, response)
    CartService-->>CartController: CartView

    alt カートが空
        CartController->>Session: 購入フローセッションをクリア
        CartController-->>Browser: redirect:/cart
    else カートあり
        CartController->>Session: CHECKOUT_FORM_SESSION_KEY を取得

        alt セッションフォームなし
            CartController-->>Browser: redirect:/checkout/input
        else フォームあり
            CartController->>Session: CHECKOUT_CONFIRM_TOKEN_SESSION_KEY を取得または新規生成
            CartController-->>Browser: HTML（pages/checkout-confirm）
        end
    end
```

---

### 4-4b. 注文確定（POST /checkout/confirm）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant CartService
    participant MemberSessionService
    participant OrderService
    participant NotificationMailService
    participant OrderRepository
    participant DB
    participant Session

    Browser->>CartController: POST /checkout/confirm（token）
    CartController->>CartService: getCart(request, response)
    CartService-->>CartController: CartView

    alt カートが空
        CartController-->>Browser: redirect:/cart
    else カートあり
        CartController->>Session: フォーム・トークンを取得

        alt フォームなし
            CartController-->>Browser: redirect:/checkout/input（フラッシュ：再入力案内）
        else トークン不一致
            CartController->>Session: 購入フローセッションをクリア
            CartController-->>Browser: redirect:/checkout/input（フラッシュ：エラー）
        else 正常
            CartController->>MemberSessionService: currentMember(session)
            MemberSessionService-->>CartController: memberId または null
            CartController->>OrderService: placeOrder(memberId, sessionForm, cart)
            OrderService->>OrderRepository: INSERT orders / order_items
            OrderRepository->>DB: SQL実行（トランザクション）
            DB-->>OrderRepository: 受注ID・注文番号
            OrderRepository-->>OrderService: 注文番号
            Note right of OrderService: トランザクションコミット後にメール送信を予約
            OrderService->>NotificationMailService: sendOrderCompleteMail(payload)
            OrderService-->>CartController: 注文番号

            CartController->>Session: 購入フローセッションをクリア
            CartController->>CartService: clear（カートを空にする）
            CartController-->>Browser: redirect:/checkout/complete/{orderNumber}
        end
    end
```

---

### 4-5. 注文完了（GET /checkout/complete/{orderNumber}）

```mermaid
sequenceDiagram
    participant Browser
    participant CartController
    participant OrderService
    participant OrderRepository
    participant DB

    Browser->>CartController: GET /checkout/complete/{orderNumber}
    CartController->>OrderService: findOrderCompleteView(orderNumber)
    OrderService->>OrderRepository: SELECT orders（注文番号）
    OrderRepository->>DB: SQL実行
    DB-->>OrderRepository: 注文情報
    OrderRepository-->>OrderService: 注文情報
    OrderService-->>CartController: OrderCompleteView（存在しない場合は404）
    CartController-->>Browser: HTML（pages/checkout-complete）
```

---

## 5. マイページ（要ログイン）

> 各画面で Spring Security または `requireLoginMember()` により未認証は 401 を返す。

### 5-1. マイページトップ（GET /mypage）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController

    Browser->>MyPageController: GET /mypage
    MyPageController-->>Browser: redirect:/mypage/orders
```

---

### 5-2. 購入履歴一覧（GET /mypage/orders）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant OrderService
    participant OrderRepository
    participant DB

    Browser->>MyPageController: GET /mypage/orders(?page=...)
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser（未認証は401）

    MyPageController->>OrderService: findMemberOrderHistories(memberId, page)
    OrderService->>OrderRepository: SELECT orders（会員ID・ページネーション）
    OrderRepository->>DB: SQL実行
    DB-->>OrderRepository: 注文一覧・件数
    OrderRepository-->>OrderService: 注文一覧・件数
    OrderService-->>MyPageController: MemberOrderHistoryPage

    opt ページ超過（補正処理）
        MyPageController->>OrderService: findMemberOrderHistories(memberId, totalPages)
        OrderService-->>MyPageController: MemberOrderHistoryPage（最終ページ）
    end

    MyPageController-->>Browser: HTML（pages/mypage-orders-list）
```

---

### 5-3. 購入履歴詳細（GET /mypage/orders/{orderNumber}）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant OrderService
    participant OrderRepository
    participant DB

    Browser->>MyPageController: GET /mypage/orders/{orderNumber}
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>OrderService: findMemberOrderDetail(memberId, orderNumber)
    OrderService->>OrderRepository: SELECT orders / order_items（会員ID + 注文番号）
    OrderRepository->>DB: SQL実行
    DB-->>OrderRepository: 注文詳細
    OrderRepository-->>OrderService: 注文詳細
    OrderService-->>MyPageController: MemberOrderDetailView（存在しない場合は404）

    MyPageController-->>Browser: HTML（pages/mypage-order-detail）
```

---

### 5-3b. 再注文（POST /mypage/orders/{orderNumber}/reorder）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant OrderService
    participant OrderRepository
    participant CartService
    participant CartCookieStore
    participant DB

    Browser->>MyPageController: POST /mypage/orders/{orderNumber}/reorder
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>OrderService: findMemberOrderDetail(memberId, orderNumber)
    OrderService->>OrderRepository: SELECT orders（存在確認）
    OrderRepository->>DB: SQL実行
    DB-->>OrderRepository: 注文
    OrderRepository-->>OrderService: 注文
    OrderService-->>MyPageController: MemberOrderDetailView または empty

    alt 存在しない
        MyPageController-->>Browser: 404エラー
    else 存在する
        MyPageController->>OrderService: findReorderItems(memberId, orderNumber)
        OrderService->>OrderRepository: SELECT order_items（再注文用バリアント情報付き）
        OrderRepository->>DB: SQL実行
        DB-->>OrderRepository: 再注文アイテムリスト
        OrderRepository-->>OrderService: 再注文アイテムリスト
        OrderService-->>MyPageController: List[OrderReorderItem]

        loop アイテム数ぶんループ
            MyPageController->>CartService: addItem(productVariantId, quantity, ...)
            CartService->>CartCookieStore: Cookieを更新
        end

        MyPageController-->>Browser: redirect:/cart（フラッシュ：成功/一部失敗メッセージ）
    end
```

---

### 5-4. お気に入り一覧（GET /mypage/favorites）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/favorites(?page=...)
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>MemberService: findFavorites(memberId, page)
    MemberService->>Repository: SELECT favorites（会員ID・ページネーション）
    Repository->>DB: SQL実行
    DB-->>Repository: お気に入り一覧・件数
    Repository-->>MemberService: お気に入り一覧・件数
    MemberService-->>MyPageController: MemberFavoritePage

    opt ページ超過（補正処理）
        MyPageController->>MemberService: findFavorites(memberId, totalPages)
        MemberService-->>MyPageController: MemberFavoritePage（最終ページ）
    end

    MyPageController-->>Browser: HTML（pages/mypage-favorites）
```

---

### 5-5. 会員情報変更フォーム表示（GET /mypage/profile）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/profile
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>MemberService: findProfileByMemberId(memberId)
    MemberService->>Repository: SELECT 会員情報
    Repository->>DB: SQL実行
    DB-->>Repository: 会員情報
    Repository-->>MemberService: 会員情報
    MemberService-->>MyPageController: MemberProfileEditForm

    MyPageController-->>Browser: HTML（pages/mypage-profile-edit）
```

---

### 5-5b. 会員情報変更送信（POST /mypage/profile）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: POST /mypage/profile（入力フォーム）
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>MyPageController: Bean Validation・法人必須ルール検証・フォーム正規化

    alt バリデーション NG
        MyPageController-->>Browser: HTML（pages/mypage-profile-edit）エラー表示
    else バリデーション OK
        MyPageController->>MemberService: updateProfile(memberId, form)
        MemberService->>Repository: UPDATE members
        Repository->>DB: SQL実行
        DB-->>Repository: 更新件数
        Repository-->>MemberService: 更新結果
        MemberService-->>MyPageController: 完了（メール重複時は例外）

        alt メール重複
            MyPageController-->>Browser: HTML（pages/mypage-profile-edit）エラー表示
        else 更新成功
            MyPageController->>MemberService: findActiveById(memberId)
            MemberService->>Repository: SELECT 会員
            Repository->>DB: SQL実行
            DB-->>Repository: 会員情報
            Repository-->>MemberService: 会員情報
            MemberService-->>MyPageController: MemberSessionUser
            MyPageController->>MemberSessionService: login(request, updatedMember)
            MyPageController-->>Browser: redirect:/mypage/profile（フラッシュ：更新完了）
        end
    end
```

---

### 5-6. お届け先一覧（GET /mypage/addresses）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/addresses(?page=...)
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser

    MyPageController->>MemberService: findAdditionalAddresses(memberId, page)
    MemberService->>Repository: SELECT member_additional_addresses（ページネーション）
    Repository->>DB: SQL実行
    DB-->>Repository: お届け先一覧・件数
    Repository-->>MemberService: お届け先一覧・件数
    MemberService-->>MyPageController: MemberAdditionalAddressPage

    MyPageController-->>Browser: HTML（pages/mypage-addresses）（上限到達フラグ付き）
```

---

### 5-7. お届け先新規登録（GET → POST /mypage/addresses/new）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/addresses/new
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MemberService: isAddressLimitReached(memberId)
    MemberService-->>MyPageController: boolean（上限到達有無）
    MyPageController-->>Browser: HTML（pages/mypage-address-form）

    Browser->>MyPageController: POST /mypage/addresses（入力フォーム）
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MyPageController: Bean Validation・法人必須ルール検証・フォーム正規化

    alt バリデーション NG
        MyPageController-->>Browser: HTML（pages/mypage-address-form）エラー表示
    else バリデーション OK
        MyPageController->>MemberService: createAdditionalAddress(memberId, form)
        MemberService->>Repository: INSERT member_additional_addresses
        Repository->>DB: SQL実行
        DB-->>Repository: 結果
        Repository-->>MemberService: 結果
        MemberService-->>MyPageController: 完了（上限超過時は例外）

        alt 上限超過
            MyPageController-->>Browser: redirect:/mypage/addresses（フラッシュ：エラー）
        else 登録成功
            MyPageController-->>Browser: redirect:/mypage/addresses
        end
    end
```

---

### 5-8. お届け先編集（GET → POST /mypage/addresses/{id}/edit）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/addresses/{memberAddressId}/edit
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MemberService: findAdditionalAddressById(memberId, memberAddressId)
    MemberService->>Repository: SELECT member_additional_addresses（会員ID + 住所ID）
    Repository->>DB: SQL実行
    DB-->>Repository: お届け先（存在しない場合は404）
    Repository-->>MemberService: お届け先
    MemberService-->>MyPageController: MemberAdditionalAddressView
    MyPageController-->>Browser: HTML（pages/mypage-address-form）既存データを初期値として表示

    Browser->>MyPageController: POST /mypage/addresses/{memberAddressId}（入力フォーム）
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MyPageController: Bean Validation・法人必須ルール検証・フォーム正規化

    alt バリデーション NG
        MyPageController-->>Browser: HTML（pages/mypage-address-form）エラー表示
    else バリデーション OK
        MyPageController->>MemberService: updateAdditionalAddress(memberId, memberAddressId, form)
        MemberService->>Repository: UPDATE member_additional_addresses
        Repository->>DB: SQL実行
        DB-->>Repository: 更新件数
        Repository-->>MemberService: 更新結果
        MemberService-->>MyPageController: boolean（更新有無）
        MyPageController-->>Browser: redirect:/mypage/addresses
    end
```

---

### 5-9. お届け先削除（POST /mypage/addresses/{id}/delete）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: POST /mypage/addresses/{memberAddressId}/delete
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MemberService: deleteAdditionalAddress(memberId, memberAddressId)
    MemberService->>Repository: DELETE member_additional_addresses
    Repository->>DB: SQL実行
    DB-->>Repository: 結果
    Repository-->>MemberService: 結果
    MyPageController-->>Browser: redirect:/mypage/addresses
```

---

### 5-10. 退会確認・退会実行（GET / POST /mypage/withdraw）

```mermaid
sequenceDiagram
    participant Browser
    participant MyPageController
    participant MemberSessionService
    participant MemberService
    participant Repository
    participant DB

    Browser->>MyPageController: GET /mypage/withdraw
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController-->>Browser: HTML（pages/mypage-withdraw）退会確認画面

    Browser->>MyPageController: POST /mypage/withdraw
    MyPageController->>MemberSessionService: requireLoginMember(session)
    MemberSessionService-->>MyPageController: MemberSessionUser
    MyPageController->>MemberService: withdraw(memberId)
    MemberService->>Repository: UPDATE members（退会フラグ / 論理削除）
    Repository->>DB: SQL実行
    DB-->>Repository: 更新件数
    Repository-->>MemberService: 結果
    MemberService-->>MyPageController: 完了
    MyPageController->>MemberSessionService: clear(session)
    Note right of MemberSessionService: セッション無効化
    MyPageController-->>Browser: redirect:/login
```

