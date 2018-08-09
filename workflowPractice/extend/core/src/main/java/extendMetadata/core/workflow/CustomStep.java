package extendMetadata.core.workflow;




import com.day.cq.dam.api.Asset;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


@Component(service = WorkflowProcess.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Custom metadata workflow",
        "process.label" + "=custom DAM metadata extraction" })
public class CustomStep implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(CustomStep.class);

    @Reference
    private ResourceResolverFactory resolverFactory;


    /**
     * The method called by the AEM Workflow Engine to perform Workflow work.
     *
     * @param workItem the work item representing the resource moving through the Workflow
     * @param workflowSession the workflow session
     * @param args arguments for this Workflow Process defined on the Workflow Model (PROCESS_ARGS, argSingle, argMulti)
     * @throws WorkflowException when the Workflow Process step cannot complete. This will cause the WF to retry.
     */
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {


        // Get the Workflow data

        final WorkflowData workflowData = workItem.getWorkflowData();
        final String type = workflowData.getPayloadType();


        // Check if the payload is a path in the JCR
        if (!type.equals("JCR_PATH")) {
            return;
        }
        // Get the path to the JCR resource from the payload
        final String path = workflowData.getPayload().toString();


        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        final ResourceResolver resourceResolver;

        try {
            resourceResolver = resolverFactory.getResourceResolver(authInfo);
            Resource payload = resourceResolver.getResource(path);


            Asset asset = payload.adaptTo(Asset.class);

            String title = asset.getMetadataValue("xmp:Categories");
            //searchable in the log
            log.info("testingTitle: " + title);

            //String customTitle = asset.getMetadataValue("xmp:Categories").toString():

            //customTitle = asset.getMetadata("xmp:Categories").toString():
        }catch (Exception e) {
            log.error("Exception while getting metadata {}", e);
        }

    }
}