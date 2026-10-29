# FEAT-002: DB・トランザクション設計（最終版）

## 1. coupons

- `id` PK
- `code` UNIQUE
- `name`
- `discount_type` (`fixed_amount` / `percentage`)
- `discount_value`
- `max_discount_amount`（定率のみ、整数円）
- `min_purchase_amount`（整数円）
- `valid_from` / `valid_to`
- `target_conditions` JSONB
- `max_usage_total` / `max_usage_per_user`
- `used_count_total`
- `is_active`
- `created_at` / `updated_at`

クーポンの物理削除は行わず、`is_active=false` で無効化する。
現行スキーマの対応型は `id` をBIGINT identity、`code` / `name` をVARCHAR、金額をNUMERIC(12,0)、日時をTIMESTAMPTZ、`target_conditions` をJSONBとする候補である。ただし文字列長、定率 `discount_value` の数値精度・許容範囲、各列のNULL/DEFAULT、利用上限NULLの意味、CHECKおよびINDEXの完全定義は人間判断事項。既存類似列から仕様値を推測して確定しない。

## 2. user_coupon_usages

- `id` PK
- `member_id` FK → `members.member_id`
- `coupon_id` FK → `coupons.id`
- `usage_count`
- `last_used_at`
- `created_at` / `updated_at`
- `UNIQUE(member_id,coupon_id)`
- FK削除動作は `RESTRICT` / `NO ACTION`。
既存の `members.member_id` はBIGINT。実績の全カラム型・DEFAULT・CHECK・INDEXおよびFKのON UPDATE規則は人間判断事項。

## 3. orders

追加:
- `applied_coupon_code`
- `applied_coupon_name`
- `coupon_discount_amount`（整数円）

`order_status` と `order_status_histories.status` に `returned` を追加する。
既存の注文状態CHECKは `received`、`awaiting_payment`、`processing`、`completed`、`cancelled` のみ許可するため、両CHECKの変更が必要。
既存 `subtotal_amount` は割引前の商品小計を保持し、割引後の商品小計は注文合計・税の算出に用いる。割引額は追加列 `coupon_discount_amount` に保存する。

`order_items` には `line_coupon_discount_amount` を追加する。`line_subtotal` は割引前小計を維持し、明細税額は明細小計から配賦割引額を引いた課税対象額を既存税率・切り捨てで計算する。明細合計は割引後明細課税対象額と明細税額の合計とする。明細単位の切り捨てによるヘッダー税額との差に追加調整は行わない。

既存行への列追加時は `NOT NULL DEFAULT 0` により初期値を0とする。過去に注文単位のクーポン割引だけを保存し明細配賦を保存していない注文は配賦額を復元できないため、注文詳細では注文全体のスナップショット割引額を表示し、明細別配賦額は表示しない。

## 4. 利用実績加算

次のロック・検証順は資料間で統一する。
1. クーポン行を取得して `FOR UPDATE`。
2. 会員注文では `INSERT ... ON CONFLICT DO NOTHING` により、`usage_count=0` の会員実績行を確保する。既存行は更新しない。
3. 会員実績行を `FOR UPDATE`。
4. ロック取得後にクーポン有効性・会員／商品条件・最低購入金額・全体／会員別利用上限を最終確認し、割引を再計算する。
5. 上限確認後、利用カウンタを一度だけ増加させ、注文とクーポンスナップショットを同一トランザクション内に保存する。
6. すべて成功した場合にCOMMITし、例外時は注文・スナップショット・カウンタ更新をまとめてROLLBACKする。

利用カウンタ更新と注文／スナップショット保存はどちらも手順5の同一トランザクション内で行う。これらの相互順序は既存仕様から一意に決定できず、人間判断事項。

ゲストで個人上限なしの場合は会員別行を作成せず、全体利用数だけを更新する。

## 5. トランザクション

注文確定、クーポン再検証、利用実績更新、注文保存を既存のDBトランザクションで処理する。外部決済APIは使用しない。

## 6. 返還

- `cancelled` または `returned` への状態遷移成功と返還を同一トランザクションで実行する。
- 状態遷移が既に完了している場合は返還しない。
- クーポンマスタは物理削除しないため返還対象を特定できる。
- 全体利用数と会員別利用数を対象注文の利用1件分だけ減算する。
- 返還処理のロック順は注文確定と同じく `coupons` → `user_coupon_usages` とする。現行Mapperの更新順は逆であり変更が必要。
- 注文識別には適用コードではなくcoupon_id等の不変識別子が必要かを含め、注文スナップショットから返還対象を特定する列構成を確定する。現在の設計は適用コードしか保持せず、コード変更後のマスタ結合が保証されないため、識別方法は人間判断事項。
- 返還可能な開始状態と `cancelled`／`returned` 間の遷移可否は人間判断事項。

## 7. 現行DB／Mapperとの対応

- 現行 `orders` と `order_status_histories` のCHECK制約には `returned` がない。
- 現行 `orders` に3つのクーポンスナップショット列がなく、`insertOrder` のINSERT列にもない。
- 現行 `OrderMapper.xml` は380行目で `</mapper>` を閉じ、その後にクーポンSQLが記載されている。XML構造を修正する必要がある。
- 現行会員実績返還SQLは `u.user_id` を参照するが、新設設計は `member_id` を使う。
- 現行SQLにクーポン用のUPSERT、`FOR UPDATE`、利用回数加算SQL、返品状態更新SQLは存在しない。

## 8. DB定義に関する人間判断事項

- クーポン各列および利用実績各列の型、列長、NULL、DEFAULT、CHECK、INDEXの完全定義。
- `discount_value` の定率精度・範囲、および `max_usage_total`／`max_usage_per_user` がNULLの場合の意味。
- `valid_from`／`valid_to` の境界時刻を含むか。
- 注文にcoupon_idを保持するか、コードを不変にするかなど、マスタ変更後の返還用識別方式。
- 各新設FKのON UPDATE動作と、既存 `orders.member_id ON DELETE SET NULL` に対するFK削除方針の適用範囲。
- 注文履歴一覧・詳細のsnapshot表示範囲。
- クーポン物理削除禁止をDB制約またはアプリケーション責務のどちらで強制するか。
- キャンセル／返品の状態遷移可能状態。
- 注文INSERT・利用数加算・スナップショット保存の相互順序。
