/**
 * 売れ筋ランキングなどの横スクロール表示を制御するモジュール。
 *
 * 画面幅に応じた表示件数を求め、前後ボタンでトラック位置を更新する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /** カード間の固定ギャップ。CSS と計算を揃えるため JS 側でも保持する。 */
  const gap = 14;

  /**
   * 現在の画面幅に対する1画面あたりの表示件数を返す。
   *
   * @return {number} 表示件数
   */
  const visibleCount = () => {
    const width = window.innerWidth;
    if (width <= 767) {
      return 1;
    }
    if (width <= 1023) {
      return 2;
    }
    return 4;
  };

  /**
   * data-carousel を持つ領域へカルーセル操作を紐付ける。
   */
  app.initCarousels = () => {
    document.querySelectorAll("[data-carousel]").forEach((root) => {
      const track = root.querySelector("[data-carousel-track]");
      const items = Array.from(root.querySelectorAll("[data-carousel-item]"));
      const prev = root.querySelector("[data-carousel-prev]");
      const next = root.querySelector("[data-carousel-next]");
      if (!track || items.length === 0 || !prev || !next) {
        return;
      }

      let index = 0;

      const maxIndex = () => Math.max(0, items.length - visibleCount());

      const render = () => {
        const itemWidth = items[0].getBoundingClientRect().width;
        const x = (itemWidth + gap) * index;
        track.style.transform = `translateX(${-x}px)`;
        const visible = visibleCount();
        const canScroll = items.length > visible;
        prev.hidden = !canScroll;
        next.hidden = !canScroll;
        prev.disabled = index <= 0;
        next.disabled = index >= maxIndex();
      };

      prev.addEventListener("click", () => {
        index = Math.max(0, index - visibleCount());
        render();
      });

      next.addEventListener("click", () => {
        index = Math.min(maxIndex(), index + visibleCount());
        render();
      });

      window.addEventListener("resize", () => {
        index = Math.min(index, maxIndex());
        render();
      });

      render();
    });
  };
})();
