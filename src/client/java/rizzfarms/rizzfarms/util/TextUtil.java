package rizzfarms.rizzfarms.util;

public final class TextUtil {
    private TextUtil() {}

    public static String stripFormatting(String s) {
        if (s == null || s.isEmpty()) return "";

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++; // skip formatting code char too
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public static int countChar(String s, char c) {
        if (s == null || s.isEmpty()) return 0;
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    public static boolean isAlnum(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) return false;
        }
        return true;
    }

    /**
     * Normalizes "fancy" Unicode characters (bold, italic, etc.) to standard ASCII.
     */
    public static String normalize(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            int cp = s.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) i++;

            int norm = cp;
            // Bold
            if (cp >= 0x1D400 && cp <= 0x1D419) norm = 'A' + (cp - 0x1D400);
            else if (cp >= 0x1D41A && cp <= 0x1D433) norm = 'a' + (cp - 0x1D41A);
            // Italic
            else if (cp >= 0x1D434 && cp <= 0x1D44D) norm = 'A' + (cp - 0x1D434);
            else if (cp >= 0x1D44E && cp <= 0x1D467) norm = 'a' + (cp - 0x1D44E);
            // Bold Italic
            else if (cp >= 0x1D468 && cp <= 0x1D481) norm = 'A' + (cp - 0x1D468);
            else if (cp >= 0x1D482 && cp <= 0x1D49B) norm = 'a' + (cp - 0x1D482);
            // Script
            else if (cp >= 0x1D49C && cp <= 0x1D4B5) norm = 'A' + (cp - 0x1D49C);
            else if (cp >= 0x1D4B6 && cp <= 0x1D4CF) norm = 'a' + (cp - 0x1D4B6);
            // Fraktur
            else if (cp >= 0x1D504 && cp <= 0x1D51D) norm = 'A' + (cp - 0x1D504);
            else if (cp >= 0x1D51E && cp <= 0x1D537) norm = 'a' + (cp - 0x1D51E);
            // Double-Struck
            else if (cp >= 0x1D538 && cp <= 0x1D551) norm = 'A' + (cp - 0x1D538);
            else if (cp >= 0x1D552 && cp <= 0x1D56B) norm = 'a' + (cp - 0x1D552);
            // Sans-Serif
            else if (cp >= 0x1D5A0 && cp <= 0x1D5B9) norm = 'A' + (cp - 0x1D5A0);
            else if (cp >= 0x1D5BA && cp <= 0x1D5D3) norm = 'a' + (cp - 0x1D5BA);
            // Monospace
            else if (cp >= 0x1D670 && cp <= 0x1D689) norm = 'A' + (cp - 0x1D670);
            else if (cp >= 0x1D68A && cp <= 0x1D6A3) norm = 'a' + (cp - 0x1D68A);
            // Enclosed Alphanumerics
            else if (cp >= 0x24B6 && cp <= 0x24CF) norm = 'A' + (cp - 0x24B6);
            else if (cp >= 0x24D0 && cp <= 0x24E9) norm = 'a' + (cp - 0x24D0);

            sb.appendCodePoint(norm);
        }
        return sb.toString();
    }
}
