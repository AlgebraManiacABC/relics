package it.hurts.sskirillss.relics.compat.jei.categories;

import it.hurts.sskirillss.relics.crafting.RunicAltarRecipe;
import it.hurts.sskirillss.relics.init.BlockRegistry;
import it.hurts.sskirillss.relics.utils.Reference;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RunicAltarCategory implements IRecipeCategory<RunicAltarRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(Reference.MODID, "runic_altar");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MODID, "textures/gui/jei/runic_altar.png");

    private final IDrawable background;
    private final IDrawable icon;

    public RunicAltarCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 144, 85);
        this.icon = helper.createDrawableIngredient(new ItemStack(BlockRegistry.RUNIC_ALTAR_BLOCK.get()));
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @NotNull Class<? extends RunicAltarRecipe> getRecipeClass() {
        return RunicAltarRecipe.class;
    }

    @Override
    public Component getTitle() {
        return new TextComponent("Runic Infusion");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setIngredients(RunicAltarRecipe runicAltarRecipe, IIngredients iIngredients) {
        iIngredients.setOutput(VanillaTypes.ITEM, runicAltarRecipe.getResultItem());

        iIngredients.setInputIngredients(runicAltarRecipe.getIngredients());
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, @NotNull RunicAltarRecipe runicAltarRecipe, IIngredients iIngredients) {
        IGuiItemStackGroup stacks = iRecipeLayout.getItemStacks();

        List<List<ItemStack>> inputs = iIngredients.getInputs(VanillaTypes.ITEM);
        List<List<ItemStack>> outputs = iIngredients.getOutputs(VanillaTypes.ITEM);

        stacks.init(0, true, 32, 33);
        stacks.init(1, true, 2, 33);
        stacks.init(2, true, 32, 2);
        stacks.init(3, true, 32, 64);
        stacks.init(4, true, 62, 33);
        stacks.init(5, false, 117, 34);

        for (int i = 0; i < inputs.size(); i++)
            stacks.set(i, inputs.get(i));

        stacks.set(5, outputs.get(0));
    }
}