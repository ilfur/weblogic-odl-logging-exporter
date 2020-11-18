package weblogic.logging.exporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author MPFEIFER */
public class RegexTester {

  // static String testString = "[2020-09-26T22:44:52.440+00:00] [soa_server_1] [WARNING] []
  // [oracle.osb.logging.pipeline] [tid: [ACTIVE].ExecuteThread: '61' for queue: 'weblogic.kerne
  //  l.Default (self-tuning)'] [userId: <anonymous>] [ecid:
  // b8d5d677-23db-4938-a1db-42c810d8c82d-00000c67,0:3] [APP: Service Bus Test Framework]
  // [partition-name: DOMAIN] [tenant-name: GLOBAL]  [EchoMitLoggingPipeline,
  // EchoMitLoggingPipeline_request, RequestStage, REQUEST] HalloWelt Service called with param
  // JAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
  static String testString =
      " \\n \\n Service Ref = ERP-HTTP-Inbound/Business"
          + "Services/ERP-WriteTo-ERP_from_Topic-Inbound\\n URI ="
          + "jms://10.1.75.201:8001,10.1.75.202:8001/weblogic.jms.XAConnectionFactory/jms.ERP_from\\n"
          + "Request metadata =\\n    <xml-fragment>\\n      <tran:headers"
          + "xsi:type=\"jms:JmsRequestHeaders\" xmlns:jms=\"http://www.bea.com/wli/sb/transports/jms\""
          + "xmlns:tran=\"http://www.bea.com/wli/sb/transports\""
          + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\\n        <tran:user-header"
          + "name=\"ReceiverID\" value=\"all\"/>\\n        <tran:user-header name=\"unixTimeStamp\""
          + "value=\"1603964993972\"/>\\n        <tran:user-header name=\"SenderID\" value=\"ERP\"/>\\n"
          + "    <tran:user-header name=\"priority\" value=\"110\"/>\\n        <tran:user-header"
          + "name=\"customHeader10\" value=\"100\"/>\\n        <tran:user-header name=\"customHeader12\""
          + "value=\"fine\"/>\\n        <tran:user-header name=\"customHeader11\" value=\"B3\"/>\\n"
          + "<tran:user-header name=\"customHeader9\" value=\"K\"/>\\n        <tran:user-header"
          + "name=\"customHeader8\" value=\"\"/>\\n        <tran:user-header name=\"customHeader7\""
          + "value=\"ignoreMessage\"/>\\n        <tran:user-header name=\"customHeader6\" value=\"1\"/>\\n"
          + "       <tran:user-header name=\"customHeader1\" value=\"3\"/>\\n        <tran:user-header"
          + "name=\"customHeader5\" value=\"1\"/>\\n        <tran:user-header name=\"customHeader4\""
          + "value=\"1\"/>\\n        <tran:user-header name=\"customHeader3\" value=\"BF0\"/>\\n"
          + "<tran:user-header name=\"customHeader2\" value=\"20\"/>\\n"
          + "<jms:JMSCorrelationID>ba4fe572-0d64-49e9-a890-407a90878e8c</jms:JMSCorrelationID>\\n"
          + "<jms:JMSType>BusinessObjectItem</jms:JMSType>\\n      </tran:headers>\\n      <tran:encoding"
          + "xmlns:tran=\"http://www.bea.com/wli/sb/transports\">UTF-8</tran:encoding>\\n"
          + "</xml-fragment>\\n Encoding = UTF-8\\n Payload = \\n<?xml version=\"1.0\""
          + "encoding=\"UTF-8\"?>\\n<BusinessObject><ApplicationArea><UniqueID>ba4fe572-0d64-49e9-a890-407a90878e8c</UniqueID><Priority>110</Priority><CreationTimeStamp>1603964993972</CreationTimeStamp><DataTimeStamp>1603964683878</DataTimeStamp><SenderID>M3</SenderID><Verb>Sync</Verb><Noun>Item</Noun><EnvironmentCode>PRD</EnvironmentCode><Company>1</Company><Division/></ApplicationArea><DataArea><Item><MITMAS><CONO>1</CONO><STAT>20</STAT><ITNO>B31513212358XXL</ITNO><ITDS>Basic Rounded V-Neck</ITDS><FUDS/><DWNO/><RESP>serap.sari</RESP><DCCD>0</DCCD><UNMS>STK</UNMS><ITGR/><ITCL/><BUAR>201</BUAR><EVGR/><ITTY>BF0</ITTY><TPCD>0</TPCD><MABU>2</MABU><CHCD>3</CHCD><STCD>1</STCD><BACD>1</BACD><VOL3>0</VOL3><NEWE>0.13</NEWE><GRWE>0.139</GRWE><PPUN>STK</PPUN><BYPR>0</BYPR><WAPC>0</WAPC><QACD>0</QACD><EPCD>0</EPCD><POCY>0</POCY><ACTI>0</ACTI><HIE1>1</HIE1><HIE2>12</HIE2><HIE3>1231</HIE3><HIE4>123110</HIE4><HIE5/><GRP1>R</GRP1><GRP2/><GRP3>03</GRP3><GRP4>06</GRP4><GRP5/><CFI1>3111419</CFI1><CFI2>0</CFI2><CFI3>STD</CFI3><CFI4>0549</CFI4><CFI5/><ECCC/><TXID>754783</TXID><DTID>0</DTID><ECVE/><ECAC/><ECMA>0</ECMA><ECFL>0</ECFL><WSCA/><PRGP/><ETRF>L</ETRF><ACRF/><INDI>3</INDI><PUUN>STK</PUUN><AUTC>0</AUTC><ALUC>0</ALUC><PDCC>2</PDCC><IEAA>0</IEAA><RIDE/><RIDC>0</RIDC><GRTI/><GRTS/><EXPD>0</EXPD><ITRF/><PRCM/><GRMT/><HAZI>0</HAZI><SALE>1</SALE><FRAG>0</FRAG><TAXC/><ATMO>CBRFHS</ATMO><ATMN>3</ATMN><TPLI>315132</TPLI><FCU1>0</FCU1><SUNO>870052</SUNO><PUPR>2.55</PUPR><PUCD>0</PUCD><CUCD>EUR</CUCD><PPDT>20131217</PPDT><SAPR>0</SAPR><STUN>STK</STUN><SACD>1</SACD><CUCS>EUR</CUCS><SPDT>20200210</SPDT><DIGI/><BGRP/><PRVG/><FRE3/><FRE4/><OTDI>1</OTDI><BOGR>2</BOGR><PRGR>2</PRGR><LAMA>0</LAMA><ACHK>0</ACHK><BPEY>0</BPEY><SPUN>STK</SPUN><SPUC>2</SPUC><ALUN>STK</ALUN><UNNN>0</UNNN><UNPA/><HAC1/><HAC2/><HAC3/><DIM1/><DIM2/><DIM3/><SPE1/><SPE2/><SPE3/><SPE4/><SPE5/><CETY/><STCN/><SPAC/><TRPA/><PROD/><SMFI>0</SMFI><TANK>0</TANK><ARPA/><ARPR/><AMPT>0</AMPT><AWDY>0</AWDY><NPTO>0</NPTO><MES1/><MES2/><MES3/><MES4/><MVA1>0</MVA1><MVA2>0</MVA2><MVA3>0</MVA3><MVA4>0</MVA4><PEQ1>0</PEQ1><PEQ2>0</PEQ2><PEQ3>0</PEQ3><PEQ4>0</PEQ4><PET1>0</PET1><PET2>0</PET2><PET3>0</PET3><PET4>0</PET4><MPGM/><ORTY/><PLCD/><MAPL/><TOHI>0</TOHI><TORE>0</TORE><FEBA>0</FEBA><MAPN>0</MAPN><DOID/><SEPR>0</SEPR><STCS>0</STCS><NESA/><NSUF/><FCCM/><DPID>0</DPID><CONC>0</CONC>";

  /*"[2020-09-26T22:44:52.496+00:00] [soa_server_1] [NOTIFICATION] [OSB-398201] [oracle.osb.resources.service.service] [tid: [ACTIVE].ExecuteThread: '11' fo      r queue: 'weblogic.kernel.Default (self-tuning)'] [userId: <anonymous>] [ecid: b8d5d677-23db-4938-a1db-42c810d8c82d-00000c67,0:3:1] [APP: Service Bus F      ramework Starter Application] [partition-name: DOMAIN] [tenant-name: GLOBAL] [["
    + "[OSB Tracing] Inbound response was sent.\\n"
    + "\\n"
    + " Service Ref = LoggingTest/LoggingService\\n"
    + " URI = /logging\\n"
    + " Message ID = a00010a.N6015afd3.0.174cc803b24.N7ffc\\n"
    + "\\n"
    + "]]";
  */

  public static void matcher(Pattern pattern) {
    long start = System.currentTimeMillis();
    Matcher matcher = pattern.matcher(testString);
    boolean found = false;
    while (matcher.find()) {
      System.out.println(
          "I found the text "
              + matcher.group(1)
              + " starting at index "
              + matcher.start()
              + " and ending at index "
              + matcher.end());
      found = true;
    }
    long end = System.currentTimeMillis();
    System.out.println(
        "This pattern " + pattern.toString() + " took " + (end - start) + " millis.");
  }

  private static String findTrcOSBService(String trc) {
    int found = trc.indexOf("Service Ref = ");
    if (found >= 0) {
      int found2 = trc.indexOf("\\n", found);
      if (found2 >= 0) return (trc.substring(found + "Service Ref =".length(), found2));
      else return "";
    } else return "";
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
      } else return "";
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

  public static void main(String[] argv) {
    /*Pattern timestamp = Pattern.compile("\\[(.{29})\\]");
    Pattern componentId = Pattern.compile("\\[.{29}\\] \\[(.*?)\\]");
    Pattern messageLevel = Pattern.compile("\\[.{29}\\] \\[.*?\\] \\[(.*?)\\]");
    Pattern messageId = Pattern.compile("\\[.{29}\\] \\[.*?\\] \\[.*?\\] \\[(.*?)\\]");
    Pattern moduleId = Pattern.compile("\\[.{29}\\] \\[.*?\\] \\[.*?\\] \\[.*?\\] \\[(.*?)\\]");
    Pattern threadId = Pattern.compile(".*\\[tid: (.*?)\\] ");
    Pattern ecId = Pattern.compile(".*\\[ecid: (.*?)\\] ");
    Pattern userId = Pattern.compile(".*\\[userId: (.*?)\\] ");
    Pattern appId = Pattern.compile(".*\\[APP: (.*?)\\] ");
    Pattern partitionName = Pattern.compile(".*\\[partition-name: (.*?)\\] ");
    Pattern tenantName = Pattern.compile(".*\\[tenant-name: (.*?)\\] ");
    Pattern osbLogComponent = Pattern.compile(".*\\[tenant-name: .*?\\]  \\[(.*?)\\]");
    Pattern osbLogMessage = Pattern.compile(".*\\[tenant-name: .*?\\]  \\[.*?\\] (.*)");
    Pattern osbTraceMessage = Pattern.compile(".*\\[tenant-name: .*?\\] \\[\\[(.*)");
     */

    String[] sPatterns = {
      ".*Service Ref = (.*?)\\n",
      ".*<jms:JMSCorrelationID>(.*)<\\/jms:JMSCorrelationID>",
      ".*Payload = (.*)$",
      ".*<jms:JMSType>(.*)<\\/jms:JMSType>",
      ".*<file:file....>(.*)<\\/file:file....>",
      ".*<jms:JMSRedelivered>(.*)<\\/jms:JMSRedelivered>",
      ".*<jms:JMSXDeliveryCount>(.*)<\\/jms:JMSXDeliveryCount>"
    };
    Pattern thePattern = null;
    for (int i = 0; i < sPatterns.length; i++) {
      thePattern = Pattern.compile(sPatterns[i]);
      matcher(thePattern);
    }

    long start = System.currentTimeMillis();
    System.out.println(findTrcOSBService(testString));
    long end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcFileName(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcIsReDelivered(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcMessagePayload(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcMessageType(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcOSBService(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    start = System.currentTimeMillis();
    System.out.println(findTrcReDeliveredCounter(testString));
    end = System.currentTimeMillis();
    System.out.println("This method took " + (end - start) + " millis.");

    /*matcher(timestamp);
    matcher(componentId);
    matcher(messageLevel);
    matcher(messageId);
    matcher(moduleId);
    matcher(threadId);
    matcher(ecId);
    matcher(userId);
    matcher(appId);
    matcher(partitionName);
    matcher(tenantName);
    matcher(osbLogComponent);
    matcher(osbLogMessage);
    testString = testString.replaceAll("\n", " : ");
    matcher(osbTraceMessage);
    matcher(osbTraceServiceRef);
    matcher(osbTracePayload);
    */
  }
}
