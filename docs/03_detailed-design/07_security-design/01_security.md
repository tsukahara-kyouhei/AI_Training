# セキュリティ設計書

## 1. 概要

Spring Securityを利用した認証・認可を実装する。設定クラスは `SecurityConfig`。

---

## 2. 認証設計

### 2.1 認証方式

| 項目 | 内容 |
|---|---|
| 認証方式 | フォーム認証（メールアドレス + パスワード） |
| パスワード暗号化 | BCryptPasswordEncoder |
| UserDetails実装 | `MemberPrincipal` |
| UserDetailsService実装 | `MemberUserDetailsService` |
| セッションタイムアウト | 60分 |

### 2.2 認証フロー

```
1. ユーザーがログイン画面(/login)にPOST
2. Spring SecurityがMemberUserDetailsServiceを呼び出す
3. membersテーブルからemailで会員を検索
4. member_status = 'active' でなければ認証失敗
5. BCrypt.matches(入力PW, password_hash) で検証
6. 成功時: MemberPrincipal をセッションに格納
7. 失敗時: /login?error にリダイレクト
```

### 2.3 認証後の処理

| イベント | 処理 |
|---|---|
| ログイン成功 | セッション固定化攻撃対策のためセッション再生成 |
| ログアウト | セッション無効化、/login にリダイレクト |
| セッション期限切れ | /login にリダイレクト |

---

## 3. 認可設計

### 3.1 URLアクセス制御

| URLパターン | 認証要否 | ロール |
|---|:---:|---|
| `/` | 不要 | 全員 |
| `/products/**` | 不要 | 全員 |
| `/categories/**` | 不要 | 全員 |
| `/cart/**` | 不要 | 全員（ゲストもカート利用可） |
| `/checkout/**` | 不要 | 全員（ゲスト購入可） |
| `/login` | 不要 | 全員 |
| `/members/register/**` | 不要 | 全員 |
| `/contact` | 不要 | 全員 |
| `/about`, `/guide`, `/legal/**` | 不要 | 全員 |
| `/announcements` | 不要 | 全員 |
| `/mypage/**` | **必要** | `ROLE_MEMBER` |
| `/internal/**` | **必要** | `ROLE_INTERNAL_API` |

### 3.2 有効会員インターセプター

`MemberActiveValidationInterceptor` がリクエスト前に実行され、  
セッション中の会員の `member_status` を毎回DBから確認する。  
`withdrawn` になっている場合は強制ログアウト → `/login` へリダイレクト。

---

## 4. CSRF対策

| 設定 | 内容 |
|---|---|
| デフォルト | Spring SecurityのCSRFトークンを全POSTに適用 |
| 除外パス | `/internal/batch/**`（内部API、ロールで保護） |
| Thymeleaf連携 | `th:action` 使用で自動でCSRFトークンが付与される |

---

## 5. パスワード管理

| 項目 | 内容 |
|---|---|
| アルゴリズム | BCrypt |
| ストレッチング | BCryptのデフォルト（コスト10）を使用 |
| 保存形式 | `$2a$10$...` 形式のハッシュ値のみ保存 |
| パスワード変更 | 本バージョンではマイページからの変更機能なし |

---

## 6. セッション管理

| 項目 | 内容 |
|---|---|
| セッションストア | サーバーサイドHTTPセッション |
| タイムアウト | 60分（application.yml: `server.servlet.session.timeout`） |
| セッション固定化対策 | ログイン成功時にセッションID再生成（`changeSessionId()` 戦略） |
| 同時ログイン | 制限なし |

---

## 7. オープンリダイレクト対策

`AuthRedirectUtils` にてリダイレクトURLの検証を実施。  
- 相対パスのみ許可  
- 外部ドメインへのリダイレクトを拒否  
- ログイン後の遷移先（`?redirect=` パラメータ）も同様に検証

---

## 8. ゲストカートのセキュリティ

- カートはCookieにシリアライズして保存（`CartCookieStore`）
- Cookie属性: `HttpOnly=true`, `SameSite=Lax`
- ゲストが会員ログイン後: CookieカートをDBカートにマージ

---

## 9. ログ・マスキング

`LogMaskingUtils` により、ログ出力時に以下のフィールドをマスク:  
- `password`, `password_hash`, `passwordHash`
- カード情報（本システムでは非対応）

---

## 10. OWASP Top 10 対応状況

| 脅威 | 対策状況 |
|---|---|
| A01: アクセス制御の不備 | Spring Security認可 + インターセプター |
| A02: 暗号化の失敗 | BCryptパスワードハッシュ |
| A03: インジェクション | MyBatisパラメータバインド（SQLインジェクション防止）, Thymeleaf自動エスケープ（XSS防止） |
| A04: 安全でない設計 | 会員退会時のstatusフラグ管理 |
| A05: セキュリティ設定ミス | application.yml管理、ローカルと本番で設定分離 |
| A07: 認証の失敗 | セッション固定化対策、タイムアウト管理 |
| A08: ソフトウェア整合性の失敗 | CSRFトークン検証 |
| A10: SSRF | 外部URLリクエスト機能なし |
