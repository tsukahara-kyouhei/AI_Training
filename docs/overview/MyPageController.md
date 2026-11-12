## 1. 機能概要

- ログイン済み会員が注文履歴や注文内容を確認し、過去に購入した商品をカートへ再度追加できる。
- お気に入り商品を一覧で確認できる。
- 登録済みの会員情報を変更できる。
- 追加のお届け先を登録・編集・削除し、購入時に利用できるよう管理できる。
- 退会内容を確認して退会手続きを行える。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP セッションのログイン会員情報 | 各マイページ機能で会員を識別する。未ログインの場合は HTTP 401 を返す。 |
| 入力 | クエリパラメータ `page` | 注文履歴、お気に入り、追加お届け先一覧のページ番号。省略時は `1`。 |
| 入力 | URL パス変数 `orderNumber` | 注文履歴詳細または再注文の対象となる注文番号。 |
| 入力 | URL パス変数 `memberAddressId` | 表示・更新・削除する追加お届け先の ID。 |
| 入力 | 会員情報フォーム `profileForm` | 個人／法人区分、氏名・氏名カナ、会社名・部署名、メールアドレス、性別、生年月日／記念日、メールマガジン希望、郵便番号、住所、配送階数、エレベーター有無、日中連絡先電話番号、FAX。 |
| 入力 | 追加お届け先フォーム `addressForm` | 氏名・氏名カナ、会社名・部署名、郵便番号、住所、配送階数、エレベーター有無、日中連絡先電話番号、FAX。 |
| 入力 | HTTP リクエスト／レスポンス | 注文履歴から商品を再びカートに追加する際に使用する。 |
| 出力 | 遷移先 `redirect:/mypage/orders` | `/mypage` にアクセスした場合の既定遷移先。 |
| 出力 | 遷移先 `pages/mypage-orders-list` | 注文履歴一覧。モデル `orders`、`currentPage`、`totalPages` を渡す。 |
| 出力 | 遷移先 `pages/mypage-order-detail` | 注文履歴詳細。モデル `orderDetail` に対象注文情報を渡す。対象がない場合は HTTP 404。 |
| 出力 | 遷移先 `redirect:/cart` | 再注文した商品のカート追加後の遷移先。 |
| 出力 | フラッシュ属性 `cartMessage` | 再注文商品の追加成功を知らせるメッセージ。 |
| 出力 | フラッシュ属性 `cartError` | 再注文できる商品がない場合、または一部商品の追加に失敗した場合の案内。 |
| 出力 | 遷移先 `pages/mypage-favorites` | お気に入り一覧。モデル `favorites`、`favoriteCount`、`favoriteLimit`、`currentPage`、`totalPages` を渡す。 |
| 出力 | 遷移先 `pages/mypage-profile-edit` | 会員情報変更画面。モデル `profileForm` に現在の会員情報または送信されたフォームを渡す。 |
| 出力 | 入力検証結果 | 会員情報に不備がある場合、項目別エラーを画面へ表示する。登録済みメールアドレスや法人会員の会社名不足も検証対象。 |
| 出力 | フラッシュ属性 `profileUpdatedMessage` | 会員情報の更新完了を知らせるメッセージ。 |
| 出力 | 遷移先 `redirect:/mypage/profile` | 会員情報の更新後の遷移先。 |
| 出力 | 遷移先 `pages/mypage-addresses` | 追加お届け先一覧。モデル `addresses`、`addressCount`、`addressLimit`、`addressLimitReached`、`currentPage`、`totalPages` を渡す。 |
| 出力 | 遷移先 `pages/mypage-address-form` | 追加お届け先の新規登録・編集画面。モデル `addressForm`、`formAction`、`isEditMode`、`cancelUrl` を渡す。 |
| 出力 | 画面モデル `addressLimitError` | 追加お届け先の新規登録画面で、登録上限に達していることを知らせる案内。 |
| 出力 | フラッシュ属性 `addressLimitError` | 追加お届け先の登録時に上限を超えた場合、一覧画面で表示するエラーメッセージ。 |
| 出力 | 入力検証結果 | 追加お届け先に不備がある場合、項目別エラーを画面へ表示する。法人会員の会社名も検証対象。 |
| 出力 | 遷移先 `redirect:/mypage/addresses` | 追加お届け先の登録・更新・削除後の一覧画面。 |
| 出力 | HTTP 404 | 指定された注文または追加お届け先が見つからない場合に返す。 |
| 出力 | HTTP 401 | ログインしていない状態でマイページ機能にアクセスした場合に返す。 |
| 出力 | 遷移先 `pages/mypage-withdraw` | 退会確認画面。 |
| 出力 | 遷移先 `redirect:/login` | 退会処理後のログイン画面。会員セッションを破棄する。 |
