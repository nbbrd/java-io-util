package nbbrd.io.sys;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OSTest {

    @Test
    public void testName() {
        assertThat(OS.Name.parse(null)).isEqualTo(OS.Name.UNKNOWN);
        assertThat(OS.Name.parse("")).isEqualTo(OS.Name.UNKNOWN);
        assertThat(OS.Name.parse(UUID.randomUUID().toString())).isEqualTo(OS.Name.UNKNOWN);
        assertThat(OS.NAME).isNotEqualTo(OS.Name.UNKNOWN);
    }
}
