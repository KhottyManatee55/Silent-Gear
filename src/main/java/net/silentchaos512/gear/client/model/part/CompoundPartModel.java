package net.silentchaos512.gear.client.model.part;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.material.IMaterial;
import net.silentchaos512.gear.api.material.IMaterialDisplay;
import net.silentchaos512.gear.api.material.MaterialLayer;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.client.material.MaterialDisplayManager;
import net.silentchaos512.gear.client.model.BakedPerspectiveModel;
import net.silentchaos512.gear.client.model.BakedWrapper;
import net.silentchaos512.gear.client.model.LayeredModel;
import net.silentchaos512.gear.client.model.PartTextures;
import net.silentchaos512.gear.gear.material.MaterialManager;

import java.util.*;
import java.util.function.Function;

public class CompoundPartModel extends LayeredModel<CompoundPartModel> {
    private final ItemCameraTransforms cameraTransforms;
    final GearType gearType;
    final PartType partType;
    private CompoundPartModelOverrideList overrideList;

    CompoundPartModel(ItemCameraTransforms cameraTransforms, GearType gearType, PartType partType) {
        this.cameraTransforms = cameraTransforms;
        this.gearType = gearType;
        this.partType = partType;
    }

    @SuppressWarnings("WeakerAccess")
    public void clearCache() {
        if (overrideList != null) {
            overrideList.clearCache();
        }
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {
        overrideList = new CompoundPartModelOverrideList(this, owner, bakery, spriteGetter, modelTransform, modelLocation);
        return new BakedWrapper(this, owner, bakery, spriteGetter, modelTransform, modelLocation, overrideList);
    }

    @SuppressWarnings({"MethodWithTooManyParameters", "WeakerAccess"})
    public IBakedModel bake(List<MaterialLayer> layers,
                            String transformVariant,
                            IModelConfiguration owner,
                            ModelBakery bakery,
                            Function<RenderMaterial, TextureAtlasSprite> spriteGetter,
                            IModelTransform modelTransform,
                            ItemOverrideList overrideList,
                            ResourceLocation modelLocation) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

        TransformationMatrix rotation = modelTransform.getRotation();
        ImmutableMap<ItemCameraTransforms.TransformType, TransformationMatrix> transforms = PerspectiveMapWrapper.getTransforms(modelTransform);

        for (int i = 0; i < layers.size(); i++) {
            MaterialLayer layer = layers.get(i);
            TextureAtlasSprite texture = spriteGetter.apply(new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, layer.getTexture(this.gearType, 0)));
            builder.addAll(getQuadsForSprite(i, texture, rotation, layer.getColor()));
        }

        // No layers?
        if (layers.isEmpty()) {
            IMaterial material = MaterialManager.get(SilentGear.getId("example"));
            if (material != null) {
                buildFakeModel(spriteGetter, builder, rotation, material);
            } else {
                // Shouldn't happen, but...
                TextureAtlasSprite texture = spriteGetter.apply(new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, SilentGear.getId("item/error")));
                builder.addAll(getQuadsForSprite(0, texture, rotation, 0xFFFFFF));
            }
        }

        TextureAtlasSprite particle = spriteGetter.apply(owner.resolveTexture("particle"));

        return new BakedPerspectiveModel(builder.build(), particle, transforms, overrideList, rotation.isIdentity(), owner.isSideLit(), getCameraTransforms(transformVariant));
    }

    private void buildFakeModel(Function<RenderMaterial, TextureAtlasSprite> spriteGetter, ImmutableList.Builder<BakedQuad> builder, TransformationMatrix rotation, IMaterial material) {
        // This method will display an example item for items with no data (ie, for advancements)
        IMaterialDisplay model = MaterialDisplayManager.get(material);
        if (model != null) {
            MaterialLayer exampleMain = model.getLayers(this.gearType, this.partType).getFirstLayer();
            if (exampleMain != null) {
                builder.addAll(getQuadsForSprite(0, spriteGetter.apply(new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, exampleMain.getTexture(gearType, 0))), rotation, exampleMain.getColor()));
            }
        }
    }

    @Override
    public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<RenderMaterial> ret = new HashSet<>();

        ret.add(new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, SilentGear.getId("item/error")));

        // Generic built-in textures
        for (PartTextures tex : PartTextures.getTextures(this.gearType)) {
            ret.add(getTexture(tex.getTexture()));
        }

        // Custom textures
        for (IMaterialDisplay materialDisplay : MaterialDisplayManager.getValues()) {
            for (MaterialLayer layer : materialDisplay.getLayers(this.gearType, this.partType)) {
                ret.add(getTexture(layer));
            }
        }

        return ret;
    }

    private RenderMaterial getTexture(MaterialLayer layer) {
        return getMaterial(layer.getTexture(this.gearType, 0));
    }

    private RenderMaterial getTexture(ResourceLocation tex) {
        String path = "item/" + gearType.getName() + "/" + tex.getPath();
        ResourceLocation location = new ResourceLocation(tex.getNamespace(), path);
        return getMaterial(location);
    }

    private static RenderMaterial getMaterial(ResourceLocation tex) {
        return new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, tex);
    }

    @Override
    public Collection<? extends IModelGeometryPart> getParts() {
        return Collections.emptyList();
    }

    @Override
    public Optional<? extends IModelGeometryPart> getPart(String name) {
        return Optional.empty();
    }

    private ItemCameraTransforms getCameraTransforms(String transformVariant) {
        return cameraTransforms;
    }
}
