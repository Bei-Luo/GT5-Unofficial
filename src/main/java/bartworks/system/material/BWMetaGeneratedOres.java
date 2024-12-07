/*
 * Copyright (c) 2018-2020 bartimaeusnek Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package bartworks.system.material;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTLanguageManager;
import gregtech.api.util.GTModHandler;
import gregtech.common.ores.BWOreAdapter;
import gregtech.common.ores.OreInfo;

public class BWMetaGeneratedOres extends BWMetaGeneratedBlocks {

    public final boolean isNatural;

    public BWMetaGeneratedOres(Material p_i45386_1_, Class<? extends TileEntity> tileEntity, String blockName, boolean natural) {
        super(p_i45386_1_, tileEntity, blockName);
        this.blockTypeLocalizedName = GTLanguageManager.addStringLocalization(
            "bw.blocktype." + OrePrefixes.ore,
            OrePrefixes.ore.mLocalizedMaterialPre + "%material" + OrePrefixes.ore.mLocalizedMaterialPost);
        this.isNatural = natural;
    }

    @Override
    protected void doRegistrationStuff(Werkstoff w) {
        if (w != null) {
            if (!w.hasItemType(OrePrefixes.ore) || (w.getGenerationFeatures().blacklist & 0b1000) != 0) return;
            GTModHandler.addValuableOre(this, w.getmID(), 1);
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.stone.getIcon(0, 0);
    }

    @Override
    public IIcon getIcon(IBlockAccess worldIn, int x, int y, int z, int side) {
        return Blocks.stone.getIcon(0, 0);
    }

    @Override
    public int getHarvestLevel(int metadata) {
        return 3;
    }

    @Override
    public String getUnlocalizedName() {
        return "bw.blockores.01";
    }

    @Override
    public void getSubBlocks(Item aItem, CreativeTabs aTab, List<ItemStack> aList) {
        if (!isNatural) {
            for (Werkstoff tMaterial : Werkstoff.werkstoffHashSet) {
                if (tMaterial != null && tMaterial.hasItemType(OrePrefixes.ore)
                    && (tMaterial.getGenerationFeatures().blacklist & 0x8) == 0) {
                    aList.add(new ItemStack(aItem, 1, tMaterial.getmID()));
                }
            }
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        EntityPlayer harvester = this.harvesters.get();

        boolean doFortune = !(harvester instanceof FakePlayer);
        boolean doSilktouch = harvester != null && EnchantmentHelper.getSilkTouchModifier(harvester);

        try (OreInfo<Werkstoff> info = BWOreAdapter.INSTANCE.getOreInfo(this, metadata);) {
            return (ArrayList<ItemStack>) BWOreAdapter.INSTANCE.getOreDrops(info, doSilktouch, doFortune ? fortune : 0);
        }
    }
}
