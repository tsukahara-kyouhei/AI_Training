package jp.co.skig.officeorder.web;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.service.product.RecommendationService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * トップ画面とお知らせ一覧画面を担当するController。
 */
@Controller
public class HomeController {

    /** 商品表示系サービス。 */
    private final ProductService productService;
    /** お知らせ表示系サービス。 */
    private final AnnouncementService announcementService;
    /** 会員セッションサービス。 */
    private final MemberSessionService memberSessionService;
    /** パーソナライズ推薦サービス。 */
    private final RecommendationService recommendationService;

    /**
     * ホームControllerを生成する。
     *
     * @param productService 商品サービス
     * @param announcementService お知らせサービス
     * @param memberSessionService 会員セッションサービス
     * @param recommendationService パーソナライズ推薦サービス
     */
    public HomeController(ProductService productService,
                          AnnouncementService announcementService,
                          MemberSessionService memberSessionService,
                          RecommendationService recommendationService) {
        this.productService = productService;
        this.announcementService = announcementService;
        this.memberSessionService = memberSessionService;
        this.recommendationService = recommendationService;
    }

    /**
     * トップ画面を表示する。
     *
     * @param session HTTPセッション
     * @param model 画面モデル
     * @return トップ画面
     */
    @GetMapping("/")
    public String top(HttpSession session, Model model) {
        var topNewArrivals = productService.findTopNewArrivals();
        model.addAttribute("topNewArrivals", topNewArrivals);
        model.addAttribute("hasTopNewArrivals", !topNewArrivals.isEmpty());
        model.addAttribute("topRankedProducts", productService.findTopRankedProducts());

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        @SuppressWarnings("unchecked")
        List<Long> recentlyViewedIds = (List<Long>) session.getAttribute(
                CatalogController.RECENTLY_VIEWED_SESSION_KEY);

        boolean hasRecentlyViewed = recentlyViewedIds != null && !recentlyViewedIds.isEmpty();
        if (member != null || hasRecentlyViewed) {
            Long memberId = member != null ? member.memberId() : null;
            List<ProductCardView> recommended = recommendationService.findTopPersonalizedRecommendations(
                    memberId, recentlyViewedIds);
            model.addAttribute("topPersonalizedRecommendations", recommended);
            model.addAttribute("hasTopPersonalizedRecommendations", !recommended.isEmpty());
        } else {
            model.addAttribute("topPersonalizedRecommendations", List.of());
            model.addAttribute("hasTopPersonalizedRecommendations", false);
        }

        return "pages/top";
    }

    /**
     * お知らせ一覧画面を表示する。
     *
     * @param model 画面モデル
     * @return お知らせ一覧画面
     */
    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("announcements", announcementService.findAnnouncementList());
        return "pages/announcements";
    }
}
