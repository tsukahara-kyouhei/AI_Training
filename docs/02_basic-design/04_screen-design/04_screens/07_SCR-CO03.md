# チェックアウト - 確認

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CO03 |
| テンプレート | `pages/checkout-confirm.html` |
| URL | `GET /checkout/confirm`、`POST /checkout/confirm` |
| Controller | `CartController#checkoutConfirm()`、`CartController#placeOrder()` |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| ご注文者情報確認 | 入力内容の確認表示（テーブル） |
| お届け先確認 | 住所・階数・EV有無の確認表示 |
| ご注文商品確認 | カート内容の一覧表示 |
| 金額サマリー | 小計・組立費・送料・税・合計 |
| 注文確定ボタン | ワンタイムトークン付き |
| 戻るボタン | 入力画面へのリンク |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `checkoutForm` | `CheckoutInputForm` | 入力済みフォーム（確認表示用） |
| `cart` | `CartView` | カート内容 |
| `checkoutConfirmToken` | `String` | 二重送信防止トークン |
| `checkoutError` | `String` | エラーメッセージ |

---

## 4. フォーム

| 項目 | 値 |
|---|---|
| action | `POST /checkout/confirm` |
| `token` | hidden（ワンタイムトークン） |

---

## 5. 処理フロー

1. **GET**: セッションからフォーム取得・カート情報とともに表示
2. **POST**: トークン検証 → 在庫確認 → 注文確定 → カートクリア → 完了画面へリダイレクト
3. トークン不正・在庫不足時: 同一画面に戻りエラー表示
