# MyBatisマッパー一覧

## 1. 概要

MyBatis Mapperインターフェースは7本。XMLファイルは `src/main/resources/mappers/` に配置。
各Mapperは対応する `@Repository` を持つか、Serviceから直接使用される。

---

## 2. Mapper一覧

| # | インターフェース | 主な責務 | XML |
|---|---|---|---|
| 1 | `ProductMapper` | 商品一覧・詳細・ランキング・フィルタオプション | ProductMapper.xml |
| 2 | `MemberMapper` | 会員認証・プロフィール・お気に入り・追加送り先 | MemberMapper.xml |
| 3 | `OrderMapper` | 注文登録・注文履歴・注文詳細 | OrderMapper.xml |
| 4 | `CartMapper` | カート表示用スナップショット・税率 | CartMapper.xml |
| 5 | `ContactMapper` | お問い合わせ保存・会員プリフィル | ContactMapper.xml |
| 6 | `AnnouncementMapper` | お知らせ取得 | AnnouncementMapper.xml |
| 7 | `BatchMapper` | ランキング・レコメンド計算バッチ用 | BatchMapper.xml |

---

## 3. ProductMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `countProducts` | `Long` | `Map<String,Object> params` | 検索条件に合う商品件数 |
| `selectProducts` | `List<ProductListMapperRow>` | `Map<String,Object> params` | 商品一覧（ページング・動的WHERE） |
| `selectProductsByIds` | `List<ProductListMapperRow>` | `productIds, now` | ID指定で複数商品取得 |
| `selectColorCodes` | `List<ProductColorCodeMapperRow>` | `productIds` | 一覧カード用カラーコード |
| `selectTopRankedProducts` | `List<ProductRankedMapperRow>` | `limit` | ランキング上位商品 |
| `selectProductDetail` | `ProductDetailMapperRow` | `productId, now` | 商品詳細（ヘッダ部） |
| `selectProductVariants` | `List<ProductVariantMapperRow>` | `productId` | 選択可能なカラーバリアント |
| `selectSeriesLinks` | `List<ProductSeriesLinkMapperRow>` | `variationGroupId, now` | 同シリーズ商品リンク |
| `selectRecommendedProducts` | `List<ProductRankedMapperRow>` | `sourceProductId, limit` | おすすめ関連商品 |
| `selectActiveColorFilterOptions` | `List<ProductFilterColorOptionMapperRow>` | なし | カラー絞り込みオプション |
| `selectActiveDeskTopShapeOptions` | `List<ProductFilterOptionMapperRow>` | なし | デスク天板形状オプション |
| `selectActiveDeskTasteOptions` | `List<ProductFilterOptionMapperRow>` | なし | デスクテイストオプション |
| `selectActiveChairFunctionOptions` | `List<ProductFilterOptionMapperRow>` | なし | チェア機能オプション |
| `selectActiveChairMaterialOptions` | `List<ProductFilterOptionMapperRow>` | なし | チェア素材オプション |
| `selectActiveChairTasteOptions` | `List<ProductFilterOptionMapperRow>` | なし | チェアテイストオプション |
| `selectActiveStorageUsageOptions` | `List<ProductFilterOptionMapperRow>` | なし | 収納棚用途オプション |
| `selectActiveStorageTasteOptions` | `List<ProductFilterOptionMapperRow>` | なし | 収納棚テイストオプション |
| `selectCurrentTaxRatePercent` | `BigDecimal` | `now` | 現在の消費税率 |

---

## 4. MemberMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `existsByEmail` | `Boolean` | `email` | メール重複チェック |
| `existsByEmailForOtherMember` | `Boolean` | `email, memberId` | 他会員のメール重複チェック |
| `selectActiveCredentialByEmail` | `MemberCredential` | `email` | ログイン認証用資格情報 |
| `selectActiveById` | `MemberSessionUser` | `memberId` | セッション用会員情報 |
| `selectProfileByMemberId` | `MemberProfileEditMapperRow` | `memberId` | プロフィール編集用情報 |
| `insertMember` | `Long` | `form, passwordHash` | 会員登録・IDを返す |
| `selectMemberTypeById` | `String` | `memberId` | 会員種別（general/corporate） |
| `countAdditionalAddresses` | `Long` | `memberId` | 追加送り先件数 |
| `selectAdditionalAddresses` | `List<MemberAdditionalAddressView>` | `memberId, limit, offset` | 追加送り先一覧（ページング） |
| `selectAdditionalAddressById` | `MemberAdditionalAddressView` | `memberId, memberAddressId` | 追加送り先1件 |
| `insertAdditionalAddress` | `int` | `memberId, form` | 追加送り先登録 |
| `updateAdditionalAddress` | `int` | `memberId, memberAddressId, form` | 追加送り先更新 |
| `deleteAdditionalAddress` | `int` | `memberId, memberAddressId` | 追加送り先削除 |
| `countFavorites` | `Long` | `memberId, now` | お気に入り件数（有効商品のみ） |
| `selectFavorites` | `List<MemberFavoriteProductMapperRow>` | `memberId, limit, offset, now` | お気に入り一覧 |
| `selectFavoriteColorCodes` | `List<MemberColorCodeMapperRow>` | `productIds` | お気に入りカラーコード |
| `existsFavorite` | `Boolean` | `memberId, productId` | お気に入り登録済みか確認 |
| `insertFavorite` | `int` | `memberId, productId` | お気に入り追加 |
| `deleteFavorite` | `int` | `memberId, productId` | お気に入り削除 |
| `updateProfile` | `int` | `memberId, form` | プロフィール更新 |
| `withdrawMember` | `int` | `memberId, now` | 退会処理（退会状態に更新） |

---

## 5. OrderMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `selectCheckoutMemberPrefill` | `CheckoutMemberPrefill` | `memberId` | 注文入力フォーム初期値 |
| `insertOrder` | `Long` | `Map<String,Object> params` | 注文ヘッダ登録・IDを返す |
| `insertOrderItem` | `int` | `Map<String,Object> params` | 注文明細登録 |
| `insertOrderStatusHistory` | `int` | `Map<String,Object> params` | ステータス履歴登録 |
| `nextOrderSequence` | `Integer` | `orderDate` | 注文番号用連番（当日分） |
| `selectOrderCompleteByOrderNumber` | `OrderCompleteMapperRow` | `orderNumber` | 完了画面・完了メール用情報 |
| `countMemberOrders` | `Long` | `memberId` | 注文履歴件数 |
| `selectMemberOrders` | `List<MemberOrderHistoryMapperRow>` | `memberId, limit, offset` | 注文履歴一覧 |
| `selectMemberOrderDetail` | `MemberOrderDetailMapperRow` | `memberId, orderNumber` | 注文詳細ヘッダ＋金額サマリー |
| `selectMemberOrderStatusHistories` | `List<MemberOrderStatusHistoryMapperRow>` | `memberId, orderNumber` | ステータス履歴（時系列） |
| `selectMemberOrderItems` | `List<MemberOrderItemMapperRow>` | `memberId, orderNumber` | 注文詳細の商品行 |
| `selectMemberReorderItems` | `List<OrderReorderItemMapperRow>` | `memberId, orderNumber` | 再注文用商品情報 |
| `selectCurrentTaxRatePercent` | `BigDecimal` | `now` | 現在の消費税率 |

---

## 6. CartMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `selectCartProductSnapshots` | `List<CartProductSnapshot>` | `productVariantIds, now` | カート内商品のスナップショット |
| `selectCurrentTaxRatePercent` | `BigDecimal` | `now` | 現在の消費税率 |

---

## 7. ContactMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `selectMemberContactPrefill` | `ContactMemberPrefill` | `memberId` | お問い合わせフォーム初期値 |
| `insertInquiry` | `Long` | `memberId, form` | お問い合わせ登録・IDを返す |

---

## 8. AnnouncementMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `selectActiveAnnouncements` | `List<AnnouncementMapperRow>` | `limit, now` | 公開中のお知らせ（新着順） |

---

## 9. BatchMapper メソッド一覧

| メソッド | 戻り値 | 引数 | 概要 |
|---|---|---|---|
| `selectPopularRankingCandidates` | `List<BatchPopularRankingCandidateMapperRow>` | `sinceAt, asOf` | ランキング計算用売上集計 |
| `deletePopularRankingsByDate` | `int` | `rankingDate` | ランキングデータ削除（当日分） |
| `insertPopularRanking` | `int` | `rankingDate, rank, productId, soldQuantity1m` | ランキング1件保存 |
| `selectOrderProductOccurrences` | `List<BatchOrderProductOccurrenceMapperRow>` | `sinceAt` | 同一注文内の商品ペア抽出 |
| `deleteRecommendedRelatedByDate` | `int` | `recommendationDate` | レコメンドデータ削除（当日分） |
| `insertRecommendedRelated` | `int` | `recommendationDate, sourceProductId, rank, recommendedProductId, score` | レコメンド1件保存 |
