# 画面一覧

## 概要

| 項目 | 値 |
|---|---|
| アプリケーション名 | OFFICE ORDER |
| コンテキストパス | `/` |
| ポート | `8080` |
| セッションタイムアウト | 60 分 |

---

## 認可ポリシー

| パスパターン | 認可 |
|---|---|
| `/mypage/**` | **要ログイン**（`ROLE_MEMBER`） |
| `/css/**`, `/js/**`, `/images/**`, `/favicon.ico`, `/error` | 全員アクセス可（静的リソース） |
| 上記以外 | 全員アクセス可 |

- ログイン：`POST /login`（`email` / `password`）
- ログアウト：`POST /logout` → `/login` へリダイレクト
- 未認証で `/mypage/**` アクセス時 → `/login?redirectPath=...` へリダイレクト

---

## 画面一覧

### 1. 共通・トップ

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 1 | トップ | `/` | GET | `pages/top` | なし | 新着商品、バナー、カテゴリへの導線 |
| 2 | お知らせ一覧 | `/announcements` | GET | `pages/announcements` | なし | サイト全体のお知らせ一覧 |

---

### 2. 商品カタログ

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 3 | 新着商品一覧 | `/products/new-arrivals` | GET | `pages/product-list-new-arrivals` | なし | 新着商品の一覧表示 |
| 4 | キーワード検索結果 | `/products/search?q={keyword}` | GET | `pages/product-list-search-results` | なし | キーワードによる商品検索・絞り込み |
| 5 | カテゴリ一覧（デスク） | `/categories/desks` | GET | `pages/product-list-category-desk` | なし | デスクカテゴリの商品一覧・フィルタリング |
| 6 | カテゴリ一覧（チェア） | `/categories/chairs` | GET | `pages/product-list-category-chair` | なし | チェアカテゴリの商品一覧・フィルタリング |
| 7 | カテゴリ一覧（収納） | `/categories/storages` | GET | `pages/product-list-category-storage` | なし | 収納家具カテゴリの商品一覧・フィルタリング |
| 8 | 商品詳細 | `/products/{productId}` | GET | `pages/product-detail` | なし | 商品情報、バリアント選択、カート追加、お気に入り登録 |
| ─ | 最近見た商品（API） | `/products/recently-viewed` | GET | ─（JSON） | なし | 最近閲覧した商品IDをJSON返却 |
| ─ | お気に入りトグル（API） | `/products/{productId}/favorite` | POST | ─（リダイレクト） | 実質要ログイン | お気に入り追加・解除（未ログイン時は `/login` へ） |

---

### 3. カート・購入フロー

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 9 | カート | `/cart` | GET | `pages/cart` | なし | カート内商品の確認・数量変更・削除 |
| ─ | カート商品追加 | `/cart/items` | POST | ─（リダイレクト） | なし | 商品をカートへ追加 |
| ─ | カート商品数量更新 | `/cart/items/{productVariantId}/update` | POST | ─（リダイレクト） | なし | カート内商品の数量変更 |
| ─ | カート商品削除 | `/cart/items/{productVariantId}/delete` | POST | ─（リダイレクト） | なし | カートから商品を1件削除 |
| ─ | カート全削除 | `/cart/clear` | POST | ─（リダイレクト） | なし | カートを全クリア |
| 10 | 購入方法選択 | `/checkout/method` | GET | `pages/checkout-method` | なし | ゲスト購入 or 会員購入の選択（カート空の場合は `/cart` へ） |
| 11 | 注文情報入力 | `/checkout/input` | GET / POST | `pages/checkout-input` | なし | 届け先・支払い情報の入力、バリデーション |
| 12 | 注文確認 | `/checkout/confirm` | GET / POST | `pages/checkout-confirm` | なし | 注文内容の最終確認・注文確定 |
| 13 | 注文完了 | `/checkout/complete/{orderNumber}` | GET | `pages/checkout-complete` | なし | 注文完了メッセージと注文番号の表示 |

---

### 4. 認証・会員登録

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 14 | ログイン | `/login` | GET / POST | `pages/login` | なし | メールアドレス・パスワードによるログイン |
| 15 | 会員登録（入力） | `/members/register` | GET | `pages/member-register` | なし | 会員情報入力フォーム |
| 16 | 会員登録（確認） | `/members/register/confirm` | POST | `pages/member-register-confirm` | なし | 入力内容の確認 |
| 17 | 会員登録（完了） | `/members/register/complete` | GET | `pages/member-register-complete` | なし | 登録完了通知（登録後に自動ログイン） |

---

### 5. マイページ（全要ログイン）

| # | 画面名 | URL | HTTPメソッド | テンプレート | 主な機能 |
|---|---|---|---|---|---|
| ─ | マイページ TOP | `/mypage` | GET | ─（リダイレクト） | `/mypage/orders` へリダイレクト |
| 18 | 購入履歴一覧 | `/mypage/orders` | GET | `pages/mypage-orders-list` | 過去の注文一覧表示 |
| 19 | 購入履歴詳細 | `/mypage/orders/{orderNumber}` | GET | `pages/mypage-order-detail` | 注文詳細・再注文 |
| ─ | 再注文 | `/mypage/orders/{orderNumber}/reorder` | POST | ─（リダイレクト） | 過去の注文商品をカートへ追加し `/cart` へ |
| 20 | お気に入り一覧 | `/mypage/favorites` | GET | `pages/mypage-favorites` | お気に入り登録商品の確認・解除 |
| 21 | 会員情報変更 | `/mypage/profile` | GET / POST | `pages/mypage-profile-edit` | 氏名・メールアドレス等の変更 |
| 22 | 追加お届け先一覧 | `/mypage/addresses` | GET | `pages/mypage-addresses` | お届け先住所の一覧 |
| 23 | 追加お届け先追加 | `/mypage/addresses/new` | GET / POST | `pages/mypage-address-form` | 新規お届け先の登録 |
| 24 | 追加お届け先編集 | `/mypage/addresses/{memberAddressId}/edit` | GET / POST | `pages/mypage-address-form` | 既存お届け先の編集・削除 |
| 25 | 退会確認 | `/mypage/withdraw` | GET / POST | `pages/mypage-withdraw` | 退会手続き（実行後セッション破棄→ `/login` へ） |

---

### 6. お問い合わせ

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 26 | お問い合わせ | `/contact` | GET / POST | `pages/contact` | なし | お問い合わせフォームの入力・送信 |

---

### 7. 固定コンテンツ

| # | 画面名 | URL | HTTPメソッド | テンプレート | 要認証 | 主な機能 |
|---|---|---|---|---|---|---|
| 27 | OFFICE ORDER について | `/about` | GET | `pages/about` | なし | サービス紹介ページ |
| 28 | ご利用ガイド | `/guide` | GET | `pages/guide` | なし | 注文・配送・返品などの説明 |
| 29 | 利用規約 | `/legal/terms` | GET | `pages/legal-terms` | なし | 利用規約の表示 |
| 30 | プライバシーポリシー | `/legal/privacy` | GET | `pages/legal-privacy` | なし | プライバシーポリシーの表示 |
| 31 | 特定商取引法に基づく表記 | `/legal/tokusho` | GET | `pages/legal-tokusho` | なし | 特定商取引法表記の表示 |

---

### 8. エラーページ

| # | 画面名 | URL | テンプレート | 説明 |
|---|---|---|---|---|
| 32 | 汎用エラー | `/error` | `error/error.html` | 404 等の汎用エラー表示 |
| 33 | 500エラー | ─（Spring Boot 自動） | `error/500.html` | サーバー内部エラー表示 |

---

### 9. 内部向け API（`local` プロファイル限定）

| # | 機能名 | URL | HTTPメソッド | 説明 |
|---|---|---|---|---|
| ─ | バッチジョブ一覧 | `/internal/batch/jobs` | GET | 登録済みジョブの一覧をJSON返却 |
| ─ | バッチ実行履歴 | `/internal/batch/jobs/{jobName}/executions` | GET | 実行履歴をJSON返却 |
| ─ | バッチジョブ起動 | `/internal/batch/jobs/{jobName}/executions` | POST | ジョブを起動しJSON返却 |
| ─ | バッチジョブ停止 | `/internal/batch/jobs/{jobName}/executions/{executionId}/stop` | POST | 実行中ジョブを停止 |
| ─ | エラーページ確認 | `/internal/test/errors/500` | GET | 強制500エラー発生（動作確認用） |

---

## 共通コンポーネント（テンプレートフラグメント）

| フラグメント | 用途 |
|---|---|
| `fragments/layout/head.html` | `<head>` タグ共通設定 |
| `fragments/layout/header.html` | グローバルナビ（カテゴリ・検索・ユーザー・カート） |
| `fragments/layout/footer.html` | フッター（リンク集・コピーライト） |
| `fragments/layout/scripts.html` | JavaScript 読み込み |
| `fragments/layout/notice-bar.html` | お知らせバー |
| `fragments/common/breadcrumb.html` | パンくずリスト |
| `fragments/common/product-card.html` | 商品カードコンポーネント |
