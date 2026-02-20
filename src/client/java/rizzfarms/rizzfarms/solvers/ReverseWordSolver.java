package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.Timing;

import java.util.regex.Matcher;

public final class ReverseWordSolver implements ChatGameSolver {

    @Override
    public SolveAttempt trySolve(String prompt) {
        Matcher m = Regexes.REVERSE_WORD.matcher(prompt);
        if (!m.find()) return null;

        String w = m.group(1);
        String ans = new StringBuilder(w).reverse().toString();
        return SolveAttempt.solved(ans, Timing.tLetters(w.length()), "reverse");
    }
}
