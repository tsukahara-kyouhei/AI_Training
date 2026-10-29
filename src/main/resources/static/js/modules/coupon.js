(() => {
  const form = document.querySelector("#coupon-apply-form");
  const removeForm = document.querySelector("#coupon-remove-form");
  if (!form) return;

  const error = document.querySelector("#coupon-error");
  const input = document.querySelector("#coupon-code");

  const showError = (message) => {
    if (error) error.textContent = message || "";
  };

  const send = async (method, code) => {
    const csrf = form.querySelector('input[name="_csrf"]');
    const headers = { Accept: "application/json" };
    if (csrf) headers["X-CSRF-TOKEN"] = csrf.value;
    if (code !== undefined) headers["Content-Type"] = "application/json";
    const response = await fetch(form.action, {
      method,
      headers,
      body: code === undefined ? undefined : JSON.stringify({ coupon_code: code }),
      credentials: "same-origin"
    });
    const body = await response.json();
    if (!response.ok || !body.success) {
      showError(body.message || "クーポンを処理できませんでした。");
      return;
    }
    window.location.reload();
  };

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    showError("");
    send("POST", input?.value || "").catch(() => showError("クーポンを処理できませんでした。"));
  });

  removeForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    showError("");
    send("DELETE").catch(() => showError("クーポンを解除できませんでした。"));
  });

})();
