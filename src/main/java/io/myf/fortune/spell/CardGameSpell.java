package io.myf.fortune.spell;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneTuning;
import io.myf.fortune.cardgame.CardGameManager;
import io.myf.fortune.registry.FSSounds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

abstract class CardGameSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final byte mode;
    private final DefaultConfig defaultConfig;

    protected CardGameSpell(ResourceLocation spellId, byte mode, SpellRarity rarity, int maxLevel,
                            int cooldownSeconds, int mana, int manaPerLevel) {
        this.spellId = spellId;
        this.mode = mode;
        this.baseManaCost = mana;
        this.manaCostPerLevel = manaPerLevel;
        this.castTime = FortuneTuning.CARD_CAST_TICKS;
        this.defaultConfig = new DefaultConfig().setSchoolResource(FortuneIds.SCHOOL_FORTUNE)
                .setMinRarity(rarity).setMaxLevel(maxLevel).setCooldownSeconds(cooldownSeconds).build();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(FSSounds.SPELL_FORTUNE_CHARGE.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (caster instanceof ServerPlayer player && CardGameManager.hasSession(player.getUUID())) {
            player.displayClientMessage(Component.translatable(FortuneIds.LANG_UI + "cast_error_in_game"), true);
            return false;
        }
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magicData) {
        if (caster instanceof ServerPlayer player) {
            CardGameManager.start(player, mode, spellLevel);
            super.onCast(level, spellLevel, caster, source, magicData);
        }
    }
}
