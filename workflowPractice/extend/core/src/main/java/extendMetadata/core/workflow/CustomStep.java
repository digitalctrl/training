package extendMetadata.core.workflow;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

//com.day.cq imports may be deprecated for 6.3 & 6.4


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
        String path = getPath(workflowData);
        log.info("path is: " + path);


        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resolverFactory.getResourceResolver(authInfo);
            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

            Asset asset = assetManager.getAsset(path);
            if(asset == null) {
                log.info(asset + " is null"); // returns null
                return;
            }

            //get the path to the metadata, needed for the metadata resource
            String metadataPath = String.format("%s/%s/%s",asset.getPath(), JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);
            Resource metadataResource = resourceResolver.getResource(metadataPath);

            //map to put the keys and values into - correlate to DAM properties
            ModifiableValueMap mvm = metadataResource.adaptTo(ModifiableValueMap.class);
            //map to access the asset metadata keys and values
            ModifiableValueMap map = asset.adaptTo(ModifiableValueMap.class);

            log.info("\n \n START \n \n");
            for (String key : map.keySet()){
                String value = map.get(key, String.class);
                log.info(key+" "+value);
                if(key != null && value != null)
                    mvm.put(key,value);
            }
            log.info("\n \n END \n \n");


        }catch (Exception e) {
            log.error("Exception while getting metadata {}", e);
        }finally {
            resourceResolver.close();
        }

    }

    private String getPath(WorkflowData workflowData) {
        String path = workflowData.getPayload().toString();
        String[]link = path.split("\\.");
        String[] extension = link[1].split("/");
        return link[0] +"."+ extension[0];

    }
}