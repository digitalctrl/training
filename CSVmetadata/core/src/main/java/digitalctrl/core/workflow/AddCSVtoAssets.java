package digitalctrl.core.workflow;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

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
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddCSVtoAssets {
    @Reference
    private ResourceResolverFactory resolverFactory;

    //assuming file is being uploaded in the same folder as the assets
    public Node getAssetsParent(Node node) {
        try {
            node = node.getParent();
        }catch(Exception e){
        }
        return node;
    }

    //TODO: add method to find asset from node, based on the shot number found in the asset name
    //assuming the parent node/path is given, as well as the target shot identifier
    //check(loop through) the list of child nodes
    //get and parse the file name
    /*if target is found, add metadata
    public Node findAssetByShotNumber(Node parentNode,String identifier){
        NodeIterator nodes = null;
        try {
            nodes = parentNode.getNodes();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        while (nodes.hasNext()){
            Node child = nodes.nextNode();
            String shotNumber = getAssetShotNumber(child);
        }

        Node node = null;
        return null;
    }
    */

    //TODO: add method to parse asset name to check shot number
    //given asset name, parse it for the shot number or other unique identifier

    protected static String getAssetShotNumber(Asset asset) {
        String filename = asset.getName();

        return filename;
    }



    //TODO: add method to add the csv to the asset metadata with MVMap
    //connect to asset metadata
    //iterate through list of metadata given
    //put all metadata into asset
    public void inputMetadata(Asset asset, ResourceResolver resourceResolver, String[][] metadata){
        //get the path to the metadata, needed for the metadata resource
        String metadataPath = String.format("%s/%s/%s",asset.getPath(), JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);
        Resource metadataResource = resourceResolver.getResource(metadataPath);

        //map to put the keys and values into - correlate to DAM properties
        ModifiableValueMap mvm = metadataResource.adaptTo(ModifiableValueMap.class);
        String shotNumberFromAsset = getAssetShotNumber(asset);


        //probably need counter
        //have all metadata
        //get first shot number (which is the 3rd column)
        //data consists of "SHOT ###", parse it so its just the number value
        int shotNumberFromMetadata;
        String key,value;
        //loop by column
        for(int i = 1;i<metadata.length;i++){
            //get the shot number from the csv line, currently hardcoded as the 3rd column
            shotNumberFromMetadata = Integer.valueOf(metadata[i][2].split(" ")[1]);

           // if(shotNumberFromMetadata == shotNumberFromAsset.toString()){

            //}
            //go through metadata - key is [0][j] value is [i][j]
            for(int j=0;j<metadata[i].length;j++){
                key = metadata[0][j];
                value = metadata[i][j];
                //TODO: sanitization & validation
                mvm.put(key,value);
            }

        }



        //mvm.put(key,value);

    }


    public Node getNodeFromWorkItem(WorkItem workItem,WorkflowSession workflowSession) {
        WorkflowData workflowData = workItem.getWorkflowData();
        String path = ParseCSV.getPath(workflowData);

        ResourceResolver resourceResolver = null;

        final Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());

        try {
            resourceResolver = resolverFactory.getResourceResolver(authInfo);
        } catch (LoginException e) {
            e.printStackTrace();
        }
        AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
        Asset asset = assetManager.getAsset(path);
        Node node = asset.adaptTo(Node.class);
        return node;
    }
}
