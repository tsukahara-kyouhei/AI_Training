/**
 * data 属性で定義したモーダル開閉を制御するモジュール。
 *
 * 画面ごとの個別実装を増やさず、共通の open/close トリガでモーダルの表示状態を切り替える。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * モーダルの開閉トリガと Escape キーによる閉じる操作を初期化する。
   */
  app.initModalToggles = () => {
    const openButtons = document.querySelectorAll("[data-modal-open]");
    openButtons.forEach((button) => {
      button.addEventListener("click", () => {
        const id = button.getAttribute("data-modal-open");
        const modal = id ? document.getElementById(id) : null;
        if (!modal) {
          return;
        }
        modal.removeAttribute("hidden");
        modal.setAttribute("aria-hidden", "false");
      });
    });

    const closeButtons = document.querySelectorAll("[data-modal-close]");
    closeButtons.forEach((button) => {
      button.addEventListener("click", () => {
        const id = button.getAttribute("data-modal-close");
        const modal = id ? document.getElementById(id) : null;
        if (!modal) {
          return;
        }
        modal.setAttribute("hidden", "");
        modal.setAttribute("aria-hidden", "true");
      });
    });

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") {
        return;
      }
      document.querySelectorAll("[data-modal].is-open, [data-modal]:not([hidden])").forEach((modal) => {
        modal.setAttribute("hidden", "");
        modal.setAttribute("aria-hidden", "true");
      });
    });
  };
})();
