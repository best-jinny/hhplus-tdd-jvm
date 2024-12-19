package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Component;

@Component
public class UserPointAdapter implements UserPointRepository {

    UserPointTable userPointTable = new UserPointTable();

    @Override
    public UserPoint getUserPointById(long userId) {
        return userPointTable.selectById(userId);
    }

    @Override
    public UserPoint save(long userId, long amount) {
        return userPointTable.insertOrUpdate(userId, amount);
    }
}
