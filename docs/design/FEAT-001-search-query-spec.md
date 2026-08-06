# FEAT-001: 商品検索機能強化 設計書（検索クエリ仕様）

## 1. 目的

- 検索キーワードの解釈と SQL 生成ルールを明確化し、要件に沿った検索挙動を定義する。

## 2. 検索キーワードの正規化

- 入力文字列は検索実行時に以下の順序で正規化する。
  1. `null` / 空文字は検索条件なし扱い
  2. 前後の空白を除去
  3. 全角スペースを半角スペースへ変換
  4. 複数スペースは単一スペースに正規化
  5. 英字を小文字化
  6. 全角数字・英字・カタカナを半角に正規化
- 正規化済みキーワードを空白で分割し、複数語入力時は AND 検索を行う。

## 3. 検索論理

### 3.1 商品コード検索

- 各キーワードに対し、商品コードについては以下の条件で一致判定する。
  - 完全一致: `normalized_keyword == normalized_product_code`
  - 前方一致: `normalized_product_code LIKE normalized_keyword || '%'`
- 商品コード検索は OR 条件でテキスト検索と結合する。

### 3.2 テキスト検索

- 各キーワードに対し、以下の項目を部分一致検索する。
  - `product_name`
  - `variation_name`
  - `description`
- 部分一致は `LIKE '%' || normalized_keyword || '%'` で評価する。
- キーワードが複数ある場合、各キーワードを AND で結合する。

### 3.3 OR / AND の組み合わせ

- 検索語に対する個別評価:
  - 1つのキーワードであれば、商品コード検索 OR テキスト検索
  - 複数キーワードであれば、各キーワードの組み合わせを AND 条件で評価
- 例: `A B` を検索した場合、
  - (code matches A OR text matches A)
  - AND
  - (code matches B OR text matches B)

## 4. テイスト絞り込み

- 検索結果画面の「テイスト」は複数選択可能なチェックボックス形式とする。
- 選択されたテイストはキーワード検索と AND 条件で組み合わせる。
- 具体的には、検索結果は以下の条件を満たす。
  - キーワード検索にマッチする商品
  - かつ 選択したテイストのいずれかを持つ商品

## 5. SQL パラメータ設計

- 検索条件として `ProductRepository.buildSearchParams(...)` に以下を追加・拡張する。
  - `keywordLike`: 検索語の LIKE 用正規化文字列
  - `normalizedKeyword`: 商品コード前方一致 / 完全一致用文字列
  - `tasteIds`: 絞り込み用テイスト ID リスト
- `ProductMapper.xml` の WHERE 句で以下を組み合わせる。
  - 商品コード条件: `product_code = #{normalizedKeyword}` OR `product_code LIKE #{normalizedKeyword} || '%'`
  - テキスト条件: `product_name LIKE #{keywordLike}` OR `variation_name LIKE #{keywordLike}` OR `description LIKE #{keywordLike}`
  - すべての条件は AND / OR 論理を正しく構成する。

## 6. 表記ゆれ吸収

- 英字は大文字小文字を区別しないため、検索語と DB 両方を正規化して比較する。
- 数字・英字・カタカナの全角半角差異を吸収する。
- これらの正規化は検索実行時にシステム内部で行い、DB 側での比較は normalized 値に対して行う。

## 7. 既存検索条件との互換性

- 既存の `ProductRepository` では `keywordLike` を利用した部分一致検索を行っている。
- 本機能強化ではその設計を拡張しつつ、既存のカテゴリ/価格/カラー絞り込みロジックを維持する。

## 8. 性能留意点

- 検索対象件数は最大 1000 件程度を想定するため、検索対象フィールドと LIKE 条件での実行計画を考慮する。
- 可能であれば `ILIKE` や正規化済み比較条件のインデックス利用を検討する。
