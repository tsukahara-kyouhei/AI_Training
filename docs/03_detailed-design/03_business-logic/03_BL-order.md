# 注文サービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `OrderService` | `service.order` | 注文確定・履歴取得・再購入 |
| `OrderRepository` | `repository` | DBアクセス |

---

## 2. 主要メソッド

### 2.1 createInitialForm(memberId)

- 注文入力フォームの初期値を構築
- 会員ログイン中の場合: `findCheckoutMemberPrefill()` で登録情報をプリフィル
- ゲストの場合: 空の `CheckoutInputForm` を返す
- **戻り値:** `CheckoutInputForm`

### 2.2 placeOrder(form, cart, memberId)

- **トランザクション:** `@Transactional` 必須
- **処理フロー:**

```
1. nextOrderSequence(today) -> ORDER BY order_number_counters FOR UPDATE
2. orderNumber = YYYYMMDD + String.format("%04d", sequence)
3. orders INSERT
4. cart.lines 山のorder_items INSERT
5. order_status_histories INSERT (received)
6. product_variants.stock_quantity -= quantity (FOR UPDATE)
7. カートクリア
8. afterCommit にメール登録
```

- **戻り値:** `orderNumber (String)`
- **エラー:** 在庫不足時 -> `StockShortageException`

### 2.3 findMemberOrderHistories(memberId, page)

- **機能:** 会員の購入履歴一覧（ページング対応）
- **ページサイズ:** `MemberService.MYPAGE_PAGE_SIZE = 20`
- **戻り値:** `Page<MemberOrderHistoryView>`

### 2.4 placeReorder(orderNumber, memberId)

- 注文詳細からアイテムを取得し、カートに追加
- 在庫がない商品はスキップ（警告のみ）
- **戻り値:** 追加件数とスキップ件数

---

## 3. 注文金額計算ロジック

```
円小計 = 合計(単価 x 数量)
消費税額 = floor(小計 x 税率)
組立費合計 = 合計(商品.assembly_fee x 数量) (組立配送選択時のみ)
送料 = 配送方法による定額 (注文入力時にバックエンドにて算出)
合計 = 小計 + 消費税額 + 送料 + 組立費合計
```

---

## 4. 支払方法ごとの追加処理

| 支払方法 | payment_instruction内容 |
|---|---|
| `bank_transfer` | 振込先口座情報 |
| `convenience_store` | 支払コード・期限 |
| `cash_on_delivery` | NULL |
