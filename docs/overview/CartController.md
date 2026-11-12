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
