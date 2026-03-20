# MemberMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/MemberMapper.xml`  
対象テーブル: `members`, `member_addresses`, `member_favorites`

---

## 2. existsByEmail（メール重複チェック）

```sql
SELECT EXISTS(
  SELECT 1 FROM members
  WHERE email = #{email}
    AND member_status != 'withdrawn'
)
```

---

## 3. selectActiveCredentialByEmail（ログイン認証用）

```sql
SELECT member_id, email, password_hash
FROM members
WHERE email = #{email}
  AND member_status = 'active'
```

---

## 4. insertMember（会員登録）

```sql
INSERT INTO members (
  last_name, first_name, last_name_kana, first_name_kana,
  company_name, department_name, email, password_hash,
  daytime_phone, fax, postal_code, prefecture, city, address_line,
  delivery_floor, has_elevator,
  personal_or_corporate, member_type,
  member_status, registered_at, created_at, updated_at
) VALUES (
  #{form.lastName}, #{form.firstName}, ...
  'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
RETURNING member_id
```

---

## 5. selectAdditionalAddresses（追加送り先一覧）

```sql
SELECT
  member_address_id, last_name, first_name, ...
FROM member_addresses
WHERE member_id = #{memberId}
ORDER BY created_at DESC
LIMIT #{limit} OFFSET #{offset}
```

---

## 6. selectFavorites（お気に入り一覧）

```sql
SELECT
  p.product_id, p.product_name,
  MIN(pv.unit_price) AS min_price,
  MAX(pv.stock_quantity) AS max_stock,
  MIN(pv.product_code) AS product_code
FROM member_favorites mf
JOIN products p ON p.product_id = mf.product_id
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE mf.member_id = #{memberId}
  AND p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
GROUP BY p.product_id, p.product_name
ORDER BY mf.created_at DESC
LIMIT #{limit} OFFSET #{offset}
```

---

## 7. withdrawMember（退会処理）

```sql
UPDATE members
SET
  member_status = 'withdrawn',
  withdrawn_at  = #{now},
  updated_at    = CURRENT_TIMESTAMP
WHERE member_id = #{memberId}
```
