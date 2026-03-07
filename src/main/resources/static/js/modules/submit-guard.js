/**
 * 二重送信防止用の送信ボタン制御を行うモジュール。
 *
 * 同一フォームの submit 開始後に、送信対象ボタンをまとめて非活性化する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * data-once-submit を持つボタンへ、初回 submit 後の非活性化処理を紐付ける。
   */
  app.initOnceSubmitButtons = () => {
    document.querySelectorAll("[data-once-submit]").forEach((button) => {
      const form = button.closest("form");
      if (!form || form.dataset.onceSubmitBound === "true") {
        return;
      }

      form.dataset.onceSubmitBound = "true";
      form.addEventListener("submit", () => {
        form.querySelectorAll("[data-once-submit]").forEach((submitButton) => {
          submitButton.disabled = true;
        });
      });
    });
  };
})();
