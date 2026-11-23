package io.myf.fortune.expansion;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.myf.fortune.expansion.movement.MovementSpells;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import io.myf.fortune.expansion.offense.OffensiveControl;
import io.myf.fortune.expansion.offense.SpellFigure;
import io.myf.fortune.expansion.rose.RoseGuard;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class BossSpell extends AbstractSpell {
    public enum Kind {
        PHASE_SHIFT(4, SpellRarity.EPIC, 0, 60, 10, 50),
        BURROW(6, SpellRarity.UNCOMMON, 40, 30, 0, 30),
        CROC_DEVOUR(1, SpellRarity.LEGENDARY, 200, 300, 0, 1000),
        PHANTOM_BELFRY(1, SpellRarity.LEGENDARY, 200, 300, 0, 1000),
        BELL_SHOCKWAVE(5, SpellRarity.RARE, 40, 10, 2, 25),
        ROSE_WRAITHS(1, SpellRarity.LEGENDARY, 60, 120, 0, 100),
        SUNSET_BLADE_DANCE(1, SpellRarity.LEGENDARY, SpellFigure.DANCE_TICKS, 300, 0, 1000),
        BONE_WINGS(1, SpellRarity.LEGENDARY, 100, 300, 0, 1000);

        public final String path = name().toLowerCase(Locale.ROOT);
        final int levels, chantTicks, cooldown, cooldownStep, mana;
        final SpellRarity minimum;
        Kind(int levels, SpellRarity minimum, int chantTicks, int cooldown, int cooldownStep, int mana) {
            this.levels = levels;
            this.minimum = minimum;
            this.chantTicks = chantTicks;
            this.cooldown = cooldown;
            this.cooldownStep = cooldownStep;
            this.mana = mana;
        }
    }

    private final Kind kind;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public BossSpell(Kind kind) {
        this.kind = kind;
        id = FortuneIds.id(kind.path);
        baseSpellPower = 1;
        baseManaCost = kind.mana;
        manaCostPerLevel = kind == Kind.BELL_SHOCKWAVE ? 5 : 0;
        castTime = kind.chantTicks;
        var school = switch (kind) {
            case PHASE_SHIFT, PHANTOM_BELFRY, BELL_SHOCKWAVE -> "bell";
            case BURROW, CROC_DEVOUR, BONE_WINGS -> "swamp";
            case ROSE_WRAITHS, SUNSET_BLADE_DANCE -> "rose";
        };
        config = new DefaultConfig().setSchoolResource(FortuneIds.id(school)).setMinRarity(kind.minimum)
                .setMaxLevel(kind.levels).setCooldownSeconds(kind.cooldown).build();
    }

    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return castTime == 0 ? CastType.INSTANT : CastType.LONG; }
    @Override public int getEffectiveCastTime(int level, LivingEntity caster) {
        return kind == Kind.SUNSET_BLADE_DANCE ? SpellFigure.DANCE_TICKS : super.getEffectiveCastTime(level, caster);
    }

    @Override public AnimationHolder getCastStartAnimation() {
        return kind == Kind.SUNSET_BLADE_DANCE ? AnimationHolder.none() : super.getCastStartAnimation();
    }
    @Override public AnimationHolder getCastFinishAnimation() {
        return kind == Kind.SUNSET_BLADE_DANCE ? AnimationHolder.none() : super.getCastFinishAnimation();
    }
    @Override public SpellRarity getRarity(int level) { return ExpansionCrafting.rarity(kind.path, level); }
    @Override public int getMinLevelForRarity(SpellRarity ink) { return ExpansionCrafting.minLevel(kind.path, ink); }

    @Override
    public boolean checkPreCastConditions(Level world, int level, LivingEntity caster, MagicData data) {
        return kind != Kind.BONE_WINGS || !caster.hasEffect(MovementSpells.BONE_WINGS.get());
    }

    // The native cooldown event occurs after onCast and doesn't expose spell level.
    private record CastInput(BossSpell spell, LivingEntity caster, int level) {}
    private static final ThreadLocal<CastInput> INPUT = new ThreadLocal<>();

    @Override
    public void castSpell(Level world, int level, ServerPlayer player, CastSource source, boolean triggerCooldown) {
        var data = MagicData.getPlayerMagicData(player);
        if (kind == Kind.SUNSET_BLADE_DANCE && data.isCasting() && getSpellId().equals(data.getCastingSpellId())
                && source == data.getCastSource()) {
            // This cast paid at the opening attack. Iron's still owns scroll consumption and cast completion.
            OffensiveControl.finishDance(player);
            return;
        }
        if (requiresRitualAnchor() && !OffensiveSpells.canRelease(kind.path,player,data.isCasting())) {
            if (data.isCasting()) Utils.serverSideCancelCast(player,false);
            else OffensiveSpells.interruptChant(player);
            return;
        }
        if (!checkPreCastConditions(world, level, player, data)) return;
        CastInput previous = INPUT.get();
        INPUT.set(new CastInput(this, player, level));
        try {
            super.castSpell(world, level, player, source, triggerCooldown);
        } finally {
            if (previous == null) INPUT.remove();
            else INPUT.set(previous);
        }
    }

    @SubscribeEvent
    public static void adjustCooldown(SpellCooldownAddedEvent.Pre event) {
        CastInput input = INPUT.get();
        if (input == null || input.spell != event.getSpell() || input.caster != event.getEntity()) return;
        event.setEffectiveCooldown(input.spell.cooldownFor(input.level, event.getEffectiveCooldown()));
    }

    public int cooldownFor(int level, int firstLevelTicks) {
        return Math.round(firstLevelTicks * (1F + (level - 1) * kind.cooldownStep / (float) kind.cooldown));
    }

    private boolean requiresRitualAnchor() {
        return kind == Kind.CROC_DEVOUR || kind == Kind.PHANTOM_BELFRY;
    }

    private boolean hasChantCircle() {
        return requiresRitualAnchor() || kind == Kind.BONE_WINGS;
    }

    private boolean empowered(LivingEntity caster, CastSource source, MagicData data) {
        if (source != CastSource.SWORD || !ExpansionCrafting.transcendent(this) || data == null) return false;
        var item = data.getPlayerCastingItem();
        // Iron's cast key supplies an empty stack and the selected hand. Right-click supplies the item.
        return item.isEmpty() ? AudienceStaffItem.isStaffSlot(caster, data.getCastingEquipmentSlot())
                : item.getItem() instanceof AudienceStaffItem;
    }

    @Override
    public void onServerPreCast(Level world, int level, LivingEntity caster, MagicData data) {
        super.onServerPreCast(world,level,caster,data);
        if (kind == Kind.SUNSET_BLADE_DANCE && caster instanceof ServerPlayer player) {
            // The dance attacks during the channel, so pay once up front even if it gets interrupted.
            super.castSpell(world, level, player, data.getCastSource(), true);
        } else if (hasChantCircle()) {
            OffensiveSpells.beginChant(kind.path,caster,empowered(caster,data.getCastSource(),data));
        }
    }

    @Override
    public void onServerCastTick(Level world, int level, LivingEntity caster, MagicData data) {
        if (kind == Kind.SUNSET_BLADE_DANCE && caster instanceof ServerPlayer player && data != null) {
            OffensiveControl.advanceDance(player, data.getCastDuration() - data.getCastDurationRemaining());
        } else if (data != null && hasChantCircle()) {
            int duration = Math.max(1, data.getCastDuration());
            OffensiveSpells.chant(kind.path, caster, 200 * (duration - data.getCastDurationRemaining()) / duration);
        }
    }

    @Override
    public void onServerCastComplete(Level world, int level, LivingEntity caster, MagicData data, boolean cancelled) {
        if (kind == Kind.SUNSET_BLADE_DANCE && cancelled && caster instanceof ServerPlayer player
                && data.isCasting() && getSpellId().equals(data.getCastingSpellId())) {
            OffensiveControl.cancelDance(player);
            // On successful LONG casts Iron's consumes the scroll before this callback.
            // Its cancellation path only consumes CONTINUOUS scrolls.
            if (data.getCastSource() == CastSource.SCROLL) Scroll.attemptRemoveScrollAfterCast(player);
        }
        // MagicManager calls completion again after castSpell returns from a final-tick cancellation.
        if (requiresRitualAnchor() && !cancelled && !data.isCasting()) return;
        // A successful onCast already released and detached the circle.
        if (requiresRitualAnchor()) OffensiveSpells.interruptChant(caster);
        if (kind == Kind.BONE_WINGS) OffensiveSpells.endWingChant(caster);
        super.onServerCastComplete(world,level,caster,data,cancelled);
    }

    @Override
    public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (world.isClientSide) return;
        float power = getSpellPower(level, caster);
        switch (kind) {
            case PHASE_SHIFT, BURROW, BONE_WINGS -> MovementSpells.cast(kind.path, level, caster, power);
            case ROSE_WRAITHS -> RoseGuard.cast(caster, power);
            default -> {
                float maxMana = (float) caster.getAttributeValue(AttributeRegistry.MAX_MANA.get());
                OffensiveSpells.cast(kind.path, level, caster, power, maxMana, empowered(caster,source,data));
            }
        }
        super.onCast(world, level, caster, source, data);
    }
}
