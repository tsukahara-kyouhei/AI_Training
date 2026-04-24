package jp.co.skig.officeorder.web;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewListResponse;
import jp.co.skig.officeorder.service.review.ReviewService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

/**
 * レビュー投稿/更新/削除/一覧取得エンドポイント。
 */
@Controller
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final MemberSessionService memberSessionService;

    public ReviewController(ReviewService reviewService,
                            MemberSessionService memberSessionService) {
        this.reviewService = reviewService;
        this.memberSessionService = memberSessionService;
    }

    /**
     * もっと見る（JSON）。
     */
    @GetMapping("/products/{productId}/reviews")
    @ResponseBody
    public ReviewListResponse moreReviews(@PathVariable long productId,
                                           @RequestParam(name = "page", defaultValue = "1") int page,
                                           @RequestParam(name = "size", defaultValue = "5") int size) {
        return reviewService.findReviews(productId, page, size);
    }

    /**
     * レビュー投稿。
     */
    @PostMapping("/products/{productId}/reviews")
    public String postReview(@PathVariable long productId,
                              @Validated @ModelAttribute("reviewForm") ReviewForm reviewForm,
                              BindingResult bindingResult,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String redirectPath = "/products/" + productId;

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        if (member == null) {
            log.info("event={} productId={} reason=not_authenticated",
                    LogEvent.REVIEW_REDIRECT_LOGIN.value(), productId);
            String encoded = UriUtils.encodeQueryParam(redirectPath, StandardCharsets.UTF_8);
            return "redirect:/login?redirect=" + encoded;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reviewFormError", true);
            redirectAttributes.addFlashAttribute("reviewForm", reviewForm);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm", bindingResult);
            return "redirect:" + redirectPath;
        }

        reviewService.postReview(member.memberId(), productId, reviewForm);
        redirectAttributes.addFlashAttribute("reviewMessage", "レビューを投稿しました。");
        return "redirect:" + redirectPath;
    }

    /**
     * レビュー更新。
     */
    @PostMapping("/products/{productId}/reviews/{reviewId}")
    public String updateReview(@PathVariable long productId,
                                @PathVariable long reviewId,
                                @Validated @ModelAttribute("reviewForm") ReviewForm reviewForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String redirectPath = "/products/" + productId;

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        if (member == null) {
            String encoded = UriUtils.encodeQueryParam(redirectPath, StandardCharsets.UTF_8);
            return "redirect:/login?redirect=" + encoded;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reviewFormError", true);
            redirectAttributes.addFlashAttribute("reviewForm", reviewForm);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm", bindingResult);
            return "redirect:" + redirectPath;
        }

        reviewService.updateReview(member.memberId(), reviewId, reviewForm);
        redirectAttributes.addFlashAttribute("reviewMessage", "レビューを更新しました。");
        return "redirect:" + redirectPath;
    }

    /**
     * レビュー削除。
     */
    @PostMapping("/products/{productId}/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable long productId,
                                @PathVariable long reviewId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String redirectPath = "/products/" + productId;

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        if (member == null) {
            String encoded = UriUtils.encodeQueryParam(redirectPath, StandardCharsets.UTF_8);
            return "redirect:/login?redirect=" + encoded;
        }

        reviewService.deleteReview(member.memberId(), reviewId);
        redirectAttributes.addFlashAttribute("reviewMessage", "レビューを削除しました。");
        return "redirect:" + redirectPath;
    }
}
