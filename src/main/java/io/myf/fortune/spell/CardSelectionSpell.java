package io.myf.fortune.spell;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneTuning;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class CardSelectionSpell extends CardGameSpell {
    public CardSelectionSpell() {
        super(FortuneIds.SPELL_CARD_SELECTION, FortuneIds.Mode.FATES_CHOICE, SpellRarity.EPIC,
                FortuneTuning.CHOICE_MAX_LEVEL, FortuneTuning.CHOICE_COOLDOWN_SECONDS,
                FortuneTuning.CHOICE_MANA, FortuneTuning.CHOICE_MANA_PER_LEVEL);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(FortuneIds.LANG_UI + "chosen_duration", FortuneTuning.chosenDurationSeconds(spellLevel)),
                Component.translatable(FortuneIds.LANG_UI + "card_count", FortuneTuning.cardCount(FortuneIds.Mode.FATES_CHOICE, spellLevel)));
    }
}
