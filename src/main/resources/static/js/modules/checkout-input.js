/**
 * 注文情報入力画面の登録済みお届け先反映を扱うモジュール。
 *
 * 会員が選択した追加お届け先の data 属性を、入力フォーム各項目へ転記する。
 */
(() => {
  const app = window.OfficeOrderSite || (window.OfficeOrderSite = {});

  /**
   * 真偽値ラジオの片方を選択状態へ切り替える。
   *
   * @param {string} name ラジオグループ名
   * @param {string} value 選択する値
   */
  const setRadioValue = (name, value) => {
    document.querySelectorAll(`input[name="${name}"]`).forEach((radio) => {
      radio.checked = radio.value === value;
    });
  };

  /**
   * 注文情報入力画面の追加お届け先選択を初期化する。
   */
  app.initCheckoutAddressSelector = () => {
    const selector = document.getElementById("selectedAdditionalAddressId");
    if (!selector) {
      return;
    }

    const applySelectedAddress = () => {
      const option = selector.options[selector.selectedIndex];
      if (!option || !option.value) {
        return;
      }

      const postal = option.dataset.postal || "";
      if (postal.length === 7) {
        const postalCodePart1 = document.getElementById("postalCodePart1");
        const postalCodePart2 = document.getElementById("postalCodePart2");
        if (postalCodePart1) {
          postalCodePart1.value = postal.substring(0, 3);
        }
        if (postalCodePart2) {
          postalCodePart2.value = postal.substring(3);
        }
      }

      const prefecture = document.getElementById("prefecture");
      if (prefecture) {
        prefecture.value = option.dataset.prefecture || "";
      }

      const city = document.getElementById("city");
      if (city) {
        city.value = option.dataset.city || "";
      }

      const addressLine = document.getElementById("addressLine");
      if (addressLine) {
        addressLine.value = option.dataset.addressLine || "";
      }

      const deliveryFloor = document.getElementById("deliveryFloor");
      if (deliveryFloor) {
        deliveryFloor.value = option.dataset.floor || "";
      }

      const daytimePhone = document.getElementById("daytimePhone");
      if (daytimePhone) {
        daytimePhone.value = option.dataset.daytimePhone || "";
      }

      const fax = document.getElementById("fax");
      if (fax) {
        fax.value = option.dataset.fax || "";
      }

      if (option.dataset.elevator === "true") {
        setRadioValue("hasElevator", "true");
      } else if (option.dataset.elevator === "false") {
        setRadioValue("hasElevator", "false");
      }
    };

    selector.addEventListener("change", applySelectedAddress);
  };
})();
