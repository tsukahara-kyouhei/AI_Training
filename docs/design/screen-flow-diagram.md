# 画面遷移図

以下は、主要なユーザー導線を Mermaid で表したものです。

```mermaid
flowchart TD
    A[トップ画面 /] --> B[お知らせ一覧 /announcements]
    A --> C[新着商品一覧 /products/new-arrivals]
    A --> D[商品検索結果 /products/search]
    A --> E[デスクカテゴリ /categories/desks]
    A --> F[チェアカテゴリ /categories/chairs]
    A --> G[収納家具カテゴリ /categories/storages]
    A --> H[お問い合わせ /contact]
    A --> I[会社案内 /about]
    A --> J[ご利用案内 /guide]
    A --> K[ログイン /login]

    C --> L[商品詳細 /products/{id}]
    D --> L
    E --> L
    F --> L
    G --> L

    L --> M[カート /cart]
    M --> N[購入方法選択 /checkout/method]
    N --> O[注文情報入力 /checkout/input]
    O --> P[注文確認 /checkout/confirm]
    P --> Q[注文完了 /checkout/complete/{orderNumber}]

    K --> R[会員登録入力 /members/register]
    R --> S[会員登録確認 /members/register/confirm]
    S --> T[会員登録完了 /members/register/complete]

    K --> U[マイページ /mypage]
    U --> V[購入履歴一覧 /mypage/orders]
    V --> W[購入履歴詳細 /mypage/orders/{orderNumber}]
    U --> X[お気に入り一覧 /mypage/favorites]
    U --> Y[プロフィール編集 /mypage/profile]
    U --> Z[配送先一覧 /mypage/addresses]
    Z --> AA[配送先登録・編集 /mypage/addresses/new or /mypage/addresses/{id}/edit]
    U --> AB[退会確認 /mypage/withdraw]
```

> 主要な遷移として、商品閲覧 → カート → 購入フロー、ログイン → 会員登録・マイページ、各静的コンテンツ画面への導線をまとめています。
