package rizzfarms.rizzfarms.util;

import rizzfarms.rizzfarms.config.ModConfig;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class WordDb {

    private WordDb() {}

    private static final Gson GSON = new Gson();
    private static final Type LIST_STRING = new TypeToken<List<String>>(){}.getType();

    private static final Set<String> all = new HashSet<>();
    private static final Set<String> learned = new HashSet<>();

    private static final Map<Integer, List<String>> byLen = new HashMap<>();

    // NEW: faster candidate buckets for blanks
    private static final Map<Integer, Map<Character, List<String>>> byLenFirst = new HashMap<>();
    private static final Map<Integer, Map<Character, List<String>>> byLenLast  = new HashMap<>();

    private static final Map<String, List<String>> anagrams = new HashMap<>();

    private static Path wordsPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rizzfarms").resolve("words.json");
    }

    private static Path extraWordlistPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rizzfarms")
                .resolve(ModConfig.cfg().extraWordlistFilename);
    }

    public static void load() {
        // base words
        addBase(Set.of(
                "water","wonderland","star","andesite","granite","diorite","stone","cobblestone",
                "zombie","skeleton","creeper","spider","villager","village","iron","gold","diamond",
                "netherite","shulker","survival","creative","grass","dirt","gravel","pickaxe","shovel",
                "axe","sword","bow","arrow","trident","potion","redstone","portal","nether","end",
                "elytra","phantom","dragon","enderman","warden","stronghold","furnace","beacon",
                "obsidian","quartz","bamboo","clay","brick","rabbit","sand","minecart","bell",
                "education" // add some obvious common wins
        ));

        // learned words
        try {
            Files.createDirectories(wordsPath().getParent());
            if (!Files.exists(wordsPath())) {
                saveLearned();
            } else {
                String json = Files.readString(wordsPath());
                List<String> list = GSON.fromJson(json, LIST_STRING);
                if (list != null) {
                    for (String w : list) {
                        if (w == null) continue;
                        String word = w.trim().toLowerCase(Locale.ROOT);
                        if (!word.isEmpty()) addLearnedWord(word, false);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[rizzfarms] words.json load failed: " + e.getMessage());
        }

        // extra wordlist
        if (ModConfig.cfg().loadExtraWordlist) {
            loadExtraWordlist();
        }
    }

    private static void loadExtraWordlist() {
        Path p = extraWordlistPath();
        try {
            Files.createDirectories(p.getParent());

            if (!Files.exists(p)) {
                Files.writeString(p,
                        "# Put one word per line here. This expands blanks/unscramble.\n" +
                                "# Example:\n" +
                                "# outstanding\n" +
                                "# table\n"
                );
                System.out.println("[rizzfarms] Created extra wordlist: " + p);
                return;
            }

            int maxLen = Math.max(1, ModConfig.cfg().maxWordLength);
            int added = 0;

            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String w = line.trim().toLowerCase(Locale.ROOT);
                    if (w.isEmpty()) continue;
                    if (w.startsWith("#")) continue;
                    if (!TextUtil.isAlnum(w)) continue;
                    if (w.length() > maxLen) continue;

                    if (!all.contains(w)) {
                        addWordInternal(w);
                        added++;
                    }
                }
            }

            if (added > 0) {
                System.out.println("[rizzfarms] Loaded extra wordlist: +" + added + " words");
            }
        } catch (Exception e) {
            System.out.println("[rizzfarms] extra_words.txt load failed: " + e.getMessage());
        }
    }

    private static void addBase(Set<String> base) {
        int maxLen = Math.max(1, ModConfig.cfg().maxWordLength);
        for (String w : base) {
            if (w == null) continue;
            String ww = w.toLowerCase(Locale.ROOT);
            if (ww.length() > maxLen) continue;
            addWordInternal(ww);
        }
    }

    public static void addLearnedWord(String word) {
        addLearnedWord(word, true);
    }

    private static void addLearnedWord(String wordRaw, boolean saveAfter) {
        String word = (wordRaw == null ? "" : wordRaw.trim().toLowerCase(Locale.ROOT));
        if (word.isEmpty()) return;

        int maxLen = Math.max(1, ModConfig.cfg().maxWordLength);
        if (word.length() > maxLen) return;

        if (all.contains(word)) return;

        addWordInternal(word);
        learned.add(word);

        if (saveAfter) saveLearned();
        System.out.println("[rizzfarms] Learned word: " + word);
    }

    private static void addWordInternal(String word) {
        all.add(word);

        byLen.computeIfAbsent(word.length(), k -> new ArrayList<>()).add(word);

        // NEW: by length + first char
        char first = word.charAt(0);
        byLenFirst
                .computeIfAbsent(word.length(), k -> new HashMap<>())
                .computeIfAbsent(first, k -> new ArrayList<>())
                .add(word);

        // NEW: by length + last char
        char last = word.charAt(word.length() - 1);
        byLenLast
                .computeIfAbsent(word.length(), k -> new HashMap<>())
                .computeIfAbsent(last, k -> new ArrayList<>())
                .add(word);

        // anagrams
        String key = sortLetters(word);
        anagrams.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }

    private static void saveLearned() {
        try {
            Files.createDirectories(wordsPath().getParent());
            List<String> out = new ArrayList<>(learned);
            Collections.sort(out);
            Files.writeString(wordsPath(), GSON.toJson(out));
        } catch (Exception e) {
            System.out.println("[rizzfarms] words.json save failed: " + e.getMessage());
        }
    }

    public static boolean matchesBlankPattern(String pattern, String word) {
        if (pattern == null || word == null) return false;
        if (pattern.length() != word.length()) return false;

        for (int i = 0; i < pattern.length(); i++) {
            char p = pattern.charAt(i);
            if (p == '_') continue;
            if (p != word.charAt(i)) return false;
        }
        return true;
    }

    private static List<String> candidatesForPattern(String pattern) {
        int len = pattern.length();
        if (len <= 0) return List.of();

        // Prefer tightest bucket: first letter if known, else last letter if known, else all by length
        char first = pattern.charAt(0);
        if (first != '_') {
            Map<Character, List<String>> m = byLenFirst.get(len);
            if (m != null) {
                List<String> l = m.get(first);
                if (l != null) return l;
            }
        }

        char last = pattern.charAt(len - 1);
        if (last != '_') {
            Map<Character, List<String>> m = byLenLast.get(len);
            if (m != null) {
                List<String> l = m.get(last);
                if (l != null) return l;
            }
        }

        List<String> l = byLen.get(len);
        return (l == null) ? List.of() : l;
    }

    public static String findFirstBlankMatch(String patternRaw, int scanCap) {
        String pattern = (patternRaw == null ? "" : patternRaw.trim().toLowerCase(Locale.ROOT));
        if (pattern.isEmpty()) return null;

        List<String> list = candidatesForPattern(pattern);
        if (list.isEmpty()) return null;

        int scanned = 0;
        for (String w : list) {
            if (matchesBlankPattern(pattern, w)) return w;
            scanned++;
            if (scanCap > 0 && scanned >= scanCap) break;
        }
        return null;
    }

    public static String unscrambleFirst(String scrambledRaw) {
        String s = (scrambledRaw == null ? "" : scrambledRaw.trim().toLowerCase(Locale.ROOT));
        if (s.isEmpty()) return null;

        String key = sortLetters(s);
        List<String> matches = anagrams.get(key);
        if (matches == null || matches.isEmpty()) return null;

        String best = matches.get(0);
        for (String m : matches) if (m.compareTo(best) < 0) best = m;
        return best;
    }

    private static String sortLetters(String s) {
        char[] c = s.toCharArray();
        Arrays.sort(c);
        return new String(c);
    }
}
