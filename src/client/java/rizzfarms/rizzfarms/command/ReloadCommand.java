package rizzfarms.rizzfarms.command;

import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.util.WordDb;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class ReloadCommand {

    private ReloadCommand() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("cgreload")
                .executes(ctx -> {
                    ModConfig.load();
                    WordDb.load();
                    ctx.getSource().sendFeedback(Component.literal("[rizzfarms] Reloaded config + words"));
                    return 1;
                })
        );
    }
}
