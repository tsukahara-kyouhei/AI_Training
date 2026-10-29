# 画面一覧

実装済みの Controller と Thymeleaf テンプレートをもとに、利用者が参照する画面を機能カテゴリ別に整理しています。`GET` は画面表示、`POST` は画面表示を伴う確認処理を示します。

## 画面構成の概要

| 項目 | 件数・内容 |
|---|---|
| 全画面数 | 32画面 |
| 認証不要の画面数 | 24画面 |
| 会員必須の画面数 | 8画面 |
| 主要な機能カテゴリ | 共通・トップ、商品・検索、カート・購入、会員登録・ログイン、マイページ、お問い合わせ・その他、エラー |

## 共通・トップ

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-001 | トップ | 新着・ランキングを表示 | GET | `/` | 不要 |
| SCR-002 | お知らせ一覧 | お知らせを一覧表示 | GET | `/announcements` | 不要 |

## 商品・検索

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-003 | 商品検索結果 | 商品を条件検索 | GET | `/products/search` | 不要 |
| SCR-004 | 新着商品一覧 | 新着商品を一覧表示 | GET | `/products/new-arrivals` | 不要 |
| SCR-005 | デスクカテゴリ一覧 | デスクを条件絞り込み | GET | `/categories/desks` | 不要 |
| SCR-006 | チェアカテゴリ一覧 | チェアを条件絞り込み | GET | `/categories/chairs` | 不要 |
| SCR-007 | 収納家具カテゴリ一覧 | 収納家具を条件絞り込み | GET | `/categories/storages` | 不要 |
| SCR-008 | 商品詳細 | 商品情報・在庫を表示 | GET | `/products/{productId}` | 不要 |

## カート・購入

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-009 | カート | カート内容・料金を確認 | GET | `/cart` | 不要 |
| SCR-010 | 購入方法選択 | 会員・ゲスト購入を選択 | GET | `/checkout/method` | 不要 |
| SCR-011 | 注文情報入力 | 注文・配送・支払情報を入力 | GET | `/checkout/input` | 不要 |
| SCR-012 | 注文確認 | 入力内容を確認 | GET | `/checkout/confirm` | 不要 |
| SCR-013 | 注文完了 | 注文内容・支払情報を表示 | GET | `/checkout/complete/{orderNumber}` | 不要 |

## 会員登録・ログイン

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-014 | ログイン | 会員ログイン | GET | `/login` | 不要 |
| SCR-015 | 会員登録入力 | 会員情報を入力 | GET | `/members/register` | 不要 |
| SCR-016 | 会員登録確認 | 登録内容を確認 | POST | `/members/register/confirm` | 不要 |
| SCR-017 | 会員登録完了 | 登録完了・会員コードを表示 | GET | `/members/register/complete` | 不要 |

## マイページ

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-018 | 購入履歴一覧 | 注文履歴を表示 | GET | `/mypage/orders` | 会員必須 |
| SCR-019 | 購入履歴詳細 | 注文・明細・履歴を表示 | GET | `/mypage/orders/{orderNumber}` | 会員必須 |
| SCR-020 | お気に入り一覧 | お気に入り商品を表示 | GET | `/mypage/favorites` | 会員必須 |
| SCR-021 | 会員情報変更 | 会員情報を表示・更新 | GET | `/mypage/profile` | 会員必須 |
| SCR-022 | 追加お届け先一覧 | 追加お届け先を管理 | GET | `/mypage/addresses` | 会員必須 |
| SCR-023 | 追加お届け先登録 | 追加お届け先を登録 | GET | `/mypage/addresses/new` | 会員必須 |
| SCR-024 | 追加お届け先編集 | 追加お届け先を編集 | GET | `/mypage/addresses/{memberAddressId}/edit` | 会員必須 |
| SCR-025 | 退会確認 | 退会操作を確認 | GET | `/mypage/withdraw` | 会員必須 |

## お問い合わせ・その他

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-026 | お問い合わせ | お問い合わせを入力・送信 | GET | `/contact` | 不要（会員は初期値反映） |
| SCR-027 | OFFICE ORDER について | サービス概要を表示 | GET | `/about` | 不要 |
| SCR-028 | ご利用ガイド | 利用方法を表示 | GET | `/guide` | 不要 |
| SCR-029 | 利用規約 | 利用規約を表示 | GET | `/legal/terms` | 不要 |
| SCR-030 | プライバシーポリシー | ポリシーを表示 | GET | `/legal/privacy` | 不要 |
| SCR-031 | 特定商取引法に基づく表記 | 法定表記を表示 | GET | `/legal/tokusho` | 不要 |

## エラー

| ID | 画面名 | 主な機能 | メソッド | パス | 認証 |
|---|---|---|---|---|---|
| SCR-032 | エラー画面 | 404/500などのエラーを表示 | - | エラー発生時（`/error`） | 不要 |

## テンプレート対応表

| ID | テンプレート |
|---|---|
| SCR-001 | `pages/top.html` |
| SCR-002 | `pages/announcements.html` |
| SCR-003 | `pages/product-list-search-results.html` |
| SCR-004 | `pages/product-list-new-arrivals.html` |
| SCR-005 | `pages/product-list-category-desk.html` |
| SCR-006 | `pages/product-list-category-chair.html` |
| SCR-007 | `pages/product-list-category-storage.html` |
| SCR-008 | `pages/product-detail.html` |
| SCR-009 | `pages/cart.html` |
| SCR-010 | `pages/checkout-method.html` |
| SCR-011 | `pages/checkout-input.html` |
| SCR-012 | `pages/checkout-confirm.html` |
| SCR-013 | `pages/checkout-complete.html` |
| SCR-014 | `pages/login.html` |
| SCR-015 | `pages/member-register.html` |
| SCR-016 | `pages/member-register-confirm.html` |
| SCR-017 | `pages/member-register-complete.html` |
| SCR-018 | `pages/mypage-orders-list.html` |
| SCR-019 | `pages/mypage-order-detail.html` |
| SCR-020 | `pages/mypage-favorites.html` |
| SCR-021 | `pages/mypage-profile-edit.html` |
| SCR-022 | `pages/mypage-addresses.html` |
| SCR-023, SCR-024 | `pages/mypage-address-form.html` |
| SCR-025 | `pages/mypage-withdraw.html` |
| SCR-026 | `pages/contact.html` |
| SCR-027 | `pages/about.html` |
| SCR-028 | `pages/guide.html` |
| SCR-029 | `pages/legal-terms.html` |
| SCR-030 | `pages/legal-privacy.html` |
| SCR-031 | `pages/legal-tokusho.html` |
| SCR-032 | `error/error.html`, `error/500.html` |

## 画面を伴わない操作

以下は画面表示用の独立画面ではなく、操作後に一覧・詳細などへリダイレクトするエンドポイントです。

| 操作 | URL | 主な遷移先 |
|---|---|---|
| カート追加・数量変更・削除・全削除 | `POST /cart/items...`, `POST /cart/clear` | カート |
| 商品のお気に入り追加・解除 | `POST /products/{productId}/favorite` | 商品詳細または指定された戻り先。未ログイン時はログイン |
| 注文情報入力送信 | `POST /checkout/input` | 注文確認、入力エラー時は注文情報入力 |
| 注文確定 | `POST /checkout/confirm` | 注文完了、エラー時は注文情報入力 |
| ログイン送信 | `POST /login` | マイページまたは指定された戻り先 |
| 会員登録確定 | `POST /members/register` | 会員登録完了、重複時は会員登録入力 |
| 購入履歴から再購入 | `POST /mypage/orders/{orderNumber}/reorder` | カート |
| 会員情報更新 | `POST /mypage/profile` | 会員情報変更 |
| 追加お届け先の登録・更新・削除 | `POST /mypage/addresses...` | 追加お届け先一覧 |
| 退会実行 | `POST /mypage/withdraw` | ログイン |
| お問い合わせ送信 | `POST /contact` | お問い合わせ（成功・失敗表示） |

## ローカル開発専用API

`local` プロファイルでのみ有効です。画面ではなく JSON API のため、画面一覧の対象外として扱います。

- `GET /internal/batch/jobs`
- `GET /internal/batch/jobs/{jobName}/executions`
- `POST /internal/batch/jobs/{jobName}/executions`
- `POST /internal/batch/executions/{executionId}/stop`
- `GET /internal/test/errors/500`（500画面の手動確認用）