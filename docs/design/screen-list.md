# 画面一覧

本ドキュメントは、現行の Web アプリケーションで提供されている主要画面を、URL と主な機能を中心に整理したものです。

| 画面名 | 主な機能 | URL |
| --- | --- | --- |
| トップ画面 | 新着商品・ランキング商品の表示、各カテゴリへの導線 | `/` |
| お知らせ一覧 | お知らせの一覧表示 | `/announcements` |
| ログイン | 会員ログイン、セッション切れ時の案内、戻り先指定 | `/login` |
| 会員登録入力 | 会員登録フォーム入力 | `/members/register` |
| 会員登録確認 | 入力内容の確認 | `/members/register/confirm` |
| 会員登録完了 | 登録完了メッセージ表示 | `/members/register/complete` |
| 新着商品一覧 | 新着商品の一覧表示、在庫絞り込み、価格帯絞り込み、並び替え | `/products/new-arrivals` |
| 商品検索結果 | キーワード検索結果の一覧表示、絞り込み、並び替え | `/products/search` |
| デスクカテゴリ一覧 | デスク商品一覧表示、属性絞り込み | `/categories/desks` |
| チェアカテゴリ一覧 | チェア商品一覧表示、属性絞り込み | `/categories/chairs` |
| 収納家具カテゴリ一覧 | 収納家具商品一覧表示、属性絞り込み | `/categories/storages` |
| 商品詳細 | 商品情報・在庫情報・お気に入り操作 | `/products/{productId}` |
| カート | 商品の確認・数量変更・削除 | `/cart` |
| 購入方法選択 | 購入方法の選択、ログイン補助 | `/checkout/method` |
| 注文情報入力 | 配送先・支払方法などの入力 | `/checkout/input` |
| 注文確認 | 注文内容の最終確認 | `/checkout/confirm` |
| 注文完了 | 注文完了メッセージ表示 | `/checkout/complete/{orderNumber}` |
| マイページトップ | 購入履歴一覧への誘導 | `/mypage` |
| 購入履歴一覧 | 会員の注文履歴一覧 | `/mypage/orders` |
| 購入履歴詳細 | 注文内容の詳細表示、再購入 | `/mypage/orders/{orderNumber}` |
| お気に入り一覧 | お気に入り登録商品一覧 | `/mypage/favorites` |
| プロフィール編集 | 会員情報の変更 | `/mypage/profile` |
| 配送先一覧 | 追加お届け先一覧表示 | `/mypage/addresses` |
| 配送先登録・編集 | 追加お届け先の新規登録・編集 | `/mypage/addresses/new`, `/mypage/addresses/{memberAddressId}/edit` |
| 退会確認 | 会員退会確認 | `/mypage/withdraw` |
| お問い合わせ | 問い合わせフォーム表示・送信 | `/contact` |
| 会社案内 | 会社概要・ブランド案内 | `/about` |
| ご利用案内 | ご利用方法・購入ガイド | `/guide` |
| 利用規約 | 利用規約表示 | `/legal/terms` |
| プライバシーポリシー | プライバシーポリシー表示 | `/legal/privacy` |
| 特定商取引法に基づく表記 | 特商法に基づく表示 | `/legal/tokusho` |
| エラー画面 | システムエラー時の表示 | `/error` |
