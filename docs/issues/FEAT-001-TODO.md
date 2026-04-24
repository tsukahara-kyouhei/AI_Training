# FEAT-001 啁E��検索機�E強匁E実裁E��画 TODO

| 頁E�� | 冁E�� |
|------|------|
| Issue ID | FEAT-001 |
| ドキュメント種別 | 実裁E��画 TODO |
| 作�E日 | 2026-04-24 |

---

## 概要E

以下�E凡例でスチE�Eタスを管琁E��る、E

| 記号 | 意味 |
|------|------|
| `[ ]` | 未着扁E|
| `[x]` | 完亁E|

実裁E�E **Phase 1 ↁEPhase 9** の頁E��進める。各 Phase 冁E�E原則として上から頁E��実施する、E
依存関係が強ぁE��E��にはメモを補足してぁE��、E

---

## Phase 1: DB マイグレーション

> DBスキーマ�E変更を最初に行う。アプリケーションコード�EチE�Eロイと同時に手動実行する！Elyway 未導�E�E�、E

- [ ] **1-1. マイグレーション SQL の実衁E*
  - ファイル: `sql/schema/` 配下に `V2__unify_taste_master.sql` として保存済み�E�Edocs/design/FEAT-001-db-design.md` § 4 参�E�E�E
  - ローカル DB に手動実行し、以下が完亁E��てぁE��ことを確認すめE
    1. `tastes` チE�Eブルの新設
    2. 旧3チE�Eブル�E�Edesk_tastes` / `chair_tastes` / `storage_tastes`�E�かめE`tastes` へのチE�Eタ移衁E
    3. `product_desk_attributes` / `product_chair_attributes` / `product_storage_attributes` の `taste_id` 外部キー張り替ぁE
    4. 旧3チE�Eブルの DROP
  - 確認クエリ侁E `SELECT * FROM tastes ORDER BY sort_order;`

- [x] **1-2. シードデータ更新: `sql/seed/masters.sql`**
  - 削除: `INSERT INTO desk_tastes ...`、`INSERT INTO chair_tastes ...`、`INSERT INTO storage_tastes ...` の3ブロチE��
  - 追加: `INSERT INTO tastes (display_name, sort_order, is_active) VALUES ...` �E�E件�E�E
  - 参老E `docs/design/FEAT-001-db-design.md` § 5

- [x] **1-3. チE��ト用シードデータの確認�E更新: `sql/seed/test-data/` 配丁E*
  - `products.sql` / `orders.sql` などで旧 `taste_id` を直接参�EしてぁE��箁E��があれ�E、新 `tastes.taste_id` に合わせて更新する
  - 現行データでは旧チE�Eブルの taste_id と新チE�Eブルの taste_id が一致する想定だが目視確認すること

---

## Phase 2: モチE��クラスの変更

> Java の record クラスのフィールド変更。後続�E全クラスが依存するため最初に行う、E

- [x] **2-1. `ProductSearchCondition` にフィールド追加**
  - ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`
  - 変更冁E��: `List<Long> tasteIds` フィールドを `colorIds` の直後！E番目�E�に追加する
  - 変更後�E record 定義�E�参老E `docs/design/FEAT-001-app-design.md` § 3�E�E
    ```java
    public record ProductSearchCondition(
        String categoryId,
        String keyword,
        boolean inStockOnly,
        List<PriceBand> priceBands,
        List<Long> colorIds,
        List<Long> tasteIds,          // ☁E追加
        ProductCategoryFilter categoryFilter,
        ProductSort sort,
        int page,
        int size,
        OffsetDateTime saleStartFrom
    ) {}
    ```
  - こ�E変更により `new ProductSearchCondition(...)` の呼び出し箁E��がコンパイルエラーになめEↁEPhase 4・5・6 で頁E��修正する

- [x] **2-2. `ProductCategoryFilter` をテイスト統合フィールドへ変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 4�E�E
    1. フィールド削除: `deskTasteIds: List<Integer>`、`chairTasteIds: List<Integer>`、`storageTasteIds: List<Integer>`
    2. フィールド追加: `tasteIds: List<Long>`�E�EstorageUsageIds` の直後！E
    3. `empty()` 冁E�E引数めE`List.of()` ÁE8 に更新
    4. `normalize()` 冁E�� `normalizeLongList(tasteIds)` を呼び出すよぁE��新
    5. `isEmpty()` に `&& tasteIds.isEmpty()` を追加�E�Eフィールド参照めEフィールドへ変更�E�E
    6. private メソチE�� `normalizeLongList(List<Long>)` を追加�E�EnormalizeList(List<Integer>)` の Long 版！E

- [x] **2-3. `ProductFilterOptionsBundle` をテイスト統合フィールドへ変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 5�E�E
    1. フィールド削除: `deskTasteOptions`、`chairTasteOptions`、`storageTasteOptions`
    2. フィールド追加: `tasteOptions: List<CategoryFilterOption>`�E�EstorageUsageOptions` の直後！E
    3. コンパクトコンストラクタ冁E�E `immutableOrEmpty` 呼び出しを3ↁEに更新

---

## Phase 3: 新規クラスの作�E

- [x] **3-1. `SearchKeywordNormalizer` を新規作�E**
  - ファイル: `src/main/java/jp/co/skig/officeorder/service/product/SearchKeywordNormalizer.java`
  - ロール: キーワード�E正規化�E��E角英数字�E半角変換・英字小文字化�E�をカプセル化すめE`@Component`
  - 実裁E�E容�E�参老E `docs/design/FEAT-001-app-design.md` § 2�E�E
    - `static final String FULLWIDTH_CHARS`  E全角英大斁E��E26 + 全角英小文孁E26 + 全角数孁E10 = 62 斁E��E
    - `static final String HALFWIDTH_CHARS`  E対応する半见E62 斁E��E
    - `normalize(String keyword): String`  Enull/空白→null、strip→文字変換→`toLowerCase(Locale.ROOT)`
    - `toLikePattern(String normalizedKeyword): String`  E`"%" + kw + "%"`
    - `toPrefixPattern(String normalizedKeyword): String`  E`kw + "%"`

---

## Phase 4: Mapper 層の変更

- [x] **4-1. `ProductMapper.java` のインターフェース変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 11�E�E
    1. 削除メソチE��: `selectActiveDeskTasteOptions()`、`selectActiveChairTasteOptions()`、`selectActiveStorageTasteOptions()`
    2. 追加メソチE��: `List<ProductFilterOptionMapperRow> selectActiveTasteOptions()`

- [x] **4-2. `ProductMapper.xml` の SQL 変更**
  - ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更冁E���E�参老E `docs/design/FEAT-001-sql-design.md`�E�E

  **① `DeskAttributeFilter` フラグメント変更**
  - `deskTasteIds` ↁE`tasteIds` にパラメータ名を変更
  - `<if test="deskTasteIds ...">` ↁE`<if test="tasteIds ...">`
  - `collection="deskTasteIds"` ↁE`collection="tasteIds"`

  **② `ChairAttributeFilter` フラグメント変更**
  - `chairTasteIds` ↁE`tasteIds` にパラメータ名を変更
  - `<if test="chairTasteIds ...">` ↁE`<if test="tasteIds ...">`
  - `collection="chairTasteIds"` ↁE`collection="tasteIds"`

  **③ `StorageAttributeFilter` フラグメント変更**
  - `storageTasteIds` ↁE`tasteIds` にパラメータ名を変更
  - `<if test="storageTasteIds ...">` ↁE`<if test="tasteIds ...">`
  - `collection="storageTasteIds"` ↁE`collection="storageTasteIds"` ↁE`collection="tasteIds"`

  **④ `TasteFilter` フラグメント新規追加**�E�EStorageAttributeFilter` の後に追加�E�E
  - カチE��リ横断チE��スト絞込フラグメント！Esql/design/FEAT-001-sql-design.md` § 4 参�E�E�E
  - `searchTasteIds` コレクションを使ぁE��desk/chair/storage の吁E��性チE�EブルめEEXISTS + IN で絞込む

  **⑤ `BaseProductWhere` フラグメント変更**
  - キーワード条件ブロチE��を以下�Eように全面変更:
    - 変更剁E `p.product_name ILIKE #{keywordLike} OR EXISTS(product_variants ILIKE #{keywordLike})`
    - 変更征E `TRANSLATE(p.product_name, ...) ILIKE #{keywordLike}` + `TRANSLATE(p.variation_name, ...) ILIKE #{keywordLike}` + `TRANSLATE(p.description, ...) ILIKE #{keywordLike}` + `EXISTS (... pvk.product_code = #{keywordExact} OR pvk.product_code ILIKE #{keywordPrefix})`
    - TRANSLATE関数の from/to 斁E���E: 全角英大小文字�E数孁E62 斁E��EↁE半见E62 斁E��！Edocs/design/FEAT-001-sql-design.md` § 2 参�E�E�E
  - `<if test="hasSearchTasteFilter">` ブロチE��を追加ぁE`<include refid="TasteFilter"/>` を呼び出す！EhasDeskFilter` の前に配置�E�E

  **⑥ selectActiveDeskTasteOptions / selectActiveChairTasteOptions / selectActiveStorageTasteOptions 削除**

  **⑦ selectActiveTasteOptions 追加**�E�統吁E`tastes` チE�Eブル参�E�E�E
  ```xml
  <select id="selectActiveTasteOptions" resultMap="ProductFilterOptionRowMap">
      SELECT
      taste_id AS option_id,
      display_name
      FROM tastes
      WHERE is_active = TRUE
      ORDER BY sort_order ASC, taste_id ASC
  </select>
  ```

---

## Phase 5: Repository 層の変更

- [x] **5-1. `ProductFilterOptionRepository` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 6�E�E
    1. 削除メソチE��: `findActiveDeskTasteOptions()`、`findActiveChairTasteOptions()`、`findActiveStorageTasteOptions()`
    2. 追加メソチE��: `findActiveTasteOptions()`  E`productMapper.selectActiveTasteOptions()` を呼び出ぁE

- [x] **5-2. `ProductRepository` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 8�E�E

  **① コンストラクタに `SearchKeywordNormalizer` をDI**
  - フィールド追加: `private final SearchKeywordNormalizer normalizer;`
  - コンストラクタ引数に `SearchKeywordNormalizer normalizer` を追加

  **② `buildSearchParams()` メソチE��変更**

  *変数宣言の変更*:
  - 削除: `List<Integer> deskTasteIds = filter.deskTasteIds();`
  - 削除: `List<Integer> chairTasteIds = filter.chairTasteIds();`
  - 削除: `List<Integer> storageTasteIds = filter.storageTasteIds();`
  - 追加: `List<Long> tasteIds = filter.tasteIds();`
  - 追加: `List<Long> searchTasteIds = condition.tasteIds() == null ? List.of() : condition.tasteIds();`

  *bool フラグの変更*:
  - `hasDeskFilter` の条件: `deskTasteIds` ↁE`tasteIds`
  - `hasChairFilter` の条件: `chairTasteIds` ↁE`tasteIds`
  - `hasStorageFilter` の条件: `storageTasteIds` ↁE`tasteIds`
  - 追加: `boolean hasSearchTasteFilter = !searchTasteIds.isEmpty();`

  *キーワードパラメータの変更*:
  - 削除: `params.put("keywordLike", toKeywordLike(condition.keyword()));`
  - 追加:
    ```java
    String normalizedKeyword = normalizer.normalize(condition.keyword());
    params.put("keywordLike",   normalizer.toLikePattern(normalizedKeyword));
    params.put("keywordExact",  normalizedKeyword);
    params.put("keywordPrefix", normalizer.toPrefixPattern(normalizedKeyword));
    ```

  *params.put の変更*:
  - 削除: `params.put("deskTasteIds", deskTasteIds);`
  - 削除: `params.put("chairTasteIds", chairTasteIds);`
  - 削除: `params.put("storageTasteIds", storageTasteIds);`
  - 追加: `params.put("tasteIds", tasteIds);`
  - 追加: `params.put("searchTasteIds", searchTasteIds);`
  - 追加: `params.put("hasSearchTasteFilter", hasSearchTasteFilter);`

  **③ `findNewestProducts()` 冁E�E `new ProductSearchCondition(...)` を更新**
  - `tasteIds` 引数 `List.of()` を追加�E�コンパイルエラー解消！E

  **④ `findRecommendedProducts()` 冁E�Eフォールバック `new ProductSearchCondition(...)` を更新**
  - `tasteIds` 引数 `List.of()` を追加�E�コンパイルエラー解消！E

  **⑤ `toKeywordLike()` プライベ�EトメソチE��を削除**�E�ESearchKeywordNormalizer` に移管�E�E

---

## Phase 6: Service 層の変更

- [x] **6-1. `ProductFilterOptionService` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 7�E�E

  **① `loadOptionsBundle()` の変更**
  - 旧3メソチE��呼び出しを `findActiveTasteOptions()` 1つに統吁E
  - `ProductFilterOptionsBundle` コンストラクタの引数数が変わるため合わせめE

  **② `buildDeskFilter()` の変更**
  - 引数吁E `rawDeskTasteIds` ↁE`rawTasteIds`
  - `extractOptionIds(optionsBundle.deskTasteOptions())` ↁE`extractOptionIds(optionsBundle.tasteOptions())`
  - `ProductCategoryFilter` コンストラクタ引数: 旧 `deskTasteIds` 位置を削除し、末尾に `toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))` を追加

  **③ `buildDeskFilter()` 単一引数版（引数5つのオーバ�Eロード）�E変更**
  - 引数吁E `rawDeskTasteIds` ↁE`rawTasteIds` に変更し、E引数版に委譲

  **④ `buildChairFilter()` の変更�E�E種 overload とも！E*
  - 引数吁E `rawChairTasteIds` ↁE`rawTasteIds`
  - `extractOptionIds(optionsBundle.chairTasteOptions())` ↁE`extractOptionIds(optionsBundle.tasteOptions())`
  - コンストラクタ引数変更�E�E-3で変更した `ProductCategoryFilter` に合わせる�E�E

  **⑤ `buildStorageFilter()` の変更�E�E種 overload とも！E*
  - 引数吁E `rawStorageTasteIds` ↁE`rawTasteIds`
  - `extractOptionIds(optionsBundle.storageTasteOptions())` ↁE`extractOptionIds(optionsBundle.tasteOptions())`
  - コンストラクタ引数変更

  **⑥ 削除メソチE��**: `deskTasteOptions()`、`chairTasteOptions()`、`storageTasteOptions()`

  **⑦ 追加メソチE��**:
  - `normalizeTasteIds(List<Integer> rawTasteIds, ProductFilterOptionsBundle optionsBundle): List<Long>`  E検索結果画面用の入力値正規化
  - `resolveSelectedTasteIds(List<Long> tasteIds, ProductFilterOptionsBundle optionsBundle): List<Long>`  E画面再表示用の選択済みID整刁E
  - `tasteOptions(): List<CategoryFilterOption>`  E統合テイスト候補一覧を返す
  - `toLongList(List<Integer>): List<Long>`  Eprivate ヘルパ�E�E�Enteger→Long変換�E�E

- [x] **6-2. `ProductService` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 9�E�E

  **① `buildCondition()` 最終形�E��E引数版）に `tasteIds: List<Long>` を追加**
  - `ProductCategoryFilter categoryFilter` の直後に `List<Long> tasteIds` 引数を追加
  - メソチE��冁E�E `new ProductSearchCondition(...)` に `tasteIds` を渡ぁE

  **② 既存�E短縮 overload�E�引数9個�E10個）を修正**
  - 最終形への委譲時に `tasteIds = List.of()` を渡ぁE

  **③ `buildNewArrivalCondition()` の変更**
  - 最終形への委譲時に `tasteIds = List.of()` を渡ぁE

- [x] **6-3. `ProductListSearchService` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 10�E�E

  **① `buildCondition()` への `tasteIds` 引数追加�E�E種 overload とも！E*
  - カチE��リフィルタなし版: `List<Long> tasteIds` を引数追加し、�E部の `buildCondition()` 委譲先に `tasteIds` を渡ぁE
  - カチE��リフィルタあり牁E 同様に `tasteIds` を引数追加ぁE`productService.buildCondition()` に渡ぁE

  **② `searchWithPageCorrection()` の変更**
  - ペ�Eジ補正時�E `new ProductSearchCondition(...)` に `condition.tasteIds()` を追加

---

## Phase 7: Controller の変更

- [x] **7-1. `CatalogController` の変更**
  - ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更冁E���E�参老E `docs/design/FEAT-001-app-design.md` § 12�E�E

  **① `searchResults()` の変更**
  - `@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds` を追加
  - `productFilterOptionService.normalizeTasteIds(rawTasteIds, optionsBundle)` でチE��スチED正規化
  - `productListSearchService.buildCondition(...)` に `tasteIds` を渡す（テイスト対忁Eoverload を呼び出す！E
  - `model.addAttribute("selectedTasteIds", ...)` と `model.addAttribute("tasteOptions", ...)` を追加

  **② `desks()` の変更**
  - `@RequestParam(name = "deskTaste", ...)` ↁE`@RequestParam(name = "taste", ...)` に変更�E�変数名も `rawTasteIds`�E�E
  - `buildDeskFilter()` の第5引数めE`rawTasteIds`�E�引数名変更に合わせる�E�E
  - `model.addAttribute("deskTasteIds", ...)` ↁE`model.addAttribute("tasteIds", deskFilter.tasteIds())`
  - `model.addAttribute("deskTasteOptions", ...)` ↁE`model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

  **③ `chairs()` の変更**
  - `@RequestParam(name = "chairTaste", ...)` ↁE`@RequestParam(name = "taste", ...)` に変更
  - `buildChairFilter()` の第3引数めE`rawTasteIds`
  - `model.addAttribute("chairTasteIds", ...)` ↁE`model.addAttribute("tasteIds", chairFilter.tasteIds())`
  - `model.addAttribute("chairTasteOptions", ...)` ↁE`model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

  **④ `storages()` の変更**
  - `@RequestParam(name = "storageTaste", ...)` ↁE`@RequestParam(name = "taste", ...)` に変更
  - `buildStorageFilter()` の第2引数めE`rawTasteIds`
  - `model.addAttribute("storageTasteIds", ...)` ↁE`model.addAttribute("tasteIds", storageFilter.tasteIds())`
  - `model.addAttribute("storageTasteOptions", ...)` ↁE`model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

---

## Phase 8: チE��プレート�E変更

- [x] **8-1. `header.html`  Eプレースホルダー変更**
  - ファイル: `src/main/resources/templates/fragments/layout/header.html`
  - 変更箁E��: `<input ... placeholder="啁E��名�E啁E��コーチE ...>` ↁE`placeholder="啁E��名�E啁E��コードなど"`

- [x] **8-2. `product-list-search-results.html`  EチE��スト絞込 UI 追加**
  - ファイル: `src/main/resources/templates/pages/product-list-search-results.html`
  - 変更箁E��1  Eサイドバーのフィルターフォームに「テイスト」セクションを追加�E�カラーフィルタの後、`<hr>` 区刁E��ありで�E�E
    ```html
    <hr>
    <div class="field">
        <label>チE��スチE/label>
        <div class="checklist">
            <label class="check-item" th:each="option : ${tasteOptions}">
                <input type="checkbox"
                       name="taste"
                       th:value="${option.id}"
                       th:checked="${selectedTasteIds != null and selectedTasteIds.contains(option.id)}">
                <span th:text="${option.label}">ベ�EシチE��</span>
            </label>
        </div>
    </div>
    ```
  - 変更箁E��2  Eソート�E表示件数フォームの hidden input に `taste` パラメータを追加:
    ```html
    <input type="hidden" name="taste" th:each="id : ${selectedTasteIds}" th:value="${id}">
    ```
  - 変更箁E��3  Eペ�Eジネ�Eションの吁E��ンク URL に `taste=${selectedTasteIds}` を追加�E�前へ/1/中間�Eージ/最終�Eージ/次へ の5箁E���E�E

- [x] **8-3. `product-list-category-desk.html`  EチE��スト参照変更**
  - ファイル: `src/main/resources/templates/pages/product-list-category-desk.html`
  - 変更冁E��:
    - `${deskTasteOptions}` ↁE`${tasteOptions}`
    - `${deskTasteIds}` ↁE`${tasteIds}`
    - `deskTasteIds.contains(...)` ↁE`tasteIds.contains(...)`
    - hidden input の `name="deskTaste"` ↁE`name="taste"`
    - ペ�Eジネ�Eションリンクの `deskTaste=${deskTasteIds}` ↁE`taste=${tasteIds}`�E�前へ/1/中閁E最絁E次へ の吁E��ンク�E�E

- [x] **8-4. `product-list-category-chair.html`  EチE��スト参照変更**
  - ファイル: `src/main/resources/templates/pages/product-list-category-chair.html`
  - 変更冁E��:
    - `${chairTasteOptions}` ↁE`${tasteOptions}`
    - `${chairTasteIds}` ↁE`${tasteIds}`
    - `chairTasteIds.contains(...)` ↁE`tasteIds.contains(...)`
    - hidden input の `name="chairTaste"` ↁE`name="taste"`
    - ペ�Eジネ�Eションリンクの `chairTaste=${chairTasteIds}` ↁE`taste=${tasteIds}`�E�E箁E���E�E

- [x] **8-5. `product-list-category-storage.html`  EチE��スト参照変更**
  - ファイル: `src/main/resources/templates/pages/product-list-category-storage.html`
  - 変更冁E��:
    - `${storageTasteOptions}` ↁE`${tasteOptions}`
    - `${storageTasteIds}` ↁE`${tasteIds}`
    - `storageTasteIds.contains(...)` ↁE`tasteIds.contains(...)`
    - hidden input の `name="storageTaste"` ↁE`name="taste"`
    - ペ�Eジネ�Eションリンクの `storageTaste=${storageTasteIds}` ↁE`taste=${tasteIds}`�E�E箁E���E�E

---

## Phase 9: 単体テスト�E実裁E

> チE��ト対象は純粋なロジチE��クラス�E�外部依存�EなぁE��の�E�を優先する、E
> チE��トクラスは `src/test/java/` 配下に、対象クラスと同一パッケージで作�Eする、E
> 既存�E JUnit チE��トコード�Eスタイル�E�テストメソチE��命名規則・アサーションライブラリなど�E�に合わせること、E

- [x] **9-1. `SearchKeywordNormalizerTest`  E新規作�E**
  - ファイル: `src/test/java/jp/co/skig/officeorder/service/product/SearchKeywordNormalizerTest.java`
  - チE��トケース�E�要件定義書 § 6.3 のチE��ト方針を実裁E��度に落とす！E

  | # | メソチE�� | 入劁E| 期征E�E劁E| 要件対忁E|
  |---|----------|------|----------|---------|
  | 1 | `normalize` | `null` | `null` |  E|
  | 2 | `normalize` | `"  "` (空白のみ) | `null` |  E|
  | 3 | `normalize` | `"�E��E��E�"` (全角大斁E��E | `"abc"` | 要件 #1, #4 |
  | 4 | `normalize` | `"�E�E��ａE` (全角小文孁E | `"abc"` | 要件 #2 |
  | 5 | `normalize` | `"�E�１！E` (全角数孁E | `"012"` | 要件 #3 |
  | 6 | `normalize` | `"ABC"` (半角大斁E��E | `"abc"` | 要件 #4 |
  | 7 | `normalize` | `"abc"` (半角小文孁E | `"abc"` |  E|
  | 8 | `normalize` | `"P0001-C01"` | `"p0001-c01"` | 要件 #5 |
  | 9 | `normalize` | `"　チE��ク　"` (全角スペ�Eス含む) | `"　チE��ク　"` (トリムなし�E変換対象夁E |  E|
  | 10 | `toLikePattern` | `"abc"` | `"%abc%"` |  E|
  | 11 | `toLikePattern` | `null` | `null` |  E|
  | 12 | `toPrefixPattern` | `"p0001"` | `"p0001%"` |  E|
  | 13 | `toPrefixPattern` | `null` | `null` |  E|

- [x] **9-2. `ProductCategoryFilterTest`  E変更対忁E*
  - ファイル: `src/test/java/jp/co/skig/officeorder/model/product/ProductCategoryFilterTest.java`�E�新規作�E or 既存更新�E�E
  - チE��トケース:

  | # | メソチE�� | 確認�E容 |
  |---|----------|---------|
  | 1 | `empty()` | 全フィールドが空リスト！EasteIds を含む 8 フィールド！E|
  | 2 | `isEmpty()` | 全フィールドが空の場合に `true` |
  | 3 | `isEmpty()` | `tasteIds` のみに値がある場合に `false` |
  | 4 | `normalize()` | `null` を含む `tasteIds` リストが重褁E��除・null 除去されめE|
  | 5 | `normalize()` | 重褁E��ぁE`tasteIds` が除去されめE|

- [x] **9-3. `ProductFilterOptionServiceTest`  E変更・追加メソチE��のチE��チE*
  - ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`�E�新規作�E or 既存更新�E�E
  - チE��トケース�E�Eockito でリポジトリをモチE���E�E

  | # | メソチE�� | 確認�E容 |
  |---|----------|---------|
  | 1 | `buildDeskFilter()` | 有効な `rawTasteIds` ぁE`ProductCategoryFilter.tasteIds()` に Long 型で格納される |
  | 2 | `buildDeskFilter()` | 許可リスト外�E `rawTasteIds` は除外される |
  | 3 | `buildChairFilter()` | 有効な `rawTasteIds` ぁE`tasteIds` に格納される |
  | 4 | `buildStorageFilter()` | 有効な `rawTasteIds` ぁE`tasteIds` に格納される |
  | 5 | `normalizeTasteIds()` | null 入劁EↁE空リスト返却 |
  | 6 | `normalizeTasteIds()` | 許可リスト�EIDのみ返却 |
  | 7 | `resolveSelectedTasteIds()` | 空入劁EↁE空リスチE|
  | 8 | `resolveSelectedTasteIds()` | 候補�E表示頁E��返却されめE|

- [x] **9-4. `ProductListSearchServiceTest`  E`buildCondition` / `searchWithPageCorrection` チE��チE*
  - ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceTest.java`�E�新規作�E or 既存更新�E�E
  - チE��トケース:

  | # | メソチE�� | 確認�E容 |
  |---|----------|---------|
  | 1 | `buildCondition()` | `tasteIds` ぁE`ProductSearchCondition.tasteIds()` に反映されめE|
  | 2 | `buildCondition()` | `tasteIds = null` ↁE`ProductSearchCondition.tasteIds()` が空リスチE|
  | 3 | `searchWithPageCorrection()` | ペ�Eジ補正時に `tasteIds` が補正後�E条件に引き継がれる |

- [x] **9-5. `ProductServiceTest`  E`buildCondition` tasteIds 対応�EチE��チE*
  - ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductServiceTest.java`�E�新規作�E or 既存更新�E�E
  - チE��トケース:

  | # | メソチE�� | 確認�E容 |
  |---|----------|---------|
  | 1 | `buildCondition()`�E�最終形�E�E| `tasteIds` が条件に設定される |
  | 2 | `buildCondition()`�E�短縮形�E�E| `tasteIds` ぁE`List.of()` になめE|
  | 3 | `buildNewArrivalCondition()` | `tasteIds` ぁE`List.of()` になめE|

---

## 動作確認チェチE��リスト（実裁E��亁E��！E

要件定義書 § 6.3 チE��ト方針�Eケースを実機で確認する、E

| # | チE��トケース | 確認方況E|
|---|------------|---------|
| 1 | 全角英字でキーワード�E劁EↁE半角英字データにヒッチE| `q=�E��E��E�` で検索ぁEABC を含む啁E��名に一致 |
| 2 | 半角英字�E劁EↁE全角英字データにヒッチE| `q=ABC` で全角英字を含む啁E��に一致 |
| 3 | 全角数字�E劁EↁE半角数字データにヒッチE| `q=�E�１２` で 012 を含む啁E��名に一致 |
| 4 | 大斁E���E劁EↁE小文字データにヒッチE| `q=DESK` で desk を含む啁E��にヒッチE|
| 5 | 啁E��コード完�E一致 | `q=P0001-C01` で当該啁E��にのみヒッチE|
| 6 | 啁E��コード前方一致 | `q=P0001` で P0001 始まり�E品にヒッチE|
| 7 | 啁E��コード中間一致しなぁE| `q=0001` で P0001 がヒチE��しなぁE|
| 8 | 説明文一致 | 説明文にあるキーワードでヒッチE|
| 9 | バリエーション名一致 | variation_name にあるキーワードでヒッチE|
| 10 | チE��スト単一絞込 | `taste=1` で該当テイスト商品�Eみ表示 |
| 11 | チE��スト褁E��絞込�E�ER�E�E| `taste=1&taste=2` で両チE��スト商品が表示 |
| 12 | チE��スト未選抁E| チE��スト絞込が行われず全啁E��表示 |
| 13 | チE��スト絞込後�Eージ遷移 | ペ�Eジ遷移後も `taste` パラメータが保持されめE|
| 14 | キーワード未入力�EチE��スト未選抁E| 全啁E��が表示されめE|
| 15 | カチE��リ一覧�E�デスク�E�テイスト絞込 | `taste=1` で `/categories/desks` のチE��ク絞込が動作すめE|
| 16 | カチE��リ一覧�E�チェア�E�テイスト絞込 | `taste=1` で `/categories/chairs` の絞込が動作すめE|
| 17 | カチE��リ一覧�E�収納）テイスト絞込 | `taste=1` で `/categories/storages` の絞込が動作すめE|
