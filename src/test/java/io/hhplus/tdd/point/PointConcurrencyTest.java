package io.hhplus.tdd.point;

import io.hhplus.tdd.mock.FakePointHistoryRepository;
import io.hhplus.tdd.mock.FakeUserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class PointConcurrencyTest {

    UserPointRepository fakePointRepository = new FakeUserPointRepository();
    PointHistoryRepository fakeHistoryRepository = new FakePointHistoryRepository();
    PointService pointService = new PointService(fakePointRepository, fakeHistoryRepository);

    @Test
    void 유저ID_1의_포인트_충전이_동시에_일어날_경우_포인트의_잔고가_정확하게_업데이트_된다() throws InterruptedException {
        // given
        long userId = 1;
        long amount = 5000; // 초기 포인트 5,000
        long chargeAmount = 1000;
        fakePointRepository.save(userId, amount);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(10); // ExecutorService 를 사용해 10개의 스레드 생성
        CountDownLatch latch = new CountDownLatch(10); // 10 개의 스레드가 끝날 때까지 기다린다

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        UserPoint updatedUserPoint = fakePointRepository.getUserPointById(userId); // 업데이트된 포인트 조회
        assertThat(updatedUserPoint.point()).isEqualTo(15000);
    }

    @Test
    void 유저ID_1의_포인트_사용이_동시에_일어날_경우_포인트의_잔고가_정확하게_업데이트_된다() throws InterruptedException {
        // given
        long userId = 1;
        long amount = 20000; // 초기 포인트 20,000
        long useAmount = 1000;
        fakePointRepository.save(userId, amount);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(10); // ExecutorService 를 사용해 10개의 스레드 생성
        CountDownLatch latch = new CountDownLatch(10); // 10 개의 스레드가 끝날 때까지 기다린다

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        UserPoint updatedUserPoint = fakePointRepository.getUserPointById(userId); // 업데이트된 포인트 조회
        assertThat(updatedUserPoint.point()).isEqualTo(10000);
    }

    @Test
    void 유저ID_1의_포인트_충전과_사용이_동시에_일어날_경우_포인트의_잔고가_정확하게_업데이트_된다() throws InterruptedException {
        // given
        long userId = 1;
        long initialAmount = 5000; // 초기 포인트 5,000
        long chargeAmount = 1000; // 충전 금액
        long useAmount = 500;     // 사용 금액
        fakePointRepository.save(userId, initialAmount);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(20); // 20개의 스레드 생성 (충전 10, 사용 10)
        CountDownLatch latch = new CountDownLatch(20); // 20개의 작업이 끝날 때까지 대기

        // 충전 스레드 10개
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 사용 스레드 10개
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        UserPoint updatedUserPoint = fakePointRepository.getUserPointById(userId);

        // 예상 결과 계산:
        // 초기 금액: 5000
        // 충전: 10 * 1000 = +10,000
        // 사용: 10 * 500 = -5,000
        long expectedAmount = initialAmount + (chargeAmount * 10) - (useAmount * 10); // 10,000

        assertThat(updatedUserPoint.point()).isEqualTo(expectedAmount);
    }

    @Test
    void 유저ID_1의_포인트_충전이_동시에_일어날_경우_충전_후_포인트의_최대_잔고를_넘으면_충전에_실패한다() throws InterruptedException {
        // given
        long userId = 1;
        long initialAmount = PointPolicy.MAX_TOTAL_AMOUNT - 5000;
        long chargeAmount = 1000;
        fakePointRepository.save(userId, initialAmount);

        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>(); // 예외를 수집할 큐

        // when
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } catch (Throwable e) {
                    exceptions.add(e); // 예외 수집
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        boolean hasExpectedException = exceptions.stream().anyMatch(e -> e.getMessage().equals(String.format("포인트 최대 잔고 %d 을 초과했습니다.", PointPolicy.MAX_TOTAL_AMOUNT)));
        assertThat(hasExpectedException).isTrue(); // 해당 예외 메세지 발생 사실 검증
    }

    @Test
    void 유저ID_1의_포인트_사용이_동시에_일어날_경우_잔고가_부족하면_사용에_실패한다() throws InterruptedException {
        // given
        long userId = 1;
        long initialAmount = 50000;
        long useAmount = 10000;
        fakePointRepository.save(userId, initialAmount);

        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>(); // 예외를 수집할 큐

        // when
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } catch (Throwable e) {
                    exceptions.add(e); // 예외 수집
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        boolean hasExpectedException = exceptions.stream().anyMatch(e -> e.getMessage().equals("포인트 잔고가 부족합니다."));
        assertThat(hasExpectedException).isTrue(); // 해당 예외 메세지 발생 사실 검증
    }

    @Test
    void 동시에_여러_요청이_들어오더라도_충전_및_사용_후_히스토리가_정확히_기록된다() throws InterruptedException {
        // given
        long userId = 1;
        long initialAmount = 5000; 
        long chargeAmount = 1000;
        long useAmount = 500;  
        fakePointRepository.save(userId, initialAmount); // 잔고 설정

        // when
        // 총 20개의 작업 (충전 10, 사용 10)
        ExecutorService executor = Executors.newFixedThreadPool(20); 
        CountDownLatch latch = new CountDownLatch(20); 

        // 충전 스레드 10개
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 사용 스레드 10개
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 작업이 끝날 때까지 대기
        executor.shutdown();

        // then
        // 히스토리 검증
        var histories = fakeHistoryRepository.findAllByUserId(userId);
        assertThat(histories).hasSize(20); // 총 20건의 히스토리가 기록되어야 함

        // 충전 기록 검증
        long chargeHistories = histories.stream()
                .filter(history -> history.type() == TransactionType.CHARGE && history.amount() == chargeAmount)
                .count();
        assertThat(chargeHistories).isEqualTo(10); // 충전 기록이 10건이어야 함

        // 사용 기록 검증
        long useHistories = histories.stream()
                .filter(history -> history.type() == TransactionType.USE && history.amount() == useAmount)
                .count();
        assertThat(useHistories).isEqualTo(10); // 사용 기록이 10건이어야 함
    }
}
