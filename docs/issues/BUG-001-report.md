# BUG-001 障害レポート

## 基本情報

| 項目 | 内容 |
|---|---|
| 障害ID | BUG-001 |
| 発生日時 | 2026/3/10 9:12 |
| 報告日 | 2026/3/31 |
| 原因確定日 | 2026/4/16 |
| 対応完了日 | 2026/4/16 |
| 影響範囲 | 注文確定機能（会員ユーザー） |
| 重大度 | 高（注文不可） |
| 状態 | **対応完了** |

---

## 事象概要

会員ユーザーがカートに商品を追加し、注文確定ボタンを押下するとシステムエラー画面に遷移する。
ローカル開発環境の初期構築直後にのみ発生し、別の開発者が構築した環境では再現しない。

---

## 確定原因

### 結論

**シードデータが生成する注文番号と、アプリが実行時に採番する注文番号が衝突し、`orders.order_number` のUNIQUE制約違反が発生している。**

### エラーログ（証跡）

提供されたエラーログにより原因が確定した。

```
### Error querying database.  Cause: org.postgresql.util.PSQLException:
    ERROR: duplicate key value violates unique constraint "orders_order_number_key"
    Detail: Key (order_number)=(ORD20260416-000001) already exists.
### The error may exist in file [...\mappers\OrderMapper.xml]
### SQL: INSERT INTO orders ( order_number, ... ) VALUES ( ?, ... ) RETURNING order_id
```

| 項目 | 内容 |
|---|---|
| 例外クラス | `org.postgresql.util.PSQLException` |
| 制約名 | `orders_order_number_key`（`orders.order_number UNIQUE` 制約） |
| 衝突した注文番号 | `ORD20260416-000001` |
| 初期構築実施日 | **2026/4/16**（注文番号の日付部分から特定） |
| 発生箇所 | `OrderMapper.xml` の `INSERT INTO orders ... RETURNING order_id` |

### 根拠となるコード・データの分析

#### (1) シードデータが注文番号に「実行当日の日付」を使用している

`sql/seed/test-data/orders.sql` では、100件のテスト注文を以下の形式で生成する。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

`CURRENT_DATE` はSQL実行時（＝初期構築時）の日付であるため、**初期構築を 2026/4/16 に実施した場合、`ORD20260416-000001` ～ `ORD20260416-000100` の 100 件が `orders` テーブルに登録される。**

#### (2) `order_number_counters` テーブルはシードで更新されない

`order_number_counters` テーブルはアプリが注文番号の連番管理に使うテーブルだが、**シードデータはこのテーブルを一切更新しない。**

`sql/seed/masters.sql`、`sql/seed/test-data/orders.sql` のいずれにも `order_number_counters` への `INSERT` は存在しない。

#### (3) 注文確定時の採番ロジック

`OrderService.generateOrderNumber()` → `OrderRepository.nextOrderSequence()` は以下の SQL を実行する。

```sql
-- OrderMapper.xml: nextOrderSequence
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence;
```

`order_number_counters` に当日分のレコードがない場合、`last_sequence = 1` で INSERT し、`1` を返す。
その結果、生成される注文番号は `ORD20260416-000001` となる。

#### (4) UNIQUE制約違反でシステムエラー発生

`orders.order_number` には `UNIQUE` 制約が設定されている。

```sql
-- sql/schema/orders.sql
order_number VARCHAR(20) NOT NULL UNIQUE,
```

既にシードデータで `ORD20260416-000001` が登録済みのため、同じ注文番号での `INSERT INTO orders` が **UNIQUE制約違反** となり、トランザクションがロールバックされて500エラーが発生する。

### 再現条件の整合性確認

| 障害票の条件 | 確定原因との整合 |
|---|---|
| 初期構築直後に発生 | init.sql 実行当日（2026/4/16）のシード注文番号 `ORD20260416-000001` と採番が衝突することをエラーログが証明 |
| 先に構築した人では再現しない | 別の日に init した人のシードは異なる日付の注文番号（例: `ORD20260308-*`）であり、2026/4/16 に注文しても衝突しない |
| 会員ユーザーで発生 | 採番ロジックは非会員でも同一であるため、非会員でも初期構築当日に初めて注文すれば同様に発生すると考えられる |
| 商品2件でも1件でも条件は同じ | 注文番号の採番は商品件数に依存しないため、件数によらず発生する |
| モジュール内容は同じ | コードには問題なく、データ状態の差異が原因 |

---

## まとめ

```
シードデータ投入（CURRENT_DATE = 初期構築日）
    ↓
orders テーブルに ORD{構築日}-000001 ～ ORD{構築日}-000100 が登録される
    ↓
order_number_counters は空のまま（シードで更新されない）
    ↓
構築当日に注文確定を実行
    ↓
nextOrderSequence() が order_number_counters に INSERT → last_sequence = 1 を返す
    ↓
生成された注文番号 ORD{構築日}-000001 が orders テーブルに既存
    ↓
INSERT INTO orders → UNIQUE制約違反 → トランザクションロールバック → システムエラー
```

別の日に初期構築した開発者では `orders` テーブルの注文番号の日付が異なるため、同日に注文しても番号が衝突せず正常に完了する。これにより「個人差」が生じる。

---

## 対応方針

### 方針

**`nextOrderSequence` の初期値算出を `orders` テーブルの既存データから補正する。**

`order_number_counters` に当日分のレコードが存在しない（初回 INSERT）場合、ハードコードされた `1` を初期値とするのではなく、`orders` テーブルに既に存在する当日付け注文番号の最大連番を参照し、その値 `+1` を初期値として挿入する。

`orders` テーブルに当日分の注文が存在しない通常時は `COALESCE` により `0` を基準とするため、`0 + 1 = 1` からの採番となり既存の動作を損なわない。

### 変更対象ファイル

| ファイル | 変更種別 | 変更内容 |
|---|---|---|
| `src/main/resources/mappers/OrderMapper.xml` | 修正 | `nextOrderSequence` の INSERT 初期値を `orders` 参照の COALESCE 式に変更 |

### 変更内容（`OrderMapper.xml` の `nextOrderSequence`）

**変更前**

```sql
INSERT INTO order_number_counters (
    order_date,
    last_sequence,
    created_at,
    updated_at
)
VALUES (
    #{orderDate},
    1,                  -- 初期値が固定で 1 のため、シード注文番号と衝突する
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (order_date) DO UPDATE
SET
    last_sequence = order_number_counters.last_sequence + 1,
    updated_at    = CURRENT_TIMESTAMP
RETURNING last_sequence
```

**変更後**

```sql
INSERT INTO order_number_counters (
    order_date,
    last_sequence,
    created_at,
    updated_at
)
VALUES (
    #{orderDate},
    COALESCE(
        (SELECT MAX(CAST(SUBSTRING(order_number FROM 13) AS INTEGER))
         FROM orders
         WHERE order_number LIKE 'ORD' || TO_CHAR(#{orderDate}, 'YYYYMMDD') || '-%'),
        0
    ) + 1,              -- orders テーブルの最大連番を参照して初期値を補正
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (order_date) DO UPDATE
SET
    last_sequence = order_number_counters.last_sequence + 1,
    updated_at    = CURRENT_TIMESTAMP
RETURNING last_sequence
```

### 変更の根拠

| 項目 | 説明 |
|---|---|
| `SUBSTRING(order_number FROM 13)` | 注文番号 `ORD{YYYYMMDD}-{seq}` の連番部分（13文字目以降の6桁）を抽出。先頭の `ORD`(3) + 日付(8) + `-`(1) = 12文字を読み飛ばす |
| `TO_CHAR(#{orderDate}, 'YYYYMMDD')` | `LocalDate` 型の `orderDate` を注文番号の日付プレフィックスと同形式に整形 |
| `COALESCE(..., 0) + 1` | 当日付けの注文が1件も存在しない場合は `0 + 1 = 1` となり、既存の動作（1から採番）を維持する |
| `ON CONFLICT ... DO UPDATE` は変更なし | カウンタが既に存在する場合は従来どおり `last_sequence + 1` でインクリメントするため変更不要 |

### 考慮事項

- Java コード（`OrderService`・`OrderRepository`）の変更は不要。`OrderMapper.xml` のみの修正で対応できる。
- シードデータ（`sql/seed/test-data/orders.sql`）自体は変更しない。
- 本修正は「初期構築当日」以外のケース（通常運用時）でも影響なく機能する。

---

## 実装結果

### 対応日

2026/4/16

### 変更ファイル

| ファイル | 変更種別 | 変更概要 |
|---|---|---|
| `src/main/resources/mappers/OrderMapper.xml` | 修正 | `nextOrderSequence` の INSERT 初期値を `COALESCE` 式に変更 |
| `src/test/java/.../service/order/OrderServiceGenerateOrderNumberTest.java` | 新規 | `generateOrderNumber` の単体テスト追加 |

### 実装内容

#### `OrderMapper.xml` の `nextOrderSequence`

`last_sequence` 初期値をハードコードの `1` から、`orders` テーブルの当日付け最大連番 `+1` を参照する `COALESCE` 式に変更した。

```sql
-- 変更後（抜粋）
VALUES (
    #{orderDate},
    COALESCE(
        (SELECT MAX(CAST(SUBSTRING(order_number FROM 13) AS INTEGER))
         FROM orders
         WHERE order_number LIKE 'ORD' || TO_CHAR(#{orderDate}, 'YYYYMMDD') || '-%'),
        0
    ) + 1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
```

#### 単体テスト `OrderServiceGenerateOrderNumberTest`

`OrderService.generateOrderNumber`（private）をリフレクション経由で検証する5ケースを追加した。

| テストメソッド | 検証内容 |
|---|---|
| `generateOrderNumber_sequence1_returnsFormattedNumber` | 連番 1 → `ORD20260416-000001` |
| `generateOrderNumber_sequence101_returnsFormattedNumber` | **BUG-001 シナリオ**: シード 100 件後に 101 が返される → `ORD20260416-000101`（衝突なし） |
| `generateOrderNumber_maxSequence_returnsFormattedNumber` | 連番 999999 → `ORD20260416-999999` |
| `generateOrderNumber_differentDate_usesDateFromDatetime` | 日付が注文番号に正しく反映される |
| `generateOrderNumber_sequenceExceedsMax_throwsIllegalState` | 上限超過時に `IllegalStateException` |

### テスト結果

```
Tests run: 135, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

（既存テスト 130 件 + 新規 5 件、全件パス）
