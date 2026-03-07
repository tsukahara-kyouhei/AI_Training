package jp.co.skig.officeorder.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ローカル開発時にエラーページを手動確認するための Controller。
 */
@Profile("local")
@Controller
@RequestMapping("/internal/test/errors")
public class InternalErrorTestController {

    /**
     * 強制的に 500 系の例外を発生させる。
     *
     * @throws IllegalStateException 常に送出
     */
    @GetMapping("/500")
    public String forceServerError() {
        throw new IllegalStateException("local error page test");
    }
}
