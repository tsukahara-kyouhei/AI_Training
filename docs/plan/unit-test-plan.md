# 単体テスト計画

## 1. テスト方針

本計画は、ユーザーとの合意に基づいて以下の方針で作成した。

| 区分                     | 方針                                                                                                                                                                              |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **対象レイヤー**         | Service 層 / Controller 層 / Repository 層 の全層                                                                                                                                 |
| **Controllerテスト手法** | `@WebMvcTest` + `MockMvc`（HTTP レベルの検証）                                                                                                                                    |
| **カバレッジ目標**       | 行カバレッジ（C0）100% / 分岐カバレッジ（C1）100%<br>※ ローカル環境で単体テストとして検証不可な部分（Spring Batch 内部実行・トランザクション同期・メール非同期送信 など）は除外可 |
| **CI 連携**              | 現時点はローカル（`./mvnw test`）のみ。将来的に GitHub Actions のプルリクエスト時に自動実行することを推奨                                                                         |

---

## 2. テスト環境・使用ツール

| ツール                                             | 用途                                         |
| -------------------------------------------------- | -------------------------------------------- |
| JUnit 5 (`@Test`, `@Nested`, `@DisplayName`)       | テスト記述・グループ化                       |
| Mockito (`mock()`, `@Mock`, `@InjectMocks`)        | 依存クラスのスタブ化                         |
| AssertJ (`assertThat`, `assertThatThrownBy`)       | 流暢なアサーション                           |
| MockMvc (`@WebMvcTest` + `MockMvcRequestBuilders`) | Controller の HTTP リクエスト/レスポンス検証 |
| Spring Boot Test (`spring-boot-starter-test`)      | 統合テスト基盤（Repository 層が対象）        |
| `@ExtendWith(MockitoExtension.class)`              | Service / Repository の純粋単体テスト        |

> **Repository 層の注意点**
> MyBatis + PostgreSQL 構成のため、Repository の SQL パラメータマッピングをテストするには Mapper をモック化し Repository クラスそのものを `new` してテストする（既存の `ProductRepositoryTest` と同アプローチ）。
> 実際の SQL 実行が必要なケース（複雑なクエリ・ページング動作）は統合テスト（Testcontainers 等）に委譲し、本計画の対象外とする。

---

## 3. テスト対象クラスと優先度

| 優先度 | 対象クラス                     | 理由                                                                     |
| :----: | ------------------------------ | ------------------------------------------------------------------------ |
|   高   | `OrderService`                 | 注文採番・在庫チェック・業務バリデーション等、金銭に関わる最重要ロジック |
|   高   | `MemberService`                | 会員登録・退会・お届け先・お気に入り等、個人データを扱う                 |
|   高   | `CartService`                  | 送料計算・数量上限・在庫チェック等の閾値処理が多い                       |
|   高   | `CartController`               | チェックアウトのガード条件・PRGパターン・トークン検証が複雑              |
|   高   | `MemberRegistrationController` | 会員登録の3ステップフロー・セッション管理                                |
|   中   | `ProductListSearchService`     | ページ補正・総ページ計算の境界値                                         |
|   中   | `BatchExecutionService`        | 無効jobName / 実行状態の分岐                                             |
|   中   | `MyPageController`             | 購入履歴・お届け先・退会等の多機能カバー                                 |
|   中   | `ContactController`            | バリデーション・フラッシュ・PRG                                          |
|   中   | `AuthController`               | 既ログイン判定・リダイレクト                                             |
|   低   | `CatalogController`            | 主にサービス委譲、既存テストあり                                         |
|   低   | `ProductRepository`            | SQL パラメータマッピング（既存テストあり・拡張）                         |
|   低   | `InternalBatchController`      | 内部 API、エラーステータスコードの検証                                   |

---

## 4. テストケース一覧

### 凡例

| 分類              | 説明                                             |
| ----------------- | ------------------------------------------------ |
| **正常**          | 正常系：期待通りの入力で成功するケース           |
| **境界値**        | 上限・下限・閾値のちょうどのケース（境界値分析） |
| **境界値(超)**    | 境界を1つ超えるケース（同値分析の隣接区間）      |
| **異常**          | 業務ロジックエラーが発生するケース               |
| **null/ブランク** | null・空文字・空白のみ等の入力                   |

---

### 4.1 `OrderService`

#### 4.1.1 `generateOrderNumber`（`placeOrder` 経由での間接テスト）

| No.  | テストケース名                                     | 分類       | 主な入力・前提条件                      | 期待する結果                   |
| ---- | -------------------------------------------------- | ---------- | --------------------------------------- | ------------------------------ |
| O-01 | 連番=1 のとき 000001 形式の注文番号が生成される    | 正常       | `nextOrderSequence` が 1 を返す         | `"ORD20260310-000001"`         |
| O-02 | 連番=999_999（上限）のとき 999999 形式が生成される | 境界値     | `nextOrderSequence` が 999_999 を返す   | `"ORD20260310-999999"`         |
| O-03 | 連番=1_000_000（上限+1）のとき例外が発生する       | 境界値(超) | `nextOrderSequence` が 1_000_000 を返す | `IllegalStateException` スロー |
| O-04 | 注文日が変わると注文番号の日付部が更新される       | 正常       | 固定時刻を 2026-12-31 に変更            | `"ORD20261231-000001"`         |

#### 4.1.2 `placeOrder`

| No.  | テストケース名                                               | 分類          | 主な入力・前提条件                                    | 期待する結果                                                         |
| ---- | ------------------------------------------------------------ | ------------- | ----------------------------------------------------- | -------------------------------------------------------------------- |
| O-05 | カートが null のとき注文できない                             | null/ブランク | `cart = null`                                         | `IllegalArgumentException`（`business.order.cartEmpty`）             |
| O-06 | カートが空のとき注文できない                                 | 異常          | `cart.isEmpty() = true`                               | `IllegalArgumentException`（`business.order.cartEmpty`）             |
| O-07 | 法人区分で会社名が null のとき注文できない                   | null/ブランク | `personalOrCorporate="corporate"`, `companyName=null` | `IllegalArgumentException`（`business.order.companyRequired`）       |
| O-08 | 法人区分で会社名が空文字のとき注文できない                   | null/ブランク | `personalOrCorporate="corporate"`, `companyName=""`   | `IllegalArgumentException`（`business.order.companyRequired`）       |
| O-09 | 法人区分で会社名が半角スペースのみのとき注文できない         | null/ブランク | `personalOrCorporate="corporate"`, `companyName=" "`  | `IllegalArgumentException`（`business.order.companyRequired`）       |
| O-10 | 個人区分で会社名がなくても注文できる                         | 正常          | `personalOrCorporate="personal"`, `companyName=null`  | 注文番号（String）返却                                               |
| O-11 | deliveryFloor が null のとき注文できない                     | null/ブランク | `deliveryFloor=null`                                  | `IllegalArgumentException`（`business.order.deliveryFloorRequired`） |
| O-12 | deliveryFloor が空文字のとき注文できない                     | null/ブランク | `deliveryFloor=""`                                    | `IllegalArgumentException`（`business.order.deliveryFloorRequired`） |
| O-13 | deliveryFloor が数字でないとき注文できない                   | 異常          | `deliveryFloor="abc"`                                 | `IllegalArgumentException`（`business.order.deliveryFloorInteger`）  |
| O-14 | deliveryFloor が整数文字列のとき正常に注文できる             | 正常          | `deliveryFloor="3"`                                   | 注文番号返却                                                         |
| O-15 | 在庫数量が 0 の商品がカートにあるとき注文できない            | 異常          | カート明細に `stockQuantity=0` の商品あり             | `IllegalArgumentException`（`business.stockShortage`）               |
| O-16 | 注文数量が在庫数量を超えるとき注文できない                   | 異常          | `quantity=5`, `stockQuantity=3`                       | `IllegalArgumentException`（`business.stockShortage`）               |
| O-17 | 在庫数量ちょうどの注文は成功する                             | 境界値        | `quantity=3`, `stockQuantity=3`                       | 注文番号返却                                                         |
| O-18 | ゲスト注文（memberId=null）が正常に完了する                  | 正常          | `memberId=null`                                       | 注文番号返却、`insertOrder` に `memberId=null` が渡される            |
| O-19 | 会員注文（memberId あり）が正常に完了する                    | 正常          | `memberId=100L`                                       | 注文番号返却、`insertOrder` に `memberId=100L` が渡される            |
| O-20 | コンビニ決済を選んだとき paymentInstructionJson が生成される | 正常          | `paymentMethod="convenience_store"`                   | `insertOrder` の引数 `paymentInstructionJson` が非 null の JSON      |
| O-21 | コンビニ以外の決済では paymentInstructionJson が null になる | 正常          | `paymentMethod="credit_card"`                         | `insertOrder` の引数 `paymentInstructionJson` が null                |
| O-22 | 注文確定後に `insertOrderItem` が明細行数分呼ばれる          | 正常          | カートに 3 明細                                       | `insertOrderItem` が 3 回呼び出される                                |
| O-23 | 注文確定後に `insertOrderStatusHistory` が呼ばれる           | 正常          | 正常注文                                              | `insertOrderStatusHistory` が 1 回呼ばれる                           |

#### 4.1.3 `createInitialForm`

| No.  | テストケース名                                              | 分類          | 主な入力・前提条件                                       | 期待する結果                                      |
| ---- | ----------------------------------------------------------- | ------------- | -------------------------------------------------------- | ------------------------------------------------- |
| O-24 | 未ログイン（member=empty）のとき空フォームが返る            | 正常          | `member = Optional.empty()`                              | 全フィールドが null の `CheckoutInputForm`        |
| O-25 | ログイン済みでも prefill データがない場合は空フォームが返る | 正常          | `findCheckoutMemberPrefill` が `Optional.empty()` を返す | 全フィールドが null のフォーム                    |
| O-26 | ログイン済みで prefill データがある場合は設定値が反映される | 正常          | `findCheckoutMemberPrefill` が有効 prefill を返す        | `lastName`, `email` 等が prefill 値に一致         |
| O-27 | 7桁の郵便番号が part1（3桁）と part2（4桁）に分割される     | 正常          | `prefill.postalCode="1234567"`                           | `postalCodePart1="123"`, `postalCodePart2="4567"` |
| O-28 | 郵便番号が null の場合は分割されない                        | null/ブランク | `prefill.postalCode=null`                                | `postalCodePart1=null`, `postalCodePart2=null`    |

#### 4.1.4 `findOrderCompleteView`

| No.  | テストケース名                                     | 分類          | 主な入力・前提条件                 | 期待する結果                                          |
| ---- | -------------------------------------------------- | ------------- | ---------------------------------- | ----------------------------------------------------- |
| O-29 | orderNumber が null のとき Optional.empty が返る   | null/ブランク | `orderNumber=null`                 | `Optional.empty()`、repository は呼ばれない           |
| O-30 | orderNumber が空文字のとき Optional.empty が返る   | null/ブランク | `orderNumber=""`                   | `Optional.empty()`                                    |
| O-31 | orderNumber が空白のみのとき Optional.empty が返る | null/ブランク | `orderNumber="  "`                 | `Optional.empty()`                                    |
| O-32 | 有効な orderNumber のとき repository に委譲される  | 正常          | `orderNumber="ORD20260310-000001"` | `findOrderCompleteByOrderNumber` が呼ばれた結果を返す |

#### 4.1.5 `findMemberOrderHistories`

| No.  | テストケース名                                | 分類       | 主な入力・前提条件 | 期待する結果                   |
| ---- | --------------------------------------------- | ---------- | ------------------ | ------------------------------ |
| O-33 | page=1 のとき page=1 で repository が呼ばれる | 正常       | `page=1`           | `findMemberOrders(..., 1, 10)` |
| O-34 | page=0 のとき page=1 に補正される             | 境界値     | `page=0`           | `findMemberOrders(..., 1, 10)` |
| O-35 | page=-1 のとき page=1 に補正される            | 境界値(超) | `page=-1`          | `findMemberOrders(..., 1, 10)` |

#### 4.1.6 `findMemberOrderDetail` / `findReorderItems`

| No.  | テストケース名                                                 | 分類          | 主な入力・前提条件 | 期待する結果                            |
| ---- | -------------------------------------------------------------- | ------------- | ------------------ | --------------------------------------- |
| O-36 | `findMemberOrderDetail`：orderNumber が null → Optional.empty  | null/ブランク | `orderNumber=null` | `Optional.empty()`、repository 呼ばれず |
| O-37 | `findMemberOrderDetail`：orderNumber が空文字 → Optional.empty | null/ブランク | `orderNumber=""`   | `Optional.empty()`                      |
| O-38 | `findReorderItems`：orderNumber が null → 空リスト             | null/ブランク | `orderNumber=null` | `List.of()`、repository 呼ばれず        |
| O-39 | `findReorderItems`：orderNumber が空文字 → 空リスト            | null/ブランク | `orderNumber=""`   | `List.of()`                             |

---

### 4.2 `MemberService`

#### 4.2.1 `findActiveCredentialByEmail` / `existsByEmail`

| No.  | テストケース名                                              | 分類          | 主な入力・前提条件           | 期待する結果                                 |
| ---- | ----------------------------------------------------------- | ------------- | ---------------------------- | -------------------------------------------- |
| M-01 | `findActiveCredentialByEmail`：null → Optional.empty        | null/ブランク | `email=null`                 | `Optional.empty()`、repository 呼ばれず      |
| M-02 | `findActiveCredentialByEmail`：空文字 → Optional.empty      | null/ブランク | `email=""`                   | `Optional.empty()`                           |
| M-03 | `findActiveCredentialByEmail`：空白のみ → Optional.empty    | null/ブランク | `email="  "`                 | `Optional.empty()`                           |
| M-04 | `findActiveCredentialByEmail`：有効メール → repository 委譲 | 正常          | `email="user@example.com"`   | `findActiveCredentialByEmail` 呼び出し       |
| M-05 | `findActiveCredentialByEmail`：前後空白がトリムされる       | 正常          | `email=" user@example.com "` | `"user@example.com"` でrepository が呼ばれる |
| M-06 | `existsByEmail`：null → false                               | null/ブランク | `email=null`                 | `false`、repository 呼ばれず                 |
| M-07 | `existsByEmail`：空文字 → false                             | null/ブランク | `email=""`                   | `false`                                      |
| M-08 | `existsByEmail`：有効メール → repository 委譲               | 正常          | `email="user@example.com"`   | `existsByEmail` の戻り値を返す               |

#### 4.2.2 `register`

| No.  | テストケース名                                                                           | 分類 | 主な入力・前提条件                                                                | 期待する結果                                       |
| ---- | ---------------------------------------------------------------------------------------- | ---- | --------------------------------------------------------------------------------- | -------------------------------------------------- |
| M-09 | 正常登録される                                                                           | 正常 | 既存なし、全項目埋まっているフォーム                                              | `MemberSessionUser` 返却、`insertMember` 呼び出し  |
| M-10 | 事前チェックでメール重複を検出したとき例外が発生する                                     | 異常 | `existsByEmail` が true を返す                                                    | `DuplicateEmailException` スロー                   |
| M-11 | DB 挿入時に DataIntegrityViolationException かつ重複ありの場合は DuplicateEmailException | 異常 | `insertMember` が `DataIntegrityViolationException`、その後 `existsByEmail=true`  | `DuplicateEmailException` スロー（race condition） |
| M-12 | DB 挿入時に DataIntegrityViolationException かつ重複なしの場合は例外を再スローする       | 異常 | `insertMember` が `DataIntegrityViolationException`、その後 `existsByEmail=false` | `DataIntegrityViolationException` 再スロー         |
| M-13 | 登録後に `findActiveById` が empty を返した場合は IllegalStateException                  | 異常 | `insertMember` 成功後 `findActiveById` が empty                                   | `IllegalStateException` スロー                     |

#### 4.2.3 `updateProfile`

| No.  | テストケース名                                                                     | 分類 | 主な入力・前提条件                                                       | 期待する結果                               |
| ---- | ---------------------------------------------------------------------------------- | ---- | ------------------------------------------------------------------------ | ------------------------------------------ |
| M-14 | 正常更新される                                                                     | 正常 | 重複なし、`updateProfile` が true                                        | `updateProfile` 呼び出し                   |
| M-15 | 他会員とメールが重複する場合は DuplicateEmailException                             | 異常 | `existsByEmailForOtherMember` が true                                    | `DuplicateEmailException` スロー           |
| M-16 | DB 更新時に DataIntegrityViolationException かつ重複ありは DuplicateEmailException | 異常 | `updateProfile` が `DataIntegrityViolationException`、事後確認で重複あり | `DuplicateEmailException` スロー           |
| M-17 | DB 更新時に DataIntegrityViolationException かつ重複なしは再スロー                 | 異常 | `updateProfile` が `DataIntegrityViolationException`、事後確認で重複なし | `DataIntegrityViolationException` 再スロー |
| M-18 | 更新件数 0 件（会員が見つからない）の場合は IllegalStateException                  | 異常 | `updateProfile` が false を返す                                          | `IllegalStateException` スロー             |

#### 4.2.4 `withdraw`

| No.  | テストケース名                            | 分類 | 主な入力・前提条件        | 期待する結果 |
| ---- | ----------------------------------------- | ---- | ------------------------- | ------------ |
| M-19 | 退会成功時は true が返る                  | 正常 | `withdrawMember` が true  | `true`       |
| M-20 | 対象会員が見つからない場合は false が返る | 異常 | `withdrawMember` が false | `false`      |

#### 4.2.5 `isAddressLimitReached` / `createAdditionalAddress`

| No.  | テストケース名                                               | 分類       | 主な入力・前提条件            | 期待する結果                           |
| ---- | ------------------------------------------------------------ | ---------- | ----------------------------- | -------------------------------------- |
| M-21 | 登録数が 19（上限-1）のとき上限未達                          | 境界値     | `countAdditionalAddresses=19` | `isAddressLimitReached=false`          |
| M-22 | 登録数が 20（上限ちょうど）のとき上限到達                    | 境界値     | `countAdditionalAddresses=20` | `isAddressLimitReached=true`           |
| M-23 | 登録数が 21（上限+1）のとき上限到達                          | 境界値(超) | `countAdditionalAddresses=21` | `isAddressLimitReached=true`           |
| M-24 | 上限未満のとき登録成功する                                   | 正常       | `countAdditionalAddresses=19` | `insertAdditionalAddress` 呼び出し     |
| M-25 | 上限到達時に登録しようとすると AddressLimitExceededException | 異常       | `countAdditionalAddresses=20` | `AddressLimitExceededException` スロー |

#### 4.2.6 `toggleFavorite`

| No.  | テストケース名                                                                       | 分類       | 主な入力・前提条件                                                                 | 期待する結果                                  |
| ---- | ------------------------------------------------------------------------------------ | ---------- | ---------------------------------------------------------------------------------- | --------------------------------------------- |
| M-26 | 既にお気に入り登録済みのとき解除（deleteFavorite）が呼ばれる                         | 正常       | `existsFavorite=true`                                                              | `deleteFavorite` 1 回呼び出し                 |
| M-27 | 未登録かつ件数が 99（上限-1）のとき登録成功する                                      | 境界値     | `existsFavorite=false`, `countFavorites=99`                                        | `insertFavorite` 呼び出し                     |
| M-28 | 未登録かつ件数が 100（上限ちょうど）のとき FavoritesLimitExceededException           | 境界値     | `existsFavorite=false`, `countFavorites=100`                                       | `FavoritesLimitExceededException` スロー      |
| M-29 | 未登録かつ件数が 101（上限+1）のとき FavoritesLimitExceededException                 | 境界値(超) | `existsFavorite=false`, `countFavorites=101`                                       | `FavoritesLimitExceededException` スロー      |
| M-30 | `insertFavorite` で DataIntegrityViolationException かつ事後確認で存在ありは正常終了 | 異常       | `insertFavorite` が `DataIntegrityViolationException`、事後 `existsFavorite=true`  | 例外なしで正常終了（race condition スキップ） |
| M-31 | `insertFavorite` で DataIntegrityViolationException かつ事後確認で存在なしは再スロー | 異常       | `insertFavorite` が `DataIntegrityViolationException`、事後 `existsFavorite=false` | `DataIntegrityViolationException` 再スロー    |

#### 4.2.7 ページ補正系

| No.  | テストケース名                                  | 分類       | 主な入力・前提条件 | 期待する結果                      |
| ---- | ----------------------------------------------- | ---------- | ------------------ | --------------------------------- |
| M-32 | `findAdditionalAddresses`：page=0 → 1 に補正    | 境界値     | `page=0`           | repository が `page=1` で呼ばれる |
| M-33 | `findAdditionalAddresses`：page=負数 → 1 に補正 | 境界値(超) | `page=-5`          | repository が `page=1` で呼ばれる |
| M-34 | `findFavorites`：page=0 → 1 に補正              | 境界値     | `page=0`           | repository が `page=1` で呼ばれる |

---

### 4.3 `CartService`

#### 4.3.1 `addItem`

| No.  | テストケース名                                                          | 分類       | 主な入力・前提条件                                           | 期待する結果                                                 |
| ---- | ----------------------------------------------------------------------- | ---------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| C-01 | productVariantId=0 のとき追加できない                                   | 境界値     | `productVariantId=0`                                         | `IllegalArgumentException`（`business.cart.invalidProduct`） |
| C-02 | productVariantId=-1 のとき追加できない                                  | 境界値(超) | `productVariantId=-1`                                        | `IllegalArgumentException`                                   |
| C-03 | productVariantId=1（最小有効値）のとき追加できる                        | 境界値     | `productVariantId=1`、在庫あり                               | snapshot 取得・追加成功                                      |
| C-04 | quantity=0 のとき追加できない                                           | 境界値     | `quantity=0`                                                 | `IllegalArgumentException`（`business.cart.quantityRange`）  |
| C-05 | quantity=1（最小）のとき追加できる                                      | 境界値     | `quantity=1`                                                 | 追加成功                                                     |
| C-06 | quantity=99（最大）のとき追加できる                                     | 境界値     | `quantity=99`                                                | 追加成功                                                     |
| C-07 | quantity=100（最大+1）のとき追加できない                                | 境界値(超) | `quantity=100`                                               | `IllegalArgumentException`（`business.cart.quantityRange`）  |
| C-08 | quantity=-1 のとき追加できない                                          | 境界値(超) | `quantity=-1`                                                | `IllegalArgumentException`                                   |
| C-09 | 在庫数量=0 のとき追加できない                                           | 境界値     | `snapshot.stockQuantity=0`                                   | `IllegalArgumentException`（`business.stockShortage`）       |
| C-10 | 在庫数量=1 のとき追加できる                                             | 境界値     | `snapshot.stockQuantity=1`                                   | 追加成功                                                     |
| C-11 | snapshot が取得できない（商品が存在しない）とき例外が発生する           | 異常       | `findProductSnapshotsByVariantIds` が空 Map を返す           | `IllegalArgumentException`（`business.cart.loadFailed`）     |
| C-12 | 同一バリアントが既存のとき数量が合算される（合計 < 99）                 | 正常       | 既存 quantity=3、追加 quantity=5                             | 更新後 quantity=8                                            |
| C-13 | 同一バリアントが既存のとき合計が 99 を超えると 99 にクランプされる      | 境界値     | 既存 quantity=95、追加 quantity=10                           | 更新後 quantity=99                                           |
| C-14 | 新規バリアント追加でカートが 100 件（満杯）のとき追加できない           | 境界値     | カート明細 100 件                                            | `IllegalArgumentException`（`business.cart.limitExceeded`）  |
| C-15 | 新規バリアント追加でカートが 99 件（満杯-1）のとき追加できる            | 境界値     | カート明細 99 件                                             | 追加成功（100 件になる）                                     |
| C-16 | assemblyRequested=true かつ assembly 不可の場合は false に正規化される  | 正常       | `assemblyRequested=true`, `snapshot.assemblyAvailable=false` | Cookie に `assemblyRequested=false` が保存される             |
| C-17 | assemblyRequested=null かつ assembly 可能の場合は null のまま保存される | 正常       | `assemblyRequested=null`, `assemblyAvailable=true`           | Cookie に `assemblyRequested=null` が保存される              |

#### 4.3.2 `updateItem`

| No.  | テストケース名                               | 分類       | 主な入力・前提条件         | 期待する結果                                                |
| ---- | -------------------------------------------- | ---------- | -------------------------- | ----------------------------------------------------------- |
| C-18 | quantity=0 のとき更新できない                | 境界値     | `quantity=0`               | `IllegalArgumentException`（`business.cart.quantityRange`） |
| C-19 | quantity=1（最小）のとき更新できる           | 境界値     | `quantity=1`、在庫あり     | 更新成功                                                    |
| C-20 | quantity=99（最大）のとき更新できる          | 境界値     | `quantity=99`、在庫あり    | 更新成功                                                    |
| C-21 | quantity=100（最大+1）のとき更新できない     | 境界値(超) | `quantity=100`             | `IllegalArgumentException`                                  |
| C-22 | 対象バリアントがカートにない場合は何もしない | 正常       | カートに該当 ID なし       | 正常終了、saveCurrentCartItems 未呼び出し                   |
| C-23 | 在庫数量=0 のとき更新できない                | 境界値     | `snapshot.stockQuantity=0` | `IllegalArgumentException`（`business.stockShortage`）      |

#### 4.3.3 `getCart`（送料計算）

| No.  | テストケース名                                                                | 分類       | 主な入力・前提条件                                | 期待する結果                   |
| ---- | ----------------------------------------------------------------------------- | ---------- | ------------------------------------------------- | ------------------------------ |
| C-24 | カートが空のとき各金額がゼロ                                                  | 正常       | Cookie にアイテムなし                             | `CartSummaryView` の全金額が 0 |
| C-25 | 税込合計が 4_999 円のとき送料が 800 円                                        | 境界値     | 税込小計 4_999 円                                 | `shippingFee=800`              |
| C-26 | 税込合計が 5_000 円のとき送料が 0 円                                          | 境界値     | 税込小計 5_000 円                                 | `shippingFee=0`                |
| C-27 | 税込合計が 5_001 円のとき送料が 0 円                                          | 境界値(超) | 税込小計 5_001 円                                 | `shippingFee=0`                |
| C-28 | Cookie のバリアント ID に対応する snapshot がない場合はその明細をスキップする | 異常       | Cookie に 2 明細あるが snapshot は 1 件分のみ返る | `CartView.items()` に 1 件のみ |

#### 4.3.4 `sanitizeRedirectPath`

| No.  | テストケース名                                       | 分類          | 主な入力・前提条件                  | 期待する結果 |
| ---- | ---------------------------------------------------- | ------------- | ----------------------------------- | ------------ |
| C-29 | null のとき null が返る                              | null/ブランク | `rawPath=null`                      | `null`       |
| C-30 | 空文字のとき null が返る                             | null/ブランク | `rawPath=""`                        | `null`       |
| C-31 | 空白のみのとき null が返る                           | null/ブランク | `rawPath="  "`                      | `null`       |
| C-32 | スラッシュ始まりの相対パスは有効                     | 正常          | `rawPath="/cart"`                   | `"/cart"`    |
| C-33 | `//` 始まりのパスは null（オープンリダイレクト防止） | 異常          | `rawPath="//evil.example.com"`      | `null`       |
| C-34 | `://` を含むパスは null（オープンリダイレクト防止）  | 異常          | `rawPath="http://evil.example.com"` | `null`       |
| C-35 | スラッシュなしのパスは null                          | 異常          | `rawPath="cart"`                    | `null`       |
| C-36 | 前後空白がトリムされて有効パスになる                 | 正常          | `rawPath=" /cart "`                 | `"/cart"`    |

#### 4.3.5 `removeItem` / `clear`

| No.  | テストケース名                                      | 分類 | 主な入力・前提条件         | 期待する結果                                            |
| ---- | --------------------------------------------------- | ---- | -------------------------- | ------------------------------------------------------- |
| C-37 | 存在する明細が削除される                            | 正常 | カートに該当バリアントあり | `saveCurrentCartItems` が呼ばれる（1件少ない状態）      |
| C-38 | 存在しない明細を削除しても正常終了する              | 正常 | カートに該当バリアントなし | `saveCurrentCartItems` が呼ばれる（変化なし）           |
| C-39 | `clear` を呼ぶと `cartCookieStore.clear` が呼ばれる | 正常 | —                          | `cartCookieStore.clear(request, response)` 呼び出し確認 |

---

### 4.4 `ProductListSearchService`

#### 4.4.1 `calculateTotalPages`

| No.  | テストケース名                                     | 分類       | 主な入力・前提条件         | 期待する結果 |
| ---- | -------------------------------------------------- | ---------- | -------------------------- | ------------ |
| P-01 | totalCount=0 のとき 1 ページ返る                   | 境界値     | `totalCount=0`, `size=15`  | `1`          |
| P-02 | totalCount=1 のとき 1 ページ                       | 境界値     | `totalCount=1`, `size=15`  | `1`          |
| P-03 | totalCount=15（ちょうど1ページ分）のとき 1 ページ  | 境界値     | `totalCount=15`, `size=15` | `1`          |
| P-04 | totalCount=16（1ページを1件超える）のとき 2 ページ | 境界値(超) | `totalCount=16`, `size=15` | `2`          |
| P-05 | totalCount=30（ちょうど2ページ分）のとき 2 ページ  | 境界値     | `totalCount=30`, `size=15` | `2`          |
| P-06 | totalCount=31 のとき 3 ページ                      | 境界値(超) | `totalCount=31`, `size=15` | `3`          |

#### 4.4.2 `searchWithPageCorrection`

| No.  | テストケース名                                                   | 分類 | 主な入力・前提条件                            | 期待する結果                                |
| ---- | ---------------------------------------------------------------- | ---- | --------------------------------------------- | ------------------------------------------- |
| P-07 | 1ページ目に結果が存在する場合は補正なし                          | 正常 | `page=1`, `totalCount=10`, 結果あり           | `productService.search` が 1 回だけ呼ばれる |
| P-08 | 件数が 0 件の場合は補正なし                                      | 正常 | `page=5`, `totalCount=0`, 結果なし            | 補正なし（`search` は 1 回のみ）            |
| P-09 | ページ超過かつ件数あり・空結果の場合は最終ページに補正して再検索 | 異常 | `page=5`, `totalCount=10`, `size=5`, 結果が空 | `search` が 2 回呼ばれ、2 回目は `page=2`   |

---

### 4.5 `BatchExecutionService`

#### 4.5.1 `stopExecution`

| No.  | テストケース名                                                               | 分類 | 主な入力・前提条件                                       | 期待する結果                                           |
| ---- | ---------------------------------------------------------------------------- | ---- | -------------------------------------------------------- | ------------------------------------------------------ |
| B-01 | executionId が存在しない場合は BatchExecutionNotFoundException               | 異常 | `jobExplorer.getJobExecution(id)=null`                   | `BatchExecutionNotFoundException` スロー               |
| B-02 | 実行中でない JobExecution の場合は BatchExecutionNotRunningException         | 異常 | `execution.status=COMPLETED`                             | `BatchExecutionNotRunningException` スロー             |
| B-03 | `jobOperator.stop()` が例外を投げる場合は IllegalStateException              | 異常 | `jobOperator.stop()` が例外                              | `IllegalStateException`（`business.batch.stopFailed`） |
| B-04 | `jobOperator.stop()` が false を返す場合は BatchExecutionNotRunningException | 異常 | `jobOperator.stop()=false`                               | `BatchExecutionNotRunningException` スロー             |
| B-05 | 正常に停止要求を受け付けた場合は停止受付レスポンスを返す                     | 正常 | `getJobExecution` が実行中 execution 返却、`stop()=true` | `BatchExecutionStopAcceptedResponse` 返却              |

#### 4.5.2 `listExecutions`

| No.  | テストケース名                                       | 分類       | 主な入力・前提条件      | 期待する結果                      |
| ---- | ---------------------------------------------------- | ---------- | ----------------------- | --------------------------------- |
| B-06 | 不正な jobName を指定すると UnknownBatchJobException | 異常       | `jobName="nonexistent"` | `UnknownBatchJobException` スロー |
| B-07 | limit=0 のとき 1 に補正される                        | 境界値     | `limit=0`               | 結果が 1 件以内に制限される       |
| B-08 | limit=100 のとき上限そのまま                         | 境界値     | `limit=100`             | 上限 100 件                       |
| B-09 | limit=101 のとき 100 に補正される                    | 境界値(超) | `limit=101`             | 最大 100 件に制限される           |

---

### 4.6 Controller 層テストケース一覧

> Controller テストはすべて `@WebMvcTest` + `MockMvc` を使用する。
> 依存する Service / Repository はすべて `@MockBean` でスタブ化する。

#### 4.6.1 `CartController`

| No.   | テストケース名                                                               | 分類 | HTTP | URL                                | 主な前提条件                                  | 期待する結果                                               |
| ----- | ---------------------------------------------------------------------------- | ---- | ---- | ---------------------------------- | --------------------------------------------- | ---------------------------------------------------------- |
| CC-01 | カート画面が表示される                                                       | 正常 | GET  | `/cart`                            | カートあり                                    | 200, `pages/cart` テンプレート, `cart` モデル属性あり      |
| CC-02 | 空カートでもカート画面が表示される                                           | 正常 | GET  | `/cart`                            | カート空                                      | 200, `isCartEmpty=true`                                    |
| CC-03 | アイテム追加成功後に指定 redirect 先にリダイレクトされる                     | 正常 | POST | `/cart/items`                      | `addItem` 正常終了、`redirect=/products/1`    | 302, `/products/1`, flash `cartMessage` あり               |
| CC-04 | アイテム追加成功・redirect 未指定のとき `/cart` にリダイレクトされる         | 正常 | POST | `/cart/items`                      | `addItem` 正常終了、redirect パラメータなし   | 302, `/cart`, flash `cartMessage` あり                     |
| CC-05 | アイテム追加失敗（業務エラー）のとき redirect 先にエラーフラッシュで戻る     | 異常 | POST | `/cart/items`                      | `addItem` が `IllegalArgumentException`       | 302, flash `cartError` あり                                |
| CC-06 | アイテム更新成功のとき `/cart` にリダイレクトされる                          | 正常 | POST | `/cart/items/{id}/update`          | `updateItem` 正常終了                         | 302, `/cart`                                               |
| CC-07 | アイテム更新失敗のとき `/cart` にエラーフラッシュでリダイレクトされる        | 異常 | POST | `/cart/items/{id}/update`          | `updateItem` が `IllegalArgumentException`    | 302, `/cart`, flash `cartError` あり                       |
| CC-08 | アイテム削除後に `/cart` にリダイレクトされる                                | 正常 | POST | `/cart/items/{id}/delete`          | —                                             | 302, `/cart`                                               |
| CC-09 | カートクリア後に `/cart` にリダイレクトされる                                | 正常 | POST | `/cart/clear`                      | —                                             | 302, `/cart`                                               |
| CC-10 | カートが空で購入方法選択画面を開くと `/cart` にリダイレクトされる            | 異常 | GET  | `/checkout/method`                 | カート空                                      | 302, `/cart`                                               |
| CC-11 | カートありで購入方法選択画面が表示される                                     | 正常 | GET  | `/checkout/method`                 | カートあり                                    | 200, `pages/checkout-method`                               |
| CC-12 | カートが空でチェックアウト入力画面を開くと `/cart` にリダイレクトされる      | 異常 | GET  | `/checkout/input`                  | カート空                                      | 302, `/cart`                                               |
| CC-13 | チェックアウト入力画面が表示される（セッションフォームなし・未ログイン）     | 正常 | GET  | `/checkout/input`                  | カートあり、セッションフォームなし            | 200, 空の `checkoutForm` がモデルにある                    |
| CC-14 | チェックアウト入力フォームのバリデーションエラー時に入力画面に戻る           | 異常 | POST | `/checkout/input`                  | 必須項目欠落                                  | 200, `pages/checkout-input`, フォームエラーあり            |
| CC-15 | チェックアウト入力正常送信後に確認画面へリダイレクトされる                   | 正常 | POST | `/checkout/input`                  | バリデーション通過                            | 302, `/checkout/confirm`                                   |
| CC-16 | セッションフォームなしで確認画面を開くと入力画面へリダイレクトされる         | 異常 | GET  | `/checkout/confirm`                | セッションフォームなし                        | 302, `/checkout/input`                                     |
| CC-17 | 確認画面が正常に表示される                                                   | 正常 | GET  | `/checkout/confirm`                | セッションフォームあり、カートあり            | 200, `pages/checkout-confirm`, `checkoutConfirmToken` あり |
| CC-18 | カートが空で注文確定すると `/cart` へリダイレクトされる                      | 異常 | POST | `/checkout/confirm`                | カート空                                      | 302, `/cart`                                               |
| CC-19 | セッションフォームなしで注文確定するとエラーフラッシュで入力へ戻る           | 異常 | POST | `/checkout/confirm`                | セッションフォームなし                        | 302, `/checkout/input`, flash `checkoutError` あり         |
| CC-20 | トークン不一致で注文確定するとエラーフラッシュで入力へ戻る                   | 異常 | POST | `/checkout/confirm`                | セッショントークン `aaa`、送信トークン `bbb`  | 302, `/checkout/input`, flash `checkoutError` あり         |
| CC-21 | 業務エラーで注文確定失敗するとエラーフラッシュで入力へ戻る                   | 異常 | POST | `/checkout/confirm`                | `placeOrder` が `IllegalArgumentException`    | 302, `/checkout/input`, flash `checkoutError` あり         |
| CC-22 | 注文確定成功後に注文完了画面へリダイレクトされる                             | 正常 | POST | `/checkout/confirm`                | `placeOrder` が `"ORD20260310-000001"` を返す | 302, `/checkout/complete/ORD20260310-000001`               |
| CC-23 | 注文完了フォールバック（`/checkout/complete`）は入力画面へリダイレクトされる | 正常 | GET  | `/checkout/complete`               | —                                             | 302, `/checkout/input`                                     |
| CC-24 | 注文完了画面が正常に表示される                                               | 正常 | GET  | `/checkout/complete/{orderNumber}` | `findOrderCompleteView` が有効データ返却      | 200, `pages/checkout-complete`                             |

#### 4.6.2 `MemberRegistrationController`

| No.   | テストケース名                                                         | 分類 | HTTP | URL                          | 主な前提条件                                        | 期待する結果                                                           |
| ----- | ---------------------------------------------------------------------- | ---- | ---- | ---------------------------- | --------------------------------------------------- | ---------------------------------------------------------------------- |
| MR-01 | 登録入力画面が表示される                                               | 正常 | GET  | `/members/register`          | —                                                   | 200, `pages/member-register`, `registerForm` あり                      |
| MR-02 | バリデーションエラー時に入力画面に戻る                                 | 異常 | POST | `/members/register/confirm`  | 必須項目欠落                                        | 200, `pages/member-register`, フォームエラーあり                       |
| MR-03 | メールアドレス重複時に入力画面にエラーが表示される                     | 異常 | POST | `/members/register/confirm`  | `existsByEmail=true`                                | 200, `pages/member-register`, `email` フィールドエラーあり             |
| MR-04 | 法人でも会社名が空のとき入力画面にエラーが表示される                   | 異常 | POST | `/members/register/confirm`  | `personalOrCorporate="corporate"`, `companyName=""` | 200, `pages/member-register`, フォームエラーあり                       |
| MR-05 | 入力正常のとき確認画面が表示される                                     | 正常 | POST | `/members/register/confirm`  | バリデーション通過、重複なし                        | 200, `pages/member-register-confirm`, セッションにフォームが保存される |
| MR-06 | セッションにフォームがない状態で確定すると入力画面へリダイレクトされる | 異常 | POST | `/members/register`          | セッションに `PENDING_REGISTER_FORM` なし           | 302, `/members/register`                                               |
| MR-07 | メール重複例外で確定が失敗すると入力画面にエラーが表示される           | 異常 | POST | `/members/register`          | `register` が `DuplicateEmailException`             | 200, `pages/member-register`, `email` フィールドエラーあり             |
| MR-08 | 確定成功後に完了画面へリダイレクトされる                               | 正常 | POST | `/members/register`          | `register` 成功                                     | 302, `/members/register/complete`, flash `registeredMemberCode` あり   |
| MR-09 | フラッシュなしで完了画面を直アクセスすると 404                         | 異常 | GET  | `/members/register/complete` | flash `registeredMemberCode` なし                   | 404                                                                    |
| MR-10 | フラッシュありで完了画面が表示される                                   | 正常 | GET  | `/members/register/complete` | flash `registeredMemberCode` あり                   | 200, `pages/member-register-complete`                                  |

#### 4.6.3 `AuthController`

| No.   | テストケース名                                                             | 分類 | HTTP | URL                   | 主な前提条件                                               | 期待する結果                                       |
| ----- | -------------------------------------------------------------------------- | ---- | ---- | --------------------- | ---------------------------------------------------------- | -------------------------------------------------- |
| AC-01 | 既ログイン済みでログイン画面を開くと `/mypage/orders` へリダイレクトされる | 正常 | GET  | `/login`              | `currentMember` が値あり                                   | 302, `/mypage/orders`                              |
| AC-02 | 未ログインでログイン画面が表示される                                       | 正常 | GET  | `/login`              | `currentMember` が empty                                   | 200, `pages/login`                                 |
| AC-03 | メールアドレス記憶 Cookie があるとき loginForm にメールが事前入力される    | 正常 | GET  | `/login`              | `findRememberedEmail` が `Optional.of("user@example.com")` | モデルの `loginForm.email` が `"user@example.com"` |
| AC-04 | `expired=true` のときセッション切れフラグがモデルにセットされる            | 正常 | GET  | `/login?expired=true` | —                                                          | モデルの `sessionExpired=true`                     |

#### 4.6.4 `MyPageController`

| No.   | テストケース名                                                              | 分類 | HTTP | URL                                    | 主な前提条件                                                 | 期待する結果                                                |
| ----- | --------------------------------------------------------------------------- | ---- | ---- | -------------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------------- |
| MP-01 | `/mypage` から `/mypage/orders` へリダイレクトされる                        | 正常 | GET  | `/mypage`                              | —                                                            | 302, `/mypage/orders`                                       |
| MP-02 | 未ログインで購入履歴一覧を開くと 401                                        | 異常 | GET  | `/mypage/orders`                       | `currentMember` が empty                                     | 401                                                         |
| MP-03 | ログイン済みで購入履歴一覧が表示される                                      | 正常 | GET  | `/mypage/orders`                       | ログイン済み                                                 | 200, `pages/mypage-orders-list`, `orders` モデル属性あり    |
| MP-04 | 存在しない注文番号で詳細を開くと 404                                        | 異常 | GET  | `/mypage/orders/{orderNumber}`         | `findMemberOrderDetail` が empty                             | 404                                                         |
| MP-05 | 購入履歴詳細が表示される                                                    | 正常 | GET  | `/mypage/orders/{orderNumber}`         | `findMemberOrderDetail` がデータあり                         | 200, `pages/mypage-order-detail`                            |
| MP-06 | 再購入で全商品が追加できない場合はエラーフラッシュで `/cart` へリダイレクト | 異常 | POST | `/mypage/orders/{orderNumber}/reorder` | 全商品が `addItem` で失敗                                    | 302, `/cart`, flash `cartError` あり                        |
| MP-07 | 再購入で全商品が追加成功するとsuccessフラッシュで `/cart` へリダイレクト    | 正常 | POST | `/mypage/orders/{orderNumber}/reorder` | 全商品が `addItem` 成功                                      | 302, `/cart`, flash `cartMessage` あり                      |
| MP-08 | 再購入で一部商品が失敗した場合は success + warning フラッシュで `/cart` へ  | 正常 | POST | `/mypage/orders/{orderNumber}/reorder` | 2件中1件失敗                                                 | 302, `/cart`, flash `cartMessage` と `cartError` の両方あり |
| MP-09 | お気に入り一覧が表示される                                                  | 正常 | GET  | `/mypage/favorites`                    | ログイン済み                                                 | 200, `pages/mypage-favorites`                               |
| MP-10 | 会員情報変更画面が表示される                                                | 正常 | GET  | `/mypage/profile`                      | ログイン済み、`findProfileByMemberId` がデータあり           | 200, `pages/mypage-profile-edit`                            |
| MP-11 | バリデーションエラー時に変更画面に戻る                                      | 異常 | POST | `/mypage/profile`                      | 必須項目欠落                                                 | 200, `pages/mypage-profile-edit`                            |
| MP-12 | メール重複時に変更画面にエラーが表示される                                  | 異常 | POST | `/mypage/profile`                      | `updateProfile` が `DuplicateEmailException`                 | 200, `pages/mypage-profile-edit`, `email` エラーあり        |
| MP-13 | 変更成功後に成功フラッシュで自画面へリダイレクトされる                      | 正常 | POST | `/mypage/profile`                      | `updateProfile` 成功                                         | 302, `/mypage/profile`, flash `profileUpdatedMessage` あり  |
| MP-14 | 追加お届け先一覧が表示される                                                | 正常 | GET  | `/mypage/addresses`                    | ログイン済み                                                 | 200, `pages/mypage-addresses`                               |
| MP-15 | 新規お届け先登録でバリデーションエラー時に登録フォームに戻る                | 異常 | POST | `/mypage/addresses`                    | 必須項目欠落                                                 | 200, `pages/mypage-address-form`                            |
| MP-16 | 上限超過時に AddressLimitExceededException でアドレス一覧へリダイレクト     | 異常 | POST | `/mypage/addresses`                    | `createAdditionalAddress` が `AddressLimitExceededException` | 302, `/mypage/addresses`, flash `addressLimitError` あり    |
| MP-17 | お届け先登録成功後に一覧へリダイレクトされる                                | 正常 | POST | `/mypage/addresses`                    | `createAdditionalAddress` 正常                               | 302, `/mypage/addresses`                                    |
| MP-18 | 存在しないお届け先の編集を開くと 404                                        | 異常 | GET  | `/mypage/addresses/{id}/edit`          | `findAdditionalAddressById` が empty                         | 404                                                         |
| MP-19 | お届け先更新成功後に一覧へリダイレクトされる                                | 正常 | POST | `/mypage/addresses/{id}`               | `updateAdditionalAddress` が true                            | 302, `/mypage/addresses`                                    |
| MP-20 | 更新対象が見つからない場合は 404                                            | 異常 | POST | `/mypage/addresses/{id}`               | `updateAdditionalAddress` が false                           | 404                                                         |
| MP-21 | お届け先削除後に一覧へリダイレクトされる                                    | 正常 | POST | `/mypage/addresses/{id}/delete`        | `deleteAdditionalAddress` 正常                               | 302, `/mypage/addresses`                                    |
| MP-22 | 退会確認画面が表示される                                                    | 正常 | GET  | `/mypage/withdraw`                     | ログイン済み                                                 | 200, `pages/mypage-withdraw`                                |
| MP-23 | 退会確定後はセッションクリアされ `/login` へリダイレクトされる              | 正常 | POST | `/mypage/withdraw`                     | ログイン済み、`withdraw` 正常                                | 302, `/login`, セッションクリア確認                         |

#### 4.6.5 `ContactController`

| No.   | テストケース名                                                 | 分類 | HTTP | URL        | 主な前提条件             | 期待する結果                                                     |
| ----- | -------------------------------------------------------------- | ---- | ---- | ---------- | ------------------------ | ---------------------------------------------------------------- |
| CO-01 | 未ログインでお問い合わせ画面が表示される（空フォーム）         | 正常 | GET  | `/contact` | `currentMember` が empty | 200, `pages/contact`, `contactForm` にデータなし                 |
| CO-02 | ログイン済みでお問い合わせ画面が表示される（事前入力フォーム） | 正常 | GET  | `/contact` | ログイン済み             | 200, `pages/contact`, `contactForm` に会員情報あり               |
| CO-03 | バリデーションエラー時にお問い合わせ画面に戻る                 | 異常 | POST | `/contact` | 必須項目欠落             | 200, `pages/contact`, フォームエラーあり                         |
| CO-04 | 送信成功後に受付フラッシュで自画面へリダイレクトされる         | 正常 | POST | `/contact` | `submit` 正常            | 302, `/contact`, flash `contactAccepted`（またはモデル属性）あり |
| CO-05 | 未ログインでもお問い合わせを送信できる                         | 正常 | POST | `/contact` | `currentMember` が empty | 302, `/contact`                                                  |

#### 4.6.6 `InternalBatchController`

| No.   | テストケース名                               | 分類 | HTTP   | URL                                              | 主な前提条件                                           | 期待する結果        |
| ----- | -------------------------------------------- | ---- | ------ | ------------------------------------------------ | ------------------------------------------------------ | ------------------- |
| IB-01 | ジョブ一覧が取得できる                       | 正常 | GET    | `/internal/batch/jobs`                           | `listJobs` が 2 件返す                                 | 200, JSON 配列 2 件 |
| IB-02 | 不正 jobName で実行履歴取得すると 404        | 異常 | GET    | `/internal/batch/jobs/{jobName}/executions`      | `listExecutions` が `UnknownBatchJobException`         | 404                 |
| IB-03 | 正常な jobName で実行履歴が取得できる        | 正常 | GET    | `/internal/batch/jobs/{jobName}/executions`      | `listExecutions` が履歴リスト返す                      | 200, JSON 配列      |
| IB-04 | 不正 jobName でジョブ起動すると 404          | 異常 | POST   | `/internal/batch/jobs/{jobName}/executions`      | `submitJob` が `UnknownBatchJobException`              | 404                 |
| IB-05 | 既に実行中でジョブ起動すると 409             | 異常 | POST   | `/internal/batch/jobs/{jobName}/executions`      | `submitJob` が `BatchAlreadyRunningException`          | 409                 |
| IB-06 | 正常にジョブ起動すると 202 が返る            | 正常 | POST   | `/internal/batch/jobs/{jobName}/executions`      | `submitJob` が受付レスポンス返す                       | 202                 |
| IB-07 | 実行 ID が存在しない場合に停止要求すると 404 | 異常 | DELETE | `/internal/batch/jobs/{jobName}/executions/{id}` | `stopExecution` が `BatchExecutionNotFoundException`   | 404                 |
| IB-08 | 実行中でない場合に停止要求すると 409         | 異常 | DELETE | `/internal/batch/jobs/{jobName}/executions/{id}` | `stopExecution` が `BatchExecutionNotRunningException` | 409                 |
| IB-09 | 正常に停止要求すると 200 が返る              | 正常 | DELETE | `/internal/batch/jobs/{jobName}/executions/{id}` | `stopExecution` が停止受付レスポンス返す               | 200                 |

---

### 4.7 Repository 層テストケース一覧

> **テスト方針**: Mapper をモックにして Repository クラスを `new` し、SQL パラメータのマッピングロジックを検証する（`ProductRepositoryTest` と同アプローチ）。
> 実 SQL の正確性（結合・ソート・ページング）は統合テストの対象とし、本計画の範囲外とする。

#### 4.7.1 `ProductRepository`（既存テストの拡張）

| No.   | テストケース名                                                     | 分類          | 主な入力・前提条件                | 期待する結果                                 |
| ----- | ------------------------------------------------------------------ | ------------- | --------------------------------- | -------------------------------------------- |
| PR-01 | 通常キーワードが trim されて `keyword`・`keywordLike` に反映される | 正常          | `keyword=" デスク "`              | `keyword="デスク"`, `keywordLike="%デスク%"` |
| PR-02 | null キーワードは `keyword`・`keywordLike` ともに null             | null/ブランク | `keyword=null`                    | 両方 null                                    |
| PR-03 | 空白のみキーワードは `keyword`・`keywordLike` ともに null          | null/ブランク | `keyword="  "`                    | 両方 null                                    |
| PR-04 | `hasTasteSearchFilter=true` がパラメータに反映される               | 正常          | カテゴリフィルタで taste 指定あり | `hasTasteSearchFilter=true`                  |
| PR-05 | カテゴリフィルタ未指定のときは `hasTasteSearchFilter=false`        | 正常          | `categoryFilter=empty`            | `hasTasteSearchFilter=false`                 |

#### 4.7.2 `OrderRepository`

| No.   | テストケース名                                                                            | 分類          | 主な入力・前提条件   | 期待する結果                                                                            |
| ----- | ----------------------------------------------------------------------------------------- | ------------- | -------------------- | --------------------------------------------------------------------------------------- |
| OR-01 | `insertOrder` が Mapper に渡す Map にすべての必須キーが含まれる                           | 正常          | 有効な `orderParams` | `insertOrder` に `orderNumber`, `orderDatetime`, `memberId`, `totalAmount` 等が含まれる |
| OR-02 | `insertOrderItem` が Mapper に渡す Map に `orderId`・`productCode`・`quantity` が含まれる | 正常          | 有効な `itemParams`  | 上記キーが Map に存在する                                                               |
| OR-03 | `findOrderCompleteByOrderNumber` が空文字の orderNumber で Mapper を呼ばない              | null/ブランク | `orderNumber=""`     | Mapper 未呼び出し（Repository 側でガードしている場合）                                  |

#### 4.7.3 `MemberRepository`

| No.    | テストケース名                                                              | 分類 | 主な入力・前提条件           | 期待する結果                                      |
| ------ | --------------------------------------------------------------------------- | ---- | ---------------------------- | ------------------------------------------------- |
| MR-R01 | `insertMember` が Mapper に渡すパラメータにハッシュ済みパスワードが含まれる | 正常 | 有効フォーム、ハッシュ文字列 | `insertMember` の引数に `passwordHash` が含まれる |
| MR-R02 | `findActiveById` が戻り値をそのまま返す                                     | 正常 | Mapper が会員データを返す    | 同じ値が返る                                      |
| MR-R03 | `withdrawMember` が Mapper に memberId を渡す                               | 正常 | `memberId=42L`               | `withdrawMember(42L)` がコールされる              |

#### 4.7.4 `CartRepository`

| No.   | テストケース名                                                                               | 分類   | 主な入力・前提条件                  | 期待する結果                        |
| ----- | -------------------------------------------------------------------------------------------- | ------ | ----------------------------------- | ----------------------------------- |
| CR-01 | `findProductSnapshotsByVariantIds` が空リストのとき Mapper を呼ばない（または空 Map を返す） | 境界値 | `variantIds=[]`                     | 空 `Map<Long, CartProductSnapshot>` |
| CR-02 | `findCurrentTaxRatePercent` が Mapper の戻り値を返す                                         | 正常   | Mapper が `BigDecimal("10")` を返す | `10`                                |

---

## 5. テスト除外・注記

| 項目                                                 | 理由                                                                                                                                 |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `NotificationMailService` のメール送信               | 実際の SMTP 接続が必要。Service テストではモック化する。メール本文の内容テストは別途メールプレビューでの目視確認を推奨               |
| トランザクションコミット後の `afterCommit` フック    | `TransactionSynchronizationManager` の実際の同期は Spring のトランザクション管理が必要。単体テストでは `MockedStatic` でスタブ化する |
| `BatchScheduler`（`@Scheduled`）                     | スケジューラのトリガ自体は Spring コンテキスト依存。`runScheduledHourlyJobs` の呼び出しは統合テストで確認する                        |
| `GlobalExceptionLoggingAdvice`・`AppErrorController` | HTTP エラーのグローバルハンドリングは MockMvc の `@WebMvcTest` でエラーページ表示のスモークテストのみ行う                            |
| SQL 実行結果の正確性                                 | 複雑な JOIN・ページング・ソートは統合テスト（Testcontainers + 実 PostgreSQL）で検証する                                              |
| Spring Security フィルタ（CSRF トークン等）          | `@WebMvcTest` で `csrf().disable()` または `.with(csrf())` を使い、認証のみを検証                                                    |

---

## 6. テスト実装ガイドライン

### 6.1 命名規則

```
テストクラス名: {対象クラス}Test
テストメソッド名: @DisplayName に日本語で説明を記述
ネスト: @Nested + @DisplayName でメソッドごとにグループ化
```

### 6.2 テストクラス構造のひな形（Service 層）

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private NotificationMailService notificationMailService;

    private OrderService service;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(any(), any(Object[].class), any()))
            .thenAnswer(inv -> inv.getArgument(0));
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("Asia/Tokyo"));
        service = new OrderService(orderRepository, new ObjectMapper(), notificationMailService, fixedClock, messageSource);
    }

    @Nested
    @DisplayName("placeOrder — 業務バリデーション")
    class PlaceOrderValidation {
        @Test
        @DisplayName("カートが null のとき IllegalArgumentException が発生する")
        void cartIsNull_throws() {
            assertThatThrownBy(() -> service.placeOrder(null, validForm(), null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

### 6.3 テストクラス構造のひな形（Controller 層）

```java
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CartService cartService;
    @MockBean private OrderService orderService;
    @MockBean private MemberSessionService memberSessionService;

    @Test
    @DisplayName("空カートで購入方法選択画面を開くと /cart へリダイレクトされる")
    void checkoutMethod_emptyCart_redirectsToCart() throws Exception {
        when(cartService.getCart(any(), any())).thenReturn(CartView.empty());

        mockMvc.perform(get("/checkout/method"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"));
    }
}
```

### 6.4 カバレッジ計測コマンド

```shell
# JaCoCo レポート生成（target/site/jacoco/index.html）
./mvnw test jacoco:report

# Surefire テスト結果確認
./mvnw test
```

### 6.5 カバレッジ除外設定（将来の `pom.xml` 追加候補）

```xml
<configuration>
  <excludes>
    <!-- フレームワーク設定クラスはカバレッジ対象外 -->
    <exclude>**/config/**</exclude>
    <exclude>**/model/**</exclude>
    <exclude>**/*Exception.class</exclude>
  </excludes>
</configuration>
```
