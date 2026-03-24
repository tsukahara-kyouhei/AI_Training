# FEAT-001 DDL設計書（インデックス追加）

## 1. 変更概要

`products.description` はテキスト型（長文）カラムであり、`ILIKE` による中間一致検索に B-tree インデックスが適用されない。  
性能目標（95パーセンタイル 2 秒以内）を達成するため、`pg_trgm` 拡張を使用した GIN インデックスを追加する。

---

## 2. 変更対象

| 対象 | 変更種別 | 内容 |
|-----|---------|-----|
| `sql/schema/` 配下マイグレーションスクリプト（新規） | DDL 追加 | `pg_trgm` 拡張の有効化と GIN インデックスの作成 |

新規ファイル: `sql/schema/products_search_idx.sql`

---

## 3. インデックス設計

### 3.1 追加インデックス一覧

| 対象カラム | インデックス名 | 種別 | 優先度 | 目的 |
|-----------|-------------|-----|-------|------|
| `products.description` | `idx_products_description_trgm` | GIN（`pg_trgm`） | **必須** | 長文テキストの `ILIKE` 中間一致高速化 |
| `products.product_name` | `idx_products_product_name_trgm` | GIN（`pg_trgm`） | 推奨 | 既存 `ILIKE` 検索の高速化 |
| `products.variation_name` | `idx_products_variation_name_trgm` | GIN（`pg_trgm`） | 推奨 | 新規追加フィールドの `ILIKE` 検索高速化 |

> **`products.description` が必須な理由**: `text` 型の長文カラムに対する `ILIKE '%keyword%'` は B-tree インデックスが利用できないため、商品数増加に従って全テーブルスキャンが発生する。

> **`product_name` / `variation_name` の位置付け**: `VARCHAR` 型かつ現状の上限は 255 / 120 文字と比較的短い。商品数 1,000 件規模ではインデックスなしでも速度目標を満たせる可能性があるが、将来の拡張性および一貫した検索方式のため推奨とする。

### 3.2 `product_variants.product_code` のインデックス

商品コードの検索は `= #{keywordExact}`（完全一致）または `ILIKE #{keywordPrefix}`（前方一致）に変更される。  
`product_code` には既に `UNIQUE` 制約が付与されており、PostgreSQL が自動的に B-tree インデックスを作成する。  
完全一致（`=`）および前方一致（`ILIKE 'prefix%'`）はいずれも B-tree インデックスが有効なため、**追加インデックスは不要**。

---

## 4. DDL

### 4.1 `pg_trgm` 拡張の有効化

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

> `pg_trgm` は PostgreSQL 標準の contrib モジュールであり、追加パッケージのインストールは不要（`contrib` が同梱済みの場合）。

### 4.2 GIN インデックスの作成

```sql
-- description: 必須（長文テキスト、B-tree 非対応のため GIN が必要）
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING gin (description gin_trgm_ops);

-- product_name: 推奨（ILIKE 検索の高速化）
CREATE INDEX IF NOT EXISTS idx_products_product_name_trgm
    ON products USING gin (product_name gin_trgm_ops);

-- variation_name: 推奨（ILIKE 検索の高速化、NULL 許容カラム）
CREATE INDEX IF NOT EXISTS idx_products_variation_name_trgm
    ON products USING gin (variation_name gin_trgm_ops);
```

### 4.3 マイグレーションスクリプト全文

ファイルパス: `sql/schema/products_search_idx.sql`

```sql
-- FEAT-001: キーワード検索性能改善のための GIN インデックス追加
-- pg_trgm 拡張を使用した ILIKE 中間一致検索の高速化

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- description（必須: 長文テキスト、B-tree では ILIKE 非対応）
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING gin (description gin_trgm_ops);

-- product_name（推奨: 既存 ILIKE 検索の高速化）
CREATE INDEX IF NOT EXISTS idx_products_product_name_trgm
    ON products USING gin (product_name gin_trgm_ops);

-- variation_name（推奨: 新規追加検索フィールドの高速化）
CREATE INDEX IF NOT EXISTS idx_products_variation_name_trgm
    ON products USING gin (variation_name gin_trgm_ops);
```

---

## 5. 適用手順

### 5.1 ローカル環境

`scripts/init-local-postgres.ps1` に以下の実行を追加する、  
または Docker コンテナ起動後に手動で実行する:

```powershell
psql -h localhost -U <user> -d <dbname> -f sql/schema/products_search_idx.sql
```

### 5.2 本番環境

DDL は `CREATE EXTENSION IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS` を使用しているためべき等（再実行安全）である。  
メンテナンスウィンドウ内でスクリプトを適用する。

> `CREATE INDEX` は対象テーブルに ShareLock を取得する。商品数 1,000 件規模であれば作成時間は数秒以内の見込みだが、  
> 本番環境では `CREATE INDEX CONCURRENTLY` の使用を検討すること（`IF NOT EXISTS` との組み合わせは PostgreSQL 12 以降でサポート）。

---

## 6. 影響範囲

| 対象 | 内容 |
|-----|-----|
| `sql/init/init.sql` | `\i sql/schema/products_search_idx.sql` の追加が必要かどうかを確認する（Docker 初期化フローに組み込む場合）|
| アプリケーション | 変更なし。インデックスは PostgreSQL が透過的に使用する |
| 既存クエリ | 変更なし。`ILIKE` クエリは自動的に GIN インデックスを使用するようになる |
