package com.bizcub.autoTp.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Duration;

@Mixin(LocateCommand.class)
public class LocateCommandMixin {

    @Inject(method = "showLocateResult*", at = @At("TAIL"))
    private static void tpPlayer(CommandSourceStack source, BlockPos sourcePos, Pair<BlockPos, ? extends Holder<?>> found, String successMessageKey, boolean includeY, String foundName, Duration taskDuration, CallbackInfoReturnable<Integer> cir) {
        if (source.getEntity() != null) {
            ServerPlayer player = source.getPlayer();
            Level level = player.level();

            int blockX = found.getFirst().getX();
            int blockZ = found.getFirst().getZ();

            // Force-load the chunk
            int chunkX = SectionPos.blockToSectionCoord(blockX);
            int chunkZ = SectionPos.blockToSectionCoord(blockZ);
            level.getChunkSource().getChunk(chunkX, chunkZ, true);

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);

            // Display previous coordinates in the chat
            source.sendSystemMessage(Component.translatableWithFallback(
                    "autoTp.message",
                    "Player %s was teleported from coordinates %s",
                    source.getPlayer().getDisplayName().getString(),
                    Component.literal("[" + sourcePos.toShortString() + "]").withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            //~ if >=1.21.5 'ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ' -> 'ClickEvent.SuggestCommand('
                            .withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + sourcePos.getX() + " " + sourcePos.getY() + " " + sourcePos.getZ()))
                            //~ if >=1.21.5 'HoverEvent(HoverEvent.Action.SHOW_TEXT, ' -> 'HoverEvent.ShowText('
                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                    )
            ));

            player.teleportTo(blockX + .5, surfaceY, blockZ + .5);
        }
    }
}
