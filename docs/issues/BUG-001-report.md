# BUG-001 障害レポート：注文確定時システムエラー

## 基本情報

| 項目 | 内容 |
|---|---|
| 障害ID | BUG-001 |
| 発生日時 | 2026/3/10 9:12 |
| レポート作成日 | 2026/4/18 |
| 対応方針決定日 | 2026/4/18 |
| 対応完了日 | 2026/4/18 |
| 対象機能 | 注文確定（`POST /checkout/confirm`） |
| 重大度 | 高（ローカル開発環境の初期構築当日に注文が一切できない） |
| 対応状況 | **対応済み** |
| 採用方針 | 方針 B：シードデータの注文番号日付部を固定の過去日付に変更 |

---

## 概要

注文確定時にシステムエラー画面に遷移する。  
初期構築を実施した当日のみ発生し、翌日以降は再現しない可能性がある（後述）。

---

## 原因の推定

### 推定原因：シードデータの注文番号と `order_number_counters` 未連携による一意制約違反

コード調査の結果、以下のメカニズムで `DataIntegrityViolationException` が発生し、システムエラーになっていると推定される。

#### ① シードデータが「当日日付」の注文番号を 100 件挿入する

`sql/seed/test-data/orders.sql` では以下の形式で注文番号を生成している。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

`generate_series(1, 100)` を使って 100 件挿入するため、初期構築を実施した当日（`CURRENT_DATE`）の注文番号として `ORD20260310-000001` ～ `ORD20260310-000100` が `orders` テーブルに登録される。

#### ② シードデータは `order_number_counters` を更新しない

注文番号の採番は `order_number_counters` テーブルで日付ごとの連番を管理している（`sql/schema/orders.sql`）。  
シードデータ（`sql/seed/test-data/orders.sql`）は `orders` テーブルと `order_items` テーブルには挿入するが、**`order_number_counters` へは一切書き込まない。**

#### ③ 初回注文時に採番連番が 1 から始まり、既存の注文番号と衝突する

`OrderMapper.xml` の `nextOrderSequence` は次の UPSERT で連番を採番する。

```sql
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
  SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence
```

`order_number_counters` に当日行がない場合は `last_sequence = 1` で挿入し、`1` を返す。  
結果として生成される注文番号は `ORD20260310-000001` となるが、これはシードデータで既に登録済みである。

#### ④ `orders.order_number` の UNIQUE 制約違反が発生する

`orders` テーブルには `UNIQUE` 制約が付与されている（`sql/schema/orders.sql`）。

```sql
order_number VARCHAR(20) NOT NULL UNIQUE,
```

重複する注文番号を INSERT しようとして PostgreSQL が一意制約違反を返す。

#### ⑤ 例外がシステムエラーとして露出する

`CartController.placeOrder()` は以下のように `IllegalArgumentException` のみを業務エラーとして処理している。

```java
} catch (IllegalArgumentException ex) {
    log.warn("event={} reason={}", LogEvent.CHECKOUT_ORDER_REJECTED.value(), ex.getMessage());
    redirectAttributes.addFlashAttribute("checkoutError", ex.getMessage());
    return "redirect:/checkout/input";
}
```

一意制約違反は Spring が `DataIntegrityViolationException`（`IllegalArgumentException` ではない）としてラップするため、このキャッチに引っかからない。  
その結果、`GlobalExceptionLoggingAdvice` でログ出力・再スローされ、Spring のデフォルトエラーハンドラがシステムエラー画面に遷移させる。

---

### 「自分の前に初期構築した人では再現しなかった」理由

シードデータの注文番号は実行時の `CURRENT_DATE` を使用する。  
前任者が別日（例：2026/3/9 以前）に初期構築していた場合、シードデータの注文番号は前任者のセットアップ日付（例：`ORD20260309-000001`）になっている。  
報告者が 2026/3/10 に初めて注文しようとした際、採番されるのは `ORD20260310-000001` であり、データベース上には当日日付の注文が存在しないため、前任者の環境では衝突が起きない。

報告者は **初期構築した当日（2026/3/10）に注文操作を行った** ため、自分自身のシードデータと衝突が発生したと考えられる。

---

### 再現条件のまとめ

| 条件 | 内容 |
|---|---|
| 必要条件① | ローカル開発環境を構築した当日に注文操作を行う |
| 必要条件② | `init-local-postgres.ps1`（`-SkipSeed` なし）でシードデータが投入されている |
| 不要条件 | ログインユーザーの種別（会員 / 非会員）は関係しない可能性が高い |
| 不要条件 | カート内商品数は影響しない（1件でも同条件で発生すると推定） |

---

## 原因確定に必要な追加情報

現時点では **状況証拠に基づく推定** であり、以下の情報が取得できれば確定できる。

### 1. アプリケーションログ（最優先）

**取得目的：** 実際に発生した例外クラスと原因を確認する。

**取得手順：**
```
log/ ディレクトリ（またはコンソール出力）から 2026/3/10 9:12 前後のログを確認する。
```

**確認すべき内容：**
- `event=UNHANDLED_EXCEPTION` のログ行と付随するスタックトレース
- スタックトレースに以下のいずれかが含まれているか確認する
  - `duplicate key value violates unique constraint "orders_order_number_key"`
  - `DataIntegrityViolationException`
  - `PSQLException`

**ログ出力形式（`GlobalExceptionLoggingAdvice` より）：**
```
event=UNHANDLED_EXCEPTION path=/checkout/confirm message=<例外メッセージ>
```

### 2. データベースの状態確認

**取得目的：** シードデータが当日日付の注文番号を持っているか、カウンタが未初期化かを確認する。

**取得手順：**  
報告者のローカル環境にて下記クエリを実行する（`psql` またはDBクライアントで接続）。

```sql
-- ① 当日（2026/3/10）の注文番号が存在するか確認
SELECT order_number, order_datetime, created_at
FROM orders
WHERE order_number LIKE 'ORD20260310-%'
ORDER BY order_number
LIMIT 10;

-- ② order_number_counters に当日行があるか確認
SELECT * FROM order_number_counters WHERE order_date = '2026-03-10';

-- ③ order_number_counters の全件確認（空かどうか）
SELECT * FROM order_number_counters ORDER BY order_date DESC;
```

**期待される結果（推定が正しければ）：**
- ①：`ORD20260310-000001` ～ `ORD20260310-000100` が存在する
- ②：行が存在しない（`0 rows`）、またはシステムエラー後の初回試行で `last_sequence = 1` の行がある
- ③：行が存在しない（`0 rows`）

### 3. セットアップ日の確認

**取得目的：** 「初期構築した当日に発生した」という仮定を裏付ける。

**取得手順：**  
以下のいずれかの方法で確認する。

```sql
-- シードデータ挿入時刻（members テーブルの作成日時を代用）
SELECT MIN(created_at) AS seed_inserted_at FROM orders;
```

上記が `2026/3/10` であれば、セットアップ日と一致する。

---

## フロー図（障害発生のシーケンス）

```
初期構築（2026/3/10）
  └─ orders シードデータ挿入
       └─ ORD20260310-000001 ～ ORD20260310-000100 が orders テーブルに登録
          ※ order_number_counters には何も登録されない

注文確定（2026/3/10 9:12）
  └─ POST /checkout/confirm
       └─ OrderService.placeOrder()
            └─ OrderRepository.nextOrderSequence(2026-03-10)
                 └─ order_number_counters に行なし
                      → INSERT last_sequence=1, RETURNING 1
            └─ generateOrderNumber() → "ORD20260310-000001"
            └─ OrderMapper.insertOrder()
                 └─ INSERT INTO orders (order_number='ORD20260310-000001', ...)
                      → UNIQUE 制約違反 (order_number_key)
                      → DataIntegrityViolationException スロー
       └─ CartController.placeOrder()
            └─ catch(IllegalArgumentException) → 該当しない
            └─ GlobalExceptionLoggingAdvice で再スロー
  └─ システムエラー画面へ遷移
```

---

## 関連ファイル

| ファイル | 関連箇所 |
|---|---|
| `sql/seed/test-data/orders.sql` | CURRENT_DATE を使った注文番号生成・order_number_counters 未更新 |
| `sql/schema/orders.sql` | `orders.order_number` の UNIQUE 制約、`order_number_counters` テーブル定義 |
| `src/main/resources/mappers/OrderMapper.xml` | `nextOrderSequence` の UPSERT ロジック |
| `src/main/java/.../service/order/OrderService.java` | `generateOrderNumber()`、`placeOrder()` |
| `src/main/java/.../web/CartController.java` | `placeOrder()` の例外ハンドリング（`IllegalArgumentException` のみキャッチ） |
| `src/main/java/.../web/GlobalExceptionLoggingAdvice.java` | 未処理例外のログ出力・再スロー |

---

## 修正方針

### 方針 A：シードデータに `order_number_counters` の初期化処理を追加する

**概要**  
`sql/seed/test-data/orders.sql` の末尾に、挿入した注文番号の最大連番を `order_number_counters` へ同期する SQL を追加する。

**変更対象ファイル**  
- `sql/seed/test-data/orders.sql`（末尾に追記）

**変更内容イメージ**

```sql
-- order_number_counters をシードデータに合わせて初期化する
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    CURRENT_DATE,
    COUNT(*),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number LIKE 'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-%'
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = EXCLUDED.last_sequence,
        updated_at    = CURRENT_TIMESTAMP;
```

**メリット**
- 変更箇所が最小限（1ファイル・数行の追記）
- 注文番号の日付部が初期化実施日を表したままなので、既存の手順書やREADMEへの影響がない
- カウンタの同期という**直接的な原因**を解消する

**デメリット**
- `generate_series(1, 100)` の件数を将来変更した場合（例：100 → 200 件）、この追記 SQL も追随して変更する必要があり、メンテナンス漏れが起きやすい
- シードデータの `order_datetime` は過去 30 日分に分散しているにもかかわらず、注文番号の日付部は全件「初期化実施日」のままという**概念的な不整合が残る**（注文番号の日付と実際の注文日時が一致しない）

---

### 方針 B：シードデータの注文番号日付部を固定の過去日付に変更する（**採用**）

**概要**  
`sql/seed/test-data/orders.sql` の注文番号生成部分で使用している `CURRENT_DATE` を、将来の `CURRENT_DATE` と絶対に一致しない固定の過去日付（`'20000101'`）へ変更する。

**変更対象ファイル**  
- `sql/seed/test-data/orders.sql`（1 行の変更）

**変更内容イメージ**

```sql
-- 変更前
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),

-- 変更後
'ORD20000101-' || LPAD(p.n::TEXT, 6, '0'),
```

> `'ORD20000101-000001'` は 18 文字であり、`orders.order_number VARCHAR(20)` の制約を満たす。

**メリット**
- **衝突リスクを根本的に排除する**。シードデータ初期化のタイミングを問わず、採番される実際の注文番号と衝突しない
- `order_number_counters` の初期化は不要であり、カウンタテーブルは実際の注文処理でのみ書き込まれるという責任分担が明確になる
- 将来的に件数を変更しても（例：100 → 200 件）この修正は一切影響しない
- 注文番号の日付部が `20000101` であることが「テスト用シードデータ」であることを一見して識別できる

**デメリット**
- シードデータの注文番号日付部（`20000101`）が `order_datetime`（直近 30 日分）と一致しない。方針 A と同様に概念的な不整合は存在するが、固定のセンチネル値を使うことで「テスト用データである」という意図をより明示的に表せる
- 注文番号フォーマットの知識なしに初めてデータを見た開発者が `ORD20000101-000001` を不思議に思う可能性がある（README や ONBOARDING.md などへのひと言追記で解消可能）

---

### 採用方針：**方針 B**

#### 採用理由

1. **根本原因を解消する**  
   方針 A は「カウンタが未初期化」という症状に対処するが、注文番号が `CURRENT_DATE` を使うという根本原因は残る。  
   方針 B は CURRENT_DATE 依存を取り除くことで、初期化当日に限らず**いかなるタイミングでも衝突しない**構造にする。

2. **メンテナンス負荷がゼロ**  
   方針 A は `generate_series` の件数変更に追随して `order_number_counters` 初期化 SQL を更新する必要がある。  
   方針 B は 1 行の変更であり、シードデータの規模や内容が変わっても追加対応が不要。

3. **変更範囲が最小**  
   変更は `sql/seed/test-data/orders.sql` の 1 行のみ。アプリケーションコード・スキーマ・他のシードファイル・テストコードへの影響は一切ない。

4. **テストデータとしての識別性が向上する**  
   固定日付 `20000101` はセンチネル値として明確であり、本番データとシードデータを混同するリスクを下げる。

#### 対応時の注意点

- `docs/ONBOARDING.md` または `README.md` にシードデータの注文番号フォーマットについてひと言コメントを追加することで、初見の開発者の混乱を防ぐことが望ましい。
- 本修正はシードデータのみの変更であり、アプリケーション本体・スキーマ・自動テストへの変更は不要。

---

## 修正内容（方針 B）

### 変更ファイル

| ファイル | 変更種別 | 内容 |
|---|---|---|
| `sql/seed/test-data/orders.sql` | 修正 | 注文番号の日付部を `CURRENT_DATE` から固定値 `20000101` へ変更 |
| `src/test/java/.../service/order/OrderServiceTest.java` | テスト追加 | 注文番号フォーマット検証テスト・連番上限超過テストを追加 |

### 変更箇所

注文番号を生成している `SELECT` リスト内の 1 行を変更する。

```diff
--- a/sql/seed/test-data/orders.sql
+++ b/sql/seed/test-data/orders.sql
@@ SELECT
-    'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
+    'ORD20000101-' || LPAD(p.n::TEXT, 6, '0'),
```

### 変更後のコード（抜粋）

```sql
SELECT
    'ORD20000101-' || LPAD(p.n::TEXT, 6, '0'),
    p.order_datetime,
    p.final_status,
    ...
FROM prepared p;
```

### 変更しないファイル

以下のファイルへの変更は不要。

| ファイル | 変更不要の理由 |
|---|---|
| `sql/schema/orders.sql` | スキーマ定義・制約は変更なし |
| `src/main/resources/mappers/OrderMapper.xml` | 採番ロジックは正しい。シードデータと衝突しなければ問題なし |
| `src/main/java/.../service/order/OrderService.java` | アプリケーションロジックは変更なし |
| `src/main/java/.../web/CartController.java` | 例外ハンドリングは変更なし |

### 追加した単体テスト

`OrderServiceTest` に以下 2 件を追加した。

| テスト名 | 目的 |
|---|---|
| `注文番号がクロック日付と6桁連番で生成されること` | `generateOrderNumber()` がアプリ Clock の日付と採番連番を使い `ORD{YYYYMMDD}-{000001}` 形式で注文番号を生成することを回帰テストとして保証する（BUG-001 の根本原因に対する回帰防止） |
| `採番連番が上限を超えるとIllegalStateExceptionがスローされること` | 連番 1,000,000 超過時に `IllegalStateException` がスローされることを検証する |

### 動作確認手順

修正後、以下の手順で再現しないことを確認する。

1. `init-local-postgres.ps1` を再実行してデータを初期化する
2. 会員ユーザー（`member01@example.com`）でログインする
3. カートに商品を 2 件（`P0001-C01`、`P0001-C02`）追加する
4. 注文確認画面まで遷移して「注文を確定する」ボタンを押下する
5. 注文完了画面に遷移することを確認する（システムエラー画面に遷移しないこと）

### 修正の副作用確認

以下のクエリでシードデータの注文番号が固定日付になっていることを確認する。

```sql
-- 固定日付の注文番号のみ存在し、CURRENT_DATE 形式の注文番号が存在しないことを確認
SELECT order_number FROM orders WHERE order_number LIKE 'ORD20000101-%' LIMIT 5;
SELECT order_number FROM orders WHERE order_number NOT LIKE 'ORD20000101-%' LIMIT 5;
-- 2件目は 0 rows になること
```

---

## 再発防止策として更新したドキュメント

本障害の対応完了（2026/4/18）に合わせて、将来の同種障害を防ぐため以下のドキュメントを更新した。

| ファイル | 更新種別 | 更新内容 |
|---|---|---|
| `docs/issues/FEAT-001-troubleList.md` | 障害事例追記 | 本障害（BUG-002 として登録）の概要・原因・対応方法・再発防止策を追記。シードデータで `CURRENT_DATE` を使った日付依存の一意キーを生成してはならないことを明記した。 |
| `docs/issues/FEAT-001-TODO.md` | 注意事項追記 | 末尾に「シードデータ追加・変更時の注意事項」セクションを新設。注文番号の日付部には固定値 `20000101` を使用する規則と、`CURRENT_DATE` 使用禁止の理由をコード例付きで明記した。 |
