package net.silentchaos512.gear.parts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.silentchaos512.gear.api.parts.IGearPart;
import net.silentchaos512.gear.api.parts.IPartData;
import net.silentchaos512.gear.api.parts.MaterialGrade;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A "lazy" version of {@link PartData}. Since {@link IGearPart}s may not exist when certain things
 * like loot tables are loaded, {@code LazyPartData} can be used to represent a future part.
 */
public class LazyPartData implements IPartData {
    private final ResourceLocation partId;
    private final MaterialGrade grade;
    private final ItemStack craftingItem;

    public LazyPartData(ResourceLocation partId) {
        this(partId, ItemStack.EMPTY);
    }

    public LazyPartData(ResourceLocation partId, MaterialGrade grade) {
        this(partId, grade, ItemStack.EMPTY);
    }

    public LazyPartData(ResourceLocation partId, MaterialGrade grade, ItemStack craftingItem) {
        this.partId = partId;
        this.grade = grade;
        this.craftingItem = craftingItem;
    }

    public LazyPartData(ResourceLocation partId, ItemStack craftingItem) {
        this.partId = partId;
        this.grade = MaterialGrade.NONE;
        this.craftingItem = craftingItem;
    }

    public static LazyPartData of(ResourceLocation partId) {
        return new LazyPartData(partId);
    }

    public static LazyPartData of(ResourceLocation partId, ItemStack craftingItem) {
        return new LazyPartData(partId, craftingItem);
    }

    @Override
    public ResourceLocation getPartId() {
        return partId;
    }

    @Nullable
    @Override
    public IGearPart getPart() {
        return PartManager.get(partId);
    }

    @Override
    public ItemStack getCraftingItem() {
        if (this.craftingItem.isEmpty()) {
            IGearPart part = getPart();
            if (part != null) {
                return PartData.of(part).getCraftingItem();
            }
        }
        return this.craftingItem;
    }

    @Override
    public CompoundNBT write(CompoundNBT tags) {
        tags.putString("ID", partId.toString());
        if (this.grade != MaterialGrade.NONE) {
            tags.putString("Grade", this.grade.name());
        }
        if (!this.craftingItem.isEmpty()) {
            tags.put("Item", this.craftingItem.write(new CompoundNBT()));
        }
        return tags;
    }

    public boolean isValid() {
        return getPart() != null;
    }

    public static LazyPartData readJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            String key = json.getAsString();
            return new LazyPartData(new ResourceLocation(key));
        }

        JsonObject jsonObject = json.getAsJsonObject();
        String key = JSONUtils.getString(jsonObject, "part");
        String gradeStr = JSONUtils.getString(jsonObject, "grade", MaterialGrade.NONE.name());
        MaterialGrade grade = MaterialGrade.fromString(gradeStr);
        return new LazyPartData(new ResourceLocation(key), grade);
    }

    @SuppressWarnings("ConstantConditions") // map says getPart might be null
    public static List<PartData> createPartList(Collection<LazyPartData> parts) {
        return parts.stream()
                .filter(LazyPartData::isValid)
                .map(p -> PartData.of(p.getPart()))
                .collect(Collectors.toList());
    }
}
