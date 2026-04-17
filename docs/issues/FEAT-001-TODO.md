# FEAT-001 商品検索機能強化 実装計画 TODO リスト

作成日: 2026-04-18  
最終更新: 2026-04-18（実装完了）  
対象要件: `docs/issues/FEAT-001-requirements.md`  
設計書: `docs/design/FEAT-001-basic-design.md` / `docs/design/FEAT-001-detail-design.md` / `docs/design/FEAT-001-test-design.md`

---

## 凡例

- `[ ]` 未着手
- `[x]` 完了
- **太字**: 新規作成ファイル
- 斜体: 既存ファイルへの変更

---

## Phase 1: 共通ユーティリティ・モデル（影響範囲最小）

### 1-1. `NormalizationUtils` の新規作成

- [x] **`src/main/java/jp/co/skig/officeorder/common/NormalizationUtils.java`** を新規作成
  - `final class`、コンストラクタ private（インスタンス化不可）
  - `public static String normalizeForSearch(String text)` を実装
    - `null` / 空文字はそのまま返す
    - 変換順序（詳細設計書 §3-1 参照）:
      1. 半角カタカナ（濁点・半濁点の 2 文字合成含む）→ 全角カタカナ
         - インデックスループで次文字を先読みし、`ﾞ`（U+FF9E）/ `ﾟ`（U+FF9F）の合成を処理
         - 単独半角カタカナ（ｦ〜ﾝ）は JIS 対応表で 1:1 変換
      2. 全角数字（`１`〜`９`、`０`）→ 半角（`U+FF10`〜`U+FF19` のオフセット変換）
      3. 全角英大文字（`Ａ`〜`Ｚ`）→ 半角（`U+FF21`〜`U+FF3A` のオフセット変換）
      4. 全角英小文字（`ａ`〜`ｚ`）→ 半角（`U+FF41`〜`U+FF5A` のオフセット変換）
      5. 全角ハイフン `－`（U+FF0D）→ 半角 `-`
      6. 全角括弧 `（`（U+FF08）→ `(`、`）`（U+FF09）→ `)`

### 1-2. `KeywordSearchParam` record の新規作成

- [x] **`src/main/java/jp/co/skig/officeorder/mapper/row/KeywordSearchParam.java`** を新規作成
  - `public record KeywordSearchParam(String keywordLike, String codeLike) {}`
  - `keywordLike`: `"%{token}%"` 形式（部分一致）
  - `codeLike`: `"{token}%"` 形式（前方一致）

### 1-3. `ProductSearchCondition` record へのフィールド追加

- [x] *`src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`* を変更
  - `saleStartFrom` の後に `List<String> tasteDisplayNames` フィールドを追加
  - `import java.util.List;` は既存のため追加不要
  - 変更後の record コンポーネント順序（詳細設計書 §4-1 参照）:
    ```
    String categoryId, String keyword, boolean inStockOnly,
    List<PriceBand> priceBands, List<Long> colorIds,
    ProductCategoryFilter categoryFilter, ProductSort sort,
    int page, int size, OffsetDateTime saleStartFrom,
    List<String> tasteDisplayNames   // NEW
    ```
  - **影響箇所の確認・修正が必要** → Phase 2〜4 で対応

---

## Phase 2: Mapper 層（SQL・インターフェース）

### 2-1. `ProductMapper` インターフェースへのメソッド追加

- [x] *`src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`* を変更
  - 以下のメソッドを追加:
    ```java
    List<String> selectUnifiedTasteDisplayNames();
    ```
  - Javadoc: 「カテゴリ横断で統合されたテイスト表示名一覧を取得する。desk_tastes / chair_tastes / storage_tastes を UNION して重複排除・ソート済みで返す。」

### 2-2. `ProductMapper.xml` の変更

- [x] *`src/main/resources/mappers/ProductMapper.xml`* を変更

  **変更①: `BaseProductWhere` のキーワード条件を修正**
  - 変更前: `keywordLike` パラメータによる単一の ILIKE 条件（`product_name` + `product_code` の部分一致 OR）
  - 変更後: `keywords`（`List<KeywordSearchParam>`）の `<foreach>` による AND 結合
  - 新しいキーワード条件のロジック（各キーワードを AND で結合、各キーワード内は OR）:
    ```xml
    <if test="keywords != null and keywords.size() > 0">
        AND
        <foreach collection="keywords" item="kw" separator=" AND ">
            (
                p.product_name ILIKE #{kw.keywordLike}
                OR (p.variation_name IS NOT NULL AND p.variation_name ILIKE #{kw.keywordLike})
                OR p.description ILIKE #{kw.keywordLike}
                OR EXISTS (
                    SELECT 1 FROM product_variants pvk
                    WHERE pvk.product_id = p.product_id
                      AND pvk.product_code ILIKE #{kw.codeLike}
                )
            )
        </foreach>
    </if>
    ```

  **変更②: テイストフィルタ条件の追加**
  - `BaseProductWhere` 内（キーワード条件の後）に追加:
    ```xml
    <if test="hasTasteFilter == true">
        AND (
            EXISTS (
                SELECT 1
                FROM product_desk_attributes da
                JOIN desk_tastes dt ON da.taste_id = dt.taste_id
                WHERE da.product_id = p.product_id
                  AND dt.display_name IN
                <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                    #{name}
                </foreach>
            )
            OR EXISTS (
                SELECT 1
                FROM product_chair_attributes ca
                JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
                WHERE ca.product_id = p.product_id
                  AND ct.display_name IN
                <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                    #{name}
                </foreach>
            )
            OR EXISTS (
                SELECT 1
                FROM product_storage_attributes sa
                JOIN storage_tastes st ON sa.taste_id = st.taste_id
                WHERE sa.product_id = p.product_id
                  AND st.display_name IN
                <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                    #{name}
                </foreach>
            )
        )
    </if>
    ```

  **変更③: `selectUnifiedTasteDisplayNames` クエリの追加**
  - ファイル末尾（`</mapper>` の直前）に追加:
    ```xml
    <select id="selectUnifiedTasteDisplayNames" resultType="string">
        SELECT display_name
        FROM (
            SELECT display_name, sort_order FROM desk_tastes    WHERE is_active = TRUE
            UNION ALL
            SELECT display_name, sort_order FROM chair_tastes   WHERE is_active = TRUE
            UNION ALL
            SELECT display_name, sort_order FROM storage_tastes WHERE is_active = TRUE
        ) combined
        GROUP BY display_name
        ORDER BY MIN(sort_order) ASC, display_name ASC
    </select>
    ```

---

## Phase 3: Repository 層

### 3-1. `ProductRepository` の変更

- [x] *`src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`* を変更

  **変更①: `import` の追加**
  - `import jp.co.skig.officeorder.mapper.row.KeywordSearchParam;` を追加
  - `import java.util.Set;` を追加（不要なら省略可）

  **変更②: `buildSearchParams()` の修正**
  - `params.put("keywordLike", toKeywordLike(condition.keyword()))` を削除
  - 以下に置き換え:
    ```java
    params.put("keywords", buildKeywords(condition.keyword()));
    List<String> tasteDisplayNames = condition.tasteDisplayNames() == null
        ? List.of() : condition.tasteDisplayNames();
    params.put("tasteDisplayNames", tasteDisplayNames);
    params.put("hasTasteFilter", !tasteDisplayNames.isEmpty());
    ```

  **変更③: `buildKeywords()` private メソッドの新規追加**
  - シグネチャ: `private List<KeywordSearchParam> buildKeywords(String normalizedKeyword)`
  - ロジック:
    1. `normalizedKeyword` が `null` または `isBlank()` → `List.of()` を返す
    2. 正規表現 `[ \u3000]+`（半角スペース・全角スペース 1 文字以上）で `split`
    3. 空要素を除去（`filter(s -> !s.isBlank())`）
    4. 各トークンに対して `new KeywordSearchParam("%" + token + "%", token + "%")` を生成
    5. `List<KeywordSearchParam>` を返す

  **変更④: `toKeywordLike()` の廃止**
  - `toKeywordLike()` メソッドを削除する（`buildKeywords()` に機能が統合されるため）
  - 他の呼び出し箇所がないことを事前確認する（`ProductRepository` 内のみのため問題なし）

  **変更⑤: `findNewestProducts()` 等の `ProductSearchCondition` 生成箇所の修正**
  - `saleStartFrom` の次に `List.of()` を追加（`tasteDisplayNames` = 空リスト）
  - 対象メソッド:
    - `findNewestProducts()` 内の `new ProductSearchCondition(...)` 呼び出し

### 3-2. `ProductFilterOptionRepository` へのメソッド追加

- [x] *`src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`* を変更
  - 以下のメソッドを追加:
    ```java
    /**
     * カテゴリ横断で統合されたテイスト表示名一覧を取得する。
     *
     * @return 重複排除・ソート済みのテイスト表示名一覧
     */
    public List<String> findUnifiedTasteDisplayNames() {
        return productMapper.selectUnifiedTasteDisplayNames();
    }
    ```

---

## Phase 4: Service 層

### 4-1. `ProductService` の変更

- [x] *`src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`* を変更

  **変更①: `NormalizationUtils` の import 追加**
  - `import jp.co.skig.officeorder.common.NormalizationUtils;` を追加
  - `import java.util.List;` は既存のため確認のみ

  **変更②: `buildCondition()` のキーワード正規化**
  - キーワードの trim 処理を正規化呼び出しに変更:
    ```java
    // 変更前
    String trimmedKeyword = keyword == null ? null : keyword.trim();

    // 変更後
    String trimmedKeyword = keyword == null ? null
        : NormalizationUtils.normalizeForSearch(keyword.trim());
    ```

  **変更③: `buildCondition()` に `tasteDisplayNames` 引数を追加**
  - `saleStartFrom` を受け取る最終オーバーロードに `List<String> tasteDisplayNames` を追加
  - `ProductSearchCondition` 生成時の末尾引数として `tasteDisplayNames` を渡す
  - 既存の短いオーバーロード群からは `null` をチェーン渡し（または `List.of()`）
    - 例: `buildCondition(category, keyword, inStockOnly, priceBandIds, colorKeys, categoryFilter, sort, page, size, defaultSort)` → `tasteDisplayNames = null` を末尾に追加してチェーン

  **変更④: `normalize()` に `tasteDisplayNames` を引き継ぐ**
  - `normalize(ProductSearchCondition condition)` 内の `return new ProductSearchCondition(...)` の最後に:
    ```java
    condition.tasteDisplayNames() == null ? List.of() : condition.tasteDisplayNames()
    ```

### 4-2. `ProductListSearchService` の変更

- [x] *`src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`* を変更

  **変更①: `buildCondition()` への引数追加**
  - カテゴリ固有条件あり版の `buildCondition()` に以下を追加:
    - `List<String> rawTasteDisplayNames`
    - `List<String> allowedTasteDisplayNames`
  - 追加処理:
    ```java
    List<String> selectedTasteDisplayNames =
        productFilterOptionService.normalizeTasteDisplayNames(
            rawTasteDisplayNames, allowedTasteDisplayNames);
    ```
  - `productService.buildCondition()` 呼び出し末尾に `selectedTasteDisplayNames` を追加

  **変更②: `searchWithPageCorrection()` での `tasteDisplayNames` 引き継ぎ**
  - ページ補正時に生成する `corrected` condition で `condition.tasteDisplayNames()` を引き継ぐ:
    ```java
    new ProductSearchCondition(
        condition.categoryId(),
        condition.keyword(),
        ...
        condition.saleStartFrom(),
        condition.tasteDisplayNames()   // NEW
    )
    ```

### 4-3. `ProductFilterOptionService` へのメソッド追加

- [x] *`src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`* を変更

  **変更①: `import` の追加**
  - `import java.util.HashSet;` を追加

  **変更②: `loadUnifiedTasteDisplayNames()` の追加**
  ```java
  /**
   * カテゴリ横断で統合されたテイスト表示名一覧を取得する。
   *
   * @return 重複排除・ソート済みのテイスト表示名一覧
   */
  public List<String> loadUnifiedTasteDisplayNames() {
      return productFilterOptionRepository.findUnifiedTasteDisplayNames();
  }
  ```

  **変更③: `normalizeTasteDisplayNames()` の追加**
  - ホワイトリストフィルタリングによる入力値正規化（セキュリティ上必須）
  ```java
  /**
   * テイスト表示名リストをホワイトリストで正規化する。
   *
   * <p>URL パラメータ経由の生値から、許可リストに存在するものだけを抽出する。
   * 不正値の混入・重複を除去する。
   *
   * @param rawTasteDisplayNames コントローラが受け取った生のテイスト名リスト
   * @param allowedDisplayNames  DB から取得した有効なテイスト名一覧
   * @return 正規化済みテイスト名リスト（重複なし）
   */
  public List<String> normalizeTasteDisplayNames(
          List<String> rawTasteDisplayNames,
          List<String> allowedDisplayNames) {
      if (rawTasteDisplayNames == null || rawTasteDisplayNames.isEmpty()) {
          return List.of();
      }
      Set<String> allowedSet = new HashSet<>(allowedDisplayNames);
      return rawTasteDisplayNames.stream()
          .filter(name -> name != null && allowedSet.contains(name))
          .distinct()
          .toList();
  }
  ```

---

## Phase 5: Controller 層

### 5-1. `CatalogController` の変更

- [x] *`src/main/java/jp/co/skig/officeorder/web/CatalogController.java`* を変更

  **変更①: `searchResults()` への `taste` パラメータ追加**
  - メソッドシグネチャに以下を追加:
    ```java
    @RequestParam(name = "taste", required = false) List<String> rawTasteDisplayNames
    ```

  **変更②: テイスト選択肢のロードと model への追加**
  - メソッド内の冒頭付近（既存フィルタオプション取得に続けて）に追加:
    ```java
    List<String> tasteFilterOptions = productFilterOptionService.loadUnifiedTasteDisplayNames();
    ```
  - model 属性として追加:
    ```java
    model.addAttribute("tasteFilterOptions", tasteFilterOptions);
    ```

  **変更③: 選択済みテイストの model への追加**
  - `buildCondition()` 呼び出し後に正規化済みリストを取得し model に追加:
    - `buildCondition()` の戻り値から `condition.tasteDisplayNames()` を取得するか、
      `ProductListSearchService.buildCondition()` の引数に `rawTasteDisplayNames` と `tasteFilterOptions` を渡して正規化済みリストを `searchResult` 経由で取得する
    - `model.addAttribute("selectedTasteDisplayNames", condition.tasteDisplayNames())` を追加

  **変更④: `buildCondition()` 呼び出しの変更**
  - `productListSearchService.buildCondition(...)` の引数末尾に `rawTasteDisplayNames` と `tasteFilterOptions` を追加

---

## Phase 6: テンプレート層

### 6-1. `product-list-search-results.html` の変更

- [x] *`src/main/resources/templates/pages/product-list-search-results.html`* を変更

  **変更①: サイドバーフォームへのテイストフィルタ追加**
  - カラーフィルタブロック（`<div class="field">` カラー）の後に追加:
    ```html
    <!-- テイストフィルタ -->
    <div class="field" th:if="${tasteFilterOptions != null and !#lists.isEmpty(tasteFilterOptions)}">
        <label>テイスト</label>
        <div>
            <label class="check-item"
                   th:each="tasteName : ${tasteFilterOptions}">
                <input type="checkbox"
                       name="taste"
                       th:value="${tasteName}"
                       th:checked="${selectedTasteDisplayNames != null
                                     and selectedTasteDisplayNames.contains(tasteName)}">
                <span th:text="${tasteName}">ナチュラル</span>
            </label>
        </div>
    </div>
    ```

  **変更②: ソート選択フォーム（desktop ツールバー内）への `taste` 引き継ぎ**
  - 既存の `<input type="hidden" name="color" ...>` の後に追加:
    ```html
    <input type="hidden" name="taste"
           th:each="t : ${selectedTasteDisplayNames}" th:value="${t}">
    ```

  **変更③: ページネーションリンクへの `taste` パラメータ追加**
  - 前へ / ページ番号 / 次へ の各 `th:href` に `,taste=${selectedTasteDisplayNames}` を追加
  - 例（前へリンク）:
    ```html
    th:href="@{/products/search(q=${keyword},page=${currentPage - 1},size=${pageSize},
        sort=${sortValue},inStockOnly=${inStockOnly ? 'true' : null},
        priceBand=${selectedPriceBandIds},color=${selectedColorKeys},
        taste=${selectedTasteDisplayNames})}"
    ```

  **変更④: モバイルモーダルへの TODO コメント挿入**
  - モバイル向けモーダルのフィルタ部分に以下のコメントを挿入（実装は後続対応）:
    ```html
    <!-- TODO: テイストフィルタ（PC版と同様）FEAT-001 後続対応 -->
    ```

---

## Phase 7: 単体テスト（UT）

> テストクラスは `src/test/java/jp/co/skig/officeorder/` 配下に配置する。  
> 既存の `src/test/` ディレクトリがない場合は新規作成する。  
> テストフレームワーク: JUnit 5 + AssertJ（`spring-boot-starter-test` に含まれる）。

### 7-1. `NormalizationUtilsTest` の新規作成

- [x] **`src/test/java/jp/co/skig/officeorder/common/NormalizationUtilsTest.java`** を新規作成
  - テスト設計書 §2-1 の全ケース（NU-01〜NU-25）を実装
  - 主要なテストケース:

    | テスト ID | 入力 | 期待値 | 観点 |
    |:---:|------|--------|------|
    | NU-01 | `"１２３"` | `"123"` | 全角数字 → 半角 |
    | NU-08 | `"ﾃﾞｽｸ"` | `"デスク"` | 半角カタカナ（濁点合成）→ 全角 |
    | NU-09 | `"ﾁｪｱ"` | `"チェア"` | 小書き文字を含む変換 |
    | NU-11 | `"ﾊﾟｲﾌﾟ"` | `"パイプ"` | 半濁点合成のケース |
    | NU-14 | `"DSK－001"` | `"DSK-001"` | 全角ハイフン → 半角 |
    | NU-15 | `"（3段）"` | `"(3段)"` | 全角括弧 → 半角 |
    | NU-18 | `"ＤＳＫ－１００１"` | `"DSK-1001"` | 複合変換（商品コード形式） |
    | NU-20 | `null` | `null` | null 安全処理 |
    | NU-21 | `""` | `""` | 空文字 安全処理 |
    | NU-23 | `"あいう"` | `"あいう"` | ひらがなは変換しない |

### 7-2. `ProductRepositoryKeywordBuildTest` の新規作成

- [x] **`src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryKeywordBuildTest.java`** を新規作成
  - テスト設計書 §2-2 の全ケース（BK-01〜BK-08）を実装
  - `buildKeywords()` は private のため、`buildSearchParams()` の出力（`params.get("keywords")`）を検証するブラックボックステストとして実装
  - `@SpringBootTest` または `@ExtendWith(MockitoExtension.class)` でモックを使用して `ProductMapper` / `AppTimeProvider` を差し替え

    | テスト ID | 入力キーワード | 期待 keywords リスト | 観点 |
    |:---:|------------|-------------------|------|
    | BK-01 | `"ナチュラル"` | size=1, `[{kL:"%ナチュラル%", cL:"ナチュラル%"}]` | 1 キーワード |
    | BK-02 | `"ナチュラル デスク"` | size=2 | 半角スペース区切り |
    | BK-03 | `"ナチュラル　デスク"` | size=2 | 全角スペース区切り |
    | BK-05 | `null` | `[]` | null → 空リスト |
    | BK-06 | `""` | `[]` | 空文字 → 空リスト |
    | BK-08 | `"デスク  チェア"` | size=2 | 連続スペース正規化 |

### 7-3. `ProductFilterOptionServiceTest` の新規作成

- [x] **`src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`** を新規作成
  - テスト設計書 §2-3 の全ケース（NT-01〜NT-07）を実装
  - `@ExtendWith(MockitoExtension.class)` でモックを使用

    | テスト ID | rawList | allowedList | 期待値 | 観点 |
    |:---:|---------|-------------|--------|------|
    | NT-01 | `["ナチュラル","モダン"]` | `["ナチュラル","モダン","北欧"]` | `["ナチュラル","モダン"]` | 有効値はすべて通過 |
    | NT-02 | `["ナチュラル","存在しない"]` | `["ナチュラル","モダン"]` | `["ナチュラル"]` | 不正値を除去 |
    | NT-03 | `["ナチュラル","ナチュラル"]` | `["ナチュラル"]` | `["ナチュラル"]` | 重複を除去 |
    | NT-04 | `null` | `["ナチュラル"]` | `[]` | null 入力 → 空リスト |
    | NT-07 | `[null,"ナチュラル"]` | `["ナチュラル"]` | `["ナチュラル"]` | null 要素を除去 |

---

## Phase 8: 統合テスト（IT）— オプション

> 統合テストは実 PostgreSQL または TestContainers が必要。  
> 現状の `pom.xml` には TestContainers が未追加のため、事前に依存関係の追加が必要。

### 8-1. pom.xml への TestContainers 依存追加（事前作業）

- [ ] *`pom.xml`* に TestContainers 依存を追加（必要な場合）
  ```xml
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter-test</artifactId>
      <version>3.0.4</version>
      <scope>test</scope>
  </dependency>
  ```

### 8-2. `ProductMapperSearchTest` の新規作成

- [ ] **`src/test/java/jp/co/skig/officeorder/mapper/ProductMapperSearchTest.java`** を新規作成
  - テスト設計書 §3-1 のケース（MS-01〜MS-19）を実装
  - テスト用商品データ（5 件程度）・テイストデータを SQL ファイルまたはアノテーションで投入
  - `@MybatisTest` + TestContainers（PostgreSQL）を使用

    **優先度高（基本動作確認）:**
    | テスト ID | キーワード | tasteDisplayNames | 期待ヒット | 観点 |
    |:---:|----------|-----------------|-----------|------|
    | MS-01 | `"ナチュラル"` | `[]` | 商品 1,2,4 | `product_name` + `description` でヒット |
    | MS-02 | `"昇降式"` | `[]` | 商品 5 | `variation_name` でヒット（**追加フィールド**）|
    | MS-04 | `"NDK"` | `[]` | 商品 1,2 | `product_code` 前方一致 |
    | MS-06 | `"001"` | `[]` | 0 件 | **前方一致変更後**の動作確認（中間一致でヒットしない）|
    | MS-07 | `"ナチュラル デスク"` | `[]` | 商品 1,2 | 複数キーワード AND 検索 |
    | MS-13 | `""` | `["ナチュラル"]` | 商品 1,2 | テイスト単独フィルタ |
    | MS-15 | `""` | `["ナチュラル","モダン"]` | 商品 1,2,3 | テイスト OR 複数選択 |
    | MS-19 | `"デスク"` | `["ナチュラル"]` | 商品 1,2 | キーワード × テイスト複合 |

### 8-3. `ProductSearchIntegrationTest` の新規作成

- [ ] **`src/test/java/jp/co/skig/officeorder/service/product/ProductSearchIntegrationTest.java`** を新規作成
  - テスト設計書 §3-2 のケース（IT-01〜IT-03）を実装
  - `@SpringBootTest` + TestContainers を使用

    | テスト ID | シナリオ | 確認ポイント |
    |:---:|---------|------------|
    | IT-01 | 全角入力 `"ﾅﾁｭﾗﾙ"` でキーワード検索 | 正規化 → 全角カタカナ → 商品 1,2,4 がヒット |
    | IT-02 | `"ＮＤＫ－００１"` でキーワード検索 | 正規化後 `"NDK-001"` → 前方一致で商品 1,2 ヒット |
    | IT-03 | キーワードなし + テイスト `"ナチュラル"` | テイスト単独検索で商品 1,2 がヒット |

---

## Phase 9: インデックス追加（任意・インフラ調整後）

- [ ] **`sql/schema/search-indexes.sql`** を新規作成
  - `pg_trgm` 拡張の有効化（インフラ担当者確認後に適用）
  - trigram GIN インデックスの追加:
    ```sql
    -- pg_trgm 拡張の有効化（初回のみ）
    CREATE EXTENSION IF NOT EXISTS pg_trgm;

    -- 商品名 trigram インデックス
    CREATE INDEX IF NOT EXISTS idx_products_product_name_trgm
        ON products USING GIN (product_name gin_trgm_ops);

    -- バリエーション名 trigram インデックス（新規検索対象）
    CREATE INDEX IF NOT EXISTS idx_products_variation_name_trgm
        ON products USING GIN (variation_name gin_trgm_ops);

    -- 説明文 trigram インデックス（TEXT 型のため特に重要）
    CREATE INDEX IF NOT EXISTS idx_products_description_trgm
        ON products USING GIN (description gin_trgm_ops);
    ```
  - ローカル Docker 環境で動作確認後、本番適用手順を別途検討

---

## Phase 10: 設計書整合確認

- [ ] `docs/design/crud-matrix.md` の機能 **05「商品一覧（キーワード検索）」** 行を更新
  - `desk_tastes` / `chair_tastes` / `storage_tastes` に **R** を追加（詳細設計書 §11-1 参照）

---

## 実装順序まとめ

```
Phase 1（モデル・共通）
  └─ 1-1. NormalizationUtils 新規作成
  └─ 1-2. KeywordSearchParam record 新規作成
  └─ 1-3. ProductSearchCondition にフィールド追加

Phase 2（Mapper）
  └─ 2-1. ProductMapper インターフェースにメソッド追加
  └─ 2-2. ProductMapper.xml のキーワード条件修正 + テイストフィルタ + 新クエリ追加

Phase 3（Repository）
  └─ 3-1. ProductRepository の buildKeywords() 追加 + buildSearchParams() 修正 + findNewestProducts() 修正
  └─ 3-2. ProductFilterOptionRepository にメソッド追加

Phase 4（Service）
  └─ 4-1. ProductService に NormalizationUtils 適用 + tasteDisplayNames 伝播
  └─ 4-2. ProductListSearchService に taste 引数追加 + searchWithPageCorrection 修正
  └─ 4-3. ProductFilterOptionService に loadUnifiedTasteDisplayNames / normalizeTasteDisplayNames 追加

Phase 5（Controller）
  └─ 5-1. CatalogController に taste パラメータ + モデル属性追加

Phase 6（テンプレート）
  └─ 6-1. product-list-search-results.html にテイストフィルタ UI 追加 + パラメータ伝播修正

Phase 7（単体テスト）
  └─ 7-1. NormalizationUtilsTest 作成
  └─ 7-2. ProductRepositoryKeywordBuildTest 作成
  └─ 7-3. ProductFilterOptionServiceTest 作成

Phase 8（統合テスト・オプション）
  └─ 8-1. pom.xml に TestContainers 追加
  └─ 8-2. ProductMapperSearchTest 作成
  └─ 8-3. ProductSearchIntegrationTest 作成

Phase 9（インデックス・任意）
  └─ 9-1. search-indexes.sql 作成

Phase 10（設計書整合確認）
  └─ 10-1. crud-matrix.md 更新
```

---

## 注意事項・実装上の確認ポイント

| # | 項目 | 内容 |
|:-:|------|------|
| 1 | `ProductSearchCondition` 変更影響 | record の末尾にフィールドを追加するため、`new ProductSearchCondition(...)` の呼び出し箇所をすべて修正する必要がある。`ProductRepository.findNewestProducts()` 内の呼び出しも対象。 |
| 2 | `buildCondition()` オーバーロード | `ProductService.buildCondition()` のオーバーロードチェーンで `tasteDisplayNames` を `null` または `List.of()` として伝播させること。 |
| 3 | `toKeywordLike()` 削除 | `ProductRepository` 内でのみ使用されていることを確認済み。削除前に grep で確認すること。 |
| 4 | セキュリティ（テイスト入力値） | `taste` パラメータは必ず `normalizeTasteDisplayNames()` によるホワイトリスト検証を経由してから SQL に渡すこと。直接 SQL パラメータに使用しないこと。 |
| 5 | MyBatis `<foreach>` の空リスト | `hasTasteFilter == true` の条件ガードにより、`tasteDisplayNames` が空のときは `<foreach>` が実行されないことを確認すること。 |
| 6 | `variation_name` の NULL | `variation_name IS NOT NULL` 条件を明示すること（PostgreSQL での NULL ILIKE の挙動確認済みだが意図を明示する）。 |
| 7 | 統合テストの前提 | `@MybatisTest` は PostgreSQL 固有の `ILIKE` を使用するため H2 では動作しない。TestContainers による PostgreSQL コンテナを使用すること。 |
