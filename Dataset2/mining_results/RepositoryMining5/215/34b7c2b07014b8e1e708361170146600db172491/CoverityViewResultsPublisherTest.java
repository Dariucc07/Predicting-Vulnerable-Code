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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import hudson.AbortException;
import jenkins.plugins.coverity.Utils.CIMInstanceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.coverity.CoverityPublisher.DescriptorImpl;
import jenkins.plugins.coverity.Utils.TestableConsoleLogger;
import jenkins.plugins.coverity.ws.TestableViewsService;
import jenkins.plugins.coverity.ws.WebServiceFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, WebServiceFactory.class, Client.class})
public class CoverityViewResultsPublisherTest {
    private CIMInstance cimInstance;
    private TestableConsoleLogger consoleLogger;
    private TaskListener listener;
    private Run run;
    private FilePath workspace;
    private Launcher launcher;
    private String expectedUrlMessage;
    private static final String expectedFinishedMessage = "[Coverity] Finished Publishing Coverity View Results";
    public Object lastBuildAction;

    @Before
    public void setup() throws IOException {
        // setup jenkins
        final Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getRootUrl()).thenReturn("/jenkins/");
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        final DescriptorImpl globalDescriptor = mock(CoverityPublisher.DescriptorImpl.class);
        cimInstance = new CIMInstanceBuilder().withName("pipeline-instance").withHost("test-cim-instance").withPort(8080)
                        .withUser("admin").withPassword("password").withUseSSL(false).withCredentialId("").build();
        final List<CIMInstance> cimInstances = Arrays.asList(cimInstance);
        when(globalDescriptor.getInstances()).thenReturn(cimInstances);
        when(jenkins.getDescriptorByType(CoverityPublisher.DescriptorImpl.class)).thenReturn(globalDescriptor);

        final CoverityViewResultsDescriptor stepDescriptor = new CoverityViewResultsDescriptor();
        when(jenkins.getDescriptorOrDie(CoverityViewResultsPublisher.class)).thenReturn(stepDescriptor);

        consoleLogger = new TestableConsoleLogger();
        listener = mock(TaskListener.class);
        run = mock(Run.class);
        when(run.getUrl()).thenReturn("pipeline_run/");
        workspace = new FilePath(new File("src/test/java/jenkins/plugins/coverity"));
        launcher = mock(Launcher.class);
        when(listener.getLogger()).thenReturn(consoleLogger.getPrintStream());

        expectedUrlMessage = "Coverity details: " + jenkins.getRootUrl() + run.getUrl() + "coverity_defects";
    }

    @Test(expected = AbortException.class)
    public void perform_unknownInstance_logsError() throws IOException, InterruptedException {
        final String instance = "my instance";
        final String projectId = "my projectId";
        final String view = "my connect view";

        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            "[Coverity] Unable to find Coverity Connect instance: " + instance);
    }

    @Test(expected = AbortException.class)
    public void perform_emptyProject_logsError() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "";
        final String view = "my connect view";

        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            "[Coverity] Coverity Connect project and view are required. But was Project: '" + projectId +"' View: '" + view + "'");
    }

    @Test(expected = AbortException.class)
    public void perform_nullView_logsError() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "my projectId";
        final String view = null;

        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            "[Coverity] Coverity Connect project and view are required. But was Project: '" + projectId +"' View: '" + view + "'");
    }

    @Test
    public void perform_withNoIssues_logsSuccess() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "view";

        setupRunToHandleBuildAction();
        setupIssues(view, 0);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(
            getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            getIssueCountMessage(0, projectId, view),
            expectedUrlMessage,
            expectedFinishedMessage);
    }

    @Test
    public void perform_withIssues_logsSuccess() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "view";

        setupRunToHandleBuildAction();
        setupIssues(view, 10);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            getIssueCountMessage(10, projectId, view),
            expectedUrlMessage,
            expectedFinishedMessage);
    }

    @Test
    public void perform_failPipeline_withIssues() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "view";
        final boolean failPipeline = true;

        setupRunToHandleBuildAction();
        setupIssues(view, 10);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);
        publisher.setFailPipeline(failPipeline);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            getIssueCountMessage(10, projectId, view),
            expectedUrlMessage,
            "[Coverity] Coverity issues were found and failPipeline was set to true, the pipeline result will be marked as FAILURE.",
            expectedFinishedMessage);
        verify(run).setResult(Result.FAILURE);
    }

    @Test
    public void perform_unstablePipeline_withIssues() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "view";
        final boolean unstablePipeline = true;

        setupRunToHandleBuildAction();
        setupIssues(view, 20);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);
        publisher.setUnstable(unstablePipeline);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            getIssueCountMessage(20, projectId, view),
            expectedUrlMessage,
            "[Coverity] Coverity issues were found and unstable was set to true, the pipeline result will be marked as UNSTABLE.",
            expectedFinishedMessage);
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test(expected = AbortException.class)
    public void perform_handlesError_failsPipeline() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "view";

        // setup exception when calling rest web service client
        final ClientHandlerException exception = new ClientHandlerException("Unexpected error");
        TestableViewsService.setupViewContentsApiThrows(exception, view);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            "[Coverity] Error Publishing Coverity View Results",
            exception.toString(),
            expectedFinishedMessage);
    }

    @Test(expected = AbortException.class)
    public void perform_handles400Error_logsMessage_failsPipeline() throws IOException, InterruptedException {
        final String instance = cimInstance.getName();
        final String projectId = "projectId";
        final String view = "group-by-view";
        final int httpStatus = 400;
        final String jsonResult = "{ \"message\": \"Exporting of group-by tables is not supported\"}";

        TestableViewsService.setupViewContentsApi(view, httpStatus, jsonResult);
        final CoverityViewResultsPublisher publisher = new CoverityViewResultsPublisher(instance, view, projectId);

        publisher.perform(run, workspace, launcher, listener);

        final RuntimeException exception = new RuntimeException(
            MessageFormat.format(
                "GET http://{0}:{1}/api/viewContents/issues/v1/{2}?projectId={3}&rowCount=1000&offset=0 returned a response status of {4}: {5}",
                cimInstance.getHost(), String.valueOf(cimInstance.getPort()), view, projectId, httpStatus, jsonResult));
        consoleLogger.verifyMessages(getInformationMessage(instance, projectId, view),
            getRetrievingMessage(projectId, view),
            "[Coverity] Error Publishing Coverity View Results",
            exception.toString(),
            expectedFinishedMessage);
    }

    public void setupIssues(String view, int count) {
        final StringBuilder viewContentsApiJsonResult = new StringBuilder("{\"viewContentsV1\": {" +
            "    \"offset\": 0," +
            "    \"totalRows\": " + count + "," +
            "    \"columns\": [" +
            "        {" +
            "            \"name\": \"cid\"," +
            "            \"label\": \"CID\"" +
            "        }," +
            "        {" +
            "            \"name\": \"checker\"," +
            "            \"label\": \"Checker\"" +
            "        }," +
            "        {" +
            "            \"name\": \"displayFile\"," +
            "            \"label\": \"File\"" +
            "        }" +
            "        {" +
            "            \"name\": \"displayFunction\"," +
            "            \"label\": \"Function\"" +
            "        }" +
            "    ]," +
            "    \"rows\": [");

        for (int i = 0; i < count; i++) {
            viewContentsApiJsonResult.append(
                "        {" +
                "            \"cid\": " + i + "," +
                "            \"checker\": \"FORWARD_NULL\"," +
                "            \"displayFile\": \"source.cpp\"" +
                "            \"displayFunction\": \"test" + i + "\"" +
                "        },");
        }

        viewContentsApiJsonResult.append(
            "    ]" +
            "}}");
        TestableViewsService.setupViewContentsApi(view, 200, viewContentsApiJsonResult.toString());
    }

    public String getInformationMessage(String instance, String projectId, String view) {
        return "[Coverity] Publish Coverity View Results { "+
            "connectInstance:'" + instance + "', " +
            "projectId:'" + projectId + "', " +
            "connectView:'" + view +
            "}";
    }

    public String getRetrievingMessage(String projectId, String view) {
        return "[Coverity] Retrieving issues for project \"" +
            projectId + "\" and view \"" +
            view + "\"";
    }

    public String getIssueCountMessage(int count, String projectId, String view) {
        return "[Coverity] Found " + count + " issues for project \"" +
            projectId + "\" and view \"" +
            view + "\"";
    }

    public void setupRunToHandleBuildAction() {
        Answer<Void> setCovActionAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                lastBuildAction = args[0];
                return null;
            }
        };

        Answer<CoverityBuildAction> getCovActionAnswer = new Answer<CoverityBuildAction>() {
            @Override
            public CoverityBuildAction answer(InvocationOnMock invocation) throws Throwable {
                return (CoverityBuildAction)lastBuildAction;
            }
        };
        doAnswer(setCovActionAnswer).when(run).addAction(any(CoverityBuildAction.class));
        when(run.getAction(CoverityBuildAction.class)).thenAnswer(getCovActionAnswer);
    }
}
