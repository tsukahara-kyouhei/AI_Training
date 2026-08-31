package jp.co.skig.officeorder.config;

import jp.co.skig.officeorder.logging.MemberMdcInterceptor;
import jp.co.skig.officeorder.logging.RequestIdMdcFilter;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.auth.MemberActiveValidationInterceptor;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web共通のMDC設定と会員有効性検証を登録する設定。
 */
@Configuration
public class WebLoggingConfig implements WebMvcConfigurer {

    /** 会員有効性確認サービス。 */
    private final MemberService memberService;
    /** 会員セッション制御サービス。 */
    private final MemberSessionService memberSessionService;

    /**
     * Webログ設定を生成する。
     *
     * @param memberService        会員サービス
     * @param memberSessionService 会員セッションサービス
     */
    public WebLoggingConfig(MemberService memberService,
            MemberSessionService memberSessionService) {
        this.memberService = memberService;
        this.memberSessionService = memberSessionService;
    }

    /**
     * リクエスト単位のMDC初期化Filterを登録する。
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<RequestIdMdcFilter> requestIdMdcFilterRegistration() {
        FilterRegistrationBean<RequestIdMdcFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestIdMdcFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }

    /**
     * MDC連携と会員有効性確認のInterceptorを登録する。
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MemberMdcInterceptor())
                .addPathPatterns("/**");
        registry.addInterceptor(new MemberActiveValidationInterceptor(memberService, memberSessionService))
                .addPathPatterns("/mypage/**");
    }
}
