# 会員サービス設計

## 1. クラス概要

| クラス | パッケージ | 責務 |
|---|---|---|
| `MemberService` | `service.member` | 会員登録・編集・退会・お気に入り・追加お届け先 |
| `MemberRepository` | `repository` | DBアクセス |

---

## 2. 定数

| 定数 | 値 | 説明 |
|---|:---:|---|
| `ADDITIONAL_ADDRESS_LIMIT` | 20 | 追加お届け先上限 |
| `FAVORITES_LIMIT` | 100 | お気に入り上限 |
| `MYPAGE_PAGE_SIZE` | 20 | 購入履歴の1ページ表示件数 |

---

## 3. 主要メソッド

### 3.1 register(form)

1. email重複確認 (`existsByEmail`) -> 重複時は `MemberEmailDuplicateException`
2. `BCrypt.encode(password)` でハッシュ生成
3. `insertMember(member)` でDB登録
4. トランザクションコミット後に登録完了メール送信

### 3.2 updateProfile(memberId, form)

- プロフィール編集（氏名・住所等）
- email変更は停止中（本バージョン）
- `@Transactional` でトランザクション管理

### 3.3 withdraw(memberId)

- `member_status = 'withdrawn'`, `withdrawn_at = now()` でUPDATE
- `member_favorites` を全削除
- Spring Securityのセッションを無効化しログアウト

### 3.4 findAdditionalAddresses(memberId)

- **上限:** `ADDITIONAL_ADDRESS_LIMIT = 20`
- **戻り値:** `List<MemberAdditionalAddressView>`

### 3.5 addAdditionalAddress(memberId, form)

1. 件数確認 (`countAdditionalAddresses >= 20`) -> 上限時はエラー
2. `insertAdditionalAddress()` でDB登録

### 3.6 addFavorite(memberId, productId)

1. 既にお気に入り登録済か確認 (`existsFavorite`) -> 登録済みは何もしない
2. 件数確認 (`countFavorites >= 100`) -> 上限時はエラー
3. `insertFavorite()` でDB登録

### 3.7 removeFavorite(memberId, productId)

- `deleteFavorite(memberId, productId)` でDB削除

---

## 4. 認証関連

`MemberUserDetailsService.loadUserByUsername(email)` が Spring Securityから呼び出される。

```
1. MemberRepository.findActiveCredentialByEmail(email)
2. レコードなし -> UsernameNotFoundException
3. member_status != 'active' -> MemberPrincipal.enabled = false
4. MemberPrincipal を返す
```
