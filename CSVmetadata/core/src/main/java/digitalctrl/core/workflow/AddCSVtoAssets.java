package digitalctrl.core.workflow;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(service = WorkflowProcess.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "= Add metadata properties to assets from parsed CSV file",
        "process.label" + "=CSV data to assets" })
public class AddCSVtoAssets  implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(AddCSVtoAssets.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {

        WorkflowData workflowData = workItem.getWorkflowData();
        String type = workflowData.getPayloadType();

        log.info(type);

       // if (!type.equals(""));


    }

}
