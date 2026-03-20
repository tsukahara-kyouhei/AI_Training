# エラーハンドリング設計書

## 1. 概要

Spring Bootの例外処理機構（`@ControllerAdvice`, `ErrorController`）を利用して、  
統一的なエラー処理とユーザーフレンドリーなエラー表示を実現する。

---

## 2. エラーページ設計

| HTTPステータス | テンプレート | 表示内容 |
|---|---|---|
| 404 Not Found | `templates/error/error.html` | 「ページが見つかりません」 |
| 500 Internal Server Error | `templates/error/500.html` | 「システムエラーが発生しました」 |
| その他4xx/5xx | `templates/error/error.html` | 汎用エラーメッセージ |

**実装クラス:** `AppErrorController`

---

## 3. 例外の種類と処理方針

### 3.1 ビジネス例外（想定内エラー）

| 例外クラス | 発生ケース | 処理方針 |
|---|---|---|
| `MemberEmailDuplicateException` | 会員登録時にメールアドレス重複 | 画面にバリデーションエラーとして表示 |
| `MemberWithdrawnException` | 退会済み会員がログイン試行 | ログイン失敗メッセージ表示 |
| `ProductNotFoundException` | 存在しない商品IDへのアクセス | 404エラーページへ |
| `StockShortageException` | 注文確定時に在庫不足 | チェックアウト画面にエラー表示 |
| `CartItemNotFoundException` | 存在しないカートアイテム操作 | カート画面にエラー表示 |
| `BatchJobNotFoundException` | 存在しないバッチジョブ名指定 | バッチ管理画面にエラー表示 |

### 3.2 システム例外（想定外エラー）

| 例外クラス | 発生ケース | 処理方針 |
|---|---|---|
| `DataAccessException` | DBアクセス失敗 | 500エラーページ + ログ出力 |
| `MailException` | メール送信失敗 | リトライ後に失敗ログ（注文処理は完了扱い） |
| `RuntimeException` | 想定外システムエラー | 500エラーページ + ログ出力 |

---

## 4. GlobalExceptionLoggingAdvice

`@ControllerAdvice` により全コントローラーの例外をインターセプト。

```
処理フロー:
1. 例外発生
2. GlobalExceptionLoggingAdvice がキャッチ
3. MDCのリクエストIDと共にERRORログ出力
4. ビジネス例外 → 画面にエラーを返す
5. システム例外 → 500エラーページへリダイレクト
```

---

## 5. バリデーションエラー処理

| 項目 | 内容 |
|---|---|
| バリデーション実装 | Bean Validation（`@NotBlank`, `@Email`, `@Size`, `@Pattern`等） |
| BindingResult | コントローラーで `BindingResult` を受け取り、エラーがあれば入力画面に返す |
| エラーメッセージ | `messages.properties` に定義 |
| 表示 | Thymeleafの `th:errors` でフィールドごとにエラーメッセージ表示 |

---

## 6. ログ設計

### 6.1 ログレベル方針

| レベル | 使用場面 |
|---|---|
| ERROR | システム例外・ビジネス例外のスタックトレース |
| WARN | リトライ発生時（メール送信等） |
| INFO | リクエスト開始/終了、バッチ実行開始/終了 |
| DEBUG | SQL実行、詳細処理（開発時のみ有効化） |

### 6.2 MDC（Mapped Diagnostic Context）

`RequestIdFilter` がリクエストごとにUUIDを生成しMDCに設定。

| MDCキー | 値 | 設定箇所 |
|---|---|---|
| `requestId` | UUID | RequestIdFilter |
| `memberId` | ログイン会員ID | MdcLoggingInterceptor |
| `uri` | リクエストURI | MdcLoggingInterceptor |

logback-spring.xmlのパターン: `[%X{requestId}] [memberId=%X{memberId}] %msg`

### 6.3 ログファイル

| ファイル | 内容 | ローテート |
|---|---|---|
| `log/app.log` | アプリ全般ログ | 日次、30日保持 |
| `log/error.log` | ERRORログのみ | 日次、90日保持 |

---

## 7. エラーメッセージ管理

エラーメッセージは `messages.properties` で一元管理。

```properties
# バリデーション
error.required={0}は必須です。
error.email=メールアドレスの形式が正しくありません。
error.postal_code=郵便番号は7桁の数字で入力してください。

# ビジネスエラー
error.member.email.duplicate=このメールアドレスは既に登録されています。
error.stock.shortage=在庫が不足しています。カートを確認してください。
```
