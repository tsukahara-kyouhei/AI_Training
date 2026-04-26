# FEAT-001 実装TODO — 商品検索機能強化

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| 作成日 | 2026-03-15 |
| ステータス | 実装完了（動作確認待ち） |
| 関連要件 | [FEAT-001-requirements.md](FEAT-001-requirements.md) |
| 関連設計 | [基本設計](../design/FEAT-001-basic-design.md) / [詳細設計](../design/FEAT-001-detail-design.md) / [画面設計](../design/FEAT-001-screen-design.md) |

---

## 実装の進め方

下記のTODOを順番に実施する。  
各タスクに「ファイルパス」「作業内容」「テスト内容」を記載する。  
完了したタスクは `[x]` にチェックを入れること。

---

## フェーズ1: DBレベルの正規化関数追加

### TODO-01: PostgreSQL正規化関数 `normalize_search_text` の作成

- [ ] **ファイル新規作成:** `sql/schema/search-functions.sql`
- [ ] **作業内容:**
  - `IMMUTABLE` / `STRICT` / `RETURNS TEXT` の PostgreSQL 関数 `normalize_search_text(input TEXT)` を定義する。
  - 変換順序:
    1. 全角数字（`０`〜`９`）→ 半角数字（`0`〜`9`）
    2. 全角英大文字（`Ａ`〜`Ｚ`）→ 半角英小文字（`a`〜`z`）
    3. 全角英小文字（`ａ`〜`ｚ`）→ 半角英小文字（`a`〜`z`）
    4. 全角カタカナ（`ア`〜`ン`、濁音・半濁音を含む） → 半角カタカナ（`ｱ`〜`ﾝ`）
       - 濁音付き（例: `ガ` → `ｶﾞ`）は2文字に分解する
       - 半濁音付き（例: `パ` → `ﾊﾟ`）は2文字に分解する
       - 小文字（例: `ァ` → `ｧ`）も対応する
    5. 英字を小文字化（`LOWER()`）
  - 実装方法: PostgreSQLの `regexp_replace()` を複数回ネスト、または `TRANSLATE()` で直接置換する。  
    全角カタカナ→半角カタカナは文字テーブルが大きいため `TRANSLATE()` に変換元・変換先の全文字列を記述する方式を採用する。
- [ ] **確認:** psql または `docker-compose exec postgres psql` で関数が動作することを確認する。
  ```sql
  SELECT normalize_search_text('Ａｂｃ１２３ナチュラル');
  -- 期待値: 'abc123ﾅﾁｭﾗﾙ'
  ```
- [ ] **ローカル初期化スクリプトへの反映:** `scripts/init-local-postgres.ps1` に `search-functions.sql` を実行する処理を追加する。

---

## フェーズ2: Java ユーティリティクラスの新規作成

### TODO-02: `SearchKeywordNormalizer` クラスの新規作成

- [ ] **ファイル新規作成:** `src/main/java/jp/co/skig/officeorder/util/SearchKeywordNormalizer.java`
- [ ] **作業内容:**
  - `final class`（インスタンス化不可）
  - `public static String normalize(String keyword)` メソッドを実装する。
  - `null` / 空文字 → `null` を返す。
  - 変換順序（TODO-01と同じロジックをJavaで実装）:
    1. 全角数字 → 半角数字（コードポイントの差分で変換: `c - 0xFF10 + '0'`）
    2. 全角英大文字 → 半角英小文字（`c - 0xFF21 + 'a'`）
    3. 全角英小文字 → 半角英小文字（`c - 0xFF41 + 'a'`）
    4. 全角カタカナ → 半角カタカナ（マッピングテーブルを `Map<Character, String>` で定義し、濁音・半濁音は2文字に分解）
    5. `String#toLowerCase(Locale.ROOT)` で英字を小文字化
  - 実装例の参考パターン: `StringBuilder` に1文字ずつ変換して append する方式。
- [ ] **単体テスト作成:** `src/test/java/jp/co/skig/officeorder/util/SearchKeywordNormalizerTest.java`
  - `null` → `null`
  - 空文字 → `null`
  - 全角数字のみ (`０１２３`) → `0123`
  - 全角英大文字のみ (`ＡＢＣ`) → `abc`
  - 全角英小文字のみ (`ａｂｃ`) → `abc`
  - 半角英大文字のみ (`ABC`) → `abc`
  - 全角カタカナ清音 (`ナチュラル`) → `ﾅﾁｭﾗﾙ`
  - 全角カタカナ濁音 (`ガギグゲゴ`) → `ｶﾞｷﾞｸﾞｹﾞｺﾞ`
  - 全角カタカナ半濁音 (`パピプペポ`) → `ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ`
  - 商品コード形式の全角入力 (`Ｐ０００１-Ｃ０１`) → `p0001-c01`
  - ひらがな混在 (`なちゅらる`) → `なちゅらる`（変換されない）
  - 混合パターン (`ＡＢＣ１２３ナチュラル`) → `abc123ﾅﾁｭﾗﾙ`

---

## フェーズ3: モデル（Record）の変更

### TODO-03: `ProductSearchCondition` に `tasteNames` フィールドを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`
- [ ] **作業内容:**
  - `List<String> tasteNames` フィールドをコンストラクタパラメータに追加する。
  - 追加位置は `colorIds` の後、`categoryFilter` の前とする。
- [ ] **ビルドエラーの修正:**
  - `ProductSearchCondition` を `new` しているすべての箇所に `tasteNames` 引数を追加する（`List.of()` を渡す）。
  - 修正箇所の目安: `ProductService.java`・`ProductRepository.java`（`findNewestProducts`・`findRecommendedProducts` 内）・`ProductListSearchService.java`（`searchWithPageCorrection` 内）。
- [ ] **確認:** コンパイルエラーがないことを確認する。

---

### TODO-04: `ProductFilterOptionsBundle` に `searchTasteOptions` フィールドを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java`
- [ ] **作業内容:**
  - `List<String> searchTasteOptions` フィールドを末尾に追加する。
  - 値: 検索結果画面で表示するテイスト名のリスト（重複排除済みの5種）。
- [ ] **ビルドエラーの修正:**
  - `new ProductFilterOptionsBundle(...)` しているすべての箇所（`ProductFilterOptionService.loadOptionsBundle()` 内）に引数を追加する。
  - この時点では暫定的に `List.of()` を渡してコンパイルを通す。最終的な値は TODO-07 で差し込む。

---

## フェーズ4: Repository層の変更

### TODO-05: `ProductFilterOptionRepository` にテイスト名一括取得メソッドを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`
- [ ] **作業内容:**
  - 以下のメソッドを追加する（既存の `findActive*Options()` パターンと同じ形式で実装）:
    ```java
    public List<String> findAllTasteDisplayNames()
    ```
  - 処理内容:
    1. `productMapper.selectAllDeskTasteNames()` で `desk_tastes.display_name` を全件取得
    2. `productMapper.selectAllChairTasteNames()` で `chair_tastes.display_name` を全件取得
    3. `productMapper.selectAllStorageTasteNames()` で `storage_tastes.display_name` を全件取得
    4. 3リストをまとめ、`LinkedHashSet` で重複排除（最初の出現順を維持）して `List` に変換して返す

### TODO-06: `ProductMapper.java` にテイスト名取得メソッドを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`
- [ ] **作業内容:**
  - 既存のMapperインターフェースに以下を追加する:
    ```java
    List<String> selectAllDeskTasteNames();
    List<String> selectAllChairTasteNames();
    List<String> selectAllStorageTasteNames();
    ```

### TODO-07（ここで後置き）: `ProductMapper.xml` にテイスト名取得SQLを追加（後述のTODO-10で実施）

---

### TODO-08: `ProductRepository.buildSearchParams()` にテイスト絞込パラメータを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
- [ ] **作業内容:** `buildSearchParams()` メソッドに以下を追加する:
  ```java
  // キーワード（完全一致/前方一致）用パラメータの追加
  String keyword = condition.keyword();
  boolean productCodeExactMatch = keyword != null && keyword.length() >= 9;
  String keywordPrefix = (keyword != null && !keyword.isBlank()) ? keyword.trim() + "%" : null;

  params.put("keyword", keyword);
  params.put("productCodeExactMatch", productCodeExactMatch);
  params.put("keywordPrefix", keywordPrefix);
  // keywordLike は既存のまま維持（product_name / variation_name / description の部分一致に使用）

  // テイスト絞込
  List<String> tasteNames = condition.tasteNames() == null ? List.of() : condition.tasteNames();
  params.put("tasteNames", tasteNames);
  ```
- [ ] **注意:** 既存の `keywordLike` パラメータは残す。`keyword`, `keywordPrefix`, `productCodeExactMatch` を追加する。

---

## フェーズ5: Service層の変更

### TODO-09: `ProductFilterOptionService` にテイスト選択肢取得メソッドを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
- [ ] **作業内容:**
  - 以下のメソッドを追加する:
    ```java
    public List<String> loadSearchTasteOptions()
    ```
  - 処理: `productFilterOptionRepository.findAllTasteDisplayNames()` を呼び出してそのまま返す。
  - `loadOptionsBundle()` 内で `searchTasteOptions` を `loadSearchTasteOptions()` の戻り値で埋める（TODO-04で`List.of()`を入れていた箇所を差し替える）。

### TODO-10: `ProductListSearchService.buildCondition()` にキーワード正規化・テイスト引数を追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
- [ ] **作業内容（カテゴリフィルタなし版の `buildCondition()` の修正）:**
  - シグネチャに `List<String> rawTasteNames` を追加する。
  - 処理内に以下を追加する:
    1. `String normalizedKeyword = SearchKeywordNormalizer.normalize(keyword);`  
       ※ `null` が返った場合は `null` のまま使用する（既存の「キーワードなし = 全件」挙動を維持）。
    2. `List<String> tasteNames = rawTasteNames == null ? List.of() : rawTasteNames.stream().distinct().toList();`
  - `ProductService.buildCondition()` の呼び出しに `normalizedKeyword` と `tasteNames` を渡す。
- [ ] **カテゴリフィルタあり版の `buildCondition()` も同様に修正する（カテゴリページはtesteNames=空で固定）。**
- [ ] **`searchWithPageCorrection()` 内の `new ProductSearchCondition(...)` に `condition.tasteNames()` を渡す（TODO-03で追加した引数の補完）。**

---

## フェーズ6: ProductService の変更

### TODO-11: `ProductService.buildCondition()` に `tasteNames` 引数を追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`
- [ ] **作業内容:**
  - `buildCondition()` の全オーバーロードに `List<String> tasteNames` パラメータを追加する。
  - `new ProductSearchCondition(...)` の生成時に `tasteNames` を渡す。
  - `buildNewArrivalCondition()` では `tasteNames = List.of()` 固定で渡す。

---

## フェーズ7: MyBatisマッパーの変更

### TODO-12: `ProductMapper.xml` のキーワード検索 SQL を変更

- [ ] **ファイル:** `src/main/resources/mappers/ProductMapper.xml`
- [ ] **作業内容（`BaseProductWhere` フラグメント内のキーワード条件を変更）:**

  **変更前:**
  ```xml
  <if test="keywordLike != null">
    AND (
      p.product_name ILIKE #{keywordLike}
      OR EXISTS (
        SELECT 1 FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND pvk.product_code ILIKE #{keywordLike}
      )
    )
  </if>
  ```

  **変更後:**
  ```xml
  <if test="keywordLike != null">
    AND (
      normalize_search_text(p.product_name)      LIKE LOWER(#{keywordLike})
      OR normalize_search_text(p.variation_name) LIKE LOWER(#{keywordLike})
      OR normalize_search_text(p.description)    LIKE LOWER(#{keywordLike})
      OR EXISTS (
        SELECT 1 FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND (
            <choose>
              <when test="productCodeExactMatch == true">
                normalize_search_text(pvk.product_code) = LOWER(#{keyword})
              </when>
              <otherwise>
                normalize_search_text(pvk.product_code) LIKE LOWER(#{keywordPrefix})
              </otherwise>
            </choose>
          )
      )
    )
  </if>
  ```

  - `ILIKE` → `LIKE` + `normalize_search_text()` 関数 + `LOWER()` で大文字小文字・全角半角を吸収する。
  - `variation_name` / `description` を `OR` で追加する。
  - 商品コードに `productCodeExactMatch` による完全一致 / 前方一致の分岐を追加する。

### TODO-13: `ProductMapper.xml` にテイスト絞込フラグメント `SearchTasteFilter` を追加

- [ ] **ファイル:** `src/main/resources/mappers/ProductMapper.xml`
- [ ] **作業内容:** 新規 `<sql id="SearchTasteFilter">` フラグメントを追加する:
  ```xml
  <sql id="SearchTasteFilter">
    <if test="tasteNames != null and !tasteNames.isEmpty()">
      AND (
        EXISTS (
          SELECT 1 FROM product_desk_attributes da
          JOIN desk_tastes dt ON dt.desk_taste_id = da.taste_id
          WHERE da.product_id = p.product_id
            AND dt.display_name IN
            <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
              #{name}
            </foreach>
        )
        OR EXISTS (
          SELECT 1 FROM product_chair_attributes ca
          JOIN chair_tastes ct ON ct.chair_taste_id = ca.taste_id
          WHERE ca.product_id = p.product_id
            AND ct.display_name IN
            <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
              #{name}
            </foreach>
        )
        OR EXISTS (
          SELECT 1 FROM product_storage_attributes sa
          JOIN storage_tastes st ON st.storage_taste_id = sa.taste_id
          WHERE sa.product_id = p.product_id
            AND st.display_name IN
            <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
              #{name}
            </foreach>
        )
      )
    </if>
  </sql>
  ```
- [ ] **`BaseProductWhere` フラグメントの末尾（または `countProducts` / `selectProducts` の WHERE 末尾）に `<include refid="SearchTasteFilter"/>` を追加する。**

### TODO-14: `ProductMapper.xml` にテイスト名取得SQL を追加

- [ ] **ファイル:** `src/main/resources/mappers/ProductMapper.xml`
- [ ] **作業内容:** 以下の3クエリを追加する:
  ```xml
  <select id="selectAllDeskTasteNames" resultType="string">
    SELECT display_name FROM desk_tastes ORDER BY desk_taste_id
  </select>
  <select id="selectAllChairTasteNames" resultType="string">
    SELECT display_name FROM chair_tastes ORDER BY chair_taste_id
  </select>
  <select id="selectAllStorageTasteNames" resultType="string">
    SELECT display_name FROM storage_tastes ORDER BY storage_taste_id
  </select>
  ```

---

## フェーズ8: Controller の変更

### TODO-15: `CatalogController.searchResults()` にテイストパラメータを追加

- [ ] **ファイル:** `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
- [ ] **作業内容:**
  1. メソッドパラメータに `@RequestParam(name = "taste", required = false) List<String> rawTasteNames` を追加する。
  2. `productListSearchService.buildCondition()` の呼び出しに `rawTasteNames` を追加する。
  3. `productFilterOptionService.loadOptionsBundle()` の結果から `searchTasteOptions` を取得してモデルへ追加する:
     ```java
     model.addAttribute("searchTasteOptions", optionsBundle.searchTasteOptions());
     ```
  4. 選択中のテイスト名をモデルへ追加する:
     ```java
     model.addAttribute("selectedTasteNames",
         result.condition().tasteNames() == null ? List.of() : result.condition().tasteNames());
     ```

---

## フェーズ9: テンプレートの変更

### TODO-16: `product-list-search-results.html` にテイスト絞込UIを追加

- [ ] **ファイル:** `src/main/resources/templates/pages/product-list-search-results.html`
- [ ] **作業内容（サイドバーの絞込フォームのカラー絞込セクションの直後）:**
  ```html
  <!-- テイスト絞込 -->
  <div class="filter-section">
    <h3 class="filter-title">テイスト</h3>
    <ul class="filter-list">
      <li th:each="tasteName : ${searchTasteOptions}">
        <label class="filter-checkbox-label">
          <input type="checkbox"
                 name="taste"
                 th:value="${tasteName}"
                 th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
          <span th:text="${tasteName}">テイスト名</span>
        </label>
      </li>
    </ul>
  </div>
  ```
- [ ] **モバイル用モーダル内にも同じ構造のセクションを追加する（モバイル用の対応箇所）。**
- [ ] **スタイル:** 既存の価格帯・カラー絞込と同じCSSクラス（`filter-section` / `filter-title` / `filter-list` / `filter-checkbox-label`）を使用するため、新規CSSは不要。

---

## フェーズ10: 動作確認・結合確認

### TODO-17: ローカル環境での動作確認

- [ ] **DB関数の確認:** Docker上のPostgreSQLに接続し、`normalize_search_text()` が正しく動作することを確認する。
- [ ] **キーワード正規化の確認:**
  - 全角数字で商品コードを検索 → ヒットする
  - 全角カタカナで商品名を検索 → ヒットする
  - 英大文字で検索 → 大文字小文字を区別せずヒットする
- [ ] **完全一致/前方一致の確認:**
  - 9文字以上の商品コード（例: `P0001-C01`）で検索 → 完全一致でヒットする
  - 9文字未満の入力（例: `P0001`）で検索 → 前方一致でヒットする（`P0001-C01`, `P0001-C02` などがヒット）
- [ ] **variation_name / description の確認:**
  - `variation_name` に含まれるキーワードで検索 → ヒットする
  - `description` に含まれるキーワードで検索 → ヒットする
- [ ] **テイスト絞込の確認:**
  - 検索結果画面にテイスト絞込が5種表示される
  - テイスト1件選択で絞り込まれる
  - テイスト複数選択でOR条件で絞り込まれる
  - テイスト未選択で全件表示される
  - 選択中のテイストにチェックが入った状態でページが表示される

---

## 実装チェックリスト

| # | TODO | フェーズ | ファイル | 完了 |
|---|------|---------|---------|------|
| 01 | `normalize_search_text` DB関数作成 | 1 | `sql/schema/search-functions.sql` (新規) | [x] |
| 02 | `SearchKeywordNormalizer` + テスト | 2 | `util/SearchKeywordNormalizer.java` (新規) | [x] |
| 03 | `ProductSearchCondition` に `tasteNames` 追加 | 3 | `model/product/ProductSearchCondition.java` | [x] |
| 04 | `ProductFilterOptionsBundle` に `searchTasteOptions` 追加 | 3 | `model/product/ProductFilterOptionsBundle.java` | [x] |
| 05 | `ProductFilterOptionRepository` にテイスト取得メソッド追加 | 4 | `repository/ProductFilterOptionRepository.java` | [x] |
| 06 | `ProductMapper.java` にテイスト取得メソッド追加 | 4 | `mapper/ProductMapper.java` | [x] |
| 07 | `ProductRepository.buildSearchParams()` にパラメータ追加 | 4 | `repository/ProductRepository.java` | [x] |
| 08 | `ProductFilterOptionService` にテイスト選択肢メソッド追加 | 5 | `service/product/ProductFilterOptionService.java` | [x] |
| 09 | `ProductListSearchService.buildCondition()` を変更 | 5 | `service/product/ProductListSearchService.java` | [x] |
| 10 | `ProductService.buildCondition()` を変更 | 6 | `service/product/ProductService.java` | [x] |
| 11 | `ProductMapper.xml` キーワードSQL変更 | 7 | `resources/mappers/ProductMapper.xml` | [x] |
| 12 | `ProductMapper.xml` `SearchTasteFilter` フラグメント追加 | 7 | `resources/mappers/ProductMapper.xml` | [x] |
| 13 | `ProductMapper.xml` テイスト名取得SQL追加 | 7 | `resources/mappers/ProductMapper.xml` | [x] |
| 14 | `CatalogController.searchResults()` を変更 | 8 | `web/CatalogController.java` | [x] |
| 15 | `product-list-search-results.html` にテイスト絞込UI追加 | 9 | `templates/pages/product-list-search-results.html` | [x] |
| 16 | ローカル動作確認 | 10 | — | [ ] |

---

## 単体テスト一覧

| テストクラス | テストクラスファイル | テスト対象 | テストケース数目安 |
|-----------|---------------|---------|--------------|
| `SearchKeywordNormalizerTest` | `util/SearchKeywordNormalizerTest.java` | `SearchKeywordNormalizer.normalize()` | 12件以上（TODO-02参照） |

> 既存テストクラスが存在しないため、`SearchKeywordNormalizerTest` が本課題における単体テストの主体となる。  
> Service層・Repository層のテストは既存プロジェクトにテストが存在しないため、本課題では対象外とする。

---

## シードデータ追加・変更時の注意事項

> **BUG-002（2026/4/18 対応）を踏まえた規則。詳細は [FEAT-001-troubleList.md](FEAT-001-troubleList.md) を参照。**

シードデータ（`sql/seed/test-data/` 配下）に注文レコードを追加・変更する際は、以下の規則を必ず守ること。

### 注文番号のフォーマット規則

- `orders` テーブルのシードデータにおける `order_number` の日付部には **固定値 `20000101`** を使用すること。
- `CURRENT_DATE` や `NOW()` など実行時の日付に依存する値は **使用禁止**。

```sql
-- ✅ 正しい例
'ORD20000101-' || LPAD(n::TEXT, 6, '0')

-- ❌ 禁止（初期構築当日に実データと衝突して UNIQUE 制約違反が発生する）
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(n::TEXT, 6, '0')
```

### 理由

アプリケーションの採番ロジック（`OrderMapper.xml` の `nextOrderSequence`）は `order_number_counters` テーブルの連番を 1 から開始する。  
シードデータが `CURRENT_DATE` で注文番号を生成していると、初期構築当日にアプリが採番する番号（例: `ORD20260310-000001`）とシードデータの番号が衝突し、`orders.order_number` の UNIQUE 制約違反でシステムエラーになる。  
固定過去日付 `20000101` を使うことで、このリスクを根本的に排除できる。
