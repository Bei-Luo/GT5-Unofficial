package gregtech.common.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneCategory;
import gregtech.api.enums.StoneType;
import gregtech.api.enums.SubTag;
import gregtech.api.enums.TextureSet;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.items.GTGenericBlock;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTLanguageManager;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.ores.GTOreAdapter;
import gregtech.common.ores.OreInfo;
import gregtech.common.render.GTRendererBlock;
import gregtech.nei.NEIGTConfig;

public class BlockOresAbstract extends GTGenericBlock implements IBlockWithTextures {

    public final List<StoneType> stoneTypes;

    public BlockOresAbstract(int series, StoneType[] stoneTypes) {
        super(ItemOres.class, "gt.blockores" + series, Material.rock);
        setStepSound(soundTypeStone);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_ORES);

        if (stoneTypes.length > 8) throw new IllegalArgumentException("stoneTypes.length must be <= 8");

        for (int i = 0; i < stoneTypes.length; i++) {
            if (!stoneTypes[i].isEnabled()) {
                stoneTypes[i] = null;
            }
        }

        this.stoneTypes = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(stoneTypes)));

        for (StoneType stoneType : stoneTypes) {
            if (stoneType != null) {
                GTOreAdapter.INSTANCE.registerOre(stoneType, this);
            }
        }

        for (int matId = 0; matId < 1000; matId++) {
            Materials mat = getMaterial(matId);

            if (mat == null) continue;

            GTLanguageManager
                .addStringLocalization(mUnlocalizedName + "." + (matId) + ".name", getLocalizedNameFormat(mat));
            GTLanguageManager.addStringLocalization(
                mUnlocalizedName + "." + (matId + GTOreAdapter.SMALL_ORE_META_OFFSET) + ".name",
                "Small " + getLocalizedNameFormat(mat));

            GTLanguageManager.addStringLocalization(mUnlocalizedName + "." + (matId) + ".tooltip", mat.getToolTip());
            GTLanguageManager.addStringLocalization(
                mUnlocalizedName + "." + (matId + GTOreAdapter.SMALL_ORE_META_OFFSET) + ".tooltip",
                mat.getToolTip());
        }

        OreInfo<Materials> info = new OreInfo<>();

        for (int matId = 0; matId < 1000; matId++) {
            info.material = getMaterial(matId);
            info.stoneType = null;

            if (!GTOreAdapter.INSTANCE.supports(info)) continue;

            for (StoneType stoneType : stoneTypes) {
                if (stoneType == null) continue;

                info.stoneType = stoneType;

                if (stoneType.getPrefix().mIsUnificatable) {
                    GTOreDictUnificator
                        .set(stoneType.getPrefix(), info.material, GTOreAdapter.INSTANCE.getStack(info, 1));
                } else {
                    GTOreDictUnificator.registerOre(stoneType.getPrefix(), GTOreAdapter.INSTANCE.getStack(info, 1));
                }
            }
        }
    }

    @Override
    public String getUnlocalizedName() {
        return mUnlocalizedName;
    }

    /**
     * The first stack with meta = 0 is always hidden in {@link NEIGTConfig} to prevent extraneous ores from showing up
     * in nei.
     */
    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        // always add meta = 0, because NEI does weird stuff when your item doesn't have subblocks
        // meta = 0 is always hidden in the nei plugin
        list.add(new ItemStack(this, 1, 0));

        OreInfo<Materials> info = new OreInfo<>();

        for (int matId = 0; matId < 1000; matId++) {
            info.material = getMaterial(matId);
            info.stoneType = null;

            if (!GTOreAdapter.INSTANCE.supports(info)) continue;

            for (StoneType stoneType : stoneTypes) {
                if (stoneType == null) continue;

                if (info.material.contains(SubTag.ICE_ORE)) {
                    // if this material only has ice ore, we only want to show the ice variants
                    if (stoneType.getCategory() != StoneCategory.Ice) continue;
                    if (stoneType.isExtraneous()) continue;

                    info.stoneType = stoneType;

                    list.add(GTOreAdapter.INSTANCE.getStack(info, 1));
                } else {
                    // if this material doesn't have ice ore, we only want to show the stone variants
                    if (stoneType.getCategory() != StoneCategory.Stone) continue;
                    if (stoneType.isExtraneous()) continue;

                    info.stoneType = stoneType;

                    list.add(GTOreAdapter.INSTANCE.getStack(info, 1));
                }
            }
        }

        info.isSmall = true;

        for (int matId = 0; matId < 1000; matId++) {
            info.material = getMaterial(matId);
            info.stoneType = null;

            if (!GTOreAdapter.INSTANCE.supports(info)) continue;

            for (StoneType stoneType : stoneTypes) {
                if (stoneType == null) continue;

                if (info.material.contains(SubTag.ICE_ORE)) {
                    // if this material only has ice ore, we only want to show the ice variants
                    if (stoneType.getCategory() != StoneCategory.Ice) continue;

                    info.stoneType = stoneType;

                    list.add(GTOreAdapter.INSTANCE.getStack(info, 1));
                } else {
                    // if this material doesn't jabe ice ore, we only want to show the stone variants
                    if (stoneType.getCategory() != StoneCategory.Stone) continue;
                    if (stoneType.isExtraneous()) continue;

                    info.stoneType = stoneType;

                    list.add(GTOreAdapter.INSTANCE.getStack(info, 1));
                }
            }
        }
    }

    public boolean isExtraneous() {
        for (StoneType stoneType : stoneTypes) {
            if (stoneType == null || stoneType.isExtraneous()) continue;

            return false;
        }

        return true;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        EntityPlayer harvester = this.harvesters.get();

        boolean doFortune = !(harvester instanceof FakePlayer);
        boolean doSilktouch = harvester != null && EnchantmentHelper.getSilkTouchModifier(harvester);

        try (OreInfo<Materials> info = GTOreAdapter.INSTANCE.getOreInfo(this, metadata)) {
            if (info == null) return new ArrayList<>();

            return (ArrayList<ItemStack>) GTOreAdapter.INSTANCE.getOreDrops(info, doSilktouch, doFortune ? fortune : 0);
        }
    }

    @Override
    public ITexture[][] getTextures(int metadata) {
        StoneType stoneType = getStoneType(metadata);
        Materials mat = getMaterial(metadata);
        boolean small = isSmallOre(metadata);

        ITexture[] textures;

        if (stoneType == null) stoneType = StoneType.Stone;

        if (mat != null) {
            ITexture iTexture = TextureFactory.builder()
                .addIcon(
                    mat.mIconSet.mTextures[small ? OrePrefixes.oreSmall.mTextureIndex : OrePrefixes.ore.mTextureIndex])
                .setRGBA(mat.mRGBa)
                .stdOrient()
                .build();

            textures = new ITexture[] { stoneType.getTexture(0), iTexture };
        } else {
            textures = new ITexture[] { stoneType.getTexture(0), TextureFactory.builder()
                .addIcon(TextureSet.SET_NONE.mTextures[OrePrefixes.ore.mTextureIndex])
                .stdOrient()
                .build() };
        }

        return new ITexture[][] { textures, textures, textures, textures, textures, textures };
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        StoneType stoneType = getStoneType(meta);

        return stoneType == null ? StoneType.Stone.getIcon(side) : stoneType.getIcon(side);
    }

    @Override
    public int getRenderType() {
        return GTRendererBlock.mRenderID;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {
        GTRendererBlock
            .addHitEffects(effectRenderer, this, worldObj, target.blockX, target.blockY, target.blockZ, target.sideHit);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        GTRendererBlock.addDestroyEffects(effectRenderer, this, world, x, y, z);
        return true;
    }

    public String getLocalizedNameFormat(Materials material) {
        String base;

        if (material.contains(SubTag.ICE_ORE)) {
            base = "%material Ice";
        } else {
            base = switch (material.mName) {
                case "InfusedAir", "InfusedDull", "InfusedEarth", "InfusedEntropy", "InfusedFire", "InfusedOrder", "InfusedVis", "InfusedWater" -> "%material Infused Stone";
                case "Vermiculite", "Bentonite", "Kaolinite", "Talc", "BasalticMineralSand", "GraniticMineralSand", "GlauconiteSand", "CassiteriteSand", "GarnetSand", "QuartzSand", "Pitchblende", "FullersEarth" -> "%material";
                default -> "%material" + OrePrefixes.ore.mLocalizedMaterialPost;
            };
        }

        if (GTLanguageManager.i18nPlaceholder) {
            return base;
        } else {
            return material.getDefaultLocalizedNameForItem(base);
        }
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
        if (entity instanceof EntityDragon) return false;

        return super.canEntityDestroy(world, x, y, z, entity);
    }

    @Override
    public boolean isToolEffective(String type, int metadata) {
        return "pickaxe".equals(type);
    }

    @Override
    public String getHarvestTool(int aMeta) {
        return "pickaxe";
    }

    @Override
    public int damageDropped(int meta) {
        try (OreInfo<Materials> info = GTOreAdapter.INSTANCE.getOreInfo(this, meta)) {
            if (info == null) return 0;

            return GTOreAdapter.INSTANCE.getBlock(info.setNatural(false))
                .rightInt();
        }
    }

    @Override
    public int getHarvestLevel(int meta) {
        try (OreInfo<Materials> info = GTOreAdapter.INSTANCE.getOreInfo(this, meta)) {
            if (info == null) return 0;

            int smallOreBonus = info.isSmall ? -1 : 0;

            int harvestLevel = GTMod.gregtechproxy.mChangeHarvestLevels
                ? GTMod.gregtechproxy.mHarvestLevel[info.material.mMetaItemSubID]
                : info.material.mToolQuality;

            return GTUtility.clamp(harvestLevel + smallOreBonus, 0, GTMod.gregtechproxy.mMaxHarvestLevel);
        }
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);

        return 1.0F + getHarvestLevel(meta);
    }

    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX,
        double explosionY, double explosionZ) {
        int meta = world.getBlockMetadata(x, y, z);

        return 1.0F + getHarvestLevel(meta);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
        float subY, float subZ) {
        if (!world.isRemote && player.capabilities.isCreativeMode && player.getHeldItem() == null) {
            try (OreInfo<Materials> info = GTOreAdapter.INSTANCE.getOreInfo(world, x, y, z);) {
                info.setNatural(!info.isNatural);

                world.setBlockMetadataWithNotify(
                    x,
                    y,
                    z,
                    GTOreAdapter.INSTANCE.getBlock(info)
                        .rightInt(),
                    3);
                GTUtility.sendChatToPlayer(player, "Set ore natural flag to " + info.isNatural);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public boolean isFireSource(World world, int x, int y, int z, ForgeDirection side) {
        return side == ForgeDirection.UP && getStoneType(getDamageValue(world, x, y, z)) == StoneType.Netherrack;
    }

    public static final int SMALL_ORE_META_OFFSET = 16000, NATURAL_ORE_META_OFFSET = 8000;

    public int getMaterialIndex(int meta) {
        return meta % 1000;
    }

    public int getStoneIndex(int meta) {
        meta %= SMALL_ORE_META_OFFSET;
        meta %= NATURAL_ORE_META_OFFSET;

        return meta / 1000;
    }

    public boolean isSmallOre(int meta) {
        return meta >= SMALL_ORE_META_OFFSET;
    }

    public boolean isNatural(int meta) {
        return (meta % SMALL_ORE_META_OFFSET) >= NATURAL_ORE_META_OFFSET;
    }

    public Materials getMaterial(int meta) {
        return GregTechAPI.sGeneratedMaterials[getMaterialIndex(meta)];
    }

    public StoneType getStoneType(int meta) {
        int stoneType = getStoneIndex(meta);

        if (stoneType < 0 || stoneType >= stoneTypes.size()) return null;

        return stoneTypes.get(stoneType);
    }
}
