package it.hurts.sskirillss.relics.system.casts.slots;

import it.hurts.sskirillss.relics.system.casts.slots.base.SlotReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySlotReference extends SlotReference {
    private int index;

    @Override
    public ItemStack gatherStack(Player player) {
        return player.getInventory().getItem(getIndex());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();

        tag.putInt("index", index);

        return tag;
    }

    @Override
    public SlotReference deserializeNBT(CompoundTag tag) {
        this.index = tag.getInt("index");

        return this;
    }
}