package com.parkingSystem.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock that follows real time but can be pushed forward, so tests can jump past a reservation hold
 * without sleeping. {@link #reset()} goes back to real time.
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock(ZoneId zone) {
        this.zone = zone;
    }

    public void advance(Duration amount) {
        offset = offset.plus(amount);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        MutableClock copy = new MutableClock(newZone);
        copy.offset = offset;
        return copy;
    }
}
