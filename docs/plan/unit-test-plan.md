# 単体テスト計画書

## 1. 目的

本計画書は office-order プロジェクトにおける単体テストの方針・範囲・実装基準を定め、品質の均一化と継続的な開発効率向上を目的とする。

---

## 2. テスト方針概要

| 項目 | 方針 |
|---|---|
| テストフレームワーク | JUnit 5 + Mockito + AssertJ（既存構成を継続） |
| カバレッジ目標 | **行カバレッジ 80% 以上**（サービス層・リポジトリ層は 85% 以上を推奨） |
| テスト対象スコープ | Controller 〜 Mapper の全層 |
| テストダブル方針 | 外部依存（DB・メール等）は原則モック化／Mapper 層のみ Testcontainers を使用 |
| パフォーマンス観点 | JUnit 5 `assertTimeout` / `@Timeout` を各層主要処理に付与 |
| テストメソッド命名 | `メソッド名_条件_期待結果`（英語スネークケース）＋ `@DisplayName` に日本語記述 |

---

## 3. 対象スコープと除外スコープ

### 3.1 対象

| 層 | 対象クラス例 |
|---|---|
| Controller | `CatalogController`, `CartController`, `OrderController`, `AuthController` |
| Service | `ProductListSearchService`, `ProductFilterOptionService`, `OrderService`, `MemberService`, `NotificationMailService` |
| Repository | `ProductRepository`, `ProductFilterOptionRepository`, `OrderRepository`, `MemberRepository`, `CartRepository` |
| Mapper（MyBatis XML） | `ProductMapper`, `OrderMapper`, `MemberMapper`, `CartMapper` |
| Util | `TextNormalizer` |
| Model（ロジックあり） | `ProductSearchCondition`, `ProductListPage`, `ProductCategory` |

### 3.2 除外

- `@Configuration` クラス（`SecurityConfig`, `BatchJobConfiguration` 等）
- Spring Boot 起動クラス（`OfficeOrderApplication`）
- getter/setter のみの単純な DTO・record フィールド（Coverage 集計上は計上するが専用テスト不要）

---

## 4. 各層のテスト方針

### 4.1 Controller 層

- **方式**: `@WebMvcTest` + `MockMvc`
- Spring Security の認証・認可を含めたリクエスト/レスポンスの HTTP レベルで検証する。
- Service 層は `@MockBean` でモック化する。
- 検証観点:
  - HTTP ステータスコード（200, 302, 400, 401, 403 等）
  - リダイレクト先 URL
  - `Model` / `Flash attribute` への格納値
  - バリデーションエラー時のビュー遷移
  - `@WithMockUser` / `@WithAnonymousUser` を使った認証状態別の動作

### 4.2 Service 層

- **方式**: Spring コンテキストなし、`mock()` でインスタンス生成、テスト対象は `new` で直接生成
- 既存テストのスタイルを踏襲する（`@BeforeEach setUp()`）。
- 検証観点:
  - 正常系・境界値・異常系（`null`, 空リスト, 0 件等）の全パターン
  - Repository への委譲呼び出し回数・引数（`verify`）
  - 例外の型・メッセージ（`assertThatThrownBy`）

### 4.3 Repository 層

- **方式**: Spring コンテキストなし、Mapper は `mock()` 化
- SQL パラメータ組み立て（`buildSearchParams()` 等）を中心に検証する。
- Mapper XML の挙動確認は Mapper 層テスト（§4.4）で行う。

### 4.4 Mapper 層（MyBatis XML）

- **方式**: `@MybatisTest` + **Testcontainers（PostgreSQL）**
- Docker 上の実 PostgreSQL を使用することで `normalize_fullwidth` 等の PostgreSQL 固有関数も正確に検証できる。
- `@Testcontainers` + `@Container` でコンテナを共有し、起動コストを最小化する（クラス単位で 1 コンテナ）。
- 検証観点:
  - 各 SELECT クエリで期待件数・期待値が返ること
  - 動的 SQL（`<if>`, `<where>`, `<foreach>`）の組み合わせパターン
  - INSERT / UPDATE / DELETE の件数・実データ反映
  - `normalize_fullwidth()` 呼び出しを含む検索クエリ

### 4.5 Util 層

- **方式**: Spring コンテキストなし、純粋 JUnit 5
- `@ParameterizedTest` + `@CsvSource` で多数の入力パターンを網羅する。

---

## 5. パフォーマンス観点のテスト

単体テスト内で処理時間の上限をアサーションとして設定し、CI で継続的に監視する。

### 5.1 使用手段

JUnit 5 標準の `assertTimeout` / `@Timeout` を使用する（追加ライブラリ不要）。

```java
// 例: assertTimeout（テスト本体）
@Test
void normalize_largeInput_completesWithinLimit() {
    assertTimeout(Duration.ofMillis(100), () -> {
        TextNormalizer.toFullWidth(largeString);
    });
}

// 例: @Timeout（メソッドレベル）
@Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
@Test
void buildCondition_complexFilter_completesWithinLimit() { ... }
```

### 5.2 タイムアウト基準値

| 対象 | 上限値の目安 | 備考 |
|---|---|---|
| `TextNormalizer.toFullWidth()` | 100ms | 1,000 文字入力 |
| `ProductListSearchService.buildCondition()` | 50ms | フルパラメータ設定時 |
| `ProductRepository.buildSearchParams()` | 50ms | 全絞り込み条件設定時 |
| Mapper SELECT（Testcontainers） | 500ms | インデックス利用クエリ |
| Mapper SELECT（複雑な動的 SQL） | 1,000ms | UNION / サブクエリ含む |
| `OrderService.generateOrderNumber()` | 50ms | シーケンス採番 |

> **注意**: Testcontainers 上の実行はコンテナ起動後の DB 安定待ち時間を含む。タイムアウト値は CI 環境で CI スパイク（GC・リソース競合）の影響を吸収できる余裕を持たせる。
> 基準値は初回計測結果をもとに調整し、本ドキュメントを更新すること。

### 5.3 パフォーマンス劣化検知フロー

1. CI（`mvn test`）実行時に `@Timeout` / `assertTimeout` がタイムアウトを検知
2. テスト失敗としてビルドを止める
3. 開発者はプロファイリングで原因（N+1、正規表現バックトラック等）を特定・修正する

---

## 6. テストダブル（モック）利用基準

| 依存先 | 方針 |
|---|---|
| Service → Repository | `mock()` でモック化 |
| Controller → Service | `@MockBean` でモック化 |
| Service → Mapper | `mock()` でモック化（Repository 経由の場合も同様） |
| Mapper → DB | Testcontainers（§4.4 参照） |
| Service → MailService | `mock()` でモック化 |
| Service → BatchService | `mock()` でモック化 |

---

## 7. テストコード規約

### 7.1 ファイル配置

```
src/test/java/jp/co/skig/officeorder/
├── web/
│   └── CatalogControllerTest.java
├── service/
│   ├── product/
│   └── order/
├── repository/
├── mapper/
│   └── ProductMapperTest.java
└── util/
    └── TextNormalizerTest.java
```

### 7.2 命名規則

| 要素 | 規約 | 例 |
|---|---|---|
| クラス名 | `{対象クラス名}Test` | `OrderServiceTest` |
| メソッド名 | `{メソッド名}_{条件}_{期待結果}`（英語スネーク） | `generateOrderNumber_sequence101_returnsFormattedNumber` |
| @DisplayName | 日本語で条件・期待結果を記述 | `@DisplayName("シーケンス 101 の場合 'ORD-00101' を返す")` |

### 7.3 テスト構造

```java
class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderService sut;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        sut = new OrderService(orderRepository);
    }

    // --- generateOrderNumber ---

    @Test
    @DisplayName("シーケンス 101 の場合 'ORD-00101' を返す")
    void generateOrderNumber_sequence101_returnsFormattedNumber() {
        when(orderRepository.nextSequence()).thenReturn(101L);
        assertThat(sut.generateOrderNumber()).isEqualTo("ORD-00101");
    }
}
```

### 7.4 アサーション

- `assertThat(actual).isEqualTo(expected)` など AssertJ を使用する。
- 例外検証は `assertThatThrownBy(() -> sut.method()).isInstanceOf(X.class).hasMessageContaining("...")` を使用する。
- 引数検証は `verify(mock).method(argThat(...))` または `ArgumentCaptor` を用いる。

---

## 8. カバレッジ計測

### 8.1 ツール

JaCoCo（Spring Boot starter-test に同梱済み）を使用する。

### 8.2 設定例（`pom.xml` への追記）

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 8.3 除外設定

以下は JaCoCo の計測対象から除外する。

```xml
<configuration>
    <excludes>
        <exclude>jp/co/skig/officeorder/config/**</exclude>
        <exclude>jp/co/skig/officeorder/OfficeOrderApplication.class</exclude>
    </excludes>
</configuration>
```

---

## 9. 依存ライブラリの追加

本計画に対応するため `pom.xml` への追記が必要な依存関係を以下に示す。

```xml
<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<!-- MyBatis Test -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter-test</artifactId>
    <version>3.0.3</version>
    <scope>test</scope>
</dependency>
```

> `testcontainers` のバージョンは Spring Boot BOM が管理するため `<version>` 指定不要。

---

## 10. CI 統合

| フェーズ | 内容 |
|---|---|
| `mvn test` | 全単体テスト実行＋カバレッジ計測 |
| カバレッジ違反 | 80% 未満の場合ビルド失敗（JaCoCo check ゴール） |
| タイムアウト違反 | `@Timeout` / `assertTimeout` 超過の場合テスト失敗 |
| レポート出力 | `target/site/jacoco/index.html` に HTML レポート生成 |

---

## 11. 優先実装クラス一覧

テスト未実装クラスのうち、下記を優先して実装する。

| 優先度 | クラス | 理由 |
|---|---|---|
| 高 | `OrderService` | 注文確定・金額計算等のビジネスコア |
| 高 | `ProductMapper`（XML） | 動的 SQL・PostgreSQL 固有関数の正確性確認 |
| 高 | `CatalogController` | 認証・バリデーション・リダイレクトの網羅 |
| 中 | `MemberService` | 会員登録・認証ロジック |
| 中 | `NotificationMailService` | メールテンプレート・宛先検証 |
| 中 | `CartRepository` | カート操作のパラメータ組み立て |
| 低 | `BatchJobService` | バッチ処理（統合テストと重複しがち） |

---

## 改訂履歴

| 日付 | 版 | 変更内容 |
|---|---|---|
| 2026-04-16 | 1.0 | 初版作成 |
