# メール一覧

## 1. 概要

`NotificationMailService` がメール送信を一元管理する。  
`MailTemplateRenderer` がテキストテンプレートをロード・変数展開する。  
メール送信は不実時性以上にトランザクション整合性を優先し、DBコミット後に送信。

---

## 2. メール一覧

| ID | 機能 | 送信トリガー | テンプレートパス |
|---|---|---|---|
| MAIL-001 | 会員登録完了通知 | 会員登録POSTのコミット後 | `mail/templates/member-registration-body.txt` |
| MAIL-002 | 注文完了通知 | 注文確定のコミット後 | `mail/templates/order-complete-body.txt` |

---

## 3. メール送信機構

```
1. Service層が NotificationMailService.send*() を呼び出す
2. NotificationMailServiceが TransactionSynchronizationManagerに
   afterCommitリスナーを登録
3. DBトランザクションコミット後にリスナーが実行されメール送信
4. 送信失敗時: MailRetryConfigのTaskSchedulerで再試行
   - 再試行間隔: 10分
   - 最大再試行回数: 3回
```

---

## 4. 共通項目

| 項目 | 内容 |
|---|---|
| Fromアドレス | `spring.mail.from`（application.yml） |
| エンコーディング | UTF-8 |
| テンプレート形式 | プレーンテキスト |
| ローカルテスト | Mailpit（localhost:1025）で受信確認 |
