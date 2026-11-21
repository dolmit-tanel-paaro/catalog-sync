package ee.paaro.catalogsync.web;

import ee.paaro.catalogsync.domain.Category;
import ee.paaro.catalogsync.repository.CategoryRepository;
import ee.paaro.catalogsync.repository.ProductRepository;
import ee.paaro.catalogsync.service.CatalogueImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the back office needs: which categories are placeholders, what is sitting
 * under them, and a way to move a product somewhere real by hand.
 */
@RestController
@RequestMapping("/api/placeholders")
public class PlaceholderController {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CatalogueImportService importService;

    public PlaceholderController(CategoryRepository categories,
                                 ProductRepository products,
                                 CatalogueImportService importService) {
        this.categories = categories;
        this.products = products;
        this.importService = importService;
    }

    public record PlaceholderView(String externalId, String name, Instant createdAt, List<String> productExternalIds) {
    }

    public record MoveRequest(@NotBlank String productExternalId, @NotBlank String categoryExternalId) {
    }

    @GetMapping
    public List<PlaceholderView> list() {
        List<Category> placeholders = categories.findAllByPlaceholderTrueOrderByCreatedAtAscIdAsc();
        if (placeholders.isEmpty()) {
            return List.of();
        }
        List<Long> ids = placeholders.stream().map(Category::getId).toList();
        Map<Long, List<String>> byCategory = products.findExternalIdsByCategoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        ProductRepository.CategoryProduct::getCategoryId,
                        Collectors.mapping(ProductRepository.CategoryProduct::getExternalId, Collectors.toList())));
        return placeholders.stream()
                .map(category -> new PlaceholderView(
                        category.getExternalId(),
                        category.getName(),
                        category.getCreatedAt(),
                        byCategory.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    @PostMapping("/move")
    public ResponseEntity<Void> move(@RequestBody MoveRequest request) {
        importService.moveProduct(request.productExternalId(), request.categoryExternalId());
        return ResponseEntity.noContent().build();
    }
}
