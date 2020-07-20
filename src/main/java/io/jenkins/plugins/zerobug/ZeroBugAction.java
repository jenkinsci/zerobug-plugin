package io.jenkins.plugins.zerobug;

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import io.jenkins.plugins.zerobug.commons.Property;

public class ZeroBugAction implements Action {
	/**
	 * token variable.
	 */
	private String token;
	/**
	 * buildId variable.
	 */
	private String buildId;
	/**
	 * srcIframe variable.
	 */
	private String srcIframe;
	private AbstractBuild<?, ?> build;

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	public ZeroBugAction(final String token, final String buildId, final AbstractBuild<?, ?> build) {
		this.token = token;
		this.buildId = buildId;
		this.build = build;
	}

	public String getToken() {
		return token;
	}

	public String getBuildId() {
		return buildId;
	}

	public String getSrcIframe() throws IOException {
		this.srcIframe = Property.getByKey("url.response");
		return srcIframe;
	}

	@Override
	public String getIconFileName() {
		return "monitor.png";
	}

	@Override
	public String getDisplayName() {
		return "zerobug";
	}

	@Override
	public String getUrlName() {
		return "zerobug";
	}

}
