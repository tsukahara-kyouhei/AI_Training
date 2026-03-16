# OFFICE ORDER — 新規参加者向けオンボーディング資料

| 項目 | 内容 |
|------|------|
| 作成日 | 2026-03-16 |
| 対象者 | プロジェクト新規参加者 |

---

## 目次

1. [プロジェクト概要](#1-プロジェクト概要)
2. [業務要件（どんなシステムか）](#2-業務要件どんなシステムか)
3. [システム構成（どう動いているか）](#3-システム構成どう動いているか)
4. [主要機能一覧](#4-主要機能一覧)
5. [ディレクトリ構成](#5-ディレクトリ構成)
6. [ローカル開発環境のセットアップ](#6-ローカル開発環境のセットアップ)
7. [現在進行中の開発（FEAT-001）](#7-現在進行中の開発feat-001)
8. [参照ドキュメント一覧](#8-参照ドキュメント一覧)

---

## 1. プロジェクト概要

**OFFICE ORDER** は、オフィス家具（デスク・チェア・収納）を取り扱う **顧客向けECサイト** です。

| 項目 | 内容 |
|------|------|
| サービス名 | OFFICE ORDER |
| 扱う商品 | オフィス家具（デスク・チェア・収納） |
| 対象ユーザー | 一般顧客（会員登録制） |
| 本リポジトリの範囲 | 商品閲覧・会員登録・カート・注文・お問い合わせ |
| 対象外 | 受注後の業務処理・管理画面（別システムの責務） |

---

## 2. 業務要件（どんなシステムか）

### 2.1 対象ユーザーとアクセス区分

| 区分 | 説明 |
|------|------|
| 一般訪問者（未ログイン） | 商品閲覧・検索のみ可能 |
| 会員（ログイン済み） | カート・注文・マイページ・お問い合わせが可能 |

### 2.2 主要な業務フロー

```
[商品を探す]
  │
  ├─ カテゴリ一覧（デスク / チェア / 収納）
  │    └─ 絞込条件：テイスト・カラー・価格帯・在庫有のみ
  │
  └─ キーワード検索
       └─ 絞込条件：テイスト・カラー・価格帯・在庫有のみ

[購入する]
  会員登録 or ログイン
    └─ カートに追加
         └─ 注文情報入力（住所・支払方法）
              └─ 注文確認
                   └─ 注文確定
                        └─ 注文完了メール送信

[アカウント管理]
  マイページ
    ├─ 注文履歴閲覧
    ├─ お届け先住所管理
    ├─ プロフィール編集
    ├─ お気に入り管理
    └─ 退会
```

### 2.3 商品データ構造の概要

商品は **カテゴリ（デスク・チェア・収納）** ごとに異なる属性を持ちます。

| カテゴリ | 独自属性（絞込条件） |
|---------|----------------|
| デスク | 天板形状、テイスト |
| チェア | 機能、素材、テイスト |
| 収納 | 用途、テイスト |
| 共通 | カラー、価格帯、在庫有無 |

商品には **バリエーション（サイズ・カラー違い等）** の概念があり、1商品に複数のバリエーションが紐づきます。  
在庫・価格はバリエーション単位で管理されます。

### 2.4 業務ルール（重要な制約）

| 区分 | ルール |
|------|-------|
| 在庫 | 在庫数 0 の商品はカートに追加不可 |
| 注文 | ログイン済み会員のみ注文可能 |
| 注文後 | 注文確定後はキャンセル不可（管理画面側の業務） |
| 会員 | メールアドレスで一意、退会後は再登録不可 |
| パスワード | BCrypt でハッシュ化して保存 |

---

## 3. システム構成（どう動いているか）

### 3.1 技術スタック

| レイヤー | 技術 | バージョン |
|--------|------|---------|
| 言語 | Java | 21 |
| フレームワーク | Spring Boot | 3.5.11 |
| 認証・認可 | Spring Security | — |
| テンプレートエンジン | Thymeleaf | — |
| O/Rマッパー | MyBatis | 3.0.4 |
| バッチ | Spring Batch | — |
| DB | PostgreSQL | 16 |
| メール | Spring Mail + Mailpit（ローカル） | — |
| コンテナ | Docker Compose | ローカル開発補助用 |

### 3.2 アプリケーションの多層アーキテクチャ

```
[ブラウザ]
   │  HTTP リクエスト
   ▼
[Controller 層]       web/ パッケージ
   │  リクエストの受取・バリデーション・レスポンスの組み立て
   ▼
[Service 層]          service/ パッケージ
   │  ビジネスロジック・トランザクション管理
   ▼
[Repository 層]       repository/ パッケージ
   │  DB アクセスの抽象化
   ▼
[Mapper 層]           mapper/ パッケージ（MyBatis）
   │  SQL 実行（XML ファイルと対になる）
   ▼
[DB]                  PostgreSQL
```

### 3.3 Javaパッケージ構成

```
jp.co.skig.officeorder
├── OfficeOrderApplication.java   # エントリポイント
├── common/                       # 共通処理（例外クラス等）
├── config/                       # Spring 設定クラス（Security・MyBatis 等）
├── logging/                      # ログフィルター等
├── mapper/                       # MyBatis Mapper インターフェース
├── model/                        # データモデル（Record クラス）
│   ├── product/                  # 商品関連モデル
│   ├── order/                    # 注文関連モデル
│   ├── member/                   # 会員関連モデル
│   └── ...
├── repository/                   # DB アクセス層
├── service/                      # ビジネスロジック層
├── util/                         # ユーティリティクラス
└── web/                          # Controller・View モデル
    ├── CatalogController.java    # 商品一覧・検索
    ├── CartController.java       # カート
    ├── AuthController.java       # ログイン・ログアウト
    ├── MemberRegistrationController.java  # 会員登録
    ├── MyPageController.java     # マイページ
    ├── ContactController.java    # お問い合わせ
    └── ...
```

### 3.4 主要ミドルウェアの役割

| コンポーネント | 用途 | ローカルURL |
|-------------|------|-----------|
| PostgreSQL | メインDB | `localhost:5432` |
| Mailpit | メール送信確認（開発用） | `http://localhost:8025` |
| DBGate | DB GUIクライアント（開発用） | `http://localhost:3000` |

---

## 4. 主要機能一覧

### 4.1 顧客向け機能

| 機能カテゴリ | 機能 | URL パターン | ログイン要否 |
|-----------|------|------------|-----------|
| 商品閲覧 | トップページ | `/` | 不要 |
| 商品閲覧 | カテゴリ一覧（デスク） | `/products/desks` | 不要 |
| 商品閲覧 | カテゴリ一覧（チェア） | `/products/chairs` | 不要 |
| 商品閲覧 | カテゴリ一覧（収納） | `/products/storages` | 不要 |
| 商品閲覧 | 新着一覧 | `/products/new-arrivals` | 不要 |
| 商品閲覧 | キーワード検索 | `/products/search` | 不要 |
| 商品閲覧 | 商品詳細 | `/products/{productId}` | 不要 |
| カート | カート確認 | `/cart` | 必要 |
| 注文 | 注文フロー | `/checkout/*` | 必要 |
| 会員 | 会員登録 | `/member/register/*` | 不要 |
| 会員 | ログイン／ログアウト | `/auth/*` | — |
| 会員 | マイページ（注文履歴等） | `/mypage/*` | 必要 |
| お問い合わせ | お問い合わせフォーム | `/contact` | 必要 |
| コンテンツ | 特集・お知らせ等 | `/content/*`, `/announcements` | 不要 |

### 4.2 バッチ機能

定期バッチとして以下が実装されています（Spring Batch）。

| バッチ | 概要 |
|-------|------|
| 注文確定バッチ | 注文をバックエンドシステムへ連携する処理 |

---

## 5. ディレクトリ構成

```
office-order/
├── src/main/java/          # アプリケーションソースコード（Java）
├── src/main/resources/
│   ├── application.yml     # アプリ設定
│   ├── logback-spring.xml  # ログ設定
│   ├── messages.properties # バリデーションメッセージ等
│   ├── mappers/            # MyBatis XML マッパー
│   ├── static/             # CSS・JS・画像（静的ファイル）
│   ├── templates/          # Thymeleaf テンプレート（HTML）
│   │   ├── fragments/      # 共通パーツ（ヘッダー・フッター等）
│   │   └── pages/          # 各ページのテンプレート
│   └── mail/               # メールテンプレート
├── src/test/               # テストコード
├── sql/
│   ├── schema/             # テーブル定義・関数定義 SQL
│   ├── seed/               # 初期データ投入 SQL
│   └── init/               # DB 初期化スクリプト（init.sql）
├── scripts/
│   └── init-local-postgres.ps1  # ローカル DB 初期化スクリプト
├── docs/
│   ├── design/             # 設計書
│   └── issues/             # 要件・課題管理
├── docker-compose.yml      # ローカル開発用コンテナ定義
├── pom.xml                 # Maven 依存関係定義
└── README.md               # クイックスタートガイド
```

---

## 6. ローカル開発環境のセットアップ

### 6.1 前提条件

- Java 21 がインストールされていること
- `./mvnw`（Maven Wrapper）が実行できること
- 以下のいずれか：
  - Docker Desktop（推奨）
  - PostgreSQL 16 + `psql` がローカルにインストールされていること

### 6.2 起動手順（Docker 利用・推奨）

```powershell
# 1. DBなどのインフラを起動
docker compose up -d

# 2. アプリを起動（ローカルプロファイル）
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

起動後は以下にアクセスして確認してください。

| 確認先 | URL |
|-------|-----|
| アプリトップ | http://localhost:8080/ |
| DB GUI（DBGate） | http://localhost:3000 |
| メール確認（Mailpit） | http://localhost:8025 |

### 6.3 ログイン確認用アカウント

初期データとして以下のテスト会員が登録されています。パスワードはすべて `password` です。

| メールアドレス |
|-------------|
| member01@example.com |
| member02@example.com |
| member04@example.com |

### 6.4 Docker を使わない場合

```powershell
# 1. PostgreSQL 16 にDBとユーザーを作成（psql で実行）
# CREATE ROLE office_order LOGIN PASSWORD 'office_order';
# CREATE DATABASE office_order OWNER office_order;

# 2. スキーマと初期データを投入
./scripts/init-local-postgres.ps1

# 3. アプリを起動
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 6.5 Dockerフル初期化（DB をゼロから作り直す場合）

```powershell
# コンテナ・ボリューム・イメージをすべて削除
docker compose down --rmi all --volumes --remove-orphans

# Compose を再起動
docker compose up -d
```

---

## 7. 現在進行中の開発（FEAT-001）

### 7.1 概要

**商品検索機能の強化** を目的とした開発が進行中です（ステータス：実装完了・動作確認待ち）。

### 7.2 変更のポイント（4つ）

| # | 変更内容 | 詳細 |
|---|---------|-----|
| 1 | 商品コード検索方式の変更 | 入力桁数で完全一致／前方一致を自動判定（9文字以上 → 完全一致、9文字未満 → 前方一致） |
| 2 | キーワード検索対象の拡張 | `商品名` に加えて `シリーズバリエーション名`（variation_name）と `説明文`（description）を検索対象に追加 |
| 3 | 全角・半角／大文字・小文字の正規化 | 数字・英字・カタカナの全角半角を区別せず検索できるよう、Javaアプリ側で正規化処理を実装 |
| 4 | 検索絞込条件に「テイスト」を追加 | 検索結果画面にテイスト（ベーシック・カジュアル・シンプル・モダン・ナチュラル）の絞込チェックボックスを追加 |

### 7.3 影響を受けるコンポーネント

```
[フロントエンド]
  product-list-search-results.html    テイスト絞込UIの追加

[Controller]
  CatalogController                   テイストパラメータの受取・Modelへのバインド

[Service]
  ProductListSearchService            キーワード正規化・テイストパラメータのセット
  ProductFilterOptionService          テイスト選択肢（重複排除）の生成

[Repository]
  ProductRepository                   テイスト絞込条件の組み込み
  ProductFilterOptionRepository       全カテゴリのテイスト名一括取得

[Mapper]
  ProductMapper.xml                   検索SQL変更・テイスト絞込フラグメント追加

[Model（Record）]
  ProductSearchCondition              tasteNames フィールドを追加
  ProductFilterOptionsBundle          searchTasteOptions フィールドを追加

[新規クラス]
  SearchKeywordNormalizer (util/)     全角→半角・大文字→小文字の正規化ユーティリティ

[DB（PostgreSQL関数）]
  normalize_search_text()             DB列側の正規化関数（search-functions.sql）
```

### 7.4 正規化の仕様まとめ

| 対象 | 変換内容 | 例 |
|-----|---------|-----|
| 全角数字 | 半角数字へ | `１２３` → `123` |
| 全角英字（大文字・小文字） | 半角英小文字へ | `ＡＢＣ` → `abc` |
| 半角英大文字 | 半角英小文字へ | `ABC` → `abc` |
| 全角カタカナ | 半角カタカナへ | `ナチュラル` → `ﾅﾁｭﾗﾙ` |
| ひらがな | 変換しない | `なちゅらる` → そのまま |

正規化はJavaアプリ（`SearchKeywordNormalizer`）とDB関数（`normalize_search_text`）の **両方** で行います。  
DB列の値も同じ関数で変換した上で比較するため、DB側のデータを変更する必要はありません。

### 7.5 データベーススキーマ変更の有無

| 対象 | 変更有無 | 内容 |
|-----|---------|------|
| テーブル定義 | **なし** | DBスキーマの変更は不要 |
| カラム追加 | **なし** | — |
| PostgreSQL関数 | **あり** | `normalize_search_text(text)` を新規追加（`sql/schema/search-functions.sql`） |

---

## 8. 参照ドキュメント一覧

| ドキュメント | 内容 | パス |
|-----------|-----|-----|
| README | クイックスタートガイド | `README.md` |
| 要件定義書（FEAT-001） | 商品検索強化の要件 | `docs/issues/FEAT-001-requirements.md` |
| 基本設計書（FEAT-001） | アーキテクチャ・処理フロー | `docs/design/FEAT-001-basic-design.md` |
| 詳細設計書（FEAT-001） | クラス・メソッド・SQL設計 | `docs/design/FEAT-001-detail-design.md` |
| 画面設計書（FEAT-001） | UI仕様・テンプレート変更箇所 | `docs/design/FEAT-001-screen-design.md` |
| 実装TODO（FEAT-001） | 実装タスク一覧・進捗 | `docs/issues/FEAT-001-TODO.md` |
| 確認事項一覧（FEAT-001） | 要件に関するQ&A | `docs/issues/FEAT-001-kadaiList.md` |
| 障害一覧（FEAT-001） | 発生した不具合と対応内容 | `docs/issues/FEAT-001-troubleList.md` |
