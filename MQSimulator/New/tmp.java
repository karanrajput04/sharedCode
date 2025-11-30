import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.*;

public class QueueStatusTimes {

    public static void main(String[] args) throws Exception {
        MQQueueManager qmgr = new MQQueueManager("QM1");
        PCFMessageAgent agent = new PCFMessageAgent(qmgr);

        PCFMessage request = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_STATUS);

        request.addParameter(MQConstants.MQCA_Q_NAME, "MY.QUEUE");
        request.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_STATUS);

        PCFMessage[] responses = agent.send(request);

        for (PCFMessage resp : responses) {

            String lastPutDate = (String) resp.getParameterValue(MQConstants.MQCACF_LAST_PUT_DATE);
            String lastPutTime = (String) resp.getParameterValue(MQConstants.MQCACF_LAST_PUT_TIME);

            String lastGetDate = (String) resp.getParameterValue(MQConstants.MQCACF_LAST_GET_DATE);
            String lastGetTime = (String) resp.getParameterValue(MQConstants.MQCACF_LAST_GET_TIME);

            System.out.println("Last Put Time: " + lastPutDate + " " + lastPutTime);
            System.out.println("Last Get Time: " + lastGetDate + " " + lastGetTime);
        }

        agent.disconnect();
        qmgr.disconnect();
    }
}
