# FEAT-001 要件・設計トレーサビリティ

## 1. 目的

要件定義書の各要求が、どの設計書・実装・テストで担保されているかを追跡できるようにする。

## 2. 追跡マトリクス

| 要件 | 画面設計 | API/Controller | Service | Repository/SQL | DB | テスト |
|---|---|---|---|---|---|---|
| 商品コード完全一致 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 商品コード前方一致 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 商品名部分一致 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| シリーズバリエーション名部分一致 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 説明文部分一致 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 英字大文字小文字同一視 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 全角/半角同一視 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| テイスト絞込 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-data-model-design.md](./FEAT-001-data-model-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 価格帯/カラー/在庫条件 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 0件時表示 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-error-design.md](./FEAT-001-error-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| ページネーション | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| ソート | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-search-design.md](./FEAT-001-search-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-test-design.md](./FEAT-001-test-design.md) |
| 性能要件 | [FEAT-001-screen-design.md](./FEAT-001-screen-design.md) | [FEAT-001-api-design.md](./FEAT-001-api-design.md) | [FEAT-001-performance-design.md](./FEAT-001-performance-design.md) | [ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | [products.sql](../../sql/schema/products.sql) | [FEAT-001-performance-test-design.md](./FEAT-001-performance-test-design.md) |

## 3. 要件と設計が担保される箇所

- 要件の「検索対象」は `ProductMapper.xml` の keyword 条件と `ProductSearchCondition` で担保される
- 要件の「境界条件」は `ProductSearchCondition` と `ProductListSearchService` / `ProductService` で担保される
- 要件の「テイスト」は `ProductCategoryFilter`, `ProductFilterOptionService`, `ProductMapper.xml` で担保される
- 要件の「0件時表示」は画面テンプレートとエラー設計で担保される
- 要件の「性能目標」は performance 設計と performance test design で担保される

## 4. 最終確定事項

- `LIKE` / `ILIKE` は PostgreSQL の実仕様に基づき、`product_name` / `description` では `ILIKE`、`product_code` では完全一致/前方一致に限定した比較を適用する
- 文字種の正規化は Java で `Normalizer.normalize(..., Form.NFKC)` と `toLowerCase(Locale.ROOT)` を使うことを確定した
- `taste_id` はカテゴリごとのマスタの主キーであり、`desk_tastes` / `chair_tastes` / `storage_tastes` は独立した値域を持つ
