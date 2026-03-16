# 処理フロー

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 基本設計書 |
| 対象システム | office-order （オフィス家具ECサイト） |
| 作成日 | 2026-03-16 |
| バージョン | 1.0 |

---

## 1. 商品検索・閲覧フロー

### 1.1 キーワード検索

```
[ブラウザ]
    │  GET /products/search?q=キーワード&...
    ▼
[CatalogController#searchResults]
    │  1. ProductFilterOptionService.loadOptionsBundle()
    │     └─ DB: colors, price_range 取得
    │  2. ProductListSearchService.buildSearchCondition()
    │     ├─ キーワードの全角→半角正規化（Java層）
    │     ├─ 価格帯ID・カラーキー → 型付きオブジェクトへ変換
    │     └─ ページ番号・サイズのバリデーション
    │  3. ProductListSearchService.searchWithPageCorrection()
    │     └─ ProductService.searchProducts(condition)
    │        └─ ProductRepository(MyBatis)
    │           └─ SQL:
    │              ① 商品コード完全一致検索
    │              ② ヒット無しの場合のみ商品コード前方一致検索
    │              ③ product_name / variation_name / description 部分一致検索
    │              ④ カテゴリ・カラー・価格帯・在庫フィルタ適用
    │  4. ページ超過時は最終ページへ補正し再検索
    │  5. Model に検索結果・フィルタ選択肢を設定
    ▼
[product-list-search-results.html レンダリング]
```

### 1.2 商品詳細表示

```
[ブラウザ]
    │  GET /products/{productId}
    ▼
[CatalogController#productDetail]
    │  1. ProductService.findDetail(productId, selectedVariantId?)
    │     └─ DB: products, product_variants, colors,
    │             product_desk/chair/storage_attributes,
    │             recommended_related_products 結合取得
    │  2. 選択中バリアント・在庫状態を解決
    │  3. ログイン会員の場合: お気に入り状態を取得
    │  4. セッションより「最近見た商品」リストを更新
    │  5. Model に ProductDetailView を設定
    ▼
[product-detail.html レンダリング]
```

---

## 2. 購入フロー

### 2.1 カート追加

```
[ブラウザ]
    │  POST /cart/add  (productVariantId, quantity, assemblyRequested)
    ▼
[CartController#addToCart]
    │  1. CartService.addToCart(request, response, ...)
    │     ├─ Cookie から CartCookieStore を取得
    │     ├─ 同一バリアント・同一組立フラグが既存ならば数量加算（上限99）
    │     └─ Cookie を更新して保存
    │  2. flash メッセージ設定
    ▼
[redirect: 元の商品詳細ページ or カート]
```

### 2.2 購入フロー（注文確定まで）

```
[ブラウザ]
    │  GET /cart/checkout/method
    ▼
[CartController#checkoutMethod]
    │  カート空チェック → 空の場合はカートへリダイレクト
    ▼
[checkout-method.html]（ゲスト / 会員ログイン 選択）

    │  POST /cart/checkout/method
    ▼
[CartController#submitCheckoutMethod]
    │  選択方式をセッション保存後 /cart/checkout/input へ転送

─────────────────────────────────────────
[ブラウザ]
    │  GET /cart/checkout/input
    ▼
[CartController#checkoutInput]
    │  1. ログイン会員の場合: OrderService.buildMemberPrefill()
    │     └─ DB: members テーブルから氏名・住所取得
    │  2. Model に CheckoutInputForm（初期値あり）を設定
    ▼
[checkout-input.html]

    │  POST /cart/checkout/input
    ▼
[CartController#submitCheckoutInput]
    │  1. Bean Validation（@Valid）
    │  2. 条件付きバリデーション（法人の場合 company_name 必須 等）
    │  3. フォームを正規化（全角→半角 等）
    │  4. セッション保存（CHECKOUT_FORM_SESSION_KEY）
    │  5. ワンタイムトークン生成・セッション保存
    ▼
[redirect: /cart/checkout/confirm]

─────────────────────────────────────────
[ブラウザ]
    │  GET /cart/checkout/confirm
    ▼
[CartController#checkoutConfirm]
    │  1. セッションから CheckoutInputForm 取得
    │  2. CartService.getCart() でカート内容取得
    │  3. 消費税率を DB から取得
    │  4. 送料・組立費・税込合計を計算表示
    ▼
[checkout-confirm.html]

    │  POST /cart/checkout/confirm  (one-time token)
    ▼
[CartController#submitCheckoutConfirm]
    │  1. セッション内トークンと照合（CSRF二重送信防止）
    │  2. OrderService.placeOrder(cartView, form, member?)
    │     ├─ 注文番号採番（order_number_counters テーブルをロック更新）
    │     ├─ orders / order_items / order_status_histories INSERT
    │     ├─ CartService.clearCart() （Cookie クリア）
    │     └─ トランザクションコミット後メール送信予約
    │  3. セッションから CheckoutInputForm・トークンを削除
    │  4. 注文完了情報をフラッシュ属性へ設定
    ▼
[redirect: /cart/checkout/complete]

─────────────────────────────────────────
[ブラウザ]
    │  GET /cart/checkout/complete
    ▼
[CartController#checkoutComplete]
    │  フラッシュ属性から OrderCompleteView 取得
    ▼
[checkout-complete.html]
```

---

## 3. 会員登録フロー

```
[ブラウザ]
    │  GET /members/register
    ▼
[MemberRegistrationController#showForm]
    │  セッションに保留フォームあれば初期値として設定
    ▼
[member-register.html]

    │  POST /members/register/confirm
    ▼
[MemberRegistrationController#confirm]
    │  1. Bean Validation
    │  2. メールアドレス重複チェック（MemberService.existsByEmail）
    │  3. 条件付きバリデーション（法人区分 等）
    │  4. フォームをセッション保存
    ▼
[member-register-confirm.html]

    │  POST /members/register/complete
    ▼
[MemberRegistrationController#complete]
    │  1. セッションから保留フォーム取得
    │  2. MemberService.registerMember(form)
    │     ├─ メールアドレス重複チェック（二重送信対策）
    │     ├─ パスワードをBCryptでハッシュ化
    │     └─ members テーブルへ INSERT
    │  3. MemberSessionService.login() でセッション確立
    │  4. NotificationMailService.sendMemberRegistrationMail()
    │  5. セッションから保留フォームを削除
    ▼
[redirect: /members/register/complete]
    ▼
[member-register-complete.html]
```

---

## 4. 認証フロー

### 4.1 ログイン

```
[ブラウザ]
    │  POST /login  (spring security フォームログイン)
    ▼
[Spring Security UserDetailsService（MemberCredential）]
    │  1. DB: members テーブルからメールアドレス（小文字で）検索
    │  2. BCryptPasswordEncoder でパスワード検証
    │  3. member_status = 'active' チェック
    │  4. セッションに MemberSessionUser を保存
    │  5. redirect パラメータに応じた戻り先へリダイレクト
    ▼
[ログイン後戻り先（マイページ等）]
```

### 4.2 ログアウト

```
[ブラウザ]
    │  POST /logout  (spring security)
    ▼
    │  セッション破棄
    ▼
[redirect: /]
```

---

## 5. マイページフロー

### 5.1 購入履歴・再注文

```
[ブラウザ]
    │  GET /mypage/orders
    ▼
[MyPageController#orders]
    │  認証チェック → 未認証はログイン画面へ
    │  OrderService.findOrderHistory(memberId, page)
    │  └─ DB: orders / order_items ページ付きで取得
    ▼
[mypage-orders-list.html]

    │  POST /mypage/orders/{orderId}/reorder
    ▼
[MyPageController#reorder]
    │  1. OrderService.findReorderItems(orderId, memberId)
    │  2. CartService.bulkAddToCart(reorderItems, ...)
    ▼
[redirect: /cart]
```

### 5.2 お気に入り

```
[ブラウザ]
    │  POST /products/{productId}/favorite  (Ajax / form)
    ▼
[CatalogController#toggleFavorite]
    │  1. 認証チェック
    │  2. MemberService.toggleFavorite(memberId, productId)
    │     ├─ 未登録なら INSERT (上限20件チェック)
    │     └─ 登録済みなら DELETE
    ▼
[JSONレスポンス or redirect]
```

---

## 6. バッチ処理フロー

```
[スケジューラ / 手動起動]
    │  POST /internal/batch/ranking
    ▼
[InternalBatchController]
    │  Spring Batch Job 起動
    │  └─ 売れ筋ランキング集計ジョブ
    │     ├─ Step1: orders / order_items から直近1か月の販売数集計
    │     ├─ Step2: popular_product_rankings テーブルを UPSERT
    │     └─ Step3: recommended_related_products テーブルを更新
    ▼
[ジョブ実行結果ログ出力]
```

---

## 7. お問い合わせフロー

```
[ブラウザ]
    │  GET /contact
    ▼
[ContactController#showForm]
    │  ログイン会員の場合は氏名・メールアドレスを初期値設定
    ▼
[contact.html]

    │  POST /contact
    ▼
[ContactController#submit]
    │  1. Bean Validation
    │  2. ContactService.save(form)
    │     └─ DB: inquiries テーブルへ INSERT
    ▼
[redirect: /contact?submitted=true]
```
