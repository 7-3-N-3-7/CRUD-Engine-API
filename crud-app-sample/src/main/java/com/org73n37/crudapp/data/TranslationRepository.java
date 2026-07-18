package com.org73n37.crudapp.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRepository extends MongoRepository<Translation, String> {
    java.util.List<Translation> findByLang(String lang);
}
