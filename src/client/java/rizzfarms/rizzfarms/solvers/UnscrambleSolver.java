package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.LearnMode;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.Timing;
import rizzfarms.rizzfarms.util.WordDb;

import java.util.Locale;
import java.util.regex.Matcher;

public final class UnscrambleSolver implements ChatGameSolver {

    @Override
    public SolveAttempt trySolve(String prompt) {
        Matcher m = Regexes.UNSCRAMBLE.matcher(prompt);
        if (!m.find()) return null;

        String scrambledRaw = m.group(1);
        String s = (scrambledRaw == null ? "" : scrambledRaw.trim().toLowerCase(Locale.ROOT));
        if (s.isEmpty()) return SolveAttempt.recognized(LearnMode.UNSCRAMBLE, null, null, 0.0, "unscramble");

        String ans = WordDb.unscrambleFirst(s);
        if (ans == null) {
            return SolveAttempt.recognized(LearnMode.UNSCRAMBLE, s, null, 0.0, "unscramble");
        }

        return SolveAttempt.recognized(
                LearnMode.UNSCRAMBLE,
                s,
                ans,
                Timing.tLetters(ans.length()),
                "unscramble"
        );
    }
}
