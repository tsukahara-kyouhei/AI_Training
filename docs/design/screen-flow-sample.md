# 画面遷移図 スタイルサンプル

> `トップ → お知らせ一覧` 周辺を題材に、Mermaid でよりグラフィカルに表現するサンプル集。

---

## パターン A: カラー＋形状で種別を表現

ノード形状で画面の種別を区別し、`classDef` で色を付けます。

```mermaid
%%{init: {"theme": "base", "themeVariables": {"primaryColor": "#f0f4ff", "edgeLabelBackground": "#ffffff"}}}%%
flowchart TD
    classDef page        fill:#4f80ff,stroke:#2255cc,color:#fff,rx:6,ry:6
    classDef list        fill:#22b97a,stroke:#158a58,color:#fff,rx:6,ry:6
    classDef auth        fill:#f5a623,stroke:#c77d00,color:#fff,rx:6,ry:6
    classDef action      fill:#ffffff,stroke:#4f80ff,color:#4f80ff,stroke-dasharray:4

    TOP(["🏠 トップ\n/"]):::page
    ANNOUNCE(["📢 お知らせ一覧\n/announcements"]):::list
    NEW_ARRIVALS(["🆕 新着商品一覧\n/products/new-arrivals"]):::list
    SEARCH(["🔍 検索結果\n/products/search"]):::list
    CAT_DESK(["🪑 デスク一覧\n/categories/desks"]):::list
    CAT_CHAIR(["🪑 チェア一覧\n/categories/chairs"]):::list
    CAT_STORAGE(["🗄️ 収納一覧\n/categories/storages"]):::list

    TOP -->|"お知らせ"| ANNOUNCE
    TOP -->|"新着"| NEW_ARRIVALS
    TOP -->|"検索"| SEARCH
    TOP -->|"カテゴリ"| CAT_DESK
    TOP -->|"カテゴリ"| CAT_CHAIR
    TOP -->|"カテゴリ"| CAT_STORAGE
```

---

## パターン B: subgraph＋背景色でゾーン分け

画面グループを `subgraph` で囲み、エリア全体の背景を塗り分けます。

```mermaid
%%{init: {"theme": "base"}}%%
flowchart LR
    classDef topPage  fill:#6c8ef5,stroke:#3a5bd9,color:#fff
    classDef listPage fill:#34c98a,stroke:#1a9962,color:#fff
    classDef detailPage fill:#f0c040,stroke:#c89000,color:#222

    subgraph GLOBAL["🌐 グローバルナビ経由"]
        direction TB
        TOP(["🏠 トップ\n/"]):::topPage
    end

    subgraph INFO["📋 情報・コンテンツ"]
        direction TB
        ANNOUNCE(["📢 お知らせ一覧\n/announcements"]):::listPage
    end

    subgraph CATALOG["🛍️ 商品カタログ"]
        direction TB
        NEW_ARRIVALS(["🆕 新着商品一覧\n/products/new-arrivals"]):::listPage
        CAT_DESK(["🪑 デスク\n/categories/desks"]):::listPage
        CAT_CHAIR(["💺 チェア\n/categories/chairs"]):::listPage
        CAT_STORAGE(["🗄️ 収納\n/categories/storages"]):::listPage
        SEARCH(["🔍 検索結果\n/products/search"]):::listPage
        PRODUCT(["📦 商品詳細\n/products/{id}"]):::detailPage
    end

    TOP -->|"お知らせ"| ANNOUNCE
    TOP -->|"新着"| NEW_ARRIVALS
    TOP -->|"検索"| SEARCH
    TOP -->|"デスク"| CAT_DESK
    TOP -->|"チェア"| CAT_CHAIR
    TOP -->|"収納"| CAT_STORAGE
    NEW_ARRIVALS --> PRODUCT
    CAT_DESK --> PRODUCT
    CAT_CHAIR --> PRODUCT
    CAT_STORAGE --> PRODUCT
    SEARCH --> PRODUCT
```

---

## パターン C: 「上から一本道」＋ステータス表示

ユーザーが辿る主動線のみに絞り、ラベルをアクション名で統一してシンプルに見せます。

```mermaid
flowchart TD
    classDef active  fill:#4f80ff,stroke:none,color:#fff,font-weight:bold
    classDef normal  fill:#e8eeff,stroke:#4f80ff,color:#4f80ff
    classDef success fill:#22b97a,stroke:none,color:#fff
    classDef warn    fill:#f5a623,stroke:none,color:#fff

    START(["`**開始**
    ブラウザでアクセス`"]):::active

    TOP(["`🏠 **トップ**
    \`/\``"]):::active

    ANNOUNCE(["`📢 **お知らせ一覧**
    \`/announcements\``"]):::normal

    NEW(["`🆕 **新着商品一覧**
    \`/products/new-arrivals\``"]):::normal

    PRODUCT(["`📦 **商品詳細**
    \`/products/{id}\``"]):::normal

    CART(["`🛒 **カート**
    \`/cart\``"]):::success

    START -->|"アクセス"| TOP
    TOP -->|"お知らせを見る"| ANNOUNCE
    TOP -->|"新着商品を見る"| NEW
    NEW -->|"商品を選ぶ"| PRODUCT
    PRODUCT -->|"カートに追加"| CART

    style START fill:#a0b4ff,stroke:none,color:#fff
```

---

## パターン D: `graph` + `linkStyle` でエッジを色分け

通常遷移・エラー遷移・リダイレクトを矢印の色で区別します。

```mermaid
graph TD
    TOP["🏠 トップ\n/"]
    ANNOUNCE["📢 お知らせ一覧\n/announcements"]
    NEW["🆕 新着商品一覧\n/products/new-arrivals"]
    SEARCH["🔍 検索結果\n/products/search"]
    CAT["📂 カテゴリ一覧\n/categories/*"]
    PRODUCT["📦 商品詳細\n/products/{id}"]
    CART["🛒 カート\n/cart"]
    LOGIN["🔐 ログイン\n/login"]

    TOP --> ANNOUNCE
    TOP --> NEW
    TOP --> SEARCH
    TOP --> CAT
    NEW --> PRODUCT
    SEARCH --> PRODUCT
    CAT --> PRODUCT
    PRODUCT -- "カートに入れる" --> CART
    PRODUCT -. "お気に入り\n（未ログイン）" .-> LOGIN

    style TOP       fill:#6c8ef5,stroke:#3a5bd9,color:#fff
    style ANNOUNCE  fill:#34c98a,stroke:#1a9962,color:#fff
    style NEW       fill:#34c98a,stroke:#1a9962,color:#fff
    style SEARCH    fill:#34c98a,stroke:#1a9962,color:#fff
    style CAT       fill:#34c98a,stroke:#1a9962,color:#fff
    style PRODUCT   fill:#f0c040,stroke:#c89000,color:#222
    style CART      fill:#ff7043,stroke:#bf360c,color:#fff
    style LOGIN     fill:#9e9e9e,stroke:#616161,color:#fff

    linkStyle 7 stroke:#ff7043,stroke-width:2px
    linkStyle 8 stroke:#9e9e9e,stroke-width:1.5px,stroke-dasharray:6
```

---

## 使い方メモ

| テクニック | 効果 |
|---|---|
| `classDef` + `:::` | ノード種別ごとに色・形を一括定義 |
| `style ノード名` | 個別ノードに直接スタイルを当てる |
| `linkStyle N` | N番目のエッジ（0-indexed）の色・太さを変更 |
| `subgraph` + `direction` | ゾーンをボックスで囲み背景を分ける |
| `([" "])` 形状 | スタジアム型（角丸ピル形）でページ感を演出 |
| 絵文字をラベルに含める | テキスト情報を補完し視覚的に分かりやすく |
| `` %%{init: ...}%% `` | テーマ全体の基調色・フォントを上書き |
