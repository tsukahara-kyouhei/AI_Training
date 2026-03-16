# DTO 定義書

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 詳細設計書 |
| 対象システム | office-order （オフィス家具ECサイト） |
| 作成日 | 2026-03-16 |
| バージョン | 1.0 |

---

## 凡例

| 記号 | 意味 |
|------|------|
| R | record（Java record class） |
| C | class（通常クラス） |
| ○ | 必須 / 常に値あり |
| △ | null 許容 |

---

## 1. 商品系 DTO

### 1.1 `ProductSearchCondition` （R）

商品一覧検索に使用する正規化済み検索条件。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `categoryId` | `String` | △ | カテゴリID（`desk` / `chair` / `storage`） |
| `keyword` | `String` | △ | 検索キーワード（正規化済み） |
| `inStockOnly` | `boolean` | ○ | 在庫ありのみフィルタ |
| `priceBands` | `List<PriceBand>` | ○ | 価格帯フィルタリスト（空なら全件） |
| `colorIds` | `List<Long>` | ○ | カラーIDフィルタリスト（空なら全件） |
| `categoryFilter` | `ProductCategoryFilter` | ○ | カテゴリ固有絞り込み条件 |
| `sort` | `ProductSort` | ○ | 並び順（`RECOMMENDED` / `NEW` / `PRICE_ASC` / `PRICE_DESC`） |
| `page` | `int` | ○ | ページ番号（1始まり） |
| `size` | `int` | ○ | 1ページ取得件数（15 / 30 / 60） |
| `saleStartFrom` | `OffsetDateTime` | △ | 販売開始日フィルタ（新着のみ使用） |

---

### 1.2 `ProductCardView` （R）

商品一覧・最近見た商品・関連商品で共通利用する商品カード表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productId` | `long` | ○ | 商品ID |
| `productName` | `String` | ○ | 商品名 |
| `priceText` | `String` | ○ | 価格表示文字列（例: `¥12,000`） |
| `colorCodes` | `List<String>` | ○ | カラーコードリスト（HEX形式） |
| `productCode` | `String` | ○ | デフォルト商品コード（最初のバリアント） |
| `inStock` | `boolean` | ○ | 在庫ありフラグ |
| `detailUrl` | `String` | ○ | 商品詳細URL |

---

### 1.3 `ProductDetailView` （R）

商品詳細画面全体の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productId` | `long` | ○ | 商品ID |
| `productName` | `String` | ○ | 商品名 |
| `description` | `String` | △ | 商品紹介文 |
| `categoryId` | `String` | ○ | カテゴリID |
| `hasVariation` | `boolean` | ○ | シリーズバリエーション有無 |
| `variationGroupId` | `Long` | △ | バリエーショングループID |
| `variationName` | `String` | △ | バリエーション名 |
| `taxRatePercent` | `BigDecimal` | ○ | 適用消費税率（例: 10.00） |
| `selectedPriceExcludingTax` | `BigDecimal` | ○ | 選択バリアントの税抜価格 |
| `selectedPriceExcludingTaxText` | `String` | ○ | 税抜価格表示文字列 |
| `selectedPriceIncludingTax` | `BigDecimal` | ○ | 選択バリアントの税込価格 |
| `selectedPriceIncludingTaxText` | `String` | ○ | 税込価格表示文字列 |
| `selectedAssemblyFee` | `BigDecimal` | ○ | 組立・設置費（0の場合は非対応） |
| `selectedAssemblyFeeText` | `String` | ○ | 組立・設置費表示文字列 |
| `selectedVariant` | `ProductVariantView` | ○ | 選択中バリアント情報 |
| `variants` | `List<ProductVariantView>` | ○ | 全バリアントリスト |
| `seriesLinks` | `List<ProductSeriesLinkView>` | ○ | バリエーション系列リンク（なければ空） |
| `relatedProducts` | `List<ProductCardView>` | ○ | 関連商品（なければ空） |
| `outOfStock` | `boolean` | ○ | 全バリアント在庫切れフラグ |

---

### 1.4 `ProductVariantView` （R）

商品詳細のカラー別バリアント情報。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productVariantId` | `long` | ○ | 商品バリアントID |
| `productCode` | `String` | ○ | 商品コード |
| `colorId` | `long` | ○ | カラーID |
| `colorName` | `String` | ○ | カラー名 |
| `colorCode` | `String` | ○ | カラーコード（HEX） |
| `unitPrice` | `BigDecimal` | ○ | 税抜価格 |
| `unitPriceText` | `String` | ○ | 税抜価格表示文字列 |
| `stockQuantity` | `int` | ○ | 在庫数 |

---

### 1.5 `ProductListPage` （R）

商品一覧の1ページ分の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `items` | `List<ProductCardView>` | ○ | 商品カードリスト |
| `totalCount` | `long` | ○ | 絞り込み後総件数 |
| `page` | `int` | ○ | 現在ページ番号 |
| `size` | `int` | ○ | 1ページ件数 |

---

### 1.6 `ProductFilterOptionsBundle` （R）

商品一覧に表示する絞り込み候補一式。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `colorOptions` | `List<ColorFilterOption>` | ○ | カラー絞り込み選択肢 |
| `deskTopShapeOptions` | `List<CategoryFilterOption>` | ○ | デスク天板形状選択肢 |
| `deskTasteOptions` | `List<CategoryFilterOption>` | ○ | デスクテイスト選択肢 |
| `chairFunctionOptions` | `List<CategoryFilterOption>` | ○ | チェア機能選択肢 |
| `chairMaterialOptions` | `List<CategoryFilterOption>` | ○ | チェア素材選択肢 |
| `chairTasteOptions` | `List<CategoryFilterOption>` | ○ | チェアテイスト選択肢 |
| `storageUsageOptions` | `List<CategoryFilterOption>` | ○ | 収納家具用途選択肢 |
| `storageTasteOptions` | `List<CategoryFilterOption>` | ○ | 収納家具テイスト選択肢 |

---

### 1.7 `ColorFilterOption` （R）

カラー絞り込みの選択肢1件。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `key` | `String` | ○ | カラーキー（URLパラメータ値） |
| `label` | `String` | ○ | カラー表示名 |
| `colorCode` | `String` | ○ | カラーコード（HEX） |

---

### 1.8 `CategoryFilterOption` （R）

カテゴリ固有絞り込みの選択肢1件。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `id` | `int` | ○ | 選択肢ID（マスタID） |
| `label` | `String` | ○ | 表示名 |

---

## 2. カート系 DTO

### 2.1 `CartCookieItem` （R）

Cookie に保存するカート1行分の最小情報。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productVariantId` | `long` | ○ | 商品バリアントID |
| `quantity` | `int` | ○ | 数量（1〜99） |
| `assemblyRequested` | `Boolean` | △ | 組立・設置希望フラグ |

---

### 2.2 `CartLineView` （R）

カート画面の1明細表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productVariantId` | `long` | ○ | 商品バリアントID |
| `productId` | `long` | ○ | 商品ID |
| `productName` | `String` | ○ | 商品名 |
| `productCode` | `String` | ○ | 商品コード |
| `colorName` | `String` | ○ | カラー名 |
| `unitPrice` | `BigDecimal` | ○ | 税抜単価 |
| `stockQuantity` | `int` | ○ | 在庫数 |
| `assemblyAvailable` | `boolean` | ○ | 組立・設置対応フラグ |
| `assemblyFeePerUnit` | `BigDecimal` | ○ | 1点あたり組立・設置費 |
| `assemblyRequested` | `boolean` | ○ | 組立・設置希望フラグ |
| `quantity` | `int` | ○ | 数量 |
| `detailUrl` | `String` | ○ | 商品詳細URL |

---

### 2.3 `CartSummaryView` （R）

カート全体の金額サマリー。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productSubtotal` | `BigDecimal` | ○ | 商品小計（税抜） |
| `assemblyFeeTotal` | `BigDecimal` | ○ | 組立・設置費合計（税抜） |
| `shippingFee` | `BigDecimal` | ○ | 送料（税抜） |
| `taxAmount` | `BigDecimal` | ○ | 消費税額 |
| `totalAmount` | `BigDecimal` | ○ | 税込合計金額 |

---

### 2.4 `CartView` （R）

カート画面全体の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `items` | `List<CartLineView>` | ○ | カート明細リスト |
| `itemTypeCount` | `int` | ○ | 商品種別数（行数） |
| `totalQuantity` | `int` | ○ | 合計数量 |
| `summary` | `CartSummaryView` | ○ | 金額サマリー |

---

## 3. 注文系 DTO

### 3.1 `CheckoutInputForm` （C, Serializable）

注文情報入力画面のフォームモデル。セッションに保存される。

| フィールド | 型 | 必須 | バリデーション | 説明 |
|-----------|------|------|-------------|------|
| `personalOrCorporate` | `String` | ○ | `personal` or `corporate` | 個人/法人区分（デフォルト `personal`） |
| `lastName` | `String` | ○ | max 50 | 姓 |
| `firstName` | `String` | ○ | max 50 | 名 |
| `lastNameKana` | `String` | ○ | max 50 / 全角カタカナ | 姓カナ |
| `firstNameKana` | `String` | ○ | max 50 / 全角カタカナ | 名カナ |
| `companyName` | `String` | △ | max 120 | 会社名（法人時は必須） |
| `departmentName` | `String` | △ | max 120 | 部署名 |
| `email` | `String` | ○ | `@Email` / max 254 | メールアドレス |
| `daytimePhone` | `String` | ○ | 10〜12桁数字 | 日中連絡可能電話番号 |
| `fax` | `String` | △ | 10〜12桁数字 | FAX番号 |
| `postalCodePart1` | `String` | ○ | 3桁数字 | 郵便番号前半 |
| `postalCodePart2` | `String` | ○ | 4桁数字 | 郵便番号後半 |
| `prefecture` | `String` | ○ | max 20 | 都道府県 |
| `city` | `String` | ○ | max 120 | 市区町村 |
| `addressLine` | `String` | ○ | max 255 | 番地・ビル名 |
| `deliveryFloor` | `String` | ○ | 数字 | お届け先階数 |
| `hasElevator` | `Boolean` | ○ | ― | エレベーター有無 |
| `paymentMethod` | `String` | ○ | `bank_transfer` / `cash_on_delivery` / `convenience_store` | 支払方法（デフォルト `bank_transfer`） |
| `selectedAdditionalAddressId` | `Long` | △ | ― | 選択した追加お届け先ID（会員のみ） |

---

### 3.2 `CheckoutMemberPrefill` （R）

注文情報入力フォームへの会員初期値セット。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `lastName` 〜 `hasElevator` | 各種 | ○ | 会員登録情報と同レイアウト |
| `additionalAddresses` | `List<MemberAdditionalAddressView>` | ○ | 追加お届け先選択肢 |

---

### 3.3 `OrderCompleteView` （R）

注文完了画面に表示する注文結果モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `orderNumber` | `String` | ○ | 注文番号 |
| `customerLastName` | `String` | ○ | 注文者姓 |
| `customerFirstName` | `String` | ○ | 注文者名 |
| `customerEmail` | `String` | ○ | 注文者メールアドレス |
| `shippingPostalCode` | `String` | ○ | 配送先郵便番号 |
| `shippingPrefecture` | `String` | ○ | 配送先都道府県 |
| `shippingCity` | `String` | ○ | 配送先市区町村 |
| `shippingAddressLine` | `String` | ○ | 配送先番地・ビル名 |
| `shippingFloor` | `Integer` | ○ | 配送先階数 |
| `shippingHasElevator` | `Boolean` | ○ | 配送先エレベーター有無 |
| `paymentMethod` | `String` | ○ | 支払方法コード |
| `convenienceStorePaymentNumber` | `String` | △ | コンビニ払込票番号 |
| `convenienceStorePaymentDueDate` | `String` | △ | コンビニ支払期限 |
| `subtotalAmount` | `BigDecimal` | ○ | 商品小計 |
| `assemblyFeeTotal` | `BigDecimal` | ○ | 組立・設置費合計 |
| `shippingFee` | `BigDecimal` | ○ | 送料 |
| `taxAmount` | `BigDecimal` | ○ | 消費税額 |
| `totalAmount` | `BigDecimal` | ○ | 税込合計金額 |

---

### 3.4 `OrderReorderItem` （R）

再購入処理でカートへ戻すための最小情報。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productVariantId` | `Long` | △ | 商品バリアントID（削除済みの場合 null） |
| `productCode` | `String` | ○ | 商品コード |
| `quantity` | `int` | ○ | 数量 |
| `assemblyRequested` | `Boolean` | △ | 組立・設置希望フラグ |

---

## 4. 会員系 DTO

### 4.1 `MemberSessionUser` （R）

ログインセッションに保持する会員の軽量情報。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `memberId` | `Long` | ○ | 会員ID |
| `email` | `String` | ○ | メールアドレス |
| `lastName` | `String` | ○ | 姓 |
| `firstName` | `String` | ○ | 名 |

---

### 4.2 `MemberRegisterForm` （C, Serializable）

会員登録フォームモデル（セッションに保存される）。

| フィールド | 型 | 必須 | バリデーション | 説明 |
|-----------|------|------|-------------|------|
| `personalOrCorporate` | `String` | ○ | `personal` or `corporate` | 個人/法人区分 |
| `lastName` 〜 `firstNameKana` | `String` | ○ | max 50 / カタカナ | 氏名・カナ |
| `companyName` | `String` | △ | max 120 | 会社名（法人時必須） |
| `departmentName` | `String` | △ | max 120 | 部署名 |
| `email` | `String` | ○ | `@Email` / max 254 | メールアドレス |
| `gender` | `String` | ○ | `male` / `female` / `no_answer` | 性別 |
| `anniversaryDate` | `LocalDate` | ○ | ― | 生年月日・記念日 |
| `password` | `String` | ○ | 8〜100文字（英数記号混在） | パスワード（平文） |
| `newsletterOptIn` | `Boolean` | ○ | ― | メールマガジン同意 |
| `postalCodePart1` / `Part2` | `String` | ○ | 3桁/4桁数字 | 郵便番号 |
| `prefecture` 〜 `addressLine` | `String` | ○ | max 各規定 | 住所 |
| `deliveryFloor` | `String` | ○ | 数字 | 階数 |
| `hasElevator` | `Boolean` | ○ | ― | エレベーター有無 |
| `daytimePhone` | `String` | ○ | 10〜12桁数字 | 日中連絡先 |
| `fax` | `String` | △ | 10〜12桁数字 | FAX |

---

### 4.3 `MemberOrderHistoryView` （R）

購入履歴一覧の1注文分の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `orderNumber` | `String` | ○ | 注文番号 |
| `orderDatetime` | `OffsetDateTime` | ○ | 注文日時 |
| `totalAmount` | `BigDecimal` | ○ | 税込合計金額 |
| `orderStatus` | `String` | ○ | 注文ステータスコード |

---

### 4.4 `MemberOrderDetailView` （R）

購入履歴詳細画面の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `orderNumber` | `String` | ○ | 注文番号 |
| `orderDatetime` | `OffsetDateTime` | ○ | 注文日時 |
| `orderStatus` | `String` | ○ | 注文ステータスコード |
| `subtotalAmount` | `BigDecimal` | ○ | 商品小計 |
| `assemblyFeeTotal` | `BigDecimal` | ○ | 組立・設置費合計 |
| `shippingFee` | `BigDecimal` | ○ | 送料 |
| `taxAmount` | `BigDecimal` | ○ | 消費税額 |
| `totalAmount` | `BigDecimal` | ○ | 税込合計金額 |
| `statusHistories` | `List<MemberOrderStatusHistoryView>` | ○ | ステータス変更履歴 |
| `items` | `List<MemberOrderItemDetailView>` | ○ | 注文明細リスト |

---

### 4.5 `MemberAdditionalAddressView` （R）

追加お届け先の表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `memberAddressId` | `Long` | ○ | 追加お届け先ID |
| `memberId` | `Long` | ○ | 会員ID |
| `lastName` 〜 `firstNameKana` | `String` | ○ | 宛名・カナ |
| `companyName` / `departmentName` | `String` | △ | 会社名・部署名 |
| `postalCode` | `String` | ○ | 郵便番号（7桁） |
| `prefecture` 〜 `addressLine` | `String` | ○ | 住所 |
| `deliveryFloor` | `Integer` | ○ | 階数 |
| `hasElevator` | `Boolean` | ○ | エレベーター有無 |
| `daytimePhone` | `String` | ○ | 日中連絡先 |
| `fax` | `String` | △ | FAX |
| `createdAt` | `OffsetDateTime` | ○ | 登録日時 |

---

### 4.6 `MemberFavoriteView` （R）

お気に入り一覧の商品パネル表示モデル。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `productId` | `long` | ○ | 商品ID |
| `productName` | `String` | ○ | 商品名 |
| `priceText` | `String` | ○ | 価格表示文字列 |
| `colorCodes` | `List<String>` | ○ | カラーコードリスト |
| `productCode` | `String` | ○ | 商品コード |
| `inStock` | `boolean` | ○ | 在庫ありフラグ |
| `detailUrl` | `String` | ○ | 商品詳細URL |

---

## 5. 認証系 DTO

### 5.1 `LoginForm` （C, Serializable）

ログイン入力フォーム。

| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `email` | `String` | ○ | メールアドレス |
| `password` | `String` | ○ | パスワード（平文） |
| `redirectPath` | `String` | △ | ログイン後の戻り先URL |
| `rememberEmail` | `boolean` | ○ | メールアドレス記憶フラグ |
