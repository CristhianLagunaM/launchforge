package com.launchforge.catalog.infrastructure;

import com.launchforge.persistence.model.catalog.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrueOrderByNameAsc();
}
