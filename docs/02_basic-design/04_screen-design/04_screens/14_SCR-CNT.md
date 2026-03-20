# お問い合わせ

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CNT |
| テンプレート | `pages/contact.html` |
| URL | `GET /contact`、`POST /contact` |
| Controller | `ContactController` |
| 認証 | 不要（ログイン済みの場合はプリフィル） |

---

## 2. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `contactForm` | `ContactForm` | お問い合わせフォーム |
| `contactSuccessMessage` | `String` | 送信成功メッセージ（Flash） |
| `contactInquiryNumber` | `String` | 受付番号 `INQ%08d`（Flash） |
| `contactError` | `String` | 送信失敗エラーメッセージ |

---

## 3. フォーム要素

`th:object="${contactForm}"` でバインド。`POST /contact`。

| フィールド名 | 型 | 必須 | 説明 |
|---|---|---|---|
| `companyName` | text | - | 会社名 |
| `departmentName` | text | - | 部署名 |
| `lastName` / `firstName` | text | ○ | 氏名 |
| `email` | email | ○ | メールアドレス |
| `phone` | tel | - | 電話番号 |
| `inquiryType` | radio | ○ | 問合せ種別（product / delivery_date / order / shipping / return_cancel / other） |
| `orderPhase` | radio | ○ | 注文前/後（before_order / after_order） |
| `productName` | text | - | 商品名 |
| `productCode` | text | - | 商品コード |
| `message` | textarea | ○ | お問い合わせ内容（1000文字以内） |

---

## 4. 表示条件

- `contactSuccessMessage` 存在: 成功アラート + 受付番号表示
- `contactError` 存在: エラーアラート表示
- 各フィールド: `th:if="${#fields.hasErrors('fieldName')}"` でエラー表示

---

## 5. 処理フロー

1. GET時: ログイン中なら会員情報でフォームプリフィル
2. POST時: `@Valid` バリデーション
3. バリデーションNG: フォーム再表示、イベント `CONTACT_INPUT_INVALID` ログ
4. `contactService.submit(memberId, form)` 実行
5. 成功: `/contact` へリダイレクト（Flashで成功メッセージ）
6. 失敗: `CONTACT_SUBMIT_FAILED` ログ、`contactError` でエラー表示
