# 商品検索機能強化 - 開発計画

> **ステータス**: 📋 開発計画確定
> **前提**: 設計変更書（04_design-changes.md v1.2）が確定済みであること

---

## 1. 概要

設計変更書（§4: 影響を受けるソースファイル一覧）に基づき、S-001〜S-009の実装を依存関係に沿ってフェーズ分割し、各ファイルの変更タスクとテストケースを定義する。

### 1.1 フェーズ全体像

```
Phase 1: 基盤（依存なし）
  └─ S-007  NormalizationUtils（新規作成）
  └─ S-005  header.html（単純変更）

Phase 2: バックエンドコア — キーワード検索強化
  └─ S-002  ProductRepository（S-007 に依存）
  └─ S-001  ProductMapper.xml（S-002 に依存）

Phase 3: バックエンド — テイストフィルター
  └─ S-008  ProductFilterOptionService（新規メソッド）
  └─ S-003  CatalogController（S-008 に依存）

Phase 4: フロントエンド — テイストフィルターUI
  └─ S-006  product-list-search-results.html（S-003 に依存）

Phase 5: 結合テスト・非機能検証
  └─ キーワード検索 E2E 検証
  └─ テイストフィルター E2E 検証
  └─ NFR-002 パフォーマンス検証
```

### 1.2 依存関係図

```
S-007 ──→ S-002 ──→ S-001

S-008 ──→ S-003 ──→ S-006
S-005（独立）
S-004（変更なし）
S-009（変更なし）
```

---

## 2. Phase 1: 基盤

### 2.1 S-007: NormalizationUtils（新規作成）

**ファイル**: `jp.co.skig.officeorder.common.NormalizationUtils`
**対応設計**: D-013（パッケージ構成）、D-011（正規化仕様）
**対応要件**: FR-021, FR-022, FR-023, FR-024

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-007-1 | クラス作成 | `common/` パッケージに `NormalizationUtils` を作成（staticメソッド or utilityクラス） |
| T-007-2 | 全角英字→半角変換 | Ａ-Ｚ → A-Z, ａ-ｚ → a-z（U+FF21〜U+FF5A → U+0041〜U+007A） |
| T-007-3 | 全角数字→半角変換 | ０-９ → 0-9（U+FF10〜U+FF19 → U+0030〜U+0039） |
| T-007-4 | 半角カタカナ→全角変換 | 清音: ｱ→ア 等。濁音結合: ｶﾞ→ガ 等。半濁音結合: ﾊﾟ→パ 等。長音: ｰ→ー |

#### テストケース

| # | テストメソッド | 入力 | 期待値 | FR |
|---|---|---|---|---|
| UT-007-01 | 全角英大文字→半角 | `"ＡＢＣＤ"` | `"ABCD"` | FR-022 |
| UT-007-02 | 全角英小文字→半角 | `"ａｂｃｄ"` | `"abcd"` | FR-022 |
| UT-007-03 | 全角数字→半角 | `"０１２３"` | `"0123"` | FR-021 |
| UT-007-04 | 半角カタカナ清音→全角 | `"ﾃﾞｽｸ"` | `"デスク"` | FR-023 |
| UT-007-05 | 半角カタカナ濁音結合→全角 | `"ｶﾞ"` | `"ガ"` | FR-023 |
| UT-007-06 | 半角カタカナ半濁音結合→全角 | `"ﾊﾟ"` | `"パ"` | FR-023 |
| UT-007-07 | 半角長音→全角 | `"ｺﾝﾋﾟｭｰﾀｰ"` | `"コンピューター"` | FR-023 |
| UT-007-08 | 混在入力の正規化 | `"Ａ１ﾃﾞｽｸabc"` | `"A1デスクabc"` | FR-021〜023 |
| UT-007-09 | 半角英数はそのまま | `"ABC123"` | `"ABC123"` | FR-022 |
| UT-007-10 | 全角カタカナはそのまま | `"デスク"` | `"デスク"` | FR-023 |
| UT-007-11 | null入力 | `null` | `null` or `""` | - |
| UT-007-12 | 空文字入力 | `""` | `""` | - |
| UT-007-13 | ひらがな・漢字はそのまま | `"机 つくえ"` | `"机 つくえ"` | - |

**テストクラス**: `NormalizationUtilsTest`（JUnit 5）

---

### 2.2 S-005: header.html

**ファイル**: `templates/fragments/header.html`
**対応設計**: D-003（共通レイアウト）
**対応要件**: FR-041, FR-046

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-005-1 | placeholder 変更 | `"商品名・商品コード"` → `"商品名・商品コード・キーワード"` |
| T-005-2 | aria-label 変更 | 同上 |
| T-005-3 | maxlength 追加 | `maxlength="100"` を input 要素に追加 |

#### テストケース

| # | テスト内容 | 検証方法 | FR |
|---|---|---|---|
| MT-005-01 | placeholder が「商品名・商品コード・キーワード」に変更されていること | 目視 / Controller スライステスト | FR-046 |
| MT-005-02 | maxlength="100" が設定されていること | 目視 / Controller スライステスト | FR-041 |
| MT-005-03 | aria-label が変更されていること | 目視 | FR-046 |

---

## 3. Phase 2: バックエンドコア — キーワード検索強化

### 3.1 S-002: ProductRepository

**ファイル**: `jp.co.skig.officeorder.repository.ProductRepository`
**対応設計**: D-011, D-012
**対応要件**: FR-001, FR-010〜012, FR-021〜024, FR-043

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-002-1 | `toKeywordLike()` 改修 | 正規化（`NormalizationUtils`）→ エスケープ（`\`→`\\`, `%`→`\%`, `_`→`\_`）→ `%` + escaped + `%` |
| T-002-2 | `toKeywordPrefix()` 新規 | 正規化 → エスケープ → escaped + `%` |
| T-002-3 | `buildSearchParams()` 改修 | `keywordPrefix` パラメータ追加、`hasTasteFilter` フラグ追加 |

#### テストケース

| # | テストメソッド | 入力 | 期待値 | FR |
|---|---|---|---|---|
| UT-002-01 | toKeywordLike_通常キーワード | `"デスク"` | `"%デスク%"` | FR-010 |
| UT-002-02 | toKeywordLike_全角英数の正規化 | `"ＡＢＣ１２３"` | `"%ABC123%"` | FR-021, 022 |
| UT-002-03 | toKeywordLike_半角カタカナの正規化 | `"ﾃﾞｽｸ"` | `"%デスク%"` | FR-023 |
| UT-002-04 | toKeywordLike_エスケープ_パーセント | `"50%OFF"` | `"%50\%OFF%"` | FR-043 |
| UT-002-05 | toKeywordLike_エスケープ_アンダースコア | `"A_B"` | `"%A\_B%"` | FR-043 |
| UT-002-06 | toKeywordLike_エスケープ_バックスラッシュ | `"C:\\"` | `"%C:\\\\%"` | FR-043 |
| UT-002-07 | toKeywordLike_null | `null` | `null` | - |
| UT-002-08 | toKeywordLike_空白のみ | `"   "` | `null` | FR-042 |
| UT-002-09 | toKeywordPrefix_通常 | `"PRD-001"` | `"PRD-001%"` | FR-001 |
| UT-002-10 | toKeywordPrefix_全角英数正規化 | `"ＰＲＤ"` | `"PRD%"` | FR-001, 022 |
| UT-002-11 | toKeywordPrefix_エスケープ | `"PRD%"` | `"PRD\%%"` | FR-043 |
| UT-002-12 | toKeywordPrefix_null | `null` | `null` | - |
| UT-002-13 | buildSearchParams_hasTasteFilter_検索結果+テイスト選択あり | category=null, deskTasteIds=[1] | `hasTasteFilter=true` | FR-030 |
| UT-002-14 | buildSearchParams_hasTasteFilter_検索結果+テイスト未選択 | category=null, 全taste空 | `hasTasteFilter=false` | FR-033 |
| UT-002-15 | buildSearchParams_hasTasteFilter_カテゴリ別 | category=desk, deskTasteIds=[1] | `hasTasteFilter=false` | FR-034 |

**テストクラス**: `ProductRepositoryTest`（既存クラスに追加、またはメソッド単位で分離）

> **備考**: `toKeywordLike()` / `toKeywordPrefix()` はprivateメソッドのため、`buildSearchParams()` 経由で結合的にテストするか、テスト容易性のためにpackage-private化を検討する。

---

### 3.2 S-001: ProductMapper.xml

**ファイル**: `resources/mappers/ProductMapper.xml`
**対応設計**: D-012
**対応要件**: FR-001, FR-010〜012, FR-021〜024, FR-030〜033, FR-043

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-001-1 | NormalizeFrom / NormalizeTo SQL fragment 定義 | 全角英数→半角、半角清音カタカナ+長音→全角の変換テーブル文字列を `<sql>` で定義 |
| T-001-2 | BaseProductWhere 改修 — キーワード条件 | 商品コードを前方一致（`keywordPrefix`）に変更、`variation_name`・`description` のILIKE追加、全4条件に `translate()` + `ESCAPE '\'` を適用 |
| T-001-3 | BaseProductWhere 改修 — テイストフィルター | `hasTasteFilter` が true のとき3カテゴリのEXISTS条件を `<trim prefixOverrides="OR">` でOR結合 |

#### テストケース（結合テスト — Testcontainers）

| # | テスト内容 | 検証ポイント | FR |
|---|---|---|---|
| IT-001-01 | 商品名部分一致でヒット | `keyword=デスク` → 商品名に「デスク」を含む商品がヒット | FR-010 |
| IT-001-02 | バリエーション名部分一致でヒット | `keyword=ウォールナット` → variation_nameに該当する商品がヒット | FR-011 |
| IT-001-03 | 説明文部分一致でヒット | `keyword=オフィス` → descriptionに「オフィス」を含む商品がヒット | FR-012 |
| IT-001-04 | 商品コード完全一致でヒット | `keyword=PRD-DESK-001` → 該当商品がヒット | FR-001 |
| IT-001-05 | 商品コード前方一致でヒット | `keyword=PRD-DESK` → PRD-DESK で始まる商品コードの商品がヒット | FR-001 |
| IT-001-06 | 商品コード中間一致で非ヒット（商品コード条件のみ） | `keyword=DESK-001`（中間のみ一致） → 商品コード前方一致条件ではヒットしない。ただし商品名・説明文等のテキスト検索でヒットする場合は表示される（FR-003 OR結合） | FR-001, 003 |
| IT-001-07 | 全角英数で半角データにヒット | `keyword=ＡＢＣ` → DB上の半角ABCにヒット | FR-022 |
| IT-001-08 | 半角カタカナで全角データにヒット | `keyword=ﾃﾞｽｸ` → DB上の全角「デスク」にヒット | FR-023 |
| IT-001-09 | 全角数字で半角データにヒット | `keyword=１２３` → DB上の半角123にヒット | FR-021 |
| IT-001-10 | ILIKE大文字小文字の同一視 | `keyword=desk` → DB上の「Desk」にヒット | FR-020 |
| IT-001-11 | `%` エスケープ | `keyword=50%` → `%`をワイルドカードとして解釈しない | FR-043 |
| IT-001-12 | `_` エスケープ | `keyword=A_B` → `_`をワイルドカードとして解釈しない | FR-043 |
| IT-001-13 | テイストフィルター — デスクテイスト選択 | deskTasteIds=[1] → デスクでテイストID=1の商品のみヒット | FR-030, 032 |
| IT-001-14 | テイストフィルター — 複数カテゴリOR結合 | deskTasteIds=[1], chairTasteIds=[2] → いずれかに該当する商品がヒット | FR-032 |
| IT-001-15 | テイストフィルター — 未選択時は全件 | 全taste空 → テイスト属性によるフィルタリングなし | FR-033 |
| IT-001-16 | テイストフィルター — NULLテイスト除外 | deskTasteIds=[1] → テイスト属性NULLの商品は非表示 | FR-033 |
| IT-001-17 | 販売期間外の商品は非表示 | sale_end_at < now の商品 → 検索結果に含まれない | FR-054 |
| IT-001-18 | translate() fragment の XML エスケープ確認 | NormalizeFrom / NormalizeTo fragmentが正しく展開されることを確認（`&`等のXML特殊文字） | FR-024 |
| IT-001-19 | 商品コード前方一致 — 商品単位で1件表示 | 同一商品の複数バリアントがキーワード前方一致でヒットした場合、商品単位で1件として表示されること | FR-002 |
| IT-001-20 | 商品コード+テキスト検索のOR結合 | 商品コード前方一致でヒットする商品と、商品名テキスト検索でヒットする別商品が、両方とも結果に含まれること | FR-003 |
| IT-001-21 | スペース含みキーワードの1語扱い | `keyword=ホワイト デスク`（スペース含む）→ 「ホワイト デスク」全体を1つの部分一致キーワードとして検索し、スペース区切りのAND分割はしないこと | FR-040 |

> **重要（Q-4 検証タスク）**: T-001-1 で定義する `NormalizeFrom` / `NormalizeTo` fragment内の文字列にXMLエスケープが必要な文字（`&`, `<`, `>` 等）が含まれないことを確認する。含まれる場合は `<![CDATA[...]]>` でラップする。

**テストクラス**: `ProductMapperSearchTest`（結合テスト、`@SpringBootTest` + Testcontainers）

---

## 4. Phase 3: バックエンド — テイストフィルター

### 4.1 S-008: ProductFilterOptionService

**ファイル**: `jp.co.skig.officeorder.service.product.ProductFilterOptionService`
**対応設計**: D-010（ビジネスロジック）
**対応要件**: FR-030〜033

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-008-1 | `buildSearchFilter()` 新規追加 | 引数: `rawDeskTasteIds`, `rawChairTasteIds`, `rawStorageTasteIds`, `optionsBundle`。マスタで正規化し、テイスト3種のみ有効な `ProductCategoryFilter` を返す |

#### テストケース

| # | テストメソッド | 入力 | 期待値 | FR |
|---|---|---|---|---|
| UT-008-01 | buildSearchFilter_正常 | desk=[1,2], chair=[3], storage=[] | filter.deskTasteIds=[1,2], chairTasteIds=[3], storageTasteIds=[] | FR-030 |
| UT-008-02 | buildSearchFilter_不正ID除外 | desk=[1,999] | filter.deskTasteIds=[1]（999はマスタに存在しないため除外） | FR-030 |
| UT-008-03 | buildSearchFilter_全空 | desk=[], chair=[], storage=[] | ProductCategoryFilter.empty() 相当 | FR-033 |
| UT-008-04 | buildSearchFilter_null入力 | desk=null, chair=null, storage=null | ProductCategoryFilter.empty() 相当 | FR-033 |
| UT-008-05 | buildSearchFilter_テイスト以外は空 | desk=[1] | filter.deskTopShapeIds=[], chairFunctionIds=[] 等すべて空 | FR-034 |

**テストクラス**: `ProductFilterOptionServiceTest`（JUnit 5 + Mockito）

---

### 4.2 S-009: ProductListSearchService（変更なし）

**ファイル**: `jp.co.skig.officeorder.service.product.ProductListSearchService`

変更不要。既存のカテゴリ別一覧パターンと同様に、`CatalogController` が `buildSearchFilter()` で構築した `ProductCategoryFilter` を11引数版 `buildCondition()` に渡す方式とするため、`ProductListSearchService` 自体への変更は発生しない。

> **根拠**: 既存のデスク一覧（`CatalogController.desks()`）では Controller が `productFilterOptionService.buildDeskFilter()` を呼び、結果を11引数版 `buildCondition(categoryFilter 付き)` に渡している。検索結果画面でも同じパターンを適用する。

---

### 4.3 S-003: CatalogController

**ファイル**: `jp.co.skig.officeorder.web.CatalogController`
**対応設計**: D-002, D-003, D-010, D-012
**対応要件**: FR-030〜033, FR-041

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-003-1 | テイストパラメータ受取 | `searchResults()` に `@RequestParam deskTaste`, `chairTaste`, `storageTaste`（`List<Integer>`, required=false）を追加 |
| T-003-2 | キーワード100文字切り詰め | `keyword` が100文字超の場合、先頭100文字に `substring` |
| T-003-3 | テイスト選択肢の Model 追加 | `deskTasteOptions`, `chairTasteOptions`, `storageTasteOptions` をModelに追加（`optionsBundle` から取得） |
| T-003-4 | テイスト選択状態の Model 追加 | `selectedDeskTasteIds`, `selectedChairTasteIds`, `selectedStorageTasteIds` をModelに追加（チェックボックス復元用） |
| T-003-5 | buildCondition 呼び出し変更 | `productFilterOptionService.buildSearchFilter()` で `ProductCategoryFilter` を構築し、11引数版 `productListSearchService.buildCondition(categoryFilter 付き)` に渡す。既存のカテゴリ別一覧（`desks()`/`chairs()`/`storages()`）と同じパターン |

#### テストケース（Spring MVC Test）

| # | テストメソッド | 入力 | 期待値 | FR |
|---|---|---|---|---|
| ST-003-01 | searchResults_テイストパラメータ受取 | `?q=デスク&deskTaste=1&deskTaste=2` | Modelにテイスト選択肢・選択状態が設定される | FR-030 |
| ST-003-02 | searchResults_テイストなし | `?q=デスク` | テイスト未選択でも正常に動作 | FR-033 |
| ST-003-03 | searchResults_キーワード100文字超 | `?q=` + 150文字 | keyword が100文字に切り詰められる | FR-041 |
| ST-003-04 | searchResults_キーワード100文字以下 | `?q=` + 50文字 | keyword がそのまま | FR-041 |
| ST-003-05 | searchResults_キーワード空 | `?q=` | 全件表示（0件メッセージなし） | FR-042 |
| ST-003-06 | searchResults_テイスト選択肢Model確認 | `?q=test` | deskTasteOptions, chairTasteOptions, storageTasteOptions がModelに存在 | FR-031 |
| ST-003-07 | searchResults_テイスト選択状態Model確認 | `?q=test&chairTaste=3` | selectedChairTasteIds=[3] がModelに存在 | FR-030 |

**テストクラス**: `CatalogControllerSearchTest`（`@WebMvcTest`）

---

## 5. Phase 4: フロントエンド — テイストフィルターUI

### 5.1 S-006: product-list-search-results.html

**ファイル**: `templates/pages/product-list-search-results.html`
**対応設計**: D-002（画面設計）
**対応要件**: FR-030〜033

#### 実装タスク

| # | タスク | 詳細 |
|---|---|---|
| T-006-1 | サイドバーフィルターにテイストアコーディオン追加 | `<details>`/`<summary>` で「デスクのテイスト」「チェアのテイスト」「収納家具のテイスト」の3グループ。各テイストをチェックボックスで選択。初期状態: closed。カテゴリ別テンプレート（`product-list-category-desk.html`）のテイスト実装パターンを参考にする |
| T-006-2 | ソート・件数変更フォームに hidden input 追加 | `deskTaste`, `chairTaste`, `storageTaste` を `th:each` ループで hidden input 化 |
| T-006-3 | ページネーションリンクにテイストパラメータ追加 | `th:href` にテイストパラメータを追加 |
| T-006-4 | チェックボックス選択状態の復元 | `selectedDeskTasteIds` 等を使い `th:checked` で復元 |

#### テストケース（目視 / E2E）

| # | テスト内容 | 検証ポイント | FR |
|---|---|---|---|
| MT-006-01 | テイストアコーディオン表示 | 3グループのアコーディオンが表示され、初期状態で折りたたまれていること | FR-031 |
| MT-006-02 | テイスト選択→絞り込み | チェックボックス選択で絞り込みが実行されること | FR-030 |
| MT-006-03 | テイスト選択状態の維持（ソート変更） | ソート順を変更してもテイスト選択状態が維持されること | FR-030 |
| MT-006-04 | テイスト選択状態の維持（ページ遷移） | ページ遷移してもテイスト選択状態が維持されること | FR-030 |
| MT-006-05 | テイスト選択状態の維持（件数変更） | 表示件数を変更してもテイスト選択状態が維持されること | FR-030 |
| MT-006-06 | テイスト未選択時は全件表示 | テイストを選択しなければフィルタリングされないこと | FR-033 |
| MT-006-07 | 複数カテゴリのテイストOR結合 | デスクとチェアのテイストを同時選択して、いずれかに該当する商品が表示されること | FR-032 |
| MT-006-08 | モバイル表示確認 | レスポンシブでテイストフィルターが使用可能であること | - |

---

## 6. Phase 5: 結合テスト・非機能検証

### 6.1 E2E テスト（手動）

| # | テスト内容 | 手順概要 | 対応要件 |
|---|---|---|---|
| E2E-01 | キーワード検索の正規化 | 1. ヘッダ検索で `ﾃﾞｽｸ` を入力 2. 全角「デスク」の商品がヒットすることを確認 | FR-023 |
| E2E-02 | 商品コード前方一致 | 1. 商品コードの先頭部分で検索 2. 該当商品がヒットすることを確認 3. 中間部分のみでは商品コード検索はヒットしないことを確認 | FR-001 |
| E2E-03 | バリエーション名・説明文検索 | 1. バリエーション名に含まれるキーワードで検索 2. 説明文に含まれるキーワードで検索 3. いずれもヒットすることを確認 | FR-011, 012 |
| E2E-04 | テイストフィルターの動作 | 1. キーワード検索後、テイストを選択 2. 絞り込まれることを確認 3. ソート・ページ遷移で選択状態が維持されることを確認 | FR-030〜033 |
| E2E-05 | キーワード100文字制限 | 1. 100文字超のキーワードを入力 2. 入力制限（maxlength）で制御されることを確認 3. APIに直接100文字超を送信し、切り詰められることを確認 | FR-041 |
| E2E-06 | 特殊文字エスケープ | 1. `%` や `_` を含むキーワードで検索 2. ワイルドカードとして解釈されないことを確認 | FR-043 |
| E2E-07 | 0件結果メッセージ | 1. ヒットしないキーワードで検索 2. 「該当する商品は見つかりませんでした。」が表示されることを確認 | FR-045 |
| E2E-08 | 空欄検索で全件表示 | 1. キーワード未入力で検索 2. 全件が表示されることを確認 | FR-042 |
| E2E-09 | 検索結果のソート順確認（回帰） | 1. キーワード検索を実行 2. おすすめ順/価格昇順/価格降順/新着順を各切替 3. 正しくソートされることを確認 | FR-052 |
| E2E-10 | 検索結果の件数切替確認（回帰） | 1. キーワード検索を実行 2. 15件→30件→60件を切替 3. 正しい件数が表示されることを確認 | FR-053 |
| E2E-11 | 検索結果のページネーション確認（回帰） | 1. 16件以上ヒットするキーワードで検索 2. ページ遷移が正しく動作することを確認 3. テイスト選択状態も維持されることを確認 | FR-051 |
| E2E-12 | カテゴリ別一覧の回帰確認 | 1. デスク/チェア/収納の各カテゴリ別一覧を表示 2. カテゴリ固有フィルター（テイスト含む）が従来通り動作することを確認 3. `hasTasteFilter` 追加による副作用がないことを確認 | FR-034 |

### 6.2 NFR-002 パフォーマンス検証

| # | 検証項目 | 方法 | 合格基準 |
|---|---|---|---|
| NFR-01 | 検索結果一覧の初回表示時間 | 1. テストデータ1,000商品を投入 2. 複数の検索パターンで初回表示時間を計測 3. 95パーセンタイルを算出 | 95%のケースで2秒以内（NFR-002） |
| NFR-02 | 60件表示時の応答時間 | size=60 で上記と同じ検証を実施 | 95%のケースで2秒以内（NFR-002） |
| NFR-03 | translate() のオーバーヘッド確認 | キーワード検索あり/なしで応答時間を比較し、translate() による劣化が許容範囲であることを確認 | 有意な劣化がないこと |

> **補足**: NFR-001 の想定（最大1,000件）に基づくテストデータを用意する。パフォーマンス問題が発覚した場合はインデックス・キャッシュの導入を検討するが、NFR-002の制約により現状アーキテクチャ内での対応を優先する。

---

## 7. S-004: ProductSearchCondition（変更なし）

**ファイル**: `jp.co.skig.officeorder.model.product.ProductSearchCondition`

変更不要。既存の `categoryFilter` フィールド（`ProductCategoryFilter` 型）でテイストIDを格納可能。`keyword` フィールドも既存のまま利用する。

---

## 8. テストデータ追加

結合テスト（IT-001-xx）の実行に必要なテストデータを `sql/seed/test-data/` に追加する。

| # | 追加内容 | 目的 |
|---|---|---|
| TD-01 | `variation_name` が設定された商品 | IT-001-02（バリエーション名検索） |
| TD-02 | `description` に検索可能なキーワードを含む商品 | IT-001-03（説明文検索） |
| TD-03 | 半角英数を含む商品名・商品コード | IT-001-07〜09（正規化テスト） |
| TD-04 | テイスト属性が設定された商品（デスク・チェア・収納各カテゴリ） | IT-001-13〜16（テイストフィルター） |
| TD-05 | テイスト属性がNULLの商品 | IT-001-16（NULLテイスト除外確認） |
| TD-06 | 販売期間外の商品 | IT-001-17（販売期間フィルタ確認） |
| TD-07 | 同一商品に複数バリアント（商品コード前方一致で複数ヒット） | IT-001-19（商品単位表示確認） |
| TD-08 | 商品コードでのみヒットする商品＋商品名でのみヒットする別商品 | IT-001-20（OR結合確認） |
| TD-09 | 商品名にスペースを含む商品（例:「ホワイト デスク A」） | IT-001-21（スペース1語扱い確認） |

---

## 9. 実装チェックリスト

最終的な実装完了の確認に使用する。

- [ ] **Phase 1**
  - [ ] S-007: NormalizationUtils 実装＋UT全パス
  - [ ] S-005: header.html 変更＋目視確認
- [ ] **Phase 2**
  - [ ] S-002: ProductRepository 改修＋UT全パス
  - [ ] S-001: ProductMapper.xml 改修＋IT全パス
  - [ ] NormalizeFrom / NormalizeTo の XML エスケープ確認（Q-4）
- [ ] **Phase 3**
  - [ ] S-008: ProductFilterOptionService.buildSearchFilter() 実装＋UT全パス
  - [ ] S-003: CatalogController 改修＋スライステスト全パス
- [ ] **Phase 4**
  - [ ] S-006: product-list-search-results.html テイストUI追加＋目視確認
- [ ] **Phase 5**
  - [ ] E2E テスト全件実施（E2E-01〜12）
  - [ ] カテゴリ別一覧の回帰確認（E2E-12）
  - [ ] NFR-002 パフォーマンス検証
- [ ] **全体**
  - [ ] `.\mvnw test` 全テストパス（既存テストの回帰確認含む）
  - [ ] コードレビュー完了

---

## 10. 変更履歴

| 日付 | 版数 | 変更内容 | 変更者 |
|---|---|---|---|
| 2026-04-07 | 1.0 | 初版作成 | - |
| 2026-04-07 | 1.1 | 検証指摘反映: S-009変更不要に修正（V-6）、IT-001-19〜21追加（V-1,2,3）、IT-001-06期待値修正（V-7）、E2E-09〜12回帰テスト追加（V-4,5）、TD-07〜09追加 | - |
