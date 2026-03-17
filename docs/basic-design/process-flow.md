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

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as CatalogController
    participant FO as ProductFilterOptionService
    participant LS as ProductListSearchService
    participant PS as ProductService
    participant DB as Database

    B->>C: GET /products/search?q=キーワード&...
    C->>FO: loadOptionsBundle()
    FO->>DB: colors, price_range 取得
    DB-->>FO: 絞り込み候補
    FO-->>C: ProductFilterOptionsBundle
    C->>LS: buildSearchCondition(...)
    Note over LS: 全角→半角正規化<br/>価格帯ID・カラーキー変換<br/>ページ番号・サイズ検証
    LS-->>C: ProductSearchCondition
    C->>LS: searchWithPageCorrection(condition)
    LS->>PS: searchProducts(condition)
    PS->>DB: ① 商品コード完全一致検索
    alt ヒットなし
        PS->>DB: ② 商品コード前方一致検索
    end
    PS->>DB: ③ product_name等 部分一致検索 + フィルタ適用
    DB-->>PS: 検索結果
    PS-->>LS: ProductListPage
    alt ページ超過
        LS->>PS: 最終ページで再検索
        PS-->>LS: 補正後結果
    end
    LS-->>C: ProductListSearchResult
    Note over C: Model へ検索結果・フィルタ選択肢を設定
    C-->>B: product-list-search-results.html
```

### 1.2 商品詳細表示

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as CatalogController
    participant PS as ProductService
    participant MS as MemberService
    participant DB as Database
    participant S as Session

    B->>C: GET /products/{productId}
    C->>PS: findDetail(productId, selectedVariantId?)
    PS->>DB: products, product_variants, colors,<br/>product_desk/chair/storage_attributes,<br/>recommended_related_products 結合取得
    DB-->>PS: 商品詳細データ
    Note over PS: 選択中バリアント・在庫状態を解決
    PS-->>C: ProductDetailView
    opt ログイン会員
        C->>MS: isFavorite(memberId, productId)
        MS->>DB: member_favorites 確認
        DB-->>MS: お気に入り状態
        MS-->>C: boolean
    end
    C->>S: 最近見た商品リストを更新（上限４件）
    Note over C: Model に ProductDetailView を設定
    C-->>B: product-detail.html
```

---

## 2. 購入フロー

### 2.1 カート追加

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as CartController
    participant CS as CartService

    B->>C: POST /cart/add (productVariantId, quantity, assemblyRequested)
    C->>CS: addToCart(request, response, ...)
    Note over CS: Cookie から CartCookieStore を取得
    alt 同一バリアント・同一組立フラグが既存
        Note over CS: 数量加算（上限99）
    else 新規アイテム
        Note over CS: リストへ追加
    end
    Note over CS: Cookie を更新して保存（有効期限30日）
    CS-->>C: 更新済みカート
    Note over C: flash メッセージ設定
    C-->>B: redirect: 元の商品詳細ページ or カート
```

### 2.2 購入フロー（注文確定まで）

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as CartController
    participant OS as OrderService
    participant CS as CartService
    participant NS as NotificationMailService
    participant DB as Database
    participant S as Session

    B->>C: GET /cart/checkout/method
    Note over C: カート空チェック（空ならカートへリダイレクト）
    C-->>B: checkout-method.html（ゲスト/会員ログイン選択）

    B->>C: POST /cart/checkout/method
    Note over C,S: 選択方式をセッション保存
    C-->>B: redirect: /cart/checkout/input

    B->>C: GET /cart/checkout/input
    opt ログイン会員
        C->>OS: buildMemberPrefill(member)
        OS->>DB: members テーブルから氏名・住所取得
        DB-->>OS: 会員情報
        OS-->>C: CheckoutMemberPrefill
    end
    C-->>B: checkout-input.html（初期値あり）

    B->>C: POST /cart/checkout/input
    Note over C: Bean Validation + 条件付き検証（法人時 company_name 必須）
    Note over C: フォーム正規化（全角→半角等）
    Note over C,S: セッション保存（CheckoutInputForm）・ワンタイムトークン生成
    C-->>B: redirect: /cart/checkout/confirm

    B->>C: GET /cart/checkout/confirm
    C->>CS: getCart()
    CS-->>C: CartView
    C->>DB: 消費税率取得
    DB-->>C: tax_rate_percent
    Note over C: 送料・組立費・税込合計を計算
    C-->>B: checkout-confirm.html

    B->>C: POST /cart/checkout/confirm（one-time token）
    Note over C: セッション内トークンと照合（二重送信防止）
    C->>OS: placeOrder(cartView, form, member?)
    OS->>DB: order_number_counters SELECT FOR UPDATE → 採番
    OS->>DB: orders INSERT
    OS->>DB: order_items INSERT（カート明細分）
    OS->>DB: order_status_histories INSERT（received）
    Note over OS: トランザクションコミット
    OS->>CS: clearCart()
    Note over CS: Cookie クリア
    OS->>NS: sendOrderCompleteMail()（コミット後非同期）
    OS-->>C: OrderCompleteView
    Note over C,S: セッションからフォーム・トークンを削除
    C-->>B: redirect: /cart/checkout/complete

    B->>C: GET /cart/checkout/complete
    Note over C: フラッシュ属性から OrderCompleteView 取得
    C-->>B: checkout-complete.html
```

---

## 3. 会員登録フロー

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as MemberRegistrationController
    participant MS as MemberService
    participant SS as MemberSessionService
    participant NS as NotificationMailService
    participant DB as Database
    participant S as Session

    B->>C: GET /members/register
    opt セッションに保留フォームあり
        C->>S: 保留フォームを取得し初期値設定
    end
    C-->>B: member-register.html

    B->>C: POST /members/register/confirm
    Note over C: Bean Validation
    C->>MS: existsByEmail(email)
    MS->>DB: members テーブル検索
    DB-->>MS: 存在確認結果
    MS-->>C: boolean
    Note over C: 条件付きバリデーション（法人区分等）
    alt エラーあり
        C-->>B: member-register.html（エラー表示）
    else エラーなし
        Note over C: フォーム正規化
        C->>S: 保留フォームをセッション保存
        C-->>B: member-register-confirm.html
    end

    B->>C: POST /members/register/complete
    C->>S: 保留フォームを取得
    C->>MS: registerMember(form)
    Note over MS: existsByEmail（二重送信対策再チェック）
    Note over MS: BCryptPasswordEncoder#encode(password)
    MS->>DB: members テーブルへ INSERT
    DB-->>MS: member_id
    MS-->>C: 登録済み会員情報
    C->>SS: login(session, memberSessionUser)
    C->>NS: sendMemberRegistrationMail(member)
    C->>S: 保留フォームを削除
    C-->>B: redirect: /members/register/complete
    B->>C: GET /members/register/complete
    C-->>B: member-register-complete.html
```

---

## 4. 認証フロー

### 4.1 ログイン

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant SP as Spring Security
    participant UDS as UserDetailsService
    participant DB as Database
    participant S as Session

    B->>SP: POST /login（email, password）
    SP->>UDS: loadUserByUsername(email)
    UDS->>DB: members テーブルをメールアドレス（小文字）で検索
    DB-->>UDS: 会員情報
    Note over UDS: BCryptPasswordEncoder でパスワード検証
    Note over UDS: member_status = 'active' チェック
    UDS-->>SP: UserDetails
    Note over SP,S: セッションに MemberSessionUser を保存
    SP-->>B: redirect パラメータに応じたページへ遷移
```

### 4.2 ログアウト

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant SP as Spring Security
    participant S as Session

    B->>SP: POST /logout
    Note over SP,S: セッション破棄
    SP-->>B: redirect: /
```

---

## 5. マイページフロー

### 5.1 購入履歴・再注文

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as MyPageController
    participant OS as OrderService
    participant CS as CartService
    participant DB as Database

    B->>C: GET /mypage/orders
    Note over C: 認証チェック（未認証はログイン画面へ）
    C->>OS: findOrderHistory(memberId, page)
    OS->>DB: orders / order_items をページ付きで取得
    DB-->>OS: 注文一覧
    OS-->>C: MemberOrderHistoryPage
    C-->>B: mypage-orders-list.html

    B->>C: POST /mypage/orders/{orderId}/reorder
    C->>OS: findReorderItems(orderId, memberId)
    OS->>DB: order_items 取得
    DB-->>OS: 注文明細
    OS-->>C: List 注文再購入情報
    C->>CS: bulkAddToCart(reorderItems, ...)
    C-->>B: redirect: /cart
```

### 5.2 お気に入り

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as CatalogController
    participant MS as MemberService
    participant DB as Database

    B->>C: POST /products/{productId}/favorite
    Note over C: 認証チェック（未認証は401）
    C->>MS: toggleFavorite(memberId, productId)
    MS->>DB: member_favorites 確認
    alt 未登録
        Note over MS: 件数チェック（上限20件）
        MS->>DB: INSERT
    else 登録済み
        MS->>DB: DELETE
    end
    DB-->>MS: 完了
    MS-->>C: 更新後状態
    C-->>B: JSON レスポンス or redirect
```

---

## 6. バッチ処理フロー

```mermaid
flowchart TD
    A(["スケジューラ / 手動起動\nPOST /internal/batch/ranking"])
    B["InternalBatchController"]
    C["Spring Batch Job 起動\n売れ筋ランキング集計ジョブ"]
    D["Step1: orders / order_items から\n直近1か月の販売数集計"]
    E["Step2: popular_product_rankings\nテーブルを UPSERT"]
    F["Step3: recommended_related_products\nテーブルを更新"]
    G(["ジョブ実行結果ログ出力"])

    A --> B --> C --> D --> E --> F --> G
```

---

## 7. お問い合わせフロー

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant C as ContactController
    participant CS as ContactService
    participant DB as Database

    B->>C: GET /contact
    opt ログイン会員
        Note over C: 氏名・メールアドレスを初期値設定
    end
    C-->>B: contact.html

    B->>C: POST /contact
    Note over C: Bean Validation
    C->>CS: save(form)
    CS->>DB: inquiries テーブルへ INSERT
    DB-->>CS: inquiry_id
    CS-->>C: 保存完了
    C-->>B: redirect: /contact?submitted=true
```
