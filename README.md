# OFFICE ORDER

OFFICE ORDER は、オフィス家具を扱う顧客向け EC サイトです。

本リポジトリで扱う範囲は、商品閲覧、会員登録、カート、注文、お問い合わせまでであり、
受注後の業務処理や管理画面は別システムの責務です。

## 技術スタック
- Java 21
- Spring Boot 3.5.11
- Spring Security
- Thymeleaf
- MyBatis
- PostgreSQL 16
- Docker Compose（ローカル補助用途）

## ローカル起動
### 前提
- Java 21
- `./mvnw` が実行できること
- 以下のどちらかを用意すること
  - Docker Desktop などで `docker compose` が使える
  - PostgreSQL 16 と `psql` がローカルに入っている

### 起動手順（Docker 利用）
1. インフラを起動
   `docker compose up -d`
2. アプリをローカル起動
   `./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"`
3. 確認先
   - アプリトップ: `http://localhost:8080/`
   - DB確認(DBGate): `http://localhost:3000`
   - メール確認(Mailpit): `http://localhost:8025`

### 起動手順（Docker なし）
1. PostgreSQL 16 に `office_order` DB と `office_order` ユーザーを作成
   - 接続先前提: `localhost:5432`
   - 既定ユーザー/パスワード: `office_order` / `office_order`
   - 例:
     ```sql
     CREATE ROLE office_order LOGIN PASSWORD 'office_order';
     CREATE DATABASE office_order OWNER office_order;
     ```
2. スキーマと seed を投入
   `./scripts/init-local-postgres.ps1`
3. アプリをローカル起動
   `./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"`
4. 確認先
   - アプリトップ: `http://localhost:8080/`

### Docker 全リセット手順
1. コンテナ、ボリューム、イメージ、orphan を削除
   `docker compose down --rmi all --volumes --remove-orphans`
2. Compose を再起動
   `docker compose up -d`

## ログ
- アプリログはコンソールと `log/office-order.log` の両方に出力されます
- ローテーション済みログは `log/archive/` に出力されます

## 初期データ
- 会員: 7件（有効会員 6件、退会済み 1件）
- 商品: 50件
- 注文: 過去1か月分を中心に約100件
- テスト会員の初期パスワード: `password`
- パスワードは Java 側で BCrypt ハッシュ化して扱う

### 代表的なログイン確認用アカウント
- `member01@example.com` / `password`
- `member02@example.com` / `password`
- `member04@example.com` / `password`

### ローカル再シード手順
#### Docker 利用時
1. Compose を停止
   `docker compose down`
2. PostgreSQL ボリュームのみ削除
   `docker volume rm office-order-postgres-data`
3. Compose を再起動
   `docker compose up -d`

#### Docker なし
- `./scripts/init-local-postgres.ps1` を再実行してください

## バッチ
- 売れ筋ランキング再計算とおすすめ関連商品再計算は、サーバー起動時に自動実行されます
- その後は毎時 0 分に自動実行されます
- `local` プロファイルでは手動実行用の内部エンドポイントも利用できます
  - `POST /internal/batch/jobs/popular-ranking/executions`
  - `POST /internal/batch/jobs/recommended-related/executions`
  - `GET /internal/batch/jobs`

### 手動バッチ実行
- `InternalBatchController` は `local` プロファイル時のみ有効です
- 売れ筋ランキング再計算
  `curl -X POST http://localhost:8080/internal/batch/jobs/popular-ranking/executions`
- おすすめ関連商品再計算
  `curl -X POST http://localhost:8080/internal/batch/jobs/recommended-related/executions`
- ジョブ状態確認
  `curl http://localhost:8080/internal/batch/jobs`
- レスポンスは `202 Accepted` と `executionId` を返します
- 実行中の同一ジョブを重複起動した場合は `409 Conflict` を返します

## システムエラーページ確認
- `local` プロファイルではシステムエラーページの手動確認用内部エンドポイントを利用できます
  - `http://localhost:8080/internal/test/errors/500`
