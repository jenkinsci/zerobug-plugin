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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

public class ZeroBugBuilder extends Builder implements SimpleBuildStep {

    private final String token;
    private final static String URL_REQUEST = "https://api.telegram.org/bot1371039721:AAHU4WBOdFPaQ3jXunlNyy6TAdVL6UWyavA/getUpdates";

    @DataBoundConstructor
    public ZeroBugBuilder(String token) {
        this.token = token;
    }

	public String getToken() {
		return token;
	}
	
	
	private String callServiceRest() throws IOException {
		URL urlConn = new URL(URL_REQUEST);
        URLConnection conn = urlConn.openConnection();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        StringBuilder sb = new StringBuilder();
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        
        return sb.toString();
	}

	@Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		String response = callServiceRest();
    	run.addAction(new ZeroBugAction(token, run.getDisplayName(), run.getId())); 
    	listener.getLogger().println("URL Request: " + URL_REQUEST);
    	listener.getLogger().println("Token: " + token);
    	listener.getLogger().println("Project Name: " + run.getId());
    	listener.getLogger().println("Build Number: " + run.getId());
    	
    	listener.getLogger().println("Response: " + response);
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }

}
