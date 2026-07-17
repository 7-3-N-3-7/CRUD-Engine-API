package com.org73n37.crudapp.data;

import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * [DATA LAYER]
 * Database representation of a Translation.
 */
@Document(collection = "translations")
@CrudResource(path = "translations", roles = {"ANYONE"})
public class Translation {
    
    @Id
    private String id;
    private String key;
    private String value;
    private String lang;

    public Translation() {}

    public Translation(String key, String value, String lang) {
        this.key = key;
        this.value = value;
        this.lang = lang;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
}
