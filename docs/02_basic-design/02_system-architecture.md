# システム構成図

## 1. アーキテクチャ概要

本システムは **レイヤードアーキテクチャ** を採用するSpring Bootモノリシックアプリケーションである。

```
ブラウザ / HTTPクライアント
        |
        v
+-----------------------------------------------+
|        Spring MVC (DispatcherServlet)         |
|  +------------------+  +-------------------+ |
|  | Controller層     |  | Spring Security   | |
|  | (/web/controller)|  | (AuthFilter等)   | |
|  +--------+---------+  +--------+----------+ |
|           |                     |             |
|           v                     |             |
|  +--------+---------+           |             |
|  | Service層         |           |             |
|  | (/domain/service)|           |             |
|  +--------+---------+           |             |
|           |                     |             |
|           v                     |             |
|  +--------+---------+           |             |
|  | Repository層      |           |             |
|  | (/domain/repo)   |           |             |
|  +--------+---------+           |             |
|           |                     |             |
+-----------|---------------------+-------------+
            |
            v
     MyBatis (Mapper XML)
            |
            v
     PostgreSQL 16 (DB)
```

## 2. 層構成

| 層 | パッケージ | 役割 |
|---|---|---|
| Controller | `jp.co.skig.officeorder.web.controller` | HTTPリクエスト受付・レスポンス返却 |
| DTO | `jp.co.skig.officeorder.web.dto` | 画面フォーム入力値・表示向けDTO |
| Service | `jp.co.skig.officeorder.domain.service` | ビジネスロジック、トランザクション制御 |
| Model | `jp.co.skig.officeorder.domain.model` | ドメインオブジェクト |
| Repository | `jp.co.skig.officeorder.domain.repository` | データアクセスインターフェース |
| Mapper | `jp.co.skig.officeorder.infrastructure.mapper` | MyBatis Mapperインターフェース |
| Config | `jp.co.skig.officeorder.config` | 各種設定クラス |
| Batch | `jp.co.skig.officeorder.batch` | Spring Batchジョブ・ステップ |

## 3. 外部システム連携

```
+------------------------------------------+
| office-order (Spring Boot)               |
|                                          |
|  Spring Mail ──> Mailpit        :1025    |
|                  (SMTPテストサーバ)      |
|  MyBatis+JDBC ──> PostgreSQL 16 :5432    |
|                   DB: office_order       |
+------------------------------------------+
```

**本番環境:**
- メール配信サーバー: 実際のSMTPサーバーに切り替え（`spring.mail.*`で設定）
- DB: 本番用PostgreSQLサーバーに接続

## 4. 認証・認可アーキテクチャ

```
HTTPリクエスト
    |
    v
Spring Security Filter Chain
    |
    +-- 公開エンドポイント (/, /products/**, /login, ...) --> 認証不要
    |
    +-- /mypage/**, POST /members/** --> ログイン必須
    |
    +-- /internal/** --> INTERNAL_APIロール必須
    |
    v
UserDetailsService
    |
    +-- membersテーブルからロード
    +-- member_status = 'active' のみ認証成功
    +-- BCryptでパスワード検証
```

**セッションタイムアウト:** 60分

## 5. 注文確定フロー

```
[Cart]-->[CheckoutMethod]-->[CheckoutInput]-->[CheckoutConfirm]-->[Order登録]
                                                        |
                                            +-----------------------+
                                            | 1. 注文番号採番       |
                                            | 2. orders INSERT      |
                                            | 3. order_items INSERT |
                                            | 4. 在庫接減処理       |
                                            | 5. カートクリア       |
                                            | 6. 注文完了メール送信 |
                                            | 7. 完了画面リダイレクト|
                                            +-----------------------+
```

## 6. バッチ処理アーキテクチャ

Spring Batchを利用し、毎時スケジュール実行（`0 0 * * * *`）:

```
Scheduler (毎時0分)
    |
    +-- RankingCalculationJob
    |       |
    |       +-- Step: ProductSalesAggregationStep
    |               過去1ヶ月の販売実績集計 -> popular_product_rankings更新
    |
    +-- RecommendationCalculationJob
            |
            +-- Step: RelatedProductCalculationStep
                    商品の類似度計算 -> recommended_related_products更新
```

## 7. Docker Compose構成（ローカル開発）

| サービス | イメージ | ポート | 用途 |
|---|---|---|---|
| db | postgres:16 | 5432 | アプリケーションDB |
| mailpit | axllent/mailpit:v1.20 | 1025(SMTP), 8025(WebUI) | メール受信テスト |
| dbgate | dbgate | 3000 | DB管理GUI |

**ローカル実行時:** `spring.profiles.active=local` で起動
