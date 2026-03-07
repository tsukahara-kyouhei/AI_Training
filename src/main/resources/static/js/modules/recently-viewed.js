/**
 * 最近見た商品を localStorage と API 応答で管理するモジュール。
 *
 * 商品詳細で閲覧履歴を記録し、フッターでは保存済み商品IDを使って最新表示を描画する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /** localStorage に保存するキー。 */
  const RECENTLY_VIEWED_KEY = "officeOrderRecentlyViewed";
  /** フッターへ表示する最大件数。 */
  const RECENTLY_VIEWED_LIMIT = 4;
  /** 閲覧履歴の保持期間。360日をミリ秒で保持する。 */
  const RECENTLY_VIEWED_TTL_MS = 360 * 24 * 60 * 60 * 1000;

  /**
   * localStorage から復元した配列を、件数・重複・期限の観点で正規化する。
   *
   * @param {unknown} raw localStorage から復元した値
   * @return {Array<{productId:number, viewedAt:number}>} 正規化済み履歴
   */
  const parseRecentEntries = (raw) => {
    if (!Array.isArray(raw)) {
      return [];
    }

    const now = Date.now();
    const result = [];
    for (const entry of raw) {
      const productId = Number(entry?.productId);
      const viewedAt = Number(entry?.viewedAt);
      if (!Number.isInteger(productId) || productId <= 0) {
        continue;
      }
      if (!Number.isFinite(viewedAt)) {
        continue;
      }
      if (now - viewedAt > RECENTLY_VIEWED_TTL_MS) {
        continue;
      }
      if (result.some((item) => item.productId === productId)) {
        continue;
      }

      result.push({ productId, viewedAt });
      if (result.length >= RECENTLY_VIEWED_LIMIT) {
        break;
      }
    }
    return result;
  };

  /**
   * localStorage から最近見た商品履歴を読み出す。
   *
   * 旧形式や期限切れデータが残っていた場合は、読み出し時に正規化して保存し直す。
   *
   * @return {Array<{productId:number, viewedAt:number}>} 履歴一覧
   */
  const loadRecentEntries = () => {
    try {
      const rawText = localStorage.getItem(RECENTLY_VIEWED_KEY);
      if (!rawText) {
        return [];
      }
      const parsed = JSON.parse(rawText);
      const normalized = parseRecentEntries(parsed);
      if (JSON.stringify(parsed) !== JSON.stringify(normalized)) {
        localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(normalized));
      }
      return normalized;
    } catch (_error) {
      return [];
    }
  };

  /**
   * 最近見た商品履歴を件数上限付きで保存する。
   *
   * @param {Array<{productId:number, viewedAt:number}>} entries 保存対象履歴
   */
  const saveRecentEntries = (entries) => {
    try {
      localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(entries.slice(0, RECENTLY_VIEWED_LIMIT)));
    } catch (_error) {
      // localStorage が利用できない場合は無視する。
    }
  };

  /**
   * API 応答から最近見た商品カードの DOM を組み立てる。
   *
   * @param {Object} item 商品一覧APIの1件分
   * @return {HTMLAnchorElement} 描画用カード要素
   */
  const createRecentlyViewedCard = (item) => {
    const anchor = document.createElement("a");
    anchor.className = "card";
    anchor.style.display = "block";
    anchor.href = item?.detailUrl || "#";

    if (item?.productCode) {
      const image = document.createElement("img");
      image.className = "product-card__image";
      image.src = `/images/products/${item.productCode}.png`;
      image.alt = item?.productName || "商品画像";
      image.loading = "lazy";
      anchor.appendChild(image);
    } else {
      const placeholder = document.createElement("div");
      placeholder.className = "ph-img";
      placeholder.setAttribute("aria-hidden", "true");
      anchor.appendChild(placeholder);
    }

    const pad = document.createElement("div");
    pad.className = "card__pad";

    const name = document.createElement("p");
    name.className = "product-name";
    name.textContent = item?.productName || "商品名未設定";
    pad.appendChild(name);

    const price = document.createElement("p");
    price.className = "product-price";
    const priceValue = document.createElement("span");
    priceValue.className = "price";
    priceValue.textContent = item?.priceText || "0";
    price.appendChild(priceValue);
    price.append("円（税込）");
    pad.appendChild(price);

    const chips = document.createElement("div");
    chips.className = "chips";
    chips.setAttribute("aria-label", "カラーラインナップ");
    const colorCodes = Array.isArray(item?.colorCodes) ? item.colorCodes : [];
    for (const colorCode of colorCodes) {
      const chip = document.createElement("span");
      chip.className = "chip";
      if (typeof colorCode === "string" && colorCode.trim().length > 0) {
        chip.style.background = colorCode;
      }
      chips.appendChild(chip);
    }
    pad.appendChild(chips);

    anchor.appendChild(pad);
    return anchor;
  };

  /**
   * 商品詳細画面で表示中の商品を最近見た商品履歴へ追加する。
   */
  app.trackCurrentProduct = () => {
    const target = document.querySelector("[data-recently-viewed-product-id]");
    if (!target) {
      return;
    }

    const productId = Number(target.getAttribute("data-recently-viewed-product-id"));
    if (!Number.isInteger(productId) || productId <= 0) {
      return;
    }

    const next = loadRecentEntries().filter((entry) => entry.productId !== productId);
    next.unshift({ productId, viewedAt: Date.now() });
    saveRecentEntries(next);
  };

  /**
   * フッターの最近見た商品領域を localStorage と API 応答から描画する。
   */
  app.renderRecentlyViewedFooter = async () => {
    const grid = document.querySelector("[data-recently-viewed-grid]");
    if (!grid) {
      return;
    }

    const empty = document.querySelector("[data-recently-viewed-empty]");
    const recentEntries = loadRecentEntries();
    if (recentEntries.length === 0) {
      grid.replaceChildren();
      if (empty) {
        empty.hidden = false;
      }
      return;
    }

    const ids = recentEntries.map((entry) => entry.productId).join(",");
    if (!ids) {
      grid.replaceChildren();
      if (empty) {
        empty.hidden = false;
      }
      return;
    }

    try {
      const response = await fetch(`/products/recently-viewed?ids=${encodeURIComponent(ids)}`, {
        method: "GET",
        headers: { Accept: "application/json" },
        credentials: "same-origin"
      });
      if (!response.ok) {
        throw new Error(`status=${response.status}`);
      }

      const products = await response.json();
      if (!Array.isArray(products) || products.length === 0) {
        grid.replaceChildren();
        if (empty) {
          empty.hidden = false;
        }
        return;
      }

      const fragment = document.createDocumentFragment();
      products.slice(0, RECENTLY_VIEWED_LIMIT).forEach((product) => {
        fragment.appendChild(createRecentlyViewedCard(product));
      });
      grid.replaceChildren(fragment);
      if (empty) {
        empty.hidden = true;
      }
    } catch (_error) {
      grid.replaceChildren();
      if (empty) {
        empty.hidden = false;
      }
    }
  };
})();
