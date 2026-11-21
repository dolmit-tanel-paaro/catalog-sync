package ee.paaro.catalogsync.repository;

import ee.paaro.catalogsync.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExternalId(String externalId);

    List<Product> findAllByCategoryId(Long categoryId);

    /**
     * The back office listing needs a category id and an external id, not products.
     * Selecting the two columns keeps the whole page to one query and builds no
     * entities, which matters because a placeholder can collect a lot of products
     * before anyone gets round to moving them.
     */
    @Query("""
            select p.category.id as categoryId, p.externalId as externalId
            from Product p
            where p.category.id in :categoryIds
            """)
    List<CategoryProduct> findExternalIdsByCategoryIdIn(@Param("categoryIds") Collection<Long> categoryIds);

    interface CategoryProduct {

        Long getCategoryId();

        String getExternalId();
    }
}
