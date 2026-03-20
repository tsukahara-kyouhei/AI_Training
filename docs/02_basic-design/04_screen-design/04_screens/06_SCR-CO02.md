# チェックアウト - 入力

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CO02 |
| テンプレート | `pages/checkout-input.html` |
| URL | `GET /checkout/input`、`POST /checkout/input` |
| Controller | `CartController#checkoutInput()`、`CartController#checkoutInputSubmit()` |
| 認証 | 不要（ゲスト購入可） |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| ご注文者情報 | 個人/法人選択、氏名、フリガナ、会社名、メール、電話、FAX |
| お届け先情報 | 登録済みお届け先セレクト/郵便番号/住所/階数/EV有無 |
| 決済手段 | 銀行振込/代引/コンビニ |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `checkoutForm` | `CheckoutInputForm` | 注文入力フォーム |
| `isMemberCheckout` | `boolean` | 会員購入か |
| `hasCheckoutAdditionalAddresses` | `boolean` | 追加お届け先の有無 |
| `checkoutAdditionalAddresses` | `List<MemberAdditionalAddressView>` | の届け先リスト |
| `checkoutError` | `String` | エラーメッセージ |

---

## 4. フォーム要素

| フィールド名 | 型 | 必須 | 説明 |
|---|---|---|---|
| `personalOrCorporate` | radio | ○ | 個人/法人区分 |
| `lastName` / `firstName` | text | ○ | 姓名 |
| `lastNameKana` / `firstNameKana` | text | ○ | フリガナ |
| `companyName` | text | 法人時○ | 会社名 |
| `departmentName` | text | - | 部署名 |
| `email` | email | ○ | メールアドレス |
| `daytimePhone` | tel | ○ | 日中連絡可能電話 |
| `fax` | tel | - | FAX |
| `selectedAdditionalAddressId` | select | - | 登録済みお届け先選択 |
| `postalCodePart1` / `postalCodePart2` | text | ○ | 郵便番号（3桁+4桁） |
| `prefecture` | text | ○ | 都道府県 |
| `city` | text | ○ | 市区町村 |
| `addressLine` | text | ○ | 番地・ビル名 |
| `deliveryFloor` | number | ○ | お届け先階数 |
| `hasElevator` | radio | ○ | エレベーター有無 |
| `paymentMethod` | radio | ○ | 決済手段 |

---

## 5. バリデーション

- `@Valid CheckoutInputForm` によるBean Validation
- 法人選択時は `companyName` 必須
- バリデーションエラー時は同一画面に戻り、エラーメッセージをフィールドごとに表示

---

## 6. 表示条件

- 追加お届け先セレクト: `isMemberCheckout = true` かつ `hasCheckoutAdditionalAddresses = true` の場合のみ
- 会員購入時: 氏名・メール等が会員情報でプリフィルされる
- POST成功時: セッションにフォームを保存し、確認画面へ遷移
