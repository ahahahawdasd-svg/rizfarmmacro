package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.PendingSend;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.Timing;
import rizzfarms.rizzfarms.core.SolveAttempt;

import java.util.Locale;
import java.util.regex.Matcher;

public final class RandomNumberSolver implements ChatGameSolver {

    private boolean active = false;
    private int lo = 1;
    private int hi = 1000;
    private long lastArmMs = 0L;

    @Override
    public SolveAttempt trySolve(String prompt) {

        // 1) Detect start
        Matcher start = Regexes.RANDOM_NUMBER_START.matcher(prompt);
        if (start.find()) {
            int a = parseIntSafe(start.group(1), 1);
            int b = parseIntSafe(start.group(2), 1000);

            lo = Math.min(a, b);
            hi = Math.max(a, b);
            active = true;

            armNextGuess();
            return null;
        }

        if (!active) return null;

        // 2) Detect reveal -> reset
        if (Regexes.ANSWER_REVEAL.matcher(prompt).find()
                || Regexes.ANSWER_REVEAL_ANY.matcher(prompt).find()) {
            reset();
            return null;
        }

        // 3) Feedback lines: require server-ish formatting to avoid player chatter triggers
        // Based on your logs: "[5] [✔] Name ▶ 750 HIGHER"
        if (!prompt.contains("▶")) return null;

        Matcher fb = Regexes.RANDOM_NUMBER_FEEDBACK.matcher(prompt);
        if (!fb.find()) return null;

        int guess = parseIntSafe(fb.group(1), Integer.MIN_VALUE);
        if (guess == Integer.MIN_VALUE) return null;

        // Ignore nonsense / troll inputs outside current bounds
        if (guess < lo || guess > hi) return null;

        String dir = fb.group(2).toUpperCase(Locale.ROOT);

        if ("HIGHER".equals(dir)) {
            lo = Math.max(lo, guess + 1);
        } else if ("LOWER".equals(dir)) {
            hi = Math.min(hi, guess - 1);
        }

        if (lo > hi) {
            reset();
            return null;
        }

        armNextGuess();
        return null;
    }

    private void armNextGuess() {
        long now = System.currentTimeMillis();
        if (now - lastArmMs < 250L) return;
        lastArmMs = now;

        int next = midpoint(lo, hi);

        int range = (hi - lo) + 1;
        double delay =
                Timing.tSequence(String.valueOf(next).length())
                        + Math.min(0.6, Math.max(0.1,
                        Math.log10(Math.max(10, range)) * 0.15));

        PendingSend.arm(delay, String.valueOf(next),
                "random-number " + lo + "-" + hi);
    }

    private int midpoint(int a, int b) {
        return a + (b - a) / 2;
    }

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void reset() {
        active = false;
        lo = 1;
        hi = 1000;
        lastArmMs = 0L;
    }
}
