# 業務ルール調査結果

## 調査の前提と責務範囲

本書は、READMEに挙げられた9機能について、実際のController、Service、Form、Mapper／SQL、スキーマ、画面テンプレートおよび関連テストを確認した結果をまとめたものです。記載はソース上の実装を説明するものであり、実装の業務上の妥当性を推測するものではありません。

READMEでは、本リポジトリの対象範囲を商品閲覧、会員登録、カート、注文、お問い合わせまでとし、受注後の業務処理や管理画面は別システムの責務としています。したがって、注文後の受注処理・出荷等は本書で実装済みとは扱いません。ランキングとおすすめ関連商品は、READMEに起動時および毎時の再計算バッチが記載されており、実装を調査対象に含めています。

「実装済み」は当該リポジトリ内に処理が存在すること、「実装対象外」はREADMEが別システムの責務とすること、「未確認」は今回確認したソースから動作・業務上の意味を確定できないことを表します。画面のチェックと、処理可否を決める業務条件は区別して記載しています。

READMEの責務範囲: [README.md:1-6](../../README.md#L1-L6)。バッチの実行タイミング: [README.md:78-98](../../README.md#L78-L98)。

## 1. 商品閲覧

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 商品一覧・検索では、検索語、在庫有のみ、価格帯、色、カテゴリ固有条件、並び順、ページ番号、表示件数を受け付ける。商品詳細は商品IDで指定する。 | [CatalogController.java:87-148](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L87-L148)、[CatalogController.java:284-301](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L284-L301) |
| 必須項目 | 一覧の検索条件は任意。詳細表示では商品IDで商品を特定する。 | [CatalogController.java:112-148](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L112-L148)、[CatalogController.java:284-301](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L284-L301) |
| 業務上の制約 | 一覧・詳細・シリーズリンクは、販売開始日時が現在以前かつ販売終了日時が未設定または現在より後の商品を対象とする。在庫有のみは一覧の絞り込み条件であり、在庫切れ商品も閲覧できる。 | [ProductMapper.xml:260-265](../../src/main/resources/mappers/ProductMapper.xml#L260-L265)、[ProductMapper.xml:440-479](../../src/main/resources/mappers/ProductMapper.xml#L440-L479) |
| 処理内容 | 新着一覧は販売開始が直近6か月以内。検索語は商品名・説明・バリエーション名・商品コードを対象とし、正規化後の複数トークンをANDで検索する。表示件数は15・30・60のいずれかに補正し、ページ番号は1以上に補正する。存在しない最終ページより大きいページは、検索結果がある場合に最終ページへ補正する。 | [ProductService.java:105-127](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java#L105-L127)、[ProductMapper.xml:267-278](../../src/main/resources/mappers/ProductMapper.xml#L267-L278)、[ProductService.java:300-319](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java#L300-L319)、[ProductListSearchService.java:226-245](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java#L226-L245) |
| 状態の変化 | 商品閲覧・検索時に商品データを更新する処理は未確認。お気に入り操作は「会員情報」を参照。 | [CatalogController.java:284-301](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L284-L301) |
| エラー条件 | 販売期間外または該当レコードのない商品詳細は404。無効な表示件数は15、1未満のページは1に補正される。検索結果のページ超過は最終ページへ補正される。利用者向けの専用エラーメッセージは未確認。 | [CatalogController.java:284-301](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L284-L301)、[ProductService.java:309-319](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java#L309-L319)、[ProductListSearchService.java:226-245](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java#L226-L245) |

## 2. 会員登録

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 個人／法人、氏名・カナ、会社・部署、メール、性別、生年月日／記念日、パスワード、メルマガ希望、住所、階数、エレベーター、電話・FAXを受け付ける。 | [MemberRegisterForm.java:24-105](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L24-L105) |
| 必須項目 | 個人／法人、氏名・カナ、メール、性別、日付、パスワード、メルマガ希望、郵便番号、都道府県・市区町村・住所、階数、エレベーター、日中電話が必須。法人の場合は会社名も必須。 | [MemberRegisterForm.java:24-102](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L24-L102)、[MemberRegistrationController.java:165-168](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L165-L168) |
| 業務上の制約 | メールアドレスが既に存在する場合は登録できない。重複確認は大文字小文字を区別せず照合し、会員状態による除外条件はSQLにない。 | [MemberRegistrationController.java:89-93](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L89-L93)、[MemberService.java:113-130](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L130)、[MemberMapper.xml:90-104](../../src/main/resources/mappers/MemberMapper.xml#L90-L104) |
| 処理内容 | 入力確認時に空白等を正規化し、入力検証・法人会社名・重複メールを確認した後、フォームをセッションに保持する。確定時にパスワードをBCryptでハッシュ化して登録し、登録後は有効会員として読み直す。 | [MemberRegistrationController.java:84-109](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L84-L109)、[MemberService.java:113-139](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L113-L139)、[SecurityConfig.java:59-60](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L59-L60) |
| 状態の変化 | 確認前のフォームはセッションに保持される。登録確定時に会員をactiveとして作成し、仮登録フォームをセッションから削除してログイン状態にし、登録完了メールを送る。 | [MemberRegistrationController.java:101-103](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L101-L103)、[MemberRegistrationController.java:135-143](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L135-L143)、[MemberMapper.xml:156-204](../../src/main/resources/mappers/MemberMapper.xml#L156-L204) |
| エラー条件 | 入力不備は登録入力画面に戻る。確認時および登録確定時の重複メールはエラーとして表示する。登録確定時に確認用フォームがセッションにない場合は登録入力へ戻る。 | [MemberRegistrationController.java:89-99](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L89-L99)、[MemberRegistrationController.java:120-134](../../src/main/java/jp/co/skig/officeorder/web/MemberRegistrationController.java#L120-L134)、[messages.properties:18](../../src/main/resources/messages.properties#L18) |

**入力制限:** 氏名・カナ50文字、会社・部署120文字、メール254文字、郵便番号は半角数字3桁＋4桁、電話・FAXは半角数字10～12桁、階数は半角数字、パスワードは印字可能なASCII文字8～64文字。カナ、個人／法人、性別の値も形式検証される。制限の業務上の理由は未確認。根拠: [MemberRegisterForm.java:28-104](../../src/main/java/jp/co/skig/officeorder/model/member/MemberRegisterForm.java#L28-L104)、[ValidationPatterns.java:11-27](../../src/main/java/jp/co/skig/officeorder/common/validation/ValidationPatterns.java#L11-L27)。

## 3. 会員情報

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | マイページでプロフィール変更、追加お届け先の追加・変更・削除、退会ができる。商品詳細のお気に入り操作も会員情報として保存する。 | [MyPageController.java:216-230](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L216-L230)、[MyPageController.java:288-406](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L288-L406)、[MyPageController.java:412-426](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L412-L426)、[CatalogController.java:320-369](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java#L320-L369) |
| 必須項目 | プロフィール・追加お届け先では氏名、カナ、郵便番号、住所、階数、電話等が必須。法人会員では会社名が必須。 | [MemberProfileEditForm.java:24-100](../../src/main/java/jp/co/skig/officeorder/model/member/MemberProfileEditForm.java#L24-L100)、[MemberAdditionalAddressForm.java:24-76](../../src/main/java/jp/co/skig/officeorder/model/member/MemberAdditionalAddressForm.java#L24-L76)、[MyPageController.java:443-460](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L443-L460) |
| 業務上の制約 | マイページはMEMBERロール必須。追加お届け先は会員あたり20件まで。お気に入りは100件までで、既に登録済みの場合は解除処理を行う。メールアドレスは他会員と重複不可。 | [SecurityConfig.java:83](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L83)、[MemberService.java:39-43](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L39-L43)、[MemberService.java:191-205](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L191-L205)、[MemberService.java:336-359](../../src/main/java/jp/co/skig/officeorder/service/member/MemberService.java#L336-L359) |
| 処理内容 | 追加お届け先の参照・更新・削除は会員IDとお届け先IDを併用して対象を絞る。プロフィール更新後は有効会員情報を再取得してセッション会員を更新する。 | [MemberMapper.xml:249-270](../../src/main/resources/mappers/MemberMapper.xml#L249-L270)、[MemberMapper.xml:314-339](../../src/main/resources/mappers/MemberMapper.xml#L314-L339)、[MyPageController.java:248-252](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L248-L252) |
| 状態の変化 | 退会は会員レコードを削除せず、activeからwithdrawnへ変更し退会日時を設定する。処理後にセッションを破棄する。退会済み会員は認証情報の検索対象外。 | [MemberMapper.xml:441-448](../../src/main/resources/mappers/MemberMapper.xml#L441-L448)、[MyPageController.java:421-426](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L421-L426)、[MemberMapper.xml:107-127](../../src/main/resources/mappers/MemberMapper.xml#L107-L127) |
| エラー条件 | 入力不備・重複メール・お届け先上限超過はエラーとして画面に返る。本人所有でない／存在しないお届け先の参照・更新は404。 | [MyPageController.java:239-252](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L239-L252)、[MyPageController.java:336-354](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L336-L354)、[MyPageController.java:307-324](../../src/main/java/jp/co/skig/officeorder/web/MyPageController.java#L307-L324) |

## 4. ログイン・認証

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | ログインフォームの入力はメールアドレスとパスワード。LoginFormにはメール形式・254文字以内、パスワード必須・64文字以内のBean Validation定義がある。 | [LoginForm.java:10-21](../../src/main/java/jp/co/skig/officeorder/model/member/LoginForm.java#L10-L21) |
| 必須項目 | メールアドレスとパスワード。 | [LoginForm.java:12-19](../../src/main/java/jp/co/skig/officeorder/model/member/LoginForm.java#L12-L19) |
| 業務上の制約 | メールアドレスに一致するactive会員のみ認証情報の検索対象。マイページはMEMBERロール必須。パスワードエンコーダーはBCrypt。 | [MemberMapper.xml:107-127](../../src/main/resources/mappers/MemberMapper.xml#L107-L127)、[SecurityConfig.java:59-60](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L59-L60)、[SecurityConfig.java:83](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L83) |
| 処理内容 | Spring Securityが`/login`を認証処理し、成功時は安全化された戻り先へ遷移する。戻り先がなければ`/mypage/orders`。ログアウト時はセッションを無効化し、認証状態と指定Cookieを消去する。 | [SecurityConfig.java:92-105](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L92-L105)、[SecurityConfig.java:117-136](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L117-L136) |
| 状態の変化 | ログイン成功で認証済み状態になる。認証後の会員が無効化されていた場合、マイページアクセス時にセッションを消し、期限切れ表示付きログイン画面へ誘導する。 | [MemberActiveValidationInterceptor.java:48-72](../../src/main/java/jp/co/skig/officeorder/web/auth/MemberActiveValidationInterceptor.java#L48-L72) |
| エラー条件 | 認証失敗時は`/login?error=true`へ遷移し、メールアドレスまたはパスワードが正しくない旨を表示する。セッション期限切れ時は再ログインを案内する。LoginFormのBean ValidationがログインPOST時にサーバーで実行される経路は確認できず、HTML入力型による検査とサーバー認証を区別する必要がある。 | [SecurityConfig.java:142-164](../../src/main/java/jp/co/skig/officeorder/config/SecurityConfig.java#L142-L164)、[login.html:21-24](../../src/main/resources/templates/pages/login.html#L21-L24)、[login.html:27-46](../../src/main/resources/templates/pages/login.html#L27-L46)、[AuthController.java:38-61](../../src/main/java/jp/co/skig/officeorder/web/AuthController.java#L38-L61) |

## 5. カート

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 商品バリアントID、数量（省略時1）、組立指定、任意の戻り先を受け付ける。カート明細はCookieからロードする。 | [CartController.java:122-154](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L122-L154)、[CartService.java:94-103](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L94-L103) |
| 必須項目 | 追加時は正のバリアントIDと数量1～99。更新時も数量1～99。 | [CartService.java:224-237](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L237)、[CartService.java:275-281](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L275-L281) |
| 業務上の制約 | 追加・更新時に在庫が0以下なら拒否する。カートの数量が在庫数以下かは注文確定時に再検査する。組立不可商品では組立指定を無効化する。 | [CartService.java:236-237](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L236-L237)、[OrderService.java:416-424](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L416-L424)、[CartService.java:430-441](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L430-L441) |
| 処理内容 | 同一商品は数量を合算し、99を上限にする。商品価格・組立費を数量倍して小計を計算し、税額は端数切捨て。税込小計5,000円以上で送料0円、未満で800円。クーポンはログイン会員のみ適用する。 | [CartService.java:242-256](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L242-L256)、[CartService.java:113-150](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L113-L150)、[CartService.java:158-180](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L158-L180) |
| 状態の変化 | 追加・更新・削除・全消去でCookieのカート明細を更新する。クーポンコードはセッションに保持する。未ログインまたは適用不可のクーポンはセッションから除去する。 | [CartService.java:224-260](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L224-L260)、[CartController.java:198-217](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L198-L217)、[CartService.java:165-180](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L165-L180) |
| エラー条件 | 不正ID、数量範囲外、在庫切れ、商品情報取得失敗、カート明細上限超過は拒否する。追加・更新時の`IllegalArgumentException`はフラッシュメッセージとして画面に返す。クーポンは有効フラグ・期間・最低購入額・会員別利用回数を検査する。割引後合計が負数になる場合は0に補正する。 | [CartService.java:229-253](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L229-L253)、[CartController.java:136-144](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L136-L144)、[CouponServiceImpl.java:25-59](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L25-L59)、[CartService.java:171-180](../../src/main/java/jp/co/skig/officeorder/service/cart/CartService.java#L171-L180) |

**クーポンと注文の未確認事項:** クーポン適用時の検証・割引計算は実装されている。一方、注文確定時に注文パラメータへクーポンコード・割引額を設定する処理、および利用回数を記録する処理が注文経路から呼ばれる箇所は確認できない。したがって、クーポン利用が注文記録・利用回数に反映されるかは未確認。根拠: [OrderService.java:148-260](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L260)、[OrderMapper.xml:124-205](../../src/main/resources/mappers/OrderMapper.xml#L124-L205)、[CouponServiceImpl.java:66-74](../../src/main/java/jp/co/skig/officeorder/service/coupon/CouponServiceImpl.java#L66-L74)。

## 6. 注文

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 注文者・配送先情報と決済方法（銀行振込、代引、コンビニ決済）を入力する。ログイン会員・ゲストのどちらも注文Serviceに渡される。 | [CheckoutInputForm.java:22-94](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L22-L94)、[OrderService.java:148-168](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L168) |
| 必須項目 | 個人／法人、氏名・カナ、メール、電話、郵便番号、住所、階数、エレベーター、決済方法が必須。法人の場合は会社名も必須。 | [CheckoutInputForm.java:22-92](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L22-L92)、[CartController.java:482-486](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L482-L486) |
| 業務上の制約 | 空カートでは注文不可。注文確定時に在庫を再確認し、在庫なしまたは数量超過なら拒否する。入力・確認画面に進む各時点でカートが空ならカートへ戻る。確認フォームとトークンをセッションで検証する。 | [OrderService.java:148-164](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L164)、[OrderService.java:416-424](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L416-L424)、[CartController.java:268-279](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L268-L279)、[CartController.java:340-355](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L340-L355)、[CartController.java:377-400](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L377-L400) |
| 処理内容 | 注文・注文明細・初期ステータス履歴を保存し、注文番号を発行する。組立指定のある明細が1つでもあれば配送方法をassemblyとする。商品小計・組立費・送料・税・合計はカート計算値を使用し、明細税額は明細小計に税率を掛けて整数単位で切捨てる。 | [OrderService.java:163-212](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L163-L212)、[OrderService.java:236-240](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L236-L240)、[OrderMapper.xml:124-205](../../src/main/resources/mappers/OrderMapper.xml#L124-L205) |
| 状態の変化 | 注文は`received`で保存され、同じ状態の履歴を作る。注文成功後に注文入力セッションとカートを消去し、完了画面へ遷移する。後続の注文状態遷移は本リポジトリの注文確定処理では未確認。 | [OrderMapper.xml:124-128](../../src/main/resources/mappers/OrderMapper.xml#L124-L128)、[OrderService.java:236-240](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L236-L240)、[CartController.java:402-409](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L402-L409) |
| エラー条件 | 空カート、在庫不足、法人の会社名未入力、階数の欠落・整数変換失敗、確認トークン不一致は注文不可。入力エラーは入力画面へ戻り、確定時の業務エラーも入力画面にメッセージ付きで戻る。 | [OrderService.java:148-153](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L153)、[OrderService.java:403-424](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L403-L424)、[OrderService.java:436-443](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L436-L443)、[CartController.java:390-412](../../src/main/java/jp/co/skig/officeorder/web/CartController.java#L390-L412) |

**実装対象外:** READMEは受注後の業務処理と管理画面を別システムの責務と明記している。本リポジトリに定義されている注文ステータス候補だけから、後続の受注・出荷等の処理が本リポジトリで実装済みとは判断しない。根拠: [README.md:3-6](../../README.md#L3-L6)、[orders.sql:5-6](../../sql/schema/orders.sql#L5-L6)。

**未確認事項:** 注文確定フローで在庫数を減らす処理は確認できなかった。追加お届け先の選択は画面JavaScriptで入力欄へ転記されるが、サーバー側で選択IDから住所を読み直して確定する処理は確認できず、送信フォームの住所項目が注文に使われる。クーポンの注文保存・利用回数の扱いは「カート」記載のとおり未確認。根拠: [OrderService.java:148-260](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java#L148-L260)、[checkout-input.js:25-87](../../src/main/resources/static/js/modules/checkout-input.js#L25-L87)、[CheckoutInputForm.java:94-120](../../src/main/java/jp/co/skig/officeorder/model/order/CheckoutInputForm.java#L94-L120)。

## 7. お問い合わせ

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 会社・部署、氏名、メール、電話、問い合わせ種別、注文状況、商品名・コード、問い合わせ内容を受け付ける。ログイン会員には会員情報の一部を初期値として設定する。 | [ContactForm.java:21-62](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L21-L62)、[ContactService.java:47-64](../../src/main/java/jp/co/skig/officeorder/service/contact/ContactService.java#L47-L64) |
| 必須項目 | 氏名、メール、問い合わせ種別、注文状況、問い合わせ内容。 | [ContactForm.java:27-62](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L27-L62) |
| 業務上の制約 | 種別はproduct／delivery_date／order／shipping／return_cancel／other、注文状況はbefore_order／after_orderに限定。ログインは必須ではない。ログイン中は有効会員なら問い合わせに会員IDを紐付ける。 | [ValidationPatterns.java:29-33](../../src/main/java/jp/co/skig/officeorder/common/validation/ValidationPatterns.java#L29-L33)、[ContactController.java:81-98](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L81-L98)、[ContactMapper.xml:18-29](../../src/main/resources/mappers/ContactMapper.xml#L18-L29) |
| 処理内容 | 入力文字列をtrimし、空欄をnullにして問い合わせレコードを登録する。成功時に問い合わせIDを`INQ`＋8桁で表示する。 | [ContactForm.java:71-96](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L71-L96)、[ContactService.java:74-83](../../src/main/java/jp/co/skig/officeorder/service/contact/ContactService.java#L74-L83)、[ContactController.java:98-114](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L98-L114)、[ContactMapper.xml:31-58](../../src/main/resources/mappers/ContactMapper.xml#L31-L58) |
| 状態の変化 | 問い合わせレコードを新規作成する。受付後の対応状態管理・担当者業務は未確認であり、README上も受注後業務等は別システムの責務。 | [ContactMapper.xml:31-58](../../src/main/resources/mappers/ContactMapper.xml#L31-L58)、[README.md:3-6](../../README.md#L3-L6) |
| エラー条件 | 入力不備は問い合わせ画面に戻り、フィールドエラーを表示する。保存結果からIDを取得できない場合は例外となり、Controllerは問い合わせ画面にエラーメッセージを設定する。 | [ContactController.java:81-107](../../src/main/java/jp/co/skig/officeorder/web/ContactController.java#L81-L107)、[ContactRepository.java:49-53](../../src/main/java/jp/co/skig/officeorder/repository/ContactRepository.java#L49-L53) |

**入力制限:** 会社・部署120文字、氏名50文字、メール254文字、電話は半角数字10～12桁、商品名255文字、商品コード32文字、問い合わせ内容1000文字。根拠: [ContactForm.java:21-62](../../src/main/java/jp/co/skig/officeorder/model/contact/ContactForm.java#L21-L62)、[ValidationPatterns.java:23-33](../../src/main/java/jp/co/skig/officeorder/common/validation/ValidationPatterns.java#L23-L33)。

## 8. 売れ筋ランキング

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 利用者入力はない。ランキング再計算はアプリ起動時および毎時0分に実行される。 | [BatchScheduler.java:32-43](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L32-L43)、[README.md:78-83](../../README.md#L78-L83) |
| 必須項目 | 集計には注文・注文明細・商品バリアントおよび商品データが必要。条件に合う注文がない場合は候補がない。 | [BatchMapper.xml:21-35](../../src/main/resources/mappers/BatchMapper.xml#L21-L35) |
| 業務上の制約 | 算出時点から直近30日かつキャンセル以外の注文が対象。商品は算出時点で販売期間内であること。 | [BatchJobService.java:60-77](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L60-L77)、[BatchMapper.xml:21-35](../../src/main/resources/mappers/BatchMapper.xml#L21-L35) |
| 処理内容 | 商品ごとの販売数量合計を計算し、数量降順・同数ならproduct_id昇順で最大10件を採用する。当日分を削除して再登録する。トップ画面には最大8件を取得する。 | [BatchJobService.java:73-84](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L73-L84)、[BatchMapper.xml:21-49](../../src/main/resources/mappers/BatchMapper.xml#L21-L49)、[ProductService.java:31-66](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java#L31-L66) |
| 状態の変化 | 当日分ランキングを全置換する。ランキングデータが空の場合、トップ表示は新着商品で代替する。 | [BatchMapper.xml:38-60](../../src/main/resources/mappers/BatchMapper.xml#L38-L60)、[ProductRepository.java:193-218](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java#L193-L218) |
| エラー条件 | バッチ実行時の失敗条件・画面向けエラー表示は、今回の調査ではランキング機能として確定できなかった。利用者向けの個別エラーメッセージは未確認。 | [BatchJobService.java:73-92](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L73-L92)、[BatchScheduler.java:32-41](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L32-L41) |

## 9. おすすめ関連商品

| 項目 | 調査結果 | 根拠となるファイル・箇所 |
|---|---|---|
| 入力条件 | 利用者からの入力はない。商品詳細の商品IDを元商品として関連商品を取得する。再計算バッチは起動時および毎時0分に実行される。 | [ProductRepository.java:222-246](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java#L222-L246)、[BatchScheduler.java:32-43](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchScheduler.java#L32-L43) |
| 必須項目 | 元商品IDが必要。対象日の推薦データがない場合は新着商品から元商品を除いて候補を作る。 | [ProductMapper.xml:483-498](../../src/main/resources/mappers/ProductMapper.xml#L483-L498)、[ProductRepository.java:454-481](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java#L454-L481) |
| 業務上の制約 | 直近30日かつキャンセル以外の注文を対象に、同一注文内に登場する商品同士を候補とする。 | [BatchJobService.java:98-114](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L98-L114)、[BatchMapper.xml:62-72](../../src/main/resources/mappers/BatchMapper.xml#L62-L72) |
| 処理内容 | 商品ペア共起回数を各商品の注文登場数で正規化するコサイン類似度を計算し、小数6桁に丸める。スコア降順・同点はrecommended_product_id昇順で並べ、元商品ごとに最大4件を採用する。当日分を削除して再登録する。 | [BatchJobService.java:135-205](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L135-L205)、[BatchMapper.xml:75-96](../../src/main/resources/mappers/BatchMapper.xml#L75-L96) |
| 状態の変化 | 当日分の推薦結果を全置換する。商品詳細では最新日付の結果を表示し、対象結果が空なら新着商品から元商品を除いて最大4件を表示する。 | [BatchMapper.xml:75-96](../../src/main/resources/mappers/BatchMapper.xml#L75-L96)、[ProductMapper.xml:483-498](../../src/main/resources/mappers/ProductMapper.xml#L483-L498)、[ProductRepository.java:454-481](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java#L454-L481) |
| エラー条件 | 推薦候補が空の場合は計算結果なしとなる。推薦表示の利用者向けエラーメッセージは未確認。推薦SQLに販売期間・在庫による対象除外条件は見当たらないが、その業務上の意図は未確認。 | [BatchJobService.java:135-138](../../src/main/java/jp/co/skig/officeorder/service/batch/BatchJobService.java#L135-L138)、[ProductMapper.xml:483-498](../../src/main/resources/mappers/ProductMapper.xml#L483-L498) |

## 全体の分類

### 1. 実装から確認できた業務ルール

- 商品一覧・詳細の販売期間判定、検索・絞り込み、ページ・表示件数の補正。
- 会員登録の入力検証、法人会社名必須条件、メール重複判定、パスワードハッシュ化。
- 会員プロフィール・追加お届け先・お気に入りの更新、お届け先／お気に入り件数上限、退会状態の更新。
- active会員の認証、ログアウト、会員状態の再確認。
- カートの数量・在庫・組立指定、税・送料計算、クーポン適用判定。
- 注文確定時のカート・在庫・入力検査、注文・明細・初期状態履歴の保存。
- お問い合わせの入力検証・保存。
- 売れ筋ランキングとおすすめ関連商品の集計・保存・表示。

### 2. 実装は存在するが、業務上の意味を判断できないもの

- 会員入力の「生年月日／記念日」の用途。
- ランキングの対象期間30日・上位10件、おすすめ商品のコサイン類似度・上位4件を採る業務上の理由。
- クーポンの最低購入額、割引方法・割引対象の業務上の意図。
- 注文ステータス候補の後続運用。
- おすすめ商品を販売期間・在庫で絞らないSQLの意図。

これらは実装上の条件・計算自体は確認できるものの、業務上の設計意図はソースから確認できません。

### 3. ソースコードからは確認できなかったもの

- 注文確定時の在庫減算処理。
- 注文の後続ステータス遷移、キャンセル・出荷等の受注後業務。
- お問い合わせ受付後の担当者による対応・状態管理。
- クーポンコード／割引額の注文保存と、注文確定時のクーポン利用回数記録。
- 追加お届け先の選択IDをサーバー側で再取得・検証して注文住所に反映する処理。

READMEの責務範囲上、受注後の業務処理・管理画面は別システムの責務であり、本リポジトリで実装済みとは扱いません。根拠: [README.md:3-6](../../README.md#L3-L6)。