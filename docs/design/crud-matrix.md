# CRUD マトリクス

## 凡例

| 記号 | 操作 | SQL |
|---|---|---|
| **C** | 登録 | INSERT |
| **R** | 参照 | SELECT |
| **U** | 更新 | UPDATE |
| **D** | 削除 | DELETE |
| — | 操作なし | — |

> `announcements`（お知らせ）は `AuthModelAdvice` によりすべての画面リクエストで共通 SELECT されるため、全機能に **R** を記載しています。

---

## CRUD マトリクス（機能 × テーブル）

### マスタ系テーブル

| テーブル（論理名） | トップ | お知らせ一覧 | 商品一覧・検索 | 商品詳細 | お気に入り | カート | 注文・購入フロー | ログイン | 会員登録 | 購入履歴 | MYお気に入り | MY会員情報変更 | MYお届け先 | MY退会 | お問い合わせ | バッチ処理 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `announcements`（お知らせ） | R | R | R | R | R | R | R | R | R | R | R | R | R | R | R | — |
| `desk_top_shapes`（デスク天板形状） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `tastes`（テイスト） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `chair_functions`（チェア機能） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `chair_materials`（チェア素材） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `storage_usages`（収納家具用途） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `tax_rates`（消費税率） | — | — | — | — | — | R | R | — | — | — | — | — | — | — | — | — |

### 商品系テーブル

| テーブル（論理名） | トップ | お知らせ一覧 | 商品一覧・検索 | 商品詳細 | お気に入り | カート | 注文・購入フロー | ログイン | 会員登録 | 購入履歴 | MYお気に入り | MY会員情報変更 | MYお届け先 | MY退会 | お問い合わせ | バッチ処理 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `colors`（カラー） | R | — | R | R | — | R | R | — | — | — | R | — | — | — | — | — |
| `products`（商品） | R | — | R | R | R | R | R | — | — | — | R | — | — | — | — | R |
| `product_variants`（商品バリアント） | R | — | R | R | R | R | R | — | — | R | R | — | — | — | — | R |
| `product_desk_attributes`（商品デスク属性） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `product_chair_attributes`（商品チェア属性） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `product_storage_attributes`（商品収納属性） | — | — | R | — | — | — | — | — | — | — | — | — | — | — | — | — |
| `popular_product_rankings`（売れ筋ランキング） | R | — | — | — | — | — | — | — | — | — | — | — | — | — | — | D・C |
| `recommended_related_products`（おすすめ関連商品） | — | — | — | R | — | — | — | — | — | — | — | — | — | — | — | D・C |

### 会員系テーブル

| テーブル（論理名） | トップ | お知らせ一覧 | 商品一覧・検索 | 商品詳細 | お気に入り | カート | 注文・購入フロー | ログイン | 会員登録 | 購入履歴 | MYお気に入り | MY会員情報変更 | MYお届け先 | MY退会 | お問い合わせ | バッチ処理 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `members`（会員） | — | — | — | — | — | — | R | R | R・C | — | — | R・U | — | U | R | — |
| `member_additional_addresses`（会員追加お届け先） | — | — | — | — | — | — | R | — | — | — | — | — | R・C・U・D | — | — | — |
| `member_favorites`（会員お気に入り） | — | — | — | R | R・C・D | — | — | — | — | — | R | — | — | — | — | — |

### 注文系テーブル

| テーブル（論理名） | トップ | お知らせ一覧 | 商品一覧・検索 | 商品詳細 | お気に入り | カート | 注文・購入フロー | ログイン | 会員登録 | 購入履歴 | MYお気に入り | MY会員情報変更 | MYお届け先 | MY退会 | お問い合わせ | バッチ処理 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `orders`（注文） | — | — | — | — | — | — | C・R | — | — | R | — | — | — | — | — | R |
| `order_items`（注文明細） | — | — | — | — | — | — | C | — | — | R | — | — | — | — | — | R |
| `order_number_counters`（注文番号採番カウンタ） | — | — | — | — | — | — | C・U | — | — | — | — | — | — | — | — | — |
| `order_status_histories`（注文ステータス履歴） | — | — | — | — | — | — | C | — | — | R | — | — | — | — | — | — |

### コンテンツ系テーブル

| テーブル（論理名） | トップ | お知らせ一覧 | 商品一覧・検索 | 商品詳細 | お気に入り | カート | 注文・購入フロー | ログイン | 会員登録 | 購入履歴 | MYお気に入り | MY会員情報変更 | MYお届け先 | MY退会 | お問い合わせ | バッチ処理 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `inquiries`（お問い合わせ） | — | — | — | — | — | — | — | — | — | — | — | — | — | — | C | — |

---

## 機能別詳細

### トップ（`/`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `products` | R | 新着商品・売れ筋ランキング商品の取得 |
| `product_variants` | R | 商品一覧 JOIN |
| `colors` | R | 商品カードのカラーコード取得 |
| `popular_product_rankings` | R | 売れ筋ランキング順位の取得 |

---

### お知らせ一覧（`/announcements`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | 有効なお知らせを全件取得 |

---

### 商品一覧・検索（`/products/new-arrivals`, `/products/search`, `/categories/*`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `products` | R | ページネーション付き商品一覧・件数取得 |
| `product_variants` | R | 商品一覧 JOIN・在庫フィルタ |
| `colors` | R | カラーフィルタ選択肢・商品カードのカラー |
| `desk_top_shapes` | R | デスクカテゴリのフィルタ選択肢 |
| `tastes` | R | テイストフィルタ選択肢 |
| `chair_functions` | R | チェアカテゴリのフィルタ選択肢 |
| `chair_materials` | R | チェアカテゴリのフィルタ選択肢 |
| `storage_usages` | R | 収納カテゴリのフィルタ選択肢 |
| `product_desk_attributes` | R | デスク属性によるサブクエリフィルタ |
| `product_chair_attributes` | R | チェア属性によるサブクエリフィルタ |
| `product_storage_attributes` | R | 収納属性によるサブクエリフィルタ |

---

### 商品詳細（`/products/{productId}`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `products` | R | 商品詳細・同シリーズリンク取得 |
| `product_variants` | R | バリアント（カラー・価格・在庫）一覧 |
| `colors` | R | バリアントJOIN |
| `member_favorites` | R | ログイン中の場合、お気に入り登録済み判定 |
| `recommended_related_products` | R | おすすめ関連商品の取得 |

---

### お気に入り（`POST /products/{productId}/favorite`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `products` | R | 商品存在バリデーション |
| `product_variants` | R | 商品存在バリデーション JOIN |
| `member_favorites` | R | 登録済み判定 |
| `member_favorites` | C | お気に入り追加 |
| `member_favorites` | D | お気に入り解除 |

---

### カート（`/cart`, カート操作 POST）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `product_variants` | R | カート内商品スナップショット取得 |
| `products` | R | カート内商品スナップショット JOIN |
| `colors` | R | カート内商品スナップショット JOIN |
| `tax_rates` | R | 現在有効な消費税率の取得 |

> カート明細の追加・更新・削除は **Cookie のみ**で管理。DB 書き込みは発生しない。

---

### 注文・購入フロー（`/checkout/*`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | R | 会員ログイン時の入力プリフィル |
| `member_additional_addresses` | R | 配送先選択オプション（会員ログイン時） |
| `product_variants` | R | カート内容のスナップショット取得 |
| `products` | R | カート内容のスナップショット JOIN |
| `colors` | R | カート内容のスナップショット JOIN |
| `tax_rates` | R | 消費税率の取得 |
| `orders` | C | 注文確定時に注文レコード INSERT |
| `orders` | R | 注文完了画面の表示 |
| `order_items` | C | 注文明細を1件ずつ INSERT |
| `order_number_counters` | C・U | 注文番号採番（当日初回: INSERT、2回目以降: UPDATE） |
| `order_status_histories` | C | 受付ステータス INSERT |

---

### ログイン（`/login`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | R | Spring Security 認証（email・パスワードハッシュ照合） |

---

### 会員登録（`/members/register`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | R | メールアドレス重複チェック |
| `members` | C | 会員レコード INSERT（登録後に自動ログイン） |

---

### 購入履歴（`/mypage/orders`, `/mypage/orders/{no}`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `orders` | R | 注文一覧・注文詳細・ページネーション件数 |
| `order_items` | R | 注文明細一覧・再注文用商品一覧 |
| `order_status_histories` | R | ステータス変更履歴一覧 |
| `product_variants` | R | 再注文時の現在在庫確認（LEFT JOIN） |

---

### MYお気に入り（`/mypage/favorites`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `member_favorites` | R | お気に入り一覧・件数取得 |
| `products` | R | お気に入り JOIN |
| `product_variants` | R | お気に入りカードのバリアント情報 JOIN |
| `colors` | R | お気に入りカードのカラーコード JOIN |

---

### MY会員情報変更（`/mypage/profile`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | R | プロフィール初期値取得・メールアドレス重複チェック・セッション更新用再取得 |
| `members` | U | プロフィール変更の保存 |

---

### MYお届け先（`/mypage/addresses`, `/mypage/addresses/**`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `member_additional_addresses` | R | お届け先一覧・件数・編集フォーム初期値 |
| `member_additional_addresses` | C | お届け先の新規登録 |
| `member_additional_addresses` | U | お届け先の編集保存 |
| `member_additional_addresses` | D | お届け先の削除 |

---

### MY退会（`/mypage/withdraw`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | U | `member_status = 'withdrawn'` に更新（論理削除） |

> 退会は物理削除ではなく `members.member_status` を `'withdrawn'` に更新する論理削除。`member_favorites` や `member_additional_addresses` の物理削除は行われない。

---

### お問い合わせ（`/contact`）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `announcements` | R | ヘッダお知らせ（AuthModelAdvice） |
| `members` | R | ログイン中の場合、フォーム初期値プリフィル |
| `inquiries` | C | お問い合わせ内容の INSERT（`RETURNING inquiry_id`） |

---

### バッチ処理（売れ筋ランキング・おすすめ関連商品）

| テーブル | CRUD | 内容 |
|---|:---:|---|
| `orders` | R | 直近30日・非キャンセル注文の集計データ取得 |
| `order_items` | R | 注文明細JOIN（商品コード・数量） |
| `product_variants` | R | 商品コードから商品ID解決 JOIN |
| `products` | R | 販売期間フィルタ（販売中商品のみ集計対象） |
| `popular_product_rankings` | D | 当日分ランキングを全削除（全置換パターン） |
| `popular_product_rankings` | C | 新ランキングを1件ずつ INSERT |
| `recommended_related_products` | D | 当日分おすすめを全削除（全置換パターン） |
| `recommended_related_products` | C | 新おすすめを1件ずつ INSERT |

---

## 設計上の注意事項

| 項目 | 内容 |
|---|---|
| **カート管理** | カート明細（追加・更新・削除・クリア）は Cookie のみで管理。DBへの書き込みは一切発生しない |
| **注文番号採番** | `order_number_counters` は UPSERT（`INSERT ... ON CONFLICT DO UPDATE`）。当日初回は INSERT、以降は UPDATE 相当 |
| **退会は論理削除** | `members.member_status = 'withdrawn'` で退会処理。関連テーブルの物理削除は行わない |
| **バッチは全置換** | `popular_product_rankings` / `recommended_related_products` は当日分を DELETE 後に INSERT で全置換 |
| **お知らせの横断 R** | `announcements` は `AuthModelAdvice` により全画面リクエストで共通 SELECT される（エラーページを除く） |
| **注文明細はスナップショット** | `order_items` の商品名・カラー名・単価は注文確定時点の値をコピー保持。`products`・`product_variants` への外部キーは持たない |
