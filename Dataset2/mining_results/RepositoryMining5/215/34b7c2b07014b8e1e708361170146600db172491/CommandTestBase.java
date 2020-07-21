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

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.plugins.coverity.CoverityUtils;
import jenkins.plugins.coverity.Utils.TestableConsoleLogger;
import org.acegisecurity.Authentication;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CoverityUtils.class, Secret.class, CredentialsMatchers.class, CredentialsProvider.class})
public abstract class CommandTestBase {

    @Mock
    protected AbstractBuild build;

    @Mock
    protected Launcher launcher;

    @Mock
    protected TaskListener listener;

    @Mock
    UsernamePasswordCredentials credentials;

    protected EnvVars envVars;
    protected String[] expectedArguments;
    protected List<String> actualArguments;
    protected TestableConsoleLogger consoleLogger;
    private int noExecutedCommands;

    @Before
    public void setup() throws IOException, InterruptedException {
        MockitoAnnotations.initMocks(this);
        envVars = new EnvVars();
        envVars.put("COV_IDIR", "TestDir");

        actualArguments = new ArrayList<String>();
        expectedArguments = null;

        setUpListener();
        setUpCoverityUtils();
        noExecutedCommands = 0;
    }

    protected void setExpectedArguments(String[] args) {
        expectedArguments = args;
    }

    private void checkCommandLineArguments() {
        assertArrayEquals(expectedArguments, actualArguments.toArray());
    }

    private void setUpListener() {
        consoleLogger = new TestableConsoleLogger();
        when(listener.getLogger()).thenReturn(consoleLogger.getPrintStream());
    }

    private void setUpCoverityUtils() throws IOException, InterruptedException {
        PowerMockito.mockStatic(CoverityUtils.class);
        setCoverityUtils_runCmd();
        setCoverityUtils_evaluateEnvVars();
        setCoverityUtils_doubleQuote();
    }

    private void setCoverityUtils_runCmd() throws IOException, InterruptedException {
        Answer<Integer> runCmd = new Answer<Integer>() {
            public Integer answer(InvocationOnMock mock) throws Throwable {
                actualArguments = (ArrayList<String>)mock.getArguments()[0];
                checkCommandLineArguments();
                noExecutedCommands++;
                return 0;
            }
        };

        when(
                CoverityUtils.runCmd(
                        Matchers.anyList(),
                        Matchers.any(AbstractBuild.class),
                        Matchers.any(Launcher.class),
                        Matchers.any(TaskListener.class),
                        Matchers.same(envVars),
                        Matchers.anyBoolean())).thenAnswer(runCmd);
    }

    private void setCoverityUtils_evaluateEnvVars() {
        Answer<String> evaluateEnvVars = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String) invocationOnMock.getArguments()[0];
            }
        };

        when(
                CoverityUtils.evaluateEnvVars(
                        Matchers.anyString(),
                        Matchers.any(EnvVars.class),
                        Matchers.anyBoolean())).thenAnswer(evaluateEnvVars);
    }

    private void setCoverityUtils_doubleQuote() {
        Answer<String> doubleQuote = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String) invocationOnMock.getArguments()[0];
            }
        };

        when(
                CoverityUtils.doubleQuote(
                        Matchers.anyString(),
                        Matchers.anyBoolean())).thenAnswer(doubleQuote);
    }

    protected void setCoverityUtils_listFiles(Collection<File> expectedFiles) {
        when(
                CoverityUtils.listFiles(
                        Matchers.any(File.class),
                        Matchers.any(FilenameFilter.class),
                        Matchers.anyBoolean())).thenReturn(expectedFiles);
    }

    protected boolean verifyNumberOfExecutedCommands(int expectedNum) {
        return expectedNum == noExecutedCommands;
    }

    protected void setCredentialManager(String username, String password){
        PowerMockito.mockStatic(CredentialsMatchers.class);
        PowerMockito.mockStatic(CredentialsProvider.class);
        credentials = Mockito.mock(UsernamePasswordCredentialsImpl.class);

        Secret secret = PowerMockito.mock(Secret.class);
        when(secret.getPlainText()).thenReturn(password);
        when(credentials.getPassword()).thenReturn(secret);
        when(credentials.getUsername()).thenReturn(username);

        when(CredentialsProvider.lookupCredentials(
                Matchers.<Class<Credentials>>any(),
                Matchers.any(ItemGroup.class),
                Matchers.any(Authentication.class),
                Matchers.anyListOf(DomainRequirement.class)
        )).thenReturn(new ArrayList<Credentials>() {});

        when(CredentialsMatchers.firstOrNull(Matchers.anyListOf(StandardCredentials.class),
                Matchers.any(CredentialsMatcher.class))).thenReturn((StandardUsernamePasswordCredentials) credentials);
    }
}
