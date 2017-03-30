package com.licht.ytranslator.data;

import com.google.gson.JsonObject;
import com.licht.ytranslator.R;
import com.licht.ytranslator.YTransApp;
import com.licht.ytranslator.data.endpoint.YandexDictionaryAPI;
import com.licht.ytranslator.data.model.WordObject;
import com.licht.ytranslator.data.model.HistoryObject;
import com.licht.ytranslator.data.model.Localization;
import com.licht.ytranslator.data.model.Result;
import com.licht.ytranslator.data.model.SupportedTranslation;
import com.licht.ytranslator.data.model.Word;
import com.licht.ytranslator.data.sources.AppPreferences;
import com.licht.ytranslator.data.sources.CacheData;
import com.licht.ytranslator.data.endpoint.YandexTranslateAPI;
import com.licht.ytranslator.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Call;

/**
 * Реализует паттерн "Фасад", инкапсулирая работу со всеми возможными источниками данных
 */
public class DataManager {
    @Inject
    YandexTranslateAPI yandexTranslateAPI;

    @Inject
    YandexDictionaryAPI yandexDictionaryAPI;

    @Inject
    CacheData cacheData;

    @Inject
    AppPreferences appPreferences;

    private Localization[] mLocalizations = null;
    private String[] mTranslateTypes = null;
    /** Используемая локализация UI */
    private String mLocalSymbol = null;

    public DataManager() {
        super();
        YTransApp.getAppComponent().inject(this);

        mLocalSymbol = LocalizationUtils.getCurrentLocalizationSymbol();

        checkCachedLocalizations();
        checkTranslateTypes();
    }

    /*
     * Обращения к данным SharedPreferences
     */

    /**
     * Проверяет наличие кэшированных данных для выбранной локализации UI
     *
     * @param localization Выбранная локализация
     * @return True, если для данной локализации данные были загружены, иначе False.
     */
    public boolean isDataForLocalizationCached(String localization) {
        return appPreferences.getDataCached(localization);
    }

    /**
     * Ставит отметку, что данные для данной локализации UI были закэшированны
     *
     * @param localization Локализация UI, для которой данные были закэшированны
     */
    public void setDataForLocalizationIsCached(String localization) {
        appPreferences.putDataCached(localization, true);
    }

    /**
     * Возвращает символьный код исходного языка.
     * Если никакой язык не был выбран, то устанавливает и возвращает значение по-умолчанию
     *
     * @return Символьный код языка, с которого будет осуществляться перевод
     */
    public String getSourceLanguageSymbol() {
        String sourceLang = appPreferences.getSourceLanguage();
        if ("".equals(sourceLang)) {
            sourceLang = "en"; //todo remove to resources
            setSourceLanguageSymbol(sourceLang);
        }

        return sourceLang;
    }

    /**
     * Устанавливает символьный код исходного языка
     *
     * @param sourceLanguageSymbol Символьный код языка, с которого будет осуществляться перевод
     */
    public void setSourceLanguageSymbol(String sourceLanguageSymbol) {
        appPreferences.putSourceLanguage(sourceLanguageSymbol);
    }

    /**
     * Возвращает символьный код языка, на который будет осуществляться перевод.
     * Если никакой язык не был выбран, то устанавливает и возвращает значение по-умолчанию
     *
     * @return Символьный код языка, на который будет осуществляться перевод
     */
    public String getDestinationLanguageSymbol() {
        String destLang = appPreferences.getDestinationLanguage();
        if ("".equals(destLang)) {
            destLang = "ru"; //todo remove to resources
            setDestinationLanguage(destLang);
        }

        return destLang;
    }

    /**
     * Устанавливает символьный код языка, на который будет осуществляться перевод
     *
     * @param languageSymbol Символьный код языка, на который будет осуществляться перевод
     */
    public void setDestinationLanguage(String languageSymbol) {
        appPreferences.putDestinationLanguage(languageSymbol);
    }

    /**
     * Возвращает название языка по символьному коду, используемое в текущей локализации UI
     *
     * @param languageSymbol Символьный код языка
     * @return Название языка, используемые в текущей локализации UI
     */
    private String getLanguageName(String languageSymbol) {
        return cacheData.getTransMeaning(mLocalSymbol, languageSymbol);
    }

    /*
     * Обращения к БД
     */

    /**
     * Возвращает символьный код языка, которому соответствует указанное имя языка
     *
     * @param languageName Имя языка, используемое в текущей локализации UI
     * @return Символьный код языка
     */
    public String getLanguageSymbolByName(String languageName) {
        checkCachedLocalizations();
        for (Localization localization : mLocalizations)
            if (localization.getLanguageTitle().equals(languageName))
                return localization.getLanguageSymbol();

        return "";
    }

    public boolean isStarredWord(String word, String direction) {
        return cacheData.isStarredWord(word, direction);
    }

    public void addWordToHistory(HistoryObject item) {
        cacheData.addWordToHistory(item);
    }

    public List<HistoryObject> getHistoryWords() {
        return cacheData.getHistoryWords();
    }

    public List<HistoryObject> getStarredWords() {
        return cacheData.getFavoritesWords();
    }

    /*
     * Обращения к API
     */

    public Call<JsonObject> getDataFromDictionary(String key, String text, String lang) {
        return yandexDictionaryAPI.getMeaning(buildMapToRequest(key, lang, text));
    }

    /**
     * Загружает данные для указанной локализации
     *
     * @param localization Локализация UI
     * @return Объект, используемый для асинхронной загрузки данных
     */
    public Call<JsonObject> loadDataForLocalization(String localization) {
        return yandexTranslateAPI.getData(YTransApp.get().getString(R.string.key_translate), localization);
    }

    /**
     * Загружает перевод для указанного текста с указанными параметрами
     *
     * @param key Ключ API
     * @param text Исходный текст
     * @param lang Направление перевода
     * @return Объект, используемый для асинхронной загрузки данных
     */
    public Call<Result> requestTranslation(String key, String text, String lang) {

        return yandexTranslateAPI.translate(buildMapToRequest(key, lang, text));
    }

    /**
     * Кэширует переданные данные приложения
     *
     * @param supportedTranslations Список доступных направлений перевода
     * @param localizations Список обёрток над локализациями
     */
    public void cacheLanguageData(List<SupportedTranslation> supportedTranslations,
                                  List<Localization> localizations) {
        cacheData.saveTranslateType(supportedTranslations);
        mTranslateTypes = null;

        cacheData.saveLocalization(localizations);
        mLocalizations = null;
    }

    /**
     * Возвращает закэшированную информацию о слове, полученную из Яндекс Словаря
     *
     * @param word Запрашиваемое слово
     * @param dir Направление перевод
     * @return Объект, содержащий информацию о слове
     */
    public Word getCachedWord(String word, String dir) {
        return cacheData.getCachedWord(word, dir);
    }

    public WordObject getCachedDictionary(long id) {
        return cacheData.getCachedDictionary(id);
    }

    public synchronized void cacheDictionaryWord(Word word) {
        cacheData.cacheDictionary(word);
    }

    /*
     * Остальные функции
     */

    /**
     * @return Название исходного языка в текущей локализации UI
     */
    public String getSourceLanguage() {
        String sourceLang = getSourceLanguageSymbol();
        return getLanguageName(sourceLang);
    }

    /**
     * @return Название языка, в который будет осуществляться перевод, в текущей локализации UI
     */
    public String getDestinationLanguage() {
        String destLang = getDestinationLanguageSymbol();
        return getLanguageName(destLang);
    }

    /**
     * @return Список языков, с которых можно осуществлять перевод
     */
    public ArrayList<String> getSourceLanguageList() {
        checkCachedLocalizations();
        final ArrayList<String> list = new ArrayList<>();
        for (Localization localization : mLocalizations)
            list.add(localization.getLanguageTitle());

        return list;
    }

    /**
     *
     * @return Список языков, в которые можно осуществить перевод из исходнего
     */
    public ArrayList<String> getDestinationLanguageList() {
        return getDestinationLanguageList(getSourceLanguageSymbol());
    }

    public ArrayList<String> getDestinationLanguageList(String sourceLanguageSymbol) {
        final ArrayList<String> result = new ArrayList<>();

        for (String translateType : mTranslateTypes)
            if (translateType.startsWith(sourceLanguageSymbol)) {
                final String destLangSym = translateType.split("-")[1];
                result.add(getLanguageName(destLangSym));
            }

        return result;
    }

    public int getCacheSize() {
        return cacheData.getCacheSize();
    }

    public void clearCache() {
        cacheData.clearCache();
    }

    private void checkCachedLocalizations() {
        if (mLocalizations == null)
            mLocalizations = cacheData.getLanguageList(mLocalSymbol);
    }

    private void checkTranslateTypes() {
        if (mTranslateTypes == null)
            mTranslateTypes = cacheData.getTranslateTypes();
    }

    private Map<String, String> buildMapToRequest(String key, String lang, String text) {
        Map<String, String> mapJSON = new HashMap<>();
        mapJSON.put("key", key);
        mapJSON.put("text", text);
        mapJSON.put("lang", lang);
        return mapJSON;
    }
}
