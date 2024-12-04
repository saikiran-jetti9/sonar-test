package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.List;

@Data
public class RecordingSong {
	private long recordingSongId;
	private long recordingSongSeqNo;
	private Song song;
	private List<SongContributor> songContributors;

	@Data
	public static class Song {
		private long songid;
		private Site site;
		private String iswc;
		private String royaltycompliance;
		private String entitystatus;
		private String lockedstatus;
		private String businessunit;
		private String title;
		private String composername;
		private String publishername;
		private String imaestrocode;
		private boolean ispublicdomain;
	}

	@Data
	public static class Site {
		private String name;
		private String code;
		private String country;
	}

	@Data
	public static class SongContributor {
		private long songContributorId;
		private long songContribitutorSeqNo;
		private Contributor contributor;
		private List<Contributor.ArtistRole> roles;
		private boolean isTraditional;
	}
}
