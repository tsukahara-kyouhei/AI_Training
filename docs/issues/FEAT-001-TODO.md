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

- [ ] **1-1. `normalize_fullwidth` PostgreSQL 関数を SQL ファイルに追加する**
  - 対象ファイル: `sql/schema/products.sql`（末尾に追記）
  - 内容: SQL設計書 §3.2 に記載の `CREATE OR REPLACE FUNCTION normalize_fullwidth(input text) RETURNS text` を追加
  - 変換対象: 半角数字 → 全角数字、半角英大小文字 → 全角英大小文字、半角カタカナ → 全角カタカナ
  - 関数属性: `IMMUTABLE`（同一入力に対して常に同一結果を返すため）
  - 注意: 半角カタカナの変換（濁点・半濁点の合成）は Java 側 `TextNormalizer` と統一した変換ロジックにすること

---

### Phase 2: Model 変更

- [ ] **2-1. `ProductSearchCondition` に `tasteNames` フィールドを追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`
  - 変更内容: `record` に `List<String> tasteNames` コンポーネントを末尾に追加
  - 影響確認: `new ProductSearchCondition(...)` 呼び出し箇所（`ProductListSearchService.buildCondition()` 内）をすべて更新すること
  - 設計根拠: テイスト絞り込みはカテゴリ横断であるため `ProductCategoryFilter` ではなく `ProductSearchCondition` 直下に配置（クラス設計書 §1 参照）

---

### Phase 3: ユーティリティクラスの新規作成

- [ ] **3-1. `TextNormalizer` クラスを新規作成する**
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

- [ ] **4-1. `ProductMapper` に `selectUnifiedSearchTasteOptions()` を追加する**
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

- [ ] **5-1. `BaseProductWhere` フラグメントのキーワード条件を変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更内容（SQL設計書 §2 参照）: 既存のキーワード条件ブロックを以下に置き換え
    - `p.product_name` に `normalize_fullwidth()` を適用して `#{keywordLike}` で部分一致
    - `p.variation_name` を新規追加（`IS NOT NULL` チェック付き）、`normalize_fullwidth()` を適用して部分一致
    - `p.description` を新規追加、`normalize_fullwidth()` を適用して部分一致
    - 商品コードの一致条件を `ILIKE #{keywordLike}`（部分一致）から `= #{keywordExact} OR ILIKE #{keywordPrefix}`（完全一致・前方一致）に変更し、`normalize_fullwidth()` を適用
  - 注意: SQL 設計書 §2.2 の変更後コードを参照すること

- [ ] **5-2. `SearchTasteFilter` フラグメントを新規追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 内容（SQL設計書 §4 参照）: `<sql id="SearchTasteFilter">` を追加
    - `hasSearchTasteFilter` が `true` のとき AND 条件を追加
    - デスク・チェア・収納の各属性テーブルに対して `EXISTS` + `display_name IN (...)` でフィルタリング
    - 3つの `EXISTS` を `OR` で結合（カテゴリ横断でいずれかにマッチすれば表示対象）

- [ ] **5-3. `BaseProductWhere` に `SearchTasteFilter` を include する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更内容: `BaseProductWhere` フラグメントの末尾（カテゴリ固有フィルタの後）に `<include refid="SearchTasteFilter"/>` を追加

- [ ] **5-4. `selectUnifiedSearchTasteOptions` クエリを新規追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 内容（SQL設計書 §5 参照）: 3カテゴリの `*_tastes` テーブルを `UNION ALL` し、`GROUP BY display_name` で重複排除、`MIN(sort_order)` 昇順で並び替え
  - `resultMap`: 既存の `ProductFilterOptionRowMap` を流用

---

### Phase 6: Repository 層の変更

- [ ] **6-1. `ProductFilterOptionRepository` に `findUnifiedSearchTasteOptions()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`
  - 追加内容（クラス設計書 §5 参照）:
    ```java
    public List<String> findUnifiedSearchTasteOptions() {
        return productMapper.selectUnifiedSearchTasteOptions()
            .stream()
            .map(ProductFilterOptionMapperRow::displayName)
            .toList();
    }
    ```
  - 注意: `optionId` は不使用のため `displayName` のみ抽出する

- [ ] **6-2. `ProductRepository.buildSearchParams()` を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
  - 変更内容（クラス設計書 §6 参照）:
    - キーワード関連: 既存の `params.put("keywordLike", toKeywordLike(condition.keyword()))` を以下に置き換え
      - キーワードが非 `null` の場合: `keywordLike`（`%keyword%`）、`keywordExact`（`keyword`）、`keywordPrefix`（`keyword%`）の3つをセット
      - キーワードが `null` の場合: 3つすべてに `null` をセット
    - テイスト関連（新規追加）: `condition.tasteNames()` から `searchTasteNames` と `hasSearchTasteFilter` をセット
  - 注意: `toKeywordLike()` ヘルパーメソッドはキーワードの正規化を行わなくなる（正規化済みキーワードを受け取る前提）。メソッドは削除または用途を明確にする

---

### Phase 7: Service 層の変更

- [ ] **7-1. `ProductListSearchService` に `normalizeSearchKeyword()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
  - 追加内容（クラス設計書 §3.1 参照）:
    - `private String normalizeSearchKeyword(String rawKeyword)` を追加
    - `rawKeyword` が `null` または空白のみの場合は `null` を返す
    - それ以外は `rawKeyword.strip()` した後 `TextNormalizer.toFullWidth()` を適用して返す

- [ ] **7-2. `ProductListSearchService.buildCondition()` を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
  - 変更内容（クラス設計書 §3.2 参照）:
    - 引数に `List<String> tasteNames` を追加
    - キーワードを `normalizeSearchKeyword()` で正規化した値を `ProductSearchCondition.keyword` に設定
    - `tasteNames` を `ProductSearchCondition.tasteNames` に設定
  - 注意: `buildCondition()` は複数のオーバーロードがあるため、検索結果画面が呼ぶシグネチャを特定して変更すること
  - `CatalogController.searchResults()` からの呼び出し箇所も合わせて更新すること

- [ ] **7-3. `ProductFilterOptionService` に `loadUnifiedSearchTasteOptions()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 追加内容（クラス設計書 §4.1 参照）:
    ```java
    public List<String> loadUnifiedSearchTasteOptions() {
        return productFilterOptionRepository.findUnifiedSearchTasteOptions();
    }
    ```

- [ ] **7-4. `ProductFilterOptionService` に `normalizeSearchTasteNames()` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 追加内容（クラス設計書 §4.2 参照）:
    - `public List<String> normalizeSearchTasteNames(List<String> rawNames, List<String> validNames)`
    - `rawNames` が `null` または空の場合は `List.of()` を返す
    - それ以外は `validNames` との集合（`HashSet`）を使ったホワイトリスト照合で有効な名称のみを残し、重複排除して返す

---

### Phase 8: Controller の変更

- [ ] **8-1. `CatalogController.searchResults()` にテイストパラメータを追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更内容（クラス設計書 §2 参照）:
    - 引数に `@RequestParam(value = "taste", required = false) List<String> rawTasteNames` を追加
    - メソッド内で `productFilterOptionService.loadUnifiedSearchTasteOptions()` を呼び出し、選択肢を取得
    - `productFilterOptionService.normalizeSearchTasteNames(rawTasteNames, tasteOptions)` でホワイトリスト照合
    - 正規化済み `tasteNames` を `productListSearchService.buildCondition()` に渡す
    - `model.addAttribute("searchTasteOptions", tasteOptions)` を追加
    - `model.addAttribute("selectedTasteNames", tasteNames)` を追加

---

### Phase 9: テンプレートの変更

- [ ] **9-1. 検索結果画面にテイスト絞り込み UI を追加する**
  - 対象ファイル: `src/main/resources/templates/pages/product-list-search-results.html`
  - 変更内容（UI設計書 §4 参照）:
    - サイドバーの絞り込みフォームの「カラー」セクションの後にテイストセクションを追加
    - Thymeleaf: `th:each="tasteName : ${searchTasteOptions}"` でチェックボックス一覧を生成
    - チェック状態: `th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}"`
    - フォーム値: `th:value="${tasteName}"`、`name="taste"`
  - ページネーションリンクに `taste=${selectedTasteNames}` を追加（UI設計書 §5.2 参照）
  - 並び替えリンクに `taste=${selectedTasteNames}` を追加（UI設計書 §5.3 参照）
  - モバイルモーダル（`modal-filters-search`）にも同じ構造を追加（UI設計書 §7 参照）

---

### Phase 10: 単体テスト

> テストディレクトリ: `src/test/java/jp/co/skig/officeorder/`（新規作成）
> テストフレームワーク: JUnit 5 + Mockito（`spring-boot-starter-test` 経由）

- [ ] **10-1. `TextNormalizerTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/util/TextNormalizerTest.java`
  - テストケース（境界値・正常系・異常系を網羅）:
    | テストメソッド名 | 入力 | 期待値 |
    |---|---|---|
    | `toFullWidth_halfWidthDigits_convertsToFullWidth` | `"123"` | `"１２３"` |
    | `toFullWidth_halfWidthUpperCase_convertsToFullWidth` | `"ABC"` | `"ＡＢＣ"` |
    | `toFullWidth_halfWidthLowerCase_convertsToFullWidth` | `"abc"` | `"ａｂｃ"` |
    | `toFullWidth_halfWidthKatakana_convertsToFullWidth` | `"ﾃﾞｽｸ"` | `"デスク"` |
    | `toFullWidth_halfWidthKatakanaWithDakuten_convertsToFullWidth` | `"ﾊﾞ"` | `"バ"` |
    | `toFullWidth_alreadyFullWidth_unchanged` | `"テスト"` | `"テスト"` |
    | `toFullWidth_hiragana_unchanged` | `"てすと"` | `"てすと"` |
    | `toFullWidth_kanji_unchanged` | `"机"` | `"机"` |
    | `toFullWidth_mixedInput_convertsOnlyHalfWidth` | `"abc123ﾃｽﾄ"` | `"ａｂｃ１２３テスト"` |
    | `toFullWidth_emptyString_returnsEmpty` | `""` | `""` |
    | `toFullWidth_null_returnsNull` | `null` | `null` |

- [ ] **10-2. `ProductFilterOptionServiceTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`
  - モック対象: `ProductFilterOptionRepository`
  - テストケース:

    **`normalizeSearchTasteNames()` のテスト:**
    | テストメソッド名 | 入力 | 期待値 |
    |---|---|---|
    | `normalizeSearchTasteNames_nullInput_returnsEmptyList` | `rawNames=null` | `[]` |
    | `normalizeSearchTasteNames_emptyInput_returnsEmptyList` | `rawNames=[]` | `[]` |
    | `normalizeSearchTasteNames_allValidNames_returnsAll` | `rawNames=["ナチュラル","ベーシック"]`, `validNames=["ナチュラル","ベーシック","モダン"]` | `["ナチュラル","ベーシック"]` |
    | `normalizeSearchTasteNames_invalidNamesFiltered` | `rawNames=["ナチュラル","無効値"]`, `validNames=["ナチュラル"]` | `["ナチュラル"]` |
    | `normalizeSearchTasteNames_duplicatesDeduped` | `rawNames=["ナチュラル","ナチュラル"]`, `validNames=["ナチュラル"]` | `["ナチュラル"]`（1件） |
    | `normalizeSearchTasteNames_allInvalid_returnsEmpty` | `rawNames=["無効1","無効2"]`, `validNames=["ナチュラル"]` | `[]` |

    **`loadUnifiedSearchTasteOptions()` のテスト:**
    | テストメソッド名 | 検証内容 |
    |---|---|
    | `loadUnifiedSearchTasteOptions_delegatesToRepository` | `productFilterOptionRepository.findUnifiedSearchTasteOptions()` が呼ばれ、その戻り値がそのまま返されること |

- [ ] **10-3. `ProductListSearchServiceTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceTest.java`
  - モック対象: `ProductService`、`ProductFilterOptionService`
  - テストケース（`buildCondition()` 経由でキーワード正規化を検証）:
    | テストメソッド名 | 入力 `rawKeyword` | `condition.keyword()` の期待値 |
    |---|---|---|
    | `buildCondition_halfWidthDigitsNormalized` | `"123"` | `"１２３"` |
    | `buildCondition_halfWidthAlphaNormalized` | `"Desk"` | `"Ｄｅｓｋ"` |
    | `buildCondition_halfWidthKatakanaNormalized` | `"ﾃﾞｽｸ"` | `"デスク"` |
    | `buildCondition_nullKeyword_conditionKeywordIsNull` | `null` | `null` |
    | `buildCondition_blankKeyword_conditionKeywordIsNull` | `"  "` | `null` |
    | `buildCondition_tasteNamesPassedThrough` | `keyword="机"`, `tasteNames=["ナチュラル"]` | `condition.tasteNames()` が `["ナチュラル"]` |
    | `buildCondition_nullTasteNames_conditionTasteNamesIsNullOrEmpty` | `tasteNames=null` | `condition.tasteNames()` が `null` または `[]` |

- [ ] **10-4. `ProductRepositoryBuildSearchParamsTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTest.java`
  - モック対象: `ProductMapper`、`AppTimeProvider`
  - 検証対象: `buildSearchParams()` が `search()` に渡す `Map<String, Object> params` の内容
    （`productMapper.countProducts(params)` の引数をキャプチャして検証する）
  - テストケース:
    | テストメソッド名 | 入力 | パラメータの期待値 |
    |---|---|---|
    | `search_withKeyword_setsThreeKeywordParams` | `keyword="デスク"` | `keywordLike="%デスク%"`, `keywordExact="デスク"`, `keywordPrefix="デスク%"` |
    | `search_withNullKeyword_setsNullKeywordParams` | `keyword=null` | `keywordLike=null`, `keywordExact=null`, `keywordPrefix=null` |
    | `search_withNonEmptyTasteNames_setsHasSearchTasteFilterTrue` | `tasteNames=["ナチュラル"]` | `hasSearchTasteFilter=true`, `searchTasteNames=["ナチュラル"]` |
    | `search_withEmptyTasteNames_setsHasSearchTasteFilterFalse` | `tasteNames=[]` | `hasSearchTasteFilter=false` |

- [ ] **10-5. `CatalogControllerSearchResultsTest` を作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/web/CatalogControllerTest.java`
  - テスト方式: `@WebMvcTest(CatalogController.class)` + `MockMvc` + Mockito による Service モック
  - テストケース:
    | テストメソッド名 | リクエスト | 検証内容 |
    |---|---|---|
    | `searchResults_withTaste_tasteOptionsAddedToModel` | `GET /products/search?q=机&taste=ナチュラル` | `model["searchTasteOptions"]` に選択肢リストが含まれること |
    | `searchResults_withTaste_selectedTasteNamesAddedToModel` | `GET /products/search?q=机&taste=ナチュラル` | `model["selectedTasteNames"]` に `"ナチュラル"` が含まれること |
    | `searchResults_withoutTaste_selectedTasteNamesIsEmpty` | `GET /products/search?q=机` | `model["selectedTasteNames"]` が空リストであること |
    | `searchResults_invalidTaste_filteredByWhitelist` | `GET /products/search?q=机&taste=無効値` | `normalizeSearchTasteNames()` が呼ばれ、`selectedTasteNames` が空になること |
    | `searchResults_withTaste_normalizeSearchTasteNamesCalledWithOptions` | `GET /products/search?q=机&taste=ナチュラル` | `productFilterOptionService.normalizeSearchTasteNames()` が `loadUnifiedSearchTasteOptions()` の戻り値をホワイトリストとして呼ばれること |

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

| Phase | ファイルパス | 種別 |
|-------|------------|------|
| 1 | `sql/schema/products.sql` | 変更 |
| 2 | `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java` | 変更 |
| 3 | `src/main/java/jp/co/skig/officeorder/util/TextNormalizer.java` | **新規** |
| 4 | `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java` | 変更 |
| 5 | `src/main/resources/mappers/ProductMapper.xml` | 変更 |
| 6 | `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java` | 変更 |
| 6 | `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java` | 変更 |
| 7 | `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java` | 変更 |
| 7 | `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java` | 変更 |
| 8 | `src/main/java/jp/co/skig/officeorder/web/CatalogController.java` | 変更 |
| 9 | `src/main/resources/templates/pages/product-list-search-results.html` | 変更 |
| 10 | `src/test/java/jp/co/skig/officeorder/util/TextNormalizerTest.java` | **新規** |
| 10 | `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java` | **新規** |
| 10 | `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceTest.java` | **新規** |
| 10 | `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTest.java` | **新規** |
| 10 | `src/test/java/jp/co/skig/officeorder/web/CatalogControllerTest.java` | **新規** |
