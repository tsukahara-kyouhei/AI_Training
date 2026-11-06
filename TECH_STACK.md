# 技術スタック調査結果

## 調査方針

依存関係定義だけでなく、ソースコード・設定・テンプレート・コンテナ定義で実際の利用を確認しました。「使用確認」は実装または実行設定で用途を確認できた技術、「定義のみ」は依存関係として登録されているものの、この調査では実際の利用箇所を確認できなかった技術を示します。バージョンが明記されていないものは「不明」としています。

| 分類 | 技術・バージョン | 状態 | 何の技術か／このプロジェクトでの役割 | 根拠ファイル |
|---|---|---|---|---|
| プログラミング言語 | Java 25（コンパイル設定） | 使用確認 | サーバーアプリケーションの実装言語。Spring Boot アプリ、Web コントローラー、サービス、データアクセス、バッチ処理を実装します。実際に起動するJDKのバージョンは環境依存ですが、Docker 実行イメージも Java 25 を指定しています。 | [`pom.xml`](pom.xml)、[`Dockerfile`](Dockerfile)、[`OfficeOrderApplication.java`](src/main/java/jp/co/skig/officeorder/OfficeOrderApplication.java) |
| プログラミング言語 | JavaScript（バージョン不明） | 使用確認 | ブラウザー側の動作を実装します。フォーム制御、商品詳細、カルーセルなどのスクリプトが配置されています。 | [`src/main/resources/static/js/`](src/main/resources/static/js/) |
| マークアップ／スタイル | HTML、CSS（バージョン不明） | 使用確認 | Thymeleaf テンプレートで画面を構成し、CSSで表示を整えます。独立したバージョン指定は確認できません。 | [`src/main/resources/templates/`](src/main/resources/templates/)、[`src/main/resources/static/css/`](src/main/resources/static/css/) |
| プログラミング言語 | SQL（方言／バージョン不明） | 使用確認 | PostgreSQL 用のスキーマ、初期データ、MyBatis クエリを記述します。 | [`sql/`](sql/)、[`src/main/resources/mappers/`](src/main/resources/mappers/) |
| アプリケーションフレームワーク | Spring Boot 4.0.3 | 使用確認 | Java アプリケーションの起動、自動設定、およびWeb・DB・メール等の連携基盤です。Maven の親POMでバージョンが指定されています。 | [`pom.xml`](pom.xml)、[`OfficeOrderApplication.java`](src/main/java/jp/co/skig/officeorder/OfficeOrderApplication.java) |
| Web／テンプレート | Spring Boot Web、Thymeleaf、Validation（各スターターのバージョンは Spring Boot 4.0.3 により管理） | 使用確認 | Web MVC による画面・リクエスト処理、サーバーサイドHTML描画、入力検証を担います。Thymeleaf属性を持つテンプレートとWebコードで利用を確認しました。 | [`pom.xml`](pom.xml)、[`src/main/resources/templates/`](src/main/resources/templates/)、[`src/main/java/jp/co/skig/officeorder/web/`](src/main/java/jp/co/skig/officeorder/web/) |
| セキュリティ | Spring Security（バージョンは Spring Boot 4.0.3 により管理） | 使用確認 | 認証・認可などWebアプリケーションのセキュリティ機能を担います。セキュリティ設定クラスがあります。 | [`pom.xml`](pom.xml)、[`SecurityConfig.java`](src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java) |
| バッチ | Spring Batch（バージョンは Spring Boot 4.0.3 により管理） | 使用確認 | ランキング等の定期／手動バッチ処理とジョブ実行を担います。バッチ設定・実行コードおよびスケジュール設定があります。 | [`pom.xml`](pom.xml)、[`BatchJobConfiguration.java`](src/main/java/jp/co/skig/officeorder/config/BatchJobConfiguration.java)、[`BatchExecutionConfig.java`](src/main/java/jp/co/skig/officeorder/config/BatchExecutionConfig.java)、[`application.yml`](src/main/resources/application.yml) |
| データアクセス | MyBatis Spring Boot Starter 4.0.0 | 使用確認 | Java の Mapper と XML SQL を連携し、アプリケーションからDBへ問い合わせます。 | [`pom.xml`](pom.xml)、[`src/main/java/jp/co/skig/officeorder/mapper/`](src/main/java/jp/co/skig/officeorder/mapper/)、[`src/main/resources/mappers/`](src/main/resources/mappers/)、[`application.yml`](src/main/resources/application.yml) |
| データアクセス | Spring JDBC（バージョンは Spring Boot 4.0.3 により管理） | 使用確認 | JDBC を使ったデータソース接続基盤を提供します。PostgreSQL接続設定と PostgreSQL JDBC ドライバーが定義されています。 | [`pom.xml`](pom.xml)、[`application.yml`](src/main/resources/application.yml) |
| データベース | PostgreSQL 18（Docker イメージのメジャーバージョン） | 使用確認 | アプリケーションのリレーショナルデータベースです。ComposeでDBコンテナを起動し、JDBC URLで接続します。Docker外で稼働するDBの実バージョンは不明です。 | [`docker-compose.yml`](docker-compose.yml)、[`application.yml`](src/main/resources/application.yml)、[`sql/`](sql/) |
| DB接続ドライバー | PostgreSQL JDBC Driver（バージョン不明） | 使用確認 | Javaアプリから PostgreSQL に JDBC 接続するドライバーです。依存関係は runtime scope で登録され、データソースのドライバー名にも指定されていますが、POMに個別バージョンはありません。 | [`pom.xml`](pom.xml)、[`application.yml`](src/main/resources/application.yml) |
| メール | Spring Boot Mail（バージョンは Spring Boot 4.0.3 により管理）、Mailpit 1.20 | 使用確認 | Spring側でメール送信を行い、ローカル環境では Mailpit がSMTPを受けてメール確認用UIを提供します。 | [`pom.xml`](pom.xml)、[`application.yml`](src/main/resources/application.yml)、[`docker-compose.yml`](docker-compose.yml)、[`src/main/java/jp/co/skig/officeorder/service/mail/`](src/main/java/jp/co/skig/officeorder/service/mail/) |
| ビルドツール | Apache Maven 3.9.12（Maven Wrapper設定） | 使用確認 | 依存関係解決、コンパイル、テスト、パッケージ作成を行います。Dockerビルドでも Maven を使います（Dockerイメージのタグは `3.9` 系までの指定）。 | [`pom.xml`](pom.xml)、[`mvnw`](mvnw)、[`mvnw.cmd`](mvnw.cmd)、[`.mvn/wrapper/maven-wrapper.properties`](.mvn/wrapper/maven-wrapper.properties)、[`Dockerfile`](Dockerfile) |
| ビルドプラグイン | Spring Boot Maven Plugin、Maven Compiler Plugin、Maven Surefire Plugin | 使用確認 | それぞれアプリケーションのパッケージ化、Javaコンパイル、テスト実行を担います。プラグイン個別のバージョン指定はなく、Maven親POM等による管理です。 | [`pom.xml`](pom.xml) |
| テスト | Spring Boot Starter Test、Spring Security Test（バージョンは Spring Boot 4.0.3 により管理） | 使用確認 | テスト用ライブラリです。JUnit Jupiter、Mockito等を利用するテストコードがあります。 | [`pom.xml`](pom.xml)、[`src/test/java/`](src/test/java/) |
| 設定処理 | Spring Boot Configuration Processor（バージョンは Spring Boot 4.0.3 により管理） | 使用確認 | `@ConfigurationProperties` に基づく設定メタデータをコンパイル時に生成する注釈プロセッサです。optional依存かつ annotation processor として定義されています。 | [`pom.xml`](pom.xml)、[`AppProperties.java`](src/main/java/jp/co/skig/officeorder/config/AppProperties.java) |
| ロギング | Logback（バージョン不明） | 使用確認 | アプリケーションログをコンソールとファイルに出力し、ローテーションします。設定ファイルから利用を確認できますが、依存関係上の個別バージョンは記載されていません。 | [`src/main/resources/logback-spring.xml`](src/main/resources/logback-spring.xml) |
| コンテナ実行環境 | Docker、Docker Compose（各バージョン不明） | 使用確認 | アプリ、PostgreSQL、Mailpit、DbGateをコンテナとして構築・起動します。Compose形式のバージョンや Docker Engine のバージョンはリポジトリ内に指定がありません。 | [`Dockerfile`](Dockerfile)、[`docker-compose.yml`](docker-compose.yml) |
| DB管理UI | DbGate（`latest` タグ、実バージョン不明） | 使用確認 | Composeで起動し、PostgreSQLの確認に使うDB管理UIです。 | [`docker-compose.yml`](docker-compose.yml) |
| OS／タイムゾーン | コンテナOSの詳細バージョン不明、Asia/Tokyo | 一部使用確認 | 実行コンテナのベースイメージは Eclipse Temurin 25 系です。Composeのアプリコンテナに `TZ=Asia/Tokyo` が設定されていますが、ホストOSは特定されていません。 | [`Dockerfile`](Dockerfile)、[`docker-compose.yml`](docker-compose.yml)、[`application.yml`](src/main/resources/application.yml) |

## 依存関係として登録されているが、用途の実使用を確認できなかったもの

| 技術 | 定義 | 状態と補足 | 根拠ファイル |
|---|---|---|---|
| `spring-boot-configuration-processor` の実行時利用 | optional依存 | 実行時ライブラリとしての利用は確認していません。一方、コンパイル時 annotation processor としての登録と設定プロパティクラスは確認できるため、上表では「設定処理」として使用確認に分類しました。 | [`pom.xml`](pom.xml)、[`AppProperties.java`](src/main/java/jp/co/skig/officeorder/config/AppProperties.java) |