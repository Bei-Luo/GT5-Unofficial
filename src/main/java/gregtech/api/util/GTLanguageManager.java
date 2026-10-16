package gregtech.api.util;

import static gregtech.GTLoggers.GT_FML_LOGGER;
import static gregtech.api.enums.GTValues.E;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import gregtech.api.GregTechAPI;

/**
 * @deprecated Use standard translation with {@link StatCollector}.
 */
@Deprecated
public class GTLanguageManager {

    /**
     * Buffer to reduce memory allocation when injecting data to LanguageRegistry.
     */
    private static final HashMap<String, String> TEMPMAP = new HashMap<>();
    /**
     * Buffer used when something is trying to add new lang entry while config file is not set up yet.
     */
    public static final Map<String, String> BUFFERMAP = new HashMap<>();
    /**
     * Map containing all the translation data coming into this class.
     */
    private static final Map<String, String> LANGMAP = new HashMap<>();
    /**
     * Config file handler bound to GregTech.lang or GregTech_(locale_name).lang. Even though it says English file, it's
     * not necessarily English, but on system it's always treated as English (as in, "default" language.)
     */
    public static Configuration sEnglishFile;
    /**
     * If the game is running with en_US language. This does not get updated when user changes language in game; GT lang
     * system cannot handle that anyway.
     */
    public static boolean isEN_US;
    /**
     * If placeholder like %material should be used for writing lang entries to file.
     */
    @Deprecated
    public static boolean i18nPlaceholder = true;
    /**
     * If there's any lang entry that is not found on lang file and waiting to be written.
     */
    private static boolean hasUnsavedEntry = false;
    /**
     * Language forced for dedicated-server lookups, or {@code null} when the server keeps the default language.
     */
    private static String sForcedServerLanguage = null;

    /**
     * Map referencing private field of StringTranslate, used by StatCollector. Used to inject lang entries there.
     */
    private static final Map<String, String> stringTranslateLanguageList;
    private static final Map<String, String> stringTranslateLanguageListFallBack;
    public static String LanguageCode = "en_US";
    public static Locale LOCALE;

    static {
        try {
            Field fieldStringTranslateLanguageList = ReflectionHelper
                .findField(net.minecraft.util.StringTranslate.class, "languageList", "field_74816_c");
            Field fieldStringTranslateInstance = ReflectionHelper
                .findField(net.minecraft.util.StringTranslate.class, "instance", "field_74817_a");
            Field fieldStatCollectorFallbackTranslator = ReflectionHelper
                .findField(net.minecraft.util.StatCollector.class, "fallbackTranslator", "field_150828_b");
            // noinspection unchecked
            stringTranslateLanguageList = (Map<String, String>) fieldStringTranslateLanguageList
                .get(fieldStringTranslateInstance.get(null));
            stringTranslateLanguageListFallBack = (Map<String, String>) fieldStringTranslateLanguageList
                .get(fieldStatCollectorFallbackTranslator.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * If you newly use this method, please consider using MC lang system instead.
     */
    public static synchronized void addStringLocalization(String aKey, String aEnglish) {
        String trimmedKey = aKey != null ? aKey.trim() : "";
        if (trimmedKey.isEmpty()) return; // RIP cascading class loading, don't use GT_Utility here
        if (aEnglish == null) aEnglish = "";
        if (sEnglishFile == null) {
            // Lang file is not set up yet
            BUFFERMAP.put(trimmedKey, aEnglish);
            return;
        }
        if (!BUFFERMAP.isEmpty()) {
            // Lang file is now set up, resolve all the buffers
            // This won't be visited twice
            for (Entry<String, String> tEntry : BUFFERMAP.entrySet()) {
                storeTranslation(tEntry.getKey(), tEntry.getValue());
            }
            BUFFERMAP.clear();
        }
        if (!LANGMAP.containsKey(trimmedKey)) {
            storeTranslation(trimmedKey, aEnglish);
        }
    }

    private static synchronized void storeTranslation(String trimmedKey, String english) {
        String translation = writeToLangFile(trimmedKey, english);
        LANGMAP.put(trimmedKey, translation);
        TEMPMAP.put(trimmedKey, translation);
        LanguageRegistry.instance()
            .injectLanguage(LanguageCode, TEMPMAP);
        TEMPMAP.clear();
    }

    private static synchronized String writeToLangFile(String trimmedKey, String aEnglish) {
        addToMCLangListFallBack(trimmedKey, aEnglish);
        // If the key is already provided by the standard lang system, don't touch GregTech.lang.
        if (StatCollector.canTranslate(trimmedKey)) {
            return StatCollector.translateToLocal(trimmedKey);
        }
        Property tProperty = sEnglishFile.get("LanguageFile", trimmedKey, aEnglish);
        if (hasUnsavedEntry && GregTechAPI.sPostloadFinished) {
            sEnglishFile.save();
            hasUnsavedEntry = false;
        }
        String translation = tProperty.getString();
        if (tProperty.wasRead()) {
            if (isEN_US && !aEnglish.equals(translation)) {
                tProperty.set(aEnglish);
                markFileDirty();
                return aEnglish;
            }
        } else {
            markFileDirty();
        }
        addToMCLangList(trimmedKey, translation);
        return translation;
    }

    private static synchronized void markFileDirty() {
        if (GregTechAPI.sPostloadFinished) {
            sEnglishFile.save();
        } else {
            hasUnsavedEntry = true;
        }
    }

    public static String getTranslation(String aKey) {
        String tTrimmedKey = aKey != null ? aKey.trim() : "";
        if (tTrimmedKey.isEmpty()) return E;

        if (StatCollector.canTranslate(tTrimmedKey)) {
            return StatCollector.translateToLocal(tTrimmedKey);
        }
        String anotherKeyToTry;
        if (tTrimmedKey.endsWith(".name")) {
            anotherKeyToTry = tTrimmedKey.substring(0, tTrimmedKey.length() - 5);
        } else {
            anotherKeyToTry = tTrimmedKey + ".name";
        }
        if (StatCollector.canTranslate(anotherKeyToTry)) {
            return StatCollector.translateToLocal(anotherKeyToTry);
        }
        return tTrimmedKey;
    }

    @SuppressWarnings("unused")
    public static String getTranslateableItemStackName(ItemStack aStack) {
        if (GTUtility.isStackInvalid(aStack)) return "null";
        final String tName = ItemStackNBT.getDisplayName(aStack);
        if (GTUtility.isStringValid(tName)) {
            return tName;
        }
        return aStack.getUnlocalizedName() + ".name";
    }

    private static void addToMCLangList(String aKey, String translation) {
        if (stringTranslateLanguageList != null) {
            stringTranslateLanguageList.put(aKey, translation);
        }
    }

    private static void addToMCLangListFallBack(String aKey, String english) {
        if (stringTranslateLanguageListFallBack != null) {
            stringTranslateLanguageListFallBack.put(aKey, english);
        }
    }

    public static synchronized void reloadLanguage(Map<String, String> languageMap) {
        if (!GregTechAPI.sFullLoadFinished) return;
        File languageDir = sEnglishFile.getConfigFile()
            .getParentFile();
        String userLang = Minecraft.getMinecraft()
            .getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
        LanguageCode = userLang;
        if (userLang.equals("en_US")) {
            reloadLanguageWithEnglish(languageDir, languageMap);
            return;
        }
        String l10nFileName = "GregTech_" + userLang + ".lang";
        File l10nFile = new File(languageDir, l10nFileName);
        if (!l10nFile.isFile()) {
            reloadLanguageWithEnglish(languageDir, languageMap);
            return;
        }
        sEnglishFile = new Configuration(l10nFile);
        isEN_US = false;
        sEnglishFile.load();
        for (String key : LANGMAP.keySet()) {
            if (languageMap.containsKey(key)) {
                LANGMAP.put(key, languageMap.get(key));
                continue;
            }
            Property tProperty = sEnglishFile.get("LanguageFile", key, stringTranslateLanguageListFallBack.get(key));
            String translation = tProperty.getString();
            languageMap.put(key, translation);
            LANGMAP.put(key, translation);
        }
    }

    public static synchronized void reloadLanguageWithEnglish(File languageDir, Map<String, String> languageMap) {
        isEN_US = true;
        sEnglishFile = new Configuration(new File(languageDir, "GregTech.lang"));
        for (String key : LANGMAP.keySet()) {
            if (languageMap.containsKey(key)) {
                LANGMAP.put(key, languageMap.get(key));
                continue;
            }
            String english = stringTranslateLanguageListFallBack.get(key);
            languageMap.put(key, english);
            LANGMAP.put(key, english);
        }
    }

    /**
     * Injects the mod's bundled lang file for the given language into the live {@link StatCollector} lookup tables.
     * <p>
     * This is meant for dedicated servers, which only ever load {@code en_US}. Text resolved server-side can be baked
     * into NBT or network packets and later displayed verbatim by clients, so without this a server would always leak
     * English (e.g. Nuclear Control panels fed by the Metrics Transmitter cover).
     *
     * @param lang the language code whose bundled lang file should be used
     */
    public static synchronized void loadLanguageForServer(String lang) {
        sForcedServerLanguage = lang;
        applyForcedServerLanguage();
    }

    /**
     * Re-applies the previously forced server language, if any. Intended to be called late in startup so that later
     * language merges cannot clobber the injected entries.
     */
    public static synchronized void reapplyForcedServerLanguage() {
        if (sForcedServerLanguage != null) applyForcedServerLanguage();
    }

    private static synchronized void applyForcedServerLanguage() {
        final Map<String, String> parsed = new HashMap<>();
        readNamespaceLangFile("gregtech", parsed);
        readNamespaceLangFile("bartworks", parsed);
        readNamespaceLangFile("detravscannermod", parsed);
        readNamespaceLangFile("ggfab", parsed);
        readNamespaceLangFile("goodgenerator", parsed);
        readNamespaceLangFile("gtneioreplugin", parsed);
        readNamespaceLangFile("gtnhintergalactic", parsed);
        readNamespaceLangFile("gtnhlanth", parsed);
        readNamespaceLangFile("ic2", parsed);
        readNamespaceLangFile("kekztech", parsed);
        readNamespaceLangFile("kubatech", parsed);
        readNamespaceLangFile("miscutils", parsed);
        readNamespaceLangFile("spiceoflife", parsed);
        readNamespaceLangFile("stevescarts", parsed);
        readNamespaceLangFile("tectech", parsed);
        // Legacy keys such as "Material.<name>" only exist in the old GregTech.lang system, so merge that file too.
        readLegacyLangFile(parsed);
        if (parsed.isEmpty()) return;
        LANGMAP.putAll(parsed);
        if (stringTranslateLanguageList != null) stringTranslateLanguageList.putAll(parsed);
        if (stringTranslateLanguageListFallBack != null) stringTranslateLanguageListFallBack.putAll(parsed);
        GT_FML_LOGGER.info("Loaded {} translations for {}", parsed.size(), sForcedServerLanguage);
    }

    private static void readNamespaceLangFile(String modid, Map<String, String> out) {
        final String path = "/assets/" + modid + "/lang/";
        readLangFile(path + sForcedServerLanguage + ".lang", out);
    }

    /** Reads a standard {@code key=value} lang file and merges its entries into {@code out}. */
    private static void readLangFile(String resource, Map<String, String> out) {
        try (InputStream stream = GTLanguageManager.class.getResourceAsStream(resource)) {
            if (stream == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') continue;
                    final int split = line.indexOf('=');
                    if (split <= 0) continue;
                    final String key = line.substring(0, split)
                        .trim();
                    final String value = line.substring(split + 1);
                    out.put(key, value);
                }
            }
        } catch (IOException e) {
            GT_FML_LOGGER.warn("Failed to read server language file " + resource, e);
        }
    }

    /** Reads a legacy {@code GregTech.lang} style Forge configuration file and merges it into {@code out}. */
    private static void readLegacyLangFile(Map<String, String> out) {
        final String path = "/gt_server_lang/legacy_" + sForcedServerLanguage + ".lang";
        try (InputStream stream = GTLanguageManager.class.getResourceAsStream(path)) {
            if (stream == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmed = line.trim();
                    if (!trimmed.startsWith("S:")) continue;
                    final int split = trimmed.indexOf('=', 2);
                    if (split <= 2) continue;
                    final String key = trimmed.substring(2, split)
                        .trim();
                    final String value = trimmed.substring(split + 1);
                    out.put(key, value);
                }
            }
        } catch (IOException e) {
            GT_FML_LOGGER.warn("Failed to read server language file " + path, e);
        }
    }

    public static boolean hasGTLocalizationKey(final String key) {
        return LANGMAP.containsKey(key);
    }
}
