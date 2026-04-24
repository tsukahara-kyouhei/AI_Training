# FEAT-008: 技術スタック バージョンアップ要件定義

## 対象バージョン

| 技術スタック | 現行バージョン | 移行先バージョン |
|---|---|---|
| Java | 21 | 25 |
| Spring Boot | 3.5.11 (Spring Framework 6.x) | 4.0.3 (Spring Framework 7.x) |
| PostgreSQL | 16 | 18 |

---

## 1. Java 21 → 25

### 1.1 バージョンアップにおける変更点

Java 22〜25 にかけて、以下の主要な変更が行われた。

#### 削除・非互換

| 変更 | 導入バージョン | 影響度 |
|---|---|---|
| Security Manager の完全無効化 | JDK 24 (JEP 486) | 高 |
| `sun.misc.Unsafe` メモリアクセスメソッドの非推奨→警告 | JDK 23 (JEP 471) → JDK 24 (JEP 498) | 中 |
| JNI 使用時の警告出力 | JDK 24 (JEP 472) | 低 |
| ZGC 非世代別モードの削除 | JDK 24 (JEP 490) | 低 |
| String Templates（プレビュー）の撤回 | JDK 23 | 低 |
| Windows 32-bit x86 ポートの削除 | JDK 24-25 | なし |

#### 正式化された主要機能

| 機能 | プレビュー | 正式化 |
|---|---|---|
| Foreign Function & Memory API | JDK 21 | JDK 22 |
| 名前なし変数・パターン (`_`) | JDK 21 | JDK 22 |
| Stream Gatherers (`Stream.gather()`) | JDK 22 | JDK 24 |
| Class-File API | JDK 22 | JDK 24 |
| Scoped Values | JDK 21 | JDK 25 |
| Module Import Declarations (`import module`) | JDK 23 | JDK 25 |
| Flexible Constructor Bodies (`super()` 前の文) | JDK 22 | JDK 25 |
| Compact Object Headers (ヒープ省メモリ化) | JDK 24 | JDK 25 |
| Compact Source Files / Instance Main Methods | JDK 21 | JDK 25 |

#### ランタイム改善

- Virtual Threads が `synchronized` ブロックでピン留めされなくなった（JDK 24, JEP 491）
- Ahead-of-Time Class Loading & Linking による起動高速化（JDK 24-25）
- ポスト量子暗号アルゴリズム ML-KEM / ML-DSA の標準搭載（JDK 24）

### 1.2 本プロジェクトへの課題

| # | 課題 | 詳細 |
|---|---|---|
| J-1 | Mockito `-javaagent` 設定 | `pom.xml` の `maven-surefire-plugin` で Mockito の `-javaagent` を明示指定している。JDK 25 では動的エージェントへの制限が強化されており、Mockito のバージョンアップと設定変更が必要になる可能性がある |
| J-2 | 依存ライブラリの JDK 25 互換性 | MyBatis、Thymeleaf 等のサードパーティが内部で `sun.misc.Unsafe` や Security Manager を使用している場合、警告またはエラーが発生する。各ライブラリの対応状況を確認する必要がある |
| J-3 | GC フラグの確認 | JVM オプションで ZGC 関連フラグ（`-XX:-ZGenerational` 等）を指定している場合は削除が必要 |

### 1.3 修正が必要な箇所と具体的な修正方法

#### 1.3.1 `pom.xml` — Java バージョンの変更

```xml
<!-- 変更前 -->
<java.version>21</java.version>

<!-- 変更後 -->
<java.version>25</java.version>
```

#### 1.3.2 `pom.xml` — Mockito エージェント設定の確認・更新

```xml
<!-- maven-surefire-plugin の argLine を確認 -->
<!-- Mockito 5.x 以降は自動エージェント検出に対応。明示的 -javaagent が不要になる場合は削除 -->
<!-- Mockito のバージョンを最新に更新し、argLine の javaagent パスが正しいか確認 -->
```

#### 1.3.3 `docker-compose.yml` / CI 環境 — JDK ディストリビューション更新

JDK 25 対応のディストリビューション（Eclipse Temurin、Amazon Corretto 等）に更新する。

---

## 2. Spring Boot 3.5.11 → 4.0.3

### 2.1 バージョンアップにおける変更点

Spring Boot 4.0 は Spring Framework 7.0 基盤で、Jakarta EE 11 を最低要件とする。

#### 基盤変更

| 項目 | 変更内容 |
|---|---|
| Jakarta EE | 10 → 11（Servlet 6.1, Bean Validation 3.1） |
| Servlet コンテナ | Tomcat 11.0+ または Jetty 12.1+ が必須。Undertow は非対応 |
| JPA | 3.1 → 3.2（Hibernate ORM 7.1+） |
| Jackson | 2.x → 3.0 がデフォルト（パッケージが `com.fasterxml.jackson` → `tools.jackson` に変更） |
| HikariCP | 7.0 に更新 |
| Mockito | 5.20 に更新 |

#### 削除された API・機能

- `spring-jcl` モジュール（Apache Commons Logging 1.3.0 に置換）
- `javax.annotation` / `javax.inject` パッケージのサポート完全削除
- `ListenableFuture`（`CompletableFuture` に移行）
- OkHttp3 クライアントサポート
- Undertow 埋め込みサーバー
- MVC テーマ機能
- パスマッチングの `suffixPatternMatch`, `trailingSlashMatch`, `favorPathExtension`

#### 非推奨化

- `RestTemplate`（`RestClient` への移行を推奨）
- Jackson 2.x サポート（3.0 への移行を推奨）
- JUnit 4 テストサポート

#### 設定プロパティの変更

| 旧プロパティ | 新プロパティ |
|---|---|
| `management.tracing.enabled` | `management.tracing.export.enabled` |
| `spring.dao.exceptiontranslation.enabled` | `spring.persistence.exceptiontranslation.enabled` |

#### その他

- `HttpHeaders` が `MultiValueMap` を直接継承しなくなった（`asMultiValueMap()` 経由で取得）
- Null-safety アノテーションが JSR 305 から JSpecify に移行
- `SpringExtension` がテストメソッドスコープの `ExtensionContext` を使用するようになった

### 2.2 本プロジェクトへの課題

| # | 課題 | 詳細 |
|---|---|---|
| S-1 | **MyBatis Spring Boot Starter の互換性** | 現在 `mybatis-spring-boot-starter 3.0.4` を使用している。Spring Framework 7 / Jakarta EE 11 に対応した MyBatis Spring Boot Starter 4.x がリリースされていない場合、移行がブロックされる。リリース状況を確認し、未リリースの場合はスナップショットビルドまたは代替手段を検討する必要がある |
| S-2 | **Jackson 3.0 への移行** | プロジェクトで Jackson をカスタム構成・直接利用している箇所がある場合、パッケージ名の変更（`com.fasterxml.jackson` → `tools.jackson`）に伴う修正が必要。ただし本プロジェクトでは Thymeleaf によるサーバーサイドレンダリングが主であり、Jackson のカスタム設定は最小限と想定される |
| S-3 | **Thymeleaf の互換性** | Spring Framework 7 に対応した Thymeleaf バージョン（想定: 4.x）が必要。Spring Boot 4.0 の BOM で管理されるが、テンプレート構文の非互換がないか確認が必要 |
| S-4 | **Servlet コンテナの更新** | 組み込み Tomcat が 11.0+ に更新される。Spring Boot の BOM で自動管理されるが、Servlet 6.1 固有の挙動変更に注意 |
| S-5 | **Spring Security 7.0** | Spring Boot 4.0 は Spring Security 7.0 が基盤。現在のセキュリティ設定（`SecurityConfig.java`）はモダンな lambda DSL を使用しているが、Security 7.0 固有の API 変更を確認する必要がある |
| S-6 | **Spring Batch メタデータスキーマ** | Spring Batch 6.x 同梱時にメタデータスキーマが変更される可能性がある。`sql/schema/spring-batch-metadata.sql` の更新が必要 |

### 2.3 修正が必要な箇所と具体的な修正方法

#### 2.3.1 `pom.xml` — Spring Boot バージョンの変更

```xml
<!-- 変更前 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.11</version>
</parent>

<!-- 変更後 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>
```

#### 2.3.2 `pom.xml` — MyBatis Spring Boot Starter の更新

```xml
<!-- 変更前 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>

<!-- 変更後（Spring Boot 4.0 対応版がリリースされ次第） -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.x.x</version> <!-- 対応バージョン要確認 -->
</dependency>
```

#### 2.3.3 `application.yml` — プロパティ名の変更

本プロジェクトで使用中のプロパティに対する変更は現時点では不要（`management.tracing.enabled` 等は未使用）。ただし Spring Boot 4.0 リリースノートで追加のプロパティ変更が発表された場合は対応が必要。

#### 2.3.4 `SecurityConfig.java` — Spring Security 7.0 対応

現在のコードは最新の lambda DSL パターンを使用しており、大幅な変更は不要と想定される。Spring Security 7.0 の Migration Guide を確認し、個別の API 変更に対応する。

```java
// 確認対象: src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java
// - authorizeHttpRequests() の API シグネチャ変更
// - CORS 事前処理リクエストのデフォルト挙動変更への対応
```

#### 2.3.5 `BatchJobConfiguration.java` — Spring Batch 6.x 対応

```java
// 確認対象: src/main/java/jp/co/skig/officeorder/config/BatchJobConfiguration.java
// - JobBuilder / StepBuilder の API 変更確認
// - メタデータスキーマの差分確認と sql/schema/spring-batch-metadata.sql の更新
```

#### 2.3.6 Jackson 関連コードの確認

プロジェクト内で `com.fasterxml.jackson` を直接 import している箇所を検索し、`tools.jackson` への移行が必要か判断する。Spring Boot の自動構成経由のみで使用している場合は自動的に移行される。

#### 2.3.7 テストコードの更新

```xml
<!-- pom.xml: Mockito バージョンが Spring Boot BOM で管理される。
     surefire プラグインの argLine（Mockito javaagent）が
     新バージョンのパスと一致するか確認 -->
```

- `SpringExtension` の `ExtensionContext` スコープ変更により、カスタム `TestExecutionListener` を使用している場合は修正が必要（本プロジェクトでは該当なしと想定）

---

## 3. PostgreSQL 16 → 18

### 3.1 バージョンアップにおける変更点

PostgreSQL 17 および 18 で以下の主要な変更が行われた。

#### PostgreSQL 17 の主な変更

| 項目 | 変更内容 |
|---|---|
| 式インデックスの `search_path` 安全性 | 式インデックス・マテリアライズドビューで使用する関数に `search_path` 指定が必要 |
| カタログ列名変更 | `pg_collation.colliculocale` → `colllocale` 等 |
| `pg_stat_bgwriter` | `buffers_backend` 等が削除、`pg_stat_io` に移行 |
| `MERGE` 構文拡張 | `WHEN NOT MATCHED BY SOURCE`, `RETURNING` 句の追加 |
| `JSON_TABLE()` | `FROM` 句で使用可能に |
| SQL/JSON 関数 | `JSON()`, `JSON_EXISTS()`, `JSON_QUERY()`, `JSON_VALUE()` 等の追加 |
| パフォーマンス改善 | B-tree `IN` 検索の効率化、`GROUP BY` 並べ替え最適化、パラレル BRIN インデックス構築 |

#### PostgreSQL 18 の主な変更

| 項目 | 変更内容 |
|---|---|
| データチェックサム | `initdb` でデフォルト有効化 |
| `VACUUM`/`ANALYZE` | パーティションテーブルの子テーブルも自動処理。`ONLY` オプションで旧挙動 |
| `COPY FROM` CSV | `\.` が EOF として扱われなくなった |
| AFTER トリガー | イベントキュー時のロールで実行（実行時のロールではなくなった） |
| 仮想生成列 | `GENERATED ALWAYS AS (expr)` のデフォルトが `VIRTUAL` に変更 |
| `jsonb` null キャスト | `jsonb` の null をスカラーにキャストすると SQL `NULL` を返すように変更 |
| MD5 パスワード認証 | 非推奨化（SCRAM-SHA-256 への移行を推奨） |
| 非同期 I/O | `io_method` パラメータによる async I/O サブシステム |
| B-tree スキップスキャン | 先頭カラムなしでの複合インデックス利用が可能に |
| `uuidv7()` | タイムスタンプ順 UUID 生成関数の追加 |
| テンポラル制約 | `WITHOUT OVERLAPS` による期間重複防止制約 |
| `NOT ENFORCED` 制約 | CHECK 制約・外部キー制約の強制無効化 |

### 3.2 本プロジェクトへの課題

| # | 課題 | 詳細 |
|---|---|---|
| P-1 | **式インデックスの `search_path` 安全性 (PG17)** | `sql/schema/products.sql` で定義している `normalize_fullwidth()` 関数を式インデックスやマテリアライズドビューで使用している場合、関数定義時に `search_path` を設定する必要がある |
| P-2 | **`VACUUM`/`ANALYZE` の挙動変更 (PG18)** | パーティションテーブルを使用している場合、`VACUUM`/`ANALYZE` が子テーブルも処理するようになる。本プロジェクトではパーティションテーブルは未使用と想定されるが確認が必要 |
| P-3 | **データチェックサム (PG18)** | 新規 `initdb` でデフォルト有効。`docker-compose.yml` で `postgres:18` イメージに変更する際、既存データボリュームとの互換性に注意。クリーンインストールであれば問題なし |
| P-4 | **全文検索インデックス (PG18)** | ICU または builtin コレーションプロバイダを使用している場合、全文検索および `pg_trgm` インデックスの再構築が必要。本プロジェクトでは `translate()` 関数ベースの検索を使用しており、FTS を直接使用していないため影響は限定的 |
| P-5 | **MD5 パスワード認証の非推奨化 (PG18)** | `docker-compose.yml` の PostgreSQL コンテナでパスワード認証方式を確認。MD5 を使用している場合は SCRAM-SHA-256 への移行が必要 |
| P-6 | **仮想生成列のデフォルト変更 (PG18)** | 今後 `GENERATED ALWAYS AS` を使用する場合、`STORED` を明示的に指定しないと `VIRTUAL`（ディスク非保存）になる。既存スキーマには影響なし |

### 3.3 修正が必要な箇所と具体的な修正方法

#### 3.3.1 `docker-compose.yml` — PostgreSQL バージョンの変更

```yaml
# 変更前
image: postgres:16

# 変更後
image: postgres:18
```

> **注意**: 既存のデータボリュームがある場合は `pg_upgrade` またはダンプ＆リストアが必要。開発環境ではボリュームを削除して再作成するのが最も簡便。

#### 3.3.2 `sql/schema/products.sql` — `normalize_fullwidth()` 関数の `search_path` 設定

```sql
-- 変更前
CREATE OR REPLACE FUNCTION normalize_fullwidth(input TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
...
$$;

-- 変更後（SET search_path を追加）
CREATE OR REPLACE FUNCTION normalize_fullwidth(input TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SET search_path = public
AS $$
...
$$;
```

#### 3.3.3 パスワード認証方式の確認と移行

```yaml
# docker-compose.yml で SCRAM-SHA-256 を明示的に設定
environment:
  POSTGRES_PASSWORD: office_order
  POSTGRES_HOST_AUTH_METHOD: scram-sha-256
  POSTGRES_INITDB_ARGS: "--auth-host=scram-sha-256"
```

#### 3.3.4 JDBC ドライバの更新

PostgreSQL JDBC ドライバは Spring Boot の BOM で管理されている。Spring Boot 4.0.3 の BOM に含まれるドライババージョンが PostgreSQL 18 に対応しているか確認する（通常は後方互換性があるため問題なし）。

---

## 4. バージョンアップ作業の推奨順序

1. **PostgreSQL 16 → 18**: データベースのバージョンアップは他のコンポーネントへの影響が最も少ない。先行して実施可能
2. **Java 21 → 25**: JDK の更新。ビルド・テストの動作確認を行う
3. **Spring Boot 3.5.11 → 4.0.3**: 最も影響範囲が広い。MyBatis Starter の対応状況がブロッカーとなる可能性があるため、最後に実施

## 5. リスクまとめ

| リスク | 影響度 | 対策 |
|---|---|---|
| MyBatis Spring Boot Starter 4.x 未リリース | **高** | リリース状況を監視。未対応の場合は Spring Boot 4.0 への移行を延期、または mybatis-spring の直接構成を検討 |
| Jackson 3.0 パッケージ変更 | 中 | プロジェクト内の Jackson 直接利用箇所を事前調査 |
| Spring Batch メタデータスキーマ変更 | 中 | 新スキーマとの差分を確認し `spring-batch-metadata.sql` を更新 |
| Mockito エージェント制限 (JDK 25) | 低 | Mockito を最新版に更新、surefire 設定を調整 |
| `normalize_fullwidth()` の search_path | 低 | 関数定義に `SET search_path` を追加 |
