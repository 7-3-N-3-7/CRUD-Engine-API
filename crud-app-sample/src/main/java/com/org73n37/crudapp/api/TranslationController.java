package com.org73n37.crudapp.api;

import com.org73n37.crudapp.data.Translation;
import com.org73n37.crudapp.data.TranslationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/translations")
@CrossOrigin(origins = "*")
public class TranslationController {

    @Autowired
    private TranslationRepository repository;

    @GetMapping
    public Flux<Translation> getAll() {
        return Flux.fromIterable(repository.findAll());
    }
}
