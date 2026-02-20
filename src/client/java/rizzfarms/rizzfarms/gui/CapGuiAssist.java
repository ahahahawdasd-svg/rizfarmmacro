package rizzfarms.rizzfarms.gui;

import rizzfarms.rizzfarms.macro.FarmingMacro;
import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.util.TextUtil;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CapGuiAssist {

    // Visuals: improved cleaner colors
    private static final int WRONG_OVERLAY = 0x77000000;   // Elegant semi-transparent black
    private static final int RIGHT_OVERLAY = 0x3300FF00;   // Soft green tint
    private static final int RIGHT_BORDER  = 0xAA00FF00;   // Distinct bright green border

    private static final Pattern CLICK_TITLE =
            Pattern.compile("(?i)\\bclick\\s+on\\b\\s*:?\\s*(.+)$");

    // Some servers use "Click to Confirm ..." on the correct item name
    private static final Pattern CONFIRM_NAME =
            Pattern.compile("(?i)\\bclick\\s+to\\s+confirm\\b|confirm\\s+your\\s+here|confirm\\s+you\\s+are\\s+here");

    private static final Map<Item, String> ITEM_ID_CACHE = new IdentityHashMap<>();
    private static final List<Entry<String, String>> NAME_TO_ID_INDEX = new ArrayList<>();
    private static final Map<String, List<String>> PATH_TOKEN_CACHE = new HashMap<>();

    private static final List<String> CHAT_GRID = new ArrayList<>();
    private static long lastCapMessageMs = 0L;

    private static final WeakHashMap<Screen, State> STATES = new WeakHashMap<>();

    private static Field F_LEFT_POS;
    private static Field F_TOP_POS;

    // ignore these words for title->id matching (eye of ender etc.)
    private static final Set<String> STOPWORDS = Set.of("of", "the", "a", "an");

    private CapGuiAssist() {}

    public static void init() {
        initReflection();
        buildItemNameIndex();

        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            STATES.put(screen, new State());
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            State st = STATES.get(screen);
            if (st != null) {
                updateState(cs, st);
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            ScreenEvents.afterRender(screen).register((s, gg, mouseX, mouseY, tickDelta) ->
                    onAfterRender(s, gg)
            );
            ScreenEvents.remove(screen).register(STATES::remove);
        });
    }

    public static void handleCapMessage(String itemId) {
        long now = System.currentTimeMillis();
        // If > 5s since last CAP message, assume it's a new captcha sequence
        if (now - lastCapMessageMs > 5000) {
            CHAT_GRID.clear();
        }
        lastCapMessageMs = now;
        if (itemId != null && !itemId.isEmpty()) {
            String id = itemId.toLowerCase(Locale.ROOT).trim();
            CHAT_GRID.add(id.contains(":") ? id : "minecraft:" + id);
        } else {
            CHAT_GRID.add("");
        }
    }

    private static void onAfterRender(Screen screen, GuiGraphics gg) {
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

        State st = STATES.get(screen);
        if (st == null) return;

        updateState(cs, st);
        if (st.targetId == null && !st.isCaptcha) return;

        int left = getLeftPos(cs);
        int top = getTopPos(cs);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return;

        Set<Slot> correct = (st.cachedCorrectSlots != null)
                ? st.cachedCorrectSlots
                : (st.cachedCorrectSlots = findCorrectSlots(cs, st.targetId));

        // Auto-click if enabled and in macro
        if (FarmingMacro.isActive() && ModConfig.cfg().autoCaptchaEnabled && !correct.isEmpty()) {
            clickCorrectSlot(cs, correct);
        }

        // Draw overlays
        for (Slot slot : cs.getMenu().slots) {
            ItemStack stack = slot.getItem();
            // Optional: skip slots that don't belong to the container (player inventory)
            // if (slot.container == Minecraft.getInstance().player.getInventory()) continue;

            int x = left + slot.x;
            int y = top + slot.y;

            int x1 = x - 1;
            int y1 = y - 1;
            int x2 = x + 17;
            int y2 = y + 17;

            if (correct.contains(slot)) {
                gg.fill(x1, y1, x2, y2, RIGHT_OVERLAY);
                drawBorder(gg, x1, y1, x2, y2, RIGHT_BORDER);
            } else if (!stack.isEmpty()) {
                // Darken all other non-empty slots if we are in a captcha
                gg.fill(x1, y1, x2, y2, WRONG_OVERLAY);
            }
        }
    }

    /**
     * Primary: match by item registry id exactly (minecraft:golden_apple, etc.)
     * If that returns != 1 match (0 or >=2), fallback:
     *   A) Unique slot whose hover name matches "Click to Confirm"
     *   B) Unique slot whose hover text contains the target id OR contains the target path tokens
     *
     * Returns the set of "correct" slots (usually size 1).
     */
    private static Set<Slot> findCorrectSlots(AbstractContainerScreen<?> cs, String targetId) {
        Set<Slot> idMatches = new HashSet<>();
        Set<Slot> nameMatches = new HashSet<>();
        Set<Slot> chatMatches = new HashSet<>();
        Set<Slot> gridMatches = new HashSet<>();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.world.entity.player.Inventory playerInv = (mc.player != null) ? mc.player.getInventory() : null;

        String effectiveTargetId = targetId;
        if (effectiveTargetId == null && CHAT_GRID.size() == 1) {
            effectiveTargetId = CHAT_GRID.get(0);
        }

        Set<Integer> targetSlots = getGridSlotIndices(cs, playerInv);

        int containerIdx = 0;
        for (Slot slot : cs.getMenu().slots) {
            if (playerInv != null && slot.container == playerInv) continue;

            boolean inGrid = targetSlots.contains(slot.index);

            if (containerIdx < CHAT_GRID.size()) {
                String chatItemId = CHAT_GRID.get(containerIdx);
                if (effectiveTargetId != null && (effectiveTargetId.equals(chatItemId) || normalizeItemId(effectiveTargetId).equals(normalizeItemId(chatItemId)))) {
                    chatMatches.add(slot);
                }
            }
            containerIdx++;

            ItemStack st = slot.getItem();
            if (st.isEmpty()) continue;

            String id = getItemId(st.getItem());
            if (effectiveTargetId != null && (effectiveTargetId.equals(id) || normalizeItemId(effectiveTargetId).equals(normalizeItemId(id)))) {
                idMatches.add(slot);
                if (inGrid) gridMatches.add(slot);
            }

            String hover = safeLower(st.getHoverName().getString());
            if (CONFIRM_NAME.matcher(hover).find()) {
                nameMatches.add(slot);
            }
        }

        // 1. If we have a single "Click to Confirm" name match in the grid, that's almost certainly it.
        Set<Slot> nameMatchesInGrid = new HashSet<>(nameMatches);
        nameMatchesInGrid.retainAll(gridMatches.isEmpty() ? idMatches : gridMatches); // Try to combine with ID info if possible
        if (nameMatchesInGrid.size() == 1) return nameMatchesInGrid;
        
        // 2. Fallback to any single "Click to Confirm" match
        if (nameMatches.size() == 1) return nameMatches;

        // 3. Fallback to grid matches for the identified ID
        if (gridMatches.size() == 1) return gridMatches;

        // 4. Fallback to chat-reported grid data
        if (chatMatches.size() == 1) return chatMatches;

        // 5. Fallback to any ID matches
        return idMatches;
    }

    private static boolean isCaptchaTitle(String title) {
        if (title == null || title.isEmpty()) return false;
        String t = safeLower(title);
        return t.contains("click on") || t.contains("captcha") || t.contains("click the") || t.contains("select the")
                || t.contains("choose the") || t.contains("find the") || t.contains("pick the") || t.contains("which one")
                || t.contains("correct item") || t.contains("verify") || t.contains("verification") || t.contains("identify")
                || t.contains("prove you") || t.contains("human verification") || t.contains("anti-bot") || t.contains("tap the")
                || t.contains("click to confirm") || t.contains("select the correct") || t.contains("click the correct");
    }

    private static void updateState(AbstractContainerScreen<?> screen, State st) {
        String title = screen.getTitle().getString();
        if (!title.equals(st.lastTitle)) {
            st.lastTitle = title;
            st.isCaptcha = isCaptchaTitle(title);
            st.targetId = resolveTargetIdFromTitle(title);
            if (st.isCaptcha && st.targetId == null && CHAT_GRID.size() == 1) {
                st.targetId = CHAT_GRID.get(0);
            }
            st.cachedCorrectSlots = null;
        }
        if (!st.isCaptcha && CHAT_GRID.size() >= 1) {
            long now = System.currentTimeMillis();
            if (now - lastCapMessageMs < 15000) {
                st.isCaptcha = true;
                if (st.targetId == null && CHAT_GRID.size() == 1) {
                    st.targetId = CHAT_GRID.get(0);
                    st.cachedCorrectSlots = null;
                }
            }
        }
    }

    private static final Pattern[] TITLE_ITEM_PREFIXES = new Pattern[] {
        Pattern.compile("(?i)click\\s+on\\s*:?\\s*(.+)"),
        Pattern.compile("(?i)select\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)choose\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)find\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)click\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)pick\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)which\\s+one\\s+(?:is\\s+)?(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)identify\\s+(?:the\\s+)?(.+)"),
        Pattern.compile("(?i)tap\\s+(?:the\\s+)?(.+)"),
    };

    private static String resolveTargetIdFromTitle(String title) {
        if (title == null) return null;
        String t = safeLower(title);

        for (Pattern p : TITLE_ITEM_PREFIXES) {
            java.util.regex.Matcher m = p.matcher(t);
            if (m.find()) {
                String after = m.group(1).trim();
                if (after.startsWith(":")) after = after.substring(1).trim();
                int dash = after.indexOf(" - ");
                if (dash > 0) after = after.substring(0, dash).trim();
                if (after.isEmpty()) continue;
                String bestId = longestItemNameMatch(after);
                if (bestId != null) return bestId;
            }
        }

        return longestItemNameMatch(t);
    }

    private static String longestItemNameMatch(String text) {
        if (text == null || text.isEmpty()) return null;
        String bestId = null;
        int maxLen = -1;
        for (Entry<String, String> entry : NAME_TO_ID_INDEX) {
            String itemName = entry.getKey();
            if (text.contains(itemName) && itemName.length() > maxLen) {
                maxLen = itemName.length();
                bestId = entry.getValue();
            }
        }
        return bestId;
    }

    private static void buildItemNameIndex() {
        NAME_TO_ID_INDEX.clear();
        for (Item it : BuiltInRegistries.ITEM) {
            String id = getItemId(it);
            if (id == null) continue;

            // 1. Path-based names
            String path = pathOf(id).replace('_', ' ');
            NAME_TO_ID_INDEX.add(new AbstractMap.SimpleEntry<>(path, id));

            String alnum = concatAlnum(path);
            if (!alnum.equals(path)) {
                NAME_TO_ID_INDEX.add(new AbstractMap.SimpleEntry<>(alnum, id));
            }

            // 2. Localized names (I18n)
            try {
                String localized = net.minecraft.client.resources.language.I18n.get(it.getDescriptionId());
                String normLoc = safeLower(localized);
                if (!normLoc.isEmpty()) {
                    NAME_TO_ID_INDEX.add(new AbstractMap.SimpleEntry<>(normLoc, id));
                    String alnumLoc = concatAlnum(normLoc);
                    if (!alnumLoc.equals(normLoc)) {
                        NAME_TO_ID_INDEX.add(new AbstractMap.SimpleEntry<>(alnumLoc, id));
                    }
                }
            } catch (Throwable ignored) {}
        }
        // Sort by length descending
        NAME_TO_ID_INDEX.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
    }

    // ---------------- helpers ----------------

    private static boolean existsExactId(String want) {
        if (want == null) return false;
        for (Item it : BuiltInRegistries.ITEM) {
            String id = getItemId(it);
            if (want.equals(id)) return true;
        }
        return false;
    }

    private static String getItemId(Item item) {
        if (item == null) return null;
        return ITEM_ID_CACHE.computeIfAbsent(item, i -> {
            try {
                return BuiltInRegistries.ITEM.getKey(i).toString();
            } catch (Throwable ignored) {
                return null;
            }
        });
    }

    private static List<String> getCachedTokensFromPath(String path) {
        if (path == null) return List.of();
        return PATH_TOKEN_CACHE.computeIfAbsent(path, CapGuiAssist::tokensFromPath);
    }

    private static String pathOf(String id) {
        int colon = id.indexOf(':');
        return (colon >= 0) ? id.substring(colon + 1) : id;
    }

    private static String normalizeItemId(String id) {
        if (id == null || id.isEmpty()) return id;
        String lower = id.toLowerCase(Locale.ROOT).trim();
        return lower.contains(":") ? lower : "minecraft:" + lower;
    }

    private static Set<Integer> getGridSlotIndices(AbstractContainerScreen<?> cs, net.minecraft.world.entity.player.Inventory playerInv) {
        List<Slot> containerSlots = new ArrayList<>();
        for (Slot slot : cs.getMenu().slots) {
            if (playerInv != null && slot.container == playerInv) continue;
            containerSlots.add(slot);
        }
        int n = containerSlots.size();
        Set<Integer> indices = new HashSet<>();
        if (n >= 54) {
            for (int i : new int[]{10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43})
                indices.add(i);
        } else if (n >= 45) {
            for (int r = 1; r <= 3; r++)
                for (int c = 0; c < 9; c++) indices.add(r * 9 + c);
        } else if (n >= 27) {
            for (int i = 0; i < n; i++) indices.add(i);
        } else {
            for (Slot s : containerSlots) indices.add(s.index);
        }
        return indices;
    }

    private static String stripFormatting(String s) {
        return TextUtil.stripFormatting(s);
    }

    private static String safeLower(String s) {
        if (s == null) return "";
        String stripped = stripFormatting(s);
        String normalized = TextUtil.normalize(stripped);
        String lower = normalized.toLowerCase(Locale.ROOT);

        // Collapse multiple spaces
        StringBuilder sb = new StringBuilder(lower.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        return sb.toString().trim();
    }

    // "Eye of Ender" -> "eye_of_ender"
    private static String toUnderscore(String s) {
        if (s == null) return "";
        String cleaned = stripFormatting(s).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(cleaned.length());
        boolean lastWasSpecial = false;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
                lastWasSpecial = false;
            } else {
                if (!lastWasSpecial && sb.length() > 0) {
                    sb.append('_');
                    lastWasSpecial = true;
                }
            }
        }
        String out = sb.toString();
        if (out.endsWith("_")) out = out.substring(0, out.length() - 1);
        return out;
    }

    // keep only letters/numbers and concatenate: "ender_eye" -> "endereye"
    private static String concatAlnum(String s) {
        if (s == null || s.isEmpty()) return "";
        String stripped = stripFormatting(s).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // "ender_eye" -> ["ender","eye"]
    private static List<String> tokensFromPath(String path) {
        if (path == null) return List.of();
        String cleaned = path.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (c == '_') {
                if (sb.length() > 0) {
                    out.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }
        if (sb.length() > 0) out.add(sb.toString());
        return out;
    }

    // "Eye of Ender" -> ["eye","ender"] (removes stopwords)
    private static List<String> tokensNoStopwords(String s) {
        if (s == null) return List.of();
        String cleaned = stripFormatting(s).toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    String t = sb.toString();
                    if (!STOPWORDS.contains(t)) out.add(t);
                    sb.setLength(0);
                }
            }
        }
        if (sb.length() > 0) {
            String t = sb.toString();
            if (!STOPWORDS.contains(t)) out.add(t);
        }
        return out;
    }

    // order-insensitive equality with duplicates
    private static boolean multisetEquals(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        HashMap<String, Integer> m = new HashMap<>();
        for (String s : a) m.put(s, m.getOrDefault(s, 0) + 1);
        for (String s : b) {
            Integer c = m.get(s);
            if (c == null) return false;
            if (c == 1) m.remove(s);
            else m.put(s, c - 1);
        }
        return m.isEmpty();
    }

    // returns true if haystack contains all needles (needles treated as multiset)
    private static boolean containsAllTokens(List<String> haystack, List<String> needles) {
        if (needles.isEmpty()) return false;
        HashMap<String, Integer> m = new HashMap<>();
        for (String s : haystack) m.put(s, m.getOrDefault(s, 0) + 1);
        for (String n : needles) {
            Integer c = m.get(n);
            if (c == null || c <= 0) return false;
            m.put(n, c - 1);
        }
        return true;
    }

    private static void drawBorder(GuiGraphics gg, int x1, int y1, int x2, int y2, int color) {
        gg.fill(x1, y1, x2, y1 + 1, color);
        gg.fill(x1, y2 - 1, x2, y2, color);
        gg.fill(x1, y1, x1 + 1, y2, color);
        gg.fill(x2 - 1, y1, x2, y2, color);
    }

    private static long lastAutoClickMs = 0L;
    private static void clickCorrectSlot(AbstractContainerScreen<?> cs, Set<Slot> correct) {
        long now = System.currentTimeMillis();
        if (now - lastAutoClickMs < 500) return; // 500ms debounce
        
        for (Slot slot : correct) {
            // Check if slot still contains an item (to avoid clicking empty)
            if (slot.hasItem()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gameMode != null && mc.player != null) {
                    mc.gameMode.handleInventoryMouseClick(cs.getMenu().containerId, slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                    lastAutoClickMs = now;
                    return;
                }
            }
        }
    }

    private static void initReflection() {
        try {
            F_LEFT_POS = AbstractContainerScreen.class.getDeclaredField("leftPos");
            F_LEFT_POS.setAccessible(true);

            F_TOP_POS = AbstractContainerScreen.class.getDeclaredField("topPos");
            F_TOP_POS.setAccessible(true);
        } catch (Throwable ignored) {
            F_LEFT_POS = null;
            F_TOP_POS = null;
        }
    }

    private static int getLeftPos(AbstractContainerScreen<?> cs) {
        if (F_LEFT_POS == null) return Integer.MIN_VALUE;
        try { return F_LEFT_POS.getInt(cs); }
        catch (Throwable ignored) { return Integer.MIN_VALUE; }
    }

    private static int getTopPos(AbstractContainerScreen<?> cs) {
        if (F_TOP_POS == null) return Integer.MIN_VALUE;
        try { return F_TOP_POS.getInt(cs); }
        catch (Throwable ignored) { return Integer.MIN_VALUE; }
    }

    public static boolean isClickAllowed(Screen screen, Slot hovered) {
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return true;
        if (hovered == null) return true;

        State st = STATES.get(screen);
        if (st == null) return true;

        updateState(cs, st);
        if (st.targetId == null && !st.isCaptcha) return true;

        Set<Slot> correct = (st.cachedCorrectSlots != null)
                ? st.cachedCorrectSlots
                : (st.cachedCorrectSlots = findCorrectSlots(cs, st.targetId));

        if (correct.isEmpty()) {
            return st.targetId == null;
        }

        return correct.contains(hovered);
    }

    private static final class State {
        String lastTitle = "";
        String targetId = null;
        boolean isCaptcha = false;
        Set<Slot> cachedCorrectSlots = null;
    }
}
