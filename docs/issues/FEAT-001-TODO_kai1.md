# FEAT-001 改修1: テイストマスタ統合 実装計画 TODO リスト

> 要件定義書: `docs/issues/FEAT-001-requirements_kai1.md`
> 設計書: `docs/design/FEAT-001-kai1-*.md`
> 実装ブランチ: `ebihara.toshikazu`

---

## 実装方針の前提確認

- **`tasteIds` の型:** `List<Integer>`（既存の `deskTasteIds` 等と同じ型を維持する）
- **URL パラメータ名:** カテゴリ一覧画面のテイスト絞込は `name="taste"` に統一する（`deskTaste`・`chairTaste`・`storageTaste` を廃止）
- **`findUnifiedSearchTasteOptions()` のメソッド名:** 検索結果画面の呼び出し元との互換性を保つためリネームしない（内部 SQL のみ変更）

---

## TODO リスト

### Phase 1: DB 変更

- [x] **1-1. `tastes` テーブルを `sql/schema/masters.sql` に追加する**
  - 対象ファイル: `sql/schema/masters.sql`
  - 追加内容: `tastes` テーブル DDL（`taste_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`、`display_name VARCHAR(100) NOT NULL UNIQUE`、`sort_order INTEGER`、`is_active BOOLEAN`、`created_at TIMESTAMPTZ`、`updated_at TIMESTAMPTZ`）
  - インデックス: `idx_tastes_active_sort ON tastes (is_active, sort_order)`
  - テーブルコメント / カラムコメント: 既存 3 テーブルに準じて追記
  - 位置: `storage_tastes` の定義の直後、`desk_top_shapes` 〜 `storage_tastes` のセクションの末尾に追記
  - 注意: 旧テーブル（`desk_tastes`・`chair_tastes`・`storage_tastes`）の DDL はこの段階では**削除しない**（手順4で削除）

- [x] **1-2. マイグレーション SQL を `sql/schema/products.sql` に追記する**
  - 対象ファイル: `sql/schema/products.sql`（末尾に追記）
  - 追記内容（順序厳守）:
    1. **データ投入：** `desk_tastes`・`chair_tastes`・`storage_tastes` 各データを `tastes` に `INSERT ... ON CONFLICT (display_name) DO NOTHING`
    2. **taste_id 更新：** `product_desk_attributes` / `product_chair_attributes` / `product_storage_attributes` の `taste_id` を、旧テーブルの `display_name` を経由して新 `tastes.taste_id` に `UPDATE ... FROM ... JOIN`
    3. **FK 差し替え：** 旧テーブルへの FK 制約を `DROP CONSTRAINT`し、`tastes` への FK 制約を `ADD CONSTRAINT`（3 属性テーブル分）
    4. **旧テーブル削除：** `DROP TABLE IF EXISTS desk_tastes`, `chair_tastes`, `storage_tastes`
  - 注意: 各 SQL は冪等性を確保すること（`INSERT ... ON CONFLICT DO NOTHING` を使用）

- [x] **1-3. `sql/seed/masters.sql` のテイスト INSERT を置き換える**
  - 対象ファイル: `sql/seed/masters.sql`
  - 変更内容:
    - 削除: `INSERT INTO desk_tastes ...`、`INSERT INTO chair_tastes ...`、`INSERT INTO storage_tastes ...` の 3 件
    - 追加: `INSERT INTO tastes (display_name, sort_order, is_active) VALUES ('ベーシック',...), ..., ('ナチュラル',...) ON CONFLICT DO NOTHING;`
  - 位置: `desk_tastes` INSERT があった箇所に置き換える

- [x] **1-4. `sql/schema/masters.sql` から旧テーブル DDL を削除する**
  - 対象ファイル: `sql/schema/masters.sql`
  - 削除対象: `desk_tastes` / `chair_tastes` / `storage_tastes` の `CREATE TABLE IF NOT EXISTS` ブロックおよびインデックス・コメント定義
  - 注意: 1-2 のマイグレーション SQL が `products.sql` に追記済みであることを確認してから実施

---

### Phase 2: Model 変更

- [x] **2-1. `ProductCategoryFilter` の taste 3 フィールドを `tasteIds` に集約する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java`
  - 変更内容:
    - **削除フィールド:** `List<Integer> deskTasteIds`（5番目）、`List<Integer> chairTasteIds`（8番目）、`List<Integer> storageTasteIds`（10番目）
    - **追加フィールド:** `List<Integer> tasteIds`（末尾、8番目）
    - **変更後のフィールド順序:** `deskTopShapeIds`, `deskWidthBandIds`, `deskDepthBandIds`, `deskHeightBandIds`, `chairFunctionIds`, `chairMaterialIds`, `storageUsageIds`, `tasteIds`
    - **`empty()` の更新:** `List.of()` を 8 個に変更
    - **`normalize()` の更新:** taste 関連を `normalizeList(tasteIds)` 1 行に統合
    - **`isEmpty()` の更新:** `deskTasteIds.isEmpty() && chairTasteIds.isEmpty() && storageTasteIds.isEmpty()` → `&& tasteIds.isEmpty()` 1 行に変更

- [x] **2-2. `ProductFilterOptionsBundle` の taste 3 フィールドを `tasteOptions` に集約する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java`
  - 変更内容:
    - **削除フィールド:** `List<CategoryFilterOption> deskTasteOptions`、`List<CategoryFilterOption> chairTasteOptions`、`List<CategoryFilterOption> storageTasteOptions`
    - **追加フィールド:** `List<CategoryFilterOption> tasteOptions`（末尾、6番目）
    - **変更後のフィールド順序:** `colorOptions`, `deskTopShapeOptions`, `chairFunctionOptions`, `chairMaterialOptions`, `storageUsageOptions`, `tasteOptions`
    - **コンパクトコンストラクタ内の `immutableOrEmpty` 呼び出し:** taste 3 行を `tasteOptions = immutableOrEmpty(tasteOptions);` 1 行に変更
  - 注意: record フィールド変更後にコンパイルエラーが発生する箇所（Service・Repository）を続く Phase で都度修正する

---

### Phase 3: Mapper インターフェース変更

- [x] **3-1. `ProductMapper.java` の旧テイストメソッドを削除し `selectActiveTasteOptions` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`
  - 削除: `List<ProductFilterOptionMapperRow> selectActiveDeskTasteOptions();`
  - 削除: `List<ProductFilterOptionMapperRow> selectActiveChairTasteOptions();`
  - 削除: `List<ProductFilterOptionMapperRow> selectActiveStorageTasteOptions();`
  - 追加: `/** カテゴリ横断テイスト選択肢を取得する。 */ List<ProductFilterOptionMapperRow> selectActiveTasteOptions();`
  - `selectUnifiedSearchTasteOptions()` は変更なし（シグネチャそのまま）

---

### Phase 4: Mapper XML 変更

- [x] **4-1. `DeskAttributeFilter` のテイスト条件を `tasteIds` に変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更箇所: `DeskAttributeFilter` フラグメント内の `deskTasteIds` → `tasteIds`
    ```xml
    <!-- 変更前 -->
    <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">
        AND da.taste_id IN
        <foreach collection="deskTasteIds" item="value" ...>

    <!-- 変更後 -->
    <if test="tasteIds != null and tasteIds.size() &gt; 0">
        AND da.taste_id IN
        <foreach collection="tasteIds" item="value" ...>
    ```

- [x] **4-2. `ChairAttributeFilter` のテイスト条件を `tasteIds` に変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更箇所: `ChairAttributeFilter` フラグメント内の `chairTasteIds` → `tasteIds`
    ```xml
    <!-- 変更前 -->
    <if test="chairTasteIds != null and chairTasteIds.size() &gt; 0">
        AND ca.taste_id IN
        <foreach collection="chairTasteIds" item="value" ...>

    <!-- 変更後 -->
    <if test="tasteIds != null and tasteIds.size() &gt; 0">
        AND ca.taste_id IN
        <foreach collection="tasteIds" item="value" ...>
    ```

- [x] **4-3. `StorageAttributeFilter` のテイスト条件を `tasteIds` に変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更箇所: `StorageAttributeFilter` フラグメント内の `storageTasteIds` → `tasteIds`
    ```xml
    <!-- 変更前 -->
    <if test="storageTasteIds != null and storageTasteIds.size() &gt; 0">
        AND sa.taste_id IN
        <foreach collection="storageTasteIds" item="value" ...>

    <!-- 変更後 -->
    <if test="tasteIds != null and tasteIds.size() &gt; 0">
        AND sa.taste_id IN
        <foreach collection="tasteIds" item="value" ...>
    ```

- [x] **4-4. `SearchTasteFilter` の JOIN 先を `tastes` に変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更箇所: `SearchTasteFilter` フラグメント内の 3 本の JOIN を変更
    - `JOIN desk_tastes dt ON da.taste_id = dt.taste_id` → `JOIN tastes t ON da.taste_id = t.taste_id`
    - `AND dt.display_name IN` → `AND t.display_name IN`
    - `JOIN chair_tastes ct ON ca.taste_id = ct.taste_id` → `JOIN tastes t ON ca.taste_id = t.taste_id`
    - `AND ct.display_name IN` → `AND t.display_name IN`
    - `JOIN storage_tastes st ON sa.taste_id = st.taste_id` → `JOIN tastes t ON sa.taste_id = t.taste_id`
    - `AND st.display_name IN` → `AND t.display_name IN`

- [x] **4-5. `selectActiveTasteOptions` クエリを追加する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 追加内容: `selectActiveDeskTasteOptions` があった位置付近に以下を追加
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
  - `resultMap` は既存の `ProductFilterOptionRowMap` を流用する

- [x] **4-6. 旧テイストクエリ 3 件を削除する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 削除対象:
    - `<select id="selectActiveDeskTasteOptions" ...>` ブロック全体
    - `<select id="selectActiveChairTasteOptions" ...>` ブロック全体
    - `<select id="selectActiveStorageTasteOptions" ...>` ブロック全体

- [x] **4-7. `selectUnifiedSearchTasteOptions` を `tastes` 直接参照に変更する**
  - 対象ファイル: `src/main/resources/mappers/ProductMapper.xml`
  - 変更内容: UNION ALL クエリを削除し、以下に置き換える
    ```xml
    <select id="selectUnifiedSearchTasteOptions" resultMap="ProductFilterOptionRowMap">
        SELECT
        NULL AS option_id,
        display_name
        FROM tastes
        WHERE is_active = TRUE
        ORDER BY sort_order ASC, taste_id ASC
    </select>
    ```

---

### Phase 5: Repository 変更

- [x] **5-1. `ProductFilterOptionRepository` の旧テイストメソッドを削除し `findActiveTasteOptions` を追加する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`
  - 削除: `findActiveDeskTasteOptions()` メソッド（`selectActiveDeskTasteOptions()` 呼び出し）
  - 削除: `findActiveChairTasteOptions()` メソッド（`selectActiveChairTasteOptions()` 呼び出し）
  - 削除: `findActiveStorageTasteOptions()` メソッド（`selectActiveStorageTasteOptions()` 呼び出し）
  - 追加:
    ```java
    /** カテゴリ横断テイスト選択肢を取得する。 */
    public List<CategoryFilterOption> findActiveTasteOptions() {
        return productMapper.selectActiveTasteOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }
    ```
  - `findUnifiedSearchTasteOptions()` は変更なし

- [x] **5-2. `ProductRepository.buildSearchParams()` の taste パラメータを集約する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
  - 変更内容:
    1. ローカル変数 `deskTasteIds`・`chairTasteIds`・`storageTasteIds` を削除
    2. ローカル変数 `tasteIds` を追加: `List<Integer> tasteIds = filter.tasteIds();`
    3. `hasDeskFilter` の条件に `|| !tasteIds.isEmpty()` を追加（既存の `!deskTasteIds.isEmpty()` を置き換え）
    4. `hasChairFilter` の条件に `|| !tasteIds.isEmpty()` を追加（既存の `!chairTasteIds.isEmpty()` を置き換え）
    5. `hasStorageFilter` の条件に `|| !tasteIds.isEmpty()` を追加（既存の `!storageTasteIds.isEmpty()` を置き換え）
    6. `params.put("deskTasteIds", deskTasteIds)` を削除
    7. `params.put("chairTasteIds", chairTasteIds)` を削除
    8. `params.put("storageTasteIds", storageTasteIds)` を削除
    9. `params.put("tasteIds", tasteIds)` を追加（`params.put("deskTopShapeIds", ...)` の近くに配置）

---

### Phase 6: Service 変更

- [x] **6-1. `ProductFilterOptionService.loadOptionsBundle()` を更新する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 変更内容: `new ProductFilterOptionsBundle(...)` のコンストラクタ呼び出しを変更
    - 削除: `productFilterOptionRepository.findActiveDeskTasteOptions()` 引数
    - 削除: `productFilterOptionRepository.findActiveChairTasteOptions()` 引数
    - 削除: `productFilterOptionRepository.findActiveStorageTasteOptions()` 引数
    - 追加: `productFilterOptionRepository.findActiveTasteOptions()` 引数（末尾）

- [x] **6-2. `ProductFilterOptionService.buildDeskFilter()` のシグネチャを変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 変更内容（5引数版・6引数版の両方）:
    - パラメータ名 `rawDeskTasteIds` → `rawTasteIds`
    - Javadoc `@param rawDeskTasteIds` → `@param rawTasteIds テイストの生入力値`
  - 6引数版の内部実装変更:
    - `List<Integer> allowedDeskTasteIds = extractOptionIds(optionsBundle.deskTasteOptions())` → `List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions())`
    - `new ProductCategoryFilter(...)` の引数を 8 個に変更し、デスク固有の 4 フィールドのあとに `List.of()(chairFunctionIds)`, `List.of()(chairMaterialIds)`, `List.of()(storageUsageIds)`, `normalizeIntegerOptions(rawTasteIds, allowedTasteIds)` の順で渡す

- [x] **6-3. `ProductFilterOptionService.buildChairFilter()` のシグネチャを変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 変更内容（4引数版・5引数版の両方）:
    - パラメータ名 `rawChairTasteIds` → `rawTasteIds`
    - Javadoc 更新
  - 5引数版の内部実装変更:
    - `List<Integer> allowedChairTasteIds = extractOptionIds(optionsBundle.chairTasteOptions())` → `List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions())`
    - `new ProductCategoryFilter(...)` の引数を 8 個に変更し、`List.of()(desk系 4 個)`, chairFunctionIds, chairMaterialIds, `List.of()(storageUsageIds)`, `normalizeIntegerOptions(rawTasteIds, allowedTasteIds)` の順で渡す

- [x] **6-4. `ProductFilterOptionService.buildStorageFilter()` のシグネチャを変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 変更内容（3引数版・4引数版の両方）:
    - パラメータ名 `rawStorageTasteIds` → `rawTasteIds`
    - Javadoc 更新
  - 4引数版の内部実装変更:
    - `List<Integer> allowedStorageTasteIds = extractOptionIds(optionsBundle.storageTasteOptions())` → `List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions())`
    - `new ProductCategoryFilter(...)` の引数を 8 個に変更し、`List.of()(desk系 4 個)`, `List.of()(chairFunctionIds)`, `List.of()(chairMaterialIds)`, normalizeIntegerOptions(rawStorageUsageIds, allowedStorageUsageIds), `normalizeIntegerOptions(rawTasteIds, allowedTasteIds)` の順で渡す

- [x] **6-5. `ProductFilterOptionService` のショートハンドメソッドを更新する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
  - 削除: `deskTasteOptions()` メソッド
  - 削除: `chairTasteOptions()` メソッド
  - 削除: `storageTasteOptions()` メソッド
  - 追加:
    ```java
    /** テイスト候補（統合）を返す。 */
    public List<CategoryFilterOption> tasteOptions() {
        return loadOptionsBundle().tasteOptions();
    }
    ```
  - `loadUnifiedSearchTasteOptions()` / `normalizeSearchTasteNames()` は変更なし

---

### Phase 7: Controller 変更

- [x] **7-1. `CatalogController.desks()` の taste 関連を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更内容:
    - `@RequestParam(name = "deskTaste", required = false) List<Integer> rawDeskTasteIds` → `@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds`
    - `buildDeskFilter(rawDeskTopShapeIds, ..., rawDeskTasteIds, optionsBundle)` → `buildDeskFilter(rawDeskTopShapeIds, ..., rawTasteIds, optionsBundle)`
    - `model.addAttribute("deskTasteIds", deskFilter.deskTasteIds())` → `model.addAttribute("tasteIds", deskFilter.tasteIds())`
    - `model.addAttribute("deskTasteOptions", optionsBundle.deskTasteOptions())` → `model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

- [x] **7-2. `CatalogController.chairs()` の taste 関連を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更内容:
    - `@RequestParam(name = "chairTaste", required = false) List<Integer> rawChairTasteIds` → `@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds`
    - `buildChairFilter(rawChairFunctionIds, rawChairMaterialIds, rawChairTasteIds, optionsBundle)` → `buildChairFilter(rawChairFunctionIds, rawChairMaterialIds, rawTasteIds, optionsBundle)`
    - `model.addAttribute("chairTasteIds", chairFilter.chairTasteIds())` → `model.addAttribute("tasteIds", chairFilter.tasteIds())`
    - `model.addAttribute("chairTasteOptions", optionsBundle.chairTasteOptions())` → `model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

- [x] **7-3. `CatalogController.storages()` の taste 関連を変更する**
  - 対象ファイル: `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
  - 変更内容:
    - `@RequestParam(name = "storageTaste", required = false) List<Integer> rawStorageTasteIds` → `@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds`
    - `buildStorageFilter(rawStorageUsageIds, rawStorageTasteIds, optionsBundle)` → `buildStorageFilter(rawStorageUsageIds, rawTasteIds, optionsBundle)`
    - `model.addAttribute("storageTasteIds", storageFilter.storageTasteIds())` → `model.addAttribute("tasteIds", storageFilter.tasteIds())`
    - `model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions())` → `model.addAttribute("tasteOptions", optionsBundle.tasteOptions())`

---

### Phase 8: テンプレート変更

- [x] **8-1. `product-list-category-desk.html` のテイスト絞込 UI を変更する**
  - 対象ファイル: `src/main/resources/templates/pages/product-list-category-desk.html`
  - 変更内容（テイストセクション全体）:
    - `th:each="option : ${deskTasteOptions}"` → `th:each="option : ${tasteOptions}"`
    - `name="deskTaste"` → `name="taste"`
    - `th:checked="${deskTasteIds != null and deskTasteIds.contains(option.id)}"` → `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"`
  - ページネーションリンク内に `deskTaste=` クエリパラメータが含まれる場合は `taste=` に変更する

- [x] **8-2. `product-list-category-chair.html` のテイスト絞込 UI を変更する**
  - 対象ファイル: `src/main/resources/templates/pages/product-list-category-chair.html`
  - 変更内容:
    - `th:each="option : ${chairTasteOptions}"` → `th:each="option : ${tasteOptions}"`
    - `name="chairTaste"` → `name="taste"`
    - `th:checked="${chairTasteIds != null and chairTasteIds.contains(option.id)}"` → `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"`
  - ページネーションリンク内に `chairTaste=` クエリパラメータが含まれる場合は `taste=` に変更する

- [x] **8-3. `product-list-category-storage.html` のテイスト絞込 UI を変更する**
  - 対象ファイル: `src/main/resources/templates/pages/product-list-category-storage.html`
  - 変更内容:
    - `th:each="option : ${storageTasteOptions}"` → `th:each="option : ${tasteOptions}"`
    - `name="storageTaste"` → `name="taste"`
    - `th:checked="${storageTasteIds != null and storageTasteIds.contains(option.id)}"` → `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"`
  - ページネーションリンク内に `storageTaste=` クエリパラメータが含まれる場合は `taste=` に変更する

---

### Phase 9: 単体テスト

- [x] **9-1. `ProductFilterOptionServiceTest.java` を更新する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`
  - 変更内容:
    - `loadUnifiedSearchTasteOptions_delegatesToRepository()` テスト: `repository.findUnifiedSearchTasteOptions()` のモックと検証は変更なし
    - `normalizeSearchTasteNames_*` テスト群: 変更なし

- [x] **9-2. `ProductFilterOptionServiceTasteOptionsTest.java` を新規作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTasteOptionsTest.java`
  - 検証内容:
    - `loadOptionsBundle_includesTasteOptions`: `repository.findActiveTasteOptions()` が呼ばれ、戻り値が `ProductFilterOptionsBundle.tasteOptions()` に設定されることを検証
    - `buildDeskFilter_withValidTasteId_setsTasteIds`: 有効な `rawTasteIds` を渡したとき、戻り値 `ProductCategoryFilter.tasteIds()` に正規化済み ID が入ることを検証
    - `buildDeskFilter_withInvalidTasteId_filtersOut`: 許容外の ID は除外されることを検証
    - `buildChairFilter_withValidTasteId_setsTasteIds`: チェア版の同様の検証
    - `buildStorageFilter_withValidTasteId_setsTasteIds`: 収納版の同様の検証
    - `tasteOptions_delegatesToBundle`: `tasteOptions()` ショートハンドが `optionsBundle.tasteOptions()` を返すことを検証
  - 実装パターン: 既存 `ProductFilterOptionServiceTest` の `mock`・`when`・`verify`・`assertThat` スタイルに準拠

- [x] **9-3. `ProductCategoryFilterTest.java` を新規作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/model/product/ProductCategoryFilterTest.java`
  - 検証内容:
    - `empty_tasteIdsIsEmpty`: `ProductCategoryFilter.empty()` の `tasteIds()` が空リストであることを検証
    - `normalize_tasteIds_deduplicatesAndFiltersNull`: `normalize()` が null 除去・重複排除を行うことを検証
    - `isEmpty_withOnlyTasteIds_returnsFalse`: `tasteIds` のみ非空のとき `isEmpty()` が `false` を返すことを検証
    - `isEmpty_allEmpty_returnsTrue`: 全フィールドが空のとき `isEmpty()` が `true` を返すことを検証

- [x] **9-4. `ProductRepositoryTasteParamsTest.java` を新規作成する**
  - 対象ファイル: `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTasteParamsTest.java`
  - 検証内容:
    - `buildSearchParams_withTasteIdsOnDeskPage_putsTasteIdsAndEnablesDeskFilter`: `categoryId=desk` かつ `tasteIds=[1]` のとき、`params["tasteIds"]=[1]` かつ `params["hasDeskFilter"]=true` になることを検証
    - `buildSearchParams_withTasteIdsOnChairPage_putsChairFilter`: `categoryId=chair` かつ `tasteIds=[1]` のとき `hasChairFilter=true`
    - `buildSearchParams_withTasteIdsOnStoragePage_putsStorageFilter`: `categoryId=storage` かつ `tasteIds=[1]` のとき `hasStorageFilter=true`
    - `buildSearchParams_emptyTasteIds_doesNotEnableFilters`: `tasteIds` が空のとき `hasDeskFilter/hasChairFilter/hasStorageFilter` が他条件に依存することを検証
    - `buildSearchParams_noDeskTasteIds_key_exists`: `params` に `deskTasteIds` / `chairTasteIds` / `storageTasteIds` キーが存在しないことを検証
  - 実装パターン: Mockito で `ProductMapper` と `AppTimeProvider` をモックし、`search()` 呼び出し時の `params` を `ArgumentCaptor` でキャプチャして検証

---

## 実装順序（依存関係に基づく推奨順）

```
Phase 1 (DB・シード)
    ↓
Phase 2 (Model)       ← record フィールド変更のため、以降は Phase 2 完了後に実施
    ↓
Phase 3 (Mapper IF)
    ↓
Phase 4 (Mapper XML)
    ↓
Phase 5 (Repository)
    ↓
Phase 6 (Service)
    ↓
Phase 7 (Controller)
    ↓
Phase 8 (Template)
    ↓
Phase 9 (Tests)       ← 各 Phase 完了後に随時追加可
```

---

## 変更ファイル一覧

| Phase | ファイルパス | 種別 | 状態 |
|-------|------------|------|------|
| 1 | `sql/schema/masters.sql` | 変更 | ✅ |
| 1 | `sql/schema/products.sql` | 変更 | ✅ |
| 1 | `sql/seed/masters.sql` | 変更 | ✅ |
| 2 | `src/main/java/.../model/product/ProductCategoryFilter.java` | 変更 | ✅ |
| 2 | `src/main/java/.../model/product/ProductFilterOptionsBundle.java` | 変更 | ✅ |
| 3 | `src/main/java/.../mapper/ProductMapper.java` | 変更 | ✅ |
| 4 | `src/main/resources/mappers/ProductMapper.xml` | 変更 | ✅ |
| 5 | `src/main/java/.../repository/ProductFilterOptionRepository.java` | 変更 | ✅ |
| 5 | `src/main/java/.../repository/ProductRepository.java` | 変更 | ✅ |
| 6 | `src/main/java/.../service/product/ProductFilterOptionService.java` | 変更 | ✅ |
| 7 | `src/main/java/.../web/CatalogController.java` | 変更 | ✅ |
| 8 | `src/main/resources/templates/pages/product-list-category-desk.html` | 変更 | ✅ |
| 8 | `src/main/resources/templates/pages/product-list-category-chair.html` | 変更 | ✅ |
| 8 | `src/main/resources/templates/pages/product-list-category-storage.html` | 変更 | ✅ |
| 9 | `src/test/java/.../service/product/ProductFilterOptionServiceTest.java` | 変更 | ✅ |
| 9 | `src/test/java/.../service/product/ProductFilterOptionServiceTasteOptionsTest.java` | **新規** | ✅ |
| 9 | `src/test/java/.../model/product/ProductCategoryFilterTest.java` | **新規** | ✅ |
| 9 | `src/test/java/.../repository/ProductRepositoryTasteParamsTest.java` | **新規** | ✅ |
