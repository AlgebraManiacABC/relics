package it.hurts.sskirillss.relics.client.screen.description.widgets.relic;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.relics.client.screen.base.IHoverableWidget;
import it.hurts.sskirillss.relics.client.screen.base.ITickingWidget;
import it.hurts.sskirillss.relics.client.screen.description.AbilityResearchScreen;
import it.hurts.sskirillss.relics.client.screen.description.RelicDescriptionScreen;
import it.hurts.sskirillss.relics.client.screen.description.data.ExperienceParticleData;
import it.hurts.sskirillss.relics.client.screen.description.data.SparkParticleData;
import it.hurts.sskirillss.relics.client.screen.description.misc.DescriptionTextures;
import it.hurts.sskirillss.relics.client.screen.description.misc.DescriptionUtils;
import it.hurts.sskirillss.relics.client.screen.description.widgets.base.AbstractDescriptionWidget;
import it.hurts.sskirillss.relics.client.screen.utils.ParticleStorage;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.lock.PacketAbilityUnlock;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RenderUtils;
import it.hurts.sskirillss.relics.utils.data.AnimationData;
import it.hurts.sskirillss.relics.utils.data.GUIRenderer;
import it.hurts.sskirillss.relics.utils.data.SpriteOrientation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

import java.awt.*;
import java.util.List;

public class AbilityCardWidget extends AbstractDescriptionWidget implements IHoverableWidget, ITickingWidget {
    private final RelicDescriptionScreen screen;
    private final String ability;

    private float scale = 1F;
    private float scaleOld = 1F;

    private int shakeDelta = 0;
    private int colorDelta = 0;

    public AbilityCardWidget(int x, int y, RelicDescriptionScreen screen, String ability) {
        super(x, y, 32, 47);

        this.screen = screen;
        this.ability = ability;
    }

    @Override
    public void onPress() {
        if (!(screen.getStack().getItem() instanceof IRelicItem relic))
            return;

        ItemStack stack = screen.getStack();

        boolean isEnoughLevel = relic.isEnoughLevel(stack, ability);
        boolean isLockUnlocked = relic.isLockUnlocked(stack, ability);

        boolean canUse = isEnoughLevel && isLockUnlocked;

        SoundManager soundManager = minecraft.getSoundManager();

        if (canUse)
            minecraft.setScreen(new AbilityResearchScreen(minecraft.player, screen.container, screen.slot, screen.screen, ability));
        else {
            if (isEnoughLevel) {
                int unlocks = relic.getLockUnlocks(stack, ability) + 1;

                RandomSource random = minecraft.player.getRandom();

                for (int i = 0; i < unlocks * 50; i++) {
                    var center = new Vec2(width / 2F, height / 2F);
                    var margin = new Vec2(random.nextInt(width), random.nextInt(height));

                    var motion = new Vec2(margin.x - center.x, margin.y - center.y).normalized().scale(5F + unlocks);

                    ParticleStorage.addParticle(screen, new SparkParticleData(new Color(150 + random.nextInt(100), 100 + random.nextInt(50), 0),
                            getX() + margin.x, getY() + margin.y, 1F + (random.nextFloat() * 0.5F), 20 + random.nextInt(100))
                            .setDeltaX(random.nextFloat() * motion.x)
                            .setDeltaY(random.nextFloat() * motion.y)
                    );
                }

                NetworkHandler.sendToServer(new PacketAbilityUnlock(screen.container, screen.slot, ability, unlocks));

                scale += 0.05F;

                shakeDelta = Math.min(30, shakeDelta + 20);

                soundManager.play(SimpleSoundInstance.forUI(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1F));

                if (unlocks >= relic.getMaxLockUnlocks()) {
                    soundManager.play(SimpleSoundInstance.forUI(SoundEvents.WITHER_BREAK_BLOCK, 1F));
                    soundManager.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE, 1F));
                }
            } else {
                shakeDelta = Math.min(20, shakeDelta + 10);
                colorDelta = Math.min(20, colorDelta + 10);

                soundManager.play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_BREAK, 1F));
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || !(screen.stack.getItem() instanceof IRelicItem relic))
            return;

        ItemStack stack = screen.getStack();

        TextureManager manager = minecraft.getTextureManager();
        PoseStack poseStack = guiGraphics.pose();

        int unlocks = relic.getLockUnlocks(stack, ability);

        boolean isEnoughLevel = relic.isEnoughLevel(stack, ability);
        boolean isLockUnlocked = relic.isLockUnlocked(stack, ability);

        boolean canUse = isEnoughLevel && isLockUnlocked;
        boolean canUpgrade = relic.mayPlayerUpgrade(minecraft.player, screen.stack, ability);

        ResourceLocation card = ResourceLocation.fromNamespaceAndPath(Reference.MODID, "textures/abilities/" + BuiltInRegistries.ITEM.getKey(screen.stack.getItem()).getPath() + "/" + relic.getAbilityData(ability).getIcon().apply(minecraft.player, screen.stack, ability) + ".png");

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        poseStack.pushPose();

        float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        float lerpedScale = Mth.lerp(partialTicks, scaleOld, scale);

        poseStack.scale(lerpedScale, lerpedScale, lerpedScale);

        poseStack.translate((getX() + (width / 2F)) / lerpedScale, (getY() + (height / 2F)) / lerpedScale, 0);

        float color = canUse ? (float) (1.05F + (Math.sin((player.tickCount + (ability.length() * 10)) * 0.2F) * 0.1F)) : canUpgrade ? 0.5F : 0.25F;

        RenderSystem.setShaderColor(color, color, color, 1F);

        GUIRenderer.begin(card, poseStack)
                .orientation(SpriteOrientation.CENTER)
                .texSize(22, 31)
                .scale(1.01F) // Blame Mojang, not me
                .end();

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        GUIRenderer.begin(canUse ? DescriptionTextures.SMALL_CARD_FRAME_ACTIVE : DescriptionTextures.SMALL_CARD_FRAME_INACTIVE, poseStack)
                .orientation(SpriteOrientation.CENTER)
                .end();

        if (isHovered())
            GUIRenderer.begin(DescriptionTextures.SMALL_CARD_FRAME_OUTLINE, poseStack)
                    .orientation(SpriteOrientation.CENTER)
                    .end();

        if (!canUse) {
            GUIRenderer.begin(isEnoughLevel ? ResourceLocation.fromNamespaceAndPath(Reference.MODID, "textures/gui/description/relic/chains_active_" + unlocks + ".png") : DescriptionTextures.CHAINS_INACTIVE, poseStack)
                    .orientation(SpriteOrientation.CENTER)
                    .pos(0, -1)
                    .end();

            MutableComponent level = Component.literal(String.valueOf(relic.getAbilityData(ability).getRequiredLevel())).withStyle(ChatFormatting.BOLD);

            color = Math.min(0.75F, colorDelta * 0.04F);

            RenderSystem.setShaderColor(1, 1 - color, 1 - color, 1);

            poseStack.pushPose();

            if (shakeDelta > 0)
                poseStack.mulPose(Axis.ZP.rotation((float) Math.sin((player.tickCount + partialTick) * 0.75F) * ((shakeDelta / 30F) * 0.75F)));

            GUIRenderer.begin(isEnoughLevel ? ResourceLocation.fromNamespaceAndPath(Reference.MODID, "textures/gui/description/relic/icons/lock_active_" + unlocks + ".png") : DescriptionTextures.LOCK_INACTIVE, poseStack)
                    .orientation(SpriteOrientation.CENTER)
                    .end();

            poseStack.scale(0.5F, 0.5F, 0.5F);

            guiGraphics.drawString(minecraft.font, level, (-(width / 2) + 16) * 2 - minecraft.font.width(level) / 2, (-(height / 2) + 24) * 2, isEnoughLevel ? 0xFFE278 : 0xB7AED9, true);

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            poseStack.popPose();
        }

        if (canUpgrade) {
            RenderSystem.setShaderTexture(0, DescriptionTextures.UPGRADE);

            manager.bindForSetup(DescriptionTextures.UPGRADE);

            RenderSystem.enableBlend();

            RenderUtils.renderAnimatedTextureFromCenter(poseStack, 0, -1, 20, 400, 20, 20, 0.9F + ((float) (Math.sin((player.tickCount + partialTick) * 0.25F) * 0.025F)), AnimationData.builder()
                    .frame(0, 2).frame(1, 2)
                    .frame(2, 2).frame(3, 2)
                    .frame(4, 2).frame(5, 2)
                    .frame(6, 2).frame(7, 2)
                    .frame(8, 2).frame(9, 2)
                    .frame(10, 2).frame(11, 2)
                    .frame(12, 2).frame(13, 2)
                    .frame(14, 2).frame(15, 2)
                    .frame(16, 2).frame(17, 2)
                    .frame(18, 2).frame(19, 2)
            );

            RenderSystem.disableBlend();
        }

        {
            if (canUse) {
                int xOff = 0;

                for (int i = 0; i < 5; i++) {
                    guiGraphics.blit(DescriptionTextures.SMALL_STAR_HOLE, -(width / 2) + xOff + 4, -(height / 2) + 40, 0, 0, 4, 4, 4, 4);

                    xOff += 5;
                }

                xOff = 0;

                int quality = relic.getAbilityQuality(screen.stack, ability);
                boolean isAliquot = quality % 2 == 1;

                for (int i = 0; i < Math.floor(quality / 2D); i++) {
                    guiGraphics.blit(DescriptionTextures.SMALL_STAR_ACTIVE, -(width / 2) + xOff + 4, -(height / 2) + 40, 0, 0, 4, 4, 4, 4);

                    xOff += 5;
                }

                if (isAliquot)
                    guiGraphics.blit(DescriptionTextures.SMALL_STAR_ACTIVE, -(width / 2) + xOff + 4, -(height / 2) + 40, 0, 0, 2, 4, 4, 4);
            }
        }

        {
            MutableComponent title = Component.literal(canUse ? String.valueOf(relic.getAbilityLevel(screen.stack, ability)) : "?").withStyle(ChatFormatting.BOLD);

            float textScale = 0.5F;

            poseStack.scale(textScale, textScale, textScale);

            guiGraphics.drawString(minecraft.font, title, -((width + 1) / 2) - (minecraft.font.width(title) / 2) + 16, (-(height / 2) - 19), canUse ? 0xFFE278 : 0xB7AED9, true);
        }

        poseStack.popPose();
    }

    @Override
    public void onTick() {
        if (!(screen.stack.getItem() instanceof IRelicItem relic))
            return;

        float maxScale = 1.1F;
        float minScale = 1F;

        RandomSource random = minecraft.player.getRandom();

        if (relic.mayPlayerUpgrade(minecraft.player, screen.stack, ability)) {
            if (minecraft.player.tickCount % 7 == 0)
                ParticleStorage.addParticle(screen, new ExperienceParticleData(new Color(200 + random.nextInt(50), 150 + random.nextInt(100), 0),
                        getX() + 5 + random.nextInt(18), getY() + 18, 1F + (random.nextFloat() * 0.5F), 100 + random.nextInt(50)));
        }

        scaleOld = scale;

        if (scale > maxScale)
            scale = Math.max(minScale, scale - 0.01F);

        if (isHovered()) {
            if (minecraft.player.tickCount % 3 == 0)
                ParticleStorage.addParticle(screen, new ExperienceParticleData(
                        new Color(200 + random.nextInt(50), 150 + random.nextInt(100), 0),
                        getX() + random.nextInt(width), getY() - 1, 1F + (random.nextFloat() * 0.5F), 100 + random.nextInt(50)));

            if (scale < maxScale)
                scale = Math.min(maxScale, scale + 0.04F);
        } else {
            if (scale > minScale)
                scale = Math.max(minScale, scale - 0.03F);
        }

        if (shakeDelta > 0)
            shakeDelta--;

        if (colorDelta > 0)
            colorDelta--;
    }

    @Override
    public void onHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!(screen.stack.getItem() instanceof IRelicItem relic))
            return;

        AbilityData data = relic.getAbilityData(ability);

        if (data == null)
            return;

        PoseStack poseStack = guiGraphics.pose();

        List<FormattedCharSequence> tooltip = Lists.newArrayList();

        int maxWidth = 110;
        int renderWidth = 0;

        List<MutableComponent> entries = Lists.newArrayList(
                Component.translatable("tooltip.relics." + BuiltInRegistries.ITEM.getKey(screen.stack.getItem()).getPath() + ".ability." + ability).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.UNDERLINE)
        );

        int level = relic.getRelicLevel(screen.stack);
        int requiredLevel = data.getRequiredLevel();

        if (level < requiredLevel) {
            entries.add(Component.literal(" "));

            entries.add(Component.literal("").append(Component.translatable("tooltip.relics.researching.relic.card.low_level", Component.literal(String.valueOf(requiredLevel)).withStyle(ChatFormatting.BOLD))));
        } else if (data.getMaxLevel() == 0) {
            entries.add(Component.literal(" "));

            entries.add(Component.literal("").append(Component.translatable("tooltip.relics.researching.relic.card.no_stats")));
        } else if (relic.mayPlayerUpgrade(minecraft.player, screen.stack, ability)) {
            entries.add(Component.literal(" "));

            entries.add(Component.literal("").append(Component.translatable("tooltip.relics.researching.relic.card.ready_to_upgrade")));
        }

        for (MutableComponent entry : entries) {
            int entryWidth = (minecraft.font.width(entry)) / 2;

            if (entryWidth > renderWidth)
                renderWidth = Math.min(entryWidth + 2, maxWidth);

            tooltip.addAll(minecraft.font.split(entry, maxWidth * 2));
        }

        int height = tooltip.size() * 5;

        int y = getHeight() / 2;

        float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        float lerpedScale = Mth.lerp(partialTicks, scaleOld, scale);

        poseStack.scale(lerpedScale, lerpedScale, lerpedScale);

        poseStack.translate((getX() + (getWidth() / 2F)) / lerpedScale, (getY() + (getHeight() / 2F)) / lerpedScale, 0);

        DescriptionUtils.drawTooltipBackground(guiGraphics, renderWidth, height, -((renderWidth + 19) / 2), y);

        int yOff = 0;

        for (FormattedCharSequence entry : tooltip) {
            poseStack.pushPose();

            poseStack.scale(0.5F, 0.5F, 0.5F);

            guiGraphics.drawString(minecraft.font, entry, -(minecraft.font.width(entry) / 2), ((y + yOff + 9) * 2), 0x662f13, false);

            yOff += 5;

            poseStack.popPose();
        }
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (screen.getStack().getItem() instanceof IRelicItem relic && relic.isAbilityUnlocked(screen.stack, ability))
            super.playDownSound(handler);
    }
}