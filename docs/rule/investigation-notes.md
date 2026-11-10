# OFFICE ORDER調査メモ

## ①調査範囲と前提

本書は、READMEと既存の調査資料をソースコードと照合し、本リポジトリの実装範囲、別システムの責務、および未確認事項を区別して整理したものです。実装が見つからないことだけを理由に、個別機能を実装対象外とは判断しません。

確認した資料:

- [README.md](../../README.md)
- [docs/rule/business-rules.md](./business-rules.md)
- [docs/rule/business-flow.md](./business-flow.md)
- [docs/rule/state-transition.md](./state-transition.md)

READMEに明記された本リポジトリの対象は商品閲覧、会員登録、カート、注文、お問い合わせです。受注後の業務処理と管理画面は別システムの責務とされています。[README.md:3-6](../../README.md#L3-L6) 既存資料の記述を、注文確定後の処理が全て本リポジトリで完結するという意味には読み替えていません。

「確認できた実装内容」はソースコード上で確認した処理、「未確認事項」はコード・READMEから仕様や運用を確定できない事項です。「実装対象外」はREADMEに明記された範囲に限ります。READMEに個別記載がない機能については、コード上で見つからなくても実装対象外と断定しません。

## ②未確認事項

### ②-1 注文・決済関連

| 項目・分類 | 現在確認できている実装内容 | 確認できていない内容 | 判断の根拠 |
|---|---|---|---|
| 外部決済サービスとの連携・結果照合<br>**未確認事項** | 注文フォームは銀行振込・代引・コンビニ決済を受け付けます。コンビニ決済では支払番号と期限を含む案内情報を生成し、注文に保存・表示します。[CheckoutInputForm.java:90-92](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L90-L92) [OrderService.java:380-398](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L380-L398) [orders.sql:23-48](../../sql/schema/orders.sql#L23-L48) | 注文確定経路で外部決済の実行、決済結果の受信・照合は確認できません。別の環境・システムでの決済連携や、その連携方式は未確認です。 | 注文フロー調査でも外部決済実行と結果照合は注文確定経路で確認できないと整理されています。[business-flow.md:196-200](./business-flow.md#L196-L200) |
| 注文確定後の在庫減算<br>**未確認事項** | 商品バリアントに非負の在庫数があります。注文直前には在庫0または注文数量超過を検査します。[products.sql:47-67](../../sql/schema/products.sql#L47-L67) [OrderService.java:412-425](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L412-L425) | 注文確定に伴う在庫減算SQL・Service処理は確認できません。別システム等での減算、実行タイミング、失敗時の扱いは未確認です。 | 注文フローでは注文時に確認できるのは在庫再検査までと記載されています。[business-flow.md:196-200](./business-flow.md#L196-L200) |
| 注文状態の更新・キャンセル<br>**未確認事項** | DDLは`received`、`awaiting_payment`、`processing`、`completed`、`cancelled`を定義します。注文作成では`received`を初期値として保存し、同じ状態の履歴を追加します。[orders.sql:1-6](../../sql/schema/orders.sql#L1-L6) [OrderService.java:236-240](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L236-L240) | 注文作成後の状態更新やキャンセル処理はアプリケーションコードで確認できません。実行中DBに別途設定されたDBオブジェクトや、外部システムでの更新は未確認です。 | 状態遷移調査では後続状態へのアプリ内遷移を確認できず、`received`は初期値として区別されています。[state-transition.md:39-58](./state-transition.md#L39-L58) 注文状態の定義だけから更新処理の存在は判断していません。 |
| クーポン情報と注文金額の記録<br>**要確認事項** | カートではクーポンを検証し割引後合計を計算します。OrderServiceはカートの`totalAmount`を注文パラメータに設定し、Mapperは注文合計に渡します。[CartService.java:150-195](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L150-L195) [OrderService.java:175-202](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L175-L202) [OrderMapper.xml:124-200](../../src/main/resources/mappers/OrderMapper.xml#L124-L200) | Serviceはクーポンコードと割引額のパラメータを設定していません。SQLには対応する列・Mapキー参照がありますが、実DBに格納される値は注文経路のDBテストで未確認です。金額内訳の妥当性も本調査では断定しません。 | 詳細は既存の金額調査に記載されています。[business-flow.md:264-272](./business-flow.md#L264-L272) |
| 完了画面の注文情報アクセス<br>**未確認事項** | 完了画面の注文取得SQLは注文番号で検索し、Controllerは注文番号を使って表示情報を取得します。[OrderMapper.xml:277-297](../../src/main/resources/mappers/OrderMapper.xml#L277-L297) | 注文者本人であることを照合する処理や、第三者が実際に有効な注文番号を取得できるかは未確認です。 | 既存調査は、注文番号を知るアクセス者に情報を表示する実装事実と、番号取得可能性が未確認であることを分けています。[business-flow.md:255-262](./business-flow.md#L255-L262) |

### ②-2 会員・認証関連

| 項目・分類 | 現在確認できている実装内容 | 確認できていない内容 | 判断の根拠 |
|---|---|---|---|
| 会員状態・退会<br>**要確認事項** | DBで確認できる会員状態は`active`と`withdrawn`です。退会処理は`active`会員のみを更新し、認証情報の検索対象も`active`です。[members.sql:23-28](../../sql/schema/members.sql#L23-L28) [MemberMapper.xml:107-127](../../src/main/resources/mappers/MemberMapper.xml#L107-L127) [MemberMapper.xml:441-449](../../src/main/resources/mappers/MemberMapper.xml#L441-L449) | 退会UPDATE件数が0件の場合の画面上の成功・失敗表示、別の既存セッションがいつ無効化されるかは未確認です。通常の再退会POSTはInterceptorが退会済み会員のセッションを消去してログインへ誘導します。[MemberActiveValidationInterceptor.java:50-71](../../src/main/java/jp/co/skig/officeorder/web/auth/MemberActiveValidationInterceptor.java#L50-L71) | 状態遷移調査でUPDATE件数0件とInterceptorによる通常の再POST拒否を区別しています。[state-transition.md:11-36](./state-transition.md#L11-L36) |
| その他の会員状態<br>**未確認事項** | DDL上の状態値は`active`／`withdrawn`です。[members.sql:23](../../sql/schema/members.sql#L23) | 利用停止など別状態の運用や、別システムによる状態更新は未確認です。 | [state-transition.md:222-230](./state-transition.md#L222-L230) |

### ②-3 お問い合わせ関連

| 項目・分類 | 現在確認できている実装内容 | 確認できていない内容 | 判断の根拠 |
|---|---|---|---|
| 受付後の対応状況管理<br>**未確認事項** | 問い合わせフォームからの受付と`inquiries`へのINSERTが実装されています。DB上の`inquiry_type`および`order_phase`は問い合わせ分類で、問い合わせ状態ではありません。[ContactController.java:81-112](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L81-L112) [ContactService.java:74-81](../../src/main/java/jp/co/skig/officeorder/service/contact/ContactService.java#L74-L81) [content.sql:18-40](../../sql/schema/content.sql#L18-L40) | 受付後の担当者割当、返信、対応中・完了などの管理と、それらを行うシステムは未確認です。 | 状態遷移調査では、問い合わせのライフサイクル状態カラムを確認できず、INSERT後の状態変更も確認できないとしています。[state-transition.md:87-100](./state-transition.md#L87-L100) |

### ②-4 その他

| 項目・分類 | 現在確認できている実装内容 | 確認できていない内容 | 判断の根拠 |
|---|---|---|---|
| 注文完了メールが最終失敗した場合の運用<br>**未確認事項** | メール送信失敗時、10秒・1分・5分の遅延再試行を行い、初回を含む最大4回の送信を試みます。上限到達時は最終失敗ログを記録し、メールサービスから送信失敗をControllerへ返しません。[NotificationMailService.java:44-48](../../src/main/java/jp/co/skig/officeorder/service/mail/NotificationMailService.java#L44-L48) [NotificationMailService.java:142-184](../../src/main/java/jp/co/skig/officeorder/service/mail/NotificationMailService.java#L142-L184) | 最終失敗の検知後に担当者へ通知・再送する運用、失敗の永続記録、再起動後の再送回復は未確認です。完了画面は送信状況によらず「注文完了メールを送信しました」と表示するテンプレートです。[checkout-complete.html:20-27](../../src/main/resources/templates/pages/checkout-complete.html#L20-L27) | 再送は`TaskScheduler`へ予約されています。失敗時の再送予約を確認するテストはありますが、上限到達後の運用を検証するテストは確認できません。[MailRetryConfig.java:13-27](../../src/main/java/jp/co/skig/officeorder/config/MailRetryConfig.java#L13-L27) [NotificationMailServiceTest.java:98-128](../../src/test/java/jp/co/skig/officeorder/service/mail/NotificationMailServiceTest.java#L98-L128) [business-flow.md:274-281](./business-flow.md#L274-L281) |
| クーポン利用実績の記録<br>**未確認事項** | クーポン利用実績の記録メソッドはあります。[CouponServiceImpl.java:66-73](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L66-L73) | 注文確定経路からこのメソッドを呼び出す箇所や、利用実績の実運用上の更新条件は確認できません。 | [business-rules.md:70](./business-rules.md#L70) [state-transition.md:119-132](./state-transition.md#L119-L132) |
| Spring Batch停止後の状態<br>**未確認事項** | アプリはSpring Batchへ停止要求を出し、受理した場合にHTTP 202を返します。[BatchExecutionService.java:140-165](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchExecutionService.java#L140-L165) [InternalBatchController.java:132-159](../../src/main/java/jp/co/skig/officeorder/web/InternalBatchController.java#L132-L159) | 停止要求後に実際に停止したかを再確認する処理や、その保証は未確認です。 | [state-transition.md:179-194](./state-transition.md#L179-L194) |

## ③実装対象外の機能

### ③-1 受注後の業務整理

| 項目・分類 | READMEで確認できる責務 | 本リポジトリの実装として確認できる範囲 | 確認できていない内容 |
|---|---|---|---|
| 受注後の業務処理<br>**実装対象外（READMEで明記）** | READMEは受注後の業務処理を別システムの責務としています。[README.md:3-6](../../README.md#L3-L6) | 本リポジトリには注文受付、注文・明細の保存、`received`初期状態・履歴の保存、完了画面表示があります。[OrderService.java:148-240](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L240) [OrderMapper.xml:124-255](../../src/main/resources/mappers/OrderMapper.xml#L124-L255) | 別システムが行う具体的な受注後作業、処理手順、データ受渡し・連携方法は未確認です。後続状態やキャンセルが別システムで実装済みとは断定しません。 |

### ③-2 管理画面

| 項目・分類 | READMEで確認できる責務 | 本リポジトリの実装として確認できる範囲 | 確認できていない内容 |
|---|---|---|---|
| 管理画面<br>**実装対象外（READMEで明記）** | READMEは管理画面を別システムの責務としています。[README.md:3-6](../../README.md#L3-L6) | READMEの責務分担として、本リポジトリの対象機能には含まれていません。 | 別システムの画面・機能・利用者権限・連携方式は未確認です。本リポジトリで個々の管理ユースケースを実装していないことだけから、別システム側の仕様を推測しません。 |

READMEはこれら以外の個別機能を別システムの責務として明記していません。そのため、決済結果照合、在庫減算、問い合わせ対応などをそれぞれ独立に「実装対象外」とは分類せず、該当する未確認事項として記録しています。

## ④実装状況や仕様の確認が必要な事項

| 項目 | 現在確認できている実装内容 | 確認が必要な点 | 根拠 |
|---|---|---|---|
| 注文の金額・クーポン記録 | カートの割引後`totalAmount`は注文INSERTの合計額に渡されます。Serviceがクーポンコード・割引額のキーを設定する処理は確認できません。[OrderService.java:175-202](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L175-L202) [OrderMapper.xml:124-200](../../src/main/resources/mappers/OrderMapper.xml#L124-L200) | 注文DBに保存される実際の金額内訳、クーポン列の値、注文時の利用実績をDBテスト等で確認する必要があります。現時点で金額内訳の誤りとは断定しません。 | [business-flow.md:264-272](./business-flow.md#L264-L272) |
| 注文完了画面のアクセス制御 | 注文番号のみで注文を検索し、完了情報を表示します。[OrderMapper.xml:277-297](../../src/main/resources/mappers/OrderMapper.xml#L277-L297) | 有効な注文番号の第三者取得可能性と、本人以外への情報表示に関する運用上の扱いは未確認です。実装事実と潜在的リスクは区別します。 | [business-flow.md:255-262](./business-flow.md#L255-L262) |
| メール送信 | 失敗時の遅延再試行、再送上限到達時のエラーログがあります。[NotificationMailService.java:142-184](../../src/main/java/jp/co/skig/officeorder/service/mail/NotificationMailService.java#L142-L184) | 最終失敗時の運用、再送予約の永続性・回復、完了画面文言との整合を確認する必要があります。 | [business-flow.md:274-281](./business-flow.md#L274-L281) |
| 問い合わせ状態 | 問い合わせの登録処理がありますが、DDLにライフサイクル状態欄は確認できません。[content.sql:18-40](../../sql/schema/content.sql#L18-L40) [ContactMapper.xml:31-65](../../src/main/resources/mappers/ContactMapper.xml#L31-L65) | 受付後の業務・管理先は未確認です。READMEで別システム責務と明記されている機能とは区別します。 | [state-transition.md:87-100](./state-transition.md#L87-L100) |
| DB外からの状態更新 | リポジトリの注文SQL・アプリ処理では、注文作成時以外の状態更新を確認できません。[state-transition.md:39-58](./state-transition.md#L39-L58) | 実行中DBに追加されたトリガー・関数や、外部システムからの更新は未確認です。 | [state-transition.md:222-230](./state-transition.md#L222-L230) |

## ⑤別システムとの責務分担

| 処理・機能 | 本リポジトリで確認できる内容 | README上の別システム責務／未確認事項 |
|---|---|---|
| 商品閲覧・会員登録・カート・注文・問い合わせ | READMEが本リポジトリの対象として列挙しています。実装詳細は業務ルール調査を参照してください。[business-rules.md:1-11](./business-rules.md#L1-L11) | 各対象機能の外部接続・別システム処理がある場合、その契約・連携方法は個別の実装で確認できない限り未確認です。 |
| 注文受付から注文完了画面まで | 注文データの保存、初期状態設定、完了画面表示を確認できます。[business-flow.md:11-28](./business-flow.md#L11-L28) | README上、受注後の業務処理は別システムの責務です。どの時点で受付処理が引き継がれるか、連携先や連携方式は未確認です。[README.md:3-6](../../README.md#L3-L6) |
| 管理画面 | 本リポジトリの責務としてはREADMEに記載されていません。 | READMEは管理画面を別システムの責務と明記しています。別システム側の画面・機能・接続方法は未確認です。[README.md:3-6](../../README.md#L3-L6) |
| 決済・在庫・問い合わせ受付後の対応 | 注文画面の決済方法・案内生成、注文直前の在庫確認、問い合わせ受付・DB登録は実装されています。[OrderService.java:175-202](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L175-L202) [OrderService.java:412-425](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L412-L425) [ContactService.java:74-81](../../src/main/java/jp/co/skig/officeorder/service/contact/ContactService.java#L74-L81) | 外部決済実行・照合、在庫減算、問い合わせ対応の具体的な別システム実装・接続は確認できません。これら個別機能をREADMEの明記なしに実装対象外と断定しません。 |
| 売れ筋ランキング・おすすめ関連商品 | READMEは再計算バッチを起動時および毎時に実行すると記載し、アプリにもスケジューラと集計処理があります。[README.md:96-98](../../README.md#L96-L98) [BatchScheduler.java:27-37](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L27-L37) | これらは実装対象外とは扱いません。READMEの記載範囲を超える別システムとの連携は未確認です。 |

## ⑥今後の確認事項

この調査から確認が必要な事項を列挙します。仕様を補完するものではありません。

1. 外部決済の実行主体、決済結果の受信・照合方法、および注文状態への反映有無。
2. 注文確定後の在庫減算の実行主体、タイミング、失敗時の扱い。
3. 注文後の状態更新・キャンセルを行うシステムと、本リポジトリとのデータ連携方法。
4. 問い合わせ受付後の対応・担当者管理の所在と連絡方法。
5. 注文完了メールが再送上限に達した場合の監視・通知・再送運用、および再起動時の扱い。
6. 注文番号だけで注文情報を表示する完了画面について、アクセス制御上の要件と注文番号の取得可能性。
7. 注文でのクーポンコード・割引額・利用実績のDB実値、および割引後合計と内訳の検証結果。
8. 実行中DBにリポジトリ外から設定されたトリガー／関数や、外部システムによる更新の有無。

### 最終照合結果

- READMEとの整合性：対象機能と、受注後業務・管理画面の別システム責務を分けて記載しました。[README.md:3-6](../../README.md#L3-L6)
- 既存調査資料との整合性：注文フロー・業務ルール・状態遷移の調査結果を参照し、注文状態の初期値`received`と後続更新、クーポン金額内訳、メール再試行、お問い合わせ状態を混同していません。`business-rules.md:104`のランキング根拠リンクは、現行READMEのバッチ記述（96-98行）ではなくREADME 78-83行を指しており、また`BatchScheduler.java:32-43`は実ファイルの行数（39行）を超えています。ランキング再計算自体は実装で確認でき、正しい根拠はREADME 96-98行、BatchScheduler 27-37行です。既存ファイルは依頼どおり変更していません。[business-rules.md:104](./business-rules.md#L104) [README.md:96-98](../../README.md#L96-L98) [BatchScheduler.java:27-37](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L27-L37)
- 分類：未実装を根拠に対象外と断定せず、READMEが明示する受注後業務・管理画面だけを実装対象外としました。
- 根拠：ソースファイル名・行番号へのリンクを各項目に付記しています。リンク先と行範囲を確認してください。
