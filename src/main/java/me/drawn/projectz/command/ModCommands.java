package me.drawn.projectz.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.drawn.projectz.ProjectZ;
import me.drawn.projectz.config.LootConfigManager;
import me.drawn.projectz.entity.AirdropCrateEntity;
import me.drawn.projectz.network.AirdropVisualPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("projectz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("airdrop")
                        .then(Commands.argument("lootId", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(LootConfigManager.getLootPoints().stream().map(p -> p.lootId).distinct(), builder))
                                .executes(context -> {
                                    String lootId = StringArgumentType.getString(context, "lootId");
                                    return triggerAirdrop(context.getSource(), lootId);
                                })
                        )
                )
                .then(Commands.literal("loot")
                        .then(Commands.argument("lootId", StringArgumentType.string())
                                .then(Commands.argument("range", DoubleArgumentType.doubleArg(1.0))
                                        .executes(context -> {
                                            String lootId = StringArgumentType.getString(context, "lootId");
                                            double range = DoubleArgumentType.getDouble(context, "range");
                                            return createLootPoint(context.getSource(), lootId, range);
                                        })
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(context -> listLootPoints(context.getSource()))
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int index = IntegerArgumentType.getInteger(context, "index");
                                    return removeLootPoint(context.getSource(), index);
                                })
                        )
                )
                .then(Commands.literal("reload")
                        .executes(context -> reloadConfigs(context.getSource()))
                )
        );
    }

    private static int triggerAirdrop(CommandSourceStack source, String lootId) {
        if (LootConfigManager.getLootConfig(lootId) == null) {
            source.sendFailure(Component.literal("Loot ID nao encontrado nas configuracoes!"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos targetPos = BlockPos.containing(source.getPosition());

        String msg = String.format("Airdrop chegando em coordenadas X: %d, Y: %d, Z: %d", targetPos.getX(), targetPos.getY(), targetPos.getZ());
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);

        Random rand = new Random();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double radius = 6000F;

        double flightHeight = targetPos.getY() + 200.0;
        double startX = targetPos.getX() + Math.cos(angle) * radius;
        double startZ = targetPos.getZ() + Math.sin(angle) * radius;

        double endX = targetPos.getX() - Math.cos(angle) * radius;
        double endZ = targetPos.getZ() - Math.sin(angle) * radius;

        PacketDistributor.sendToPlayersInDimension(level, new AirdropVisualPayload(
                targetPos.getX(), flightHeight, targetPos.getZ(),
                startX, flightHeight, startZ,
                endX, flightHeight, endZ,
                LootConfigManager.config.planeSize,
                lootId,
                LootConfigManager.config.planeFlightDuration
        ));

        int spawnDelayTicks = LootConfigManager.config.planeFlightDuration / 2;
        ProjectZ.scheduler.schedule(spawnDelayTicks, () -> {
            AirdropCrateEntity crate = new AirdropCrateEntity(ProjectZ.AIRDROP_CRATE_ENTITY.get(), level);
            crate.setLootId(lootId);
            crate.setPos(targetPos.getX() + 0.5, flightHeight, targetPos.getZ() + 0.5);
            level.addFreshEntity(crate);

            level.playSound(null, targetPos, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
        });

        source.sendSuccess(() -> Component.literal("Airdrop iniciado com sucesso!"), true);
        return 1;
    }

    private static int createLootPoint(CommandSourceStack source, String lootId, double range) {
        if (LootConfigManager.getLootConfig(lootId) == null) {
            source.sendFailure(Component.literal("Loot ID nao encontrado!"));
            return 0;
        }

        BlockPos pos = BlockPos.containing(source.getPosition());
        String dim = source.getLevel().dimension().location().toString();

        LootConfigManager.LootPoint point = new LootConfigManager.LootPoint(pos, dim, lootId, range);
        LootConfigManager.addLootPoint(point);

        source.sendSuccess(() -> Component.literal(String.format("Ponto de loot adicionado em %s com range %.1f!", pos.toShortString(), range)), true);
        return 1;
    }

    private static int listLootPoints(CommandSourceStack source) {
        var points = LootConfigManager.getLootPoints();
        if (points.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nenhum ponto de loot registrado."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("=== Pontos de Loot Registrados ==="), false);
        for (int i = 0; i < points.size(); i++) {
            var p = points.get(i);
            String info = String.format("[%d] ID: %s | Dim: %s | Pos: %s | Range: %.1f",
                    i, p.lootId, p.dimension, p.pos.toShortString(), p.range);
            source.sendSuccess(() -> Component.literal(info), false);
        }
        return 1;
    }

    private static int removeLootPoint(CommandSourceStack source, int index) {
        if (LootConfigManager.removeLootPoint(index)) {
            source.sendSuccess(() -> Component.literal("Ponto de loot removido com sucesso."), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Indice invalido!"));
            return 0;
        }
    }

    private static int reloadConfigs(CommandSourceStack source) {
        LootConfigManager.loadGlobalConfig();
        LootConfigManager.loadLootTables();
        LootConfigManager.loadLootPoints();
        source.sendSuccess(() -> Component.literal("Configuracoes e tabelas recarregadas com sucesso!"), true);
        return 1;
    }
}