package io.hhplus.tdd.point;

import io.hhplus.tdd.mock.FakePointHistoryRepository;
import io.hhplus.tdd.mock.FakeUserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class PointServiceTest {

    UserPointRepository fakePointRepository = new FakeUserPointRepository();
    PointHistoryRepository fakeHistoryRepository = new FakePointHistoryRepository();
    PointService pointService = new PointService(fakePointRepository, fakeHistoryRepository);

    @Test
    void 유저의_포인트를_충전하면_충전_금액만큼_잔고가_업데이트_된다() {
        // given
        long userId = 1;
        long balance = 1000;
        long chargeAmount = 2000;
        fakePointRepository.save(userId, balance); // 유저의 포인트 잔고 설정

        // when
        UserPoint userPoint = pointService.charge(userId, chargeAmount);

        // then
        assertThat(userPoint.point()).isEqualTo(3000);
    }

    @Test
    void 최소_충전_포인트에_미달하면_포인트_충전에_실패한다() {
        // given
        long userId = 1;
        long amount = PointPolicy.MIN_AMOUNT_PER_CHARGE - 500; // 최소 충전 포인트(1,000)보다 작은 금액으로 설정합니다.

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, amount);
        });

        // 실패 케이스에서는 모두 에러 메세지를 검증합니다
        assertThat(exception.getMessage())
                .isEqualTo(String.format("최소 충전 포인트는 %d 입니다.", PointPolicy.MIN_AMOUNT_PER_CHARGE));
    }

    @ParameterizedTest
    @ValueSource(longs = {1110, 1001})
    void 포인트의_충전_단위가_일치하지_않으면_충전에_실패한다(long invalidAmount) { // 100 단위의 충전만 가능하기 때문에 1, 10 단위를 테스트합니다.
        // given
        long userId = 1;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });

        assertThat(exception.getMessage())
                .isEqualTo(String.format("포인트 충전 단위는 %d 입니다.", PointPolicy.CHARGE_UNIT));

    }

    @Test
    void 한_번에_최대_충전할_수_있는_포인트를_초과하면_충전에_실패한다() {
        // given
        long userId = 1;
        long amount = PointPolicy.MAX_AMOUNT_PER_CHARGE + 1000; // 1회 최대 충전 포인트(100,000)보다 큰 금액으로 설정합니다.

        // when & then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pointService.charge(userId, amount);
        });

        assertThat(exception.getMessage())
                .isEqualTo(String.format("1회 최대 충전 금액 %d 을 초과했습니다.", PointPolicy.MAX_AMOUNT_PER_CHARGE));

    }

    @Test
    void 충전_후_포인트의_최대_잔고를_넘으면_충전에_실패한다() {
        // given
        long userId = 1;
        long chargeAmount = 5000; // 해당 금액을 충전하면 최대 잔고 이상이 되는 상황
        fakePointRepository.save(1, PointPolicy.MAX_TOTAL_AMOUNT - 3000); // 최대 잔고에서 3,000 만큼 작은 금액을 보유하고 있다고 가정

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pointService.charge(userId, chargeAmount);
        });

        // then
        assertThat(exception.getMessage())
                .isEqualTo(String.format("포인트 최대 잔고 %d 을 초과했습니다.", PointPolicy.MAX_TOTAL_AMOUNT));
    }


    @Test
    void 특정_유저의_포인트를_조회할_수_있다() {
        // given
        long userId = 1;
        long balance = 1000;
        fakePointRepository.save(userId, balance);

        // when
        UserPoint userPoint = pointService.getUserPoint(userId);

        // then
        assertThat(userPoint.point()).isEqualTo(balance);
    }

    @Test
    void 특정_유저의_포인트_충전_또는_사용_내역을_조회할_수_있다() {
        // given
        long userId = 1;
        long chargeAmount = 10000;
        long useAmount = 5000;
        fakeHistoryRepository.save(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        fakeHistoryRepository.save(userId, useAmount, TransactionType.USE, System.currentTimeMillis());

        // when
        List<PointHistory> histories = pointService.getPointHistory(userId);

        // then
        // 리스트의 내용, 순서를 검증합니다.
        assertThat(histories).hasSize(2);
        PointHistory chargeHistory = histories.get(0);
        assertThat(chargeHistory.amount()).isEqualTo(chargeAmount);
        assertThat(chargeHistory.type()).isEqualTo(TransactionType.CHARGE);

        PointHistory useHistory = histories.get(1);
        assertThat(useHistory.amount()).isEqualTo(useAmount);
        assertThat(useHistory.type()).isEqualTo(TransactionType.USE);
    }

    @Test
    void 사용된_포인트_만큼_잔고가_차감되어야_한다() {
        // given
        long userId = 1;
        long balance = 5000;
        long useAmount = 1000;
        fakePointRepository.save(1, balance);

        // when
        UserPoint userPoint = pointService.use(userId, useAmount);

        // then
        assertThat(userPoint.point()).isEqualTo(4000);
    }

    @Test
    void 잔고가_부족할_경우_포인트_사용은_실패해야_한다() {
        // given
        long userId = 1;
        long balance = 300;
        long useAmount = 500;
        fakePointRepository.save(userId, balance); // 포인트 잔고 설정

        // when
        Throwable thrown = catchThrowable(() -> pointService.use(userId, useAmount));

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("포인트 잔고가 부족합니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {1010, 1001})
    void 포인트_사용_단위가_맞지_않으면_실패해야_한다(long invalidAmount) {
        // given
        long userId = 1;
        fakePointRepository.save(1, 5000);

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(userId, invalidAmount);
        });

        // then
        assertThat(exception.getMessage())
                .isEqualTo(String.format("포인트 사용 단위는 %d 입니다.", PointPolicy.USE_UNIT));
    }

    @Test
    void 최소_사용_포인트에_미달하면_실패한다() {
        // given
        long userId = 1;
        long balance = 5000;
        long amount = PointPolicy.MIN_AMOUNT_PER_USE - 100;
        fakePointRepository.save(userId, balance);

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(userId, amount);
        });

        assertThat(exception.getMessage())
                .isEqualTo(String.format("%d 포인트부터 사용 가능합니다.", PointPolicy.MIN_AMOUNT_PER_USE));
    }


}
