# システム概要書

## 1. プロジェクト概要

| 項目 | 内容 |
|---|---|
| プロジェクト名 | OFFICE ORDER |
| システム種別 | 顧客向け EC サイト（B2C） |
| 取扱商品 | オフィス家具（デスク・チェア・収納） |
| グループID | jp.co.skig |
| アーティファクトID | office-order |
| バージョン | 0.0.1-SNAPSHOT |

## 2. システムの目的

オフィス家具（デスク・チェア・収納）を取り扱う顧客向け EC サイトを提供する。  
ゲスト・会員の双方が商品を閲覧・購入でき、会員は注文履歴やお気に入り管理などの付加機能を利用できる。

## 3. 対象範囲

### 本システムの責務

- 商品カタログの閲覧（カテゴリ別一覧・検索・フィルタ・詳細表示）
- 会員登録・ログイン・マイページ管理
- ショッピングカート（ゲスト・会員両対応）
- 注文処理（支払方法選択→配送先入力→確認→完了の4ステップ）
- お問い合わせフォーム
- お知らせ・静的コンテンツ表示
- バッチ処理（売れ筋ランキング計算・おすすめ関連商品計算）
- メール通知（会員登録完了・注文完了）

### 本システムの責務外

- 受注後の業務処理（出荷・配送管理等）
- 管理画面（商品登録・在庫管理・注文管理等）
- 決済連携（外部決済サービスとの接続）

## 4. 対象ユーザー

| ユーザー種別 | 説明 |
|---|---|
| ゲストユーザー | 会員登録なしで商品閲覧・カート利用・注文が可能 |
| 会員ユーザー | ログインにより注文履歴・お気に入り・住所管理・プロフィール編集等の追加機能を利用可能 |

## 5. システム構成概要

```
┌─────────────────────────────────────────────────────┐
│                  クライアント（ブラウザ）                │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP (port 8080)
┌─────────────────────▼───────────────────────────────┐
│              Spring Boot アプリケーション               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │Controller│→│ Service  │→│Repository│            │
│  └──────────┘ └──────────┘ └────┬─────┘            │
│  ┌──────────┐ ┌──────────┐      │                  │
│  │Thymeleaf │ │  Batch   │      │ MyBatis          │
│  └──────────┘ └──────────┘      │                  │
└─────────────────────────────────┼──────────────────┘
                                  │ JDBC (port 5432)
┌─────────────────────────────────▼──────────────────┐
│              PostgreSQL 16                          │
│              (office_order DB)                      │
└─────────────────────────────────────────────────────┘

┌──────────────────┐    ┌──────────────────┐
│  Mailpit         │    │  DbGate          │
│  (SMTP: 1025)    │    │  (Web UI: 3000)  │
│  (Web UI: 8025)  │    │  DB管理ツール      │
└──────────────────┘    └──────────────────┘
```

### Docker Compose 構成

| サービス | イメージ | ポート | 用途 |
|---|---|---|---|
| db | postgres:16 | 5432 | メインデータベース |
| mailpit | axllent/mailpit:v1.20 | 1025 (SMTP) / 8025 (Web UI) | 開発用メールサーバー |
| dbgate | dbgate/dbgate:latest | 3000 | DB管理Web UI |

## 6. アプリケーション構成

### レイヤー構成

| レイヤー | パッケージ | 役割 |
|---|---|---|
| Web | `web`, `web.auth`, `web.view` | HTTPリクエスト処理、認証、画面表示補助 |
| Model | `model.*` | 画面表示用ビューモデル、フォームオブジェクト |
| Service | `service.*` | ビジネスロジック |
| Repository | `repository` | データアクセスインターフェース |
| Mapper | `mapper`, `mapper.row` | MyBatis マッパーインターフェース、結果行マッピング |
| Config | `config` | 各種設定（セキュリティ、バッチ、メール等） |
| Logging | `logging` | ログ制御（MDC、リクエストID、マスキング） |
| Common | `common` | 共通ユーティリティ（時刻、金額フォーマット、バリデーション） |

### 主要機能領域

| 機能領域 | コントローラー | 主なサービス |
|---|---|---|
| トップページ | HomeController | AnnouncementService, ProductService |
| 商品カタログ | CatalogController | ProductService, ProductListSearchService, ProductFilterOptionService |
| カート・注文 | CartController | CartService, OrderService |
| 会員登録 | MemberRegistrationController | MemberService |
| 認証 | AuthController | MemberUserDetailsService |
| マイページ | MyPageController | MemberService, OrderService |
| お問い合わせ | ContactController | ContactService |
| 静的コンテンツ | ContentController | - |
| バッチ（内部API） | InternalBatchController | BatchJobService, BatchExecutionService |
| エラー | AppErrorController | - |

## 7. データ構成概要

| カテゴリ | テーブル数 | 主なテーブル |
|---|---|---|
| 会員 | 3 | members, member_additional_addresses, member_favorites |
| 商品 | 6 | products, product_variants, colors, 各カテゴリ属性テーブル |
| 注文 | 4 | orders, order_items, order_number_counters, order_status_histories |
| コンテンツ | 2 | announcements, inquiries |
| マスタ | 7 | desk_top_shapes, desk_tastes, chair_functions 等 |
| バッチ | 3 | tax_rates, popular_product_rankings, recommended_related_products |
| **合計** | **25** | |

## 8. 初期データ

| データ種別 | 件数 | 備考 |
|---|---|---|
| 会員 | 7件 | 有効会員6件、退会済み1件 |
| 商品 | 50件 | デスク・チェア・収納 |
| 注文 | 約100件 | 過去1か月分を中心 |
| テスト用パスワード | `password` | BCrypt ハッシュ化して格納 |
