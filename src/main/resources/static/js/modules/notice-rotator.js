/**
 * ヘッダ下のお知らせ表示を順番に切り替えるモジュール。
 *
 * テンプレートが埋め込んだ data 属性を読み取り、一定間隔でタイトルと掲載日を更新する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * 1つのお知らせ表示領域へローテーション処理を紐付ける。
   *
   * @param {Element} root お知らせ表示ルート要素
   */
  const startNoticeRotator = (root) => {
    const items = Array.from(root.querySelectorAll("[data-notice-item]"));
    const target = root.querySelector("[data-notice-text]");
    const targetDate = root.querySelector("[data-notice-date]");
    if (!items.length || !target) {
      return;
    }

    let index = 0;
    let timerId = null;
    let paused = false;

    const render = () => {
      const item = items[index % items.length];
      const title = item.getAttribute("data-notice-title") ?? "";
      const date = item.getAttribute("data-notice-date") ?? "";
      target.textContent = title;
      if (targetDate) {
        targetDate.textContent = date;
      }
    };

    const tick = () => {
      if (paused) {
        return;
      }
      index = (index + 1) % items.length;
      render();
    };

    render();
    timerId = window.setInterval(tick, 5000);

    root.addEventListener("mouseenter", () => {
      paused = true;
    });
    root.addEventListener("mouseleave", () => {
      paused = false;
    });
    window.addEventListener("beforeunload", () => timerId && window.clearInterval(timerId), { once: true });
  };

  /**
   * 画面内のお知らせ表示領域を走査してローテーションを初期化する。
   */
  app.initNoticeRotators = () => {
    document.querySelectorAll("[data-notice-rotator]").forEach((root) => startNoticeRotator(root));
  };
})();
