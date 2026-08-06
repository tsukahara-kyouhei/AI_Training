# FEAT-001 実装計画（TODO）

## 1. 実装方針

既存の商品一覧検索フローに沿って実装する。

- 入口: [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)
- 検索条件組み立て: [src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java)
- 検索条件正規化: [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java)
- SQL検索実体: [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- SQL定義: [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)
- 画面テンプレート: [src/main/resources/templates/pages/product-list-search-results.html](../../src/main/resources/templates/pages/product-list-search-results.html)

---

## 2. 実装タスク一覧

### 1. 検索キーワード正規化処理の追加
- [ ] 文字列の前後空白除去、英字小文字化、全角/半角正規化を行うユーティリティを追加する
- [ ] 既存の検索条件生成箇所で、入力キーワードを正規化済み値として扱うようにする
- [ ] 空入力・空白入力時の扱いを既存一覧画面の挙動に合わせて実装する

対象候補:
- [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java)
- 新規追加候補: [src/main/java/jp/co/skig/officeorder/service/product/SearchKeywordNormalizer.java](../../src/main/java/jp/co/skig/officeorder/service/product/SearchKeywordNormalizer.java)

### 2. 商品コード検索条件の実装
- [ ] 商品コードについて、完全一致および前方一致のみを対象にする条件をSQLに追加する
- [ ] 中間一致を発生させないよう、SQL条件を `=` または `LIKE 'keyword%'` に限定する
- [ ] 商品コード比較時に前後空白を除去した値で比較する

対象候補:
- [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)

### 3. 商品名・シリーズ名・説明文の部分一致検索の実装
- [ ] 商品名、シリーズ名、説明文を部分一致検索対象として追加する
- [ ] 英字大文字小文字の差異を吸収する
- [ ] 英数字・カタカナの全角/半角差異を吸収する
- [ ] 検索条件に応じて、SQLの `LIKE` 条件を適切に生成する

対象候補:
- [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)

### 4. 検索結果の優先順位・ソートの実装
- [ ] 検索結果の関連度順として、商品コード一致 → 商品名一致 → シリーズ名一致 → 説明文一致を優先する
- [ ] 既存の一覧画面の並び順と衝突しないよう、検索一覧での優先順位を明確にする
- [ ] 既定の初期表示順は「おすすめ順」または「新着順」に合わせて扱えるようにする

対象候補:
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)
- [src/main/java/jp/co/skig/officeorder/model/product/ProductSort.java](../../src/main/java/jp/co/skig/officeorder/model/product/ProductSort.java)

### 5. テイスト絞り込みの追加
- [ ] 検索結果画面の絞り込み条件に「テイスト」を追加する
- [ ] 既存カテゴリ一覧画面で実装済みの絞り込みロジック・コンポーネントを再利用できるようにする
- [ ] キーワード検索結果に対して、テイスト条件を AND 条件で適用する
- [ ] 複数選択に対応する

対象候補:
- [src/main/resources/templates/pages/product-list-search-results.html](../../src/main/resources/templates/pages/product-list-search-results.html)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java)
- [src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java](../../src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java)

### 6. 検索結果0件時・空入力時の表示改善
- [ ] 検索結果が0件の場合に、要件に沿ったメッセージを表示する
- [ ] 空入力・空白入力時の既存画面挙動を維持しつつ、必要に応じてメッセージを表示する
- [ ] 画面の表示条件をテンプレート側で整理する

対象候補:
- [src/main/resources/templates/pages/product-list-search-results.html](../../src/main/resources/templates/pages/product-list-search-results.html)
- [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)

### 7. 単体テストの追加
- [ ] 正規化ユーティリティの単体テストを追加する
- [ ] 商品検索条件生成の単体テストを追加する
- [ ] 商品コード完全一致・前方一致・中間一致防止のケースをテストする
- [ ] 空入力時のケースをテストする

対象候補:
- [src/test/java](../../src/test/java)
- 既存のテスト構成に合わせて JUnit テストを追加する

### 8. 統合確認・受け入れ確認
- [ ] ヘッダー検索から検索結果一覧へ遷移することを確認する
- [ ] 検索結果一覧で商品カードが表示されることを確認する
- [ ] テイスト絞り込みとキーワード検索が併用できることを確認する
- [ ] 0件時メッセージが表示されることを確認する
- [ ] 既存カテゴリ一覧画面の検索・絞り込み画面と互換性があることを確認する

---

## 3. 実装順序

1. 正規化・空入力の基盤実装
2. 商品コード・商品名・シリーズ名・説明文の検索条件実装
3. ソート・優先順位の実装
4. テイスト絞り込みの画面・条件連携
5. 0件時・空入力の表示改善
6. 単体テスト追加
7. 動作確認

---

## 4. 完了条件

- 要件定義書に記載した検索条件が実装されていること
- 商品コード検索で完全一致/前方一致のみヒットすること
- 商品名・シリーズ名・説明文で部分一致検索が動作すること
- 英字・数字・カタカナの全角/半角差異が吸収されること
- テイスト絞り込みとキーワード検索が併用できること
- 0件時表示が適切に行われること
- 単体テストが追加され、主要ケースをカバーしていること
