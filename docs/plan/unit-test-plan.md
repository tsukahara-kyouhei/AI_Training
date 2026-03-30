# 単体テスト計画書

| 項目 | 内容 |
|------|------|
| ドキュメントID | PLAN-TEST-001 |
| 対象プロジェクト | office-order |
| 作成日 | 2026-03-30 |
| 最終更新日 | 2026-03-30 |

---

## 1. 目的

本計画書は、`office-order` プロジェクトにおける単体テストの実施方針・対象範囲・実装ルールを定め、品質基準を明確にすることを目的とする。

---

## 2. テスト対象スコープ

### 2.1 対象レイヤー

**Service 層・Repository 層・主要 Controller 層** の3層を対象とする。

| レイヤー | 役割概要 | テスト方式 |
|---------|---------|-----------|
| **Service 層** | ビジネスロジック（注文採番・税計算・会員登録・カート計算など） | `@ExtendWith(MockitoExtension.class)` によるピュアモックテスト |
| **Repository 層** | MyBatis Mapper 行の整形・JSON変換・パラメータ組み立て | `@ExtendWith(MockitoExtension.class)` によるピュアモックテスト |
| **Controller 層（主要）** | リクエスト受付・バリデーション・画面遷移ロジック | `@WebMvcTest` + `@MockitoBean` によるスライスモックテスト |

### 2.2 対象クラス一覧

#### Service 層

| パッケージ | クラス名 | テストクラス名 |
|-----------|--------|--------------|
| `service.order` | `OrderService` | `OrderServiceTest` ※既存あり |
| `service.cart` | `CartService` | `CartServiceTest` |
| `service.member` | `MemberService` | `MemberServiceTest` |
| `service.product` | `ProductService` | `ProductServiceTest` |
| `service.product` | `ProductListSearchService` | `ProductListSearchServiceTest` |
| `service.product` | `ProductFilterOptionService` | `ProductFilterOptionServiceTest` |

#### Repository 層

| パッケージ | クラス名 | テストクラス名 |
|-----------|--------|--------------|
| `repository` | `OrderRepository` | `OrderRepositoryTest` |
| `repository` | `ProductRepository` | `ProductRepositoryTest` |
| `repository` | `MemberRepository` | `MemberRepositoryTest` |

#### Controller 層

| パッケージ | クラス名 | テストクラス名 |
|-----------|--------|--------------|
| `web` | `CartController` | `CartControllerTest` |
| `web` | `CatalogController` | `CatalogControllerTest` |
| `web` | `MyPageController` | `MyPageControllerTest` |
| `web` | `HomeController` | `HomeControllerTest` |

### 2.3 スコープ外

以下は単体テストのスコープ外とする（別途 E2E テスト・結合テストで対応）。

- MyBatis Mapper（`*.xml`）と実データベース間の SQL 動作検証
- Thymeleaf テンプレートのレンダリング結果検証
- Spring Security の認証フィルターチェーン
- Spring Batch ジョブの実行フロー
- メール送信の実際の SMTP 通信

---

## 3. テストダブル戦略

### 3.1 基本方針：Mockito ピュアモック

すべての単体テストにおいて、外部依存（データベース・メール・セッション等）を **Mockito モック** で置き換える。データベースへの実接続は行わない。

```java
// Service 層テストの基本構造
@ExtendWith(MockitoExtension.class)
class FooServiceTest {

    @Mock
    private FooRepository fooRepository;

    private FooService fooService;

    @BeforeEach
    void setUp() {
        fooService = new FooService(fooRepository);
    }
}
```

```java
// Controller 層テストの基本構造
@WebMvcTest(FooController.class)
@Import(SecurityTestConfig.class)  // Spring Security を無効化
class FooControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FooService fooService;
}
```

### 3.2 時刻固定

`Clock` や `AppTimeProvider` に依存するテストでは、`Clock.fixed()` を使って時刻を固定し、テストの再現性を確保する。

```java
Clock fixedClock = Clock.fixed(
    LocalDate.of(2026, 3, 30).atStartOfDay(ZoneOffset.UTC).toInstant(),
    ZoneOffset.UTC);
```

### 3.3 トランザクション同期の手動制御

`OrderService` など `@Transactional` の `afterCommit` コールバックに依存するクラスは、`TransactionSynchronizationManager` を `@BeforeEach` / `@AfterEach` で手動初期化・クリアする（既存 `OrderServiceTest` の実装に準じる）。

---

## 4. テストカバレッジ目標

### 4.1 目標値

| レイヤー | 対象 | 行カバレッジ目標 |
|---------|------|----------------|
| Service 層 | 全 Service クラス | **80%以上** |
| Repository 層 | 全 Repository クラス（整形ロジック部分） | **70%以上** |
| Controller 層 | 主要 Controller クラス | **60%以上** |

### 4.2 計測方法

Maven の JaCoCo プラグインで計測する。

```bash
mvn test jacoco:report
# レポート出力先：target/site/jacoco/index.html
```

### 4.3 カバレッジ除外対象

以下はカバレッジ計測から除外する。

- `model` パッケージ（DTO・フォームクラス）
- `mapper` パッケージ（MyBatis Mapper インターフェース）
- `logging` パッケージ（`LogEvent` 列挙型など）
- `*Application.java`（エントリポイント）

---

## 5. ビルド・CI 連携方針

### 5.1 テスト実行タイミング

`mvn test` で **常時実行** する。テスト失敗時はビルドを失敗とし、次工程への成果物出力を停止する。

```bash
# 標準実行
mvn test

# スキップ禁止（CI では -DskipTests を使用しない）
```

### 5.2 pom.xml 設定追加（JaCoCo）

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
    <configuration>
        <excludes>
            <exclude>jp/co/skig/officeorder/model/**</exclude>
            <exclude>jp/co/skig/officeorder/mapper/**</exclude>
            <exclude>jp/co/skig/officeorder/logging/**</exclude>
            <exclude>jp/co/skig/officeorder/*Application.class</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

## 6. テストの命名規則・構造

### 6.1 命名規則

すべてのテストメソッドに `@DisplayName` で日本語説明を付与する。メソッド名は英語のまま維持する。

```java
@Test
@DisplayName("シード初期化後は連番101から採番される")
void placeOrder_returnsOrderNumber000101_whenSequenceIsInitializedAfterSeed() {
    // ...
}
```

**メソッド名の形式：** `{対象メソッド名}_{期待動作}_{前提条件}`

### 6.2 テストクラス構造

テストメソッドは以下のブロックコメントでグループ化する。`@Nested` は冗長になりやすいため、可読性が明らかに向上する場合にのみ使用する。

```java
// -----------------------------------------------------------------------
// 正常系：〇〇が△△する場合
// -----------------------------------------------------------------------

// -----------------------------------------------------------------------
// 異常系：□□が欠けている場合
// -----------------------------------------------------------------------

// -----------------------------------------------------------------------
// 境界値：上限・下限
// -----------------------------------------------------------------------
```

### 6.3 テストデータ構築

ヘルパメソッドはクラス末尾に `private` メソッドとしてまとめ、`build〇〇()` / `stub〇〇()` の命名префикс を使う。

```java
// テストデータ構築
private CheckoutInputForm buildValidForm() { ... }
private CartView buildSingleItemCart() { ... }

// モックスタブ設定
private void stubSuccessfulOrder() { ... }
```

### 6.4 アサーション

AssertJ を使用する（`assertThat`）。JUnit の `assertEquals` は使用しない。

---

## 7. テストクラス別 検証観点一覧

### 7.1 `OrderService` ／ `OrderServiceTest`（既存）

> 既存テストは本計画の方針に適合している。以下の追加観点でテストを拡充する。

| 分類 | 検証観点 |
|------|---------|
| 正常系 | ゲスト注文時（`memberId=null`）の注文が成功する |
| 正常系 | 会員注文時（`memberId` あり）の注文が成功する |
| 正常系 | 法人区分で会社名あり・部署名ありの注文が成功する |
| 正常系 | `createInitialForm` でゲスト時は空フォームが返る |
| 正常系 | `createInitialForm` で7桁郵便番号がパート1・2に分割されてフォームへ反映される |
| 正常系 | `createInitialForm` で会員の既存情報がフォームへ反映される |
| 正常系 | 購入履歴一覧が会員IDとページ番号を渡して取得できる |
| 既存 | シード初期化後の採番が 101 になる（BUG-001） |
| 既存 | 新規 DB 環境での採番が 001 になる |
| 既存 | 連番最大値（999999）の正常採番 |
| 既存 | 連番超過時に `IllegalStateException` |
| 既存 | 空カートで `IllegalArgumentException` |
| 既存 | null カートで `IllegalArgumentException` |
| 既存 | 在庫超過数量で `IllegalArgumentException` |
| 既存 | 法人区分で会社名 null の場合 `IllegalArgumentException` |

### 7.2 `CartService` ／ `CartServiceTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | 税率10%・組立なし・1点3,000円で税額・合計が正しく計算される |
| 正常系 | 税込5,000円以上で送料が 0 円になる（送料無料しきい値） |
| 正常系 | 税込5,000円未満で送料が 800 円になる |
| 正常系 | 組立費が小計に加算されてから消費税・送料が計算される |
| 正常系 | 複数明細の合計金額が正しく計算される |
| 正常系 | `addItem` で数量が既存明細に加算される |
| 正常系 | `addItem` で数量が 99 を超える場合 99 に切り詰められる |
| 正常系 | Cookie にない商品バリアントは明細に含まれない（スナップショット不在） |
| 境界値 | 数量 0 以下は 1 に正規化される |
| 境界値 | 数量 99 はそのまま维持される |
| 境界値 | 組立不可商品で組立指定が `false` に正規化される |
| 異常系 | 存在しない商品バリアントIDを追加した場合は `IllegalArgumentException` |

### 7.3 `MemberService` ／ `MemberServiceTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | 新規メールアドレスで会員登録が成功し `MemberSessionUser` が返る |
| 正常系 | 登録時にパスワードがハッシュ化されてリポジトリへ渡される |
| 正常系 | `existsByEmail` で null 入力は `false` を返す |
| 正常系 | `existsByEmail` で空文字入力は `false` を返す |
| 正常系 | `findActiveCredentialByEmail` で null 入力は `Optional.empty()` を返す |
| 正常系 | `findActiveCredentialByEmail` で前後空白は trim されてリポジトリへ渡される |
| 正常系 | お気に入り上限（100件）に達していない場合に追加できる |
| 正常系 | 追加お届け先 `findAdditionalAddresses` でページ番号 0 以下は 1 に補正される |
| 異常系 | 既存メールアドレスで登録した場合 `DuplicateEmailException` がスローされる |
| 異常系 | INSERT 後に重複が判明した場合（レースコンディション）も `DuplicateEmailException` |
| 異常系 | お気に入り上限（100件）超過時に `FavoritesLimitExceededException` |
| 異常系 | 追加お届け先上限（20件）超過時に `AddressLimitExceededException` |

### 7.4 `ProductService` ／ `ProductServiceTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `search` が正規化済み条件でリポジトリへ委譲し結果を返す |
| 正常系 | 許可外ページサイズが最近傍の許可値（15 / 30 / 60）へ丸められる |
| 正常系 | ページ番号 0 以下が 1 に補正される |
| 正常系 | `findRecentlyViewedProducts` が商品IDとlimitをリポジトリへ渡す |
| 正常系 | `findDetail` でリポジトリの結果をそのまま返す |
| 境界値 | `findTopNewArrivals` が limit=4 でリポジトリを呼び出す |
| 境界値 | `findTopRankedProducts` が limit=8 でリポジトリを呼び出す |

### 7.5 `ProductListSearchService` ／ `ProductListSearchServiceTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `buildCondition` で不正ページサイズが補正される |
| 正常系 | `buildCondition` で許可外カラーキーが除外される |
| 正常系 | `buildCondition` で許可外価格帯IDが除外される |
| 正常系 | `searchWithPageCorrection` で総件数0件はページ補正せずそのまま返す |
| 正常系 | `searchWithPageCorrection` でページ超過時は最終ページへ再検索される |
| 正常系 | `buildNewArrivalCondition` のデフォルトソートが `NEWEST` になる |

### 7.6 `ProductFilterOptionService` ／ `ProductFilterOptionServiceTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `buildDeskFilter` で許可外レンジIDが除外される |
| 正常系 | `buildChairFilter` で許可外素材IDが除外される |
| 正常系 | `buildStorageFilter` で全ID null の場合は空フィルターが返る |
| 正常系 | `loadOptionsBundle` でリポジトリの全候補が詰め込まれて返る |
| 境界値 | デスク幅許可レンジID（1〜11）の境界値が正しくフィルターされる |

### 7.7 `OrderRepository` ／ `OrderRepositoryTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `findCurrentTaxRatePercent` でMapper が null を返した場合、デフォルト 10 が返る |
| 正常系 | `findCurrentTaxRatePercent` でMapper が値を返した場合、その値がそのまま返る |
| 正常系 | `findCheckoutMemberPrefill` でMapper が null を返した場合 `Optional.empty()` が返る |
| 正常系 | `insertOrder` で返却値が正常にキャストされ注文IDとして返る |
| 正常系 | `nextOrderSequence` でMapper が正常値を返した場合、その連番が返る |
| 正常系 | `findMemberOrders` でページネーション計算（offset = (page-1) * size）が正しい |
| 異常系 | `insertOrder` で Mapper が null を返した場合 `IllegalStateException` |
| 異常系 | `nextOrderSequence` で Mapper が null を返した場合 `IllegalStateException` |
| 異常系 | `nextOrderSequence` で Mapper が 0 以下を返した場合 `IllegalStateException` |
| 正常系 | `parsePaymentInstruction` で有効な JSON が正しくパースされる |
| 正常系 | `parsePaymentInstruction` で null JSON の場合は空 Map が返る |
| 異常系 | `parsePaymentInstruction` で不正 JSON の場合は空 Map が返る（例外伝播しない） |

### 7.8 `ProductRepository` ／ `ProductRepositoryTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `findByProductIds` で null 入力は空リストを返す |
| 正常系 | `findByProductIds` で空リスト入力は空リストを返す |
| 正常系 | `findByProductIds` で limit=0 は空リストを返す |
| 正常系 | `findByProductIds` で重複IDが除去される |
| 正常系 | `findByProductIds` で `limit` の上限を超えるIDは切り捨てられる |
| 正常系 | `findByProductIds` で入力 ID 順に結果が返る（順序保持） |
| 正常系 | `findTopRankedProducts` でランキングテーブルが空の場合、新着商品で代替される |
| 正常系 | デスク幅レンジID 1 が `null〜999` の `RangeValue` へ変換される |
| 正常系 | デスク幅レンジID 11 が `3600〜null` の `RangeValue` へ変換される |
| 境界値 | `findNewestProducts` で新着判定基準時刻が「現在から6ヶ月前」として渡される |

### 7.9 `MemberRepository` ／ `MemberRepositoryTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `findActiveCredentialByEmail` でMapper が null を返した場合 `Optional.empty()` |
| 正常系 | `findProfileByMemberId` で7桁郵便番号が part1（3桁）・part2（4桁）に分割される |
| 正常系 | `findProfileByMemberId` で郵便番号 null の場合は分割されない |
| 正常系 | `insertMember` で Mapper が会員IDを返した場合、その ID が返る |
| 異常系 | `insertMember` で Mapper が null を返した場合 `IllegalStateException` |

### 7.10 `CartController` ／ `CartControllerTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `GET /cart` でカート画面（200 OK）が返る |
| 正常系 | `POST /cart/items` でカートへ商品追加後リダイレクトされる |
| 正常系 | `GET /cart/checkout/method` でカートが空の場合 `/cart` へリダイレクトされる |
| 正常系 | `GET /cart/checkout/confirm` でセッションにフォームがない場合 `/cart` へリダイレクトされる |
| 正常系 | `POST /cart/checkout/place` でワンタイムトークン不一致の場合 `/cart` へリダイレクトされる |
| 異常系 | `POST /cart/items` でバリデーションエラー（数量 0）の場合 400 を返す |

### 7.11 `CatalogController` ／ `CatalogControllerTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `GET /products/{productId}` で存在する商品ID は 200 OK と詳細画面を返す |
| 異常系 | `GET /products/{productId}` で存在しない商品ID は 404 を返す |
| 正常系 | `GET /products/recently-viewed` で空の ids パラメータは空リストを返す |
| 正常系 | `GET /products/search` でキーワードなし検索は 200 OK を返す |
| 正常系 | `GET /categories/desks` で 200 OK と一覧画面を返す |

### 7.12 `MyPageController` ／ `MyPageControllerTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `GET /mypage` は `/mypage/orders` へリダイレクトされる |
| 正常系 | ログイン済みで `GET /mypage/orders` は 200 OK を返す |
| 異常系 | 未ログインで `GET /mypage/orders` は 401 を返す |
| 異常系 | 未ログインで `GET /mypage/favorites` は 401 を返す |
| 異常系 | `GET /mypage/orders/{orderNumber}` で存在しない注文番号は 404 を返す |
| 正常系 | `POST /mypage/withdraw` でセッション破棄後 `/login` へリダイレクトされる |

### 7.13 `HomeController` ／ `HomeControllerTest`

| 分類 | 検証観点 |
|------|---------|
| 正常系 | `GET /` でトップ画面（200 OK）が返る |
| 正常系 | `GET /` で新着・ランキング商品がモデルに設定される |
| 正常系 | `GET /announcements` でお知らせ一覧画面（200 OK）が返る |

---

## 8. 実装優先度・ロードマップ

### 8.1 優先度定義

| 優先度 | 基準 |
|--------|------|
| **P1（最優先）** | ビジネスクリティカルなロジック（注文採番・税計算・金額計算・会員登録） |
| **P2（高）** | 業務バリデーション・エラーハンドリング（在庫チェック・上限件数チェック） |
| **P3（中）** | 一覧検索条件組み立て・データ整形ロジック |
| **P4（低）** | 単純な委譲のみのメソッド・Controller の描画確認 |

### 8.2 実装ロードマップ

| フェーズ | 対象 | 優先度 | 状態 |
|---------|------|--------|------|
| Phase 1 | `OrderServiceTest`（既存テスト拡充） | P1 | 一部完了 |
| Phase 1 | `CartServiceTest`（金額計算・送料計算） | P1 | 未着手 |
| Phase 1 | `MemberServiceTest`（会員登録・重複チェック） | P1 | 未着手 |
| Phase 2 | `OrderRepositoryTest`（JSON変換・null安全） | P2 | 未着手 |
| Phase 2 | `MemberRepositoryTest`（郵便番号分割・null安全） | P2 | 未着手 |
| Phase 2 | `ProductRepositoryTest`（ID正規化・フォールバック） | P2 | 未着手 |
| Phase 3 | `ProductServiceTest` | P3 | 未着手 |
| Phase 3 | `ProductListSearchServiceTest` | P3 | 未着手 |
| Phase 3 | `ProductFilterOptionServiceTest` | P3 | 未着手 |
| Phase 4 | `CartControllerTest` | P4 | 未着手 |
| Phase 4 | `CatalogControllerTest` | P4 | 未着手 |
| Phase 4 | `MyPageControllerTest` | P4 | 未着手 |
| Phase 4 | `HomeControllerTest` | P4 | 未着手 |

---

## 9. テスト実行環境・依存ライブラリ

### 9.1 依存ライブラリ（pom.xml 既存）

| ライブラリ | バージョン | 用途 |
|-----------|----------|-----|
| `spring-boot-starter-test` | 3.5.x | JUnit 5 / Mockito / AssertJ 統合 |
| `junit-jupiter` | 5.x | テストフレームワーク |
| `mockito-junit-jupiter` | 5.x | Mockito + JUnit5 統合 |
| `assertj-core` | 3.x | 流暢アサーション |

### 9.2 JaCoCo（追加が必要）

`pom.xml` に JaCoCo Maven Plugin を追加する（セクション 4.2 参照）。

### 9.3 テスト実行コマンド

```bash
# 全テスト実行
mvn test

# 特定テストクラスのみ実行
mvn test -Dtest=OrderServiceTest

# テスト + カバレッジレポート生成
mvn test jacoco:report
```

---

## 10. テストコード配置規則

```
src/test/java/jp/co/skig/officeorder/
├── service/
│   ├── order/
│   │   └── OrderServiceTest.java       ← 既存
│   ├── cart/
│   │   └── CartServiceTest.java
│   ├── member/
│   │   └── MemberServiceTest.java
│   └── product/
│       ├── ProductServiceTest.java
│       ├── ProductListSearchServiceTest.java
│       └── ProductFilterOptionServiceTest.java
├── repository/
│   ├── OrderRepositoryTest.java
│   ├── ProductRepositoryTest.java
│   └── MemberRepositoryTest.java
└── web/
    ├── CartControllerTest.java
    ├── CatalogControllerTest.java
    ├── MyPageControllerTest.java
    └── HomeControllerTest.java
```

テストクラスは **テスト対象クラスと同一パッケージ** に配置する（パッケージプライベートへのアクセスを確保するため）。

---

## 11. 完了基準

以下をすべて満たした状態をテスト実装の完了とする。

- [ ] Phase 1〜4 のすべてのテストクラスが作成され、各検証観点に対応するテストメソッドが実装されている
- [ ] `mvn test` がエラーなく完了する
- [ ] Service 層の行カバレッジが **80% 以上**
- [ ] Repository 層の行カバレッジが **70% 以上**
- [ ] Controller 層の行カバレッジが **60% 以上**
- [ ] すべてのテストメソッドに `@DisplayName` による日本語説明が付与されている
- [ ] テスト実行時間が 60 秒以内（CI での待機時間を考慮）
