# テストデータ定義

## 1. 概要

テストデータは `sql/seed/test-data/` 以下に管理する。

---

## 2. テストデータファイル一覧

| ファイル | 内容 |
|---|---|
| `members.sql` | テスト用会員（5名程度） |
| `products.sql` | テスト用商品（デスク/チェア/収納各カテゴリ） |
| `orders.sql` | テスト用注文データ |
| `content.sql` | テスト用お知らせコンテンツ |
| `post-seed-check.sql` | データ投入後の整合性確認用SELECT |

---

## 3. テスト会員定義

| 会員ID | メール | パスワード | 機能 |
|---|---|---|---|
| 1 | `test-general@example.com` | `Password1!` | 一般会員 |
| 2 | `test-corporate@example.com` | `Password1!` | 法人会員 |
| 3 | `test-nofavorite@example.com` | `Password1!` | お気に入りなし |
| 4 | `test-withdrawn@example.com` | （退会済み） | 退会済み会員（ログイン不可） |

---

## 4. テスト商品定義

| 商品ID | 商品名 | カテゴリ | 状態 |
|---|---|---|---|
| 101 | テストデスクア | desk | 在庫あり |
| 102 | テストデスクイ（在庫なし） | desk | 在庫0 |
| 201 | テストチェアア | chair | 在庫あり |
| 301 | テスト収納棚ア | storage | 在庫あり |

---

## 5. テスト注文定義

| 注文番号 | 会員ID | ステータス | 内容 |
|---|---|---|---|
| 20240101-0001 | 1 | received | 通常注文 |
| 20240102-0001 | 1 | shipped | 発送済み注文 |
| 20240103-0001 | 2 | cancelled | キャンセル注文 |

---

## 6. テストデータ投入手順

```powershell
# テストデータ投入（ローカル）
psql -h localhost -U postgres -d office_order -f sql/seed/test-data/members.sql
psql -h localhost -U postgres -d office_order -f sql/seed/test-data/products.sql
psql -h localhost -U postgres -d office_order -f sql/seed/test-data/orders.sql
psql -h localhost -U postgres -d office_order -f sql/seed/test-data/content.sql

# 整合性確認
psql -h localhost -U postgres -d office_order -f sql/seed/test-data/post-seed-check.sql
```
