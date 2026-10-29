# FEAT-001 改修ファイル一覧

**作成日**: 2026年09月02日  
**対象**: FEAT-001 商品検索機能（キーワード検索・prefix検索）  
**改修状況**: 実装・テスト・ドキュメント完了

---

## 📄 ドキュメント（設計書）

### 新規作成

| # | ファイルパス | 概要 | 改修理由 |
|---|-----------|------|---------|
| 1 | [docs/issues/FEAT-001-requirements.md](FEAT-001-requirements.md) | 要件定義書（keyword normalization、exact/prefix/contains） | FEAT-001 の正式な要件を定義 |
| 2 | [docs/design/FEAT-001-search-design.md](../design/FEAT-001-search-design.md) | 詳細設計書（boundary conditions、SQL仕様） | 要件を実装レベルに落とし込み |
| 3 | [docs/issues/FEAT-001-TODO-final.md](FEAT-001-TODO-final.md) | 最終TODO・チェックリスト | 実装・テスト・リリースの追跡 |
| 4 | [docs/issues/FEAT-001-FINAL-REPORT.md](FEAT-001-FINAL-REPORT.md) | 最終実装監査報告書 | 全検証の根拠を文書化 |

---

## 💻 ソースコード（実装）

### Java / Spring Boot

#### 改修・作成ファイル

| # | ファイルパス | 改修内容 | 行数 |
|---|-----------|---------|------|
| 1 | [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java) | 新規メソッド実装<br>・ `normalizeKeyword(String keyword)`<br>・ `buildProductCodePrefixPattern(String keyword)`<br>・ `buildKeywordContainsPattern(String keyword)`<br>・ `escapeLikePattern(String pattern)`<br>・ `buildSearchParams(ProductSearchCondition condition)` | 新規実装 |
| 2 | [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java) | 検証・既存実装の確認<br>・ `normalize(ProductSearchCondition condition)` 呼び出し<br>・ `buildCondition(...)` でのkeyword正規化 | 既存確認 |
| 3 | [src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java) | 検証・既存実装の確認<br>・ `searchWithPageCorrection()` での count/select 処理<br>・ ページオーバー補正ロジック | 既存確認 |
| 4 | [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java) | 検証・既存実装の確認<br>・ `searchResults(@RequestParam keyword)` エンドポイント<br>・ `/products/search` マッピング | 既存確認 |

### SQL / MyBatis

#### 改修・作成ファイル

| # | ファイルパス | 改修内容 | 修正点 |
|---|-----------|---------|--------|
| 1 | [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) | SQL 条件式の修正<br>・ exact match: `LOWER(pvk.product_code) = #{keywordExactNormalized}`<br>・ prefix match: `LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'`<br>・ contains match: `ILIKE #{keywordContainsPattern} ESCAPE '\'` | `ESCAPE '\'` を全 LIKE 句に明示指定<br>product_code では contains を対象外 |

### Thymeleaf テンプレート

#### 検証ファイル（改修なし）

| # | ファイルパス | 状態 | 確認内容 |
|---|-----------|------|---------|
| 1 | [src/main/resources/templates/pages/product-list-search-results.html](../../src/main/resources/templates/pages/product-list-search-results.html) | 既存・検証 | キーワード表示、件数バッジ、検索フォーム |
| 2 | [src/main/resources/templates/fragments/layout/header.html](../../src/main/resources/templates/fragments/layout/header.html) | 既存・検証 | `/products/search` フォーム、名前="q" |

---

## 🧪 テストコード

### 新規作成

| # | ファイルパス | テスト内容 | テスト件数 | 状態 |
|---|-----------|---------|----------|------|
| 1 | [src/test/java/jp/co/skig/officeorder/service/product/ProductKeywordSearchTest.java](../../src/test/java/jp/co/skig/officeorder/service/product/ProductKeywordSearchTest.java) | 単体テスト<br>・ keyword normalization (trim, NFKC, toLowerCase)<br>・ exact pattern 生成<br>・ prefix pattern 生成 (boundary conditions)<br>・ contains pattern 生成 (escape) | 4 | ✅ PASS |
| 2 | [src/test/java/jp/co/skig/officeorder/service/product/ProductSearchIntegrationTest.java](../../src/test/java/jp/co/skig/officeorder/service/product/ProductSearchIntegrationTest.java) | 統合テスト（DB + API）<br>・ repository.search() 実DB実行<br>・ count/select parity<br>・ endpoint HTTP レスポンス | 3 | ✅ PASS |

### テスト実行結果

```
Tests run: 7
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS ✅
```

---

## 📊 DB スキーマ

### 確認ファイル（改修なし）

| # | ファイルパス | 確認内容 |
|---|-----------|---------|
| 1 | [sql/schema/products.sql](../../sql/schema/products.sql) | product_variants.product_code が正しい位置に存在を確認 |

---

## 🔍 改修概要（カテゴリ別）

### ドキュメント
- **新規**: 4 ファイル
  - 要件定義、詳細設計、最終TODO、監査報告書

### コード実装
- **新規実装**: ProductRepository の 5 メソッド
- **既存確認**: ProductService / ProductListSearchService / CatalogController（ロジック検証済み）
- **SQL修正**: ProductMapper.xml の ESCAPE 構文追加

### テスト
- **新規作成**: 2 テストクラス（ProductKeywordSearchTest、ProductSearchIntegrationTest）
- **テスト結果**: 7/7 PASS

### テンプレート
- **改修**: なし（既存テンプレートで要件を満たす）

---

## 📝 改修ファイルの構成図

```
docs/
├── issues/
│   ├── FEAT-001-requirements.md          [新規] 要件定義
│   ├── FEAT-001-TODO-final.md            [新規] 最終TODO
│   └── FEAT-001-FINAL-REPORT.md          [新規] 監査報告書
└── design/
    └── FEAT-001-search-design.md         [新規] 詳細設計

src/main/
├── java/jp/co/skig/officeorder/
│   ├── repository/
│   │   └── ProductRepository.java        [改修] keyword処理メソッド追加
│   ├── service/product/
│   │   ├── ProductService.java           [検証] 既存実装確認
│   │   └── ProductListSearchService.java [検証] 既存実装確認
│   └── web/
│       └── CatalogController.java        [検証] 既存実装確認
├── resources/
│   ├── mappers/
│   │   └── ProductMapper.xml             [改修] ESCAPE '\' 追加
│   └── templates/pages/
│       └── product-list-search-results.html [検証] 既存テンプレート
└── test/java/jp/co/skig/officeorder/service/product/
    ├── ProductKeywordSearchTest.java     [新規] 単体テスト 4件
    └── ProductSearchIntegrationTest.java [新規] 統合テスト 3件

sql/
└── schema/
    └── products.sql                      [検証] スキーマ確認
```

---

## ✅ 検証状況サマリー

| 項目 | ファイル数 | 新規 | 改修 | 検証 | 状態 |
|------|-----------|------|------|------|------|
| ドキュメント | 4 | 4 | 0 | 0 | ✅ 完了 |
| コード実装 | 4 | 1 | 1 | 2 | ✅ 完了 |
| SQL | 1 | 0 | 1 | 0 | ✅ 完了 |
| テスト | 2 | 2 | 0 | 0 | ✅ 完了 (7/7 PASS) |
| テンプレート | 2 | 0 | 0 | 2 | ✅ 完了 |
| DB スキーマ | 1 | 0 | 0 | 1 | ✅ 完了 |
| **合計** | **14** | **7** | **2** | **5** | **✅ 全て完了** |

---

## 🎯 主要な改修ポイント

### 1. Keyword Normalization
```java
// ProductRepository.normalizeKeyword()
trim → NFKC正規化 → toLowerCase(Locale.ROOT)
```

### 2. Product Code Pattern
```java
// ProductRepository.buildProductCodePrefixPattern()
p\d{4}              // P0001 → p0001
p\d{4}-[a-z]+       // P0001-C → p0001-c
```

### 3. SQL ESCAPE 構文
```sql
-- ProductMapper.xml
LIKE #{keywordPrefixPattern} ESCAPE '\'
ILIKE #{keywordContainsPattern} ESCAPE '\'
-- % / _ / \ は literal として処理
```

### 4. Contains は product_code を対象外
```xml
<!-- productName / variationName / description のみ -->
<!-- product_code では exact/prefix のみ -->
```

---

## 📋 チェックリスト

- [x] 要件定義書の作成
- [x] 詳細設計書の作成
- [x] ProductRepository.normalizeKeyword() 実装
- [x] ProductRepository.buildProductCodePrefixPattern() 実装
- [x] ProductRepository.buildKeywordContainsPattern() 実装
- [x] ProductRepository.escapeLikePattern() 実装
- [x] ProductRepository.buildSearchParams() 実装
- [x] ProductMapper.xml の ESCAPE '\' 追加
- [x] ProductKeywordSearchTest 作成（4 tests）
- [x] ProductSearchIntegrationTest 作成（3 tests）
- [x] テスト実行 - すべて PASS
- [x] 画面確認 - 4 キーワードで検証
- [x] 旧 keywordLike 削除確認
- [x] 最終報告書の作成

---

**すべての改修ファイルは要件・設計・テストの三点セットで検証済みです。**
