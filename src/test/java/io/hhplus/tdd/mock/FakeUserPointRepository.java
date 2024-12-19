package io.hhplus.tdd.mock;

import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.repository.UserPointRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FakeUserPointRepository implements UserPointRepository {
    private final Map<Long, UserPoint> table = new HashMap<>();

    @Override
    public UserPoint getUserPointById(long id) {
        throttle(200);
        return table.getOrDefault(id, UserPoint.empty(id));
    }

    @Override
    public UserPoint save(long id, long amount) {
        throttle(300);
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());
        table.put(id, userPoint);
        return userPoint;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
