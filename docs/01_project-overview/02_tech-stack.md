# 技術スタック

## 1. 言語・ランタイム

| 技術 | バージョン | 用途 |
|---|---|---|
| Java | 21 | アプリケーション開発言語 |

## 2. フレームワーク

| 技術 | バージョン | 用途 |
|---|---|---|
| Spring Boot | 3.5.11 | アプリケーションフレームワーク |
| Spring Web (MVC) | Spring Boot 同梱 | Webアプリケーション基盤 |
| Spring Security | Spring Boot 同梱 | 認証・認可 |
| Spring Batch | Spring Boot 同梱 | バッチ処理基盤 |
| Spring Mail | Spring Boot 同梱 | メール送信 |
| Spring Validation | Spring Boot 同梱 | 入力バリデーション（Bean Validation） |
| Spring JDBC | Spring Boot 同梱 | データベース接続基盤 |

## 3. テンプレートエンジン・UI

| 技術 | バージョン | 用途 |
|---|---|---|
| Thymeleaf | Spring Boot 同梱 | サーバーサイドHTMLテンプレート |
| HTML5 / CSS3 | - | 画面マークアップ・スタイリング |
| JavaScript | - | クライアントサイド処理 |

## 4. データベース・ORM

| 技術 | バージョン | 用途 |
|---|---|---|
| PostgreSQL | 16 | リレーショナルデータベース |
| MyBatis | 3.0.4 (mybatis-spring-boot-starter) | SQLマッパー（ORM） |

## 5. 開発ツール・インフラ

| 技術 | バージョン | 用途 |
|---|---|---|
| Docker Compose | - | ローカル開発環境構築 |
| Mailpit | v1.20 | 開発用メールサーバー（SMTP モック） |
| DbGate | latest | データベース管理Web UI |
| Maven | Maven Wrapper 同梱 | ビルド・依存管理 |

## 6. テスト

| 技術 | バージョン | 用途 |
|---|---|---|
| Spring Boot Test | Spring Boot 同梱 | 統合テスト基盤 |
| Spring Security Test | Spring Boot 同梱 | セキュリティテスト |
| Mockito | Spring Boot 同梱 | モック・スタブ |

## 7. その他ライブラリ

| 技術 | バージョン | 用途 |
|---|---|---|
| Spring Boot Configuration Processor | Spring Boot 同梱 | カスタム設定プロパティのメタデータ生成 |

## 8. 主要設定値

### アプリケーション設定

| 設定項目 | 値 | 説明 |
|---|---|---|
| サーバーポート | 8080 | HTTPリッスンポート |
| セッションタイムアウト | 60分 | HTTPセッション有効期限 |
| タイムゾーン | Asia/Tokyo | アプリケーション基準タイムゾーン |

### データベース設定

| 設定項目 | 値 |
|---|---|
| JDBC URL | jdbc:postgresql://localhost:5432/office_order |
| ユーザー名 | office_order |
| DB名 | office_order |

### MyBatis設定

| 設定項目 | 値 | 説明 |
|---|---|---|
| マッパーファイル | classpath:/mappers/*.xml | XMLマッパーの格納場所 |
| キャメルケース変換 | 有効 | DB列名のスネークケースを自動変換 |

### メール設定

| 設定項目 | 値 | 説明 |
|---|---|---|
| SMTPホスト | localhost | メールサーバー |
| SMTPポート | 1025 | Mailpit のSMTPポート |
| From | no-reply@office-order.local | 送信元アドレス |
| Reply-To | support@office-order.local | 返信先アドレス |

### バッチ設定

| 設定項目 | 値 | 説明 |
|---|---|---|
| 自動実行 | 無効 (job.enabled: false) | 起動時の自動ジョブ実行は無効 |
| 定時実行 | 毎時0分 (0 0 * * * *) | ランキング・レコメンド再計算 |

### カート設定

| 設定項目 | 値 | 説明 |
|---|---|---|
| Cookie セキュアモード | auto | HTTPS検出による自動切替 |
