# デプロイ手順書

## 1. 前提条件

| ソフトウェア | バージョン |
|---|---|
| Java | 21 |
| Maven | 3.9.x（あるいは `./mvnw` 内蔵） |
| Docker | 24以上 |
| Docker Compose | v2 |
| PostgreSQL | 16 |

---

## 2. ローカル開発環境構築手順

### 2.1 リポジトリクローン

```powershell
git clone <repository-url>
cd office-order
```

### 2.2 Docker Compose起動

```powershell
docker-compose up -d
```

起動確認:
- DB: `localhost:5432`
- Mailpit WebUI: `http://localhost:8025`
- DbGate: `http://localhost:3000`

### 2.3 DB初期化

```powershell
# 初期化SQL実行（自動実行される場合は不要）
.\scripts\init-local-postgres.ps1
```

手動実行の場合:
```powershell
# スキーマ作成
psql -h localhost -U postgres -d office_order -f sql/schema/members.sql
psql -h localhost -U postgres -d office_order -f sql/schema/products.sql
# ...他スキーマファイルも同様

# 初期データ投入
psql -h localhost -U postgres -d office_order -f sql/seed/masters.sql
psql -h localhost -U postgres -d office_order -f sql/seed/products.sql
# ...
```

### 2.4 アプリ起動

```powershell
.\mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

ブラウザで `http://localhost:8080` を開く。

---

## 3. ビルド

```powershell
# テストをスキップしてjarビルド
.\mvnw package -DskipTests

# 作成されるjar
.\target\office-order-*.jar
```

---

## 4. 本番デプロイ手順 (参考)

```bash
# 1. ビルド
./mvnw package -DskipTests

# 2. jarを本番サーバーに転送
scp target/office-order-*.jar user@server:/opt/office-order/

# 3. 起動スクリプト (例)
java -jar /opt/office-order/office-order-*.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://db-host:5432/office_order \
  --spring.datasource.username=app_user \
  --spring.datasource.password=${DB_PASSWORD} \
  --spring.mail.host=smtp-host
```

---

## 5. 再起動・停止

```powershell
# ローカル停止
Ctrl+C (アプリ)
docker-compose down

# DBデータを維持したまま停止
docker-compose stop
```
