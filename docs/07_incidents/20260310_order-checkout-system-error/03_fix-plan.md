# 注文確定時システムエラー - 修正計画

> **ステータス**: ✅ 修正完了
> **前提**: 原因調査（02_root-cause-analysis.md）で根本原因が確定済みであること

---

## 1. 修正方針

`sql/seed/test-data/orders.sql`（テストデータ投入SQL）が `order_number_counters` テーブルを更新していなかったことが根本原因のため、同ファイルの末尾に `order_number_counters` への INSERT 文を追加する。

- テストデータ100件分の注文番号採番済みとして `last_sequence = 100` を登録する
- `ON CONFLICT ... DO UPDATE SET last_sequence = GREATEST(...)` により、すでにカウンターが存在する場合（アプリ経由で注文済みの場合）は既存値を保護する
- Controller・Service・Mapper・テンプレート等のアプリコードには変更不要

---

## 2. 修正対象モジュール一覧

### 2.1 Controller層

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

### 2.2 Service層

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

### 2.3 Repository / Mapper層

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

### 2.4 Model / DTO

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

### 2.5 テンプレート（Thymeleaf）

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

### 2.6 SQL（テストデータ投入スクリプト）

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| M-001 | `sql/seed/test-data/orders.sql` | 修正 | ファイル末尾に `order_number_counters` への INSERT 文を追加。シード実行当日付けのカウンターを 100 に設定する |

**追加した SQL（`sql/seed/test-data/orders.sql` 末尾）：**

```sql
-- 注文番号採番カウンターを更新する（テストデータ100件分）
-- シード実行当日に注文確定すると order_number が衝突する問題を防ぐ
INSERT INTO order_number_counters (order_date, last_sequence)
VALUES (CURRENT_DATE, 100)
ON CONFLICT (order_date) DO UPDATE SET last_sequence = GREATEST(order_number_counters.last_sequence, 100);
```

### 2.7 設定ファイル

| # | 対象ファイル | 変更種別 | 変更概要 |
|---|---|---|---|
| - | - | なし | - |

---

## 3. データ修正

障害が発生した環境では `order_number_counters` にシード実行日のエントリが欠落している。
以下の SQL を対象環境で実行して不整合を解消する。

| # | 対象テーブル | 修正内容 | 修正SQL |
|---|---|---|---|
| D-001 | `order_number_counters` | シード実行日付けのカウンターを 100 に設定（または既存値を 100 に切り上げ） | 下記参照 |

```sql
-- シード実行日の orders 件数からカウンターを補正する
-- ※ シード実行日は orders テーブルの order_number プレフィックスから判断する
INSERT INTO order_number_counters (order_date, last_sequence)
SELECT
    TO_DATE(SUBSTRING(MIN(order_number), 4, 8), 'YYYYMMDD'),
    COUNT(*)
FROM orders
WHERE order_number LIKE 'ORD' || TO_CHAR(
    TO_DATE(SUBSTRING((SELECT MIN(order_number) FROM orders), 4, 8), 'YYYYMMDD'),
    'YYYYMMDD'
) || '%'
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence);
```

> **補足**: 次回以降の初期構築では修正済みの `seed/test-data/orders.sql` を実行するため自動的に解消される。既存環境への手動適用が必要な場合は上記SQLを実行すること。

---

## 4. 修正スケジュール

| マイルストーン | 目標日 | 実績日 |
|---|---|---|
| 修正完了 | 2026-04-07 | 2026-04-07 |
| テスト完了 | 2026-04-07 | - |
| リリース（リポジトリへの反映） | 2026-04-07 | - |

---

## 5. リリース手順

アプリコードの変更はないため通常のデプロイは不要。
次回以降のローカル環境構築時に修正済み `seed/test-data/orders.sql` が自動的に適用される。

既存のローカル環境（すでに初期構築済み）への対応が必要な場合は以下を実施する。

1. DockerでPostgreSQLが起動していることを確認する
2. 上記「3. データ修正」の SQL を実行して `order_number_counters` を補正する
3. アプリを再起動する（不要な場合もある）

---

## 6. 変更履歴

| 日付 | 更新内容 | 更新者 |
|---|---|---|
| 2026-04-07 | 初版作成・修正完了 | - |
