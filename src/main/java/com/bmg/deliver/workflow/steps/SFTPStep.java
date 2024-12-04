package com.bmg.deliver.workflow.steps;

import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepField;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Getter
@Setter
public class SFTPStep extends Step {
	@StepField(key = AppConstants.SFTP_HOST)
	private String host;

	@StepField(key = AppConstants.SFTP_USERNAME)
	private String username;

	@StepField(key = AppConstants.SFTP_PASSWORD)
	private String password;

	@StepField(key = AppConstants.SFTP_REMOTE_PATH)
	private String remotePath;

	@StepField(key = AppConstants.SFTP_PORT)
	private Integer port;

	public SFTPStep(StepParams stepParams) {
		super(stepParams.getId(), stepParams.getWorkflow(), stepParams.getExecutionOrder(), stepParams.getName(),
				stepParams.getType(), stepParams.getStepConfigurations());
	}

	@Override
	public StepResult run() throws JSchException {
		if (host == null || username == null || password == null || port == null) {
			logger.info("SFTP connection parameters are missing.");
			return new StepResult(false, "SFTP connection parameters are missing.");
		}
		ChannelSftp channelSftp = setupJsch();

		if (null != channelSftp) {
			channelSftp.connect();
			long totalSize = 0;

			for (Map.Entry<String, String> entry : context.getFilesToUpload().entrySet()) {
				String remoteFilePath = entry.getKey();
				String localFilePath = entry.getValue();
				context.getLogger().info("Uploading %s to %s with size %s", localFilePath, remoteFilePath,
						readableByteCount(new File(localFilePath).length()));

				String remoteDirPath = remoteFilePath;
				if (remoteFilePath.contains("/")) {
					remoteDirPath = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
				}

				// **Create Remote Directories If They Don't Exist**
				if (!createRemoteDirectories(channelSftp, remoteDirPath)) {
					logger.info("Failed to create remote directories for: %s", remoteDirPath);
					continue; // Skip uploading this file
				}

				// **Upload the File**
				try (FileInputStream fis = new FileInputStream(localFilePath)) {
					totalSize += new File(localFilePath).length();
					channelSftp.put(fis, remoteFilePath);
					logger.info("Successfully uploaded: %s", localFilePath);
				} catch (IOException | SftpException ex) {
					logger.error("Error uploading file: %s %s", localFilePath, ExceptionUtils.getStackTrace(ex));
				}
			}
			logger.info("Uploaded all files and Total size of files uploaded: %s", readableByteCount(totalSize));
			channelSftp.exit();
		}

		return new StepResult(true, "SFTP successfully");
	}

	public ChannelSftp setupJsch() {
		try {
			JSch jsch = new JSch();
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch.setConfig(config);
			Session jschSession = jsch.getSession(username, host);
			jschSession.setPassword(password);
			jschSession.connect();
			return (ChannelSftp) jschSession.openChannel("sftp");
		} catch (JSchException e) {
			logger.error("Error in setup jsch {}", ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	private boolean createRemoteDirectories(ChannelSftp channelSftp, String remoteDirPath) {
		try {
			String[] folders = remoteDirPath.split("/");
			StringBuilder path = new StringBuilder();

			// Handle absolute paths
			if (remoteDirPath.startsWith("/")) {
				path.append("/");
			}

			for (String folder : folders) {
				if (folder.isEmpty()) {
					continue; // Skip empty parts}
				}
				if (path.length() > 1 && !path.toString().endsWith("/")) {
					path.append("/");
				}
				path.append(folder);
				String currentPath = path.toString();
				try {
					channelSftp.stat(currentPath);
					// Directory exists
				} catch (SftpException e) {
					// Directory does not exist, attempt to create it
					try {
						channelSftp.mkdir(currentPath);
					} catch (SftpException ex) {
						logger.error("Failed to create directory: %s %s", currentPath, ExceptionUtils.getStackTrace(e));
						return false;
					}
				}
			}
			return true;
		} catch (Exception e) {
			logger.error("Error creating remote directories: %s %s", remoteDirPath, ExceptionUtils.getStackTrace(e));
			return false;
		}
	}

	private String readableByteCount(long bytes) {
		long maxSize = 1024L;
		if (bytes < maxSize) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String pre = String.valueOf("KMGTPE".charAt(exp - 1));
		return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
	}
}
