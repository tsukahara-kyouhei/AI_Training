# お知らせサービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `AnnouncementService` | `service.announcement` | 公開中お知らせ取得 |
| `AnnouncementRepository` | `repository` | DBアクセス |
| `HomeController` | `web.controller` | トップ画面・お知らせ一覧 |

---

## 2. 公開中お知らせの定義

```sql
WHERE is_active = TRUE
  AND published_start_at <= CURRENT_TIMESTAMP
  AND (published_end_at IS NULL OR published_end_at > CURRENT_TIMESTAMP)
ORDER BY published_start_at DESC
```

---

## 3. 主要メソッド

### 3.1 findActiveAnnouncements(limit)

- **機能:** 公開中お知らせを指定件数分取得
- **トップ画面:** 上位3件
- **お知らせ一覧画面:** 全件取得（ページングなし）
- **戻り値:** `List<AnnouncementView>`

---

## 4. AnnouncementView

| フィールド | 型 | 説明 |
|---|---|---|
| `announcementId` | Long | ID |
| `title` | String | タイトル |
| `body` | String | 本文（Thymeleafにて自動エスケープ・テキストのみ） |
| `publishStartAt` | LocalDateTime | 掲載開始日時 |

---

## 5. 備考

- お知らせの登録・編集は管理画面（本システム外）で実施
- 公開期間の切れたお知らせは自動的に非表示となる（アプリ層で条件フィルタリング）
