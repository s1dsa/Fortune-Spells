package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.AudienceSpell;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = RandomizeSpellFunction.class, remap = false)
abstract class AudienceLootMixin {
    // The same native loot function rolls both scrolls and imbued equipment.
    @Redirect(method = {"run", "m_7372_"}, at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/loot/SpellFilter;getApplicableSpells()Ljava/util/List;"))
    private List<AbstractSpell> fortune$scrollOnly(SpellFilter filter, ItemStack stack, LootContext context) {
        List<AbstractSpell> spells = filter.getApplicableSpells();
        boolean scroll = stack.getItem() instanceof Scroll;
        return spells.stream().filter(spell -> !AudienceStaffItem.exclusive(spell)
                && (scroll || !(spell instanceof AudienceSpell))).toList();
    }
}
