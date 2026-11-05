# OFFICE ORDER システム全体構成資料

## 1. システム概要

OFFICE ORDER は、オフィス家具を扱う顧客向け EC サイトです。商品を探して閲覧し、会員登録・ログインを行い、カートから注文したり、注文後に履歴を確認したりする機能を提供します。商品レビュー、お問い合わせ、告知表示などの機能も含みます。

アプリケーションは Java / Spring Boot で構成され、画面は Thymeleaf テンプレート、データアクセスは MyBatis と PostgreSQL を利用します。バッチ処理では売れ筋ランキングとおすすめ関連商品を更新します。README によると、受注後の業務処理や管理画面は別システムの担当範囲です。

本資料では、ワークスペース内の実プロジェクトルート `AI_Training/` を対象にしています。`practice` に関係するディレクトリおよびファイルは調査対象から除外しています。`target/` はビルドによって生成される出力であり、実装の一次情報は `src/`、設定・SQL・ドキュメントは各ソースディレクトリを参照してください。

## 2. 全体のディレクトリ構成

```text
AI_Training/
├── .github/
│   └── modernize/
│       └── java-upgrade/
│           └── hooks/
│               └── scripts/
├── .mvn/
│   └── wrapper/
├── .vscode/
├── docs/
│   ├── design/
│   ├── issues/
│   └── plan/
├── log/
│   └── archive/
├── scripts/
├── sql/
│   ├── init/
│   ├── schema/
│   └── seed/
│       └── test-data/
├── src/
│   ├── main/
│   │   ├── java/jp/co/skig/officeorder/
│   │   │   ├── common/validation/
│   │   │   ├── config/
│   │   │   ├── logging/
│   │   │   ├── mapper/row/
│   │   │   ├── model/
│   │   │   │   ├── announcement/
│   │   │   │   ├── batch/
│   │   │   │   ├── cart/
│   │   │   │   ├── contact/
│   │   │   │   ├── coupon/
│   │   │   │   ├── mail/
│   │   │   │   ├── member/
│   │   │   │   ├── order/
│   │   │   │   ├── product/
│   │   │   │   └── review/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   │   ├── announcement/
│   │   │   │   ├── batch/
│   │   │   │   ├── cart/
│   │   │   │   ├── contact/
│   │   │   │   ├── coupon/
│   │   │   │   ├── mail/
│   │   │   │   ├── member/
│   │   │   │   ├── order/
│   │   │   │   ├── product/
│   │   │   │   └── review/
│   │   │   └── web/
│   │   │       ├── auth/
│   │   │       ├── review/
│   │   │       └── view/
│   │   └── resources/
│   │       ├── mail/templates/
│   │       ├── mappers/
│   │       ├── static/
│   │       │   ├── css/pages/
│   │       │   ├── images/products/
│   │       │   └── js/modules/
│   │       └── templates/
│   │           ├── error/
│   │           ├── fragments/
│   │           │   ├── common/
│   │           │   └── layout/
│   │           ├── pages/
│   │           └── review/
│   └── test/
│       └── java/jp/co/skig/officeorder/service/
│           ├── announcement/
│           ├── cart/
│           ├── contact/
│           ├── mail/
│           ├── member/
│           ├── order/
│           └── product/
└── target/  (ビルド生成物)
    ├── classes/
    ├── generated-sources/annotations/
    ├── generated-test-sources/test-annotations/
    ├── maven-status/maven-compiler-plugin/
    ├── surefire-reports/
    └── test-classes/
```

上記の階層図はディレクトリを中心に示しています。画面テンプレートや SQL など個別ファイルの内容は、次の役割一覧にある該当ディレクトリで管理されます。

## 3. 主要ディレクトリ・パッケージの役割

| パス | 役割 | 初学者向けの説明 |
|---|---|---|
| `.github/modernize/java-upgrade/` | Java のアップグレード作業に関するフックや補助スクリプト | 開発作業を補助する自動化設定です。利用者向け機能の実装ではありません。 |
| `.mvn/wrapper/` | Maven Wrapper の設定 | 開発環境に Maven を別途用意しなくても、プロジェクト指定の Maven を使うための設定です。 |
| `.vscode/` | VS Code 用のワークスペース設定 | エディターでプロジェクトを開発するときの設定です。 |
| `docs/design/` | 画面一覧、画面遷移、ER 図、CRUD 表、検索設計など | 実装前後の設計を人が確認するための資料です。 |
| `docs/issues/` | 要件、作業 TODO、障害報告など | 機能要望や課題の記録です。 |
| `docs/plan/` | テスト計画など | 作業や検証の進め方をまとめます。 |
| `log/`、`log/archive/` | アプリケーションログとローテーション済みログ | 稼働中に起きたことを調べるための記録です。 |
| `scripts/` | ローカル PostgreSQL 初期化用スクリプト | 開発用データベースを準備する手順を自動化します。 |
| `sql/init/` | DB の初期化 SQL | 新しいデータベースを準備する最初の SQL です。 |
| `sql/schema/` | 商品、会員、注文、バッチなどのテーブル定義 | アプリが保存するデータの形を定義します。 |
| `sql/seed/`、`sql/seed/test-data/` | 初期データ・テスト用データ投入 SQL | 開発や動作確認に使うサンプルデータを登録します。 |
| `src/main/java/.../common/` | 時刻、金額表示、入力チェックなどの共通処理 | 複数の機能で使う小さな道具をまとめています。 |
| `src/main/java/.../config/` | Spring、セキュリティ、メール、バッチなどの設定 | アプリ全体の動作ルールを組み立てます。 |
| `src/main/java/.../logging/` | リクエスト ID、会員情報のログ連携、マスキング | 問い合わせ時に処理を追跡しやすくし、ログに出す情報を制御します。 |
| `src/main/java/.../web/` | Web リクエストを受け付け、画面や応答につなぐコントローラー | ブラウザーから来た操作の受付窓口です。 |
| `src/main/java/.../web/auth/` | ログイン状態、会員セッション、認証まわりの補助 | 「誰がログインしているか」を扱います。 |
| `src/main/java/.../web/review/` | レビュー画面の Web 処理 | レビュー機能の画面要求を受け付けます。 |
| `src/main/java/.../web/view/` | 画面表示向けの補助処理 | HTML 画面を作るときの補助を行います。 |
| `src/main/java/.../service/` | 各業務機能の処理 | 画面の指示に応じて、業務上のルールや手順を実行します。 |
| `src/main/java/.../repository/` | DB やカート Cookie への読み書き | データをどこから取り出し、どこへ保存するかを担当します。 |
| `src/main/java/.../mapper/`、`mapper/row/` | MyBatis の DB 呼び出し口と検索結果の受け取り型 | SQL と Java の間でデータを受け渡します。 |
| `src/main/java/.../model/` | フォーム、画面表示データ、各機能の値オブジェクト | 画面入力や処理結果を、機能間で受け渡すデータの型です。 |
| `src/main/resources/application.yml` など | アプリ設定、ログ設定、メッセージ定義 | 実行時の接続先や表示文言など、コード以外の設定です。 |
| `src/main/resources/mappers/` | MyBatis の SQL マッピング XML | Java の Mapper と SQL 文を結び付けます。 |
| `src/main/resources/templates/` | Thymeleaf の HTML 画面 | ブラウザーに表示されるページや共通部品です。 |
| `src/main/resources/templates/fragments/` | ヘッダー、フッター、商品カードなどの共通画面部品 | 複数ページで同じ見た目や部品を再利用します。 |
| `src/main/resources/static/` | CSS、JavaScript、画像 | ブラウザーが直接読み込む見た目・操作・画像のファイルです。 |
| `src/main/resources/mail/` | メール件名・テンプレート | 注文完了などの通知メールを組み立てる素材です。 |
| `src/test/java/` | サービス層などの自動テスト | 機能の処理が期待どおりかを機械的に確認します。 |
| `target/` | Maven のコンパイル済みクラス、コピー済みリソース、テスト結果など | ビルド時に作られる出力です。通常、ソースを修正する場所ではありません。 |

## 4. Java パッケージの機能別構成

`jp.co.skig.officeorder` がアプリケーションのルートパッケージです。おおむね「web（受付）→ service（業務処理）→ repository / mapper（データアクセス）」の順に処理が流れ、`model` が各層の間でデータを運びます。

| パッケージ | 主な責務 |
|---|---|
| `common` | 全機能共通の時刻取得、金額表示、入力検証パターン |
| `config` | Web・Security・メール再試行・Spring Batch などの構成 |
| `logging` | リクエスト ID や会員識別子のログ付加、ログ用情報のマスキング |
| `web` | ホーム、商品、カート、会員登録、マイページ、お問い合わせ、告知、クーポン、エラー、内部バッチなどの入口 |
| `web.auth` | ログイン・ログアウト周辺のセッション、リダイレクト、会員状態確認 |
| `web.review` | 商品レビューの画面入口 |
| `web.view` | 静的アセットのバージョン付与など画面表示の補助 |
| `service.announcement` | お知らせの取得・表示 |
| `service.batch` | バッチの実行、スケジュール、状態管理、実行ログ |
| `service.cart` | カート内容の操作・集計 |
| `service.contact` | お問い合わせの受付 |
| `service.coupon` | クーポン情報と利用に関する処理 |
| `service.mail` | メール本文の組み立てと通知送信 |
| `service.member` | 会員登録・会員情報・認証用会員情報などの処理 |
| `service.order` | 注文、注文完了、注文履歴・再注文に関する処理 |
| `service.product` | 商品一覧・詳細、検索、絞り込み、表示順などの処理 |
| `service.review` | 商品レビューの登録・一覧などの処理 |
| `repository` | 各業務領域の永続データアクセスとカート保存の仲介 |
| `mapper`、`mapper.row` | MyBatis Mapper と、SQL の検索結果を受け取る行データ型 |
| `model` 配下の各領域 | 会員、商品、注文、カート、レビューなどのフォーム・表示用データ型 |

## 5. 主要機能ドメイン一覧

以下は、Java の `model`・`service`・`web` の分割、画面テンプレート、SQL スキーマ／初期データから読み取れる機能領域です。横断的な仕組みも含め、アプリケーションの責務を機能単位で整理しています。

| 機能ドメイン | 主な機能 | 主な実装・資料の所在 |
|---|---|---|
| トップ・コンテンツ | トップページ、サイト案内、利用ガイド、プライバシー・規約・特定商取引法表記、お知らせ | `web/HomeController`、`web/ContentController`、`web` の画面テンプレート、`model/announcement`、`service/announcement` |
| 商品カタログ・商品検索 | 商品一覧、カテゴリ別一覧、新着、キーワード検索、絞り込み、並び替え、商品詳細、色・バリエーション、シリーズ関連商品 | `web/CatalogController`、`service/product`、`model/product`、`mapper/ProductMapper`、`mappers/ProductMapper.xml`、`templates/pages/product-*` |
| 会員登録・認証 | 会員登録、ログイン／ログアウト、会員の有効状態確認、ログイン情報やセッションの管理 | `web/MemberRegistrationController`、`web/AuthController`、`web/auth`、`service/member`、`model/member`、`config/SecurityConfig` |
| マイページ・会員管理 | プロフィール変更、住所管理、退会、会員のお気に入り、注文履歴・注文詳細 | `web/MyPageController`、`service/member`、`service/order`、`model/member`、`templates/pages/mypage-*` |
| お気に入り | 商品のお気に入り登録・一覧表示 | `model/member`、`service/member`、`repository/MemberRepository`、マイページのお気に入り画面 |
| カート | 商品の追加・表示・数量などのカート操作、カート内容の保持 | `web/CartController`、`service/cart`、`repository/CartRepository`、`repository/CartCookieStore`、`model/cart`、`templates/pages/cart.html` |
| 注文・チェックアウト | 注文情報入力、確認、注文確定、完了画面、注文履歴からの再注文 | `service/order`、`model/order`、`web/CartController` 等の Web 入口、チェックアウト画面テンプレート、`sql/schema/orders.sql` |
| クーポン | クーポン情報と会員による利用状況 | `web/CouponController`、`service/coupon`、`model/coupon`、`mapper/CouponMapper`、`sql/schema/coupons.sql` |
| 商品レビュー | 商品レビューの一覧、投稿フォーム、レビュー表示 | `web/review`、`service/review`、`model/review`、`mapper/ReviewMapper`、`templates/review/` |
| お問い合わせ | お問い合わせ入力・受付、お問い合わせ時の会員情報補助 | `web/ContactController`、`service/contact`、`model/contact`、`mapper/ContactMapper`、`sql/schema/content.sql` |
| 通知メール | 注文完了などのメール内容生成・送信、メールテンプレート | `service/mail`、`model/mail`、`resources/mail/`、`config/MailRetryConfig` |
| ランキング・おすすめバッチ | 売れ筋ランキングとおすすめ関連商品の再計算、定期実行、実行状況の確認 | `config/BatchJobConfiguration`、`service/batch`、`web/InternalBatchController`、`model/batch`、`mapper/BatchMapper`、`sql/schema/batch.sql` |
| セキュリティ・入力検証 | 認証・認可、入力値チェック、会員状態に応じた制御 | `config/SecurityConfig`、`common/validation`、`web/auth`、Validation 設定 |
| ログ・エラー処理 | リクエスト追跡、個人情報のマスキング、例外ログ、エラー画面 | `logging`、`web/GlobalExceptionLoggingAdvice`、`web/AppErrorController`、`templates/error/`、`log/` |
| 共通画面・ブラウザー動作 | 共通レイアウト、パンくず、商品カード、フォーム制御、モーダル、カルーセルなど | `templates/fragments/`、`static/css/`、`static/js/` |

### 機能境界について

- このアプリは顧客向け EC の範囲を担います。README 上、管理画面や受注後の業務処理は別システムの担当です。
- `InternalBatchController` などの内部向け機能は、顧客向け画面とは別に、バッチ実行やローカル動作確認を支援します。
- `repository` と `mapper` は機能ドメインを横断してデータの読み書きを支えます。テーブル定義は `sql/schema/`、画面／メール素材は `src/main/resources/` と対応付けて読むと、機能の流れを追いやすくなります。

## 6. データベース・画面資材の見方

| 場所 | 内容 |
|---|---|
| `sql/schema/masters.sql` | 商品カテゴリなどの基礎マスタ |
| `sql/schema/products.sql` | 商品・商品関連データ |
| `sql/schema/members.sql` | 会員データ |
| `sql/schema/orders.sql` | 注文データ |
| `sql/schema/coupons.sql` | クーポンデータ |
| `sql/schema/content.sql` | お知らせ、お問い合わせ等のコンテンツ関連データ |
| `sql/schema/batch.sql`、`spring-batch-metadata.sql` | アプリ独自のバッチ関連データと Spring Batch の実行管理データ |
| `src/main/resources/mappers/` | Mapper ごとの SQL 定義 |
| `src/main/resources/templates/pages/` | トップ、商品、会員、カート、注文などの画面 |
| `src/main/resources/templates/review/` | レビュー画面 |
| `src/main/resources/static/` | CSS・JavaScript・商品画像 |

## 7. ルート直下の主要ファイル

| ファイル | 役割 |
|---|---|
| `pom.xml` | Maven のプロジェクト、依存ライブラリ、ビルド設定 |
| `mvnw`、`mvnw.cmd` | macOS/Linux 用、Windows 用の Maven Wrapper 起動スクリプト |
| `Dockerfile` | アプリケーションコンテナーのイメージ定義 |
| `docker-compose.yml` | 開発用の PostgreSQL などをまとめて起動する設定 |
| `README.md` | システム概要、ローカル起動、初期データ、バッチ操作などの案内 |
| `REVIEW_TODO.md` | レビューや確認事項の記録 |
| `ibm-bob-adoption-plan.md` | 開発支援ツール導入に関する計画資料 |
| `.gitignore` | Git の管理対象から除外するファイルの指定 |

