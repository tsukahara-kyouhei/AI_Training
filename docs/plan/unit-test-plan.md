# 単体テスト計画書

**プロジェクト**: OFFICE ORDER（法人向け家具 EC サイト）  
**バージョン**: 0.0.1-SNAPSHOT  
**作成日**: 2026-04-04  
**確定日**: 2026-04-04  
**ステータス**: 確定

> 本計画書は [`unit-test-plan-kakunin-list.md`](unit-test-plan-kakunin-list.md) の確認事項（Q-1〜Q-10）への回答を反映した確定版です。

---

## 目次

1. [目的・スコープ](#1-目的スコープ)
2. [テスト対象システム概要](#2-テスト対象システム概要)
3. [テスト環境](#3-テスト環境)
4. [使用フレームワーク・ツール](#4-使用フレームワークツール)
5. [テスト方針（全体）](#5-テスト方針全体)
6. [フロントモジュール テスト方針](#6-フロントモジュール-テスト方針)
7. [バックモジュール テスト方針](#7-バックモジュール-テスト方針)
8. [SQL テスト方針（MyBatis マッパー）](#8-sql-テスト方針mybatis-マッパー)
9. [テスト対象クラス一覧](#9-テスト対象クラス一覧)
10. [カバレッジ目標](#10-カバレッジ目標)
11. [テストデータ方針](#11-テストデータ方針)
12. [テスト命名規則](#12-テスト命名規則)
13. [除外事項](#13-除外事項)
14. [テスト実行方法](#14-テスト実行方法)
15. [完了基準](#15-完了基準)

---

## 1. 目的・スコープ

### 1.1 目的

- バックエンド（Java）・SQL（MyBatis マッパー）モジュールが仕様どおりに動作することを、
  最小単位でかつ自動的に検証する
- 開発中・保守時のリグレッション（機能の意図せぬ後退）を早期に検出する
- 将来の機能追加・リファクタリングに対して安全なフィードバックループを確立する

### 1.2 スコープ

| 対象 | 範囲 |
|------|------|
| **バックエンド（Java）** | Controller / Service / Repository / 共通コンポーネント |
| **SQL（MyBatis マッパー）** | Mapper インターフェース + XML クエリ |
| ~~フロントエンド（JavaScript）~~ | **今回スコープ外**（手動テストで対応。将来の E2E テスト計画で対応予定） |

> 結合テスト・E2E テスト・性能テスト・セキュリティ検査は本計画の対象外とする。

---

## 2. テスト対象システム概要

### 2.1 技術スタック

| 分類 | 技術 | バージョン |
|------|------|-----------|
| 言語 | Java | 21 |
| フレームワーク | Spring Boot | 3.5.11 |
| Web / UI | Spring MVC + Thymeleaf | Boot 管理 |
| 認証 | Spring Security | Boot 管理 |
| DB アクセス | MyBatis Spring Boot Starter | 3.0.4 |
| バッチ | Spring Batch | Boot 管理 |
| DB | PostgreSQL | Boot 管理 (runtime) |
| メール | Spring Mail (+ MailHog) | Boot 管理 |
| フロント JS | Vanilla JS (ESM モジュール) | — |
| ビルド | Maven (mvnw) | — |

### 2.2 アーキテクチャ概要

```
Browser
  │
  ▼ HTTP (Spring MVC)
[Controller 層]  ─ Thymeleaf SSR
  │
  ▼
[Service 層]     ─ ビジネスロジック
  │
  ▼
[Repository 層]  ─ DB アクセスの集約点
  │
  ▼
[Mapper 層]      ─ MyBatis (Mapper I/F + XML)
  │
  ▼
PostgreSQL
```

カートは **Cookie ベース**（`CartCookieStore`）で DB を使用しない。  
バッチ（売れ筋ランキング・レコメンド集計）は Spring Batch で実装される。

---

## 3. テスト環境

| 項目 | 内容 |
|------|------|
| OS | Windows 11 / Linux (CI) |
| JDK | 21 (Temurin 推奨) |
| Maven | 3.9.x（`mvnw` 使用） |
| テスト DB | **Testcontainers（Docker 上の PostgreSQL 16）** |
| CI/CD | ※別途確認 |

---

## 4. 使用フレームワーク・ツール

### 4.1 バックエンド

| ツール | 用途 |
|--------|------|
| **JUnit 5** (spring-boot-starter-test 同梱) | テストランナー・アサーション |
| **Mockito** (spring-boot-starter-test 同梱) | モック・スタブ生成 |
| **Spring MockMvc** | Controller 層のリクエスト/レスポンス検証 |
| **spring-security-test** | Security コンテキスト付きテスト (`@WithMockUser` 等) |
| **MyBatis Test** (`mybatis-spring-boot-starter-test`) | Mapper 層テスト（pom.xml 追加が必要） |
| **AssertJ** (spring-boot-starter-test 同梱) | 流暢なアサーション |
| **Testcontainers**（PostgreSQL） | SQL テスト用 DB（`spring-boot-testcontainers` + `testcontainers:postgresql` を pom.xml に追加） |
| **JaCoCo** | カバレッジ計測（`jacoco-maven-plugin` を pom.xml に追加）|

> フロントエンド JS のテストは今回スコープ外（Q-2 の決定による）。手動テストで品質を担保し、将来の E2E テスト計画に委ねる。

---

## 5. テスト方針（全体）

### 5.1 基本方針

1. **クラス単位の独立性を保つ**  
   テスト対象クラスの依存オブジェクトはモック（Mockito）に差し替え、
   「そのクラスの責務のみ」を検証する。

2. **外部リソースに依存しない**  
   本番 DB・外部 API・メールサーバーへの実接続はテスト内で行わない。
   ただし Mapper 層（SQL テスト）は例外とし専用の軽量 DB を使用する。

3. **ハッピーパス + 境界値 + 異常系を網羅する**  
   正常ケースだけでなく、入力値の境界（null・空文字・最大値）および
   例外発生パス（DB エラー・バリデーションエラー等）を必ずテストする。

4. **テストは繰り返し実行可能かつ冪等とする**  
   テスト実行順序に依存せず、何度実行しても同じ結果となるよう設計する。

5. **既存テストとの一貫性を保つ**  
   現在唯一の単体テストである `SearchKeywordNormalizerTest.java` のスタイル・
   アノテーション規約を踏襲する。

### 5.2 テストの種類と想定割合

| テスト種別 | 対象 | 想定割合 |
|-----------|------|---------|
| 純粋単体テスト | Service・共通コンポーネント | 65 % |
| スライステスト（@WebMvcTest） | Controller | 20 % |
| スライステスト（@MybatisTest） | Mapper + SQL | 15 % |

---

## 6. フロントモジュール テスト方針

> **本フェーズのスコープ外**  
> Q-2 の確認事項への回答により、フロントエンド JavaScript の自動テストは今回の単体テスト計画には含めない。  
> Node.js / Jest 環境の導入コストを考慮し、フロント JS の品質は **手動テスト**により担保する。  
> 将来の E2E テスト計画（Playwright 等）にて自動化を検討すること。

対象外の主な JS モジュール（参考）：

| ファイル | 主な責務 |
|---------|----------|
| `modules/submit-guard.js` | フォーム二重送信防止 |
| `modules/product-detail.js` | バリアント選択・画像ギャラリー |
| `modules/recently-viewed.js` | localStorage 読み書き |
| `modules/form-controls.js` | パスワード表示トグル等 |
| `modules/checkout-input.js` | 住所セレクター連動 |
| `modules/modal.js` | モーダル開閉制御 |

---

## 7. バックモジュール テスト方針

### 7.1 Controller 層

#### 7.1.1 テスト手法

`@WebMvcTest` スライステストを使用する。  
- Spring MVC コンテキストのみロードし、起動コストを抑える
- `@MockBean` で Service 層をモック化する
- Spring Security の認証コンテキストは **段階的に以下の方針で対応する**（Q-6 より）
  1. まずは `@WithMockUser` でシンプルに認証チェックを実装する
  2. `MyPageController` 等、`MemberSessionUser` の独自フィールドを参照するケースではカスタムアノテーション **`@WithMockMember`** と専用ヘルパーを `src/test/java/` に作成し、全テストクラスで共通利用する

```java
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)   // Security 設定が必要な場合
class CatalogControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

    @Test
    void 商品不存在時に404が返ること() throws Exception {
        given(productService.findById(anyLong())).willReturn(Optional.empty());
        mockMvc.perform(get("/products/99999"))
               .andExpect(status().isNotFound());
    }
}
```

#### 7.1.2 テスト観点

| 観点 | 内容 |
|------|------|
| HTTPステータス | 正常時 200/302、異常時 400/404/500 |
| リダイレクト先 | ログイン必須ページへの未認証アクセス → ログインページへリダイレクト |
| モデル属性 | `Model` / `ModelAndView` に期待値が格納されること |
| バリデーション | 不正入力時に `BindingResult` にエラーが入りエラーページへ遷移すること |
| 認可制御 | 非ログインユーザーが `/mypage/**` にアクセスすると 401/302 となること |
| CSRF | POST リクエストに CSRF トークンがない場合に 403 となること（設定に依存） |

#### 7.1.3 主要テスト対象メソッド

| Controller | テスト対象メソッド（例） |
|-----------|----------------------|
| `CatalogController` | 商品一覧取得、商品詳細取得、存在しない商品ID |
| `MemberRegistrationController` | 会員登録フォーム表示、確認画面、確認→完了、バリデーションエラー |
| `CartController` | カート追加、削除、一覧表示 |
| `AuthController` | ログイン画面表示、ログアウト |
| `MyPageController` | 注文履歴、マイページトップ（認証必須）|
| `ContactController` | お問い合わせ送信、バリデーションエラー |
| `AppErrorController` | 404/500 エラーページ表示 |

### 7.2 Service 層

#### 7.2.1 テスト手法

`@ExtendWith(MockitoExtension.class)` を使用した純粋単体テストを行う。  
- Spring コンテキストを起動しないため高速
- Repository・Mapper をすべて `@Mock`（または `@MockBean`）でモック化する
- ビジネスロジックのみに集中してテストする

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks MemberService memberService;
    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void メールアドレス重複時に登録例外が投げられること() {
        given(memberRepository.existsByEmail("dup@example.com")).willReturn(true);
        assertThatThrownBy(() -> memberService.register(duplicateForm))
            .isInstanceOf(DuplicateEmailException.class);
    }
}
```

#### 7.2.2 テスト観点

| 観点 | 内容 |
|------|------|
| 正常系 | 期待する戻り値・副作用（モック呼び出し回数・引数）が得られること |
| 異常系 | Repository が例外を返したとき Service が適切に例外をスロー/変換すること |
| 境界値 | null 入力、空リスト、最大値等 |
| ビジネスルール | 在庫不足で注文不可、削除済み会員のログイン不可、メール重複チェック等 |
| モック検証 | 意図しないメソッドが呼ばれていないことを `verifyNoMoreInteractions` で確認 |

#### 7.2.3 主要テスト対象クラス

| Service クラス | テスト観点（抜粋） |
|--------------|-----------------|
| `MemberService` | 会員登録、メール重複チェック、パスワード BCrypt ハッシュ化確認、退会 |
| `MemberUserDetailsService` | 認証情報取得、論理削除済み会員でロード失敗 |
| `ProductListSearchService` | 検索条件の組み立て、`SearchKeywordNormalizer` との連携 |
| `ProductService` | 商品詳細取得、存在しない ID で Optional.empty() |
| `CartService` | カート追加・削除・件数集計 |
| `OrderService` | 注文登録、在庫チェック、注文番号採番 |
| `ContactService` | お問い合わせ保存、メール送信呼び出し確認 |
| `AnnouncementService` | 公開中お知らせの期間フィルタリング |
| `MailTemplateRenderer` | テンプレートから正しい本文が生成されること |
| `NotificationMailService` | メール送信処理、送信失敗時のリトライ設定 |
| `SearchKeywordNormalizer` | 全角→半角変換、大小文字正規化（**実装済みテストあり**） |

### 7.3 Repository 層

#### 7.3.1 テスト手法

Repository は Mapper（MyBatis）のラッパーとして薄い実装が多いため、
**Mapper をモック化した純粋単体テスト**を基本とする。

- Mapper インターフェースを `@Mock` に差し替え、Repository のロジックを検証
- データ変換・条件組み立てなどの独自ロジックがある場合は重点テスト

```java
@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest {

    @InjectMocks ProductRepository productRepository;
    @Mock ProductMapper productMapper;

    @Test
    void 商品コードで商品が取得できること() {
        var expected = new ProductListMapperRow();
        given(productMapper.findByCode("DESK-001")).willReturn(Optional.of(expected));
        var result = productRepository.findByCode("DESK-001");
        assertThat(result).contains(expected);
    }
}
```

#### 7.3.2 特殊ケース：CartCookieStore

CartCookieStore は DB を使用せず `HttpServletRequest` / `HttpServletResponse` のクッキーを
直接操作するため、以下の方針で検証する。

- `MockHttpServletRequest` / `MockHttpServletResponse`（Spring Test 同梱）を使用
- クッキーシリアライズ・デシリアライズの正確性、有効期限設定をテスト

### 7.4 共通コンポーネント

| クラス | テスト手法 | テスト観点 |
|--------|----------|----------|
| `SearchKeywordNormalizer` | 純粋単体（実装済） | 全角/半角、大文字/小文字、記号の正規化 |
| `MoneyFormatter` | 純粋単体 | 金額フォーマット（カンマ区切り、通貨記号） |
| `AppTimeProvider` | 純粋単体 | `Clock` の差し替えによる現在時刻の制御 |
| `LogMaskingUtil` | 純粋単体 | パスワード・個人情報のマスキング処理 |
| バリデーションアノテーション（`common/validation/`） | 純粋単体 | 各制約の合否判定 |

---

## 8. SQL テスト方針（MyBatis マッパー）

### 8.1 テスト手法

`mybatis-spring-boot-starter-test` が提供する `@MybatisTest` スライステストを使用する。  
DB は **Testcontainers（PostgreSQL）** を使用して本番と同一の RDBMS を稼働させる
（詳細は `unit-test-plan-kakunin-list.md` Q-1 参照）。

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductMapperTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired ProductMapper productMapper;

    @Test
    @Sql("/sql/test/product-seed.sql")
    void カテゴリ指定で商品が絞り込まれること() {
        var cond = new ProductSearchCondition();
        cond.setCategory("desk");
        var results = productMapper.searchList(cond);
        assertThat(results).allMatch(r -> "desk".equals(r.getCategory()));
    }
}
```

### 8.2 スキーマ・データ管理

| 役割 | 方法 |
|------|------|
| スキーマ適用 | `src/main/resources/sql/schema/` を `@Sql` またはコンテナ起動時スクリプトで適用 |
| テストデータ投入 | `src/test/resources/sql/` 配下にテスト専用シードファイルを用意 |
| テスト後クリーンアップ | `@Transactional` によるロールバック、または `@Sql(executionPhase = AFTER_TEST_METHOD)` |

### 8.3 テスト優先度と観点

#### 8.3.1 ProductMapper（優先度：高）

複雑な動的クエリを多く含むため最も重点テストが必要。

| テストケース | 検証内容 |
|------------|---------|
| カテゴリ絞込（desk/chair/storage） | カテゴリ条件が SQL に正しく適用されること |
| キーワード検索 | 商品名・バリアント名・説明への部分一致搜索が正しく動作すること |
| 価格帯絞込 | `min_price` / `max_price` の範囲フィルターが正しく機能すること |
| カラー絞込 | カラー ID での絞込が正しく機能すること |
| 在庫あり絞込 | 在庫ゼロの商品が除外されること |
| テイスト絞込 | 各カテゴリのテイストマスタとの JOIN が正しいこと |
| ソート | 新着順・価格順・人気順が正しく機能すること |
| ペジネーション | LIMIT / OFFSET が正しく適用されること |
| 商品詳細 | バリアント・カラー情報が正しく結合されること |
| 存在しない商品 ID | Optional.empty() が返ること |

#### 8.3.2 OrderMapper（優先度：高）

| テストケース | 検証内容 |
|------------|---------|
| 注文登録 INSERT | 注文ヘッダ・明細・ステータス履歴が正しく挿入されること |
| 注文番号採番 | `order_number_counters` の採番ロジックが正しく動作すること（連番・現在日付） |
| 注文完了画面 SELECT | 注文ヘッダと明細がすべて返ること |
| 注文履歴 SELECT | 会員 ID で絞り込まれた注文一覧が返ること |
| 注文ステータス更新 | ステータス変更と履歴追記が正しく行われること |

#### 8.3.3 MemberMapper（優先度：中）

| テストケース | 検証内容 |
|------------|---------|
| メールで会員取得 | 存在するメールで会員情報が返ること |
| 論理削除会員の除外 | `member_status = 'withdrawn'` の会員が認証で取得されないこと |
| お気に入り追加・削除 | 複合 PK での INSERT / DELETE が正しく行われること |
| 追加住所の一覧取得 | 会員 ID で絞り込まれた住所が返ること |

#### 8.3.4 CartMapper（優先度：中）

| テストケース | 検証内容 |
|------------|---------|
| カート商品の在庫・価格プリフィル | 商品コードで在庫・現在価格が取得できること |

#### 8.3.5 AnnouncementMapper / ContactMapper（優先度：低）

| テストケース | 検証内容 |
|----------|--------|
| 公開中お知らせ取得 | 掇載期間内のものだけ返ること |
| お問い合わせ INSERT | 全フィールドが正しく挿入されること |

> **BatchMapper について**: Q-7 の回答により、バッチ処理は今回の単体テストスコープ外としたため、`BatchMapper` の Mapper テスト・バッチジョブ全体のテストを含めない。将来のバッチテスト計画で対応する。

### 8.4 注意事項

- PostgreSQL 固有の構文（`JSONB`・`ILIKE`・`generate_series` 等）は H2 では再現不可のため、
  **Testcontainers**（`postgres:16`）を使用する（Q-1 の回答により確定）
- `order_number_counters` や `popular_product_rankings` のような日付依存テストは
  `AppTimeProvider` の `Clock` を固定日時に差り替えて実施する
- テスト後クリーンアップは `@Transactional` ロールバックを基本とし、採番ロジック等 `REQUIRES_NEW` を使う
  特殊ケースのみ `@Sql(executionPhase = AFTER_TEST_METHOD)` によるクリーンアップ SQL を使用する（Q-9 の回答により確定）

---

## 9. テスト対象クラス一覧

### 9.1 バックエンド Java（優先度順）

| 優先度 | パッケージ | クラス | テスト種別 |
|--------|----------|--------|-----------|
| 高 | `service.member` | `MemberService` | Mockito 単体 |
| 高 | `service.order` | `OrderService` | Mockito 単体 |
| 高 | `service.product` | `ProductListSearchService` | Mockito 単体 |
| 高 | `web` | `MemberRegistrationController` | @WebMvcTest |
| 高 | `web` | `CartController` | @WebMvcTest |
| 高 | `web` | `CatalogController` | @WebMvcTest |
| 中 | `service.cart` | `CartService` | Mockito 単体 |
| 中 | `service.product` | `ProductService` | Mockito 単体 |
| 中 | `service.contact` | `ContactService` | Mockito 単体 |
| 中 | `service.announcement` | `AnnouncementService` | Mockito 単体 |
| 中 | `service.mail` | `MailTemplateRenderer` | Mockito 単体 |
| 中 | `service.mail` | `NotificationMailService` | Mockito 単体 |
| 中 | `service.member` | `MemberUserDetailsService` | Mockito 単体 |
| 中 | `web` | `MyPageController` | @WebMvcTest |
| 中 | `web` | `ContactController` | @WebMvcTest |
| 中 | `repository` | `CartCookieStore` | MockServletRequest |
| 中 | `common` | `MoneyFormatter` | 純粋単体 |
| 中 | `common` | `AppTimeProvider` | 純粋単体 |
| 低 | `common` | `LogMaskingUtil` | 純粋単体 |
| 低 | `web` | `AuthController` | @WebMvcTest |
| 低 | `web` | `AppErrorController` | @WebMvcTest |
| 済 | `util` | `SearchKeywordNormalizer` | 純粋単体 (**実装済**) |

### 9.2 MyBatis マッパー（優先度順）

| 優先度 | Mapper | テスト種別 |
|--------|--------|-----------|
| 高 | `ProductMapper` | @MybatisTest + Testcontainers |
| 高 | `OrderMapper` | @MybatisTest + Testcontainers |
| 中 | `MemberMapper` | @MybatisTest + Testcontainers |
| 中 | `CartMapper` | @MybatisTest + Testcontainers |
| 低 | `AnnouncementMapper` | @MybatisTest + Testcontainers |
| 低 | `ContactMapper` | @MybatisTest + Testcontainers |
| 低 | `BatchMapper` | @MybatisTest + Testcontainers |

### 9.3 フロント JavaScript（優先度順）

| 優先度 | モジュール | テスト種別 |
|--------|----------|-----------|
| 高 | `submit-guard.js` | Jest + jsdom |
| 高 | `product-detail.js` | Jest + jsdom |
| 高 | `recently-viewed.js` | Jest + jsdom |
| 中 | `form-controls.js` | Jest + jsdom |
| 中 | `checkout-input.js` | Jest + jsdom |
| 低 | `modal.js` | Jest + jsdom |

---

## 10. カバレッジ目標

### 10.1 バックエンド（JaCoCo）

Q-3 の回答により、**ライン カバレッジ一律 80 % 以上**（案 A）を目標値とする。

| 対象 | 行カバレッジ目標 | 備考 |
|------|--------------|------|
| `service.*` パッケージ全体 | **80 % 以上** | ビジネスロジックの中核 |
| `web.*` パッケージ全体 | **80 % 以上** | Controller の主要パス |
| `repository.*` パッケージ全体 | **80 % 以上** | |
| `common.*` パッケージ全体 | **80 % 以上** | ユーティリティは達成しやすい |
| `mapper.*` パッケージ全体 | **80 % 以上** | SQL テストで担保 |
| `config.*` / `logging.*` | **除外** | Spring Boot オートコンフィグ機能のテストは除外 |

> JaCoCo の `check` ゴールを `verify` フェーズに追加し、CI ゲートとして機能させることを推奨する。

---

## 11. テストデータ方針

### 11.1 基本原則

- テストデータはテストクラス内または `src/test/resources/sql/` に集約する
- 本番データ・開発用シードデータ（`sql/seed/`）はテストに使用しない
- テスト間でのデータ干渉を防ぐため `@Transactional` ロールバックを基本とする

### 11.2 データ準備方法

| 方法 | 用途 |
|------|------|
| テストクラス内でオブジェクト生成 | Service・Repository の Mockito テスト |
| `@Sql` アノテーション | Mapper テストのシードデータ投入 |
| `ObjectMother` パターン | 頻出するドメインオブジェクトの生成を共通化（任意） |

### 11.3 固定値ガイドライン

| 項目 | 固定値ガイドライン |
|------|-----------------|
| 会員メール | `test-member@example.com` |
| パスワード（生） | `Test!Pass1` |
| 商品コード | `DESK-TEST-001`（テスト専用プレフィックス） |
| 現在日時 | `Clock.fixed(Instant.parse("2026-01-01T09:00:00Z"), ZoneId.of("Asia/Tokyo"))` |

---

## 12. テスト命名規則

### 12.1 Java テストクラス

- ファイル名: テスト対象クラス名 + `Test`（例: `MemberServiceTest.java`）
- 配置: テスト対象クラスと同一パッケージ構成
  ```
  src/test/java/jp/co/skig/officeorder/service/member/MemberServiceTest.java
  ```

### 12.2 Java テストメソッド

- **日本語メソッド名**で意図を明確に記述する
- 形式: `[状態/条件]_[操作/入力]_[期待する結果]` または自然な日本語の文章
  ```java
  @Test void メールアドレス重複時に登録例外が投げられること() { ... }
  @Test void 在庫ゼロの商品はカートに追加できないこと() { ... }
  @Test void null入力を受け取った場合にIllegalArgumentExceptionをスローすること() { ... }
  ```

---

## 13. 除外事項

以下は本単体テスト計画の対象外とする。

| 除外事項 | 理由 |
|---------|------|
| **フロントエンド JavaScript 自動テスト** | Q-2 の回答によりスコープ外。手動テストで対応、将来の E2E 計画で自動化予定 |
| **`BatchMapper` およびバッチジョブ全体** | Q-7 の回答によりスコープ外。将来のバッチテスト計画で対応 |
| Thymeleaf テンプレートのレンダリング検証 | E2E / 結合テストで対応 |
| `config/` パッケージの設定クラス | Spring Boot のオートコンフィグ機能のテストは除外 |
| `logging/` パッケージ（MDC/Filter） | ロギング動作検証は結合テストで対応 |
| `InternalErrorTestController` | 開発・デバッグ用のコントローラー |
| `site.js` | 初期化エントリポイントのみで独自ロジックなし |
| DB マイグレーション・スキーマ変更 | Flyway/Liquibase 未使用のため別途管理 |

---

## 14. テスト実行方法

### 14.1 バックエンド（Maven）

```bash
# 全テスト実行
./mvnw test

# 特定クラスのみ実行
./mvnw test -Dtest=MemberServiceTest

# カバレッジレポート生成（JaCoCo 設定後）
./mvnw verify
# 出力先: target/site/jacoco/index.html
```

> フロントエンド JS のテスト実行は今回スコープ外。

### 14.2 pom.xml への追加設定（必要に応じて）

```xml
<!-- MyBatis Test -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter-test</artifactId>
    <version>3.0.4</version>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- JaCoCo -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## 15. 完了基準

以下の条件をすべて満たした時点で単体テストフェーズ完了とみなす。

| 番号 | 完了条件 |
|------|---------|
| 1 | `./mvnw test` が **CI 環境で全件グリーン**（失敗 0 件）であること |
| 2 | バックエンド全体のライン カバレッジが **目標値以上**であること |
| 3 | カバレッジ目標を未達のクラスについて **レビューを経て承認** されていること |
| 4 | テスト対象一覧（第 9 章）の優先度「高」クラスが **すべてテスト済み**であること |
| 5 | フロント JS テストが **優先度「高」モジュールについて実装済み**であること |
| 6 | テストコードが **コードレビューを通過**していること |

---

*本計画に関する未決事項は別紙 [`unit-test-plan-kakunin-list.md`](unit-test-plan-kakunin-list.md) を参照。*
