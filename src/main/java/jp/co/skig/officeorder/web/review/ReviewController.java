package jp.co.skig.officeorder.web.review;

import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.service.review.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String showReviewList(@PathVariable Long productId, Model model) {
        // レビュー一覧と集計情報を取得
        var reviewPage = reviewService.getProductReviews(productId, 10, 0);
        var reviewSummary = reviewService.getProductReviewSummary(productId);

        // モデルに追加
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("reviewSummary", reviewSummary);
        return "review/list";
    }

    // ② レビュー投稿フォームを表示する (GETリクエスト)
    @GetMapping("/new")
    public String showReviewForm(@PathVariable Long productId, Model model) {
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