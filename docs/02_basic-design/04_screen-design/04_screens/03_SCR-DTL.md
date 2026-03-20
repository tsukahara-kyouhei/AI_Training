# 商品詳細

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-DTL |
| テンプレート | `pages/product-detail.html` |
| URL | `GET /products/{id}` |
| Controller | `CatalogController#productDetail()` |
| 認証 | 不要（お気に入り操作はログイン必要） |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| 商品画像 | バリアント別サムネイル一覧 + メイン画像 |
| 商品情報 | 商品名・税抜価格・税込価格・説明文 |
| カラー選択 | ラジオボタン（カラースウォッチ付き） |
| 数量選択 | セレクトボックス（1〜5） |
| カート追加ボタン | 在庫切れ時は無効化 |
| お気に入りボタン | トグル（追加/削除） |
| 組立・設置費 | 対応商品のみ表示 |
| シリーズリンク | 同一バリエーショングループの別商品へのリンク |
| おすすめ関連商品 | レコメンド商品カード（最大4件） |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `detail` | `ProductDetailView` | 商品詳細情報（下記） |
| `detailPagePath` | `String` | 現在ページパス |
| `isFavorite` | `boolean` | お気に入り登録済みか |
| `cartMessage` | `String` | カート追加成功メッセージ（Flash） |
| `cartError` | `String` | カートエラー（Flash） |
| `favoriteLimitError` | `String` | お気に入り上限エラー（Flash） |

### ProductDetailView の主要プロパティ

| プロパティ | 型 | 説明 |
|---|---|---|
| `productId` | `long` | 商品ID |
| `productName` | `String` | 商品名 |
| `description` | `String` | 説明文 |
| `selectedVariant` | `ProductVariantView` | 選択中のバリアント |
| `variants` | `List<ProductVariantView>` | 全バリアント |
| `selectedPriceExcludingTaxText` | `String` | 税抜価格表示 |
| `selectedPriceIncludingTaxText` | `String` | 税込価格表示 |
| `taxRatePercent` | `BigDecimal` | 消費税率 |
| `outOfStock` | `boolean` | 在庫切れフラグ |
| `hasVariation` | `boolean` | バリエーション有無 |
| `seriesLinks` | `List<SeriesLinkView>` | シリーズリンク |
| `selectedAssemblyFee` | `BigDecimal` | 組立費 |

---

## 4. フォーム

### 4.1 カート追加フォーム

| 項目 | 値 |
|---|---|
| action | `POST /cart/items` |
| `productVariantId` | hidden（選択中バリアントID） |
| `quantity` | select（1〜5） |
| `redirect` | hidden（現在ページパス） |

### 4.2 お気に入りトグルフォーム

| 項目 | 値 |
|---|---|
| action | `POST /products/{productId}/favorite` |
| `action` | hidden（削除時: "remove"） |
| `redirect` | hidden（現在ページパス） |

---

## 5. 表示条件

- 組立費: `selectedAssemblyFee > 0` の場合のみ表示
- シリーズリンク: `hasVariation = true` かつシリーズ商品が2件以上の場合
- お気に入りボタン: ログイン済みの場合のみ表示
- カート追加ボタン: `outOfStock = true` の場合は無効化
