# FEAT-001 リポジトリ詳細設計書

| 項目         | 内容                                                  |
| ------------ | ----------------------------------------------------- |
| Issue ID     | FEAT-001                                              |
| 作成日       | 2026-04-27                                            |
| 対象クラス   | `jp.co.skig.officeorder.repository.ProductRepository` |
| 対象メソッド | `buildSearchParams()`                                 |

---

## 1. 変更概要

| 対象                  | 変更種別     | 内容                                                   |
| --------------------- | ------------ | ------------------------------------------------------ |
| `buildSearchParams()` | 修正         | `keyword` パラメータを新規追加し、`keywordLike` と分離 |
| `buildSearchParams()` | 修正         | `hasTasteSearchFilter` フラグを新規追加                |
| `toKeywordLike()`     | 削除（廃止） | `buildSearchParams()` 内にインライン化するため不要に   |

---

## 2. `buildSearchParams()` の変更

### 2-1. keyword / keywordLike の分離

#### 現状

```java
params.put("keywordLike", toKeywordLike(condition.keyword()));
```

`toKeywordLike()` は `%keyword%` を返す。商品コード検索も `ILIKE #{keywordLike}` で中間一致していた。

#### 変更後

```java
// NFKC 正規化は Controller 済み。ここでは trim のみ行う。
String kw = (condition.keyword() == null || condition.keyword().isBlank())
    ? null
    : condition.keyword().trim();
params.put("keyword",     kw);
params.put("keywordLike", kw == null ? null : "%" + kw + "%");
```

| パラメータ名  | 値                  | 用途                                                                               |
| ------------- | ------------------- | ---------------------------------------------------------------------------------- |
| `keyword`     | trim 済みの生値     | 商品コードの完全一致・前方一致（`LOWER(pvk.product_code) = LOWER(#{keyword})` 等） |
| `keywordLike` | `%` + keyword + `%` | 商品名・バリエーション名・説明文の部分一致（ILIKE）                                |

> `toKeywordLike()` プライベートメソッドは上記変更後に不要となるため削除する。

### 2-2. `hasTasteSearchFilter` フラグの追加

#### 追加コード

```java
// 既存フラグ（hasDeskFilter, hasChairFilter, hasStorageFilter）の直下に追加
boolean hasTasteSearchFilter = normalizedCategoryId == null
    && (!deskTasteIds.isEmpty() || !chairTasteIds.isEmpty() || !storageTasteIds.isEmpty());

params.put("hasTasteSearchFilter", hasTasteSearchFilter);
```

#### フラグの条件

| 条件                                                           | `hasTasteSearchFilter` |
| -------------------------------------------------------------- | ---------------------- |
| `categoryId == null`（検索ページ）かつテイストID が1つ以上選択 | `true`                 |
| `categoryId != null`（カテゴリページ）                         | `false`                |
| `categoryId == null` かつテイストID が全て未選択               | `false`                |

#### 既存フラグとの使い分け

| フラグ                 | 有効になる条件                            | 用途                                                |
| ---------------------- | ----------------------------------------- | --------------------------------------------------- |
| `hasDeskFilter`        | `categoryId == 'desk'` かつ属性ID あり    | デスクカテゴリページのフィルタ                      |
| `hasChairFilter`       | `categoryId == 'chair'` かつ属性ID あり   | チェアカテゴリページのフィルタ                      |
| `hasStorageFilter`     | `categoryId == 'storage'` かつ属性ID あり | 収納カテゴリページのフィルタ                        |
| `hasTasteSearchFilter` | `categoryId == null` かつテイストID あり  | **検索ページ専用**のクロスカテゴリ テイストフィルタ |

> カテゴリページのテイスト絞り込み（例: デスク一覧でのテイスト選択）は引き続き `hasDeskFilter` が担う。
> `hasTasteSearchFilter` は検索ページ（categoryId = null）に限定して有効になる。

---

## 3. `buildSearchParams()` 全体の変更差分

```java
private Map<String, Object> buildSearchParams(ProductSearchCondition condition) {
    // ... 既存のコード（変更なし） ...

    // ↓ 変更前
    // params.put("keywordLike", toKeywordLike(condition.keyword()));

    // ↓ 変更後
    String kw = (condition.keyword() == null || condition.keyword().isBlank())
        ? null
        : condition.keyword().trim();
    params.put("keyword",     kw);                                              // 新規
    params.put("keywordLike", kw == null ? null : "%" + kw + "%");

    // ... 既存のコード（変更なし） ...

    // ↓ 追加（既存の hasStorageFilter の直下）
    boolean hasTasteSearchFilter = normalizedCategoryId == null
        && (!deskTasteIds.isEmpty() || !chairTasteIds.isEmpty() || !storageTasteIds.isEmpty());
    params.put("hasTasteSearchFilter", hasTasteSearchFilter);                   // 新規

    // ... 以降既存のコード（変更なし） ...
    return params;
}
```

---

## 4. `toKeywordLike()` の削除

```java
// ↓ このメソッドを削除
private String toKeywordLike(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return null;
    }
    return "%" + keyword.trim() + "%";
}
```

ロジックは `buildSearchParams()` 内にインライン化されるため不要となる。

---

## 5. SQL との対応（参照: FEAT-001-sql-design.md）

`buildSearchParams()` で設定するパラメータと SQL フラグメントの対応関係:

| パラメータ名           | 参照する SQL フラグメント                         |
| ---------------------- | ------------------------------------------------- |
| `keyword`              | `BaseProductWhere` — 商品コード完全一致・前方一致 |
| `keywordLike`          | `BaseProductWhere` — ILIKE 部分一致               |
| `hasTasteSearchFilter` | `TasteSearchFilter`（新規フラグメント）           |
