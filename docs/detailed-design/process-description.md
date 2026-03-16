# 処理記述

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 詳細設計書 |
| 対象システム | office-order （オフィス家具ECサイト） |
| 作成日 | 2026-03-16 |
| バージョン | 1.0 |

---

## 1. 商品一覧・検索（CatalogController）

### 1.1 商品検索結果表示 `GET /products/search`

**クラス:** `CatalogController#searchResults`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `ProductFilterOptionService#loadOptionsBundle()` を呼び出し、カラー・絞り込み候補一覧を取得する |
| 2 | `ProductListSearchService#buildSearchCondition()` を呼び出し、リクエストパラメータを `ProductSearchCondition` へ変換する。キーワードの全角→半角正規化はこのステップで実施する |
| 3 | `ProductListSearchService#searchWithPageCorrection()` を呼び出し、検索実行とページ補正を行う |
| 4 | 検索結果と絞り込み候補を `Model` へ設定し、テンプレートを返す |

**バリデーション:**
- `keyword` は最大255文字。超過した場合はページングエラーとして扱う
- `page` が検索結果の総ページ数を超えた場合、最終ページへ自動補正する
- `size` は許容値（15 / 30 / 60）以外は 15 へ補正する

**副作用:** なし（参照のみ）

---

### 1.2 商品詳細表示 `GET /products/{productId}`

**クラス:** `CatalogController#productDetail`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `ProductService#findDetail(productId, selectedVariantId)` で商品詳細情報を取得する |
| 2 | 指定のバリアントが存在しない場合は最安値バリアントをデフォルト選択する |
| 3 | `MemberService#isFavorite(memberId, productId)` でお気に入り状態を判定する（ログイン時のみ） |
| 4 | セッション内「最近見た商品」リストを更新する（上限4件、LIFO方式） |
| 5 | `ProductDetailView` を `Model` へ設定し、テンプレートを返す |

**異常系:**
- `productId` に対応する商品が存在しない場合 → `ResponseStatusException(404)` をスロー
- 販売期間外の商品 → 404 扱い

---

### 1.3 カテゴリ一覧表示 `GET /products/category/{category}`

**クラス:** `CatalogController` （desk / chair / storage ごとにメソッド分離）

| ステップ | 処理内容 |
|---------|---------|
| 1 | URL の `category` をカテゴリIDとして `ProductSearchCondition` を構築する |
| 2 | カテゴリに応じた絞り込み候補（天板形状・テイスト・素材 等）を取得する |
| 3 | `ProductListSearchService#searchWithPageCorrection()` で一覧取得する |

---

### 1.4 お気に入り登録・解除 `POST /products/{productId}/favorite`

**クラス:** `CatalogController#toggleFavorite`

| ステップ | 処理内容 |
|---------|---------|
| 1 | セッションからログイン会員を取得する。未ログインは 401 を返す |
| 2 | `MemberService#toggleFavorite(memberId, productId)` を呼び出す |
| 3 | 既登録の場合は DELETE、未登録の場合は件数チェック後 INSERT |
| 4 | 上限20件超過の場合 `FavoritesLimitExceededException` をスローし、エラーメッセージを返す |

---

## 2. カート・購入フロー（CartController）

### 2.1 カートへ追加 `POST /cart/add`

**クラス:** `CartController#addToCart`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `CartService#addToCart(request, response, productVariantId, quantity, assemblyRequested)` を呼び出す |
| 2 | Cookie `cart` を読み込み、JSON から `List<CartCookieItem>` へデシリアライズする |
| 3 | 同一 `productVariantId` + `assemblyRequested` の組み合わせが既存なら数量加算（上限99） |
| 4 | 新規アイテムの場合はリストへ追加する |
| 5 | リストを JSON にシリアライズして Cookie へ保存する（有効期限30日） |

**異常系:**
- `productVariantId` が存在しない場合 → 404

---

### 2.2 注文情報入力 `GET /cart/checkout/input`

**クラス:** `CartController#checkoutInput`

| ステップ | 処理内容 |
|---------|---------|
| 1 | カートが空の場合 `/cart` へリダイレクトする |
| 2 | セッションに既存フォームがあれば再表示する |
| 3 | ログイン会員の場合 `OrderService#buildMemberPrefill(member)` で初期値を生成し `CheckoutInputForm` へコピーする |
| 4 | 会員の追加お届け先一覧をモデルへ設定する |

---

### 2.3 注文情報確認送信 `POST /cart/checkout/input`

**クラス:** `CartController#submitCheckoutInput`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `@Valid` による Bean Validation を実行する |
| 2 | 法人区分 = `corporate` の場合 `company_name` 必須チェックを追加実施する |
| 3 | エラーあれば入力画面に戻る |
| 4 | `CheckoutInputForm#normalize()` でトリム・正規化を行う |
| 5 | フォームをセッション（`CHECKOUT_FORM_SESSION_KEY`）へ保存する |
| 6 | `UUID.randomUUID()` でワンタイムトークンを生成してセッションへ保存する |
| 7 | `/cart/checkout/confirm` へリダイレクトする |

---

### 2.4 注文確定 `POST /cart/checkout/confirm`

**クラス:** `CartController#submitCheckoutConfirm`

| ステップ | 処理内容 |
|---------|---------|
| 1 | セッション内トークンとリクエストトークンを照合する。不一致の場合 400 エラー |
| 2 | `OrderService#placeOrder(cartView, form, member)` を @Transactional で呼び出す |
| 3 | `OrderRepository#nextOrderSequence(today)` で日次連番を SELECT FOR UPDATE により採番する |
| 4 | 注文番号を `{YYYYMMDD}-{NNNNNN}` 形式で生成する |
| 5 | `orders` テーブルへ INSERT する |
| 6 | カート明細分だけ `order_items` テーブルへ INSERT する |
| 7 | `order_status_histories` テーブルへ最初のステータス `received` を INSERT する |
| 8 | コミット後フックで `NotificationMailService#sendOrderCompleteMail()` を呼び出す |
| 9 | `CartService#clearCart()` で Cookie をクリアする |
| 10 | セッションからフォームとトークンを削除する |
| 11 | `OrderCompleteView` をフラッシュ属性へ設定し `/cart/checkout/complete` へリダイレクトする |

**トランザクション境界:** ステップ3〜9が1トランザクション。メール送信はトランザクション外。

---

## 3. 会員登録（MemberRegistrationController）

### 3.1 会員登録確認 `POST /members/register/confirm`

**クラス:** `MemberRegistrationController#confirm`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `@Valid` による Bean Validation |
| 2 | `MemberService#existsByEmail(email)` でメールアドレス重複チェック |
| 3 | 法人区分時の `company_name` 必須チェック |
| 4 | エラーがあれば入力画面に戻る |
| 5 | `MemberRegisterForm#normalize()` でトリム |
| 6 | セッション（`PENDING_REGISTER_FORM`）へ保存する |
| 7 | 確認画面テンプレートを返す |

---

### 3.2 会員登録完了 `POST /members/register/complete`

**クラス:** `MemberRegistrationController#complete`

| ステップ | 処理内容 |
|---------|---------|
| 1 | セッションから保留フォームを取得する |
| 2 | `MemberService#registerMember(form)` を呼び出す（@Transactional） |
| 3 | メールアドレス重複チェック（二重送信対策の再チェック） |
| 4 | `BCryptPasswordEncoder#encode(password)` でパスワードをハッシュ化する |
| 5 | `members` テーブルへ INSERT する |
| 6 | `MemberSessionService#login(session, memberSessionUser)` でセッションを確立する |
| 7 | `NotificationMailService#sendMemberRegistrationMail(member)` でウェルカムメールを送信する |
| 8 | セッションから保留フォームを削除する |

---

## 4. マイページ（MyPageController）

### 4.1 購入履歴一覧 `GET /mypage/orders`

**クラス:** `MyPageController#orders`

| ステップ | 処理内容 |
|---------|---------|
| 1 | セッションからログイン会員を取得する。未ログインはログイン画面へリダイレクト |
| 2 | `OrderService#findOrderHistory(memberId, page)` を呼び出す |
| 3 | DB: `orders` テーブルから `member_id` 絞り込み・降順でページ付き取得 |
| 4 | `MemberOrderHistoryPage` を `Model` へ設定する |

---

### 4.2 会員情報編集 `POST /mypage/profile/edit`

**クラス:** `MyPageController#updateProfile`

| ステップ | 処理内容 |
|---------|---------|
| 1 | `@Valid` による Bean Validation |
| 2 | メールアドレスを変更した場合、重複チェックを実施する |
| 3 | `MemberService#updateProfile(memberId, form)` で `members` テーブルを UPDATE する |
| 4 | セッション内 `MemberSessionUser` を最新情報で更新する |

---

## 5. バッチ処理（InternalBatchController）

### 5.1 売れ筋ランキング集計

**起動方法:** `POST /internal/batch/ranking`（内部呼び出しのみ。外部公開不可）

| ステップ | 処理内容 |
|---------|---------|
| 1 | Spring Batch の `JobLauncher` でランキング集計ジョブを起動する |
| 2 | `order_items` と `orders` を JOIN し、直近30日の `product_id` 別販売数量を集計する |
| 3 | 上位10件を `popular_product_rankings` テーブルへ UPSERT する（集計日でパーティション） |
| 4 | 類似商品スコアを計算し `recommended_related_products` へ UPSERT する |

---

## 6. エラーハンドリング

| 例外クラス | 発生場面 | HTTP ステータス | 遷移先 |
|-----------|---------|----------------|--------|
| `ResponseStatusException(404)` | 商品未存在 | 404 | `error/error.html` |
| `ResponseStatusException(400)` | トークン不一致等 | 400 | `error/error.html` |
| `DuplicateEmailException` | メールアドレス重複 | ― | 入力画面にエラー表示 |
| `FavoritesLimitExceededException` | お気に入り上限超過 | ― | エラーメッセージを JSON 返却 |
| `AddressLimitExceededException` | お届け先上限超過 | ― | フラッシュメッセージ表示 |
| 非ハンドリング例外 | 予期せぬエラー | 500 | `error/500.html` |

**共通:** `GlobalExceptionLoggingAdvice` で全例外をロギングする。
