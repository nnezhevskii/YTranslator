package com.licht.ytranslator.presenters;

import com.google.gson.JsonObject;
import com.licht.ytranslator.R;
import com.licht.ytranslator.YTransApp;
import com.licht.ytranslator.data.DataManager;
import com.licht.ytranslator.data.model.HistoryObject;
import com.licht.ytranslator.data.model.Result;
import com.licht.ytranslator.data.model.Word;
import com.licht.ytranslator.data.model.WordObject;
import com.licht.ytranslator.data.sources.UserPreferences;
import com.licht.ytranslator.ui.TranslateView.ITranslateView;
import com.licht.ytranslator.utils.DictionaryAnswerParser;

import java.util.ArrayList;

import javax.inject.Inject;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TranslatePresenter implements IPresenter<ITranslateView> {
    @Inject
    DataManager dataManager;

    private UserPreferences userPreferences;

    private ITranslateView view;

    public TranslatePresenter() {
        super();
        YTransApp.getAppComponent().inject(this);
        userPreferences = new UserPreferences();
    }

    @Override
    public void bindView(ITranslateView iTranslateView) {
        this.view = iTranslateView;
    }

    @Override
    public void unbindView() {
        this.view = null;
    }

    /**
     * Инициализирует окно перевода значениями, которые были при закрытии
     */
    public void requestData() {
        String input = userPreferences.getInputText();
        if (input == null) {
            input = "";
            userPreferences.setInputText(input);
        }

        String translateDirection = userPreferences.getTranslateDirection();
        if (translateDirection == null || "".equals(translateDirection)) {
            translateDirection = "en-ru";
            userPreferences.setDirectionText(translateDirection);
        }

        initializeData(input, translateDirection);

    }

    /**
     * Инициализирует окно перевода переданными значениями
     *
     * @param inputText          Переводимый текст
     * @param translateDirection Направление перевода
     */
    public void initializeData(String inputText, String translateDirection) {
        final String[] languages = translateDirection.split("-");

        view.setInputText(inputText);
        view.setLanguagePair(dataManager.getLanguageByCode(languages[0]),
                dataManager.getLanguageByCode(languages[1]));
    }

    /**
     * Вызывается при изменении содержимого поля ввода текста на экране перевода
     *
     * @param content Новый текст
     */
    public void onTextInput(String content) {
        if (content == null)
            return;

        if ("".equals(content)) {
            view.setTranslatedText("");
            return;
        }

        userPreferences.setInputText(content);

        if (view != null) {
            view.detailsAreAvailable(false);
            view.isStarVisible(false);
        }

        translateText();
    }


    public void translateText() {
        final String text = userPreferences.getInputText();
        final String direction = userPreferences.getTranslateDirection();

        final HistoryObject historyObject = dataManager.getHistoryWord(text, direction);
        if (historyObject != null)
            initializeTranslate(historyObject);
        else {
            final String key = YTransApp.get().getString(R.string.key_translate);
            requestTranslation(key, text, direction);
        }

        final Word word = dataManager.getCachedWord(text, direction);
        if (word != null)
            initializeDictionary(word);
        else {
            final String keyDict = YTransApp.get().getString(R.string.key_dictionary);
            requestMeaningFromDictionary(keyDict, text, direction);
        }
    }

    public void onKeyboardHide() {
        // Если пользователь закрыл клавиатуру, то он просматривает перевод слова
        // В этой ситуации мы сохраняем слово в историю

        // Помечаем, что слово, которое мы ранее закэшировали, теперь входит в историю
        addWordToHistory();

    }

    /**
     * Вызывается при изменении пользователем исходного языка
     *
     * @param newSourceLanguage Название нового исходного языка, написанное в используемой локализации
     */
    public void onUpdateSourceLanguage(String newSourceLanguage) {
        // Переводим название языка в его кодовое обозначение
        final String langSymbol = dataManager.getLanguageSymbolByName(newSourceLanguage);

        final String currentDirection = userPreferences.getTranslateDirection();
        final String[] tokens = currentDirection.split("-");
        final String newDirection = String.format("%s-%s", langSymbol, tokens[1]);

        userPreferences.setDirectionText(newDirection);

        updateLanguagePairInView(newDirection);
    }

    /**
     * Вызывается при изменении пользователем языка, на который осуществляется перевод
     *
     * @param newDestinationLanguage Название языка, написанное в используемой локализации
     */
    public void onUpdateDestinationLanguage(String newDestinationLanguage) {
        // Переводим название языка в его кодовое обозначение
        final String langSymbol = dataManager.getLanguageSymbolByName(newDestinationLanguage);

        final String currentDirection = userPreferences.getTranslateDirection();
        final String[] tokens = currentDirection.split("-");
        final String newDirection = String.format("%s-%s", tokens[0], langSymbol);

        userPreferences.setDirectionText(newDirection);

        updateLanguagePairInView(newDirection);
    }

    public void onSwapLanguages() {
        final String currentDirection = userPreferences.getTranslateDirection();
        final String[] tokens = currentDirection.split("-");
        final String newDirection = String.format("%s-%s", tokens[1], tokens[0]);
        userPreferences.setDirectionText(newDirection);

        HistoryObject obj = dataManager.getHistoryWord(userPreferences.getInputText(), currentDirection);
        view.setInputText(obj.getTranslate());
        view.setLanguagePair(dataManager.getLanguageByCode(tokens[1]),
                dataManager.getLanguageByCode(tokens[0]));

    }


    public void onOpenDictionaryClick() {
        if (view != null)
            view.openDictionary(userPreferences.getInputText(), userPreferences.getTranslateDirection());
    }

    public void onStarredClick() {
        HistoryObject obj = dataManager.getHistoryWord(userPreferences.getInputText(),
                userPreferences.getTranslateDirection());
        if (obj == null)
            return;

        final boolean isFavorites = obj.isFavorites();

        updateStarredWord(!isFavorites);
        view.setIsStarredView(!isFavorites);
    }

    public void onClearInput() {
        view.isStarVisible(false);
        view.detailsAreAvailable(false);
        view.setInputText("");
    }

    public String getSourceLanguage() {
        final String sym = userPreferences.getTranslateDirection().split("-")[0];
        return dataManager.getLanguageByCode(sym);
    }

    public String getDestinationLanguage() {
        final String sym = userPreferences.getTranslateDirection().split("-")[1];
        return dataManager.getLanguageByCode(sym);
    }

    public ArrayList<String> getSourceLanguages() {
        return dataManager.getSourceLanguageList();
    }

    public ArrayList<String> getDestinationLanguages() {
        return dataManager.getDestinationLanguageList(userPreferences.getTranslateDirection().split("-")[0]);
    }


    private void requestTranslation(String key, String text, String direction) {
        dataManager.requestTranslation(key, text, direction).enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                final int code = response.body().code; // todo check code

                final String result = response.body().text.get(0);
                HistoryObject historyObject = new HistoryObject(text, result, direction, false, false);
                dataManager.addWordToHistory(historyObject);

                initializeTranslate(historyObject);
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                // todo
            }
        });
    }

    private void requestMeaningFromDictionary(String key, String text, String direction) {
        dataManager.getDataFromDictionary(key, text, direction).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // todo check result

                RealmList<WordObject> dicts = DictionaryAnswerParser.parse(response.body());
                Word w = new Word(text, direction, dicts);
                dataManager.cacheDictionaryWord(w);
                initializeDictionary(w);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // todo
            }
        });
    }

    private void initializeTranslate(HistoryObject result) {
        if (view == null)
            return;
        view.setTranslatedText(result.getTranslate());
        view.isStarVisible(true);
        view.setIsStarredView(result.isFavorites());
    }

    private void initializeDictionary(Word w) {
        final boolean detailsAreAvailable = w.getDictionaries().size() > 0;
        if (view != null)
            view.detailsAreAvailable(detailsAreAvailable);
    }

    private void doInTranslateFailure() {
        // todo
    }

    private void addWordToHistory() {
        final String text = userPreferences.getInputText();
        final String direction = userPreferences.getTranslateDirection();


        dataManager.updateHistoryWord(text, direction, true);
    }

    private void updateStarredWord(boolean isStarredNow) {
        final String text = userPreferences.getInputText();
        final String direction = userPreferences.getTranslateDirection();

        dataManager.updateStarredWord(text, direction, isStarredNow);
    }

    private void updateLanguagePairInView(String direction) {
        final String[] tokens = direction.split("-");

        final String sourceLanguage = dataManager.getLanguageByCode(tokens[0]);
        final String destinationLanguage = dataManager.getLanguageByCode(tokens[1]);

        view.setLanguagePair(sourceLanguage, destinationLanguage);
    }
}
