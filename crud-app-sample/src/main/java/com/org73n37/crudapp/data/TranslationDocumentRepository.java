package com.org73n37.crudapp.data;

import com.org73n37.crudapp.domain.TranslationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranslationDocumentRepository extends MongoRepository<TranslationDocument, String> {
    Optional<TranslationDocument> findBySlugAndLanguage(String slug, String language);
}
