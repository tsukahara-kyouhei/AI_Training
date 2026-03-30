# 画面遷移図

本ドキュメントは OFFICE ORDER の主要な画面遷移を Mermaid 記法で表したものです。

---

## 1. 全体フロー概観

```mermaid
flowchart TD
    TOP["トップ\n/"]
    ANM["お知らせ一覧\n/announcements"]
    CAT_D["デスク一覧\n/categories/desks"]
    CAT_C["チェア一覧\n/categories/chairs"]
    CAT_S["収納家具一覧\n/categories/storages"]
    SRCH["検索結果\n/products/search"]
    NEW["新着商品\n/products/new-arrivals"]
    PRD["商品詳細\n/products/{id}"]
    CART["カート\n/cart"]
    LOGIN["ログイン\n/login"]
    REG["会員登録\n/members/register"]
    MYP["マイページ\n/mypage/orders"]

    TOP --> ANM
    TOP --> CAT_D
    TOP --> CAT_C
    TOP --> CAT_S
    TOP --> NEW
    TOP --> SRCH
    CAT_D --> PRD
    CAT_C --> PRD
    CAT_S --> PRD
    SRCH --> PRD
    NEW --> PRD
    PRD --> CART
    CART --> LOGIN
    CART --> REG
    LOGIN --> MYP
    REG --> MYP
```

---

## 2. 商品カタログ・検索フロー

```mermaid
flowchart TD
    TOP["トップ\n/"]
    CAT_D["デスク一覧\n/categories/desks"]
    CAT_C["チェア一覧\n/categories/chairs"]
    CAT_S["収納家具一覧\n/categories/storages"]
    NEW["新着商品一覧\n/products/new-arrivals"]
    SRCH["キーワード検索結果\n/products/search?q=..."]
    PRD["商品詳細\n/products/{productId}"]
    CART["カート\n/cart"]

    TOP --"カテゴリ「デスク」選択"--> CAT_D
    TOP --"カテゴリ「チェア」選択"--> CAT_C
    TOP --"カテゴリ「収納家具」選択"--> CAT_S
    TOP --"新着商品をもっと見る"--> NEW
    TOP --"キーワード検索"--> SRCH
    CAT_D --"絞り込み・ページ遷移"--> CAT_D
    CAT_C --"絞り込み・ページ遷移"--> CAT_C
    CAT_S --"絞り込み・ページ遷移"--> CAT_S
    NEW --"絞り込み・ページ遷移"--> NEW
    SRCH --"絞り込み・ページ遷移"--> SRCH
    CAT_D --"商品選択"--> PRD
    CAT_C --"商品選択"--> PRD
    CAT_S --"商品選択"--> PRD
    NEW --"商品選択"--> PRD
    SRCH --"商品選択"--> PRD
    PRD --"カートに追加"--> CART
    PRD --"カラー/サイズ変更"--> PRD
```

---

## 3. 購入フロー

```mermaid
flowchart TD
    CART["カート\n/cart"]
    METHOD["購入方法選択\n/checkout/method"]
    LOGIN["ログイン\n/login"]
    REG["会員登録（入力）\n/members/register"]
    INPUT["注文情報入力\n/checkout/input"]
    CONFIRM["注文確認\n/checkout/confirm"]
    COMPLETE["注文完了\n/checkout/complete/{orderNumber}"]
    MYP_ORD["購入履歴一覧\n/mypage/orders"]

    CART --"購入手続きへ"--> METHOD
    METHOD --"ログインして購入"--> LOGIN
    METHOD --"会員登録して購入"--> REG
    METHOD --"ゲストとして購入"--> INPUT
    LOGIN --"ログイン成功"--> INPUT
    REG --"登録完了後"--> INPUT
    INPUT --"注文情報入力完了（POST）"--> CONFIRM
    CONFIRM --"修正する"--> INPUT
    CONFIRM --"注文を確定する（POST）"--> COMPLETE
    COMPLETE --"購入履歴を見る（ログイン済）"--> MYP_ORD
    COMPLETE --"トップへ戻る"--> TOP["トップ\n/"]
```

---

## 4. 会員登録フロー

```mermaid
flowchart TD
    TOP["トップ\n/"]
    LOGIN["ログイン\n/login"]
    REG_INPUT["会員登録（入力）\n/members/register"]
    REG_CONFIRM["会員登録（確認）\n※POST /members/register/confirm"]
    REG_COMPLETE["会員登録完了\n/members/register/complete"]
    MYP["マイページ\n/mypage/orders"]

    TOP --"会員登録ボタン"--> REG_INPUT
    LOGIN --"新規会員登録リンク"--> REG_INPUT
    REG_INPUT --"入力完了（POST）"--> REG_CONFIRM
    REG_CONFIRM --"修正する"--> REG_INPUT
    REG_CONFIRM --"登録する（POST）"--> REG_COMPLETE
    REG_COMPLETE --"マイページへ（自動ログイン）"--> MYP
```

---

## 5. 認証フロー

```mermaid
flowchart TD
    ANY["任意の画面（要ログイン）"]
    LOGIN["ログイン\n/login"]
    TOP["トップ\n/"]
    BACK["ログイン前の画面\n（リダイレクトバック）"]
    MYP["マイページ\n/mypage/orders"]

    ANY --"未ログインでアクセス"--> LOGIN
    LOGIN --"ログイン成功（通常遷移）"--> MYP
    LOGIN --"ログイン成功（リダイレクトバック）"--> BACK
    LOGIN --"新規会員登録リンク"--> REG["会員登録\n/members/register"]
    TOP --"ログインリンク"--> LOGIN
```

---

## 6. マイページフロー

```mermaid
flowchart TD
    LOGIN["ログイン\n/login"]
    MYP["マイページ\n/mypage\n（購入履歴へリダイレクト）"]
    ORDERS["購入履歴一覧\n/mypage/orders"]
    ORDER_DTL["注文詳細\n/mypage/orders/{orderNumber}"]
    FAVS["お気に入り一覧\n/mypage/favorites"]
    PROFILE["会員情報変更\n/mypage/profile"]
    ADDRS["お届け先管理\n/mypage/addresses"]
    ADDR_FORM_NEW["お届け先追加\n/mypage/addresses/new"]
    ADDR_FORM_EDIT["お届け先編集\n/mypage/addresses/{id}/edit"]
    WITHDRAW["退会手続き\n/mypage/withdraw"]
    CART["カート\n/cart"]
    PRD["商品詳細\n/products/{id}"]

    LOGIN --"ログイン成功"--> MYP
    MYP --"リダイレクト"--> ORDERS
    ORDERS --"注文を選択"--> ORDER_DTL
    ORDERS --"ページ切替"--> ORDERS
    ORDER_DTL --"再注文（POST）"--> CART
    ORDER_DTL --"戻る"--> ORDERS
    MYP --"お気に入り"--> FAVS
    FAVS --"商品を選択"--> PRD
    MYP --"会員情報変更"--> PROFILE
    PROFILE --"保存（POST）"--> PROFILE
    MYP --"お届け先管理"--> ADDRS
    ADDRS --"追加する"--> ADDR_FORM_NEW
    ADDRS --"編集する"--> ADDR_FORM_EDIT
    ADDR_FORM_NEW --"保存（POST）"--> ADDRS
    ADDR_FORM_EDIT --"保存（POST）"--> ADDRS
    MYP --"退会手続き"--> WITHDRAW
    WITHDRAW --"退会を実行（POST）"--> LOGIN
```

---

## 7. お問い合わせフロー

```mermaid
flowchart TD
    ANY["任意の画面（フッターリンク等）"]
    CONTACT["お問い合わせ\n/contact"]
    SENT["送信完了\n（同一ページにメッセージ表示）"]

    ANY --"お問い合わせリンク"--> CONTACT
    CONTACT --"送信（POST）"--> SENT
    SENT --"トップへ戻る"--> TOP["トップ\n/"]
```

---

## 8. コンテンツページ

```mermaid
flowchart LR
    TOP["トップ\n/"]
    ABOUT["OFFICE ORDERについて\n/about"]
    GUIDE["ご利用ガイド\n/guide"]
    TERMS["利用規約\n/legal/terms"]
    PRIVACY["プライバシーポリシー\n/legal/privacy"]
    TOKUSHO["特定商取引法に基づく表記\n/legal/tokusho"]
    ANM["お知らせ一覧\n/announcements"]

    TOP --> ABOUT
    TOP --> GUIDE
    TOP --> ANM
    GUIDE --> TERMS
    GUIDE --> PRIVACY
    GUIDE --> TOKUSHO
```
