# FEAT-001 障害一覧

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| 作成日 | 2026-03-15 |
| 関連TODO | [FEAT-001-TODO.md](FEAT-001-TODO.md) |

---

## 障害一覧

### BUG-001: 検索キーワード入力時にシステムエラー発生（500エラー）

| 項目 | 内容 |
|------|------|
| 障害ID | BUG-001 |
| 発生日 | 2026-03-15 |
| 重大度 | **高**（検索機能が完全に使用不能） |
| 再現手順 | ヘッダー検索ボックスまたは検索結果ページでキーワードを入力して検索する |
| 発生画面 | `/products/search` |

#### 障害内容

キーワード（例: `1`）を検索ボックスに入力して検索すると、商品一覧が表示されず HTTP 500 エラー画面が表示される。

```
org.postgresql.util.PSQLException: ERROR: column "desk_taste_id" does not exist
```

ログ上のエラーメッセージ:

```
### Error querying database.  Cause: org.postgresql.util.PSQLException:
ERROR: column "desk_taste_id" does not exist
### The error may exist in file [.../mappers/ProductMapper.xml]
```

#### 障害原因

`ProductMapper.xml` に記述した SQL で、テイストマスタテーブルの主キー列名を誤って指定していた。

`desk_tastes` / `chair_tastes` / `storage_tastes` の各テーブルはいずれも主キー列が **`taste_id`** であるにもかかわらず、以下の6箇所で存在しない列名（`desk_taste_id` / `chair_taste_id` / `storage_taste_id`）を使用していた。

| 箇所 | 誤り | 正しい値 |
|------|------|--------|
| `SearchTasteFilter` フラグメント — desk JOIN条件 | `dt.desk_taste_id = da.taste_id` | `dt.taste_id = da.taste_id` |
| `SearchTasteFilter` フラグメント — chair JOIN条件 | `ct.chair_taste_id = ca.taste_id` | `ct.taste_id = ca.taste_id` |
| `SearchTasteFilter` フラグメント — storage JOIN条件 | `st.storage_taste_id = sa.taste_id` | `st.taste_id = sa.taste_id` |
| `selectAllDeskTasteNames` — ORDER BY句 | `ORDER BY sort_order ASC, desk_taste_id ASC` | `ORDER BY sort_order ASC, taste_id ASC` |
| `selectAllChairTasteNames` — ORDER BY句 | `ORDER BY sort_order ASC, chair_taste_id ASC` | `ORDER BY sort_order ASC, taste_id ASC` |
| `selectAllStorageTasteNames` — ORDER BY句 | `ORDER BY sort_order ASC, storage_taste_id ASC` | `ORDER BY sort_order ASC, taste_id ASC` |

スキーマ定義（`sql/schema/masters.sql`）では全テイストテーブルの主キー列名はテーブルを問わず `taste_id` で統一されている。実装時に誤ってテーブル名を接頭辞とした列名（`desk_taste_id` 等）を想定してしまったことが原因。

#### 対応方法

`src/main/resources/mappers/ProductMapper.xml` の上記6箇所を `taste_id` に修正済み。

**修正箇所一覧:**

```xml
<!-- 修正前 -->
JOIN desk_tastes dt ON dt.desk_taste_id = da.taste_id
JOIN chair_tastes ct ON ct.chair_taste_id = ca.taste_id
JOIN storage_tastes st ON st.storage_taste_id = sa.taste_id
SELECT display_name FROM desk_tastes    ORDER BY sort_order ASC, desk_taste_id ASC
SELECT display_name FROM chair_tastes   ORDER BY sort_order ASC, chair_taste_id ASC
SELECT display_name FROM storage_tastes ORDER BY sort_order ASC, storage_taste_id ASC

<!-- 修正後 -->
JOIN desk_tastes dt ON dt.taste_id = da.taste_id
JOIN chair_tastes ct ON ct.taste_id = ca.taste_id
JOIN storage_tastes st ON st.taste_id = sa.taste_id
SELECT display_name FROM desk_tastes    ORDER BY sort_order ASC, taste_id ASC
SELECT display_name FROM chair_tastes   ORDER BY sort_order ASC, taste_id ASC
SELECT display_name FROM storage_tastes ORDER BY sort_order ASC, taste_id ASC
```

#### 再発防止策

- SQL を記述する際は実際のスキーマ定義（`sql/schema/*.sql`）を必ず参照し、列名を確認してから記述する。
- 特に「テーブル名 + 列名」のような命名パターンが全テーブルで統一されているとは限らないため、思い込みによる記述を避ける。

---

### BUG-002: 注文確定時にシステムエラー発生（初期構築当日のみ）

| 項目 | 内容 |
|------|------|
| 障害ID | BUG-002（詳細レポート: [BUG-001-report.md](BUG-001-report.md)） |
| 発生日 | 2026/3/10 |
| 対応完了日 | 2026/4/18 |
| 重大度 | **高**（ローカル開発環境の初期構築当日に注文が一切できない） |
| 再現手順 | `init-local-postgres.ps1` を実行した当日に会員ユーザーで注文確定操作を行う |
| 発生画面 | `POST /checkout/confirm` |

#### 障害内容

ローカル開発環境を初期構築した当日に注文確定操作（「注文を確定する」ボタン押下）を行うと、注文完了画面に遷移せずシステムエラー画面が表示される。翌日以降は再現しない。

#### 障害原因

`sql/seed/test-data/orders.sql` が注文番号を `CURRENT_DATE`（実行日付）を使って生成していたため、初期構築当日に実際の注文操作で採番される注文番号（例: `ORD20260310-000001`）とシードデータの注文番号が衝突し、`orders.order_number` の UNIQUE 制約違反（`DataIntegrityViolationException`）が発生していた。

さらに `order_number_counters` テーブルがシードデータによって初期化されていなかったため、カウンタが常に 1 から採番を開始することで確実に衝突が起きていた。

#### 対応方法

`sql/seed/test-data/orders.sql` の注文番号生成の日付部を `TO_CHAR(CURRENT_DATE, 'YYYYMMDD')` から固定値 `20000101` へ変更した（1行の変更）。

```diff
- 'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
+ 'ORD20000101-' || LPAD(p.n::TEXT, 6, '0'),
```

#### 再発防止策

- **シードデータで日付依存の一意キーを生成する場合は `CURRENT_DATE` / `NOW()` を使用しない。**  
  固定の過去日付（例: `20000101`）を使用することで、将来の実行タイミングに関係なく実データと衝突しないことを保証する。
- シードデータの注文番号は `ORD20000101-XXXXXX` 形式（日付部: `20000101`）で統一されており、テスト用データであることを明示するセンチネル値として機能する。
- 将来シードデータに注文レコードを追加・変更する際は、この規則を必ず踏襲すること（`FEAT-001-TODO.md` の「シードデータ追加時の注意事項」も参照）。

---

*以上*
