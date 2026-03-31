# FEAT-001 商品検索機能強化 実装計画 TODO リスト

> 要件定義書: `docs/issues/FEAT-001-requirements.md`
> 設計書: `docs/design/FEAT-001-*.md`
> 実装ブランチ: `ebihara.toshikazu`

---

## 実装方針の前提確認（未決事項 #1 の決定）

- **全角・半角正規化の方式**: DB カラム値は PostgreSQL カスタム関数 `normalize_fullwidth(text)` で全角変換する方式を採用する（SQL設計書 §3.1 の推奨方式）
- **正規化の方向**: 半角 → 全角（数字・英字・カタカナ）
- **Java 側の変換**: `TextNormalizer.toFullWidth()` を新規作成し、文字コード算術変換で実装する（ICU4J 等の外部ライブラリは追加しない）

---

## TODO リスト

### Phase 1: DB 変更

- [x] **1-1. `normalize_fullwidth` PostgreSQL 関数を SQL ファイルに追加する**
  - 対象ファイル: `sql/schema/products.sql`（末尾に追記）
  - 内容: SQL設計書 §3.2 に記載の `CREATE OR REPLACE FUNCTION normalize_fullwidth(input text) RETURNS text` を追加
  - 変換対象: 半角数字 → 全角数字、半角英大小文字 → 全角英大小文字、半角カタカナ → 全角カタカナ
  - 関数属性: `IMMUTABLE`（同一入力に対して常に同一結果を返すため）
  - 注意: 半角カタカナの変換（濁点・半濁点の合成）は Java 側 `TextNormalizer` と統一した変換ロジックにすること

---

### Phase 2: Model 変更

- [x] **2-1. `ProductSearchCondition` に `tasteNames` フィールドを追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`
  - 変更内容: `record` に `List<String> tasteNames` コンポーネントを末尾に追加
  - 影響確認: `new ProductSearchCondition(...)` 呼び出し箇所（`ProductListSearchService.buildCondition()` 内）をすべて更新すること
  - 設計根拠: テイスト絞り込みはカテゴリ横断であるため `ProductCategoryFilter` ではなく `ProductSearchCondition` 直下に配置（クラス設計書 §1 参照）
  - **実装済み更新箇所**: `ProductService.buildCondition()` / `normalize()`, `ProductRepository.findNewestProducts()` / `findRecommendedProducts()`, `ProductListSearchService.searchWithPageCorrection()`

---

### Phase 3: ユーティリティクラスの新規作成

- [x] **3-1. `TextNormalizer` クラスを新規作成する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/util/TextNormalizer.java`（新規作成）
  - パッケージ: `jp.co.skig.officeorder.util`
  - クラス仕様:
    - `final class`、コンストラクタは `private`（インスタンス化禁止）
    - `public static String toFullWidth(String input)` メソッドを実装
  - 変換仕様（クラス設計書 §3.3 参照）:
    - `null` または空文字の場合は入力値をそのまま返す
    - 半角数字 (`0`–`9`) → 全角数字 (`０`–`９`): コードポイント差分 `+0xFEE0` の算術変換
    - 半角英大文字 (`A`–`Z`) → 全角英大文字 (`Ａ`–`Ｚ`): コードポイント差分 `+0xFEE0` の算術変換
    - 半角英小文字 (`a`–`z`) → 全角英小文字 (`ａ`–`ｚ`): コードポイント差分 `+0xFEE0` の算術変換
    - 半角カタカナ（濁点・半濁点の合字を含む）→ 全角カタカナ: 文字対応テーブルでマッピング
    - 上記以外の文字（全角文字・ひらがな・漢字等）: 変換しない

---

### Phase 4: Mapper インターフェースの変更

- [x] **4-1. `ProductMapper` に `selectUnifiedSearchTasteOptions()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`
  - 追加内容:
    ```java
    /** 検索結果画面用の統合テイスト選択肢（3カテゴリ統合）を取得する。 */
    List<ProductFilterOptionMapperRow> selectUnifiedSearchTasteOptions();
    ```
  - 戻り値の型: 既存の `ProductFilterOptionMapperRow`（`optionId`, `displayName`）を再利用
  - 注意: `optionId` は本クエリでは使用しない（`displayName` のみ使用）

---

### Phase 5: Mapper XML の変更

- [x] **5-1. `BaseProductWhere` フラグメントのキーワード条件を変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更内容（SQL設計書 §2 参照）: 既存のキーワード条件ブロックを以下に置き換え
    - `p.product_name` に `normalize_fullwidth()` を適用して `#{keywordLike}` で部分一致
    - `p.variation_name` を新規追加（`IS NOT NULL` チェック付き）、`normalize_fullwidth()` を適用して部分一致
    - `p.description` を新規追加、`normalize_fullwidth()` を適用して部分一致
    - 商品コードの一致条件を `ILIKE #{keywordLike}`（部分一致）から `= #{keywordExact} OR ILIKE #{keywordPrefix}`（完全一致・前方一致）に変更し、`normalize_fullwidth()` を適用

- [x] **5-2. `SearchTasteFilter` フラグメントを新規追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 内容（SQL設計書 §4 参照）: `<sql id="SearchTasteFilter">` を追加
    - `hasSearchTasteFilter` が `true` のとき AND 条件を追加
    - デスク・チェア・収納の各属性テーブルに対して `EXISTS` + `display_name IN (...)` でフィルタリング
    - 3つの `EXISTS` を `OR` で結合（カテゴリ横断でいずれかにマッチすれば表示対象）

- [x] **5-3. `BaseProductWhere` に `SearchTasteFilter` を include する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更内容: `BaseProductWhere` フラグメントの末尾（カテゴリ固有フィルタの後）に `<include refid="SearchTasteFilter"/>` を追加

- [x] **5-4. `selectUnifiedSearchTasteOptions` クエリを新規追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 内容（SQL設計書 §5 参照）: 3カテゴリの `*_tastes` テーブルを `UNION ALL` し、`GROUP BY display_name` で重複排除、`MIN(sort_order)` 昇順で並び替え
  - `resultMap`: 既存の `ProductFilterOptionRowMap` を流用（`NULL AS option_id` を明示してマッピングを安定させる）

---

### Phase 6: Repository 層の変更

- [x] **6-1. `ProductFilterOptionRepository` に `findUnifiedSearchTasteOptions()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`
  - 追加内容（クラス設計書 §5 参照）: `displayName` のみを抽出して返す

- [x] **6-2. `ProductRepository.buildSearchParams()` を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
  - 変更内容（クラス設計書 §6 参照）:
    - キーワード関連: `keywordLike`（`%keyword%`）、`keywordExact`（`keyword`）、`keywordPrefix`（`keyword%`）の3つをセット
    - テイスト関連（新規追加）: `searchTasteNames` と `hasSearchTasteFilter` をセット
  - `toKeywordLike()` ヘルパーメソッドは削除（インライン化）

---

### Phase 7: Service 層の変更

- [x] **7-1. `ProductListSearchService` に `normalizeSearchKeyword()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
  - `private String normalizeSearchKeyword(String rawKeyword)` を追加
  - `rawKeyword.strip()` → `TextNormalizer.toFullWidth()` で全角正規化

- [x] **7-2. `ProductListSearchService.buildCondition()` を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
  - 変更内容: 新規 11-param オーバーロード（タスト名追加）を追加。既存 10-param は維持
  - キーワードを `normalizeSearchKeyword()` で正規化してから `ProductSearchCondition.keyword` に設定
  - `tasteNames` を `ProductSearchCondition.tasteNames` に設定

- [x] **7-3. `ProductFilterOptionService` に `loadUnifiedSearchTasteOptions()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - `productFilterOptionRepository.findUnifiedSearchTasteOptions()` に委譲

- [x] **7-4. `ProductFilterOptionService` に `normalizeSearchTasteNames()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - `HashSet` を使ったホワイトリスト照合で有効な名称のみを残し、重複排除して返す

---

### Phase 8: Controller の変更

- [x] **8-1. `CatalogController.searchResults()` にテイストパラメータを追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更内容（クラス設計書 §2 参照）:
    - 引数に `@RequestParam(value = "taste", required = false) List<String> rawTasteNames` を追加
    - `loadUnifiedSearchTasteOptions()` → `normalizeSearchTasteNames()` → `buildCondition(..., tasteNames)` の順で処理
    - `model.addAttribute("searchTasteOptions", tasteOptions)` / `"selectedTasteNames"` を追加

---

### Phase 9: テンプレートの変更

- [x] **9-1. 検索結果画面にテイスト絞り込み UI を追加する**
  - 対象ファイル: `src/main/resources/templates/pages/product-list-search-results.html`
  - 変更内容（UI設計書 §4 参照）:
    - サイドバーの絞り込みフォームの「カラー」セクションの後にテイストセクションを追加
    - Thymeleaf: `th:each="tasteName : ${searchTasteOptions}"` でチェックボックス一覧を生成
    - ページネーションリンクに `taste=${selectedTasteNames}` を追加
    - 並び替えフォームに `<input type="hidden" name="taste" th:each="...">` を追加

---

### Phase 10: 単体テスト

> テストディレクトリ: `src/test/java/jp/co/skig/officeorder/`（新規作成）
> テストフレームワーク: JUnit 5 + Mockito（`spring-boot-starter-test` 経由）

- [x] **10-1. `TextNormalizerTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/util/TextNormalizerTest.java`
  - 半角数字・英字・カタカナ（濁点・半濁点含む）変換、null/空文字、全角入力の変換なし、べき等性を検証

- [x] **10-2. `ProductFilterOptionServiceTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`
  - `normalizeSearchTasteNames()`: null・空・有効値・無効値除外・重複排除・全無効 を検証
  - `loadUnifiedSearchTasteOptions()`: Repository への委譲を検証

- [x] **10-3. `ProductListSearchServiceKeywordNormalizationTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceKeywordNormalizationTest.java`
  - `buildCondition(…, tasteNames)` 経由でキーワード正規化・tasteNames 設定を検証
  - null キーワード・空白キーワード・tasteNames の pass-through を検証

- [ ] **10-4. `ProductRepositoryTest` を作成する** *(未実装 – 優先度: 中)*
  - `buildSearchParams()` が `search()` に渡す Map パラメータをキャプチャして `keywordLike` / `keywordExact` / `keywordPrefix` / `hasSearchTasteFilter` を検証

- [ ] **10-5. `CatalogControllerSearchResultsTest` を作成する** *(未実装 – 優先度: 中)*
  - `@WebMvcTest` + MockMvc を使い、`?taste=ナチュラル` リクエストでモデル属性を検証

---

## 実装順序（依存関係に基づく推奨順）

```
Phase 1 (DB)    → Phase 2 (Model) → Phase 3 (Util)
                                         ↓
Phase 4 (Mapper IF) ← ─────────── ─────┤
     ↓                                   │
Phase 5 (Mapper XML)                     │
     ↓                                   │
Phase 6 (Repository)                     │
     ↓                                   │
Phase 7 (Service) ←─────────────────────┘
     ↓
Phase 8 (Controller)
     ↓
Phase 9 (Template)
     ↓
Phase 10 (Unit Tests)  ←── 各 Phase 完了後に随時追加可
```

---

## 変更ファイル一覧

| Phase | ファイルパス | 種別 | 状態 |
|-------|------------|------|------|
| 1 | `sql/schema/products.sql` | 変更 | ✅ |
| 2 | `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java` | 変更 | ✅ |
| 2 | `src/main/java/jp/co/skig/officeorder/service/product/ProductService.java` | 変更 | ✅ |
| 2 | `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java` | 変更 | ✅ |
| 2 | `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java` | 変更 | ✅ |
| 3 | `src/main/java/jp/co/skig/officeorder/util/TextNormalizer.java` | **新規** | ✅ |
| 4 | `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java` | 変更 | ✅ |
| 5 | `src/main/resources/mappers/ProductMapper.xml` | 変更 | ✅ |
| 6 | `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java` | 変更 | ✅ |
| 6 | `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java` | 変更 | ✅ |
| 7 | `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java` | 変更 | ✅ |
| 7 | `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java` | 変更 | ✅ |
| 8 | `src/main/java/jp/co/skig/officeorder/web/CatalogController.java` | 変更 | ✅ |
| 9 | `src/main/resources/templates/pages/product-list-search-results.html` | 変更 | ✅ |
| 10 | `src/test/java/jp/co/skig/officeorder/util/TextNormalizerTest.java` | **新規** | ✅ |
| 10 | `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java` | **新規** | ✅ |
| 10 | `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceNormalizeTest.java` | **新規** | ✅ |
| 10 | `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceKeywordNormalizationTest.java` | **新規** | ✅ |
| 10 | `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTest.java` | **新規** | ⬜ 未実装 |
| 10 | `src/test/java/jp/co/skig/officeorder/web/CatalogControllerTest.java` | **新規** | ⬜ 未実装 |
