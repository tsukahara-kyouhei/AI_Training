# FEAT-003: API・URL 設計書

## 1. 会員向けエンドポイント

### 1.1 レビュー一覧（もっと見る）

| 項目 | 値 |
|------|----|
| Method | GET |
| URL | `/products/{productId}/reviews` |
| 認証 | 不要（公開データ） |
| パラメータ | `page` (int, default 1), `size` (int, default 5) |
| レスポンス | JSON: `{ items: ReviewView[], hasNext: boolean }` |
| 用途 | 商品詳細ページの「もっと見る」ボタン押下時に非同期で追加取得 |

### 1.2 レビュー投稿

| 項目 | 値 |
|------|----|
| Method | POST |
| URL | `/products/{productId}/reviews` |
| 認証 | 必須（ログイン会員） |
| リクエスト | フォーム: `rating`, `title`, `body`, `isPublished` |
| バリデーション | `rating`: 1〜5 必須, `title`: 最大100文字, `body`: 必須・最大1000文字 |
| 成功時 | リダイレクト `302` → `/products/{productId}` |
| エラー時 | 商品詳細ページを再表示（フォームエラー表示） |

### 1.3 レビュー更新

| 項目 | 値 |
|------|----|
| Method | POST |
| URL | `/products/{productId}/reviews/{reviewId}` |
| 認証 | 必須（投稿者本人） |
| リクエスト | フォーム: `rating`, `title`, `body`, `isPublished` |
| バリデーション | 投稿と同一 |
| 前提条件 | `is_blocked = false` であること |
| 成功時 | リダイレクト `302` → `/products/{productId}` |

### 1.4 レビュー削除

| 項目 | 値 |
|------|----|
| Method | POST |
| URL | `/products/{productId}/reviews/{reviewId}/delete` |
| 認証 | 必須（投稿者本人） |
| 前提条件 | `is_blocked = false` であること |
| 成功時 | リダイレクト `302` → `/products/{productId}` |

### 1.5 レビュー投稿/編集フォーム表示

商品詳細ページ (`GET /products/{productId}`) のレスポンスにインラインフォームを含める。
既存の `CatalogController.productDetail()` を拡張し、以下のモデル属性を追加する。

| モデル属性 | 型 | 説明 |
|------------|----|------|
| `reviewSummary` | `ReviewSummaryView` | 平均評価・件数 |
| `reviews` | `List<ReviewView>` | 初期表示分のレビュー一覧 |
| `hasMoreReviews` | `boolean` | 「もっと見る」表示判定 |
| `canPostReview` | `boolean` | 投稿可能判定（ログイン＋購入履歴あり＋未投稿 or 投稿済み） |
| `hasPurchased` | `boolean` | 購入履歴有無 |
| `myReview` | `ReviewView` | 自分の投稿済みレビュー（なければ null） |
| `reviewForm` | `ReviewForm` | 投稿/編集フォームバインド用 |

## 2. 管理者向けエンドポイント

> 管理者向けエンドポイントは今後のレベルアップ要件とし、本課題のスコープ外とする。

## 3. SecurityConfig 変更

| パターン | ルール |
|----------|--------|
| `POST /products/*/reviews/**` | 認証チェックをコントローラで実施（未ログイン時は `/login?redirect=...` へリダイレクト） |

> 備考: 既存のお気に入りトグル（`POST /products/{productId}/favorite`）と同じパターンで、コントローラ側でセッションから会員情報を取得し、未ログイン時はログインページへリダイレクトする。
