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
| E2Eテスト | 画面操作 | 手動テスト | - |

---

## 3. 単体テスト方針

### 3.1 Serviceテスト

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
    void placeOrder_success() { ... }
    @Test
    void placeOrder_stockShortage_throwsException() { ... }
}
```

### 3.2 Controllerテスト（Spring MVC Test）

- `@WebMvcTest` でController層のみ起動
- Serviceを`@MockBean`でモック化
- HTTPステータス・レスポンス内容を検証

---

## 4. 結合テスト方針

### 4.1 Repositoryテスト

- `@SpringBootTest` + `@Transactional`（テスト後ロールバック）
- テスト用PostgreSQLはDockerComposeまたはTestcontainersを利用
- `sql/seed/test-data/`以下のテストデータを投入

---

## 5. カバレッジ目標

| 対象 | 目標 |
|---|---|
| Service層 | 80%以上（分岐カバレッジ） |
| Repository層 | 主要メソッドの結合テスト |
| Controller層 | 正常ケース・バリデーションエラーケース |

---

## 6. テスト実行方法

```powershell
# 全テスト実行
.\mvnw test

# 特定のテストクラスのみ
.\mvnw test -Dtest=OrderServiceTest

# カバレッジレポート生成（JaCoCo使用時）
.\mvnw verify
```

---

## 7. テストデータ

`sql/seed/test-data/` 以下に標準テストデータを管理する。

| ファイル | 内容 |
|---|---|
| members.sql | テスト用会員データ |
| products.sql | テスト用商品データ |
| orders.sql | テスト用注文データ |
| content.sql | テスト用コンテンツデータ |
| post-seed-check.sql | データ投入後の整合性確認クエリ |
