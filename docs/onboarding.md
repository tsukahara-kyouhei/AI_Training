# OFFICE ORDER — 新規参画メンバー向けガイド

> 作成日: 2026-03-16

---

## 1. プロジェクト概要

**OFFICE ORDER** は、オフィス家具を販売する顧客向け EC サイトです。  
エンドユーザー（購入者）が直接ブラウザで操作する **フロントエンド + バックエンドを一体化した Web アプリケーション**です。

### 本リポジトリのスコープ

| 対象       | 説明                                                     |
|------------|----------------------------------------------------------|
| ✅ 含む    | 商品閲覧、会員登録・マイページ、カート、注文（受注まで）、お問い合わせ |
| ❌ 含まない | 受注後の社内業務処理（出荷・請求など）、管理画面（別システム） |

---

## 2. 技術スタック

| 分野           | 技術                                         |
|----------------|----------------------------------------------|
| 言語           | Java 21                                      |
| フレームワーク | Spring Boot 3.5.11                           |
| 認証・認可     | Spring Security（フォームログイン + BCrypt）  |
| ビュー         | Thymeleaf                                    |
| DB アクセス    | MyBatis（XML マッパー）                       |
| バッチ         | Spring Batch                                 |
| DB             | PostgreSQL 16                                |
| ビルド         | Maven Wrapper (`mvnw`)                       |
| ローカル補助   | Docker Compose（PostgreSQL・Mailpit・DBGate）|

---

## 3. アーキテクチャ概要

```
ブラウザ
  │  HTTP
  ▼
Controller（web/）
  │
Service（service/）
  │
Repository（repository/） ──── MyBatis XML Mapper（resources/mappers/）
  │
PostgreSQL（office_order DB）
```

- **Controller** — URL マッピング・リクエスト受付・レスポンス返却
- **Service** — ビジネスロジック（トランザクション境界はここ）
- **Repository** — DB 操作の窓口（MyBatis Mapper インターフェース + XML）
- **Model** — 画面表示用 View オブジェクト・フォームクラス・Enum など

---

## 4. 主要機能一覧

### 4-1. 商品カタログ

| 機能               | URL                                        |
|--------------------|--------------------------------------------|
| トップ             | `/`                                        |
| 新着商品一覧       | `/products/new-arrivals`                   |
| カテゴリ別一覧（デスク）  | `/products/category/desk`             |
| カテゴリ別一覧（チェア）  | `/products/category/chair`            |
| カテゴリ別一覧（収納）    | `/products/category/storage`          |
| キーワード検索結果  | `/products/search`                        |
| 商品詳細           | `/products/{productId}`                    |

**商品の種類:**

| カテゴリ | カテゴリID | 固有属性                             |
|----------|------------|--------------------------------------|
| デスク   | `desk`     | 天板形状・サイズ（幅/奥行/高さ）・テイスト |
| チェア   | `chair`    | 機能・素材・テイスト                 |
| 収納     | `storage`  | 用途・サイズ（幅/奥行/高さ）         |

**商品バリエーション:**
- 1 商品は複数の **カラー × 価格** の組み合わせ（`product_variants`）を持てる
- `has_variation = TRUE` の場合、同一シリーズの別商品を「バリエーションリンク」として表示

**一覧絞り込み:**
- 在庫あり / 価格帯 / カラー / 並び順（新着順・価格昇降順）

### 4-2. お気に入り

- ログイン会員のみ利用可能（上限件数あり）
- 商品詳細ページ・一覧ページからToggleで登録・解除
- マイページ `/mypage/favorites` で一覧閲覧

### 4-3. カート

- Cookie ベースでゲスト・会員共通で管理
- 商品詳細から「カートに追加」→ カート画面 `/cart` に遷移
- 数量変更・削除・アイテムごとの組立オプション選択が可能

### 4-4. 購入フロー（チェックアウト）

```
カート
  └→ 購入方法選択 (/checkout/method)
         └→ 注文情報入力 (/checkout/input)
                └→ 注文確認 (/checkout/confirm)
                       └→ 注文完了 (/checkout/complete)
```

**対応支払方法:**

| コード                  | 説明         |
|-------------------------|--------------|
| `bank_transfer`         | 銀行振込     |
| `cash_on_delivery`      | 代金引換     |
| `convenience_store`     | コンビニ払い |

**配送方法:**

| コード      | 説明               |
|-------------|--------------------|
| `normal`    | 通常配送           |
| `assembly`  | 組立付き配送       |

- ゲスト注文・会員注文の両方に対応
- 注文確認画面にワンタイムトークンを使用して二重送信を防止
- 注文確定と同時に **注文番号採番**・**注文完了メール送信**（非同期）

**注文番号体系:**  
`YYYYMMDD` + 6桁連番（例: `20260316000001`）

**注文ステータス:**

| ステータス          | 説明         |
|---------------------|--------------|
| `received`          | 受注済み     |
| `awaiting_payment`  | 入金待ち     |
| `processing`        | 処理中       |
| `completed`         | 完了         |
| `cancelled`         | キャンセル   |

### 4-5. 会員登録

```
/register (入力) → /register/confirm (確認) → /register/complete (完了)
```

- 個人 / 法人の区分あり（法人の場合は会社名必須）
- メールアドレスは一意（大文字小文字区別なし）
- パスワードは BCrypt ハッシュ化して保存
- 登録完了後に **会員登録完了メール**を送信

### 4-6. マイページ

| 機能          | URL                               |
|---------------|-----------------------------------|
| 購入履歴一覧  | `/mypage/orders`                  |
| 注文詳細      | `/mypage/orders/{orderNumber}`    |
| お気に入り    | `/mypage/favorites`               |
| プロフィール編集 | `/mypage/profile/edit`         |
| 追加配送先一覧 | `/mypage/addresses`              |
| 追加配送先登録/編集 | `/mypage/addresses/new` など  |
| 退会          | `/mypage/withdraw`                |

- **再注文機能:** 過去注文の商品をカートに追加できる
- **追加配送先:** 最大件数制限あり（`AddressLimitExceededException`）
- 退会した会員は `member_status = 'withdrawn'` に更新される（論理削除）

### 4-7. お問い合わせ

- `/contact` — 会員・ゲスト共通
- 問い合わせ種別: `product`（商品）/ `delivery_date`（納期）/ `order`（注文）/ `shipping`（配送）/ `return_cancel`（返品・キャンセル）/ `other`（その他）
- 注文前・注文後の区分あり
- 送信内容は `inquiries` テーブルに保存

### 4-8. コンテンツページ

| ページ       | URL               |
|--------------|-------------------|
| お知らせ一覧 | `/announcements`  |
| ご利用ガイド | `/guide`          |
| 会社概要     | `/about`          |
| 特定商取引法 | `/legal/tokusho`  |
| プライバシーポリシー | `/legal/privacy` |
| 利用規約     | `/legal/terms`    |

---

## 5. 認証・認可

- Spring Security のフォームログインを使用
- ログインURL: `/login`、ログアウト: `/logout`
- `ROLE_MEMBER` を持つ会員のみ `/mypage/**` にアクセス可能
- 未認証でマイページにアクセスするとログイン画面にリダイレクト（ログイン後に元 URL へ戻る）
- ログイン画面のメールアドレス記憶は Cookie で実現（`LoginEmailCookieService`）

---

## 6. バッチ処理

2 種類のバッチジョブが定期実行されます。

| ジョブ名                  | 説明                                                    |
|---------------------------|---------------------------------------------------------|
| `popular-ranking`         | 直近1か月の販売数に基づき売れ筋ランキング（上位10件）を再計算 |
| `recommended-related`     | カテゴリ・属性の類似度スコアでおすすめ関連商品（上位4件）を再計算 |

**実行タイミング:**
- アプリ起動時に1回自動実行
- その後は **毎時0分**（`0 0 * * * *`）に定期実行

**ローカル環境での手動実行（`local` プロファイルのみ）:**

```sh
# 売れ筋ランキング再計算
curl -X POST http://localhost:8080/internal/batch/jobs/popular-ranking/executions

# おすすめ関連商品再計算
curl -X POST http://localhost:8080/internal/batch/jobs/recommended-related/executions

# ジョブ実行履歴確認
curl http://localhost:8080/internal/batch/jobs
```

---

## 7. メール送信

- **SMTP** （`spring.mail`）で送信（ローカルでは Mailpit が受信）
- 送信される通知メール:

| トリガー         | テンプレート                                  |
|------------------|-----------------------------------------------|
| 会員登録完了     | `mail/templates/member-registration-body.txt` |
| 注文完了         | `mail/templates/order-complete-body.txt`      |

- メール件名は `mail/subjects.properties` で管理
- 送信失敗時はリトライ設定あり（`MailRetryConfig`）

---

## 8. データベース設計（主要テーブル）

### 会員系

| テーブル                       | 説明                       |
|--------------------------------|----------------------------|
| `members`                      | 会員情報（個人/法人）       |
| `member_additional_addresses`  | 会員の追加配送先            |
| `member_favorites`             | お気に入り（商品との紐付け）|

### 商品系

| テーブル                       | 説明                               |
|--------------------------------|------------------------------------|
| `products`                     | 商品マスタ                         |
| `product_variants`             | カラー・価格・在庫数のバリエーション |
| `product_desk_attributes`      | デスク固有属性                     |
| `product_chair_attributes`     | チェア固有属性                     |
| `product_storage_attributes`   | 収納固有属性                       |
| `colors`                       | カラーマスタ                       |

### 注文系

| テーブル                  | 説明                               |
|---------------------------|------------------------------------|
| `orders`                  | 注文ヘッダ（顧客情報・金額・ステータス）|
| `order_items`             | 注文明細（商品名・単価スナップショット）|
| `order_number_counters`   | 日次連番カウンタ                   |

### コンテンツ・その他

| テーブル                        | 説明                                  |
|---------------------------------|---------------------------------------|
| `announcements`                 | お知らせ                              |
| `inquiries`                     | お問い合わせ                          |
| `tax_rates`                     | 消費税率（有効期間管理）              |
| `popular_product_rankings`      | 売れ筋ランキング（バッチ結果）        |
| `recommended_related_products`  | おすすめ関連商品（バッチ結果）        |

> **注意:** `order_items` は注文時点の商品名・単価を**スナップショット**として保持します。  
> 後から商品マスタが変更されても注文の内容は変わりません。

---

## 9. ディレクトリ構成

```
src/main/java/jp/co/skig/officeorder/
├── OfficeOrderApplication.java   # エントリーポイント
├── common/                       # 共通ユーティリティ・バリデーション
├── config/                       # Spring 設定クラス群
│   ├── SecurityConfig.java       # 認証・認可
│   ├── BatchJobConfiguration.java# バッチジョブ定義
│   ├── AppProperties.java        # application.yml のプロパティバインド
│   └── ...
├── logging/                      # ログ関連ユーティリティ
├── mapper/                       # MyBatis Mapper インターフェース
├── model/                        # ビューモデル・フォーム・Enum
│   ├── announcement/
│   ├── batch/
│   ├── cart/
│   ├── member/
│   ├── order/
│   └── product/
├── repository/                   # DB アクセス層
├── service/                      # ビジネスロジック層
│   ├── batch/                    # バッチ実行・スケジュール
│   ├── cart/
│   ├── mail/
│   ├── member/
│   ├── order/
│   └── product/
└── web/                          # Controller 層
    ├── auth/                     # 認証補助クラス
    ├── view/                     # ビュー補助クラス
    ├── CartController.java       # カート・購入フロー
    ├── CatalogController.java    # 商品一覧・詳細・お気に入り
    ├── ContactController.java    # お問い合わせ
    ├── ContentController.java    # お知らせ・コンテンツページ
    ├── HomeController.java       # トップページ
    ├── MemberRegistrationController.java  # 会員登録
    ├── MyPageController.java     # マイページ
    └── ...

src/main/resources/
├── application.yml               # アプリケーション設定
├── logback-spring.xml            # ログ設定
├── messages.properties           # バリデーション・UI メッセージ
├── mappers/                      # MyBatis XML マッパー
├── mail/                         # メールテンプレート・件名
├── static/                       # CSS / JS / 画像
└── templates/                    # Thymeleaf HTML テンプレート
    ├── fragments/                # ヘッダ・フッタ等の共通部品
    └── pages/                    # 各画面の HTML
```

---

## 10. ローカル環境のセットアップ

### 前提条件

- Java 21
- Docker Desktop（推奨）または ローカル PostgreSQL 16 + `psql`

### 手順（Docker 利用・推奨）

```powershell
# 1. DB・メールサーバーなどインフラを起動
docker compose up -d

# 2. アプリを起動（local プロファイル）
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 確認 URL

| 用途         | URL                        |
|--------------|----------------------------|
| アプリ       | http://localhost:8080/     |
| DB 管理 (DBGate) | http://localhost:3000  |
| メール確認 (Mailpit) | http://localhost:8025 |

### テストアカウント

| メールアドレス             | パスワード  | 備考       |
|----------------------------|-------------|------------|
| `member01@example.com`     | `password`  | 個人会員   |
| `member02@example.com`     | `password`  | 個人会員   |
| `member04@example.com`     | `password`  | 法人会員   |

初期データ: 会員 7件 / 商品 50件 / 注文 約100件（過去1か月分）

---

## 11. ログ

- コンソールと `log/office-order.log` の両方に出力
- ローテーション済みログは `log/archive/` に格納
- ログレベル・出力形式は [logback-spring.xml](../src/main/resources/logback-spring.xml) で定義

---

## 12. 主要な設定値（`application.yml`）

| キー                         | 説明                                       |
|------------------------------|--------------------------------------------|
| `app.time-zone`              | アプリ全体のタイムゾーン（`Asia/Tokyo`）   |
| `app.batch.hourly-cron`      | バッチの定期実行 CRON 式（`0 0 * * * *`）  |
| `app.mail.from`              | メール送信元アドレス                       |
| `app.mail.site-url`          | メール内リンクのベース URL                 |
| `app.cookie.cart.secure-mode`| カート Cookie の Secure 属性制御           |
| `server.servlet.session.timeout` | セッションタイムアウト（60分）        |

---

## 13. 開発上の注意点

1. **注文明細はスナップショット保存**  
   `order_items` に商品名・単価を時点情報として保持するため、後から商品マスタを変更しても過去注文には影響しません。

2. **メールアドレスの一意性は小文字化インデックスで管理**  
   `members` テーブルに `LOWER(email)` の一意インデックスが張られています。大文字・小文字違いでも重複登録にはなりません。

3. **バッチ結果は日次で差し替え**  
   `popular_product_rankings` / `recommended_related_products` はバッチ実行日の結果をまるごと再書き込みします。トップページ等では最新の集計日付けのレコードを参照します。

4. **消費税率はテーブルで管理**  
   `tax_rates` テーブルで有効期間付きの税率を管理しており、ハードコードされていません。

5. **CSRF 除外**  
   内部バッチ API（`/internal/batch/**`）は CSRF 検証を除外しています。`local` プロファイルでのみ起動するコントローラです。

6. **チェックアウトの二重送信防止**  
   注文確認画面にワンタイムトークンをセッションに保存し、確定 POST 時に検証します。

---

## 14. 関連ドキュメント

- [README.md](../README.md) — ローカル起動手順・バッチ手動実行コマンドなど
- [sql/schema/](../sql/schema/) — DDL（テーブル定義）
- [sql/seed/](../sql/seed/) — 初期データ投入 SQL
