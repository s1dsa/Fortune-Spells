package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.BossSpell;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TooltipsUtils.class, remap = false)
abstract class BossCooldownTooltipMixin {
    private static final String COOLDOWN = "Lio/redspace/ironsspellbooks/capabilities/magic/MagicManager;getEffectiveSpellCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;)I";

    @Redirect(method = "formatActiveSpellTooltip", at = @At(value = "INVOKE", target = COOLDOWN))
    private static int fortune$bookCooldown(AbstractSpell spell, Player player, CastSource source,
                                             ItemStack book, SpellData data, CastSource slot, LocalPlayer reader) {
        int base = MagicManager.getEffectiveSpellCooldown(spell, player, source);
        return spell instanceof BossSpell boss ? boss.cooldownFor(spell.getLevelFor(data.getLevel(), player), base) : base;
    }

    @Redirect(method = "formatScrollTooltip", at = @At(value = "INVOKE", target = COOLDOWN))
    private static int fortune$scrollCooldown(AbstractSpell spell, Player player, CastSource source, ItemStack scroll, Player reader) {
        int base = MagicManager.getEffectiveSpellCooldown(spell, player, source);
        if (!(spell instanceof BossSpell boss)) return base;
        int level = ISpellContainer.get(scroll).getSpellAtIndex(0).getLevel();
        return boss.cooldownFor(spell.getLevelFor(level, player), base);
    }
}
