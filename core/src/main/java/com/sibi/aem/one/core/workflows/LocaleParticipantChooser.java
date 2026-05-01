package com.sibi.aem.one.core.workflows;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.ParticipantStepChooser;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = ParticipantStepChooser.class,
        property = {"chooser.label=Locale-Based Translator Chooser"}
)
public class LocaleParticipantChooser implements ParticipantStepChooser {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleParticipantChooser.class);

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        // Logical check for locale-specific groups
        if (payloadPath.contains("/fr/") || payloadPath.contains("/fr_fr/")) {
            LOG.info("Assigning task to French translation group.");
            return "fr-translators"; // This must be a valid AEM Group/User ID
        } else if (payloadPath.contains("/de/")) {
            return "de-translators";
        }
        // Fallback for default reviewers
        return "global-reviewers";
    }
}
