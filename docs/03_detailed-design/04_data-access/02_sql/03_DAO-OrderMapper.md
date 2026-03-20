# OrderMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/OrderMapper.xml`  
対象テーブル: `orders`, `order_items`, `order_status_histories`, `order_number_counters`, `members`, `tax_rates`

---

## 2. nextOrderSequence（注文番号連番）

当日の連番をアトミックに発行する。

```sql
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
VALUES (#{orderDate}, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (order_date) DO UPDATE
  SET last_sequence = order_number_counters.last_sequence + 1,
      updated_at    = CURRENT_TIMESTAMP
RETURNING last_sequence
```

> 注文番号フォーマット：`yyyyMMdd` + 4桁ゼロ埋め連番（例: `202401230004`）

---

## 3. insertOrder（注文登録）

```sql
INSERT INTO orders (
  order_number, order_datetime, order_status, customer_type,
  personal_or_corporate, member_id,
  customer_last_name, customer_first_name, ...,
  shipping_postal_code, shipping_prefecture, shipping_city, shipping_address_line,
  payment_method, payment_instruction,  -- JSONB
  shipping_fee, assembly_fee_total, subtotal_amount, tax_amount, total_amount,
  created_at, updated_at
) VALUES (
  ..., 'received', ..., CAST(#{params.paymentInstructionJson} AS jsonb), ...
)
RETURNING order_id
```

---

## 4. insertOrderItem（注文明細登録）

```sql
INSERT INTO order_items (
  order_id, product_code, product_name, color_name,
  unit_price, quantity, line_subtotal, line_tax_amount, line_total_amount,
  assembly_available, assembly_fee,
  created_at, updated_at
) VALUES (...)
```

---

## 5. insertOrderStatusHistory（ステータス履歴登録）

```sql
INSERT INTO order_status_histories (
  order_id, status, changed_by_system, changed_at
) VALUES (
  #{params.orderId}, #{params.status}, #{params.changedBySystem}, CURRENT_TIMESTAMP
)
```

---

## 6. selectMemberOrders（注文履歴一覧）

```sql
SELECT
  order_number, order_datetime, total_amount,
  order_status
FROM orders
WHERE member_id = #{memberId}
ORDER BY order_datetime DESC
LIMIT #{limit} OFFSET #{offset}
```

---

## 7. selectMemberOrderDetail（注文詳細）

```sql
SELECT
  order_number, order_datetime, order_status,
  subtotal_amount, assembly_fee_total, shipping_fee, tax_amount, total_amount
FROM orders
WHERE member_id = #{memberId}
  AND order_number = #{orderNumber}
```

---

## 8. selectCheckoutMemberPrefill（注文フォーム初期値）

```sql
SELECT
  personal_or_corporate, last_name, first_name, ...,
  postal_code, prefecture, city, address_line,
  delivery_floor, has_elevator
FROM members
WHERE member_id = #{memberId}
  AND member_status = 'active'
```
