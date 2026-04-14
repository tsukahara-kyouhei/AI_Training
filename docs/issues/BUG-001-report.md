# BUG-001 障害レポート — 注文確定時のシステムエラー

| 項目 | 内容 |
|------|------|
| 発生日時 | 2026/3/10 9:12 |
| 報告日 | 2026/4/14 |
| 対象機能 | 注文確定（POST /checkout/confirm） |
| 重大度 | High（ローカル環境で初期構築当日に注文操作が完全に不可能） |

---

## 1. 事象

`POST /checkout/confirm` で「注文を確定する」ボタンを押下するとシステムエラー画面に遷移する。

### 再現手順
1. 会員ユーザー（member01@example.com）でログイン
2. カートに商品を 2 件（P0001-C01、P0001-C02）追加
3. カートから注文確認画面まで遷移（各入力項目はデフォルトのまま）
4. 「注文を確定する」ボタン押下
5. → システムエラー画面に遷移

### 再現条件
- **ローカル開発環境の初期構築当日（`init-local-postgres.ps1` 実行日と同日）に発生**
- 前日以前に初期構築した環境では再現しない（コードは同一）

---

## 2. 推定原因

### 根本原因（High Confidence）

**シードデータが `CURRENT_DATE`（初期化実行日）を注文番号のプレフィックスとして使用しており、同日に新規注文を置くと `orders.order_number` の UNIQUE 制約違反が発生する。**

#### 詳細

##### ① シードデータの注文番号生成（`sql/seed/test-data/orders.sql` 86行目付近）

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
```

`generate_series(1, 100)` のループで、以下 100 件の注文番号を生成・挿入する：

```
ORD{初期化実行日YYYYMMDD}-000001
ORD{初期化実行日YYYYMMDD}-000002
...
ORD{初期化実行日YYYYMMDD}-000100
```

例：2026/3/10 に初期構築を実施した場合、`orders` テーブルには `ORD20260310-000001` ～ `ORD20260310-000100` が存在する。

##### ② 日次連番テーブルが未投入（`order_number_counters` 空）

シードデータは `order_number_counters` テーブルに一切 INSERT しない。
初期構築直後、このテーブルは空の状態になる。

##### ③ 新規注文の番号採番ロジック（`OrderService.generateOrderNumber()`）

```java
private String generateOrderNumber(OffsetDateTime orderDatetime) {
    LocalDate orderDate = orderDatetime.toLocalDate();
    int sequence = orderRepository.nextOrderSequence(orderDate);  // → 1
    ...
    return "ORD" + datePart + "-" + String.format("%06d", sequence); // → "ORD20260310-000001"
}
```

`nextOrderSequence` は `order_number_counters` テーブルへの `INSERT ... ON CONFLICT DO UPDATE ... RETURNING last_sequence` を実行する：

```sql
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
  SET last_sequence = order_number_counters.last_sequence + 1
RETURNING last_sequence
```

`order_number_counters` が空なので初回は必ず `last_sequence = 1` が返る。

##### ④ UNIQUE 制約違反の発生フロー

| ステップ | 内容 |
|----------|------|
| 1 | `generateOrderNumber(2026-03-10T09:12:...)` が呼ばれる |
| 2 | `nextOrderSequence('2026-03-10')` → `1` を返す（`order_number_counters` は空） |
| 3 | 注文番号 `ORD20260310-000001` を生成 |
| 4 | `insertOrder(...)` で `orders` テーブルへ INSERT を試みる |
| 5 | `order_number` 列の UNIQUE 制約違反（`ORD20260310-000001` は既にシードデータで挿入済み） |
| 6 | `DataIntegrityViolationException` が `@Transactional` メソッド外に伝播 |
| 7 | 未ハンドリングの例外 → システムエラー画面に遷移 |

#### 人によって再現しない理由

| ケース | 初期構築日 | 注文試行日 | シードの注文番号プレフィックス | 生成される注文番号 | 結果 |
|--------|-----------|------------|-------------------------------|-------------------|------|
| 先に構築した人 | 3/8 以前 | 3/10 | `ORD20260308-*` | `ORD20260310-000001` | **衝突なし → 正常** |
| 今回の報告者 | 3/10 | 3/10 | `ORD20260310-*` | `ORD20260310-000001` | **衝突 → エラー** |

---

## 3. 原因確定に必要な追加情報

以下の情報が取得できれば原因を確定できる。**現時点では状況証拠のみであり、仮説の段階であることに留意すること。**

### 3-1. アプリケーションログ（最優先）

発生日時（2026/3/10 9:12）付近の Spring Boot ログ（`log/` または標準出力）から例外スタックトレースを確認する。

```bash
# 例: logback-spring.xml のログ出力先を確認しファイルを参照
# または console 出力をスクロールする
```

**確認ポイント**: 例外クラスが `org.springframework.dao.DataIntegrityViolationException` または `org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "orders_order_number_key"` であれば本仮説が確定する。

### 3-2. データベース状態の確認

現在のローカル環境が 2026/3/10 に構築されたものであれば、以下のクエリで確認できる。

```sql
-- ① シードで挿入された当日分の注文番号が存在するか
SELECT count(*), min(order_number), max(order_number)
FROM orders
WHERE order_number LIKE 'ORD20260310-%';
-- 期待値: count = 100 (if 3/10 setup)

-- ② order_number_counters に 2026-03-10 のレコードが存在するか
SELECT * FROM order_number_counters WHERE order_date = '2026-03-10';
-- 仮説通りなら: 0 行（もしくは last_sequence = 1 で失敗した痕跡）
```

### 3-3. 初期構築の実施日

`git log` や `orders` テーブルの `created_at` 最小値から、初期構築が 2026/3/10 であったことを確認する。

```sql
SELECT MIN(created_at) FROM orders;
```

---

## 4. 影響範囲

| 項目 | 内容 |
|------|------|
| 対象環境 | ローカル開発環境のみ（本番・ステージング環境のシードデータ構成による） |
| 再現日 | 初期構築実行日のみ（翌日以降は `CURRENT_DATE` が変わるため衝突しない） |
| 会員 / 非会員 | 未確認。注文番号採番ロジックはどちらも共通のため、同様に再現すると考えられる |
| カート内の商品数 | 注文番号採番時点（`insertOrder` 直前）に衝突するため、1 件でも同様に再現すると考えられる |
| 本番環境 | 本番環境ではシードデータは投入しないため、通常は影響なし |

---

## 5. 暫定回避策

初期構築当日の注文動作確認をスキップするか、以下のいずれかを手動実施する。

```sql
-- ① 当日分のカウンターを100に設定する（シードの最大連番に合わせる）
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
VALUES (CURRENT_DATE, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (order_date)
  DO UPDATE SET last_sequence = 100, updated_at = CURRENT_TIMESTAMP;
```

これにより次回の `nextOrderSequence` は `101` を返し、`ORD{TODAY}-000101` から採番されて衝突しなくなる。

---

## 6. 修正方針（案）

ログおよびDB状態確認で本仮説が確定した後、以下いずれかの方針で `sql/seed/test-data/orders.sql` を修正する。

| 案 | 内容 | メリット | デメリット |
|----|------|----------|------------|
| **A（推奨）** | 注文番号のプレフィックスを過去固定日付（例: `'20250101'`）に変更 | シンプルで確実 | 注文日時との整合性がとれなくなる |
| **B** | シードの末尾で `order_number_counters` に当日分レコードを挿入する | テーブルの整合性が取れる | シードの実行日依存が残る |
| **C** | 注文番号のプレフィックスに `(NOW() - INTERVAL '1 day')::date` を使用 | 前日付で発行されるため衝突しない | 翌日に再実行すると再度衝突する可能性あり |

---

## 7. 参照ファイル

| ファイル | 役割 |
|---------|------|
| [sql/seed/test-data/orders.sql](../../sql/seed/test-data/orders.sql) | 問題のシードデータ（`CURRENT_DATE` を使用） |
| [sql/schema/orders.sql](../../sql/schema/orders.sql) | `orders.order_number UNIQUE` 制約の定義 |
| [scripts/init-local-postgres.ps1](../../scripts/init-local-postgres.ps1) | 初期構築スクリプト |
| [src/main/java/jp/co/skig/officeorder/service/order/OrderService.java](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java) | `generateOrderNumber()` / `placeOrder()` |
| [src/main/resources/mappers/OrderMapper.xml](../../src/main/resources/mappers/OrderMapper.xml) | `nextOrderSequence` SQL |
