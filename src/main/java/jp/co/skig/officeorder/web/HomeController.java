package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * トップ画面とお知らせ一覧画面を担当するController。
 */
@Controller
public class HomeController {

    /** 商品表示系サービス。 */
    private final ProductService productService;
    /** お知らせ表示系サービス。 */
    private final AnnouncementService announcementService;

    /**
     * ホームControllerを生成する。
     *
     * @param productService      商品サービス
     * @param announcementService お知らせサービス
     */
    public HomeController(ProductService productService,
            AnnouncementService announcementService) {
        this.productService = productService;
        this.announcementService = announcementService;
    }

    /**
     * トップ画面を表示する。
     *
     * @param model 画面モデル
     * @return トップ画面
     */
    @GetMapping("/")
    public String top(Model model) {
        var topNewArrivals = productService.findTopNewArrivals();
        model.addAttribute("topNewArrivals", topNewArrivals);
        model.addAttribute("hasTopNewArrivals", !topNewArrivals.isEmpty());
        model.addAttribute("topRankedProducts", productService.findTopRankedProducts());
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
