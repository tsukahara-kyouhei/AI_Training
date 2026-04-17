# 画面一覧

最終更新: 2026-03-10

---

## 1. 公開画面（認証不要）

| No. | 画面名 | 主な機能 | HTTP メソッド | URL | テンプレート |
|----:|--------|----------|:---:|-----|------------|
| 1 | トップ | 新着商品・おすすめ商品のハイライト表示 | GET | `/` | `pages/top` |
| 2 | お知らせ一覧 | サイトからのお知らせ一覧表示 | GET | `/announcements` | `pages/announcements` |
| 3 | 新着商品一覧 | 新着商品のフィルタ付き一覧・ページング | GET | `/products/new-arrivals` | `pages/product-list-new-arrivals` |
| 4 | 検索結果一覧 | キーワード検索による商品一覧 | GET | `/products/search` | `pages/product-list-search-results` |
| 5 | デスク一覧 | デスクカテゴリの商品フィルタ付き一覧 | GET | `/categories/desks` | `pages/product-list-category-desk` |
| 6 | チェア一覧 | チェアカテゴリの商品フィルタ付き一覧 | GET | `/categories/chairs` | `pages/product-list-category-chair` |
| 7 | 収納家具一覧 | 収納家具カテゴリの商品フィルタ付き一覧 | GET | `/categories/storages` | `pages/product-list-category-storage` |
| 8 | 商品詳細 | 商品情報・バリアント選択・お気に入り登録 | GET | `/products/{productId}` | `pages/product-detail` |
| 9 | カート | カート内商品の確認・数量変更・削除 | GET | `/cart` | `pages/cart` |
| 10 | 購入方法選択 | ゲスト購入 / 会員ログイン購入の選択 | GET | `/checkout/method` | `pages/checkout-method` |
| 11 | 注文情報入力 | 配送先・支払い方法などの注文情報入力 | GET / POST | `/checkout/input` | `pages/checkout-input` |
| 12 | 注文内容確認 | 注文情報の最終確認 | GET / POST | `/checkout/confirm` | `pages/checkout-confirm` |
| 13 | 注文完了 | 注文番号・完了メッセージ表示 | GET | `/checkout/complete/{orderNumber}` | `pages/checkout-complete` |
| 14 | お問い合わせ | 問い合わせ内容入力・送信（ログイン時は初期値補完） | GET / POST | `/contact` | `pages/contact` |
| 15 | OFFICE ORDER について | サービス概要・会社情報 | GET | `/about` | `pages/about` |
| 16 | ご利用ガイド | 注文・配送・返品などの利用手順案内 | GET | `/guide` | `pages/guide` |
| 17 | 利用規約 | サービス利用規約 | GET | `/legal/terms` | `pages/legal-terms` |
| 18 | プライバシーポリシー | 個人情報の取扱い方針 | GET | `/legal/privacy` | `pages/legal-privacy` |
| 19 | 特定商取引法に基づく表記 | 特商法必須表記 | GET | `/legal/tokusho` | `pages/legal-tokusho` |

---

## 2. 認証画面

| No. | 画面名 | 主な機能 | HTTP メソッド | URL | テンプレート |
|----:|--------|----------|:---:|-----|------------|
| 20 | ログイン | メールアドレス・パスワードによる会員ログイン | GET | `/login` | `pages/login` |
| 21 | 会員登録入力 | 会員情報（氏名・メール・パスワード等）の入力 | GET | `/members/register` | `pages/member-register` |
| 22 | 会員登録確認 | 入力した会員情報の確認 | POST | `/members/register/confirm` | `pages/member-register-confirm` |
| 23 | 会員登録完了 | 登録完了・会員コード表示 | GET | `/members/register/complete` | `pages/member-register-complete` |

---

## 3. マイページ（会員専用）

| No. | 画面名 | 主な機能 | HTTP メソッド | URL | テンプレート |
|----:|--------|----------|:---:|-----|------------|
| 24 | 購入履歴一覧 | 過去の注文一覧・ページング | GET | `/mypage/orders` | `pages/mypage-orders-list` |
| 25 | 購入履歴詳細 | 注文内容詳細・再購入 | GET | `/mypage/orders/{orderNumber}` | `pages/mypage-order-detail` |
| 26 | お気に入り一覧 | お気に入り登録商品の一覧・ページング | GET | `/mypage/favorites` | `pages/mypage-favorites` |
| 27 | 会員情報変更 | 氏名・メールアドレス・パスワード等の編集 | GET / POST | `/mypage/profile` | `pages/mypage-profile-edit` |
| 28 | 追加お届け先一覧 | 登録済みお届け先の一覧・ページング | GET | `/mypage/addresses` | `pages/mypage-addresses` |
| 29 | お届け先登録 | 新規お届け先の入力・登録 | GET / POST | `/mypage/addresses/new` | `pages/mypage-address-form` |
| 30 | お届け先編集 | 既存お届け先の修正・更新 | GET / POST | `/mypage/addresses/{memberAddressId}/edit` | `pages/mypage-address-form` |
| 31 | 退会確認 | 退会意思の最終確認・退会実行 | GET / POST | `/mypage/withdraw` | `pages/mypage-withdraw` |

---

## 4. エラー画面

| No. | 画面名 | 主な機能 | HTTP メソッド | URL | テンプレート |
|----:|--------|----------|:---:|-----|------------|
| 32 | エラー（汎用） | 404 / 4xx 系エラーの表示 | ANY | `/error` | `error/error` |
| 33 | サーバーエラー | 500 系エラーの表示 | ANY | `/error` | `error/500` |

---

## 5. API エンドポイント（画面なし）

| No. | 機能名 | 概要 | HTTP メソッド | URL | レスポンス |
|----:|--------|------|:---:|-----|-----------|
| A1 | 最近見た商品取得 | セッション内の最近閲覧商品リストを返す | GET | `/products/recently-viewed` | JSON |
| A2 | お気に入り切り替え | 商品のお気に入り追加・削除（未ログイン時はログイン画面へ） | POST | `/products/{productId}/favorite` | リダイレクト |
| A3 | カート商品追加 | 指定商品をカートに追加 | POST | `/cart/items` | リダイレクト |
| A4 | カート数量更新 | カート内商品の数量・組立指定を更新 | POST | `/cart/items/{productVariantId}/update` | リダイレクト |
| A5 | カート商品削除 | カート内の指定商品を削除 | POST | `/cart/items/{productVariantId}/delete` | リダイレクト |
| A6 | カートクリア | カートを空にする | POST | `/cart/clear` | リダイレクト |
| A7 | 再購入 | 購入履歴の注文内容をカートに追加 | POST | `/mypage/orders/{orderNumber}/reorder` | リダイレクト |
| A8 | お届け先削除 | マイページのお届け先を削除 | POST | `/mypage/addresses/{memberAddressId}/delete` | リダイレクト |
| A9 | 退会処理 | 会員退会・セッション破棄・ログイン画面へ | POST | `/mypage/withdraw` | リダイレクト |

---

## 備考

| 項目 | 内容 |
|------|------|
| 認証方式 | Spring Security による フォームログイン |
| セッション管理 | 購入フロー・会員登録確認はセッションで入力情報を保持 |
| アクセス制御 | `/mypage/**` は会員ログイン必須。未認証時はログイン画面へリダイレクト |
| ゲスト購入 | ログインなしで注文情報入力から購入完了まで可能 |
