package it.hurts.sskirillss.relics.capability.handlers;

import it.hurts.sskirillss.relics.capability.utils.CapabilityUtils;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.capability.CapabilitySyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SyncHandler {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        Level level = player.getLevel();

        if (level.isClientSide())
            return;

        NetworkHandler.sendToClient(new CapabilitySyncPacket(CapabilityUtils.getRelicsCapability(player).serializeNBT()), (ServerPlayer) player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Level level = player.getLevel();

        if (level.isClientSide())
            return;

        NetworkHandler.sendToClient(new CapabilitySyncPacket(CapabilityUtils.getRelicsCapability(player).serializeNBT()), (ServerPlayer) player);
    }
}