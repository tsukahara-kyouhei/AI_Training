# お知らせ一覧

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-ANN |
| テンプレート | `pages/announcements.html` |
| URL | `GET /announcements` |
| Controller | `HomeController#announcements()` |
| 認証 | 不要 |

---

## 2. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `announcements` | `List` | お知らせリスト（`announcementService.findAnnouncementList()`） |

---

## 3. 表示項目

`th:each="announcement : ${announcements}"` でループ。

| 項目 | 属性 | 説明 |
|---|---|---|
| 公開日 | `announcement.publishedDateDisplay` | 表示用日付文字列 |
| 公開日(ISO) | `announcement.publishedDateIso` | `<time>` タグの `datetime` 属性 |
| タイトル | `announcement.title` | お知らせタイトル |
| 本文 | `announcement.body` | お知らせ本文 |

---

## 4. 表示条件

- `announcements` が空または null: 「お知らせはありません」表示
