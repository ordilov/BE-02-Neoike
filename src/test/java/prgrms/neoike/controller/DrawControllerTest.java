package prgrms.neoike.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import prgrms.neoike.controller.dto.drawdto.DrawItem;
import prgrms.neoike.controller.dto.drawdto.DrawSaveRequest;
import prgrms.neoike.controller.dto.drawdto.ItemSizeAndQuantity;
import prgrms.neoike.controller.mapper.DrawMapper;
import prgrms.neoike.domain.draw.DrawStatus;
import prgrms.neoike.service.DrawService;
import prgrms.neoike.service.DrawTicketService;
import prgrms.neoike.service.dto.drawdto.DrawResponse;
import prgrms.neoike.service.dto.drawdto.ServiceDrawSaveDto;
import prgrms.neoike.service.dto.drawdto.ServiceItemDto;
import prgrms.neoike.service.dto.drawticketdto.DrawTicketsResponse;
import prgrms.neoike.service.dto.drawticketdto.DrawTicketResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.JsonFieldType.*;


@WebMvcTest(DrawController.class)
@AutoConfigureRestDocs
class DrawControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    DrawService drawService;

    @MockBean
    DrawTicketService drawTicketService;

    @MockBean
    DrawMapper drawMapper;

    @Test
    @DisplayName("/api/v1/draws 에서 draws 저장")
    void saveDrawTest() throws Exception {
        // given
        LocalDateTime fastDate = LocalDateTime.of(2025, 06, 12, 12, 00, 00);
        LocalDateTime middleDate = LocalDateTime.of(2025, 06, 13, 12, 00, 00);
        LocalDateTime lateDate = LocalDateTime.of(2025, 06, 15, 12, 00, 00);

        ItemSizeAndQuantity sneakerItems = new ItemSizeAndQuantity(275, 10);
        ServiceItemDto sneakerItemsInService = new ServiceItemDto(275, 10);

        DrawSaveRequest drawSaveRequest = DrawSaveRequest.builder()
                .sneakerId(1L)
                .startDate(fastDate)
                .endDate(middleDate)
                .winningDate(lateDate)
                .sneakerItems(new ArrayList<>(){{
                    add(sneakerItems);
                }})
                .quantity(50)
                .build();

        ServiceDrawSaveDto serviceDrawSaveDto = ServiceDrawSaveDto.builder()
                .sneakerId(2L)
                .startDate(fastDate)
                .endDate(middleDate)
                .winningDate(lateDate)
                .sneakerItems(new ArrayList<>(){{
                    add(sneakerItemsInService);
                }})
                .quantity(50)
                .build();

        DrawResponse drawResponse = new DrawResponse(3L);

        given(drawMapper.toDrawSaveDto(any(DrawSaveRequest.class))).willReturn(serviceDrawSaveDto);
        given(drawService.save(any(ServiceDrawSaveDto.class))).willReturn(drawResponse);

        // when // then
        mockMvc.perform(post("/api/v1/draws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(drawSaveRequest))
                )
                .andExpect(status().isCreated())
                .andDo(print())
                .andDo(document("save-draw",
                        requestFields(
                                fieldWithPath("sneakerId").type(NUMBER).description("sneaker id"),
                                fieldWithPath("startDate").type(STRING).description("응모 시작 날짜"),
                                fieldWithPath("endDate").type(STRING).description("응모 종료 날짜"),
                                fieldWithPath("winningDate").type(STRING).description("추첨 날짜"),
                                fieldWithPath("quantity").type(NUMBER).description("응모 개수"),
                                fieldWithPath("winningDate").type(STRING).description("추첨 날짜"),
                                fieldWithPath("sneakerItems").type(ARRAY).description("응모 상품들"),
                                fieldWithPath("sneakerItems[].size").type(NUMBER).description("응모 상품 사이즈"),
                                fieldWithPath("sneakerItems[].quantity").type(NUMBER).description("응모 상품 응모 개수")
                        ),
                        responseFields(
                                fieldWithPath("drawId").type(NUMBER).description("생성된 응모 id")
                        )));
    }

    @Test
    @DisplayName("/api/v1/draws/win 에서 추첨을 진행한다")
    void winDrawIdTest() throws Exception {
        // given
        DrawTicketsResponse drawTicketResponses = new DrawTicketsResponse(
                Arrays.asList(
                        DrawTicketResponse.builder()
                                .drawTicketId(1L)
                                .drawStatus(DrawStatus.WINNING)
                                .sneakerName("air jordan")
                                .price(27500)
                                .code("AB1234")
                                .size(275)
                                .build()
                )
        );

        given(drawService.drawWinner(1L)).willReturn(drawTicketResponses);

        // when // then
        mockMvc.perform(post("/api/v1/draws/win")
                        .param("drawId", String.valueOf(1L))
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("win-draw",
                        responseFields(
                                fieldWithPath("drawTicketResponses").type(ARRAY).description("응모권 배열"),
                                fieldWithPath("drawTicketResponses[].drawTicketId").type(NUMBER).description("응모권 아이디"),
                                fieldWithPath("drawTicketResponses[].drawStatus").type(STRING).description("응모권 상태"),
                                fieldWithPath("drawTicketResponses[].sneakerName").type(STRING).description("신발 이름"),
                                fieldWithPath("drawTicketResponses[].price").type(NUMBER).description("가격"),
                                fieldWithPath("drawTicketResponses[].code").type(STRING).description("코드"),
                                fieldWithPath("drawTicketResponses[].size").type(NUMBER).description("사이즈")
                        )));

    }
}