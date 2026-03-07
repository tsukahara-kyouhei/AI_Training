package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.model.batch.BatchExecutionAcceptedResponse;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.batch.BatchExecutionService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link InternalBatchController} の利用者向け応答文言を確認するテスト。
 */
@ActiveProfiles("local")
@WebMvcTest(controllers = InternalBatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchExecutionService batchExecutionService;

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
     * バッチ起動受付成功時は、業務文言を含む JSON 応答を返すことを確認する。
     */
    @Test
    void startJob_returnsAcceptedMessageWhenJobIsSubmitted() throws Exception {
        when(batchExecutionService.submitJob("popular-ranking"))
                .thenReturn(new BatchExecutionAcceptedResponse(
                        "100",
                        "popular-ranking",
                        OffsetDateTime.parse("2026-03-07T19:30:00+09:00"),
                        "ジョブを受け付けました。"
                ));

        mockMvc.perform(post("/internal/batch/jobs/popular-ranking/executions"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").value("100"))
                .andExpect(jsonPath("$.message").value("ジョブを受け付けました。"));
    }
}
