package it.hurts.sskirillss.relics.utils;

import it.hurts.sskirillss.relics.configs.variables.level.RelicLevel;
import it.hurts.sskirillss.relics.configs.variables.worldgen.RelicLoot;
import it.hurts.sskirillss.relics.items.RelicItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RelicUtils {
    public static class Owner {
        private static final String TAG_OWNER = "owner";

        @Nullable
        public static UUID getOwnerUUID(ItemStack stack) {
            String uuid = NBTUtils.getString(stack, TAG_OWNER, "");
            return uuid.equals("") ? null : UUID.fromString(uuid);
        }

        @Nullable
        public static PlayerEntity getOwner(ItemStack stack, World world) {
            UUID uuid = getOwnerUUID(stack);
            return uuid != null ? world.getPlayerByUUID(uuid) : null;
        }

        public static void setOwnerUUID(ItemStack stack, UUID uuid) {
            NBTUtils.setString(stack, TAG_OWNER, uuid.toString());
        }
    }

    public static class Level {
        public static HashMap<RelicItem, RelicLevel> LEVEL = new HashMap<RelicItem, RelicLevel>();

        private static final String TAG_LEVEL = "level";
        private static final String TAG_EXPERIENCE = "experience";

        public static int getLevel(ItemStack stack) {
            if (!(stack.getItem() instanceof RelicItem)) return 0;
            return NBTUtils.getInt(stack, TAG_LEVEL, 0);
        }

        public static int getLevelFromExperience(ItemStack stack, int experience) {
            if (!(stack.getItem() instanceof RelicItem)) return 0;
            RelicLevel relicLevel = getRelicLevel(stack.getItem());
            int min = 0;
            int max = relicLevel.getMaxLevel();
            while (min <= max) {
                int mid = (min + max) / 2;
                int exp = getTotalExperienceForLevel(stack, mid);
                if (exp > experience) max = mid - 1;
                else min = mid + 1;
            }
            return max;
        }

        public static void setLevel(ItemStack stack, int level) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            setExperience(stack, getTotalExperienceForLevel(stack, level));
        }

        public static void addLevel(ItemStack stack, int level) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            setLevel(stack, getLevel(stack) + level);
        }

        public static void takeLevel(ItemStack stack, int level) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            setLevel(stack, getLevel(stack) - level);
        }

        public static int getExperience(ItemStack stack) {
            if (!(stack.getItem() instanceof RelicItem)) return 0;
            return NBTUtils.getInt(stack, TAG_EXPERIENCE, 0);
        }

        public static int getExperienceForLevel(ItemStack stack, int level) {
            if (!(stack.getItem() instanceof RelicItem)) return 0;
            return getTotalExperienceForLevel(stack, level + 1) - getTotalExperienceForLevel(stack, level);
        }

        public static int getTotalExperienceForLevel(ItemStack stack, int level) {
            if (!(stack.getItem() instanceof RelicItem)) return 0;
            return getTotalExperienceForLevel(getRelicLevel(stack.getItem()), level);
        }

        public static int getTotalExperienceForLevel(RelicLevel relicLevel, int level) {
            return (2 * relicLevel.getInitialExp() + relicLevel.getExpRatio() * (level - 1)) * level / 2;
        }

        public static void setExperience(ItemStack stack, int experience) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            RelicLevel relicLevel = getRelicLevel(stack.getItem());
            experience = Math.max(0, Math.min(relicLevel.getMaxExperience(), experience));
            NBTUtils.setInt(stack, TAG_LEVEL, Math.max(0, Math.min(relicLevel.getMaxLevel(), getLevelFromExperience(stack, experience))));
            NBTUtils.setInt(stack, TAG_EXPERIENCE, experience);
        }

        public static void addExperience(ItemStack stack, int experience) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            setExperience(stack, getExperience(stack) + experience);
        }

        public static void takeExperience(ItemStack stack, int experience) {
            if (!(stack.getItem() instanceof RelicItem)) return;
            setExperience(stack, getExperience(stack) - experience);
        }

        protected static RelicLevel getRelicLevel(Item item) {
            if (!(item instanceof RelicItem)) return null;
            return LEVEL.get(item);
        }
    }

    public static class Worldgen {
        public static HashMap<RelicItem, RelicLoot> LOOT = new HashMap<RelicItem, RelicLoot>();

        public static final List<ResourceLocation> AQUATIC = Arrays.asList(
                LootTables.UNDERWATER_RUIN_BIG,
                LootTables.UNDERWATER_RUIN_SMALL,
                LootTables.SHIPWRECK_TREASURE
        );

        public static final List<ResourceLocation> NETHER = Arrays.asList(
                LootTables.NETHER_BRIDGE,
                LootTables.BASTION_BRIDGE,
                LootTables.BASTION_OTHER,
                LootTables.BASTION_TREASURE,
                LootTables.BASTION_HOGLIN_STABLE,
                LootTables.RUINED_PORTAL
        );

        public static final List<ResourceLocation> COLD = Arrays.asList(
                LootTables.IGLOO_CHEST,
                LootTables.VILLAGE_SNOWY_HOUSE,
                LootTables.VILLAGE_TAIGA_HOUSE
        );

        public static final List<ResourceLocation> DESERT = Arrays.asList(
                LootTables.DESERT_PYRAMID,
                LootTables.VILLAGE_DESERT_HOUSE
        );

        public static final List<ResourceLocation> CAVE = Arrays.asList(
                LootTables.STRONGHOLD_CORRIDOR,
                LootTables.STRONGHOLD_CROSSING,
                LootTables.STRONGHOLD_LIBRARY,
                LootTables.ABANDONED_MINESHAFT
        );
    }
}