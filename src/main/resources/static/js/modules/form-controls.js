/**
 * フォーム入力補助の共通UIを扱うモジュール。
 *
 * 現在はパスワード表示切替だけを持つが、入力補助系の軽量な処理をここへ集約する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * パスワード表示切替チェックボックスを初期化する。
   */
  app.initPasswordToggles = () => {
    document.querySelectorAll("[data-password-toggle]").forEach((checkbox) => {
      checkbox.addEventListener("change", () => {
        const targetId = checkbox.getAttribute("data-target");
        if (!targetId) {
          return;
        }
        const input = document.getElementById(targetId);
        if (!input) {
          return;
        }
        input.setAttribute("type", checkbox.checked ? "text" : "password");
      });
    });
  };
})();
