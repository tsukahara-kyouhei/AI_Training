package jp.co.skig.officeorder.web.review;

import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.service.review.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // コンストラクタでServiceを読み込む
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ① レビュー一覧画面を表示する (GETリクエスト)
    @GetMapping
    public String showReviewList(@PathVariable Long productId,
            @AuthenticationPrincipal MemberPrincipal principal,
            Model model) {
        // レビュー一覧と集計情報を取得
        var reviewPage = reviewService.getProductReviews(productId, 10, 0);
        var reviewSummary = reviewService.getProductReviewSummary(productId);

        // モデルに追加
        reviewPage.setProductId(productId);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("reviewSummary", reviewSummary);

        // ▼▼ ここから書き換え：購入判定と投稿済み判定 ▼▼
        boolean hasPurchased = false;
        boolean hasReviewed = false;
        boolean isLoggedIn = false;
        Long currentMemberId = null;

        // ログインしている場合のみ判定処理を行う
        if (principal != null) {
            isLoggedIn = true;
            currentMemberId = principal.getMemberId();
            hasPurchased = reviewService.hasPurchasedProduct(currentMemberId, productId);
            hasReviewed = reviewService.getMemberReview(currentMemberId, productId).isPresent();
        }

        // 画面（Thymeleaf）に判定結果のフラグを渡す
        model.addAttribute("hasPurchased", hasPurchased);
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("currentMemberId", currentMemberId);
        // ▲▲ ここまで書き換え ▲▲

        return "review/list";
    }

    // ② レビュー投稿フォームを表示する (GETリクエスト)
    @GetMapping("/new")
    public String showReviewForm(@PathVariable Long productId,
            @AuthenticationPrincipal MemberPrincipal principal,
            Model model) {
        Long memberId = principal.getMemberId();

        // 【関所1】購入したことがあるかチェック
        if (!reviewService.hasPurchasedProduct(memberId, productId)) {
            // 購入していない場合は、強制的に一覧画面へ戻す
            return "redirect:/products/" + productId + "/reviews";
        }

        // 【関所2】すでにレビュー投稿済みかチェック（1人1件の制限）
        if (reviewService.getMemberReview(memberId, productId).isPresent()) {
            // 投稿済みの場合は、強制的に編集画面へ飛ばす
            return "redirect:/products/" + productId + "/reviews/edit?memberId=" + memberId;
        }

        // 新しいレビュー入力フォームを作成
        ReviewForm form = new ReviewForm();
        form.setProductId(productId);
        model.addAttribute("reviewForm", form);
        return "review/form";
    }

    // ③ レビューを投稿する (POSTリクエスト)
    @PostMapping
    public String submitReview(@PathVariable Long productId,
            @ModelAttribute ReviewForm form,
            @RequestParam Long memberId) {
        // 会員がその商品を購入したことがあるか判定
        if (!reviewService.hasPurchasedProduct(memberId, productId)) {
            throw new IllegalStateException("この商品は購入していないため、レビューを投稿できません。");
        }

        // レビューを新規登録または更新する
        reviewService.saveOrUpdateReview(memberId, form);
        return "redirect:/products/" + productId + "/reviews";
    }

    // ② レビュー入力画面を表示する (GETリクエスト)
    @GetMapping("/edit")
    public String showEditReviewForm(@PathVariable Long productId,
            @RequestParam Long memberId,
            Model model) {
        // 既存のレビューを取得
        var existingReview = reviewService.getMemberReview(memberId, productId);
        if (existingReview.isEmpty()) {
            throw new IllegalStateException("この商品に対するレビューは存在しません。");
        }

        // 既存のレビューをフォームにセット
        ReviewForm form = new ReviewForm();
        form.setProductId(productId);
        form.setRating(existingReview.get().getRating());
        form.setTitle(existingReview.get().getTitle());
        form.setBody(existingReview.get().getBody());
        model.addAttribute("reviewForm", form);
        return "review/form";
    }

    // ③ レビューを保存して一覧画面にリダイレクトする (POSTリクエスト)
    @PostMapping("/edit")
    public String submitEditReview(@PathVariable Long productId,
            @ModelAttribute ReviewForm form,
            @RequestParam Long memberId) {
        // 会員がその商品を購入したことがあるか判定
        if (!reviewService.hasPurchasedProduct(memberId, productId)) {
            throw new IllegalStateException("この商品は購入していないため、レビューを投稿できません。");
        }

        // レビューを新規登録または更新する
        reviewService.saveOrUpdateReview(memberId, form);
        return "redirect:/products/" + productId + "/reviews";
    }

}