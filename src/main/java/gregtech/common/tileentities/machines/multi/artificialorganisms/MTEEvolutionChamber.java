package gregtech.common.tileentities.machines.multi.artificialorganisms;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.GTValues.AuthorFourIsTheNumber;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_BIOVAT_EMPTY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_BIOVAT_EMPTY_GLOW;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.ItemDrawable;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.drawable.UITexture;
import com.gtnewhorizons.modularui.api.forge.ItemStackHandler;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.api.widget.IWidgetBuilder;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.DynamicTextWidget;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.factory.artificialorganisms.MTEHatchAOOutput;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.gui.modularui.GUITextureSet;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.multitileentity.multiblock.casing.Glasses;
import gregtech.api.objects.ArtificialOrganism;
import gregtech.api.objects.ArtificialOrganism.Trait;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.blocks.BlockCasings12;
import gregtech.common.tileentities.machines.IDualInputHatch;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MTEEvolutionChamber extends MTEExtendedPowerMultiBlockBase<MTEEvolutionChamber>
    implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<MTEEvolutionChamber> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEEvolutionChamber>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            new String[][] { { "BBB", "BAB", "BAB", "BAB", "B~B" }, { "BBB", "A A", "A A", "A A", "BBB" },
                { "BBB", "BAB", "BAB", "BAB", "BBB" } })
        .addElement(
            'B',
            ofChain(
                buildHatchAdder(MTEEvolutionChamber.class)
                    .atLeast(InputBus, Maintenance, Energy, InputHatch, SpecialHatchElement.BioOutput)
                    .casingIndex(((BlockCasings12) GregTechAPI.sBlockCasings12).getTextureIndex(0))
                    .dot(1)
                    .build(),
                onElementPass(
                    MTEEvolutionChamber::onCasingAdded,
                    StructureUtility.ofBlocksTiered(
                        MTEEvolutionChamber::getTierFromMeta,
                        ImmutableList.of(
                            Pair.of(GregTechAPI.sBlockCasings12, 0),
                            Pair.of(GregTechAPI.sBlockCasings12, 1),
                            Pair.of(GregTechAPI.sBlockCasings12, 2)),
                        -3,
                        MTEEvolutionChamber::setCasingTier,
                        MTEEvolutionChamber::getCasingTier))))
        .addElement('A', Glasses.chainAllGlasses())
        .build();

    private enum SpecialHatchElement implements IHatchElement<MTEEvolutionChamber> {

        BioOutput(MTEEvolutionChamber::addBioHatch, MTEHatchAOOutput.class) {

            @Override
            public long count(MTEEvolutionChamber gtMetaTileEntityEvolutionChamber) {
                return gtMetaTileEntityEvolutionChamber.bioHatches.size();
            }
        };

        private final List<Class<? extends IMetaTileEntity>> mteClasses;
        private final IGTHatchAdder<MTEEvolutionChamber> adder;

        @SafeVarargs
        SpecialHatchElement(IGTHatchAdder<MTEEvolutionChamber> adder, Class<? extends IMetaTileEntity>... mteClasses) {
            this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
            this.adder = adder;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return mteClasses;
        }

        public IGTHatchAdder<? super MTEEvolutionChamber> adder() {
            return adder;
        }
    }

    private final ArrayList<MTEHatchAOOutput> bioHatches = new ArrayList<>();

    ArtificialOrganism currentSpecies = new ArtificialOrganism();

    private int intelligence;
    private int strength;
    private int count;
    private int sentience;

    private int casingTier;
    private int maxAOs;

    private int traitCount = 0;

    public MTEEvolutionChamber(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEEvolutionChamber(String aName) {
        super(aName);
    }

    public int getCasingTier() {
        return casingTier;
    }

    public void setCasingTier(int i) {
        casingTier = i;
    }

    private static Integer getTierFromMeta(Block block, Integer metaID) {
        if (block != GregTechAPI.sBlockCasings12) return -1;
        if (metaID < 0 || metaID > 2) return -2;
        return metaID + 1;
    }

    @Override
    public IStructureDefinition<MTEEvolutionChamber> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public void onValueUpdate(byte aValue) {
        casingTier = aValue;
    }

    @Override
    public byte getUpdateData() {
        return (byte) casingTier;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEEvolutionChamber(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        int casingMeta = Math.max(casingTier - 1, 0);
        if (side == aFacing) {
            rTexture = new ITexture[] {
                Textures.BlockIcons
                    .getCasingTextureForId(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, casingMeta)),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_BIOVAT_EMPTY)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_BIOVAT_EMPTY_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        } else {
            rTexture = new ITexture[] { Textures.BlockIcons
                .getCasingTextureForId(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, casingMeta)) };
        }
        return rTexture;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Artificial Organism Creator")
            .addInfo("Used to create and maintain Artificial Organisms")
            .addInfo("Use higher tier vat casings to get more AO culture slots")
            .addInfo(AuthorFourIsTheNumber)
            .addSeparator()
            .beginStructureBlock(3, 5, 3, true)
            .addController("Front Center")
            .addCasingInfoMin("Solid Steel Machine Casing", 85, false)
            .addCasingInfoExactly("Steel Pipe Casing", 24, false)
            .addInputBus("Any Solid Steel Casing", 1)
            .addOutputBus("Any Solid Steel Casing", 1)
            .addInputHatch("Any Solid Steel Casing", 1)
            .addOutputHatch("Any Solid Steel Casing", 1)
            .addEnergyHatch("Any Solid Steel Casing", 1)
            .addMaintenanceHatch("Any Solid Steel Casing", 1)
            .toolTipFinisher("GregTech");
        return tt;
    }

    private void updateTextures() {
        getBaseMetaTileEntity().issueTextureUpdate();
        int textureID = GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, casingTier - 1);
        for (MTEHatch h : mInputBusses) h.updateTexture(textureID);
        for (MTEHatch h : mInputHatches) h.updateTexture(textureID);
        for (IDualInputHatch h : mDualInputHatches) h.updateTexture(textureID);
        for (MTEHatch h : mMaintenanceHatches) h.updateTexture(textureID);
        for (MTEHatch h : mEnergyHatches) h.updateTexture(textureID);
        for (MTEHatch h : bioHatches) h.updateTexture(textureID);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 1, 4, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivialBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 1, 4, 0, elementBudget, env, false, true);
    }

    private int mCasingAmount;

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        casingTier = -3;
        bioHatches.clear();
        mEnergyHatches.clear();

        if (!checkPiece(STRUCTURE_PIECE_MAIN, 1, 4, 0)) return false;
        if (casingTier < 1) return false;
        updateTextures();
        maxAOs = 500000 * casingTier;
        return mCasingAmount >= 0;
    }

    private final static FluidStack nutrientcost = Materials.NutrientBroth.getFluid(5);

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);

        if (!aBaseMetaTileEntity.isServerSide() || aTick % 20 != 0 || currentSpecies == null || !finalizedSpecies)
            return;

        /*
         * for (MTEHatchInput hatch : mInputHatches) {
         * if (drain(hatch, nutrientcost, true)) {
         * break;
         * }
         * }
         */

        if (currentSpecies.getCount() < maxAOs) currentSpecies.doReproduction();
        updateSpecies();
    }

    private void updateSpecies() {
        intelligence = currentSpecies.getIntelligence();
        strength = currentSpecies.getStrength();
        count = currentSpecies.getCount();
        sentience = currentSpecies.getSentience();
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(GTUtility.roundUpVoltage(this.getMaxInputVoltage()));
        logic.setAvailableAmperage(1L);
    }

    private boolean addBioHatch(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity != null) {
            final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof MTEHatchAOOutput hatch) {
                if (currentSpecies != null) hatch.setSpecies(currentSpecies);
                return bioHatches.add(hatch);
            }
        }
        return false;
    }

    Boolean finalizedSpecies = false;

    private void createNewAOs() {
        finalizedSpecies = true;
        currentSpecies.finalize(maxAOs);
        for (MTEHatchAOOutput hatch : bioHatches) hatch.setSpecies(currentSpecies);
    }

    // TODO: consider whether this should be disabled. for now, since i've overwritten the gui, it is less confusing
    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("casingTier", Math.max(0, casingTier));
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        currentTip.add("Tier: " + EnumChatFormatting.WHITE + tag.getInteger("casingTier"));
    }

    @Override
    public GUITextureSet getGUITextureSet() {
        return GUITextureSet.ORGANIC;
    }

    // UI Pit of Doom

    protected ItemStackHandler inputSlotHandler = new ItemStackHandler(1);
    private static final int TRAIT_WINDOW_ID = 9;

    Trait activeTraitWindow;

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        builder.widget(
            new DrawableWidget().setDrawable(GTUITextures.PICTURE_SCREEN_BLACK)
                .setPos(24, 4)
                .setSize(170, 85));

        final DynamicPositionedColumn screenElements = new DynamicPositionedColumn();
        screenElements.setSynced(false)
            .setSpace(0)
            .setPos(34, 7);
        screenElements.widget(new TextWidget("Current Species: ").setDefaultColor(COLOR_TEXT_WHITE.get()));
        screenElements.widget(
            new DynamicTextWidget(() -> new Text("Intelligence: " + intelligence)).setSynced(false)
                .setDefaultColor(COLOR_TEXT_WHITE.get()))
            .widget(new FakeSyncWidget.IntegerSyncer(() -> intelligence, val -> intelligence = val))
            .widget(
                new DynamicTextWidget(() -> new Text("Strength: " + strength)).setSynced(false)
                    .setDefaultColor(COLOR_TEXT_WHITE.get()))
            .widget(new FakeSyncWidget.IntegerSyncer(() -> strength, val -> strength = val))
            .widget(
                new DynamicTextWidget(() -> new Text("Count: " + count)).setSynced(false)
                    .setDefaultColor(COLOR_TEXT_WHITE.get()))
            .widget(new FakeSyncWidget.IntegerSyncer(() -> count, val -> count = val))
            .widget(
                new DynamicTextWidget(() -> new Text("Sentience: " + sentience)).setSynced(false)
                    .setDefaultColor(COLOR_TEXT_WHITE.get()))
            .widget(new FakeSyncWidget.IntegerSyncer(() -> sentience, val -> sentience = val));
        // screenElements.setEnabled(widget -> currentSpecies != null);

        builder.widget(createPurgeButton(builder));
        builder.widget(createSentienceBar(builder));
        builder.widget(createTraitWindowButton(builder, Trait.Photosynthetic, new Pos2d(4, 4)));
        builder.widget(createTraitWindowButton(builder, Trait.HiveMind, new Pos2d(4, 20)));
        builder.widget(createTraitWindowButton(builder, Trait.Laborer, new Pos2d(4, 36)));
        builder.widget(createTraitWindowButton(builder, Trait.Decaying, new Pos2d(4, 52)));
        builder.widget(screenElements);

        // Windows
        buildContext.addSyncedWindow(TRAIT_WINDOW_ID, this::createTraitWindow);
    }

    private Widget createSentienceBar(IWidgetBuilder<?> builder) {
        return new ProgressBar().setProgress(() -> (float) sentience / 100)
            .setDirection(ProgressBar.Direction.UP)
            .setTexture(GTUITextures.PROGRESSBAR_SENTIENCE, 64)
            .setSynced(true, false)
            .setSize(16, 64)
            .setPos(173, 120);
    }

    private ModularWindow createTraitWindow(final EntityPlayer player) {
        ModularWindow.Builder builder = ModularWindow.builder(170, 85)
            .setDraggable(false);

        builder.widget(
            new DrawableWidget().setDrawable(GTUITextures.PICTURE_SCREEN_BLACK)
                .setPos(0, 0)
                .setSize(170, 85))
            .widget(createTraitItemWidget(new ItemStack(activeTraitWindow.cultureItem, 1)).setPos(95, 50))
            .widget(createAddTraitButton().setPos(95, 66))
            .widget(createFinalizeAOsButton().setPos(116, 66));

        final DynamicPositionedColumn screenElements = new DynamicPositionedColumn();
        screenElements.setSynced(false)
            .setSpace(0)
            .setPos(34, 7);
        screenElements.widget(
            new TextWidget(
                EnumChatFormatting.UNDERLINE
                    + StatCollector.translateToLocal("GT5U.artificialorganisms.traitname" + activeTraitWindow.id))
                        .setDefaultColor(COLOR_TEXT_WHITE.get()));
        screenElements.widget(
            new TextWidget(StatCollector.translateToLocal("GT5U.artificialorganisms.traitdesc" + activeTraitWindow.id))
                .setDefaultColor(COLOR_TEXT_WHITE.get()));
        builder.widget(screenElements);

        return builder.build();
    }

    private ButtonWidget createTraitWindowButton(IWidgetBuilder<?> builder, Trait trait, Pos2d pos) {
        Widget button = new ButtonWidget().setOnClick((clickData, widget) -> {
            activeTraitWindow = trait;
            if (!widget.isClient()) widget.getContext()
                .openSyncedWindow(TRAIT_WINDOW_ID);
        })
            .setPlayClickSound(supportsVoidProtection())
            .setBackground(
                () -> new IDrawable[] { GTUITextures.BUTTON_STANDARD_PRESSED,
                    new ItemDrawable(new ItemStack(trait.cultureItem)) })
            .addTooltip(StatCollector.translateToLocal("GT5U.artificialorganisms.traitname" + trait.id))
            .setTooltipShowUpDelay(TOOLTIP_DELAY)
            .setPos(pos)
            .setSize(16, 16);
        return (ButtonWidget) button;
    }

    private ButtonWidget createPurgeButton(IWidgetBuilder<?> builder) {
        Widget button = new ButtonWidget().setOnClick((clickData, widget) -> {
            intelligence = 0;
            strength = 0;
            count = 0;
            sentience = 0;
            currentSpecies = new ArtificialOrganism();
            traitCount = 0;
            for (MTEHatchAOOutput hatch : bioHatches) hatch.setSpecies(currentSpecies);
        })
            .setPlayClickSound(supportsVoidProtection())
            .setBackground(() -> {
                List<UITexture> ret = new ArrayList<>();
                ret.add(getVoidingMode().buttonTexture);
                ret.add(getVoidingMode().buttonOverlay);
                if (!supportsVoidProtection()) {
                    ret.add(GTUITextures.OVERLAY_BUTTON_FORBIDDEN);
                }
                return ret.toArray(new IDrawable[0]);
            })
            .addTooltip("Purge tank")
            .setTooltipShowUpDelay(TOOLTIP_DELAY)
            .setPos(26, 91)
            .setSize(16, 16);
        return (ButtonWidget) button;
    }

    private ButtonWidget createFinalizeAOsButton() {
        Widget button = new ButtonWidget().setOnClick((clickData, widget) -> {
            if (!widget.isClient()) {
                createNewAOs();
            }
        })
            .setPlayClickSound(supportsVoidProtection())
            .setBackground(() -> {
                List<UITexture> ret = new ArrayList<>();
                ret.add(getVoidingMode().buttonTexture);
                ret.add(getVoidingMode().buttonOverlay);
                if (!supportsVoidProtection()) {
                    ret.add(GTUITextures.OVERLAY_BUTTON_FORBIDDEN);
                }
                return ret.toArray(new IDrawable[0]);
            })
            .addTooltip("Finalize Population")
            .setTooltipShowUpDelay(TOOLTIP_DELAY)
            .setSize(16, 16);
        return (ButtonWidget) button;
    }

    private ButtonWidget createAddTraitButton() {
        Widget button = new ButtonWidget().setOnClick((clickData, widget) -> {
            if (!widget.isClient()) {
                ItemStack inputItem = inputSlotHandler.getStackInSlot(0);
                if (inputItem == null) return;
                if (inputItem.getItem() == activeTraitWindow.cultureItem && traitCount < casingTier) {
                    inputItem.stackSize -= 1;
                    currentSpecies.addTrait(activeTraitWindow);
                    updateSpecies();
                    traitCount++;
                }
            }
        })
            .setPlayClickSound(supportsVoidProtection())
            .setBackground(() -> {
                List<UITexture> ret = new ArrayList<>();
                ret.add(getVoidingMode().buttonTexture);
                ret.add(getVoidingMode().buttonOverlay);
                if (!supportsVoidProtection()) {
                    ret.add(GTUITextures.OVERLAY_BUTTON_FORBIDDEN);
                }
                return ret.toArray(new IDrawable[0]);
            })
            .addTooltip("Add Culture")
            .setTooltipShowUpDelay(TOOLTIP_DELAY)
            .setSize(16, 16);
        return (ButtonWidget) button;
    }

    public Widget createTraitItemWidget(final ItemStack costStack) {
        // Item slot
        ItemStack handlerStack = costStack.copy();
        handlerStack.stackSize = 1;
        return new SlotWidget(inputSlotHandler, 0).setAccess(true, true)
            .setFilter((stack) -> stack.getItem() == costStack.getItem())
            .setRenderStackSize(false)
            .setPos(26, 91)
            .setSize(16, 16)
            .setBackground(() -> new IDrawable[] { GTUITextures.BUTTON_STANDARD_PRESSED, new ItemDrawable(costStack) });
    }
}
