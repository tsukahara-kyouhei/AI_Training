# 画面遷移図

以下は、実装されている Controller および Thymeleaf テンプレートから確認できる主要な画面遷移を Mermaid で表したものです。

```mermaid
flowchart TD
    A[トップ画面 /] --> B[お知らせ一覧 /announcements]
    A --> C[商品検索結果 /products/search]
    A --> D[新着商品一覧 /products/new-arrivals]
    A --> E[デスクカテゴリ /categories/desks]
    A --> F[チェアカテゴリ /categories/chairs]
    A --> G[収納家具カテゴリ /categories/storages]
    A --> H[ログイン /login]
    A --> I[カート /cart]
    A --> J[OFFICE ORDERについて /about]
    A --> K[ご利用ガイド /guide]
    A --> L[お問い合わせ /contact]

    C --> M[商品詳細 /products/{productId}]
    D --> M
    E --> M
    F --> M
    G --> M

    M --> N[カート /cart]
    M --> O[お気に入り追加・解除 /products/{productId}/favorite]
    M --> L
    M --> K

    I --> P[購入方法選択 /checkout/method]
    P --> Q[注文情報入力 /checkout/input]
    Q --> R[注文確認 /checkout/confirm]
    R --> S[注文完了 /checkout/complete/{orderNumber}]

    H --> T[会員登録入力 /members/register]
    T --> U[会員登録確認 /members/register/confirm]
    U --> V[会員登録完了 /members/register/complete]

    H --> W[マイページ /mypage/orders]
    W --> X[購入履歴詳細 /mypage/orders/{orderNumber}]
    W --> Y[お気に入り一覧 /mypage/favorites]
    W --> Z[会員情報変更 /mypage/profile]
    W --> AA[追加お届け先一覧 /mypage/addresses]
    W --> AB[退会確認 /mypage/withdraw]

    AA --> AC[追加お届け先登録・編集 /mypage/addresses/new /mypage/addresses/{memberAddressId}/edit]

    X --> N
    Y --> M
    AA --> N

    Q --> N
    R --> N
    S --> W

    J --> J
    K --> K
    L --> L
```

> 注: Mermaid 図に含めた遷移は、実装上確認できたものに限定しています。未確認の遷移は示していません。
