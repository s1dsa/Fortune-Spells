package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CataclysmSpell extends AbstractSpell {
    public static final ResourceLocation ID = FortuneIds.id("cataclysm");
    public static final int CHANT_TICKS = 600;
    private final DefaultConfig config = new DefaultConfig().setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(1).setCooldownSeconds(0).build();

    public CataclysmSpell() {
        castTime = CHANT_TICKS;
        baseSpellPower = 1;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getEffectiveCastTime(int spellLevel, LivingEntity caster) { return CHANT_TICKS; }
    @Override public boolean allowLooting() { return false; }
    @Override public boolean allowCrafting() { return false; }
    @Override public boolean canBeCraftedBy(Player player) { return false; }

    @Override
    public boolean attemptInitiateCast(ItemStack stack, int level, Level world, Player player, CastSource source,
                                       boolean cooldown, String slot) {
        if (source != CastSource.SWORD || !AudienceStaffItem.isStaffSlot(player, slot)) return false;
        return super.attemptInitiateCast(stack, level, world, player, source, cooldown, slot);
    }

    @Override
    public CastResult canBeCastedBy(int level, CastSource source, MagicData data, Player player) {
        var selection = new SpellSelectionManager(player).getSelection();
        if (source != CastSource.SWORD || selection == null || selection.spellData.getSpell() != this
                || !AudienceStaffItem.isStaffSlot(player, selection.slot))
            return new CastResult(CastResult.Type.FAILURE, Component.translatable("message.fortune_spells.cataclysm.staff_only"));
        return super.canBeCastedBy(level, source, data, player);
    }

    @Override public void onServerPreCast(Level world, int level, LivingEntity caster, MagicData data) {
        super.onServerPreCast(world, level, caster, data);
        if (caster instanceof ServerPlayer player && data.getCastSource() == CastSource.SWORD
                && AudienceStaffItem.isStaffSlot(player, data.getCastingEquipmentSlot())) CataclysmField.begin(player);
    }

    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (!(caster instanceof ServerPlayer player) || source != CastSource.SWORD
                || !AudienceStaffItem.isStaffSlot(player, data.getCastingEquipmentSlot())) return;
        var field = CataclysmField.active(player);
        if (field == null || !field.release((float)(player.getAttributeValue(AttributeRegistry.MAX_MANA.get())
                * getSpellPower(level, player) * 50.0))) return;
        data.setMana(0);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        super.onCast(world, level, caster, source, data);
    }

    @Override public void onServerCastComplete(Level world, int level, LivingEntity caster, MagicData data, boolean cancelled) {
        if (caster instanceof ServerPlayer player) {
            var field = CataclysmField.active(player);
            if (field != null && !field.released()) field.discard();
        }
        super.onServerCastComplete(world, level, caster, data, cancelled);
    }
}
