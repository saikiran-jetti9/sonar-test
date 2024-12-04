package com.bmg.deliver.workflow.steps;

import com.bmg.deliver.enums.ProductTranscodeOption;
import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.model.product.Product;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.utils.StoreHelper;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;
import com.bmg.deliver.workflow.step.StepParams;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DDEXStepTest {

	private DDEXStep ddexStep;

	@Mock
	private ProductHelper productHelper;

	@Mock
	private StoreHelper storeHelper;

	@Mock
	private Workflow workflow;

	@Mock
	private Logger logger;

	@Mock
	private ExecutionContext context;

	@BeforeEach
	void setUp() {
		List<WorkflowStepConfiguration> stepConfigurations = new ArrayList<>();
		WorkflowStepConfiguration config1 = new WorkflowStepConfiguration();
		config1.setKey(AppConstants.DDEX_RELEASE_TYPE);
		config1.setValue(String.valueOf(ReleaseType.RELEASE_BY_RELEASE));

		WorkflowStepConfiguration config2 = new WorkflowStepConfiguration();
		config2.setKey(AppConstants.PRODUCT_ASSET_OPTION);
		config2.setValue(ProductTranscodeOption.NONE.getAssetOption());

		WorkflowStepConfiguration config3 = new WorkflowStepConfiguration();
		config3.setKey(AppConstants.TRACK_ASSET_OPTION);
		config3.setValue(ProductTranscodeOption.NONE.getAssetOption());

		WorkflowStepConfiguration config4 = new WorkflowStepConfiguration();
		config4.setKey(AppConstants.INCLUDE_ALBUM_STREAMING_DEALS);
		config4.setValue("true");

		stepConfigurations.add(config1);
		stepConfigurations.add(config2);
		stepConfigurations.add(config3);
		stepConfigurations.add(config4);

		StepParams stepParams = new StepParams(1L, workflow, 1, "Test Step", WorkflowStepType.DDEX, stepConfigurations);
		String templateCode = "";
		ddexStep = new DDEXStep(stepParams, productHelper, storeHelper, templateCode);

		ReflectionTestUtils.setField(ddexStep, "logger", logger);
		ReflectionTestUtils.setField(ddexStep, "storeHelper", storeHelper);
		ReflectionTestUtils.setField(ddexStep, "context", context);
	}

	@Test
	void testCreateDeals_digitalProduct() throws FileNotFoundException {
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/ddexstep/product.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		ddexStep.createDeals(product);
	}

	@Test
	void testCreateDeals_packageProduct() throws FileNotFoundException {
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/ddexstep/productWithPackageIndicator.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		ddexStep.createDeals(product);
	}

	@Test
	void testCreateDeals_digitalProductWithInstantGrat() throws FileNotFoundException {
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/ddexstep/productWithInstantGrat.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		ddexStep.createDeals(product);
	}

	@Test
	void testCreateDeals_digitalProductWithIncludeAlbumStreamingDealsFalse() throws FileNotFoundException {
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/ddexstep/product.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		ReflectionTestUtils.setField(ddexStep, "includeAlbumStreamingDeals", false);
		ddexStep.createDeals(product);
	}

	@Test
	void testHasValidPriceCode_withValidPriceCode() throws FileNotFoundException {
		Gson gson = new Gson();
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/products/product.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		boolean hasValidPriceCode = ddexStep.hasValidPriceCode(product.getReleaseProduct());
		assertTrue(hasValidPriceCode);
	}

	@Test
	void testHasValidPriceCode_permanentDownloadFalse() throws FileNotFoundException {
		JsonReader fis = new JsonReader(new FileReader("src/test/resources/products/product.json"));
		Product product = new Gson().fromJson(fis, Product.class);
		boolean hasValidPriceCode = ddexStep.hasValidPriceCode(product.getReleaseProduct());
		assertTrue(hasValidPriceCode);
	}
}
