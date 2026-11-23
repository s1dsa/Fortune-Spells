package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class FSDamageTypes {
    public static final ResourceKey<DamageType> FORTUNE = ResourceKey.create(Registries.DAMAGE_TYPE, FortuneIds.DAMAGE_FORTUNE);
    public static final ResourceKey<DamageType> FORTUNE_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, FortuneIds.DAMAGE_FORTUNE_MAGIC);

    private FSDamageTypes() {}

    public static FortuneStrike fortune(Level level, LivingEntity attacker, DamageSource original) {
        return fortune(level, attacker, original, 0);
    }

    public static FortuneStrike fortune(Level level, LivingEntity attacker, DamageSource original, float incoming) {
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FORTUNE);
        return new FortuneStrike(type, attacker, original, incoming);
    }

    public static final class FortuneStrike extends DamageSource {
        private final DamageSource original;
        public final float incoming;

        private FortuneStrike(Holder<DamageType> type, LivingEntity attacker, DamageSource original, float incoming) {
            super(type, original.getDirectEntity(), attacker);
            this.original = original;
            this.incoming = incoming;
        }

        @Override
        public boolean scalesWithDifficulty() {
            return original.scalesWithDifficulty();
        }

        @Override
        public boolean is(TagKey<DamageType> tag) {
            if (tag.equals(DamageTypeTags.BYPASSES_COOLDOWN) || tag.equals(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return original.is(tag);
            }
            return super.is(tag);
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            return Component.translatable("death.attack." + getMsgId() + ".player",
                    victim.getDisplayName(), getEntity().getDisplayName());
        }
    }
}
