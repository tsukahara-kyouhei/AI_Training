# 画面一覧

## 概要

本ドキュメントは OFFICE ORDER の全画面を一覧化したものです。

---

## 1. 一般（未ログイン・ログイン共通）

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| TOP001 | トップ | `/` | `pages/top.html` | カテゴリ選択、新着商品表示、お知らせ表示 | 不要 |
| TOP002 | お知らせ一覧 | `/announcements` | `pages/announcements.html` | サイト全体のお知らせ一覧表示 | 不要 |

---

## 2. 商品カタログ

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| CAT001 | カテゴリ一覧（デスク） | `/categories/desks` | `pages/product-list-category-desk.html` | デスクカテゴリの商品一覧、絞り込み・ソート | 不要 |
| CAT002 | カテゴリ一覧（チェア） | `/categories/chairs` | `pages/product-list-category-chair.html` | チェアカテゴリの商品一覧、絞り込み・ソート | 不要 |
| CAT003 | カテゴリ一覧（収納家具） | `/categories/storages` | `pages/product-list-category-storage.html` | 収納家具カテゴリの商品一覧、絞り込み・ソート | 不要 |
| SRCH001 | 新着商品一覧 | `/products/new-arrivals` | `pages/product-list-new-arrivals.html` | 新着商品一覧、価格帯・カラー等での絞り込み・ソート | 不要 |
| SRCH002 | キーワード検索結果 | `/products/search?q={keyword}` | `pages/product-list-search-results.html` | キーワード検索結果一覧、絞り込み・ソート | 不要 |
| PRD001 | 商品詳細 | `/products/{productId}` | `pages/product-detail.html` | 商品スペック・画像表示、カラー/サイズ選択、カート追加、お気に入り登録 | 不要（お気に入りはログイン要） |

---

## 3. カート・購入フロー

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| CART001 | カート | `/cart` | `pages/cart.html` | カート内商品の確認・数量変更・削除、購入手続きへの遷移 | 不要 |
| CHK001 | 購入方法選択 | `/checkout/method` | `pages/checkout-method.html` | ゲスト購入またはログイン購入の選択 | 不要 |
| CHK002 | 注文情報入力 | `/checkout/input` | `pages/checkout-input.html` | 配送先・支払い方法等の注文情報入力 | ログイン or ゲスト |
| CHK003 | 注文確認 | `/checkout/confirm` | `pages/checkout-confirm.html` | 注文内容の最終確認、注文確定 | ログイン or ゲスト |
| CHK004 | 注文完了 | `/checkout/complete/{orderNumber}` | `pages/checkout-complete.html` | 注文受付完了メッセージ、注文番号の表示 | ログイン or ゲスト |

---

## 4. 会員登録

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| REG001 | 会員登録（入力） | `/members/register` | `pages/member-register.html` | 氏名・メールアドレス・パスワード等の入力 | 不要 |
| REG002 | 会員登録（確認） | `/members/register/confirm` (POST→GET) | `pages/member-register-confirm.html` | 入力内容の確認 | 不要 |
| REG003 | 会員登録完了 | `/members/register/complete` | `pages/member-register-complete.html` | 登録完了通知、ログイン促進 | 不要 |

---

## 5. 認証

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| AUTH001 | ログイン | `/login` | `pages/login.html` | メールアドレス・パスワードによるログイン | 不要 |

---

## 6. マイページ

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| MYP001 | マイページトップ | `/mypage` | （`/mypage/orders` へリダイレクト） | マイページ入口（購入履歴へ転送） | 要 |
| MYP002 | 購入履歴一覧 | `/mypage/orders` | `pages/mypage-orders-list.html` | 過去注文の一覧表示、ページネーション | 要 |
| MYP003 | 注文詳細 | `/mypage/orders/{orderNumber}` | `pages/mypage-order-detail.html` | 注文明細・配送状況の確認、再注文 | 要 |
| MYP004 | お気に入り一覧 | `/mypage/favorites` | `pages/mypage-favorites.html` | お気に入り商品の一覧表示・削除 | 要 |
| MYP005 | 会員情報変更 | `/mypage/profile` | `pages/mypage-profile-edit.html` | 氏名・パスワード等の会員情報編集 | 要 |
| MYP006 | お届け先管理 | `/mypage/addresses` | `pages/mypage-addresses.html` | 追加お届け先の一覧表示 | 要 |
| MYP007 | お届け先追加・編集 | `/mypage/addresses/new`<br>`/mypage/addresses/{id}/edit` | `pages/mypage-address-form.html` | お届け先の新規追加・編集 | 要 |
| MYP008 | 退会手続き | `/mypage/withdraw` | `pages/mypage-withdraw.html` | 退会の確認・実行 | 要 |

---

## 7. お問い合わせ・コンテンツ

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| CNT001 | お問い合わせ | `/contact` | `pages/contact.html` | お問い合わせフォームの入力・送信 | 不要 |
| CNT002 | OFFICE ORDERについて | `/about` | `pages/about.html` | サービス概要の説明 | 不要 |
| CNT003 | ご利用ガイド | `/guide` | `pages/guide.html` | 注文・支払い・配送等のガイド説明 | 不要 |
| CNT004 | 利用規約 | `/legal/terms` | `pages/legal-terms.html` | 利用規約の表示 | 不要 |
| CNT005 | プライバシーポリシー | `/legal/privacy` | `pages/legal-privacy.html` | プライバシーポリシーの表示 | 不要 |
| CNT006 | 特定商取引法に基づく表記 | `/legal/tokusho` | `pages/legal-tokusho.html` | 特定商取引法の表記 | 不要 |

---

## 8. エラー画面

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| ERR001 | 404 Not Found | （自動） | `error/404.html` | ページが見つからない旨の表示 | 不要 |
| ERR002 | 500 Internal Server Error | （自動） | `error/500.html` | サーバーエラーの表示 | 不要 |

---

## 9. 内部管理（Internal）

| 画面ID | 画面名 | URL | テンプレート | 主な機能 | 認証要否 |
|--------|--------|-----|------------|---------|---------|
| INT001 | バッチジョブ一覧 | `/jobs` | — | Spring Batchジョブの一覧・実行状況確認 | 内部限定 |
| INT002 | バッチ実行履歴 | `/jobs/{jobName}/executions` | — | 指定ジョブの実行履歴確認・新規実行 | 内部限定 |
