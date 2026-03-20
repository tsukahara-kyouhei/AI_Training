# サイト情報

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-ABT |
| Controller | `ContentController` |
| 認証 | 全ページ不要 |

以下5ページはすべて静的コンテンツ。Model変数なし、DBアクセスなし。

---

## 2. OFFICE ORDER について

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/about.html` |
| URL | `GET /about` |
| 内容 | サービス紹介・会社情報 |

---

## 3. ご利用ガイド

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/guide.html` |
| URL | `GET /guide` |
| 内容 | よくある質問（FAQ） |

ページ内アンカーリンク: `#faq-product`、`#faq-delivery-date`、`#faq-order`、`#faq-shipping`、`#faq-return`、`#faq-other`

---

## 4. 利用規約

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/legal-terms.html` |
| URL | `GET /legal/terms` |
| 内容 | 利用規約全文 |

---

## 5. プライバシーポリシー

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/legal-privacy.html` |
| URL | `GET /legal/privacy` |
| 内容 | 個人情報保護方針 |

---

## 6. 特定商取引法に基づく表記

| 項目 | 内容 |
|---|---|
| テンプレート | `pages/legal-tokusho.html` |
| URL | `GET /legal/tokusho` |
| 内容 | 特定商取引法に基づく表記 |
