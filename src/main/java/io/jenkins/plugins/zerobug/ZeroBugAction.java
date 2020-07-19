package io.jenkins.plugins.zerobug;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class ZeroBugAction implements RunAction2 {
	private transient Run run;
	
	private String token;
	private String buildId;
	private String srcIframe;
	
	private final static String URL_RESPONSE = "https://plugins.jenkins.io";

    public ZeroBugAction(String token, String buildId) {
    	this.token = token;
        this.buildId = buildId;
    }

	public String getToken() {
		return token;
	}

    public String getBuildId() {
		return buildId;
	}

	public String getSrcIframe() {
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

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run; 
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run; 
    }

    public Run getRun() { 
    	this.srcIframe = URL_RESPONSE + "?token=" + token + "&buildId=" + buildId;
        return run;
    }
}