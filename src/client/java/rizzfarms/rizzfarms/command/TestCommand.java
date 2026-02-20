package rizzfarms.rizzfarms.command;

import rizzfarms.rizzfarms.RizzfarmsClient;
import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.core.MathSolver;
import rizzfarms.rizzfarms.core.PendingSend;
import rizzfarms.rizzfarms.gui.CapGuiAssist;
import rizzfarms.rizzfarms.util.TextUtil;
import rizzfarms.rizzfarms.util.Timing;
import rizzfarms.rizzfarms.util.WordDb;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class TestCommand {

    private TestCommand() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("cgtest")
                .then(argument("mode", StringArgumentType.word())
                        .then(argument("payload", StringArgumentType.greedyString())
                                .executes(TestCommand::runGreedy)))
                .executes(ctx -> {
                    reply(ctx, "Usage: /cgtest <reverse|math|seq|blanks|unscramble|gui> <text/item>");
                    reply(ctx, "GUI test example:");
                    reply(ctx, "  /cgtest gui anything");
                    return 1;
                })
        );
    }

    private static int runGreedy(CommandContext<FabricClientCommandSource> ctx) {
        String mode = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);
        String payload = StringArgumentType.getString(ctx, "payload");

        switch (mode) {
            case "reverse": {
                String w = payload.trim();
                if (w.isEmpty()) return fail(ctx, "reverse requires text");
                String ans = new StringBuilder(w).reverse().toString();
                PendingSend.arm(Timing.tLetters(w.length()), ans, "test reverse");
                reply(ctx, "Queued reverse → READY soon");
                return 1;
            }
            case "seq":
            case "sequence": {
                String s = payload.trim();
                if (s.isEmpty()) return fail(ctx, "seq requires text");
                PendingSend.arm(Timing.tSequence(s.length()), s, "test sequence");
                reply(ctx, "Queued sequence → READY soon");
                return 1;
            }
            case "math": {
                String expr = payload.trim();
                if (expr.isEmpty()) return fail(ctx, "math requires expression");
                String ans = MathSolver.solve(expr);
                if (ans == null) return fail(ctx, "math parse failed");
                PendingSend.arm(Timing.tMath(expr), ans, "test math");
                reply(ctx, "Queued math → READY soon");
                return 1;
            }
            case "blanks":
            case "blank": {
                String pattern = payload.trim().toLowerCase(Locale.ROOT);
                if (pattern.isEmpty()) return fail(ctx, "blanks requires pattern like outstan_ing");

                String guess = WordDb.findFirstBlankMatch(pattern, ModConfig.cfg().blankScanCap);
                if (guess == null) return fail(ctx, "no match in word DB");

                int blanks = TextUtil.countChar(pattern, '_');
                PendingSend.arm(Timing.tBlanks(blanks), guess, "test blanks");
                reply(ctx, "Queued blanks → READY soon (guess: " + guess + ")");
                return 1;
            }
            case "unscramble": {
                String s = payload.trim().toLowerCase(Locale.ROOT);
                if (s.isEmpty()) return fail(ctx, "unscramble requires text");

                String ans = WordDb.unscrambleFirst(s);
                if (ans == null) return fail(ctx, "no anagram match in DB");

                PendingSend.arm(Timing.tLetters(ans.length()), ans, "test unscramble");
                reply(ctx, "Queued unscramble → READY soon (guess: " + ans + ")");
                return 1;
            }
            case "gui": {
                return runCapGuiTest(ctx);
            }
            default:
                reply(ctx, "Unknown mode: " + mode + " (use reverse|math|seq|blanks|unscramble|gui)");
                return 1;
        }
    }

    /**
     * CAP-style visual test:
     * - 7x4 region populated with items (shuffled)
     * - Title: "Click on <TARGET>" (uppercase)
     * - We intentionally do NOT try to look like a full chest with player inventory.
     */
    private static int runCapGuiTest(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return fail(ctx, "player is null");

        List<Item> pool = new ArrayList<>();
        pool.add(Items.HEART_OF_THE_SEA);
        pool.add(Items.SHIELD);
        pool.add(Items.FIRE_CHARGE);
        pool.add(Items.ENDER_EYE);
        pool.add(Items.COMPASS);
        pool.add(Items.IRON_PICKAXE);
        pool.add(Items.DIAMOND_HELMET);
        pool.add(Items.CARROT_ON_A_STICK);
        pool.add(Items.TOTEM_OF_UNDYING);
        pool.add(Items.FLINT_AND_STEEL);
        pool.add(Items.BOW);
        pool.add(Items.TRIDENT);
        pool.add(Items.SHEARS);
        pool.add(Items.BLAZE_ROD);
        pool.add(Items.FISHING_ROD);
        pool.add(Items.SLIME_BALL);
        pool.add(Items.DIAMOND_SWORD);
        pool.add(Items.GOLDEN_APPLE);
        pool.add(Items.MAGMA_CREAM);
        pool.add(Items.ELYTRA);
        pool.add(Items.GHAST_TEAR);
        pool.add(Items.NETHER_STAR);
        pool.add(Items.RABBIT_FOOT);
        pool.add(Items.BEACON);
        pool.add(Items.CLOCK);
        pool.add(Items.ENCHANTED_GOLDEN_APPLE);
        pool.add(Items.ENCHANTED_BOOK);

        RandomSource rnd = RandomSource.create();

        Item target = pool.get(rnd.nextInt(pool.size()));
        String targetName = Component.translatable(target.getDescriptionId()).getString();
        Component title = Component.literal("Click on " + targetName.toUpperCase(Locale.ROOT));

        // Backing container size doesn't matter much for visuals; we keep 6*9 like before
        int rows = 6;
        int size = rows * 9;
        SimpleContainer container = new SimpleContainer(size);
        for (int i = 0; i < size; i++) container.setItem(i, ItemStack.EMPTY);

        // CAP region: cols 1..7, rows 1..4
        List<Integer> regionSlots = new ArrayList<>(28);
        int startRow = 1, startCol = 1;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 7; c++) {
                regionSlots.add((startRow + r) * 9 + (startCol + c));
            }
        }

        Collections.shuffle(regionSlots, new Random(rnd.nextLong()));
        List<Item> itemsToPlace = new ArrayList<>(pool);
        Collections.shuffle(itemsToPlace, new Random(rnd.nextLong()));

        int placeCount = Math.min(itemsToPlace.size(), regionSlots.size());
        for (int i = 0; i < placeCount; i++) {
            container.setItem(regionSlots.get(i), new ItemStack(itemsToPlace.get(i)));
        }

        ChestMenu menu = ChestMenu.sixRows(0, mc.player.getInventory(), container);

        // Pass regionSlots so the screen only draws those slot boxes (no weird extras)
        Set<Integer> regionSet = new HashSet<>(regionSlots);
        mc.execute(() -> mc.setScreen(new TestChestScreen(menu, mc.player.getInventory(), title, regionSet)));

        reply(ctx, "Opened CAP-style GUI test.");
        return 1;
    }

    private static void reply(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Component.literal("[cgtest] " + msg));
    }

    private static int fail(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendError(Component.literal("[cgtest] " + msg));
        return 0;
    }

    /**
     * Draw ONLY the CAP 7x4 region slot boxes.
     * Do NOT draw player inventory/hotbar slot boxes (removes the "weird extra slots" look).
     */
    private static final class TestChestScreen extends AbstractContainerScreen<ChestMenu> {

        private final Set<Integer> regionSlotIndices;

        TestChestScreen(ChestMenu menu, Inventory inv, Component title, Set<Integer> regionSlotIndices) {
            super(menu, inv, title);
            this.regionSlotIndices = regionSlotIndices;

            // Small panel; not a full chest look.
            this.imageWidth = 176;
            this.imageHeight = 140; // tighter like CAP
            this.inventoryLabelY = 9999; // push label off-screen
        }

        @Override
        protected void renderBg(@NotNull GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
            int x0 = this.leftPos;
            int y0 = this.topPos;

            // Panel background
            gg.fill(x0, y0, x0 + this.imageWidth, y0 + this.imageHeight, 0xFF2B2B2B);
            gg.fill(x0, y0, x0 + this.imageWidth, y0 + 16, 0xFF222222);

            // Draw slot boxes ONLY for region slots
            for (int i = 0; i < this.menu.slots.size(); i++) {
                if (!regionSlotIndices.contains(i)) continue;

                Slot slot = this.menu.slots.get(i);
                drawSlotBox(gg, x0 + slot.x, y0 + slot.y);
            }
        }

        private static void drawSlotBox(GuiGraphics gg, int x, int y) {
            gg.fill(x - 1, y - 1, x + 17, y + 17, 0xFF1D1D1D);
            gg.fill(x, y, x + 16, y + 16, 0xFF4A4A4A);
            gg.fill(x, y, x + 16, y + 1, 0xFF606060);
            gg.fill(x, y, x + 1, y + 16, 0xFF606060);
        }

        @Override
        public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(gg, mouseX, mouseY, partialTick);
            super.render(gg, mouseX, mouseY, partialTick);
            this.renderTooltip(gg, mouseX, mouseY);
        }
    }
}
