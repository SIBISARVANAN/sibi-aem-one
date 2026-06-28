package com.sibi.aem.one.core.workflows;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * Workflow Process Step that activates (publishes) the payload as the FINAL
 * step of a product approval workflow.
 *
 * <h2>Real-World Scenario</h2>
 * <p>A new product page goes through an approval workflow: Legal Review →
 * Pricing Review → <strong>Publish Step (this class)</strong>. Once both
 * approvals pass, this step publishes the page automatically — no separate
 * manual "Activate Page" action needed from the approver.</p>
 *
 * <h2>Technical Intricacies Demonstrated</h2>
 * <ul>
 *   <li>Calling {@link Replicator#replicate} from inside a {@code WorkflowProcess}
 *       — the standard way to combine workflow approval with publishing.</li>
 *   <li>Adapting the {@code WorkflowSession}'s underlying JCR {@code Session}
 *       (workflow steps already run with a session — no need for a separate
 *       service-user resolver here, since the workflow itself runs as a
 *       configured workflow service user).</li>
 *   <li>Writing the replication outcome back into {@code WorkflowData} metadata
 *       so a subsequent Notification step (see {@link ApprovalNotificationProcess})
 *       can report success/failure to the requester.</li>
 *   <li>Proper {@link WorkflowException} propagation — throwing this causes the
 *       workflow instance to halt and show as failed in the AEM workflow console,
 *       which is the correct behaviour when publishing fails.</li>
 * </ul>
 */
@Component(service = WorkflowProcess.class, immediate = true,
        property = { "process.label=Publish Approved Product" })
public class ProductPublishProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPublishProcess.class);

    @Reference
    private Replicator replicator;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args)
            throws WorkflowException {

        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        LOG.info("Publishing approved product page: {}", payloadPath);

        try {
            // The WorkflowSession already wraps a JCR Session bound to the
            // workflow's configured service user — no extra resolver needed.
            Session session = workflowSession.getSession();

            replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath);

            // Record success in the WorkflowData metadata map so it survives
            // into the next step (the notification step reads this).
            workItem.getWorkflowData().getMetaDataMap().put("publishStatus", "SUCCESS");
            workItem.getWorkflowData().getMetaDataMap().put("publishedPath", payloadPath);

            LOG.info("Successfully published: {}", payloadPath);

        } catch (ReplicationException e) {
            // Record failure for the notification step to report to the requester,
            // THEN halt the workflow by throwing — this is the correct dual behaviour:
            // downstream steps can still read what happened, but the workflow stops.
            workItem.getWorkflowData().getMetaDataMap().put("publishStatus", "FAILED");
            workItem.getWorkflowData().getMetaDataMap().put("publishError", e.getMessage());

            LOG.error("Failed to publish {}: {}", payloadPath, e.getMessage(), e);
            throw new WorkflowException("Publishing failed for " + payloadPath, e);
        }
    }
}
