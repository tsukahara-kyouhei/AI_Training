# FEAT-001 商品検索機能強化 要件定義書

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| タイトル | 商品検索機能強化 |
| 作成日 | 2026-03-31 |
| ステータス | Draft |

---

## 1. 背景・目的

商品検索のヒット率が低く、検索結果がゼロまたは意図しない結果になる場合に離脱が発生している。  
検索精度を改善することで離脱率を低下させ、購買転換率の向上を図る。

---

## 2. 対象範囲

| 対象 | 説明 |
|------|------|
| ヘッダ検索ボックス | 全ページ共通ヘッダに設置されているキーワード入力欄 |
| 検索結果一覧画面 | `/products/search` — `product-list-search-results.html` が対応する画面全体 |

対象外（現状踏襲）：カテゴリ別一覧（`/categories/*`）、新着一覧（`/products/new-arrivals`）。

---

## 3. 現状の動作

### 3.1 キーワード検索の処理フロー

```
ヘッダ検索ボックス (q=...)
  → GET /products/search
    → CatalogController#searchResults
      → ProductListSearchService#buildCondition
        → ProductRepository#buildSearchParams
            toKeywordLike(keyword) → "%" + keyword.trim() + "%"
        → ProductMapper.xml: BaseProductWhere
            p.product_name ILIKE #{keywordLike}
            OR product_variants.product_code ILIKE #{keywordLike}
```

### 3.2 現状の課題

| 課題 | 説明 |
|------|------|
| 商品コードが中間一致 | `toKeywordLike` が `%キーワード%` を生成するため、商品コードの数字が意図せず部分一致する |
| 大文字/小文字と全角/半角の非統一 | PostgreSQL の `ILIKE` は大文字小文字を区別しないが、全角半角の正規化は未実装 |
| 説明文・バリエーション名が検索対象外 | `product_name` と `product_code` のみ対象。`description`・バリエーション名（`variation_name`）は未対応 |
| 検索結果にテイスト絞込なし | カテゴリ別一覧にあるテイスト絞込条件が検索結果画面には存在しない |

---

## 4. 機能要件

### 4.1 商品コード検索条件の変更

#### 要件

- 商品コード（`product_variants.product_code`）の検索は **完全一致** または **前方一致** のみを対象とする。
- 中間一致（`%keyword%`）は採用しない。

#### 判定仕様

キーワードが商品コードとして扱われる判定は行わず、**全キーワードに対して** 以下のロジックを適用する。

```
product_code = #{keyword}                -- 完全一致
OR product_code ILIKE #{keywordPrefix}   -- 前方一致（keywordPrefix = keyword + "%"）
```

> **理由**: 数字・英字を組み合わせた商品コードは中間一致で意図しない商品がヒットするため。  
> 例: キーワード `100` で商品コード `SKU-21001` が誤ってヒットするケースを防ぐ。

### 4.2 部分一致検索の対象拡大

以下のフィールドを **部分一致**（正規化後 ILIKE `%keyword%`）の検索対象に追加する。

| フィールド | テーブル/カラム | 現状 | 変更後 |
|-----------|------------|------|--------|
| 商品名 | `products.product_name` | 部分一致 ✔ | 変更なし |
| シリーズバリエーション名 | `products.variation_name` | 対象外 | **部分一致に追加**（`has_variation = FALSE` の商品は `variation_name` が NULL のため検索対象外） |
| 商品説明文 | `products.description` | 対象外 | **部分一致に追加** |
| 商品コード | `product_variants.product_code` | 部分一致（中間） | **完全一致 または 前方一致** に変更（4.1参照） |

#### 複数ワード検索（スペース区切り）

- キーワードにスペース（全角・半角）が含まれる場合、スペースで分割した各ワードを **AND 条件** で検索する。
- 例: 「モダン デスク」→「モダン」を含む AND「デスク」を含む商品がヒット。
- 各ワードは §4.3・§4.4 の正規化を個別に適用する。
- キーワードが空白のみの場合は、**キーワード未入力と同等** とみなし、キーワード条件は付与しない。
- 検索に使用するワード数は **最大 5 ワード** とし、6 ワード目以降は検索条件に使用しない。

### 4.3 大文字/小文字の区別なし検索

- 英字を含むキーワードについて、大文字・小文字を区別せずに検索できること。
- PostgreSQL の `ILIKE` 演算子または `lower()` 関数を利用して実現する。

### 4.4 全角/半角の区別なし検索

数字・英字・カタカナについて、入力された全角・半角を区別せずに検索できること。

#### 正規化方針

- DB への登録時は半角で格納する運用ルールとし、**入力キーワードのみ** アプリケーション層（`ProductRepository#toKeywordLike` 相当のメソッド）で正規化してから SQL に渡す。
- DB カラム側の変換は行わない。
- 半角格納の運用ルールは **`product_variants.product_code` を必須対象** とする。
- `products.product_name`・`products.variation_name`・`products.description` に含まれる数字・英字・カタカナについては、登録時に同一文字種を混在させない運用とし、本機能リリース前に既存データの表記ゆれを点検・補正する。

| 対象文字 | 正規化内容 |
|---------|----------|
| 全角数字（０–９） | 半角数字（0–9）に変換 |
| 全角英字（Ａ–Ｚ、ａ–ｚ） | 半角英字（A–Z、a–z）に変換 |
| 全角カタカナ | 半角カタカナに変換 |
| 半角カタカナ | 正規化後の比較対象として使用 |
| 全角記号（全角ハイフン・全角スラッシュ等） | 対応する半角記号に変換 |

### 4.5 検索結果画面へのテイスト絞込条件の追加

#### 要件

- 検索結果画面の絞込条件サイドバー（`product-list-search-results.html`）に「**テイスト**」チェックボックス群を追加する。
- テイストの選択肢・絞込ロジックは **各カテゴリ一覧の「テイスト」と同じ仕様** に従う。
- 検索結果画面のテイスト絞込は **カテゴリ条件を問わず横断的に絞り込む**（カテゴリを指定しない全商品を対象とする）。

#### 既存のテイスト仕様（参照元）

各カテゴリのテイストは独立したマスタテーブルで管理されている。

| カテゴリ | テーブル | 選択肢例 |
|---------|---------|---------|
| デスク | `desk_tastes` | ベーシック・カジュアル・シンプル・モダン・ナチュラル |
| チェア | `chair_tastes` | ベーシック・カジュアル・シンプル・モダン・ナチュラル |
| 収納 | `storage_tastes` | ベーシック・カジュアル・シンプル・モダン・ナチュラル |

カテゴリ別画面では `ProductFilterOptionsBundle`（`deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions`）を Model に渡し、Thymeleaf テンプレートでチェックボックスを描画している。

#### 検索結果画面での仕様

- 検索はカテゴリを横断するため、**全カテゴリのテイスト選択肢を統合して1つのリストとして表示** する。
  - 同名テイスト（例: 3テーブルすべてに「モダン」が存在）は画面表示上は1つにまとめる。
  - テイスト選択肢の重複排除は `display_name` の文字列一致で行い、`sort_order` 昇順で表示する。
  - 実際の絞込は、選択されたテイスト名に一致する `desk_tastes`・`chair_tastes`・`storage_tastes` の ID をテイスト名の文字列マッチングで解決し、それぞれの絞込条件に適用する。この変換は `ProductFilterOptionService` に追加する変換メソッドが担い、結果を `ProductCategoryFilter` へ詰めて `ProductSearchCondition.categoryFilter` に渡す。
- SQL では検索結果画面向けに **新規フラグ `hasSearchTasteFilter`** を設け、`deskTasteIds`・`chairTasteIds`・`storageTasteIds` の総件数が 1 件以上の場合に有効化する。
- SQL フラグメントはカテゴリ別一覧の既存条件とは分離し、**テイスト条件のみを扱う `SearchTasteFilter`** を新設する。
- `SearchTasteFilter` では、各カテゴリの taste_id リストが空でない場合にのみ `EXISTS` 句を出力し、空リストに対して `IN ()` は生成しない。
- テイスト複数選択時は **OR 条件**（いずれかのテイストに一致する商品）で絞り込む。
- キーワード検索とテイスト絞込を同時に指定した場合は **AND 条件**（キーワード検索結果をさらにテイストで絞り込む）とする。
- チェックボックスのUIデザイン・インタラクション（送信方法・リセット動作など）は既存のカテゴリ一覧画面のテイスト絞込と同じとする。
- URLパラメータ名は `taste`（複数選択可）とする。
- テイストを選択した状態でページ遷移・ソート変更・件数変更を行っても選択が維持されること。

#### 絞込ロジック

```
テイスト名リスト(選択済み) が空でない場合:
  -- 選択されたテイスト名から各カテゴリの taste_id を解決
  deskTasteIds   = desk_tastes   WHERE display_name IN (選択テイスト名リスト) AND is_active = TRUE
  chairTasteIds  = chair_tastes  WHERE display_name IN (選択テイスト名リスト) AND is_active = TRUE
  storageTasteIds= storage_tastes WHERE display_name IN (選択テイスト名リスト) AND is_active = TRUE

  -- カテゴリ条件なしで横断絞込
  AND (
    EXISTS (SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id AND da.taste_id IN (deskTasteIds))
    OR EXISTS (SELECT 1 FROM product_chair_attributes ca
               WHERE ca.product_id = p.product_id AND ca.taste_id IN (chairTasteIds))
    OR EXISTS (SELECT 1 FROM product_storage_attributes sa
               WHERE sa.product_id = p.product_id AND sa.taste_id IN (storageTasteIds))
  )
```

### 4.6 現状踏襲事項

本課題で変更しない既存機能は以下の通り。

| 機能 | 内容 |
|------|------|
| カテゴリ絞込 | 検索結果画面にカテゴリ絞込は追加しない |
| 在庫あり絞込 | 変更なし |
| 価格帯絞込 | 変更なし |
| カラー絞込 | 変更なし |
| 並び順 | 変更なし（おすすめ順・新しい順・価格昇順・価格降順） |
| ページング | 変更なし（15・30・60件） |
| ヘッダ検索ボックスのUI | 変更なし（入力欄・送信ボタンのデザイン・パラメータ名 `q`） |

---

## 5. 非機能要件

### 5.1 性能要件

| 指標 | 目標値 |
|------|--------|
| 検索対象商品数 | 最大 1,000 件程度 |
| 一覧初回表示のレスポンスタイム | 95 パーセンタイルで **2 秒以内** |

#### 性能確保のための考慮事項

- キーワード検索対象カラムへのインデックスを確認・整備する（`products.product_name`・`products.variation_name`・`products.description`・`product_variants.product_code`）。
- 全角/半角の正規化はアプリ層のみで実施し、DB 側の正規化インデックスは採用しない。
- テイスト絞込追加後のクエリ結合数増加を考慮し、`EXPLAIN ANALYZE` で実行計画を確認する。

---

## 6. 画面設計

### 6.1 検索結果画面 絞込サイドバー（変更後イメージ）

```
条件で絞り込む
─────────────────────────
□ 在庫有のみ表示

価格帯（税込）
  □ ～20,000円
  □ 20,000円～40,000円
  □ 40,000円～60,000円
  □ 60,000円～80,000円
  □ 80,000円～100,000円
  □ 100,000円～

カラー
  [スウォッチ一覧: 現状と同じ]

テイスト  ← 新規追加
  □ ベーシック
  □ カジュアル
  □ シンプル
  □ モダン
  □ ナチュラル

[検索] [リセット]
```

> テイストの選択肢は `desk_tastes` / `chair_tastes` / `storage_tastes` の有効レコード（`is_active = TRUE`）の `display_name` を `sort_order` 昇順でマージ・重複排除した一覧を使用する。

---

## 7. 改修対象コンポーネント

| レイヤ | ファイル | 変更内容 |
|--------|---------|---------|
| Controller | `CatalogController.java` | `searchResults` でリクエストパラメータ `taste` を受け取り、`tasteOptions`・`selectedTasteNames` を Model に追加 |
| Service | `ProductListSearchService.java` | `buildCondition` にテイスト名リストを渡せるよう引数変更 |
| Service | `ProductFilterOptionService.java` | 全カテゴリ横断テイスト選択肢取得メソッドを追加、テイスト名→カテゴリ別 taste_id 変換メソッドを追加 |
| Model | `ProductSearchCondition.java` | 変更なし（テイスト絞込は `categoryFilter` を通じて既存フィールドで保持） |
| Repository | `ProductRepository.java` | `toKeywordLike` 相当処理をキーワードワード分割対応へ変更（全角/半角・記号変換・スペース分割 AND 対応）、`buildSearchParams` に `keywordWords`・`keywordPrefixWords`・`hasSearchTasteFilter` を追加 |
| Mapper XML | `ProductMapper.xml` | `BaseProductWhere` の keyword 条件を `<foreach collection="keywordWords">` で動的生成する方式に変更し、商品コード用 `keywordPrefixWords` を使用。検索結果画面向けに `SearchTasteFilter` を追加 |
| Template | `product-list-search-results.html` | テイスト絞込チェックボックス追加（デスクトップ用サイドバーのみ）、ページネーションリンクに `taste` パラメータを追加 |
| SQL | 必要に応じて `schema/` または `migration` | `ILIKE` / 前方一致検索を前提とした索引設計を確認 |

---

## 8. 検索ロジック変更詳細

### 8.1 変更前 SQL イメージ

```sql
-- ProductMapper.xml BaseProductWhere (現状)
AND (
  p.product_name ILIKE '%keyword%'
  OR EXISTS (
    SELECT 1 FROM product_variants pvk
    WHERE pvk.product_id = p.product_id
    AND pvk.product_code ILIKE '%keyword%'   -- 中間一致
  )
)
```

### 8.2 変更後 SQL イメージ

スペース区切りで分割された各ワードを AND で結合する。

```sql
-- ProductMapper.xml BaseProductWhere (変更後)
-- ※ キーワード "モダン デスク" → keywordWords = ["モダン", "デスク"]
<foreach collection="keywordWords" item="word" index="index">
AND (
  p.product_name      ILIKE CONCAT('%', #{word}, '%')
  OR p.variation_name ILIKE CONCAT('%', #{word}, '%')
  OR p.description    ILIKE CONCAT('%', #{word}, '%')
  OR EXISTS (
    SELECT 1 FROM product_variants pvk
    WHERE pvk.product_id = p.product_id
    AND (
      pvk.product_code = #{word}
      OR pvk.product_code ILIKE #{keywordPrefixWords[index]}
    )
  )
)
</foreach>

<if test="hasSearchTasteFilter">
AND (
  <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">
    EXISTS (SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id
            AND da.taste_id IN
            <foreach collection="deskTasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>)
  </if>
  <if test="chairTasteIds != null and chairTasteIds.size() &gt; 0">
    <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">OR</if>
    EXISTS (SELECT 1 FROM product_chair_attributes ca
            WHERE ca.product_id = p.product_id
            AND ca.taste_id IN
            <foreach collection="chairTasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>)
  </if>
  <if test="storageTasteIds != null and storageTasteIds.size() &gt; 0">
    <if test="(deskTasteIds != null and deskTasteIds.size() &gt; 0) or (chairTasteIds != null and chairTasteIds.size() &gt; 0)">OR</if>
    EXISTS (SELECT 1 FROM product_storage_attributes sa
            WHERE sa.product_id = p.product_id
            AND sa.taste_id IN
            <foreach collection="storageTasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>)
  </if>
)
</if>
```

> - 各ワードは `ProductRepository` で全角/半角・全角記号・大文字/小文字の正規化を施した後、スペースで分割する。
> - テイスト絞込が指定されている場合は上記に加えて AND でテイスト条件を結合する。

---

## 9. 受入条件

| # | 確認観点 | 合否基準 |
|---|---------|---------|
| AC-01 | 商品コードの完全一致検索 | 商品コード `SKU-100` を入力すると当該商品のみヒットする |
| AC-02 | 商品コードの前方一致検索 | 商品コード `SKU-1` を入力すると `SKU-100`・`SKU-101` 等がヒットする |
| AC-03 | 商品コードの中間一致排除 | キーワード `100` を入力したとき、商品コード `SKU-21001` が商品コードの前方一致・完全一致でヒットしないこと（商品名や説明文に「100」を含む場合は引き続きヒットする） |
| AC-04 | 商品名の部分一致 | 商品名の一部を入力するとヒットする（現状踏襲） |
| AC-05 | バリエーション名の部分一致 | バリエーション名の一部を入力するとヒットする（バリエーション名が設定されていない商品（`has_variation = FALSE`）は対象外） |
| AC-06 | 説明文の部分一致 | 説明文に含まれる単語を入力するとヒットする |
| AC-07 | 英字大文字小文字不問 | `desk`、`DESK`、`Desk` いずれも同じ結果になる |
| AC-08 | 全角数字 | `１００` と `100` で同じ結果になる |
| AC-09 | 全角英字 | `ＳＫＵ` と `SKU` で同じ結果になる |
| AC-10 | 全角/半角カタカナ | `デスク`（全角）と `ﾃﾞｽｸ`（半角）で同じ結果になる |
| AC-11 | テイスト絞込表示 | 検索結果画面の絞込サイドバーにテイスト選択肢が表示される |
| AC-12 | テイスト絞込結果 | テイスト「モダン」を選択すると、モダンテイストの商品のみに絞り込まれる |
| AC-13 | テイスト状態保持 | ソート変更・ページ遷移後もテイストの選択が維持される |
| AC-14 | テイストリセット | リセットリンクでテイスト選択が解除される |
| AC-15 | 性能 | 商品1,000件での検索レスポンスが 95 パーセンタイルで 2 秒以内 |
| AC-16 | 複数ワード AND 検索 | キーワード `モダン デスク` を入力したとき、「モダン」かつ「デスク」を含む商品のみヒットし、どちらか一方しか含まない商品はヒットしない |
| AC-17 | 全角記号変換 | キーワード `ＳＫＵ－100`（全角ハイフン）と `SKU-100`（半角ハイフン）で同じ検索結果になる |
| AC-18 | テイスト絞込のカテゴリ横断 | テイスト `モダン` を選択したとき、デスク・チェア・収納の全カテゴリを横断してモダンテイストの商品がヒットする |
| AC-19 | 空白のみ入力 | キーワードに空白のみを入力したとき、キーワード条件なしの検索結果が表示される |
| AC-20 | テイスト選択肢の重複排除 | テイスト選択肢に同名のテイスト（例: `モダン`）が重複表示されず、1件のみ表示される |

---

## 10. 未決事項

解決済み事項（参考）

| # | 事項 | 決定内容 |
|---|------|----------|
| Q-01 | 全角/半角正規化の対象範囲 | **アプリ層のみ**で対応。DB登録時は半角で格納する運用ルールとし、DB側のインデックス変換は不要。 |
| Q-02 | テイスト横断検索の方式 | **テイスト名の文字列マッチング**で `desk_tastes`・`chair_tastes`・`storage_tastes` を横断して taste_id を解決し、検索結果画面向けに `hasSearchTasteFilter` と `SearchTasteFilter` を追加する方式を採用。 |
| Q-03 | モバイル絞込モーダルへのテイスト追加 | **スコープ外**。デスクトップ用サイドバーのみに追加する。現状のモーダルは暫定UIのまま据え置く。 |

---

## 11. 関連ファイル（参照先）

- [src/main/java/.../web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)
- [src/main/java/.../service/product/ProductListSearchService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java)
- [src/main/java/.../service/product/ProductFilterOptionService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java)
- [src/main/java/.../model/product/ProductSearchCondition.java](../../src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java)
- [src/main/java/.../model/product/ProductFilterOptionsBundle.java](../../src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java)
- [src/main/java/.../model/product/ProductCategoryFilter.java](../../src/main/java/jp/co/skig/officeorder/model/product/ProductCategoryFilter.java)
- [src/main/java/.../repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)
- [src/main/resources/templates/pages/product-list-search-results.html](../../src/main/resources/templates/pages/product-list-search-results.html)
- [src/main/resources/templates/pages/product-list-category-desk.html](../../src/main/resources/templates/pages/product-list-category-desk.html)
- [sql/schema/masters.sql](../../sql/schema/masters.sql)
- [sql/seed/masters.sql](../../sql/seed/masters.sql)
