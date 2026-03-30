# BUG-001 障害レポート：注文確定時のシステムエラー

## 基本情報

| 項目 | 内容 |
|------|------|
| 障害 ID | BUG-001 |
| 発生日時 | 2026/03/30 13:00 頃 |
| 重大度 | 高（注文確定が全件不可） |
| 報告日 | 2026/03/30 |

---

## 事象

会員ユーザー（member01@example.com）が、カートに商品 P0001-C01・P0001-C02 を追加し
注文確定ボタンを押下した際に、注文完了画面へ遷移せずシステムエラー画面に遷移する。

---

## 原因

**シードデータの `order_number` と `order_number_counters` テーブルの不整合による UNIQUE 制約違反**

### 原因の詳細

#### （1）シードデータの order_number 生成ロジック

`sql/seed/test-data/orders.sql` は、100 件の注文を以下の SQL で生成している。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

シードを実行した当日の日付（`CURRENT_DATE`）をそのまま使うため、
2026/03/30 に初期構築を行った場合、以下の order_number が `orders` テーブルに挿入される。

```
ORD20260330-000001
ORD20260330-000002
...
ORD20260330-000100
```

#### （2）`order_number_counters` テーブルにシードデータが存在しない

`order_number_counters` テーブルはシードから一切 INSERT されない。
初期構築直後は空テーブル（0 レコード）の状態になる。（③により確認済み）

#### （3）注文番号採番ロジック（OrderMapper.xml）

注文確定時、`nextOrderSequence` SQL は以下を実行する。

```sql
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
  SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence
```

`order_number_counters` に当日レコードが存在しない場合、`last_sequence = 1` で INSERT し、
戻り値として `1` を返す。

#### （4）生成される注文番号の衝突

`sequence = 1` を受け取った `OrderService.generateOrderNumber()` は
`ORD20260330-000001` を生成する。

しかしこの order_number はシードデータで既に `orders` テーブルに挿入済みのため（②により確認済み）、
`insertOrder()` の INSERT 時に **UNIQUE 制約違反** が発生し、
例外がそのままスローされてトランザクションがロールバックされる。

Spring の例外ハンドラが未処理例外として受け取り、システムエラー画面（500）へ遷移する。

### エラー発生箇所

```
CartController.placeOrder()                        (CartController.java:372)
  └─ OrderService.placeOrder()                     (OrderService.java:201)
       └─ OrderRepository.insertOrder(params)      (OrderRepository.java:84)
            └─ OrderMapper.insertOrder             ← ここで UNIQUE 制約違反が発生
                 └─ INSERT INTO orders (order_number, ...) VALUES ('ORD20260330-000001', ...)
                    → PSQLException: duplicate key value violates unique constraint "orders_order_number_key"
                       詳細: Key (order_number)=(ORD20260330-000001) already exists.
```

### ログ上の証跡

`log/office-order.log` の 12:58:25（13:00 頃の障害に対応）に以下の ERROR が記録されている。

```
2026-03-30 12:58:25.656 ERROR [d2ae74a3-92b8-41a8-a2c1-c823ee7a5445 1] POST /checkout/confirm
  : j.c.s.o.w.GlobalExceptionLoggingAdvice - event=unhandled_exception path=/checkout/confirm
  message=
### Error querying database.  Cause: org.postgresql.util.PSQLException:
  ERROR: duplicate key value violates unique constraint "orders_order_number_key"
  詳細: Key (order_number)=(ORD20260330-000001) already exists.
```

また、同日 12:11 の別セッションでは `ORD20260330-000001` での注文が正常完了している（`event=order_place_end orderId=101`）。
この成功は初回の Docker 起動時のものであり、以降に Docker ボリュームをリセットして再初期化したことで
シードデータが当日日付で再挿入され、`order_number_counters` も空に戻ったことで再現条件が揃った。

### 再現条件の整理

| 条件 | 状態 |
|------|------|
| DB 初期化当日（2026/03/30）に注文を試みる | シード order_number と衝突 → **再現する** |
| 別の日に初期化し、その後 2026/03/30 に注文 | シード order_number の日付が異なるため衝突しない → **再現しない** |

「他の人に確認したところ、私の前に初期構築を実施した人では再現しなかった」という補足は
上記の条件の差で説明できる。先に構築した人は異なる日付で初期構築を行ったため、
シードによる order_number の日付が 2026/03/30 ではなく、衝突が発生しなかった。

---

## 影響範囲

- DB 初期構築当日に注文操作を試みた全ユーザー（会員・非会員を問わず）
- 影響は注文確定処理全体に及ぶ（アカウント種別・商品内容・数量は無関係）

---

## 参考：関連ファイル

| ファイル | 役割 |
|----------|------|
| `sql/seed/test-data/orders.sql` | `CURRENT_DATE` で order_number を生成するシード SQL |
| `sql/schema/orders.sql` | orders テーブル定義（UNIQUE 制約） |
| `src/main/resources/mappers/OrderMapper.xml` | `nextOrderSequence` / `insertOrder` SQL |
| `src/main/java/.../service/order/OrderService.java` | `generateOrderNumber()` / `placeOrder()` |
| `src/main/java/.../repository/OrderRepository.java` | `nextOrderSequence()` / `insertOrder()` |
| `log/office-order.log` | 障害時のスタックトレース（12:58:25 付近） |

---

## 修正方針

### 方針の概観

根本原因は「シードデータが `orders` テーブルに今日付けの order_number を挿入する一方で、
採番管理テーブル `order_number_counters` を初期化していない」という **シードデータの不整合** である。
修正方針は大きく 3 つ考えられる。

---

### 方針 A：シード SQL に `order_number_counters` の初期化を追記する

`sql/seed/test-data/orders.sql` の末尾に、`orders` テーブルへの INSERT 完了後に
`order_number_counters` へ当日レコードを upsert する SQL を追加する。

```sql
-- order_number_counters をシードで挿入した注文の最大連番で初期化する
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    CURRENT_DATE,
    MAX(CAST(SUBSTRING(order_number FROM '[0-9]+$') AS INTEGER)),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number LIKE 'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-%'
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
```

これにより、シード実行直後の `order_number_counters` は `(order_date=当日, last_sequence=100)` となり、
次に発行される連番は 101 以降になってシードデータとの衝突が解消される。

| 観点 | 内容 |
|------|------|
| **メリット** | 変更ファイルが `sql/seed/test-data/orders.sql` 1 ファイルのみ |
| | アプリケーションコード・スキーマへの変更不要 |
| | 根本原因（counters 未初期化）を直接解消する |
| | `MAX(CAST(...))` による動的カウントにより、シード件数を将来変更しても自動追従する |
| **デメリット** | シードの order_number が実行日に集中するという既存の不自然さは残る |
| | `order_datetime` と order_number の日付部分が一致しないレコードが存在し続ける |

---

### 方針 B：シード SQL の order_number 生成を `order_datetime` ベースに変更する

現在 `CURRENT_DATE` を使っている order_number 生成部分を、各レコードの `order_datetime` の
日付に基づく値に変更し、日付ごとに連番を振り直す。さらに `order_number_counters` を
日付ごとの実績最大連番で初期化する。

```sql
-- (変更前)
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')

-- (変更後：order_datetime の日付＋日付内連番)
'ORD'
    || TO_CHAR(p.order_datetime AT TIME ZONE 'Asia/Tokyo', 'YYYYMMDD')
    || '-'
    || LPAD(
           ROW_NUMBER() OVER (
               PARTITION BY DATE(p.order_datetime AT TIME ZONE 'Asia/Tokyo')
               ORDER BY p.n
           )::TEXT,
           6, '0'
       )
```

```sql
-- order_number_counters を日付ごとの最大連番で初期化
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    DATE(order_datetime AT TIME ZONE 'Asia/Tokyo'),
    MAX(CAST(SUBSTRING(order_number FROM '[0-9]+$') AS INTEGER)),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
GROUP BY DATE(order_datetime AT TIME ZONE 'Asia/Tokyo')
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
```

| 観点 | 内容 |
|------|------|
| **メリット** | order_number の日付部と `order_datetime` の値が意味的に一致する（設計上最も正しい） |
| | シード実行日と order_number 日付が必ず異なるため、`CURRENT_DATE` との衝突が本質的に発生しない |
| | `order_number_counters` も正しく日付ごとに初期化される |
| **デメリット** | シード SQL の変更規模が大きく、ウィンドウ関数と `PARTITION BY` が必要になり複雑化する |
| | `AT TIME ZONE 'Asia/Tokyo'` の扱いと `appClock` の設定差異に注意が必要 |
| | ローカル開発環境用のテストデータとして考えると変更コストが高い |

---

### 方針 C：アプリケーション側でリトライ処理を実装する

`OrderService.placeOrder()` で UNIQUE 制約違反（`PSQLException`）を捕捉し、
`generateOrderNumber()` を再呼び出しして別の連番を取得するリトライループを実装する。

```java
// イメージ（実装例）
for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
    try {
        String orderNumber = generateOrderNumber(orderDatetime);
        long orderId = orderRepository.insertOrder(buildParams(orderNumber, ...));
        ...
        return orderNumber;
    } catch (DataIntegrityViolationException e) {
        if (attempt >= MAX_RETRY) throw e;
        log.warn("order_number conflict, retrying attempt={}", attempt);
    }
}
```

| 観点 | 内容 |
|------|------|
| **メリット** | シードデータの不整合があっても自己回復できる |
| | データ不整合が発生するあらゆる状況（本番移行後も含む）に対して堅牢 |
| **デメリット** | シードデータという根本原因を修正せず、問題を隠蔽する |
| | アプリケーションコードが複雑化する（リトライ上限・ログ管理が必要） |
| | `order_number_counters` の整合性保証はDB層の責務であり、アプリ層での回避は設計上好ましくない |
| | `nextOrderSequence` の UPSERT は atomic であるため、本来リトライは不要 |

---

### 推奨方針：**方針 A**（シード SQL への `order_number_counters` 初期化追記）

**推奨理由：**

1. **根本原因への直接対処**  
   障害の直接原因は「`order_number_counters` がシードで初期化されていないこと」であり、
   方針 A はその責任範囲（シード SQL）で修正を完結させる。

2. **変更範囲が最小**  
   `sql/seed/test-data/orders.sql` への追記のみで済み、アプリケーションコード・スキーマへの
   変更がゼロ。デグレードリスクが最も低い。

3. **本番コードの健全性を維持**  
   方針 C のようにアプリ側にリトライを追加すると、正常に機能している採番ロジックを
   不必要に複雑化させる。問題のない箇所は変更しない原則を守る。

4. **方針 B との比較**  
   order_number と order_datetime の日付一致という観点では方針 B が最も意味的に正しいが、
   ローカル開発環境のテストデータとして許容される不整合であり、修正コストに見合わない。
   順序として方針 A で即時修正し、テストデータの品質向上は別 Issue で検討する粒度が妥当。

---

## 修正ステータス

| 項目 | 状態 |
|------|------|
| 原因特定 | ✅ 完了 |
| 修正方針決定 | ✅ 完了（方針 A を採択） |
| 修正実施 | ✅ 完了（`sql/seed/test-data/orders.sql` に `order_number_counters` 初期化 SQL を追記） |
| 単体テスト追加 | ✅ 完了（`OrderServiceTest` 新規作成・8ケース） |
| 動作確認 | 🔲 未着手 |

### 修正内容

`sql/seed/test-data/orders.sql` の末尾に以下を追記した。

```sql
-- BUG-001 fix: order_number_counters を当日シードの最大連番で初期化する。
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    CURRENT_DATE,
    MAX(CAST(SUBSTRING(order_number FROM '[0-9]+$') AS INTEGER)),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number LIKE 'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-%'
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
```

これにより、`orders` テーブルへのシード INSERT が完了した直後に
`order_number_counters` が当日付け・連番 `100` で初期化される。
次回の注文採番は `101` 以降となり、シードデータとの衝突は発生しない。

### 追加した単体テスト

`src/test/java/jp/co/skig/officeorder/service/order/OrderServiceTest.java` を新規作成した。

| テストメソッド | 目的 |
|----------------|------|
| `placeOrder_returnsOrderNumber000101_whenSequenceIsInitializedAfterSeed` | BUG-001 核心：シード後 counters=100 → 採番 101 → `ORD20260330-000101` |
| `placeOrder_returnsOrderNumber000001_whenCountersIsEmpty` | counters 空（新規 DB）で採番 1 → `ORD20260330-000001` |
| `placeOrder_returnsOrderNumber999999_whenSequenceIsAtMax` | 最大値境界 999999 → `ORD20260330-999999`（正常系境界値） |
| `placeOrder_throwsIllegalStateException_whenSequenceExceedsMax` | 採番 1000000 → `IllegalStateException` |
| `placeOrder_throwsIllegalArgumentException_whenCartIsEmpty` | 空カート → `IllegalArgumentException` |
| `placeOrder_throwsIllegalArgumentException_whenCartIsNull` | null カート → `IllegalArgumentException` |
| `placeOrder_throwsIllegalArgumentException_whenQuantityExceedsStock` | 在庫超過数量 → `IllegalArgumentException` |
| `placeOrder_throwsIllegalArgumentException_whenCorporateWithoutCompanyName` | 法人区分で会社名なし → `IllegalArgumentException` |

実行結果：**Tests run: 8, Failures: 0, Errors: 0, Skipped: 0**

### 動作確認手順（未着手）

1. Docker ボリュームをリセットして `docker compose up` で環境を再構築する
2. 会員ログイン後、カートに商品を追加して注文確定を実行する
3. 注文完了画面に遷移し、`orders` テーブルに `order_number` が `ORD20260330-000101` 以降で挿入されることを確認する
4. `order_number_counters` テーブルに `(order_date=当日, last_sequence=101)` のレコードが存在することを確認する
