# テスト方針書

## 1. テスト戦略

本プロジェクトは単体テストを中心に置き、表層の結合テストを補助的に実施する。

---

## 2. テストレベル一覧

| レベル | 対象 | ツール | 場所 |
|---|---|---|---|
| 単体テスト | Service・ユーティリティ | JUnit 5, Mockito | src/test/java |
| スライステスト | Controller・認証 | Spring MVC Test | src/test/java |
| 結合テスト | Repository + DB | Spring Boot Test + Testcontainers | src/test/java |
| バッチテスト | Spring Batch ジョブ | @SpringBatchTest | src/test/java |
| E2Eテスト | 画面操作 | 手動テスト（対象外） | - |

---

## 3. テスト命名規則

### 3.1 メソッド名規約

テストメソッドは `対象メソッド名_条件_期待結果` の形式で統一する。

```java
// 正例
void placeOrder_success_returnsOrderNumber()
void placeOrder_stockShortage_throwsStockException()
void login_withWithdrawnMember_throwsDisabledException()
void findById_notExists_returnsEmpty()
```

### 3.2 テストクラス名規約

| 区分 | 命名規則 | 例 |
|---|---|---|
| Serviceテスト | `{クラス名}Test` | `OrderServiceTest` |
| Controllerテスト | `{クラス名}Test` | `OrderControllerTest` |
| Repositoryテスト | `{クラス名}Test` | `OrderMapperTest` |
| バッチテスト | `{ジョブクラス名}Test` | `RankingCalculationJobTest` |

---

## 4. 単体テスト方針

### 4.1 Serviceテスト

- MockitoでRepositoryをモック化
- クロックは `AppClockConfig` を使って固定時刻に切り替えてテスト
- 後条件とエラーケースを両方検証

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CartService cartService;
    @InjectMocks OrderService orderService;

    @Test
    void placeOrder_success_returnsOrderNumber() { ... }
    @Test
    void placeOrder_stockShortage_throwsStockException() { ... }
}
```

### 4.2 Controllerテスト（Spring MVC Test）

- `@WebMvcTest` でController層のみ起動
- Serviceを`@MockBean`でモック化
- HTTPステータス・レスポンス内容・バリデーションエラーを検証

### 4.3 セキュリティテスト

- `spring-security-test` を使用（`@WithMockUser`、`@WithUserDetails`）
- 主要な検証観点：

| 検証観点 | 方法 |
|---|---|
| 未認証でのアクセス制御 | `@WithAnonymousUser` で `/mypage/**` にアクセスし、302リダイレクトを検証 |
| 認証済みアクセス | `@WithMockUser(roles = "MEMBER")` で正常レスポンス（200）を検証 |
| CSRF検証 | POSTリクエストにCSRFトークンを含めない場合に403を検証 |
| 退会済み会員の強制ログアウト | `@WithUserDetails` で `MemberActiveValidationInterceptor` の動作を検証 |

```java
@WebMvcTest(MyPageController.class)
class MyPageControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void mypage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/mypage"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void mypage_authenticated_returnsOk() throws Exception {
        mockMvc.perform(get("/mypage"))
            .andExpect(status().isOk());
    }
}
```

### 4.4 メール送信テスト

- `JavaMailSender` を `@MockBean` でモック化し、`NotificationMailService` の送信呼び出しを検証
- `ArgumentCaptor` にて送信先アドレス・件名の主要項目を検証
- メールはトランザクションコミット後に送信されるため、Service層の単体テストで `afterCommit` コールバックが登録されることを確認する

```java
@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks NotificationMailService notificationMailService;

    @Test
    void sendRegistrationMail_success_sendsToCorrectAddress() {
        ArgumentCaptor<SimpleMailMessage> captor =
            ArgumentCaptor.forClass(SimpleMailMessage.class);

        notificationMailService.sendRegistrationMail("test@example.com", "テストユーザー");

        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).contains("test@example.com");
    }
}
```

---

## 5. 結合テスト方針

### 5.1 Repositoryテスト

- `@SpringBootTest` + `@Transactional`（テスト後ロールバック）
- テスト用PostgreSQLはTestcontainersを利用（`@Testcontainers` + `@Container`）
- `sql/seed/test-data/`以下のテストデータを投入

```java
@SpringBootTest
@Testcontainers
@Transactional
class OrderMapperTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void findById_existingOrder_returnsOrder() { ... }
}
```

---

## 6. バッチテスト方針

- `@SpringBatchTest` + `JobLauncherTestUtils` を使用
- バッチ実行後の結果テーブルや集計値をDBで直接検証
- テスト用DBはTestcontainersを使用（結合テストと同じ方針）

| ジョブ | テスト観点 |
|---|---|
| `rankingCalculationJob` | `popular_product_rankings` テーブルの再計算結果を検証 |
| `recommendationCalculationJob` | `recommended_related_products` テーブルの再計算結果を検証 |

```java
@SpringBatchTest
@SpringBootTest
@Testcontainers
class RankingCalculationJobTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Test
    void rankingCalculationJob_success_completesWithCompletedStatus() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob();
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void rankingCalculationJob_success_updatesRankingTable() throws Exception {
        // テストデータ投入後にジョブ実行 → popular_product_rankings の内容を検証
    }
}
```

---

## 7. テスト環境設定

### 7.1 テスト用プロパティ

`src/test/resources/application-test.yml` にテスト専用設定を定義する。Testcontainersが起動後に `@DynamicPropertySource` でデータソースURLを上書きするため、デフォルト値はダミーで問題ない。

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test_placeholder
  mail:
    host: localhost
    port: 1025
  batch:
    job:
      enabled: false
```

### 7.2 Testcontainers 使用方針

| 使用場面 | 方法 |
|---|---|
| Repositoryテスト | `@Testcontainers` + `static @Container` でクラス単位に起動 |
| バッチテスト | 同上 |
| 複数クラス間でコンテナ共有 | 共通の `AbstractContainerBaseTest` を継承させる |
| ローカル開発の確認 | `docker-compose.yml` のDBを直接使用することも可 |

---

## 8. カバレッジ目標

### 8.1 Service層（単体テスト）

Service を業務上の重要度で2段階に分けて目標を設定する。

| 区分 | 対象Service | 目標 | 理由 |
|---|---|---|---|
| クリティカル | `OrderService`、`CartService` | **95%以上**（分岐カバレッジ） | 在庫操作・金額計算・ゲスト/会員分岐が金銭に直結 |
| 標準 | `MemberService` | **90%以上**（分岐カバレッジ） | 上限チェック・email重複がセキュリティ・業務ルールに直結 |
| 参照系 | `ProductService`、`AnnouncementService`、`ContactService` 等 | **80%以上**（分岐カバレッジ） | 参照・表示ロジック中心で業務影響が限定的 |

### 8.2 単体テストのカバレッジ対象外

以下は単体テストの計測対象外とし、結合テストで担保する。

| 対象外の分岐 | 代替検証手段 |
|---|---|
| `TransactionSynchronizationManager` の `afterCommit` コールバック（メール送信トリガー） | `NotificationMailService` の結合テストでコミット後の送信を確認 |
| DB接続断・メールサーバー停止時のインフラ例外 `catch` ブロック | メール再試行（`MailRetryConfig`）の結合テストで動作確認 |
| Spring Batch フレームワーク制御フロー（ステップのロールバックパス等） | バッチ結合テスト（`@SpringBatchTest`）で異常系ジョブ実行を検証 |

> 対象外コードは `@ExcludeFromJacocoGeneratedReport` アノテーションを付与し、JaCoCoの集計から明示的に除外する。

### 8.3 その他の層

| 対象 | 目標 |
|---|---|
| Repository層 | 主要メソッドの結合テスト（Testcontainers使用） |
| Controller層 | 正常ケース・バリデーションエラーケース・未認証リダイレクト |

> JaCoCo は現時点では `pom.xml` に未設定。導入する場合は `maven-jacoco-plugin` を追加した上で `.\mvnw verify` でレポートを生成する。

---

## 9. テスト実行方法

```powershell
# 全テスト実行
.\mvnw test

# 特定のテストクラスのみ
.\mvnw test -Dtest=OrderServiceTest

# カバレッジレポート生成（JaCoCo導入後）
.\mvnw verify
```

---

## 10. テストデータ

`sql/seed/test-data/` 以下に標準テストデータを管理する。

| ファイル | 内容 |
|---|---|
| members.sql | テスト用会員データ |
| products.sql | テスト用商品データ |
| orders.sql | テスト用注文データ |
| content.sql | テスト用コンテンツデータ |
| post-seed-check.sql | データ投入後の整合性確認クエリ |
