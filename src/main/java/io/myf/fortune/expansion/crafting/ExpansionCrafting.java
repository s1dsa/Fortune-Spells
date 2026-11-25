package io.myf.fortune.expansion.crafting;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.registry.FSSpells;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.fluids.NoopFluid;
import io.redspace.ironsspellbooks.fluids.SimpleTintedClientFluidType;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.CreativeTabRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.redspace.ironsspellbooks.api.spells.SpellRarity.*;

public final class ExpansionCrafting {
    private static final String INK_ID = "transcendent_ink";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FortuneIds.MODID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FortuneIds.MODID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, FortuneIds.MODID);

    private static final RegistryObject<FluidType> INK_TYPE = TYPES.register(INK_ID, () -> new FluidType(FluidType.Properties.create()) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new SimpleTintedClientFluidType(ResourceLocation.fromNamespaceAndPath("forge", "block/milk_still"), 0x8B1E2D));
        }
    });
    private static final RegistryObject<Fluid> INK_FLUID = FLUIDS.register(INK_ID, ExpansionCrafting::createFluid);
    public static final RegistryObject<Item> INK = ITEMS.register(INK_ID, ExpansionCrafting::createInk);

    private ExpansionCrafting() {}

    private static InkItem createInk() {
        // Iron's 3.15 supplies its own item properties. 3.16 takes them from the caller.
        try {
            try {
                return InkItem.class.getConstructor(SpellRarity.class, Supplier.class, Item.Properties.class)
                        .newInstance(LEGENDARY, INK_FLUID, new Item.Properties());
            } catch (NoSuchMethodException olderVersion) {
                return InkItem.class.getConstructor(SpellRarity.class, Supplier.class)
                        .newInstance(LEGENDARY, INK_FLUID);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create transcendent ink with the installed Iron's Spellbooks", e);
        }
    }

    private static Fluid createFluid() {
        return new NoopFluid(new ForgeFlowingFluid.Properties(INK_TYPE, INK_FLUID, INK_FLUID).bucket(() -> Items.AIR));
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ExpansionCrafting::addIngredients);
        bus.addListener(EventPriority.LOWEST, ExpansionCrafting::hideStaffScroll);
    }

    private static void hideStaffScroll(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != CreativeTabRegistry.SCROLLS_TAB.get()) return;
        AbstractSpell spell = FSSpells.CATACLYSM.get();
        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, level, scroll);
            event.getEntries().remove(scroll);
        }
    }

    public static List<AbstractSpell> spellsWithScrolls() {
        return SpellRegistry.getEnabledSpells().stream().filter(spell -> spell != FSSpells.CATACLYSM.get()).toList();
    }

    private static void addIngredients(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == CreativeTabRegistry.MATERIALS_TAB.get()) {
            event.getEntries().putAfter(ItemRegistry.INK_LEGENDARY.get().getDefaultInstance(),
                    INK.get().getDefaultInstance(), TabVisibility.PARENT_AND_SEARCH_TABS);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(INK, TabVisibility.PARENT_TAB_ONLY);
        }
    }

    public static MutableComponent tierName() {
        return Component.translatable("rarity.fortune_spells.transcendent").withStyle(style -> style.withColor(0xF5C542));
    }

    public static boolean transcendent(AbstractSpell spell) {
        if (!spell.getSpellResource().getNamespace().equals(FortuneIds.MODID)) return false;
        return switch (spell.getSpellResource().getPath()) {
            case "croc_devour", "phantom_belfry", "sunset_blade_dance", "bone_wings" -> true;
            default -> false;
        };
    }

    // Iron's 3.16.4 exposes five native rarity values. Anvil upgrade costs use these
    // brackets; the separate transcendent tier is checked against the ink item itself.
    private static final List<SpellRarity> SINGLE_LEVEL = List.of(LEGENDARY);
    private static final Map<String, List<SpellRarity>> LEVEL_TIERS = Map.of(
            "phase_shift", List.of(EPIC, EPIC, LEGENDARY, LEGENDARY),
            "burrow", List.of(UNCOMMON, UNCOMMON, RARE, RARE, EPIC, LEGENDARY),
            "bell_shockwave", List.of(RARE, RARE, EPIC, EPIC, LEGENDARY));

    public static SpellRarity rarity(String spell, int level) {
        var tiers = LEVEL_TIERS.getOrDefault(spell, SINGLE_LEVEL);
        return tiers.get(Math.min(Math.max(level, 1), tiers.size()) - 1);
    }

    public static int minLevel(String spell, SpellRarity ink) {
        return LEVEL_TIERS.getOrDefault(spell, SINGLE_LEVEL).indexOf(ink) + 1;
    }

    public static boolean matchesInkTier(AbstractSpell spell, ItemStack inkStack) {
        return matchesInkTier(spell, inkStack.getItem());
    }

    public static boolean matchesInkTier(AbstractSpell spell, Item ink) {
        return transcendent(spell) == (ink == INK.get());
    }
}
