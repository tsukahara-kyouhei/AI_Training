# office-order アーキテクチャ概要

## 1. プロジェクト基本情報

| 項目                | 内容                      |
| ------------------- | ------------------------- |
| アーティファクト ID | `jp.co.skig:office-order` |
| バージョン          | 0.0.1-SNAPSHOT            |
| Java                | 21                        |
| Spring Boot         | 3.5.11                    |
| ビルドツール        | Maven (mvnw)              |
| DBドライバ          | PostgreSQL                |

---

## 2. 主要依存ライブラリ

| カテゴリ          | ライブラリ                                                                             |
| ----------------- | -------------------------------------------------------------------------------------- |
| Web / UI          | spring-boot-starter-web, spring-boot-starter-thymeleaf, spring-boot-starter-validation |
| セキュリティ      | spring-boot-starter-security                                                           |
| DB / O/R マッパー | spring-boot-starter-jdbc, mybatis-spring-boot-starter 3.0.4                            |
| バッチ            | spring-boot-starter-batch                                                              |
| メール            | spring-boot-starter-mail                                                               |
| RDBMS             | PostgreSQL (runtime)                                                                   |

---

## 3. レイヤー構成

```
src/main/java/jp/co/skig/officeorder/
│
├── OfficeOrderApplication.java          # エントリポイント
│
├── config/                              # 設定クラス群
│   ├── SecurityConfig.java              # Spring Security 設定
│   ├── BatchExecutionConfig.java        # バッチ実行設定
│   ├── BatchJobConfiguration.java       # バッチジョブ定義
│   ├── AppProperties.java               # app.* カスタムプロパティ
│   ├── AppClockConfig.java              # 時刻プロバイダ Bean
│   ├── MailRetryConfig.java             # メール再送設定
│   └── WebLoggingConfig.java            # リクエストログ設定
│
├── web/                                 # Controller 層
│   ├── HomeController.java
│   ├── CatalogController.java           # 商品一覧・詳細
│   ├── CartController.java              # カート
│   ├── MemberRegistrationController.java
│   ├── AuthController.java              # ログイン・ログアウト
│   ├── MyPageController.java            # マイページ一式
│   ├── ContentController.java           # 静的コンテンツ (ガイド等)
│   ├── ContactController.java           # お問い合わせ
│   ├── InternalBatchController.java     # バッチ手動実行 API (内部用)
│   ├── InternalErrorTestController.java # エラー確認用 (開発)
│   ├── AppErrorController.java          # エラーページ
│   ├── GlobalExceptionLoggingAdvice.java
│   └── view/
│       └── AssetVersionResolver.java    # 静的リソースキャッシュ制御
│
├── service/                             # ビジネスロジック層
│   ├── product/
│   │   ├── ProductService.java
│   │   ├── ProductListSearchService.java
│   │   └── ProductFilterOptionService.java
│   ├── cart/
│   │   └── CartService.java
│   ├── order/
│   │   └── OrderService.java
│   ├── member/
│   │   ├── MemberService.java
│   │   ├── MemberUserDetailsService.java  # Spring Security 連携
│   │   ├── DuplicateEmailException.java
│   │   ├── AddressLimitExceededException.java
│   │   └── FavoritesLimitExceededException.java
│   ├── mail/
│   │   ├── NotificationMailService.java
│   │   └── MailTemplateRenderer.java
│   ├── batch/
│   │   ├── BatchJobService.java
│   │   ├── BatchExecutionService.java
│   │   ├── BatchScheduler.java           # @Scheduled 定期実行
│   │   ├── BatchJobNames.java
│   │   └── (各種例外クラス)
│   ├── announcement/
│   └── contact/
│
├── repository/                          # DB アクセス層 (Mapper ラッパ)
│   ├── ProductRepository.java
│   ├── ProductFilterOptionRepository.java
│   ├── MemberRepository.java
│   ├── OrderRepository.java
│   ├── CartRepository.java
│   ├── CartCookieStore.java             # カートを Cookie に永続化
│   ├── AnnouncementRepository.java
│   ├── ContactRepository.java
│   └── BatchRepository.java
│
├── mapper/                              # MyBatis Mapper インタフェース
│   ├── ProductMapper.java
│   ├── MemberMapper.java
│   ├── OrderMapper.java
│   ├── CartMapper.java
│   ├── AnnouncementMapper.java
│   ├── ContactMapper.java
│   └── BatchMapper.java
│
├── model/                               # ドメインモデル / DTO
│   └── (各ドメインのサブパッケージ)
│
├── common/                              # 共通ユーティリティ
│   ├── AppTimeProvider.java
│   ├── MoneyFormatter.java
│   └── validation/
│
└── logging/                             # ロギング共通処理
```

---

## 4. テンプレート (Thymeleaf) 一覧

| テンプレートファイル                                       | 機能                         |
| ---------------------------------------------------------- | ---------------------------- |
| top.html                                                   | トップページ                 |
| product-list-category-desk.html                            | デスク一覧                   |
| product-list-category-chair.html                           | チェア一覧                   |
| product-list-category-storage.html                         | 収納一覧                     |
| product-list-new-arrivals.html                             | 新着一覧                     |
| product-list-search-results.html                           | 検索結果                     |
| product-detail.html                                        | 商品詳細                     |
| cart.html                                                  | カート                       |
| checkout-input.html                                        | 注文入力                     |
| checkout-method.html                                       | 支払い方法選択               |
| checkout-confirm.html                                      | 注文確認                     |
| checkout-complete.html                                     | 注文完了                     |
| login.html                                                 | ログイン                     |
| member-register.html                                       | 会員登録入力                 |
| member-register-confirm.html                               | 会員登録確認                 |
| member-register-complete.html                              | 会員登録完了                 |
| mypage-order-detail.html                                   | マイページ：注文詳細         |
| mypage-orders-list.html                                    | マイページ：注文履歴         |
| mypage-profile-edit.html                                   | マイページ：プロフィール編集 |
| mypage-addresses.html                                      | マイページ：配送先住所一覧   |
| mypage-address-form.html                                   | マイページ：住所追加・編集   |
| mypage-favorites.html                                      | マイページ：お気に入り       |
| mypage-withdraw.html                                       | マイページ：退会             |
| announcements.html                                         | お知らせ一覧                 |
| contact.html                                               | お問い合わせ                 |
| about.html / guide.html                                    | 会社概要 / ご利用ガイド      |
| legal-terms.html / legal-privacy.html / legal-tokusho.html | 法的ページ                   |

---

## 5. データベーステーブル一覧

### 5-1. 会員系 (`sql/schema/members.sql`)

| テーブル                      | 説明                                             |
| ----------------------------- | ------------------------------------------------ |
| `members`                     | 会員マスタ (個人/法人、認証情報、デフォルト住所) |
| `member_additional_addresses` | 会員の追加配送先住所                             |
| `member_favorites`            | お気に入り商品 (member_id × product_id)          |

### 5-2. 商品系 (`sql/schema/products.sql`)

| テーブル                     | 説明                                          |
| ---------------------------- | --------------------------------------------- |
| `products`                   | 商品マスタ (カテゴリ: desk / chair / storage) |
| `product_variants`           | 商品バリアント (カラー × 価格 × 在庫)         |
| `colors`                     | カラーマスタ                                  |
| `product_desk_attributes`    | デスク固有属性 (天板形状・サイズ等)           |
| `product_chair_attributes`   | チェア固有属性                                |
| `product_storage_attributes` | 収納固有属性                                  |
| `product_images`             | 商品画像パス                                  |

### 5-3. 注文系 (`sql/schema/orders.sql`)

| テーブル                | 説明                                          |
| ----------------------- | --------------------------------------------- |
| `orders`                | 注文ヘッダ (ゲスト・会員両対応、支払い方法等) |
| `order_items`           | 注文明細 (スナップショット形式)               |
| `order_number_counters` | 注文番号採番用カウンタ                        |

### 5-4. マスタ系 (`sql/schema/masters.sql`)

| テーブル                       | 説明                            |
| ------------------------------ | ------------------------------- |
| `desk_top_shapes`              | デスク天板形状マスタ            |
| `desk_tastes`                  | デスクテイストマスタ            |
| `chair_functions`              | チェア機能マスタ                |
| `chair_materials`              | チェア素材マスタ                |
| `chair_tastes`                 | チェアテイストマスタ            |
| `storage_usages`               | 収納用途マスタ                  |
| `tax_rates`                    | 消費税率マスタ (有効期間管理)   |
| `popular_product_rankings`     | 人気商品ランキング (バッチ生成) |
| `recommended_related_products` | 関連商品推薦 (バッチ生成)       |

### 5-5. コンテンツ系 (`sql/schema/content.sql`)

| テーブル        | 説明                    |
| --------------- | ----------------------- |
| `announcements` | お知らせ (掲載期間管理) |
| `inquiries`     | お問い合わせ受付        |

### 5-6. バッチメタデータ (`sql/schema/batch.sql` / `spring-batch-metadata.sql`)

| テーブル                    | 説明           |
| --------------------------- | -------------- |
| `tax_rates` (再掲)          | バッチ参照用   |
| Spring Batch 標準テーブル群 | ジョブ実行履歴 |

---

## 6. 設定・インフラ概要

### application.yml 主要設定

| 設定                    | 値                                              |
| ----------------------- | ----------------------------------------------- |
| サーバーポート          | 8080                                            |
| セッションタイムアウト  | 60 分                                           |
| DB                      | `jdbc:postgresql://localhost:5432/office_order` |
| メールサーバ (ローカル) | localhost:1025 (MailHog 等を想定)               |
| タイムゾーン            | Asia/Tokyo                                      |
| バッチ自動起動          | 無効 (`spring.batch.job.enabled: false`)        |
| バッチ定期実行          | 毎時 0 分 (`app.batch.hourly-cron`)             |
| MyBatis Mapper XML      | `classpath:/mappers/*.xml`                      |

### Docker Compose

`docker-compose.yml` にてローカル開発用インフラを定義（PostgreSQL + メールサーバを想定）。

### ローカル起動手順

```powershell
# PostgreSQL 初期化 (初回のみ)
.\scripts\init-local-postgres.ps1

# アプリケーション起動
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

---

## 7. 全体フロー概略

```
ブラウザ
  │
  ▼
Spring Security (SecurityConfig)
  │  認証・認可チェック
  ▼
Controller (web/)
  │  リクエスト受付・バリデーション
  ▼
Service (service/)
  │  ビジネスロジック
  ▼
Repository (repository/)
  │  DB アクセス組み立て
  ▼
MyBatis Mapper (mapper/ + mappers/*.xml)
  │
  ▼
PostgreSQL (office_order DB)
```

バッチ処理は `BatchScheduler` により毎時起動し、ランキング集計・推薦データ更新などを行う。
メール送信は `NotificationMailService` → SMTP サーバへ。
