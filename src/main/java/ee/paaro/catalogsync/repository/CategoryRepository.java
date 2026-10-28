package ee.paaro.catalogsync.repository;

import ee.paaro.catalogsync.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByExternalId(String externalId);

    List<Category> findAllByPlaceholderTrueOrderByCreatedAtAsc();
}
