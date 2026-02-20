package rizzfarms.rizzfarms;

import rizzfarms.rizzfarms.util.TextUtil;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public final class GuiItemHighlighter {

    // Improved visual colors
    private static final int HIDE_OVERLAY = 0x77000000;  // Elegant semi-transparent black
    private static final int RIGHT_BORDER = 0xAA00FF00;  // Distinct bright green border
    private static final int RIGHT_TINT   = 0x3300FF00;  // Soft green tint

    private static final Map<String, Item> NAME_TO_ITEM = new HashMap<>();
    private static List<Entry<String, Item>> NAME_MATCH_ORDER = List.of();

    private static final Map<Screen, State> STATES = new WeakHashMap<>();

    private static Field F_LEFT_POS;
    private static Field F_TOP_POS;

    private GuiItemHighlighter() {}

    public static void init() {
        buildItemNameIndex();
        initReflection();

        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            State st = new State();
            STATES.put(screen, st);

            ScreenEvents.afterRender(screen).register((s, gg, mouseX, mouseY, tickDelta) -> {
                onAfterRender(s, gg);
            });

            ScreenEvents.remove(screen).register(s -> STATES.remove(s));
        });
    }

    private static void onAfterRender(Screen screen, GuiGraphics gg) {
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

        State st = STATES.get(screen);
        if (st == null) return;

        // Detect target item from GUI title
        if (st.targetItem == null) {
            String title = screen.getTitle().getString();
            st.targetItem = detectItemFromTitle(title);
            if (st.targetItem == null) return;
        }

        st.matchSlots = findMatchingSlotsByItem(cs, st.targetItem);
        if (st.matchSlots.isEmpty()) return;

        int left = getLeftPos(cs);
        int top = getTopPos(cs);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return;

        for (Slot slot : cs.getMenu().slots) {
            int x = left + slot.x;
            int y = top + slot.y;

            int x1 = x - 1;
            int y1 = y - 1;
            int x2 = x + 17;
            int y2 = y + 17;

            if (st.matchSlots.contains(slot)) {
                gg.fill(x1, y1, x2, y2, RIGHT_TINT);
                drawBorder(gg, x1, y1, x2, y2, RIGHT_BORDER);
            } else {
                gg.fill(x1, y1, x2, y2, HIDE_OVERLAY);
            }
        }
    }

    private static Set<Slot> findMatchingSlotsByItem(AbstractContainerScreen<?> cs, Item target) {
        Set<Slot> out = new HashSet<>();
        for (Slot s : cs.getMenu().slots) {
            ItemStack stack = s.getItem();
            if (!stack.isEmpty() && stack.getItem() == target) {
                out.add(s);
            }
        }
        return out;
    }

    private static void buildItemNameIndex() {
        NAME_TO_ITEM.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            String display = I18n.get(item.getDescriptionId());
            String dn = norm(display);
            if (!dn.isEmpty()) NAME_TO_ITEM.putIfAbsent(dn, item);

            String path = BuiltInRegistries.ITEM.getKey(item).getPath().replace('_', ' ');
            String pn = norm(path);
            if (!pn.isEmpty()) NAME_TO_ITEM.putIfAbsent(pn, item);
        }

        List<Entry<String, Item>> entries = new ArrayList<>(NAME_TO_ITEM.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        NAME_MATCH_ORDER = entries;
    }

    private static Item detectItemFromTitle(String title) {
        if (title == null) return null;
        String t = norm(title);

        for (Entry<String, Item> e : NAME_MATCH_ORDER) {
            if (!e.getKey().isEmpty() && t.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
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

    private static void drawBorder(GuiGraphics gg, int x1, int y1, int x2, int y2, int color) {
        gg.fill(x1, y1, x2, y1 + 1, color);
        gg.fill(x1, y2 - 1, x2, y2, color);
        gg.fill(x1, y1, x1 + 1, y2, color);
        gg.fill(x2 - 1, y1, x2, y2, color);
    }

    private static String norm(String s) {
        if (s == null) return "";
        String cleaned = TextUtil.stripFormatting(s).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(cleaned.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastWasSpace = false;
            } else {
                if (!lastWasSpace && sb.length() > 0) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            }
        }
        String out = sb.toString();
        if (out.endsWith(" ")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private static final class State {
        Item targetItem;
        Set<Slot> matchSlots = Collections.emptySet();
    }
}
