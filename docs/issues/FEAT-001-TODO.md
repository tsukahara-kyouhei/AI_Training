# FEAT-001 商品検索機能強化 実装TODO

## 1. 文書情報

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| タイトル | 商品検索機能強化 実装TODO |
| 作成日 | 2026-04-02 |
| 参照要件 | ../issues/FEAT-001-requirements.md |
| 参照設計 | ../design/FEAT-001-design-overview.md |

---

## 2. 実装方針

- 既存の Controller → Service → Repository → Mapper XML → Thymeleaf の責務分離を維持する。
- `ProductSearchCondition` は変更せず、検索結果画面のテイスト条件は `ProductCategoryFilter` を再利用する。
- 検索キーワードの正規化・分割・前方一致用パラメータ生成は `ProductRepository` に集約する。
- テイスト候補の統合とテイスト名→カテゴリ別 taste_id 変換は `ProductFilterOptionService` で実装する。
- 単体テストは新規作成前提で TODO に含める。現状 `src/test/java` は未整備のため、必要な最小構成から作成する。

---

## 3. 実装TODO

### 3.1 事前確認

- [ ] `CatalogController`、`ProductListSearchService`、`ProductFilterOptionService`、`ProductRepository`、`ProductMapper.xml` の現行実装を再確認する
- [ ] `product-list-search-results.html` の hidden パラメータ引継ぎ箇所を一覧化する
- [ ] `ProductMapper.xml` の既存 `BaseProductWhere` とカテゴリ別絞込 SQL の影響範囲を確認する

### 3.2 Controller 実装

対象:
- `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`

TODO:
- [ ] `searchResults` に `@RequestParam(name = "taste", required = false) List<String> rawTasteNames` を追加する
- [ ] `ProductFilterOptionService.buildSearchTasteOptions(...)` を呼び出して `tasteOptions` を Model に設定する
- [ ] `ProductFilterOptionService.buildSearchTasteFilter(...)` を呼び出して `ProductCategoryFilter` を組み立てる
- [ ] `ProductListSearchService.buildCondition(..., ProductCategoryFilter, ...)` を使うように変更する
- [ ] `selectedTasteNames` を Model に設定する
- [ ] `keyword` 表示ロジックが既存どおり維持されることを確認する

完了条件:
- [ ] `/products/search` が `taste` パラメータを受け取れる
- [ ] 画面描画に必要な `tasteOptions` / `selectedTasteNames` が Model に設定される

### 3.3 ProductFilterOptionService 実装

対象:
- `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

TODO:
- [ ] 検索結果画面向けテイスト候補生成メソッド `buildSearchTasteOptions(ProductFilterOptionsBundle optionsBundle)` を追加する
- [ ] リクエスト値正規化メソッド `normalizeSearchTasteNames(...)` を追加する
- [ ] テイスト名→カテゴリ別 taste_id 変換メソッド `resolveTasteIds(...)` を追加する
- [ ] `buildSearchTasteFilter(List<String>, ProductFilterOptionsBundle)` を追加する
- [ ] `deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` から重複排除済み候補を生成する
- [ ] 不正なテイスト名は無視する実装にする
- [ ] `ProductCategoryFilter` にはテイスト項目のみ設定し、他項目は空配列で返す

完了条件:
- [ ] 同名テイストが重複せず `tasteOptions` として返る
- [ ] 入力された `taste` 文字列からカテゴリ別 taste_id 一覧が得られる

### 3.4 ProductListSearchService 実装

対象:
- `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`

TODO:
- [ ] 検索結果画面から `ProductCategoryFilter.empty()` ではなく検索用テイストフィルタを渡す前提で呼び出し元が使えることを確認する
- [ ] `searchWithPageCorrection` が追加仕様の影響を受けないことを確認する
- [ ] 必要であれば JavaDoc を更新する

完了条件:
- [ ] 既存オーバーロードで検索結果画面のテイスト絞込が通る

### 3.5 ProductRepository 実装

対象:
- `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

TODO:
- [ ] `toKeywordLike` の利用箇所を整理し、`keywordWords` / `keywordPrefixWords` 方式へ移行する
- [ ] `normalizeAndSplitKeywordWords(String keyword)` を追加する
- [ ] `normalizeKeywordToken(String token)` を追加する
- [ ] `toKeywordPrefixWords(List<String>)` を追加する
- [ ] 全角スペース→半角スペース変換を実装する
- [ ] 全角数字→半角数字変換を実装する
- [ ] 全角英字→半角英字変換を実装する
- [ ] 全角カタカナ→半角カタカナ変換を実装する
- [ ] 全角記号（少なくとも全角ハイフン）→半角記号変換を実装する
- [ ] 空白のみ入力時は空リストを返すようにする
- [ ] 最大 5 ワード制限を実装する
- [ ] 重複ワード除外を実装する
- [ ] `buildSearchParams` で `keywordWords` と `keywordPrefixWords` を `params` に設定する
- [ ] `hasSearchTasteFilter` を追加する
- [ ] `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` はカテゴリ別画面用として既存動作を維持する
- [ ] `keywordLike` を参照しなくなるため、不要であれば削除する

完了条件:
- [ ] SQL に必要な検索用パラメータがすべて `params` に設定される
- [ ] カテゴリ別一覧の既存挙動に影響しない

### 3.6 MyBatis Mapper / SQL 実装

対象:
- `src/main/resources/mappers/ProductMapper.xml`
- `src/main/java/jp/co/skig/officeorder/mapper/ProductMapper.java`（必要時のみ）

TODO:
- [ ] `BaseProductWhere` の `keywordLike` 条件を `keywordWords` の `<foreach>` ベースへ変更する
- [ ] 商品名・バリエーション名・説明文の部分一致条件を追加する
- [ ] 商品コードの完全一致条件を追加する
- [ ] 商品コードの前方一致条件を `keywordPrefixWords[index]` で実装する
- [ ] `SearchTasteFilter` SQL フラグメントを追加する
- [ ] `hasSearchTasteFilter` による条件分岐を追加する
- [ ] `deskTasteIds` / `chairTasteIds` / `storageTasteIds` が空のとき `IN ()` を生成しないようにする
- [ ] `countProducts` / `selectProducts` が新条件を参照することを確認する
- [ ] 既存カテゴリ別一覧の `DeskAttributeFilter` / `ChairAttributeFilter` / `StorageAttributeFilter` に影響がないことを確認する

完了条件:
- [ ] 複数ワード AND 検索が SQL 上で表現される
- [ ] カテゴリ横断テイスト絞込が SQL 上で表現される

### 3.7 Thymeleaf テンプレート実装

対象:
- `src/main/resources/templates/pages/product-list-search-results.html`

TODO:
- [ ] デスクトップ用サイドバーにテイストチェックボックス群を追加する
- [ ] `tasteOptions` と `selectedTasteNames` を使ってチェック状態を反映する
- [ ] フィルタフォーム送信時に `taste` が送信されるようにする
- [ ] 並び順変更フォームに `taste` hidden 項目を追加する
- [ ] 表示件数変更フォームに `taste` hidden 項目を追加する
- [ ] ページネーションリンクに `taste` パラメータを追加する
- [ ] リセットリンクで `taste` が除去されることを確認する
- [ ] モバイルモーダルは変更しない

完了条件:
- [ ] 検索結果画面上でテイスト条件が保持・反映される

### 3.8 仕上げ・確認

TODO:
- [ ] 変更した Java ファイルのコンパイルエラーを確認する
- [ ] 変更した Thymeleaf テンプレートの構文崩れを確認する
- [ ] 変更した SQL の XML 構文崩れを確認する
- [ ] リクエストパラメータ名 `taste` が Controller / Template / URL で統一されていることを確認する

---

## 4. 単体テストTODO

### 4.1 テスト基盤整備

TODO:
- [ ] `src/test/java` 配下にプロダクトコードと同等のパッケージ構成を作成する
- [ ] 既存のテスト依存関係と実行方法を確認する
- [ ] 必要に応じて JUnit / Mockito / Spring Test の利用方針を確認する

### 4.2 ProductFilterOptionService テスト

対象候補:
- `src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java`

TODO:
- [ ] `buildSearchTasteOptions` が重複排除済みのテイスト名一覧を返すテストを作成する
- [ ] `buildSearchTasteFilter` がテイスト名からカテゴリ別 taste_id を正しく生成するテストを作成する
- [ ] 不正なテイスト名を無視するテストを作成する
- [ ] 空入力時に空の `ProductCategoryFilter` を返すテストを作成する

### 4.3 ProductRepository テスト

対象候補:
- `src/test/java/jp/co/skig/officeorder/repository/ProductRepositoryTest.java`

TODO:
- [ ] 空白のみ入力が空ワード扱いになるテストを作成する
- [ ] 全角数字→半角数字変換のテストを作成する
- [ ] 全角英字→半角英字変換のテストを作成する
- [ ] 全角記号→半角記号変換のテストを作成する
- [ ] 複数ワード分割のテストを作成する
- [ ] 最大 5 ワード制限のテストを作成する
- [ ] 重複ワード除外のテストを作成する
- [ ] `hasSearchTasteFilter` が 3 カテゴリいずれかに値があると true になるテストを作成する
-
- 実装メモ:
- `buildSearchParams` や正規化メソッドが private の場合は、テスト方針を以下から選択する
- [ ] `search(...)` を通じた振る舞いテストで担保する
- [ ] package-private な補助メソッドへ切り出してテストしやすくする

### 4.4 Controller テスト

対象候補:
- `src/test/java/jp/co/skig/officeorder/web/CatalogControllerTest.java`

TODO:
- [ ] `/products/search` が `taste` パラメータを受け取って Model に反映するテストを作成する
- [ ] `tasteOptions` / `selectedTasteNames` が設定されるテストを作成する
- [ ] キーワード未入力時に `keyword` が空文字で設定されるテストを作成する

### 4.5 テンプレート / 結合寄り確認

TODO:
- [ ] 検索結果画面の HTML に `name="taste"` のチェックボックスが出力されることを確認する
- [ ] 並び順変更・表示件数変更フォームに `taste` hidden 項目が出力されることを確認する
- [ ] ページネーションリンクに `taste` が含まれることを確認する

---

## 5. 実施順序

1. `ProductFilterOptionService` の機能追加
2. `CatalogController` の `taste` 受け取り追加
3. `ProductRepository` のキーワード正規化・分割実装
4. `ProductMapper.xml` の検索条件変更
5. `product-list-search-results.html` の UI 更新
6. 単体テスト作成
7. コンパイル・表示確認

---

## 6. 完了判定

以下を満たしたら完了とする。

- [ ] `q` と `taste` を含む `/products/search` が要件どおり動作する
- [ ] 商品コードの完全一致 / 前方一致が動作する
- [ ] 商品名 / バリエーション名 / 説明文の部分一致が動作する
- [ ] 全角半角正規化と複数ワード AND 検索が動作する
- [ ] テイスト絞込がカテゴリ横断で動作する
- [ ] テイスト選択状態が並び順、表示件数、ページネーションで保持される
- [ ] 単体テストが追加され、主要ロジックが検証されている
- [ ] 既存カテゴリ一覧・新着一覧への回帰がない
