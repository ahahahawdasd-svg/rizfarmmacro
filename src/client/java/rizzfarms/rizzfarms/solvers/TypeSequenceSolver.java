package rizzfarms.rizzfarms.solvers;

import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.Timing;

import java.util.regex.Matcher;

public final class TypeSequenceSolver implements ChatGameSolver {

    @Override
    public SolveAttempt trySolve(String prompt) {
        Matcher m = Regexes.TYPE_SEQUENCE.matcher(prompt);
        if (!m.find()) return null;

        String seq = m.group(1);
        return SolveAttempt.solved(seq, Timing.tSequence(seq.length()), "sequence");
    }
}
