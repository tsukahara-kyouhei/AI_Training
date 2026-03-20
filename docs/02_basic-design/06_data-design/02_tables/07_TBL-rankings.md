# ランキングテーブル定義

## 1. popular_product_rankings（売れ筋ランキング）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | ranking_date | DATE | NOT NULL | PK（複合） | ランキング日 |
| 2 | rank | INTEGER | NOT NULL | PK（複合）, CHECK: 1〜10 | 順位 |
| 3 | product_id | BIGINT | NOT NULL | FK → products.product_id | 商品ID |
| 4 | sales_count | INTEGER | NOT NULL | CHECK >= 0 | 販売数 |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**更新タイミング:** バッチ処理（売れ筋ランキング計算ジョブ）により毎時0分に更新  
**集計範囲:** 過去1か月の販売実績

---

## 2. recommended_related_products（おすすめ関連商品）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_id | BIGINT | NOT NULL | PK（複合）, FK → products.product_id | 商品ID |
| 2 | related_product_id | BIGINT | NOT NULL | PK（複合）, FK → products.product_id | 関連商品ID |
| 3 | similarity_score | NUMERIC | NOT NULL | - | 類似度スコア |
| 4 | rank | INTEGER | NOT NULL | CHECK: 1〜4 | 表示順位 |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**更新タイミング:** バッチ処理（おすすめ関連商品計算ジョブ）により毎時0分に更新  
**表示件数:** 商品あたり上位4件
