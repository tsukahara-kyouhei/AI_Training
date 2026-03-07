/**
 * 画面共通 JavaScript の初期化エントリポイント。
 *
 * 分割済みモジュールが `window.OfficeOrderSite` に登録した初期化関数を、
 * DOM 準備完了後にまとめて起動する。
 */
(() => {
  const app = window.OfficeOrderSite || {};

  document.addEventListener("DOMContentLoaded", () => {
    app.initNoticeRotators?.();
    app.initModalToggles?.();
    app.initCarousels?.();
    app.initPasswordToggles?.();
    app.initOnceSubmitButtons?.();
    app.initProductGalleries?.();
    app.initProductVariantSelectors?.();
    app.initCheckoutAddressSelector?.();
    app.trackCurrentProduct?.();
    app.renderRecentlyViewedFooter?.();
  });
})();
