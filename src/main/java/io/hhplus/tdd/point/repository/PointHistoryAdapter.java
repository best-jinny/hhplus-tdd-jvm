package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointHistoryAdapter implements PointHistoryRepository {

    PointHistoryTable pointHistoryTable = new PointHistoryTable();

    @Override
    public PointHistory save(long userId, long amount, TransactionType transactionType, long updateMillis) {
        return pointHistoryTable.insert(userId, amount, transactionType, updateMillis);
    }

    @Override
    public List<PointHistory> findAllByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

}
