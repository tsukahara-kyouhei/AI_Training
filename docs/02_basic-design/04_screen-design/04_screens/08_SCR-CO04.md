# チェックアウト - 完了

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CO04 |
| テンプレート | `pages/checkout-complete.html` |
| URL | `GET /checkout/complete` |
| Controller | `CartController`（確認POST後のリダイレクト先） |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| 完了メッセージ | 「ご注文ありがとうございます」 |
| 注文番号 | 採番された注文番号表示 |
| 注文者情報 | 氏名・メール |
| お届け先情報 | 住所・階数・EV有無 |
| 決済情報 | 決済方法ラベル |
| コンビニ決済案内 | 受付番号・支払期限（コンビニ決済時のみ） |
| 合計金額 | 総支払額表示 |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `order` | `OrderCompleteView` | 注文完了情報 |

### OrderCompleteView の主要プロパティ

| プロパティ | 説明 |
|---|---|
| `orderNumber` | 注文番号 |
| `customerLastName` / `customerFirstName` | 注文者氏名 |
| `customerEmail` | メールアドレス |
| `paymentMethodLabel` | 決済方法ラベル |
| `shippingPostalCode` / `shippingPrefecture` / `shippingCity` / `shippingAddressLine` | お届け先住所 |
| `shippingFloor` / `shippingHasElevator` | 階数・EV有無 |
| `totalAmountText` | 合計金額 |
| `convenienceStorePayment` | コンビニ決済フラグ |
| `convenienceStorePaymentNumber` | 受付番号 |
| `convenienceStorePaymentDueDate` | 支払期限 |

---

## 4. 表示条件

- コンビニ決済案内: `convenienceStorePayment = true` の場合のみ表示
- 注文完了メールが同時に送信される旨を表示
