package org.tsdl.infrastructure.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/**
 * Bundles utility methods to be used across TSDL components.
 */
public final class TsdlUtil {
  private TsdlUtil() {
  }

  private static final DecimalFormat VALUE_FORMATTER;

  static {
    // Double has a limited precision of 53 bits as per IEEE754, which amounts to roughly 16 decimal digits. Therefore, 16 significant decimal places
    // after a mandatory one should be enough (https://en.wikipedia.org/wiki/Floating-point_arithmetic#IEEE_754:_floating_point_in_modern_computers).
    VALUE_FORMATTER = new DecimalFormat("0.0################");
    var symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setDecimalSeparator('.');
    VALUE_FORMATTER.setDecimalFormatSymbols(symbols);
    VALUE_FORMATTER.setGroupingUsed(false);
  }

  public static double parseNumber(String str) throws ParseException {
    return VALUE_FORMATTER.parse(str).doubleValue();
  }

  public static String formatNumber(Number num) {
    return VALUE_FORMATTER.format(num);
  }
}