# エラーページ

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-ERR |
| Controller | `AppErrorController`（Spring Bootの`ErrorController`実装） |
| 認証 | 不要 |
| コンテントネゴシエーション | AcceptヘッダーによりHTML/JSONを切り替え |

---

## 2. 4xxエラーページ

| 項目 | 内容 |
|---|---|
| テンプレート | `error/error.html` |
| 対象 | 404 Not Found、その他4xxエラー |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `statusCode` | `int` | HTTPステータスコード |
| `errorTitle` | `String` | エラータイトル（`error.page.notFound.title` または `error.page.requestError.title`） |
| `errorMessage` | `String` | エラーメッセージ（`error.page.notFound.message` または `error.page.requestError.message`） |

### 画面構成

- ステータスコード表示
- エラータイトル・メッセージ
- トップページへのリンク（`/`）

---

## 3. 5xxエラーページ

| 項目 | 内容 |
|---|---|
| テンプレート | `error/500.html` |
| 対象 | 500 Internal Server Error、その他5xxエラー |

### Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `statusCode` | `int` | HTTPステータスコード |
| `errorTitle` | `String` | エラータイトル（`error.page.serverError.title`） |
| `errorMessage` | `String` | エラーメッセージ（`error.page.serverError.message`） |
| `requestId` | `String` | リクエストID（`RequestIdMdcFilter` から取得） |
| `errorPath` | `String` | エラー発生元URL |

### 画面構成

- ステータスコード表示
- エラータイトル・メッセージ
- リクエストID表示（お問い合わせ時の参照用）
- エラー発生元パス表示
- トップページリンク（`/`）+ お問い合わせリンク（`/contact`）

---

## 4. JSONレスポンス

Acceptヘッダーが `application/json` の場合、HTMLの代わりにJSONを返却。

```json
{
  "status": 404,
  "code": "NOT_FOUND",
  "message": "タイトル: メッセージ",
  "requestId": "abc123",
  "path": "/original/path"
}
```

`code` 値: `NOT_FOUND` / `REQUEST_ERROR` / `SERVER_ERROR`
