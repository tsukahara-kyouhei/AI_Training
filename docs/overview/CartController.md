## 1. 機能概要

- 商品をカートに追加・更新・削除したり、カートを空にしたりして、購入前の商品を管理できる。
- クーポンを適用・解除し、割引を反映したカート内容を確認できる。
- 購入方法を選び、注文者・配送先などの情報を入力・確認して注文を確定できる。
- 注文完了後に注文内容を確認でき、会員には登録済みの追加お届け先やログイン情報を利用した購入を支援する。
- 入力内容や購入条件に問題がある場合は、修正や再試行に必要な案内を提供する。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP リクエスト／レスポンス | カートの取得・更新・削除に使用する。カート画面や購入手続き画面の表示にも使用する。 |
| 入力 | URL パス変数 `productVariantId` | カートに追加・更新・削除する商品バリエーションの識別子。 |
| 入力 | URL パス変数 `orderNumber` | 注文完了画面に表示する注文番号。 |
| 入力 | リクエストパラメータ `quantity` | カートに追加・更新する数量。省略時は `1`。 |
| 入力 | リクエストパラメータ `assemblyRequested` | 商品の組立を希望するかどうか。省略可能。 |
| 入力 | リクエストパラメータ `redirect` | 商品追加後の戻り先候補。安全なパスに補正し、利用できない場合は `/cart` に戻す。 |
| 入力 | リクエストパラメータ `couponCode` | カートに適用するクーポンコード。 |
| 入力 | チェックアウトフォーム `checkoutForm` | 注文者・配送先・支払いに関するフォーム。`personalOrCorporate`、`lastName`、`firstName`、`lastNameKana`、`firstNameKana`、`companyName`、`departmentName`、`email`、`daytimePhone`、`fax`、`postalCodePart1`、`postalCodePart2`、`prefecture`、`city`、`addressLine`、`deliveryFloor`、`hasElevator`、`paymentMethod`、`selectedAdditionalAddressId` を含む。 |
| 入力 | リクエストパラメータ `token` | 注文確認画面から送信されるワンタイム確認トークン。 |
| 入力 | HTTP セッション `checkout_input_form` | 入力済みの注文情報フォーム。確認画面表示・注文確定時に参照する。 |
| 入力 | HTTP セッション `checkout_confirm_token` | 注文確認画面に対応するワンタイムトークン。 |
| 入力 | HTTP セッション `appliedCouponCode` | カートに適用中のクーポンコード。クーポン適用・解除時に設定／削除される。 |
| 入力 | ログイン会員情報 | 会員購入かどうかの判定、初期フォームの生成、登録済み追加お届け先の取得に使用する。 |
| 入力 | 記憶済みメールアドレス Cookie | 購入方法選択画面のログインフォームに表示するメールアドレス。 |
| 出力 | 遷移先 `pages/cart` | カート画面。カート操作のリダイレクト先としても使用する。 |
| 出力 | 画面モデル `cart` | カート内容、商品数、数量、金額集計などを含む表示データ。 |
| 出力 | 画面モデル `isCartEmpty` | カートが空かどうかを示す真偽値。 |
| 出力 | フラッシュ属性 `cartMessage` | 商品追加成功などをカート画面で知らせるメッセージ。 |
| 出力 | フラッシュ属性 `cartError` | カート操作またはクーポンコードの入力に関するエラーメッセージ。 |
| 出力 | 遷移先 `pages/checkout-method` | 購入方法選択画面。カートが空の場合は `/cart` へリダイレクトする。 |
| 出力 | 画面モデル `rememberedLoginEmail`、`rememberLoginEmail` | 記憶済みメールアドレスと、その有無を示す値。 |
| 出力 | 画面モデル `checkoutRedirectPath` | 購入方法選択後の遷移先として `/checkout/input` を示す。 |
| 出力 | 遷移先 `pages/checkout-input` | 注文情報入力画面。カートが空の場合は `/cart` へリダイレクトする。 |
| 出力 | 画面モデル `checkoutForm` | 注文情報入力フォーム。セッションに保存された値、ログイン会員向け初期値、または入力値を表示する。 |
| 出力 | 画面モデル `cart` | 注文情報入力時点のカート内容。 |
| 出力 | 画面モデル `isMemberCheckout` | ログイン会員による購入かどうかを示す真偽値。 |
| 出力 | 画面モデル `checkoutAdditionalAddresses`、`hasCheckoutAdditionalAddresses` | 会員が選択できる追加お届け先一覧と、その有無。ゲストの場合は空一覧・`false`。 |
| 出力 | 入力検証結果 | 注文情報に不備がある場合、フィールドごとのエラーをフォーム画面に表示するための情報。 |
| 出力 | フラッシュ属性 `checkoutForm` | 確認画面への遷移時に入力内容を引き継ぐ。 |
| 出力 | フラッシュ属性 `checkoutError` | 注文入力の再試行や確認トークン不一致、注文確定失敗を知らせるメッセージ。 |
| 出力 | 遷移先 `pages/checkout-confirm` | 注文内容の確認画面。フォーム情報がない場合は入力画面へ、カートが空の場合はカート画面へリダイレクトする。 |
| 出力 | 画面モデル `checkoutForm`、`cart` | 確認する注文者・配送先情報とカート内容。 |
| 出力 | 画面モデル `couponDiscountAmount`、`appliedCouponCode` | 注文確認時に表示するクーポン割引額と適用中コード。 |
| 出力 | 画面モデル `checkoutConfirmToken` | 注文確定時に照合するワンタイムトークン。 |
| 出力 | 遷移先 `/checkout/complete/{orderNumber}` | 注文確定後の完了画面へのリダイレクト先。 |
| 出力 | 遷移先 `pages/checkout-complete` | 注文完了画面。画面モデル `order` に注文完了情報を渡す。 |
| 出力 | HTTP 404 | 指定された注文番号に対応する注文完了情報が見つからない場合に返す。 |

## 3. 処理フロー

### `cart`（`GET /cart`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カート情報を `cart`、空カートかどうかを `isCartEmpty` として画面モデルに設定する。
3. `pages/cart` を返す。

### `addItem`（`POST /cart/items`）

1. `CartService.sanitizeRedirectPath` を呼び出し、指定された戻り先を検証する。
   - 戻り先が利用できない場合：戻り先を `/cart` にする。
2. `CartService.addItem` を呼び出し、商品バリエーション、数量、組立指定をカートに追加する。
   - 追加に成功した場合：成功メッセージをフラッシュ属性 `cartMessage` に設定する。
   - `IllegalArgumentException` が発生した場合：警告ログを記録し、例外メッセージをフラッシュ属性 `cartError` に設定する。
3. 決定した戻り先へリダイレクトする。

### `updateItem`（`POST /cart/items/{productVariantId}/update`）

1. `CartService.updateItem` を呼び出し、対象商品の数量と組立指定を更新する。
   - 更新に成功した場合：追加のメッセージは設定しない。
   - `IllegalArgumentException` が発生した場合：警告ログを記録し、例外メッセージをフラッシュ属性 `cartError` に設定する。
2. `/cart` へリダイレクトする。

### `deleteItem`（`POST /cart/items/{productVariantId}/delete`）

1. `CartService.removeItem` を呼び出し、指定された商品をカートから削除する。
2. `/cart` へリダイレクトする。

### `applyCoupon`（`POST /cart/apply-coupon`、`POST /cart/coupon/apply`）

1. クーポンコードの前後の空白を取り除く。
2. クーポンコードが空の場合：入力を促すメッセージをフラッシュ属性 `cartError` に設定し、`/cart` へリダイレクトする。
3. クーポンコードが空でない場合：HTTP セッション属性 `appliedCouponCode` にコードを設定し、`/cart` へリダイレクトする。

### `removeCoupon`（`POST /cart/remove-coupon`、`POST /cart/coupon/remove`）

1. HTTP セッションから `appliedCouponCode` を削除する。
2. `/cart` へリダイレクトする。

### `clearCart`（`POST /cart/clear`）

1. `CartService.clear` を呼び出し、カートを空にする。
2. `/cart` へリダイレクトする。

### `checkoutMethod`（`GET /checkout/method`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カートが空の場合：`/cart` へリダイレクトする。
3. カートに商品がある場合：
   1. `bindRememberedLoginEmail` を呼び出し、記憶済みメールアドレスを画面モデルに設定する。
   2. `checkoutRedirectPath` に `/checkout/input` を設定する。
   3. `pages/checkout-method` を返す。

### `checkoutInput`（`GET /checkout/input`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カートが空の場合：`/cart` へリダイレクトする。
3. 画面モデルに `checkoutForm` がない場合：
   - セッションに `checkout_input_form` がある場合：保存済みフォームを `checkoutForm` として設定する。
   - 保存済みフォームがない場合：`MemberSessionService.currentMember` を呼び出す。会員情報をもとに `OrderService.createInitialForm` を呼び出して初期フォームを作成し、`checkoutForm` として設定する。
4. カートを `cart` として画面モデルに設定する。
5. `bindCheckoutAddressOptions` を呼び出し、会員状態と追加お届け先の選択肢を画面モデルに設定する。
6. `pages/checkout-input` を返す。

### `checkoutInputSubmit`（`POST /checkout/input`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カートが空の場合：`/cart` へリダイレクトする。
3. 入力フォームを正規化し、`validateCheckoutFormBusinessRules` を呼び出して業務ルールを検証する。
4. 入力エラーがある場合：
   1. 警告ログを記録する。
   2. カートを画面モデルに設定する。
   3. `bindCheckoutAddressOptions` を呼び出し、会員状態と追加お届け先の選択肢を設定する。
   4. `pages/checkout-input` を返し、入力画面を再表示する。
5. 入力エラーがない場合：
   1. 正規化したフォームをセッション属性 `checkout_input_form` に保存する。
   2. 新しい確認用トークンを生成し、`checkout_confirm_token` に保存する。
   3. フォームをフラッシュ属性 `checkoutForm` に設定する。
   4. `/checkout/confirm` へリダイレクトする。

### `checkoutConfirm`（`GET /checkout/confirm`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カートが空の場合：`clearCheckoutSession` を呼び出して購入手続き情報を削除し、`/cart` へリダイレクトする。
3. セッション属性 `checkout_input_form` を確認する。
   - フォームがない場合：`/checkout/input` へリダイレクトする。
4. セッションの確認用トークンを確認する。
   - トークンがない場合：新しいトークンを生成してセッションに保存する。
5. フォーム、カート、クーポン割引額、適用中クーポンコード、確認用トークンを画面モデルに設定する。
6. `pages/checkout-confirm` を返す。

### `placeOrder`（`POST /checkout/confirm`）

1. `CartService.getCart` を呼び出し、現在のカートを取得する。
2. カートが空の場合：`clearCheckoutSession` を呼び出して購入手続き情報を削除し、`/cart` へリダイレクトする。
3. セッション属性 `checkout_input_form` を確認する。
   - フォームがない場合：再入力メッセージをフラッシュ属性 `checkoutError` に設定し、`/checkout/input` へリダイレクトする。
4. セッションの確認用トークンと送信されたトークンを照合する。
   - トークンがない、または一致しない場合：警告ログを記録し、`clearCheckoutSession` を呼び出して購入手続き情報を削除する。確認エラーメッセージを `checkoutError` に設定し、`/checkout/input` へリダイレクトする。
5. トークンが一致する場合：
   1. `MemberSessionService.currentMember` を呼び出して会員 ID を取得する（未ログインの場合は会員 ID なし）。
   2. `OrderService.placeOrder` を呼び出して注文を確定する。
   3. `clearCheckoutSession` と `CartService.clear` を呼び出し、購入手続き情報とカートを消去する。
   4. 注文確定ログを記録し、注文番号を含む `/checkout/complete/{orderNumber}` へリダイレクトする。
   - 注文確定で `IllegalArgumentException` が発生した場合：警告ログを記録し、例外メッセージを `checkoutError` に設定して `/checkout/input` へリダイレクトする。

### `checkoutCompleteFallback`（`GET /checkout/complete`）

1. `/checkout/input` へリダイレクトする。

### `checkoutComplete`（`GET /checkout/complete/{orderNumber}`）

1. `OrderService.findOrderCompleteView` を呼び出し、注文番号に対応する完了情報を取得する。
   - 注文情報が見つからない場合：HTTP 404 を返す。
2. 取得した注文情報を `order` として画面モデルに設定する。
3. `pages/checkout-complete` を返す。
