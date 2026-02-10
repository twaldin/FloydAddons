package floydaddons.not.dogshit.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Registers /floydaddons and /fa client commands.
 */
public final class FloydAddonsCommand {
    private FloydAddonsCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommand("floydaddons"));
            dispatcher.register(buildCommand("fa"));
            dispatcher.register(buildStalkCommand());
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
        return ClientCommandManager.literal(name)
                .executes(FloydAddonsCommand::openGui)
                .then(ClientCommandManager.literal("stalk")
                        .then(ClientCommandManager.argument("ign", StringArgumentType.word())
                                .executes(FloydAddonsCommand::stalkPlayer))
                        .executes(FloydAddonsCommand::stalkToggle))
                .then(ClientCommandManager.literal("s")
                        .then(ClientCommandManager.argument("ign", StringArgumentType.word())
                                .executes(FloydAddonsCommand::stalkPlayer))
                        .executes(FloydAddonsCommand::stalkToggle));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildStalkCommand() {
        return ClientCommandManager.literal("stalk")
                .then(ClientCommandManager.argument("ign", StringArgumentType.word())
                        .executes(FloydAddonsCommand::stalkPlayer))
                .executes(FloydAddonsCommand::stalkToggle);
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(new FloydAddonsScreen(Text.literal("FloydAddons"))));
        return Command.SINGLE_SUCCESS;
    }

    private static int stalkPlayer(CommandContext<FabricClientCommandSource> context) {
        String ign = StringArgumentType.getString(context, "ign");
        StalkManager.setTarget(ign);
        context.getSource().sendFeedback(Text.literal("\u00a7aStalking \u00a7f" + ign));
        return Command.SINGLE_SUCCESS;
    }

    private static int stalkToggle(CommandContext<FabricClientCommandSource> context) {
        if (StalkManager.isEnabled()) {
            String old = StalkManager.getTargetName();
            StalkManager.disable();
            context.getSource().sendFeedback(Text.literal("\u00a7cStopped stalking \u00a7f" + old));
        } else {
            context.getSource().sendFeedback(Text.literal("\u00a7cUsage: /fa stalk <ign>"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
