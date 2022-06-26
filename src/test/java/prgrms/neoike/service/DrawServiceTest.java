package prgrms.neoike.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import prgrms.neoike.common.exception.EntityNotFoundException;
import prgrms.neoike.domain.draw.DrawStatus;
import prgrms.neoike.domain.member.*;
import prgrms.neoike.domain.sneaker.*;
import prgrms.neoike.repository.*;
import prgrms.neoike.service.dto.drawdto.DrawResponse;
import prgrms.neoike.service.dto.drawdto.ServiceDrawSaveDto;
import prgrms.neoike.service.dto.drawdto.ServiceItemDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest
@Transactional
class DrawServiceTest {
    @Autowired
    DrawService drawService;

    @Autowired
    DrawTicketService drawTicketService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    DrawTicketRepository drawTicketRepository;

    @Autowired
    SneakerRepository sneakerRepository;
    @Autowired
    SneakerStockRepository sneakerStockRepository;

    @Autowired
    DrawRepository drawRepository;


    LocalDateTime startDate = LocalDateTime.of(2025, 06, 12, 12, 00, 00);
    LocalDateTime endDate = LocalDateTime.of(2025, 06, 13, 12, 00, 00);
    LocalDateTime winningDate = LocalDateTime.of(2025, 06, 14, 12, 00, 00);


    @Test
    @DisplayName("Draw 엔티티를 저장한다.")
    void saveDrawTest() {
        // given
        Sneaker sneaker = savedSneaker();
        saveSneakerStock(275, 10, sneaker);
        saveSneakerStock(285, 10, sneaker);
        Map<Integer, Integer> sizeToQuantity = new HashMap<>(){{
            put(275, 50);
            put(285, 5);
        }};

        ServiceDrawSaveDto drawSaveDto = validDrawSaveDto(sneaker.getId(), 50, sizeToQuantity);

        // when
        drawService.save(drawSaveDto);

        // then
        assertThat(drawRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("저장된 stock 의 사이즈와 입력 사이즈가 달라 Draw 엔티티를 저장할때 오류 발생")
    void failSaveDrawTest() {
        // given
        Sneaker sneaker = savedSneaker();
        saveSneakerStock(275, 10, sneaker);
        saveSneakerStock(285, 10, sneaker);
        Map<Integer, Integer> sizeToQuantity = new HashMap<>(){{
            put(275, 50);
            put(295, 5);
        }};

        ServiceDrawSaveDto drawSaveDto = validDrawSaveDto(sneaker.getId(), 50, sizeToQuantity);

        // when // then
        ;
        assertThatThrownBy(() -> drawService.save(drawSaveDto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("등록 순서가 잘못된 Draw 엔티티를 저장할때 오류 발생")
    void invalidSaveDrawTest() {
        // given
        Sneaker sneaker = savedSneaker();

        // when // then
        assertThatThrownBy(() -> drawService.save(
                ServiceDrawSaveDto.builder()
                        .sneakerId(sneaker.getId())
                        .startDate(endDate)
                        .endDate(startDate)
                        .winningDate(winningDate)
                        .quantity(50)
                        .build()
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> drawService.save(
                ServiceDrawSaveDto.builder()
                        .sneakerId(sneaker.getId())
                        .startDate(startDate)
                        .endDate(winningDate)
                        .winningDate(endDate)
                        .quantity(50)
                        .build()
        )).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DisplayName("추첨을 진행한다")
    void drawWinTest() {
        // given
        Sneaker sneaker = savedSneaker();
        saveSneakerStock(275, 5, sneaker);
        Map<Integer, Integer> sizeToQuantity = new HashMap<>(){{
            put(275, 5);
        }};

        ServiceDrawSaveDto drawSaveDto = validDrawSaveDto(sneaker.getId(), 5, sizeToQuantity);
        DrawResponse drawResponse = drawService.save(drawSaveDto);

        List<Member> members = Stream.generate(() -> memberRepository.save(validMember()))
                .limit(5)
                .toList();

        members.stream().forEach(
                (member) -> drawTicketService.save(member.getId(), drawResponse.drawId(), 275)
        );


        // when
        drawService.drawWinner(drawResponse.drawId());

        // then
        System.out.println(drawTicketRepository.findByMember(members.get(0)).get(0).getDrawStatus());
        long count = members.stream().map(
                        (member) -> drawTicketRepository.findByMember(member).get(0).getDrawStatus()
                )
                .filter((status) -> status == DrawStatus.WINNING)
                .count();

        Assertions.assertThat(count).isEqualTo(5);
    }

    private Sneaker savedSneaker() {
        return sneakerRepository.save(
                Sneaker.builder()
                        .memberCategory(MemberCategory.MEN)
                        .sneakerCategory(SneakerCategory.BASKETBALL)
                        .name("air jordan")
                        .price(75000)
                        .description("1")
                        .code("AB1234")
                        .releaseDate(LocalDateTime.now())
                        .build()
        );
    }

    private void saveSneakerStock(int size, int quantity, Sneaker sneaker) {
        SneakerStock stock = SneakerStock.builder()
                .size(size)
                .stock(new Stock(quantity))
                .build();
        stock.setSneaker(sneaker);

        sneakerStockRepository.save(stock);
    }

    private ServiceDrawSaveDto validDrawSaveDto(Long sneakerId, int quantity, Map<Integer, Integer> sizeToQuantity) {
        List<ServiceItemDto> items = new ArrayList<>();
        sizeToQuantity.forEach (
                (size, quan) -> items.add(new ServiceItemDto(size, quan))
        );

        return ServiceDrawSaveDto.builder()
                .sneakerId(sneakerId)
                .startDate(startDate)
                .endDate(endDate)
                .winningDate(winningDate)
                .sneakerItems(items)
                .quantity(quantity)
                .build();
    }

    private Member validMember() {
        return Member.builder()
                .name("이용훈")
                .password(new Password("123abcAB!!"))
                .phoneNumber(new PhoneNumber(CountryType.KOR, "01012341566"))
                .address(new Address("도시", "거리", "000222"))
                .birthDay(LocalDateTime.now())
                .email(new Email("test@test.com"))
                .gender(Gender.MALE)
                .build();
    }
}