package it.hurts.sskirillss.relics.init;

import com.mojang.serialization.MapCodec;
import it.hurts.sskirillss.relics.level.RelicLootModifier;
import it.hurts.sskirillss.relics.utils.Reference;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LootCodecRegistry {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Reference.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RelicLootModifier>> RELIC_LOOT = CODECS.register("relic_loot", RelicLootModifier.CODEC);

    public static void register(IEventBus bus) {
        CODECS.register(bus);
    }
}