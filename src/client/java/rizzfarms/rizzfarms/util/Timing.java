package rizzfarms.rizzfarms.util;

import rizzfarms.rizzfarms.config.ModConfig;

import java.util.concurrent.ThreadLocalRandom;

public final class Timing {
    private Timing() {}

    /** Minimum delay so answers are never sent instantly (human reaction floor). */
    private static final double MIN_DELAY_SECONDS = 0.15;

    public static double tBlanks(int blanks) {
        ModConfig.Config c = ModConfig.cfg();
        double base = rand(c.blanksBaseMin, c.blanksBaseMax);
        double per = rand(c.blanksPerBlankMin, c.blanksPerBlankMax) * Math.max(blanks, 0);
        return Math.max(MIN_DELAY_SECONDS, Math.min(base + per, c.blanksCapSeconds));
    }

    public static double tLetters(int n) {
        ModConfig.Config c = ModConfig.cfg();
        return Math.max(MIN_DELAY_SECONDS, rand(c.lettersMin, c.lettersMax) * Math.max(n, 1));
    }

    public static double tSequence(int n) {
        ModConfig.Config c = ModConfig.cfg();
        return Math.max(MIN_DELAY_SECONDS, rand(c.sequenceMin, c.sequenceMax) * Math.max(n, 1));
    }

    public static double tMath(String exprRaw) {
        ModConfig.Config c = ModConfig.cfg();
        String expr = exprRaw == null ? "" : exprRaw;

        int len = expr.length();
        int ops = 0;
        for (char ch : expr.toCharArray()) if (ch == '+' || ch == '-' || ch == '*' || ch == '/' ) ops++;

        double base = rand(c.mathBaseMin, c.mathBaseMax);
        double extra = c.mathExtraPerChar * Math.max(len - 3, 0) + c.mathExtraPerOp * Math.max(ops - 1, 0);
        double out = base + extra;

        if (out < c.mathMin) out = c.mathMin;
        if (out > c.mathMax) out = c.mathMax;
        return Math.max(MIN_DELAY_SECONDS, out);
    }

    private static double rand(double a, double b) {
        double min = Math.min(a, b);
        double max = Math.max(a, b);
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}
