# 単体テスト計画

| 項目 | 内容 |
|---|---|
| ドキュメントID | TEST-PLAN-001 |
| バージョン | 1.0 |
| 作成日 | 2026-04-27 |
| 対象プロジェクト | office-order |

---

## 1. 目的

本計画書は、`office-order` プロジェクトにおける単体テストの方針・対象範囲・命名規則・クラス構造を定める。
チームメンバーが一貫したスタイルでテストを作成・維持できるよう、実践的なガイドラインを提供することを目的とする。

---

## 2. 方針サマリ

| 観点 | 方針 |
|---|---|
| テスト対象レイヤー | **Service 層・Model 層・Common 層** のみ |
| Controller / Mapper / Repository | **テスト対象外** |
| カバレッジ目標 | **定量目標なし**（定性的な品質基準で管理） |
| テスト構造 | **新規作成分から `@Nested` を採用**（既存クラスは任意移行） |
| 実行環境依存 | **なし**（Spring コンテキスト・DB 不使用）|

---

## 3. テスト対象スコープ

### 3.1 テスト対象

| レイヤー | パッケージ | 方針 |
|---|---|---|
| Service 層 | `jp.co.skig.officeorder.service.*` | 依存クラスを Mockito でモック化してビジネスロジックを検証 |
| Model 層 | `jp.co.skig.officeorder.model.*` | POJO / Record を直接インスタンス化して変換・正規化ロジックを検証 |
| Common 層 | `jp.co.skig.officeorder.common.*` | 外部依存のないユーティリティクラスを直接検証 |

### 3.2 テスト対象外

| レイヤー | 理由 |
|---|---|
| Controller 層 | Spring MVC スライステストの導入コストを現時点では許容しない |
| Repository 層 | DB 依存テストは実施しない |
| Mapper 層 | MyBatis XML / SQL は統合テスト・手動確認で担保する |
| Thymeleaf テンプレート | UI レンダリングは単体テストの対象外 |
| Config クラス | Spring コンテキストを必要とするため対象外 |

---

## 4. 使用ライブラリ

| ライブラリ | バージョン | 用途 |
|---|---|---|
| JUnit 5 (`junit-jupiter`) | Spring Boot 管理 | テストランナー |
| Mockito | Spring Boot 管理 | モック・スタブ・引数検証 |
| AssertJ | Spring Boot 管理 | アサーション |
| `spring-boot-starter-test` | 3.5.11 | 上記をすべて一括提供 |

> **原則**: Spring コンテキスト（`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` 等）は使用しない。
> `@ExtendWith(MockitoExtension.class)` のみで完結させる。

### テスト実行コマンド

```bash
# 全テスト実行
./mvnw test

# 特定クラスのみ実行
./mvnw test -Dtest=ProductServiceTest

# 特定メソッドのみ実行
./mvnw test "-Dtest=ProductServiceTest#buildCondition*"
```

---

## 5. テストクラスの構造・命名規則

### 5.1 クラス名

規則: **`{対象クラス名}Test`**

| 対象クラス | テストクラス名 |
|---|---|
| `ProductService` | `ProductServiceTest` |
| `SearchKeywordNormalizer` | `SearchKeywordNormalizerTest` |
| `ProductCategoryFilter` | `ProductCategoryFilterTest` |

テストクラスは対象クラスと同じパッケージ階層に配置する。

```
src/main/java/jp/co/skig/officeorder/service/product/ProductService.java
src/test/java/jp/co/skig/officeorder/service/product/ProductServiceTest.java  ← 同パッケージ
```

### 5.2 テストメソッド名

**命名パターン**: 日本語 `メソッド名_条件_期待結果`

```java
@Test
void buildCondition_tasteIdsがnullの場合_空リストに正規化される() { ... }

@Test
void normalize_全角英数字を含む場合_半角に変換される() { ... }

@Test
void toLikePattern_空文字列の場合_パーセントのみを返す() { ... }
```

- 日本語を使用することで、テストレポートで仕様が読み取れるようにする
- `_` でメソッド名・条件・期待結果の 3 セクションに区切る

### 5.3 `@Nested` による階層構造（新規作成分より適用）

新規に作成するテストクラスは、対象メソッドごとに `@Nested` クラスで階層化する。

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private AppTimeProvider appTimeProvider;

    @InjectMocks
    private ProductService productService;

    @Nested
    class BuildConditionTest {

        @Test
        void tasteIdsがnullの場合_空リストに正規化される() { ... }

        @Test
        void tasteIdsを指定した場合_そのまま渡される() { ... }

        @Test
        void 短縮オーバーロードはtasteIdsが空リストになる() { ... }
    }
}
```

> **既存 5 クラスへの `@Nested` 適用は任意**。
> PR レビュー・リファクタリングの機会に順次移行する。

### 5.4 テストメソッドの構造（AAA パターン）

各テストメソッドは **Arrange → Act → Assert** の 3 ブロック構成を基本とする。

```java
@Test
void buildCondition_tasteIdsがnullの場合_空リストに正規化される() {
    // Arrange
    List<Long> tasteIds = null;

    // Act
    ProductSearchCondition result = productService.buildCondition(..., tasteIds);

    // Assert
    assertThat(result.tasteIds()).isEmpty();
}
```

---

## 6. モック・スタブの使用方針

### 6.1 基本ルール

| ルール | 説明 |
|---|---|
| `@Mock` + `@InjectMocks` を使用 | コンストラクタインジェクション互換で依存を解決する |
| `@Spy` の使用は原則禁止 | 部分モックが必要な場合は設計を見直す |
| `@MockBean` の使用禁止 | Spring コンテキストを使用しないため不要 |
| モックは依存クラスのみ | テスト対象クラス自体はモックしない |

### 6.2 スタブ設定の配置

| 配置場所 | 用途 |
|---|---|
| `@BeforeEach` | クラス内の全テストで共通するデフォルトのスタブのみ |
| テストメソッド内 | テストケース固有のスタブ・デフォルトの上書き |

### 6.3 引数検証

```java
// 引数の中身を検証する場合は ArgumentCaptor を使用する
ArgumentCaptor<List<Long>> tasteCaptor = ArgumentCaptor.forClass(List.class);
verify(productService).buildCondition(..., tasteCaptor.capture());
assertThat(tasteCaptor.getValue()).isEmpty();

// 呼び出し有無・回数の検証
verify(productRepository, times(1)).findByCondition(any());
verify(productRepository, never()).findByCondition(any());
```

---

## 7. テストデータの扱い方

| パターン | 使用場面 | 例 |
|---|---|---|
| `static final` 定数（クラスレベル） | 複数のテストメソッドで共通するダミーオブジェクト | `EMPTY_BUNDLE`, `DUMMY_CONDITION` |
| `@BeforeEach` のインスタンスフィールド | テストメソッド間で共有するがセットアップが複雑なオブジェクト | `bundle = new ProductFilterOptionsBundle(...)` |
| テストメソッド内インライン | 1 件のテストのみで使用する値 | `List.of(1L, 2L)` |

```java
// クラスレベル定数の例
private static final ProductFilterOptionsBundle EMPTY_BUNDLE =
    new ProductFilterOptionsBundle(List.of(), List.of(), List.of());

private static final ProductSearchCondition DUMMY_CONDITION =
    new ProductSearchCondition(null, null, List.of(), List.of(), null, null, null, null, null, List.of());
```

> テストデータに本番相当の個人情報（メールアドレス・氏名等）を使用しない。
> ダミーデータには `test@example.com` / `テスト太郎` 等の架空値を使用する。

---

## 8. アサーションスタイル

**AssertJ を統一して使用する**。JUnit 5 の `assertEquals` / `assertTrue` は使用しない。

```java
// OK — AssertJ
assertThat(result).isEmpty();
assertThat(result).containsExactly(1L, 2L);
assertThat(result.keyword()).isEqualTo("abc");
assertThat(result).isNull();
assertThatThrownBy(() -> service.doSomething(null))
    .isInstanceOf(IllegalArgumentException.class);

// NG — JUnit 5 ネイティブ（使用しない）
assertEquals(List.of(), result);
assertTrue(result.isEmpty());
assertNull(result);
```

---

## 9. 既存テスト一覧

| テストクラス | 対象クラス | レイヤー | テスト種別 | `@Nested` |
|---|---|---|---|---|
| `SearchKeywordNormalizerTest` | `SearchKeywordNormalizer` | Common | POJO（直接インスタンス化） | なし（既存） |
| `ProductCategoryFilterTest` | `ProductCategoryFilter` | Model | POJO / Record | なし（既存） |
| `ProductFilterOptionServiceTest` | `ProductFilterOptionService` | Service | Mockito | なし（既存） |
| `ProductListSearchServiceTest` | `ProductListSearchService` | Service | Mockito + ArgumentCaptor | なし（既存） |
| `ProductServiceTest` | `ProductService` | Service | Mockito | なし（既存） |

---

## 10. テスト追加ガイドライン

### 10.1 追加すべきタイミング

- **新規クラス・メソッドを実装したとき**（Service 層・Model 層・Common 層）
- **バグ修正を行ったとき**（再発防止テストを必ず追加する）
- **既存コードをリファクタリングしたとき**（変更前の挙動を先にテストで記述してから変更する）

### 10.2 検証すべき観点

| 観点 | 説明 |
|---|---|
| 正常系 | 仕様通りの入力で期待値が返ること |
| 異常系・防御的処理 | `null` / 空リスト / 空文字列が渡された場合の挙動 |
| 境界値 | リスト要素数 0 / 1 / 複数、数値の最小値・最大値近辺 |
| 分岐網羅 | if / switch の各分岐が少なくとも 1 件のテストで通過すること |
| 順序・重複 | ソート済みリストの並び順・重複除去の挙動 |

### 10.3 テストを書かなくてよいケース

- `record` の accessor（`keyword()`, `tasteIds()` 等のゲッター相当）
- DI 設定のみの `@Configuration` クラス
- `toString` / `equals` / `hashCode`
  ただし `record` のカスタム正規化ロジック（`normalize()` 等）は対象

### 10.4 テストの独立性・再現性

- 各テストメソッドは他のテストの実行順序・状態に依存しない
- `static` な可変フィールドをテストクラス内で使用しない
- 現在時刻を直接参照する代わりに `AppTimeProvider` をモックする（`Clock` の固定化）
- ランダム値を使用しない

---

## 11. テストレビュー観点

PR レビュー時に以下を確認する。

| # | 観点 | チェック内容 |
|---|---|---|
| 1 | 命名 | 日本語 `メソッド名_条件_期待結果` 形式になっているか |
| 2 | 構造 | 新規クラスは `@Nested` を使用しているか |
| 3 | AAA | Arrange → Act → Assert の構造になっているか |
| 4 | スコープ | Controller / Mapper 等の対象外レイヤーをテストしていないか |
| 5 | アサーション | AssertJ を使用しているか（JUnit 5 ネイティブは不使用） |
| 6 | モック | `@Spy` / `@MockBean` を不要に使用していないか |
| 7 | 独立性 | テストメソッド間で状態を共有していないか |
| 8 | 再現性 | 現在時刻・ランダム値を固定せずに使っていないか |
| 9 | テストデータ | 実在しうる個人情報を使用していないか |
| 10 | 観点網羅 | 正常系・異常系・境界値が考慮されているか |

---

## 12. 今後のテスト拡充候補

以下は現時点では対象外だが、プロジェクトの成長・チームの習熟に応じて導入を検討する。

| 項目 | 概要 | 前提条件 |
|---|---|---|
| Controller スライステスト | `@WebMvcTest` + SpringSecurity で認可設定・ルーティングを検証 | Spring Security テストの知識習得 |
| Mapper テスト（Testcontainers） | PostgreSQL コンテナを使った SQL 検証（`ILIKE`, `TRANSLATE` 等） | Docker 運用が已定着（`docker-compose.yml` 完備済み） |
| カバレッジレポート | JaCoCo を導入して HTML レポートを生成 | テスト件数が一定数を超えてから |
| CI 自動実行 | GitHub Actions 等でプッシュ時に `./mvnw test` を実行 | CI 環境の整備 |
