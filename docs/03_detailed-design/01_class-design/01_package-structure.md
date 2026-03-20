# パッケージ構成図

ベースパッケージ: `jp.co.skig.officeorder`

## 1. 全体ディレクトリツリー

```
jp.co.skig.officeorder
├── config/                      # 設定クラス
│   ├── SecurityConfig            # Spring Security認証・認可設定
│   ├── BatchJobConfiguration     # Spring Batchジョブ定義
│   ├── BatchExecutionConfig      # バッチ非同期実行設定
│   ├── AppClockConfig            # Clock Bean定義
│   ├── WebLoggingConfig          # リクエストログ用インターセプター
│   ├── MailRetryConfig           # メール送信リトライ設定
│   └── AppProperties             # application.yml app.* マッピング
│
├── web/                         # プレゼンテーション層
│   ├── controller/
│   │   ├── HomeController        # トップページ・お知らせ一覧
│   │   ├── CatalogController     # 商品一覧・検索・詳細
│   │   ├── CartController        # カート・チェックアウト
│   │   ├── AuthController        # ログイン画面
│   │   ├── MemberRegistrationController # 会員登録フロー
│   │   ├── MyPageController      # マイページ全般
│   │   ├── ContactController     # お問い合わせ
│   │   ├── ContentController     # 静的コンテンツ（会社概要等）
│   │   ├── InternalBatchController # バッチ管理API (local profile)
│   │   └── AppErrorController    # エラーページ
│   ├── advice/
│   │   ├── GlobalExceptionLoggingAdvice # 全例外ログ出力
│   │   └── AuthModelAdvice       # ログイン状態をModelに追加
│   └── auth/
│       ├── MemberPrincipal       # UserDetails実装
│       ├── MemberSessionService  # セッション操作
│       ├── MemberUserDetailsService # UserDetailsService実装
│       ├── LoginEmailCookieService  # メールRemember-Me
│       ├── MemberActiveValidationInterceptor # 有効会員チェック
│       └── AuthRedirectUtils     # オープンリダイレクト対策
│
├── service/                     # ビジネスロジック層
│   ├── product/
│   │   ├── ProductService        # 商品検索・詳細取得
│   │   └── ProductFilterOptionService # フィルターオプション管理
│   ├── cart/
│   │   └── CartService           # カート操作（Cookie/DB）
│   ├── order/
│   │   └── OrderService          # 注文確定・履歴管理
│   ├── member/
│   │   ├── MemberService         # 会員登録・編集・退会
│   │   ├── MemberRegistrationService # 登録フロー
│   │   └── MemberProfileService  # プロフィール・お届け先管理
│   ├── contact/
│   │   └── ContactService        # 問い合わせ受付
│   ├── announcement/
│   │   └── AnnouncementService   # お知らせ取得
│   ├── mail/
│   │   ├── NotificationMailService # メール送信
│   │   └── MailTemplateRenderer  # テンプレートレンダリング
│   └── batch/
│       ├── BatchJobService       # バッチジョブ実行
│       └── BatchScheduler        # スケジューラー
│
├── repository/                  # データアクセス層
│   ├── ProductRepository
│   ├── MemberRepository
│   ├── OrderRepository
│   ├── CartRepository
│   ├── AnnouncementRepository
│   ├── ContactRepository
│   ├── BatchRepository
│   └── ProductFilterOptionRepository
│
├── mapper/                      # MyBatis Mapperインターフェース
│   ├── ProductMapper
│   ├── MemberMapper
│   ├── OrderMapper
│   ├── CartMapper
│   ├── ContactMapper
│   ├── AnnouncementMapper
│   ├── BatchMapper
│   └── row/                     # SQLResult Rowレコード（20個）
│
├── model/                       # ドメインモデル・DTO
│   ├── product/                 # ProductCardView, ProductDetailView等 (15クラス)
│   ├── member/                  # MemberRegisterForm, MemberSessionUser等 (16クラス)
│   ├── order/                   # CheckoutInputForm, OrderCompleteView等 (4クラス)
│   ├── cart/                    # CartView, CartLineView等 (5クラス)
│   ├── contact/                 # ContactForm, ContactMemberPrefill (2クラス)
│   ├── announcement/            # AnnouncementView (1クラス)
│   ├── batch/                   # BatchJobSummaryResponse等 (5クラス)
│   └── mail/                    # OrderCompleteMailPayload (1クラス)
│
├── logging/                     # ロギング・MDC
│   ├── RequestIdFilter          # リクエストIDをMDCに設定
│   ├── MdcLoggingInterceptor    # コントローラー前後のMDCログ
│   └── LogMaskingUtils          # パスワード等マスキング
│
└── common/                      # 共通ユーティリティ
```

## 2. 層ごとの依存関係

```
[web] ──> [service] ──> [repository] ──> [mapper] ──> DB
  │                          │
  └─── [model] ─────────────┘

[config] ──> 全層に設定を提供
[logging] ──> 全層をインターセプト
```

## 3. パッケージ別クラス数

| パッケージ | クラス数 | 備考 |
|---|:---:|---|
| config | 7 | Spring設定クラス |
| web/controller | 10 | HTTPエンドポイント |
| web/advice | 2 | ControllerAdvice |
| web/auth | 7 | 認証補助クラス |
| service | 14 | ビジネスロジック |
| repository | 9 | データアクセス |
| mapper | 7 + 20 | MyBatisインターフェース + Rowレコード |
| model | 49 | ドメインモデル・DTO |
| logging | 5 | MDC・ロギング補助 |
| common | 3 | ユーティリティ |
| **合計** | **約145** | |
