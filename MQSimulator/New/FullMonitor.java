// MQMonitor.java
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.PCFException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MQMonitor {

    private String qmName;
    private String host;
    private int port;
    private String channel;

    private MQQueueManager qmgr;
    private PCFMessageAgent agent;

    public MQMonitor(String qmName, String host, int port, String channel) throws Exception {
        this.qmName = qmName;
        this.host = host;
        this.port = port;
        this.channel = channel;
        connect();
    }

    private void connect() throws Exception {
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");

        // Setup client connection properties
        java.util.Hashtable<String, Object> props = new java.util.Hashtable<>();
        props.put(MQConstants.HOST_NAME_PROPERTY, host);
        props.put(MQConstants.PORT_PROPERTY, port);
        props.put(MQConstants.CHANNEL_PROPERTY, channel);
        props.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);

        qmgr = new MQQueueManager(qmName, props);
        agent = new PCFMessageAgent(qmgr);
    }

    /** -----------------------
     *  QUEUE DEFINITIONS
     *  ----------------------- */
    public List<Map<String, Object>> inquireQueues(String pattern) throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q);
        req.addParameter(MQConstants.MQCA_Q_NAME, pattern);
        req.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_LOCAL);

        PCFMessage[] responses = agent.send(req);

        List<Map<String, Object>> result = new ArrayList<>();

        for (PCFMessage r : responses) {
            Map<String, Object> map = new HashMap<>();

            map.put("QueueName", r.getStringParameterValue(MQConstants.MQCA_Q_NAME).trim());
            map.put("CurrentDepth", r.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));
            map.put("MaxDepth", r.getIntParameterValue(MQConstants.MQIA_MAX_Q_DEPTH));
            map.put("QueueType", r.getIntParameterValue(MQConstants.MQIA_Q_TYPE));

            result.add(map);
        }

        return result;
    }


    /** -----------------------
     *  QUEUE STATUS (PutTime / GetTime)
     *  ----------------------- */
    public Map<String, String> inquireQueueStatus(String queueName) throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_STATUS);
        req.addParameter(MQConstants.MQCA_Q_NAME, queueName);
        req.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_STATUS);

        PCFMessage[] responses = agent.send(req);

        Map<String, String> map = new HashMap<>();

        for (PCFMessage r : responses) {

            map.put("QueueName", queueName);
            map.put("LastPutDate", r.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_DATE).trim());
            map.put("LastPutTime", r.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_TIME).trim());
            map.put("LastGetDate", r.getStringParameterValue(MQConstants.MQCACF_LAST_GET_DATE).trim());
            map.put("LastGetTime", r.getStringParameterValue(MQConstants.MQCACF_LAST_GET_TIME).trim());
            map.put("OpenInputCount", String.valueOf(r.getIntParameterValue(MQConstants.MQIACF_OPEN_INPUT_COUNT)));
            map.put("OpenOutputCount", String.valueOf(r.getIntParameterValue(MQConstants.MQIACF_OPEN_OUTPUT_COUNT)));
        }

        return map;
    }


    /** -----------------------
     *  CHANNEL DEFINITIONS
     *  ----------------------- */
    public List<Map<String, Object>> inquireChannels(String pattern) throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL);
        req.addParameter(MQConstants.MQCACH_CHANNEL_NAME, pattern);

        PCFMessage[] responses = agent.send(req);

        List<Map<String, Object>> result = new ArrayList<>();

        for (PCFMessage r : responses) {
            Map<String, Object> map = new HashMap<>();

            map.put("ChannelName", r.getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim());
            map.put("ChannelType", r.getIntParameterValue(MQConstants.MQIACH_CHANNEL_TYPE));
            map.put("ConnectionName", r.getStringParameterValue(MQConstants.MQCACH_CONNECTION_NAME).trim());
            map.put("MaxInstances", r.getIntParameterValue(MQConstants.MQIACH_MAX_INSTANCES));

            result.add(map);
        }

        return result;
    }


    /** -----------------------
     *  CHANNEL STATUS
     *  ----------------------- */
    public List<Map<String, Object>> inquireChannelStatus(String pattern) throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
        req.addParameter(MQConstants.MQCACH_CHANNEL_NAME, pattern);

        PCFMessage[] responses = agent.send(req);

        List<Map<String, Object>> result = new ArrayList<>();

        for (PCFMessage r : responses) {
            Map<String, Object> map = new HashMap<>();

            map.put("ChannelName", r.getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim());
            map.put("Status", r.getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS));
            map.put("ConnName", r.getStringParameterValue(MQConstants.MQCACH_CONNECTION_NAME).trim());
            map.put("BytesSent", r.getLongParameterValue(MQConstants.MQIACH_BYTES_SENT));
            map.put("BytesReceived", r.getLongParameterValue(MQConstants.MQIACH_BYTES_RECEIVED));

            result.add(map);
        }

        return result;
    }


    /** -----------------------
     *  QUEUE MANAGER STATUS
     *  ----------------------- */
    public Map<String, Object> inquireQueueManagerStatus() throws Exception {

        PCFMessage req = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_MGR_STATUS);

        PCFMessage[] resp = agent.send(req);

        Map<String, Object> map = new HashMap<>();

        for (PCFMessage r : resp) {
            map.put("QMgrName", r.getStringParameterValue(MQConstants.MQCACF_Q_MGR_NAME).trim());
            map.put("Status", r.getIntParameterValue(MQConstants.MQIACF_Q_MGR_STATUS));
        }

        return map;
    }


    /** -----------------------
     *  CLOSE & CLEANUP
     *  ----------------------- */
    public void close() {
        try { if (agent != null) agent.disconnect(); } catch (Exception ignored) {}
        try { if (qmgr != null) qmgr.disconnect(); } catch (Exception ignored) {}
    }


    /** -----------------------
     *  TEST MAIN
     *  ----------------------- */
    public static void main(String[] args) throws Exception {

        MQMonitor mon = new MQMonitor("QM1", "localhost", 1414, "DEV.ADMIN.SVRCONN");

        System.out.println("\n=== QUEUE LIST ===");
        System.out.println(mon.inquireQueues("MY.QUEUE"));

        System.out.println("\n=== QUEUE STATUS ===");
        System.out.println(mon.inquireQueueStatus("MY.QUEUE"));

        System.out.println("\n=== CHANNEL LIST ===");
        System.out.println(mon.inquireChannels("*"));

        System.out.println("\n=== CHANNEL STATUS ===");
        System.out.println(mon.inquireChannelStatus("*"));

        System.out.println("\n=== QMGR STATUS ===");
        System.out.println(mon.inquireQueueManagerStatus());

        mon.close();
    }
}
