/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.fluo.integration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import io.fluo.accumulo.format.FluoFormatter;
import io.fluo.api.client.FluoClient;
import io.fluo.api.client.Snapshot;
import io.fluo.api.config.FluoConfiguration;
import io.fluo.api.config.ObserverConfiguration;
import io.fluo.api.config.ScannerConfiguration;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.iterator.ColumnIterator;
import io.fluo.api.iterator.RowIterator;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.MiniAccumuloInstance;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base Integration Test class
 */
public class ITBase {

  protected final static String USER = "root";
  protected final static String PASSWORD = "ITSecret";
  protected final static String TABLE_BASE = "table";
  protected final static String IT_INSTANCE_NAME_PROP = FluoConfiguration.FLUO_PREFIX
      + ".it.instance.name";
  protected final static String IT_INSTANCE_CLEAR_PROP = FluoConfiguration.FLUO_PREFIX
      + ".it.instance.clear";

  protected static String instanceName;
  protected static Connector conn;
  protected static Instance miniAccumulo;
  private static MiniAccumuloCluster cluster;
  private static boolean startedCluster = false;

  protected static FluoConfiguration config;
  protected static FluoClient client;

  private static AtomicInteger tableCounter = new AtomicInteger(1);
  protected static AtomicInteger testCounter = new AtomicInteger();

  @BeforeClass
  public static void setUpAccumulo() throws Exception {
    instanceName = System.getProperty(IT_INSTANCE_NAME_PROP, "it-instance-default");
    File instanceDir = new File("target/accumulo-maven-plugin/" + instanceName);
    boolean instanceClear =
        System.getProperty(IT_INSTANCE_CLEAR_PROP, "true").equalsIgnoreCase("true");
    if (instanceDir.exists() && instanceClear) {
      FileUtils.deleteDirectory(instanceDir);
    }
    if (!instanceDir.exists()) {
      MiniAccumuloConfig cfg = new MiniAccumuloConfig(instanceDir, PASSWORD);
      cfg.setInstanceName(instanceName);
      cluster = new MiniAccumuloCluster(cfg);
      cluster.start();
      startedCluster = true;
    }
    miniAccumulo = new MiniAccumuloInstance(instanceName, instanceDir);
    conn = miniAccumulo.getConnector(USER, new PasswordToken(PASSWORD));
  }

  protected List<ObserverConfiguration> getObservers() {
    return Collections.emptyList();
  }

  public String getCurTableName() {
    return TABLE_BASE + tableCounter.get();
  }

  public String getNextTableName() {
    return TABLE_BASE + tableCounter.incrementAndGet();
  }

  protected void printTable() throws Exception {
    Scanner scanner = conn.createScanner(getCurTableName(), Authorizations.EMPTY);
    FluoFormatter af = new FluoFormatter();

    af.initialize(scanner, true);

    while (af.hasNext()) {
      System.out.println(af.next());
    }
  }

  protected void printSnapshot() throws Exception {
    try (Snapshot s = client.newSnapshot()) {
      RowIterator iter = s.get(new ScannerConfiguration());

      System.out.println("== snapshot start ==");
      while (iter.hasNext()) {
        Entry<Bytes, ColumnIterator> rowEntry = iter.next();
        ColumnIterator citer = rowEntry.getValue();
        while (citer.hasNext()) {
          Entry<Column, Bytes> colEntry = citer.next();
          System.out.println(rowEntry.getKey() + " " + colEntry.getKey() + "\t"
              + colEntry.getValue());
        }
      }
      System.out.println("=== snapshot end ===");
    }
  }

  @AfterClass
  public static void tearDownAccumulo() throws Exception {
    if (startedCluster) {
      cluster.stop();
    }
  }
}