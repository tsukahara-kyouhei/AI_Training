# IBM Db2 12.1.5.0 手動検証手順

## 1. 目的と前提

本書は、現在のPostgreSQL用SQLファイル群とデータベース名`office_order`を、IBM Db2 Community Edition 12.1.5.0で手動検証するための手順です。SQLの自動変換や既存ファイルの書き換えは行いません。

調査日: 2026年11月17日

- 検証イメージ: `icr.io/db2_community/db2:12.1.5.0`
- 検証は既存PostgreSQL環境と分離した一時コンテナ・一時ボリュームで行います。
- 現在の[`docker-compose.yml`](../../docker-compose.yml)はPostgreSQL 18用です。本書の検証用Compose例は別ファイルに置き、既存ComposeとPostgreSQL用ボリュームを変更しません。
- IBMはDb2 Community Editionコンテナを非本番用途・サポート対象外として案内しています。検証データ以外を保存しないでください。

## 2. 検証用Db2コンテナを起動する

既存の[`docker-compose.yml`](../../docker-compose.yml)を変更せず、以下を`docker-compose.db2-validation.yml`という一時ファイルとして手動で用意します。`sql/db2-validation`には、後述の手順で作成したDb2向けの作業コピーだけを置きます。既存のPostgreSQL用SQLをそのままマウント・実行しないでください。

```yaml
services:
  db2-validation:
    image: icr.io/db2_community/db2:12.1.5.0
    container_name: db2-validation
    privileged: true
    ports:
      - "50000:50000"
    environment:
      LICENSE: accept
      DB2INST1_PASSWORD: ${DB2INST1_PASSWORD:?Set DB2INST1_PASSWORD}
    volumes:
      - db2-validation-data:/database
      - ./sql/db2-validation:/validation:ro

volumes:
  db2-validation-data:
```

この例では、`DBNAME`を設定せず、データベースをCLPから手動作成します。IBMの旧来のDb2コンテナ起動例では、`DBNAME`を省略すると初期データベースを作成せず、後から作成できると説明されています。一方、現行の12.1ドキュメントは、起動時にインスタンス／データベースを構成できるイメージを12.1.6以降として説明しています。**12.1.5.0での`DBNAME`環境変数の挙動は未確認**のため、この検証では環境変数に依存しない手動作成を先に行います。

Windows PowerShellでは、Composeを実行するセッションに`DB2INST1_PASSWORD`を設定してから起動します。パスワードをComposeファイルやGit管理対象へ書き込まないでください。

```powershell
docker compose -f .\docker-compose.db2-validation.yml up -d
docker compose -f .\docker-compose.db2-validation.yml ps
docker compose -f .\docker-compose.db2-validation.yml logs -f db2-validation
```

ログで`Setup has completed`を確認したら、ログ表示を`Ctrl+C`で終了します。コンテナを停止する操作ではありません。初回起動完了前に次へ進まないでください。

`docker-compose.db2-validation.yml`と検証用SQLディレクトリは、この手順を実施する際に別途用意するものです。本書作成時点では作成していません。

## 3. `office_order`の作成・接続を検証する

コンテナ内でDb2インスタンス所有者としてCLPを実行します。

```powershell
docker compose -f .\docker-compose.db2-validation.yml exec db2-validation bash
```

コンテナ内で次を実行します。

```bash
su - db2inst1
db2 list db directory
```

一覧に`office_order`がすでにある場合は、重複作成せず接続確認へ進みます。ない場合は次を実行します。

```bash
db2 create database office_order
db2 list db directory
db2 connect to office_order
db2 connect reset
db2 terminate
```

確認項目:

1. `CREATE DATABASE`がエラーなく完了する。
2. `LIST DB DIRECTORY`に作成したデータベース名／別名が表示される。
3. `CONNECT TO office_order`が成功する。

IBMのCREATE DATABASE資料では、データベース名は一意で、少なくとも空白を含まないことが示されています。Db2の通常SQL識別子では英字・数字・アンダースコアが使え、小文字は処理時に大文字へ変換されます。`office_order`は空白を含まず、この一般的な文字種の規則には合致します。ただし、一般識別子の規則だけでデータベース名固有の全制限を確定できません。今回参照した資料では最大長を確認できなかったため、**12.1.5.0での実際の作成結果を最終判断**とし、失敗した場合はエラーコードとメッセージを記録してください。

`DBNAME=office_order`によるコンテナ起動時の自動作成も確認したい場合は、上記のCLP試験とは分けて、**新しい検証専用コンテナと空のボリューム**で試します。既存ボリュームを使い回さないでください。

## 4. SQLファイルの調査・変換ポイント

現在の[`sql/init/init.sql`](../../sql/init/init.sql)は`psql`のメタコマンドを含み、Db2 CLPへ直接渡せません。`sql/schema`と`sql/seed`のファイルもPostgreSQL固有の構文を含むため、Db2向けに手動で作成した作業コピーを用意します。

| 現在の構文・ファイル | 検証・修正のポイント |
| --- | --- |
| `\echo`、`\set`、`\ir`（`sql/init/init.sql`、`sql/seed/*.sql`、`sql/seed/test-data/post-seed-check.sql`） | `psql`専用コマンドです。SQLファイルから除き、CLPコマンドまたは手動の実行手順で置き換えます。 |
| `BIGSERIAL`（例: `sql/schema/coupons.sql`） | Db2向けのIDENTITY列として定義し直します。 |
| `GENERATED ... AS IDENTITY`および固定IDを含むseed | Db2側のIDENTITY定義と、明示的IDの挿入可否を検証します。`GENERATED ALWAYS`列に固定IDを挿入する方式と、その後の採番位置の整合を確認してください。 |
| `TIMESTAMPTZ`、`NOW() - INTERVAL ...`、`MAKE_INTERVAL`（schema／test-data） | Db2側の型・日時演算に置き換えます。タイムゾーンを保持する必要がある列は、単純な型置換で意味が変わらないか確認します。 |
| 正規表現演算子`~`（例: `sql/schema/members.sql`、`products.sql`） | Db2で使える制約／関数に置き換えるか、アプリケーション側の入力検証へ移すか決定し、同じ境界値で試験します。 |
| `ON CONFLICT`（`sql/seed/masters.sql`） | Db2向けに`MERGE`等の重複時処理へ置き換え、seedを再実行したときの冪等性を確認します。 |
| `CREATE INDEX IF NOT EXISTS`、部分インデックス、式インデックス（例: `sql/schema/members.sql`、`products.sql`） | Db2 12.1.5.0での構文・機能可否を個別に確認し、必要なら通常のインデックスや別の検索設計に変更します。 |
| `generate_series`、`ARRAY[...]`、`LATERAL`、`ON TRUE`、`::TEXT`（`sql/seed/test-data/content.sql`） | Db2向けの行生成・結合・型変換に書き換える必要があります。テスト用データは、固定行のINSERTへ置き換える案もあります。 |
| `sql/schema/spring-batch-metadata.sql` | Spring BatchのDb2向けメタデータスキーマと照合します。PostgreSQL版をそのまま適用しないでください。 |

上記は現在のファイルで確認できた代表例で、全差分の一覧ではありません。Db2向け作業コピーを作成後、ファイルごとにCLPで実行してエラーを記録します。

## 5. Db2向けSQLを手動実行する

1. 既存の`sql`を直接編集せず、`sql/db2-validation`にDb2向けのDDL／seedの作業コピーを手動で作成します。
2. `sql/init/init.sql`に書かれた依存順をベースに、Db2で使える実行順の一覧を作ります。
3. Db2コンテナ内で`db2inst1`に切り替え、データベースへ接続します。
4. Db2 CLPから、変換済みファイルを**1ファイルずつ**適用します。エラーが出た時点で後続を実行せず、エラーコードと該当SQLを記録します。
5. DDLをすべて適用できた後にseedを実行し、テーブル件数、外部キー、アプリで使用する主要データを確認します。
6. 重複時処理を修正したseedについて、再実行して二重登録や制約違反が起きないことを確認します。

現在の初期化ファイルに記載された適用順は次のとおりです。

1. **スキーマ:** `masters.sql` → `products.sql` → `members.sql` → `orders.sql` → `coupons.sql` → `spring-batch-metadata.sql` → `content.sql` → `batch.sql`
2. **seed:** `masters.sql` → `products.sql` → `members.sql` → `orders.sql` → `content.sql`
3. 現在の`seed/products.sql`、`seed/members.sql`、`seed/orders.sql`、`seed/content.sql`は`test-data`を`\ir`で読み込みます。Db2版ではその内容も実行対象に含めるか、別ファイルとして順序に追加します。`post-seed-check.sql`も`psql`コマンドを含むため、Db2用の確認SQLに置き換える必要があります。

CLPの実行例（`/validation`はCompose例でマウントした検証用ディレクトリ）:

```bash
db2 connect to office_order
db2 -s -t -v -f /validation/schema/masters.sql
db2 -s -t -v -f /validation/schema/products.sql
```

ファイルを適用したら次のファイルへ進みます。ここで指定するファイルはDb2向けに手動変換した作業コピーであり、現在のPostgreSQL版ファイルではありません。`-s`を指定して最初のSQLエラーで実行を止め、成功・失敗したファイルとSQLCODEを記録します。

## 6. 結果記録と終了

最低限、次を記録してください。

- イメージタグ、Rancher Desktopのバージョン、ホストOS／CPUアーキテクチャ
- `office_order`の作成結果、`LIST DB DIRECTORY`の結果、接続結果とSQLCODE
- 各DDL／seedファイルの実行結果、修正箇所、seed再実行結果
- Db2の起動ログ、初回起動に要した時間、データ永続化の確認結果
- 未確認の構文、次に必要なアプリケーション／Mapper／Spring Batchの修正

検証コンテナを停止するときは、まず次を実行します。

```powershell
docker compose -f .\docker-compose.db2-validation.yml down
```

`down -v`は検証ボリュームを削除するため、保持が不要と確認できるまで実行しないでください。既存のPostgreSQL用ボリューム名`office-order-postgres-data-v18`はこの手順では使用・削除しません。

## 7. 根拠資料

- [IBM Docs: Db2 Community Edition for Docker](https://www.ibm.com/docs/en/db2/12.1.x?topic=deployments-db2-community-edition-docker)
- [IBM Db2 Communityコンテナの起動例・環境変数](https://hub.docker.com/r/ibmcom/db2)（旧版の説明を含むため、12.1.5.0固有の動作は別途検証）
- [IBM Container Registry: Db2 Communityイメージタグ](https://icr.io/v2/db2_community/db2/tags/list)
- [IBM Docs: CREATE DATABASE command](https://www.ibm.com/docs/en/db2/12.1?topic=commands-create-database)
- [IBM Docs: Identifiers](https://www.ibm.com/docs/en/db2/12.1.x?topic=elements-identifiers)
