package nbbrd.io.text;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class StylishWriterTest {

    // each body element is a row; each column extracts one cell from that row
    @SuppressWarnings({"null", "DataFlowIssue"})
    private static StylishWriter<String[]> newWriter() {
        return StylishWriter.<String[]>builder()
                .separator("\n")
                .column(row -> row[0])
                .column(row -> row[1])
                .build();
    }

    private static String[] row(String left, String right) {
        return new String[]{left, right};
    }

    @Test
    public void testBuilderDefaults() {
        StylishWriter<String[]> writer = StylishWriter.<String[]>builder().build();

        assertThat(writer.getDelimiter()).isEqualTo("  ");
        assertThat(writer.getSeparator()).isEqualTo(System.lineSeparator());
        assertThat(writer.getColumns()).isEmpty();
    }

    @Test
    @SuppressWarnings({"null", "DataFlowIssue"})
    public void testWriteNullChecks() {
        StylishWriter<String[]> writer = newWriter();

        assertThatNullPointerException()
                .isThrownBy(() -> writer.write(null, "header", emptyList(), "footer"));
    }

    @Test
    @SuppressWarnings({"null", "DataFlowIssue"})
    public void testWriteAllNullChecks() {
        StylishWriter<String[]> writer = newWriter();

        Function<String, CharSequence> header = s -> s;
        Function<String, List<String[]>> body = x -> emptyList();
        Function<String, CharSequence> footer = s -> s;

        assertThatNullPointerException()
                .isThrownBy(() -> writer.writeAll(null, singletonList("x"), header, body, footer));
        assertThatNullPointerException()
                .isThrownBy(() -> writer.writeAll(new StringBuilder(), null, header, body, footer));
        assertThatNullPointerException()
                .isThrownBy(() -> writer.writeAll(new StringBuilder(), singletonList("x"), null, body, footer));
        assertThatNullPointerException()
                .isThrownBy(() -> writer.writeAll(new StringBuilder(), singletonList("x"), header, null, footer));
        assertThatNullPointerException()
                .isThrownBy(() -> writer.writeAll(new StringBuilder(), singletonList("x"), header, body, null));
    }

    @Test
    public void testWriteHeaderOnly() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, "header", null, null);

        assertThat(sb.toString()).isEqualTo("header\n");
    }

    @Test
    public void testWriteFooterOnly() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, null, "footer");

        assertThat(sb.toString()).isEqualTo("  \n  footer\n");
    }

    @Test
    public void testWriteBodyOnly() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, singletonList(row("a", "b")), null);

        assertThat(sb.toString()).isEqualTo("  a  b\n");
    }

    @Test
    public void testWriteEmptyBody() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, emptyList(), null);

        assertThat(sb.toString()).isEmpty();
    }

    @Test
    public void testWriteNothing() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, null, null);

        assertThat(sb.toString()).isEmpty();
    }

    @Test
    public void testWriteBodyColumnPadding() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, asList(row("aaa", "b"), row("c", "ddd")), null);

        // each column is padded to the widest visible cell in that column
        assertThat(sb.toString())
                .isEqualTo("  aaa  b  \n  c    ddd\n");
    }

    @Test
    public void testWriteFull() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, "header", singletonList(row("a", "b")), "footer");

        assertThat(sb.toString())
                .isEqualTo("header\n  a  b\n  \n  footer\n");
    }

    @Test
    @SuppressWarnings({"null", "DataFlowIssue"})
    public void testCustomDelimiterAndSeparator() throws IOException {
        StylishWriter<String[]> writer = StylishWriter.<String[]>builder()
                .delimiter("|")
                .separator(";")
                .column(row -> row[0])
                .column(row -> row[1])
                .build();

        StringBuilder sb = new StringBuilder();
        writer.write(sb, "header", singletonList(row("a", "b")), "footer");

        assertThat(sb.toString())
                .isEqualTo("header;|a|b;|;|footer;");
    }

    @Test
    public void testWriteAll() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().writeAll(sb, asList("x", "y"),
                s -> "header-" + s,
                s -> singletonList(row(s + "1", s + "2")),
                s -> "footer-" + s);

        assertThat(sb.toString())
                .isEqualTo("header-x\n  x1  x2\n  \n  footer-x\n\nheader-y\n  y1  y2\n  \n  footer-y\n");
    }

    @Test
    public void testWriteAllEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().writeAll(sb, emptyList(),
                (String s) -> s,
                s -> emptyList(),
                (String s) -> s);

        assertThat(sb.toString()).isEmpty();
    }

    @Test
    public void testVisibleLengthIgnoresAnsiCodes() throws IOException {
        StringBuilder sb = new StringBuilder();
        newWriter().write(sb, null, asList(row("\u001B[31mred\u001B[0m", "x"), row("z", "y")), null);

        // padding is based on the visible length ("red" -> 3), not the raw length
        assertThat(sb.toString())
                .isEqualTo("  \u001B[31mred\u001B[0m  x\n  z    y\n");
    }
}






