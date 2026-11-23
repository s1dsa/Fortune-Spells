package io.myf.fortune.spell;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneTuning;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CFortuneFx;
import io.myf.fortune.registry.FSEffects;
import io.myf.fortune.registry.FSSounds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class AllInSpell extends AbstractSpell {
    private final DefaultConfig defaultConfig = new DefaultConfig().setSchoolResource(FortuneIds.SCHOOL_FORTUNE)
            .setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(FortuneTuning.ALL_IN_MAX_LEVEL)
            .setCooldownSeconds(FortuneTuning.ALL_IN_COOLDOWN_SECONDS).build();

    public AllInSpell() {
        this.baseManaCost = FortuneTuning.ALL_IN_MANA;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return FortuneIds.SPELL_ALL_IN;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(FSSounds.ALL_IN_CAST.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable(FortuneIds.LANG_UI + "next_hit_double"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magicData) {
        if (level.isClientSide) {
            return;
        }
        caster.addEffect(new MobEffectInstance(FSEffects.ALL_IN.get(), MobEffectInstance.INFINITE_DURATION));
        FSNetwork.sendTracking(caster, new S2CFortuneFx(FortuneIds.FxKind.ALL_IN_PRIMED, caster.getId(), 0));
        super.onCast(level, spellLevel, caster, source, magicData);
    }
}
