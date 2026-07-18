package com.sibi.aem.one.core.workflows;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Value;
import java.util.Collections;
import java.util.Map;

/**
 * Workflow Process Step that sends an EMAIL NOTIFICATION to the workflow
 * initiator once the product page has been published (or failed to publish).
 *
 * <h2>Real-World Scenario</h2>
 * <p>This is the final step in the product approval workflow, running
 * immediately after {@link ProductPublishProcess}. It reads the
 * {@code publishStatus} written by the previous step and emails the
 * person who originally started the workflow, telling them whether their
 * product page is now live or if publishing failed and why.</p>
 *
 * <h2>Technical Intricacies Demonstrated</h2>
 * <ul>
 *   <li><strong>Reading the workflow initiator</strong> — {@code WorkItem.getWorkflow()
 *       .getInitiator()} gives the user ID who started the workflow, which is
 *       then resolved to an email address via the JCR user profile.</li>
 *   <li><strong>Cross-step data flow</strong> — reading {@code publishStatus} /
 *       {@code publishError} written into {@code WorkflowData} metadata by the
 *       previous step. This is the standard way steps communicate.</li>
 *   <li><strong>Sling Commons Mail Service</strong> — using AEM's built-in
 *       {@code MailService} (configured via the Felix Console SMTP settings)
 *       rather than hand-rolling JavaMail configuration.</li>
 *   <li><strong>Non-fatal failure handling</strong> — a notification failure
 *       (e.g. SMTP misconfigured) should NOT fail the whole workflow, since the
 *       page has already been published successfully. We log and swallow the
 *       exception rather than throwing {@link WorkflowException}.</li>
 * </ul>
 */
@Component(service = WorkflowProcess.class, immediate = true,
        property = {"process.label=Notify Product Owner"})
public class ApprovalNotificationProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalNotificationProcess.class);

    private static final String SERVICE_USER = "approval-notification-service";

    /**
     * AEM's built-in mail service, backed by the SMTP configuration set in
     * {@code /system/console/configMgr} → "Day CQ Mail Service".
     */
    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args)
            throws WorkflowException {

        MetaDataMap wfData = workItem.getWorkflowData().getMetaDataMap();

        // Read the outcome written by the previous step (ProductPublishProcess)
        String publishStatus = wfData.get("publishStatus", "UNKNOWN");
        String publishedPath = wfData.get("publishedPath", workItem.getWorkflowData().getPayload().toString());
        String publishError = wfData.get("publishError", "");

        // Resolve who initiated the workflow — this is who gets notified
        String initiator = workItem.getWorkflow().getInitiator();
        String recipientEmail = resolveUserEmail(initiator);
        if (StringUtils.isBlank(recipientEmail)) {
            LOG.error("Could not resolve email for initiator {}, skipping notification", initiator);
            return;
        }

        String subject;
        String body;

        if ("SUCCESS".equals(publishStatus)) {
            subject = "Product page published: " + publishedPath;
            body = "Good news — your product page has been approved and is now live:\n\n"
                    + publishedPath
                    + "\n\nNo further action is required.";
        } else {
            subject = "Product page publishing FAILED: " + publishedPath;
            body = "Your product page was approved, but automatic publishing failed.\n\n"
                    + "Path: " + publishedPath + "\n"
                    + "Reason: " + publishError + "\n\n"
                    + "Please contact the AEM support team or retry publishing manually.";
        }

        sendNotification(recipientEmail, subject, body);
    }

    /**
     * Sends the notification email. Failures here are logged but deliberately
     * NOT propagated as a {@link WorkflowException} — a failed email should
     * never cause the workflow instance to show as "failed" when the actual
     * business outcome (publishing) already succeeded or was already recorded.
     */
    private void sendNotification(String recipientEmail, String subject, String body) {
        HtmlEmail email = new HtmlEmail();
        try {
            email.setSubject(subject);
            email.setHtmlMsg(body);
            email.addTo(recipientEmail);

            MessageGateway<HtmlEmail> gateway = messageGatewayService.getGateway(HtmlEmail.class);
            gateway.send(email);
        } catch (EmailException e) {
            LOG.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves a JCR user ID to an email address.
     * Stubbed here for brevity — in production this adapts a service-user
     * ResourceResolver to {@code UserManager}, looks up the {@code User} by ID,
     * and reads the {@code profile/email} property.
     */
    private String resolveUserEmail(String userId) {
        if (userId == null) {
            return null;
        }
        try (ResourceResolver resolver = getServiceResolver()) { // add a service-user resolver, subservice e.g. "user-lookup-service"
            UserManager userManager = resolver.adaptTo(UserManager.class);
            Authorizable authorizable = userManager.getAuthorizable(userId);
            if (authorizable != null) {
                Value[] emailValues = authorizable.getProperty("profile/email");
                if (emailValues != null && emailValues.length > 0) {
                    return emailValues[0].getString();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to resolve email for user {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }

    private ResourceResolver getServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }
}
