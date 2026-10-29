# FEAT-002: クーポン機能追加 実装TODO（最終版）

以下のチェックリストは実装前に整理した作業項目であり、現在の完了状態を示すチェック表ではない。現在の実装・テスト状態はソース、テスト結果、差分で確認する。

## 1. DB
- [ ] `coupons` テーブル作成
- [ ] `user_coupon_usages` テーブル作成（`member_id` FK、複合UNIQUE、RESTRICT/NO ACTION）
- [ ] `orders` にクーポンスナップショット3項目を追加
- [ ] `order_status` / `order_status_histories.status` に `returned` を追加
- [ ] クーポン物理削除を禁止し `is_active=false` で無効化
- [ ] `target_conditions` の登録時構造検証を実装

## 2. データアクセス
- [ ] `Coupon` / `CouponRepository`
- [ ] `UserCouponUsage` / `UserCouponUsageRepository`
- [ ] `member_id` を使用した会員別利用実績検索・ロック
- [ ] `OrderRepository` / `OrderMapper` にスナップショット保存・取得を追加
- [ ] `returned` 状態の更新・履歴保存SQLを追加

## 3. クーポンロジック
- [ ] 検証順序を存在→有効→重複→期間→会員→対象→最低購入→利用上限で統一
- [ ] カテゴリ条件と商品条件をORで判定
- [ ] `target_subtotal` を対象商品の単価×数量で算出
- [ ] 定率割引をFLOOR計算
- [ ] 最大割引額は定率のみ適用
- [ ] 最終割引額を対象小計以下に制限
- [ ] 0円対象商品は対象条件成立、最低購入金額は別判定
- [ ] ゲストは個人上限ありクーポン不可、個人上限なしは利用可

## 4. クーポン状態・API
- [ ] クーポンコードをセッションへ保存
- [ ] 適用時の `member_id` をセッションに関連付け、会員変更時は同期的に解除
- [ ] `POST /api/cart/coupon`
- [ ] `DELETE /api/cart/coupon`
- [ ] `GET /checkout/confirm` で再検証
- [ ] `POST /checkout/confirm` で最終再検証・利用実績確定
- [ ] `/api/checkout/confirm` と `/api/checkout/execute` は追加しない

## 5. 排他・トランザクション
- [ ] `coupons` → `user_coupon_usages` の順にロック
- [ ] `ON CONFLICT DO NOTHING` で利用実績行を確保
- [ ] `FOR UPDATE` 後に上限確認・1回加算
- [ ] 注文保存と利用実績更新を同一TXで処理
- [ ] キャンセル／返品の状態遷移と返還を同一TXで処理
- [ ] 人間判断で注文INSERT・利用数加算・スナップショット保存の順序を確定し、DD-02～05に反映

## 6. キャンセル・返品
- [ ] `cancelled` 遷移時の利用実績返還
- [ ] `returned` 遷移時の利用実績返還
- [ ] 返品は注文単位、部分返品なし
- [ ] 二重返還防止

## 7. テスト
- [ ] validation/calculation単体テスト
- [ ] REST API結合テスト
- [ ] MVC checkout結合テスト
- [ ] PostgreSQL DDL・制約テスト
- [ ] 全体上限・会員別上限の同時実行テスト
- [ ] ゲスト上限あり／なしテスト
- [ ] セッション保持・失効テスト
- [ ] `returned` 遷移・二重返還テスト
- [ ] `target_conditions` 不正登録テスト
- [ ] 未知キー・存在しない商品IDの登録ルール決定後に該当テスト
- [ ] 既存注文・送料・税・オフライン決済の回帰テスト
- [ ] 注文履歴一覧と注文詳細のどちらへsnapshotを表示するか決定後に表示・取得テスト

## 8. 計算確定事項
- 税額は割引後商品小計＋組立費を基準に算出する。
- 送料無料判定は割引前の現行課税対象小計（税込み）で行う。5,000円以上は無料、それ未満は800円。
- 注文合計は割引後商品小計＋組立費＋税額＋送料。
- 明細割引は対象商品小計比で配賦し、整数円の端数は剰余の大きい明細から配る（同率はカート内明細順）。
- 明細税額は割引配賦後の課税対象額から既存税率・切り捨てで計算し、ヘッダーとの差を追加調整しない。
- カート内容変更後もクーポンコードをセッション保持し、確認画面で再検証する。
- 会員変更時はクーポンをセッションから解除する。

## 9. 実装対象ファイル（確認済み既存ファイル・候補）

| 区分 | ファイル | 内容 |
|---|---|---|
| 変更 | `src/main/java/jp/co/skig/officeorder/web/CartController.java` | API、確認・確定時のセッション連携 |
| 変更 | `src/main/java/jp/co/skig/officeorder/service/cart/CartService.java` | クーポン適用後集計 |
| 変更 | `src/main/java/jp/co/skig/officeorder/service/order/OrderService.java` | 注文確定TX、税、スナップショット、利用数 |
| 変更 | `src/main/java/jp/co/skig/officeorder/repository/OrderRepository.java` | 注文・状態履歴・返還 |
| 変更 | `src/main/java/jp/co/skig/officeorder/mapper/OrderMapper.java` | 注文／返還Mapper契約 |
| 変更 | `src/main/resources/mappers/OrderMapper.xml` | 注文、スナップショット、状態、利用実績SQL |
| 変更 | `src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java` | 認証成功時の会員変更検出とセッションクーポン解除 |
| 変更 | `sql/schema/orders.sql`、`sql/init/init.sql` | 注文列・状態CHECK・新設テーブルの適用 |
| 変更 | `src/main/resources/templates/pages/cart.html`、`checkout-confirm.html`、`checkout-complete.html`、`mypage-order-detail.html` | クーポン操作・金額・snapshot表示 |
| 変更 | `src/main/resources/static/js/site.js` | カートクーポンREST APIの画面連携 |
| 変更 | `src/main/java/jp/co/skig/officeorder/mapper/row/OrderCompleteMapperRow.java`、`src/main/java/jp/co/skig/officeorder/model/order/OrderCompleteView.java`、`src/main/java/jp/co/skig/officeorder/mapper/row/MemberOrderDetailMapperRow.java`、`src/main/java/jp/co/skig/officeorder/model/member/MemberOrderDetailView.java` | 注文完了・注文詳細のsnapshot参照 |
| 変更 | `src/main/java/jp/co/skig/officeorder/web/MyPageController.java`、`src/main/java/jp/co/skig/officeorder/service/order/OrderService.java`、`src/main/java/jp/co/skig/officeorder/repository/OrderRepository.java` | 注文詳細表示のデータ取得経路 |
| 新規 | `CouponMapper`、`CouponMapper.xml`、クーポン用Request/Response型 | REST Controller／Mapper／JSON DTOの具体構成とJSON契約は人間判断事項 |
| 新規 | `src/main/java/jp/co/skig/officeorder/model/coupon/Coupon.java`、`CouponErrorCode.java`、`CouponValidationResult.java`、`src/main/java/jp/co/skig/officeorder/repository/CouponRepository.java`、`src/main/java/jp/co/skig/officeorder/service/coupon/CouponValidationException.java`、`CouponValidationService.java`、`src/main/java/jp/co/skig/officeorder/mapper/row/UserCouponUsageMapperRow.java` | OrderServiceにimport記載があるが定義は存在しない。新規作成が必要 |
| 新規 | `src/test/java/jp/co/skig/officeorder/service/coupon/CouponValidationServiceTest.java`、`src/test/java/jp/co/skig/officeorder/repository/CouponRepositoryTest.java`、`src/test/java/jp/co/skig/officeorder/web/CouponCartControllerTest.java`、`src/test/java/jp/co/skig/officeorder/web/CouponCheckoutFlowTest.java`、`src/test/java/jp/co/skig/officeorder/service/order/CouponOrderTransactionTest.java` | DD-05の新規テスト配置案。PostgreSQL統合テスト実行基盤は人間判断事項 |

## 10. 人間判断事項（着手前に確定）
- キャンセル／返品の許可遷移元と `cancelled`／`returned` 間の相互遷移。
- RESTエラーHTTPステータス、MVC確定失敗時の戻り先・表示・フォーム保持。
- クーポンマスタ登録機能をFEAT-002に含めるか（既存管理登録経路なし）。
- 新設DB列の型・長さ・NULL/DEFAULT・CHECK/INDEX、定率範囲、日時境界、上限NULLの意味、FK ON UPDATE。
- クーポンマスタ変更後の返還識別キー。
- 注文INSERT・利用数加算・snapshot保存の相互順序。
- PostgreSQL同時実行テストの実行環境。
- 注文履歴snapshotの表示先（既存一覧・詳細画面のどこまでを対象とするか）。
- クーポン適用APIの確定JSON field名・型、`subtotal`／`total` が税・送料を含むか。
- クーポン物理削除禁止をDB制約またはアプリケーション責務のどちらで強制するか。
