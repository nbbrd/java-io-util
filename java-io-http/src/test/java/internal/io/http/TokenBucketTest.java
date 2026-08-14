package internal.io.http;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TokenBucketTest {

    private static final long SECOND_NANOS = TimeUnit.SECONDS.toNanos(1);

    @Test
    void constructorRejectsInvalidArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenBucket(0, 1, SECOND_NANOS));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenBucket(1, 0, SECOND_NANOS));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenBucket(1, 1, -1));
    }

    @Test
    void reserveReturnsZeroWhenTokensAvailable() {
        TokenBucket bucket = new TokenBucket(10, 5, TimeUnit.HOURS.toNanos(1));
        // Burst capacity is 5, so the first 5 reservations at the same instant wait 0.
        long now = 1_000_000_000L;
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.reserve(now)).isEqualTo(0);
        }
    }

    @Test
    void reserveReturnsIncreasingWaitForConcurrentReservations() {
        // 1 permit/second, burst 1: after consuming the initial token, each
        // further reservation at the same instant must wait progressively longer.
        TokenBucket bucket = new TokenBucket(1, 1, TimeUnit.HOURS.toNanos(1));
        long now = 1_000_000_000L;

        assertThat(bucket.reserve(now)).isEqualTo(0);
        long first = bucket.reserve(now);
        long second = bucket.reserve(now);

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
        // At 1 permit/s, each additional permit costs ~1 second.
        assertThat(second - first).isCloseTo(SECOND_NANOS, org.assertj.core.data.Offset.offset(1_000_000L));
    }

    @Test
    void reserveReturnsMinusOneWhenExceedingMaxWait() {
        // 1 permit/second, burst 1, max wait 500ms: the second reservation would
        // need ~1s and must be rejected.
        TokenBucket bucket = new TokenBucket(1, 1, TimeUnit.MILLISECONDS.toNanos(500));
        long now = 1_000_000_000L;

        assertThat(bucket.reserve(now)).isEqualTo(0);
        assertThat(bucket.reserve(now)).isEqualTo(-1);
    }

    @Test
    void refillRestoresTokensOverTime() {
        TokenBucket bucket = new TokenBucket(1, 1, TimeUnit.HOURS.toNanos(1));
        long now = 1_000_000_000L;

        assertThat(bucket.reserve(now)).isEqualTo(0);
        // One second later a token is available again.
        assertThat(bucket.reserve(now + SECOND_NANOS)).isEqualTo(0);
    }

    @Test
    void isUnlimitedReflectsRate() {
        assertThat(new TokenBucket(TokenBucket.UNLIMITED_THRESHOLD, 1, SECOND_NANOS).isUnlimited()).isTrue();
        assertThat(new TokenBucket(10, 1, SECOND_NANOS).isUnlimited()).isFalse();
    }

    @Test
    void setPermitsPerSecondUpdatesRate() {
        TokenBucket bucket = new TokenBucket(10, 1, SECOND_NANOS);
        bucket.setPermitsPerSecond(2);
        assertThat(bucket.getPermitsPerSecond()).isEqualTo(2);
        assertThatIllegalArgumentException().isThrownBy(() -> bucket.setPermitsPerSecond(0));
    }
}

