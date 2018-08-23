package digitalctrl.core.workflow;

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
        log.info(CSVdata);

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        try {
            ResourceResolver resourceResolver = resolverFactory.getResourceResolver(authInfo);
            Node node = resourceResolver.getResource(path+"/jcr:content/renditions/original/jcr:content").adaptTo(Node.class);
            InputStream in = node.getProperty("jcr:data").getBinary().getStream();

            //////////////////////////////////////////////////////////////////////
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Map<String, List<String>> csvOutput = new HashMap<>();
            String line,header;
            List<String> data;


            while ((line = reader.readLine()) != null) {
                header = line.split("\\,")[0];
                data = Arrays.asList(line.split("\\,"));
                data.remove(0);
                //Map contains key as the head of the column, and the data contains all cells
                csvOutput.put(header,data);
            }
            reader.close();
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