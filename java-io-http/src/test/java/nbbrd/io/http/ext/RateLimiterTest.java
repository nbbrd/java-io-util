package nbbrd.io.http.ext;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void fixedLimiterDoesNotAdapt() {
        RateLimiter limiter = RateLimiter.fixed(10, 1, Duration.ofSeconds(60));
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onThrottled(Duration.ofSeconds(1))).isNull();
    }

    @Test
    void getMaxWaitReturnsConfiguredValue() {
        assertThat(RateLimiter.fixed(10, 1, Duration.ofSeconds(30)).getMaxWait())
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void reserveReturnsNoWaitThenWaitExceeded() {
        // 1 permit/second, burst 1, tiny wait cap.
        RateLimiter limiter = RateLimiter.fixed(1, 1, Duration.ofMillis(1));
        assertThat(limiter.reserve()).isEqualTo(RateLimiter.NO_WAIT);
        // Second reservation needs ~1s which exceeds the 1ms cap.
        assertThat(limiter.reserve()).isEqualTo(RateLimiter.WAIT_EXCEEDED);
    }

    @Test
    void unlimitedAdaptiveNeverWaitsUntilThrottled() {
        RateLimiter limiter = RateLimiter.unlimitedAdaptive(Duration.ofSeconds(60));
        assertThat(limiter.reserve()).isEqualTo(RateLimiter.NO_WAIT);
    }

    @Test
    void adaptiveReducesRateOnThrottled() {
        RateLimiter limiter = RateLimiter.adaptive(100, 1, Duration.ofSeconds(60));
        RateLimiter.RateChange change = limiter.onThrottled(Duration.ofSeconds(1));
        assertThat(change).isNotNull();
        assertThat(change.getOldRate()).isEqualTo(100.0);
        assertThat(change.getNewRate()).isEqualTo(50.0);
    }

    @Test
    void adaptiveDerivesRateFromRetryAfterWhenUnlimited() {
        RateLimiter limiter = RateLimiter.unlimitedAdaptive(Duration.ofSeconds(60));
        RateLimiter.RateChange change = limiter.onThrottled(Duration.ofSeconds(4));
        assertThat(change).isNotNull();
        assertThat(change.getNewRate()).isEqualTo(0.25); // 1 / 4s
    }

    @Test
    void adaptiveRecoversAfterSuccessStreak() {
        RateLimiter limiter = RateLimiter.adaptive(100, 1, Duration.ofSeconds(60));
        limiter.onThrottled(Duration.ofSeconds(1)); // 100 -> 50

        // Recovery only after the streak threshold (5) is reached.
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        RateLimiter.RateChange recovery = limiter.onSuccess();
        assertThat(recovery).isNotNull();
        assertThat(recovery.getOldRate()).isEqualTo(50.0);
        assertThat(recovery.getNewRate()).isEqualTo(100.0); // capped at initial rate
    }

    @Test
    void adaptiveSuccessStreakResetOnThrottled() {
        RateLimiter limiter = RateLimiter.adaptive(100, 1, Duration.ofSeconds(60));
        limiter.onThrottled(Duration.ofSeconds(1)); // 100 -> 50
        limiter.onSuccess();
        limiter.onSuccess();
        limiter.onThrottled(Duration.ofSeconds(1)); // 50 -> 25, streak reset

        // Streak was reset, so 4 successes are not enough to recover.
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNull();
        assertThat(limiter.onSuccess()).isNotNull(); // 5th triggers recovery
    }
}

