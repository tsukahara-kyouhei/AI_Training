# トップページ

## 1. 基本情報

| 項目 | 内容 |
|---|---|
| 画面ID | SCR-TOP |
| テンプレート | `pages/top.html` |
| URL | `GET /` |
| Controller | `HomeController#top()` |
| 認証 | 不要 |

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| ヘッダー | 共通ヘッダー（カテゴリナビ・検索・カート・ログイン） |
| お知らせバー | `notice-bar` フラグメント（5秒ローテーション） |
| 新着商品セクション | カルーセル形式で商品カードを表示 |
| 売れ筋ランキングセクション | ランキング付き商品カード |
| フッター | 共通フッター（最近見た商品・リンク） |

---

## 3. Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `topNewArrivals` | `List<ProductCardView>` | 新着商品リスト |
| `hasTopNewArrivals` | `boolean` | 新着商品があるか |
| `topRankedProducts` | `List<RankedProductCardView>` | 売れ筋ランキング商品 |
| `headerAnnouncements` | `List<AnnouncementView>` | お知らせバー用データ（`AuthModelAdvice` で全画面共通設定） |

---

## 4. フラグメント

| フラグメント | 用途 |
|---|---|
| `fragments/layout/header :: header` | 共通ヘッダー |
| `fragments/layout/notice-bar :: noticeBar` | お知らせバー |
| `fragments/common/product-card :: productCardByView` | 新着商品カード |
| `fragments/common/product-card :: rankedProductCardByView` | ランキング商品カード |
| `fragments/layout/footer :: footer` | 共通フッター |

---

## 5. 表示条件

- 新着商品が0件の場合、新着セクションを非表示
- お知らせが0件の場合、お知らせバーを非表示
