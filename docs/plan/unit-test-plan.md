# 単体テスト計画書

| 項目 | 内容 |
|------|------|
| ドキュメント ID | TEST-PLAN-001 |
| 作成日 | 2026-04-18 |
| 最終更新 | 2026-04-18 |
| 対象プロジェクト | office-order |
| ステータス | 作業中 |

---

## 1. 目的

本計画書は、office-order アプリケーションの単体テスト（Unit Test）の方針・規約・実施範囲・品質基準を定め、テスト活動の一貫性と品質を担保することを目的とする。

---

## 2. スコープ

### 2-1. 対象（In Scope）

- Java クラス単位の単体テスト（依存関係をモックで置き換えた純粋なロジック検証）
- 対象レイヤー: **Service 層 / Repository 層の非 SQL ロジック / Common ユーティリティ**
- テストフレームワーク: JUnit 5 + AssertJ + Mockito（pom.xml 既定の構成）

### 2-2. 対象外（Out of Scope）

| 種別 | 理由 | 今後の方針 |
|------|------|-----------|
| Mapper XML / SQL ロジック | DB 接続が必要なため UT スコープ外 | 統合テスト（IT）で TestContainers + 実 PostgreSQL を使用 |
| Controller 層 | Spring MVC コンテキストが必要 | `@WebMvcTest` による IT で対応 |
| Spring Security 設定 | コンテキスト依存 | `@SpringBootTest` による IT で対応 |
| E2E / 受け入れテスト | ブラウザ操作が必要 | 別途手動 or Playwright で対応 |
| Spring Batch ジョブ全体 | ジョブコンテキストが必要 | バッチ IT で対応 |

---

## 3. テストフレームワーク・ライブラリ

| ライブラリ | バージョン | 用途 |
|-----------|-----------|------|
| JUnit 5 (`junit-jupiter`) | spring-boot-starter-test 同梱 | テストランナー・アノテーション |
| AssertJ | spring-boot-starter-test 同梱 | 流暢なアサーション |
| Mockito | spring-boot-starter-test 同梱 | モック・スタブ・スパイ |
| `mockito-junit-jupiter` | 同梱 | `@ExtendWith(MockitoExtension.class)` 連携 |
| spring-security-test | pom.xml 追加済み | 認証コンテキスト操作（IT で使用） |

> TestContainers（PostgreSQL）は将来の統合テスト用として採用予定。現行 UT では不使用。

---

## 4. 命名規則・コーディング規約

### 4-1. テストクラス

| 規則 | 形式 | 例 |
|------|------|----|
| 基本形 | `{対象クラス名}Test` | `OrderServiceTest` |
| 特定メソッドに限定する場合 | `{対象クラス名}{メソッド名}Test` | `OrderServiceGenerateOrderNumberTest` |
| パッケージ | 本体クラスと同一パッケージ（`src/test/java` 以下） | `jp.co.skig.officeorder.service.order` |

### 4-2. テストメソッド

| 規則 | 詳細 |
|------|------|
| メソッド名 | **英語・短く**（例: `returnsEmpty`, `throwsWhenLimitExceeded`） |
| `@DisplayName` | **日本語**で条件と期待結果を明記する（例: `"注文数量が上限を超えた場合は例外をスローする"` ）|
| テスト ID 接頭辞 | テスト設計書に ID がある場合はメソッド名の先頭に付ける（例: `nt01_allValid`） |

```java
// 命名例
@Test
@DisplayName("注文合計が送料無料しきい値以上の場合、送料は0円になる")
void zeroShippingFeeAboveThreshold() { ... }
```

### 4-3. アレンジ・アクト・アサート（AAA パターン）

```java
@Test
@DisplayName("...")
void methodName() {
    // Arrange
    ...
    // Act
    var result = sut.method(input);
    // Assert
    assertThat(result)...;
}
```

### 4-4. その他規約

- テスト対象インスタンスは `sut`（System Under Test）と命名する。
- `@BeforeEach` でのセットアップを標準とし、テストごとに状態が独立することを保証する。
- `static` テストデータ定数は `private static final` で宣言し、テストクラス内に閉じる。
- `Assertions.assertThat()` のみを使い、JUnit の `assertEquals` は使わない。

---

## 5. カバレッジ目標

本プロジェクトでは**コードの品質を数値ではなく網羅性**で担保する方針を採用する。

| 指標 | 目標値 | 備考 |
|------|--------|------|
| **ライン カバレッジ** | **100%** | テスト対象として登録したクラスに限る |
| **ブランチ カバレッジ** | **100%** | `if` / `switch` / 三項演算子・Optional 分岐を含む |

> **対象外クラスへの適用について**  
> スコープ外（Controller、Config、Mapper インターフェース等）はカバレッジ計測から除外する。  
> JaCoCo の `exclude` 設定で除外パターンを管理する（[6-3 節](#6-3-除外パターン例) 参照）。

### 5-1. カバレッジ計測コマンド

```bash
./mvnw test jacoco:report
# レポート: target/site/jacoco/index.html
```

> JaCoCo は spring-boot-starter-test に同梱されていないため、必要に応じて `pom.xml` に追加すること。

### 5-2. カバレッジ計測の除外パターン例（将来 pom.xml に追加）

```xml
<exclude>jp/co/skig/officeorder/config/**</exclude>
<exclude>jp/co/skig/officeorder/mapper/**</exclude>
<exclude>jp/co/skig/officeorder/web/**</exclude>
<exclude>jp/co/skig/officeorder/OfficeOrderApplication.class</exclude>
<exclude>jp/co/skig/officeorder/logging/**</exclude>
<exclude>jp/co/skig/officeorder/model/**</exclude>
```

---

## 6. テスト対象一覧と優先度

### 6-1. 優先度基準

| 優先度 | 基準 |
|--------|------|
| **A（必須）** | ビジネスロジックの複雑さが高い・バグ時の影響が大きい（金額計算・採番・上限チェック等） |
| **B（推奨）** | 中程度の複雑さを持つ・外部依存のある処理 |
| **C（任意）** | 委譲・単純変換・定数定義のみ |

FEAT 番号ごとに追加テストが発生した場合は、各 FEAT の設計書（`docs/design/FEAT-XXX-test-design.md`）に個別のテストケースを記載し、本計画書のステータスを更新する。

### 6-2. テスト対象クラス一覧

| 優先 | パッケージ | クラス | 主なテスト観点 | テストクラス | ステータス |
|:----:|-----------|--------|--------------|------------|:--------:|
| A | `common` | `NormalizationUtils` | 全角→半角変換・半角カナ→全角・記号変換・null/空文字 | `NormalizationUtilsTest` | ✅ 完了 |
| A | `service.order` | `OrderService` | 注文番号採番（採番上限・フォーマット）、注文合計計算、送料反映、初期フォーム生成 | `OrderServiceGenerateOrderNumberTest` / `OrderServiceTest` | 🔶 一部完了 |
| A | `service.cart` | `CartService` | 数量上限チェック（MAX 99）、送料計算（しきい値 5,000 円）、追加・更新・削除、リダイレクトパス検証 | `CartServiceTest` | ⬜ 未着手 |
| A | `service.member` | `MemberService` | 追加お届け先上限（20 件）、お気に入り上限（100 件）、会員登録時のパスワードハッシュ化、退会、メール重複チェック | `MemberServiceTest` | ⬜ 未着手 |
| A | `common` | `MoneyFormatter` | 3 桁区切り、null、ゼロ、小数点以下切り捨て、負数 | `MoneyFormatterTest` | ⬜ 未着手 |
| A | `repository` | `ProductRepository`（`buildKeywords`） | キーワード分割・正規化・空文字/null 処理 | `ProductRepositoryKeywordBuildTest` | ✅ 完了 |
| B | `service.product` | `ProductFilterOptionService` | テイスト名正規化・許可リスト外除去・重複除去 | `ProductFilterOptionServiceTest` | ✅ 完了 |
| B | `service.product` | `ProductListSearchService` | 検索条件の組み立て・ページング計算 | `ProductListSearchServiceTest` | ⬜ 未着手 |
| B | `service.product` | `ProductService` | 商品詳細取得・存在しない場合の例外スロー | `ProductServiceTest` | ⬜ 未着手 |
| B | `service.mail` | `MailTemplateRenderer` | テンプレート変数置換・件名取得 | `MailTemplateRendererTest` | ⬜ 未着手 |
| B | `service.batch` | `BatchExecutionService` | バッチ起動可否チェック・状態遷移例外 | `BatchExecutionServiceTest` | ⬜ 未着手 |
| C | `service.announcement` | `AnnouncementService` | お知らせ一覧取得（委譲のみ） | `AnnouncementServiceTest` | ⬜ 未着手 |
| C | `common` | `AppTimeProvider` | 現在時刻取得（Clock の差し替え） | `AppTimeProviderTest` | ⬜ 未着手 |

---

## 7. テストケース設計方針

### 7-1. 同値分割・境界値分析

各メソッドの入力は以下の観点で分類し、テストケースを網羅する。

| 観点 | 具体例 |
|------|--------|
| 正常系（有効値） | 通常の入力値・典型的なユースケース |
| 境界値（下限） | 0、1、上限 − 1 |
| 境界値（上限） | 上限ちょうど、上限 + 1 |
| null / 空文字 | すべての String・Collection パラメータ |
| 異常系 | 上限超過・不正値・削除済みデータへのアクセス |

### 7-2. 例外テスト

`assertThatThrownBy` を使い、例外クラス・メッセージを検証する。

```java
assertThatThrownBy(() -> sut.createAdditionalAddress(memberId, form))
    .isInstanceOf(AddressLimitExceededException.class);
```

### 7-3. private メソッドの扱い

| パターン | 対応方針 |
|---------|---------|
| public API を通じてテスト可能 | public メソッドの出力で間接的に検証する（推奨） |
| ロジックが独立して複雑（例: `generateOrderNumber`） | リフレクションを用いた直接呼び出し（`Method.invoke`）を許容する |
| 将来的に切り出し可能 | `/* TODO: package-private 化を検討 */` コメントを残す |

### 7-4. 時刻依存ロジック

`Clock` を依存注入してテストする。固定日時は `Clock.fixed(Instant, ZoneId)` で生成し、乱数・現在時刻に依存する挙動を決定的にする。

```java
Clock fixedClock = Clock.fixed(
    Instant.parse("2026-03-10T00:12:00Z"), ZoneOffset.ofHours(9));
sut = new OrderService(orderRepository, objectMapper, mailService, fixedClock, messageSource);
```

---

## 8. テストダブル方針

### 8-1. モックの方針

| 依存オブジェクト | 方針 | 注記 |
|----------------|------|------|
| `*Repository` | `@Mock` で完全モック | UT スコープではすべてモック化 |
| `*Mapper`（MyBatis） | `@Mock` で完全モック | SQL ロジックは IT スコープ |
| `MessageSource` | `@Mock` でスタブ | メッセージ文字列の内容は検証しない |
| `PasswordEncoder` | `@Mock` でスタブ | BCrypt の実際の処理はテスト不要 |
| `ObjectMapper` | 実インスタンス使用 | 軽量かつ実挙動確認が重要なため |
| `Clock` | `Clock.fixed(...)` | 時刻固定のため実装を直接差し替え |
| `HttpServletRequest/Response` | `@Mock` or `MockHttpServletRequest` | Spring Test の Mock を活用 |

### 8-2. モック検証の方針

- 戻り値の検証が主目的のため `verify()` は**原則使わない**。
- ただし「メール送信が1回だけ呼ばれる」等の副作用が重要な場合は明示的に `verify()` する。
- `verifyNoMoreInteractions()` は過剰指定になりやすいため**使わない**。

---

## 9. テストデータ管理方針

- テストデータはテストクラス内の `private static final` 定数、または `@BeforeEach` のローカル変数で管理する。
- **テスト専用 DB・外部ファイルは UT スコープでは使用しない**。
- 繰り返し登場する共通フィクスチャ（例: `MemberSessionUser` の標準インスタンス）は、テストクラス内の `private static` ファクトリメソッドで生成する。

```java
private static MemberSessionUser testMember(long id) {
    return new MemberSessionUser(id, "test@example.com", MemberType.PERSONAL, ...);
}
```

- 本番データのコピーや実際のメールアドレスをテストデータに含めない（プライバシー配慮）。

---

## 10. テスト実行方法

### 10-1. 全テスト実行

```bash
./mvnw test
```

### 10-2. 特定クラスのみ実行

```bash
./mvnw test -Dtest=OrderServiceTest
```

### 10-3. 特定メソッドのみ実行

```bash
./mvnw test -Dtest="OrderServiceTest#zeroShippingFeeAboveThreshold"
```

### 10-4. テスト除外

```bash
./mvnw test -Dexclude="**/OrderServiceTest.java"
```

### 10-5. IDE での実行

IntelliJ IDEA / VS Code（Java Test Runner）から直接クラス・メソッド単位で実行可能。  
緑のランアイコン（▶）から個別実行することを標準ワークフローとする。

---

## 11. 品質基準・完了条件

テストの完了条件は以下をすべて満たすこととする。

| # | 基準 | 確認方法 |
|---|------|---------|
| 1 | 全テストが PASS していること | `mvn test` の終了コード = 0 |
| 2 | テスト対象クラスのライン カバレッジ 100% | JaCoCo レポート |
| 3 | テスト対象クラスのブランチ カバレッジ 100% | JaCoCo レポート |
| 4 | 新規テストクラスに `@DisplayName`（日本語）が付与されていること | コードレビュー |
| 5 | テストが他のテストに副作用を与えないこと（テスト順序非依存） | `@TestMethodOrder` 未使用かつ全テスト PASS |
| 6 | モック設定が過剰でなく、テスト意図が明確であること | コードレビュー |

---

## 12. FEAT ごとのテスト追加手順

新機能（FEAT-XXX）を実装する際は以下の手順でテストを追加する。

1. `docs/design/FEAT-XXX-test-design.md` にテストケース一覧を記載する。
2. 本計画書の「テスト対象クラス一覧」（[6-2 節](#6-2-テスト対象クラス一覧)）に対象クラスを追加し、ステータスを更新する。
3. テストクラスを実装し、カバレッジ 100% を確認してから PR をマージする。

---

## 13. 今後の拡張計画

本計画書は **UT のみ** を対象とする。下記の拡張は別計画として整備する。

| 種別 | 方針 | 優先度 |
|------|------|--------|
| **統合テスト（IT）** | TestContainers + 実 PostgreSQL で Mapper XML・Repository を検証 | 中 |
| **Spring MVC テスト** | `@WebMvcTest` で Controller のリクエスト/レスポンス検証 | 中 |
| **CI / CD 組み込み** | GitHub Actions 等に `mvn test` を組み込みPR ごとに実行 | 未定 |
| **E2E テスト** | Playwright によるブラウザ自動テスト | 低 |

---

## 付録：テストステータス凡例

| 記号 | 意味 |
|:----:|------|
| ✅ | 実装・カバレッジ 100% 確認済み |
| 🔶 | 一部実装済み・カバレッジ未達 |
| ⬜ | 未着手 |
