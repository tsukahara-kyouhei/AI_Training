# FEAT-001 実装計画 TODO リスト

| 項目     | 内容                                                                 |
| -------- | -------------------------------------------------------------------- |
| Issue ID | FEAT-001                                                             |
| 作成日   | 2026-04-27                                                           |
| 関連文書 | `docs/issues/FEAT-001-requirements.md` / `docs/design/FEAT-001-*.md` |

---

## 実装前の注意事項（コード調査で判明した設計書との差異）

| #   | 設計書の記述                               | 実際のコード                                                       | 対応方針                                 |
| --- | ------------------------------------------ | ------------------------------------------------------------------ | ---------------------------------------- |
| 1   | `CategoryFilterOption.displayName()`       | `CategoryFilterOption.label()`                                     | テンプレートでは `opt.label` を使う      |
| 2   | `CategoryFilterOption.id()` の型 `Integer` | `int`（プリミティブ）                                              | `contains()` の引数型に注意              |
| 3   | `RankedProductCardView.productCard()`      | `RankedProductCardView.product()`                                  | テンプレートでは `ranked.product` を使う |
| 4   | `buildSearchTasteFilter()` の実装方法      | 既存の `normalizeIntegerOptions()` + `extractOptionIds()` が使える | 設計書の `normalizeOptionIds` は不要     |

---

## フェーズ 1: バックエンド実装

### 1-1. ProductFilterOptionService — `buildSearchTasteFilter()` 追加

- [x] `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
      **追加内容**: `buildSearchTasteFilter(List<Integer> rawDeskTasteIds, List<Integer> rawChairTasteIds, List<Integer> rawStorageTasteIds, ProductFilterOptionsBundle optionsBundle)` メソッド
  - 既存の `extractOptionIds()` でテイストオプションから許容 ID を取得
  - 既存の `normalizeIntegerOptions()` でホワイトリスト検証
  - テイスト以外のフィールドはすべて `List.of()` を設定
  - 参照設計書: `docs/design/FEAT-001-service-design.md` §2

---

### 1-2. ProductService — `getTopRankedProducts()` 追加

- [x] `src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`
      **追加内容**: `getTopRankedProducts(int limit)` メソッド
  - `repository.findTopRankedProducts(limit)` への委譲のみ（1行実装）
  - 既存の `findTopRankedProducts()` （引数なし）との混同に注意
  - 参照設計書: `docs/design/FEAT-001-service-design.md` §4

---

### 1-3. ProductRepository — `buildSearchParams()` 修正

- [x] `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
      **変更内容①**: `keyword` / `keywordLike` の2パラメータ分離
  - `params.put("keywordLike", toKeywordLike(condition.keyword()))` を削除
  - `kw` 変数（trim のみ）を新設し、`params.put("keyword", kw)` と `params.put("keywordLike", ...)` に差し替え

- [x] **変更内容②**: `hasTasteSearchFilter` フラグ追加
  - `normalizedCategoryId == null` かつ任意のテイスト ID が存在する場合 `true`
  - `hasStorageFilter` の `params.put(...)` 直後に追記

- [x] **変更内容③**: `toKeywordLike()` プライベートメソッドを削除
  - 参照設計書: `docs/design/FEAT-001-repository-design.md`

---

### 1-4. ProductMapper.xml — `BaseProductWhere` 修正・`TasteSearchFilter` 追加

- [x] `src/main/resources/mappers/ProductMapper.xml`
      **変更内容①**: `BaseProductWhere` のキーワード条件を差し替え
  - `test="keywordLike != null"` → `test="keyword != null and keyword != ''"`
  - `p.variation_name ILIKE #{keywordLike}` 追加
  - `p.description ILIKE #{keywordLike}` 追加
  - 商品コード: `ILIKE #{keywordLike}` を廃止し、完全一致 `LOWER(pvk.product_code) = LOWER(#{keyword})` + 前方一致 `LIKE LOWER(#{keyword}) || '%'` に変更

- [x] **変更内容②**: `TasteSearchFilter` フラグメントを新規追加
  - デスク・チェア・収納それぞれのテイストを `category_id != 'xxx' OR EXISTS(...)` パターンで結合
  - `BaseProductWhere` 末尾に `<if test="hasTasteSearchFilter"><include refid="TasteSearchFilter"/></if>` を追加
  - 参照設計書: `docs/design/FEAT-001-sql-design.md`

---

### 1-5. CatalogController — `searchResults()` 修正

- [x] `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
      **変更内容①**: インポート追加
  - `import java.text.Normalizer;`
  - `import jp.co.skig.officeorder.model.product.RankedProductCardView;`

- [x] **変更内容②**: メソッドシグネチャにパラメータ追加
  - `@RequestParam(name = "deskTaste", required = false) List<Integer> rawDeskTasteIds`
  - `@RequestParam(name = "chairTaste", required = false) List<Integer> rawChairTasteIds`
  - `@RequestParam(name = "storageTaste", required = false) List<Integer> rawStorageTasteIds`

- [x] **変更内容③**: NFKC 正規化ロジック追加（メソッド先頭）
  - `Normalizer.normalize(keyword.strip(), Normalizer.Form.NFKC)`
  - keyword が null / 空白の場合は `normalizedKeyword = null`

- [x] **変更内容④**: 100文字バリデーション追加
  - 超過時は optionsBundle をロードしてテイスト候補をモデルにセットし、`keywordTooLong = true` を設定して即返却
  - 空の `ProductListPage(List.of(), 0, 1, size)` を使用（`ProductListPage.empty()` は未定義のため）

- [x] **変更内容⑤**: テイストフィルタ組み立て・`buildCondition()` 呼び出し変更
  - `productFilterOptionService.buildSearchTasteFilter(...)` 呼び出し
  - `buildCondition()` に `tasteFilter` を第6引数として渡す（既存11引数オーバーロード使用）

- [x] **変更内容⑥**: 0件時処理追加
  - `result.productPage().totalCount() == 0` で `productService.getTopRankedProducts(8)` 呼び出し

- [x] **変更内容⑦**: Model 属性追加
  - `suggestedProducts`、`deskTasteIds`、`chairTasteIds`、`storageTasteIds`
  - `deskTasteOptions`、`chairTasteOptions`、`storageTasteOptions`
  - 参照設計書: `docs/design/FEAT-001-controller-design.md`

---

## フェーズ 2: 画面実装

### 2-1. product-list-search-results.html — テイストフィルタ追加

- [x] `src/main/resources/templates/pages/product-list-search-results.html`
      **変更内容①**: バリデーションエラーメッセージ追加
  - 既存キーワード表示カードの直後に `th:if="${keywordTooLong}"` ブロック追加

- [x] **変更内容②**: デスクトップサイドバーフォームにテイストフィルタ追加
  - カラーフィールドの後・アクションボタンの前
  - Thymeleaf: `th:each="opt : ${deskTasteOptions}"` でチェックボックス生成
  - `th:value="${opt.id}"`, `th:text="${opt.label}"` を使用（`displayName` ではなく `label`）
  - `th:checked="${deskTasteIds != null and deskTasteIds.contains(opt.id)}"`
  - 同様にチェア・収納のテイストフィルタを追加
  - `th:if` でオプションが空の場合は非表示

---

### 2-2. product-list-search-results.html — 選択状態の引き継ぎ

- [x] **変更内容③**: 並び順フォーム（`toolbar__group` 内フォーム）に hidden input 追加
  - `th:each="id : ${deskTasteIds}"` で `name="deskTaste"` を複数出力
  - チェア・収納も同様

- [x] **変更内容④**: ページネーションリンクの `th:href` にテイストパラメータ追加
  - 前へ・次へ・ページ番号リンク（5か所）すべてに `deskTaste=${deskTasteIds},chairTaste=${chairTasteIds},storageTaste=${storageTasteIds}` を追記

---

### 2-3. product-list-search-results.html — 0件時おすすめ商品

- [x] **変更内容⑤**: 0件メッセージカードの直後におすすめ商品エリア追加
  - `th:if="${suggestedProducts != null and !#lists.isEmpty(suggestedProducts)}"`
  - `th:each="ranked : ${suggestedProducts}"`
  - フラグメント呼び出し: `productCardByView(${ranked.product})` （`productCard` ではなく `product`）
  - 参照設計書: `docs/design/FEAT-001-screen-design.md`

---

## フェーズ 3: 単体テスト実装

テストディレクトリ: `src/test/java/jp/co/skig/officeorder/`
フレームワーク: JUnit 5 + Mockito + AssertJ（`spring-boot-starter-test` 含む）

---

### 3-1. ProductFilterOptionServiceTest

- [x] `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`
      テスト対象メソッド: `buildSearchTasteFilter()`

  | テストケース                   | 確認内容                                       |
  | ------------------------------ | ---------------------------------------------- |
  | 有効な ID のみ渡す             | 対応するフィールドに正規化済み ID が設定される |
  | 許容外 ID を含む               | 許容外 ID はフィルタリングされる               |
  | `null` を渡す                  | 空リストが設定される                           |
  | テイスト以外のフィールド       | すべて空リストであること                       |
  | デスク・チェア・収納を同時指定 | 各テイストが独立して設定される                 |
  | 重複 ID を含む                 | 重複が排除される                               |

---

### 3-2. ProductServiceTest

- [x] `src/test/java/jp/co/skig/officeorder/service/product/ProductServiceTest.java`
      テスト対象メソッド: `getTopRankedProducts(int limit)`

  | テストケース                         | 確認内容                                         |
  | ------------------------------------ | ------------------------------------------------ |
  | `limit=8` を渡す                     | `repository.findTopRankedProducts(8)` が呼ばれる |
  | リポジトリが返した結果がそのまま返る | 委譲の確認                                       |

---

### 3-3. ProductRepositoryTest（ユニット）

- [x] `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTest.java`
      テスト対象: `buildSearchParams()` のパラメータ生成ロジック
      ※ `buildSearchParams()` は `private` のため、`search()` のモック経由で検証する。

  | テストケース                                   | 確認内容                                                            |
  | ---------------------------------------------- | ------------------------------------------------------------------- |
  | keyword あり                                   | `params["keyword"]` = trim 済み値、`params["keywordLike"]` = `%値%` |
  | keyword が null                                | `params["keyword"]` = `null`、`params["keywordLike"]` = `null`      |
  | keyword が空白のみ                             | `params["keyword"]` = `null`、`params["keywordLike"]` = `null`      |
  | categoryId=null + テイスト ID あり             | `params["hasTasteSearchFilter"]` = `true`                           |
  | categoryId='desk' + テイスト ID あり           | `params["hasTasteSearchFilter"]` = `false`                          |
  | categoryId=null + テイスト ID なし             | `params["hasTasteSearchFilter"]` = `false`                          |
  | 全角キーワード（正規化は Controller 済み前提） | trim のみ行われ、NFKC 変換は行わない                                |

---

### 3-4. CatalogControllerTest（NFKC 正規化・バリデーション）

- [x] `src/test/java/jp/co/skig/officeorder/web/CatalogControllerTest.java`
      テスト方針: `@ExtendWith(MockitoExtension.class)` でサービス層をモック化したユニットテスト
      テスト対象メソッド: `searchResults()`

  **正規化テスト（FR-004）**

  | テストケース | 入力 `q` | 期待値（`normalizedKeyword`） |
  | ------------ | -------- | ----------------------------- |
  | 全角英字     | `ＤＳＫ` | `DSK`                         |
  | 全角数字     | `１２３` | `123`                         |
  | 全角記号     | `！＠＃` | `!@#`                         |
  | ひらがな     | `てすと` | `てすと`（変換なし）          |
  | 前後の空白   | `desk`   | `desk`                        |
  | null         | `null`   | `null`（後続処理スキップ）    |

  **バリデーションテスト（FR-008）**

  | テストケース                      | 確認内容                                                                                 |
  | --------------------------------- | ---------------------------------------------------------------------------------------- |
  | 100文字ちょうど                   | 正常に検索処理へ進む                                                                     |
  | 101文字                           | `"pages/product-list-search-results"` を返し、`keywordTooLong=true` がモデルに設定される |
  | 101文字でもデータ検索は行われない | `productListSearchService.buildCondition()` が呼ばれないこと                             |

  **0件時テスト（FR-007）**

  | テストケース     | 確認内容                                            |
  | ---------------- | --------------------------------------------------- |
  | 検索結果 0件     | `productService.getTopRankedProducts(8)` が呼ばれる |
  | 検索結果 0件     | `suggestedProducts` がモデルに設定される            |
  | 検索結果 1件以上 | `getTopRankedProducts()` は呼ばれない               |
  | 検索結果 1件以上 | `suggestedProducts` が空リストでモデルに設定される  |

  **テイストフィルタ・Model テスト（FR-005）**

  | テストケース       | 確認内容                                                                               |
  | ------------------ | -------------------------------------------------------------------------------------- |
  | テイスト ID を渡す | `buildSearchTasteFilter()` が正しい引数で呼ばれる                                      |
  | テイスト ID を渡す | モデルに `deskTasteIds` / `chairTasteIds` / `storageTasteIds` が設定される             |
  | 常に               | モデルに `deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` が設定される |

---

## フェーズ 4: 動作確認

- [×] ローカル環境でアプリケーション起動（`./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"`）
- [×] 検索結果画面でテイストフィルタのチェックボックスが表示されること
- [×] テイストフィルタを選択して絞り込みが効くこと
- [×] ページネーションを移動してもテイスト選択が維持されること
- [×] 全角キーワードで検索してヒットすること（例: `ＤＳＫ` → `DSK` と同じ結果）
- [×] 0件の検索でおすすめ商品が表示されること
- [×] 101文字以上のキーワードでエラーメッセージが表示されること
- [×] 既存フィルタ（在庫あり・価格帯・カラー）が引き続き正常に動作すること

---
