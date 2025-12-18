import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;

public class MQPCFUtilityNew {

    private final PCFMessageAgent agent;

    public MQPCFUtility(MQQueueManager qMgr) throws MQException {
        this.agent = new PCFMessageAgent(qMgr);
    }

    /* ================= QUEUES ================= */

    public void displayQueues() throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q);
        req.addParameter(MQConstants.MQCA_Q_NAME, "*");
        req.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_ALL);

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Queue  : " +
                    msg.getStringParameterValue(MQConstants.MQCA_Q_NAME));
            System.out.println("Desc   : " +
                    msg.getStringParameterValue(MQConstants.MQCA_Q_DESC));
            System.out.println("Depth  : " +
                    msg.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));
            System.out.println("-------");
        }
    }

    /* ================= CHANNEL DEFINITIONS ================= */

    public void displayChannels() throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL);
        req.addParameter(MQConstants.MQCACF_CHANNEL_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Channel : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_CHANNEL_NAME));
            System.out.println("Type    : " +
                    msg.getIntParameterValue(
                            MQConstants.MQIACF_CHANNEL_TYPE));
            System.out.println("Desc    : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_DESC));
            System.out.println("XMITQ   : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_XMIT_Q_NAME));
            System.out.println("-------");
        }
    }

    /* ================= CHANNEL STATUS ================= */

    public void displayChannelStatus() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);

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
            System.out.println("Conn    : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_CONNECTION_NAME));
            System.out.println("MCAUser : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_MCA_USER_ID));
            System.out.println("-------");
        }
    }

    /* ================= QUEUE MANAGER ================= */

    public void displayQueueManager() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_MGR);

        PCFMessage msg = agent.send(req)[0];

        System.out.println("QM Name : " +
                msg.getStringParameterValue(
                        MQConstants.MQCA_Q_MGR_NAME));
        System.out.println("Desc    : " +
                msg.getStringParameterValue(
                        MQConstants.MQCA_Q_MGR_DESC));
        System.out.println("DLQ     : " +
                msg.getStringParameterValue(
                        MQConstants.MQCA_DEAD_LETTER_Q_NAME));
    }

    /* ================= CONNECTIONS ================= */

    public void displayConnections() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_CONNECTION);

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("ConnId  : " +
                    msg.getByteParameterValue(
                            MQConstants.MQBACF_CONNECTION_ID));
            System.out.println("App     : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_APPL_NAME));
            System.out.println("Channel : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_CHANNEL_NAME));
            System.out.println("User    : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_USER_IDENTIFIER));
            System.out.println("-------");
        }
    }

    /* ================= TOPICS ================= */

    public void displayTopics() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_TOPIC);

        req.addParameter(MQConstants.MQCACF_TOPIC_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Topic   : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_TOPIC_NAME));
            System.out.println("String  : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_TOPIC_STRING));
            System.out.println("Desc    : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_TOPIC_DESC));
            System.out.println("-------");
        }
    }

    /* ================= SUBSCRIPTIONS ================= */

    public void displaySubscriptions() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_SUBSCRIPTION);

        req.addParameter(MQConstants.MQCACF_SUB_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Sub     : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_SUB_NAME));
            System.out.println("Topic   : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_TOPIC_STRING));
            System.out.println("DestQ   : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_DESTINATION));
            System.out.println("-------");
        }
    }

    /* ================= AUTH RECORDS ================= */

    public void displayAuthRecords() throws Exception {

        PCFMessage req =
                new PCFMessage(MQConstants.MQCMD_INQUIRE_AUTH_RECS);

        req.addParameter(MQConstants.MQCACF_PROFILE_NAME, "*");

        PCFMessage[] res = agent.send(req);

        for (PCFMessage msg : res) {
            System.out.println("Profile : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_PROFILE_NAME));
            System.out.println("Entity  : " +
                    msg.getStringParameterValue(
                            MQConstants.MQCACF_ENTITY_NAME));
            System.out.println("Type    : " +
                    msg.getIntParameterValue(
                            MQConstants.MQIACF_ENTITY_TYPE));
            System.out.println("-------");
        }
    }

    /* ================= CLEANUP ================= */

    public void close() {
        agent.disconnect();
    }
}
