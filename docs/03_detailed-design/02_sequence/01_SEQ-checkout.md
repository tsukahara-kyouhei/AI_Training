# チェックアウトシーケンス図

## 1. 全体フロー（4ステップ）

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant CO1 as CartController
    participant CS as CartService
    participant CO2 as 支払方法選択
    participant CO3 as 注文入力
    participant CO4 as 注文確認
    participant OS as OrderService
    participant OR as OrderRepository
    participant CR as CartRepository
    participant MS as NotificationMailService

    U->>CO1: GET /checkout/method
    CO1->>CS: getCart()
    CS-->>CO1: CartView
    CO1-->>U: 支払方法選択画面

    U->>CO1: GET /checkout/input
    CO1->>OS: createInitialForm()
    OS->>OR: findCheckoutMemberPrefill(memberId)
    OR-->>OS: CheckoutMemberPrefill
    OS-->>CO1: CheckoutInputForm
    CO1-->>U: 注文情報入力画面

    U->>CO1: POST /checkout/input
    CO1->>CO1: BindingResultバリデーション
    alt バリデーションエラー
        CO1-->>U: 入力画面に戳り
    else OK
        CO1->>CS: getCart()
        CS->>CR: findCurrentTaxRatePercent()
        CR-->>CS: 税率
        CS-->>CO1: CartView
        CO1->>CO1: 確認画面用トークン発行
        CO1-->>U: 注文確認画面
    end

    U->>CO1: POST /checkout/confirm
    CO1->>CO1: トークン検証
    alt トークンNG
        CO1-->>U: 入力画面にり設
    else OK
        CO1->>OS: placeOrder(form, cart)
        OS->>OR: nextOrderSequence(today)
        OR-->>OS: 連番番号
        OS->>OR: insertOrder(order)
        OS->>OR: insertOrderItem(items)
        OS->>OR: insertOrderStatusHistory(received)
        OS->>CR: 在庫接減(variants)
        OS->>CS: clear()
        OS->>MS: sendOrderCompleteMail()をツーコアイヤーに登録
        OS-->>CO1: orderNumber
        CO1-->>U: 302 /checkout/complete/{orderNumber}
        MS-->>U: 注文完了メール（非同期）
    end
```

---

## 2. 注文番号採番詳細

```
ルール: YYYYMMDD + 4桁ゼロ埋め連番
例: 20260320-0001

1. order_number_counters WHERE order_date = today FOR UPDATE
2. レコードなし → INSERT (last_sequence = 1)
   レコードあり → UPDATE last_sequence = last_sequence + 1
3. 返却値で注文番号組み立て
```

## 3. メール送信タイミング

メール送信はトランザクションコミット後に実行される。  
`TransactionSynchronizationManager`で登録し、DBコミット成功時のみ送信。  
送信失敗時はリトライ（`MailRetryConfig`で設定）。

## 4. ゲスト/会員の違い

| 項目 | ゲスト | 会員 |
|---|---|---|
| カート管理 | Cookie | DB (shopping_cart + cart_lines) |
| 注文情報初期値 | 空白 | 登録済み会員情報をプリフィル |
| member_id | NULL | 設定 |
| customer_type | 'guest' | 'member' |
