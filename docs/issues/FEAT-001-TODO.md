# FEAT-001 — 実装 TODO リスト

> 作成日: 2026-03-16  
> 関連要件: [FEAT-001-requirements.md](FEAT-001-requirements.md)  
> 関連設計: [docs/design/FEAT-001-design-overview.md](../design/FEAT-001-design-overview.md)

---

## 進め方

- 実装は **依存関係の順（下位レイヤーから）** に進める
- 各タスク完了後にコンパイルエラーがないことを確認する（`./mvnw compile`）
- 単体テストは実装タスクと対で進める（実装 → テスト の順）
- 全タスク完了後に `./mvnw test` でテスト全量を通す

---

## フェーズ 1: バックエンド実装

### T-01 `ProductRepository` — `normalizeKeyword()` の追加

**ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

- [ ] `import java.text.Normalizer;` を追加する
- [ ] `normalizeKeyword(String keyword)` プライベートメソッドを追加する
  - `null` / 空白のみの場合は `null` を返す
  - `keyword.trim()` したうえで `Normalizer.normalize(text, Normalizer.Form.NFKC)` を適用する
  - 結果が空文字列になった場合も `null` を返す

**参考実装:**
```java
private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return null;
    }
    String normalized = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKC);
    return normalized.isEmpty() ? null : normalized;
}
```

---

### T-02 `ProductRepository` — `toKeywordPrefix()` の追加

**ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

- [ ] `toKeywordPrefix(String normalizedKeyword)` プライベートメソッドを追加する
  - `normalizedKeyword` が `null` の場合は `null` を返す
  - `normalizedKeyword + "%"` を返す

**参考実装:**
```java
private String toKeywordPrefix(String normalizedKeyword) {
    if (normalizedKeyword == null) {
        return null;
    }
    return normalizedKeyword + "%";
}
```

---

### T-03 `ProductRepository` — `buildSearchParams()` の変更

**ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

- [ ] 既存の `params.put("keywordLike", toKeywordLike(condition.keyword()));` を以下に変更する:
  ```java
  String normalized = normalizeKeyword(condition.keyword());
  params.put("keywordLike",   toKeywordLike(normalized));
  params.put("keywordPrefix", toKeywordPrefix(normalized));
  ```
- [ ] `toKeywordLike()` への引数を `condition.keyword()` から `normalized` に変更したため、`toKeywordLike()` 内の `keyword.trim()` は不要になる。`trim()` を除去し `null` チェックのみに変更する:
  ```java
  private String toKeywordLike(String normalizedKeyword) {
      if (normalizedKeyword == null) {
          return null;
      }
      return "%" + normalizedKeyword + "%";
  }
  ```
- [ ] `hasSearchTasteFilter` フラグを計算し `params` に追加する:
  ```java
  boolean hasSearchTasteFilter = condition.categoryId() == null
          && (!deskTasteIds.isEmpty() || !chairTasteIds.isEmpty() || !storageTasteIds.isEmpty());
  params.put("hasSearchTasteFilter", hasSearchTasteFilter);
  ```
  > **配置:** `hasStorageFilter` の計算直後、`params.put(...)` の連続ブロック内に追加する

---

### T-04 `ProductMapper.xml` — `BaseProductWhere` のキーワード条件変更

**ファイル:** `src/main/resources/mappers/ProductMapper.xml`

- [ ] キーワード条件の `<if>` ブロックを以下に変更する:

  ```xml
  <if test="keywordLike != null">
    AND (
      p.product_name    ILIKE #{keywordLike}
      OR p.variation_name ILIKE #{keywordLike}
      OR p.description    ILIKE #{keywordLike}
      OR EXISTS (
        SELECT 1 FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND pvk.product_code ILIKE #{keywordPrefix}
      )
    )
  </if>
  ```

  変更ポイント:
  - `p.variation_name ILIKE #{keywordLike}` を追加（新規）
  - `p.description ILIKE #{keywordLike}` を追加（新規）
  - `pvk.product_code ILIKE #{keywordLike}` → `pvk.product_code ILIKE #{keywordPrefix}` に変更

---

### T-05 `ProductMapper.xml` — `SearchTasteFilter` フラグメントの追加

**ファイル:** `src/main/resources/mappers/ProductMapper.xml`

- [ ] 既存の `StorageAttributeFilter` SQL フラグメントの直後に `SearchTasteFilter` フラグメントを追加する:

  ```xml
  <sql id="SearchTasteFilter">
    <if test="hasSearchTasteFilter">
      AND (
        <trim prefixOverrides="OR">
          <if test="deskTasteIds != null and deskTasteIds.size() > 0">
            EXISTS (
              SELECT 1 FROM product_desk_attributes da
              WHERE da.product_id = p.product_id
                AND da.taste_id IN
                <foreach item="id" collection="deskTasteIds" open="(" separator="," close=")">
                  #{id}
                </foreach>
            )
          </if>
          <if test="chairTasteIds != null and chairTasteIds.size() > 0">
            OR EXISTS (
              SELECT 1 FROM product_chair_attributes ca
              WHERE ca.product_id = p.product_id
                AND ca.taste_id IN
                <foreach item="id" collection="chairTasteIds" open="(" separator="," close=")">
                  #{id}
                </foreach>
            )
          </if>
          <if test="storageTasteIds != null and storageTasteIds.size() > 0">
            OR EXISTS (
              SELECT 1 FROM product_storage_attributes sa
              WHERE sa.product_id = p.product_id
                AND sa.taste_id IN
                <foreach item="id" collection="storageTasteIds" open="(" separator="," close=")">
                  #{id}
                </foreach>
            )
          </if>
        </trim>
      )
    </if>
  </sql>
  ```

- [ ] `BaseProductWhere` フラグメントの末尾（`<include refid="StorageAttributeFilter"/>` の直後）に以下を追加する:

  ```xml
  <include refid="SearchTasteFilter"/>
  ```

---

### T-06 `ProductFilterOptionService` — `allTasteDisplayNames()` の追加

**ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

- [ ] `import java.util.ArrayList;` `import java.util.LinkedHashSet;` を追加する（未インポートの場合）
- [ ] `allTasteDisplayNames(ProductFilterOptionsBundle bundle)` パブリックメソッドを追加する:
  - `LinkedHashSet<String>` を使い、desk → chair → storage の順で `opt.label()` を挿入する（重複は自動排除・順序保持）
  - `new ArrayList<>(seen)` を返す

  ```java
  public List<String> allTasteDisplayNames(ProductFilterOptionsBundle bundle) {
      LinkedHashSet<String> seen = new LinkedHashSet<>();
      for (CategoryFilterOption opt : bundle.deskTasteOptions()) {
          seen.add(opt.label());
      }
      for (CategoryFilterOption opt : bundle.chairTasteOptions()) {
          seen.add(opt.label());
      }
      for (CategoryFilterOption opt : bundle.storageTasteOptions()) {
          seen.add(opt.label());
      }
      return new ArrayList<>(seen);
  }
  ```

---

### T-07 `ProductFilterOptionService` — `resolveTasteFilter()` の追加

**ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

- [ ] `import java.util.HashSet;` `import java.util.Set;` を追加する（未インポートの場合）
- [ ] `resolveTasteFilter(List<String> displayNames, ProductFilterOptionsBundle bundle)` パブリックメソッドを追加する:
  - `displayNames` が `null` または空の場合は `ProductCategoryFilter.empty()` を返す
  - 入力 `displayNames` を `HashSet` に変換してホワイトリスト照合を高速化する
  - `bundle.deskTasteOptions()` / `chairTasteOptions()` / `storageTasteOptions()` をそれぞれ stream でフィルタリングして taste_id を取得する
  - `new ProductCategoryFilter(List.of(), List.of(), List.of(), List.of(), deskTasteIds, List.of(), List.of(), chairTasteIds, List.of(), storageTasteIds)` を返す
  
  > **注意:** `ProductCategoryFilter` のコンストラクター引数順は以下の通り:  
  > `deskTopShapeIds, deskWidthBandIds, deskDepthBandIds, deskHeightBandIds, deskTasteIds, chairFunctionIds, chairMaterialIds, chairTasteIds, storageUsageIds, storageTasteIds`

---

### T-08 `ProductListSearchService` — `buildCondition()` の変更

**ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`

- [ ] `categoryFilter` を受け取らないオーバーロード（検索結果画面から呼ばれる版）のシグネチャに `List<String> rawTasteNames` を追加する
- [ ] メソッド本体で `ProductCategoryFilter.empty()` ではなく `productFilterOptionService.resolveTasteFilter(rawTasteNames, resolvedBundle)` を呼び出す

  変更前:
  ```java
  public ProductSearchCondition buildCondition(String categoryId, String keyword,
          boolean inStockOnly, List<Integer> rawPriceBandIds, List<String> rawColorKeys,
          String sort, int page, int size, ProductSort defaultSort,
          ProductFilterOptionsBundle optionsBundle) {
      return buildCondition(categoryId, keyword, inStockOnly, rawPriceBandIds, rawColorKeys,
              ProductCategoryFilter.empty(), sort, page, size, defaultSort, optionsBundle);
  }
  ```

  変更後:
  ```java
  public ProductSearchCondition buildCondition(String categoryId, String keyword,
          boolean inStockOnly, List<Integer> rawPriceBandIds, List<String> rawColorKeys,
          List<String> rawTasteNames,
          String sort, int page, int size, ProductSort defaultSort,
          ProductFilterOptionsBundle optionsBundle) {
      List<Integer> selectedPriceBandIds = productFilterOptionService.normalizePriceBandIds(rawPriceBandIds);
      ProductFilterOptionsBundle resolvedBundle = optionsBundle == null
              ? productFilterOptionService.loadOptionsBundle() : optionsBundle;
      List<String> selectedColorKeys = productFilterOptionService.normalizeColorKeys(rawColorKeys, resolvedBundle);
      List<Long> colorIds = productFilterOptionService.resolveColorIds(selectedColorKeys, resolvedBundle);
      ProductCategoryFilter tasteFilter = productFilterOptionService.resolveTasteFilter(rawTasteNames, resolvedBundle);
      return productService.buildCondition(categoryId, keyword, inStockOnly,
              selectedPriceBandIds, colorIds, sort, page, size, defaultSort, tasteFilter);
  }
  ```

  > **注意:** このオーバーロードの変更により、呼び出し元 (`CatalogController`) のコンパイルエラーが発生する。T-09 で合わせて修正すること。

---

### T-09 `CatalogController` — `searchResults()` の変更

**ファイル:** `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`

- [ ] `searchResults()` メソッドのパラメーターに `@RequestParam(name = "taste", required = false) List<String> rawTasteNames` を追加する（`rawColorKeys` の直後）
- [ ] `buildCondition()` 呼び出しに `rawTasteNames` を追加する（`rawColorKeys` の直後・`sort` の前）
- [ ] テイスト選択肢と選択済み値をモデルに追加する（`model.addAttribute("keyword", ...)` の直後）:
  ```java
  List<String> tasteOptions = productFilterOptionService.allTasteDisplayNames(optionsBundle);
  List<String> selectedTasteNames = rawTasteNames != null ? rawTasteNames : List.of();
  model.addAttribute("tasteOptions",       tasteOptions);
  model.addAttribute("selectedTasteNames", selectedTasteNames);
  ```

---

### T-10 `product-list-search-results.html` — テイスト絞込 UI の追加

**ファイル:** `src/main/resources/templates/pages/product-list-search-results.html`

- [ ] デスクトップ用サイドバーフィルターフォームに、カラー絞込セクションの直後にテイスト絞込セクションを追加する:
  ```html
  <div class="field" th:if="${not #lists.isEmpty(tasteOptions)}">
    <label class="filter-label">テイスト</label>
    <div class="checklist">
      <label class="check-item" th:each="tasteName : ${tasteOptions}">
        <input type="checkbox"
               name="taste"
               th:value="${tasteName}"
               th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
        <span th:text="${tasteName}">ナチュラル</span>
      </label>
    </div>
  </div>
  ```

- [ ] ツールバーフォーム（ソート変更・件数変更用）に、`color` の hidden フィールド群の直後に `taste` の hidden フィールドを追加する:
  ```html
  <input type="hidden" th:each="tasteName : ${selectedTasteNames}"
         name="taste" th:value="${tasteName}">
  ```

- [ ] ページネーションのリンク/フォームに `taste` パラメーターを追加する（既存の `color` パラメーターと同じ方式で）

  > **確認:** ページネーションが `th:href` 形式か hidden 付きフォーム submit 形式かを確認してから追加すること。[フロントエンド詳細設計](../design/FEAT-001-frontend-design.md) の「3-4. ページネーション」を参照。

---

## フェーズ 2: 単体テスト実装

テストフレームワーク: JUnit 5 + Mockito 5 + AssertJ  
テストルート: `src/test/java/jp/co/skig/officeorder/`

---

### T-11 `ProductRepositoryKeywordTest` の作成

**ファイル:** `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryKeywordTest.java`

テスト対象: `ProductRepository` の `normalizeKeyword()` / `toKeywordLike()` / `toKeywordPrefix()` / `buildSearchParams()` のキーワード関連ロジック

アノテーション: `@ExtendWith(MockitoExtension.class)`

#### テストケース一覧

**`normalizeKeyword()` のテスト（リフレクションで private メソッドにアクセス、または package-private に変更）**

> `normalizeKeyword()` は private なので、`buildSearchParams()` 経由でテストするか、メソッドアクセス修飾子をパッケージプライベートに変更してテストする。以下は `buildSearchParams()` 経由のテストとして記述する。

- [ ] `キーワードがnullのとき_keywordLikeもkeywordPrefixもnullになること`
  - `condition.keyword() = null`
  - `params["keywordLike"]` が `null` であること
  - `params["keywordPrefix"]` が `null` であること

- [ ] `キーワードが空白のみのとき_keywordLikeもkeywordPrefixもnullになること`
  - `condition.keyword() = "   "`

- [ ] `半角英数キーワードは変化せずkeywordLikeとkeywordPrefixに変換されること`
  - `condition.keyword() = "DK-100"`
  - `params["keywordLike"]` が `"%DK-100%"` であること
  - `params["keywordPrefix"]` が `"DK-100%"` であること

- [ ] `全角英数キーワードはNFKC正規化されて半角になること`
  - `condition.keyword() = "ＤＫ－１００"`
  - `params["keywordLike"]` が `"%DK-100%"` であること
  - `params["keywordPrefix"]` が `"DK-100%"` であること

- [ ] `半角カタカナキーワードはNFKC正規化されて全角になること`
  - `condition.keyword() = "ｱｰﾑﾁｪｱ"`
  - `params["keywordLike"]` が `"%アームチェア%"` であること
  - `params["keywordPrefix"]` が `"アームチェア%"` であること

- [ ] `前後の空白はトリムされること`
  - `condition.keyword() = "  デスク  "`
  - `params["keywordLike"]` が `"%デスク%"` であること

- [ ] `全角カタカナキーワードはNFKC正規化後も全角のままであること`
  - `condition.keyword() = "アームチェア"`
  - `params["keywordLike"]` が `"%アームチェア%"` であること

**`buildSearchParams()` の `keywordPrefix` テスト**

- [ ] `keywordPrefixはkeywordLikeと同じ正規化キーワードに基づいて生成されること`
  - `keyword = "ｱｰﾑﾁｪｱ"` → `keywordLike = "%アームチェア%"` かつ `keywordPrefix = "アームチェア%"`

**`buildSearchParams()` の `hasSearchTasteFilter` テスト**

- [ ] `categoryIdがnullかつdeskTasteIdsが非空のとき_hasSearchTasteFilterがtrueになること`
- [ ] `categoryIdがnullかつchairTasteIdsが非空のとき_hasSearchTasteFilterがtrueになること`
- [ ] `categoryIdがnullかつstorageTasteIdsが非空のとき_hasSearchTasteFilterがtrueになること`
- [ ] `categoryIdがnullかつ全tasteIdsが空のとき_hasSearchTasteFilterがfalseになること`
- [ ] `categoryIdがdeskのとき_tasteIdsが非空でもhasSearchTasteFilterがfalseになること`
  - カテゴリ指定時はカテゴリ固有フィルター（`hasDeskFilter` 等）が担うため

---

### T-12 `ProductFilterOptionServiceTasteTest` の作成

**ファイル:** `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTasteTest.java`

テスト対象: `ProductFilterOptionService.allTasteDisplayNames()` / `resolveTasteFilter()`

アノテーション: `@ExtendWith(MockitoExtension.class)`

モック: `ProductFilterOptionRepository`（`@Mock`）

#### `allTasteDisplayNames()` のテストケース

- [ ] `3カテゴリに同名テイストがある場合_重複排除して1件のみ返すこと`
  - desk: [id=1, "ナチュラル"], chair: [id=2, "ナチュラル"], storage: [id=3, "ナチュラル"]
  - 結果: `["ナチュラル"]`（1件）

- [ ] `全カテゴリのテイストを統合してdeskの順序を優先して返すこと`
  - desk: ["ナチュラル", "モダン"], chair: ["シンプル", "モダン"], storage: ["ナチュラル", "シンプル"]
  - 結果: `["ナチュラル", "モダン", "シンプル"]`（desk 順 → chair/storage の初出 "シンプル" が追加）

- [ ] `全カテゴリのテイストが空のとき_空リストを返すこと`

- [ ] `deskTasteOptionsが空でchairに要素があるとき_chairの要素が返ること`

#### `resolveTasteFilter()` のテストケース

- [ ] `displayNamesがnullのとき_emptyなProductCategoryFilterを返すこと`

- [ ] `displayNamesが空リストのとき_emptyなProductCategoryFilterを返すこと`

- [ ] `deskにのみ存在するテイスト名を渡したとき_deskTasteIdsにのみIDがセットされること`
  - desk: [id=1, "ナチュラル"], chair: [], storage: []
  - 入力: `["ナチュラル"]`
  - 結果: `deskTasteIds=[1], chairTasteIds=[], storageTasteIds=[]`

- [ ] `3カテゴリ共通のテイスト名を渡したとき_各カテゴリのIDがそれぞれにセットされること`
  - desk: [id=1, "ナチュラル"], chair: [id=3, "ナチュラル"], storage: [id=5, "ナチュラル"]
  - 入力: `["ナチュラル"]`
  - 結果: `deskTasteIds=[1], chairTasteIds=[3], storageTasteIds=[5]`

- [ ] `存在しないテイスト名を渡したとき_空リストのフィルターを返すこと`
  - 入力: `["存在しない"]`
  - 結果: 全フィールドが空

- [ ] `複数テイストを渡したとき_複数IDが解決されること`
  - desk: [id=1, "ナチュラル"], [id=2, "モダン"]
  - 入力: `["ナチュラル", "モダン"]`
  - 結果: `deskTasteIds=[1, 2]`

- [ ] `taste以外のfilterフィールドは全て空リストであること`
  - `deskTopShapeIds`, `deskWidthBandIds` 等が `List.of()` であること

---

### T-13 `ProductListSearchServiceSearchTest` の作成

**ファイル:** `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceSearchTest.java`

テスト対象: `ProductListSearchService.buildCondition()` の `rawTasteNames` 処理

アノテーション: `@ExtendWith(MockitoExtension.class)`

モック: `ProductService`（`@Mock`）、`ProductFilterOptionService`（`@Mock`）

#### テストケース一覧

- [ ] `rawTasteNamesがnullのとき_emptyなProductCategoryFilterでProductService.buildConditionが呼ばれること`
  - `resolveTasteFilter(null, bundle)` が呼ばれることを `verify` する
  - `ProductService.buildCondition()` の `categoryFilter` 引数が `ProductCategoryFilter.empty()` と等価であること

- [ ] `rawTasteNamesが非空のとき_resolveTasteFilterの結果がProductService.buildConditionに渡されること`
  - `resolveTasteFilter(["ナチュラル"], bundle)` が `CategoryFilter(deskTasteIds=[1])` を返すようにスタブ設定
  - `ProductService.buildCondition()` が `deskTasteIds=[1]` を持つ `categoryFilter` で呼ばれることを検証

- [ ] `categoryFilter付きオーバーロードはrawTasteNamesを無視すること`（デスク一覧画面向けのオーバーロードが影響を受けないこと）

---

### T-14 `CatalogControllerSearchTest` の作成

**ファイル:** `src/test/java/jp/co/skig/officeorder/web/CatalogControllerSearchTest.java`

テスト対象: `CatalogController.searchResults()`

アノテーション: `@WebMvcTest(CatalogController.class)`

モック: `@MockBean ProductListSearchService`, `@MockBean ProductFilterOptionService`  
（他に `CatalogController` が依存するサービスがあれば `@MockBean` を追加）

#### テストケース一覧

- [ ] `tasteパラメーターなしでリクエストしたとき_200が返り正常に画面が表示されること`
  - `GET /products/search?q=テスト`
  - ステータス 200
  - ビュー名 `pages/product-list-search-results`

- [ ] `tasteパラメーターありでリクエストしたとき_buildConditionにrawTasteNamesが渡されること`
  - `GET /products/search?q=テスト&taste=ナチュラル&taste=モダン`
  - `productListSearchService.buildCondition()` が `rawTasteNames = ["ナチュラル", "モダン"]` で呼ばれることを `verify`

- [ ] `tasteOptionsがモデルに含まれていること`
  - `productFilterOptionService.allTasteDisplayNames()` が `["ナチュラル", "モダン"]` を返すようにスタブ設定
  - `model.getAttribute("tasteOptions")` が `["ナチュラル", "モダン"]` であること

- [ ] `selectedTasteNamesがモデルに含まれていること`
  - `GET /products/search?taste=ナチュラル`
  - `model.getAttribute("selectedTasteNames")` が `["ナチュラル"]` であること

- [ ] `tasteパラメーターなしのとき_selectedTasteNamesが空リストとしてモデルに含まれること`
  - `GET /products/search?q=テスト`（taste パラメーターなし）
  - `model.getAttribute("selectedTasteNames")` が空リストであること

- [ ] `keywordモデル属性が引き続き正しくセットされること`
  - `GET /products/search?q=デスク`
  - `model.getAttribute("keyword")` が `"デスク"` であること

---

## フェーズ 3: ビルドとコンパイル確認

### T-15 コンパイル確認

- [ ] `./mvnw compile` がエラーなく通ること

---

## フェーズ 4: テスト実行

### T-16 単体テスト実行

- [ ] `./mvnw test` を実行し、T-11〜T-14 のテストが全件グリーンになること

---

## フェーズ 5: 動作確認（手動）

### T-17 ローカル環境でのスモークテスト

以下は `./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"` で起動後に確認する。

#### FR-01: 商品コードの前方一致検索

- [ ] ヘッダーの検索ボックスに `DK-` と入力して検索し、`product_code` が `DK-` から始まる商品がヒットすること
- [ ] 商品コードの中間部分（例: `-100`）で検索し、商品コードのみによるヒットがないこと（商品名等にその文字が含まれている場合はヒットしてよい）

#### FR-03: 全角・半角の正規化

- [ ] 全角英数（例: `ＤＫ`）でヘッダー検索し、半角 `DK` と同じ検索結果が返ること
- [ ] 半角カタカナ（例: `ﾃﾞｽｸ`）でヘッダー検索し、全角 `デスク` と同じ検索結果が返ること

#### FR-04: テイスト絞込

- [ ] 検索結果画面（`/products/search`）にテイスト絞込チェックボックスが表示されること
- [ ] テイストを 1 つ選択して「検索」を押したとき、そのテイストに該当する商品のみに絞り込まれること
- [ ] テイストを複数選択したとき、OR 条件でいずれかのテイストに該当する商品が表示されること
- [ ] テイストを選択した状態でページネーションのリンクをクリックし、次のページでもテイスト選択状態が保持されていること
- [ ] テイストを選択した状態でソートを変更しても、テイスト選択状態が保持されていること
- [ ] 「リセット」リンクをクリックするとテイスト選択が解除されること

---

## 補足: テスト実装パターン

### 単体テストの基本構成

```java
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTasteTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    @InjectMocks
    private ProductFilterOptionService service;

    @Test
    void deskにのみ存在するテイスト名を渡したとき_deskTasteIdsにのみIDがセットされること() {
        // Arrange
        var bundle = new ProductFilterOptionsBundle(
                List.of(),                                         // colorOptions
                List.of(),                                         // deskTopShapeOptions
                List.of(new CategoryFilterOption(1, "ナチュラル")), // deskTasteOptions
                List.of(), List.of(),                              // chair系
                List.of(),                                         // chairTasteOptions
                List.of(),                                         // storageUsage
                List.of()                                          // storageTasteOptions
        );

        // Act
        ProductCategoryFilter result = service.resolveTasteFilter(List.of("ナチュラル"), bundle);

        // Assert
        assertThat(result.deskTasteIds()).containsExactly(1);
        assertThat(result.chairTasteIds()).isEmpty();
        assertThat(result.storageTasteIds()).isEmpty();
    }
}
```

### コントローラーテストの基本構成

```java
@WebMvcTest(CatalogController.class)
class CatalogControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductListSearchService productListSearchService;

    @MockBean
    private ProductFilterOptionService productFilterOptionService;
    
    // 他に必要な @MockBean があれば追加

    @BeforeEach
    void setUp() {
        // スタブの共通設定（空の戻り値など）
        given(productFilterOptionService.loadOptionsBundle())
                .willReturn(new ProductFilterOptionsBundle(...));
        given(productListSearchService.buildCondition(any(), any(), anyBoolean(),
                any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .willReturn(/* ProductSearchCondition stub */);
        given(productListSearchService.searchWithPageCorrection(any()))
                .willReturn(/* ProductListSearchResult stub */);
        given(productFilterOptionService.allTasteDisplayNames(any()))
                .willReturn(List.of("ナチュラル", "モダン"));
    }

    @Test
    void tasteパラメーターありでリクエストしたとき_buildConditionにrawTasteNamesが渡されること() throws Exception {
        mockMvc.perform(get("/products/search")
                        .param("q", "テスト")
                        .param("taste", "ナチュラル")
                        .param("taste", "モダン"))
                .andExpect(status().isOk());

        verify(productListSearchService).buildCondition(
                isNull(), eq("テスト"), eq(false),
                isNull(), isNull(),
                eq(List.of("ナチュラル", "モダン")),  // ← rawTasteNames
                isNull(), eq(1), eq(15),
                eq(ProductSort.RECOMMENDED), any());
    }
}
```

---

## 作業リスト（チェックボックスサマリー）

### 実装

- [ ] T-01: `normalizeKeyword()` 追加
- [ ] T-02: `toKeywordPrefix()` 追加
- [ ] T-03: `buildSearchParams()` 変更（`toKeywordLike()` 修正・`keywordPrefix`・`hasSearchTasteFilter` 追加）
- [ ] T-04: `ProductMapper.xml` キーワード条件変更（`variation_name`・`description` 追加・`keywordPrefix` 適用）
- [ ] T-05: `ProductMapper.xml` `SearchTasteFilter` フラグメント追加
- [ ] T-06: `allTasteDisplayNames()` 追加
- [ ] T-07: `resolveTasteFilter()` 追加
- [ ] T-08: `ProductListSearchService.buildCondition()` 変更
- [ ] T-09: `CatalogController.searchResults()` 変更
- [ ] T-10: `product-list-search-results.html` テイスト UI 追加

### 単体テスト

- [ ] T-11: `ProductRepositoryKeywordTest` 作成・実行
- [ ] T-12: `ProductFilterOptionServiceTasteTest` 作成・実行
- [ ] T-13: `ProductListSearchServiceSearchTest` 作成・実行
- [ ] T-14: `CatalogControllerSearchTest` 作成・実行

### ビルド・確認

- [ ] T-15: `./mvnw compile` 通過確認
- [ ] T-16: `./mvnw test` 全件グリーン確認
- [ ] T-17: ローカル手動スモークテスト（FR-01 / FR-03 / FR-04）
