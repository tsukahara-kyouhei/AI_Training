# 画面遷移図

```mermaid
flowchart TD
    TOP["S-01 トップ\n/"]

    subgraph CAT["商品カタログ"]
        NEW["S-10 新着商品一覧\n/products/new-arrivals"]
        SEARCH["S-11 検索結果一覧\n/products/search"]
        DESK["S-12 デスク一覧\n/categories/desks"]
        CHAIR["S-13 チェア一覧\n/categories/chairs"]
        STORAGE["S-14 収納家具一覧\n/categories/storages"]
        DETAIL["S-15 商品詳細\n/products/{productId}"]
    end

    subgraph FLOW["カート・購入フロー"]
        CART["S-20 カート\n/cart"]
        METHOD["S-21 購入方法選択\n/checkout/method"]
        INPUT["S-22 注文情報入力\n/checkout/input"]
        CONFIRM["S-23 注文確認\n/checkout/confirm"]
        COMPLETE["S-24 注文完了\n/checkout/complete/{orderNumber}"]
    end

    subgraph AUTH["認証・会員登録"]
        LOGIN["S-30 ログイン\n/login"]
        REG["S-31 会員登録（入力）\n/members/register"]
        REG_C["S-32 会員登録（確認）\n/members/register/confirm"]
        REG_DONE["S-33 会員登録（完了）\n/members/register/complete"]
    end

    subgraph MYPAGE["マイページ（要ログイン）"]
        O_LIST["S-40 購入履歴一覧\n/mypage/orders"]
        O_DETAIL["S-41 購入履歴詳細\n/mypage/orders/{orderNumber}"]
        FAV["S-42 お気に入り一覧\n/mypage/favorites"]
        PROFILE["S-43 会員情報変更\n/mypage/profile"]
        ADDR["S-44 追加お届け先一覧\n/mypage/addresses"]
        ADDR_FORM["S-45/46 追加お届け先登録・編集\n/mypage/addresses/new\n/mypage/addresses/{id}/edit"]
        WITHDRAW["S-47 退会確認\n/mypage/withdraw"]
    end

    subgraph CONTENT["固定コンテンツ・その他"]
        ANNOUNCE["S-02 お知らせ一覧\n/announcements"]
        CONTACT["S-50 お問い合わせ\n/contact"]
        ABOUT["S-60 OFFICE ORDER について\n/about"]
        GUIDE["S-61 ご利用ガイド\n/guide"]
        LEGAL["S-62/63/64 各種法的表記\n/legal/terms など"]
    end

    ERR["S-70/71 エラー画面\n/error"]

    %% ── トップ from 各所 ──────────────────────────────
    TOP --> NEW
    TOP --> DESK
    TOP --> CHAIR
    TOP --> STORAGE
    TOP --> SEARCH
    TOP --> DETAIL
    TOP --> ANNOUNCE

    %% ── 商品一覧 → 詳細 ──────────────────────────────
    NEW --> DETAIL
    SEARCH --> DETAIL
    DESK --> DETAIL
    CHAIR --> DETAIL
    STORAGE --> DETAIL

    %% ── 商品詳細 → カート・お気に入り ────────────────
    DETAIL -->|"カートに追加"| CART
    DETAIL -->|"お気に入り追加\n（未ログイン時）"| LOGIN

    %% ── カート → 購入フロー ──────────────────────────
    CART -->|"購入手続きへ"| METHOD
    METHOD -->|"カートが空"| CART
    METHOD -->|"ゲスト購入を選択"| INPUT
    METHOD -->|"会員ログインを選択"| LOGIN

    %% ── ログイン ─────────────────────────────────────
    LOGIN -->|"ログイン成功\n（購入フロー中）"| INPUT
    LOGIN -->|"ログイン成功\n（通常 / 既ログイン時）"| O_LIST
    LOGIN -->|"新規会員登録へ"| REG

    %% ── 注文情報入力 → 確認 → 完了 ──────────────────
    INPUT -->|"カートが空"| CART
    INPUT -->|"入力完了・次へ"| CONFIRM
    CONFIRM -->|"修正する"| INPUT
    CONFIRM -->|"カートが空\nまたはセッション切れ"| CART
    CONFIRM -->|"注文を確定する"| COMPLETE
    COMPLETE -->|"購入履歴へ"| O_LIST

    %% ── 会員登録フロー ───────────────────────────────
    REG -->|"確認画面へ"| REG_C
    REG_C -->|"修正する"| REG
    REG_C -->|"登録する"| REG_DONE
    REG_DONE -->|"自動ログイン後\nマイページへ"| O_LIST

    %% ── マイページ内 ─────────────────────────────────
    O_LIST --> O_DETAIL
    O_DETAIL -->|"再購入する"| CART
    FAV --> DETAIL
    PROFILE -->|"保存完了（自画面へ）"| PROFILE
    ADDR --> ADDR_FORM
    ADDR_FORM -->|"保存・削除完了"| ADDR
    WITHDRAW -->|"退会する"| LOGIN

    %% ── 未認証リダイレクト ───────────────────────────
    O_LIST -->|"未ログイン時"| LOGIN
    O_DETAIL -->|"未ログイン時"| LOGIN
    FAV -->|"未ログイン時"| LOGIN
    PROFILE -->|"未ログイン時"| LOGIN
    ADDR -->|"未ログイン時"| LOGIN
    WITHDRAW -->|"未ログイン時"| LOGIN

    %% ── エラー ───────────────────────────────────────
    DETAIL -->|"商品が存在しない（404）"| ERR
    O_DETAIL -->|"注文が見つからない（404）"| ERR
    COMPLETE -->|"注文番号が無効（404）"| ERR
    CONFIRM -->|"サーバーエラー（500）"| ERR
```

---

## 補足

### 購入フローのセッション制御

```
カート（Cookie）→ 購入方法選択 → 注文情報入力
                                  ↓ セッションに保存（フォーム＋ワンタイムトークン）
                               注文確認
                                  ↓ トークン照合 OK → 注文確定 → セッション破棄・カートクリア
                               注文完了
```

### ログイン後リダイレクト

`/login?redirect={path}` パラメータ付きでアクセスした場合、ログイン成功後は `redirect` で指定されたパスへ遷移する（お気に入り操作・購入フローからの誘導などで利用）。

### `/mypage` へのアクセス

`GET /mypage` は `GET /mypage/orders`（購入履歴一覧）へ自動リダイレクトする。
