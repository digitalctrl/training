package digitalctrl.core.workflow;

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
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


@Component(service = WorkflowProcess.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Parse csv file to be usable",
        "process.label" + "=CSV Parser" })
public class ParseCSV  implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(ParseCSV.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        log.info("\n \n CSV workflow start \n \n");

        WorkflowData workflowData = workItem.getWorkflowData();
        String path = getPath(workflowData);

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        try {
            ResourceResolver resourceResolver = resolverFactory.getResourceResolver(authInfo);
            Node node = resourceResolver.getResource(path+"/jcr:content/renditions/original/jcr:content").adaptTo(Node.class);
            InputStream in = node.getProperty("jcr:data").getBinary().getStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Map<String, List<String[]>> metadata = new HashMap<>();
            String line;


            //reads through CSV file and maps metadata in hashmap
            //assuming row #3 is the shotNumber
            try {
                String[] firstRow = reader.readLine().split(",",-1);
                while ((line = reader.readLine()) != null) {
                    List<String[]> metaValues = new ArrayList<>();
                    String[] currentRow = line.split(",", -1);
                    for (int i = 0; i < currentRow.length; i++) {
                        //key should be shotNumber, first array value should be firstRow header, second is the value of the row
                        metaValues.add(i,new String[]{firstRow[i],currentRow[i]});
                    }
                    //input arrayList of String[], mapped to the shot number
                    metadata.put(currentRow[2].split(" ")[1],metaValues);
                }

            }catch (Exception e){
                log.info("Error putting metadata in");
            }finally{
                reader.close();
            }
            Node assetNode = resourceResolver.getResource(path).adaptTo(Node.class);
            Node parentNode = assetNode.getParent();
            //loop through assets in the folder
            for (NodeIterator it = parentNode.getNodes(); it.hasNext();) {
                Node child = (Node) it.next();
                //ex) FA18CAMBRIA_004_0109.jpg -> 0109
                //big assumption that only the images have underscores, and must have the same format
                if(child.getName().contains("_")) {
                    String shotNumber = child.getName().split("_")[2].split("\\.")[0];
                    //get metadata from shotNumber
                    //TODO: validation?
                    List<String[]> metaValues1 = metadata.get(shotNumber);
                    //get map to be able to input metadata

                    AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
                    Asset asset = assetManager.getAsset(child.getPath());

                    String metadataPath = String.format("%s/%s/%s",asset.getPath(), JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);
                    Resource metadataResource = resourceResolver.getResource(metadataPath);
                    ModifiableValueMap mvm = metadataResource.adaptTo(ModifiableValueMap.class);
                    //put metadata into asset from arrayList

                    String[] property;
                    //issue - looping through ALL metadata for each asset
                    for(int i=0;i<metaValues1.size();i++){
                        //get properties - location 0 is the property type, location 1 is the property value
                        property = metaValues1.get(i);
                        mvm.put(property[0],property[1]);
                    }

                }
            }
            resourceResolver.close();

        }catch(Exception e) {
            log.error(e.toString());
        }
    }

    protected static String getPath(WorkflowData workflowData) {
        String path = workflowData.getPayload().toString();
        String[]link = path.split("\\.");
        String[] extension = link[1].split("/");
        return link[0] +"."+ extension[0];

    }
    private String getExtension(String path){
        String[]link = path.split("\\.");
        String[] extension = link[1].split("/");
        return extension[0];

    }

}