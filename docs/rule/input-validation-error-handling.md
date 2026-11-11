# OFFICE ORDER 入力チェック・エラー処理調査資料

## ① 調査範囲と前提

本資料は、商品閲覧、会員登録、会員情報・住所管理、ログイン、カート、注文、お問い合わせの入力チェックとエラー処理について、ソースコードで確認できた事実を整理したものです。画面入力がControllerからService、Repository／Mapper、SQLへ渡る範囲、および関連テストを確認対象としています。

READMEに記載された本リポジトリの対象範囲は、商品閲覧、会員登録、カート、注文、お問い合わせです。受注後の業務処理と管理画面は別システムの責務とされています。[README.md:3-6](../../README.md#L3-L6) 会員情報やログインなど、READMEの列挙にない機能については、ソースコード上に実装がある場合は実装事実として記載し、READMEだけを根拠に対象外とはしていません。

本書での分類:

- **実装確認済み**: 今回確認したソースコードで具体的なチェックやエラー処理を確認できたもの。
- **未確認**: 今回確認したコードや資料から仕様・運用・挙動を確定できないもの。コード中に見つからないことだけで対象外とは判断しません。
- **実装対象外**: READMEで別システムの責務と明記されている受注後の業務処理、管理画面に限ります。

既存の調査資料は調査の前提・責務範囲の照合に用いました。入力検証・エラー処理の事実は、可能な範囲で該当する実装コードを根拠として記載しています。[business-rules.md](./business-rules.md) [business-flow.md](./business-flow.md) [state-transition.md](./state-transition.md) [investigation-notes.md](./investigation-notes.md)

## ② 入力チェックの共通的な仕組み

| 層 | 確認できた仕組み | 根拠 |
|---|---|---|
| Form | 会員登録・会員情報・追加お届け先・注文・お問い合わせのFormに、`@NotBlank`、`@NotNull`、`@Size`、`@Email`、`@Pattern`等が設定されています。電話番号、郵便番号、カナ、会員種別、決済方法などのパターンは共通定数を参照します。 | [MemberRegisterForm.java:19-110](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L19-L110)、[MemberProfileEditForm.java:19-99](../../src/main/java/jp/co/skig/officeorder/model/member/MemberProfileEditForm.java#L19-L99)、[MemberAdditionalAddressForm.java:15-75](../../src/main/java/jp/co/skig/officeorder/model/member/MemberAdditionalAddressForm.java#L15-L75)、[CheckoutInputForm.java:17-91](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L17-L91)、[ContactForm.java:16-61](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L16-L61)、[ValidationPatterns.java:9-33](../../src/main/java/jp/co/skig/officeorder/common/validation/ValidationPatterns.java#L9-L33) |
| Controller | `@Valid`と`BindingResult`で検証結果を受け、エラーがある場合は入力画面を再表示する実装があります。Formを正規化してから条件付きチェックを追加するControllerもあります。 | [MemberRegistrationController.java:84-103](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L103)、[MyPageController.java:231-243](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L231-L243)、[CartController.java:303-323](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L303-L323)、[ContactController.java:82-93](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L82-L93) |
| Service | 重複メール、住所件数上限、カート数量・在庫、注文時のカート・在庫・配送階数等、Formの宣言だけでは完結しない条件を確認します。 | [MemberService.java:113-130](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L130)、[MemberService.java:188-205](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L188-L205)、[MemberService.java:243-261](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L243-L261)、[CartService.java:224-255](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L255)、[OrderService.java:148-153](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L153)、[OrderService.java:407-442](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L407-L442) |
| DBスキーマ | NOT NULL、CHECK、UNIQUE、外部キー制約があります。これらの制約違反がすべての画面で同じメッセージへ変換されることは確認できません。Repository／Mapperで行う検索・更新条件は、機能ごとの処理として記載しています。 | [members.sql:1-30](../../sql/schema/members.sql#L1-L30)、[orders.sql:1-45](../../sql/schema/orders.sql#L1-L45)、[orders.sql:62-89](../../sql/schema/orders.sql#L62-L89)、[products.sql:47-67](../../sql/schema/products.sql#L47-L67)、[content.sql:18-35](../../sql/schema/content.sql#L18-L35) |
| Spring Security | `/mypage/**`とレビューの一部を会員ロールに制限し、フォームログイン失敗・認証要求・無効セッションをリダイレクトで処理します。これはFormのBean Validationとは別の認証・アクセス制御です。 | [SecurityConfig.java:71-108](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L71-L108)、[SecurityConfig.java:142-191](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L142-L191) |

`ValidationPatterns`は、パターンの定義を集約しています。各Formがどの値に適用するかはForm側の注釈で確認できます。[ValidationPatterns.java:9-33](../../src/main/java/jp/co/skig/officeorder/common/validation/ValidationPatterns.java#L9-L33)

## ③ 機能別の入力チェック・エラー処理

### 1. 商品閲覧

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `CatalogController`、`ProductListSearchService`、`ProductFilterOptionService`、`ProductService`。商品検索条件はFormではなくリクエストパラメータから組み立てられます。[CatalogController.java:85-137](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L85-L137) |
| 入力項目・チェック | 一覧の`page`、`size`、`sort`、キーワード、在庫条件、価格帯・カラー・カテゴリ別条件を受け取ります。価格帯IDは既知のIDに限定し、nullや重複を除外します。カラーキーは現在の候補に含まれるものだけを残し、重複を除きます。ページは1未満を補正し、表示件数は許可済みの候補値以外を既定値へ正規化します。[ProductFilterOptionService.java:245-294](../../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java#L245-L294) [ProductService.java:258-318](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java#L258-L318) |
| エラー条件・画面 | 商品詳細や対象商品が存在しない場合、Controllerは`ResponseStatusException(NOT_FOUND)`を送出します。[CatalogController.java:291-291](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L291-L291) [CatalogController.java:335-335](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L335-L335) 404は共通エラー処理を経由します。検索パラメータが不正な場合、ここで確認した処理は一律エラー画面ではなく、既知の値への除外・補正です。 |
| HTTP／メッセージ | 404は404ステータスで共通エラー画面またはJSON応答になります。検索パラメータの正規化時に利用者へエラーメッセージを表示する処理は確認できません。[AppErrorController.java:47-89](../../src/main/java/jp/co/skig/officeorder/web/AppErrorController.java#L47-L89) |
| テスト | `ProductServiceTest`は不正なページ・件数・ソート条件の正規化を確認し、`ProductFilterOptionServiceTest`は価格帯ID・カラーキーの許可値絞り込み等を確認します。`ProductListSearchServiceTest`は条件生成とページ補正を確認します。[ProductServiceTest.java:61-80](../../src/test/java/jp/co/skig/officeorder/service/product/ProductServiceTest.java#L61-L80) [ProductFilterOptionServiceTest.java:30-45](../../src/test/java/jp/co/skig/officeorder/service/product/ProductFilterOptionServiceTest.java#L30-L45) [ProductListSearchServiceTest.java:32-59](../../src/test/java/jp/co/skig/officeorder/service/product/ProductListSearchServiceTest.java#L32-L59) |
| 未確認事項 | リクエストパラメータの型変換エラーなど、個々の異常値に対する最終HTTP応答・画面表示は、今回の確認範囲では個別のControllerテストで確認できません。商品一覧Controllerの統合テストも今回確認した範囲では確認できませんでした。 |

### 2. 会員登録

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `MemberRegisterForm`、`MemberRegistrationController`、`MemberService`、会員Repository／SQL。[MemberRegisterForm.java:19-110](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L19-L110) [MemberRegistrationController.java:84-155](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L155) |
| 入力項目・チェック | Formでは氏名、フリガナ、会員種別、性別、メール、パスワード、住所、郵便番号、電話・FAX等に必須・形式・最大長の注釈があります。パスワード形式は登録用パターンを適用します。Controllerは正規化後、法人の場合に会社名必須を追加検証し、登録確認時にメール重複を確認します。[MemberRegisterForm.java:19-110](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L19-L110) [MemberRegistrationController.java:84-103](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L103) [MemberRegistrationController.java:165-171](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L165-L171) |
| Service／DBチェック | 確認画面からの最終登録時にもServiceがメール重複を再確認します。登録中のデータ整合性違反について、該当メールが重複していると確認された場合は重複例外に変換する処理があります。DBにはメールの大文字小文字を無視した一意インデックスがあります。[MemberService.java:113-130](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L130) [members.sql:23-30](../../sql/schema/members.sql#L23-L30) |
| エラー条件・画面 | 確認画面POSTで入力エラーがあれば登録入力画面を再表示します。最終登録時に登録確認用セッションデータがなければ登録入力へリダイレクトし、重複メールならメール項目のエラーを付けて入力画面を表示します。完了画面に完了用Flash属性がない場合は404です。[MemberRegistrationController.java:84-103](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L103) [MemberRegistrationController.java:108-135](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L108-L135) [MemberRegistrationController.java:145-155](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L145-L155) |
| メッセージ・テスト | フィールドエラーは登録画面の各入力欄に表示します。[member-register.html:23-136](../../src/main/resources/templates/pages/member-register.html#L23-L136) `MemberServiceTest`は既存メール拒否と新規メール登録を確認します。[MemberServiceTest.java:51-78](../../src/test/java/jp/co/skig/officeorder/service/member/MemberServiceTest.java#L51-L78) Controllerの画面遷移・メッセージに関するテストは今回確認した範囲では確認できませんでした。 |
| 未確認事項 | メール送信失敗時の登録確定処理への影響など、入力検証とは別の通知運用は本資料の確認対象ではなく、ここでは未確認です。 |

### 3. 会員情報・住所・退会

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `MemberProfileEditForm`、`MemberAdditionalAddressForm`、`MyPageController`、`MemberService`、会員Repository／SQL。[MemberProfileEditForm.java:19-99](../../src/main/java/jp/co/skig/officeorder/model/member/MemberProfileEditForm.java#L19-L99) [MemberAdditionalAddressForm.java:15-75](../../src/main/java/jp/co/skig/officeorder/model/member/MemberAdditionalAddressForm.java#L15-L75) |
| 入力項目・チェック | プロフィール・住所Formには氏名、フリガナ、会社名、メール、住所、電話番号、FAX、階数等の必須・長さ・形式制約があります。プロフィール更新と住所登録・更新では、法人会員の会社名必須をControllerが追加検証します。メール重複はServiceで検査し、追加住所はServiceで登録上限を検査します。[MyPageController.java:231-252](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L231-L252) [MyPageController.java:331-352](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L331-L352) [MyPageController.java:445-460](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L445-L460) [MemberService.java:188-205](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L188-L205) [MemberService.java:243-261](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L243-L261) |
| 所有者・存在チェック | 追加住所の取得・更新・削除では会員IDと住所IDを組み合わせて処理します。対象住所が見つからない場合、編集・更新等のController経路では404を送出します。[MyPageController.java:318-325](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L318-L325) [MyPageController.java:363-393](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L363-L393) |
| エラー条件・画面 | 入力エラーでは該当入力画面を再表示し、住所上限超過は住所一覧へリダイレクトしてFlashエラーを設定します。メール重複はプロフィール入力画面にフィールドエラーを設定します。[MyPageController.java:231-252](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L231-L252) [MyPageController.java:331-360](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L331-L360) |
| 退会・認証 | 退会POSTはServiceの戻り値（更新件数0件等を示すboolean）を画面表示に使わず、セッションをクリアしてログイン画面へリダイレクトします。通常の退会済みユーザーによる再POSTは、会員領域へのアクセスとしてSecurity／セッション確認の対象です。[MyPageController.java:413-425](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L413-L425) 退会更新件数0件の扱いと、認証セッションが残った状態での再退会等の詳細は、既存調査資料の未確認事項を参照してください。[state-transition.md](./state-transition.md) |
| DB制約 | 会員テーブルには会員状態の値制約、法人会社名の条件、退会日時の条件、メール一意インデックスがあります。追加住所にも郵便番号・電話番号・階数等の制約があります。[members.sql:1-30](../../sql/schema/members.sql#L1-L30) [members.sql:34-53](../../sql/schema/members.sql#L34-L53) |
| メッセージ・テスト | 入力エラーはプロフィール・住所フォームの各項目付近に表示します。[mypage-profile-edit.html:42-150](../../src/main/resources/templates/pages/mypage-profile-edit.html#L42-L150) [mypage-address-form.html:41-111](../../src/main/resources/templates/pages/mypage-address-form.html#L41-L111) `MemberServiceTest`は重複メールと住所上限のService動作を確認します。[MemberServiceTest.java:78-86](../../src/test/java/jp/co/skig/officeorder/service/member/MemberServiceTest.java#L78-L86) [MemberServiceTest.java:112-120](../../src/test/java/jp/co/skig/officeorder/service/member/MemberServiceTest.java#L112-L120) 退会結果の画面表示を確認するControllerテストは今回確認した範囲では確認できませんでした。 |
| 未確認事項 | 住所削除時に更新件数0件をどう扱うか、退会更新件数0件を利用者に成功・失敗として区別して知らせるべきかは、現在の仕様を確認できません。実装上の戻り値の扱いだけで仕様不一致とは断定できません。 |

### 4. ログイン

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `LoginForm`、`SecurityConfig`、`MemberUserDetailsService`、会員認証検索処理。[LoginForm.java:1-21](../../src/main/java/jp/co/skig/officeorder/model/member/LoginForm.java#L1-L21) [SecurityConfig.java:91-108](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L91-L108) |
| 入力項目・検証 | `LoginForm`にはメール必須・形式・最大長とパスワード必須・最大長のBean Validation注釈があります。ただし`/login` POSTはSpring Securityの`loginProcessingUrl`で処理され、Controllerの`@Valid`／`BindingResult`を経由する構成は確認できません。従って、このFormの制約がログインPOSTで実行されるかは未確認です。[LoginForm.java:10-18](../../src/main/java/jp/co/skig/officeorder/model/member/LoginForm.java#L10-L18) [SecurityConfig.java:91-100](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L91-L100) |
| 認証条件 | 認証処理は`MemberUserDetailsService`から会員の認証情報を取得します。認証対象をactive会員に限定する検索が実装されています。[MemberUserDetailsService.java:39-41](../../src/main/java/jp/co/skig/officeorder/service/member/MemberUserDetailsService.java#L39-L41) [MemberService.java:73-77](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L73-L77) |
| エラー条件・遷移 | 認証失敗時は`/login?error=true`へリダイレクトし、失敗理由クラス名とマスク済みメールをログ出力します。会員領域に未認証でアクセスした場合はログイン画面へ誘導し、無効セッション時は会員領域なら`expired=true`付きでログイン画面へ、公開領域なら元のリクエストへリダイレクトします。[SecurityConfig.java:142-191](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L142-L191) |
| メッセージ・テスト | ログイン画面は`error`、`expired`等に応じた表示を行います。[login.html:1-50](../../src/main/resources/templates/pages/login.html#L1-L50) `MemberUserDetailsServiceTest`は認証情報取得成功と未登録時の`UsernameNotFoundException`を確認します。[MemberUserDetailsServiceTest.java:21-33](../../src/test/java/jp/co/skig/officeorder/service/member/MemberUserDetailsServiceTest.java#L21-L33) ログイン処理Controllerのテストは今回確認した範囲では確認できませんでした。 |
| 未確認事項 | `LoginForm`のBean ValidationがSecurityの認証処理へ実際に適用されるか、認証失敗時に画面上で表示されるメッセージの全条件は未確認です。明示的な`AccessDeniedHandler`は確認できず、認可拒否時の具体的な表示は未確認です。 |

### 5. カート

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `CartController`、`CartService`、`CartCookieStore`、商品バリアント取得処理。[CartController.java:122-233](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L122-L233) [CartService.java:224-300](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L300) |
| 入力項目・チェック | 商品バリアントID、数量、組立指定を受け取ります。追加・更新で数量は1〜99、商品バリアントIDは正数であること、対象商品の在庫が正であることをServiceが確認します。新規明細数には上限があり、追加時は既存明細との合算数量も上限内に補正されます。[CartService.java:224-255](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L255) [CartService.java:272-300](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L272-L300) |
| Cookie検証 | Cookie内のバリアントID・数量・組立指定を解析し、不正な形式、正でないID、数量1〜99の範囲外などを除外します。Cookie内の同一IDは重複排除します。Cookie自体を空にする処理もあります。[CartCookieStore.java:20-79](../../src/main/java/jp/co/skig/officeorder/repository/CartCookieStore.java#L20-L79) [CartCookieStore.java:101-149](../../src/main/java/jp/co/skig/officeorder/repository/CartCookieStore.java#L101-L149) |
| エラー条件・遷移 | 追加・更新でServiceが`IllegalArgumentException`を送出するとControllerが捕捉し、Flash属性`cartError`へメッセージを設定してカートへリダイレクトします。追加時には成功メッセージも設定します。[CartController.java:122-168](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L122-L168) カート画面は`cartError`を表示します。[cart.html:19-19](../../src/main/resources/templates/pages/cart.html#L19-L19) |
| テスト | `CartServiceTest`は数量正規化、明細のマージ・更新、カートサマリー、ログイン会員へのクーポン適用、リダイレクト先の安全化等を確認します。[CartServiceTest.java:58-146](../../src/test/java/jp/co/skig/officeorder/service/cart/CartServiceTest.java#L58-L146) |
| 未確認事項 | Cookieの署名・改ざん防止機構の有無を含むセキュリティ要件との適合性は、仕様が確認できないため判断していません。不正Cookieを受け取った場合の除外・正規化自体は実装で確認できます。 |

### 6. 注文

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `CheckoutInputForm`、`CartController`、`OrderService`、注文Repository／Mapper／SQL。[CheckoutInputForm.java:17-116](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L17-L116) [CartController.java:303-449](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L303-L449) |
| 入力項目・チェック | 注文者・配送先情報、メール、電話番号、郵便番号、階数、支払方法、配送関連項目等に必須・形式・長さ制約があります。法人では会社名必須をControllerが追加確認します。入力POST時に`@Valid`、`BindingResult`、正規化を適用します。[CheckoutInputForm.java:17-116](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L17-L116) [CartController.java:303-323](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L303-L323) [CartController.java:449-455](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L449-L455) |
| 確定時の再検査 | 確認画面の注文確定POSTでは、カートが空でないこと、セッションに入力Formがあること、確認トークンが一致することを検証します。`OrderService`も空カート、法人会社名、在庫数・注文数量、配送階数の変換を検査します。[CartController.java:361-410](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L361-L410) [OrderService.java:148-153](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L153) [OrderService.java:407-442](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L407-L442) |
| エラー条件・画面 | 入力エラー時は注文入力画面を再表示します。空カートはカートへ戻します。セッションForm欠落・トークン不一致・Serviceの`IllegalArgumentException`は注文入力へリダイレクトし、`checkoutError`を設定します。存在しない注文完了情報は404です。[CartController.java:303-323](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L303-L323) [CartController.java:361-410](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L361-L410) [CartController.java:438-442](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L438-L442) |
| メッセージ・テスト | 入力エラーは注文入力画面の各欄に表示し、確定時のFlashエラーは注文入力／確認画面に表示します。[checkout-input.html:20-164](../../src/main/resources/templates/pages/checkout-input.html#L20-L164) [checkout-confirm.html:20-20](../../src/main/resources/templates/pages/checkout-confirm.html#L20-L20) `OrderServiceTest`は空カート拒否、支払方法に応じた案内情報生成等を確認します。[OrderServiceTest.java:77-122](../../src/test/java/jp/co/skig/officeorder/service/order/OrderServiceTest.java#L77-L122) |
| 未確認事項 | 追加お届け先IDはフォームや画面で扱われていますが、選択IDに対応する住所をサーバー側で再取得し所有者確認する処理は今回の確認範囲では確認できません。期待仕様が未確認のため不具合とは断定しません。[CheckoutInputForm.java:17-116](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L17-L116) |
| 責務境界 | 決済サービスとの連携、注文後の在庫減算・注文状態更新・キャンセル等を入力チェック実装済みとは扱いません。注文確定後の業務処理はREADMEで別システムの責務とされています。[README.md:3-6](../../README.md#L3-L6) 未確認事項の詳細は[調査メモ](./investigation-notes.md)を参照してください。 |

### 7. お問い合わせ

| 項目 | 調査結果 |
|---|---|
| 対象クラス | `ContactForm`、`ContactController`、`ContactService`、お問い合わせRepository／SQL。[ContactForm.java:16-61](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L16-L61) [ContactController.java:72-112](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L72-L112) |
| 入力項目・チェック | 氏名、メール、問い合わせ種別、問い合わせ段階は必須です。メール形式、電話番号パターン、部署名・商品名・商品コード・本文の最大長、問い合わせ種別・注文段階の値パターンがFormに設定されています。[ContactForm.java:16-61](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L16-L61) |
| 処理・エラー | Controllerは`@Valid`と`BindingResult`を使用し、検証エラー時はお問い合わせ画面を再表示します。正常時はログイン会員ID（ゲストならnull）とFormをServiceに渡して保存し、受理メッセージと問い合わせ番号をFlash属性に設定して画面へ戻します。`IllegalStateException`は捕捉してエラーメッセージを画面へ返します。[ContactController.java:82-109](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L82-L109) |
| DB制約・テスト | DBには問い合わせ種別・注文段階のCHECK制約、電話番号制約、会員外部キーがあります。[content.sql:18-35](../../sql/schema/content.sql#L18-L35) `ContactServiceTest`は初期フォーム・会員情報のプレフィルと正規化後の保存を確認します。[ContactServiceTest.java:27-54](../../src/test/java/jp/co/skig/officeorder/service/contact/ContactServiceTest.java#L27-L54) |
| 未確認事項 | `IllegalStateException`以外のDB例外は共通例外処理へ進みます。お問い合わせ受付後の対応状況管理はREADME上の別システムの責務として明記されておらず、実装有無だけで対象外とは判断しません。お問い合わせ状態管理の詳細は未確認事項です。[state-transition.md](./state-transition.md) |

## ④ 共通エラー処理

### GlobalExceptionLoggingAdvice

Controllerで処理されなかった`Exception`を捕捉し、4xxの`ResponseStatusException`は警告ログ、それ以外はエラーログを出力した後、元の例外を再送出します。ここでは画面遷移先を直接決めず、エラーController等の標準的なエラー処理に委ねます。[GlobalExceptionLoggingAdvice.java:16-44](../../src/main/java/jp/co/skig/officeorder/web/GlobalExceptionLoggingAdvice.java#L16-L44)

### AppErrorController

エラー要求のHTTPステータスをもとに、404、その他4xx、500の表示内容を決定します。HTML要求では404・その他4xxは`error/error`、500は`error/500`を返し、レスポンスにも該当ステータスを設定します。`Accept`に`application/json`を含む要求には、同じステータスのJSON（status、code、message、requestId、path）を返します。[AppErrorController.java:36-89](../../src/main/java/jp/co/skig/officeorder/web/AppErrorController.java#L36-L89) [AppErrorController.java:117-140](../../src/main/java/jp/co/skig/officeorder/web/AppErrorController.java#L117-L140)

### Spring Security

- `/mypage/**`およびレビューの指定GET／POSTは`MEMBER`ロールを要求し、それ以外は許可する設定です。[SecurityConfig.java:76-89](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L76-L89)
- ログイン処理はSpring Securityのフォームログインで、失敗時は`/login?error=true`へ遷移します。[SecurityConfig.java:91-100](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L91-L100) [SecurityConfig.java:142-163](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L142-L163)
- 無効セッション時はセッションCookieを期限切れにし、会員領域では`/login?expired=true`へ、公開領域では元のURLへリダイレクトします。[SecurityConfig.java:176-191](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L176-L191)
- 明示的な`AccessDeniedHandler`は今回確認できません。認可拒否時に実際に返る画面・HTTPステータスは、設定・実行時挙動を含めて未確認です。

### エラーメッセージとログ

フォームのフィールドエラーはThymeleafの`th:errors`等で画面に表示し、Controllerで処理する業務エラーはFlash属性として表示する実装があります。例: カートの`cartError`、注文の`checkoutError`、お問い合わせの`contactError`。[cart.html:19-19](../../src/main/resources/templates/pages/cart.html#L19-L19) [checkout-input.html:20-20](../../src/main/resources/templates/pages/checkout-input.html#L20-L20) [contact.html:26-90](../../src/main/resources/templates/pages/contact.html#L26-L90) 各Controller／Serviceでは、入力エラー件数や拒否理由等をログに記録する箇所があります。ログに記録する情報のマスキングは、ログイン失敗処理でメールアドレスに対して実施します。[SecurityConfig.java:154-160](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L154-L160)

## ⑤ 未確認事項・補足事項

| 項目 | 現時点の整理 | 分類 |
|---|---|---|
| LoginFormの制約がログインPOSTで実行されるか | Form上の制約はあるが、POSTはSpring Securityの認証処理であり、`@Valid`を介した実行は確認できない。 | 未確認／仕様・実装確認 |
| 認可拒否時の具体的な応答 | `AccessDeniedHandler`の明示設定を確認できず、画面・HTTPステータスの実動作は未確認。 | 未確認 |
| Cookie・不正トークン | カートCookieの解析時の除外や注文確認トークンの比較処理は実装されている。改ざん耐性などの要件適合性、異常なCookie・トークンを受けた場合の全HTTP応答は未確認。 | 未確認／仕様確認 |
| 更新・登録のDB例外全般 | 一部の例外はController／Serviceで変換・表示するが、すべてのDB例外が機能別の利用者向けメッセージに変換されるとは確認できない。 | 未確認 |
| 追加お届け先の選択ID | 注文画面の入力・表示には存在するが、選択IDから会員所有の住所をサーバー側で再取得して注文情報へ反映する処理は今回の確認範囲で確認できない。期待仕様も未確認。 | 追加確認が必要 |
| 退会・住所削除の更新件数0件 | 退会Serviceのboolean結果をControllerが画面表示に利用しないこと、住所削除結果を画面で区別しないことは確認できる。要件上の成功・失敗表示は未確認。 | 追加確認が必要 |
| 注文後の処理 | 決済結果照合、注文後在庫減算、注文状態更新等は、注文確定経路で実装済みとは確認できない。READMEが別システムの責務と明記する受注後の業務処理は実装対象外。 | 未確認事項および実装対象外（README明記範囲のみ） |
| Controllerテスト | 本資料で参照したテストはServiceテストが中心です。Controllerの入力エラー・画面遷移を確認するテストは、今回確認した範囲では該当するテストコードを確認できませんでした。 | 補足。テスト全体に存在しないとの断定ではない |

**実装対象外**として明記できるのは、READMEに別システムの責務とある「受注後の業務処理」「管理画面」です。[README.md:3-6](../../README.md#L3-L6) 決済連携や問い合わせ後の対応等は、READMEに個別の責務分担が示されているか、実装の有無・仕様を確認できるかを個別に判断し、記載のないものを推測で実装対象外とはしていません。

今回のソース確認だけでは、仕様との不一致や修正が必要な入力チェック・エラー処理を確定できません。ログインFormの検証適用、選択住所IDのサーバー検証、更新件数0件時の画面上の扱いは、期待仕様を確認した後に判断すべき事項であり、本資料では修正候補と断定していません。

## ⑥ 全体まとめ

- 商品閲覧では、検索条件を既知の候補に絞り、ページ番号・表示件数を正規化する実装が確認できました。商品が存在しない場合は404経路へ進みます。
- 会員登録、会員情報、追加お届け先、注文、お問い合わせでは、FormのBean ValidationとControllerの`BindingResult`を使い、入力画面を再表示する実装があります。法人会社名などはControllerの条件付き検証、メール重複・住所上限・在庫等はServiceでも確認します。
- ログインはSpring Securityが認証を処理します。`LoginForm`には制約があるものの、そのBean ValidationがログインPOSTで実行されるかは未確認です。
- カートはCookie値を解析・正規化し、数量・商品ID・在庫等をServiceで検証します。注文確定時にもカート、セッション、トークン、在庫等を検査します。
- 共通例外処理は未処理例外を記録して再送出し、`AppErrorController`が404・その他4xx・500をHTMLまたはJSONで返します。
- テストはService単位で一部の正常系・異常系を確認できます。一方、Controllerの画面遷移や全HTTPエラー応答については、今回確認した範囲ではテストコードを確認できない箇所があります。
- 受注後の業務処理と管理画面だけをREADMEに基づく実装対象外として扱いました。その他の未確認事項は、仕様や責務を確認する必要がある事項として残しています。
