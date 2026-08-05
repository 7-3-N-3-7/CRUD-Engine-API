package com.org73n37.crudapp.data;

import com.org73n37.crudapp.domain.IngestedHtml;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestedHtmlRepository extends JpaRepository<IngestedHtml, String> {
}
