package com.licht.ytranslator.data.sources;

import com.facebook.stetho.Stetho;
import com.licht.ytranslator.YTransApp;
import com.licht.ytranslator.data.model.ExampleObject;
import com.licht.ytranslator.data.model.HistoryObject;
import com.licht.ytranslator.data.model.Localization;
import com.licht.ytranslator.data.model.SupportedTranslation;
import com.licht.ytranslator.data.model.DictionaryObject;
import com.licht.ytranslator.data.model.WordMeaningObject;
import com.licht.ytranslator.data.model.WordObject;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class CacheData {

    @Inject
    UtilsPreferences utilsPreferences;

    public CacheData() {
        super();
        Realm.init(YTransApp.get());

        YTransApp.getAppComponent().inject(this);

        Stetho.initialize(
                Stetho.newInitializerBuilder(YTransApp.get())
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(YTransApp.get()))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(YTransApp.get()).build())
                        .build());
    }

    public void saveTranslateType(List<SupportedTranslation> types) {
        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.delete(SupportedTranslation.class);
        realm.copyToRealm(types);
        realm.commitTransaction();
    }

    public String[] getTranslateTypes() {
        final Realm realm = Realm.getDefaultInstance();
        RealmResults<SupportedTranslation> results = realm.where(SupportedTranslation.class).findAll();

        String[] translateTypes = new String[results.size()];
        for (int i = 0; i < results.size(); ++i)
            translateTypes[i] = results.get(i).getTranslation();

        return translateTypes;
    }

    public Localization[] getLanguageList(String localSymbol) {
        final Realm realm = Realm.getDefaultInstance();
        RealmResults<Localization> localizations = realm.where(Localization.class)
                .equalTo("locale", localSymbol)
                .findAll();
        return localizations.toArray(new Localization[localizations.size()]);
    }

    public void saveLocalization(List<Localization> localizations) {
        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        if (realm.where(Localization.class)
                .equalTo("locale", localizations.get(0).getLanguageSymbol())
                .count() > 0)
            realm.delete(Localization.class);

        realm.copyToRealm(localizations);
        realm.commitTransaction();
    }

    public String getTransMeaning(String localSymbol, String transSymbol) {
        final Realm realm = Realm.getDefaultInstance();
        Localization l = realm.where(Localization.class)
                .equalTo("locale", localSymbol)
                .equalTo("languageSymbol", transSymbol)
                .findFirst();

        return l.getLanguageTitle();
    }

    public DictionaryObject getCachedWord(String word, String dir) {
        final Realm realm = Realm.getDefaultInstance();
        DictionaryObject w = realm.where(DictionaryObject.class)
                            .equalTo("word", word)
                            .equalTo("direction", dir)
                            .findFirst();
        if (w != null)
            w = realm.copyFromRealm(w);
        return w;
    }

    public WordObject getCachedDictionary(long id) {
        final Realm realm = Realm.getDefaultInstance();
        WordObject wordObject = realm.where(WordObject.class)
                .equalTo("id", id)
                .findFirst();
        if (wordObject != null)
            wordObject = realm.copyFromRealm(wordObject);

        return wordObject;
    }

    public void cacheDictionary(DictionaryObject dictionaryObject) {
        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealm(dictionaryObject);
        realm.commitTransaction();
    }

    public void addWordToHistory(HistoryObject item) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(r -> {
            RealmResults<HistoryObject> it = r.where(HistoryObject.class)
                    .equalTo("word", item.getWord())
                    .equalTo("direction", item.getDirection())
                    .findAll();

            if (it.size() > 0) {
                HistoryObject historyObject = it.first();
                historyObject.setFavorites(item.isFavorites());
            } else
                r.copyToRealm(item);
        });
    }

    public boolean isStarredWord(String word, String direction) {
        final Realm realm = Realm.getDefaultInstance();
        HistoryObject obj = realm.where(HistoryObject.class)
                .equalTo("translate", word)
                .equalTo("direction", direction)
                .findFirst();
        if (obj != null)
            obj = realm.copyFromRealm(obj);
        return obj != null && obj.isFavorites();
    }

    public HistoryObject getWordFromHistory(String word, String direction) {
        final Realm realm = Realm.getDefaultInstance();
        HistoryObject historyObject = realm.where(HistoryObject.class)
                .equalTo("word", word)
                .equalTo("direction", direction)
                .findFirst();

        if (historyObject != null)
            historyObject = realm.copyFromRealm(historyObject);
        return historyObject;
    }

    public List<HistoryObject> getHistoryWords() {
        final Realm realm = Realm.getDefaultInstance();
        List<HistoryObject> res = realm.where(HistoryObject.class).equalTo("inHistory", true).findAll();

        // Открепляем объекты от realm, для того, чтоб модифицировать их не в транзакциях
        List<HistoryObject> historyObjects = new ArrayList<>();
        for (HistoryObject obj : res)
            historyObjects.add(realm.copyFromRealm(obj));

        return historyObjects;
    }


    public void updateHistoryWord(String word, String direction, boolean isHistoryWord) {
        final Realm realm = Realm.getDefaultInstance();
        final HistoryObject w = realm.where(HistoryObject.class)
                .equalTo("word", word)
                .equalTo("direction", direction).findFirst();
        if (w == null)
            return;

        realm.beginTransaction();
        w.setInHistory(isHistoryWord);
        realm.commitTransaction();
    }

    public List<HistoryObject> getFavoritesWords() {
        final Realm realm = Realm.getDefaultInstance();
        return realm.where(HistoryObject.class)
                .equalTo("isFavorites", true)
                .findAll();
    }

    public void setWordStarred(String word, String direction, boolean iStarred) {
        final Realm realm = Realm.getDefaultInstance();
        final HistoryObject w = realm.where(HistoryObject.class)
                .equalTo("word", word)
                .equalTo("direction", direction).findFirst();
        realm.beginTransaction();
        w.setFavorites(iStarred);
        realm.commitTransaction();
    }

    public int getCacheSize() {
        final Realm realm = Realm.getDefaultInstance();
        return realm.where(DictionaryObject.class).findAll().size();
    }

    public void clearCache() {
        final Realm realm = Realm.getDefaultInstance();
        final RealmQuery query = realm.where(DictionaryObject.class);
        for (HistoryObject hist : getHistoryWords())
            query.notEqualTo("word", hist.getWord());
        final List<DictionaryObject> wordsToRemove = query.findAll();

        // Realm не поддерживает каскадное удаление объектов,
        // поэтому очистка осуществляется вручную
        realm.beginTransaction();
        for (DictionaryObject dictionaryObject : wordsToRemove) {
            for (WordObject wordObject : dictionaryObject.getDictionaries()) {
                for (WordMeaningObject m : wordObject.getWordMeaningObjects()) {
                    m.getSynonimes().deleteAllFromRealm();
                    m.getMeanings().deleteAllFromRealm();

                    for (ExampleObject ex : m.getExampleObjects())
                        ex.getTranslates().deleteAllFromRealm();
                    m.getExampleObjects().deleteAllFromRealm();
                }
                wordObject.getWordMeaningObjects().deleteAllFromRealm();
            }
            dictionaryObject.getDictionaries().deleteAllFromRealm();
        }
        query.findAll().deleteAllFromRealm();

        realm.commitTransaction();

    }
}
