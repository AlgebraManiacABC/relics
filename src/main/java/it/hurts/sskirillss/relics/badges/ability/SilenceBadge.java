package it.hurts.sskirillss.relics.badges.ability;

import it.hurts.sskirillss.relics.badges.base.AbilityBadge;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import net.minecraft.world.item.ItemStack;

public class SilenceBadge extends AbilityBadge {
    public SilenceBadge() {
        super("silence");
    }

    @Override
    public boolean isVisible(ItemStack stack, String ability) {
        if (!(stack.getItem() instanceof IRelicItem relic))
            return false;

        return relic.getAbilityCastData(ability).getType() != CastType.NONE;
    }
}