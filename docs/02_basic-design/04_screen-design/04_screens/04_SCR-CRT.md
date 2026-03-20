# カート

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-CRT |
| テンプレート | `pages/cart.html` |
| URL | `GET /cart` |
| Controller | `CartController#cart()` |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| カートヘッダー | 「カート（X点）」|
| 一括削除ボタン | カート全品削除 |
| 商品明細テーブル | 商品名・カラー・単価・数量・組立指定・削除 |
| 金額サマリー | 小計・組立費・送料・消費税・合計 |
| 購入手続きボタン | ログイン状態で遷移先が変わる |
| カート空表示 | カートが空の場合のメッセージ |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `cart` | `CartView` | カート内容 |
| `isCartEmpty` | `boolean` | カート空フラグ |
| `isLoggedIn` | `boolean` | ログイン状態（`AuthModelAdvice` で全画面共通設定） |
| `cartMessage` | `String` | 成功メッセージ（Flash） |
| `cartError` | `String` | エラーメッセージ（Flash） |

### CartView の構造

| プロパティ | 説明 |
|---|---|
| `totalQuantity` | 商品点数 |
| `items[]` | カート明細（productName, productCode, colorName, unitPriceText, quantity, assemblyAvailable, assemblyRequested, assemblyFeePerUnitText） |
| `summary` | 金額サマリー（productSubtotalText, assemblyFeeTotalText, shippingFeeText, taxAmountText, totalAmountText） |

---

## 4. フォーム

| 操作 | action | method | パラメータ |
|---|---|---|---|
| 数量変更 | `POST /cart/items/{id}/update` | POST | `quantity` (1〜99) |
| 商品削除 | `POST /cart/items/{id}/delete` | POST | - |
| 全品削除 | `POST /cart/clear` | POST | - |

---

## 5. 表示条件・遷移

- カート空時: 明細テーブル非表示、「カートに商品がありません」表示
- 組立チェック: `assemblyAvailable = true` の商品のみ表示
- 購入手続きボタンの遷移先:
  - ログイン済み: `/checkout/input`
  - 未ログイン: `/checkout/method`
