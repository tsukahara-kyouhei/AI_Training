# 設計書一覧

## 概要

本ドキュメントは、Office Order プロジェクトにおける全設計書の一覧です。

## 設計書一覧

| 章番号 | 第1階層 | 第2階層 | 第3階層 | ファイル名（英名） | ファイル名（日本語名） | 内容 | 対応する主な機能 |
|---|---|---|---|---|---|---|---|
| 00 | - | - | - | 00_document-list.md | 設計書一覧 | 全設計書の一覧・所在・概要 | プロジェクト全体 |
| 01-01 | 01_project-overview/ | - | - | 01_system-overview.md | システム概要書 | プロジェクトの目的・対象ユーザー・全体構成 | プロジェクト全体 |
| 01-02 | 01_project-overview/ | - | - | 02_tech-stack.md | 技術スタック | 使用技術・フレームワーク・バージョン一覧 | プロジェクト全体 |
| 01-03 | 01_project-overview/ | - | - | 03_glossary.md | 用語集 | 業務用語・技術用語の定義 | プロジェクト全体 |
| 02-01 | 02_basic-design/ | - | - | 01_feature-list.md | 機能一覧 | 全機能の洗い出しとカテゴリ分類 | 全機能 |
| 02-02 | 02_basic-design/ | - | - | 02_system-architecture.md | システム構成図 | アプリ/DB/メール/Docker構成 | docker-compose.yml |
| 02-03 | 02_basic-design/ | - | - | 03_code-definition.md | コード定義書 | ステータス値・区分値・定数の定義 | カテゴリ、注文ステータス、支払方法等 |
| 02-04-01 | 02_basic-design/ | 04_screen-design/ | - | 01_screen-list.md | 画面一覧 | 全画面のID・名称・URL対応表 | 全画面 |
| 02-04-02 | 02_basic-design/ | 04_screen-design/ | - | 02_screen-transition.md | 画面遷移図 | 画面間の遷移フロー | 全画面 |
| 02-04-03 | 02_basic-design/ | 04_screen-design/ | - | 03_common-layout.md | 共通レイアウト | ヘッダー/フッター/サイドバー等の共通部品 | fragments/ |
| 02-04-04-01 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 01_SCR-TOP.md | トップページ | 新着商品・ランキング・お知らせ表示 | HomeController |
| 02-04-04-02 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 02_SCR-CAT.md | 商品一覧 | カテゴリ別商品表示・検索・フィルタ | CatalogController |
| 02-04-04-03 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 03_SCR-DTL.md | 商品詳細 | 商品情報・カラーバリエーション・関連商品 | CatalogController |
| 02-04-04-04 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 04_SCR-CRT.md | カート | カート内容表示・数量変更・削除 | CartController |
| 02-04-04-05 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 05_SCR-CO01.md | チェックアウト - 支払方法選択 | 支払方法の選択画面 | CartController |
| 02-04-04-06 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 06_SCR-CO02.md | チェックアウト - 入力 | 配送先・連絡先の入力 | CartController |
| 02-04-04-07 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 07_SCR-CO03.md | チェックアウト - 確認 | 注文内容の最終確認 | CartController |
| 02-04-04-08 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 08_SCR-CO04.md | チェックアウト - 完了 | 注文完了表示 | CartController |
| 02-04-04-09 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 09_SCR-LGN.md | ログイン | メール/パスワード認証 | AuthController |
| 02-04-04-10 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 10_SCR-REG01.md | 会員登録 | 会員情報入力フォーム | MemberRegistrationController |
| 02-04-04-11 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 11_SCR-REG02.md | 会員登録確認 | 入力内容の確認画面 | MemberRegistrationController |
| 02-04-04-12 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 12_SCR-REG03.md | 会員登録完了 | 登録完了表示 | MemberRegistrationController |
| 02-04-04-13 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 13_SCR-MY.md | マイページ | プロフィール・住所・注文履歴・お気に入り | MyPageController |
| 02-04-04-14 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 14_SCR-CNT.md | お問い合わせ | 問い合わせフォーム | ContactController |
| 02-04-04-15 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 15_SCR-ANN.md | お知らせ | お知らせ一覧・詳細表示 | HomeController |
| 02-04-04-16 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 16_SCR-ABT.md | サイト情報 | 会社概要・利用規約・特商法・プライバシー | ContentController |
| 02-04-04-17 | 02_basic-design/ | 04_screen-design/ | 04_screens/ | 17_SCR-ERR.md | エラーページ | 404/500等のエラー表示 | AppErrorController |
| 02-05-01 | 02_basic-design/ | 05_api-design/ | - | 01_url-list.md | URL一覧 | 全エンドポイントのメソッド・パス・パラメータ | 全コントローラー |
| 02-05-02 | 02_basic-design/ | 05_api-design/ | - | 02_batch-api.md | バッチAPI定義 | バッチ実行用REST API仕様 | InternalBatchController |
| 02-06-01 | 02_basic-design/ | 06_data-design/ | - | 01_er-diagram.md | ER図 | テーブル間の関連図 | 全テーブル |
| 02-06-02-01 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 01_TBL-members.md | 会員テーブル定義 | members, member_additional_addresses | 会員管理 |
| 02-06-02-02 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 02_TBL-products.md | 商品テーブル定義 | products, product_variants, 各属性テーブル | 商品管理 |
| 02-06-02-03 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 03_TBL-orders.md | 注文テーブル定義 | orders, order_lines | 注文管理 |
| 02-06-02-04 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 04_TBL-shopping-cart.md | カートテーブル定義 | shopping_cart, cart_lines | カート機能 |
| 02-06-02-05 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 05_TBL-announcements.md | お知らせテーブル定義 | announcements | お知らせ管理 |
| 02-06-02-06 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 06_TBL-inquiries.md | 問い合わせテーブル定義 | inquiries | お問い合わせ |
| 02-06-02-07 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 07_TBL-rankings.md | ランキングテーブル定義 | popular_product_rankings, recommended_related_products | バッチ処理 |
| 02-06-02-08 | 02_basic-design/ | 06_data-design/ | 02_tables/ | 08_TBL-tax-rates.md | 税率テーブル定義 | tax_rates | 税率管理 |
| 02-06-03 | 02_basic-design/ | 06_data-design/ | - | 03_master-data.md | マスタデータ定義 | colors, desk_top_shapes, chair_functions等 | マスタ管理 |
| 02-06-04 | 02_basic-design/ | 06_data-design/ | - | 04_crud-diagram.md | CRUD図 | 機能×テーブルの操作マトリクス | 全機能・全テーブル |
| 03-01-01 | 03_detailed-design/ | 01_class-design/ | - | 01_package-structure.md | パッケージ構成図 | web/service/repository/model/config各層の構造 | 全パッケージ |
| 03-01-02 | 03_detailed-design/ | 01_class-design/ | - | 02_class-diagram.md | クラス図 | 主要クラスの関連図 | 全クラス |
| 03-02-01 | 03_detailed-design/ | 02_sequence/ | - | 01_SEQ-checkout.md | チェックアウトシーケンス図 | 4ステップの注文処理フロー | CartController, OrderService |
| 03-02-02 | 03_detailed-design/ | 02_sequence/ | - | 02_SEQ-register.md | 会員登録シーケンス図 | 会員登録の処理フロー | MemberRegistrationController |
| 03-02-03 | 03_detailed-design/ | 02_sequence/ | - | 03_SEQ-login.md | ログインシーケンス図 | 認証処理フロー | AuthController, Spring Security |
| 03-02-04 | 03_detailed-design/ | 02_sequence/ | - | 04_SEQ-cart.md | カート操作シーケンス図 | カート追加・変更・削除フロー | CartController, CartService |
| 03-02-05 | 03_detailed-design/ | 02_sequence/ | - | 05_SEQ-order.md | 注文処理シーケンス図 | 注文確定・通知メール送信フロー | OrderService, NotificationMailService |
| 03-03-01 | 03_detailed-design/ | 03_business-logic/ | - | 01_BL-product.md | 商品サービス設計 | 商品検索・フィルタ・詳細取得ロジック | ProductService |
| 03-03-02 | 03_detailed-design/ | 03_business-logic/ | - | 02_BL-cart.md | カートサービス設計 | カート操作・Cookie管理・セッション管理 | CartService |
| 03-03-03 | 03_detailed-design/ | 03_business-logic/ | - | 03_BL-order.md | 注文サービス設計 | 注文作成・番号採番・在庫確認 | OrderService |
| 03-03-04 | 03_detailed-design/ | 03_business-logic/ | - | 04_BL-member.md | 会員サービス設計 | 会員登録・認証・退会ロジック | MemberService |
| 03-03-05 | 03_detailed-design/ | 03_business-logic/ | - | 05_BL-contact.md | 問い合わせサービス設計 | 問い合わせ受付・保存処理 | ContactService |
| 03-03-06 | 03_detailed-design/ | 03_business-logic/ | - | 06_BL-announcement.md | お知らせサービス設計 | 公開期間管理・一覧取得 | AnnouncementService |
| 03-04-01 | 03_detailed-design/ | 04_data-access/ | - | 01_mapper-list.md | MyBatisマッパー一覧 | 全マッパーのメソッド一覧 | 全Repository |
| 03-04-02-01 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 01_DAO-ProductMapper.md | 商品マッパーSQL定義 | 商品関連SQL詳細 | ProductMapper.xml |
| 03-04-02-02 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 02_DAO-MemberMapper.md | 会員マッパーSQL定義 | 会員関連SQL詳細 | MemberMapper.xml |
| 03-04-02-03 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 03_DAO-OrderMapper.md | 注文マッパーSQL定義 | 注文関連SQL詳細 | OrderMapper.xml |
| 03-04-02-04 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 04_DAO-CartMapper.md | カートマッパーSQL定義 | カート関連SQL詳細 | CartMapper.xml |
| 03-04-02-05 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 05_DAO-ContactMapper.md | 問い合わせマッパーSQL定義 | 問い合わせ関連SQL詳細 | ContactMapper.xml |
| 03-04-02-06 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 06_DAO-AnnouncementMapper.md | お知らせマッパーSQL定義 | お知らせ関連SQL詳細 | AnnouncementMapper.xml |
| 03-04-02-07 | 03_detailed-design/ | 04_data-access/ | 02_sql/ | 07_DAO-BatchMapper.md | バッチマッパーSQL定義 | バッチ関連SQL詳細 | BatchMapper.xml |
| 03-05-01 | 03_detailed-design/ | 05_batch-design/ | - | 01_batch-list.md | バッチ処理一覧 | 全バッチジョブの概要・スケジュール | BatchJobConfiguration |
| 03-05-02 | 03_detailed-design/ | 05_batch-design/ | - | 02_BAT-ranking.md | 人気ランキング計算 | 月間売上集計・トップ10更新ロジック | BatchScheduler |
| 03-05-03 | 03_detailed-design/ | 05_batch-design/ | - | 03_BAT-recommend.md | レコメンド計算 | 類似商品スコア算出・上位4件更新 | BatchScheduler |
| 03-05-04 | 03_detailed-design/ | 05_batch-design/ | - | 04_BAT-tax.md | 税率更新 | 有効期間に基づく税率切替処理 | BatchScheduler |
| 03-06-01 | 03_detailed-design/ | 06_mail-design/ | - | 01_mail-list.md | メール一覧 | 全メールテンプレートの概要 | NotificationMailService |
| 03-06-02 | 03_detailed-design/ | 06_mail-design/ | - | 02_MAIL-registration.md | 会員登録通知メール | 登録完了メールのテンプレート・送信条件 | member-registration-body.txt |
| 03-06-03 | 03_detailed-design/ | 06_mail-design/ | - | 03_MAIL-order.md | 注文完了通知メール | 注文完了メールのテンプレート・送信条件 | order-complete-body.txt |
| 03-07-01 | 03_detailed-design/ | 07_security-design/ | - | 01_security.md | セキュリティ設計書 | 認証・認可・セッション・CSRF・暗号化 | SecurityConfig |
| 03-08-01 | 03_detailed-design/ | 08_error-handling/ | - | 01_error-handling.md | エラーハンドリング設計書 | 例外処理・エラー画面・ログ出力方針 | AppErrorController |
| 04-01 | 04_non-functional/ | - | - | 01_logging.md | ログ設計書 | MDC・リクエストトラッキング・マスキング | logback-spring.xml |
| 04-02 | 04_non-functional/ | - | - | 02_environment.md | 環境定義書 | 開発/ステージング/本番の構成差分 | application.yml |
| 04-03 | 04_non-functional/ | - | - | 03_deployment.md | デプロイ手順書 | Docker Compose構築・DB初期化手順 | docker-compose.yml, scripts/ |
| 05-01 | 05_testing/ | - | - | 01_test-strategy.md | テスト方針書 | テストレベル・方針・カバレッジ基準 | テスト全般 |
| 05-02 | 05_testing/ | 02_test-cases/ | - | - | テストケース | 機能別テストケース定義 | テスト全般 |
| 05-03 | 05_testing/ | - | - | 03_test-data.md | テストデータ定義 | テスト用データの定義・投入手順 | sql/seed/test-data/ |
