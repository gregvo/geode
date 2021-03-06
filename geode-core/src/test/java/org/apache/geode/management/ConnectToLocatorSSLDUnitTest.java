/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management;

import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_ENABLED;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_TRUSTSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_ENABLED;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_TRUSTSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_CIPHERS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_ENABLED_COMPONENTS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE_PASSWORD;
import static org.apache.geode.internal.Assert.assertTrue;
import static org.apache.geode.util.test.TestUtil.getResourcePath;

import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.security.SecurableCommunicationChannels;
import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.rules.CleanupDUnitVMsRule;
import org.apache.geode.test.dunit.rules.GfshShellConnectionRule;
import org.apache.geode.test.dunit.rules.LocatorServerStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.categories.DistributedTest;
import org.apache.geode.test.junit.rules.serializable.SerializableTemporaryFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

@Category(DistributedTest.class)
public class ConnectToLocatorSSLDUnitTest {
  private TemporaryFolder folder = new SerializableTemporaryFolder();
  private LocatorServerStartupRule lsRule = new LocatorServerStartupRule();
  private CleanupDUnitVMsRule cleanupDUnitVMsRule = new CleanupDUnitVMsRule();

  @Rule
  public RuleChain ruleChain =
      RuleChain.outerRule(cleanupDUnitVMsRule).around(folder).around(lsRule);

  private File jks = null;
  private File securityPropsFile = null;
  private Properties securityProps;

  @Before
  public void before() throws Exception {
    this.jks = new File(getResourcePath(getClass(), "/ssl/trusted.keystore"));
    securityPropsFile = folder.newFile("security.properties");
    securityProps = new Properties();
  }

  @After
  public void after() throws Exception {
    securityPropsFile.delete();
  }

  private void setUpLocatorAndConnect(Properties securityProps) throws Exception {
    MemberVM locator = lsRule.startLocatorVM(0, securityProps);

    // saving the securityProps to a file
    OutputStream out = new FileOutputStream(securityPropsFile);
    securityProps.store(out, null);


    /*
     * When using SSL, the GfshShellConnectionRule seems to leave behind state in the JVM that
     * causes test flakinesss. (Each test method will pass if run in isolation, but when all run
     * together, the second and third tests will fail.) To avoid this issue, we connect to our
     * locator from a remote VM which is cleaned up by the CleanupDUnitVMsRule in between tests.
     */

    final int locatorPort = locator.getPort();
    final String securityPropsFilePath = securityPropsFile.getCanonicalPath();
    Host.getHost(0).getVM(1).invoke(() -> {
      GfshShellConnectionRule gfshConnector = new GfshShellConnectionRule();
      try {
        gfshConnector.connectAndVerify(locatorPort, GfshShellConnectionRule.PortType.locator,
            CliStrings.CONNECT__SECURITY_PROPERTIES, securityPropsFilePath);
      } finally {
        gfshConnector.close();
      }
    });
  }

  @Test
  public void testConnectToLocatorWithSSLJMX() throws Exception {
    securityProps.setProperty(SSL_ENABLED_COMPONENTS, SecurableCommunicationChannels.JMX);
    securityProps.setProperty(SSL_KEYSTORE, jks.getCanonicalPath());
    securityProps.setProperty(SSL_KEYSTORE_PASSWORD, "password");
    // securityProps.setProperty(SSL_KEYSTORE_TYPE, "JKS");
    securityProps.setProperty(SSL_TRUSTSTORE, jks.getCanonicalPath());
    securityProps.setProperty(SSL_TRUSTSTORE_PASSWORD, "password");
    securityProps.setProperty(SSL_PROTOCOLS, "TLSv1.2");
    securityProps.setProperty(SSL_CIPHERS, "any");

    setUpLocatorAndConnect(securityProps);
  }

  @Test
  public void testConnectToLocatorWithLegacyClusterSSL() throws Exception {
    securityProps.setProperty(CLUSTER_SSL_ENABLED, "true");
    securityProps.setProperty(CLUSTER_SSL_KEYSTORE, jks.getCanonicalPath());
    securityProps.setProperty(CLUSTER_SSL_KEYSTORE_PASSWORD, "password");
    securityProps.setProperty(CLUSTER_SSL_KEYSTORE_TYPE, "JKS");
    securityProps.setProperty(CLUSTER_SSL_TRUSTSTORE, jks.getCanonicalPath());
    securityProps.setProperty(CLUSTER_SSL_TRUSTSTORE_PASSWORD, "password");

    setUpLocatorAndConnect(securityProps);
  }

  @Test
  public void testConnectToLocatorWithLegacyJMXSSL() throws Exception {
    securityProps.setProperty(JMX_MANAGER_SSL_ENABLED, "true");
    securityProps.setProperty(JMX_MANAGER_SSL_KEYSTORE, jks.getCanonicalPath());
    securityProps.setProperty(JMX_MANAGER_SSL_KEYSTORE_PASSWORD, "password");
    securityProps.setProperty(JMX_MANAGER_SSL_KEYSTORE_TYPE, "JKS");
    securityProps.setProperty(JMX_MANAGER_SSL_TRUSTSTORE, jks.getCanonicalPath());
    securityProps.setProperty(JMX_MANAGER_SSL_TRUSTSTORE_PASSWORD, "password");

    setUpLocatorAndConnect(securityProps);
  }

}
