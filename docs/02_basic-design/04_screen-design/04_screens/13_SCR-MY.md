# マイページ

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-MY |
| URL | `/mypage/*` |
| Controller | `MyPageController` |
| 認証 | 全ページ必須（`requireLoginMember()` でセッション確認、未ログイン時 401） |

### 共通サイドナビゲーション

全マイページに共通で以下のナビを表示。

| リンク | URL |
|---|---|
| 購入履歴 | `/mypage/orders` |
| お気に入り | `/mypage/favorites` |
| 会員情報変更 | `/mypage/profile` |
| お届け先管理 | `/mypage/addresses` |
| 退会 | `/mypage/withdraw` |
| ログアウト | `POST /logout` |

---

## 2. 購入履歴一覧

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-orders-list.html` |
| URL | `GET /mypage/orders?page={n}` |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `orders` | `List` | 注文サマリ一覧 |
| `currentPage` | `int` | 現在ページ |
| `totalPages` | `int` | 総ページ数 |
| `cartMessage` | `String` | 再購入成功メッセージ（Flash） |
| `cartError` | `String` | 再購入エラーメッセージ（Flash） |

### 表示項目×操作

| 項目 | メソッド |
|---|---|
| 注文番号 | `order.orderNumber()` → 詳細リンク |
| 注文日時 | `order.orderDatetimeDisplay()` |
| 合計金額 | `order.totalAmountText()` |
| ステータス | `order.orderStatusLabel()` |
| 再購入 | `POST /mypage/orders/{orderNumber}/reorder` → `/cart` へリダイレクト |

### ページネーション

- `totalPages > 1` の場合のみ表示
- 10件/ページ
- 前へ/次へボタン + ページ番号

### 表示条件

- 注文が空: 「購入履歴はありません」表示
- `cartMessage` 存在: 成功アラート表示
- `cartError` 存在: エラーアラート表示

---

## 3. 購入履歴詳細

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-order-detail.html` |
| URL | `GET /mypage/orders/{orderNumber}` |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `orderDetail` | `OrderDetailView` | 注文詳細情報 |

### 表示項目

**注文基本情報:**

| 項目 | メソッド |
|---|---|
| 注文番号 | `orderDetail.orderNumber()` |
| 注文日時 | `orderDetail.orderDatetimeDisplay()` |
| ステータス | `orderDetail.orderStatusLabel()` |

**ステータス履歴:** `th:each="history : ${orderDetail.statusHistories()}"`

| 項目 | メソッド |
|---|---|
| 変更日時 | `history.changedAtDisplay()` |
| ステータス | `history.statusLabel()` |

**注文商品明細:** `th:each="item : ${orderDetail.items()}"`

| 項目 | メソッド |
|---|---|
| 商品名 | `item.productName()` |
| カラー/品番 | `item.colorName() + item.productCode()` |
| 単価 | `item.unitPriceText()` |
| 組立費 | `item.assemblyFeeText()` |
| 数量 | `item.quantity()` |
| 小計 | `item.lineSubtotalText()` |

**金額サマリ:**

| 項目 | メソッド |
|---|---|
| 商品合計 | `orderDetail.subtotalAmountText()` |
| 組立費合計 | `orderDetail.assemblyFeeTotalText()` |
| 送料 | `orderDetail.shippingFeeText()` |
| 消費税 | `orderDetail.taxAmountText()` |
| 合計 | `orderDetail.totalAmountText()` |

### 操作

- 再購入ボタン: `POST /mypage/orders/{orderNumber}/reorder`
- 注文が存在しない場合: HTTP 404

---

## 4. お気に入り一覧

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-favorites.html` |
| URL | `GET /mypage/favorites?page={n}` |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `favorites` | `List` | お気に入り商品リスト |
| `favoriteCount` | `int` | 現在の登録数 |
| `favoriteLimit` | `int` | 上限数（`MemberService.FAVORITES_LIMIT`） |
| `favoriteLimitError` | `String` | 上限エラーメッセージ |
| `currentPage` | `int` | 現在ページ |
| `totalPages` | `int` | 総ページ数 |

### 表示項目

- 登録数/上限表示: 「登録数: N / M」
- 商品カード: `fragments/common/product-card :: productCardByView` フラグメントで表示
- お気に入り削除: `POST /products/{productId}/favorite`（`action=remove`）

### 表示条件

- お気に入りが空: 「お気に入りは登録されていません」+ 「商品を探す」リンク
- `favoriteLimitError` 存在: エラーアラート表示

---

## 5. 会員情報変更

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-profile-edit.html` |
| URL | `GET /mypage/profile`、`POST /mypage/profile` |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `profileForm` | `MemberProfileEditForm` | プロフィール編集フォーム |
| `profileUpdatedMessage` | `String` | 更新成功メッセージ（Flash） |

### フォーム要素

`th:object="${profileForm}"` でバインド。フィールド構成はSCR-REG01（会員登録）と同一。

| フィールド名 | 型 | 説明 |
|---|---|---|
| `personalOrCorporate` | radio | 個人/法人 |
| `lastName` / `firstName` | text | 姓名 |
| `lastNameKana` / `firstNameKana` | text | フリガナ |
| `companyName` | text | 会社名 |
| `departmentName` | text | 部署名 |
| `email` | email | メールアドレス |
| `gender` | radio | 性別 |
| `anniversaryDate` | date | 生年月日 |
| `newsletterOptIn` | radio | メルマガ |
| `postalCodePart1/2` | text | 郵便番号 |
| `prefecture` / `city` / `addressLine` | text | 住所 |
| `deliveryFloor` | number | 階数 |
| `hasElevator` | radio | EV有無 |
| `daytimePhone` / `fax` | tel | 電話/FAX |

### バリデーション

- `@Valid MemberProfileEditForm` + `BindingResult`
- 法人時 `companyName` 必須（`validateProfileFormForMemberType()`）
- メール重複時: `DuplicateEmailException` をキャッチしフィールドエラー追加
- 更新成功時: セッションの会員情報も更新（`memberSessionService.login()`）

---

## 6. お届け先管理

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-addresses.html`、`pages/mypage-address-form.html` |
| URL | `/mypage/addresses*` |

### 6-1. お届け先一覧

| URL | `GET /mypage/addresses?page={n}` |
|---|---|

#### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `addresses` | `List` | お届け先リスト |
| `addressCount` | `int` | 現在の登録数 |
| `addressLimit` | `int` | 上限数（`MemberService.ADDITIONAL_ADDRESS_LIMIT`） |
| `addressLimitReached` | `boolean` | 上限到達フラグ |
| `currentPage` / `totalPages` | `int` | ページネーション |

#### 表示項目

| 項目 | メソッド |
|---|---|
| 氏名 | `address.fullName()` |
| 住所 | `address.addressSummary()` |
| 電話 | `address.daytimePhone()` |
| 編集 | `/mypage/addresses/{id}/edit` リンク |
| 削除 | `POST /mypage/addresses/{id}/delete` |

#### 表示条件

- 上限未到達: 「追加」ボタン有効（`/mypage/addresses/new`）
- 上限到達: 「追加」ボタン無効化
- `addressLimitError` 存在: エラーアラート表示

### 6-2. お届け先追加・編集フォーム

| URL | `GET /mypage/addresses/new`、`GET /mypage/addresses/{id}/edit` |
|---|---|

#### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `addressForm` | `MemberAdditionalAddressForm` | お届け先フォーム |
| `formAction` | `String` | 送信先URL（追加: `/mypage/addresses`、編集: `/mypage/addresses/{id}`） |
| `cancelUrl` | `String` | キャンセル先 |
| `isEditMode` | `boolean` | 編集モードフラグ |

#### フォーム要素

| フィールド名 | 型 | 説明 |
|---|---|---|
| `lastName` / `firstName` | text | 姓名 |
| `lastNameKana` / `firstNameKana` | text | フリガナ |
| `companyName` | text | 会社名 |
| `departmentName` | text | 部署名 |
| `postalCodePart1/2` | text | 郵便番号 |
| `prefecture` / `city` / `addressLine` | text | 住所 |
| `deliveryFloor` | number | 階数 |
| `hasElevator` | radio | EV有無 |
| `daytimePhone` / `fax` | tel | 電話/FAX |

#### バリデーション

- `@Valid MemberAdditionalAddressForm` + `BindingResult`
- 法人会員の場合 `companyName` 必須（`validateAddressFormForMemberType()`）
- 追加時: `AddressLimitExceededException` キャッチ

---

## 7. 退会

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/mypage-withdraw.html` |
| URL | `GET /mypage/withdraw`、`POST /mypage/withdraw` |

### 画面構成

- 退会確認メッセージ（静的テキスト）
- 退会ボタン: `POST /mypage/withdraw`
- キャンセル: `/mypage/orders` へのリンク

### 処理フロー

1. 退会ボタン押下 → `POST /mypage/withdraw`
2. `memberService.withdraw(memberId)` 実行
3. `memberSessionService.clear(session)` でセッションクリア
4. `/login` へリダイレクト
