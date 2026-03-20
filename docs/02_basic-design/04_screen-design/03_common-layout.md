# 共通レイアウト

## 1. ページ全体構成

```
┌─────────────────────────────────────────────────────────┐
│ <head>                                                   │
│   メタ情報・CSS・タイトル                                   │
│   (fragments/layout/head.html)                          │
├─────────────────────────────────────────────────────────┤
│ ヘッダー (fragments/layout/header.html)                  │
│   ロゴ / グローバルナビ / 検索バー / カートアイコン / ログイン │
├─────────────────────────────────────────────────────────┤
│ お知らせバー (fragments/layout/notice-bar.html)          │
│   最新お知らせの表示（存在する場合のみ）                      │
├─────────────────────────────────────────────────────────┤
│ メインコンテンツ (各ページ固有)                              │
│   └── パンくずリスト (fragments/common/breadcrumb.html)  │
│   └── ページコンテンツ                                    │
├─────────────────────────────────────────────────────────┤
│ フッター (fragments/layout/footer.html)                  │
│   ナビリンク / 特商法リンク / コピーライト                   │
├─────────────────────────────────────────────────────────┤
│ <scripts> (fragments/layout/scripts.html)               │
│   JavaScript（ページ末尾）                                │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 各フラグメント詳細

### 2.1 head.html

| 要素 | 内容 |
|---|---|
| `<title>` | ページ名 + ` - OFFICE ORDER` |
| CSS | `static/css/base.css`, `components.css`, `layout.css` |
| ページ固有CSS | `static/css/pages/*.css` |
| charset | UTF-8 |
| viewport | `width=device-width, initial-scale=1` |

### 2.2 header.html

| 要素 | 内容 |
|---|---|
| ロゴ | サイトトップ(`/`)へのリンク |
| グローバルナビ | デスク / チェア / 収納 / 新着 / お知らせ |
| 検索バー | キーワード検索フォーム（`/products/search` へGET） |
| カートアイコン | カート内件数バッジ付きアイコン（`/cart`へリンク） |
| 認証リンク | 未ログイン時: ログイン・会員登録 / ログイン時: マイページ・ログアウト |

### 2.3 notice-bar.html

| 要素 | 内容 |
|---|---|
| 表示条件 | 公開中のお知らせが1件以上存在する場合 |
| 内容 | 最新お知らせのタイトルと `announcements` ページへのリンク |

### 2.4 breadcrumb.html

| 要素 | 内容 |
|---|---|
| 構造 | `ホーム > [中間カテゴリ >] 現在ページ` |
| ホームリンク | `/` へのリンク |
| 現在ページ | リンクなし（テキストのみ） |
| 適用画面 | 商品一覧・詳細・チェックアウト・マイページ等 |

### 2.5 product-card.html

| 要素 | 内容 |
|---|---|
| 用途 | 商品カード（一覧表示・ランキング・お気に入り等）の共通テンプレート |
| 表示内容 | 商品画像・商品名・価格・カラーバリエーション数 |

### 2.6 footer.html

| 要素 | 内容 |
|---|---|
| ナビリンク | 各カテゴリ・お問い合わせ・会社概要・利用ガイド |
| 法的リンク | 利用規約・プライバシーポリシー・特定商取引法に基づく表記 |
| コピーライト | `© OFFICE ORDER` |

### 2.7 scripts.html

| 要素 | 内容 |
|---|---|
| JS | `static/js/site.js` |
| JS モジュール | `static/js/modules/*.js` |
| 読み込みタイミング | `</body>` 直前 |

---

## 3. Thymeleafレイアウト設計

各ページはフラグメントを以下のように参照します：

```html
<!-- head.html の呼び出し例 -->
<th:block th:replace="~{fragments/layout/head :: head(title='商品一覧')}"></th:block>

<!-- header.html の呼び出し例 -->
<th:block th:replace="~{fragments/layout/header :: header}"></th:block>

<!-- notice-bar.html の呼び出し例 -->
<th:block th:replace="~{fragments/layout/notice-bar :: noticeBar}"></th:block>

<!-- breadcrumb.html の呼び出し例 -->
<th:block th:replace="~{fragments/common/breadcrumb :: breadcrumb(items=${breadcrumbItems})}"></th:block>

<!-- scripts.html の呼び出し例 -->
<th:block th:replace="~{fragments/layout/scripts :: scripts}"></th:block>
```
