/*******************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 *******************************************************************************/
package jenkins.plugins.coverity;

import hudson.Launcher;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.remoting.LocalChannel;
import hudson.tools.ToolLocationNodeProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.coverity.Utils.*;
import jenkins.plugins.coverity.ws.TestWebServiceFactory;
import jenkins.plugins.coverity.ws.TestWebServiceFactory.TestConfigurationService;
import jenkins.plugins.coverity.ws.WebServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, Executor.class, ToolLocationNodeProperty.class, WebServiceFactory.class})
public class CheckConfigTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private CoverityPublisher.DescriptorImpl descriptor;

    @Mock
    protected Launcher launcher;

    @Mock
    protected TaskListener listener;

    private WebServiceFactory testWsFactory;
    private TestableConsoleLogger testLogger;

    @Before
    public void setup() throws IOException {
        // setup web service factory
        testWsFactory = new TestWebServiceFactory();
        PowerMockito.mockStatic(WebServiceFactory.class);
        when(WebServiceFactory.getInstance()).thenReturn(testWsFactory);

        // setup jenkins
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        descriptor = mock(CoverityPublisher.DescriptorImpl.class);
        when(jenkins.getDescriptorOrDie(CoverityPublisher.class)).thenReturn(descriptor);

        testLogger = new TestableConsoleLogger();
        when(listener.getLogger()).thenReturn(testLogger.getPrintStream());
    }

    @Test
    public void checkStreamTest_NoCIMStream() {
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();
        CIMStream cimStream = new CIMStream(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);

        CheckConfig.StreamStatus status = CheckConfig.checkStream(publisher, null);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertEquals("Could not connect to a Coverity instance. \n " +
                "Verify that a Coverity instance has been configured for this job",
                statusMessage);
        assertNull(status.getVersion());

        status = CheckConfig.checkStream(publisher, cimStream);
        assertNotNull(status);
        assertFalse(status.isValid());
        statusMessage = status.getStatus();
        assertEquals("[Stream] null/null/null : Could not connect to a Coverity instance. \n" +
                " Verify that a Coverity instance has been configured for this job", statusMessage);
        assertNull(status.getVersion());
    }

    @Test
    public void checkStreamTest_NoCIMInstance() {
        when(descriptor.getInstance(any(String.class))).thenReturn(null);
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();
        CIMStream cimStream = new CIMStream("test-cim-instance", StringUtils.EMPTY, StringUtils.EMPTY);

        CheckConfig.StreamStatus status = CheckConfig.checkStream(publisher, cimStream);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Stream] test-cim-instance/null/null : Could not connect to a Coverity instance. \n" +
                " Verify that a Coverity instance has been configured for this job", statusMessage);
        assertNull(status.getVersion());
    }

    @Test
    public void checkStreamTest_NoStreamConfiguredForCIMStream() {
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("test-cim-instance").withHost("test-cim-instance").withPort(8080)
                .withUser("admin").withPassword("password").withUseSSL(false).withCredentialId("")
                .build();
        CIMStream cimStream = new CIMStream("test-cim-instance", StringUtils.EMPTY, StringUtils.EMPTY);
        when(descriptor.getInstance(any(CoverityPublisher.class))).thenReturn(cimInstance);

        CheckConfig.StreamStatus status = CheckConfig.checkStream(publisher, cimStream);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Stream] test-cim-instance/null/null : " +
                "Could not find any Stream that matches the given configuration for this job.", statusMessage);
        assertNull(status.getVersion());
    }

    @Test
    public void checkStreamTest_WhenCIMInstance_DoCheckFail() throws IOException {
        CIMInstance cimInstance = Mockito.mock(CIMInstance.class);
        when(cimInstance.doCheck()).thenReturn(FormValidation.error(StringUtils.EMPTY));
        when(descriptor.getInstance(any(CoverityPublisher.class))).thenReturn(cimInstance);
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();
        CIMStream cimStream = new CIMStream("test-cim-instance", StringUtils.EMPTY, "test-stream");

        CheckConfig.StreamStatus status = CheckConfig.checkStream(publisher, cimStream);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Stream] test-cim-instance/null/test-stream : Could not connect to instance: ERROR: ", statusMessage);
        assertNull(status.getVersion());
    }

    @Test
    public void checkStreamTest_WhenStreamDoesNotExist() throws IOException {
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();
        CIMInstance cimInstance = Mockito.mock(CIMInstance.class);
        TestConfigurationService testConfigurationService = (TestConfigurationService)WebServiceFactory.getInstance().getConfigurationService(cimInstance);
        testConfigurationService.setupExternalVersion("2017.07");
        testConfigurationService.setupProjects(StringUtils.EMPTY, 0, StringUtils.EMPTY, 0);
        when(cimInstance.doCheck()).thenReturn(FormValidation.ok());
        when(cimInstance.getConfigurationService()).thenReturn(testConfigurationService);
        when(descriptor.getInstance(any(CoverityPublisher.class))).thenReturn(cimInstance);
        CIMStream cimStream = new CIMStream("test-cim-instance", StringUtils.EMPTY, "test-stream");

        CheckConfig.StreamStatus status = CheckConfig.checkStream(publisher, cimStream);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Stream] test-cim-instance/null/test-stream : Stream does not exist", statusMessage);
        assertNotNull(status.getVersion());
        assertEquals(new CoverityVersion(2017, 07), status.getVersion());
    }

    @Test
    public void checkTaOptionBlockTest_Success() {
        TaOptionBlock taOptionBlock =
                new TaOptionBlockBuilder().
                        withJavaOptionBlock(true).
                        withJavaCoverageTool("Jacoco").
                        withJunitFramework(true).
                        withPolicyFile("TestPolicyFilePath").
                        build();

        CheckConfig.Status status = CheckConfig.checkTaOptionBlock(taOptionBlock);
        assertNotNull(status);
        assertTrue(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Test Advisor] Configuration is valid!", statusMessage);
    }

    @Test
    public void checkTaOptionBlockTest_NoCoverityLanguageSelected() {
        TaOptionBlock taOptionBlock =
                new TaOptionBlockBuilder().
                        withPolicyFile("TestPolicyFilePath").
                        build();

        CheckConfig.Status status = CheckConfig.checkTaOptionBlock(taOptionBlock);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[Test Advisor] No Coverage language was chosen, please pick at least one \n",
                statusMessage);
    }

    @Test
    public void checkScmOptionBlockTest_Success() {
        ScmOptionBlock scmOptionBlock =
                new ScmOptionBlockBuilder().
                        withScmSystem("git").
                        build();

        CheckConfig.Status status = CheckConfig.checkScmOptionBlock(scmOptionBlock);
        assertNotNull(status);
        assertTrue(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[SCM] Configuration is valid!", statusMessage);
    }

    @Test
    public void checkScmOptionBlockTest_NoAccurevRepo() {
        ScmOptionBlock scmOptionBlock =
                new ScmOptionBlockBuilder().
                        withScmSystem("accurev").
                        build();

        CheckConfig.Status status = CheckConfig.checkScmOptionBlock(scmOptionBlock);
        assertNotNull(status);
        assertFalse(status.isValid());
        String statusMessage = status.getStatus();
        assertNotNull(statusMessage);
        assertEquals("[SCM] Please specify AccuRev's source control repository under 'Advanced' \n",
                statusMessage);
    }

    @Test
    public void checkNode_whenNoInstallationFound() throws IOException, InterruptedException {
        final String nodeName = "test-node";
        final FreeStyleBuild build = TestUtils.getFreeStyleBuild();
        TestUtils.setupCurrentExecutorWithNode(nodeName, build, null);

        final CheckConfig.NodeStatus result = CheckConfig.checkNode(null, build, launcher, listener);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("[Node] " + nodeName + " : Could not find Coverity Analysis tools installation.", result.getStatus());
    }

    @Test
    public void checkNode_withInstallation() throws IOException, InterruptedException {
        final LocalChannel channel = mock(LocalChannel.class);
        when(launcher.getChannel()).thenReturn(channel);

        final CoverityToolInstallation installation = TestUtils.getTestInstallation();
        when(descriptor.getInstallations()).thenReturn(new CoverityToolInstallation[] { installation });

        final String nodeName = "test-node";
        final FreeStyleBuild build = TestUtils.getFreeStyleBuild(new CoverityPublisherBuilder().build());
        TestUtils.setupCurrentExecutorWithNode(nodeName, build, installation);

        final CheckConfig.NodeStatus result = CheckConfig.checkNode(null, build, launcher, listener);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("[Node] " + nodeName + " : version 2017.07", result.getStatus());
    }
}
