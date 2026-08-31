/**
 * 商品詳細画面のギャラリーとカラー選択を制御するモジュール。
 *
 * サーバー描画済みの data 属性を基に、画像・価格・在庫・購入対象SKUの表示を同期する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * data 属性から取り出した数値を安全に数値化する。
   *
   * @param {string | null} value data 属性の値
   * @return {number} 不正値時は 0
   */
  const parseNumber = (value) => {
    const num = Number(value);
    return Number.isFinite(num) ? num : 0;
  };

  /**
   * 金額を画面表示用の円表記へ整形する。
   *
   * @param {number} value 金額
   * @return {string} 3桁区切りの文字列
   */
  const formatYen = (value) => {
    const rounded = Math.max(0, Math.floor(value));
    return new Intl.NumberFormat("ja-JP").format(rounded);
  };

  /**
   * 組立・設置費の表示文言を決定する。
   *
   * @param {number} assemblyFee 組立・設置費
   * @return {string} 画面表示用文言
   */
  const toAssemblyText = (assemblyFee) => {
    return assemblyFee > 0 ? `+${formatYen(assemblyFee)}円(税込)` : "お届けのみ";
  };

  /**
   * 商品詳細画像ギャラリーのメイン画像切替を初期化する。
   */
  app.initProductGalleries = () => {
    document.querySelectorAll(".pd-gallery").forEach((gallery) => {
      const mainImage = gallery.querySelector("[data-pd-main-image]");
      const subItems = Array.from(gallery.querySelectorAll(".pd-sub-item[data-image-src]"));
      if (!mainImage || subItems.length === 0) {
        return;
      }

      const prevButton = gallery.querySelector(".pd-main-nav--prev");
      const nextButton = gallery.querySelector(".pd-main-nav--next");
      let currentIndex = 0;

      const applyByIndex = (index) => {
        currentIndex = index;
        const selected = subItems[currentIndex];
        if (!selected) {
          return;
        }

        const src = selected.getAttribute("data-image-src") ?? "";
        const alt = selected.getAttribute("data-image-alt") ?? "";
        if (src) {
          mainImage.setAttribute("src", src);
        }
        if (alt) {
          mainImage.setAttribute("alt", alt);
        }

        subItems.forEach((item, itemIndex) => {
          item.classList.toggle("is-selected", itemIndex === currentIndex);
        });
      };

      subItems.forEach((item, index) => {
        item.addEventListener("click", () => applyByIndex(index));
      });

      if (prevButton) {
        prevButton.addEventListener("click", () => {
          applyByIndex((currentIndex - 1 + subItems.length) % subItems.length);
        });
      }

      if (nextButton) {
        nextButton.addEventListener("click", () => {
          applyByIndex((currentIndex + 1) % subItems.length);
        });
      }

      const initialIndex = subItems.findIndex((item) => item.classList.contains("is-selected"));
      applyByIndex(initialIndex >= 0 ? initialIndex : 0);
    });
  };

  /**
   * カラー選択に合わせて価格・在庫・購入対象SKU表示を切り替える。
   *
   * 色カード選択時に、右パネルの表示と hidden 項目を同じSKUへ同期する。
   */
  app.initProductVariantSelectors = () => {
    document.querySelectorAll(".pd-summary").forEach((summary) => {
      const cards = Array.from(summary.querySelectorAll(".pd-color-card[data-pd-variant-card]"));
      if (cards.length === 0) {
        return;
      }

      const taxRate = parseNumber(summary.getAttribute("data-pd-tax-rate"));
      const assemblyFee = parseNumber(summary.getAttribute("data-pd-assembly-fee"));
      const assemblyFeeText = toAssemblyText(assemblyFee);
      const productName = summary.getAttribute("data-pd-product-name") || "商品";

      const priceIncludingTax = summary.querySelector("[data-pd-price-incl]");
      const priceExcludingTax = summary.querySelector("[data-pd-price-excl]");
      const assemblyText = summary.querySelector("[data-pd-assembly-text]");
      const buyImage = summary.querySelector("[data-pd-buy-image]");
      const buyColor = summary.querySelector("[data-pd-buy-color]");
      const buyCode = summary.querySelector("[data-pd-buy-code]");
      const buyStock = summary.querySelector("[data-pd-buy-stock]");
      const buyPriceIncludingTax = summary.querySelector("[data-pd-buy-price-incl]");
      const buyAssembly = summary.querySelector("[data-pd-buy-assembly]");
      const cartVariantId = summary.querySelector("[data-pd-cart-variant-id]");
      const cartButton = summary.querySelector("[data-pd-cart-btn]");
      const cartButtonText = summary.querySelector("[data-pd-cart-btn-text]");

      const applyCard = (selectedCard) => {
        cards.forEach((card) => {
          const isSelected = card === selectedCard;
          card.classList.toggle("is-selected", isSelected);
          const input = card.querySelector('input[type="radio"]');
          if (input) {
            input.checked = isSelected;
          }
        });

        const variantId = selectedCard.getAttribute("data-variant-id") || "";
        const productCode = selectedCard.getAttribute("data-product-code") || "";
        const colorName = selectedCard.getAttribute("data-color-name") || "";
        const unitPrice = parseNumber(selectedCard.getAttribute("data-unit-price"));
        const unitPriceText = selectedCard.getAttribute("data-unit-price-text") || formatYen(unitPrice);
        const unitPriceExcludingTaxText = selectedCard.getAttribute("data-unit-price-excl-text") || formatYen(Math.floor(unitPrice / (1 + taxRate / 100)));
        const stockQuantity = Math.max(0, Math.floor(parseNumber(selectedCard.getAttribute("data-stock-quantity"))));
        const outOfStock = stockQuantity <= 0;

        if (priceIncludingTax) {
          priceIncludingTax.textContent = unitPriceText;
        }
        if (priceExcludingTax) {
          priceExcludingTax.textContent = `${unitPriceExcludingTaxText} 円`;
        }
        if (assemblyText) {
          assemblyText.textContent = assemblyFeeText;
        }
        if (buyImage && productCode) {
          buyImage.setAttribute("src", `/images/products/${productCode}.png`);
          buyImage.setAttribute("alt", `${productName} - ${colorName}`);
        }
        if (buyColor) {
          buyColor.textContent = colorName;
        }
        if (buyCode) {
          buyCode.textContent = productCode;
        }
        if (buyStock) {
          buyStock.textContent = String(stockQuantity);
        }
        if (buyPriceIncludingTax) {
          buyPriceIncludingTax.textContent = `${priceWithTaxText}円`;
        }
        if (buyAssembly) {
          buyAssembly.textContent = assemblyFeeText;
        }
        if (cartVariantId) {
          cartVariantId.value = variantId;
        }
        if (cartButton) {
          cartButton.disabled = outOfStock;
        }
        if (cartButtonText) {
          cartButtonText.textContent = outOfStock ? "入荷待ち" : "カートに入れる";
        }
      };

      cards.forEach((card) => {
        card.addEventListener("click", () => applyCard(card));
        const input = card.querySelector('input[type="radio"]');
        if (input) {
          input.addEventListener("change", () => applyCard(card));
        }
      });

      applyCard(cards.find((card) => card.classList.contains("is-selected")) || cards[0]);
    });
  };
})();
