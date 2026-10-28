package ee.paaro.catalogsync.web;

import ee.paaro.catalogsync.domain.Category;
import ee.paaro.catalogsync.domain.Product;
import ee.paaro.catalogsync.repository.CategoryRepository;
import ee.paaro.catalogsync.repository.ProductRepository;
import ee.paaro.catalogsync.service.CatalogueImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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
        return categories.findAllByPlaceholderTrueOrderByCreatedAtAsc().stream()
                .map(this::toView)
                .toList();
    }

    @PostMapping("/move")
    public ResponseEntity<Void> move(@RequestBody MoveRequest request) {
        importService.moveProduct(request.productExternalId(), request.categoryExternalId());
        return ResponseEntity.noContent().build();
    }

    private PlaceholderView toView(Category category) {
        List<String> ids = products.findAllByCategoryId(category.getId()).stream()
                .map(Product::getExternalId)
                .toList();
        return new PlaceholderView(category.getExternalId(), category.getName(), category.getCreatedAt(), ids);
    }
}
