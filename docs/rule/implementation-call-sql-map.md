# OFFICE ORDER 関連クラス・メソッド・SQL調査資料

## ① 調査範囲と前提

本資料は、これまでに行ったソース調査結果を統合し、各機能の呼び出し経路、関連メソッド、MyBatis SQL、DB対象を対応付けたものです。今回、新たなソース調査やアプリケーション起動・DB実行は行っていません。

対象機能:

1. 商品一覧
2. 会員登録
3. 会員情報・住所管理
4. ログイン・ログアウト
5. カート
6. 注文
7. お問い合わせ
8. お気に入り
9. クーポン
10. レビュー
11. 商品販売状態・在庫
12. Spring Batch

READMEでは本リポジトリを顧客向けECサイトとし、商品閲覧、会員登録、カート、注文、お問い合わせを扱う一方、受注後の業務処理と管理画面は別システムの責務としています。[README.md:1-6](../../README.md#L1-L6) バッチについては起動時および毎時のランキング・おすすめ再計算が記載されています。[README.md:78-98](../../README.md#L78-L98)

指定された既存資料名のうち、`business-rules.md`、`business-flow.md`、`state-transition.md`は`docs/rule/`内の実在資料を参照しました。依頼文にあった`investigation.md`は存在せず、実在する[調査メモ](./investigation-notes.md)を読み替えています。先行調査結果は参照情報として用い、実装事実と同一視していません。

記載の意味:

- **定義**: Mapper XMLやDDLにSQL・制約が記載されていること。
- **呼出経路確認**: Javaコード上で呼び出し関係を追えたこと。DBへの実発行・実行結果を意味しません。
- **未確認**: 今回までの調査範囲で呼出元、実行結果、または仕様を確認できなかったこと。存在しないとの断定ではありません。

## ② 機能ごとの処理経路・SQL

### 1. 商品一覧

**処理経路**

`CatalogController`の一覧・検索・カテゴリメソッド → `ProductListSearchService`／`ProductFilterOptionService` → `ProductService` → `ProductRepository.search` → `ProductMapper` → `ProductMapper.xml`。[CatalogController.java:85-148](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L85-L148) [ProductRepository.java:99-121](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java#L99-L121)

| SQL ID | 種類 | 主な対象テーブル | 処理・呼び出し経路 |
|---|---|---|---|
| `countProducts` | SELECT | `products`, `product_variants`、属性テーブル | 件数取得。`ProductRepository.search`から呼出し。販売期間、カテゴリ、検索語、商品属性、バリアント条件は共有SQL断片／動的条件で適用。[ProductMapper.xml:85-352](../../src/main/resources/mappers/ProductMapper.xml#L85-L352) [ProductMapper.xml:353-359](../../src/main/resources/mappers/ProductMapper.xml#L353-L359) |
| `selectProducts` | SELECT | `products`, `product_variants` | 一覧のページデータを取得。ソート、LIMIT、OFFSETを適用。[ProductMapper.xml:360-390](../../src/main/resources/mappers/ProductMapper.xml#L360-L390) |
| `selectColorCodes` | SELECT | `product_variants`, `colors` | 表示商品の色コードを取得。[ProductMapper.xml:409-421](../../src/main/resources/mappers/ProductMapper.xml#L409-L421) |
| `selectProductsByIds` | SELECT | `products`, `product_variants` | 商品ID群から販売期間内の商品表示データを取得。最近見た商品等の表示に利用。[ProductMapper.xml:391-407](../../src/main/resources/mappers/ProductMapper.xml#L391-L407) |
| `selectActiveColorFilterOptions`、`selectActiveDeskTopShapeOptions`、`selectActiveDeskTasteOptions`、`selectActiveChairFunctionOptions`、`selectActiveChairMaterialOptions`、`selectActiveChairTasteOptions`、`selectActiveStorageUsageOptions`、`selectActiveStorageTasteOptions` | SELECT | 色・カテゴリ属性マスタ | 有効な絞り込み選択肢を取得。`ProductFilterOptionRepository`の各`findActive...Options`から呼出し。[ProductFilterOptionRepository.java:41-125](../../src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java#L41-L125) [ProductMapper.xml:501-573](../../src/main/resources/mappers/ProductMapper.xml#L501-L573) |

Java側では検索条件の正規化・ページ補正、RepositoryでSQLパラメータ作成を行います。販売期間等の検索条件はSQL側にもあります。検索条件ごとの厳密な動的分岐は`ProductMapper.xml`の`BaseProductWhere`等を参照してください。

### 2. 会員登録

**処理経路**

確認POST: `MemberRegistrationController.confirm` → `MemberService.existsByEmail` → `MemberRepository.existsByEmail` → `MemberMapper.existsByEmail` → SQL。登録確定POST: Controller → `MemberService.register` → Repository → Mapper SQL → 登録後`findActiveById`。[MemberRegistrationController.java:84-143](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L143) [MemberService.java:113-139](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L139)

| SQL ID | 種類 | 対象テーブル | 処理・呼び出し元 |
|---|---|---|---|
| `existsByEmail` | SELECT | `members` | 大文字小文字を無視したメール重複確認。登録ControllerとService経由で利用。[MemberMapper.xml:90-97](../../src/main/resources/mappers/MemberMapper.xml#L90-L97) |
| `insertMember` | INSERT | `members` | 会員情報とハッシュ化済みパスワードを登録し、`active`を設定。`MemberRepository.insertMember`から呼出し。[MemberMapper.xml:156-210](../../src/main/resources/mappers/MemberMapper.xml#L156-L210) |
| `selectActiveById` | SELECT | `members` | 登録後のactive会員情報取得。[MemberMapper.xml:119-128](../../src/main/resources/mappers/MemberMapper.xml#L119-L128) |

メール重複確認・パスワードハッシュ化はJava側、メール一意性はDBにも定義されています。[MemberService.java:113-139](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L139) [members.sql:23-30](../../sql/schema/members.sql#L23-L30)

### 3. 会員情報・住所管理

**処理経路**

`MyPageController` → `MemberService` → `MemberRepository` → `MemberMapper` → `MemberMapper.xml`。Controllerはプロフィール・住所の入力、一覧、編集、削除、退会を受け付けます。[MyPageController.java:216-425](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L216-L425)

| SQL ID | 種類 | 対象テーブル | 処理・呼び出し元 |
|---|---|---|---|
| `existsByEmailForOtherMember` | SELECT | `members` | プロフィール更新時の他会員とのメール重複確認。`MemberService.updateProfile`経由。[MemberMapper.xml:98-105](../../src/main/resources/mappers/MemberMapper.xml#L98-L105) |
| `selectProfileByMemberId` | SELECT | `members` | 会員ID・active条件でプロフィール取得。[MemberMapper.xml:130-154](../../src/main/resources/mappers/MemberMapper.xml#L130-L154) |
| `updateProfile` | UPDATE | `members` | プロフィール更新。会員IDかつactiveがWHERE条件。[MemberMapper.xml:414-439](../../src/main/resources/mappers/MemberMapper.xml#L414-L439) |
| `selectMemberTypeById` | SELECT | `members` | 会員種別取得。[MemberMapper.xml:212-216](../../src/main/resources/mappers/MemberMapper.xml#L212-L216) |
| `countAdditionalAddresses` | SELECT | `member_additional_addresses` | 住所上限判定。[MemberMapper.xml:218-222](../../src/main/resources/mappers/MemberMapper.xml#L218-L222) |
| `selectAdditionalAddresses` | SELECT | `member_additional_addresses` | 会員IDで住所一覧取得。[MemberMapper.xml:224-247](../../src/main/resources/mappers/MemberMapper.xml#L224-L247) |
| `selectAdditionalAddressById` | SELECT | `member_additional_addresses` | 会員IDと住所IDで対象を取得。[MemberMapper.xml:249-271](../../src/main/resources/mappers/MemberMapper.xml#L249-L271) |
| `insertAdditionalAddress` | INSERT | `member_additional_addresses` | 追加住所を登録。[MemberMapper.xml:273-312](../../src/main/resources/mappers/MemberMapper.xml#L273-L312) |
| `updateAdditionalAddress` | UPDATE | `member_additional_addresses` | 会員IDと住所IDで更新。[MemberMapper.xml:314-334](../../src/main/resources/mappers/MemberMapper.xml#L314-L334) |
| `deleteAdditionalAddress` | DELETE | `member_additional_addresses` | 会員IDと住所IDで削除。[MemberMapper.xml:336-340](../../src/main/resources/mappers/MemberMapper.xml#L336-L340) |
| `withdrawMember` | UPDATE | `members` | active会員をwithdrawnへ変更。[MemberMapper.xml:441-449](../../src/main/resources/mappers/MemberMapper.xml#L441-L449) |

メール重複、住所上限、更新件数の扱いはService側にもあります。[MemberService.java:188-218](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L188-L218) [MemberService.java:243-305](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L243-L305) 退会時にControllerがServiceの戻り値によって遷移を分ける処理は確認できません。[MyPageController.java:421-426](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L421-L426)

### 4. ログイン・ログアウト

**処理経路**

ログインPOSTはSpring Securityのフォームログイン → `MemberUserDetailsService.loadUserByUsername` → `MemberService.findActiveCredentialByEmail` → `MemberRepository.findActiveCredentialByEmail` → `MemberMapper.selectActiveCredentialByEmail`。[SecurityConfig.java:91-108](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L91-L108) [MemberUserDetailsService.java:39-47](../../src/main/java/jp/co/skig/officeorder/service/member/MemberUserDetailsService.java#L39-L47)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `selectActiveCredentialByEmail` | SELECT | `members` | メール一致かつactiveの会員認証情報を取得。[MemberMapper.xml:107-117](../../src/main/resources/mappers/MemberMapper.xml#L107-L117) |

ログアウトはSpring Securityが認証・HTTPセッションを破棄しCookieを削除します。ログイン・ログアウトに対応する会員DBのINSERT／UPDATEは確認できません。[SecurityConfig.java:91-108](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L91-L108) 会員セッションを読む`MemberSessionService.currentMember`等はDB更新SQLではありません。[MemberSessionService.java:20-85](../../src/main/java/jp/co/skig/officeorder/web/auth/MemberSessionService.java#L20-L85)

### 5. カート

**処理経路**

`CartController` → `CartService` → `CartCookieStore`、`CartRepository` → `CartMapper` → SQL。カート明細はCookieに保持され、DBのカート明細テーブルへの書込みSQLは確認されていません。[CartController.java:100-195](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L100-L195) [CartCookieStore.java:48-155](../../src/main/java/jp/co/skig/officeorder/repository/CartCookieStore.java#L48-L155)

| SQL ID | 種類 | 対象テーブル | 処理・呼び出し元 |
|---|---|---|---|
| `selectCartProductSnapshots` | SELECT | `product_variants`, `products`, `colors` | CookieのバリアントIDに対して商品・価格・在庫・組立情報を取得。`CartRepository.findProductSnapshotsByVariantIds`経由。[CartMapper.xml:21-42](../../src/main/resources/mappers/CartMapper.xml#L21-L42) |
| `selectCurrentTaxRatePercent` | SELECT | `tax_rates` | 有効期間内の税率を取得。`CartRepository.findCurrentTaxRatePercent`経由。[CartMapper.xml:43-54](../../src/main/resources/mappers/CartMapper.xml#L43-L54) |

数量・在庫、Cookie明細の正規化、税・送料・クーポン割引計算はJava側です。[CartService.java:94-194](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L94-L194) [CartService.java:224-300](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L300)

### 6. 注文

**処理経路**

`CartController.placeOrder` → `CartService.getCart`（注文対象CartViewを構成）→ `OrderService.placeOrder` → `OrderRepository` → `OrderMapper` → `OrderMapper.xml`。`placeOrder`はトランザクションメソッドです。[CartController.java:377-410](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L377-L410) [OrderService.java:147-240](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L147-L240)

| SQL ID | 種類 | 対象テーブル | 処理・呼び出し元 |
|---|---|---|---|
| `selectCurrentTaxRatePercent` | SELECT | `tax_rates` | 注文用税率取得。[OrderMapper.xml:375-384](../../src/main/resources/mappers/OrderMapper.xml#L375-L384) |
| `nextOrderSequence` | INSERT／UPDATE | `order_number_counters` | 日付行を作成、または連番を加算。[OrderMapper.xml:257-275](../../src/main/resources/mappers/OrderMapper.xml#L257-L275) |
| `insertOrder` | INSERT | `orders` | 注文ヘッダ、金額等を保存し、初期状態`received`を設定。[OrderMapper.xml:124-207](../../src/main/resources/mappers/OrderMapper.xml#L124-L207) |
| `insertOrderItem` | INSERT | `order_items` | 注文明細を保存。[OrderMapper.xml:209-240](../../src/main/resources/mappers/OrderMapper.xml#L209-L240) |
| `insertOrderStatusHistory` | INSERT | `order_status_histories` | 初期状態履歴を保存。[OrderMapper.xml:242-255](../../src/main/resources/mappers/OrderMapper.xml#L242-L255) |
| `selectOrderCompleteByOrderNumber` | SELECT | `orders`等 | 注文番号で完了画面用情報を取得。[OrderMapper.xml:277-298](../../src/main/resources/mappers/OrderMapper.xml#L277-L298) |
| `countMemberOrders`、`selectMemberOrders`、`selectMemberOrderDetail`、`selectMemberOrderStatusHistories`、`selectMemberOrderItems`、`selectMemberReorderItems` | SELECT | `orders`, `order_items`等 | 会員購入履歴、注文詳細、明細、履歴、再購入情報を取得。[OrderMapper.xml:300-373](../../src/main/resources/mappers/OrderMapper.xml#L300-L373) |
| `countMemberPurchasedProduct` | SELECT | `orders`, `order_items`, `product_variants` | 会員の商品購入履歴確認。レビューの購入条件で利用。[OrderMapper.xml:386-395](../../src/main/resources/mappers/OrderMapper.xml#L386-L395) |

**クーポン・金額の受け渡し:** `CartService`がクーポンを検証し、割引後の`totalAmount`、`appliedCouponCode`、`couponDiscountAmount`を`CartSummaryView`に設定します。[CartService.java:150-194](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L150-L194) `CartController`は注文POSTでもカートを再取得して`CartView`をServiceへ渡します。[CartController.java:377-402](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L377-L402) `OrderService.placeOrder`は割引後合計をMapに設定しますが、クーポンコードと割引額を設定する処理は確認できません。[OrderService.java:175-204](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L175-L204) 一方`insertOrder` SQLは両キーを参照します。[OrderMapper.xml:150-200](../../src/main/resources/mappers/OrderMapper.xml#L150-L200)

注文の後続ステータス更新、決済結果照合、注文後在庫減算は、READMEの責務境界も踏まえ、注文登録処理として記載していません。[README.md:3-6](../../README.md#L3-L6) `received`は注文作成時の初期値です。

### 7. お問い合わせ

**処理経路**

`ContactController` → `ContactService` → `ContactRepository` → `ContactMapper` → `ContactMapper.xml`。[ContactController.java:62-109](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L62-L109) [ContactService.java:45-81](../../src/main/java/jp/co/skig/officeorder/service/contact/ContactService.java#L45-L81)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `selectMemberContactPrefill` | SELECT | `members` | active会員のお問い合わせ初期表示情報を取得。[ContactMapper.xml:18-29](../../src/main/resources/mappers/ContactMapper.xml#L18-L29) |
| `insertInquiry` | INSERT | `inquiries` | 問い合わせ情報を保存。[ContactMapper.xml:31-70](../../src/main/resources/mappers/ContactMapper.xml#L31-L70) |

お問い合わせ受付後の対応状況管理・更新は、今回までの調査範囲では確認できませんでした。READMEがそれを明示的に別システム責務としているとは確認できないため、実装対象外とは断定しません。

### 8. お気に入り

**処理経路**

商品詳細からのPOST → `CatalogController.toggleFavorite` → `MemberService.toggleFavorite`／`removeFavorite` → `MemberRepository` → `MemberMapper` → SQL。[CatalogController.java:320-379](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L320-L379) [MemberService.java:335-371](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L335-L371)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `existsFavorite` | SELECT | `member_favorites` | 会員・商品のお気に入り登録有無を判定。[MemberMapper.xml:384-391](../../src/main/resources/mappers/MemberMapper.xml#L384-L391) |
| `countFavorites`、`selectFavorites`、`selectFavoriteColorCodes` | SELECT | `member_favorites`, `products`, `product_variants`, `colors` | 一覧・件数・色表示用取得。商品販売期間条件を含む。[MemberMapper.xml:342-382](../../src/main/resources/mappers/MemberMapper.xml#L342-L382) |
| `insertFavorite` | INSERT | `member_favorites` | 会員・商品のお気に入りを登録。[MemberMapper.xml:393-406](../../src/main/resources/mappers/MemberMapper.xml#L393-L406) |
| `deleteFavorite` | DELETE | `member_favorites` | 会員・商品のお気に入りを削除。[MemberMapper.xml:408-412](../../src/main/resources/mappers/MemberMapper.xml#L408-L412) |

登録上限はServiceで判定されます。DBには会員IDと商品IDの複合主キーがあります。[MemberService.java:335-371](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L335-L371) [coupons.sql:35-45](../../sql/schema/coupons.sql#L35-L45)

### 9. クーポン

**処理経路**

カート表示／注文準備時: `CartController` → `CartService.getCart` → `CouponService.validateAndCalculateDiscount` → `CouponMapper` → SQL。利用実績登録: `CouponServiceImpl.recordCouponUsage` → `CouponMapper.insertUsage`。ただし、これを呼ぶ注文確定経路は確認できていません。[CartService.java:150-194](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L150-L194) [CouponServiceImpl.java:25-74](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L25-L74)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `findByCode` | SELECT | `coupons` | クーポンコードで検索。[CouponMapper.xml:9-14](../../src/main/resources/mappers/CouponMapper.xml#L9-L14) |
| `countUsage` | SELECT | `member_coupon_usage` | 会員・クーポン別の利用数合計。[CouponMapper.xml:15-22](../../src/main/resources/mappers/CouponMapper.xml#L15-L22) |
| `insertUsage` | INSERT | `member_coupon_usage` | 利用実績を挿入するSQL定義。`recordCouponUsage`から呼ぶ実装はあるが、同メソッドの呼出元は今回の調査範囲で確認できませんでした。[CouponMapper.xml:23-39](../../src/main/resources/mappers/CouponMapper.xml#L23-L39) |

`recordCouponUsage`はmemberId、couponId、usageCount=1を設定します。注文IDを含む処理は確認できず、注文確定時に利用実績を登録する仕様かどうかは未確認です。[CouponServiceImpl.java:66-74](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L66-L74)

### 10. レビュー

**処理経路**

`ReviewController` → `ReviewService` → `ReviewRepository` → `ReviewMapper` → `ReviewMapper.xml`。投稿可否の購入確認では`OrderRepository` → `OrderMapper.countMemberPurchasedProduct`も呼びます。[ReviewController.java:23-150](../../src/main/java/jp/co/skig/officeorder/web/review/ReviewController.java#L23-L150) [ReviewService.java:29-65](../../src/main/java/jp/co/skig/officeorder/service/review/ReviewService.java#L29-L65)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `selectProductReviews`、`countProductReviews`、`selectProductReviewSummary` | SELECT | `product_review`, `members` | 公開済みレビュー一覧・件数・平均評価を取得。`published=true`で絞り込み。[ReviewMapper.xml:9-44](../../src/main/resources/mappers/ReviewMapper.xml#L9-L44) |
| `selectMemberProductReview` | SELECT | `product_review` | 会員・商品別のレビュー取得。[ReviewMapper.xml:46-61](../../src/main/resources/mappers/ReviewMapper.xml#L46-L61) |
| `insertReview` | INSERT | `product_review` | 新規レビューを登録し、SQL内で`published=true`を設定。[ReviewMapper.xml:63-83](../../src/main/resources/mappers/ReviewMapper.xml#L63-L83) |
| `updateReview` | UPDATE | `product_review` | 評価・タイトル・本文を更新。公開フラグは更新しない。[ReviewMapper.xml:86-96](../../src/main/resources/mappers/ReviewMapper.xml#L86-L96) |
| `countMemberPurchasedProduct` | SELECT | `orders`, `order_items`, `product_variants` | 会員・商品に購入履歴があるかを確認。[OrderMapper.xml:386-395](../../src/main/resources/mappers/OrderMapper.xml#L386-L395) |

`ReviewForm`とControllerでBean Validation／独自Validatorによる入力チェックは今回の調査範囲では確認できません。DB定義には評価1～5、本文NOT NULL、会員・商品ごとの一意制約があります。[ReviewForm.java:1-41](../../src/main/java/jp/co/skig/officeorder/model/review/ReviewForm.java#L1-L41) [members.sql:59-73](../../sql/schema/members.sql#L59-L73) 新規時に公開状態をtrueにする理由、公開切替処理、レビュー入力仕様は未確認です。パス変数`productId`と`ReviewForm.productId`の同一性確認もコード上では確認できていません。

### 11. 商品販売状態・在庫

販売期間は商品一覧・詳細・カートのSELECT条件で判定されます。商品一覧の共通条件は`BaseProductWhere`、詳細は`selectProductDetail`、カート用商品情報は`selectCartProductSnapshots`です。[ProductMapper.xml:260-350](../../src/main/resources/mappers/ProductMapper.xml#L260-L350) [ProductMapper.xml:440-470](../../src/main/resources/mappers/ProductMapper.xml#L440-L470) [CartMapper.xml:21-42](../../src/main/resources/mappers/CartMapper.xml#L21-L42)

`product_variants.stock_quantity`は一覧・詳細・お気に入り・カート等の検索で参照されます。カートServiceは在庫切れ商品を拒否し、注文Serviceは`CartView`内の在庫数と数量を比較します。[CartService.java:224-300](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L300) [OrderService.java:412-425](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L412-L425)

今回までの調査では、アプリケーションJava、Mapper XML、リポジトリSQL内の在庫数を更新するUPDATEは確認できませんでした。DBでは在庫数非負のCHECK制約があります。[products.sql:47-67](../../sql/schema/products.sql#L47-L67) 在庫減算の担当処理、別システム連携、同時注文時の競合制御は未確認です。

### 12. Spring Batch

**処理経路**

自動起動: `BatchScheduler.runStartupJobs`／`runHourlyJobs` → `BatchExecutionService` → Spring Batch Job／Step（`BatchJobConfiguration`）→ `BatchJobService` → `BatchRepository` → `BatchMapper` → SQL。[BatchScheduler.java:28-43](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L28-L43) [BatchJobConfiguration.java:24-105](../../src/main/java/jp/co/skig/officeorder/config/BatchJobConfiguration.java#L24-L105)

| SQL ID | 種類 | 対象テーブル | 処理 |
|---|---|---|---|
| `selectPopularRankingCandidates` | SELECT | `orders`, `order_items`, `product_variants`, `products` | ランキング対象の販売数量候補を集計。[BatchMapper.xml:21-36](../../src/main/resources/mappers/BatchMapper.xml#L21-L36) |
| `deletePopularRankingsByDate` | DELETE | `popular_product_rankings` | 指定日のランキングを削除。[BatchMapper.xml:38-41](../../src/main/resources/mappers/BatchMapper.xml#L38-L41) |
| `insertPopularRanking` | INSERT | `popular_product_rankings` | 順位別ランキングを保存。[BatchMapper.xml:43-60](../../src/main/resources/mappers/BatchMapper.xml#L43-L60) |
| `selectOrderProductOccurrences` | SELECT | `orders`, `order_items`, `product_variants` | おすすめ計算用に注文内の商品出現を取得。[BatchMapper.xml:62-73](../../src/main/resources/mappers/BatchMapper.xml#L62-L73) |
| `deleteRecommendedRelatedByDate` | DELETE | `recommended_related_products` | 指定日のおすすめ結果を削除。[BatchMapper.xml:75-78](../../src/main/resources/mappers/BatchMapper.xml#L75-L78) |
| `insertRecommendedRelated` | INSERT | `recommended_related_products` | 算出した関連商品を保存。[BatchMapper.xml:80-98](../../src/main/resources/mappers/BatchMapper.xml#L80-L98) |

集計・候補選別は`BatchJobService.executePopularRanking`／`executeRecommendedRelated`が行い、対象日の削除・挿入を`BatchRepository`が呼び出します。[BatchJobService.java:72-118](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L72-L118) [BatchRepository.java:35-88](../../src/main/java/jp/co/skig/officeorder/repository/BatchRepository.java#L35-L88)

localプロファイルの内部APIから実行履歴・起動・停止要求を操作する経路があります。[InternalBatchController.java:63-150](../../src/main/java/jp/co/skig/officeorder/web/InternalBatchController.java#L63-L150) 停止APIは`JobOperator.stop`の受付結果を返すもので、停止要求の受付とジョブ停止完了は別です。[BatchExecutionService.java:141-166](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchExecutionService.java#L141-L166) 実際のBatch状態管理はSpring Batch側に委ねられています。

## ③ 複数機能で共通利用されるクラス・SQL

| 共通要素 | 利用機能・関係 |
|---|---|
| `MemberService`／`MemberRepository`／`MemberMapper` | 登録、会員情報、住所、認証、退会、お気に入りで利用。会員状態・メール・プロフィール等を扱います。 |
| `ProductMapper`／`ProductRepository` | 商品一覧・詳細、販売期間・在庫表示、ランキング・おすすめ表示、絞り込み選択肢で利用。 |
| `OrderRepository`／`OrderMapper` | 注文登録・履歴・完了画面取得に加え、レビュー購入条件で利用。 |
| `selectCurrentTaxRatePercent` | カート・注文でそれぞれCartMapper、OrderMapperに定義され、有効税率検索に利用。 |
| `product_variants.stock_quantity` | 商品一覧・詳細・お気に入り・カートで表示／判定に参照。注文時はCartViewに入った値との比較に利用。 |
| `member_coupon_usage` | クーポン適用時の利用数照会と、呼出元未確認の利用実績INSERTで利用。 |

同名のSQLが異なるMapperに個別定義されている場合は、別々のSQL定義として扱っています。上表は共通の役割を整理したもので、実行回数やDB実行結果を示すものではありません。

## ④ 重要な処理・注意点

1. **注文とクーポン:** 割引後合計は`CartSummaryView`から注文用Mapに設定されます。`appliedCouponCode`と`couponDiscountAmount`をMapへ設定する処理は確認できませんが、`insertOrder` SQLは両キーを参照します。DBに実際に保存された値は未確認です。[OrderService.java:175-204](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L175-L204) [OrderMapper.xml:150-200](../../src/main/resources/mappers/OrderMapper.xml#L150-L200)
2. **クーポン利用実績:** `recordCouponUsage`からMapper INSERTへの経路はありますが、注文確定処理からの呼出し元を確認できませんでした。[CouponServiceImpl.java:66-74](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L66-L74)
3. **注文時在庫:** OrderServiceの在庫再確認はControllerから渡された`CartView`内の値との比較です。OrderService内で最新在庫を再取得する処理は確認できませんでした。[CartController.java:377-402](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L377-L402) [OrderService.java:412-425](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L412-L425)
4. **クーポンURL:** `CouponController`と`CartController`にPOST `/cart/coupon/apply`とPOST `/cart/coupon/remove`の重複Mapping宣言があります。実際のアプリ起動時の登録結果は確認していません。[CouponController.java:12-30](../../src/main/java/jp/co/skig/officeorder/web/CouponController.java#L12-L30) [CartController.java:198-222](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L198-L222)
5. **レビュー:** Serviceは既存レビューの有無によりINSERT／UPDATEを選択し、新規INSERT SQLは`published=true`を設定します。Form／ControllerのBean Validationは確認できず、公開切替処理も確認できませんでした。[ReviewService.java:57-65](../../src/main/java/jp/co/skig/officeorder/service/review/ReviewService.java#L57-L65) [ReviewMapper.xml:63-96](../../src/main/resources/mappers/ReviewMapper.xml#L63-L96)
6. **レビューの商品ID:** 購入確認はURLのパス変数`productId`、INSERT SQLは`ReviewForm.productId`を参照します。両者の一致をControllerで確認する処理は今回の調査範囲では確認できず、仕様・業務上の扱いは未確認です。[ReviewController.java:87-102](../../src/main/java/jp/co/skig/officeorder/web/review/ReviewController.java#L87-L102) [ReviewMapper.xml:63-83](../../src/main/resources/mappers/ReviewMapper.xml#L63-L83)
7. **Spring Batch停止:** 停止APIは要求受付とその応答を扱い、停止完了の確認とは区別されます。状態管理はSpring Batch側です。[BatchExecutionService.java:141-166](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchExecutionService.java#L141-L166)

## ⑤ 未確認事項・注意事項

| 項目 | 今回までに確認できたこと | 未確認のこと |
|---|---|---|
| 重複Mapping | ソースに同一HTTPメソッド・URLのMapping宣言が複数ある。 | 実起動時のハンドラ登録結果、起動可否、実際に応答するハンドラ。 |
| 注文クーポン保存 | 割引後合計は注文Mapへ設定される。コード・割引額の設定は確認できない。 | SQL実行時のパラメータバインドと実DBの保存値。 |
| クーポン利用実績 | `recordCouponUsage`と`insertUsage`は定義され、前者から後者を呼ぶ。 | 注文確定時に利用実績を登録する仕様か、実際の呼出元、実行トランザクション。 |
| 在庫減算 | 商品表示・カート・注文前に在庫を参照・確認する。 | 在庫減算を担当する処理・別システム、実際の連携、同時注文競合制御。 |
| レビュー | DBの評価制約、公開表示条件、新規INSERT時の公開値を確認。 | 入力値の業務仕様、`published=true`の意図、公開状態を変更する運用・処理、パス値とForm値の関係。 |
| SQL実行結果 | Mapper XMLのSQL定義とJavaからの呼出関係を一部追跡。 | 今回はアプリ・DBを実行していないため、SQLの実発行、DB更新件数、永続化結果。 |
| 受注後業務 | READMEは受注後業務・管理画面を別システム責務とする。 | 別システムの具体的な処理・連携方法。本資料では本リポジトリの実装済みとは扱わない。 |

既存資料とソース確認の差がある場合は、上記「重要な処理・注意点」に分けて記載しています。SQL定義があることは、実行時にそのSQLが呼び出されることや、DBに特定値が保存されたことを意味しません。
