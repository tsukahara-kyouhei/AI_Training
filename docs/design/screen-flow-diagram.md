# 画面遷移図

本ドキュメントはシステムの主要な画面遷移を Mermaid 記法で表したものです。
各画面の詳細は [screen-list.md](./screen-list.md) を参照してください。

---

## 凡例

| 矢印の種類 | 意味 |
|-----------|------|
| `A --> B` | 通常のページ遷移（リンク・ボタン押下） |
| `A -- ラベル --> B` | ラベル付き通常遷移 |
| `A -.-> B` | リダイレクト遷移（POST後のリダイレクト、条件付き遷移） |
| `A -. ラベル .-> B` | ラベル付きリダイレクト遷移 |

---

## 画面遷移図

```mermaid
flowchart TD
    %% ===================================================
    %% 情報・固定ページ
    %% ===================================================
    subgraph STATIC["情報・固定ページ"]
        TOP["SC-001: トップ<br/>(GET /)"]
        ANN["SC-002: お知らせ一覧<br/>(GET /announcements)"]
        ABOUT["SC-003: OFFICE ORDER について<br/>(GET /about)"]
        GUIDE["SC-004: ご利用ガイド<br/>(GET /guide)"]
        CONTACT["SC-005: お問い合わせ<br/>(GET /contact)"]
        TERMS["SC-006: 利用規約<br/>(GET /legal/terms)"]
        PRIVACY["SC-007: プライバシーポリシー<br/>(GET /legal/privacy)"]
        TOKUSHO["SC-008: 特定商取引法表記<br/>(GET /legal/tokusho)"]
    end

    %% ===================================================
    %% 認証・会員登録
    %% ===================================================
    subgraph AUTH["認証・会員登録"]
        LOGIN["SC-009: ログイン<br/>(GET /login)"]
        REG["SC-010: 会員登録入力<br/>(GET /members/register)"]
        REG_CONF["SC-011: 会員登録確認<br/>(POST /members/register/confirm)"]
        REG_COMP["SC-012: 会員登録完了<br/>(GET /members/register/complete)"]
    end

    %% ===================================================
    %% 商品カタログ
    %% ===================================================
    subgraph CATALOG["商品カタログ"]
        NEW["SC-013: 新着商品一覧<br/>(GET /products/new-arrivals)"]
        SEARCH["SC-014: キーワード検索結果<br/>(GET /products/search)"]
        DESKS["SC-015: デスクカテゴリ一覧<br/>(GET /categories/desks)"]
        CHAIRS["SC-016: チェアカテゴリ一覧<br/>(GET /categories/chairs)"]
        STORAGES["SC-017: 収納家具カテゴリ一覧<br/>(GET /categories/storages)"]
        DETAIL["SC-018: 商品詳細<br/>(GET /products/{productId})"]
    end

    %% ===================================================
    %% カート・購入フロー
    %% ===================================================
    subgraph PURCHASE["カート・購入フロー"]
        CART["SC-019: カート<br/>(GET /cart)"]
        METHOD["SC-020: 購入方法選択<br/>(GET /checkout/method)"]
        INPUT["SC-021: 注文情報入力<br/>(GET /checkout/input)"]
        CONFIRM["SC-022: 注文確認<br/>(GET /checkout/confirm)"]
        COMPLETE["SC-023: 注文完了<br/>(GET /checkout/complete/{orderNumber})"]
    end

    %% ===================================================
    %% マイページ（要ログイン）
    %% ===================================================
    subgraph MYPAGE["マイページ（要ログイン）"]
        ORDERS["SC-024: 購入履歴一覧<br/>(GET /mypage/orders)"]
        ORDER_DETAIL["SC-025: 購入履歴詳細<br/>(GET /mypage/orders/{orderNumber})"]
        FAVORITES["SC-026: お気に入り一覧<br/>(GET /mypage/favorites)"]
        PROFILE["SC-027: 会員情報変更<br/>(GET /mypage/profile)"]
        ADDRS["SC-028: 追加お届け先一覧<br/>(GET /mypage/addresses)"]
        ADDR_FORM["SC-029: 追加お届け先フォーム<br/>(GET /mypage/addresses/new)<br/>(GET /mypage/addresses/{id}/edit)"]
        WITHDRAW["SC-030: 退会確認<br/>(GET /mypage/withdraw)"]
    end

    %% ===================================================
    %% エラー
    %% ===================================================
    ERROR["SC-031: エラー画面<br/>(/error)"]

    %% ===================================================
    %% 遷移: トップ → 各画面
    %% ===================================================
    TOP --> ANN
    TOP --> ABOUT
    TOP --> GUIDE
    TOP --> CONTACT
    TOP --> TERMS
    TOP --> PRIVACY
    TOP --> TOKUSHO
    TOP --> NEW
    TOP --> DESKS
    TOP --> CHAIRS
    TOP --> STORAGES
    TOP --> CART
    TOP --> LOGIN

    %% ===================================================
    %% 遷移: 商品カタログ内
    %% ===================================================
    %% ヘッダー検索フォームから任意の画面→検索結果へ遷移可能
    NEW --> DETAIL
    SEARCH --> DETAIL
    DESKS --> DETAIL
    CHAIRS --> DETAIL
    STORAGES --> DETAIL

    DETAIL -- カートへ追加 --> CART
    DETAIL -- お気に入り登録<br/>（未ログイン時はログインへ） --> LOGIN

    %% ===================================================
    %% 遷移: カート・購入フロー
    %% ===================================================
    CART -- 購入へ進む --> METHOD
    METHOD -- ゲスト購入 --> INPUT
    METHOD -- ログインして購入 --> LOGIN

    INPUT -- 入力確認へ --> CONFIRM
    INPUT -. バリデーションエラー .-> INPUT

    CONFIRM -- 注文確定 --> COMPLETE
    CONFIRM -. バリデーションエラー<br/>・在庫不足 .-> INPUT

    COMPLETE --> TOP
    COMPLETE --> ORDERS

    %% ===================================================
    %% 遷移: 認証・会員登録
    %% ===================================================
    LOGIN -. ログイン成功（通常） .-> ORDERS
    LOGIN -. ログイン成功（購入途中） .-> INPUT
    LOGIN --> REG

    REG -- 確認へ --> REG_CONF
    REG_CONF -. バリデーションエラー .-> REG
    REG_CONF -- 登録確定 --> REG_COMP
    REG_COMP -. 自動ログイン後 .-> ORDERS

    %% ===================================================
    %% 遷移: マイページ内
    %% ===================================================
    ORDERS --> ORDER_DETAIL
    ORDER_DETAIL -- 再購入 --> CART

    ORDERS --> FAVORITES
    ORDERS --> PROFILE
    ORDERS --> ADDRS
    ORDERS --> WITHDRAW

    ADDRS --> ADDR_FORM
    ADDR_FORM -. 保存完了 .-> ADDRS

    PROFILE -. 更新完了 .-> PROFILE

    WITHDRAW -. 退会完了 .-> LOGIN

    %% ===================================================
    %% 遷移: 未認証アクセス
    %% ===================================================
    MYPAGE -. 未ログイン時リダイレクト .-> LOGIN

    %% ===================================================
    %% 遷移: エラー
    %% ===================================================
    STATIC & CATALOG & PURCHASE & AUTH -. サーバーエラー .-> ERROR
    MYPAGE -. サーバーエラー .-> ERROR
```

---

## 補足

### 全画面共通のグローバルナビゲーション

ヘッダー・フッターから以下の遷移が全画面より可能ですが、図の簡潔さのため省略しています。

| 遷移元 | 遷移先 |
|--------|--------|
| 任意の画面（ヘッダーロゴ） | SC-001: トップ (`/`) |
| 任意の画面（ヘッダー検索フォーム） | SC-014: キーワード検索結果 (`/products/search?q=...`) |
| 任意の画面（ヘッダーカートアイコン） | SC-019: カート (`/cart`) |
| 任意の画面（ヘッダーログインリンク） | SC-009: ログイン (`/login`) |
| ログイン済み・任意の画面（ヘッダーマイページリンク） | SC-024: 購入履歴一覧 (`/mypage/orders`) |
| 任意の画面（フッター各リンク） | SC-003〜SC-008: 各固定コンテンツページ |

### 購入方法選択画面（SC-020）のログインフォーム

SC-020（購入方法選択）には、ログインフォームが埋め込まれています。
このフォームから会員ログインを行うと、Spring Security により `/checkout/input` へリダイレクトされます（`checkoutRedirectPath` セッション属性による制御）。

### マイページの未認証アクセス

`/mypage/**` への未認証アクセス時は、`requireLoginMember()` が HTTP 401 をスローし、
Spring Security のエラーハンドリングにより SC-009（ログイン画面）へリダイレクトされます。
