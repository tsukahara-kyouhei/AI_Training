# ログイン

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-LGN |
| テンプレート | `pages/login.html` |
| URL | `GET /login` |
| Controller | `AuthController#login()` |
| 認証 | 不要（既ログイン時は `/mypage/orders` へリダイレクト） |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| ログインフォーム | メール/パスワード入力 |
| 次回入力省略 | メールアドレス記憶チェックボックス |
| エラーメッセージ | ログイン失敗時・セッション切れ時 |
| 会員登録リンク | `/members/register` へのリンク |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `loginForm` | `LoginForm` | フォームオブジェクト |
| `rememberLoginEmail` | `boolean` | メール記憶フラグ |
| `sessionExpired` | `boolean` | セッション切れフラグ |
| `param.error` | - | Spring Security認証エラーフラグ |

---

## 4. フォーム

| 項目 | 値 |
|---|---|
| action | `POST /login`（Spring Securityが処理） |
| `email` | メールアドレス |
| `password` | パスワード |
| `remember_me` | 次回入力省略チェック |
| `redirectPath` | hidden（ログイン後遷移先） |

---

## 5. 表示条件

- `sessionExpired = true`: 「セッションが切れました」メッセージ表示
- `param.error` 存在: 「メールアドレスまたはパスワードが正しくありません」表示
- 記憶済みメール: Cookieから取得してフォームにプリフィル
