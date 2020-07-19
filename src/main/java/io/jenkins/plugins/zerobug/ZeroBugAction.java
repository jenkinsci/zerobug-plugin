package io.jenkins.plugins.zerobug;

import hudson.model.AbstractBuild;
import hudson.model.Action;

public class ZeroBugAction implements Action {
//	private transient Run run;
	
	private String token;
	private String buildId;
	private String srcIframe;
	
	private final static String URL_RESPONSE = "https://plugins.jenkins.io";

	
	private AbstractBuild<?, ?> build;
	
	public AbstractBuild<?, ?> getBuild() {
        return build;
    }
	
    public ZeroBugAction(String token, String buildId, final AbstractBuild<?, ?> build) {
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

	public String getSrcIframe() {
		this.srcIframe = URL_RESPONSE + "?token=" + token + "&buildId=" + buildId;
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