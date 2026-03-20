# カート操作シーケンス図

## 1. カート表示

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant CC as CartController
    participant CS as CartService
    participant CR as CartRepository

    U->>CC: GET /cart
    CC->>CS: getCart()
    alt 会員
        CS->>CR: findMemberCart(memberId)
        CR-->>CS: cart_lines
    else ゲスト
        CS->>CS: CookieからCartCookieItem[]復元
        CS->>CR: findProductSnapshotsByVariantIds(ids)
        CR-->>CS: CartProductSnapshot[]
    end
    CS->>CR: findCurrentTaxRatePercent()
    CR-->>CS: 税率
    CS-->>CC: CartView
    CC-->>U: カート画面
```

---

## 2. 商品をカートに追加

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant CC as CartController
    participant CS as CartService
    participant CR as CartRepository

    U->>CC: POST /cart/items (variantId, quantity)
    CC->>CS: addItem(variantId, quantity)
    alt 会員
        CS->>CR: カート存在確認
        alt カートなし
            CS->>CR: insertCart(memberId)
        end
        CS->>CR: upsertCartLine(cartId, variantId, quantity)
    else ゲスト
        CS->>CS: CookieアイテムにvariantIdがあれば数量加算
        CS->>CS: Cookieに保存
    end
    CC-->>U: 302 /cart
```

---

## 3. 数量変更・商品削除・全削除

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant CC as CartController
    participant CS as CartService

    U->>CC: POST /cart/items/{id}/update (quantity)
    CC->>CS: updateItem(id, quantity)
    CS-->>CC: 完了
    CC-->>U: 302 /cart

    U->>CC: POST /cart/items/{id}/delete
    CC->>CS: removeItem(id)
    CS-->>CC: 完了
    CC-->>U: 302 /cart

    U->>CC: POST /cart/clear
    CC->>CS: clear()
    CS-->>CC: 完了
    CC-->>U: 302 /cart
```

---

## 4. ゲスト/会員のカート実装差分

| 項目 | ゲスト | 会員 |
|---|---|---|
| データ保存先 | Cookie (`CartCookieStore`) | DB (`shopping_cart`, `cart_lines`) |
| 永続化 | ブラウザを閉じると消なる場合あり | セッションを跨いて永続 |
| 制限件数 | 99数量ぐらい | 99数量ぐらい |
| ログイン時 | Cookieカート → DBカートにマージ | - |
