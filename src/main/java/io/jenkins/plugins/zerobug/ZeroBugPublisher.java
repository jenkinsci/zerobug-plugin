package io.jenkins.plugins.zerobug;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.zerobug.commons.Property;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ZeroBugPublisher extends Recorder implements SimpleBuildStep {

	private final Secret token;
	private final String webSite;
	private final boolean onlyBuildSuccess;

	@DataBoundConstructor
	public ZeroBugPublisher(final Secret token, final String webSite, final boolean onlyBuildSuccess) {
		this.token = token;
		this.webSite = webSite;
		this.onlyBuildSuccess = onlyBuildSuccess;
	}

	public Secret getToken() {
		return token;
	}

	public String getWebSite() {
		return webSite;
	}

	public boolean isOnlyBuildSuccess() {
		return onlyBuildSuccess;
	}

	@Symbol("ZeroBugPublisher")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private Secret token;
		private String webSite;
		private boolean onlyBuildSuccess;
		
		public DescriptorImpl() {
			super(ZeroBugPublisher.class);
			load();
		}

		public Secret getToken() {
			return token;
		}

		public void setToken(Secret token) {
			this.token = token;
		}
		
		public String getWebSite() {
			return webSite;
		}

		public void setWebSite(String webSite) {
			this.webSite = webSite;
		}

		public boolean isOnlyBuildSuccess() {
			return onlyBuildSuccess;
		}

		public void setOnlyBuildSuccess(boolean onlyBuildSuccess) {
			this.onlyBuildSuccess = onlyBuildSuccess;
		}

		public ListBoxModel doFillWebSiteItems() throws IOException {
			ListBoxModel items = new ListBoxModel();
			CloseableHttpClient httpClient = null;
			CloseableHttpResponse response = null;
			try {
				httpClient = HttpClients.createDefault();
				HttpGet request = new HttpGet(Property.getByKey("url.get.list.site"));
				response = httpClient.execute(request);
				
				HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                String result = EntityUtils.toString(entity);
	            }

				items.add("http://www.google.com", "1");
				items.add("http://www.globo.com", "2");
				items.add("http://www.jenkins.com", "3");
				items.add("http://www.java.com", "4");

			} finally {
				response.close();
				httpClient.close();
			}

			return items;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			req.bindParameters(this);
			this.token = Secret.fromString(formData.getString("token"));
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckToken(@QueryParameter final String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_errors_missingToken());
			}
			return FormValidation.ok();
		}

		@POST
		@SuppressWarnings("unused")
		public FormValidation doValidateConnection(@QueryParameter final String token) throws IOException {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			return validateConnection(token);
		}

		private FormValidation validateConnection(final String token) throws IOException {
			CloseableHttpClient httpClient = null;
			CloseableHttpResponse response = null;
			try {
				httpClient = HttpClients.createDefault();
				HttpGet request = new HttpGet(Property.getByKey("url.get.list.site"));
				response = httpClient.execute(request);

				if (response.getStatusLine().getStatusCode() == 200) {
					return FormValidation.ok(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Success());
				} else {
					return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Reject()
							+ response.getStatusLine().getStatusCode());
				}
			} catch (Exception e) {
				return FormValidation
						.error(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Error() + e.toString());
			} finally {
				response.close();
				httpClient.close();
			}
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

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {

		if ((onlyBuildSuccess && Result.SUCCESS == run.getResult()) || !onlyBuildSuccess) {
			CloseableHttpClient httpClient = null;
			try {
				httpClient = HttpClients.createDefault();
				HttpGet request = new HttpGet(Property.getByKey("url.request"));
				httpClient.execute(request);
				run.addAction(new ZeroBugAction(token, webSite, run.getUrl(), run));
			} finally {
				httpClient.close();
			}
		}
	}

}
