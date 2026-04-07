# 注文確定時システムエラー - 原因調査

> **ステータス**: ✅ 根本原因特定
> **前提**: 障害表（01_incident-report.md）が起票済みであること

---

## 1. 調査方針

ログが取得できていないため、本ドキュメントでは以下の方針で調査を進める。

1. **仮説の列挙**：コード・処理フロー・環境の各観点から考えられる原因を洗い出す
2. **必要情報の整理**：各仮説を検証するために収集すべき情報・確認手順を明確化する
3. **仮説の絞り込み**：収集した情報をもとに原因を特定する

**前提として把握している処理フロー**

```
POST /checkout/confirm
  └─ CartController.placeOrder()
       ├─ トークン検証
       ├─ セッションからフォーム取得
       ├─ CartService.getCart()（商品スナップショット取得・送料計算）
       └─ OrderService.placeOrder() [@Transactional]
            ├─ カート空チェック
            ├─ 業務ルール検証（法人の場合は会社名必須など）
            ├─ 在庫再確認（revalidateCartStock）
            ├─ 注文番号採番（order_number_counters テーブル）
            ├─ orders テーブルへ INSERT
            ├─ order_items テーブルへ INSERT × 商品件数分（今回2件）
            ├─ order_status_histories テーブルへ INSERT
            └─ コミット後：メール送信予約（afterCommit フック）
```

---

## 2. 仮説一覧

以下の仮説を優先度順に調査する。

| 優先度 | 仮説ID | 仮説 | 根拠 | 結論 |
|---|---|---|---|---|
| ★★★ | H-01 | DBの初期化が不完全（シードデータ欠損） | 「初期構築直後」「別環境では再現しない」という条件に合致する | ❌ 否定（tax_rates・商品・会員データは正常に存在） |
| ★★★ | H-02 | 税率マスタがDBに存在しない | `findCurrentTaxRatePercent()` でレコード0件の場合エラーになりえる | ❌ 否定（2件存在・現在有効） |
| ★★★ | H-03 | 商品データがDBに存在しない | 商品未登録の場合カート組み立て・在庫確認時にエラーになりえる | ❌ 否定（P0001-C01・P0001-C02 共に存在） |
| ★★ | H-04 | DBスキーマの適用漏れ | スキーマSQLの実行が部分的に失敗している場合INSERT/SELECT時にエラーになる | ❌ 否定（全31テーブル存在確認済み） |
| ★★ | H-05 | 環境設定ファイルの差異 | 別環境で再現しないことから設定値の違いが原因の可能性がある | ❌ 否定（以下の根本原因で説明可能） |
| ★ | H-06 | メール設定の不備 | afterCommit フックの実行のためシステムエラー画面には繋がりにくい | ❌ 否定 |
| **★★★** | **H-07** | **テストデータの注文番号と `order_number_counters` 未更新による UNIQUE 制約違反** | `seed/test-data/orders.sql` が `CURRENT_DATE` を注文番号に使用するが `order_number_counters` を更新しないため、同日に注文すると番号が衝突する | **✅ 根本原因として特定** |

---

## 3. 必要情報と収集手順

### H-01 / H-03：DB初期化・データの確認

**目的**：シードデータ（マスタデータ・商品データ）が正しくDBに投入されているかを確認する

#### 3.1.1 初期化手順の実施確認

下記の初期化ステップがすべて実施されたかを確認する。

| # | 確認内容 | 確認方法 | 結果 |
|---|---|---|---|
| 1 | `sql/schema/` 配下の全スキーマSQLが実行済みか | pgAdmin または psql でテーブル一覧を確認 | ⏳ 未確認 |
| 2 | `sql/init/init.sql` が実行済みか | 実施記録・作業ログを確認 | ⏳ 未確認 |
| 3 | `sql/seed/` 配下の全シードSQLが実行済みか | 実施記録・作業ログを確認 | ⏳ 未確認 |
| 4 | `scripts/init-local-postgres.ps1` を使用した場合は、スクリプトがエラーなく完了したか | スクリプトの実行ログを確認 | ⏳ 未確認 |

#### 3.1.2 データ確認SQL

以下のSQLをDBで実行して結果を記録する。

```sql
-- 税率マスタの確認（H-02）
-- 現在有効な税率レコードが1件以上あることが正常
-- ※実際のカラム名は effective_start_at（effective_from は誤り）
SELECT * FROM tax_rates WHERE effective_start_at <= NOW() ORDER BY effective_start_at DESC LIMIT 5;

-- 商品データの確認（H-03）
-- P0001-C01, P0001-C02 のレコードが存在することが正常
SELECT product_code, stock_quantity, unit_price
FROM product_variants
WHERE product_code IN ('P0001-C01', 'P0001-C02');

-- 会員データの確認
-- member01@example.com が存在することが正常
SELECT member_id, email, member_type FROM members WHERE email = 'member01@example.com';

-- シードデータ全般の投入件数確認
SELECT 'product_variants' AS tbl, COUNT(*) FROM product_variants
UNION ALL SELECT 'members', COUNT(*) FROM members
UNION ALL SELECT 'tax_rates', COUNT(*) FROM tax_rates;
```

| 確認クエリ | 期待結果 | 実際の結果 | 判定 |
|---|---|---|---|
| tax_rates 有効レコード | 1件以上 | （次ステップで確認） | ⏳ |
| P0001-C01 存在確認 | 1件 | | ⏳ |
| P0001-C02 存在確認 | 1件 | | ⏳ |
| member01@example.com 存在確認 | 1件 | | ⏳ |

---

### H-04：DBスキーマの確認

**目的**：テーブル・カラムが正しく作成されているかを確認する

```sql
-- orders テーブルの存在・カラム確認
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'orders' ORDER BY ordinal_position;

-- order_items テーブルの存在・カラム確認
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'order_items' ORDER BY ordinal_position;

-- order_number_counters テーブルの存在確認
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'order_number_counters' ORDER BY ordinal_position;

-- order_status_histories テーブルの存在確認
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'order_status_histories' ORDER BY ordinal_position;
```

| 確認テーブル | 期待結果 | 実際の結果 | 判定 |
|---|---|---|---|
| orders | カラム一覧が返る | | ⏳ |
| order_items | カラム一覧が返る | | ⏳ |
| order_number_counters | カラム一覧が返る | | ⏳ |
| order_status_histories | カラム一覧が返る | | ⏳ |

---

### H-05：環境設定の確認

**目的**：別の担当者の環境と設定差異がないかを確認する

| # | 確認内容 | 確認方法 | 結果 |
|---|---|---|---|
| 1 | `src/main/resources/application.yml` のDB接続先・プロファイルが正しいか | ファイルを直接確認 | ⏳ 未確認 |
| 2 | ローカル用の設定ファイル（`application-local.yml` 等）が存在し、正しく読み込まれているか | ファイルを直接確認 | ⏳ 未確認 |
| 3 | 起動時のSpring Profileが想定通りか | アプリ起動時のコンソールログを確認 | ⏳ 未確認 |
| 4 | Dockerを使用している場合、`docker-compose.yml` の設定が正しいか | ファイルを直接確認 | ⏳ 未確認 |

---

### ログ取得手順（最優先）

上記確認と並行して、以下のいずれかの方法でエラーログの取得を試みる。

#### 方法1：アプリケーションログファイルを確認する

```
log/ ディレクトリ配下のログファイルを確認する
（ファイルが存在する場合は、システムエラー発生時刻前後の行を確認）
```

#### 方法2：コンソール（標準出力）で再現させてログを取得する

1. アプリケーションを起動する
2. 障害表の再現手順を実施する
3. **「注文を確定する」ボタン押下直後のコンソール出力**をすべてコピーして保存する
4. スタックトレースの先頭行（`Caused by:` 以降）を確認する

#### 方法3：H2/DBのエラーログを確認する

```
PostgreSQL の場合: pg_log/ 配下のログファイルを確認する
```

---

## 4. 調査ログ

### 4.1 DB・データ確認

**2026-04-07 実施（dockerコンテナ内 psql で確認）**

| 確認クエリ | 期待結果 | 実際の結果 | 判定 |
|---|---|---|---|
| tax_rates 有効レコード | 1件以上 | 2件（10%・8%） | ✅ |
| P0001-C01 存在確認 | 1件 | stock=12, price=49800 | ✅ |
| P0001-C02 存在確認 | 1件 | stock=5, price=49800 | ✅ |
| member01@example.com 存在確認 | 1件 | member_id=1 | ✅ |
| 全テーブル存在確認 | 31件 | 31件 | ✅ |
| order_number_counters の内容 | - | 2026-04-07のみ（last_sequence=4） | ⚠️ シード実行日（3/11）のエントリが存在しない |
| orders の日付プレフィックス分布 | - | ORD20260311: 100件（テストデータ）, ORD20260407: 4件（アプリ経由） | ⚠️ テストデータが3/11付けで100件生成されている |

**決定的証拠**：`order_number_counters` にシード実行日（2026-03-11）のエントリが存在しない

```
order_date | last_sequence
-----------+--------------
2026-04-07 |            4    ← アプリからの注文のみ（テストデータ分は採番されていない）
```

現在の環境（シード実行日=2026-03-11）で 2026-03-11 に注文していたら同じエラーが発生していた。

### 4.2 ログ確認

- アーカイブログ（`office-order.2026-03-11.0.log.gz`）を展開・確認
- 2026-03-11 のログには起動・バッチ処理のみ記録。注文確定操作のログなし
- 2026-03-10 付けのアーカイブは存在しないため、障害発生時のスタックトレースは取得不可
- 本日（2026-04-07）のログでは `order_place_start` → `order_place_end` が正常に記録されており、現在の環境は正常動作中

### 4.3 調査タイムライン

| 日時 | 調査内容 | 結果・気づき |
|---|---|---|
| 2026-04-07 | 原因調査ドキュメント作成・調査方針確定 | - |
| 2026-04-07 | tax_rates テーブルのSQL確認（effective_from で実行） | `column "effective_from" does not exist` → テーブルは存在。正しいカラム名は `effective_start_at` |
| 2026-04-07 | シードデータ・スキーマ全体確認（psql にて） | tax_rates/商品/会員データはすべて正常。全テーブル存在。H-01〜H-04 否定 |
| 2026-04-07 | アーカイブログ確認（2026-03-11） | 障害時ログなし。現環境は正常動作を確認 |
| 2026-04-07 | `seed/orders.sql` → `\ir test-data/orders.sql` の参照を発見 | test-data/orders.sql が CURRENT_DATE で注文番号を生成するが order_number_counters を更新しない実装を確認 |
| 2026-04-07 | orders テーブルの日付プレフィックス分布確認 | ORD20260311 が100件（シード時の CURRENT_DATE=3/11）、order_number_counters に 3/11 エントリなし → 根本原因確定 |

---

## 5. 根本原因

> **特定日時**: 2026-04-07

`sql/seed/test-data/orders.sql`（テストデータ投入SQL）が `TO_CHAR(CURRENT_DATE, 'YYYYMMDD')` を使って注文番号（`ORD{シード実行日}-000001` ～ `ORD{シード実行日}-000100`）を生成するが、注文番号採番カウンターテーブル（`order_number_counters`）を更新しない実装になっていた。

そのため、ローカル環境の初期構築（シードデータ投入）を行った当日に注文確定操作を行うと、`OrderService.nextOrderSequence()` がカウンターを 1 から採番し `ORD{当日}-000001` を生成し、`orders.order_number` の UNIQUE 制約に違反する例外が発生してシステムエラー画面に遷移する。

**再現条件**: ローカル環境の初期構築（シードデータ投入）を行った当日に注文確定操作を実行する

**別環境で再現しなかった理由**: 先に初期構築を行った担当者のシード実行日が 2026-03-10 以前であったため、2026-03-10 時点では日付プレフィックスが一致せず、注文番号の衝突が発生しなかった

---

## 6. 原因の分類

| 項目 | 内容 |
|---|---|
| 原因種別 | 実装バグ |
| 混入工程 | 実装（テストデータSQL作成時） |
| 混入原因 | テストデータ投入SQL（`seed/test-data/orders.sql`）が `order_number_counters` テーブルを更新せずに注文データを直接挿入したため、採番カウンターとの不整合が生じた |

---

## 7. 再発防止策

| # | 対策内容 | 種別 | 担当 | 期限 |
|---|---|---|---|---|
| 1 | `seed/test-data/orders.sql` の末尾に `order_number_counters` への INSERT を追加し、シード実行日のカウンターを 100 に設定する | 恒久対応（バグ修正） | - | - |
| 2 | または、テストデータの注文番号を過去固定日付（例: `'20240101'`）で生成するよう変更し、実運用日との衝突を根本的に防ぐ | 恒久対応（バグ修正） | - | - |
| 3 | ローカル環境セットアップ手順書に「シード実行当日は注文確定が失敗する場合がある（修正前の注意事項）」を追記する | プロセス改善 | - | 恒久対応完了まで |

---

## 8. 変更履歴

| 日付 | 更新内容 | 更新者 |
|---|---|---|
| 2026-04-07 | 初版作成（仮説整理・必要情報収集手順まとめ） | - |
| 2026-04-07 | DB調査・ログ確認・コード調査により根本原因特定 | - |
| 2026-04-07 | 修正完了（`sql/seed/test-data/orders.sql` 末尾に `order_number_counters` INSERT追加） | - |
