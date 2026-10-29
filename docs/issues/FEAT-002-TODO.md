# FEAT-002: クーポン機能追加 実装計画 (TODOリスト)

> **注意**: 以下のTODOは、既存実装 (`OrderService`, `OrderRepository`, `CartService`, `CartController`, MyBatis Mapper/XML, PostgreSQL schema) と設計書 (`FEAT-002-DD-01`〜`05`) を照合して作成された。既存実装に存在しないクラス・テーブル・ステータス・方式を前提にしない。

## 0. 既存実装の調査

既存ソースコード・DB・設計を確認した結果、以下の事項を把握した。各項目の詳細は各セクションに反映する。

- [x] **DBマイグレーション方式を確認** — `sql/schema/*.sql` に DDL を追加し、`sql/init/init.sql` の `\ir` 列挙に追加する。Liquibase や Flyway は未使用。PostgreSQL 方言（`GENERATED ALWAYS AS IDENTITY`, `TIMESTAMPTZ`, `JSONB`, `CHECK`, `ON CONFLICT`）
- [x] **Model/Repository構成を確認** — MyBatis `@Mapper` インターフェース + `src/main/resources/mappers/*.xml` + `@Repository` で Mapper をラップする Repository クラス。モデルは Java `record`、DB 結果専用に `*MapperRow` record を別途用意
- [x] **Service/Domain構成を確認** — `@Service` でビジネスロジックを保持。`OrderService.placeOrder()` が `@Transactional` で注文確定を一括処理。`CartService` は Cookie ベースのカート操作を担う
- [x] **Controller/API構成を確認** — すべて `@Controller` (MVC + Thymeleaf) テンプレート返却。`@RestController` は `InternalBatchController`（`/internal/batch`）のみ。クーポン API は設計書通り `@RestController` JSON レスポンスを新設
- [x] **カート処理を確認** — `CartCookieStore` で Cookie に `variantId:quantity:assembly` トークンを保存。`CartService.getCart()` が Cookie + DB スナップショットから `CartView` を構築。CartView/CartSummaryView にクーポン関連フィールドは未追加
- [x] **注文確認・注文確定処理を確認** — `CartController.checkoutConfirm()` (GET `/checkout/confirm`) は確認画面表示。`CartController.placeOrder()` (POST `/checkout/confirm`) が `OrderService.placeOrder()` を呼び出す。ワンタイムトークンによる二重送信防止済み
- [x] **決済処理とDBトランザクション境界を確認** — **外部決済APIは存在しない**。決済手段は `bank_transfer` / `cash_on_delivery` / `convenience_store`（すべてオフライン）。`convenience_store` は支払案内 JSON（支払番号・期限）を生成するのみ。`placeOrder()` は1つの `@Transactional` で受注・明細・ステータス履歴を一括保存し、コミット後に完了メールを送信
- [x] **注文キャンセル・返品処理を確認** — **キャンセル・返品処理は未実装**。DBの `order_status` に `cancelled` が存在するが、遷移させるコードはない。返品専用ステータス（`returned` 等）も存在しない。`order_status_histories` テーブルは存在する
- [x] **テストの記述方式・命名規則を確認** — JUnit 5 + Mockito + AssertJ。`@Nested` + `@DisplayName` でグループ化、メソッド名は snake_case、テストIDは `ORD-001` のように付番。`TransactionSynchronizationManager.initSynchronization()` / `clearSynchronization()` でトランザクション境界をシミュレート。Mockito モックによる手動 DI（`@SpringBootTest` は使用しない）
- [x] **上記調査結果に合わせて実装場所・実装方式を確定** — 設計書・既存実装の対応表は各セクションの「実装方針」に記載

### 0.1 調査結果の対応表

| 設計書での前提 | 実際の既存実装 | 設計書 DD-04 との整合 |
|---|---|---|
| `OrderCheckoutService`（注文確定） | `OrderService.placeOrder()` (`@Transactional`) | DD-04 の「外部決済API」前提は不一致。既存はオフライン決済1本のトランザクション |
| `OrderCancelService`（キャンセル・返品） | **未実装**。`cancelled` ステータスのみ存在。返品専用ステータスなし | キャンセルは新規作成 / 返品は別処理としてスコープ確定（§3.4, §3.5参照） |
| 外部決済APIとの非同期トランザクション | 外部決済APIは**存在しない**。全てオフライン | DD-04 2.2 の「外部決済API」仮定を調査で否定。同期1TX方式に決定（§3.3, §7参照） |
| 注文ステータス `COMPLETED` / `SHIPPED` | `received` / `awaiting_payment` / `processing` / `completed` / `cancelled` | `shipped` は存在しない。新規会員判定は `completed` のみ（§2.3, §7.3参照） |
| `Cart.vue` / `CheckoutConfirm.vue` | Thymeleaf `cart.html` / `checkout-confirm.html` | 設計書のファイル名を実際のテンプレート名に修正（§5参照） |
| `@RestController` JSON API | 全コントローラが `@Controller` (MVC) | クーポン API は `@RestController` を新設（既存コードの唯一の例: `InternalBatchController`） |
| `user_coupon_usages` の初回行確保 | PostgreSQL の `INSERT ... ON CONFLICT (member_id, coupon_id) DO NOTHING` | DD-04 §4。競合時に既存利用回数を変更しない |
| 会員別利用回数更新 | 行確保後に `SELECT ... FOR UPDATE`、上限確認後に一度だけ加算 | DD-04 §4。UPSERTで利用回数を加算しない |

## 1. データベース・マイグレーション

### 実装方針
- 新規テーブルは `sql/schema/coupons.sql` に DDL を追加し、`sql/init/init.sql` に `\ir schema/coupons.sql` を追記
- `orders` テーブルの拡張は `sql/schema/coupons.sql` に `ALTER TABLE orders ADD COLUMN IF NOT EXISTS` で追加（既存テーブルがデータを保持する可能性を考慮）
- PostgreSQL 方言に統一: `GENERATED ALWAYS AS IDENTITY`, `TIMESTAMPTZ`, `NUMERIC`, `JSONB`

### 1.1 `coupons` テーブル（新規: `sql/schema/coupons.sql`)
- [ ] `coupons` テーブルの DDL 作成
  - `id` (BIGINT, PK, `GENERATED ALWAYS AS IDENTITY`)
  - `code` (VARCHAR(50), UNIQUE NOT NULL)。PostgreSQLのUNIQUE制約が一意性と検索用インデックスを提供するため、同じ列への別INDEXは追加しない
  - `name` (VARCHAR(100), NOT NULL)
  - `discount_type` (VARCHAR(20), NOT NULL, `CHECK (discount_type IN ('fixed_amount', 'percentage'))`)
  - `discount_value` (精度・スケールはDD-01 §6.4の判断後に確定、NOT NULL、0以上)
  - `max_discount_amount` (精度・スケールはDD-01 §6.4の判断後に確定、NULL可、設定時0以上) — 定率割引の最大割引額
  - `min_purchase_amount` (精度・スケールはDD-01 §6.4の判断後に確定、NOT NULL、DEFAULT 0、0以上)
  - `valid_from` (TIMESTAMPTZ, NOT NULL)
  - `valid_to` (TIMESTAMPTZ, NOT NULL)
  - `target_conditions` (JSONB, NOT NULL) — `allowed_category_ids`, `allowed_product_ids`, `target_user_type`
  - `max_usage_total` (INT, NULLまたは0以上) — 全体利用上限
  - `max_usage_per_user` (INT, NULLまたは0以上) — 会員別利用上限
  - `used_count_total` (INT, NOT NULL, DEFAULT 0, 0以上)
  - `is_active` (BOOLEAN, NOT NULL, DEFAULT TRUE)
  - `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
  - `CHECK (valid_to > valid_from)`
  - 金額列・利用上限・利用回数に非負CHECK制約を設定する。金額精度・小数円の扱いはDD-01 §6.4の人間判断後に確定する
- [ ] `sql/init/init.sql` に `\ir schema/coupons.sql` を追加

### 1.2 `user_coupon_usages` テーブル（新規: `sql/schema/coupons.sql`)
- [ ] `user_coupon_usages` テーブルの DDL 作成
  - `id` (BIGINT, PK, `GENERATED ALWAYS AS IDENTITY`)
  - `member_id` (BIGINT, NOT NULL, FK → `members(member_id)`)
  - `coupon_id` (BIGINT, NOT NULL, FK → `coupons(id)`)
  - `usage_count` (INT, NOT NULL, DEFAULT 0)
  - `last_used_at` (TIMESTAMPTZ, NULL)
  - `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
  - **UNIQUE KEY** (`member_id`, `coupon_id`) — PostgreSQLでは `UNIQUE (member_id, coupon_id)` で定義
  - 会員FK・クーポンFKのON DELETE動作はDD-01 §6.5に基づき決定
  - ON UPDATEは既存スキーマのPostgreSQL既定動作 `NO ACTION`
  - 利用回数・利用上限の非負制約を設定

### 1.3 `orders` テーブル拡張（`sql/schema/coupons.sql` に追加）
- [ ] `ALTER TABLE orders ADD COLUMN IF NOT EXISTS` で以下3カラムを追加
  - `applied_coupon_code` (VARCHAR(50), NULL)
  - `applied_coupon_name` (VARCHAR(100), NULL) — DD-01 §1 で追加が決定
  - `coupon_discount_amount` (NUMERIC(12,0), NOT NULL, DEFAULT 0, `CHECK (coupon_discount_amount >= 0)`)

### 1.4 注文ステータス値の修正（DD-01・DD-04・DD-05 との整合）
- [x] 新規会員判定は既存DBに存在する `completed` の過去注文のみを対象とする。`shipped` 状態は存在しない
  - `SHIPPED` ステータスは**存在しない**
  - 新規会員判定は `order_status = 'completed'` で判定する（§2.3で対応）

## 2. モデル・リポジトリ (データアクセス層)

### 実装方針
- MyBatis `@Mapper` インターフェース + XML マッパーのペアで実装
- Repository は Mapper をラップし `@Repository` を付与
- モデルは Java `record`、DB 結果専用に `*MapperRow` record を別途作成
- 時刻依存クエリは `AppTimeProvider` を注入して `nowOffsetDateTime()` を使用

### 2.1 `Coupon` モデル・リポジトリ
- [ ] `CouponMapper` インターフェース (`src/main/java/jp/co/skig/officeorder/mapper/CouponMapper.java`) 作成
  - `selectByCode(@Param("code") String code)` — コードで検索
  - `selectByCodeForUpdate(@Param("code") String code)` — `FOR UPDATE` 付き検索
  - `incrementUsedCountTotal(@Param("couponId") long couponId)` — 全体利用回数インクリメント
  - `decrementUsedCountTotal(@Param("couponId") long couponId)` — 全体利用回数デクリメント（0未満にならない）
- [ ] `CouponMapper.xml` (`src/main/resources/mappers/CouponMapper.xml`) 作成
  - `selectByCode`: `SELECT ... FROM coupons WHERE code = #{code} AND is_active = TRUE`
  - `selectByCodeForUpdate`: 同上 + `FOR UPDATE`
  - `incrementUsedCountTotal`: `UPDATE coupons SET used_count_total = used_count_total + 1 WHERE id = #{couponId}`
  - `decrementUsedCountTotal`: `UPDATE coupons SET used_count_total = used_count_total - 1 WHERE id = #{couponId} AND used_count_total > 0`（ガード付きUPDATE。作用行数0の検知はRepository/Service側で行う。`GREATEST(0, ...)` は使用しない。§3.6参照）
- [ ] `CouponRepository` (`src/main/java/jp/co/skig/officeorder/repository/CouponRepository.java`) 作成
  - `findByCode(String code)` / `findByCodeForUpdate(String code)` — `CouponMapperRow` → `Coupon` 変換
  - `incrementTotalUsage(long couponId)` / `decrementTotalUsage(long couponId)`

### 2.2 `UserCouponUsage` モデル・リポジトリ
- [ ] `UserCouponUsageMapper` インターフェース (`src/main/java/jp/co/skig/officeorder/mapper/UserCouponUsageMapper.java`) 作成
  - 行確保: `INSERT ... (member_id, coupon_id, usage_count) VALUES (..., ..., 0) ON CONFLICT (member_id, coupon_id) DO NOTHING`。競合時は既存行・回数を変更しない
  - `selectByMemberIdAndCouponIdForUpdate(@Param("memberId") long memberId, @Param("couponId") long couponId)` — 行確保後の排他ロック付き検索
  - 上限確認後に `usage_count = usage_count + 1` を一度だけ実行し、`last_used_at` と `updated_at` を更新
  - `decrementForUpdate(@Param("memberId") long memberId, @Param("couponId") long couponId)` — `WHERE usage_count > 0` ガード付きデクリメント（作用行数0を検知。`GREATEST(0, ...)` は使用しない。§3.6参照）
- [ ] `UserCouponUsageMapper.xml` (`src/main/resources/mappers/UserCouponUsageMapper.xml`) 作成
- [ ] `UserCouponUsageRepository` (`src/main/java/jp/co/skig/officeorder/repository/UserCouponUsageRepository.java`) 作成
  - `ensureRowAndLock(long memberId, long couponId)` — 同一トランザクションで行確保後にロック取得
  - `incrementUsage(long memberId, long couponId)` — ロック・上限確認後に一度だけ加算
  - `decrementUsage(long memberId, long couponId)` — FOR UPDATE 後にガード付きデクリメント（`WHERE usage_count > 0`、作用行数0を検知）

### 2.3 `OrderRepository` の拡張
- [ ] `findByOrderNumberForUpdate(String orderNumber)` — 注文取得時にクーポン行より先に注文行をロックしない。返還フローのロック順序はDD-04 §5に従う
- [ ] `countCompletedOrdersByMemberId(long memberId)` — 新規会員判定用
  - 既存 `countMemberOrders(memberId)` は全ステータスをカウントするため、**新規メソッド**を追加
  - SQL: `SELECT COUNT(*) FROM orders WHERE member_id = #{memberId} AND order_status = 'completed'`
  - 新規会員判定は要件・DD-01どおり `completed` のみ
- [ ] `updateOrderCouponSnapshot(long orderId, String couponCode, String couponName, BigDecimal discountAmount)` — 注文確定時スナップショット保存
- [ ] `updateOrderStatusIfNotCancelled(orderId, newStatus)` — キャンセル処理の冪等化（既に `cancelled` の場合は0件更新、§3.4参照）
- [ ] 返品処理用の状態更新メソッド（§3.5 の採択方式に応じて追加）
  - (a) `returned` ステータス案: `updateOrderStatusIfNotReturned(orderId)`（既に `returned` / `cancelled` の場合は0件更新）
  - (b) 返品レコードテーブル案: `insertOrderReturn(orderId, ...)` + 一意制約（`order_id` UNIQUE）による既返品検出
  - ※ 実装前に §3.5 のスコープ確定が必要

## 3. サービス・ドメインロジック (ビジネスロジック層)

### 3.1 `CouponValidationService` (適用可否判定ロジック)
- [ ] `CouponValidationService` (`src/main/java/jp/co/skig/officeorder/service/coupon/CouponValidationService.java`) 作成
  - [ ] マスタ存在・有効フラグ・有効期間のチェック — `COUPON_NOT_FOUND` / `COUPON_EXPIRED`
  - [ ] 重複適用チェック（既にカートに別クーポンが存在するか） — `COUPON_ALREADY_APPLIED`
  - [ ] 会員条件チェック（`target_user_type == 'new'` 時、`completed` ステータス注文0件） — `COUPON_USER_CONDITION_ERROR`
    - 新規会員判定は要件・DD-01どおり `completed` のみ
  - [ ] 対象商品（カテゴリ条件 `allowed_category_ids` と商品ID条件 `allowed_product_ids`の **OR**）の抽出と小計(`target_subtotal`)計算 — `COUPON_TARGET_ERROR`
    - `products.category_id` は `'desk'` / `'chair'` / `'storage'` の文字列値
  - [ ] 最小購入金額チェック（**対象商品の小計** `target_subtotal >= min_purchase_amount`） — `COUPON_MIN_AMOUNT_ERROR`
  - [ ] 利用回数上限チェック（`used_count_total >= max_usage_total` または `usage_count >= max_usage_per_user`） — `COUPON_USAGE_LIMIT_REACHED`
  - [ ] 各チェックNG時に対応する `error_code` を返す（例外または Result オブジェクト）

### 3.2 `CouponCalculationService` (割引額計算ロジック)
- [ ] `CouponCalculationService` (`src/main/java/jp/co/skig/officeorder/service/coupon/CouponCalculationService.java`) 作成
  - [ ] 定額(`fixed_amount`)割引の計算: `raw_discount = discount_value`
  - [ ] 定率(`percentage`)割引の計算: `raw_discount = FLOOR(target_subtotal * discount_value / 100)`（端数切り捨て）
    - Java では `BigDecimal` の `RoundingMode.DOWN` または `intValue()` で Floor を実現
  - [ ] `max_discount_amount` を定率割引にのみ適用する（定額割引には適用しない。要件 §3.2）
  - [ ] 割引額が対象小計を超えない最終調整: `final_discount = MIN(raw_discount, target_subtotal)`
  - [ ] 割引後の合計は0円未満にならないことを保証

### 3.3 `OrderService` — 注文確定時のクーポン再検証・利用実績確保
- [ ] **実装方針**: DD-04 §2.2 の「外部決済API」は存在しない。既存 `placeOrder()` は1つの `@Transactional` で完結する
  - DD-04 が提案する「TX1: 仮確保 → 決済API → TX2: 確定/解放」の2段階方式は**不要**
  - 理由: 外部決済APIがないため「決済成功・DB失敗不整合」「決済APIタイムアウト」リスクは不存在
  - `placeOrder()` 内で以下の順序で処理する:
    1. カート再バリデーション（既存: `revalidateCartStock`）
    2. **クーポン再検証**（DD-04 §5の順序で `coupons` をロックし、必要な会員別実績行を確保・ロック後に `CouponValidationService`） — DD-05 §1.1 の判定順序
    3. **クーポン使用回数の確保 + 注文保存**（上限確認後に `coupons.used_count_total` と `user_coupon_usages.usage_count` を各1回加算し、`orders` にスナップショット3項目を保存）
    4. 注文明細・ステータス履歴保存（既存ロジック）
    5. コミット後メール送信（既存ロジック）
- [ ] **「注文確定」「注文完了」「completed」の関係を定義（重要）**
  - 既存 `placeOrder()` は注文を **`received`（受付）ステータス**で作成し、ステータス履歴にも `received` を記録する（`order_status` の DEFAULT は `'received'`）
  - **「注文確定」** = `placeOrder()` の実行 = 注文受付（`received` でDB保存・コミット）を行う、ユーザーが注文確定ボタンを押下する時点
  - **「注文完了」** = 注文完了画面（`/checkout/complete/{orderNumber}`）の表示 = 注文確定成功の直後の画面。DD-02 1.3 の「注文完了画面へ遷移」と同一イベント（同一トランザクション内で成立）
  - **`completed` ステータス** = 受付 → 入金待ち → 処理中 → **完了**、のフルフィルメント全体の最終ステータス。Webフロントには `completed` へ遷移させるコードは存在せず、バックオフィス/シードデータが `back-office` として遷移させる
  - **クーポン使用回数の確定は `completed` ステータス到達時ではない**。注文確定（= `received` での保存・コミット）時点で確定する（DD-01 §1「注文確定時（決済直前・直後）」、カゴ落ちによる他ユーザーの利用機会損失を防ぐ趣旨に一致）
  - ※ 要件・元TODOの「注文完了時に使用回数を確定」は、この整理における「注文確定成功 = 注文完了画面到達」を意味する。`completed` ステータスへの追従ではない
- [ ] `OrderService.placeOrder()` にクーポン再検証ロジックを組み込む
  - `CouponValidationService` と `CouponCalculationService` を注入
  - 再検証NG時は `COUPON_*` `error_code` に対応する例外をスローし、`CartController` が 409 レスポンスに変換
  - `OrderCheckoutService` は存在しないため、`OrderService` に直接実装する
- [ ] `orders` テーブルへのスナップショット3項目（`applied_coupon_code`, `applied_coupon_name`, `coupon_discount_amount`）の保存
- [ ] **決済失敗時のクーポン使用回数・注文データ整合性**
  - 既存システムはオフライン決済のため決済API失敗はないが、`placeOrder()` 内の例外によるロールバックでクーポン使用回数が不整合にならないことを保証
  - テスト: `OrderServiceTest` に「注文保存失敗時にクーポン使用回数がインクリメントされない」テストを追加

### 3.4 `OrderCancelService` — キャンセル処理（新規作成）
- [ ] `OrderCancelService` (`src/main/java/jp/co/skig/officeorder/service/order/OrderCancelService.java`) 作成
  - **背景**: キャンセル処理は既存システムに未実装。`order_status` に `cancelled` が存在するが遷移コードはない
  - **対象状態**: 既存ステータスは `received` / `awaiting_payment` / `processing` / `completed` / `cancelled`。どの状態をキャンセル可能とするかはDD-01 §6.1で決定する
  - [ ] キャンセル処理: 選択された対象状態から `cancelled` へ遷移し、ステータス履歴を記録
  - [ ] キャンセル時に、注文にクーポンが適用されている場合のみ使用回数をデクリメント
    - 判定: `orders.applied_coupon_code IS NOT NULL`。割引額が0でも注文確定時に利用回数を加算していれば返還する
  - [ ] キャンセル処理の冪等化
    - 既に `cancelled` ステータスの注文に対して再度キャンセル処理を実行しても**二重減算しない**
    - 実装方式: `UPDATE orders SET order_status = 'cancelled' WHERE order_id = ? AND order_status != 'cancelled'` とし、作用行数が0の場合は既にキャンセル済みとしてデクリメントをスキップ
  - [ ] `@Transactional` 内でDD-04 §5の順にクーポン全体行、会員別実績行をロックしてから状態遷移・回数確認・返還を行う。デクリメントは `WHERE usage_count > 0` ガード + 作用行数0検知（§3.6参照）

### 3.5 `OrderReturnService` — 返品処理（新規作成・キャンセルとは別処理）
- [ ] `OrderReturnService` (`src/main/java/jp/co/skig/officeorder/service/order/OrderReturnService.java`) 作成
  - **背景**: **返品処理は既存システムに存在しない**（`order_status` に返品専用ステータス `returned` 等はない。CHECK 制約は `received`, `awaiting_payment`, `processing`, `completed`, `cancelled` の5値のみ）
  - **キャンセルと返品を同一処理にしない理由**:
    - キャンセルは未出荷・未納品の注文全体を取り消す（`cancelled` へ遷移）
    - 返品は出荷・納品済み（`completed` 等）の注文に対する返却・返金であり、注文全体を `cancelled` に遷移させるのは不正（出荷実績・ステータス履歴と矛盾する）
    - DD-05 §2 のテスト表は「注文キャンセル時、および返品時」の両トリガー（キャンセル・返品）を別々に列挙しており、同一処理としては扱っていない
  - [ ] **返品処理のスコープを定義する（実装前に確定）**
    - 既存DBに返品ステータスがないため、以下のいずれかを採択してTODOの実装対象を確定する
      - (a) `orders.order_status` の CHECK 制約へ `returned`（返品完了）を追加し、`returned` への遷移として返品を表現する
      - (b) 返品レコード専用テーブル（例: `order_returns`）を新設し、注文ステータスは変更せず返品実績を管理する
    - ※ 採択前に設計書との整合確認が必要。既存実装に存在しないステータス・テーブルを勝手に前提としない
  - [ ] 返品時に使用回数をデクリメント（クーポン適用注文のみ）
  - [ ] **返品処理の冪等化（二重減算防止）**
    - 返品処理を再実行しても二重減算しないこと
    - 実装方式: (a) なら `UPDATE orders SET order_status = 'returned' WHERE order_id = ? AND order_status != 'returned'`、(b) なら返品レコードの一意制約（例: `order_id` に UNIQUE）で既返品を検出し、デクリメントをスキップ
  - [ ] キャンセルと返品の対象状態・相互排他はDD-01 §6.1で業務判断後に確定する。集計利用回数の0超過ガードだけでは注文単位の二重返還を判定できないため、返還済み判定に代用しない

### 3.6 使用回数デクリメントの安全性（冪等化と異常検知の両立）
- [ ] **デクリメントSQLに `GREATEST(0, usage_count - 1)` を使用しない（最終レビューで修正）**
  - `GREATEST(0, ...)` によるクランプは「0未満を0に丸める」だけで、**既に0の状態で減算しようとした異常を検知できない**（二重減算やデータ不整合を握りつぶす）
- [ ] **デクリメントは「ガード付きUPDATE + 作用行数チェック」で実装する**
  - `UPDATE user_coupon_usages SET usage_count = usage_count - 1 WHERE member_id = ? AND coupon_id = ? AND usage_count > 0`
  - `UPDATE coupons SET used_count_total = used_count_total - 1 WHERE id = ? AND used_count_total > 0`
  - **0未満にならない**: `WHERE usage_count > 0` により構造的に0未満への減算を防ぐ
  - **異常を検知する**: 作用行数が0の場合、既存のエラー通知・ログ方針に従って異常を表面化させ、処理を成功扱いにしない
- [ ] **保護の責務分離を明確化**
  - 二重減算の防止（冪等化）: 選択された返品記録方式に応じた注文単位の状態遷移または一意な記録で担保。集計カウントだけで冪等化しない
  - 異常な減算の検知: ガード付きデクリメントの作用行数0を検知してログ/例外で通知
  - 両者は別レイヤーであり、算術クランプだけに頼らない

## 4. API・コントローラ層

### 実装方針
- クーポン API は `@RestController` で JSON レスポンスを返す（設計書 DD-03 に準拠）
- **既存コントローラはすべて `@Controller` (MVC + Thymeleaf)**。クーポン API の `@RestController` は新しいパターンだが、既存で唯一の例が `InternalBatchController`（`/internal/batch`）
  - 参考: `FEAT-001-api-design.md` 2.4 に「現行実装では `@RestController` は使われず」と記載あり
- `CartController` に追加するか、新規 `CouponController` を作成するかは実装時に判断

### 4.1 `POST /api/cart/coupon` (クーポン適用API)
- [ ] クーポン適用エンドポイントのハンドラを追加
  - [ ] リクエストバリデーション (`coupon_code` の必須チェック)
  - [ ] カート情報（Cookie）の取得
  - [ ] `CouponValidationService` による即時バリデーション
  - [ ] `CouponCalculationService` による割引計算
  - [ ] クーポンコードをカート状態へ保持（Cookie またはセッション）
  - [ ] 成功時: CartSummaryView に `coupon_discount_amount`, `applied_coupon_code`, `applied_coupon_name` を含む JSON 返却 (HTTP 200)
  - [ ] 失敗時: `error_code` に対応する JSON 返却 (HTTP 400)

### 4.2 `DELETE /api/cart/coupon` (クーポン解除API)
- [ ] クーポン解除エンドポイントのハンドラを追加
  - [ ] カート状態からクーポン情報をクリア
  - [ ] 成功時: `coupon_discount_amount=0`, `applied_coupon_code=null` の JSON 返却 (HTTP 200)

### 4.3 `GET /checkout/confirm` (既存MVC注文確認画面)
- [ ] 既存 `CartController` の注文確認GETにクーポン状態の再検証を接続
  - 成功時はThymeleafの注文確認画面へ割引・スナップショット候補を表示
  - 失敗時の画面遷移・解除・エラー表示はDD-02／DD-03に従う。JSON APIや409応答へ置き換えない

### 4.4 `POST /checkout/confirm` (既存MVC注文確定)
- [ ] 既存の注文確定POSTから `OrderService.placeOrder()` を呼び出す
  - 同一 `@Transactional` 内でクーポン再検証、注文保存、全体・会員別利用実績更新を行う
  - 再検証NG時の確認画面への戻りとエラー表示はDD-02／DD-03に従う。注文確認・確定のための追加REST APIは設けない
  - **決済エラー**: 既存システムはオフライン決済のため決済APIエラーは発生しない

### 4.5 エラーコード (`messages.properties`)
- [ ] DD-03 §2 の `error_code` 一覧を `src/main/resources/messages.properties` に追加
  - `error.coupon.not_found=クーポンが存在しないか、無効です。`
  - `error.coupon.already_applied=既に別のクーポンが適用されています。解除してからお試しください。`
  - `error.coupon.expired=クーポンの有効期限外です。`
  - `error.coupon.user_condition=このクーポンは対象外の会員です。`
  - `error.coupon.target_error=このクーポンは対象外の商品です。`
  - `error.coupon.min_amount=最低購入金額（〇〇円）を満たしていません。`
  - `error.coupon.usage_limit=クーポンの利用上限に達しました。`

## 5. フロントエンド・UI層

### 実装方針
- テンプレートは Thymeleaf (`src/main/resources/templates/pages/`) + バニラJS (`src/main/resources/static/js/modules/`)
- **設計書で `Cart.vue` / `CheckoutConfirm.vue` / `OrderDetail.vue` と記載されているが、実際のファイルは `cart.html` / `checkout-confirm.html` / `mypage-order-detail.html`**
- クーポン適用状態の保存先はDD-01 §6.2の人間判断事項。決定前にCookieまたはセッションのいずれかを実装前提として固定しない。フロントエンドは選択された保持方式を通じて、API／MVCで `applied_coupon_code` / `applied_coupon_name` / `coupon_discount_amount` を表示する

### 5.1 カート画面 (`cart.html`)
- [ ] クーポン入力フォームと適用・解除ボタンの実装
  - [ ] 適用中は入力フォームを非活性化（DD-02 UI-Flow 1.1）
  - [ ] `POST /api/cart/coupon` / `DELETE /api/cart/coupon` を fetch 呼び出し
  - [ ] `CartSummaryView` に `coupon_discount_amount`, `applied_coupon_code`, `applied_coupon_name` フィールドを追加
  - [ ] APIレスポンスの `error_code` に応じた赤字エラーメッセージ表示
  - [ ] 金額明細にクーポン割引額（`- ¥XXX`）とクーポン名を表示
  - [ ] 既に別クーポンが適用中の場合: 「既に別のクーポンが適用されています。解除してからお試しください。」と表示

### 5.2 注文確認画面 (`checkout-confirm.html`)
- [ ] 既存MVCの `GET /checkout/confirm` で再検証結果を表示
  - [ ] 再検証NG時: DD-02／DD-03に定めるカート画面への遷移・エラー表示
  - [ ] 金額明細でのクーポン割引表示
  - [ ] `checkout_data` に `subtotal`, `shipping_fee`, `coupon_discount_amount`, `total`, `applied_coupon_code`, `applied_coupon_name` を表示

### 5.3 注文確定処理 (`checkout-confirm.html`)
- [ ] 既存MVCの `POST /checkout/confirm` 呼び出し側を改修
  - [ ] 再検証NG時に確認画面へ戻り、DD-02／DD-03に定めるエラーを表示
  - [ ] `OrderCompleteView` に `applied_coupon_name`, `coupon_discount_amount` を追加
  - [ ] 成功時: 注文完了画面へ遷移

### 5.4 会員マイページ 注文履歴詳細 (`mypage-order-detail.html`)
- [ ] 過去の注文スナップショット(`applied_coupon_name`, `coupon_discount_amount`)をもとに割引情報を表示
  - `MemberOrderDetailView` / `MemberOrderDetailMapperRow` に `applied_coupon_code`, `applied_coupon_name`, `coupon_discount_amount` を追加
  - 注文詳細明細行に「利用クーポン: `[applied_coupon_name] を利用`」と「クーポン割引額: `- ¥XXX`」を表示

## 6. 単体テスト・結合テスト

### 実装方針
- JUnit 5 + Mockito + AssertJ。`@Nested` + `@DisplayName` でグループ化、メソッド名は snake_case
- テストIDは `ORD-001` に類する `CPN-001` のように付番。
- Mockito による手動 DI（`@SpringBootTest` は使用しない）
- トランザクション境界は `TransactionSynchronizationManager.initSynchronization()` / `clearSynchronization()` でシミュレート
- ファイル: `src/test/java/jp/co/skig/officeorder/service/coupon/CouponValidationServiceTest.java` 等

### 6.1 ロジック単体テスト (`CouponValidationServiceTest`, `CouponCalculationServiceTest`)
- [ ] **クーポンコード重複入力** — 既に別クーポンが適用中の場合 `COUPON_ALREADY_APPLIED`
- [ ] **有効期限境界値** — 有効開始日時ちょうど / 有効終了日時ちょうど / 開始前 / 終了後 → `COUPON_EXPIRED`
- [ ] **最低購入金額の境界値** — 対象商品小計が最小購入金額と同額（OK） / 1円少ない（NG） → `COUPON_MIN_AMOUNT_ERROR`
- [ ] **対象商品/カテゴリOR条件** — カテゴリ一致 OR 商品ID一致のいずれかに合致すればOK
- [ ] **対象外商品のみの場合** → `COUPON_TARGET_ERROR`
- [ ] **0円対象商品** — 対象商品が存在すれば対象条件は成立し、`min_purchase_amount` は別判定する
- [ ] **定率割引のFloor** — 小数点以下切り捨て確認（`RoundingMode.DOWN`）
- [ ] **割引上限** — 定率割引に対する `max_discount_amount` ちょうど／超過時の上限適用。定額割引には適用しない
- [ ] **対象商品の小計を基準にした最低購入金額判定** — 対象外商品の金額で水増しせず、対象商品小計のみで判定
- [ ] **定額割引が対象小計を超える場合** — 割引額が小計と同額になる（マイナスにならない）
- [ ] **新規会員条件** — `target_user_type == 'new'` で `completed` ステータス注文0件の場合OK / 1件以上NG → `COUPON_USER_CONDITION_ERROR`
- [ ] **クーポン使用回数上限** — 全体上限到達 / 会員別上限到達 → `COUPON_USAGE_LIMIT_REACHED`
- [ ] **無効クーポン / 存在しないクーポン** → `COUPON_NOT_FOUND`

### 6.2 API結合テスト
- [ ] `POST /api/cart/coupon`: 正常適用、エラーコード返却の網羅
  - `COUPON_NOT_FOUND`, `COUPON_ALREADY_APPLIED`, `COUPON_EXPIRED`, `COUPON_USER_CONDITION_ERROR`, `COUPON_TARGET_ERROR`, `COUPON_MIN_AMOUNT_ERROR`, `COUPON_USAGE_LIMIT_REACHED`
- [ ] `DELETE /api/cart/coupon`: 正常解除
- [ ] `GET /checkout/confirm`: MVC再検証結果と画面遷移
- [ ] `POST /checkout/confirm`: MVC再検証結果、画面遷移、スナップショット保存
- [ ] クーポン適用・解除APIのCSRF検証と既存セッション／カート状態の維持

### 6.3 トランザクション・排他制御テスト
- [ ] **同時注文による使用回数上限超過防止** — 同一クーポンに対する複数会員からの同時注文（全体上限1枚の取り合い）
  - FOR UPDATE ロック + 再検証で上限超過を防ぐ
- [ ] **会員別使用回数の同時更新** — 同一会員からの同時注文（個人上限1枚の取り合い・行確保UPSERT競合テスト）
  - PostgreSQL `ON CONFLICT DO NOTHING` で行を確保し、FOR UPDATE後の上限確認・一度の加算を検証
- [ ] **注文確認時の再バリデーション失敗** — `GET /checkout/confirm` でクーポン期限切れ・上限到達
- [ ] **注文確定時の再バリデーション失敗** — `POST /checkout/confirm` でクーポン期限切れ・上限到達
- [ ] **決済失敗時のクーポン使用回数・注文データ整合性** — 既存システムはオフライン決済のため決済API失敗はないが、`placeOrder()` 内の例外によるロールバックでクーポン使用回数が不整合にならないことを確認
  - `OrderServiceTest` に「注文保存失敗時にクーポン使用回数がインクリメントされない」テストを追加

### 6.4 キャンセル・返品時の連動テスト
- [ ] **キャンセル時の使用回数復元（キャンセル処理）** — キャンセル処理後に `used_count_total` と `usage_count` が各-1されることを確認
- [ ] **返品時の使用回数復元（返品処理）** — 返品処理後に `used_count_total` と `usage_count` が各-1されることを確認（§3.5 の採択方式に応じて）
- [ ] **キャンセル処理の再実行による二重減算防止** — 既に `cancelled` ステータスの注文に対して再度キャンセル処理を実行しても二重減算されないことを確認
  - 実装方式: `UPDATE orders SET order_status = 'cancelled' WHERE order_id = ? AND order_status != 'cancelled'` で作用行数0を検出しデクリメントをスキップ
- [ ] **返品処理の再実行による二重減算防止** — 返品処理を再度実行しても二重減算されないことを確認（§3.5 の返品冪等化方式に応じて）
- [ ] キャンセルと返品の対象状態・相互排他に関するテストは、DD-01 §6.1の人間判断で対象状態が確定してから追加
- [ ] **デクリメントの異常検知** — `usage_count = 0` の状態でデクリメントを試みた場合、作用行数0が検知されログ警告（または例外）が出力され、値が0未満にならないことを確認（§3.6参照）
- [ ] **クーポン未適用注文のキャンセル・返品** — `applied_coupon_code` がNULLの注文をキャンセル・返品してもデクリメントが実行されないことを確認

---

## 7. トランザクション・決済設計（DD-04 との整合）

### 7.1 既存決済方式の調査結果
- **外部決済APIは存在しない**。決済手段は `bank_transfer` / `cash_on_delivery` / `convenience_store` の3種類（すべてオフライン）
- `convenience_store` は注文保存時に支払案内 JSON（支払番号・期限）を生成し、`payment_instruction` カラムに保存する
- `OrderService.placeOrder()` は1つの `@Transactional` で受注・明細・ステータス履歴を一括保存し、コミット後に完了メールを送信
- `order_status` の初期値は `'received'`（CHECK 制約: `received`, `awaiting_payment`, `processing`, `completed`, `cancelled`）

### 7.2 トランザクション境界の決定
- **方式**: 既存の同期1トランザクション方式（DD-04 §2.2 の「TX1+TX2二段階」は不要）
- **理由**: 外部決済APIがないため「決済成功・DB失敗不整合」「決済APIタイムアウト」リスクは不存在
- **実装**: `OrderService.placeOrder()` 内でクーポン再検証 → 使用回数インクリメント → 注文保存をすべて同じトランザクションで実行
  - いずれかで例外発生 → ロールバック → クーポン使用回数も自動復帰

### 7.3 クーポン使用回数の確保・確定タイミング
- **確保タイミング**: 注文確定時（`placeOrder()` 内）、`FOR UPDATE` ロック後に即座に確定
- **理由**: DD-01 §1 「注文確定時（決済直前・直後）」に準ずる。既存システムはオフライン決済のため「決済直前」=「注文保存直前」
- **注文確定・注文完了・`completed` の関係（§3.3参照）**: 注文確定（= `placeOrder()` 実行 = `received` で保存・コミット）と注文完了画面到達は同一イベント。`completed` はフルフィルメント全体の最終ステータスであり、**使用回数確定のトリガーにはしない**
- **仮確保不要**: カゴ落ち防止のため仮確保を検討したが、既存フロー（`received` ステータスで即保存）に合わせて即確定とする。キャンセル・返品時に返還する方式（DD-01 §1）と組み合わせる
- **会員別実績行確保**: `user_coupon_usages` の `ON CONFLICT DO NOTHING` 後に行をロックし、上限確認後に利用回数を一度だけ加算
- **キャンセルの冪等性**: `UPDATE orders SET order_status = 'cancelled' WHERE order_id = ? AND order_status != 'cancelled'` で作用行数0を検出し、デクリメントをスキップする（§3.4参照）
- **返品の冪等性**: §3.5 の採択方式（`returned` ステータス or 返品レコード一意制約）で既返品を検出し、デクリメントをスキップする（§3.5参照）

### 7.4 キャンセル・返品時の二重減算防止
- [ ] キャンセル処理の使用回数減算を**冪等化**（同一キャンセルの再実行で二重減算しない。§3.4参照。キャンセル処理は未実装）
- [ ] 返品処理の使用回数減算を**冪等化**（同一返品の再実行で二重減算しない。§3.5参照）
- [x] キャンセルと返品は**別処理として分離**する（既存DBに返品ステータスがないため、§3.5 でスコープを確定してから実装）
- [ ] キャンセル・返品の再実行時に二重減算されないことを確認（§6.4のテスト）
- [ ] **デクリメントの異常検知**: `GREATEST(0, usage_count - 1)` は使わず、`WHERE usage_count > 0` ガード + 作用行数0の検知（ログ警告/例外）で「異常な減算」を表面化する（§3.6参照）
- [ ] 二重減算防止・異常検知のテストを追加（§6.4）

---

## 8. 設計書との整合性確認チェックリスト

- [ ] 1注文につき適用可能なクーポンは1件のみ（DD-03 §2.1 / 要件 §3.1）
- [ ] コード入力時に即時バリデーション（DD-03 §2.1 / 要件 §3.1）
- [ ] 有効期間チェック（DD-05 §1.1 step 4）
- [ ] 最低購入金額チェック（DD-05 §1.1 step 7）
- [ ] 対象カテゴリ/商品チェック（DD-05 §1.1 step 6）
- [ ] 会員条件チェック（DD-05 §1.1 step 5）
- [ ] 使用回数上限チェック（DD-05 §1.1 step 8）
- [ ] 対象カテゴリ/商品条件はOR（DD-01 §2 / DD-05 §1.1 step 6）
- [ ] 最低購入金額・割引計算の対象はクーポン対象商品の小計（DD-01 §3）
- [ ] 固定額割引（要件 §3.2 / DD-05 §1.2）
- [ ] 割合割引（要件 §3.2 / DD-05 §1.2）
- [ ] 割合割引の端数処理はFLOOR（DD-01 §3 / DD-05 §1.2）
- [ ] `max_discount_amount` は定率割引のみ（要件 §3.2 / DD-01 §3）
- [ ] カートで適用・削除（DD-03 §2.1・§2.2）
- [ ] 注文確認時の再バリデーション（DD-03 §2.3 / DD-05 §3）
- [ ] 注文確定時の最終再バリデーション（DD-03 §2.4 / DD-05 §3）
- [ ] 条件不成立時はクーポンを解除して再計算（DD-02 UI-Flow 1.2, 1.3）
- [ ] 注文にはクーポンコード・クーポン名・割引額をスナップショット保存（DD-01 §1 / DD-03 API §4）
- [ ] 注文確定時（= 注文完了画面到達時）に使用回数を確定（DD-01 §1。`completed` ステータス到達時ではない。§3.3参照）
- [ ] キャンセル・返品時に使用回数を戻す（DD-01 §1。キャンセルと返品は別処理として分離。§3.4, §3.5参照）
- [ ] My Page/注文詳細で適用結果を確認できる（DD-02 §1.4）
- [ ] エラーコードは設計書と一致させる（DD-03 §2 / DD-05 §1.1）
- [ ] 全体・会員別利用上限について排他制御を実装する（DD-04 §4）
- [ ] 会員別利用実績を行確保UPSERT後にロックし、上限確認後に一度だけ加算する（DD-04 §4）
- [ ] 既存オフライン決済の1DBトランザクション内でクーポン処理を実行する（DD-04 §4）
- [ ] 新規会員判定は既存 `completed` 状態のみを対象とする（要件 §3.4 / DD-01 §1）
- [ ] `OrderCheckoutService` という存在しないクラス名を `OrderService` に修正（§3.3参照）
- [ ] キャンセルを `OrderCancelService`、返品を `OrderReturnService` に分離（§3.4, §3.5, §7.4参照）
- [ ] 使用回数デクリメントは `GREATEST(0, ...)` ではなくガード付きUPDATE + 作用行数0検知（§3.6参照）