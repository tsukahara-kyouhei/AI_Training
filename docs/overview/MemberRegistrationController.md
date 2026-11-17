## 1. 機能概要

- 新しい会員が登録情報を入力し、内容を確認して会員登録できる機能を提供する。
- 入力内容の不備や登録済みメールアドレスを知らせ、修正して再度登録できるようにする。
- 登録完了後にログイン状態へ移行し、会員コードを表示するとともに登録完了メールを送信する。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP GET リクエスト `/members/register` | 会員登録入力画面を要求する。 |
| 入力 | HTTP POST リクエスト `/members/register/confirm` | 会員登録フォームの入力内容を確認するリクエスト。 |
| 入力 | HTTP POST リクエスト `/members/register` | 確認済みの会員登録内容を確定するリクエスト。 |
| 入力 | HTTP GET リクエスト `/members/register/complete` | 会員登録完了画面を要求する。 |
| 入力 | 会員登録フォーム `registerForm` | 個人／法人区分、氏名・氏名カナ、会社名・部署名、メールアドレス、性別、生年月日／記念日、パスワード、メールマガジン希望、郵便番号、住所、配送階数、エレベーター有無、日中連絡先電話番号、FAX を含む。 |
| 入力 | HTTP セッション属性 `PENDING_REGISTER_FORM` | 確認待ちの会員登録フォーム。登録確定時に参照する。 |
| 入力 | HTTP リクエスト／セッション | 登録完了後のログイン状態を設定するために使用する。 |
| 出力 | 遷移先 `pages/member-register` | 会員登録入力画面。入力検証エラーや登録済みメールアドレスの場合も表示する。 |
| 出力 | 遷移先 `pages/member-register-confirm` | 会員登録内容の確認画面。 |
| 出力 | 画面モデル `registerForm` | 入力または確認対象の会員登録情報。確認画面ではパスワードを伏字で表示する。 |
| 出力 | 入力検証結果 | 必須項目・形式・法人の場合の会社名などの検証結果を画面に表示する。 |
| 出力 | HTTP セッション属性 `PENDING_REGISTER_FORM` | 確認済みの会員登録フォームを確定まで保持する。 |
| 出力 | HTTP セッションの認証状態 | 登録完了後に作成した会員をログイン状態にする。 |
| 出力 | 登録完了メール | 会員登録が完了したことを登録メールアドレス宛に通知する。 |
| 出力 | フラッシュ属性 `registeredMemberCode` | 登録完了画面に表示する会員コード（`MEM` と 7 桁の会員 ID）。 |
| 出力 | 遷移先 `redirect:/members/register/complete` | 登録確定後の完了画面へのリダイレクト先。 |
| 出力 | 遷移先 `pages/member-register-complete` | 会員コードと登録完了案内を表示する画面。完了コードがない場合は HTTP 404。 |

## 3. 処理フロー

### `showForm`（GET `/members/register`）

1. 画面モデルに `registerForm` が既に設定されているか確認する。
2. `registerForm` が設定されていない場合：
   1. セッションから確認待ちの `PENDING_REGISTER_FORM` を取得する。
   2. 確認待ちフォームがある場合はそれを画面モデルに設定し、ない場合は新しい `MemberRegisterForm` を設定する。
3. 会員登録入力画面 `pages/member-register` を返す。

### `confirm`（POST `/members/register/confirm`）

1. 入力フォームを正規化する。
2. フォームの基本項目を検証し、会員種別に応じた追加ルールを検証する。
   - 法人会員で会社名が未入力または空白の場合：会社名の入力エラーを追加する。
3. メールアドレスにエラーがない場合、`MemberService` を呼び出して登録済みメールアドレスか確認する。
   - 既に登録されている場合：メールアドレスの重複エラーを追加する。
4. 入力エラーがある場合：
   1. エラー件数をログに記録する。
   2. 会員登録入力画面 `pages/member-register` を返す。
5. 入力エラーがない場合：
   1. 正規化したフォームをセッション属性 `PENDING_REGISTER_FORM` に保存する。
   2. フォームを画面モデル `registerForm` に設定する。
   3. 確認画面 `pages/member-register-confirm` を返す。

### `register`（POST `/members/register`）

1. セッションから確認待ちの `PENDING_REGISTER_FORM` を取得する。
2. 確認待ちフォームがない場合、`redirect:/members/register` へリダイレクトする。
3. 確認待ちフォームがある場合、`MemberService` を呼び出して会員登録を確定する。
4. 登録時にメールアドレスの重複が判明した場合：
   1. 登録拒否をログに記録する。
   2. メールアドレスの重複エラーを画面モデルに設定する。
   3. 会員登録入力画面 `pages/member-register` を返す。
5. 登録に成功した場合：
   1. セッションから `PENDING_REGISTER_FORM` を削除する。
   2. `MemberSessionService` を呼び出して、作成した会員をログイン状態にする。
   3. `NotificationMailService` を呼び出して、会員登録完了メールを送信する。
   4. 登録完了をログに記録する。
   5. 会員 ID を `MEM` と 7 桁の数字で整形し、フラッシュ属性 `registeredMemberCode` に設定する。
   6. `redirect:/members/register/complete` へリダイレクトする。

### `complete`（GET `/members/register/complete`）

1. 画面モデルに `registeredMemberCode` が設定されているか確認する。
2. `registeredMemberCode` が設定されていない場合、HTTP 404 の例外を送出する。
3. 設定されている場合、会員登録完了画面 `pages/member-register-complete` を返す。
