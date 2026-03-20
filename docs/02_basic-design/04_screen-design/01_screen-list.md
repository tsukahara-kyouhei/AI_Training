# 画面一覧

## 凡例

- **認証**: `public` = 認証不要 / `member` = ログイン必須
- **画面ID**: 各画面定義書のファイル名プレフィックスに対応

---

## 1. 公開画面

| 画面ID | 画面名 | URL | 認証 | テンプレート |
|---|---|---|---|---|
| SCR-TOP | トップページ | `/` | public | `top.html` |
| SCR-ANN | お知らせ一覧 | `/announcements` | public | `announcements.html` |
| SCR-CAT-NEW | 新着商品一覧 | `/products/new-arrivals` | public | `product-list-new-arrivals.html` |
| SCR-CAT-SEARCH | 検索結果一覧 | `/products/search` | public | `product-list-search-results.html` |
| SCR-CAT-DESK | デスク一覧 | `/categories/desks` | public | `product-list-category-desk.html` |
| SCR-CAT-CHAIR | チェア一覧 | `/categories/chairs` | public | `product-list-category-chair.html` |
| SCR-CAT-STORAGE | 収納家具一覧 | `/categories/storages` | public | `product-list-category-storage.html` |
| SCR-DTL | 商品詳細 | `/products/{productId}` | public | `product-detail.html` |
| SCR-CRT | カート | `/cart` | public | `cart.html` |
| SCR-CO01 | チェックアウト - 支払方法選択 | `/checkout/method` | public | `checkout-method.html` |
| SCR-CO02 | チェックアウト - 注文情報入力 | `/checkout/input` | public | `checkout-input.html` |
| SCR-CO03 | チェックアウト - 確認 | `/checkout/confirm` | public | `checkout-confirm.html` |
| SCR-CO04 | チェックアウト - 完了 | `/checkout/complete/{orderNumber}` | public | `checkout-complete.html` |
| SCR-LGN | ログイン | `/login` | public | `login.html` |
| SCR-REG01 | 会員登録 | `/members/register` | public | `member-register.html` |
| SCR-REG02 | 会員登録確認 | `/members/register/confirm` (POST) | public | `member-register-confirm.html` |
| SCR-REG03 | 会員登録完了 | `/members/register/complete` | public | `member-register-complete.html` |
| SCR-CNT | お問い合わせ | `/contact` | public | `contact.html` |
| SCR-ABT | 会社概要 | `/about` | public | `about.html` |
| SCR-GDE | ご利用ガイド | `/guide` | public | `guide.html` |
| SCR-TRM | 利用規約 | `/legal/terms` | public | `legal-terms.html` |
| SCR-PRV | プライバシーポリシー | `/legal/privacy` | public | `legal-privacy.html` |
| SCR-TOK | 特定商取引法に基づく表記 | `/legal/tokusho` | public | `legal-tokusho.html` |
| SCR-ERR | エラーページ | `/error` | public | `error/error.html` / `error/500.html` |

---

## 2. 会員専用画面（マイページ）

| 画面ID | 画面名 | URL | 認証 | テンプレート |
|---|---|---|---|---|
| SCR-MY | マイページ（注文履歴一覧） | `/mypage/orders` | member | `mypage-orders-list.html` |
| SCR-MY-ORD | 注文詳細 | `/mypage/orders/{orderNumber}` | member | `mypage-order-detail.html` |
| SCR-MY-FAV | お気に入り一覧 | `/mypage/favorites` | member | `mypage-favorites.html` |
| SCR-MY-PRF | プロフィール編集 | `/mypage/profile` | member | `mypage-profile-edit.html` |
| SCR-MY-ADR | 追加お届け先一覧 | `/mypage/addresses` | member | `mypage-addresses.html` |
| SCR-MY-ADRF | 追加お届け先フォーム | `/mypage/addresses/new` / `…/{id}/edit` | member | `mypage-address-form.html` |
| SCR-MY-WDR | 退会確認 | `/mypage/withdraw` | member | `mypage-withdraw.html` |

---

## 3. 画面数サマリー

| 種別 | 件数 |
|---|---|
| 公開画面 | 24 |
| 会員専用画面 | 7 |
| **合計** | **31** |
