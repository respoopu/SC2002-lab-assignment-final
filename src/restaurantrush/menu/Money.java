package restaurantrush.menu;

import java.util.Locale;

/** All cents arithmetic and money formatting. */
public final class Money {
    private Money() {
    }

    /** Applies percentOff to cents, rounding half up to the nearest cent. */
    public static int applyDiscount(int cents, int percentOff) {
        if (cents < 0 || percentOff < 0 || percentOff > 100) {
            throw new IllegalArgumentException("cents must be >= 0 and percentOff within 0..100");
        }
        return (cents * (100 - percentOff) + 50) / 100;
    }

    /** "$18.00" */
    public static String format(int cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }
}
