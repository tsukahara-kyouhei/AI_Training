# 画面遷移図

## 凡例

```
[画面名]   : 画面（ページ）
-->        : 画面遷移（ユーザー操作）
-.->       : リダイレクト（条件分岐による自動遷移）
```

---

## 遷移図

```mermaid
flowchart TD
    %% ─────────────────────────────
    %% ノード定義
    %% ─────────────────────────────

    subgraph PUBLIC["一般公開画面"]
        TOP["トップ\n/"]
        ANNOUNCE["お知らせ一覧\n/announcements"]
        ABOUT["会社概要\n/about"]
        GUIDE["ご利用ガイド\n/guide"]
        LEGAL_T["利用規約\n/legal/terms"]
        LEGAL_P["プライバシーポリシー\n/legal/privacy"]
        LEGAL_K["特定商取引法\n/legal/tokusho"]
        CONTACT["お問い合わせ\n/contact"]
    end

    subgraph CATALOG["商品カタログ"]
        NEW_ARR["新着商品一覧\n/products/new-arrivals"]
        SEARCH["商品検索結果\n/products/search"]
        CAT_DESK["カテゴリ一覧（デスク）\n/categories/desks"]
        CAT_CHAIR["カテゴリ一覧（チェア）\n/categories/chairs"]
        CAT_STOR["カテゴリ一覧（収納）\n/categories/storages"]
        PRODUCT["商品詳細\n/products/{productId}"]
    end

    subgraph CHECKOUT["購入フロー"]
        CART["カート\n/cart"]
        CHK_METHOD["購入方法選択\n/checkout/method"]
        CHK_INPUT["注文情報入力\n/checkout/input"]
        CHK_CONFIRM["注文確認\n/checkout/confirm"]
        CHK_COMPLETE["注文完了\n/checkout/complete/{orderNumber}"]
    end

    subgraph AUTH["会員登録・認証"]
        LOGIN["ログイン\n/login"]
        REG_FORM["会員登録入力\n/members/register"]
        REG_CONFIRM["会員登録確認\n（POST /members/register/confirm）"]
        REG_COMPLETE["会員登録完了\n/members/register/complete"]
    end

    subgraph MYPAGE["マイページ（要ログイン）"]
        MY_ORDERS["購入履歴一覧\n/mypage/orders"]
        MY_ORDER_DET["購入履歴詳細\n/mypage/orders/{orderNumber}"]
        MY_FAV["お気に入り一覧\n/mypage/favorites"]
        MY_PROFILE["会員情報変更\n/mypage/profile"]
        MY_ADDR_LIST["追加お届け先一覧\n/mypage/addresses"]
        MY_ADDR_FORM["追加お届け先登録・編集\n/mypage/addresses/new\n/mypage/addresses/{id}/edit"]
        MY_WITHDRAW["退会確認\n/mypage/withdraw"]
    end

    ERROR["エラー画面\n/error"]

    %% ─────────────────────────────
    %% 遷移定義
    %% ─────────────────────────────

    %% トップ → 各所
    TOP --> ANNOUNCE
    TOP --> NEW_ARR
    TOP --> CAT_DESK
    TOP --> CAT_CHAIR
    TOP --> CAT_STOR
    TOP --> SEARCH
    TOP --> ABOUT
    TOP --> GUIDE
    TOP --> CONTACT
    TOP --> LEGAL_T
    TOP --> LEGAL_P
    TOP --> LEGAL_K
    TOP -->|ナビ: マイページ| MY_ORDERS
    TOP -->|ナビ: カート| CART

    %% カタログ → 商品詳細
    NEW_ARR --> PRODUCT
    SEARCH --> PRODUCT
    CAT_DESK --> PRODUCT
    CAT_CHAIR --> PRODUCT
    CAT_STOR --> PRODUCT

    %% 商品詳細 → カート
    PRODUCT -->|"カートに追加\n（POST /cart/items）"| CART

    %% ─── 購入フロー ───
    CART -->|購入手続きへ| CHK_METHOD
    CART -..->|カートが空| CART

    CHK_METHOD -->|ゲスト購入 / ログイン済み| CHK_INPUT
    CHK_METHOD -->|会員ログイン| LOGIN
    LOGIN -..->|認証成功（チェックアウト中）| CHK_INPUT
    LOGIN -..->|認証成功（通常）| MY_ORDERS

    CHK_INPUT -->|入力確認| CHK_CONFIRM
    CHK_INPUT -..->|カートが空| CART

    CHK_CONFIRM -->|"注文確定\n（POST /checkout/confirm）"| CHK_COMPLETE
    CHK_CONFIRM -->|修正する| CHK_INPUT
    CHK_CONFIRM -..->|トークン不一致| CHK_INPUT
    CHK_CONFIRM -..->|カートが空| CART

    CHK_COMPLETE -->|"トップへ戻る /\n購入履歴を見る"| TOP

    %% ─── 会員登録 ───
    LOGIN --> REG_FORM
    REG_FORM -->|入力確認| REG_CONFIRM
    REG_CONFIRM -->|登録確定| REG_COMPLETE
    REG_CONFIRM -..->|バリデーションエラー| REG_FORM
    REG_COMPLETE -..->|登録完了（自動ログイン）| MY_ORDERS

    %% ─── マイページ内遷移 ───
    MY_ORDERS --> MY_ORDER_DET
    MY_ORDERS --> MY_FAV
    MY_ORDERS --> MY_PROFILE
    MY_ORDERS --> MY_ADDR_LIST
    MY_ORDERS --> MY_WITHDRAW

    MY_ORDER_DET -->|"再購入\n（POST /mypage/.../reorder）"| CART

    MY_FAV --> PRODUCT

    MY_PROFILE -..->|更新完了| MY_PROFILE

    MY_ADDR_LIST --> MY_ADDR_FORM
    MY_ADDR_FORM -..->|登録・更新完了| MY_ADDR_LIST

    MY_WITHDRAW -..->|退会完了| LOGIN

    %% ─── お問い合わせ ───
    CONTACT -..->|送信完了（フラッシュ）| CONTACT

    %% ─── エラー ───
    CHK_CONFIRM -..->|"システムエラー\n（例: BUG-001 DataIntegrityViolationException）"| ERROR
    MY_ORDERS -..->|未ログイン→401| ERROR
```

---

## フロー別サマリー

### 購入フロー（ゲスト）

```
商品詳細 → カートに追加 → カート → 購入方法選択
  → ゲスト購入を選択 → 注文情報入力 → 注文確認 → 注文完了
```

### 購入フロー（会員）

```
商品詳細 → カートに追加 → カート → 購入方法選択
  → ログイン → 注文情報入力（会員情報を自動反映） → 注文確認 → 注文完了
```

### 会員登録フロー

```
ログイン画面から登録リンク → 会員登録入力 → 会員登録確認
  → 登録確定（自動ログイン） → 購入履歴一覧（マイページ）
```

### マイページ操作フロー

```
ログイン → 購入履歴一覧（マイページ起点）
  ├─ 購入履歴詳細 → 再購入 → カート
  ├─ お気に入り一覧 → 商品詳細
  ├─ 会員情報変更
  ├─ 追加お届け先一覧 → 登録・編集
  └─ 退会確認 → 退会完了 → ログイン
```

### チェックアウト中のガード遷移

| 条件                                          | 遷移先                                                   |
| --------------------------------------------- | -------------------------------------------------------- |
| `购入方法選択` アクセス時にカートが空         | `/cart` へリダイレクト                                   |
| `注文情報入力` アクセス時にカートが空         | `/cart` へリダイレクト                                   |
| `注文確認` アクセス時にセッションフォームなし | `/checkout/input` へリダイレクト                         |
| `注文確認` POSTでトークン不一致               | `/checkout/input` へリダイレクト（エラーメッセージ付き） |
| `注文確定` でビジネスエラー（在庫切れ等）     | `/checkout/input` へリダイレクト（エラーメッセージ付き） |
| `注文確定` でシステムエラー                   | エラー画面（500）                                        |
