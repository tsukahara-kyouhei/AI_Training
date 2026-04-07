# 機能一覧

## 1. 公開機能（ゲスト・会員共通）

### 1.1 トップページ

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-TOP-001 | トップページ表示 | 新着商品・売れ筋ランキング・お知らせを表示 | HomeController |

### 1.2 商品カタログ

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-CAT-001 | 新着商品一覧 | 新しく追加された商品の一覧表示 | CatalogController |
| F-CAT-002 | キーワード検索 | 商品名・商品コード・バリエーション名・説明文でキーワード検索。全角半角・大文字小文字を区別しない。テイスト絞込み対応 | CatalogController |
| F-CAT-003 | デスク一覧 | デスクカテゴリの商品一覧。フィルタ・ソート対応 | CatalogController |
| F-CAT-004 | チェア一覧 | チェアカテゴリの商品一覧。フィルタ・ソート対応 | CatalogController |
| F-CAT-005 | 収納家具一覧 | 収納カテゴリの商品一覧。フィルタ・ソート対応 | CatalogController |
| F-CAT-006 | 商品詳細表示 | 商品情報・バリアント・関連商品・組立オプション表示 | CatalogController |
| F-CAT-007 | 最近見た商品 | セッションに記録した閲覧履歴表示 | CatalogController |

### 1.3 カート

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-CRT-001 | カート表示 | カート内容の一覧表示。ゲスト:Cookie / 会員:DB | CartController |
| F-CRT-002 | 商品追加 | 商品詳細からバリアントをカートに追加 | CartController |
| F-CRT-003 | 数量変更 | カート内商品の数量を変更（1〜99） | CartController |
| F-CRT-004 | 商品削除 | カートから特定商品を削除 | CartController |
| F-CRT-005 | カートクリア | カート内の全商品を削除 | CartController |

### 1.4 チェックアウト（購入手続き）

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-CO-001 | 支払方法選択 | 銀行振込・代金引換・コンビニ決済から選択 | CartController |
| F-CO-002 | 注文情報入力 | 配送先・連絡先・組立オプション入力 | CartController |
| F-CO-003 | 注文内容確認 | 入力内容の最終確認（ワンタイムトークン発行） | CartController |
| F-CO-004 | 注文確定 | 在庫確認・注文登録・カートクリア・メール送信 | CartController |
| F-CO-005 | 注文完了表示 | 注文番号と完了メッセージを表示 | CartController |

### 1.5 お問い合わせ

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-CNT-001 | お問い合わせフォーム表示 | 会員ログイン時は氏名・メールを自動入力 | ContactController |
| F-CNT-002 | お問い合わせ送信 | 入力内容をDBに保存して受付完了 | ContactController |

### 1.6 静的コンテンツ

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-CON-001 | 会社概要 | Aboutページの表示 | ContentController |
| F-CON-002 | ご利用ガイド | 購入手順・サービス説明の表示 | ContentController |
| F-CON-003 | 利用規約 | 利用規約の表示 | ContentController |
| F-CON-004 | プライバシーポリシー | プライバシーポリシーの表示 | ContentController |
| F-CON-005 | 特定商取引法に基づく表記 | 法的表記ページの表示 | ContentController |

### 1.7 お知らせ

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-ANN-001 | お知らせ一覧 | 公開期間内のお知らせ一覧表示 | HomeController |

---

## 2. 認証機能

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-AUTH-001 | ログイン画面表示 | メールアドレス・パスワード入力フォーム（メアドCookie読込） | AuthController |
| F-AUTH-002 | ログイン | BCryptで検証・セッション生成・リダイレクト | AuthController / Spring Security |
| F-AUTH-003 | ログアウト | セッション破棄・トップページへリダイレクト | Spring Security |

---

## 3. 会員登録

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-REG-001 | 会員登録フォーム表示 | 個人/法人・氏名・住所・メール等の入力フォーム | MemberRegistrationController |
| F-REG-002 | 入力確認 | 入力内容の確認画面へ遷移（メール重複チェック） | MemberRegistrationController |
| F-REG-003 | 会員登録確定 | パスワードハッシュ化・DB登録・自動ログイン | MemberRegistrationController |
| F-REG-004 | 登録完了表示 | 登録完了とメール送信の案内を表示 | MemberRegistrationController |

---

## 4. 会員専用機能（マイページ）

### 4.1 注文管理

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-MY-001 | 注文履歴一覧 | ページネーション対応の注文一覧表示 | MyPageController |
| F-MY-002 | 注文詳細表示 | 注文明細・ステータス履歴の詳細表示 | MyPageController |
| F-MY-003 | 再購入 | 過去注文の商品をカートに再追加 | MyPageController |

### 4.2 お気に入り

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-MY-004 | お気に入り一覧 | ページネーション対応のお気に入り商品一覧 | MyPageController |
| F-MY-005 | お気に入り追加 | 商品詳細からお気に入りに追加（最大100件） | CatalogController |
| F-MY-006 | お気に入り削除 | お気に入りから商品を削除 | CatalogController |

### 4.3 プロフィール・住所管理

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-MY-007 | プロフィール編集 | 氏名・住所・連絡先等の会員情報更新 | MyPageController |
| F-MY-008 | 追加お届け先一覧 | 登録済み住所の一覧表示 | MyPageController |
| F-MY-009 | 追加お届け先追加 | 新規お届け先の登録（最大20件） | MyPageController |
| F-MY-010 | 追加お届け先編集 | 既存お届け先の更新 | MyPageController |
| F-MY-011 | 追加お届け先削除 | 既存お届け先の削除 | MyPageController |

### 4.4 退会

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-MY-012 | 退会確認・処理 | 退会確認画面表示と退会処理（論理削除） | MyPageController |

---

## 5. バッチ処理（内部）

| 機能ID | 機能名 | 概要 | 実行タイミング |
|---|---|---|---|
| F-BAT-001 | 売れ筋ランキング計算 | 過去1か月の販売実績からトップ10を集計・更新 | 毎時0分 / 手動 |
| F-BAT-002 | おすすめ関連商品計算 | 商品間の類似スコアを算出・上位4件を更新 | 毎時0分 / 手動 |

---

## 6. メール通知

| 機能ID | 機能名 | 概要 | 送信トリガー |
|---|---|---|---|
| F-MAIL-001 | 会員登録完了メール | 登録完了を通知するメール送信 | 会員登録確定時 |
| F-MAIL-002 | 注文完了メール | 注文内容を通知するメール送信 | 注文確定時 |

---

## 7. 内部API（localプロファイル限定）

| 機能ID | 機能名 | 概要 | コントローラー |
|---|---|---|---|
| F-INT-001 | バッチジョブ一覧取得 | 実行可能なジョブの一覧を返すREST API | InternalBatchController |
| F-INT-002 | バッチ実行履歴取得 | ジョブの実行履歴を返すREST API | InternalBatchController |
| F-INT-003 | バッチ手動起動 | ジョブを手動で起動するREST API | InternalBatchController |
| F-INT-004 | バッチ停止 | 実行中ジョブを停止するREST API | InternalBatchController |
