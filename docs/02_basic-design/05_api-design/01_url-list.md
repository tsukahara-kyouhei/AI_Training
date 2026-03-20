# URL一覧

## 凡例

- **認証**: `public` = 認証不要 / `member` = ログイン必須
- **パスパラメータ**: `{xxx}` 形式
- **テンプレート**: Thymeleafテンプレートファイルのパス（`templates/pages/` 配下）

---

## 1. トップ・お知らせ（HomeController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/` | public | トップページ | `top.html` |
| GET | `/announcements` | public | お知らせ一覧 | `announcements.html` |

---

## 2. 商品カタログ（CatalogController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/products/new-arrivals` | public | 新着商品一覧 | `product-list-new-arrivals.html` |
| GET | `/products/search` | public | キーワード検索結果 | `product-list-search-results.html` |
| GET | `/categories/desks` | public | デスク一覧 | `product-list-category-desk.html` |
| GET | `/categories/chairs` | public | チェア一覧 | `product-list-category-chair.html` |
| GET | `/categories/storages` | public | 収納家具一覧 | `product-list-category-storage.html` |
| GET | `/products/{productId}` | public | 商品詳細 | `product-detail.html` |
| GET | `/products/recently-viewed` | public | 最近見た商品（セッション） | - (JSON/redirect) |
| POST | `/products/{productId}/favorite` | member | お気に入り追加・削除 | - (redirect) |

---

## 3. カート（CartController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/cart` | public | カート表示 | `cart.html` |
| POST | `/cart/items` | public | 商品追加 | - (redirect) |
| POST | `/cart/items/{productVariantId}/update` | public | 数量変更 | - (redirect) |
| POST | `/cart/items/{productVariantId}/delete` | public | 商品削除 | - (redirect) |
| POST | `/cart/clear` | public | カートクリア | - (redirect) |

---

## 4. チェックアウト（CartController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/checkout/method` | public | 支払方法選択 | `checkout-method.html` |
| GET | `/checkout/input` | public | 注文情報入力 | `checkout-input.html` |
| POST | `/checkout/input` | public | 入力値検証・確認画面へ | `checkout-confirm.html` / redirect |
| GET | `/checkout/confirm` | public | 注文確認画面 | `checkout-confirm.html` |
| POST | `/checkout/confirm` | public | 注文確定（ワンタイムトークン検証） | - (redirect) |
| GET | `/checkout/complete` | public | 注文完了（セッションから注文番号取得） | `checkout-complete.html` |
| GET | `/checkout/complete/{orderNumber}` | public | 注文完了（注文番号指定） | `checkout-complete.html` |

---

## 5. 認証（AuthController / Spring Security）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/login` | public | ログイン画面 | `login.html` |
| POST | `/login` | public | ログイン処理（Spring Security） | - (redirect) |
| POST | `/logout` | member | ログアウト（Spring Security） | - (redirect) |

---

## 6. 会員登録（MemberRegistrationController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/members/register` | public | 会員登録フォーム | `member-register.html` |
| POST | `/members/register/confirm` | public | 入力確認・重複チェック | `member-register-confirm.html` |
| POST | `/members/register` | public | 会員登録確定 | - (redirect) |
| GET | `/members/register/complete` | public | 登録完了画面 | `member-register-complete.html` |

---

## 7. マイページ（MyPageController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/mypage` | member | マイページトップ | - (redirect) |
| GET | `/mypage/orders` | member | 注文履歴一覧 | `mypage-orders-list.html` |
| GET | `/mypage/orders/{orderNumber}` | member | 注文詳細 | `mypage-order-detail.html` |
| POST | `/mypage/orders/{orderNumber}/reorder` | member | 再購入 | - (redirect) |
| GET | `/mypage/favorites` | member | お気に入り一覧 | `mypage-favorites.html` |
| GET | `/mypage/profile` | member | プロフィール編集フォーム | `mypage-profile-edit.html` |
| POST | `/mypage/profile` | member | プロフィール更新 | - (redirect) |
| GET | `/mypage/addresses` | member | 追加お届け先一覧 | `mypage-addresses.html` |
| GET | `/mypage/addresses/new` | member | 追加お届け先登録フォーム | `mypage-address-form.html` |
| GET | `/mypage/addresses/{memberAddressId}/edit` | member | 追加お届け先編集フォーム | `mypage-address-form.html` |
| POST | `/mypage/addresses` | member | 追加お届け先登録 | - (redirect) |
| POST | `/mypage/addresses/{memberAddressId}` | member | 追加お届け先更新 | - (redirect) |
| POST | `/mypage/addresses/{memberAddressId}/delete` | member | 追加お届け先削除 | - (redirect) |
| GET | `/mypage/withdraw` | member | 退会確認画面 | `mypage-withdraw.html` |
| POST | `/mypage/withdraw` | member | 退会処理 | - (redirect) |

---

## 8. お問い合わせ（ContactController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/contact` | public | お問い合わせフォーム | `contact.html` |
| POST | `/contact` | public | お問い合わせ送信 | - (redirect) |

---

## 9. 静的コンテンツ（ContentController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| GET | `/about` | public | 会社概要 | `about.html` |
| GET | `/guide` | public | ご利用ガイド | `guide.html` |
| GET | `/legal/terms` | public | 利用規約 | `legal-terms.html` |
| GET | `/legal/privacy` | public | プライバシーポリシー | `legal-privacy.html` |
| GET | `/legal/tokusho` | public | 特定商取引法に基づく表記 | `legal-tokusho.html` |

---

## 10. エラー（AppErrorController）

| メソッド | URL | 認証 | 概要 | テンプレート |
|---|---|---|---|---|
| ANY | `/error` | public | エラーハンドリング（Springデフォルト） | `error/error.html` / `error/500.html` |

---

## 11. 内部API（InternalBatchController）※ localプロファイル限定

| メソッド | URL | 認証 | 概要 | レスポンス |
|---|---|---|---|---|
| GET | `/internal/batch/jobs` | - | ジョブ一覧取得 | JSON |
| GET | `/internal/batch/jobs/{jobName}/executions` | - | 実行履歴取得 | JSON |
| POST | `/internal/batch/jobs/{jobName}/executions` | - | バッチ手動起動 | JSON |
| POST | `/internal/batch/executions/{executionId}/stop` | - | バッチ停止 | JSON |
