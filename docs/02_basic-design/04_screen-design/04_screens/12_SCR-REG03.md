# 会員登録完了

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-REG03 |
| テンプレート | `pages/member-register-complete.html` |
| URL | `GET /members/register/complete` |
| Controller | `MemberRegistrationController#complete()` |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| 完了メッセージ | 「会員登録が完了しました」 |
| 会員ID表示 | 採番された会員コード |
| ログインページ移動 | `/login` へのリンク |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `registeredMemberCode` | `String` | 登録された会員ID（Flash属性） |
