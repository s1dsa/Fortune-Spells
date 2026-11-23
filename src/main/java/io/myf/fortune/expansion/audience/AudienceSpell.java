package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AudienceSpell extends AbstractSpell {
    private static final ResourceLocation ID = FortuneIds.id("audience");
    private static final String SCROLL_ONLY = "message.fortune_spells.audience.scroll_only";
    private final DefaultConfig config = new DefaultConfig().setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(1).setCooldownSeconds(0).build();

    public AudienceSpell() {
        castTime = 35 * 20;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public boolean allowCrafting() { return false; } // The cauldron brews the scroll.

    @Override
    public int getEffectiveCastTime(int level, @Nullable LivingEntity caster) {
        return Math.max(31 * 20, super.getEffectiveCastTime(level, caster));
    }

    @Override
    public CastResult canBeCastedBy(int level, CastSource source, MagicData data, Player player) {
        if (source != CastSource.SCROLL) {
            return new CastResult(CastResult.Type.FAILURE, Component.translatable(SCROLL_ONLY));
        }
        return super.canBeCastedBy(level, source, data, player);
    }

    @Override
    public void onServerPreCast(Level world, int level, LivingEntity caster, MagicData data) {
        super.onServerPreCast(world, level, caster, data);
        if (caster instanceof ServerPlayer player && data.getCastSource() == CastSource.SCROLL)
            AudienceRitual.beginDrawing(player, data.getCastDuration());
    }

    @Override
    public void onServerCastComplete(Level world, int level, LivingEntity caster, MagicData data, boolean cancelled) {
        if (caster instanceof ServerPlayer player) AudienceRitual.endDrawing(player);
        super.onServerCastComplete(world, level, caster, data, cancelled);
    }

    @Override
    public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (source == CastSource.SCROLL && caster instanceof ServerPlayer player) {
            AudienceRitual.summon(player);
            super.onCast(world, level, caster, source, data);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable(SCROLL_ONLY), Component.translatable("message.fortune_spells.audience.upkeep"));
    }
}
