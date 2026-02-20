package rizzfarms.rizzfarms.core;

/**
 * Result of a solver attempting to solve a prompt.
 *
 * If {@link #answer} is null/empty, the prompt was recognized but could not be solved
 * (e.g., blanks had no wordlist match). Learning state may still be armed.
 */
public final class SolveAttempt {

    public final LearnMode learnMode;
    public final String learnKey;

    public final String answer;
    public final double delaySeconds;
    public final String why;

    public SolveAttempt(LearnMode learnMode, String learnKey, String answer, double delaySeconds, String why) {
        this.learnMode = learnMode == null ? LearnMode.NONE : learnMode;
        this.learnKey = learnKey;
        this.answer = answer;
        this.delaySeconds = delaySeconds;
        this.why = why;
    }

    public static SolveAttempt solved(String answer, double delaySeconds, String why) {
        return new SolveAttempt(LearnMode.NONE, null, answer, delaySeconds, why);
    }

    public static SolveAttempt recognized(LearnMode mode, String key, String answer, double delaySeconds, String why) {
        return new SolveAttempt(mode, key, answer, delaySeconds, why);
    }
}
