# 会員登録シーケンス図

## 1. 会員登録フロー

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant RC as MemberRegistrationController
    participant MS as MemberService
    participant MR as MemberRepository
    participant NS as NotificationMailService

    U->>RC: GET /members/register
    RC-->>U: 会員登録入力画面

    U->>RC: POST /members/register/confirm
    RC->>RC: BindingResultバリデーション
    alt バリデーションエラー
        RC-->>U: 入力画面に返る
    else OK
        RC->>MS: existsByEmail(email)
        MS->>MR: existsByEmail(email)
        MR-->>MS: boolean
        alt メール重複
            MS--xRC: MemberEmailDuplicateException
            RC-->>U: 入力画面（重複エラー表示）
        else OK
            RC->>RC: セッションに入力値を保存
            RC-->>U: 会員登録確認画面
        end
    end

    note over U,RC: ユーザーが確認画面で内容を確認

    U->>RC: POST /members/register
    RC->>RC: セッションから入力値取得
    RC->>MS: register(form)
    MS->>MS: BCrypt.encode(password)
    MS->>MR: insertMember(member)
    MR-->>MS: member_id
    MS->>NS: sendMemberRegistrationCompleteMail()ティング登録
    MS-->>RC: 完了
    RC-->>U: 302 /members/register/complete
    NS-->>U: 会員登録完了メール（非同期）

    U->>RC: GET /members/register/complete
    RC-->>U: 登録完了画面
```

---

## 2. バリデーションルール

| フィールド | ルール |
|---|---|
| `email` | `@Email`, UNIQUE確認（サービス層） |
| `password` | 8桁以上 |
| `password_confirm` | `password`と一致 |
| `last_name`, `first_name` | `@NotBlank`, 50文字以内 |
| `postal_code` | `@Pattern(7桁数字)` |
| `daytime_phone` | `@Pattern(10〜12桁数字)` |

## 3. 確認画面のセッション管理

- POST /confirm 時、入力内容をセッションに保存
- POST /register 時、セッションから内容を取得しDB保存
- 登録完了後にセッションデータをクリア
