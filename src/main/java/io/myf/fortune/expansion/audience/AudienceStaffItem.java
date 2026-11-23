package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.UniqueItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class AudienceStaffItem extends CastingItem implements IPresetSpellContainer, UniqueItem {
    public static final int TRANSCENDENT_LEVEL = 10;
    private static final String[] SPELLS = {"croc_devour", "phantom_belfry", "sunset_blade_dance", "bone_wings", "cataclysm"};
    public static final int MAX_NAME_LENGTH = 32;
    private static final String ID = "AudienceStaffId";
    private static final String INSCRIBED = "AudienceStaffInscribed";
    private static final int[] INKS = {0xffc660, 0xff797e, 0xf089df, 0xae9bff, 0x76daff, 0x88edbb};

    public AudienceStaffItem() {
        super(new Properties().stacksTo(1));
    }

    public static boolean exclusive(AbstractSpell spell) {
        return spell.getSpellId().equals("fortune_spells:cataclysm");
    }

    public static boolean isStaffSlot(LivingEntity caster, String slot) {
        return (SpellSelectionManager.MAINHAND.equals(slot) && caster.getMainHandItem().getItem() instanceof AudienceStaffItem)
                || (SpellSelectionManager.OFFHAND.equals(slot) && caster.getOffhandItem().getItem() instanceof AudienceStaffItem);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || ISpellContainer.isSpellContainer(stack)) return;
        var spells = ISpellContainer.create(SPELLS.length, true, false).mutableCopy();
        for (String path : SPELLS) {
            AbstractSpell spell = SpellRegistry.getSpell(FortuneIds.id(path));
            if (spell == SpellRegistry.none()) return;
            spells.addSpell(spell, exclusive(spell) ? 1 : TRANSCENDENT_LEVEL, true);
        }
        ISpellContainer.set(stack, spells.toImmutable());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && !ISpellContainer.isSpellContainer(stack)) initializeSpellContainer(stack);
        if (entity instanceof ServerPlayer owner && owner.tickCount % 20 == 0) StaffTitles.recognize(owner, stack);
        if (entity instanceof ServerPlayer player && !isInscribed(stack) && identity(stack) == null) {
            stack.getOrCreateTag().putUUID(ID, UUID.randomUUID());
            player.getInventory().setChanged();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isInscribed(held)) return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        return super.use(level, player, hand);
    }

    public static UUID identity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(ID) ? tag.getUUID(ID) : null;
    }

    public static boolean isInscribed(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(INSCRIBED);
    }

    public static boolean canInscribe(String name) {
        return !name.isBlank() && name.codePointCount(0, name.length()) <= MAX_NAME_LENGTH
                && name.codePoints().allMatch(c -> c != '§' && Character.isDefined(c)
                && !Character.isISOControl(c) && Character.getType(c) != Character.FORMAT
                && Character.getType(c) != Character.SURROGATE
                && Character.getType(c) != Character.LINE_SEPARATOR && Character.getType(c) != Character.PARAGRAPH_SEPARATOR);
    }

    public static int findUninscribed(Inventory inventory, UUID id, int preferredSlot) {
        int size = inventory.getContainerSize();
        if (preferredSlot >= 0 && preferredSlot < size && matches(inventory.getItem(preferredSlot), id)) {
            return preferredSlot;
        }
        for (int i = 0; i < size; i++) {
            if (matches(inventory.getItem(i), id)) return i;
        }
        return -1;
    }

    private static boolean matches(ItemStack stack, UUID id) {
        return stack.getItem() instanceof AudienceStaffItem && !isInscribed(stack) && id.equals(identity(stack));
    }

    static void inscribe(ServerPlayer owner, int slot, UUID id, String text) {
        Inventory inventory = owner.getInventory();
        String name = text.strip();
        if (slot < 0 || slot >= inventory.getContainerSize() || !canInscribe(name)) return;
        int currentSlot = findUninscribed(inventory, id, slot);
        if (currentSlot < 0) return;
        ItemStack stack = inventory.getItem(currentSlot);
        stack.setHoverName(colouredName(name));
        stack.getOrCreateTag().putBoolean(INSCRIBED, true);
        StaffTitles.recognize(owner, stack);
        inventory.setChanged();
        owner.containerMenu.broadcastChanges();
    }

    public static MutableComponent colouredName(String name) {
        MutableComponent result = Component.empty();
        int[] characters = name.codePoints().toArray();
        for (int i = 0; i < characters.length; i++) {
            float position = i * (INKS.length - 1f) / Math.max(1, characters.length - 1);
            int index = (int) position;
            int rgb = FastColor.ARGB32.lerp(position - index, INKS[index], INKS[Math.min(index + 1, INKS.length - 1)]);
            result.append(Component.literal(Character.toString(characters[i])).setStyle(Style.EMPTY.withColor(rgb).withItalic(false)));
        }
        return result;
    }
}
