# 画面一覧

本ドキュメントはシステムが提供するすべての画面を一覧にまとめたものです。
遷移の概要は [screen-flow-diagram.md](./screen-flow-diagram.md) を参照してください。

---

## 凡例

| 記号 | 意味 |
|------|------|
| −   | ログイン不要（未認証でも閲覧可） |
| ○   | ログイン必須（未認証時はログイン画面へリダイレクト） |
| △   | 任意（ゲスト可・ログイン済みは会員情報を初期表示） |

---

## 画面一覧表

| 画面ID | カテゴリ | 画面名 | URL（メソッド） | 主な機能 | ログイン要否 | 担当コントローラー |
|--------|----------|--------|----------------|----------|-------------|-------------------|
| SC-001 | 情報・固定ページ | トップ | `GET /` | 新着商品・おすすめ商品のプロモーション表示 | − | `HomeController` |
| SC-002 | 情報・固定ページ | お知らせ一覧 | `GET /announcements` | 運営からのお知らせ一覧表示 | − | `HomeController` |
| SC-003 | 情報・固定ページ | OFFICE ORDER について | `GET /about` | ブランド紹介・会社概要の固定コンテンツ | − | `ContentController` |
| SC-004 | 情報・固定ページ | ご利用ガイド | `GET /guide` | 購入フロー・配送・返品などのガイド固定コンテンツ | − | `ContentController` |
| SC-005 | 情報・固定ページ | お問い合わせ | `GET /contact`<br>`POST /contact` | お問い合わせフォームの入力・送信（メール会員情報を初期値反映） | △ | `ContactController` |
| SC-006 | 情報・固定ページ | 利用規約 | `GET /legal/terms` | 利用規約の固定コンテンツ表示 | − | `ContentController` |
| SC-007 | 情報・固定ページ | プライバシーポリシー | `GET /legal/privacy` | プライバシーポリシーの固定コンテンツ表示 | − | `ContentController` |
| SC-008 | 情報・固定ページ | 特定商取引法に基づく表記 | `GET /legal/tokusho` | 特定商取引法表記の固定コンテンツ表示 | − | `ContentController` |
| SC-009 | 認証 | ログイン | `GET /login` | メールアドレス・パスワードによるログイン（Spring Security が処理）。ログイン済みの場合は購入履歴一覧へリダイレクト | − | `AuthController` |
| SC-010 | 会員登録 | 会員登録入力 | `GET /members/register` | 会員登録フォームの入力（氏名・メールアドレス・パスワード等） | − | `MemberRegistrationController` |
| SC-011 | 会員登録 | 会員登録確認 | `POST /members/register/confirm` | 入力内容の確認表示。バリデーションエラー時は入力画面を再表示 | − | `MemberRegistrationController` |
| SC-012 | 会員登録 | 会員登録完了 | `GET /members/register/complete` | 登録完了メッセージと会員コードの表示。登録と同時に自動ログイン | − | `MemberRegistrationController` |
| SC-013 | 商品カタログ | 新着商品一覧 | `GET /products/new-arrivals` | 新着商品の一覧表示。価格帯・カラー絞り込み・並び替え・ページネーション | − | `CatalogController` |
| SC-014 | 商品カタログ | キーワード検索結果 | `GET /products/search` | キーワード・価格帯・カラー・テイスト絞り込み検索結果の一覧表示。ページネーション | − | `CatalogController` |
| SC-015 | 商品カタログ | デスクカテゴリ一覧 | `GET /categories/desks` | デスク商品の一覧表示。天板形状・幅・奥行き・高さ・テイスト・価格帯・カラー絞り込み・ページネーション | − | `CatalogController` |
| SC-016 | 商品カタログ | チェアカテゴリ一覧 | `GET /categories/chairs` | チェア商品の一覧表示。機能・素材・テイスト・価格帯・カラー絞り込み・ページネーション | − | `CatalogController` |
| SC-017 | 商品カタログ | 収納家具カテゴリ一覧 | `GET /categories/storages` | 収納家具商品の一覧表示。用途・テイスト・価格帯・カラー絞り込み・ページネーション | − | `CatalogController` |
| SC-018 | 商品カタログ | 商品詳細 | `GET /products/{productId}` | 商品画像・仕様・カラーバリエーション・在庫状況の表示。カート追加・お気に入り登録（要ログイン） | − | `CatalogController` |
| SC-019 | カート・購入フロー | カート | `GET /cart` | カート内商品の一覧・数量変更・削除・合計金額表示。購入フローへの導線 | − | `CartController` |
| SC-020 | カート・購入フロー | 購入方法選択 | `GET /checkout/method` | ゲスト購入・会員ログインの選択。ログインフォーム（メールアドレス記憶対応）を含む | − | `CartController` |
| SC-021 | カート・購入フロー | 注文情報入力 | `GET /checkout/input`<br>`POST /checkout/input` | 配送先・支払方法の入力。会員はプロフィール・追加お届け先から選択可。バリデーション実施 | △ | `CartController` |
| SC-022 | カート・購入フロー | 注文確認 | `GET /checkout/confirm`<br>`POST /checkout/confirm` | 入力内容・カートの最終確認。二重送信防止トークン検証。注文確定 POST で入力画面へリダイレクト（エラー時）または注文完了へ遷移 | △ | `CartController` |
| SC-023 | カート・購入フロー | 注文完了 | `GET /checkout/complete/{orderNumber}` | 注文番号・注文内容のサマリー表示。注文完了メール送信済み通知 | − | `CartController` |
| SC-024 | マイページ | 購入履歴一覧 | `GET /mypage/orders` | 自分の注文一覧のページネーション表示。`GET /mypage` からリダイレクト | ○ | `MyPageController` |
| SC-025 | マイページ | 購入履歴詳細 | `GET /mypage/orders/{orderNumber}` | 注文の詳細情報（商品・金額・ステータス履歴）表示。再購入（カートへ追加） | ○ | `MyPageController` |
| SC-026 | マイページ | お気に入り一覧 | `GET /mypage/favorites` | お気に入り登録済み商品の一覧表示。ページネーション | ○ | `MyPageController` |
| SC-027 | マイページ | 会員情報変更 | `GET /mypage/profile`<br>`POST /mypage/profile` | 氏名・メールアドレス・パスワード等の会員情報変更。保存後セッション更新 | ○ | `MyPageController` |
| SC-028 | マイページ | 追加お届け先一覧 | `GET /mypage/addresses` | 追加お届け先の一覧表示。登録件数上限（5件）の制御 | ○ | `MyPageController` |
| SC-029 | マイページ | 追加お届け先フォーム（新規・編集） | `GET /mypage/addresses/new`<br>`POST /mypage/addresses`<br>`GET /mypage/addresses/{id}/edit`<br>`POST /mypage/addresses/{id}` | 追加お届け先の新規登録・編集。新規・編集で同一テンプレートを共用 | ○ | `MyPageController` |
| SC-030 | マイページ | 退会確認 | `GET /mypage/withdraw`<br>`POST /mypage/withdraw` | 退会の最終確認と実行。退会後はセッションを破棄してログイン画面へリダイレクト | ○ | `MyPageController` |
| SC-031 | エラー | エラー画面 | `/error` | HTTPステータスコードに応じたエラーメッセージ表示（4xx: `error/error.html`、5xx: `error/500.html`）。リクエストIDを表示 | − | `AppErrorController` |

---

## API エンドポイント（画面なし）

以下は HTML 画面を返さない API エンドポイントです。画面一覧の対象外ですが参考として記載します。

| URL（メソッド） | 説明 |
|----------------|------|
| `POST /login` | Spring Security が処理するログイン認証エンドポイント |
| `POST /logout` | Spring Security が処理するログアウトエンドポイント |
| `GET /products/recently-viewed` | 最近見た商品一覧を JSON で返す（非同期リクエスト用） |
| `POST /products/{productId}/favorite` | お気に入り追加・解除（リダイレクトレスポンス） |
| `POST /cart/items` | カートへ商品追加（リダイレクトレスポンス） |
| `POST /cart/items/{productVariantId}/update` | カート数量・組立指定更新（リダイレクトレスポンス） |
| `POST /cart/items/{productVariantId}/delete` | カート明細削除（リダイレクトレスポンス） |
| `POST /cart/clear` | カート全削除（リダイレクトレスポンス） |
| `POST /mypage/orders/{orderNumber}/reorder` | 再購入（カートへ追加・リダイレクトレスポンス） |
| `POST /mypage/addresses/{id}/delete` | 追加お届け先削除（リダイレクトレスポンス） |
