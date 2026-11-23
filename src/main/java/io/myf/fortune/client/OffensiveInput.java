package io.myf.fortune.client;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import io.myf.fortune.expansion.offense.OffensiveControl;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public final class OffensiveInput {
    private OffensiveInput() {}

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if (!event.getEntity().hasEffect(OffensiveSpells.STUNNED.get())) return;
        var in = event.getInput();
        in.leftImpulse = in.forwardImpulse = 0;
        in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
    }

    @SubscribeEvent public static void action(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (player != null && (player.hasEffect(OffensiveSpells.STUNNED.get()) ||
                ((event.isAttack() || event.isUseItem()) && ClientMagicData.isCasting()
                        && OffensiveControl.DANCE_ID.equals(ClientMagicData.getCastingSpellId())))) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
