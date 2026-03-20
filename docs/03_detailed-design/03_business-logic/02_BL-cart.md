# カートサービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `CartService` | `service.cart` | カート操作（ゲスト/会員共通） |
| `CartCookieStore` | `web` | ゲスト用Cookie一元管理 |
| `CartRepository` | `repository` | DBアクセス（会員カート・商品スナップショット・税率） |

---

## 2. カート実装の二層構造

```
ゲストカート: CartCookieStore -> Cookie(JSON)
会員カート: CartRepository -> DB(shopping_cart, cart_lines)

ログイン時: CartService.mergeCookieCartToMemberCart()
  1. Cookieからアイテム取得
  2. 各アイテムをDBカートにupsert
  3. Cookieをクリア
```

---

## 3. 主要メソッド

### 3.1 getCart()

- Cookieまたは DBからカートアイテム取得
- `findProductSnapshotsByVariantIds()` で現在の商品情報をスナップショット
- `findCurrentTaxRatePercent()` で現在税率取得
- `CartView` を構築して返す

```java
// CartViewの金額計算
subtotal = sum(line.unitPrice * line.quantity)
taxAmount = floor(subtotal * taxRate)
totalAmount = subtotal + taxAmount + shippingFee
```

### 3.2 addItem(variantId, quantity)

1. `variantId` が許可な商品か確認（在庫あり・販売期間内）
2. ゲスト: Cookieに追加（同一variantIdがあれば数量加算）
3. 会員: DBに`UPSERT`（同一variantIdがあれば数量加算）

### 3.3 updateItem(id, quantity)

- `quantity == 0` の場合は `removeItem()` と同等
- 数量制限: 1〜99

### 3.4 removeItem(id)

- ゲスト: Cookieから疲弊消
- 会員: `cart_lines.cart_line_id` でDELETE

### 3.5 clear()

- ゲスト: Cookieを空配列に
- 会員: `cart_lines` をcart_id指定でDELETE全件

---

## 4. Cookieフォーマット

```json
[{"productVariantId": 123, "quantity": 2}, ...]
```

- Cookie名: `cart`
- `HttpOnly=true`, `SameSite=Lax`
- 検証: デシリアライズ時に形式チェック

---

## 5. 制約値

| 項目 | 値 | 定義場所 |
|---|---|---|
| 最大数量/アイテム | 99 | `CartService` 定数 |
| 最大種類/カート | 制限なし（アプリ層） | - |
