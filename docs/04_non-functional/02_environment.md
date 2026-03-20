# 環境定義書

## 1. 環境一覧

| 環境 | Spring Profile | DB | メール | 終わり方 |
|---|---|---|---|---|
| ローカル開発 | `local` | PostgreSQL（Docker Compose） | Mailpit | `docker-compose up` |
| 本番 | `prod` | PostgreSQL（外部サーバー） | 実際SMTP | 本番マシンでjar実行 |

---

## 2. 共通設定（application.yml）

| 設定キー | 値 | 説明 |
|---|---|---|
| `server.port` | 8080 | サーバーポート |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/office_order | DB接続 |
| `spring.datasource.driver-class-name` | org.postgresql.Driver | JDBCDriver |
| `spring.thymeleaf.cache` | false (ローカル) | テンプレートキャッシュ |
| `server.servlet.session.timeout` | 60m | セッションタイムアウト |
| `app.cart.max-quantity-per-item` | 99 | カート最大数量 |

---

## 3. ローカル環境の設定

`application-local.yml` で `application.yml` を上書き:

| 設定キー | ローカル値 |
|---|---|
| `spring.mail.host` | localhost |
| `spring.mail.port` | 1025 (Mailpit) |
| `logging.level.jp.co.skig` | DEBUG |
| `spring.thymeleaf.cache` | false |

---

## 4. Docker Compose構成（ローカル）

| コンテナー | イメージ | ポート | 用途 |
|---|---|---|---|
| db | postgres:16 | 5432 | アプリケーションDB |
| mailpit | axllent/mailpit:v1.20 | 1025/8025 | SMTPテスト/WebUI |
| dbgate | dbgate | 3000 | DB管理GUI |

**起動手順:**
```powershell
# Docker Compose起動
docker-compose up -d

# Spring Boot起動
.\mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 5. 本番環境要件

| 項目 | 要件 |
|---|---|
| Java | 21 (LTS) |
| PostgreSQL | 16 |
| メモリ | 512MB以上 |
| タイムゾーン | Asia/Tokyo |
| SMTP | 事業者向けSMTPサーバー |
