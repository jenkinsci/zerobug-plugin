package io.jenkins.plugins.zerobug;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.servlet.ServletException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import io.jenkins.plugins.zerobug.model.ResponseListUrl;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ZeroBugPublisher extends Recorder implements SimpleBuildStep {

	private final static String INVALID_TOKEN = "no";

	private Secret token;
	private final String webSite;
	private final boolean onlyBuildSuccess;

	@DataBoundConstructor
	public ZeroBugPublisher(final String webSite, final boolean onlyBuildSuccess) {
		this.token = getToken();
		this.webSite = webSite;
		this.onlyBuildSuccess = onlyBuildSuccess;
	}

	public Secret getToken() {
		if (token == null) {
			token = getDescriptor().getToken();
		}
		return token;
	}

	public String getWebSite() {
		return webSite;
	}

	public boolean isOnlyBuildSuccess() {
		return onlyBuildSuccess;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	private String generateBuildId(String token, String webSite) {
		String password = token + webSite + LocalDateTime.now();
		return DigestUtils.md5Hex(password).toUpperCase();
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {

		if (StringUtils.isBlank(Secret.toString(token))) {
			listener.getLogger().println(Messages.ZeroBugPublisher_DescriptorImpl_errors_missingToken());
			run.setResult(Result.FAILURE);
		}

		if (StringUtils.isBlank(webSite)) {
			listener.getLogger().println(Messages.ZeroBugPublisher_DescriptorImpl_errors_missingWebsite());
			run.setResult(Result.FAILURE);
		}

		if ((onlyBuildSuccess && Result.SUCCESS == run.getResult()) || !onlyBuildSuccess) {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			try {
				String buildId = generateBuildId(Secret.toString(token), webSite);

				listener.getLogger().println("INFO: Web Site ID - " + webSite);
				listener.getLogger().println("INFO: Build ID - " + buildId);
				listener.getLogger().println("INFO: Only Build Success - " + onlyBuildSuccess);

				HttpPost httpPost = new HttpPost(Property.getByKey("url.request.build"));
				StringEntity entity = new StringEntity("token=" + token + "&id=" + buildId + "&id_target=" + webSite);
				httpPost.setEntity(entity);
				httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
				CloseableHttpResponse response = httpClient.execute(httpPost);

				if (response.getStatusLine().getStatusCode() == 200) {
					String result = EntityUtils.toString(response.getEntity());
					if (INVALID_TOKEN.equalsIgnoreCase(result)) {
						listener.getLogger().println("ERROR: " + Messages.ZeroBugPublisher_DescriptorImpl_errors_invalidToken());
						run.setResult(Result.FAILURE);
					} else {
						run.addAction(new ZeroBugAction(token, webSite, buildId, run));
					}
				} else {
					listener.getLogger().println("ERROR: " + Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Error());
					run.setResult(Result.FAILURE);
				}
			} finally {
				httpClient.close();
			}
		}
	}

	@Symbol({"zeroBug", "ZeroBugPublisher"}) // Prefer zeroBug, accept ZeroBugPublisher as keyword
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private Secret token;
		private String webSite;

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

		public ListBoxModel doFillWebSiteItems() throws IOException {
			ListBoxModel items = new ListBoxModel();
			if (!StringUtils.isBlank(Secret.toString(this.token))) {
				CloseableHttpClient httpClient = HttpClients.createDefault();
				try {
					HttpPost httpPost = new HttpPost(Property.getByKey("url.get.list.site"));
					StringEntity stringEntity = new StringEntity("token=" + token);
					httpPost.setEntity(stringEntity);
					httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

					CloseableHttpResponse response = httpClient.execute(httpPost);

					if (response.getStatusLine().getStatusCode() == 200) {
						String result = EntityUtils.toString(response.getEntity());
						if (!INVALID_TOKEN.equalsIgnoreCase(result)) {
							ObjectMapper objectMapper = new ObjectMapper();
							ResponseListUrl responseListUrl = objectMapper.readValue(result, ResponseListUrl.class);
							responseListUrl.getResultado().stream().forEach(resultado -> {
								items.add(resultado.getUrl(), resultado.getId());
							});
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					httpClient.close();
				}
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
			if (StringUtils.isBlank(token)) {
				return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Error());
			}

			return validateConnection(token);
		}

		private FormValidation validateConnection(final String token) throws IOException {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			try {
				HttpPost httpPost = new HttpPost(Property.getByKey("url.valid.token"));
				StringEntity entity = new StringEntity("token=" + token);
				httpPost.setEntity(entity);
				httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
				CloseableHttpResponse response = httpClient.execute(httpPost);

				if (response.getStatusLine().getStatusCode() == 200) {
					String result = EntityUtils.toString(response.getEntity());
					if (!INVALID_TOKEN.equalsIgnoreCase(result)) {
						return FormValidation.ok(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Success());
					} else {
						return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_errors_invalidToken());
					}
				} else {
					return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Reject() + " "
							+ response.getStatusLine().getStatusCode());
				}
			} catch (Exception e) {
				return FormValidation
						.error(Messages.ZeroBugPublisher_DescriptorImpl_Validate_Connect_Error() + " " + e.toString());
			} finally {
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

}
