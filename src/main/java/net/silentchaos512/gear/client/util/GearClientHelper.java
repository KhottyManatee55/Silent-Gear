package net.silentchaos512.gear.client.util;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.api.parts.PartDataList;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.ItemStats;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.traits.ITrait;
import net.silentchaos512.gear.config.Config;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.item.CompoundPartItem;
import net.silentchaos512.gear.item.gear.CoreArmor;
import net.silentchaos512.gear.parts.PartData;
import net.silentchaos512.gear.parts.type.CompoundPart;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.gear.util.TraitHelper;
import net.silentchaos512.lib.event.ClientTicks;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public final class GearClientHelper {
    public static Map<String, IBakedModel> modelCache = new HashMap<>();

    private GearClientHelper() {
    }

    public static void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        GearTooltipFlag flagTC = flag instanceof GearTooltipFlag
                ? (GearTooltipFlag) flag
                : GearTooltipFlag.withModifierKeys(flag.isAdvanced(), true, true);
        addInformation(stack, world, tooltip, flagTC);
    }

    public static void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, GearTooltipFlag flag) {
        /*
        LoaderState state = Loader.instance().getLoaderState();
        if (state == LoaderState.INITIALIZATION || state == LoaderState.SERVER_ABOUT_TO_START || state == LoaderState.SERVER_STOPPING) {
            // Skip tooltips during block/item remapping
            // JEI tooltip caches are done in AVAILABLE, in-game is SERVER_STARTED
            return;
        }
        */

        if (!(stack.getItem() instanceof ICoreItem)) return;

        ICoreItem item = (ICoreItem) stack.getItem();

        if (GearHelper.isBroken(stack)) {
            tooltip.add(Math.min(1, tooltip.size()), misc("broken").func_240699_a_(TextFormatting.RED));
        }

        if (GearData.isExampleGear(stack)) {
            tooltip.add(Math.min(1, tooltip.size()), misc("exampleOutput1").func_240699_a_(TextFormatting.YELLOW));
            tooltip.add(Math.min(2, tooltip.size()), misc("exampleOutput2").func_240699_a_(TextFormatting.YELLOW));
        }

        PartDataList constructionParts = GearData.getConstructionParts(stack);

        if (constructionParts.getMains().isEmpty()) {
            tooltip.add(misc("invalidParts").func_240699_a_(TextFormatting.RED));
            tooltip.add(misc("lockedStats").func_240699_a_(TextFormatting.RED));
        } else if (GearData.hasLockedStats(stack)) {
            tooltip.add(misc("lockedStats").func_240699_a_(TextFormatting.YELLOW));
        } else {
            // TODO: Need to generify this
            if (item.requiresPartOfType(PartType.ROD) && constructionParts.getRods().isEmpty()) {
                tooltip.add(misc("missingRod").func_240699_a_(TextFormatting.RED));
            }
            if (item.requiresPartOfType(PartType.BOWSTRING) && constructionParts.getPartsOfType(PartType.BOWSTRING).isEmpty()) {
                tooltip.add(misc("missingBowstring").func_240699_a_(TextFormatting.RED));
            }
        }

        // Let parts add information if they need to
        Collections.reverse(constructionParts);
        for (PartData data : constructionParts) {
            data.getPart().addInformation(data, stack, tooltip, flag);
        }

        // Traits
        addTraitsInfo(stack, tooltip, flag);

        // Stats
        addStatsInfo(stack, tooltip, flag, item);

        // Tool construction
        IFormattableTextComponent textConstruction = misc("tooltip.construction").func_240699_a_(TextFormatting.GOLD);
        if (flag.altDown && flag.showConstruction) {
            tooltip.add(textConstruction);
            Collections.reverse(constructionParts);
            tooltipListParts(stack, tooltip, constructionParts);
        } else if (flag.showConstruction) {
            textConstruction.func_230529_a_(new StringTextComponent(" ")
                    .func_240699_a_(TextFormatting.GRAY)
                    .func_230529_a_(misc("tooltip.construction.key")));
            tooltip.add(textConstruction);
        }
    }

    public static void addStatsInfo(ItemStack stack, List<ITextComponent> tooltip, GearTooltipFlag flag, ICoreItem item) {
        IFormattableTextComponent textStats = misc("tooltip.stats").func_240699_a_(TextFormatting.GOLD);
        if (flag.ctrlDown && flag.showStats) {
            tooltip.add(textStats);

            tooltip.add(misc("tier", GearData.getTier(stack)));

            float synergyDisplayValue = GearData.getSynergyDisplayValue(stack);
            TextFormatting color = synergyDisplayValue < 1 ? TextFormatting.RED : synergyDisplayValue > 1 ? TextFormatting.GREEN : TextFormatting.WHITE;
            tooltip.add(new StringTextComponent("Synergy: " + color + String.format("%d%%", Math.round(100 * synergyDisplayValue))));

            // Display only stats relevant to the item class
            Collection<ItemStat> relevantStats = item.getRelevantStats(stack);
            Collection<ItemStat> displayStats = flag.isAdvanced() && SilentGear.isDevBuild() ? ItemStats.allStatsOrdered() : relevantStats;

            for (ItemStat stat : displayStats) {
                float statValue = GearData.getStat(stack, stat);

                // Used for the total armor/toughness a full suit of armor would provide
                float totalArmor = -1;
                if (item instanceof CoreArmor) {
                    if (stat == ItemStats.ARMOR) {
                        // Armor value varies by type
                        totalArmor = statValue;
                        statValue = (float) ((CoreArmor) item).getArmorProtection(stack);
                    } else if (stat == ItemStats.ARMOR_TOUGHNESS) {
                        // Toughness split equally to each piece
                        totalArmor = statValue;
                        statValue /= 4;
                    }
                }

                StatInstance inst = new StatInstance(statValue, StatInstance.Operation.AVG);
                TextFormatting nameColor = relevantStats.contains(stat) ? stat.getNameColor() : TextFormatting.DARK_GRAY;
                ITextComponent textName = new StringTextComponent("- ").func_240699_a_(nameColor).func_230529_a_(stat.getDisplayName());
                ITextComponent textStat = new StringTextComponent(inst.formattedString(stat, stat.isDisplayAsInt() ? 0 : 2, false));

                // Some stat-specific formatting...
                if (stat == ItemStats.DURABILITY) {
                    int durabilityLeft = stack.getMaxDamage() - stack.getDamage();
                    int durabilityMax = stack.getMaxDamage();
                    textStat = statText("durabilityFormat", durabilityLeft, durabilityMax);
                } else if (stat == ItemStats.ARMOR || stat == ItemStats.ARMOR_TOUGHNESS) {
                    String str1 = String.format("%.1f", statValue);
                    String str2 = String.format("%.1f", totalArmor);
                    textStat = statText("armorFormat", str1, str2);
                }

                tooltip.add(statText("displayFormat", textName, textStat));
            }
        } else if (flag.showStats) {
            textStats.func_240702_b_(" ").func_230529_a_(misc("tooltip.stats.key").func_240699_a_(TextFormatting.GRAY));
            tooltip.add(textStats);
        }
    }

    private static void addTraitsInfo(ItemStack stack, List<ITextComponent> tooltip, GearTooltipFlag flag) {
        Map<ITrait, Integer> traits = TraitHelper.getCachedTraits(stack);
        List<ITrait> visibleTraits = traits.keySet().stream()
                .filter(t -> t != null && t.showInTooltip(flag))
                .collect(Collectors.toList());
        int traitIndex = getTraitDisplayIndex(visibleTraits.size(), flag);
        if (traitIndex < 0) {
            tooltip.add(misc("tooltip.traits").func_240699_a_(TextFormatting.GOLD));
        }
        int i = 0;
        for (ITrait trait : visibleTraits) {
            if (traitIndex < 0 || traitIndex == i) {
                final int level = traits.get(trait);
                trait.addInformation(level, tooltip, flag, text -> {
                    if (traitIndex >= 0) {
                        return misc("tooltip.traits")
                                .func_240699_a_(TextFormatting.GOLD)
                                .func_230529_a_(new StringTextComponent(": ")
                                        .func_240699_a_(TextFormatting.WHITE)
                                        .func_230529_a_(text));
                    }
                    return new StringTextComponent("- ").func_230529_a_(text);
                });
            }
            ++i;
        }
    }

    private static int getTraitDisplayIndex(int numTraits, GearTooltipFlag flag) {
        if (flag.ctrlDown || numTraits == 0)
            return -1;
        return ClientTicks.ticksInGame() / 20 % numTraits;
    }

    private static IFormattableTextComponent misc(String key, Object... formatArgs) {
        return new TranslationTextComponent("misc.silentgear." + key, formatArgs);
    }

    private static IFormattableTextComponent statText(String key, Object... formatArgs) {
        return new TranslationTextComponent("stat.silentgear." + key, formatArgs);
    }

    public static void tooltipListParts(ItemStack gear, List<ITextComponent> tooltip, Collection<PartData> parts) {
        for (PartData part : parts) {
            if (part.getPart().isVisible()) {
                tooltip.add(new StringTextComponent("- ").func_230529_a_(part.getDisplayName(gear)));

                // List materials for compound parts
                if (part.getPart() instanceof CompoundPart) {
                    List<MaterialInstance> materials = CompoundPartItem.getMaterials(part.getCraftingItem());
                    for (MaterialInstance material : materials) {
                        tooltip.add(new StringTextComponent("  - ").func_230529_a_(material.getDisplayNameWithGrade(part.getType())));
                    }
                }
            }
        }
    }

    public static boolean hasEffect(ItemStack stack) {
        return Config.Client.allowEnchantedEffect.get() && stack.isEnchanted();
    }

    public static boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.equals(newStack);
    }
}
