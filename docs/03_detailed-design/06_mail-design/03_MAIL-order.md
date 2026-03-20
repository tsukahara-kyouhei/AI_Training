# 注文完了通知メール

## 1. 概要

| 項目 | 内容 |
|---|---|
| メールID | MAIL-002 |
| 送信トリガー | 注文確定（`OrderService.placeOrder()`のコミット後） |
| To | 注文者のメールアドレス |
| テンプレート | `mail/templates/order-complete-body.txt` |
| 件名 | `mail/subjects.properties` の `mail.subject.order-complete` |

---

## 2. テンプレート変数

| 変数名 | 内容 |
|---|---|
| `orderNumber` | 注文番号 |
| `orderDatetime` | 注文日時 |
| `lastName` | 注文者姓 |
| `firstName` | 注文者名 |
| `orderItems` | 注文商品リスト（商品名・数量・単価） |
| `totalAmount` | 合計金額 |
| `shippingAddress` | 届け先住所 |
| `paymentMethod` | 支払方法 |

---

## 3. メール本文構成イメージ

```
件名: 【office-order】ご注文完了のお知らせ - 注文番号: {orderNumber}

{lastName} {firstName} 様

ご注文ありがとうございます。
以下の内容で注文を受け付けました。

[注文情報]
注文番号: {orderNumber}
注文日時: {orderDatetime}

[お届け先]
{shippingAddress}

[注文商品]
{orderItemsの透永展開}

[合計]
合計金額: {totalAmount}円

[支払方法]
{paymentMethod}

このメールは自動送信されました。
返信はお受けできません。
```
