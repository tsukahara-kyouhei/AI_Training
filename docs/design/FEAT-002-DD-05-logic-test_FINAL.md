# FEAT-002: ロジック・テスト設計（最終版）

## 1. バリデーション順序

1. 存在確認 → `COUPON_NOT_FOUND`
2. `is_active` → `COUPON_NOT_FOUND`
3. 重複適用 → `COUPON_ALREADY_APPLIED`
4. 有効期間 → `COUPON_EXPIRED`
5. 会員条件 → `COUPON_USER_CONDITION_ERROR`
6. 対象商品／カテゴリOR → `COUPON_TARGET_ERROR`
7. 最低購入金額 → `COUPON_MIN_AMOUNT_ERROR`
8. 全体／会員別利用上限 → `COUPON_USAGE_LIMIT_REACHED`

ゲストで `max_usage_per_user` が設定された場合は会員条件エラーとする。未設定の場合は会員別利用実績を作らず全体上限のみ確認する。

## 2. 割引計算

- 定額: `discount_value`
- 定率: `FLOOR(target_subtotal * discount_value / 100)`
- 最大割引額: 定率のみ
- 最終値: `MIN(算出割引額,target_subtotal)`
- 金額は整数円
- 送料・手数料は割引対象外
- 送料無料判定は割引前の現行課税対象小計（税込み）で行い、5,000円以上で無料、それ未満で800円。割引後に再判定しない
- 税額は割引後商品小計＋組立費を基準に `FLOOR((割引後商品小計 + 組立費) * 税率 / 100)`
- 注文合計は割引後商品小計＋組立費＋割引後基準の税額＋割引前判定の送料
- 明細割引は対象商品小計比で配賦し、整数円の端数は剰余の大きい明細から配る（同率はカート内明細順）。
- 明細税額は割引配賦後の課税対象額を明細ごとに既存税率・切り捨てで計算し、ヘッダーとの差を追加調整しない。
- 明細別切り捨てとヘッダー一括計算により1円差が生じる複数明細ケースを確認する。注文合計にはヘッダー税額を使い、明細税額合計は配賦後金額に対する明細別税額の合計として保持する。

## 3. 境界値

| 観点 | ケース |
|---|---|
| 期間 | 開始ちょうど、終了ちょうど、開始前、終了後 |
| 最低購入 | 同額、1円不足 |
| 対象 | 0件、対象・対象外混在、カテゴリOR・商品OR |
| 0円 | 0円対象商品あり／最低購入0円・正額 |
| 割引 | 定額が小計超過、定率端数、最大割引額境界 |
| 上限 | 全体上限直前・到達、会員別上限直前・到達 |
| 会員 | all、new、completed注文あり／なし |
| ゲスト | 個人上限あり／なし |
| 状態 | 無効、存在しない、重複 |
| 再検証 | 確認時・確定時の期限切れ／上限到達 |

## 4. 排他・トランザクション

- `ON CONFLICT DO NOTHING` による行確保。
- `FOR UPDATE` 後に上限確認・加算。
- ロック順序 `coupons` → `user_coupon_usages`。
- 複数会員同時注文で全体上限超過しない。
- 同一会員同時注文で会員別上限超過しない。
- 注文保存失敗時に利用実績がロールバックされる。
- 利用数更新と注文保存の相互順序は人間判断事項の確定後に、同一トランザクションの順序を検証する。

## 5. 注文・返品

- 注文にコード・名前・割引額が保存される。
- クーポン無効化後も注文履歴のスナップショット表示が維持される。
- `cancelled` 遷移で1回だけ返還。
- `returned` 遷移で1回だけ返還。
- 返品再実行で二重返還しない。
- 部分返品は本機能では発生しない。
- 状態遷移元ごとの可否・キャンセル／返品の相互排他を確認するテストは、人間判断事項で許可遷移が決定した後に定義する。

## 6. JSON

- 必須キー欠落・null・JSON構文不正を登録時に拒否。
- `allowed_category_ids` は `desk` / `chair` / `storage` だけを許可する。
- `allowed_product_ids` は正の整数のみを許可する。
- 配列型不正を登録時に拒否。
- 配列要素型不正を登録時に拒否。
- `target_user_type` の不正値を登録時に拒否。
- 未知キーと存在しない商品IDの登録可否は人間判断事項。

## 7. API・画面

- `POST /api/cart/coupon` の正常・全error_code。
- `DELETE /api/cart/coupon` の正常解除。
- `GET /checkout/confirm` の再検証成功／失敗。
- `POST /checkout/confirm` の最終再検証成功／失敗。
- 注文確定テスト対象は既存MVCの `POST /checkout/confirm`。`/checkout/execute` および `/api/checkout/execute` は設計上追加しない。
- クライアント送信割引額を信頼しない。
- セッション失効時のクーポン解除。
- REST各エラーのHTTPステータス、注文確定エラー時の遷移先・フォーム保持は人間判断事項。
- 適用時会員IDと現在会員IDの照合、会員変更時のクーポン解除、商品追加・削除・数量変更・一括削除後のクーポン保持と確認時再検証をテストする。

## 8. 回帰

クーポン未適用注文、商品・数量変更、送料、税、注文番号、注文明細、ワンタイムトークン、オフライン決済、注文履歴を確認する。

## 9. テスト配置・実行基盤

既存のテスト配置は `src/test/java` 配下のJUnit 5。注文テストとして `OrderServiceTest` と `OrderRepositoryTest` が存在する。クーポン専用テストはない。`pom.xml` にSpring Boot Testはあるが、PostgreSQL Testcontainers依存は確認できない。したがってPostgreSQLの同時実行・トランザクションテストは、既存のDBテスト実行環境を確認してから配置・起動方法を決める事項であり、Testcontainers追加を前提としない。

| 追加テスト対象（新規作成） | 種別 | 観点 |
|---|---|---|
| CouponValidationServiceTest | Unit | 存在、有効期間、会員条件、対象条件、最低金額、利用上限 |
| CouponDiscountCalculatorTest | Unit | 定額、定率FLOOR、最大割引、対象小計上限、0円商品 |
| CouponRepositoryTest | Repository／Mapper | 行確保、`FOR UPDATE`、加算・返還、制約、snapshot |
| CouponCartControllerTest | API | 適用・解除、CSRF、エラー応答、セッション |
| CouponCheckoutFlowTest | MVC | 確認・確定再検証、session、カート変更、会員変更 |
| CouponOrderTransactionTest | PostgreSQL Integration | 上限同時実行、ロールバック、返還、状態履歴 |

上記クラス名は設計上の新規テスト配置案であり、実在クラスではない。PostgreSQL Integrationの実行基盤とエラー応答仕様の確定は人間判断事項。

## 10. 人間判断事項

- 注文明細への割引配賦と明細税額の差額処理。
- キャンセル／返品可能状態と相互排他を検証する状態遷移表。
- RESTエラーHTTPステータス、注文確定エラー時の画面遷移・フォーム保持。
- クーポン管理登録機能を今回の対象範囲に含めるか。
- PostgreSQL統合・競合テストを実行する既存または新規の環境。
- DB列仕様および注文確定更新順序（DD-04の人間判断事項参照）。
