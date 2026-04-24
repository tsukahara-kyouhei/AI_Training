/**
 * レビュー機能の JavaScript モジュール。
 * - フォーム展開/非表示切替
 * - 星評価クリック選択
 * - 本文の文字数カウンター
 * - 「もっと見る」の非同期取得
 * - 削除確認ダイアログ
 */
document.addEventListener('DOMContentLoaded', function () {
    initReviewToggle();
    initStarSelect();
    initCharCounter();
    initMoreReviews();
    initDeleteConfirm();
});

/** フォーム展開/非表示切替 */
function initReviewToggle() {
    var toggleBtns = document.querySelectorAll('[data-review-toggle]');
    var cancelBtns = document.querySelectorAll('[data-review-cancel]');
    var formSection = document.getElementById('review-form-section');
    if (!formSection) return;

    toggleBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            formSection.style.display = formSection.style.display === 'none' ? 'block' : 'none';
        });
    });
    cancelBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            formSection.style.display = 'none';
        });
    });

    // バリデーションエラー時は自動展開
    if (formSection.querySelector('.field-error')) {
        formSection.style.display = 'block';
    }
}

/** 星評価クリック選択 */
function initStarSelect() {
    document.querySelectorAll('[data-star-select]').forEach(function (container) {
        var stars = container.querySelectorAll('.star-select__star');
        var hiddenInput = container.parentElement.querySelector('input[type="hidden"]');
        stars.forEach(function (star) {
            star.addEventListener('click', function () {
                var value = parseInt(star.getAttribute('data-value'), 10);
                if (hiddenInput) hiddenInput.value = value;
                stars.forEach(function (s) {
                    var sv = parseInt(s.getAttribute('data-value'), 10);
                    if (sv <= value) {
                        s.classList.add('is-selected');
                    } else {
                        s.classList.remove('is-selected');
                    }
                });
            });
        });
    });
}

/** 本文の文字数カウンター */
function initCharCounter() {
    document.querySelectorAll('[data-review-body]').forEach(function (textarea) {
        var counter = textarea.parentElement.querySelector('[data-review-counter]');
        if (!counter) return;
        function update() {
            counter.textContent = textarea.value.length + '/1000文字';
        }
        textarea.addEventListener('input', update);
        update();
    });
}

/** もっと見る */
function initMoreReviews() {
    var moreBtn = document.querySelector('[data-review-more]');
    if (!moreBtn) return;
    var productId = moreBtn.getAttribute('data-product-id');
    var page = 2;
    var size = 5;

    moreBtn.addEventListener('click', function () {
        moreBtn.disabled = true;
        fetch('/products/' + productId + '/reviews?page=' + page + '&size=' + size, {
            headers: { 'Accept': 'application/json' }
        })
        .then(function (res) { return res.json(); })
        .then(function (data) {
            var list = document.querySelector('[data-review-list]');
            if (!list) return;
            data.items.forEach(function (review) {
                var item = document.createElement('div');
                item.className = 'review-item';
                var stars = '';
                for (var i = 1; i <= 5; i++) {
                    stars += i <= review.rating ? '★' : '☆';
                }
                var dateStr = review.createdAt ? review.createdAt.substring(0, 10) : '';
                var titleHtml = review.title ? '<p class="review-item__title">' + escapeHtml(review.title) + '</p>' : '';
                item.innerHTML =
                    '<div class="review-item__header">' +
                        '<span class="review-stars">' + stars + '</span>' +
                        '<span class="review-item__date">' + dateStr + '</span>' +
                    '</div>' +
                    titleHtml +
                    '<p class="review-item__body">' + escapeHtml(review.body) + '</p>';
                list.appendChild(item);
            });
            page++;
            moreBtn.disabled = false;
            if (!data.hasNext) {
                moreBtn.style.display = 'none';
            }
        })
        .catch(function () {
            moreBtn.disabled = false;
        });
    });
}

/** 削除確認 */
function initDeleteConfirm() {
    document.querySelectorAll('[data-review-delete-form]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!confirm('このレビューを削除してもよろしいですか？')) {
                e.preventDefault();
            }
        });
    });
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}
