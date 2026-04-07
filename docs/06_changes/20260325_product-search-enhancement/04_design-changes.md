# 商品検索機能強化 - 設計検討

> **ステータス**: � 設計確定（設計書反映済み）
> **前提**: 要件定義書（02_requirements.md v1.0）が確定済みであること

---

## 1. 概要

要件定義の内容を実現するために、どの設計ドキュメントをどのように修正・追加するかを一覧化する。

---

## 2. 修正対象ドキュメント一覧

### 2.1 基本設計（02_basic-design）

| # | 対象ドキュメント | 変更種別 | 変更概要 | 対応要件 |
|---|---|---|---|---|
| D-001 | [01_feature-list.md](../../02_basic-design/01_feature-list.md) | 修正 | F-CAT-002「キーワード検索」の概要を更新。現行「商品名・説明文でキーワード検索」→「商品名・商品コード・バリエーション名・説明文でキーワード検索。全角半角・大文字小文字を区別しない。テイスト絞込み対応」 | FR-001〜FR-024, FR-030 |
| D-002 | [04_screens/02_SCR-CAT.md](../../02_basic-design/04_screen-design/04_screens/02_SCR-CAT.md) | 修正 | §5 フォーム要素に検索結果ページ用のテイストフィルターパラメータ（`deskTaste`, `chairTaste`, `storageTaste`）を追加。§4 カテゴリ専用フィルターに「検索結果」パターンを追加（3カテゴリのテイストをアコーディオン形式で表示する旨を記載） | FR-030〜FR-033 |
| D-003 | [04_screen-design/03_common-layout.md](../../02_basic-design/04_screen-design/03_common-layout.md) | 修正 | ヘッダ検索ボックスの仕様変更: `placeholder` と `aria-label` を「商品名・商品コード・キーワード」に変更、`maxlength="100"` を追記 | FR-041, FR-046 |
| D-004 | [05_api-design/01_url-list.md](../../02_basic-design/05_api-design/01_url-list.md) | 修正 | `GET /products/search` の行の概要を更新。リクエストパラメータにテイストフィルター（`deskTaste`, `chairTaste`, `storageTaste`）を追記 | FR-030〜FR-033 |
| D-005 | [06_data-design/04_crud-diagram.md](../../02_basic-design/06_data-design/04_crud-diagram.md) | 修正 | §2 商品系テーブルの「F-CAT: 商品一覧・検索」行と §5 マスタ・バッチ系テーブルの「F-CAT: 商品フィルタ」行に、検索結果画面でのテイストマスタ参照（R）を補足として追記。※テーブル・操作種別そのものは変更なし（既にRが記載済み） | FR-030〜FR-033 |

### 2.2 詳細設計（03_detailed-design）

| # | 対象ドキュメント | 変更種別 | 変更概要 | 対応要件 |
|---|---|---|---|---|
| D-010 | [03_business-logic/01_BL-product.md](../../03_detailed-design/03_business-logic/01_BL-product.md) | 修正 | §2.3 `search(condition)` のProductSearchConditionフィールド表に以下を追記: テイスト絞込み用フィールドがcategoryId未指定時にも利用される旨の注記（検索結果画面用） | FR-030〜FR-033 |
| D-011 | [03_business-logic/01_BL-product.md](../../03_detailed-design/03_business-logic/01_BL-product.md) | 修正 | §2.3 に「表記ゆれ正規化」の仕様を追加。Java側: 全角英数カナを半角へ変換してLIKE文字列を生成。SQL側: `translate()` でDB上のカラム値も正規化して比較する | FR-021〜FR-024 |
| D-012 | [04_data-access/02_sql/01_DAO-ProductMapper.md](../../03_detailed-design/04_data-access/02_sql/01_DAO-ProductMapper.md) | 修正 | §2 `countProducts / selectProducts` のパラメータ表を更新。キーワード検索のWHERE条件変更（商品コード前方一致、`products.variation_name` ・`products.description` の部分一致追加、`translate()` 正規化）、テイストフィルター追加 の内容を反映 | FR-001, FR-011〜FR-012, FR-021〜FR-024, FR-030〜FR-033 |
| D-013 | [01_class-design/01_package-structure.md](../../03_detailed-design/01_class-design/01_package-structure.md) | 修正 | `common/` 配下に全角半角正規化ユーティリティクラスを追記 | FR-024 |
| D-014 | [01_class-design/02_class-diagram.md](../../03_detailed-design/01_class-design/02_class-diagram.md) | 修正なし | CatalogController → ProductService → ProductRepository → ProductMapper の依存構造は変更なし。新規クラス追加はユーティリティのみで依存関係図に影響しないため更新不要 | - |

### 2.3 非機能設計（04_non-functional）

| # | 対象ドキュメント | 変更種別 | 変更概要 | 対応要件 |
|---|---|---|---|---|
| D-020 | なし | なし | 性能目標（NFR-001, NFR-002）は要件定義書 §5 にて管理。既存の非機能設計ドキュメントへの追記は不要（ログ・環境・デプロイに変更なし） | - |

### 2.4 テスト（05_testing）

| # | 対象ドキュメント | 変更種別 | 変更概要 | 対応要件 |
|---|---|---|---|---|
| D-030 | [01_test-strategy.md](../../05_testing/01_test-strategy.md) | 修正なし | テスト戦略（JUnit5 + Testcontainers方針）自体は変更なし。テストケースは開発計画にて整理 | - |

---

## 3. 変更詳細

以下、ドキュメント修正の影響を受けるソースコードの対応箇所を設計ドキュメントとの対応付きで整理する。

### 3.1 キーワード検索条件の変更（D-012）

**現行（BaseProductWhere内）:**

```xml
<if test="keywordLike != null and keywordLike != ''">
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

**変更後の方針:**

| 対象 | 検索方式 | JOINまたはEXISTS | 正規化 |
|---|---|---|---|
| `products.product_name` | 部分一致（ILIKE） | 本体テーブル | `translate()` で全角→半角変換して比較 |
| `product_variants.product_code` | **前方一致に変更**（`ILIKE 'keyword%'`） | EXISTS副問合せ | `translate()` で全角→半角変換して比較 |
| `products.description` | 部分一致（ILIKE） | 本体テーブル | `translate()` で全角→半角変換して比較 |
| `products.variation_name` | 部分一致（ILIKE） | 本体テーブル（NULLの商品はこの条件をスキップ） | `translate()` で全角→半角変換して比較 |

- 商品コード用のLIKE文字列は別パラメータ（`keywordPrefix`）で `'keyword%'` 形式にする
- テキスト検索用のLIKE文字列は既存の `keywordLike` で `'%keyword%'` 形式を継続
- `toKeywordPrefix()` でも既存の特殊文字エスケープ処理（`%` `_` のエスケープ）を適用する（FR-043）
- エスケープ文字は `\`。対象: `\` → `\\`, `%` → `\%`, `_` → `\_`。処理順序: 正規化→エスケープ→LIKE文字列生成
- 現行の `toKeywordLike()` にはエスケープ処理が未実装のため、本改修で新規に追加する
- SQL側で各 ILIKE 句に `ESCAPE '\'` を付与する
- `translate()` に渡す変換テーブル（全角→半角マッピング）はSQL fragment（`<sql id="FullWidthChars">` / `<sql id="HalfWidthChars">`）として定義
- `variation_name` は `products` テーブルに直接存在するため、FROM句へのJOIN追加は不要
- 4条件はOR結合

### 3.2 表記ゆれ正規化（D-011, D-013）

**Java側（アプリケーション層）:**

- `common/` パッケージに正規化ユーティリティを追加
  - 全角英字（Ａ-Ｚ, ａ-ｚ）→ 半角（A-Z, a-z）
  - 全角数字（０-９）→ 半角（0-9）
  - 半角カタカナ → 全角カタカナ（ｱ→ア, ｶﾞ→ガ 等。濁音・半濁音の2文字結合も全角1文字に変換）
- `ProductRepository#toKeywordLike()` 内でユーティリティを呼び出し、正規化済みキーワードでLIKE文字列を生成
- 新規に `toKeywordPrefix()` メソッドを追加し、商品コード検索用の前方一致文字列を生成

**SQL側:**

- `BaseProductWhere` fragment内で `translate()` を使用し、カラム値を正規化して比較
  - 英数字: 全角→半角に変換
  - カタカナ: 半角清音＋長音→全角に変換（1:1変換可能な文字のみ。濁音結合文字は対象外、DBデータは通常全角格納のため問題なし）

```sql
-- 例: product_nameの比較（英数字は全角→半角、カタカナ清音は半角→全角）
translate(p.product_name,
  'ＡＢＣ...Ｚａｂｃ...ｚ０１２...９ｱｲｳ...ﾝｰ',
  'ABC...Za bc...z012...9アイウ...ンー'
) ILIKE #{keywordLike}
```

- 変換テーブル文字列はSQL fragment（`<sql id="NormalizeFrom">` / `<sql id="NormalizeTo">`）として1箇所に定義し、重複を排除

### 3.3 テイスト絞込み（D-002, D-004, D-010, D-012）

**画面（テンプレート）:**

- `product-list-search-results.html` のサイドバーフィルターフォームにテイスト絞込み欄を追加
  - 「デスクのテイスト」「チェアのテイスト」「収納家具のテイスト」の3グループ
  - `<details>` / `<summary>` でアコーディオン（折りたたみ）UIを実装
  - 各テイストをチェックボックスで選択（複数選択可）
  - パラメータ名: `deskTaste`, `chairTaste`, `storageTaste`
  - 初期状態: 折りたたみ（closed）

**コントローラー:**

- `CatalogController#searchResults()` に `deskTaste`, `chairTaste`, `storageTaste` のリクエストパラメータを追加
- テイスト選択肢をModelに追加（`deskTasteOptions`, `chairTasteOptions`, `storageTasteOptions`）
- テイスト選択状態をModelに追加（`selectedDeskTasteIds`, `selectedChairTasteIds`, `selectedStorageTasteIds`）— テンプレートでチェックボックスの選択復元に使用

**リポジトリ:**

- `ProductRepository#buildSearchParams()` で、検索結果画面（`categoryId == null`）の場合にもテイストフィルターフラグを設定するロジックを追加
- 現行は `category == ProductCategory.DESK` 等の条件下でのみテイストフィルターが有効だが、カテゴリ未指定でもテイスト選択があればフィルターを適用する
- `hasTasteFilter` の計算: `(category == null) && (!deskTasteIds.isEmpty() || !chairTasteIds.isEmpty() || !storageTasteIds.isEmpty())`
- カテゴリ別一覧では `hasTasteFilter` は常に `false`（従来の `hasDeskFilter` 等で制御）

**SQL（ProductMapper.xml）:**

- テイスト絞込み選択時、3カテゴリの属性テーブルにOR結合でEXISTS条件を追加
- テイスト絞込みが1つでも選択されている場合、テイスト属性がNULLの商品は結果から除外される
- 概要ロジック:

```sql
-- テイストフィルターが選択されている場合（<trim prefixOverrides="OR"> で先頭OR除去）
AND (
    EXISTS (SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id
            AND da.taste_id IN (...deskTasteIds...))
    OR EXISTS (SELECT 1 FROM product_chair_attributes ca
              WHERE ca.product_id = p.product_id
              AND ca.taste_id IN (...chairTasteIds...))
    OR EXISTS (SELECT 1 FROM product_storage_attributes sa
              WHERE sa.product_id = p.product_id
              AND sa.taste_id IN (...storageTasteIds...))
)
```

- 未選択カテゴリのテイストはOR条件に含めない（例: デスクのテイストのみ選択時はデスクのEXISTSのみ）

### 3.4 ヘッダ検索ボックスの変更（D-003）

- `header.html` の検索ボックスに `maxlength="100"` を追加
- サーバー側でもキーワードを100文字で切り詰めるバリデーションを追加（`CatalogController#searchResults()` にて実施）
- `placeholder` を「商品名・商品コード」→「商品名・商品コード・キーワード」に変更
- `aria-label` も同様に変更

---

## 4. 影響を受けるソースファイル一覧

設計ドキュメント変更に伴い修正が必要なソースファイルの一覧。詳細は開発計画（05_development-plan.md）にて整理する。

| # | ファイル | 変更概要 | 対応設計変更 |
|---|---|---|---|
| S-001 | `ProductMapper.xml` | BaseProductWhere変更（商品コード前方一致、バリエーション名・説明文追加、translate()正規化、テイスト絞込み） | D-012 |
| S-002 | `ProductRepository.java` | `toKeywordLike()` に正規化+エスケープ処理追加、`toKeywordPrefix()` 新規、`buildSearchParams()` に `hasTasteFilter` フラグ追加 | D-011, D-012 |
| S-003 | `CatalogController.java` | `searchResults()` にテイストパラメータ追加、Model にテイスト選択肢＋選択状態追加（`deskTasteOptions`/`selectedDeskTasteIds` 等 各3変数）、キーワード100文字切り詰め | D-010, D-012 |
| S-004 | `ProductSearchCondition.java` | 変更なし（既存フィールドで対応可能。`categoryFilter` にテイストIDを格納） | - |
| S-005 | `header.html` | `placeholder` と `aria-label` の文言変更、`maxlength="100"` 追加 | D-003 |
| S-006 | `product-list-search-results.html` | テイストフィルターの追加箇所は3箇所: ①サイドバーフィルターフォームにアコーディオン3グループ追加、②ソート・件数変更フォームにテイスト hidden input（`deskTaste`/`chairTaste`/`storageTaste` を `th:each` ループ）追加、③ページネーションリンクの `th:href` にテイストパラメータ追加 | D-002 |
| S-007 | 正規化ユーティリティ（新規） | 検索キーワード正規化処理クラス（`common/` 配下）。英数字は全角→半角、カタカナは半角→全角（濁音結合含む） | D-013 |
| S-008 | `ProductFilterOptionService.java` | `buildSearchFilter()` メソッド新規追加（検索結果画面用テイスト入力を `ProductCategoryFilter` へ構築） | D-010 |
| S-009 | `ProductListSearchService.java` | `searchResults()` から `buildSearchFilter()` 経由で `ProductCategoryFilter` を構築し、カテゴリ固有条件付き `buildCondition()` を呼び出す | D-010 |

---

## 5. 新規作成ドキュメント

| # | ドキュメントパス | 概要 |
|---|---|---|
| N-001 | なし | 新規設計ドキュメントの作成は不要（既存ドキュメントの修正で対応可能） |

---

## 6. 変更なし（対象外）の確認

| ドキュメント | 対象外の理由 |
|---|---|
| [02_basic-design/02_system-architecture.md](../../02_basic-design/02_system-architecture.md) | システム構成に変更なし（既存アーキテクチャ内の実装） |
| [02_basic-design/03_code-definition.md](../../02_basic-design/03_code-definition.md) | コード体系に変更なし |
| [02_basic-design/04_screen-design/01_screen-list.md](../../02_basic-design/04_screen-design/01_screen-list.md) | 画面一覧に変更なし（SCR-CAT-SEARCHの追加・削除なし） |
| [02_basic-design/04_screen-design/02_screen-transition.md](../../02_basic-design/04_screen-design/02_screen-transition.md) | 画面遷移パターンに変更なし |
| [02_basic-design/06_data-design/01_er-diagram.md](../../02_basic-design/06_data-design/01_er-diagram.md) | テーブル追加・カラム追加なし |
| [02_basic-design/06_data-design/02_tables/](../../02_basic-design/06_data-design/02_tables/) | テーブル定義変更なし |
| [02_basic-design/06_data-design/03_master-data.md](../../02_basic-design/06_data-design/03_master-data.md) | マスタデータ変更なし |
| [03_detailed-design/01_class-design/02_class-diagram.md](../../03_detailed-design/01_class-design/02_class-diagram.md) | 依存関係に変更なし（補足: D-014参照） |
| [03_detailed-design/02_sequence/](../../03_detailed-design/02_sequence/) | シーケンス図の新規追加・修正なし（処理フローの大枠は同一） |
| [03_detailed-design/03_business-logic/02_BL-cart.md〜06_BL-announcement.md](../../03_detailed-design/03_business-logic/) | 商品検索以外のビジネスロジックに変更なし |
| [03_detailed-design/04_data-access/02_sql/02〜07_DAO-*.md](../../03_detailed-design/04_data-access/02_sql/) | ProductMapper以外のMapper変更なし |
| [03_detailed-design/05_batch-design/](../../03_detailed-design/05_batch-design/) | バッチ処理に変更なし |
| [03_detailed-design/06_mail-design/](../../03_detailed-design/06_mail-design/) | メール設計に変更なし |
| [03_detailed-design/07_security-design/](../../03_detailed-design/07_security-design/) | セキュリティ設計に変更なし |
| [03_detailed-design/08_error-handling/](../../03_detailed-design/08_error-handling/) | エラーハンドリング方針に変更なし |
| [04_non-functional/](../../04_non-functional/) | 非機能設計（ログ・環境・デプロイ）に変更なし |
| [05_testing/01_test-strategy.md](../../05_testing/01_test-strategy.md) | テスト戦略方針に変更なし |

---

## 7. 変更履歴

| 日付 | 版数 | 変更内容 | 変更者 |
|---|---|---|---|
| 2026-03-25 | 0.1 | 初版作成 | - |
| 2026-04-07 | 1.0 | 設計書反映完了（D-001〜D-013） | - |
| 2026-04-07 | 1.1 | 実装レビュー指摘7件反映（エスケープ仕様追加、SQL trim修正、hasTasteFilterロジック定義、buildSearchFilter()追加、キーワード制限箇所確定、translate() fragment方式統一、モバイル対応記載） | - |
| 2026-04-07 | 1.2 | 開発レビュー5件反映（Q-1確認: products.variation_nameで正、Q-5: カタカナ正規化を半角→全角に変更（A案）、Q-2: テイスト選択状態Model属性追加、Q-3: テンプレートhidden input 3箇所具体化、SQL fragment名をNormalizeFrom/NormalizeToに変更） | - |
