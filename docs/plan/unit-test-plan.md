# 単体テスト計画

| 項目 | 内容 |
|---|---|
| プロジェクト | OFFICE ORDER（office-order） |
| 対象フェーズ | 開発フェーズ全般（継続的に適用） |
| 作成日 | 2026-04-14 |
| 最終更新 | 2026-04-14 |

---

## 1. 目的・位置づけ

本計画書は、OFFICE ORDER プロジェクトにおける単体テスト（UT）の方針・範囲・完了基準を定める。

テスト全体の中での位置づけは以下のとおり。

```
[単体テスト (UT)]   ← 本文書の対象
    ↓
[結合テスト (IT)]   後続フェーズで別計画書を策定
    ↓
[システムテスト (ST)]
```

単体テストの主な目的：
- Service 層のビジネスロジック（分岐・計算・変換）の正確性をコード変更のたびに自動検証する
- リグレッションを早期に検出し、手戻りコストを最小化する
- 仕様の「実行可能なドキュメント」としてテストコードを機能させる

---

## 2. 前提・用語定義

| 用語 | 定義 |
|---|---|
| 単体テスト (UT) | Spring Context を起動せず、Mockito でコラボレータをモックしてクラス単体を検証するテスト |
| 結合テスト (IT) | Spring Context もしくは実 DB を使用して複数コンポーネントを組み合わせて検証するテスト（本計画の対象外） |
| SUT | System Under Test。テスト対象のクラスインスタンス |
| テストクラス | SUT に 1:1 で対応する JUnit 5 テストクラス |
| カバレッジ (C0) | 行カバレッジ（Line Coverage）。実行された行数 / 全行数 |

---

## 3. テスト対象スコープ

### 3.1 対象：Service 層

テスト対象は `jp.co.skig.officeorder.service` パッケージ配下の全 Service クラス。

| # | クラス名 | パッケージ | 行数 | 主な責務 |
|---|---|---|---:|---|
| 1 | `AnnouncementService` | `announcement` | 48 | お知らせ取得件数の切り替え |
| 2 | `BatchExecutionService` | `batch` | 313 | バッチ実行制御・状態管理 |
| 3 | `BatchJobService` | `batch` | 223 | バッチジョブ実行（ランキング・おすすめ更新） |
| 4 | `BatchScheduler` | `batch` | 39 | バッチスケジューリング |
| 5 | `CartService` | `cart` | 419 | カート追加・更新・削除・金額計算 |
| 6 | `ContactService` | `contact` | 81 | お問い合わせフォーム初期化・送信 |
| 7 | `MailTemplateRenderer` | `mail` | 132 | メールテンプレートレンダリング |
| 8 | `NotificationMailService` | `mail` | 339 | 注文確定・その他通知メール送信 |
| 9 | `MemberService` | `member` | 371 | 会員登録・プロフィール変更・退会・お気に入り |
| 10 | `MemberUserDetailsService` | `member` | 50 | Spring Security 認証用ユーザー詳細取得 |
| 11 | `OrderService` | `order` | 435 | 注文番号採番・注文確定・購入履歴 |
| 12 | `ProductFilterOptionService` | `product` | 506 | 絞り込み選択肢の取得・検証 |
| 13 | `ProductListSearchService` | `product` | 212 | 商品一覧検索・ページネーション |
| 14 | `ProductService` | `product` | 287 | 商品表示・価格帯判定・新着取得 |

**合計: 14 クラス / 約 3,455 行**

### 3.2 対象外

以下は単体テストの対象外とし、結合テストまたは目視確認で代替する。

| 分類 | 理由 |
|---|---|
| Controller 層（`web` パッケージ） | Spring MVC の設定依存が大きく、単体テストでの検証価値が低い |
| Repository 層の SQL 部分（MyBatis XML） | 実 DB なしでは検証不可。結合テストで対応 |
| 例外クラス（`*Exception.java`） | ロジックなし |
| バッチリスナー・定数クラス（`*Listener.java`、`*Names.java`） | ロジックなし |
| Thymeleaf テンプレート | 結合テストまたは E2E テストで対応 |
| Spring Batch メタデータテーブル操作 | フレームワーク責務 |

> **例外規定**: Repository 層でも純粋な Java ロジック（文字列正規化など）を持つメソッドは単体テスト対象とする（例: `ProductRepository#normalizeKeyword`）。

---

## 4. テスト設計方針

### 4.1 フレームワーク・アノテーション

| 項目 | 採用 |
|---|---|
| テストランナー | `@ExtendWith(MockitoExtension.class)` |
| モックライブラリ | Mockito 5（`@Mock`、`@InjectMocks`、`when/verify`） |
| アサーションライブラリ | AssertJ |
| Spring Context | **起動しない**（単体テストのため） |

```java
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService sut;
}
```

### 4.2 テストクラスの配置規則

| 本番クラス | テストクラス |
|---|---|
| `src/main/java/jp/co/skig/officeorder/service/cart/CartService.java` | `src/test/java/jp/co/skig/officeorder/service/cart/CartServiceTest.java` |

- 1 クラスに 1 テストクラスを作成する
- テストクラス名: `{ClassName}Test.java`
- 特定のメソッドに特化したテストを分割する場合: `{ClassName}{TargetMethod}Test.java`（既存の `OrderServiceGenerateOrderNumberTest` に倣う）

### 4.3 テストメソッド命名規則

**形式**: `{methodName}_{scenario}_{expectedBehavior}()`

| 要素 | 規則 |
|---|---|
| `methodName` | テスト対象のメソッド名（lowerCamelCase） |
| `scenario` | テスト条件・入力状態（英語スネークケース） |
| `expectedBehavior` | 期待する結果・振る舞い（英語スネークケース） |

**例:**
```java
// 正常系
@Test
void calculateShipping_normalDelivery_returnsBaseShippingFee() { ... }

// 異常系・境界値
@Test
void addItem_quantityExceedsMax_throwsIllegalArgumentException() { ... }

// 戻り値が void の場合
@Test
void withdrawMember_activeStatus_setsWithdrawnAt() { ... }
```

> **既存テストの移行**: 既存 5 ファイルは日本語命名を採用しているが、次回改修時に英語スネークケースへ順次移行する。新規作成するテストは本規則に従う。

### 4.4 テスト構造（AAA パターン）

すべてのテストメソッドは **Arrange / Act / Assert** の 3 ブロックで構成する。

```java
@Test
void generateOrderNumber_firstOrderOfDay_returnsSequence001() {
    // Arrange
    LocalDate today = LocalDate.of(2026, 4, 14);
    when(orderRepository.nextOrderSequence(today)).thenReturn(1);

    // Act
    String result = sut.generateOrderNumber(today);

    // Assert
    assertThat(result).isEqualTo("20260414-001");
}
```

### 4.5 モックポリシー

| コラボレータの種類 | 方針 |
|---|---|
| Repository | `@Mock` でモック化する |
| 他 Service | `@Mock` でモック化する |
| `Clock` / `AppTimeProvider` | `Clock.fixed()` または `@Mock` で固定時刻を注入する |
| `MessageSource` | `@Mock` でモック化する |
| `HttpServletRequest` / `HttpServletResponse` | `@Mock` でモック化する（CartService など） |
| `ObjectMapper` | `@Mock` でモック化する |

### 4.6 テストデータ方針

- テストデータはテストクラス内の `private static final` 定数または `@BeforeEach` セットアップメソッドで定義する（外部ファイル不使用）
- 境界値・null・空文字・最大長など、仕様上の境界条件を網羅する
- ランダム値や `new Date()` など非決定的な値はテストメソッド内で使用しない

### 4.7 優先テスト観点

以下の観点を重点的にテストする。

| 観点 | 例 |
|---|---|
| 正常系（メインフロー） | 標準的な入力での期待動作 |
| 境界値 | 数量上限・件数上限・文字長 |
| null / 空値 | 任意項目が null の場合の振る舞い |
| 条件分岐 | 個人/法人区分、ゲスト/会員区分 |
| 例外スロー | 上限超過・重複チェック失敗など |
| 時刻依存ロジック | 販売期間判定・消費税率適用期間 |

---

## 5. カバレッジ目標

| 対象 | 指標 | 目標値 |
|---|---|:---:|
| Service 層（§3.1 全 14 クラス） | 行カバレッジ（C0） | **100%** |

### 計測方法

JaCoCo Maven プラグインを使用して計測する。`pom.xml` に以下を追加する（未設定の場合）。

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
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

計測コマンド:
```
mvn test jacoco:report
```

レポート出力先: `target/site/jacoco/index.html`

### カバレッジ除外設定

以下は計測対象から除外する（カバレッジ分母に含めない）。

| 除外パターン | 理由 |
|---|---|
| `**/service/**/*Exception.class` | ロジックなし |
| `**/service/batch/BatchJobNames.class` | 定数クラス |
| `**/service/batch/BatchJobExecutionLoggingListener.class` | イベントリスナー（IT で検証） |

---

## 6. テスト完了基準

以下の **すべて** を満たした時点で、単体テストフェーズを完了とみなす。

### 必須基準

| # | 基準 | 確認方法 |
|---|---|---|
| UT-1 | `mvn test` がエラーなく成功すること（FAILED 0 件） | CI ビルドログ |
| UT-2 | Service 層の行カバレッジ（C0）が **100%** に達していること | JaCoCo レポート |
| UT-3 | テスト対象 14 クラス全てにテストクラスが存在すること | ファイル存在確認 |
| UT-4 | テストメソッド名が §4.3 の命名規則に従っていること（新規作成分） | コードレビュー |
| UT-5 | `@SpringBootTest` など Spring Context を起動するアノテーションを使用していないこと | コードレビュー |

### 品質基準

| # | 基準 | 確認方法 |
|---|---|---|
| UT-6 | テストメソッドが AAA パターンで記述されていること | コードレビュー |
| UT-7 | 各テストメソッドが独立して実行できること（テスト間に依存関係がないこと） | `@TestMethodOrder` 非使用の確認 |
| UT-8 | `Thread.sleep()` など外部タイマーに依存するテストコードがないこと | コードレビュー |

### CI ゲート

> **本プロジェクトでは `mvn test` の成功を CI パイプラインのゲート条件とする。**  
> テスト失敗のままマージすることは認めない。

---

## 7. ツール・フレームワーク

| 用途 | ライブラリ | バージョン |
|---|---|---|
| テストランナー | JUnit 5 (Jupiter) | Spring Boot 3.5.11 同梱 |
| モック | Mockito | Spring Boot 3.5.11 同梱 |
| アサーション | AssertJ | Spring Boot 3.5.11 同梱 |
| セキュリティテスト | Spring Security Test | Spring Boot 3.5.11 同梱 |
| カバレッジ計測 | JaCoCo Maven Plugin | 0.8.x（要 `pom.xml` 追加） |
| ビルド | Maven Surefire Plugin | Spring Boot 3.5.11 同梱 |

---

## 8. CI 連携

### ビルド実行コマンド

```sh
# テスト実行のみ
mvn test

# テスト実行 + カバレッジレポート生成
mvn test jacoco:report
```

### Maven Surefire 設定（現状）

`pom.xml` に Mockito エージェントの `argLine` 設定済み:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-javaagent:"...mockito-core-${mockito.version}.jar"</argLine>
    </configuration>
</plugin>
```

JaCoCo 追加後は `argLine` を JaCoCo エージェントと合成するよう更新が必要。

```xml
<argLine>${jacoco.agent.argLine} -javaagent:"...mockito-core-${mockito.version}.jar"</argLine>
```

---

## 9. 既存テストとの関係

本計画書策定時点で以下の 5 テストクラスが存在する。

| テストクラス | 層 | 命名規則 | 本計画での扱い |
|---|---|---|---|
| `OrderServiceGenerateOrderNumberTest` | Service | 日本語 | **適合**（Service 層）。次回改修時に英語命名へ移行 |
| `ProductListSearchServiceSearchTest` | Service | 日本語 | **適合**（Service 層）。次回改修時に英語命名へ移行 |
| `ProductFilterOptionServiceTasteTest` | Service | 日本語 | **適合**（Service 層）。次回改修時に英語命名へ移行 |
| `ProductRepositoryKeywordTest` | Repository | 日本語 | **例外規定適用**（純粋 Java ロジック対象）。存続 |
| `CatalogControllerSearchTest` | Controller | 日本語 | **対象外**（Controller 層）。削除対象ではないが新規追加しない |

---

## 10. 改訂履歴

| バージョン | 日付 | 変更内容 |
|---|---|---|
| 1.0 | 2026-04-14 | 初版作成 |
