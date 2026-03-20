# 会員登録通知メール

## 1. 概要

| 項目 | 内容 |
|---|---|
| メールID | MAIL-001 |
| 送信トリガー | 会員登録完了（`MemberService.register()`） |
| To | 登録メールアドレス |
| テンプレート | `mail/templates/member-registration-body.txt` |
| 件名 | `mail/subjects.properties` の `mail.subject.member-registration` |

---

## 2. テンプレート変数

| 変数名 | 内容 |
|---|---|
| `lastName` | 姓 |
| `firstName` | 名 |
| `email` | メールアドレス |
| `registeredAt` | 登録日時 |

---

## 3. メール本文構成イメージ

```
件名: 【office-order】会員登録完了のお知らせ

{lastName} {firstName} 様

会員登録を完了しました。

登録日時: {registeredAt}
メールアドレス: {email}

このメールは自動送信されました。
返信はお受けできません。
```
