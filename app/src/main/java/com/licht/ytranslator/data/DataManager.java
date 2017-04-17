package com.licht.ytranslator.data;

import com.google.gson.JsonObject;
import com.licht.ytranslator.R;
import com.licht.ytranslator.YTransApp;
import com.licht.ytranslator.data.endpoint.YandexDictionaryAPI;
import com.licht.ytranslator.data.endpoint.YandexTranslateAPI;
import com.licht.ytranslator.data.model.HistoryObject;
import com.licht.ytranslator.data.model.Localization;
import com.licht.ytranslator.data.model.Result;
import com.licht.ytranslator.data.model.SupportedTranslation;
import com.licht.ytranslator.data.model.DictionaryObject;
import com.licht.ytranslator.data.model.WordObject;
import com.licht.ytranslator.data.sources.CacheData;
import com.licht.ytranslator.data.sources.CachedPreferences;
import com.licht.ytranslator.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * Реализует паттерн "Фасад", инкапсулирая работу со всеми возможными источниками данных
 */
public class DataManager {
    private final YandexTranslateAPI yandexTranslateAPI;
    private final YandexDictionaryAPI yandexDictionaryAPI;
    private final CacheData cacheData;
    private final CachedPreferences cachedPreferences;

    private Localization[] mLocalizations = null;

    /**
     * Используемая локализация UI
     */
    private String mLocalSymbol = null;

    public DataManager(YandexTranslateAPI yandexTranslateAPI,
                       YandexDictionaryAPI yandexDictionaryAPI,
                       CacheData cacheData,
                       CachedPreferences cachedPreferences) {
        super();

        this.yandexTranslateAPI = yandexTranslateAPI;
        this.yandexDictionaryAPI = yandexDictionaryAPI;
        this.cacheData = cacheData;
        this.cachedPreferences = cachedPreferences;

        mLocalSymbol = LocalizationUtils.getCurrentLocalizationSymbol();

        checkCachedLocalizations();
    }

    /*
     * Обращения к API
     */

    /**
     * Получает перевод текста в указанном направлении через API Яндекс Словаря
     *
     * @param key ключ API
     * @param text переводимый текст
     * @param lang направление перевода
     * @param ui используемая локализация приложения
     * @return Объект, используемый для асинхронной загрузки данных
     */
    public Call<JsonObject> getDataFromDictionary(String key, String text, String lang, String ui) {
        return yandexDictionaryAPI.getMeaning(buildMapToRequest(key, lang, text, ui));
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
     * @param key  Ключ API
     * @param text Исходный текст
     * @param lang Направление перевода
     * @return Объект, используемый для асинхронной загрузки данных
     */
    public Call<Result> requestTranslation(String key, String text, String lang) {
        return yandexTranslateAPI.translate(buildMapToRequest(key, lang, text));
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
        return cachedPreferences.getDataCached(localization);
    }

    /**
     * Ставит отметку, что данные для данной локализации UI были закэшированны
     *
     * @param localization Локализация UI, для которой данные были закэшированны
     */
    public void setDataForLocalizationIsCached(String localization) {
        cachedPreferences.putDataCached(localization, true);
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
     * Обращения к данным БД
     */

    /**
     * Добавляет или удаляет перевод из списка избранного
     *
     * @param word Переводимый текст
     * @param direction Направление перевода
     * @param iStarred Добавлен ли перевод в избранное сейчас
     */
    public void setWordStarred(String word, String direction, boolean iStarred) {
        cacheData.setWordStarred(word, direction, iStarred);
    }

    /**
     * Добавляет перевод в историю
     *
     * @param item Объект перевода
     */
    public void addWordToHistory(HistoryObject item) {
        cacheData.addWordToHistory(item);
    }

    /**
     * Возвращает список переводов из истории
     *
     * @return Список переводов из истории
     */
    public List<HistoryObject> getHistoryWords() {
        return cacheData.getHistoryWords();
    }

    /**
     * Находит объект перевода по переводимому тексту и направлению перевода
     *
     * @param word Переводимый текст
     * @param direction Направление перевода
     * @return Объект перевода
     */
    public HistoryObject getHistoryWord(String word, String direction) {
        return cacheData.getWordFromHistory(word, direction);
    }

    /**
     * Возвращает список переводов, попавших в избранное
     *
     * @return Список избранных переводов
     */
    public List<HistoryObject> getStarredWords() {
        return cacheData.getFavoritesWords();
    }

    /**
     * Удаляет все переводы из истории
     *
     * @param starredOnly True, если удаляются только избранные переводы. False, если удаляется вся история
     */
    public void clearHistory(boolean starredOnly) {
        if (starredOnly)
            cacheData.clearStarredList();
        else
            cacheData.clearHistory();
    }

    /**
     * Кэширует переданные данные приложения
     *
     * @param supportedTranslations Список доступных направлений перевода
     * @param localizations         Список обёрток над локализациями
     */
    public void cacheLanguageData(List<SupportedTranslation> supportedTranslations,
                                  List<Localization> localizations) {
        cacheData.saveTranslateType(supportedTranslations);
        cacheData.saveLocalization(localizations);

        mLocalizations = null;
    }

    /**
     * Возвращает закэшированную информацию о слове, полученную из Яндекс Словаря
     *
     * @param word Запрашиваемое слово
     * @param dir  Направление перевод
     * @return Объект, содержащий информацию о слове
     */
    public DictionaryObject getCachedWord(String word, String dir) {
        return cacheData.getCachedWord(word, dir);
    }

    public WordObject getCachedDictionary(long id) {
        return cacheData.getCachedDictionary(id);
    }

    public void cacheDictionaryWord(DictionaryObject dictionaryObject) {
        cacheData.cacheDictionary(dictionaryObject);
    }

    public void updateHistoryWord(String word, String direction, boolean isHistoryWord) {
        cacheData.updateHistoryWord(word, direction, isHistoryWord);
    }


    /**
     * Кэш перевода живёт какое-то ограниченное количество дней
     * При каждом запуске приложения проводится очистка кэша. Если какой-то перевод хранится больше,
     * чем заданное количество дней, то он удаляется.
     *
     * Если перевод попал в историю, то он не удаляется никогда
     */
    public void clearCacheIfNecessary() {
            cacheData.clearCache();
    }


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

    /*
     * Остальные функции
     */

    private ArrayList<String> mCachedLanguagesList = null;

    public ArrayList<String> getLanguagesList() {
        if (mCachedLanguagesList != null)
            return mCachedLanguagesList;

        checkCachedLocalizations();
        mCachedLanguagesList = new ArrayList<>();
        for (Localization localization : mLocalizations)
            mCachedLanguagesList.add(localization.getLanguageTitle());

        Collections.sort(mCachedLanguagesList, String::compareTo);

        return mCachedLanguagesList;
    }

    private void checkCachedLocalizations() {
        if (mLocalizations == null)
            mLocalizations = cacheData.getLanguageList(mLocalSymbol);
    }


    public String getLanguageByCode(String code) {
        checkCachedLocalizations();
        for (Localization localization : mLocalizations)
            if (localization.getLanguageSymbol().equals(code))
                return localization.getLanguageTitle();
        return "";
    }


    /*
     * Вспомогательные функции
     */

    // Функции составляют объект для POST-запросов
    private Map<String, String> buildMapToRequest(String key, String lang, String text) {
        Map<String, String> mapJSON = new HashMap<>();
        mapJSON.put("key", key);
        mapJSON.put("text", text);
        mapJSON.put("lang", lang);
        return mapJSON;
    }

    private Map<String, String> buildMapToRequest(String key, String lang, String text, String ui) {
        Map<String, String> mapJSON = buildMapToRequest(key, lang, text);
        mapJSON.put("ui", ui);
        return mapJSON;
    }
}
