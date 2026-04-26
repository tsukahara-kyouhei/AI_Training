# シーケンス図一覧

本ドキュメントは、office-order システムの主要なユースケースのシーケンス図を Mermaid 記法でまとめた設計資料です。

## 目次

1. [会員登録フロー](#1-会員登録フロー)
2. [ログインフロー](#2-ログインフロー)
3. [商品一覧・詳細閲覧フロー](#3-商品一覧詳細閲覧フロー)
4. [お気に入り登録・解除フロー](#4-お気に入り登録解除フロー)
5. [カート操作フロー](#5-カート操作フロー)
6. [チェックアウト（注文確定）フロー](#6-チェックアウト注文確定フロー)
7. [購入履歴・再購入フロー](#7-購入履歴再購入フロー)
8. [会員情報変更フロー](#8-会員情報変更フロー)
9. [追加お届け先管理フロー](#9-追加お届け先管理フロー)
10. [退会フロー](#10-退会フロー)
11. [お問い合わせ送信フロー](#11-お問い合わせ送信フロー)
12. [バッチ実行フロー](#12-バッチ実行フロー)

---

## 1. 会員登録フロー

入力 → 確認 → 確定の3ステップで会員登録を行う。確定時にセッションログインとメール送信を実施する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant RC as MemberRegistrationController
    participant MS as MemberService
    participant MR as MemberRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService
    participant NMS as NotificationMailService

    Browser->>RC: GET /members/register
    RC-->>Browser: 登録入力画面 (member-register.html)

    Browser->>RC: POST /members/register/confirm (入力フォーム送信)
    RC->>RC: @Valid バリデーション
    alt バリデーションエラー
        RC-->>Browser: 登録入力画面 (エラー表示)
    else バリデーションOK
        RC->>MS: existsByEmail(email)
        MS->>MR: existsByEmail(email)
        MR->>DB: SELECT COUNT(*) FROM members WHERE email = ?
        DB-->>MR: 0 または 1
        MR-->>MS: true / false
        MS-->>RC: true / false
        alt メールアドレス重複
            RC-->>Browser: 登録入力画面 (重複エラー表示)
        else 重複なし
            RC->>RC: session.setAttribute(PENDING_REGISTER_FORM, form)
            RC-->>Browser: 登録確認画面 (member-register-confirm.html)
        end
    end

    Browser->>RC: POST /members/register (確定)
    RC->>RC: session.getAttribute(PENDING_REGISTER_FORM)
    alt セッションにフォームなし
        RC-->>Browser: redirect:/members/register
    else フォームあり
        RC->>MS: register(form)
        MS->>MR: insertMember(form)
        MR->>DB: INSERT INTO members (...)
        DB-->>MR: memberId
        MR-->>MS: MemberSessionUser
        MS-->>RC: MemberSessionUser
        RC->>MSS: login(request, memberSessionUser)
        MSS->>MSS: セッション固定化 (session.invalidate + 新セッション生成)
        MSS->>MSS: session.setAttribute(MEMBER, memberSessionUser)
        RC->>NMS: sendMemberRegistrationCompleteMail(memberSessionUser)
        NMS->>NMS: メール本文生成 (member-registration-body.txt)
        NMS-->>Browser: 会員登録完了メール送信 (非同期)
        RC-->>Browser: redirect:/members/register/complete
    end

    Browser->>RC: GET /members/register/complete
    RC-->>Browser: 登録完了画面 (member-register-complete.html)
```

---

## 2. ログインフロー

Spring Security のフォームログイン機能は使用せず、独自の `MemberSessionService` でセッション管理を行う。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant AC as AuthController
    participant MSS as MemberSessionService
    participant MR as MemberRepository
    participant DB as PostgreSQL
    participant LECS as LoginEmailCookieService

    Browser->>AC: GET /login
    AC->>MSS: currentMember(session)
    MSS-->>AC: Optional<MemberSessionUser>
    alt 既ログイン済み
        AC-->>Browser: redirect:/mypage/orders
    else 未ログイン
        AC->>LECS: findRememberedEmail(request)
        LECS-->>AC: Optional<String> (Cookie から取得)
        AC-->>Browser: ログイン画面 (login.html)
    end

    Browser->>AC: POST /login (メール・パスワード送信)
    Note over AC: Spring Security は使用しない
    AC->>MSS: login(request, loginForm)
    MSS->>MR: findActiveCredentialByEmail(email)
    MR->>DB: SELECT * FROM members WHERE email = ? AND member_status = 'active'
    DB-->>MR: MemberCredential または null
    MR-->>MSS: Optional<MemberCredential>
    alt 会員が見つからない
        MSS-->>AC: ログイン失敗
        AC-->>Browser: ログイン画面 (認証エラー表示)
    else 会員あり
        MSS->>MSS: BCrypt パスワード照合
        alt パスワード不一致
            MSS-->>AC: ログイン失敗
            AC-->>Browser: ログイン画面 (認証エラー表示)
        else パスワード一致
            MSS->>MSS: session.invalidate() + 新セッション生成
            MSS->>MSS: session.setAttribute(MEMBER, memberSessionUser)
            MSS->>LECS: saveOrClearRememberedEmail(response, email, rememberMe)
            MSS-->>AC: ログイン成功 (MemberSessionUser)
            AC-->>Browser: redirect: (redirectPath または /mypage/orders)
        end
    end

    Browser->>AC: POST /logout
    AC->>MSS: clear(session)
    MSS->>MSS: session.invalidate()
    AC-->>Browser: redirect:/login
```

---

## 3. 商品一覧・詳細閲覧フロー

カテゴリ一覧・キーワード検索・新着一覧は共通の `ProductService` を経由する。商品詳細ではおすすめ関連商品も合わせて取得する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant CC as CatalogController
    participant PS as ProductService
    participant PFOS as ProductFilterOptionService
    participant PLSS as ProductListSearchService
    participant PR as ProductRepository
    participant PFOR as ProductFilterOptionRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Browser->>CC: GET /categories/desks (またはchairs/storages/products/search)
    CC->>PFOS: findFilterOptions(category)
    PFOS->>PFOR: findColorOptions() / findDeskFilterOptions() 等
    PFOR->>DB: SELECT * FROM colors / desk_top_shapes / desk_tastes 等
    DB-->>PFOR: マスタデータ
    PFOR-->>PFOS: フィルタ選択肢一覧
    PFOS-->>CC: ProductFilterOptionsBundle

    CC->>PS: findProductList(condition)
    PS->>PR: findList(condition)
    PR->>DB: SELECT products.*, product_variants.* ... WHERE category = ? ...
    DB-->>PR: 商品一覧 (ページング)
    PR-->>PS: ProductListPage
    PS-->>CC: ProductListPage

    CC->>MSS: currentMember(session)
    MSS-->>CC: Optional<MemberSessionUser>
    note over CC: ログイン済みの場合はお気に入りIDセットも取得
    CC-->>Browser: 商品一覧画面 (product-list-category-desk.html 等)

    Browser->>CC: GET /products/{productId}
    CC->>PS: findDetail(productId, inStockOnly)
    PS->>PR: findById(productId)
    PR->>DB: SELECT products.*, product_variants.*, product_desk_attributes.* 等
    DB-->>PR: 商品詳細
    PR-->>PS: ProductDetailView
    PS-->>CC: Optional<ProductDetailView>
    alt 商品が存在しない
        CC-->>Browser: 404 Not Found
    else 商品あり
        CC->>PS: findRecommendedRelated(productId)
        PS->>PR: findRecommendedRelatedProducts(productId)
        PR->>DB: SELECT * FROM recommended_related_products JOIN products ...
        DB-->>PR: おすすめ関連商品一覧
        PR-->>PS: List<ProductCardView>
        PS-->>CC: List<ProductCardView>
        CC->>MSS: currentMember(session)
        MSS-->>CC: Optional<MemberSessionUser>
        CC-->>Browser: 商品詳細画面 (product-detail.html)
    end
```

---

## 4. お気に入り登録・解除フロー

未ログイン時はログイン画面へリダイレクトする。登録上限超過時はエラーをフラッシュメッセージで表示する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant CC as CatalogController
    participant MSS as MemberSessionService
    participant MS as MemberService
    participant MR as MemberRepository
    participant DB as PostgreSQL

    Browser->>CC: POST /products/{productId}/favorite (action=toggle または remove)
    CC->>CC: productService.findDetail(productId) で商品存在確認
    alt 商品が存在しない
        CC-->>Browser: 404 Not Found
    else 商品あり
        CC->>MSS: currentMember(session)
        MSS-->>CC: Optional<MemberSessionUser>
        alt 未ログイン
            CC-->>Browser: redirect:/login?redirect={元のURL}
        else ログイン済み
            alt action = remove
                CC->>MS: removeFavorite(memberId, productId)
                MS->>MR: deleteFavorite(memberId, productId)
                MR->>DB: DELETE FROM member_favorites WHERE member_id = ? AND product_id = ?
                DB-->>MR: 削除件数
                MR-->>MS: void
                MS-->>CC: void
                CC-->>Browser: redirect:{元のURL}
            else action = toggle
                CC->>MS: toggleFavorite(memberId, productId)
                MS->>MR: existsFavorite(memberId, productId)
                MR->>DB: SELECT COUNT(*) FROM member_favorites WHERE ...
                DB-->>MR: 0 または 1
                MR-->>MS: true / false
                alt 既にお気に入り登録済み → 解除
                    MS->>MR: deleteFavorite(memberId, productId)
                    MR->>DB: DELETE FROM member_favorites WHERE ...
                    DB-->>MR: OK
                else 未登録 → 登録
                    MS->>MR: countFavorites(memberId)
                    MR->>DB: SELECT COUNT(*) FROM member_favorites WHERE member_id = ?
                    DB-->>MR: 件数
                    alt 上限超過
                        MS-->>CC: FavoritesLimitExceededException
                        CC-->>Browser: redirect:{元のURL} (上限エラーをフラッシュ表示)
                    else 上限以内
                        MS->>MR: insertFavorite(memberId, productId)
                        MR->>DB: INSERT INTO member_favorites (...)
                        DB-->>MR: OK
                    end
                end
                CC-->>Browser: redirect:{元のURL}
            end
        end
    end
```

---

## 5. カート操作フロー

カートデータは Cookie で管理される。DB アクセスは商品バリアント情報・税率の参照のみ。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant CC as CartController
    participant CS as CartService
    participant CR as CartRepository
    participant DB as PostgreSQL

    Note over Browser,DB: カート表示
    Browser->>CC: GET /cart
    CC->>CS: getCart(request, response)
    CS->>CS: Cookie からカートデータ (JSON) をデシリアライズ
    CS->>CR: findProductSnapshotsByVariantIds(variantIds)
    CR->>DB: SELECT pv.*, p.name, c.name FROM product_variants pv JOIN products p JOIN colors c WHERE pv.id IN (...)
    DB-->>CR: 商品バリアント情報
    CR-->>CS: List<ProductVariantSnapshot>
    CS->>CR: findCurrentTaxRatePercent()
    CR->>DB: SELECT rate_percent FROM tax_rates WHERE effective_from <= NOW() ORDER BY effective_from DESC LIMIT 1
    DB-->>CR: 税率
    CR-->>CS: BigDecimal
    CS->>CS: CartView 組み立て（小計・税額・合計計算）
    CS-->>CC: CartView
    CC-->>Browser: カート画面 (cart.html)

    Note over Browser,DB: カートへ商品追加
    Browser->>CC: POST /cart/items (productVariantId, quantity, assemblyRequested)
    CC->>CS: addItem(request, response, productVariantId, quantity, assemblyRequested)
    CS->>CR: findProductSnapshotsByVariantIds([productVariantId])
    CR->>DB: SELECT ... FROM product_variants WHERE id = ?
    DB-->>CR: 商品バリアント情報
    CR-->>CS: 商品スナップショット
    alt 商品が存在しない / 数量範囲外 / カート上限超過
        CS-->>CC: IllegalArgumentException
        CC-->>Browser: redirect:{元画面} (エラーフラッシュ表示)
    else 追加OK
        CS->>CS: カートデータを更新して Cookie に書き戻し
        CS-->>CC: void
        CC-->>Browser: redirect:{元画面} (追加完了フラッシュ表示)
    end

    Note over Browser,DB: カート明細の数量更新
    Browser->>CC: POST /cart/items/{productVariantId}/update (quantity)
    CC->>CS: updateItem(request, response, productVariantId, quantity, assemblyRequested)
    CS->>CS: Cookie からカートデータ取得・バリデーション・更新
    CS->>CS: 更新済みカートを Cookie に書き戻し
    CS-->>CC: void
    CC-->>Browser: redirect:/cart

    Note over Browser,DB: カート明細削除
    Browser->>CC: POST /cart/items/{productVariantId}/delete
    CC->>CS: removeItem(request, response, productVariantId)
    CS->>CS: Cookie からカートデータ取得・該当明細削除
    CS->>CS: 更新済みカートを Cookie に書き戻し
    CS-->>CC: void
    CC-->>Browser: redirect:/cart
```

---

## 6. チェックアウト（注文確定）フロー

PRG パターン + ワンタイムトークンで二重送信を防止する。確定成功後にカートをクリアしてメール送信する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant CC as CartController
    participant CS as CartService
    participant OS as OrderService
    participant OR as OrderRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService
    participant NMS as NotificationMailService

    Browser->>CC: GET /checkout/method
    CC->>CS: getCart(request, response)
    CS-->>CC: CartView
    alt カートが空
        CC-->>Browser: redirect:/cart
    else カートあり
        CC-->>Browser: 購入方法選択画面 (checkout-method.html)
    end

    Browser->>CC: GET /checkout/input (ログイン済みまたはゲストで進む)
    CC->>CS: getCart(request, response)
    CS-->>CC: CartView
    CC->>MSS: currentMember(session)
    MSS-->>CC: Optional<MemberSessionUser>
    CC->>OS: createInitialForm(member)
    OS-->>CC: CheckoutInputForm (会員情報で事前入力)
    CC-->>Browser: 注文情報入力画面 (checkout-input.html)

    Browser->>CC: POST /checkout/input (フォーム送信)
    CC->>CC: @Valid バリデーション + 業務ルール検証
    alt バリデーションエラー
        CC-->>Browser: 注文情報入力画面 (エラー表示)
    else バリデーションOK
        CC->>CC: session.setAttribute(CHECKOUT_FORM, form)
        CC->>CC: session.setAttribute(CHECKOUT_TOKEN, UUID)
        CC-->>Browser: redirect:/checkout/confirm
    end

    Browser->>CC: GET /checkout/confirm
    CC->>CS: getCart(request, response)
    CS-->>CC: CartView
    CC->>CC: session.getAttribute(CHECKOUT_FORM)
    alt フォームがセッションにない
        CC-->>Browser: redirect:/checkout/input
    else フォームあり
        CC-->>Browser: 注文確認画面 (checkout-confirm.html, トークン埋め込み)
    end

    Browser->>CC: POST /checkout/confirm (token送信)
    CC->>CS: getCart(request, response)
    CS-->>CC: CartView
    alt カートが空
        CC-->>Browser: redirect:/cart
    else カートあり (トークン検証)
        CC->>CC: session.getAttribute(CHECKOUT_TOKEN) と比較
        alt トークン不一致（二重送信）
            CC->>CC: clearCheckoutSession(session)
            CC-->>Browser: redirect:/checkout/input (トークン不一致エラー)
        else トークン一致
            CC->>MSS: currentMember(session)
            MSS-->>CC: Optional<MemberSessionUser>
            CC->>OS: placeOrder(memberId, form, cart)
            OS->>OS: 業務ルール検証（在庫確認・法人条件・階数等）
            alt 業務エラー（在庫不足等）
                OS-->>CC: IllegalArgumentException
                CC-->>Browser: redirect:/checkout/input (業務エラーフラッシュ)
            else 検証OK
                OS->>OR: nextOrderSequence(orderDate)
                OR->>DB: INSERT INTO order_number_counters ... ON CONFLICT DO UPDATE SET last_sequence = last_sequence + 1
                DB-->>OR: 採番された連番
                OR-->>OS: 連番
                OS->>OS: 注文番号生成 (ORD{date}-{seq6桁})
                OS->>OR: insertOrder(orderData)
                OR->>DB: INSERT INTO orders (...)
                DB-->>OR: orderId
                OS->>OR: insertOrderItem(orderItems) ×明細数
                OR->>DB: INSERT INTO order_items (...) ×明細数
                OS->>OR: insertOrderStatusHistory(orderId, '受付')
                OR->>DB: INSERT INTO order_status_histories (...)
                DB-->>OR: OK
                OR-->>OS: void
                OS-->>CC: orderNumber
                CC->>CC: clearCheckoutSession(session)
                CC->>CS: clear(request, response)
                CS->>CS: カート Cookie をクリア
                CC->>NMS: (トランザクションコミット後) 注文完了メール送信
                NMS->>NMS: メール本文生成 (order-complete-body.txt)
                NMS-->>Browser: 注文完了メール送信
                CC-->>Browser: redirect:/checkout/complete/{orderNumber}
            end
        end
    end

    Browser->>CC: GET /checkout/complete/{orderNumber}
    CC->>OR: findOrderCompleteByOrderNumber(orderNumber)
    OR->>DB: SELECT * FROM orders WHERE order_number = ?
    DB-->>OR: 注文情報
    OR-->>CC: OrderCompleteView
    CC-->>Browser: 注文完了画面 (checkout-complete.html)
```

---

## 7. 購入履歴・再購入フロー

購入履歴は `order_items` のスナップショットデータを表示する。再購入は現在有効な商品バリアントを検索しカートへ追加する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant MPC as MyPageController
    participant OS as OrderService
    participant OR as OrderRepository
    participant CS as CartService
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Note over Browser,DB: 購入履歴一覧
    Browser->>MPC: GET /mypage/orders?page=1
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser (未ログインなら 401)
    MPC->>OS: findMemberOrderHistories(memberId, page)
    OS->>OR: findMemberOrders(memberId, page)
    OR->>DB: SELECT * FROM orders WHERE member_id = ? ORDER BY ordered_at DESC LIMIT ? OFFSET ?
    DB-->>OR: 注文一覧
    OR-->>OS: MemberOrderHistoryPage
    OS-->>MPC: MemberOrderHistoryPage
    MPC-->>Browser: 購入履歴一覧画面 (mypage-orders-list.html)

    Note over Browser,DB: 購入履歴詳細
    Browser->>MPC: GET /mypage/orders/{orderNumber}
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser
    MPC->>OS: findMemberOrderDetail(memberId, orderNumber)
    OS->>OR: findMemberOrderDetail(memberId, orderNumber)
    OR->>DB: SELECT orders.*, order_items.*, order_status_histories.* WHERE order_number = ? AND member_id = ?
    DB-->>OR: 注文詳細
    OR-->>OS: Optional<MemberOrderDetailView>
    alt 注文が見つからない
        MPC-->>Browser: 404 Not Found
    else 注文あり
        MPC-->>Browser: 購入履歴詳細画面 (mypage-order-detail.html)
    end

    Note over Browser,DB: 再購入
    Browser->>MPC: POST /mypage/orders/{orderNumber}/reorder
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser
    MPC->>OS: findReorderItems(memberId, orderNumber)
    OS->>OR: findMemberReorderItems(memberId, orderNumber)
    OR->>DB: SELECT oi.*, pv.id AS product_variant_id FROM order_items oi LEFT JOIN product_variants pv ON oi.product_code = pv.product_code WHERE order_number = ?
    DB-->>OR: 再購入商品一覧 (現在有効なバリアントIDを結合)
    OR-->>OS: List<OrderReorderItem>
    OS-->>MPC: List<OrderReorderItem>
    loop 各再購入商品
        MPC->>CS: addItem(request, response, productVariantId, quantity, assemblyRequested)
        CS-->>MPC: 成功 または IllegalArgumentException (在庫切れ等)
    end
    alt 全商品追加失敗
        MPC-->>Browser: redirect:/cart (全件失敗フラッシュ)
    else 1件以上追加成功
        MPC-->>Browser: redirect:/cart (成功フラッシュ、部分失敗があれば警告も表示)
    end
```

---

## 8. 会員情報変更フロー

変更成功後はセッションの会員情報を最新化してからリダイレクトする。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant MPC as MyPageController
    participant MS as MemberService
    participant MR as MemberRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Browser->>MPC: GET /mypage/profile
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser (未ログインなら 401)
    MPC->>MS: findProfileByMemberId(memberId)
    MS->>MR: findProfileByMemberId(memberId)
    MR->>DB: SELECT * FROM members WHERE member_id = ? AND member_status = 'active'
    DB-->>MR: 会員プロフィール
    MR-->>MS: MemberProfileEditForm
    MS-->>MPC: MemberProfileEditForm
    MPC-->>Browser: 会員情報変更画面 (mypage-profile-edit.html)

    Browser->>MPC: POST /mypage/profile (フォーム送信)
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser
    MPC->>MPC: @Valid バリデーション + 法人条件チェック
    alt バリデーションエラー
        MPC-->>Browser: 会員情報変更画面 (エラー表示)
    else バリデーションOK
        MPC->>MS: updateProfile(memberId, form)
        MS->>MR: existsByEmailForOtherMember(email, memberId)
        MR->>DB: SELECT COUNT(*) FROM members WHERE email = ? AND member_id != ?
        DB-->>MR: 0 または 1
        alt メールアドレス重複
            MS-->>MPC: DuplicateEmailException
            MPC-->>Browser: 会員情報変更画面 (重複エラー表示)
        else 重複なし
            MS->>MR: updateProfile(memberId, form)
            MR->>DB: UPDATE members SET last_name=?, first_name=?, email=?, ... WHERE member_id=?
            DB-->>MR: 更新件数
            MR-->>MS: void
            MS-->>MPC: void
            MPC->>MS: findActiveById(memberId)
            MS->>MR: findActiveById(memberId)
            MR->>DB: SELECT * FROM members WHERE member_id = ?
            DB-->>MR: 最新会員情報
            MR-->>MS: MemberSessionUser
            MS-->>MPC: MemberSessionUser
            MPC->>MSS: login(request, updatedMember)
            MSS->>MSS: セッション会員情報を最新値で更新
            MPC-->>Browser: redirect:/mypage/profile (更新完了フラッシュ表示)
        end
    end
```

---

## 9. 追加お届け先管理フロー

新規登録・編集・削除の3操作を含む。登録上限（システム定義の上限件数）に達した場合は追加不可。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant MPC as MyPageController
    participant MS as MemberService
    participant MR as MemberRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Note over Browser,DB: 一覧表示
    Browser->>MPC: GET /mypage/addresses
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser
    MPC->>MS: findAdditionalAddresses(memberId, page)
    MS->>MR: findAdditionalAddresses(memberId, page)
    MR->>DB: SELECT * FROM member_additional_addresses WHERE member_id = ? ORDER BY created_at LIMIT ? OFFSET ?
    DB-->>MR: お届け先一覧
    MR-->>MS: MemberAdditionalAddressPage
    MS-->>MPC: MemberAdditionalAddressPage
    MPC-->>Browser: お届け先一覧画面 (mypage-addresses.html)

    Note over Browser,DB: 新規登録
    Browser->>MPC: GET /mypage/addresses/new
    MPC->>MS: isAddressLimitReached(memberId)
    MS->>MR: countAdditionalAddresses(memberId)
    MR->>DB: SELECT COUNT(*) FROM member_additional_addresses WHERE member_id = ?
    DB-->>MR: 件数
    MR-->>MS: 件数
    MS-->>MPC: 上限到達フラグ
    MPC-->>Browser: お届け先登録フォーム (mypage-address-form.html)

    Browser->>MPC: POST /mypage/addresses (フォーム送信)
    MPC->>MPC: @Valid バリデーション + 法人条件チェック
    alt バリデーションエラー
        MPC-->>Browser: お届け先登録フォーム (エラー表示)
    else バリデーションOK
        MPC->>MS: createAdditionalAddress(memberId, form)
        MS->>MR: countAdditionalAddresses(memberId)
        MR->>DB: SELECT COUNT(*) FROM member_additional_addresses WHERE member_id = ?
        DB-->>MR: 件数
        alt 上限超過
            MS-->>MPC: AddressLimitExceededException
            MPC-->>Browser: redirect:/mypage/addresses (上限エラーフラッシュ)
        else 上限以内
            MS->>MR: insertAdditionalAddress(memberId, form)
            MR->>DB: INSERT INTO member_additional_addresses (...)
            DB-->>MR: OK
            MR-->>MS: void
            MS-->>MPC: void
            MPC-->>Browser: redirect:/mypage/addresses
        end
    end

    Note over Browser,DB: 編集
    Browser->>MPC: POST /mypage/addresses/{memberAddressId} (フォーム送信)
    MPC->>MS: findAdditionalAddressById(memberId, memberAddressId)
    MS->>MR: findAdditionalAddressById(memberId, memberAddressId)
    MR->>DB: SELECT * FROM member_additional_addresses WHERE id = ? AND member_id = ?
    DB-->>MR: 1件 または null
    alt 見つからない
        MPC-->>Browser: 404 Not Found
    else 見つかった
        MPC->>MPC: @Valid バリデーション + 法人条件チェック
        alt バリデーションエラー
            MPC-->>Browser: お届け先編集フォーム (エラー表示)
        else OK
            MPC->>MS: updateAdditionalAddress(memberId, memberAddressId, form)
            MS->>MR: updateAdditionalAddress(memberId, memberAddressId, form)
            MR->>DB: UPDATE member_additional_addresses SET ... WHERE id = ? AND member_id = ?
            DB-->>MR: 更新件数
            MR-->>MS: boolean
            MS-->>MPC: boolean
            MPC-->>Browser: redirect:/mypage/addresses
        end
    end

    Note over Browser,DB: 削除
    Browser->>MPC: POST /mypage/addresses/{memberAddressId}/delete
    MPC->>MS: deleteAdditionalAddress(memberId, memberAddressId)
    MS->>MR: deleteAdditionalAddress(memberId, memberAddressId)
    MR->>DB: DELETE FROM member_additional_addresses WHERE id = ? AND member_id = ?
    DB-->>MR: 削除件数
    MR-->>MS: void
    MS-->>MPC: void
    MPC-->>Browser: redirect:/mypage/addresses
```

---

## 10. 退会フロー

物理削除ではなく `member_status='withdrawn'` への論理削除を行い、セッションを破棄してログアウトする。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant MPC as MyPageController
    participant MS as MemberService
    participant MR as MemberRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Browser->>MPC: GET /mypage/withdraw
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser (未ログインなら 401)
    MPC-->>Browser: 退会確認画面 (mypage-withdraw.html)

    Browser->>MPC: POST /mypage/withdraw (退会確定)
    MPC->>MSS: currentMember(session)
    MSS-->>MPC: MemberSessionUser
    MPC->>MS: withdraw(memberId)
    MS->>MR: withdrawMember(memberId)
    MR->>DB: UPDATE members SET member_status='withdrawn', withdrawn_at=NOW() WHERE member_id=?
    DB-->>MR: 更新件数
    MR-->>MS: void
    MS-->>MPC: void
    MPC->>MSS: clear(session)
    MSS->>MSS: session.invalidate()
    MPC-->>Browser: redirect:/login
```

---

## 11. お問い合わせ送信フロー

ログイン済み会員の場合は氏名・メールアドレスをフォームに事前入力する。送信成功後は PRG でリダイレクトし、フラッシュメッセージを表示する。

```mermaid
sequenceDiagram
    actor Browser as ブラウザ
    participant CoC as ContactController
    participant CoS as ContactService
    participant CR as ContactRepository
    participant DB as PostgreSQL
    participant MSS as MemberSessionService

    Browser->>CoC: GET /contact
    CoC->>MSS: currentMember(session)
    MSS-->>CoC: Optional<MemberSessionUser>
    alt ログイン済み
        CoC->>CoS: createInitialForm(member)
        CoS->>CR: findMemberPrefill(memberId)
        CR->>DB: SELECT last_name, first_name, email FROM members WHERE member_id = ?
        DB-->>CR: 会員情報
        CR-->>CoS: ContactMemberPrefill
        CoS-->>CoC: ContactForm (事前入力済み)
    else 未ログイン
        CoC->>CoS: createInitialForm(empty)
        CoS-->>CoC: 空の ContactForm
    end
    CoC-->>Browser: お問い合わせ画面 (contact.html)

    Browser->>CoC: POST /contact (フォーム送信)
    CoC->>CoC: @Valid バリデーション
    alt バリデーションエラー
        CoC-->>Browser: お問い合わせ画面 (エラー表示)
    else バリデーションOK
        CoC->>MSS: currentMember(session)
        MSS-->>CoC: Optional<MemberSessionUser>
        CoC->>CoS: submit(memberId, form)
        CoS->>CR: insertInquiry(memberId, form)
        CR->>DB: INSERT INTO inquiries (member_id, inquiry_type, contact_message, ...) VALUES (...)
        DB-->>CR: inquiryId
        CR-->>CoS: inquiryId
        CoS-->>CoC: inquiryId
        CoC-->>Browser: redirect:/contact (受付完了フラッシュ表示)
    end
```

---

## 12. バッチ実行フロー

`InternalBatchController` の REST API 経由で Spring Batch ジョブを起動・停止する。バッチは `order_items` を集計して `popular_product_rankings` や `recommended_related_products` を洗い替え更新する。

```mermaid
sequenceDiagram
    actor Caller as 呼び出し元 (スケジューラ等)
    participant IBC as InternalBatchController
    participant BES as BatchExecutionService
    participant SBL as Spring Batch Launcher
    participant BR as BatchRepository
    participant DB as PostgreSQL

    Note over Caller,DB: ジョブ一覧取得
    Caller->>IBC: GET /internal/batch/jobs
    IBC->>BES: listJobs()
    BES-->>IBC: List<BatchJobSummaryResponse>
    IBC-->>Caller: 200 OK (ジョブ一覧 JSON)

    Note over Caller,DB: ジョブ起動
    Caller->>IBC: POST /internal/batch/jobs/{jobName}/executions
    IBC->>BES: startJob(jobName)
    BES->>BES: JobRegistry からジョブを検索
    alt ジョブが存在しない
        BES-->>IBC: UnknownBatchJobException
        IBC-->>Caller: 404 Not Found
    else ジョブあり
        BES->>BES: 実行中の同名ジョブ確認
        alt 既に実行中
            BES-->>IBC: BatchAlreadyRunningException
            IBC-->>Caller: 409 Conflict
        else 実行可能
            BES->>SBL: JobLauncher.run(job, jobParameters)
            SBL->>DB: INSERT INTO batch_job_instance / batch_job_execution / batch_job_execution_params
            SBL-->>BES: JobExecution (非同期実行開始)
            BES-->>IBC: BatchStartResponse (executionId)
            IBC-->>Caller: 202 Accepted (executionId)

            Note over SBL,DB: バッチ処理内容 (例: 売れ筋ランキング集計)
            SBL->>BR: findPopularRankingCandidates(targetDate)
            BR->>DB: SELECT product_id, COUNT(*) FROM order_items GROUP BY product_id ORDER BY COUNT(*) DESC
            DB-->>BR: 集計結果
            BR-->>SBL: List<PopularRankingCandidate>
            SBL->>BR: replacePopularRankings(targetDate, candidates)
            BR->>DB: DELETE FROM popular_product_rankings WHERE ranking_date = ?
            BR->>DB: INSERT INTO popular_product_rankings (...) ×件数
            DB-->>BR: OK
            BR-->>SBL: void
            SBL->>DB: UPDATE batch_job_execution SET status='COMPLETED', end_time=NOW()
        end
    end

    Note over Caller,DB: ジョブ停止
    Caller->>IBC: DELETE /internal/batch/jobs/{jobName}/executions/{executionId}
    IBC->>BES: stopJob(executionId)
    BES->>SBL: JobOperator.stop(executionId)
    alt executionId が存在しない
        BES-->>IBC: BatchExecutionNotFoundException
        IBC-->>Caller: 404 Not Found
    else 実行中でない
        BES-->>IBC: BatchExecutionNotRunningException
        IBC-->>Caller: 409 Conflict
    else 停止要求成功
        SBL->>DB: UPDATE batch_job_execution SET status='STOPPING'
        BES-->>IBC: void
        IBC-->>Caller: 200 OK (停止要求受付)
    end
```
