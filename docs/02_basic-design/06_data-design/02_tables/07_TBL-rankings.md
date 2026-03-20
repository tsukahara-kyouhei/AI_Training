# ランキングテーブル定義

## 1. popular_product_rankings（売れ筋ランキング）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | ranking_date | DATE | NOT NULL | PK（複合） | ランキング日 |
| 2 | rank | SMALLINT | NOT NULL | PK（複合）, CHECK: 1〜10 | 順位 |
| 3 | product_id | BIGINT | NOT NULL | FK → products.product_id, UNIQUE(ranking_date, product_id) | 商品ID |
| 4 | sold_quantity_1m | INTEGER | NOT NULL | DEFAULT 0, CHECK >= 0 | 直近1か月販売数量 |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**更新タイミング:** バッチ処理（売れ筋ランキング計算ジョブ）により毎時0分に更新  
**集計範囲:** 過去1か月の販売実績

---

## 2. recommended_related_products（おすすめ関連商品）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | recommendation_date | DATE | NOT NULL | PK（複合） | 推薦日 |
| 2 | source_product_id | BIGINT | NOT NULL | PK（複合）, FK → products.product_id | 基準商品ID |
| 3 | rank | SMALLINT | NOT NULL | PK（複合）, CHECK: 1〜4 | 順位 |
| 4 | recommended_product_id | BIGINT | NOT NULL | FK → products.product_id, UNIQUE(recommendation_date, source_product_id, recommended_product_id) | 推薦商品ID |
| 5 | score | NUMERIC(10,6) | NULL | - | 類似度スコア |
| 6 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 7 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**制約:** `source_product_id <> recommended_product_id`（自分自身は推薦しない）  
**更新タイミング:** バッチ処理（おすすめ関連商品計算ジョブ）により毎時0分に更新  
**表示件数:** 商品あたり上位4件
