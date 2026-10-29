# FEAT-002: API設計（最終版）

## 1. API構成

| 用途 | Method | URL | 方式 |
|---|---|---|---|
| クーポン適用 | POST | `/api/cart/coupon` | REST |
| クーポン解除 | DELETE | `/api/cart/coupon` | REST |
| 注文確認 | GET | `/checkout/confirm` | 既存MVC |
| 注文確定 | POST | `/checkout/confirm` | 既存MVC |

`/api/checkout/confirm`、`/api/checkout/execute` は追加しない。

## 2. POST `/api/cart/coupon`

Request:
```json
{"coupon_code":"SUMMER2026"}
```

成功:
```json
{"success":true,"cart":{"subtotal":5000,"coupon_discount_amount":500,"total":4500,"applied_coupon_code":"SUMMER2026","applied_coupon_name":"夏得クーポン"}}
```

失敗:
```json
{"success":false,"error_code":"COUPON_EXPIRED","message":"クーポンの有効期限外です。"}
```

成功時はクーポンコードと適用時の会員ID（ゲストはNULL）をセッションへ保存する。割引額はサーバー計算値を返す。
成功レスポンスの金額項目は、割引適用後のカート集計（割引前商品小計、割引額、割引後商品小計、組立費、税、送料、注文合計）を返す。RESTエラーのHTTPステータスは人間判断事項。
上記JSON例の `subtotal`／`total` は既存モデルとの対応や税・送料包含を確定したフィールド定義ではなく、確定レスポンスのプロパティ名・型は人間判断事項。

## 3. DELETE `/api/cart/coupon`

- Request bodyなし。
- セッションのクーポンコードを削除する。
- カート金額を再計算する。
- 成功時は `coupon_discount_amount=0`、クーポンコード・名前を未設定として返す。

## 4. GET `/checkout/confirm`

- セッションからクーポンコードを取得する。
- 現在のカートを再構成し、クーポンを再検証・再計算する。
- 成功時は確認画面にクーポン名・割引額を表示する。
- 失敗時はクーポンをセッションから削除し、金額を再計算してカートへ戻す。
- 商品カートCookieは読み取り・再構成し、クーポン状態はHttpSessionから取得する。
- セッションに保存した適用時会員IDと現在の会員IDが異なる場合はコードを破棄し、再適用を要求する。
- ログイン成功後も会員ID変更を検知した時点で解除する。ログアウト時は現行設定でセッション全体が無効化される。
- セッションが失効してクーポンコードがない場合は未適用として既存確認フローを続ける。

## 5. POST `/checkout/confirm`

- 既存のワンタイムトークンを使用する。
- DBトランザクション内で最終再検証する。
- `coupons` → `user_coupon_usages` の順にロックする。
- 成功時は注文保存、利用実績更新、3項目のスナップショット保存を同一トランザクションで実行する。
- 再検証失敗時は注文を保存せず、利用実績も確定せず、クーポンを解除して確認画面へ戻す。
- セッションのクーポンコードを `OrderService.placeOrder` に渡す。既存Controllerはクーポンコードを渡していない。
- 成功後にセッション上のチェックアウト情報・クーポン状態を消去する。
- MVCエラー時のHTTPステータス、確認画面へ戻す実際のController処理は人間判断事項。

## 6. ゲスト

- `max_usage_per_user` 設定クーポンは `COUPON_USER_CONDITION_ERROR`。
- 未設定クーポンは利用可能。
- ゲストの会員別利用実績は作成しない。

## 7. エラーコード

- `COUPON_NOT_FOUND`
- `COUPON_ALREADY_APPLIED`
- `COUPON_EXPIRED`
- `COUPON_USER_CONDITION_ERROR`
- `COUPON_TARGET_ERROR`
- `COUPON_MIN_AMOUNT_ERROR`
- `COUPON_USAGE_LIMIT_REACHED`

## 8. セキュリティ・金額

- 既存CSRF保護を維持する。
- クライアントから送信された割引額を信頼しない。
- 金額は整数円。
- 送料判定はクーポン適用前の現行条件を使う。税額はクーポン割引後の商品小計＋組立費を基準にFLOORする。
- 注文合計は割引後商品小計＋組立費＋割引後基準の税額＋割引前判定で固定した送料。
- 明細割引はクーポン対象商品小計に比例配賦し、整数円の配賦端数は剰余の大きい明細から配る（同率はカート内明細順）。明細税額は配賦後の課税対象金額を既存税率・切り捨てで計算する。明細単位の切り捨てによるヘッダーとの差は追加調整しない。

## 9. エラー挙動

| error_code | 条件 | API応答 | セッション | カート／画面 |
|---|---|---|---|---|
| `COUPON_NOT_FOUND` | コード不存在または無効 | JSONエラー。HTTPステータスは人間判断事項 | 適用済みコードを変更しない | 商品カートを保持。エラーを表示 |
| `COUPON_ALREADY_APPLIED` | 別コード適用済み | JSONエラー。HTTPステータスは人間判断事項 | 現行コードを保持 | 商品カートを保持。上書きしない |
| `COUPON_EXPIRED` | 有効期間外 | JSONエラー。HTTPステータスは人間判断事項 | 新規適用時は未変更。確認時失敗なら解除 | 商品カートを保持。確認時はカートへ戻す |
| `COUPON_USER_CONDITION_ERROR` | 会員条件不一致または個人上限ありクーポンをゲストが適用 | JSONエラー。HTTPステータスは人間判断事項 | 新規適用時は未変更。確認時失敗なら解除 | 商品カートを保持 |
| `COUPON_TARGET_ERROR` | 対象商品・カテゴリ条件不成立 | JSONエラー。HTTPステータスは人間判断事項 | 新規適用時は未変更。確認時失敗なら解除 | 商品カートを保持 |
| `COUPON_MIN_AMOUNT_ERROR` | 対象小計が最低金額未満 | JSONエラー。HTTPステータスは人間判断事項 | 新規適用時は未変更。確認時失敗なら解除 | 商品カートを保持 |
| `COUPON_USAGE_LIMIT_REACHED` | 全体または会員別上限到達 | JSONエラー。HTTPステータスは人間判断事項 | 新規適用時は未変更。確認時失敗なら解除 | 商品カートを保持 |

MVC確認GET失敗時はセッションのクーポンを解除し `/cart` へ戻す。注文確定POSTでの失敗時は注文を成立させず、クーポンを解除する。戻り先とHTTP/MVC応答は人間判断事項であり、既存実装は現在 `/checkout/input` に戻す。

## 10. 人間判断事項

- RESTエラーごとのHTTPステータス。
- 注文確定時の再検証エラーで、現行入力画面と設計記載の確認画面のどちらへ戻すか、および確認画面へ戻す際の入力フォーム保持方法。
- `target_conditions` 管理登録経路を本機能へ含めるか。現行ソースに登録API／管理Controllerはない。
- 成功レスポンスの確定JSONプロパティ名・型（現行例の `subtotal` / `total` と、表示項目の全金額列との対応）。
