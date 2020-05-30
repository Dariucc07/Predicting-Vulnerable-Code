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
package jenkins.plugins.coverity.CoverityTool;

import jenkins.plugins.coverity.*;
import jenkins.plugins.coverity.Utils.CIMInstanceBuilder;
import jenkins.plugins.coverity.Utils.CoverityPublisherBuilder;
import jenkins.plugins.coverity.Utils.InvocationAssistanceBuilder;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CovCommitDefectsCommandTest extends CommandTestBase {

    @Test
    public void prepareCommandTest() throws IOException, InterruptedException {
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                                    .withUser("TestUser").withPassword("TestPassword").withUseSSL(false).withCredentialId("")
                                    .build();

        InvocationAssistance invocationAssistance = new InvocationAssistanceBuilder().build();
        CoverityPublisher publisher =
                new CoverityPublisherBuilder().withCimStream(cimStream).
                        withInvocationAssistance(invocationAssistance).build();

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        setExpectedArguments(new String[] {
                "cov-commit-defects", "--dir", "TestDir", "--host", "Localhost",
                "--port", "8080", "--stream", "TestStream", "--user", "TestUser"
        });
        covCommitDefectsCommand.runCommand();
        assertEquals("TestPassword", envVars.get("COVERITY_PASSPHRASE"));
        consoleLogger.verifyLastMessage("[Coverity] cov-commit-defects command line arguments: " + actualArguments.toString());
    }

    @Test
    public void addHttpsPortTest() throws IOException, InterruptedException {
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                .withUser("TestUser").withPassword("TestPassword").withUseSSL(true).withCredentialId("")
                .build();

        InvocationAssistance invocationAssistance = new InvocationAssistanceBuilder().build();
        SSLConfigurations sslConfigurations = new SSLConfigurations(true, null);
        sslConfigurations.setCertFileName(new SSLCertFileName("TestCertFile"));

        CoverityPublisher.DescriptorImpl descriptor = mock(CoverityPublisher.DescriptorImpl.class);
        CoverityPublisher publisher = mock(CoverityPublisher.class);

        when(publisher.getDescriptor()).thenReturn(descriptor);
        when(publisher.getCimStream()).thenReturn(cimStream);
        when(publisher.getInvocationAssistance()).thenReturn(invocationAssistance);
        when(publisher.getInvocationAssistance()).thenReturn(invocationAssistance);
        when(descriptor.getSslConfigurations()).thenReturn(sslConfigurations);

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        setExpectedArguments(new String[] {
                "cov-commit-defects", "--dir", "TestDir", "--host", "Localhost",
                "--ssl", "--https-port", "8080", "--on-new-cert", "trust", "--certs", "TestCertFile",
                "--stream", "TestStream", "--user", "TestUser"
        });
        covCommitDefectsCommand.runCommand();
        assertEquals("TestPassword", envVars.get("COVERITY_PASSPHRASE"));
        consoleLogger.verifyLastMessage("[Coverity] cov-commit-defects command line arguments: " + actualArguments.toString());
    }

    @Test
    public void addCommitArgumentsTest() throws IOException, InterruptedException {
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                .withUser("TestUser").withPassword("TestPassword").withUseSSL(false).withCredentialId("")
                .build();

        InvocationAssistance invocationAssistance = new InvocationAssistanceBuilder().withCommitArguments("AdditionalCommitArguments").build();
        CoverityPublisher publisher =
                new CoverityPublisherBuilder().withCimStream(cimStream).
                        withInvocationAssistance(invocationAssistance).build();

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        setExpectedArguments(new String[] {
                "cov-commit-defects", "--dir", "TestDir", "--host", "Localhost",
                "--port", "8080", "--stream", "TestStream", "--user", "TestUser", "AdditionalCommitArguments"
        });
        covCommitDefectsCommand.runCommand();
        assertEquals("TestPassword", envVars.get("COVERITY_PASSPHRASE"));
        consoleLogger.verifyLastMessage("[Coverity] cov-commit-defects command line arguments: " + actualArguments.toString());
    }

    @Test
    public void addCommitArgumentsTest_WithCredentials() throws IOException, InterruptedException {

        setCredentialManager("TestUser", "TestPassword");
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                .withUser("").withPassword("").withUseSSL(false).withCredentialId("TestCredentialId")
                .build();

        InvocationAssistance invocationAssistance = new InvocationAssistanceBuilder().withCommitArguments("AdditionalCommitArguments").build();
        CoverityPublisher publisher =
                new CoverityPublisherBuilder().withCimStream(cimStream).
                        withInvocationAssistance(invocationAssistance).build();

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        setExpectedArguments(new String[] {
                "cov-commit-defects", "--dir", "TestDir", "--host", "Localhost",
                "--port", "8080", "--stream", "TestStream", "--user", "TestUser", "AdditionalCommitArguments"
        });
        covCommitDefectsCommand.runCommand();
        assertEquals("TestPassword", envVars.get("COVERITY_PASSPHRASE"));
        consoleLogger.verifyLastMessage("[Coverity] cov-commit-defects command line arguments: " + actualArguments.toString());
    }

    @Test
    public void addCommitArgumentsTest_WithParseException() throws IOException, InterruptedException {
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                .withUser("TestUser").withPassword("TestPassword").withUseSSL(false).withCredentialId("")
                .build();

        InvocationAssistance invocationAssistance = new InvocationAssistanceBuilder().withCommitArguments("\'").build();
        CoverityPublisher publisher =
                new CoverityPublisherBuilder().withCimStream(cimStream).
                        withInvocationAssistance(invocationAssistance).build();

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        try{
            covCommitDefectsCommand.runCommand();
            fail("RuntimeException should have been thrown");
        }catch (RuntimeException e) {
            assertEquals("ParseException occurred during tokenizing the cov-commit-defect commit arguments.", e.getMessage());
        }
    }

    @Test
    public void doesNotExecute_WithoutInvocationAssistance() throws IOException, InterruptedException {
        CIMStream cimStream = new CIMStream("TestInstance", "TestProject", "TestStream");
        CIMInstance cimInstance = new CIMInstanceBuilder().withName("TestInstance").withHost("Localhost").withPort(8080)
                .withUser("TestUser").withPassword("TestPassword").withUseSSL(false).withCredentialId("")
                .build();
        CoverityPublisher publisher = new CoverityPublisherBuilder().build();

        Command covCommitDefectsCommand = new CovCommitDefectsCommand(build, launcher, listener, publisher, StringUtils.EMPTY, envVars, cimStream, cimInstance);
        covCommitDefectsCommand.runCommand();
        verifyNumberOfExecutedCommands(0);
    }
}
