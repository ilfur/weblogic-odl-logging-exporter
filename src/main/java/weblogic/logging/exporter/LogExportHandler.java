/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package weblogic.logging.exporter;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import oracle.core.ojdl.logging.ODLLogRecord;
import oracle.core.ojdl.logging.context.LoggingContext;
import weblogic.diagnostics.logging.LogVariablesImpl;
import weblogic.diagnostics.query.QueryException;
import weblogic.i18n.logging.Severities;
import weblogic.logging.WLLevel;
import weblogic.logging.WLLogRecord;
import weblogic.logging.exporter.config.Config;
import weblogic.logging.exporter.config.FilterConfig;

@SuppressWarnings("UnnecessaryContinue")
class LogExportHandler extends Handler {

  private static final String DOC_TYPE = "_doc";
  private static final String INDEX = " { \"index\" : { }} ";
  private static final int offValue = Level.OFF.intValue();

  private final Client httpClient = ClientBuilder.newClient();
  private List<FilterConfig> filterConfigs = new ArrayList<>();
  private final List<String> payloadBulkList = new ArrayList<>();
  private Map trcExtendMap = null;
  private Map trcExtendPatternMap = new HashMap();

  //
  //  These will all be set by initialize()
  //
  private String indexName;
  private String publishHost;
  private int publishPort;
  private int bulkSize;
  private String httpHostPort;
  private String singleURL;
  private String bulkURL;
  private String fluentdURL;
  private String domainUID;
  private String destination;
  private String loggerName;
  private boolean sendTraceMsg;
  private int traceMsgMaxSize;
  private boolean isDebug = false;
  private boolean createMapping = true;

  public LogExportHandler(Config config) {
    initialize(config);
    if (createMapping) {
      if (loggerName.equals("WeblogicLogging")) {
        createMappings();
      } else {
        createMappingsODL();
      }
    }
  }

  @Override
  public void publish(LogRecord record) {
    WLLogRecord wlLogRecord = null;
    String payload = null;
    // System.out.println(record.getMessage());
    if (this.loggerName.equals("WeblogicLogging")) {
      wlLogRecord = (WLLogRecord) record;
    }
    if (!isLoggable(record)) {
      // System.out.println("==========RECORD IS NOT LOGGABLE!==========");
      return;
    }

    if (this.loggerName.equals("WeblogicLogging")) {
      payload = recordToPayload(wlLogRecord);
    } else {
      payload = recordToPayloadODL(record);
    }

    if (bulkSize <= 1) {
      Result result = executePutOrPostOnUrl(singleURL, payload, true);
      if (!result.successful) {
        System.out.println(
            "<weblogic.logging.exporter.LogExportHandler> logging of "
                + payload
                + " got result "
                + result);
      }
    } else {
      payloadBulkList.add(payload);
      if (payloadBulkList.size() >= bulkSize) {
        writeOutAllRecords();
      }
    }
  }

  @Override
  public void flush() {
    writeOutAllRecords();
  }

  @Override
  public void close() throws SecurityException {}

  @Override
  public boolean isLoggable(LogRecord logEntry) {
    final int levelValue = getLevel().intValue();
    if (logEntry.getLevel().intValue() < levelValue || levelValue == offValue) {
      return false;
    }
    for (FilterConfig oneConfig : filterConfigs) {

      List<String> servers = oneConfig.getServers();
      if (servers.size() == 0) {
        if (oneConfig.getQuery() != null) {
          if (applyFilter(oneConfig, (WLLogRecord) logEntry, null)) {
            continue;
          } else {
            return false;
          }
        }
      } else {
        for (String server : servers) {
          if (applyFilter(oneConfig, (WLLogRecord) logEntry, server)) {
            continue;
          } else {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean applyFilter(FilterConfig oneConfig, LogRecord logEntry, String serverName) {
    if (this.loggerName.equals("WeblogicLogging")) {
      WLLogRecord myLogEntry = (WLLogRecord) logEntry;
      try {
        if ((serverName == null) || (serverName.equals(myLogEntry.getServerName()))) {
          return oneConfig
              .getQuery()
              .executeQuery(LogVariablesImpl.getInstance().getLogVariablesResolver(myLogEntry));
        } else {
          return true;
        }
      } catch (QueryException ex) {
        // if there is any error with this expression.
        // TODO: give warning ?
        return true;
      }
    } else {
      return false;
    }
  }

  private String dataAsJson(String fieldName, String data) {
    return "\"" + fieldName + "\": \"" + data.replace("\"", "\\\"") + "\"";
  }

  private String dataAsJson(String fieldName, long data) {
    return "\"" + fieldName + "\": " + data;
  }

  private void writeOutAllRecords() {
    StringBuilder buffer = new StringBuilder();
    for (String oneRecord : payloadBulkList) {
      buffer.append(INDEX);
      buffer.append("\n");
      buffer.append(oneRecord);
      buffer.append("\n");
    }
    payloadBulkList.clear();
    Result result = executePutOrPostOnUrl(bulkURL, buffer.toString(), true);
    if (!result.successful) {
      System.out.println(
          "<weblogic.logging.exporter.LogExportHandler> logging of "
              + buffer.toString()
              + " got result "
              + result);
    }
  }

  private Result executePutOrPostOnUrl(String url, String payload, boolean post) {
    WebTarget target = httpClient.target(url);
    Invocation.Builder invocationBuilder = target.request().accept("application/json");
    Response response = null;
    try {
      response =
          post
              ? invocationBuilder.post(Entity.json(payload))
              : invocationBuilder.put(Entity.json(payload));
      String responseString = null;
      int status = response.getStatus();
      boolean successful = false;
      if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
        successful = true;
        if (response.hasEntity()) {
          responseString = String.valueOf(response.readEntity(String.class));
        }
      }
      return new Result(responseString, status, successful);
    } catch (Exception ex) {
      return new Result(ex.getMessage(), 0, false);
    }
  }

  private String recordToPayload(WLLogRecord wlLogRecord) {
    return "{"
        + dataAsJson("messageID", wlLogRecord.getId())
        + ","
        + dataAsJson("message", wlLogRecord.getMessage())
        + ","
        + dataAsJson("timestamp", wlLogRecord.getMillis())
        + ","
        + dataAsJson("serverName", wlLogRecord.getServerName())
        + ","
        + dataAsJson("threadName", wlLogRecord.getThreadName())
        + ","
        + dataAsJson("severity", wlLogRecord.getSeverityString())
        + ","
        + dataAsJson("userId", wlLogRecord.getUserId())
        + ","
        + dataAsJson("level", wlLogRecord.getLevel().toString())
        + ","
        + dataAsJson("loggerName", wlLogRecord.getLoggerName())
        + ","
        + dataAsJson("formattedDate", wlLogRecord.getFormattedDate())
        + ","
        + dataAsJson("subSystem", wlLogRecord.getSubsystem())
        + ","
        + dataAsJson("machineName", wlLogRecord.getMachineName())
        + ","
        + dataAsJson("transactionId", wlLogRecord.getTransactionId())
        + ","
        + dataAsJson("diagnosticContextId", wlLogRecord.getDiagnosticContextId())
        + ","
        + dataAsJson("sequenceNumber", wlLogRecord.getSequenceNumber())
        + ","
        + dataAsJson("domainUID", domainUID)
        + "}";
  }

  private String patternMatcher(LogRecord lrec, Pattern pattern) {
    // String sLog = lrec.getMessage().replaceAll("\n", " :: ");
    // System.out.println("========= LOG MESSAGE: " + sLog);
    Matcher matcher = pattern.matcher(lrec.getMessage());
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return "";
    }
  }

  private static String findTrcOSBService(String trc) {
    int found = trc.indexOf("Service Ref = ");
    if (found >= 0) {
      int found2 = trc.indexOf("\\n", found);
      if (found2 >= 0) {
        return (trc.substring(found + "Service Ref =".length(), found2));
      } else {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String findTrcMessageType(String trc) {
    int found = trc.indexOf("<jms:JMSType>");
    if (found >= 0) {
      int found2 = trc.indexOf("</jms:JMSType>", found);
      if (found2 >= 0) {
        return (trc.substring(found + "<jms:JMSType>".length(), found2));
      } else {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String findTrcCorrelationID(String trc) {
    int found = trc.indexOf("<jms:JMSCorrelationID>");
    if (found >= 0) {
      int found2 = trc.indexOf("</jms:JMSCorrelationID>", found);
      if (found2 >= 0) {
        return (trc.substring(found + "<jms:JMSCorrelationID>".length(), found2));
      } else {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String findTrcIsReDelivered(String trc) {
    int found = trc.indexOf("<jms:JMSRedelivered>");
    if (found >= 0) {
      int found2 = trc.indexOf("</jms:JMSRedelivered>", found);
      if (found2 >= 0) {
        return (trc.substring(found + "<jms:JMSRedelivered>".length(), found2));
      } else {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String findTrcReDeliveredCounter(String trc) {
    int found = trc.indexOf("<jms:JMSXDeliveryCount>");
    if (found >= 0) {
      int found2 = trc.indexOf("</jms:JMSXDeliveryCount>", found);
      if (found2 >= 0) {
        return (trc.substring(found + "<jms:JMSXDeliveryCount>".length(), found2));
      } else {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String findTrcFileName(String trc) {
    int found = trc.indexOf("<file:fileName>");
    if (found >= 0) {
      int found2 = trc.indexOf("</file:fileName>", found);
      if (found2 >= 0) {
        return (trc.substring(found + "<file:fileName>".length(), found2));
      } else {
        return "";
      }
    } else {
      found = trc.indexOf("<file:filePath>");
      if (found >= 0) {
        int found2 = trc.indexOf("</file:filePath>", found);
        if (found2 >= 0) {
          return (trc.substring(found + "<file:filePath>".length(), found2));
        } else {
          return "";
        }
      } else {
        return "";
      }
    }
  }

  private static String findTrcMessagePayload(String trc) {
    int found = trc.indexOf("Payload = ");
    if (found >= 0) {
      return trc.substring(found + "Payload = ".length());
    } else {
      return "";
    }
  }

  private String recordToPayloadODL(LogRecord lrec) {
    Pattern osbLogComponent = Pattern.compile("\\[(.*?)\\]");
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    LoggingContext ctx = null;
    boolean isODLLogRecord = lrec instanceof ODLLogRecord;

    if (isODLLogRecord) {
      ctx = ((ODLLogRecord) lrec).getLoggingContext();
      //  System.out.println("============ YEP, logrecord is an ODLLogRecord!");
    }
    // RuntimeContext rc = RuntimeContextManager.getRuntimeContext();
    //         if(rc != null)
    //             String appName = rc.getApplicationName();
    Object args[] = ((ODLLogRecord) lrec).getParameters();
    String sOsbTrace = "";
    String v = null;
    if (args != null && args.length > 0) {
      for (int k = 0; k < args.length; k++) {
        v = args[k] == null ? null : args[k].toString();
        if (v != null) {
          sOsbTrace = sOsbTrace.concat(" \\n ").concat(v);
        }
      }
    }

    String trcFields = "";
    Iterator<Map.Entry<String, Object>> it2 = trcExtendPatternMap.entrySet().iterator();
    Map.Entry<String, Object> entry = null;
    Pattern rxPat = null;
    String trcMsg = "";
    if (sendTraceMsg)
      trcMsg =
          sOsbTrace.substring(
              0, sOsbTrace.length() > traceMsgMaxSize ? traceMsgMaxSize : sOsbTrace.length());
    trcMsg = trcMsg.replace("\\", "\\\\");
    trcMsg = trcMsg.replace("\n", "\\n");
    trcMsg = trcMsg.replace("\t", "\\t");
    trcMsg = trcMsg.replace("\r", "\\r");
    trcMsg = trcMsg.replace("\b", "\\b");
    trcMsg = trcMsg.replace("\f", "\\f");

    String cleanTrace = sOsbTrace.replace("\\", "\\\\");
    cleanTrace = cleanTrace.replace("\n", "\\n");
    cleanTrace = cleanTrace.replace("\t", "\\t");
    cleanTrace = cleanTrace.replace("\r", "\\r");
    cleanTrace = cleanTrace.replace("\b", "\\b");
    cleanTrace = cleanTrace.replace("\f", "\\f");
    while (it2.hasNext()) {
      entry = (Map.Entry<String, Object>) it2.next();
      rxPat = (Pattern) entry.getValue();
      if (entry.getKey().equals("trc_OSBService")) {
        trcFields =
            trcFields.concat(",").concat(dataAsJson(entry.getKey(), findTrcOSBService(cleanTrace)));
      } else if (entry.getKey().equals("trc_MessageType")) {
        trcFields =
            trcFields
                .concat(",")
                .concat(dataAsJson(entry.getKey(), findTrcMessageType(cleanTrace)));
      } else if (entry.getKey().equals("trc_CorrelationID")) {
        trcFields =
            trcFields
                .concat(",")
                .concat(dataAsJson(entry.getKey(), findTrcCorrelationID(cleanTrace)));
      } else if (entry.getKey().equals("trc_IsReDelivered")) {
        trcFields =
            trcFields
                .concat(",")
                .concat(dataAsJson(entry.getKey(), findTrcIsReDelivered(cleanTrace)));
      } else if (entry.getKey().equals("trc_ReDeliveredCounter")) {
        trcFields =
            trcFields
                .concat(",")
                .concat(dataAsJson(entry.getKey(), findTrcReDeliveredCounter(cleanTrace)));
      } else if (entry.getKey().equals("trc_FileName")) {
        trcFields =
            trcFields.concat(",").concat(dataAsJson(entry.getKey(), findTrcFileName(cleanTrace)));
      } else if (entry.getKey().equals("trc_MessagePayload")) {
        trcFields =
            trcFields
                .concat(",")
                .concat(dataAsJson(entry.getKey(), findTrcMessagePayload(cleanTrace)));
      } else {
        Matcher matcher = rxPat.matcher(cleanTrace);
        if (matcher.find()) {
          if (isDebug) {
            System.out.println("Match found for " + entry.getKey() + ": " + matcher.group(1));
          }
          trcFields = trcFields.concat(",");
          // String tmpStr = matcher.group(1).replaceAll("::", " ");
          trcFields = trcFields.concat(dataAsJson(entry.getKey(), matcher.group(1)));
        } else {
          if (isDebug) {
            System.out.println("NO Match found for " + entry.getKey());
          }
          trcFields = trcFields.concat(",").concat(dataAsJson(entry.getKey(), ""));
        }
      }
    }

    String jsonLog =
        "{"
            + dataAsJson("componentID", "")
            + ","
            + dataAsJson("messageLevel", lrec.getLevel().getName())
            + ","
            + dataAsJson("timestamp", dateFormatter.format(lrec.getMillis()))
            + ","
            + dataAsJson(
                "messageId",
                ((ODLLogRecord) lrec).getMessageId() == null
                    ? ""
                    : ((ODLLogRecord) lrec).getMessageId())
            + ","
            + dataAsJson("moduleId", lrec.getLoggerName())
            + ","
            + dataAsJson("appId", "") // rc.getApplicationName())
            + ","
            + dataAsJson("threadId", new Integer(lrec.getThreadID()).toString())
            + ","
            + dataAsJson("tenantName", "") // rc.getTenantName())
            + ","
            + dataAsJson("ecId", "") // ctx.getECID() == null ? "" : ctx.getECID())
            + ","
            + dataAsJson("partitionName", "") // rc.getPartitionName())
            + ","
            + dataAsJson("loggerName", lrec.getLoggerName())
            + ","
            + dataAsJson("userId", "")
            + ","
            + dataAsJson("osbLogComponent", patternMatcher(lrec, osbLogComponent))
            + ","
            + dataAsJson("logMessage", lrec.getMessage())
            + ","
            + dataAsJson("traceMessage", trcMsg)
            + ","
            + dataAsJson("domainUID", domainUID)
            + trcFields
            + "}";

    if (isDebug) {
      System.out.println("========== WebLogicLogExporter:\n" + jsonLog);
    }

    return jsonLog;
  }

  private void initialize(Config config) {

    publishHost = config.getHost();
    publishPort = config.getPort();
    @SuppressWarnings("unused")
    boolean enabled = config.getEnabled();
    String severity = config.getSeverity();
    if (severity != null) {
      setLevel(WLLevel.getLevel(Severities.severityStringToNum(severity)));
    }
    indexName = config.getIndexName();
    bulkSize = config.getBulkSize();
    filterConfigs = config.getFilterConfigs();
    httpHostPort = "http://" + publishHost + ":" + publishPort;
    singleURL = httpHostPort + "/" + indexName + "/" + DOC_TYPE + "/?pretty";
    bulkURL = httpHostPort + "/" + indexName + "/" + DOC_TYPE + "/_bulk?pretty";
    domainUID = config.getDomainUID();
    loggerName = config.getLoggerName();
    trcExtendMap = config.getTrcExtendMap();
    traceMsgMaxSize = config.getTraceMsgMaxSize();
    sendTraceMsg = config.getSendTraceMsg();
    isDebug = config.getDebug();
    createMapping = config.getCreateMapping();

    Iterator<Map.Entry<String, Object>> it2 = trcExtendMap.entrySet().iterator();
    Map.Entry<String, Object> entry = null;
    Pattern rxPat = null;
    while (it2.hasNext()) {
      entry = (Map.Entry<String, Object>) it2.next();
      rxPat = Pattern.compile((String) entry.getValue());
      trcExtendPatternMap.put(entry.getKey(), rxPat);
    }

    //
    //  Set up the publishing variables...
    //
    httpHostPort = "http://" + publishHost + ":" + publishPort;
    singleURL = httpHostPort + "/" + indexName + "/" + DOC_TYPE + "/?pretty";
    bulkURL = httpHostPort + "/" + indexName + "/" + DOC_TYPE + "/_bulk?pretty";
    fluentdURL = httpHostPort + "/" + indexName;
  }

  private void createMappingsODL() {

    Iterator<Map.Entry<String, Object>> it2 = trcExtendMap.entrySet().iterator();
    String trcFields = "";
    Map.Entry<String, Object> entry = null;
    while (it2.hasNext()) {
      entry = (Map.Entry<String, Object>) it2.next();
      trcFields = trcFields.concat(", \"").concat((String) entry.getKey());
      if (((String) entry.getKey()).equals("trc_MessagePayload")) {
        trcFields =
            trcFields.concat("\": {\"type\": \"text\", \"store\": true, \"norms\": false  }");
      } else {
        trcFields = trcFields.concat("\": {\"type\": \"keyword\" }");
      }
    }

    // create mapping for wls elasticsearch document
    final String mappings =
        "{"
            + "  \"mappings\": {"
            + "    \""
            + DOC_TYPE
            + "\": {"
            + "      \"properties\": {"
            + "        \"timestamp\": {"
            + "\"type\": \"date\", "
            + "\"format\":\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\""
            + "},"
            + "        \"componentId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"messageLevel\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"messageId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"moduleId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"threadId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"appId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"threadId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"tenantName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"ecId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"partitionName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"loggerName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"userId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"osbLogComponent\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"logMessage\": {"
            + "\"type\": \"text\" "
            + "},"
            + "        \"traceMessage\": {"
            + "\"type\": \"text\" "
            + "},"
            + "        \"domainUID\": {"
            + "\"type\": \"keyword\" "
            + "}"
            + trcFields
            + "      }"
            + "    }"
            + "  }"
            + "}";

    if (isDebug) {
      System.out.println("========== WebLogicLogExporter:\n" + mappings);
    }
    Result result = executePutOrPostOnUrl(httpHostPort + "/" + indexName, mappings, false);
    if (!result.successful) {
      //noinspection StatementWithEmptyBody
      if (result.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
        // ignore.  this is the case where the index has been created in elastic search.
      } else {
        System.out.println(
            "<weblogic.logging.exporter.LogExportHandler> issue of "
                + mappings
                + " got result "
                + result);
      }
    }
  }

  private void createMappings() {
    // create mapping for wls elasticsearch document
    final String mappings =
        "{"
            + "  \"mappings\": {"
            + "    \""
            + DOC_TYPE
            + "\": {"
            + "      \"properties\": {"
            + "        \"timestamp\": {"
            + "\"type\": \"date\" "
            + "},"
            + "        \"sequenceNumber\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"severity\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"level\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"serverName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"threadName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"userId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"loggerName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"subSystem\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"machineName\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"transactionId\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"messageID\": {"
            + "\"type\": \"keyword\" "
            + "},"
            + "        \"domainUID\": {"
            + "\"type\": \"keyword\" "
            + "}"
            + "      }"
            + "    }"
            + "  }"
            + "}";
    Result result = executePutOrPostOnUrl(httpHostPort + "/" + indexName, mappings, false);
    if (!result.successful) {
      //noinspection StatementWithEmptyBody
      if (result.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
        // ignore.  this is the case where the index has been created in elastic search.
      } else {
        System.out.println(
            "<weblogic.logging.exporter.LogExportHandler> issue of "
                + mappings
                + " got result "
                + result);
      }
    }
  }
}
