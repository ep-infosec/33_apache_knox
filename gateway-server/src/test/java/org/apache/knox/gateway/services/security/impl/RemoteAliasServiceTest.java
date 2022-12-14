/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.services.security.impl;

import org.apache.knox.gateway.config.GatewayConfig;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.knox.gateway.services.security.impl.RemoteAliasService.REMOTE_ALIAS_SERVICE_TYPE;
import static org.easymock.EasyMock.capture;

/**
 * Test for {@link RemoteAliasService}
 */
public class RemoteAliasServiceTest {
  @Rule
  public final TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testNoRemoteAlias() throws Exception {
    GatewayConfig gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.isRemoteAliasServiceEnabled())
        .andReturn(false).anyTimes();
    String keystoreDir = testFolder.newFolder().getAbsolutePath();
    EasyMock.expect(gc.getGatewayKeystoreDir()).andReturn(keystoreDir).anyTimes();
    EasyMock.expect(gc.getCredentialStoreType()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_TYPE).anyTimes();
    EasyMock.expect(gc.getCredentialStoreAlgorithm()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_ALG).anyTimes();
    EasyMock.replay(gc);

    final String expectedClusterName = "sandbox";
    final String expectedAlias = "knox.test.alias";
    final String expectedPassword = "dummyPassword";

    final String expectedClusterNameDev = "development";
    final String expectedAliasDev = "knox.test.alias.dev";
    final String expectedPasswordDev = "otherDummyPassword";

    final DefaultMasterService ms = EasyMock
        .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
        .anyTimes();
    EasyMock.replay(ms);

    DefaultKeystoreService ks = new DefaultKeystoreService();
    ks.setMasterService(ms);
    ks.init(gc, Collections.emptyMap());

    DefaultAliasService defaultAlias = new DefaultAliasService();
    defaultAlias.setKeystoreService(ks);
    defaultAlias.setMasterService(ms);
    defaultAlias.init(gc, Collections.emptyMap());

    final RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.init(gc, Collections.emptyMap());
    remoteAliasService.start();

    /* Put */
    remoteAliasService.addAliasForCluster(expectedClusterName, expectedAlias,
        expectedPassword);
    remoteAliasService.addAliasForCluster(expectedClusterNameDev, expectedAliasDev,
        expectedPasswordDev);

    /* GET all Aliases */
    List<String> aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    List<String> aliasesDev = remoteAliasService
        .getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertEquals(aliasesDev.size(), 1);

    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));
    Assert.assertTrue("Expected alias 'knox.test.alias.dev' not found ",
        aliasesDev.contains(expectedAliasDev));

    final char[] result = remoteAliasService
        .getPasswordFromAliasForCluster(expectedClusterName, expectedAlias);
    final char[] result1 = remoteAliasService
        .getPasswordFromAliasForCluster(expectedClusterNameDev,
            expectedAliasDev);

    Assert.assertEquals(expectedPassword, new String(result));
    Assert.assertEquals(expectedPasswordDev, new String(result1));

    /* Remove */
    remoteAliasService.removeAliasForCluster(expectedClusterNameDev, expectedAliasDev);

    /* Make sure expectedAliasDev is removed*/
    aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    aliasesDev = remoteAliasService.getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliasesDev.size(), 0);
    Assert.assertFalse("Expected alias 'knox.test.alias.dev' to be removed but found.",
        aliasesDev.contains(expectedAliasDev));

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));
  }

  @Test
  public void testDoesNotGeneratePasswordIfAlreadyExists() throws Exception {
    GatewayConfig gatewayConfig = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gatewayConfig.isRemoteAliasServiceEnabled())
            .andReturn(true).anyTimes();
    String keystoreDir = testFolder.newFolder().getAbsolutePath();
    EasyMock.expect(gatewayConfig.getGatewayKeystoreDir()).andReturn(keystoreDir).anyTimes();
    EasyMock.expect(gatewayConfig.getCredentialStoreType()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_TYPE).anyTimes();
    EasyMock.expect(gatewayConfig.getCredentialStoreAlgorithm()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_ALG).anyTimes();

    EasyMock.replay(gatewayConfig);

    final DefaultMasterService ms = EasyMock
            .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
            .anyTimes();
    EasyMock.replay(ms);

    DefaultKeystoreService ks = new DefaultKeystoreService();
    ks.setMasterService(ms);
    ks.init(gatewayConfig, Collections.emptyMap());

    DefaultAliasService defaultAlias = new DefaultAliasService();
    defaultAlias.setKeystoreService(ks);
    defaultAlias.setMasterService(ms);
    defaultAlias.init(gatewayConfig, Collections.emptyMap());

    final RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.init(gatewayConfig, Collections.emptyMap());
    remoteAliasService.start();

    defaultAlias.addAliasForCluster("cluster", "alias", "existing");
    Assert.assertEquals("existing",
            new String(remoteAliasService.getPasswordFromAliasForCluster("cluster", "alias", true)));
  }

  @Test
  public void testGeneratesPasswordIfDoesNotExist() throws Exception {
    char[] GENERATED_PASSWORD = "generated".toCharArray();
    DefaultMasterService ms = EasyMock
            .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray()).anyTimes();

    DefaultAliasService defaultAlias = EasyMock.createNiceMock(DefaultAliasService.class);
    EasyMock.expect(defaultAlias.getPasswordFromAliasForCluster("cluster", "alias"))
            .andReturn(null)
            .andReturn(GENERATED_PASSWORD);

    EasyMock.replay(ms, defaultAlias);

    RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.start();

    Assert.assertEquals("generated",
            new String(remoteAliasService.getPasswordFromAliasForCluster("cluster", "alias", true)));

    EasyMock.verify(defaultAlias);
  }

  @Test
  public void testNoRemoteAliasNoConfigs() throws Exception {
    GatewayConfig gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.isRemoteAliasServiceEnabled())
        .andReturn(true).anyTimes();
    String keystoreDir = testFolder.newFolder().getAbsolutePath();
    EasyMock.expect(gc.getGatewayKeystoreDir()).andReturn(keystoreDir).anyTimes();
    EasyMock.expect(gc.getCredentialStoreType()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_TYPE).anyTimes();
    EasyMock.expect(gc.getCredentialStoreAlgorithm()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_ALG).anyTimes();

    EasyMock.replay(gc);

    final String expectedClusterName = "sandbox";
    final String expectedAlias = "knox.test.alias";
    final String expectedPassword = "dummyPassword";

    final String expectedClusterNameDev = "development";
    final String expectedAliasDev = "knox.test.alias.dev";
    final String expectedPasswordDev = "otherDummyPassword";

    final DefaultMasterService ms = EasyMock
                                        .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
        .anyTimes();
    EasyMock.replay(ms);

    DefaultKeystoreService ks = new DefaultKeystoreService();
    ks.setMasterService(ms);
    ks.init(gc, Collections.emptyMap());

    DefaultAliasService defaultAlias = new DefaultAliasService();
    defaultAlias.setKeystoreService(ks);
    defaultAlias.setMasterService(ms);
    defaultAlias.init(gc, Collections.emptyMap());

    final RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.init(gc, Collections.emptyMap());
    remoteAliasService.start();

    /* Put */
    remoteAliasService.addAliasForCluster(expectedClusterName, expectedAlias,
        expectedPassword);
    remoteAliasService.addAliasForCluster(expectedClusterNameDev, expectedAliasDev,
        expectedPasswordDev);

    /* GET all Aliases */
    List<String> aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    List<String> aliasesDev = remoteAliasService
                                  .getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertEquals(aliasesDev.size(), 1);

    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));
    Assert.assertTrue("Expected alias 'knox.test.alias.dev' not found ",
        aliasesDev.contains(expectedAliasDev));

    final char[] result = remoteAliasService
                              .getPasswordFromAliasForCluster(expectedClusterName, expectedAlias);
    final char[] result1 = remoteAliasService
                               .getPasswordFromAliasForCluster(expectedClusterNameDev,
                                   expectedAliasDev);

    Assert.assertEquals(expectedPassword, new String(result));
    Assert.assertEquals(expectedPasswordDev, new String(result1));

    /* Remove */
    remoteAliasService.removeAliasForCluster(expectedClusterNameDev, expectedAliasDev);

    /* Make sure expectedAliasDev is removed*/
    aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    aliasesDev = remoteAliasService.getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliasesDev.size(), 0);
    Assert.assertFalse("Expected alias 'knox.test.alias.dev' to be removed but found.",
        aliasesDev.contains(expectedAliasDev));

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));

    /* Test auto-generate password for alias */
    final String testAutoGeneratedpasswordAlias = "knox.test.alias.auto";

    final char[] autoGeneratedPassword = remoteAliasService
                                             .getPasswordFromAliasForCluster(expectedClusterName,
                                                 testAutoGeneratedpasswordAlias, true);
    aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);

    Assert.assertNotNull(autoGeneratedPassword);
    Assert.assertEquals(aliases.size(), 2);
    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(testAutoGeneratedpasswordAlias));
  }

  @Test
  public void testRemoteAliasServiceLoading() throws Exception {
    Map<String, String> remoteAliasConfigs = new HashMap<>();
    remoteAliasConfigs.put(REMOTE_ALIAS_SERVICE_TYPE, "test");

    GatewayConfig gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.isRemoteAliasServiceEnabled()).andReturn(true).anyTimes();
    EasyMock.expect(gc.getRemoteAliasServiceConfiguration()).andReturn(remoteAliasConfigs).anyTimes();
    EasyMock.expect(gc.getCredentialStoreType()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_TYPE).anyTimes();
    EasyMock.expect(gc.getCredentialStoreAlgorithm()).andReturn(GatewayConfig.DEFAULT_CREDENTIAL_STORE_ALG).anyTimes();
    EasyMock.replay(gc);

    final String expectedClusterName = "sandbox";
    final String expectedAlias = "knox.test.alias";
    final String expectedPassword = "dummyPassword";

    final String expectedClusterNameDev = "development";
    final String expectedAliasDev = "knox.test.alias.dev";
    final String expectedPasswordDev = "otherDummyPassword";

    final DefaultMasterService ms = EasyMock
                                        .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
        .anyTimes();
    EasyMock.replay(ms);

    // Mock Alias Service
    final DefaultAliasService defaultAlias = EasyMock
                                                 .createNiceMock(DefaultAliasService.class);
    // Captures for validating the alias creation for a generated topology
    final Capture<String> capturedCluster = EasyMock.newCapture();
    final Capture<String> capturedAlias = EasyMock.newCapture();
    final Capture<String> capturedPwd = EasyMock.newCapture();

    defaultAlias
        .addAliasForCluster(capture(capturedCluster), capture(capturedAlias),
            capture(capturedPwd));
    EasyMock.expectLastCall().anyTimes();

    /* defaultAlias.getAliasesForCluster() never returns null */
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterName))
        .andReturn(new ArrayList<>()).anyTimes();
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterNameDev))
        .andReturn(new ArrayList<>()).anyTimes();

    EasyMock.replay(defaultAlias);

    final RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.init(gc, Collections.emptyMap());
    remoteAliasService.start();

    /* Put */
    remoteAliasService.addAliasForCluster(expectedClusterName, expectedAlias,
        expectedPassword);
    remoteAliasService.addAliasForCluster(expectedClusterNameDev, expectedAliasDev,
        expectedPasswordDev);

    /* GET all Aliases */
    List<String> aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    List<String> aliasesDev = remoteAliasService
                                  .getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertEquals(aliasesDev.size(), 1);

    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));
    Assert.assertTrue("Expected alias 'knox.test.alias.dev' not found ",
        aliasesDev.contains(expectedAliasDev));

    final char[] result = remoteAliasService
                              .getPasswordFromAliasForCluster(expectedClusterName, expectedAlias);
    final char[] result1 = remoteAliasService
                               .getPasswordFromAliasForCluster(expectedClusterNameDev,
                                   expectedAliasDev);

    Assert.assertEquals(expectedPassword, new String(result));
    Assert.assertEquals(expectedPasswordDev, new String(result1));

    /* Remove */
    remoteAliasService.removeAliasForCluster(expectedClusterNameDev, expectedAliasDev);

    /* Make sure expectedAliasDev is removed*/
    aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    aliasesDev = remoteAliasService.getAliasesForCluster(expectedClusterNameDev);

    Assert.assertEquals(aliasesDev.size(), 0);
    Assert.assertFalse("Expected alias 'knox.test.alias.dev' to be removed but found.",
        aliasesDev.contains(expectedAliasDev));

    Assert.assertEquals(aliases.size(), 1);
    Assert.assertTrue("Expected alias 'knox.test.alias' not found ",
        aliases.contains(expectedAlias));
  }

  /*
   * Test the bulk alias removal method.
   */
  @Test
  public void testRemoveAliasesForCluster() throws Exception {
    Map<String, String> remoteAliasConfigs = new HashMap<>();
    remoteAliasConfigs.put(REMOTE_ALIAS_SERVICE_TYPE, "test");

    GatewayConfig gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.isRemoteAliasServiceEnabled()).andReturn(true).anyTimes();
    EasyMock.expect(gc.getRemoteAliasServiceConfiguration()).andReturn(remoteAliasConfigs).anyTimes();
    EasyMock.replay(gc);

    final String expectedClusterName = "sandbox";
    final String expectedAlias = "knox.test.alias";
    final String expectedPassword = "dummyPassword";

    final int aliasCount = 5;
    final Set<String> expectedAliases = new HashSet<>();
    for (int i = 0; i < aliasCount ; i++) {
      expectedAliases.add(expectedAlias + i);
    }

    final String expectedClusterNameDev = "development";
    final String expectedAliasDev = "knox.test.alias.dev";
    final String expectedPasswordDev = "otherDummyPassword";

    final int devAliasCount = 3;
    final Set<String> expectedDevAliases = new HashSet<>();
    for (int i = 0; i < 3 ; i++) {
      expectedDevAliases.add(expectedAliasDev + i);
    }

    final DefaultMasterService ms = EasyMock.createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray()).anyTimes();
    EasyMock.replay(ms);

    // Mock Alias Service
    final DefaultAliasService defaultAlias = EasyMock.createNiceMock(DefaultAliasService.class);
    // Captures for validating the alias creation for a generated topology
    final Capture<String> capturedCluster = EasyMock.newCapture();
    final Capture<String> capturedAlias = EasyMock.newCapture();
    final Capture<String> capturedPwd = EasyMock.newCapture();

    defaultAlias
            .addAliasForCluster(capture(capturedCluster), capture(capturedAlias),
                    capture(capturedPwd));
    EasyMock.expectLastCall().anyTimes();

    // defaultAlias.getAliasesForCluster() never returns null
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterName))
            .andReturn(new ArrayList<>()).anyTimes();
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterNameDev))
            .andReturn(new ArrayList<>()).anyTimes();

    EasyMock.replay(defaultAlias);

    final RemoteAliasService remoteAliasService = new RemoteAliasService(defaultAlias, ms);
    remoteAliasService.init(gc, Collections.emptyMap());
    remoteAliasService.start();

    // Put
    for (String alias : expectedAliases) {
      remoteAliasService.addAliasForCluster(expectedClusterName, alias, expectedPassword);
    }
    for (String alias : expectedDevAliases) {
      remoteAliasService.addAliasForCluster(expectedClusterNameDev, alias, expectedPasswordDev);
    }

    Assert.assertEquals(aliasCount, remoteAliasService.getAliasesForCluster(expectedClusterName).size());
    Assert.assertEquals(devAliasCount, remoteAliasService.getAliasesForCluster(expectedClusterNameDev).size());

    // Invoke the bulk removal method for the dev cluster
    remoteAliasService.removeAliasesForCluster(expectedClusterNameDev, expectedDevAliases);
    List<String> aliasesDev = remoteAliasService.getAliasesForCluster(expectedClusterNameDev);
    Assert.assertEquals("Expected 'knox.test.alias.dev' aliases to have been removed.", 0, aliasesDev.size());

    // Invoke the bulk removal method for the sandbox cluster
    remoteAliasService.removeAliasesForCluster(expectedClusterName, expectedAliases);
    List<String> aliases = remoteAliasService.getAliasesForCluster(expectedClusterName);
    Assert.assertEquals("Expected 'knox.test.alias' aliases to have been removed.", 0, aliases.size());
  }

}
