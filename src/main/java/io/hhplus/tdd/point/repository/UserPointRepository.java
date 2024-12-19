package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.UserPoint;

public interface UserPointRepository {
    UserPoint getUserPointById(long userId);
    UserPoint save(long userId, long amount);
}
