package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointPolicy;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public UserPoint getUserPoint(long userId) {
        return userPointRepository.getUserPointById(userId);
    }

    public UserPoint charge(long userId, long amount) {

        if(PointPolicy.MIN_AMOUNT_PER_CHARGE > amount) {
            throw new IllegalArgumentException(String.format("최소 충전 포인트는 %d 입니다.", PointPolicy.MIN_AMOUNT_PER_CHARGE));
        }
        if(amount % PointPolicy.CHARGE_UNIT != 0) {
            throw new IllegalArgumentException(String.format("포인트 충전 단위는 %d 입니다.", PointPolicy.CHARGE_UNIT));
        }
        if(amount > PointPolicy.MAX_AMOUNT_PER_CHARGE) {
            throw new RuntimeException(String.format("1회 최대 충전 금액 %d 을 초과했습니다.", PointPolicy.MAX_AMOUNT_PER_CHARGE));
        }

        // 현재 포인트 조회 필요
        UserPoint userPoint = getUserPoint(userId);
        if(userPoint.point() + amount > PointPolicy.MAX_TOTAL_AMOUNT) {
            throw new RuntimeException(String.format("포인트 최대 잔고 %d 을 초과했습니다.", PointPolicy.MAX_TOTAL_AMOUNT));
        }

        userPoint = userPointRepository.save(userId, userPoint.point() + amount);
        pointHistoryRepository.save(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPoint;
    }

    public UserPoint use(long userId, long amount) {

        UserPoint userPoint = getUserPoint(userId);
        if(userPoint.point() - amount < 0) {
            throw new RuntimeException("포인트 잔고가 부족합니다.");
        }
        if(amount % PointPolicy.USE_UNIT != 0) {
            throw new IllegalArgumentException(String.format("포인트 사용 단위는 %d 입니다.", PointPolicy.USE_UNIT));
        }
        if(PointPolicy.MIN_AMOUNT_PER_USE > amount) {
            throw new IllegalArgumentException(String.format("%d 포인트부터 사용 가능합니다.", PointPolicy.MIN_AMOUNT_PER_USE));
        }

        userPoint = userPointRepository.save(userId, userPoint.point() - amount);
        pointHistoryRepository.save(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return userPoint;

    }

    public List<PointHistory> getPointHistory(long userId) {
        return pointHistoryRepository.findAllByUserId(userId);
    }
}
