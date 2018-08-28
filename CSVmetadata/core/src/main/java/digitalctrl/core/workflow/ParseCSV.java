package digitalctrl.core.workflow;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.poi.ss.usermodel.*;
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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component(service = WorkflowProcess.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Parse csv file to be usable",
        "process.label" + "=CSV Parser" })
public class ParseCSV  implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(ParseCSV.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        log.info("\n \n WORKFLOW START \n \n");

        WorkflowData workflowData = workItem.getWorkflowData();
        String path = getPath(workflowData);
        log.info("\n \n "+path+" \n \n");

        String fileType = getExtension(path);
        String CSVdata = "";

        //check if file is csv, xls, or xlsx
        if (!isCompatible(fileType)){
            //convert xls/xlsx file to csv
            //CSVdata = excelToCSV(path);
        }

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        try {
            ResourceResolver resourceResolver = resolverFactory.getResourceResolver(authInfo);
            Node node = resourceResolver.getResource(path+"/jcr:content/renditions/original/jcr:content").adaptTo(Node.class);

            InputStream in = node.getProperty("jcr:data").getBinary().getStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            //TODO: change to arraylist or something to take up actual amount of space
            String[][] csvOutput = new String[100][50];
            log.info("\n \n created csvOutput \n \n");
            String line;
            String[] row;
            int x=0;

            //reads through CSV file and puts it into a 2d string array csvOutput
            try {
                while ((line = reader.readLine()) != null) {
                    row = line.split(",", -1);

                    for (int i = 0; i < row.length; i++) {
                        csvOutput[x][i] = row[i];
                    }
                    x++;
                }
            }catch (Exception e){
            }finally{
                reader.close();
            }

            //loop through assets to then put the metadata into
            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
            Node assetNode = resourceResolver.getResource(path).adaptTo(Node.class);
            Node parentNode = assetNode.getParent();
            //loop through assets in the folder
            for (NodeIterator it = parentNode.getNodes(); it.hasNext();) {
                Node child = (Node) it.next();
                //String nodePath = child.getPath()+"/jcr:content/renditions/original/jcr:content";
                //always returning same value
                //child = resourceResolver.getResource(path).adaptTo(Node.class);

                //ex) FA18CAMBRIA_004_0109.jpg -> 0109
                //big assumption that only the images have underscores, and must have the same format
                if(child.getName().contains("_")) {
                    String shotNumber = child.getName().split("_")[2].split("\\.")[0];
                    //have the shot number from the asset
                    //find shot number from the csv list, linear to start
                    //TODO: change csvOutput to hashmap
                    //if shotNumber == csvShotNumber, input metadata


                }
            }





        }catch(Exception e){
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
    private boolean isCompatible(String fileType){
        switch (fileType){
            case "csv":
                log.info("Attempting to parse CSV file...");
                return true;
            case "xls":
            case "xlsx":
                log.info("Attempting to convert file to CSV...");
                return false;
            default:
                log.info("File is not in CSV, xls, or xlsx format. Shutting down...");
                System.exit(0);
        }
        return false;
    }

    /***********************NOT WORKING*****************************/
    private String excelToCSV(String path){
        log.info("\n \n in excelToCSV method \n \n");
        InputStream inputStream = null;
        StringBuilder CSVoutput = new StringBuilder();

        try{
            inputStream = getClass().getResourceAsStream(path);

            log.info("\n \n "+ inputStream.toString()+" \n \n");

            //Workbook workbook = new XSSFWorkbook(inputStream);
            Workbook workbook = WorkbookFactory.create(new File(path));

            log.info("\n \n "+workbook.getSheetName(0)+" \n \n");

            DataFormatter formatter = new DataFormatter();

            log.info("\n \n after DataFormatter \n \n");

            for (Sheet sheet : workbook){
                for (Row row : sheet){
                    boolean firstCell = true;
                    for (Cell cell : row){
                        if(!firstCell)
                            CSVoutput.append(",");
                        CSVoutput.append(formatter.formatCellValue(cell));
                        firstCell = false;
                    }
                    CSVoutput.append("\n");
                }
            }
            log.info("\n \n after for loop \n \n");
        }catch(Exception e){
            log.info(e.toString());
        }finally{
            try {
                inputStream.close();
            }catch(Exception e){
                log.info(e.toString());
            }
        }

        return CSVoutput.toString();
    }

}