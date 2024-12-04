package com.bmg.deliver.utils;

import lombok.Getter;

import java.util.*;

public class AppConstants {

	public static final String DEFAULT_VALUE = "0";
	public static final String PAUSED = "paused";
	public static final String XK = "XK";

	public static final String NONE = "NONE";
	public static final String MASTER_ROLE_ENV = "MASTER_ROLE";
	public static final String WORKER_ROLE_ENV = "WORKER_ROLE";
	public static final String MASTER_ROLE = "master";
	public static final String WORKER_ROLE = "worker";
	public static final String WORKFLOW_ID_NOT_FOUND = "Workflow not found with id ";
	public static final String WORKFLOW_ALIAS_NOT_FOUND = "Workflow not found with alias ";
	public static final String WORKFLOW_ALIAS_ALREADY_EXISTS = "Workflow alias already exists with ";
	public static final String EXECUTION_ORDER_EXIST = "Execution order already exist for the workflow ";
	public static final String ASSETS = "assets";
	public static final String TRANSCODES = "transcodes";
	public static final String CLIENT_REGISTRATION_ID = "okta";
	public static final String ROLE_OKTA_USER = "ROLE_OKTA_USER";
	public static final String ROLE_APP_CLIENT = "ROLE_APP_CLIENT";
	public static final String PRODUCT_ASSET_OPTION = "productAssetOption";
	public static final String TRACK_ASSET_OPTION = "trackAssetOption";
	public static final String ASSET = "asset";
	public static final String ASSET_ID = "assetId";
	public static final String INTERNAL_SERVER_ERROR = "Internal server error";
	public static final String TRANSCODE_DETAILS = "transcodeDetails";
	public static final String WAV = "wav";
	public static final String WORKFLOW_STEPS_NOT_FOUND = "Workflow steps not found for the workflow with ID ";
	public static final String ITEM_TYPE = "itemType";
	public static final String EXTERNAL_DOWNLOAD_URL = "externalDownloadUrl";
	public static final String WORKFLOW_INSTANCES_NOT_FOUND = "Workflow Instances not found for the workflow with ID ";
	public static final String WORKFLOW_INSTANCE_ARTIFACT_NOT_FOUND = "Workflow Instance artifact not found for the workflow with ID ";
	public static final String ID = "id";
	public static final String TRANSCODE_ID = "transcodeId";
	public static final String EXTERNAL_URL_VALIDITY = "externalUrlValidity";
	public static final String EXTERNAL_URL_CREATED_DATE = "externalUrlCreatedDate";
	public static final String WORKFLOW_INSTANCE_ID_NOT_FOUND = "Workflow Instance not found  with ID ";
	public static final String WORKFLOW_STEP_TEMPLATE_NOT_FOUND = "Workflow Step Template not found for the workflowStep with ID ";
	public static final String WORKFLOW_STEP_NOT_FOUND = "WorkflowStep  Not Found with ID ";
	public static final String ERROR_RETRIEVING_INSTANCES = "Unable to retrieve workflow instances with the status";
	public static final String DELETED_WORKFLOW_SUCCESS = "Workflow With Id  %d Deleted Successfully";
	public static final String DELETED_WORKFLOW_ERROR = "Cannot delete workflow because it is referenced by other entities.";
	public static final String DELETED_TEMPLATE_ERROR = "Cannot delete template because it is referenced by other entities.";
	public static final String AUDIO = "Audio";
	public static final String VIDEO = "Video";
	public static final String SOUND_RECORDING = "SoundRecording";
	public static final String SHORT_FORM_MUSICAL_WORK_VIDEO = "ShortFormMusicalWorkVideo";
	public static final String MUSICAL_WORK_SOUND_RECORDING = "MusicalWorkSoundRecording";
	public static final String VOCAL = "Vocal";
	public static final String VOCALS = "Vocals";
	public static final String COMPOSER = "Composer";
	public static final String ISNI = "ISNI";
	public static final String SOLO = "SOLO";
	public static final String VARIOUS_ARTISTS = "VARIOUS ARTISTS";
	public static final String BAND_NAME = "BandName";
	public static final String MULTIPLE_ARTIST = "MultipleArtist";
	public static final String UNKNOWN = "UNKNOWN";
	public static final String UNKNOWN_TITLE_CASE = "Unknown";
	public static final String CLEAN = "CLEAN";
	public static final String EXPLICIT = "Explicit";
	public static final String EXPLICIT_UPPER_CASE = "EXPLICIT";
	public static final String EXPLICIT_CONTENT_EDITED = "ExplicitContentEdited";
	public static final String NOT_EXPLICIT = "NotExplicit";
	public static final String TITLE = "title";
	public static final String STORE_ID = "storeId";
	public static final String RELEASE_PRODUCT = "releaseProduct";
	public static final String CAPTURE_PRODUCT = "captureProduct";
	public static final String PRODUCT_SUMMERY = "productSummary";
	public static final String LOW = "LOW";
	public static final String MINOR = "MINOR";
	public static final String HIGH = "HIGH";
	public static final String MEDIUM = "MEDIUM";
	public static final String PRIORITY = "priority";
	public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
	public static final String DATE_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final String DATE_FORMAT_COMPACT_TIMESTAMP = "yyyyMMddHHmmssSSS";
	public static final String DATE_FORMAT_FULL_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String DATE_FORMAT_ISO_LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String DATE_FORMAT_ISO_LOCAL_DATE_TIME_WITH_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final String DATE_FORMAT_COMPACT_DATE_TIME_WITH_ZONE = "yyyyMMdd'T'HHmmssX";
	public static final String BARCODE = "barcode";
	public static final String FORMAT = "format";
	public static final String ASC = "asc";
	public static final String STATUS = "status";
	public static final String DURATION = "duration";
	public static final String CREATED = "created";
	public static final String NAME = "name";
	public static final String ENABLED = "enabled";
	public static final String ERROR_CREATING_WORKFLOW_STEP_CONFIGURATION = "Error creating workflow step configuration";

	public static final String ERROR_DELETING_WORKFLOW_STEP_CONFIGURATION = "Error deleting workflow step configuration";

	public static final String ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION = "Error getting workflow step configuration";

	public static final String ERROR_CREATING_BOOKMARK = "Error creating bookmark: ";

	public static final String BOOKMARK_DELETED_SUCESSFULLY = "Bookmark deleted successfully.";

	public static final String BOOKMARK_NOT_FOUND = "Bookmark not found.";

	public static final String ERROR_DELETING_BOOKMARK = "Error deleting bookmark: ";

	public static final String ERROR_FETCHING_REMOTE_USER_BY_USERNAME = "Error fetching the remote user by username: ";

	public static final String ERROR_UPDATING_WORKFLOW_STEP_CONFIGURATION = "Error updating workflow step configuration";

	public static final String SFTP_USERNAME = "sftp_username";
	public static final String SFTP_PASSWORD = "sftp_password";
	public static final String SFTP_HOST = "sftp_host";
	public static final String SFTP_PORT = "sftp_port";
	public static final String SFTP_REMOTE_PATH = "sftp_remote_path";
	public static final String GCS_BUCKET_NAME = "gcs_bucket_name";
	public static final String GCS_SERVICE_ACCOUNT = "gcs_service_account";
	public static final String NAS_FILE_PATH = "nasFilePath";
	public static final String DDEX_RELEASE_TYPE = "releaseType";
	public static final String RESOURCES = "Resources";
	public static final String ATTACHMENT_DIR_PATH = "%s/%s.%s";
	public static final String RECIPIENT_NAME = "recipientName";
	public static final String ARTIST = "artist";
	public static final String ARTIST_NAME = "ArtistName";
	public static final String FEAT = "feat";
	public static final String WITH = "with";
	public static final String FORMAT_NAME = "formatName";
	public static final String RELEASE_DATE = "releaseDate";
	public static final String PROCESS_NAME = "processName";
	public static final String INSTANCE_ID = "instanceNumber";
	public static final String INSTANCE_URL = "instanceUrl";
	public static final String EMAIL_DELETED_SUCCESSFULLY = "Email Deleted Successfully";
	public static final String EMAIL_NOT_FOUND = "Email Not Found with Specific Id";
	public static final String EMAIL_SENT_SUCCESSFULLY = "Email sent successfully";
	public static final String NO_EMAILS_FOUND_FOR_WORKFLOW = "No emails found for workflow with id: ";
	public static final String ERROR_SENDING_EMAIL = "Error sending email: ";
	public static final String ERROR_DELETING_EMAIL = "Error in deleting Email with Specific Id";
	public static final String DOWNLOAD = "download";
	public static final String STREAM = "stream";
	public static final String DOWNLOAD_PREORDER = "downloadPreorder";
	public static final String DOWNLOAD_INSTANT_GRAT = "downloadInstantGrat";
	public static final String TRACK_DOWNLOAD = "trackDownload";
	public static final String TRACK_STREAM = "trackStream";
	public static final String CHECKSUM = "checksum";
	public static final String TEMPLATE_ID_NOT_FOUND = "Template Not Found with Specific Id";
	public static final String CHANNELS = "channels";
	public static final String AUDIO_CODEC = "audioCodec";
	public static final String SAMPLE_RATE = "sampleRate";
	public static final String SAMPLE_SIZE = "sampleSize";
	public static final String FILE_NAME = "fileName";
	public static final Long WAIT_UNTIL_LAST_INSTANCE_COMPLETE = 1L;
	public static final String DELIVERY_TYPE = "deliveryType";
	public static final String IS_DATA_ONLY_TRIGGER = "isDataOnlyTrigger";
	public static final String FILE_SIZE = "fileSize";
	public static final String ORIGINAL_FILE_NAME = "originalFileName";
	public static final String UPLOAD_FILE_NAME = "uploadFileName";
	public static final String UPLOAD_ID = "uploadId";
	public static final String CONTENT_TYPE = "contentType";
	public static final String COLOR_TYPE = "colorType";
	public static final String DIMENSION = "dimension";
	public static final String TRANSCODED_DATE = "transCodedDate";

	@Getter
	protected static final Set<String> allCountries = new HashSet<>(Arrays.asList("AD", "AE", "AF", "AG", "AI", "AL",
			"AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW", "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI",
			"BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG",
			"CH", "CI", "CK", "CL", "CM", "CN", "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM",
			"DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD",
			"GE", "GF", "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
			"HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM", "JO", "JP",
			"KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS",
			"LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK", "ML", "MM", "MN", "MO", "MP", "MQ",
			"MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP",
			"NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW", "PY",
			"QA", "RE", "RO", "RS", "RU", "RW", "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM",
			"SN", "SO", "SR", "SS", "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM",
			"TN", "TO", "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
			"VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"));

	@Getter
	protected static final List<String> ddexCompliantArtistRoles = new ArrayList<>(
			Arrays.asList("Actor", "Artist", "Adapter", "Arranger", "Author", "Band", "Choir", AppConstants.COMPOSER,
					"Conductor", "Director", "Dancer", "Ensemble", "FeaturedArtist", "Lyricist", "MainArtist",
					"Narrator", "Orchestra", "Producer", "Programmer", "Translator"));

	@Getter
	protected static final List<String> ddexArtistRoles = new ArrayList<>(Arrays.asList("Artist", "Adapter", "Arranger",
			"Author", "Band", "Choir", "Composer", "Conductor", "Dancer", "Director", "Ensemble", "FeaturedArtist",
			"Lyricist", "MainArtist", "Narrator", "Orchestra", "Producer", "Programmer", "Translator"));

	@Getter
	protected static final List<String> ddexDirectContributorRoles = new ArrayList<>(Arrays.asList("Actor", "Announcer",
			"Artist", "Band", "Choir", "Comedian", "Commentator", "Conductor", "Dancer", "Director", "DJ", "Editor",
			"Engineer", "Ensemble", "FeaturedArtist", "Interviewer", "MainArtist", "Mixer", "Narrator", "Orchestra",
			"Performer", "Presenter", "Producer", "Programmer", "Remixer", "Speaker"));

	@Getter
	protected static final List<String> ddexIndirectContributorRoles = new ArrayList<>(Arrays.asList("Adapter",
			"Arranger", "Author", "Composer", "Compiler", "Lyricist", "Reporter", "Translator"));

	@Getter
	protected static final List<String> ddexRecordingTypes = new ArrayList<>(Arrays.asList(AppConstants.AUDIO,
			"Audio Clip", AppConstants.AUDIO_VISUAL, "Audiovisual (Non Music)", "Menu Loop", "Non-Musical Asset",
			AppConstants.VIDEO_CLIP, "Audio (Dolby Atmos)", "Audio (Sony 360)", "Audiovisual (Music Video)",
			"Audiovisual (Lyric Video)", "Audiovisual (Packshot Video)", "Audiovisual (Visualiser)"));

	public static final String AUDIO_VISUAL = "Audiovisual";
	public static final String VIDEO_CLIP = "Video Clip";
	public static final String REPLACE_DOWNLOAD_LINK = "replaceWithDownloadLinks";
	public static final String INCLUDE_ALBUM_STREAMING_DEALS = "albumStreamingDeals";
	public static final String INCLUDE_KOSOVO = "includeKosovo";
	public static final String USE_DATE_TIME = "useDateTime";

	private AppConstants() {
		// Private constructor to prevent instantiation
		throw new AssertionError("Constants class cannot be instantiated");
	}
}
