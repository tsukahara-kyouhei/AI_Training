# 会員登録

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-REG01 |
| テンプレート | `pages/member-register.html` |
| URL | `GET /members/register`、`POST /members/register/confirm` |
| Controller | `MemberRegistrationController#showForm()`、`#confirm()` |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| 基本情報 | 個人/法人・氏名・フリガナ・会社名・メール・性別・生年月日 |
| パスワード | パスワード入力 |
| メルマガ | 送付希望/不希望 |
| 住所情報 | 郵便番号・住所・階数・EV・電話・FAX |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `registerForm` | `MemberRegisterForm` | 会員登録フォーム |

---

## 4. フォーム要素

| フィールド名 | 型 | 必須 | 説明 |
|---|---|---|---|
| `personalOrCorporate` | radio | ○ | 個人/法人 |
| `lastName` / `firstName` | text | ○ | 姓名 |
| `lastNameKana` / `firstNameKana` | text | ○ | フリガナ |
| `companyName` | text | 法人時○ | 会社名 |
| `departmentName` | text | - | 部署名 |
| `email` | email | ○ | メールアドレス |
| `gender` | radio | ○ | 性別（男/女/無回答） |
| `anniversaryDate` | date | - | 生年月日 |
| `password` | password | ○ | パスワード |
| `newsletterOptIn` | radio | ○ | メルマガ送付希望 |
| `postalCodePart1` / `postalCodePart2` | text | ○ | 郵便番号 |
| `prefecture` / `city` / `addressLine` | text | ○ | 住所 |
| `deliveryFloor` | number | ○ | 階数 |
| `hasElevator` | radio | ○ | EV有無 |
| `daytimePhone` | tel | ○ | 日中電話 |
| `fax` | tel | - | FAX |

---

## 5. バリデーション

- `@Valid MemberRegisterForm` によるBean Validation
- 法人時 `companyName` 必須チェック
- メール重複チェック（`existsByEmail`）
- バリデーションOK時: セッションに保存し確認画面へ遷移
