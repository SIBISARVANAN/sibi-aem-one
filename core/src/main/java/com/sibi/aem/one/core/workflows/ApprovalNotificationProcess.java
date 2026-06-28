package com.sibi.aem.one.core.workflows;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * AEM's built-in mail service, backed by the SMTP configuration set in
     * {@code /system/console/configMgr} → "Day CQ Mail Service".
     */
    @Reference
    private MessageGatewayService messageGatewayService;

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

        sendNotification(initiator, subject, body);
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
        // In a real project, resolve the user's email from their JCR profile
        // (/home/users/.../rep:profile/email) rather than assuming userId IS the email.
        // Placeholder: assume corporate email convention. Replace with real
        // UserManager / profile lookup in production.
        return userId + "@mysite.com";
    }
}
