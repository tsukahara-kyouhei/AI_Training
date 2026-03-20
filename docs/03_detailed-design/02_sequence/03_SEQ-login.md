# ログインシーケンス図

## 1. ログインフロー

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant AC as AuthController
    participant SF as Spring Security Filter
    participant UDS as MemberUserDetailsService
    participant MR as MemberRepository
    participant SS as MemberSessionService

    U->>AC: GET /login
    AC-->>U: ログイン画面

    U->>SF: POST /login (email, password)
    SF->>UDS: loadUserByUsername(email)
    UDS->>MR: findActiveCredentialByEmail(email)
    MR-->>UDS: MemberCredential (パスワードハッシュ・ステータス)

    alt メール不存在
        UDS--xSF: UsernameNotFoundException
        SF-->>U: 302 /login?error
    else member_status = 'withdrawn'
        UDS-->>SF: アカウント無効 (disabled)
        SF-->>U: 302 /login?error
    else OK
        SF->>SF: BCrypt.matches(input, hash)
        alt パスワード不一致
            SF-->>U: 302 /login?error
        else 一致
            SF->>SF: セッション固定化止め (changeSessionId)
            SF->>SS: MemberPrincipalをセッションに格納
            SF-->>U: 302 / (または元ページ)
        end
    end
```

---

## 2. ログイン後のリダイレクト

| ケース | リダイレクト先 |
|---|---|
| 通常ログイン | `POST /login` 前にアクセスしたページ（保存された場合） |
| `redirect`パラメータあり | 検証済みの相対パスにリダイレクト |
| デフォルト | `/` |

## 3. ログアウトフロー

```
1. POST /logout (CSRFトークン付き)
2. Spring Securityがセッションを無効化
3. SecurityContextをクリア
4. 302 /login へリダイレクト
```

## 4. エラーメッセージ

| ケース | メッセージ |
|---|---|
| メールまたはパスワード誤り | 「メールアドレスまたはパスワードが正しくありません」 |
| 退会済みアカウント | 同上（内部的に判定するがユーザーには区別しない） |
