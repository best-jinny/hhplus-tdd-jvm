package io.hhplus.tdd.mock;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.repository.PointHistoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FakePointHistoryRepository implements PointHistoryRepository {
    private final List<PointHistory> table = new ArrayList<>();
    private long cursor = 1;

    @Override
    public PointHistory save(long userId, long amount, TransactionType transactionType, long updateMillis) {
        throttle(200);
        PointHistory pointHistory = new PointHistory(cursor++, userId, amount, transactionType, updateMillis);
        table.add(pointHistory);
        return pointHistory;
    }

    @Override
    public List<PointHistory> findAllByUserId(long userId) {
        return table.stream().filter(pointHistory -> pointHistory.userId() == userId).toList();
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
