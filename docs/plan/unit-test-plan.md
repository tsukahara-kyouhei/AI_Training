# 単体テスト計画書 (Unit Test Plan)

## 1. 目的
本ドキュメントは、`jp.co.skig.officeorder.service` パッケージ配下のサービス層における単体テストの実施方針を定義する。

## 2. テスト対象
`src/main/java/jp.co.skig.officeorder.service` 配下のすべてのサービス類（`announcement`, `batch`, `cart`, `contact`, `mail`, `member`, `product`）。

## 3. テストフレームワーク・ツール
* **テストフレームワーク**: JUnit 5 (JUnit Jupiter)
* **アサーション**: AssertJ
* **モックフレームワーク**: Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`)

## 4. テスト方針
* **独立性の確保**: データベースや外部依存クラスはすべて Mockito を用いてモック化し、サービス単体のロジックを検証する。
* **テスト範囲**: 各メソッドの正常系処理、および例外・境界値などの主要な異常系処理。
* **カバレッジ目標**: C0（行カバレッジ）80% 以上を維持する。