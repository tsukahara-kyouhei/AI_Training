# 会員登録確認

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-REG02 |
| テンプレート | `pages/member-register-confirm.html` |
| URL | POST成功時の表示（PRGパターン前） |
| Controller | `MemberRegistrationController#confirm()` の成功時表示 |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| 確認テーブル | 入力内容の全フィールドを表形式で表示（th:text） |
| 登録ボタン | `POST /members/register` へ送信 |
| 戻るボタン | 入力画面へ戻る |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `registerForm` | `MemberRegisterForm` | 入力済みフォーム（確認表示用） |

---

## 4. フォーム

| 項目 | 値 |
|---|---|
| action | `POST /members/register` |
| method | POST |

フォームデータはセッションから取得（hiddenフィールドなし）。

---

## 5. 処理フロー

1. 登録ボタン押下 → `POST /members/register`
2. セッションからフォーム取得 → 会員INSERT → パスワードBCryptハッシュ
3. 登録完了メール送信
4. 完了画面へリダイレクト
