# メッセージ一覧

本ドキュメントは、office-order システムで使用するすべてのメッセージを整理した設計資料です。

**ソースファイル:**

- `src/main/resources/messages.properties` — バリデーション・フラッシュ・業務・エラーページメッセージ
- `src/main/resources/mail/subjects.properties` — メール件名
- `src/main/resources/mail/templates/*.txt` — メール本文テンプレート

---

## 1. バリデーションメッセージ（`validation.*`）

Spring Validation の制約違反時にフォームエラーとして表示されるメッセージです。

### 1-1. 氏名・フリガナ

| メッセージキー                      | メッセージ内容                         | 対象フォームフィールド |
| ----------------------------------- | -------------------------------------- | ---------------------- |
| `validation.lastName.required`      | 姓を入力してください。                 | `lastName`             |
| `validation.lastName.max`           | 姓は50文字以内で入力してください。     | `lastName`             |
| `validation.firstName.required`     | 名を入力してください。                 | `firstName`            |
| `validation.firstName.max`          | 名は50文字以内で入力してください。     | `firstName`            |
| `validation.lastNameKana.required`  | セイを入力してください。               | `lastNameKana`         |
| `validation.lastNameKana.max`       | セイは50文字以内で入力してください。   | `lastNameKana`         |
| `validation.lastNameKana.pattern`   | セイは全角カタカナで入力してください。 | `lastNameKana`         |
| `validation.firstNameKana.required` | メイを入力してください。               | `firstNameKana`        |
| `validation.firstNameKana.max`      | メイは50文字以内で入力してください。   | `firstNameKana`        |
| `validation.firstNameKana.pattern`  | メイは全角カタカナで入力してください。 | `firstNameKana`        |

### 1-2. 会社・組織

| メッセージキー                             | メッセージ内容                          | 対象フォームフィールド |
| ------------------------------------------ | --------------------------------------- | ---------------------- |
| `validation.personalOrCorporate.required`  | 個人/法人を選択してください。           | `personalOrCorporate`  |
| `validation.personalOrCorporate.invalid`   | 個人/法人の値が不正です。               | `personalOrCorporate`  |
| `validation.companyName.max`               | 会社名は120文字以内で入力してください。 | `companyName`          |
| `validation.companyName.corporateRequired` | 法人の場合は会社名を入力してください。  | `companyName`          |
| `validation.departmentName.max`            | 部署名は120文字以内で入力してください。 | `departmentName`       |

### 1-3. メールアドレス・パスワード

| メッセージキー                        | メッセージ内容                                          | 対象フォームフィールド   |
| ------------------------------------- | ------------------------------------------------------- | ------------------------ |
| `validation.email.required`           | メールアドレスを入力してください。                      | `email`                  |
| `validation.email.format`             | メールアドレスの形式で入力してください。                | `email`                  |
| `validation.email.max`                | メールアドレスは254文字以内で入力してください。         | `email`                  |
| `validation.email.duplicate`          | 既に登録済みです。                                      | `email`                  |
| `validation.password.required`        | パスワードを入力してください。                          | `password`               |
| `validation.password.register.format` | パスワードは半角英数字記号8〜64文字で入力してください。 | `password`（登録時）     |
| `validation.password.login.max`       | パスワードは64文字以内で入力してください。              | `password`（ログイン時） |

### 1-4. 個人属性

| メッセージキー                        | メッセージ内容                               | 対象フォームフィールド |
| ------------------------------------- | -------------------------------------------- | ---------------------- |
| `validation.gender.required`          | 性別を選択してください。                     | `gender`               |
| `validation.gender.invalid`           | 性別の値が不正です。                         | `gender`               |
| `validation.anniversaryDate.required` | 生年月日/記念日を入力してください。          | `anniversaryDate`      |
| `validation.newsletterOptIn.required` | メールマガジンの送付希望を選択してください。 | `newsletterOptIn`      |

### 1-5. 住所・配送先

| メッセージキー                        | メッセージ内容                                 | 対象フォームフィールド |
| ------------------------------------- | ---------------------------------------------- | ---------------------- |
| `validation.postalCodePart1.required` | 郵便番号（前3桁）を入力してください。          | `postalCodePart1`      |
| `validation.postalCodePart1.pattern`  | 郵便番号（前3桁）は数字3桁で入力してください。 | `postalCodePart1`      |
| `validation.postalCodePart2.required` | 郵便番号（後4桁）を入力してください。          | `postalCodePart2`      |
| `validation.postalCodePart2.pattern`  | 郵便番号（後4桁）は数字4桁で入力してください。 | `postalCodePart2`      |
| `validation.prefecture.required`      | 都道府県を入力してください。                   | `prefecture`           |
| `validation.prefecture.max`           | 都道府県は20文字以内で入力してください。       | `prefecture`           |
| `validation.city.required`            | 市区町村名を入力してください。                 | `city`                 |
| `validation.city.max`                 | 市区町村名は120文字以内で入力してください。    | `city`                 |
| `validation.addressLine.required`     | 番地・ビル名を入力してください。               | `addressLine`          |
| `validation.addressLine.max`          | 番地・ビル名は255文字以内で入力してください。  | `addressLine`          |
| `validation.deliveryFloor.required`   | お届け先階数を入力してください。               | `deliveryFloor`        |
| `validation.deliveryFloor.max`        | お届け先階数は20文字以内で入力してください。   | `deliveryFloor`        |
| `validation.deliveryFloor.pattern`    | お届け先階数は数字のみで入力してください。     | `deliveryFloor`        |
| `validation.hasElevator.required`     | エレベーターの有無を選択してください。         | `hasElevator`          |
| `validation.daytimePhone.required`    | 日中連絡可能な電話番号を入力してください。     | `daytimePhone`         |
| `validation.daytimePhone.pattern`     | 電話番号は数字のみ10〜12桁で入力してください。 | `daytimePhone`         |
| `validation.fax.pattern`              | FAXは数字のみ10〜12桁で入力してください。      | `fax`                  |

### 1-6. 決済・注文

| メッセージキー                      | メッセージ内容               | 対象フォームフィールド |
| ----------------------------------- | ---------------------------- | ---------------------- |
| `validation.paymentMethod.required` | 決済手段を選択してください。 | `paymentMethod`        |
| `validation.paymentMethod.invalid`  | 決済手段の値が不正です。     | `paymentMethod`        |

### 1-7. お問い合わせ

| メッセージキー                       | メッセージ内容                                      | 対象フォームフィールド |
| ------------------------------------ | --------------------------------------------------- | ---------------------- |
| `validation.inquiryType.required`    | お問い合わせの種類を選択してください。              | `inquiryType`          |
| `validation.inquiryType.invalid`     | お問い合わせの種類の値が不正です。                  | `inquiryType`          |
| `validation.orderPhase.required`     | 注文状況を選択してください。                        | `orderPhase`           |
| `validation.orderPhase.invalid`      | 注文状況の値が不正です。                            | `orderPhase`           |
| `validation.productName.max`         | お問い合わせ商品名は255文字以内で入力してください。 | `productName`          |
| `validation.productCode.max`         | 商品コードは32文字以内で入力してください。          | `productCode`          |
| `validation.contactMessage.required` | お問い合わせ内容を入力してください。                | `contactMessage`       |
| `validation.contactMessage.max`      | お問い合わせ内容は1000文字以内で入力してください。  | `contactMessage`       |

---

## 2. 型変換エラーメッセージ（`typeMismatch.*`）

フォームバインディング時に型変換が失敗した場合に表示されるメッセージです。Spring MVC の `typeMismatch` 規約に従います。

| メッセージキー                                          | メッセージ内容                                  | 発生ケース                                                                  |
| ------------------------------------------------------- | ----------------------------------------------- | --------------------------------------------------------------------------- |
| `typeMismatch.registerForm.anniversaryDate`             | 生年月日/記念日は正しい日付を入力してください。 | 会員登録フォームで `anniversaryDate` が `LocalDate` に変換失敗              |
| `typeMismatch.profileForm.anniversaryDate`              | 生年月日/記念日は正しい日付を入力してください。 | 会員情報変更フォームで `anniversaryDate` が `LocalDate` に変換失敗          |
| `typeMismatch.registerForm.newsletterOptIn`             | メールマガジンの送付希望を選択してください。    | 会員登録フォームで `newsletterOptIn` が `Boolean` に変換失敗                |
| `typeMismatch.profileForm.newsletterOptIn`              | メールマガジンの送付希望を選択してください。    | 会員情報変更フォームで `newsletterOptIn` が `Boolean` に変換失敗            |
| `typeMismatch.registerForm.hasElevator`                 | エレベーターの有無を選択してください。          | 会員登録フォームで `hasElevator` が `Boolean` に変換失敗                    |
| `typeMismatch.profileForm.hasElevator`                  | エレベーターの有無を選択してください。          | 会員情報変更フォームで `hasElevator` が `Boolean` に変換失敗                |
| `typeMismatch.addressForm.hasElevator`                  | エレベーターの有無を選択してください。          | お届け先フォームで `hasElevator` が `Boolean` に変換失敗                    |
| `typeMismatch.checkoutForm.hasElevator`                 | エレベーターの有無を選択してください。          | チェックアウトフォームで `hasElevator` が `Boolean` に変換失敗              |
| `typeMismatch.checkoutForm.selectedAdditionalAddressId` | 登録済みお届け先の選択値が不正です。            | チェックアウトフォームで `selectedAdditionalAddressId` が `Long` に変換失敗 |
| `typeMismatch.selectedAdditionalAddressId`              | 登録済みお届け先の選択値が不正です。            | フォーム名省略形（フォールバック）                                          |
| `typeMismatch.anniversaryDate`                          | 生年月日/記念日は正しい日付を入力してください。 | フィールド名省略形（フォールバック）                                        |
| `typeMismatch.java.time.LocalDate`                      | 日付を正しく入力してください。                  | `LocalDate` 型全般のフォールバック                                          |
| `typeMismatch.java.lang.Boolean`                        | 選択値が不正です。                              | `Boolean` 型全般のフォールバック                                            |
| `typeMismatch.java.lang.Long`                           | 数値の形式が不正です。                          | `Long` 型全般のフォールバック                                               |

---

## 3. フラッシュメッセージ（`flash.*`）

操作完了後にリダイレクト先画面で一度だけ表示されるフラッシュスコープのメッセージです。

| メッセージキー                        | メッセージ内容                                             | 発生タイミング                             | 表示画面                |
| ------------------------------------- | ---------------------------------------------------------- | ------------------------------------------ | ----------------------- |
| `flash.cart.added`                    | カートに追加しました。                                     | 商品詳細からカート追加成功後               | カート画面              |
| `flash.checkout.inputRetry`           | 入力内容を再確認してください。                             | チェックアウト入力で業務エラー検出後       | チェックアウト入力画面  |
| `flash.checkout.tokenMismatch`        | 注文の再送信を検出しました。入力内容を再確認してください。 | PRG トークン不一致（二重送信検出）後       | チェックアウト入力画面  |
| `flash.contact.accepted`              | お問い合わせを受け付けました。                             | お問い合わせ送信成功後                     | お問い合わせ画面        |
| `flash.mypage.reorder.none`           | 再購入可能な商品がありませんでした。                       | 再購入時に全商品が対象外だった場合         | マイページ注文詳細画面  |
| `flash.mypage.reorder.success`        | 再購入商品をカートに追加しました。                         | 再購入で全商品をカート追加成功後           | カート画面              |
| `flash.mypage.reorder.partialFailure` | 一部商品は追加できませんでした（商品コード: {0}）          | 再購入で一部商品のカート追加が失敗した場合 | カート画面              |
| `flash.mypage.profile.updated`        | 会員情報を更新しました。                                   | 会員情報変更成功後                         | マイページ会員情報画面  |
| `flash.batch.startAccepted`           | ジョブを受け付けました。                                   | バッチ起動リクエスト受付後                 | （内部 API レスポンス） |
| `flash.batch.stopAccepted`            | 停止要求を受け付けました。                                 | バッチ停止リクエスト受付後                 | （内部 API レスポンス） |

> `{0}` は実行時に置換されるプレースホルダーです（`MessageFormat` 形式）。

---

## 4. 業務エラーメッセージ（`business.*`）

業務ロジック内で発生したエラーを画面に表示するためのメッセージです。主に `IllegalArgumentException` としてスローされ、コントローラでキャッチされてフォームエラーまたはフラッシュメッセージとして表示されます。

### 4-1. 在庫・カート

| メッセージキー                 | メッセージ内容                           | 発生ケース                                             | 主なスロー箇所 |
| ------------------------------ | ---------------------------------------- | ------------------------------------------------------ | -------------- |
| `business.stockShortage`       | 在庫が不足しているため注文できません。   | 注文確定時に在庫が不足している場合                     | `OrderService` |
| `business.cart.invalidProduct` | カート追加対象の商品が不正です。         | 存在しない商品バリアントをカートに追加しようとした場合 | `CartService`  |
| `business.cart.quantityRange`  | 数量は1〜99の範囲で入力してください。    | カート追加数量が範囲外の場合                           | `CartService`  |
| `business.cart.limitExceeded`  | カートにはこれ以上商品を追加できません。 | カート内の商品種別数または合計数量が上限に達した場合   | `CartService`  |
| `business.cart.loadFailed`     | 商品情報の取得に失敗しました。           | カート表示時に商品バリアント情報の取得に失敗した場合   | `CartService`  |

### 4-2. 注文

| メッセージキー                         | メッセージ内容                         | 発生ケース                                             | 主なスロー箇所 |
| -------------------------------------- | -------------------------------------- | ------------------------------------------------------ | -------------- |
| `business.order.cartEmpty`             | カートに商品がありません。             | カートが空の状態でチェックアウトを進めようとした場合   | `OrderService` |
| `business.order.companyRequired`       | 法人の場合は会社名を入力してください。 | 注文確定時に法人選択なのに会社名が未入力の場合         | `OrderService` |
| `business.order.deliveryFloorRequired` | お届け先階数を入力してください。       | 注文確定時に建物がある場合にお届け先階数が未入力の場合 | `OrderService` |
| `business.order.deliveryFloorInteger`  | お届け先階数は整数で入力してください。 | 注文確定時にお届け先階数が整数に変換できない場合       | `OrderService` |

### 4-3. 会員

| メッセージキー                          | メッセージ内容                       | 発生ケース                                                   | 主なスロー箇所  |
| --------------------------------------- | ------------------------------------ | ------------------------------------------------------------ | --------------- |
| `business.member.duplicateEmail`        | メールアドレスが重複しています。     | 会員登録・情報変更時にメールアドレスが既存会員と重複する場合 | `MemberService` |
| `business.member.addressLimitExceeded`  | これ以上お届け先を追加できません。   | 追加お届け先が登録上限数に達している場合                     | `MemberService` |
| `business.member.favoriteLimitExceeded` | これ以上お気に入りに追加できません。 | お気に入りが登録上限数に達している場合                       | `MemberService` |

### 4-4. バッチ

| メッセージキー                       | メッセージ内容                                            | 発生ケース                                       | 主なスロー箇所 |
| ------------------------------------ | --------------------------------------------------------- | ------------------------------------------------ | -------------- |
| `business.batch.jobNotFound`         | 指定されたジョブは存在しません。jobName={0}               | 存在しないジョブ名で起動リクエストが行われた場合 | `BatchService` |
| `business.batch.alreadyRunning`      | 既に同じバッチが実行中です。jobName={0}                   | 同一ジョブが既に実行中の場合                     | `BatchService` |
| `business.batch.executionNotFound`   | 指定された実行IDは存在しません。executionId={0}           | 存在しない実行 ID で停止リクエストが行われた場合 | `BatchService` |
| `business.batch.executionNotRunning` | 実行中のジョブではないため停止できません。executionId={0} | 停止対象のジョブが既に終了している場合           | `BatchService` |
| `business.batch.startFailed`         | バッチ起動に失敗しました。                                | Spring Batch のジョブ起動で例外が発生した場合    | `BatchService` |
| `business.batch.stopFailed`          | バッチ停止要求に失敗しました。                            | Spring Batch のジョブ停止で例外が発生した場合    | `BatchService` |

> `{0}` は実行時に置換されるプレースホルダーです（`MessageFormat` 形式）。

---

## 5. エラーページメッセージ（`error.page.*`）

HTTP エラーレスポンス時のエラーページ（`templates/error/`）で使用されるタイトルと説明文です。

| メッセージキー                    | メッセージ内容                                               | 表示ページ         | HTTP ステータス           |
| --------------------------------- | ------------------------------------------------------------ | ------------------ | ------------------------- |
| `error.page.notFound.title`       | ページが見つかりません                                       | `error/error.html` | 404 Not Found             |
| `error.page.notFound.message`     | 対象のページは移動または削除された可能性があります。         | `error/error.html` | 404 Not Found             |
| `error.page.requestError.title`   | リクエストを処理できませんでした                             | `error/error.html` | 4xx 系                    |
| `error.page.requestError.message` | 時間をおいて再度お試しいただくか、操作内容をご確認ください。 | `error/error.html` | 4xx 系                    |
| `error.page.serverError.title`    | システムエラーが発生しました                                 | `error/500.html`   | 500 Internal Server Error |
| `error.page.serverError.message`  | 時間をおいて再度お試しください。                             | `error/500.html`   | 500 Internal Server Error |

---

## 6. メール件名（`mail/subjects.properties`）

送信メールの件名テンプレートです。`{変数名}` は送信時に実際の値で置換されます。

| メッセージキー                | 件名テンプレート                                                       | 送信タイミング |
| ----------------------------- | ---------------------------------------------------------------------- | -------------- |
| `member-registration.subject` | 【OFFICE ORDER】会員登録完了のお知らせ                                 | 会員登録完了後 |
| `order-complete.subject`      | 【OFFICE ORDER】ご注文ありがとうございます（注文番号: {order_number}） | 注文確定完了後 |

---

## 7. メール本文テンプレート（`mail/templates/*.txt`）

メール本文のテンプレートファイルです。`{変数名}` は `MailTemplateRenderer` によって送信時に置換されます。

### 7-1. 会員登録完了メール（`member-registration-body.txt`）

| プレースホルダー  | 内容                         |
| ----------------- | ---------------------------- |
| `{last_name}`     | 会員の姓                     |
| `{first_name}`    | 会員の名                     |
| `{member_id}`     | 会員 ID                      |
| `{site_url}`      | サイト URL                   |
| `{contact_email}` | お問い合わせ先メールアドレス |

**テンプレート本文:**

```
{last_name} {first_name} 様

OFFICE ORDERをご利用いただきありがとうございます。
会員登録が完了しました。

会員ID: {member_id}

今後ともOFFICE ORDERをよろしくお願いいたします。

ご登録ありがとうございます。

サイトURL:
{site_url}

お問い合わせ先:
{contact_email}
```

### 7-2. 注文完了メール（`order-complete-body.txt`）

| プレースホルダー          | 内容                                             |
| ------------------------- | ------------------------------------------------ |
| `{customer_last_name}`    | 注文者の姓                                       |
| `{customer_first_name}`   | 注文者の名                                       |
| `{order_number}`          | 注文番号                                         |
| `{order_datetime}`        | 注文日時                                         |
| `{shipping_name}`         | お届け先氏名                                     |
| `{shipping_postal_code}`  | お届け先郵便番号                                 |
| `{shipping_prefecture}`   | お届け先都道府県                                 |
| `{shipping_city}`         | お届け先市区町村                                 |
| `{shipping_address_line}` | お届け先番地・ビル名                             |
| `{shipping_floor}`        | お届け先階数                                     |
| `{shipping_has_elevator}` | エレベーターの有無                               |
| `{daytime_phone}`         | 日中連絡可能な電話番号                           |
| `{order_items_lines}`     | 注文明細行（商品名・数量・単価の複数行テキスト） |
| `{subtotal_amount}`       | 商品小計（税抜）                                 |
| `{assembly_fee_total}`    | 組立・設置費合計                                 |
| `{shipping_fee}`          | 送料                                             |
| `{tax_amount}`            | 消費税額                                         |
| `{total_amount}`          | 合計金額（税込）                                 |
| `{contact_email}`         | お問い合わせ先メールアドレス                     |

**テンプレート本文:**

```
{customer_last_name} {customer_first_name} 様

このたびはOFFICE ORDERをご利用いただきありがとうございます。
以下の内容でご注文を受け付けました。

注文番号: {order_number}
注文日時: {order_datetime}

■お届け情報
氏名: {shipping_name}
住所: 〒{shipping_postal_code} {shipping_prefecture}{shipping_city}{shipping_address_line}
階数: {shipping_floor}
エレベーター: {shipping_has_elevator}
電話番号: {daytime_phone}

■ご注文内容
{order_items_lines}

商品小計: {subtotal_amount}円
組立・設置費: {assembly_fee_total}円
送料: {shipping_fee}円
消費税: {tax_amount}円
合計: {total_amount}円（税込）

お問い合わせ先:
{contact_email}
```

---

## メッセージ種別サマリー

| 種別           | プレフィックス      |    件数    | 主な用途                            |
| -------------- | ------------------- | :--------: | ----------------------------------- |
| バリデーション | `validation.*`      |     43     | フォーム入力検証エラーの表示        |
| 型変換エラー   | `typeMismatch.*`    |     14     | バインディング型変換失敗時の表示    |
| フラッシュ     | `flash.*`           |     10     | 操作完了後の一時通知メッセージ      |
| 業務エラー     | `business.*`        |     15     | 業務ロジック違反時のエラー表示      |
| エラーページ   | `error.page.*`      |     6      | HTTP エラーページのタイトル・説明文 |
| メール件名     | subjects.properties |     2      | 送信メールの件名                    |
| メール本文     | templates/\*.txt    | 2 ファイル | 送信メールの本文テンプレート        |
