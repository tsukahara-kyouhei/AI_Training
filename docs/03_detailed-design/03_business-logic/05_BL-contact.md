# 問い合わせサービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `ContactService` | `service.contact` | 問い合わせ受付・DB保存 |
| `ContactController` | `web.controller` | 入力画面・確認画面・送信処理 |
| `ContactRepository` | `repository` | DBアクセス |

---

## 2. 処理フロー

```
1. GET /contact -> 入力画面表示
   ログイン中の場合: メールアドレス・氏名をプリフィル

2. POST /contact?step=confirm -> バリデーション
   エラーあり: 入力画面戳り
   OK: 確認画面返嚗（セッションに入力内容保存）

3. POST /contact?step=send -> ContactService.submit(form)
   - inquiriesテーブルにINSERT
   - 302 /contact?complete へリダイレクト
```

---

## 3. ContactForm バリデーション

| フィールド | データ型 | ルール |
|---|---|---|
| `inquiryType` | String | `@NotBlank` |
| `lastName`, `firstName` | String | `@NotBlank`, 50文字以内 |
| `email` | String | `@Email`, `@NotBlank` |
| `message` | String | `@NotBlank`, 1000文字以内 |
| `productCode` | String | 任意 |

---

## 4. 備考

- 問い合わせ受付後の自動返信メールは本バージョンでは実装なし
- 問い合わせ内容は `inquiries` テーブルに保存するのみ
