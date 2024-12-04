package com.bmg.deliver.workflow.steps;

import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.model.product.Product;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepField;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;

public class XMLStep extends Step {
	private final ProductHelper productHelper;

	@StepField(key = AppConstants.DDEX_RELEASE_TYPE)
	private ReleaseType releaseType;

	@StepField(key = AppConstants.FILE_NAME)
	private String fileName;

	private final String templateCode;

	public XMLStep(StepParams params, ProductHelper productHelper, String templateCode) {
		super(params.getId(), params.getWorkflow(), params.getExecutionOrder(), params.getName(), params.getType(),
				params.getStepConfigurations());
		this.productHelper = productHelper;
		this.templateCode = templateCode;
	}

	@Override
	public StepResult run() {
		if (this.templateCode == null) {
			return new StepResult(false, "XML Runner Failed");
		}

		if (context.getTriggerData() instanceof Product) {
			Product product = (Product) this.context.getTriggerData();
			String barcode = product.getReleaseProduct().getBarcode();

			fileName = fileName == null ? product.getReleaseProduct().getBarcode() + ".xml" : fileName + ".xml";

			VelocityEngine velocityEngine = new VelocityEngine();
			Properties props = new Properties();
			props.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
			props.setProperty("string.resource.loader.class",
					"org.apache.velocity.runtime.resource.loader.StringResourceLoader");
			velocityEngine.init(props);

			// Create the Velocity context and add data
			VelocityContext templateContext = new VelocityContext();
			templateContext.put("releaseProduct", product.getReleaseProduct());
			templateContext.put("captureProduct", product.getCaptureProduct());
			templateContext.put("productDeals", product.getProductDeals());
			templateContext.put("trackDeals", product.getTrackDeals());

			ToolManager toolManager = new ToolManager(true, true);
			toolManager.configure("velocity-tools.xml");
			templateContext.put("date", new DateTool());
			templateContext.put("esc", new EscapeTool());
			templateContext.put("tools", toolManager.getToolboxFactory().createToolbox("application"));
			templateContext.put("artistRoles", AppConstants.getDdexArtistRoles());
			templateContext.put("ddexRecordingTypes", AppConstants.getDdexRecordingTypes());
			templateContext.put("directContributorRoles", AppConstants.getDdexDirectContributorRoles());
			templateContext.put("indirectContributorRoles", AppConstants.getDdexIndirectContributorRoles());

			// Merge the template and data into a string
			StringWriter writer = new StringWriter();
			try {
				velocityEngine.evaluate(templateContext, writer, "XMLTemplate", templateCode);
			} catch (Exception e) {
				logger.error("Exception while generating xml %s", e);
			}
			String remotePath = productHelper.baseFolderName(releaseType, barcode);
			getContext().addArtifacts(writer, fileName, remotePath + product.getReleaseProduct().getBarcode() + ".xml");
		}
		return new StepResult(true, "XML processed successfully");
	}
}
