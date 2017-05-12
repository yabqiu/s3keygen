package cc.unmi.aws;

import org.junit.Test;

import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

public class S3KeyGenTest {

    @Test
    public void testForLongNumberWithoutZeroEnding() {
        assertThat(S3KeyGen.encode(0L)).isEqualTo("0");
        assertThat(S3KeyGen.encode(1L)).isEqualTo("1");
        assertThat(S3KeyGen.encode(9L)).isEqualTo("9");

        assertThat(S3KeyGen.decode("9")).isEqualTo(9L);

        assertThat(S3KeyGen.encode(11L)).isEqualTo("B");
        assertThat(S3KeyGen.encode(21L)).isEqualTo("C");

        assertThat(S3KeyGen.decode("C")).isEqualTo(21L);
    }

    @Test
    public void testForLongNumberWithZeroEnding() {
        String key = S3KeyGen.encode(10L);
        assertThat(key).isEqualTo("1");

        key = S3KeyGen.encode(234524400L);
        assertThat(key).isEqualTo("MZgz2-1");
        assertThat(S3KeyGen.decode("MZgz2-1")).isEqualTo(Long.valueOf(234524400L));

        key = S3KeyGen.encode(1000000000000000000L);
        assertThat(key).isEqualTo("cZiYq440O7-H");
    }

    @Test
    public void testForMaximunLongNumber() {
        String keyForMax = S3KeyGen.encode(Long.MAX_VALUE);

        assertThat(keyForMax).isEqualTo("xrpuHADvQR8");
        assertThat(S3KeyGen.decode("xrpuHADvQR8")).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void testFirstLetterDistribution() {
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        Map<Character, Integer> distribution = LongStream.rangeClosed(0, 90000L)
            .mapToObj(value -> CompletableFuture.supplyAsync(() -> S3KeyGen.encode(value), executorService))
            .map(CompletableFuture::join)
            .collect(Collectors.groupingBy(o -> o.charAt(0))).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().size()));

        assertThat(distribution).hasSize(62);
        IntSummaryStatistics statistics = distribution.values().stream().mapToInt(Integer::intValue).summaryStatistics();

        double variance = Math.sqrt(distribution.values().stream().mapToDouble(value ->
            Math.pow(value - statistics.getAverage(), 2)).sum() / distribution.size());

        System.out.println(distribution);

        assertThat(variance).isEqualTo(3.3226197930672114);

        assertThat(statistics.getAverage()).isEqualTo(1451.6290322580646);
        assertThat(statistics.getMax()).isEqualTo(1456);
        assertThat(statistics.getMin()).isEqualTo(1447);
    }
}