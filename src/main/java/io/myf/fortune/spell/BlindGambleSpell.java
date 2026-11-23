package io.myf.fortune.spell;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneTuning;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class BlindGambleSpell extends CardGameSpell {
    public BlindGambleSpell() {
        super(FortuneIds.SPELL_BLIND_GAMBLE, FortuneIds.Mode.BLIND_GAMBLE, SpellRarity.LEGENDARY,
                FortuneTuning.BLIND_MAX_LEVEL, FortuneTuning.BLIND_COOLDOWN_SECONDS, FortuneTuning.BLIND_MANA, 0);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int ticksPerMinute = FortuneTuning.TICKS_PER_SECOND * 60;
        return List.of(
                Component.translatable(FortuneIds.LANG_UI + "card_count", FortuneTuning.cardCount(FortuneIds.Mode.BLIND_GAMBLE, spellLevel)),
                Component.translatable(FortuneIds.LANG_UI + "blind_win", FortuneTuning.BLIND_WIN_TICKS / ticksPerMinute),
                Component.translatable(FortuneIds.LANG_UI + "blind_lose", FortuneTuning.BLIND_LOSE_TICKS / ticksPerMinute));
    }
}
