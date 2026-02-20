package rizzfarms.rizzfarms.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class MathSolver {
    private MathSolver() {}

    public static String solve(String exprRaw) {
        if (exprRaw == null) return null;

        String expr = exprRaw;
        // Optimization: avoid multiple replace calls if no special characters
        if (expr.indexOf('×') >= 0 || expr.indexOf('÷') >= 0 || expr.indexOf('x') >= 0 || expr.indexOf('X') >= 0) {
            expr = expr.replace("×", "*").replace("÷", "/").replace("x", "*").replace("X", "*");
        }

        StringBuilder sb = new StringBuilder(expr.length());
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c != '=' && c != ' ' && c != '?') sb.append(c);
        }
        expr = sb.toString();
        if (expr.isEmpty()) return null;

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (!((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == '.')) return null;
        }

        Double val = eval(expr);
        if (val == null || !Double.isFinite(val)) return null;

        if (Math.abs(val - Math.rint(val)) < 1e-12) {
            return Long.toString((long) Math.rint(val));
        }

        String s = String.format(Locale.ROOT, "%.10f", val);
        int lastNonZero = -1;
        int dotIdx = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') dotIdx = i;
            else if (c != '0') lastNonZero = i;
        }
        if (dotIdx != -1) {
            if (lastNonZero < dotIdx) return s.substring(0, dotIdx);
            return s.substring(0, lastNonZero + 1);
        }
        return s;
    }

    private static Double eval(String s) {
        try {
            Deque<Double> values = new ArrayDeque<>();
            Deque<Character> ops = new ArrayDeque<>();

            int i = 0;
            while (i < s.length()) {
                char ch = s.charAt(i);

                if (Character.isDigit(ch) || ch == '.' || (ch == '-' && isUnaryMinus(s, i))) {
                    int start = i;
                    i++;
                    while (i < s.length()) {
                        char c2 = s.charAt(i);
                        if (Character.isDigit(c2) || c2 == '.') i++;
                        else break;
                    }
                    double num = Double.parseDouble(s.substring(start, i));
                    values.push(num);
                    continue;
                }

                if (ch == '(') {
                    ops.push(ch);
                    i++;
                    continue;
                }

                if (ch == ')') {
                    while (!ops.isEmpty() && ops.peek() != '(') {
                        if (!applyTop(values, ops.pop())) return null;
                    }
                    if (ops.isEmpty() || ops.pop() != '(') return null;
                    i++;
                    continue;
                }

                if (isOp(ch)) {
                    while (!ops.isEmpty() && isOp(ops.peek()) && prec(ops.peek()) >= prec(ch)) {
                        if (!applyTop(values, ops.pop())) return null;
                    }
                    ops.push(ch);
                    i++;
                    continue;
                }

                return null;
            }

            while (!ops.isEmpty()) {
                char op = ops.pop();
                if (op == '(' || op == ')') return null;
                if (!applyTop(values, op)) return null;
            }

            if (values.size() != 1) return null;
            return values.pop();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean applyTop(Deque<Double> values, char op) {
        if (values.size() < 2) return false;
        double b = values.pop();
        double a = values.pop();
        double out;
        switch (op) {
            case '+': out = a + b; break;
            case '-': out = a - b; break;
            case '*': out = a * b; break;
            case '/': out = a / b; break;
            default: return false;
        }
        values.push(out);
        return true;
    }

    private static boolean isOp(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static int prec(char c) {
        if (c == '*' || c == '/') return 2;
        if (c == '+' || c == '-') return 1;
        return 0;
    }

    private static boolean isUnaryMinus(String s, int i) {
        if (s.charAt(i) != '-') return false;
        if (i == 0) return true;
        char prev = s.charAt(i - 1);
        return prev == '(' || isOp(prev);
    }
}
