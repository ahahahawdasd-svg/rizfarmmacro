package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.MathSolver;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.Timing;

import java.util.regex.Matcher;

public final class MathGameSolver implements ChatGameSolver {

    @Override
    public SolveAttempt trySolve(String prompt) {
        Matcher m = Regexes.SOLVE_EQUATION.matcher(prompt);
        if (!m.find()) return null;

        String expr = m.group(1);
        String ans = MathSolver.solve(expr);
        if (ans == null) {
            // recognized, but we couldn't compute (should be rare)
            return SolveAttempt.recognized(null, null, null, 0.0, "math");
        }
        return SolveAttempt.solved(ans, Timing.tMath(expr), "math");
    }
}
