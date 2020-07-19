package io.jenkins.plugins.zerobug;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

public class ZeroBugPublisher extends Recorder {

    private final String token;
    private final boolean onlyBuildSuccess;
    private final static String URL_REQUEST = "https://api.telegram.org/bot1371039721:AAHU4WBOdFPaQ3jXunlNyy6TAdVL6UWyavA/getUpdates";

    @DataBoundConstructor
    public ZeroBugPublisher(String token, boolean onlyBuildSuccess) {
        this.token = token;
        this.onlyBuildSuccess = onlyBuildSuccess;
    }

	public String getToken() {
		return token;
	}
	
	public boolean isOnlyBuildSuccess() {
		return onlyBuildSuccess;
	}

	private String callServiceRest() {
		StringBuilder sb = null;
		
		try {
			URL urlConn = new URL(URL_REQUEST);
	        URLConnection conn;
			conn = urlConn.openConnection();
			InputStream is = new BufferedInputStream(conn.getInputStream());
	        
	        InputStreamReader inputStreamReader = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(inputStreamReader);
	        String inputLine = "";
	        sb = new StringBuilder();
	        while ((inputLine = br.readLine()) != null) {
	            sb.append(inputLine);
	        }
	        inputStreamReader.close();
	        is.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
                
        return sb.toString();
	}

	@Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener){
		if((onlyBuildSuccess && Result.SUCCESS == build.getResult()) || !onlyBuildSuccess) {
			String response = callServiceRest();
			build.addAction(new ZeroBugAction(token, build.getUrl(), build)); 
	    	listener.getLogger().println("URL Request: " + URL_REQUEST);
	    	listener.getLogger().println("Token: " + token);
	    	listener.getLogger().println("Build: " + build.getUrl());
	    	
	    	listener.getLogger().println("Response: " + response);			
	    	
	    	return true;
		}
    	
    	return false;
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckToken(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_errors_missingToken());
            
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.ZeroBugPublisher_DescriptorImpl_DisplayName();
        }

    }

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

}
