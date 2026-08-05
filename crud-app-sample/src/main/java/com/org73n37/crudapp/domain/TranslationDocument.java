package com.org73n37.crudapp.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "translations")
@CompoundIndex(def = "{'slug': 1, 'language': 1}", unique = true)
public class TranslationDocument {

    @Id
    private String id;

    private String slug;

    private String language;

    private Map<String, Object> translations;

    public TranslationDocument() {
    }

    public TranslationDocument(String slug, String language, Map<String, Object> translations) {
        this.slug = slug;
        this.language = language;
        this.translations = translations;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Object> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, Object> translations) {
        this.translations = translations;
    }
}
