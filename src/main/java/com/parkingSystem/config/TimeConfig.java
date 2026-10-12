package com.parkingSystem.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Services read the current time from this {@link Clock} instead of calling {@code LocalDateTime.now()},
 * so tests can fix or move time (billing durations, reservation expiry) without sleeping.
 */
@Configuration
public class TimeConfig {

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
