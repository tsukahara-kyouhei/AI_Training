# FEAT-001 商品検索機能強化 要件定義書

| 項目 | 内容 |
|------|------|
| Issue ID | FEAT-001 |
| タイトル | 商品検索機能強化 |
| 作成日 | 2026-03-16 |
| ステータス | 設計・製造着手可能 |

---

## 1. 背景・目的

現状の商品検索は、商品名・商品コードの部分一致（ILIKE `%keyword%`）のみに対応しており、
検索ヒット率が低くユーザーの離脱につながっている。

本機能強化では以下の改善を行い、検索ヒット率の向上と離脱率の低減を図る。

- 検索対象フィールドの拡充（シリーズバリエーション名・説明文の追加）
- 商品コード検索の精度向上（中間一致の廃止・完全一致または前方一致への変更）
- 文字種の表記ゆれ（大文字小文字・全角半角）を吸収した検索の実現
- 検索結果画面の絞込条件（テイスト）の追加

---

## 2. 対象範囲

| 対象 | 詳細 |
|------|------|
| ヘッダ検索ボックス | `<input name="q">` からの `GET /products/search` リクエスト |
| 検索結果一覧画面 | `/products/search` ページ（`product-list-search-results.html`） |

カテゴリ別一覧ページ（`/categories/desks` 等）、商品詳細ページは対象外。

---

## 3. 機能要件

### 3.1 検索対象フィールドの変更

#### 現状

| フィールド | テーブル | マッチ方式 |
|------------|----------|------------|
| 商品名 | `products.product_name` | 部分一致（ILIKE `%kw%`） |
| 商品コード | `product_variants.product_code` | 部分一致（ILIKE `%kw%`） |

#### 変更後

| フィールド | テーブル.カラム | マッチ方式 | 備考 |
|------------|----------------|------------|------|
| 商品名 | `products.product_name` | 部分一致 | 変更なし |
| シリーズバリエーション名 | `products.variation_name` | 部分一致 | **新規追加** |
| 説明文 | `products.description` | 部分一致 | **新規追加** |
| 商品コード | `product_variants.product_code` | **完全一致または前方一致** | **変更** |

- 商品コード以外は **OR 条件**で結合する（いずれかのフィールドに一致すれば検索にヒットする）
- `variation_name` が NULL の商品（バリエーションなし）は該当フィールドの検索から除外される（NULL は一致しない）

### 3.2 商品コード検索の変更

#### 要件

- 商品コードの検索は **完全一致** または **前方一致** のみとする
- 中間一致（`LIKE '%CODE%'`）は廃止する

#### 理由

商品コードは連番形式（例：`P0001-C01`）であり、コードの一部の数字が意図せず他商品にヒットしてしまうことを防ぐため。

#### 検索ロジック（SQL イメージ）

```sql
-- 変更前: 中間一致
pvk.product_code ILIKE '%' || #{normalizedKeyword} || '%'

-- 変更後: 完全一致 OR 前方一致
(pvk.product_code = #{normalizedKeyword}
 OR pvk.product_code ILIKE #{normalizedKeyword} || '%')
```

### 3.3 大文字小文字を区別しない検索

- 英字については大文字・小文字どちらで入力しても検索できること
- 実現方法：入力キーワードをすべて小文字に正規化したうえで、DBの照合にはケースインセンシティブな比較（PostgreSQL の `ILIKE`）を使用する

### 3.4 全角半角を区別しない検索

以下の文字種について、全角・半角どちらで入力しても同一とみなして検索できること。  
半角カタカナは対象外とする（一般ユーザが半角カタカナで検索する可能性が低いため）。

| 文字種 | 対象文字（例） |
|--------|----------------|
| 数字 | `0-9`（半角）⇔ `０-９`（全角） |
| 英字 | `a-zA-Z`（半角）⇔ `ａ-ｚＡ-Ｚ`（全角） |

#### 正規化ルール

入力キーワードに対して以下の変換をこの順序で適用する（Java サービス層で処理）。

1. 全角数字 → 半角数字（`０`→`0` … `９`→`9`）
2. 全角英字 → 半角英字（`Ａ`→`A` … `ｚ`→`z`）
3. 英字を小文字へ統一（大文字小文字不区別のため）

DB カラム値も検索時に PostgreSQL の `TRANSLATE` 関数を用いてクエリ内で正規化し、正規化後のキーワードと比較する。  
具体的には全角英字（52文字）・全角数字（10文字）を半角へ変換したうえで `ILIKE` による部分一致を行う。  
マッパーへ渡すパラメータは以下の3種とする。

| パラメータ名 | 形式 | 用途 |
|------------|------|------|
| `keywordLike` | `%abc%` | 商品名・バリエーション名・説明文の部分一致 |
| `keywordExact` | `abc` | 商品コードの完全一致 |
| `keywordPrefix` | `abc%` | 商品コードの前方一致 |

> **実装判断**
> 商品数は最大 1,000 件程度であり、クエリ実行時に正規化関数を適用しても性能要件（初回表示 95% で 2 秒以内）を満たすと判断する。
> ただし性能測定の結果、問題が発生した場合は検索用正規化カラムのプリコンピュート方式に切り替える。

### 3.5 検索結果画面への「テイスト」絞込条件の追加

#### 要件

- 検索結果の絞込条件に「テイスト」を追加する
- 絞込の動作仕様はカテゴリ一覧ページの「テイスト」絞込と同じとする：
  - チェックボックス形式（複数選択可）
  - 選択なしの場合はテイストによる絞込を行わない
  - 選択した場合は、選択テイストのいずれかに一致する商品のみを表示する

#### テイストマスタの統一（DB 変更を伴う）

現状、テイストマスタは `desk_tastes`・`chair_tastes`・`storage_tastes` の 3 テーブルに分散している。
検索結果画面はカテゴリをまたぐため、カテゴリ横断の統一テイストフィルタを実現するために以下の変更を行う。

**新設テーブル：`tastes`（テイスト統合マスタ）**

| カラム | 型 | 説明 |
|--------|----|------|
| `taste_id` | BIGINT IDENTITY PK | テイストID |
| `display_name` | VARCHAR(100) NOT NULL UNIQUE | 表示名 |
| `sort_order` | INTEGER NOT NULL DEFAULT 0 | 表示順 |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | 有効フラグ |
| `created_at` | TIMESTAMPTZ | 作成日時 |
| `updated_at` | TIMESTAMPTZ | 更新日時 |

**既存テーブルの変更**

| 対象テーブル | 変更内容 |
|-------------|---------|
| `product_desk_attributes.taste_id` | FK 参照先を `desk_tastes` → `tastes` に変更 |
| `product_chair_attributes.taste_id` | FK 参照先を `chair_tastes` → `tastes` に変更 |
| `product_storage_attributes.taste_id` | FK 参照先を `storage_tastes` → `tastes` に変更 |
| `desk_tastes` | 廃止（マイグレーション後に DROP） |
| `chair_tastes` | 廃止（マイグレーション後に DROP） |
| `storage_tastes` | 廃止（マイグレーション後に DROP） |

**データ移行方針**

- 既存 3 テーブルの `display_name` を `UNION` したうえで重複を排除し、新 `tastes` テーブルへ投入する
- `sort_order` はいずれかのテーブルの値を引き継ぐ（同名エントリは `MIN(sort_order)` を採用）
- カテゴリ属性テーブルの `taste_id` を新 `tastes.taste_id` の対応する行に更新する
- アプリケーションコード（Mapper・Service・Controller・テンプレート）の参照を新テーブルへ移行する

> **注意**
> 本変更はカテゴリ一覧ページのテイスト絞込にも影響する。
> 各カテゴリ一覧ページの動作は変更しないが、DB 参照先が変わるためカテゴリ一覧ページのコードも合わせて修正する。

#### テイスト絞込の検索 SQL イメージ（検索結果画面）

```sql
-- tasteIds が選択された場合
AND (
    EXISTS (SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id AND da.taste_id IN (...))
    OR EXISTS (SELECT 1 FROM product_chair_attributes ca
               WHERE ca.product_id = p.product_id AND ca.taste_id IN (...))
    OR EXISTS (SELECT 1 FROM product_storage_attributes sa
               WHERE sa.product_id = p.product_id AND sa.taste_id IN (...))
)
```

### 3.6 現状踏襲事項

以下は変更しない。

- 在庫ありのみ表示フィルタ
- 価格帯フィルタ
- カラーフィルタ
- 並び順（おすすめ順・新しい順・価格安い順・価格高い順）
- 表示件数（15・30・60）
- ページネーション
- キーワード未入力時は全商品を対象とする挙動
- `GET /products/search?q=` のエンドポイント

---

## 4. 画面仕様

### 4.1 検索結果画面（`/products/search`）

#### 4.1.1 ヘッダ検索ボックス

変更なし。プレースホルダーテキストは現状のまま（「商品名・商品コード」）とするが、
実際には説明文・シリーズバリエーション名も検索対象となる。

> **決定**：説明文・バリエーション名が検索対象に追加されたことをユーザーに示すため、
> プレースホルダーを「商品名・商品コードなど」に変更する。

#### 4.1.2 絞込条件サイドバー（デスクトップ）

現状の絞込条件に「テイスト」セクションを追加する。

**追加位置**：カラーフィルタの下（セパレータ `<hr>` ありで区切る）

**追加 HTML 構造イメージ**：

```html
<div class="field">
    <label>テイスト</label>
    <div class="checklist">
        <label class="check-item" th:each="option : ${tasteOptions}">
            <input type="checkbox"
                   name="taste"
                   th:value="${option.id}"
                   th:checked="${selectedTasteIds != null and selectedTasteIds.contains(option.id)}">
            <span th:text="${option.label}">ベーシック</span>
        </label>
    </div>
</div>
```

**URL パラメータ**：`taste`（整数のID、複数指定可）

例：`/products/search?q=デスク&taste=1&taste=3`

#### 4.1.3 モバイル絞込モーダル

現状「モバイルでは現在、簡易版の絞込みUIを表示しています。」と表示している暫定実装のまま据え置く。
テイスト追加はデスクトップの絞込のみを対象とし、モバイルの完全対応は本 Issue のスコープ外とする。

#### 4.1.4 テイスト絞込のリセット

「リセット」リンク（`<a class="btn" th:href="@{/products/search(q=${keyword})}">リセット</a>`）で
テイスト選択も含め全絞込条件をクリアする（現状のリセット動作と同じ）。

#### 4.1.5 絞込条件のページ遷移時引き回し

ページネーションリンクの `href` に `taste=${selectedTasteIds}` を追加し、
ページ遷移後も選択したテイスト条件が保持されることを確認する。

---

## 5. 変更対象ファイル一覧

### 5.1 バックエンド (Java)

| ファイル | 変更内容 |
|---------|---------|| `SearchKeywordNormalizer.java` | **新規作成**。キーワードの正規化処理（全角数字・全角英字→半角、英字小文字化）を担当する `service/product/` 下のクラス || `ProductSearchCondition.java` | `tasteIds: List<Long>` フィールドの追加 |
| `ProductCategoryFilter.java` | `deskTasteIds` / `chairTasteIds` / `storageTasteIds` の3フィールドを削除し、単一の `tasteIds: List<Long>` に統合。`normalize()` / `isEmpty()` も合わせて修正 |
| `ProductFilterOptionsBundle.java` | 統合 `tasteOptions` フィールドに変更（既存3フィールドを置換） |
| `ProductFilterOptionService.java` | 統合 `tastes` テーブルからオプション取得・パラメータ正規化の実装。`buildDeskFilter()` / `buildChairFilter()` / `buildStorageFilter()` の `tasteIds` 引数を統一 |
| `ProductListSearchService.java` | `SearchKeywordNormalizer` を呼び出してキーワード正規化を行い、正規化済みキーワードを検索条件に反映。テイスト条件の受け取りを追加 |
| `CatalogController.java` | `searchResults` メソッドへ `taste` パラメータ追加。`desks()` / `chairs()` / `storages()` の `tasteIds` 渡し方を統合フィールドに変更 |
| `ProductMapper.java` | `selectActiveTasteOptions` から統合テーブル参照へ変更。`selectActiveDeskTasteOptions` / `selectActiveChairTasteOptions` / `selectActiveStorageTasteOptions` を削除 |

### 5.2 SQL マッパー

| ファイル | 変更内容 |
|---------|---------|
| `ProductMapper.xml` | `BaseProductWhere` のキーワード条件変更（対象フィールド追加・商品コード一致変更）、テイスト絞込 SQL 追加、統合テイストオプション取得クエリ追加 |

### 5.3 DB マイグレーション（新規作成）

| ファイル | 内容 |
|---------|------|
| `V2__unify_taste_master.sql` | `tastes` テーブル新設、既存テイストデータ移行、FK 変更、旧テーブル DROP |

### 5.4 テンプレート（Thymeleaf）

| ファイル | 変更内容 |
|---------|---------|
| `product-list-search-results.html` | テイスト絞込 UI 追加、ページネーションリンクおよびソート・表示件数フォームに `taste` パラメータ追加 |
| `fragments/layout/header.html` | 検索ボックスのプレースホルダー変更 |
| `product-list-category-desk.html` | テイスト候補参照を `deskTasteOptions` → `tasteOptions` に変更、選択値参照を `deskTasteIds` → `tasteIds` に変更 |
| `product-list-category-chair.html` | テイスト候補参照を `chairTasteOptions` → `tasteOptions` に変更、選択値参照を `chairTasteIds` → `tasteIds` に変更 |
| `product-list-category-storage.html` | テイスト候補参照を `storageTasteOptions` → `tasteOptions` に変更、選択値参照を `storageTasteIds` → `tasteIds` に変更 |

---

## 6. 非機能要件

### 6.1 性能要件

| 指標 | 目標値 |
|------|--------|
| 検索対象商品数 | 最大 1,000 件 |
| 一覧初回表示レスポンス（P95） | 2 秒以内 |

- 性能測定は商品 1,000 件の状態で負荷試験またはプロファイリングにより確認する
- 正規化関数（`TRANSLATE` 等）の適用でクエリ処理時間が増加した場合は、検索用正規化カラムのプリコンピュート方式を検討する

### 6.2 互換性

- 既存の URL（`/products/search?q=`）の変更はなく、`taste` パラメータは任意付加とする
- `taste` パラメータ未指定時の動作は現状と同一であること

### 6.3 テスト方針

以下のケースについてテストコードまたは動作確認を行う。

| # | テストケース |
|---|------------|
| 1 | 全角英字でキーワード入力 → 半角英字データにヒットする |
| 2 | 半角英字でキーワード入力 → 全角英字データにヒットする |
| 3 | 全角数字でキーワード入力 → 半角数字データにヒットする |
| 4 | 大文字英字でキーワード入力 → 小文字英字データにヒットする |
| 5 | 商品コードの完全一致でヒットする（例：`P0001-C01`） |
| 6 | 商品コードの前方一致でヒットする（例：`P0001`） |
| 7 | 商品コードの中間一致ではヒットしない（例：`0001` で P0001 がヒットしない） |
| 8 | 説明文に一致するキーワードでヒットする |
| 9 | シリーズバリエーション名に一致するキーワードでヒットする |
| 10 | テイスト絞込で選択したテイストの商品のみ表示される |
| 11 | テイスト複数選択時は OR 条件で絞込まれる |
| 12 | テイスト未選択時はテイストによる絞込が行われない |
| 13 | テイスト絞込後にページ遷移しても条件が維持される |
| 14 | キーワード未入力・テイスト未選択で全商品が表示される |

---

## 7. 対象外事項

- モバイル絞込モーダルへのテイスト追加
- オートコンプリート・サジェスト機能
- 検索ログ・分析機能
- ひらがな・カタカナ間の変換（例：「つくえ」→「デスク」といった意味検索）
- 検索結果画面でのカテゴリ固有フィルタ（天板形状・チェア機能・素材・サイズ等）の追加
- カテゴリ別一覧ページ固有のテイストフィルタのUI変更

---

## 8. 決定事項（要件補足）

要件定義の記載内容が不足・曖昧であった点について、以下のとおり決定した。

| # | 論点 | 決定内容 |
|---|------|---------|
| 1 | 商品コード以外の新規検索対象 | `variation_name`（シリーズバリエーション名）と `description`（説明文）を追加する |
| 2 | 完全一致と前方一致に優先順位はあるか | 検索結果の並び順に優先順位は設けない（SQLの1条件としてOR結合する）。並び順は既存のソートロジック（おすすめ/新着/価格）に従う |
| 3 | 正規化の実施場所 | Java サービス層でキーワードを正規化し、DB カラム値はクエリ実行時に PostgreSQL 関数で正規化する（プリコンピュート方式は性能問題発生時に検討） |
| 4 | テイストマスタの統一方針 | `tastes` 統合テーブルを新設し、`desk_tastes`・`chair_tastes`・`storage_tastes` の 3 テーブルを廃止する。同名テイストは単一エントリとして統合する |
| 5 | 検索結果画面のテイストフィルタのパラメータ名 | `taste`（整数ID、複数指定可）とする |
| 6 | ヘッダ検索ボックスのプレースホルダー | 「商品名・商品コードなど」に変更し、説明文・バリエーション名も検索対象であることを示唆する |
| 7 | Full-text search（全文検索エンジン）の利用 | 本 Issue では導入しない。PostgreSQL の ILIKE と TRANSLATE 関数レベルで対応する |
| 8 | 旧テイストテーブルの DROP タイミング | 旧テーブルのバックアップ・リネームは行わない。`V2__unify_taste_master.sql`（旧テーブル DROP を含む）はアプリケーションコードのデプロイと同時に適用する |
| 9 | DB マイグレーションツールの利用有無 | Flyway は導入しない。スキーマ変更 SQL は手動で実行する。ファイル名の `V2__` プレフィックスは適用順序の整理目的であり、自動実行は行わない |
| 10 | `ProductCategoryFilter` のテイストフィールド設計 | `deskTasteIds` / `chairTasteIds` / `storageTasteIds` の3フィールドを廃止し、単一の `tasteIds: List<Long>` に統合する |
| 11 | 全角半角正規化のスコープ | 半角カタカナ→全角カタカナの変換は対象外とする。正規化対象は全角英字・全角数字→半角への変換と英字小文字化のみとし、DB側は `TRANSLATE`（62文字マッピング）＋`ILIKE` で実現する || 12 | `ProductFilterOptionsBundle` のテイストフィールド設計 | `deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` の3フィールドを廃止し、単一の `tasteOptions: List<CategoryFilterOption>` に統合する。カテゴリ一覧ページも同フィールドを参照する |
| 13 | キーワード正規化の実装場所 | `service/product/` パッケージ下に `SearchKeywordNormalizer.java` を新規作成し、正規化ロジックをカプセル化する。`ProductListSearchService.buildCondition()` から呼び出す |