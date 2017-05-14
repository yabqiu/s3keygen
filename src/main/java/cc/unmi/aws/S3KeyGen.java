package cc.unmi.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class S3KeyGen {

  private static List<Character> baseChars = new ArrayList<>(62);

  static {
    IntStream.rangeClosed('0', '9').forEach(value -> baseChars.add((char) value));
    IntStream.rangeClosed('A', 'Z').forEach(value -> baseChars.add((char) value));
    IntStream.rangeClosed('a', 'z').forEach(value -> baseChars.add((char) value));
  }

  public static String encode(long number) {
    checkArgument(number >= 0, "Should be a positive number");

    long reversed = Long.parseLong(reverse(String.valueOf(number)));
    int zeros = String.valueOf(number).length() - String.valueOf(reversed).length();

    if(zeros > 1) {
      reversed *= (long) Math.pow(10, zeros - 1);
    }

    StringBuilder key = new StringBuilder();
    encode(key, reversed);

    if (zeros > 1) {
      key.append("-").append(baseChars.get(zeros - 1));
    }
    return key.toString();
  }

  private static void encode(StringBuilder sb, long number) {
    char c = baseChars.get((int) (number % baseChars.size()));

    sb.append(c);

    long quotient = number / baseChars.size();
    if (quotient >= baseChars.size()) {
      encode(sb, quotient);
    } else if (quotient != 0) {
      sb.append((char) baseChars.get((int) quotient));
    }
  }

  public static long decode(String encodedS3key) {
    checkArgument(encodedS3key != null, "Encoded S3 key cannot be null");

    encodedS3key.chars().forEach(value -> {
      char c = (char) value;
      checkArgument(c == '-' || baseChars.contains(c), "Not a valid encoded S3 Key");
    });

    String[] split = encodedS3key.split("-");

    checkState(split.length <= 2, "Could_not contain more than one -");

    long sum = 0;
    char[] chars = reverse(split[0]).toCharArray();

    for (int i = 0; i < chars.length; i++) {
      int index = Collections.binarySearch(baseChars, chars[i]);
      sum += index * (long)Math.pow(baseChars.size(), chars.length - 1 - i);
    }

    checkState(sum >= 0, "No a valid long number in string");

    StringBuilder result = new StringBuilder(reverse(String.valueOf(sum)));

    if (split.length == 2) {
      IntStream.range(0, Collections.binarySearch(baseChars, (char)(split[1].charAt(0) + 1)))
          .forEach(value -> result.append("0"));
    }

    return Long.parseLong(result.toString());
  }

  private static String reverse(String str) {
    return str == null?null:(new StringBuilder(str)).reverse().toString();
  }
}
