package jp.co.skig.officeorder.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.product.ProductReviewForm;
import jp.co.skig.officeorder.service.product.ProductReviewService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;

/**
 * レビュー投稿処理を担当するController。
 */
@Controller
@RequestMapping("/products/{productId}/reviews")
public class ProductReviewController {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewController.class);

    private final ProductReviewService productReviewService;
    private final MemberSessionService memberSessionService;

    public ProductReviewController(ProductReviewService productReviewService,
                                   MemberSessionService memberSessionService) {
        this.productReviewService = productReviewService;
        this.memberSessionService = memberSessionService;
    }

    /**
     * レビュー投稿を受け付ける。
     */
    @PostMapping
    public String createReview(
            @ModelAttribute ProductReviewForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        if (member == null) {
            return "redirect:/login";
        }

        try {
            productReviewService.createReview(member.memberId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "レビューを投稿しました。");
        } catch (IllegalStateException e) {
            log.warn("レビュー投稿失敗: memberId={}, productId={}, reason={}", 
                    member.memberId(), form.productId(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/products/" + form.productId();
    }
}