package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory save(long userId, long amount, TransactionType transactionType, long updateMillis);
    List<PointHistory> findAllByUserId(long userId);
}
