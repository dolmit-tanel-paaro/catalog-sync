package ee.paaro.catalogsync;

import org.springframework.boot.SpringApplication;

public class TestCatalogSyncApplication {

	public static void main(String[] args) {
		SpringApplication.from(CatalogSyncApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
