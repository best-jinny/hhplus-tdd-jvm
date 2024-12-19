package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PointService pointService;

    private static final long USER_ID = 1;
    private static final long INITIAL_AMOUNT = 1000;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        pointService.charge(USER_ID, INITIAL_AMOUNT); // 각 테스트에서 필요한 포인트 초기화
    }

    @Test
    @DisplayName("특정 유저의 포인트 조회 테스트")
    void getUserPoint() throws Exception {

        mockMvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(INITIAL_AMOUNT));
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전 테스트")
    void chargePoint() throws Exception {
        // given
        long chargeAmount = 5000;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(6000));
    }

    @Test
    @DisplayName("특정 유저의 포인트 사용 테스트")
    void usePoint() throws Exception {
        // given
        long useAmount = 900;

        // when & then
        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(100));
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/사용 내역 조회 테스트")
    void getPointHistories() throws Exception {
        // given
        pointService.charge(USER_ID, 2000); // 추가 충전 2000
        pointService.use(USER_ID, 500); // 사용 500

        // when & then
        mockMvc.perform(get("/point/{id}/histories", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(USER_ID))
                .andExpect(jsonPath("$[0].amount").value(1000))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.toString()))
                .andExpect(jsonPath("$[1].userId").value(USER_ID))
                .andExpect(jsonPath("$[1].amount").value(2000))
                .andExpect(jsonPath("$[1].type").value(TransactionType.CHARGE.toString()))
                .andExpect(jsonPath("$[2].userId").value(USER_ID))
                .andExpect(jsonPath("$[2].amount").value(500))
                .andExpect(jsonPath("$[2].type").value(TransactionType.USE.toString()));
    }
}



