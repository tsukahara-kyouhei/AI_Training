## 1. 機能概要

- カートでクーポンコードを適用し、利用可能な場合に注文金額へ割引を反映できる。
- 適用中のクーポンを解除し、クーポンを使わない状態に戻せる。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP POST リクエスト `/cart/coupon/apply` | カートにクーポンを適用するリクエスト。 |
| 入力 | フォームパラメータ `couponCode` | 適用対象のクーポンコード。 |
| 入力 | HTTP セッション | 適用中のクーポンコードを保存または解除するために使用する。 |
| 入力 | HTTP POST リクエスト `/cart/coupon/remove` | カートに適用中のクーポンを解除するリクエスト。 |
| 出力 | HTTP セッション属性 `appliedCouponCode` | 適用時は受け取ったクーポンコードを設定し、解除時は属性を削除する。 |
| 出力 | 遷移先 `redirect:/cart` | クーポン適用または解除後にカート画面へリダイレクトする。 |

## 3. 処理フロー

### `applyCoupon`（POST `/cart/coupon/apply`）

1. リクエストからクーポンコード `couponCode` を受け取る。
2. HTTP セッション属性 `appliedCouponCode` に受け取ったクーポンコードを設定する。
3. `redirect:/cart` へリダイレクトする。

### `removeCoupon`（POST `/cart/coupon/remove`）

1. HTTP セッションから `appliedCouponCode` 属性を削除する。
2. `redirect:/cart` へリダイレクトする。
