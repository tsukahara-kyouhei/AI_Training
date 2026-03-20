# ログ設計書

## 1. 概要

`logback-spring.xml` で定義。2系統でSpring Profileを切り替える。

---

## 2. ログ復写

| ファイル | 内容 | ローテーション |
|---|---|---|
| `log/app.log` | INFO以上の全ログ | 日次・最大30日保持 |
| `log/error.log` | ERRORレベルのみ | 日次・最大90日保持 |
| 標準出力 | ローカル開発時のConsole出力 | - |

---

## 3. MDC（Mapped Diagnostic Context）

### 3.1 設定クラス

| クラス | 設定MDCキー | タイミング |
|---|---|---|
| `RequestIdFilter` | `requestId` (UUID) | リクエスト入口 |
| `MdcLoggingInterceptor` | `memberId`, `uri` | Controller帏前 |

### 3.2 ログパターン

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId}] [memberId=%X{memberId}] [%X{uri}] %logger{36} - %msg%n
```

---

## 4. ログレベル方針

| レベル | 使用場面 | 例 |
|---|---|---|
| ERROR | 想定外例外・システム障害 | DB接続失敗、メール送信失敗 |
| WARN | ビジネスエラー・リトライ | メール再試行、在庫不足 |
| INFO | 平常操作ログ | リクエスト開始/終了、バッチ実行 |
| DEBUG | 開発時の詳細ログ | SQL実行、内部状態 |

---

## 5. マスキング

`LogMaskingUtils` にてログ出力時に以下のフィールドをマスク:

- `password`, `passwordHash`, `password_hash`
- エンドポイントリクエストパラメータの `password` キー

---

## 6. バッチ処理ログ

| イベント | レベル | 内容 |
|---|---|---|
| ジョブ開始 | INFO | `Starting job: {jobName} at {time}` |
| ジョブ完了 | INFO | `Completed job: {jobName} status={status} duration={ms}ms` |
| ジョブ失敗 | ERROR | `Failed job: {jobName}` + スタックトレース |
