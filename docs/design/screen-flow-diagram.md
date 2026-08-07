# 画面遷移図

以下は主要画面間の遷移図（Mermaid）です。リンクや分岐を簡潔に示しています。

```mermaid
flowchart TD
  Home[/ホーム \n (/)]
  ProductSearch[/商品検索 \n (/products/search)]
  ProductDetail[/商品詳細 \n (/products/{productCode})]
  Cart[/カート \n (/cart)]
  CheckoutInput[/注文入力 \n (/checkout/input)]
  CheckoutConfirm[/注文確認 \n (/checkout/confirm)]
  CheckoutComplete[/注文完了 \n (/checkout/complete)]
  Login[/ログイン \n (/login)]
  MemberRegister[/会員登録 \n (/member/register)]
  MemberRegisterConfirm[/会員登録確認 \n (/member/register/confirm)]
  MemberRegisterComplete[/会員登録完了 \n (/member/register/complete)]
  MyOrders[/注文履歴 \n (/mypage/orders)]
  OrderDetail[/注文詳細 \n (/mypage/orders/{orderId})]
  Contact[/お問い合わせ \n (/contact)]
  Announcements[/お知らせ \n (/announcements)]
  About[/会社情報/法務 \n (/about, /legal/*)]
  Guide[/利用ガイド \n (/guide)]

  %% 基本ナビゲーション
  Home --> ProductSearch
  Home --> Announcements
  Home --> About
  Home --> Guide
  Home --> Login

  %% 商品フロー
  ProductSearch --> ProductDetail
  ProductDetail --> Cart
  Cart --> CheckoutInput
  CheckoutInput --> CheckoutConfirm
  CheckoutConfirm --> CheckoutComplete

  %% ログイン・会員登録フロー
  Login --> MemberRegister
  MemberRegister --> MemberRegisterConfirm
  MemberRegisterConfirm --> MemberRegisterComplete
  Login --> MyOrders
  MyOrders --> OrderDetail

  %% その他
  ProductDetail --> Contact
  AnyFooter[(Footer Links)] --> About
  AnyFooter --> Guide
  AnyFooter --> Contact

  %% ガード/分岐（要ログイン）
  CheckoutInput -. requires login .-> Login
  MyOrders -. requires login .-> Login

  %% モバイル固有
  ProductSearch -->|open filters modal| Cart

```

> 備考: 実装に合わせて遷移や URL が異なる場合があります。必要ならば各コントローラの `@RequestMapping` とテンプレートを参照して図を更新してください。
