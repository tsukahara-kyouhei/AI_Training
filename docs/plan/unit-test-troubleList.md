# 単体テスト実装 障害一覧

| ドキュメントバージョン | 1.0 |
|---|---|
| 作成日 | 2026-04-04 |
| 作成者 | 大橋 |
| 対象フェーズ | サービス層 単体テスト実装 |

---

## 概要

`docs/plan/unit-test-plan.md` をもとにサービスパッケージ全体の単体テストを実装した際に発生した障害を記録する。  
各障害について、発生事象・根本原因・修正内容・再発防止策を示す。

---

## 障害一覧

| No. | 分類 | 影響ファイル | 事象 | ステータス |
|---|---|---|---|---|
| T-001 | コンパイルエラー | `NotificationMailServiceTest.java` | Java 予約語 `null` をメソッド名に使用 | 解決済み |
| T-002 | コンパイルエラー | `ProductListSearchServiceTest.java` | 演算子 `+` をメソッド名に使用 | 解決済み |
| T-003 | コンパイルエラー | `ContactServiceTest.java` | 存在しないセッタ `setContactMessage` を呼び出し | 解決済み |
| T-004 | コンパイルエラー | `ProductListSearchServiceTest.java` | `ProductCardView` コンストラクタの引数数不一致 | 解決済み |
| T-005 | テスト失敗 | `CartServiceTest.java` | 空カートの送料に対するアサーション誤り | 解決済み |
| T-006 | テストエラー | `CartServiceTest.java` | Mockito UnnecessaryStubbingException | 解決済み |
| T-007 | テストエラー | `ProductListSearchServiceTest.java` | Mockito PotentialStubbingProblem | 解決済み |
| T-008 | コンパイルエラー | `MemberServiceTest.java` | `@BeforeEach` アノテーションの記述漏れ・フィールド名誤り | 解決済み |

---

## 詳細

---

### T-001 Java 予約語 `null` をメソッド名に使用

**影響ファイル**  
`src/test/java/.../service/mail/NotificationMailServiceTest.java`

**事象**  
以下のメソッド名を付けたところコンパイルエラーが発生した。

```java
void null payloadを渡すと送信せずreturnすること() { ... }
//   ^^^^ 予約語
```

エラー内容:
```
<identifier>がありません / 無効なメソッド宣言です。戻り値の型が必要です。
```

**根本原因**  
Java のメソッド識別子には予約語（`null`, `true`, `false`, `class`, `void` 等）を含めることができない。  
日本語テストメソッド名の書き方に慣れており、`null` がキーワードである意識が薄かった。

**修正内容**  
メソッド名の `null` を削除し、意味が同等の表現に変更した。

```java
// 修正前
void null payloadを渡すと送信せずreturnすること()

// 修正後
void nullのpayloadを渡すと送信せずreturnすること()
```

**再発防止策**  
- 日本語テストメソッド名を記述する際も、Java 識別子として禁止されている英単語を含まないよう確認する。  
- 主な禁止単語例: `null`, `true`, `false`, `class`, `void`, `return`, `new`, `this`  
- コーディングガイドラインに追記する。

---

### T-002 演算子 `+` をメソッド名に使用

**影響ファイル**  
`src/test/java/.../service/product/ProductListSearchServiceTest.java`

**事象**  
以下のメソッド名を付けたところコンパイルエラーが発生した。

```java
void 総件数がサイズ+1の場合は2ページが返ること() { ... }
//              ^ 演算子
```

エラー内容:
```
'('がありません / 無効なメソッド宣言です。戻り値の型が必要です。
```

**根本原因**  
Java のメソッド識別子には演算子（`+`, `-`, `*`, `/`, `>`, `<` 等）を含めることができない。  
数値の差分を説明するつもりで自然言語的に `+1` と書いたが、Java の字句解析の対象になった。

**修正内容**  
演算子を含まない表現に変更した。

```java
// 修正前
void 総件数がサイズ+1の場合は2ページが返ること()

// 修正後
void 総件数がサイズPlusOneの場合は2ページが返ること()
```

**再発防止策**  
- テストメソッド名に算術演算子・比較演算子・記号（`+`, `-`, `<`, `>`, `=`, `*`, `%` 等）を使わない。  
- 差分や比較を表す場合は `PlusN`, `GreaterThan`, `LessThan`, `以上`, `未満` などの自然言語表現を使う。  
- コーディングガイドラインに追記する。

---

### T-003 存在しないセッタ `setContactMessage` を呼び出し

**影響ファイル**  
`src/test/java/.../service/contact/ContactServiceTest.java`

**事象**  
テストヘルパメソッドでフォームを組み立てる際に以下のメソッドを呼び出したが、コンパイルエラーが発生した。

```java
form.setContactMessage("テストメッセージ");
// シンボルを見つけられません
```

**根本原因**  
`ContactForm` のフィールド名は `message`（セッタ: `setMessage()`）だが、テスト実装時にフィールド名を `contactMessage` と誤認した。  
プロダクションコードを読まずにフィールド名を推測したことが原因。

**修正内容**  
実際のセッタ名に修正した。

```java
// 修正前
form.setContactMessage("テストメッセージ");

// 修正後
form.setMessage("テストメッセージ");
```

**再発防止策**  
- テスト実装前に対象クラスのフィールド定義・getter/setter を必ず確認する。  
- フォームクラスは `@NotBlank` 等のバリデーションアノテーションを持つフィールドが多く、フィールド名とプレフィックスが一致しないケースがある。  
- IDE の補完機能を積極的に活用する（推測でタイプしない）。

---

### T-004 `ProductCardView` コンストラクタの引数数不一致

**影響ファイル**  
`src/test/java/.../service/product/ProductListSearchServiceTest.java`

**事象**  
テストデータ生成で `ProductCardView` を 9 引数で生成しようとしたが、コンパイルエラーが発生した。

```java
new ProductCardView(1L, "P001", "テスト", null, null, null, null, null, null)
// The constructor ProductCardView(...) is undefined
```

**根本原因**  
`ProductCardView` の実際のコンストラクタは 7 フィールドのレコードだが、誤って 9 フィールドと想定した。

```java
// 実際の定義
public record ProductCardView(
    long productId,
    String productName,
    String priceText,
    List<String> colorCodes,
    String productCode,
    boolean inStock,
    String detailUrl
) {}
```

**修正内容**  
正しい引数構成に修正した。

```java
new ProductCardView(1L, "テスト商品", "¥1,000", List.of(), "P001", true, "/products/1")
```

**再発防止策**  
- テストデータ生成でモデルクラスを直接 `new` するときは、クラス定義を事前に確認する。  
- record クラスの仕様（コンストラクタ引数 = フィールド宣言順）を理解して正確な引数を渡す。  
- 引数数が多いモデルクラスにはビルダーやファクトリメソッドの採用を検討する。

---

### T-005 空カートの送料に対するアサーション誤り

**影響ファイル**  
`src/test/java/.../service/cart/CartServiceTest.java`

**事象**  
`Cookieが空の場合に空カートが返ること` テストで送料が 0 であることをアサーションしたが、テストが失敗した。

```
expected: 0
 but was: 800
```

**根本原因**  
`CartService` の送料計算ロジックは「税込商品小計が 5,000 円以上なら送料無料、未満なら一律 800 円」である。  
空カートの場合、商品小計は 0 円（＜ 5,000 円）なので送料 800 円が発生する。これは正しい仕様の動作だが、  
「カートが空なら送料も 0」という誤った前提でアサーションを書いてしまった。

**修正内容**  
テストの目的を「空カートであること」の確認に絞り、送料アサーションを削除して `productSubtotal` が 0 であることの確認に変更した。

```java
// 修正前
assertThat(cart.summary().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);

// 修正後
assertThat(cart.isEmpty()).isTrue();
assertThat(cart.totalQuantity()).isEqualTo(0);
assertThat(cart.summary().productSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
```

**再発防止策**  
- テスト実装前に対象メソッドの計算ロジックを必ず読み、境界値の仕様を把握する。  
- テストケース設計書（またはコメント）に期待値の根拠を明記する。  
- 「空 = 0」という思い込みを排除し、ロジックベースで期待値を導く。

---

### T-006 Mockito UnnecessaryStubbingException

**影響ファイル**  
`src/test/java/.../service/cart/CartServiceTest.java`

**事象**  
`在庫ゼロの商品を追加するとIllegalArgumentExceptionがスローされること` テストで `UnnecessaryStubbingException` が発生した。

```
Unnecessary stubbings detected.
  1. -> cartCookieStore.load(request) の stub はここで設定されましたが、実行されませんでした
```

**根本原因**  
`CartService.addItem()` の処理順序は次の通りである。

1. `productVariantId <= 0` チェック
2. `quantity` 範囲チェック
3. **`findSnapshot()` → `stockQuantity <= 0` チェック**  ← ここで例外スロー
4. `loadCurrentCartItems(request)` → `cartCookieStore.load(request)` 呼び出し

在庫ゼロのテストでは 3 番で例外がスローされるため、4 番の `cartCookieStore.load` は呼ばれない。  
しかしテストで `when(cartCookieStore.load(request)).thenReturn(List.of())` を定義していたため、Mockito の strict 検証に引っかかった。

**修正内容**  
`cartCookieStore.load` の stub を削除した。

```java
// 修正前
when(cartCookieStore.load(request)).thenReturn(List.of());  // ← 削除
when(cartRepository.findProductSnapshotsByVariantIds(...)).thenReturn(...);

// 修正後
when(cartRepository.findProductSnapshotsByVariantIds(...)).thenReturn(...);
```

**再発防止策**  
- テスト対象メソッドの処理フローを把握し、テストシナリオで呼ばれる mock メソッドのみ stub を定義する。  
- `@ExtendWith(MockitoExtension.class)` は デフォルトで `STRICT_STUBS` 設定であり、未使用の stub はエラーになる。  
- 「とりあえず stub を設定する」実装パターンを避ける。

---

### T-007 Mockito PotentialStubbingProblem

**影響ファイル**  
`src/test/java/.../service/product/ProductListSearchServiceTest.java`

**事象**  
`buildConditionでpriceBandIds正規化が呼ばれること` テストで `PotentialStubbingProblem` が発生した。

```
this invocation of 'buildCondition' method:
    productService.buildCondition(...)
was stubbed with DIFFERENT arguments
```

**根本原因**  
`ProductListSearchService.buildCondition()` が内部で呼ぶ `productService.buildCondition()` は 10 引数の overload である。  
テスト内で 9 引数版のシグネチャで stub を定義したため、実際の呼び出しと引数が一致しなかった。  
Mockito は呼び出しシグネチャが一致しない stub を "潜在的な問題" として扱う。

**修正内容**  
`productService.buildCondition` の stub を削除し（戻り値が不要なため）、`lenient()` により `productFilterOptionService` の stub が厳格検証対象から外れるよう修正した。

```java
// 修正後
org.mockito.Mockito.lenient().when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
org.mockito.Mockito.lenient().when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
// ...
```

**再発防止策**  
- stub を定義する前に、対象メソッドのシグネチャ（引数数・型）を確認する。  
- overload の多いメソッドの stub は特に注意し、実際に呼ばれる overload を特定してから stub を書く。  
- テストで検証したい内容（verify）に必要な mock のみを stub し、余分な stub は持ち込まない。

---

### T-008 `@BeforeEach` アノテーション記述漏れ・フォームフィールド名誤り

**影響ファイル**  
`src/test/java/.../service/member/MemberServiceTest.java`

**事象 (1)** `@BeforeEach` アノテーション記述漏れ  
`@BeforeEach` が文字列リテラルとして書かれており、アノテーションとして機能していなかった。  
セットアップが実行されず、`NullPointerException` が発生した。

**事象 (2)** フォームフィールド名の誤り  
`buildRegisterForm()` ヘルパで `setAddressLine1` を呼び出したが、`MemberRegisterForm` のフィールド名は `addressLine` であった。コンパイルエラーが発生した。

**事象 (3)** 必須フィールドの設定漏れ  
`MemberRegisterForm` の必須フィールド（`anniversaryDate`, `deliveryFloor`, `hasElevator`, `daytimePhone`, `newsletterOptIn`）の設定が漏れており、テストが正常に動作しなかった。

**根本原因**  
プロダクションコードのフィールド定義を事前に確認せず、フィールド名や必須項目を推測で記述した。  
特にフォームクラスは必須フィールドが多く、画面仕様にもとづいた項目が存在するため、定義の確認が不可欠であった。

**修正内容**

```java
// @BeforeEach 修正
@BeforeEach         // ← アノテーションとして正しく記述
void init() {
    setUp();
}

// フィールド名修正
form.setAddressLine("千代田1-1");  // setAddressLine1 → setAddressLine

// 必須フィールド追加
form.setAnniversaryDate(java.time.LocalDate.of(1990, 1, 1));
form.setDeliveryFloor("1");
form.setHasElevator(Boolean.TRUE);
form.setDaytimePhone("0312345678");
form.setNewsletterOptIn(Boolean.TRUE);
```

**再発防止策**  
- テストヘルパでフォームクラスを組み立てる際は、必ずプロダクションコードのフィールド定義を読んでセッタ名と必須項目を確認する。  
- フォームクラスのバリデーションアノテーション（`@NotNull`, `@NotBlank`）の付与フィールドを確認して必須項目を網羅する。  
- テストを組立てた後、コンパイル・テスト実行を段階的に行い、早期に誤りを検出する。

---

## 再発防止チェックリスト（テスト実装時）

テスト実装前後に以下を確認する。

### 実装前
- [ ] テスト対象クラスのソースコードを読み、メソッドの処理フロー・呼び出し順序を把握しているか
- [ ] フォーム・モデルクラスを使う場合、実際のフィールド名（getter/setter）を確認しているか
- [ ] record クラスを使う場合、フィールド数と宣言順序を確認しているか
- [ ] mock する必要があるのはどのメソッド呼び出しかを明確にしているか
- [ ] テストメソッド名に Java 識別子として使えない文字・単語が含まれていないか（予約語・演算子）

### 実装後
- [ ] `mvnw test-compile` でコンパイルエラーがないことを確認しているか
- [ ] 各テストの期待値がプロダクションコードの仕様・ロジックから正しく導かれているか
- [ ] スタブ定義が実際に呼ばれるメソッド・引数と一致しているか
- [ ] UnnecessaryStubbingException が発生しないか（`mvnw test` で確認）

---

## 障害統計

| 分類 | 件数 |
|---|---|
| コンパイルエラー | 5 |
| テスト失敗 (Assertion) | 1 |
| テストエラー (Mockito) | 2 |
| **合計** | **8** |

| 根本原因 | 件数 |
|---|---|
| ソースコード未確認（フィールド名・引数推測） | 4 |
| Java 識別子ルールの見落とし | 2 |
| 仕様誤認（ビジネスロジックの理解不足） | 1 |
| Mock の処理フロー把握不足 | 1 |
