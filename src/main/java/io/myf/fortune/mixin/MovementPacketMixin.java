package io.myf.fortune.mixin;

import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class MovementPacketMixin {
    @Shadow(aliases = "f_9736_", remap = false) private boolean clientIsFloating;

    @Inject(method = {"handleMovePlayer", "m_7185_"}, at = @At("RETURN"), remap = false)
    private void fortuneSpells$allowBurrowHover(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        var player = ((ServerGamePacketListenerImpl)(Object)this).player;
        if (player.hasEffect(MovementSpells.BOG_BURROW.get()) && !player.hasEffect(MovementSpells.PHASING.get()))
            clientIsFloating = false;
    }

    @Inject(method = {"isPlayerCollidingWithAnythingNew", "m_288208_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortuneSpells$acceptPhasing(LevelReader level, AABB before, double x, double y, double z,
                                            CallbackInfoReturnable<Boolean> ci) {
        var player = ((ServerGamePacketListenerImpl)(Object)this).player;
        if (player.hasEffect(MovementSpells.PHASING.get())) {
            // The listener has already moved the player, so measure from the saved pre-move box.
            double dx = x - (before.minX + before.maxX) * 0.5;
            double dy = y - before.minY;
            double dz = z - (before.minZ + before.maxZ) * 0.5;
            ci.setReturnValue(dx * dx + dy * dy + dz * dz > 12 * 12);
            return;
        }
        if (!player.hasEffect(MovementSpells.BOG_BURROW.get())) return;
        AABB requested = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
        AABB sweep = before.minmax(requested);
        ci.setReturnValue(!MovementSpells.withinBorder(player.level().getWorldBorder(), requested)
                || requested.minY < player.level().getMinBuildHeight() || requested.maxY > player.level().getMaxBuildHeight()
                || sweep.getXsize() > 12 || sweep.getYsize() > 12 || sweep.getZsize() > 12
                || MovementSpells.protectedBlocks(player, sweep));
    }
}
