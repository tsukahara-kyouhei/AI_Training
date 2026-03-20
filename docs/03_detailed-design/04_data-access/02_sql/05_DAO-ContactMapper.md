# ContactMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/ContactMapper.xml`  
対象テーブル: `inquiries`, `members`

---

## 2. selectMemberContactPrefill（フォーム初期値）

ログイン済み会員のお問い合わせフォームに氏名・メールをプリフィルする。

```sql
SELECT
  m.last_name,
  m.first_name,
  m.email
FROM members m
WHERE m.member_id = #{memberId}
  AND m.member_status = 'active'
```

---

## 3. insertInquiry（お問い合わせ登録）

```sql
INSERT INTO inquiries (
  member_id,
  inquiry_type,
  last_name,
  first_name,
  email,
  phone,
  body,
  created_at
) VALUES (
  #{memberId},
  #{form.inquiryType},
  #{form.lastName},
  #{form.firstName},
  #{form.email},
  #{form.phone},
  #{form.body},
  CURRENT_TIMESTAMP
)
RETURNING inquiry_id
```

**動作:**
- ゲスト送信時は `memberId = NULL`
- ログイン済みの場合は会員IDを設定して保存
- 返り値は採番された `inquiry_id`
