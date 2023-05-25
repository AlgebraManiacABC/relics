package it.hurts.sskirillss.relics.client.hud.abilities;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import it.hurts.sskirillss.relics.init.HotkeyRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.AbilityCastPredicate;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.data.PredicateData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.data.PredicateEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.data.PredicateInfo;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.CastUtils;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.abilities.SpellCastPacket;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RenderUtils;
import it.hurts.sskirillss.relics.utils.data.AnimationData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;

@OnlyIn(value = Dist.CLIENT)
public class AbilitiesRenderHandler {
    private static final Minecraft MC = Minecraft.getInstance();

    private static List<AbilityEntry> entries = new ArrayList<>();
    private final static Map<String, AbilityCache> cache = new HashMap<>();

    private static int selectedIndex = 0;

    private static boolean animationDown = false;
    private static int animationDelta = 0;

    private static int mouseDelta = 0;

    private static void updateCaches(Player player) {
        if (animationDelta == 0)
            return;

        entries = ActiveAbilityUtils.getActiveEntries(player);

        for (AbilityEntry entry : entries) {
            cache.putIfAbsent(entry.getAbility(), new AbilityCache());

            AnimationCache animationCache = entry.getCache().getAnimation();

            if (animationCache.iconFadeDelta > 0)
                animationCache.iconFadeDelta--;
            if (animationCache.iconShakeDelta > 0)
                animationCache.iconShakeDelta--;

            String abilityName = entry.ability;

            ItemStack stack = ActiveAbilityUtils.getStackInCuriosSlot(player, entry.slot);
            RelicItem relic = (RelicItem) stack.getItem();

            AbilityCastPredicate predicate = CastUtils.getAbilityCastPredicates(relic, abilityName);

            if (predicate == null)
                continue;

            for (Map.Entry<String, PredicateEntry> data : predicate.getPredicates().entrySet()) {
                entry.getCache().predicate.info.put(data.getKey(), data.getValue().getPredicate().apply(new PredicateData(player, stack)));
            }
        }
    }

    public static void render(PoseStack poseStack, float partialTicks) {
        if (animationDelta == 0)
            return;

        Window window = MC.getWindow();

        LocalPlayer player = MC.player;

        if (player == null || entries.isEmpty())
            return;

        int x = (window.getGuiScaledWidth()) / 2;
        int y = -40;

        poseStack.pushPose();

        poseStack.translate(0, (animationDelta - (animationDelta != 5 ? partialTicks * (animationDown ? -1 : 1) : 0)) * 16, 0);

        drawAbility(poseStack, player, -2, x - 65, y, partialTicks);
        drawAbility(poseStack, player, -1, x - 34, y, partialTicks);
        drawAbility(poseStack, player, 0, x, y, partialTicks);
        drawAbility(poseStack, player, 1, x + 34, y, partialTicks);
        drawAbility(poseStack, player, 2, x + 65, y, partialTicks);

        RenderSystem.setShaderTexture(0, new ResourceLocation(Reference.MODID, "textures/hud/abilities/background.png"));

        RenderSystem.enableBlend();

        RenderUtils.renderTextureFromCenter(poseStack, x - 95, y, 2, 2, 256, 256, 18, 29, 1F + (mouseDelta < 0 ? Math.abs(mouseDelta) * 0.01F : 0));
        if (mouseDelta < 0)
            RenderUtils.renderTextureFromCenter(poseStack, x - 95, y, 25, 1, 256, 256, 24, 35, 1F + Math.abs(mouseDelta) * 0.01F);

        RenderUtils.renderTextureFromCenter(poseStack, x + 95, y, 2, 38, 256, 256, 18, 29, 1F + (mouseDelta > 0 ? Math.abs(mouseDelta) * 0.01F : 0));
        if (mouseDelta > 0)
            RenderUtils.renderTextureFromCenter(poseStack, x + 95, y, 25, 37, 256, 256, 24, 35, 1F + Math.abs(mouseDelta) * 0.01F);

        RenderSystem.disableBlend();

        AbilityEntry selectedAbility = getAbilityByIndex(selectedIndex);
        ItemStack stack = ActiveAbilityUtils.getStackInCuriosSlot(player, selectedAbility.getSlot());

        String registryName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();

        MutableComponent name = Component.translatable("tooltip.relics." + registryName + ".ability." + selectedAbility.getAbility());

        MC.font.drawShadow(poseStack, name, x - MC.font.width(name) / 2F, y - 35, 0xFFFFFF);

        poseStack.popPose();

        poseStack.pushPose();

        poseStack.translate((animationDelta - (animationDelta != 5 ? partialTicks * (animationDown ? -1 : 1) : 0)) * 16, 0, 0);

        int yOff = 0;

        x = -70;
        y = 25;

        for (Map.Entry<String, PredicateInfo> entry : cache.get(selectedAbility.getAbility()).predicate.info.entrySet()) {
            String predicateName = entry.getKey();
            PredicateInfo info = entry.getValue();

            RenderSystem.setShaderTexture(0, info.getCondition() ? new ResourceLocation(Reference.MODID, "textures/gui/description/icons/completed.png")
                    : info.getIcon() != null ? info.getIcon() : new ResourceLocation(Reference.MODID, "textures/gui/description/icons/" + registryName + "/" + predicateName + ".png"));

            RenderUtils.renderTextureFromCenter(poseStack, x, y + yOff, 0, 0, 16, 16, 16, 16, 0.5F);

            poseStack.scale(0.5F, 0.5F, 0.5F);

            MC.font.drawShadow(poseStack, Component.translatable("tooltip.relics." + registryName + ".ability." + selectedAbility.ability + ".predicate." + predicateName, info.getPlaceholders().toArray()).withStyle(info.getCondition() ? ChatFormatting.STRIKETHROUGH : ChatFormatting.RESET), (x + 7) * 2F, (y - 2 + yOff) * 2F, info.getCondition() ? 0xbeffb8 : 0xf17f9c);

            poseStack.scale(2F, 2F, 2F);

            yOff += 10;
        }

        poseStack.popPose();
    }

    private static void drawAbility(PoseStack poseStack, LocalPlayer player, int realIndex, float x, float y, float partialTicks) {
        AbilityEntry ability = getAbilityByIndex(getRelativeIndex(realIndex));

        if (ability == null)
            return;

        ItemStack stack = ActiveAbilityUtils.getStackInCuriosSlot(player, ability.getSlot());

        if (!(stack.getItem() instanceof RelicItem relic))
            return;

        boolean isLocked = !AbilityUtils.canPlayerUseActiveAbility(player, stack, ability.getAbility());

        ResourceLocation card = new ResourceLocation(Reference.MODID, "textures/gui/description/cards/" + ForgeRegistries.ITEMS.getKey(ActiveAbilityUtils.getStackInCuriosSlot(player, ability.getSlot()).getItem()).getPath() + "/" + ability.getAbility() + ".png");

        RenderSystem.setShaderTexture(0, card);

        RenderSystem.enableBlend();

        poseStack.pushPose();

        int width = 20;
        int height = 29;

        float scale = (float) ((1F + Mth.clamp(Math.pow(13.5F, -Math.abs(realIndex)), 0F, 0.2F)) + (realIndex == 0 ? (Math.sin((player.tickCount + partialTicks) * 0.1F) * 0.05F) : 0F));

        RenderUtils.renderTextureFromCenter(poseStack, x - scale, y - scale, width, height, scale);

        int cooldown = AbilityUtils.getAbilityCooldown(stack, ability.getAbility());
        int cap = AbilityUtils.getAbilityCooldownCap(stack, ability.getAbility());

        float percentage = cooldown / (cap / 100F) / 100F;

        String iconDescription = "";

        if (cooldown > 0) {
            RenderSystem.setShaderTexture(0, card);

            RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1F);

            RenderUtils.renderTextureFromCenter(poseStack, x - scale, (y - scale + (height * scale) / 2F) - (height * scale / 2F) * percentage, 0, height - height * percentage, width, height, width, height * percentage, scale);

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            RenderSystem.setShaderTexture(0, new ResourceLocation(Reference.MODID, "textures/hud/abilities/widgets/cooldown.png"));

            drawAbilityStatusIcon(ability, poseStack, x - scale, y - scale, 20, 300, scale - 0.1F, AnimationData.builder()
                            .frame(0, 2).frame(1, 2).frame(2, 2)
                            .frame(3, 2).frame(4, 2).frame(5, 2)
                            .frame(6, 2).frame(7, 2).frame(8, 2)
                            .frame(9, 2).frame(10, 8).frame(11, 2)
                            .frame(12, 2).frame(13, 2).frame(14, 2),
                    cap - cooldown, partialTicks);

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            iconDescription = String.valueOf(MathUtils.round(cooldown / 20D, 1));
        } else {
            PredicateCache predicateCache = cache.get(ability.getAbility()).predicate;
            Collection<PredicateInfo> infoEntries = predicateCache.info.values();

            int successPredicates = 0;

            for (PredicateInfo info : infoEntries) {
                if (info.getCondition())
                    successPredicates++;
            }

            int failedPredicates = infoEntries.size() - successPredicates;

            if (failedPredicates > 0) {
                RenderSystem.setShaderTexture(0, new ResourceLocation(Reference.MODID, "textures/hud/abilities/widgets/locked.png"));

                drawAbilityStatusIcon(ability, poseStack, x - scale, y - scale, 20, 20, scale - 0.1F, null, player.tickCount, partialTicks);

                iconDescription = successPredicates + "/" + failedPredicates;
            }
        }

        if (!iconDescription.isEmpty()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);

            MC.font.drawShadow(poseStack, iconDescription, (x - 1) * 2F - (MC.font.width(iconDescription) / 2F), (y - 6 + scale * 15) * 2F, 0xFFFFFF);

            poseStack.scale(2F, 2F, 2F);
        }

        RenderSystem.setShaderTexture(0, new ResourceLocation(Reference.MODID, "textures/hud/abilities/background.png"));

        RenderUtils.renderTextureFromCenter(poseStack, x, y, 66, isLocked ? 40 : 2, 256, 256, 28, 37, scale);

        if (realIndex == 0)
            RenderUtils.renderTextureFromCenter(poseStack, x - 1, y - 20, 53, isLocked ? 14 : 2, 256, 256, 6, 11, scale - 0.1F);

        poseStack.popPose();
    }

    public static void drawAbilityStatusIcon(AbilityEntry ability, PoseStack matrix, float x, float y, float texWidth, float texHeight, float scale, @Nullable AnimationData animation, long ticks, float partialTicks) {
        matrix.pushPose();

        matrix.translate(x, y, 0);

        AnimationCache animationCache = ability.getCache().getAnimation();

        if (animationCache.iconShakeDelta > 0) {
            float color = animationCache.iconShakeDelta * 0.05F;

            RenderSystem.setShaderColor(1F, 1F - color, 1F - color, 1F);

            matrix.mulPose(Vector3f.ZP.rotation((float) Math.sin((ticks + partialTicks) * 0.5F) * 0.05F));

            scale += (animationCache.iconShakeDelta - partialTicks) * 0.025F;
        }

        if (animation != null)
            RenderUtils.renderTextureFromCenter(matrix, 0, 0, texWidth, texHeight, scale, animation, ticks);
        else
            RenderUtils.renderTextureFromCenter(matrix, 0, 0, texWidth, texHeight, scale);

        matrix.popPose();
    }

    private static int getRelativeIndex(int offset) {
        int current = selectedIndex;
        int sum = current + offset;
        int max = entries.size() - 1;

        return sum > max ? Math.min(max, sum - (max + 1)) : sum < 0 ? Math.max(0, sum + (max + 1)) : sum;
    }

    @Nullable
    private static AbilityEntry getAbilityByIndex(int index) {
        if (entries.isEmpty())
            return null;

        return entries.get(Mth.clamp(index, 0, entries.size()));
    }

    private static void applyDelta(int delta) {
        int current = selectedIndex;
        int sum = current + delta;
        int max = entries.size() - 1;

        selectedIndex = sum > max ? sum - max - 1 : sum < 0 ? max : sum;
    }

    @Data
    @AllArgsConstructor
    public static class AbilityEntry {
        private int slot;

        private String ability;

        public AbilityCache getCache() {
            return cache.get(ability);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityCache {
        private AnimationCache animation = new AnimationCache();
        private PredicateCache predicate = new PredicateCache();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnimationCache {
        private boolean shouldIconFade = false;

        private int iconFadeDelta = 0;

        private int iconShakeDelta = 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredicateCache {
        private Map<String, PredicateInfo> info = new HashMap<>();
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            if (!HotkeyRegistry.ABILITY_LIST.isDown() || entries.isEmpty())
                return;

            int current = selectedIndex;

            applyDelta(event.getScrollDelta() > 0 ? -1 : 1);

            if (current != selectedIndex) {
                mouseDelta = event.getScrollDelta() > 0 ? -10 : 10;

                LocalPlayer player = Minecraft.getInstance().player;

                if (player != null)
                    player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5F, 1.5F + player.getRandom().nextFloat() * 0.25F);
            }

            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END)
                return;

            Player player = event.player;

            if (player == null)
                return;

            if (mouseDelta > 0)
                mouseDelta--;
            else if (mouseDelta < 0)
                mouseDelta++;

            if (HotkeyRegistry.ABILITY_LIST.isDown()) {
                AbilityEntry ability = getAbilityByIndex(selectedIndex);

                if (ability != null) {
                    ItemStack stack = ActiveAbilityUtils.getStackInCuriosSlot(player, ability.getSlot());

                    if (stack.getItem() instanceof RelicItem relic && AbilityUtils.canPlayerUseActiveAbility(player, stack, ability.getAbility()))
                        relic.tickActiveAbilitySelection(stack, player, ability.getAbility());
                }

                if (animationDelta < 5)
                    animationDelta++;

                animationDown = true;
            } else {
                if (animationDelta > 0)
                    animationDelta--;

                animationDown = false;
            }

            if (animationDelta == 0)
                return;

            updateCaches(player);

            if (selectedIndex > entries.size() || selectedIndex < 0)
                selectedIndex = 0;
        }

        @SubscribeEvent
        public static void onKeyPressed(InputEvent.MouseButton.Pre event) {
            if (animationDelta == 0 || event.getAction() != InputConstants.PRESS
                    || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_1)
                return;

            Minecraft MC = Minecraft.getInstance();

            if (MC.screen != null)
                return;

            Player player = MC.player;

            if (player == null)
                return;

            AbilityEntry ability = getAbilityByIndex(selectedIndex);

            if (ability == null)
                return;

            ItemStack stack = ActiveAbilityUtils.getStackInCuriosSlot(player, ability.getSlot());

            if (!(stack.getItem() instanceof RelicItem relic))
                return;

            if (!AbilityUtils.canPlayerUseActiveAbility(player, stack, ability.getAbility())) {
                int delta = ability.getCache().getAnimation().iconShakeDelta;

                ability.getCache().getAnimation().setIconShakeDelta(Math.min(15, delta + (delta > 0 ? 5 : 10)));

                event.setCanceled(true);

                return;
            }

            NetworkHandler.sendToServer(new SpellCastPacket(ability.getAbility(), ability.getSlot()));

            relic.endCastActiveAbility(stack, player, ability.getAbility());

            event.setCanceled(true);
        }
    }
}