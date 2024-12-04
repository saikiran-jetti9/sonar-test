package com.bmg.deliver.serviceimpl.email;

import com.bmg.deliver.utils.EmailContentBuilder;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailContentBuilderTest {

	@InjectMocks
	private EmailContentBuilder emailContentBuilder;

	private Gson gson = new Gson();

	@BeforeEach
	void setUp() {
	}

	// @Test
	// void testBuildEmailContent() throws IOException {
	// String recipientName = "test";
	// JsonObject jsonObject;
	// try (FileReader reader = new
	// FileReader("src/test/resources/ddexstep/product.json")) {
	// jsonObject = gson.fromJson(reader, JsonObject.class);
	// }
	// Long id = 1L;
	// String workflowName = "Test Workflow";
	// String status = "SUCCESS";
	// String emailContent = emailContentBuilder.buildEmailContent(recipientName,
	// jsonObject, id, workflowName,
	// status);
	// assertNotNull(emailContent);
	// assertTrue(emailContent.contains(recipientName));
	// }
}
