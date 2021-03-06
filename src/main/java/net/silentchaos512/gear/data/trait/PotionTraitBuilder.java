package net.silentchaos512.gear.data.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.traits.PotionEffectTrait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PotionTraitBuilder extends TraitBuilder {
    private final Map<GearType, List<PotionEffectTrait.PotionData>> potions = new LinkedHashMap<>();

    public PotionTraitBuilder(ResourceLocation traitId, int maxLevel) {
        super(traitId, maxLevel, PotionEffectTrait.SERIALIZER);
    }

    public PotionTraitBuilder addEffect(GearType gearType, boolean requiresFullSet, Effect effect, int... levels) {
        this.potions.computeIfAbsent(gearType, gt -> new ArrayList<>())
                .add(PotionEffectTrait.PotionData.of(requiresFullSet, effect, levels));
        return this;
    }

    @Override
    public JsonObject serialize() {
        if (this.potions.isEmpty()) {
            throw new IllegalStateException("Potion effect trait '" + this.traitId + "' has no effects");
        }

        JsonObject json = super.serialize();

        JsonObject effectsJson = new JsonObject();
        this.potions.forEach(((gearType, effects) -> {
            JsonArray array = new JsonArray();
            effects.forEach(e -> array.add(e.serialize()));
            effectsJson.add(gearType.getName(), array);
        }));
        json.add("potion_effects", effectsJson);

        return json;
    }
}
