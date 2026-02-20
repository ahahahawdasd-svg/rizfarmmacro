package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.LearnMode;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.TextUtil;
import rizzfarms.rizzfarms.util.Timing;
import rizzfarms.rizzfarms.util.WordDb;

import java.util.Locale;
import java.util.regex.Matcher;

public final class FillBlanksSolver implements ChatGameSolver {

    @Override
    public SolveAttempt trySolve(String prompt) {
        Matcher m = Regexes.FILL_BLANKS.matcher(prompt);
        if (!m.find()) return null;

        String patternRaw = m.group(1);
        String pattern = (patternRaw == null ? "" : patternRaw.trim().toLowerCase(Locale.ROOT));
        if (pattern.isEmpty()) return SolveAttempt.recognized(LearnMode.BLANKS, null, null, 0.0, "blanks");

        String guess = WordDb.findFirstBlankMatch(pattern, ModConfig.cfg().blankScanCap);
        if (guess == null) {
            // Recognized, no solution now -> keep learning armed with pattern
            return SolveAttempt.recognized(LearnMode.BLANKS, pattern, null, 0.0, "blanks " + patternRaw);
        }

        int blanks = TextUtil.countChar(pattern, '_');
        return SolveAttempt.recognized(
                LearnMode.BLANKS,
                pattern,
                guess,
                Timing.tBlanks(blanks),
                "blanks " + patternRaw
        );
    }
}
