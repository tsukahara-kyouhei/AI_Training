# AnnouncementMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/AnnouncementMapper.xml`  
対象テーブル: `announcements`

---

## 2. selectActiveAnnouncements（公開中お知らせ取得）

指定時刻 `now` における公開中のお知らせを、公開日の降順で取得する。

```sql
SELECT
  announcement_id,
  title,
  body,
  published_at
FROM announcements
WHERE published_at <= #{now}
  AND (closed_at IS NULL OR closed_at > #{now})
ORDER BY published_at DESC
LIMIT #{limit}
```

**用途:**
- トップページのお知らせ一覧
- お知らせ一覧ページ

**引数:**
- `now` (OffsetDateTime): アプリケーション時刻（`AppClock.now()` の値）
- `limit` (Integer): 取得件数上限（未指定時は `NULL` を渡すと全件）
