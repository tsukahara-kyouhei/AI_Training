# 画面遷移図

本図は、実装済みの Controller の画面表示・リダイレクトをもとにした主要な画面遷移を示します。画面を伴わない更新処理は、処理ノードから遷移先へ表現しています。

画面名を中心に、利用者がたどる主要な流れを機能単位で整理しています。画面ID、URL、認証条件は [画面一覧](screen-list.md) を参照してください。

通常遷移は実線、入力エラー・認証エラー・対象なしなどの例外遷移は破線で表します。

## ① 全体概要

```mermaid
flowchart LR
    TOP[トップ] --> CATALOG[商品・検索]
    CATALOG --> DETAIL[商品詳細]
    DETAIL --> CART[カート]
    CART --> CHECKOUT[購入]
    CHECKOUT --> COMPLETE[注文完了]

    CART --> LOGIN[ログイン]
    LOGIN --> REGISTER[会員登録]
    LOGIN --> MYPAGE[マイページ系<br/>入口: 購入履歴一覧]
    REGISTER --> MYPAGE

    TOP --> OTHER[その他]
    DETAIL -.-> ERROR[エラー画面]
    CHECKOUT -.-> ERROR

    classDef group fill:#e8f1f5,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class CATALOG,DETAIL,CART,CHECKOUT,LOGIN,REGISTER,MYPAGE,OTHER group;
    class ERROR error;
```

## ② 商品・検索系画面遷移

検索結果、新着、カテゴリの各一覧から商品詳細へ進み、商品詳細からカートへ進みます。

```mermaid
flowchart LR
    subgraph ENTRY[商品を探す]
        TOP[トップ]
        SEARCH[商品検索結果]
        NEW[新着商品一覧]
        DESK[デスクカテゴリ一覧]
        CHAIR[チェアカテゴリ一覧]
        STORAGE[収納家具カテゴリ一覧]
    end

    TOP -->|検索| SEARCH
    TOP -->|新着| NEW
    TOP -->|カテゴリ選択| DESK
    TOP -->|カテゴリ選択| CHAIR
    TOP -->|カテゴリ選択| STORAGE

    SEARCH --> DETAIL[商品詳細]
    NEW --> DETAIL
    DESK --> DETAIL
    CHAIR --> DETAIL
    STORAGE --> DETAIL
    DETAIL -->|カート追加| CART[カート]
    DETAIL -.->|商品なし| ERROR[エラー画面]

    classDef screen fill:#edf5f7,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class TOP,SEARCH,NEW,DESK,CHAIR,STORAGE,DETAIL,CART screen;
    class ERROR error;
```

## ③ 購入系画面遷移

```mermaid
flowchart TD
    CART[カート] --> METHOD[購入方法選択]
    METHOD -->|会員購入| LOGIN[ログイン]
    METHOD -->|ゲスト購入| INPUT[注文情報入力]
    LOGIN -->|ログイン成功| INPUT
    INPUT -->|入力送信| CONFIRM[注文確認]
    CONFIRM -->|注文確定成功| COMPLETE[注文完了]

    CONFIRM -.->|在庫不足・業務エラー| INPUT
    CONFIRM -.->|DB等の未処理例外| ERROR[エラー画面]
    METHOD -.->|カート空| CART

    classDef screen fill:#edf5f7,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class CART,METHOD,LOGIN,INPUT,CONFIRM,COMPLETE screen;
    class ERROR error;
```

## ④ 会員登録・ログイン系画面遷移

```mermaid
flowchart LR
    LOGIN[ログイン] -->|会員登録| REGISTER[会員登録入力]
    REGISTER -->|入力確認| REGISTER_CONFIRM[会員登録確認]
    REGISTER_CONFIRM -->|登録確定| REGISTER_COMPLETE[会員登録完了]
    REGISTER_COMPLETE -->|ログイン状態で利用| MYPAGE[購入履歴一覧]
    LOGIN -->|ログイン成功| MYPAGE

    classDef screen fill:#edf5f7,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class LOGIN,REGISTER,REGISTER_CONFIRM,REGISTER_COMPLETE,MYPAGE screen;
```

## ⑤ マイページ系画面遷移

マイページ系の入口は購入履歴一覧です。`/mypage` へのアクセスは購入履歴一覧へリダイレクトされます。各機能画面からの操作先を中心に示します。

```mermaid
flowchart LR
    MYPAGE[購入履歴一覧] --> ORDER_DETAIL[購入履歴詳細]
    ORDER_DETAIL -->|再購入| CART[カート]
    MYPAGE --> FAVORITES[お気に入り一覧]
    MYPAGE --> PROFILE[会員情報変更]
    MYPAGE --> ADDRESSES[追加お届け先一覧]
    MYPAGE --> WITHDRAW[退会確認]

    ADDRESSES -->|新規登録| ADDRESS_NEW[追加お届け先登録]
    ADDRESSES -->|編集| ADDRESS_EDIT[追加お届け先編集]
    ADDRESS_NEW -->|登録| ADDRESSES
    ADDRESS_EDIT -->|更新| ADDRESSES
    WITHDRAW -->|退会実行| LOGIN[ログイン]

    MYPAGE -.->|未認証・無効セッション| LOGIN
    ORDER_DETAIL -.->|注文なし| ERROR[エラー画面]
    ADDRESS_EDIT -.->|お届け先なし| ERROR

    classDef screen fill:#edf5f7,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class MYPAGE,ORDER_DETAIL,FAVORITES,PROFILE,ADDRESSES,ADDRESS_NEW,ADDRESS_EDIT,WITHDRAW,CART,LOGIN screen;
    class ERROR error;
```

## ⑥ その他画面

```mermaid
flowchart LR
    TOP[トップ] --> ANNOUNCE[お知らせ一覧]
    TOP --> CONTACT[お問い合わせ]
    TOP --> ABOUT[OFFICE ORDER について]
    TOP --> GUIDE[ご利用ガイド]
    TOP --> TERMS[利用規約]
    TOP --> PRIVACY[プライバシーポリシー]
    TOP --> TOKUSHO[特定商取引法に基づく表記]

    TOP -.-> ERROR[エラー画面]

    classDef screen fill:#edf5f7,stroke:#426b7a,color:#17313b;
    classDef error fill:#fff0f0,stroke:#b44b4b,color:#5b1f1f;
    class TOP,ANNOUNCE,CONTACT,ABOUT,GUIDE,TERMS,PRIVACY,TOKUSHO screen;
    class ERROR error;
```

## 補足

- `/mypage/**` は Spring Security により会員ロールが必要です。未認証または無効セッションの場合はログイン画面へ誘導されます。
- 「マイページ」は独立画面ではありません。`/mypage` は購入履歴一覧へリダイレクトされ、画面上のマイページ系入口は購入履歴一覧です。
- 商品詳細のお気に入り操作は、未ログイン時にログイン画面へ誘導されます。ログイン済みの場合は同じ商品詳細を再表示します。
- カート追加、数量変更、削除、全削除は画面を伴わない POST 操作で、操作後はカートへ戻ります。
- ログイン、会員登録確定、会員情報更新、住所の登録・更新・削除、退会実行は画面を伴わない POST 操作です。
- 注文情報入力、注文確認、会員登録入力で入力エラーが発生した場合は、それぞれの入力画面へ戻ります。注文確定時の在庫不足・業務エラーは注文情報入力へ戻ります。
- 注文確定後の DB 保存などで未処理例外が発生した場合は、共通エラー処理を経てエラー画面になります。対象なしの場合もエラー画面になります。
- `GET /products/recently-viewed` は JSON API、`/internal/batch/**` はローカル開発専用の JSON API です。画面ではないため遷移図から意図的に除外しています。
- `/internal/test/errors/500` はローカル開発時のエラー画面確認用で、通常の利用者遷移から意図的に除外しています。