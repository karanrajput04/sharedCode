import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;

public class MQPCFUtilitySafe {

    private final PCFMessageAgent agent;

    public MQPCFUtilitySafe(MQQueueManager qMgr) throws MQException {
        this.agent = new PCFMessageAgent(qMgr);
    }

    /* ---------- QUEUES ---------- */
    public void displayQueues() throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q);
        req.addParameter(MQConstants.MQCA_Q_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Queue : " +
                msg.getStringParameterValue(MQConstants.MQCA_Q_NAME));

            System.out.println("Desc  : " +
                msg.getStringParameterValue(MQConstants.MQCA_Q_DESC));

            System.out.println("Depth : " +
                msg.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));

            System.out.println("----");
        }
    }

    /* ---------- CHANNEL DEFINITIONS ---------- */
    public void displayChannels() throws Exception {

        PCFMessage req =
            new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL);

        req.addParameter(MQConstants.MQCACF_CHANNEL_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Channel : " +
                msg.getStringParameterValue(
                    MQConstants.MQCACF_CHANNEL_NAME));

            System.out.println("Type    : " +
                msg.getIntParameterValue(
                    MQConstants.MQIACF_CHANNEL_TYPE));

            System.out.println("----");
        }
    }

    /* ---------- CHANNEL STATUS ---------- */
    public void displayChannelStatus() throws Exception {

        PCFMessage req =
            new PCFMessage(
                MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);

        req.addParameter(
            MQConstants.MQCACF_CHANNEL_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Channel : " +
                msg.getStringParameterValue(
                    MQConstants.MQCACF_CHANNEL_NAME));

            System.out.println("Status  : " +
                msg.getIntParameterValue(
                    MQConstants.MQIACF_CHANNEL_STATUS));

            System.out.println("MCAUser : " +
                msg.getStringParameterValue(
                    MQConstants.MQCACF_MCA_USER_ID));

            System.out.println("----");
        }
    }

    /* ---------- CONNECTIONS ---------- */
    public void displayConnections() throws Exception {

        PCFMessage req =
            new PCFMessage(
                MQConstants.MQCMD_INQUIRE_CONNECTION);

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("ConnId : " +
                msg.getByteParameterValue(
                    MQConstants.MQBACF_CONNECTION_ID));

            System.out.println("App    : " +
                msg.getStringParameterValue(
                    MQConstants.MQCACF_APPL_NAME));

            System.out.println("User   : " +
                msg.getStringParameterValue(
                    MQConstants.MQCACF_USER_IDENTIFIER));

            System.out.println("----");
        }
    }

    public void close() {
        agent.disconnect();
    }
}
