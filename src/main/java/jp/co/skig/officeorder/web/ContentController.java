package jp.co.skig.officeorder.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 固定コンテンツページへの遷移を担当するController。
 */
@Controller
public class ContentController {

    /**
     * OFFICE ORDER についてページを表示する。
     *
     * @return 固定ページテンプレート
     */
    @GetMapping("/about")
    public String about() {
        return "pages/about";
    }

    /**
     * ご利用ガイドページを表示する。
     *
     * @return 固定ページテンプレート
     */
    @GetMapping("/guide")
    public String guide() {
        return "pages/guide";
    }

    /**
     * 利用規約ページを表示する。
     *
     * @return 固定ページテンプレート
     */
    @GetMapping("/legal/terms")
    public String legalTerms() {
        return "pages/legal-terms";
    }

    /**
     * プライバシーポリシーページを表示する。
     *
     * @return 固定ページテンプレート
     */
    @GetMapping("/legal/privacy")
    public String legalPrivacy() {
        return "pages/legal-privacy";
    }

    /**
     * 特定商取引法に基づく表記ページを表示する。
     *
     * @return 固定ページテンプレート
     */
    @GetMapping("/legal/tokusho")
    public String legalTokusho() {
        return "pages/legal-tokusho";
    }
}
