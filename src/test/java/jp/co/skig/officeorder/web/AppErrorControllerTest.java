package jp.co.skig.officeorder.web;

import jakarta.servlet.RequestDispatcher;
import jp.co.skig.officeorder.logging.RequestIdMdcFilter;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AppErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * 500 系エラーでは、専用のシステムエラーページと requestId が返されることを確認する。
     */
    @Test
    void handleError_returnsDedicatedHtmlPageForServerError() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/products/1")
                        .requestAttr(RequestIdMdcFilter.REQUEST_ID_ATTRIBUTE, "req-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute("statusCode", 500))
                .andExpect(model().attribute("errorTitle", "システムエラーが発生しました"))
                .andExpect(model().attribute("requestId", "req-500"))
                .andExpect(model().attribute("errorPath", "/products/1"));
    }

    /**
     * JSON を要求したエラー応答では、画面ではなく API 向けのエラーJSONが返ることを確認する。
     */
    @Test
    void handleError_returnsJsonForJsonRequest() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/missing")
                        .requestAttr(RequestIdMdcFilter.REQUEST_ID_ATTRIBUTE, "req-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("req-404"))
                .andExpect(jsonPath("$.path").value("/missing"));
    }
}
