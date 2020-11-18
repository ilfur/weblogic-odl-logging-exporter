/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weblogic.logging.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.yaml.snakeyaml.Yaml;

/** @author MPFEIFER */
public class ConfigExtendTester {

  public static void main(String[] args) {
    System.out.println("Running...");
    try {
      File file = new File("samples/WebLogicLoggingExporter.yaml");
      Map<String, Object> config = new Yaml().load(new FileInputStream(file));

      Iterator it = config.keySet().iterator();
      String keyName = null;
      Map trcExtendMap = new HashMap();

      Iterator<Entry<String, Object>> it2 = config.entrySet().iterator();
      keyName = null;
      Entry<String, Object> entry = null;
      while (it2.hasNext()) {
        entry = (Entry<String, Object>) it2.next();
        keyName = (String) entry.getKey();
        if (keyName.startsWith("trc_")) {
          System.out.println(keyName);
          System.out.println((String) entry.getValue());
          trcExtendMap.put(keyName, (String) entry.getValue());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
