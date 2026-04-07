# FEAT-001 商品検索機能強化 実装計画 TODO リスト

| 項目 | 内容 |
|------|------|
| 機能ID | FEAT-001 |
| 機能名 | 商品検索機能強化 |
| 作成日 | 2026-04-06 |
| 関連設計書 | [基本設計](../design/FEAT-001-basic-design.md) / [クラス設計](../design/FEAT-001-class-design.md) / [SQL設計](../design/FEAT-001-sql-design.md) / [画面仕様](../design/FEAT-001-screen-spec.md) |

---

## 凡例

- `[x]` 未着手
- `[x]` 完了

実装は **Phase 1（ユーティリティ・モデル）→ Phase 2（DB / Mapper）→ Phase 3（Repository / Service）→ Phase 4（Controller）→ Phase 5（テンプレート）→ Phase 6（単体テスト）** の順で進める。各 Phase は前 Phase の完了後に着手すること。

---

## Phase 1：ユーティリティ・モデル変更

依存するクラスが存在せず単独で実装・テストできる範囲。

### 1-A　`KeywordNormalizer`（新規）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/util/KeywordNormalizer.java`

- [x] **1-A-1** パッケージ `jp.co.skig.officeorder.util` を作成し `KeywordNormalizer` クラスを新規作成する
  - インスタンス化不可（`private` コンストラクタ）
  - `public static String normalize(String input)` を実装する

- [x] **1-A-2** `normalize()` の変換ロジックを実装する
  - `null` または空文字はそのまま返す
  - 全角 ASCII 数字（U+FF10〜U+FF19）→ 半角数字（U+0030〜U+0039）
  - 全角 ASCII 大文字（U+FF21〜U+FF3A）→ 半角小文字（U+0061〜U+007A）
  - 全角 ASCII 小文字（U+FF41〜U+FF5A）→ 半角小文字（U+0061〜U+007A）
  - 半角カタカナ濁音・半濁音（2文字形式: ﾞﾟ 付き）→ 対応する全角カタカナ1文字
  - 残りの半角カタカナ（U+FF66〜U+FF9F）→ 対応する全角カタカナ
  - 半角英字大文字（A-Z）→ 小文字（`String.toLowerCase(Locale.ENGLISH)`）

  > **実装ヒント：** 変換表定数 `HALF_KANA` / `FULL_KANA`（クラス設計書 §1.1 参照）を用意し、ループで文字ごとに置換する。

---

### 1-B　`ProductCategoryFilter`（変更）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java`

- [x] **1-B-1** record コンポーネント末尾に `List<String> searchTasteNames` を追加する

- [x] **1-B-2** `empty()` ファクトリメソッドの引数末尾に `List.of()` を追加する
  ```java
  return new ProductCategoryFilter(
      List.of(), List.of(), List.of(), List.of(), List.of(),
      List.of(), List.of(), List.of(), List.of(), List.of(),
      List.of()  // searchTasteNames
  );
  ```

- [x] **1-B-3** `normalize()` メソッドの末尾に `searchTasteNames` の正規化処理を追加する
  - 既存の `normalizeList(List<Integer>)` を参考に `private static List<String> normalizeStringList(List<String> values)` を追加する（null/空文字除去・distinct・不変リスト化）

- [x] **1-B-4** `isEmpty()` メソッドに `searchTasteNames.isEmpty()` の評価を追加する

---

### 1-C　`ProductFilterOptionsBundle`（変更）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java`

- [x] **1-C-1** record コンポーネント末尾に `List<String> searchTasteOptions` を追加する

- [x] **1-C-2** compact コンストラクタに `searchTasteOptions = immutableOrEmpty(searchTasteOptions);` を追加する

---

## Phase 2：Mapper インターフェース・XML（DB 層）

### 2-A　`ProductMapper`（インターフェース追加）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`

- [x] **2-A-1** 以下のメソッドを追加する
  ```java
  /**
   * 検索結果画面用のテイスト統合選択肢を取得する。
   * desk_tastes / chair_tastes / storage_tastes を display_name で UNION・重複排除・ソートして返す。
   */
  List<String> selectActiveSearchTasteOptions();
  ```

---

### 2-B　`ProductMapper.xml`（SQL 追加・変更）

**ファイル：**
`src/main/resources/mappers/ProductMapper.xml`

- [x] **2-B-1** `selectActiveSearchTasteOptions` クエリを追加する（SQL設計書 §2.1）
  ```xml
  <select id="selectActiveSearchTasteOptions" resultType="string">
      SELECT DISTINCT display_name
      FROM (
          SELECT display_name FROM desk_tastes    WHERE is_active = TRUE
          UNION ALL
          SELECT display_name FROM chair_tastes   WHERE is_active = TRUE
          UNION ALL
          SELECT display_name FROM storage_tastes WHERE is_active = TRUE
      ) t
      ORDER BY display_name
  </select>
  ```

- [x] **2-B-2** `BaseProductWhere` SQL フラグメントを更新する（SQL設計書 §3.1）
  - テキスト検索対象を `product_name` から `product_name` / `variation_name` / `description` の3フィールドに拡張する
  - 商品コードの一致方式を `ILIKE #{keywordLike}` から `productCodeMatchMode` による `<choose>` 分岐（完全一致 / 前方一致）に変更する
  - `hasSearchTasteFilter` フラグによる `<include refid="SearchTasteFilter"/>` を末尾に追加する

- [x] **2-B-3** `SearchTasteFilter` SQL フラグメントを新規追加する（SQL設計書 §3.2）
  - デスク・チェア・収納の各属性テーブルと各テイストマスタを JOIN し、`display_name IN (#{name}, ...)` で絞り込む3つの `EXISTS` を `OR` でつなぐ

---

## Phase 3：Repository / Service 変更

### 3-A　`ProductFilterOptionRepository`（メソッド追加）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`

- [x] **3-A-1** `findActiveSearchTasteOptions()` メソッドを追加する
  ```java
  public List<String> findActiveSearchTasteOptions() {
      return productMapper.selectActiveSearchTasteOptions();
  }
  ```

---

### 3-B　`ProductFilterOptionService`（変更）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

- [x] **3-B-1** `loadOptionsBundle()` の `ProductFilterOptionsBundle` コンストラクタ呼び出し末尾に以下を追加する
  ```java
  productFilterOptionRepository.findActiveSearchTasteOptions()  // ← 追加
  ```

- [x] **3-B-2** `buildSearchCategoryFilter(List<String> rawTasteNames, ProductFilterOptionsBundle optionsBundle)` を追加する
  1. `optionsBundle.searchTasteOptions()` から有効名称の `Set<String>` を構築する
  2. `rawTasteNames` から有効名称のみ抽出（null フィルタ・重複排除含む）する
  3. `ProductCategoryFilter.empty()` をベースに `searchTasteNames` のみ設定した新インスタンスを返す（他フィールドはすべて `List.of()`）

  > **実装ヒント：** 既存の `buildDeskFilter` 等の `filterToAllowedIds` パターン（`normalizeIntegerOptions`）を参考に実装する。ただし ID ではなく文字列名称でフィルタリングする点が異なる。

- [x] **3-B-3** `buildSearchCategoryFilter(List<String> rawTasteNames)` オーバーロードを追加する（内部で `loadOptionsBundle()` を呼び出すバリアント）

---

### 3-C　`ProductRepository`（検索ロジック変更）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

- [x] **3-C-1** `buildSearchParams(ProductSearchCondition condition, String productCodeMatchMode)` にシグネチャを変更する（第2引数 `productCodeMatchMode` を追加）

- [x] **3-C-2** `buildSearchParams()` 内で以下の新規パラメータをマップに追加する
  - `productCodeMatchMode` ← 引数の値をそのまま設定
  - `productCodeExact` ← `normalizedKeyword`（`keyword` 値を正規化済みとしてそのまま使用）
  - `productCodePrefix` ← `normalizedKeyword + "%"`
  - `searchTasteNames` ← `condition.categoryFilter().searchTasteNames()`
  - `hasSearchTasteFilter` ← `!searchTasteNames.isEmpty()`

- [x] **3-C-3** `search()` メソッドを2パス検索方式に更新する
  ```
  1. buildSearchParams(condition, "exact") でパラメータ構築
  2. countProducts(params) で件数取得（totalCount）
  3. totalCount == 0 かつ keyword が非空のとき:
       buildSearchParams(condition, "prefix") で再構築
       countProducts(params) で再取得
  4. totalCount == 0 → 空の ProductListPage を返す
  5. params に limit / offset を追加して selectProducts(params) を実行
  6. toCard() 変換して ProductListPage を返す
  ```

- [x] **3-C-4** `toKeywordLike(String keyword)` プライベートメソッドを削除し、`buildSearchParams()` 内でインライン化する
  - `keywordLike` パラメータは引き続き `"%" + normalizedKeyword + "%"` で設定する

---

### 3-D　`ProductListSearchService`（キーワード正規化追加）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`

- [x] **3-D-1** `buildCondition()` の11引数版（メイン実装）内でキーワードを `ProductSearchCondition` に格納する前に `KeywordNormalizer.normalize()` を適用する
  ```java
  String normalizedKeyword = KeywordNormalizer.normalize(
      keyword == null ? null : keyword.trim()
  );
  ```
  以降の処理では `normalizedKeyword` を使う。

---

## Phase 4：Controller 変更

### 4-A　`CatalogController`（変更）

**ファイル：**
`src/main/java/jp/co/skig/officeorder/web/CatalogController.java`

- [x] **4-A-1** `searchResults()` メソッドにリクエストパラメータを追加する
  ```java
  @RequestParam(name = "taste", required = false) List<String> rawTasteNames
  ```

- [x] **4-A-2** `searchResults()` 内で `buildSearchCategoryFilter()` を呼び出し `ProductCategoryFilter` を構築する
  ```java
  ProductCategoryFilter searchFilter =
      productFilterOptionService.buildSearchCategoryFilter(rawTasteNames, optionsBundle);
  ```

- [x] **4-A-3** `productListSearchService.buildCondition()` の呼び出しを11引数版に変更し、`searchFilter` を渡す
  ```java
  ProductSearchCondition condition = productListSearchService.buildCondition(
      null, keyword, inStockOnly, rawPriceBandIds, rawColorKeys,
      searchFilter,   // ← 追加
      sort, page, size, ProductSort.RECOMMENDED, optionsBundle
  );
  ```

- [x] **4-A-4** `model` へ以下の属性を追加する
  ```java
  model.addAttribute("searchTasteOptions", optionsBundle.searchTasteOptions());
  model.addAttribute("selectedTasteNames",
      result.condition().categoryFilter().searchTasteNames());
  ```

---

## Phase 5：テンプレート変更

**ファイル：**
`src/main/resources/templates/pages/product-list-search-results.html`

- [x] **5-1** **デスクトップ用絞り込みサイドバー**にテイストチェックリストを追加する（画面仕様 §4.1）
  - 追加位置：カラーフィルタ `<div class="field">` の直後・「絞り込む」ボタンの直前
  - `th:each="tasteName : ${searchTasteOptions}"` で選択肢を生成
  - `th:checked` で `selectedTasteNames.contains(tasteName)` によるチェック状態復元

- [x] **5-2** **デスクトップ用ツールバーフォーム**（並び順・件数切り替えフォーム）にテイスト用 hidden input を追加する（画面仕様 §4.2）
  - `<input type="hidden" name="taste" th:each="name : ${selectedTasteNames}" th:value="${name}">`

- [x] **5-3** **ページネーションリンク**（前へ・先頭・ページ番号・最終・次へ）全 `th:href` に `taste=${selectedTasteNames}` を追加する（画面仕様 §4.3）
  - 計5箇所を変更する（リセットリンクは変更不要）

- [x] **5-4** **モバイル用絞り込みモーダル**を本格実装へ置き換える（画面仕様 §4.4）
  - プレースホルダテキストのみの現行モーダルを、デスクトップ用と同等のフォームへ書き換える
  - 在庫フィルタ・価格帯・カラー・テイストのすべてのチェックボックスと送信ボタンを含める

---

## Phase 6：単体テスト作成

**前提:** テストディレクトリが存在しないため、まず環境を準備する。
`src/test/java/jp/co/skig/officeorder/` 配下に以下のパッケージ構成でテストクラスを作成する。
テストフレームワークは JUnit 5 / Mockito を使用する（`pom.xml` の `spring-boot-starter-test` 依存で利用可能）。

---

### 6-A　`KeywordNormalizerTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/util/KeywordNormalizerTest.java`

正規化ユーティリティのロジック検証。依存なし・シンプルなユニットテスト。

- [x] **6-A-1** `null` 入力 → `null` を返すことを確認する
- [x] **6-A-2** 空文字入力 → 空文字を返すことを確認する
- [x] **6-A-3** 全角数字 `"１２３"` → `"123"` に変換されることを確認する
- [x] **6-A-4** 全角英字大文字 `"ＡＢＣ"` → `"abc"` に変換されることを確認する
- [x] **6-A-5** 全角英字小文字 `"ａｂｃ"` → `"abc"` に変換されることを確認する
- [x] **6-A-6** 半角カタカナ（濁音なし） `"ｱｲｳ"` → `"アイウ"` に変換されることを確認する
- [x] **6-A-7** 半角カタカナ（濁音付き） `"ｶﾞｷﾞ"` → `"ガギ"` に変換されることを確認する
- [x] **6-A-8** 半角カタカナ（半濁音付き） `"ﾊﾟﾋﾟ"` → `"パピ"` に変換されることを確認する
- [x] **6-A-9** 半角英字大文字 `"DESK"` → `"desk"` に変換されることを確認する
- [x] **6-A-10** 混在入力 `"ＡＢＣ１２３ｱｲｳ"` → `"abc123アイウ"` に変換されることを確認する
- [x] **6-A-11** ひらがな・漢字は変換されないことを確認する（例：`"デスクあいう机"` → そのまま）
- [x] **6-A-12** 既に半角小文字の文字列は変換されないことを確認する

---

### 6-B　`ProductCategoryFilterTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/model/product/ProductCategoryFilterTest.java`

モデルの正規化・ファクトリメソッドの検証。

- [x] **6-B-1** `empty()` が返すインスタンスの全フィールドが空リストであることを確認する（`searchTasteNames` 含む）
- [x] **6-B-2** `normalize()` が `searchTasteNames` から `null` と空文字列を除去することを確認する
- [x] **6-B-3** `normalize()` が `searchTasteNames` の重複を除去することを確認する
- [x] **6-B-4** `normalize()` が返すリストが不変（`UnsupportedOperationException` をスローする）ことを確認する
- [x] **6-B-5** `isEmpty()` が全フィールド空リストのとき `true` を返すことを確認する（`searchTasteNames` 含む）
- [x] **6-B-6** `isEmpty()` が `searchTasteNames` のみ値を持つとき `false` を返すことを確認する

---

### 6-C　`ProductFilterOptionsBundle`（既存record のコンパクトコンストラクタ検証）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundleTest.java`

- [x] **6-C-1** `searchTasteOptions` に `null` を渡したとき空リストに変換されることを確認する
- [x] **6-C-2** `searchTasteOptions` に値を渡したとき不変リストになっていることを確認する

---

### 6-D　`ProductFilterOptionServiceTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`

`ProductFilterOptionRepository` を Mockito でモックして検証する。

- [x] **6-D-1** `loadOptionsBundle()` が `findActiveSearchTasteOptions()` の戻り値を `searchTasteOptions` に設定した `ProductFilterOptionsBundle` を返すことを確認する

- [x] **6-D-2** `buildSearchCategoryFilter(rawTasteNames, optionsBundle)` が有効な名称のみを `searchTasteNames` に設定することを確認する
  - `optionsBundle.searchTasteOptions()` = `["ベーシック", "ナチュラル", "モダン"]`
  - `rawTasteNames` = `["ベーシック", "不正値", "ナチュラル"]`
  - → `searchTasteNames` = `["ベーシック", "ナチュラル"]`

- [x] **6-D-3** `rawTasteNames` が `null` または空のとき `searchTasteNames` が空リストになることを確認する

- [x] **6-D-4** 重複した `rawTasteNames` が除去されることを確認する（例：`["ベーシック", "ベーシック"]` → `["ベーシック"]`）

---

### 6-E　`ProductRepositorySearchTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/repository/ProductRepositorySearchTest.java`

`ProductMapper` を Mockito でモックして `search()` の2パス制御を検証する。

- [x] **6-E-1** キーワードあり・完全一致で件数 > 0 のとき `countProducts` が1回だけ呼ばれ、前方一致フォールバックが実行されないことを確認する

- [x] **6-E-2** キーワードあり・完全一致で件数 = 0 のとき `countProducts` が2回呼ばれ（1回目 `"exact"` / 2回目 `"prefix"`）、2回目で件数が返ることを確認する

- [x] **6-E-3** キーワードあり・完全一致 0 件・前方一致も 0 件のとき空の `ProductListPage` が返ることを確認する

- [x] **6-E-4** キーワードなしのとき `productCodeMatchMode = "exact"` のパラメータで1回だけ呼ばれ、前方一致フォールバックが発生しないことを確認する

- [x] **6-E-5** `hasSearchTasteFilter = true` のとき `buildSearchParams` の返却マップに `searchTasteNames` が含まれることを確認する

- [x] **6-E-6** `hasSearchTasteFilter = false`（`searchTasteNames` が空リスト）のとき `buildSearchParams` の返却マップで `hasSearchTasteFilter = false` が設定されることを確認する

---

### 6-F　`KeywordNormalizationInSearchTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceNormalizationTest.java`

`ProductService` を Mockito でモックして、`buildCondition()` がキーワードを正規化してから `ProductSearchCondition` に格納することを検証する。

- [x] **6-F-1** 全角英字キーワード `"ＤＥＳＫチェア"` が `"deskチェア"` に正規化されて `condition.keyword()` に格納されることを確認する

- [x] **6-F-2** 半角カタカナキーワード `"ﾃﾞｽｸ"` が `"デスク"` に正規化されることを確認する

- [x] **6-F-3** `null` キーワードが `null` のまま格納されることを確認する（正規化でエラーが発生しないことも確認する）

---

### 6-G　`CatalogControllerSearchTest`（新規）

**ファイル：**
`src/test/java/jp/co/skig/officeorder/web/CatalogControllerSearchTest.java`

`@WebMvcTest(CatalogController.class)` + 関連 Service を `@MockBean` で置換して MVC レイヤーを検証する。

- [x] **6-G-1** `GET /products/search?q=desk` のリクエストが HTTP 200 を返すことを確認する

- [x] **6-G-2** `taste` リクエストパラメータ（`?taste=ベーシック&taste=モダン`）が Model の `selectedTasteNames` に正しくバインドされることを確認する

- [x] **6-G-3** `taste` パラメータが未指定のとき `selectedTasteNames` が空リストになることを確認する

- [x] **6-G-4** Model に `searchTasteOptions` 属性が存在すること（`null` でないこと）を確認する

- [x] **6-G-5** 不正な `taste` 値（`optionsBundle.searchTasteOptions()` に含まれない名称）がフィルタリングされ、`selectedTasteNames` に反映されないことを確認する

---

## 実装チェックリスト（完了確認）

実装完了後、以下をすべて満たしていることを確認する。

- [x] `KeywordNormalizer.normalize("ＤＥＳＫｲｽ")` → `"deskイス"` を返す
- [x] `/products/search?q=ＤＥＳＫｱｲｳ` へアクセスしたとき、正規化後キーワード `"deskアイウ"` でDB検索が実行される
- [x] 商品コードが完全一致するキーワードで検索したとき、前方一致フォールバックは実行されない
- [x] `/products/search?taste=ベーシック&taste=ナチュラル` でアクセスしたとき、3カテゴリのテイスト属性テーブルを OR 横断した絞り込みが実行される
- [x] 検索結果画面のテイストチェックボックスが `searchTasteOptions` 一覧から生成される
- [x] テイストを選択して絞り込み後にページ遷移しても、選択状態が維持される（hidden input およびページネーション URL にパラメータが含まれる）
- [x] モバイル用モーダルからもテイスト絞り込みが操作できる
- [x] 既存の `q`, `inStockOnly`, `priceBand`, `color`, `sort`, `page`, `size` パラメータの挙動が変わらない
- [x] 全単体テストがグリーンになる
