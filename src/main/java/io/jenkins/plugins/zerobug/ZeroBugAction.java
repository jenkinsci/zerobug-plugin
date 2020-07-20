package io.jenkins.plugins.zerobug;

import java.io.IOException;

import hudson.model.Action;
import hudson.model.Run;
import hudson.util.Secret;
import io.jenkins.plugins.zerobug.commons.Property;

public class ZeroBugAction implements Action {
	private Secret token;
	private String buildId;
	private String srcIframe;
	private Run<?, ?> run;

	public Run<?, ?> getRun() {
		return run;
	}

	public ZeroBugAction(final Secret token, final String buildId, final Run<?, ?> run) {
		this.token = token;
		this.buildId = buildId;
		this.run = run;
	}

	public Secret getToken() {
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
