## 1. 機能概要

- トップページで新着商品や売れ筋商品を紹介し、利用者が商品を見つける入口を提供する。
- 公開中のお知らせを一覧で表示し、利用者が最新の案内を確認できるようにする。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP GET リクエスト `/` | トップページを要求する。URL パラメータやフォーム入力は受け取らない。 |
| 入力 | HTTP GET リクエスト `/announcements` | お知らせ一覧ページを要求する。URL パラメータやフォーム入力は受け取らない。 |
| 出力 | 遷移先 `pages/top` | トップページ。 |
| 出力 | 画面モデル `topNewArrivals` | トップページに表示する新着商品一覧（最大 4 件）。 |
| 出力 | 画面モデル `hasTopNewArrivals` | 新着商品が存在するかを示す真偽値。 |
| 出力 | 画面モデル `topRankedProducts` | トップページに表示する売れ筋ランキング商品一覧（最大 8 件）。 |
| 出力 | 遷移先 `pages/announcements` | お知らせ一覧ページ。 |
| 出力 | 画面モデル `announcements` | 公開中のお知らせ一覧。 |

## 3. 処理フロー

### `top`（GET `/`）

1. `ProductService` を呼び出し、トップページ用の新着商品一覧を取得する。
2. 新着商品一覧を画面モデル `topNewArrivals` に設定する。
3. 新着商品一覧が空でないかを判定し、結果を画面モデル `hasTopNewArrivals` に設定する。
4. `ProductService` を呼び出し、トップページ用の売れ筋商品一覧を取得して画面モデル `topRankedProducts` に設定する。
5. トップページ `pages/top` を返す。

### `announcements`（GET `/announcements`）

1. `AnnouncementService` を呼び出し、お知らせ一覧を取得する。
2. お知らせ一覧を画面モデル `announcements` に設定する。
3. お知らせ一覧ページ `pages/announcements` を返す。
