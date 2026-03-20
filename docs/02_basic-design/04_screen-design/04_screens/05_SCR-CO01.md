# チェックアウト - 支払方法選択

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CO01 |
| テンプレート | `pages/checkout-method.html` |
| URL | `GET /checkout/method` |
| Controller | `CartController#checkoutMethod()` |
| 認証 | 不要（未ログイン時の購入入口） |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| ログインフォーム | メール/パスワード入力 + 記憶チェック |
| 会員登録リンク | `/members/register` へのリンク |
| 会員登録なしで購入 | `/checkout/input` へのリンク |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `rememberLoginEmail` | `boolean` | メールアドレス記憶フラグ |
| `rememberedLoginEmail` | `String` | 記憶済みメールアドレス |
| `checkoutRedirectPath` | `String` | ログイン後の遷移先 |

---

## 4. フォーム

| 項目 | 値 |
|---|---|
| action | `POST /login` |
| `email` | メールアドレス |
| `password` | パスワード |
| `remember_me` | 次回入力省略チェック |
| `redirectPath` | hidden（`/checkout/input`） |
