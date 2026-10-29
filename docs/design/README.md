# FEAT-001 設計書一覧

本ディレクトリは、要件定義書 [../issues/FEAT-001-requirements.md](../issues/FEAT-001-requirements.md) を実装するための設計を集約した場所である。

## 1. 設計書構成

- [FEAT-001-search-design.md](./FEAT-001-search-design.md)
  - 検索全体の処理フロー、条件生成、SQL 条件構成、AND/OR の設計
- [FEAT-001-screen-design.md](./FEAT-001-screen-design.md)
  - ヘッダ検索、検索結果画面、フィルタ UI、入力値・ページングの設計
- [FEAT-001-data-model-design.md](./FEAT-001-data-model-design.md)
  - 検索条件モデル、カテゴリ絞込モデル、DB テーブルとカラムの対応
- [FEAT-001-performance-design.md](./FEAT-001-performance-design.md)
  - 性能要件、SQL/DB 設計、負荷観点の設計
- [FEAT-001-performance-test-design.md](./FEAT-001-performance-test-design.md)
  - 性能計測方法と観測基準のテスト設計
- [FEAT-001-api-design.md](./FEAT-001-api-design.md)
  - Controller / Endpoint / Request / Response / Error の設計
- [FEAT-001-error-design.md](./FEAT-001-error-design.md)
  - 不正パラメータ・0件・DB エラーなどの例外設計
- [FEAT-001-test-design.md](./FEAT-001-test-design.md)
  - 正常系・異常系・境界値・性能・セキュリティのテスト設計
- [FEAT-001-requirement-design-traceability.md](./FEAT-001-requirement-design-traceability.md)
  - 要件と設計、DB、SQL、画面、テストのトレーサビリティ

## 2. 参照実装

設計は現行コードと整合させている。主な確認先は以下。

- [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java](../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java](../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java)
- [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- [src/main/resources/mappers/ProductMapper.xml](../src/main/resources/mappers/ProductMapper.xml)
- [src/main/resources/templates/pages/product-list-search-results.html](../src/main/resources/templates/pages/product-list-search-results.html)
- [sql/schema/products.sql](../sql/schema/products.sql)

## 3. 設計方針

- 要件定義書の文言を変更しない。
- 既存実装で確認できた事実と今回設計を分離する。
- 実装可能な最終仕様として確定できた事項はそのまま設計に反映する。
- 実装者が設計書だけで判断できる粒度まで記述する。
- SQL の条件構造、入力値変換、エラー挙動、UI パラメータを明確化する。
- 重要: 現行の `CatalogController.searchResults()` と `ProductMapper.xml` は `keywordLike` ベースの contains 検索を実際に持っており、`product_code` の exact/prefix は実装済みではない。設計書はこの実体を前提に、対象機能の設計差分を記述する。

## 4. 重要な区分

- EXISTING: 実装・SQL・テーブル定義から確認できたもの
- FEAT-001 DESIGN: 要件を達成するために追加で確定した仕様
- UNRESOLVED: なし。すべて最終設計に落とし込んだ

## 5. 最終確定した実装基準

- 商品コード検索は「完全一致または前方一致」のみとする。中間一致・後方一致は許可しない。
- `prefix` は `P0001` を入力したとき `P0001-C01` / `P0001-C02` をヒットさせ、`P00010-C01` はヒットさせない。これは `P0001-` を境界とする前方一致である。
- `P0001-C` は `P0001-C%` を prefix として扱い、`P00010` や `P0001XYZ` は prefix 対象外とする。
- キーワードの正規化は Java で `trim()` + `Normalizer.normalize(..., Form.NFKC)` + `toLowerCase(Locale.ROOT)` を実施し、SQL 側では PostgreSQL の `ILIKE` による比較と `%` / `_` / `\` のエスケープを用いる。
- `taste_id` はカテゴリごとのマスタの主キーであり、`desk_tastes` / `chair_tastes` / `storage_tastes` はそれぞれ別テーブルの識別子である。数字が同じでも意味はカテゴリごとに異なる。
- `/products/search` は `q`, `inStockOnly`, `priceBand`, `color`, `sort`, `page`, `size` を持ち、`deskTaste` / `chairTaste` / `storageTaste` はカテゴリ別 endpoint のみ許可する。
- `normalizedProductName` / `normalizedVariationName` / `normalizedDescription` は DB カラムではなく、比較用の論理値である。
- `ProductMapper.xml` では文字列検索の OR 条件とカテゴリ属性フィルタの AND 条件をそのまま利用し、テイスト条件は `EXISTS (...) AND taste_id IN (...)` で表す。
- 画面入力の不正値は候補マスタに対して正規化し、マスタ外の ID / key は無視する。
- すべての仕様は設計として確定済みであり、追加判断を要する事項は残さない。性能試験は実測のみを対象とする。
