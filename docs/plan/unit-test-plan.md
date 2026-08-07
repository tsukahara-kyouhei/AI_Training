# 単体テスト計画

## 目的

本計画は、本プロジェクトにおける単体テストの対象、進め方、品質基準を定めるものです。対象はサービス層、ユーティリティ／ドメイン層を中心とし、コントローラ層は単体テストの範囲外とします。

## テスト方針

### 1. 対象範囲

- 主にテストするのは以下の層
  - サービスクラス（`service` パッケージ）
  - ドメインロジック／ユーティリティクラス（`common`、`model`、`service/*` 内のビジネスロジック）
  - 独立した振る舞いを持つヘルパークラス（`SearchKeywordNormalizer`、`MoneyFormatter`、`AppTimeProvider` など）
- 単体テスト対象外:
  - コントローラ層（`web` パッケージ）の MVC 入出力ロジック
  - MyBatis や JDBC などデータアクセス層の実行を伴う実DB依存処理
  - Spring ApplicationContext を必要とする統合的なコンテキストテスト

### 2. テスト技法

- 1クラスあたり1テストクラスを作成し、`src/test/java` にプロダクションコードと同じパッケージ構成で配置する。
- テストフレームワークは JUnit Jupiter（JUnit 5）を使用する。
- モックフレームワークは Mockito を基本とする。
- テストは `パス/正常系`、`境界値`、`例外系/異常系` の3分類で整理する。

## モック（Mock）の利用ルール

### 1. 基本ルール

- 単体テストでは「テスト対象クラス以外の依存先」を Mockito でモックする。
- 例: サービスクラスのテストではリポジトリ、外部サービス、メッセージソース、外部 API クライアント、時刻提供者などをモックする。
- 依存対象をモックすることで、テスト対象のビジネスロジックのみを評価し、外部要素の状態変動を排除する。

### 2. モックすべき例

- `OrderService` の依存先: `OrderRepository`, `NotificationMailService`, `ObjectMapper`, `Clock`, `MessageSource`
- `MemberService` の依存先: `MemberRepository`, `CartRepository`, `NotificationMailService` など
- `CartService` の依存先: `CartRepository`, `ProductRepository`, `MemberRepository`
- `ProductListSearchService` / `ProductService` の依存先: `ProductRepository`, `ProductFilterOptionRepository`, `SearchKeywordNormalizer`（必要に応じて実インスタンスとする）

### 3. モックしないもの

- ドメインモデル、DTO、フォームオブジェクト、単純な値オブジェクト
- テスト対象クラスが本来生成・操作するオブジェクト
- 上記はできるだけ実オブジェクトを使って、挙動を観察する

### 4. Mockito の使い方ガイド

- コンストラクタインジェクションが利用できるクラスでは `@InjectMocks` を使うか、テスト内で `new` にモックを渡す。
- `@Mock` で依存先を定義し、`when(...).thenReturn(...)` で戻り値を設定する。
- `verify(...)` で重要な呼び出しや副作用の発生を検証する。
- `spy` は状態検証や部分モックが必要な場合に限定して使い、簡潔さを優先する。

### 5. モックの乱用を避けるための注意

- 依存先の内部実装に強く結びつく期待値は避ける。
- モックした依存先の動作を過度に細かく定義すると、リファクタリング時にテストが壊れやすくなる。
- 可能ならば、ドメインロジックはモックではなく実インスタンスで検証する。

## 対象クラスの優先度

### 優先度高（最初にカバーすべき）

- `jp.co.skig.officeorder.service.order.OrderService`
- `jp.co.skig.officeorder.service.cart.CartService`
- `jp.co.skig.officeorder.service.member.MemberService`
- `jp.co.skig.officeorder.service.product.ProductService`
- `jp.co.skig.officeorder.service.product.ProductListSearchService`
- `jp.co.skig.officeorder.service.product.ProductFilterOptionService`
- `jp.co.skig.officeorder.service.mail.NotificationMailService`
- `jp.co.skig.officeorder.service.contact.ContactService`
- `jp.co.skig.officeorder.web.auth.MemberUserDetailsService`（ドメインの認証ロジックを含むため）

### 優先度中

- `jp.co.skig.officeorder.common.MoneyFormatter`
- `jp.co.skig.officeorder.service.product.SearchKeywordNormalizer`
- `jp.co.skig.officeorder.web.auth.LoginEmailCookieService`
- `jp.co.skig.officeorder.service.member.MemberSessionService`
- `jp.co.skig.officeorder.config.AppClockConfig` などの設定ユーティリティ（必要に応じて）

### 優先度低

- 単純なリポジトリラッパや POJO だけのクラス
- MyBatis マッパーインターフェース自体（別途統合テストでカバー）

## テストケース設計の指針

各テストクラスで以下を必ず扱う。

### 1. 正常系

- 期待どおりの戻り値が返ること
- 依存先の呼び出しが想定どおり行われること
- 入力データの正規化・集計・変換が正しく行われること

### 2. 異常系

- 入力検証で例外やエラーが発生する箇所（`IllegalArgumentException` など）
- 外部依存が `null` や空リストを返した場合の挙動
- 期待される例外メッセージやログ出力の確認

### 3. 境界値・分岐

- 数値境界値（数量、金額、税計算など）
- 任意/必須項目切り替えによる分岐
- `memberId` が `null`（ゲスト）と非 `null`（会員）での異なるフロー
- 在庫確認、組立オプション、支払方法ごとの分岐

## カバレッジ目標基準

### 1. 全体目標

- プロジェクト全体の単体テストカバレッジは `80%以上` を目標とします。

### 2. 重要ドメインの重点目標

- `order`, `cart`, `member`, `product` の主要サービスクラスでは `90%以上` を目標とします。
- 重要箇所は網羅的な分岐カバレッジを重視し、単に行数を通すだけでなく、主要ビジネスフローの分岐を確認します。

### 3. カバレッジ測定対象

- メソッドレベルの網羅だけでなく、分岐（`if` / `switch`）と例外パスも含める。
- Getter/Setter のみのクラスは対象外だが、独自ロジックを持つ DTO は含む。

## 実行と運用

### 1. 実行コマンド

- 単体テスト実行:
  ```bash
  ./mvnw.cmd test
  ```
- カバレッジ測定（JaCoCo などを追加する場合）:
  - 現状の POM には JaCoCo が含まれていないため、必要なら `jacoco-maven-plugin` を追加し、`mvnw.cmd test jacoco:report` を実行する。

### 2. テストファイル配置

- `src/test/java/jp/co/skig/officeorder/...`
- `src/test/resources` は必要に応じてダミーデータやメッセージリソースを配置する。

### 3. 命名規則

- テストクラス名: `対象クラス名 + Test` 例: `OrderServiceTest`
- テストメソッド名: `scenario_expectedBehavior` 例: `placeOrder_guestWithValidCart_createsOrder`

## 追加ルール

### 1. 依存性の分離

- 単体テストでは Spring コンテキストをロードせず、純粋に JUnit + Mockito で完結するものを原則とする。
- `@SpringBootTest` や `@WebMvcTest` は単体テスト計画には含めず、後段の統合テスト / Web スライステストで扱う。

### 2. テストが読めること

- テストコードはビジネスロジックの仕様書としても機能するよう、条件・期待値を明確に記述する。
- 複雑なモック設定は helper メソッドに切り出し、テストの意図が読みやすいようにする。

### 3. モックの検証方法

- 主要な副作用は `verify()` で確認する。
- 返却値の検証だけでなく、重要な `insert`/`send`/`notify` 呼び出しが行われたかをチェックする。

## 今後の拡張

- リポジトリ層／MyBatis マッパーの検証は、別途「統合テスト計画」にて DB を用いた検証対象とする。
- Web 入出力とコントローラの振る舞いは、別途 `Web スライステスト計画` を作成する。

---

ファイル: `docs/plan/unit-test-plan.md`
