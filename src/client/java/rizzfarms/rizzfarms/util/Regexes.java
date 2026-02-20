package rizzfarms.rizzfarms.util;

import java.util.regex.Pattern;

public final class Regexes {
    private Regexes() {}

    public static final Pattern SOLVE_EQUATION =
            Pattern.compile("Solve this equation:\\s*(.+?)\\s*=\\s*\\?+",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern REVERSE_WORD =
            Pattern.compile("Reverse this word:\\s*([A-Za-z0-9_]+)",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern TYPE_SEQUENCE =
            Pattern.compile("Type\\s+(?:this|the|following)\\s+sequence:\\s*([A-Za-z0-9_]+)",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern FILL_BLANKS =
            Pattern.compile("Fill in the blanks:\\s*([A-Za-z0-9_]+)",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern UNSCRAMBLE =
            Pattern.compile("Unscramble(?: this word)?:\\s*([A-Za-z0-9_]+)",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern ANSWER_REVEAL =
            Pattern.compile("type the answer\\s+([A-Za-z0-9_]+)\\s+in\\s+\\d+(?:\\.\\d+)?\\s+seconds",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern ANSWER_REVEAL_ANY =
            Pattern.compile("^\\s*(.+?)\\s+type the answer\\s+([A-Za-z0-9_]+)\\s+in\\s+(\\d+(?:\\.\\d+)?)\\s+seconds\\b",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern RANDOM_NUMBER_START =
            Pattern.compile("Guess a random number between\\s+(\\d+)\\s+and\\s+(\\d+)!?",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern RANDOM_NUMBER_FEEDBACK =
            Pattern.compile("\\b(\\d+)\\s+(HIGHER|LOWER)\\b\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** Captcha item id from chat: "CAP: item_id", "Captcha: item_id", "[CAP] item_id", etc. */
    public static final Pattern CAP_MESSAGE =
            Pattern.compile("(?:CAP|captcha)\\s*:?\\s*\\[?\\s*([A-Za-z0-9_.:-]+)", Pattern.CASE_INSENSITIVE);
}
