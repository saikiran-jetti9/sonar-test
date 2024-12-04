package com.bmg.deliver.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.serviceimpl.worker.StoreService;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;
import com.google.gson.*;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductHelperTest {
	@Mock
	private StoreService storeService;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private StoreHelper storeHelper;

	@Mock
	private Logger logger;

	@InjectMocks

	ProductHelper productHelper;

	private JsonObject releaseProduct;

	Gson gson = new Gson();

	JsonObject componentProduct;
	JsonObject digitalProduct;
	JsonObject digitalProductStoreResponse;
	JsonObject packageProductStoreResponse;

	@BeforeEach
	void setUp() throws IOException {
		String componentProductPath = "src/test/resources/products/packageReleaseProduct.json";
		String releaseProductPath = "src/test/resources/products/digitalReleaseProduct.json";
		String digitalProductStoreResponsePath = "src/test/resources/products/digitalProductStoreResponse.json";
		String packageProductStoreResponsePath = "src/test/resources/products/packageProductStoreResponse.json";

		FileReader fileReader = new FileReader(releaseProductPath);
		digitalProduct = gson.fromJson(fileReader, JsonObject.class);

		fileReader = new FileReader(componentProductPath);
		componentProduct = gson.fromJson(fileReader, JsonObject.class);

		fileReader = new FileReader(digitalProductStoreResponsePath);
		digitalProductStoreResponse = gson.fromJson(fileReader, JsonObject.class);

		fileReader = new FileReader(packageProductStoreResponsePath);
		packageProductStoreResponse = gson.fromJson(fileReader, JsonObject.class);
	}

	@Test
	void testBaseFolderName_withReleaseByReleaseReleaseType() {
		String barcode = "4099964062991";
		LocalDateTime mockNow = LocalDateTime.of(2024, 8, 17, 15, 30, 45, 123000000);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		String expectedTimestamp = mockNow.format(formatter);

		try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
			mockedStatic.when(LocalDateTime::now).thenReturn(mockNow);
			String baseFolderName = productHelper.baseFolderName(ReleaseType.RELEASE_BY_RELEASE, barcode);
			assertEquals(barcode + "_" + expectedTimestamp, baseFolderName);
		}
	}

	@Test
	void testBaseFolderName_withBatchedReleaseType() {
		String barcode = "4099964062991";
		LocalDateTime mockNow = LocalDateTime.of(2024, 8, 17, 15, 30, 45, 123000000);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		String expectedTimestamp = mockNow.format(formatter);

		try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
			mockedStatic.when(LocalDateTime::now).thenReturn(mockNow);
			String baseFolderName = productHelper.baseFolderName(ReleaseType.BATCHED, barcode);
			assertEquals(expectedTimestamp + "/" + barcode, baseFolderName);
		}
	}

	@Test
	void testBaseFolderName_withStandardReleaseType() {
		String barcode = "4099964062991";
		String baseFolderName = productHelper.baseFolderName(ReleaseType.STANDARD, barcode);
		assertEquals(barcode, baseFolderName);
	}
}
