# FEAT-003: DB 設計書

## 1. 新規テーブル

### 1.1 reviews テーブル

```sql
CREATE TABLE reviews (
    review_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    BIGINT       NOT NULL REFERENCES members(member_id),
    product_id   BIGINT       NOT NULL REFERENCES products(product_id),
    rating       SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title        VARCHAR(100),
    body         VARCHAR(1000) NOT NULL,
    is_published BOOLEAN      NOT NULL DEFAULT TRUE,
    is_blocked   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (member_id, product_id)
);

CREATE INDEX idx_reviews_product_created
    ON reviews (product_id, created_at DESC);
```

### 1.2 カラム説明

| カラム | 説明 |
|--------|------|
| review_id | サロゲートキー（GENERATED ALWAYS AS IDENTITY） |
| member_id | 投稿会員。members テーブルへの FK |
| product_id | 対象商品。products テーブルへの FK |
| rating | 1〜5 の評価値。CHECK 制約で担保 |
| title | 任意入力のタイトル |
| body | 必須入力の本文 |
| is_published | 公開状態。投稿者または管理者が制御 |
| is_blocked | 管理者による更新ブロック。true の場合、投稿者による更新・削除を不可とする |
| created_at | 初回投稿日時 |
| updated_at | 最終更新日時。更新時に now() をセット |

### 1.3 制約

| 制約 | 対象 | 目的 |
|------|------|------|
| PK | `review_id` | 行の一意識別 |
| FK | `member_id` → `members(member_id)` | 参照整合性 |
| FK | `product_id` → `products(product_id)` | 参照整合性 |
| UNIQUE | `(member_id, product_id)` | 1 会員 1 商品 1 レビュー |
| CHECK | `rating BETWEEN 1 AND 5` | 評価値の範囲制約 |

### 1.4 インデックス

| 名前 | カラム | 用途 |
|------|--------|------|
| `idx_reviews_product_created` | `(product_id, created_at DESC)` | 商品別レビュー一覧の最新順取得 |
| UNIQUE index (自動生成) | `(member_id, product_id)` | 重複投稿防止 + 会員の投稿済みチェック |

## 2. 購入履歴判定クエリ

会員が商品を購入済みかどうかを判定する SQL パターン。

```sql
SELECT EXISTS (
    SELECT 1
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    JOIN products p ON p.product_code = oi.product_code
    WHERE o.member_id = #{memberId}
      AND o.order_status != 'cancelled'
      AND p.product_id = #{productId}
)
```

## 3. 集計クエリ

### 3.1 商品別の平均評価・件数

```sql
SELECT product_id,
       COUNT(*)        AS review_count,
       AVG(rating)     AS average_rating
FROM reviews
WHERE is_published = TRUE
GROUP BY product_id
```

> 商品一覧での利用時はサブクエリまたは LEFT JOIN で結合する。パフォーマンス劣化が見られる場合は products テーブルへのサマリカラム追加を検討する。

### 3.2 商品別レビュー一覧（最新順・ページネーション）

```sql
SELECT r.review_id, r.rating, r.title, r.body, r.created_at
FROM reviews r
WHERE r.product_id = #{productId}
  AND r.is_published = TRUE
ORDER BY r.created_at DESC
LIMIT #{size} OFFSET #{offset}
```

## 4. スキーマファイル配置

- `sql/schema/reviews.sql` に DDL を新規作成する。
