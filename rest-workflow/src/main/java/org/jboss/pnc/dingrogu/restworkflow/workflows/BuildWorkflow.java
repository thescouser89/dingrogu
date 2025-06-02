package org.jboss.pnc.dingrogu.restworkflow.workflows;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.builddriver.dto.BuildCompleted;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.ArtifactQuality;
import org.jboss.pnc.api.enums.BuildCategory;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryArtifact;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteResult;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.api.reqour.dto.ReqourCallback;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildExecutionConfigurationSimplifiedDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkflowClearEnvironmentDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.adapter.BuildDriverAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.EnvironmentDriverCompleteAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.EnvironmentDriverCreateAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverPromoteAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverSealAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverSetupAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourAdjustAdapter;
import org.jboss.pnc.dingrogu.restadapter.client.GenericClient;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.RepositoryType;
import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.model.TargetRepository;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.spi.BuildResult;
import org.jboss.pnc.spi.builddriver.BuildDriverResult;
import org.jboss.pnc.spi.coordinator.CompletionStatus;
import org.jboss.pnc.spi.coordinator.ProcessException;
import org.jboss.pnc.spi.environment.EnvironmentDriverResult;
import org.jboss.pnc.spi.executor.BuildExecutionConfiguration;
import org.jboss.pnc.spi.repositorymanager.RepositoryManagerResult;
import org.jboss.pnc.spi.repour.RepourResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

/**
 * Build process workflow implementation
 */
@ApplicationScoped
public class BuildWorkflow implements Workflow<BuildWorkDTO> {

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @Inject
    RepositoryDriverSetupAdapter repositoryDriverSetupAdapter;

    @Inject
    BuildDriverAdapter buildDriverAdapter;

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @Inject
    EnvironmentDriverCompleteAdapter environmentDriverCompleteAdapter;

    @Inject
    RepositoryDriverSealAdapter repositoryDriverSealAdapter;

    @Inject
    RepositoryDriverPromoteAdapter repositoryDriverPromoteAdapter;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GenericClient genericClient;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    QueueEndpoint queueEndpoint;

    @ConfigProperty(name = "rexclient.build.queue_name")
    String rexQueueName;

    @ConfigProperty(name = "rexclient.build.queue_size")
    int rexQueueSize;

    private static final Set<State> STATE_FAILED = Set.of(State.FAILED, State.START_FAILED, State.STOP_FAILED);

    @Override
    @Deprecated
    public CorrelationId submitWorkflow(BuildWorkDTO buildWorkDTO) throws WorkflowSubmissionException {
        throw new UnsupportedOperationException("Use submitWorkflow(StartRequest) instead");
    }

    /**
     * Variant of submitWorkflow accepting the Rex startRequest
     *
     * @param startRequest
     * @return
     * @throws WorkflowSubmissionException
     */
    public CorrelationId submitWorkflow(StartRequest startRequest) throws WorkflowSubmissionException {
        BuildWorkDTO buildWorkDTO = objectMapper.convertValue(startRequest.getPayload(), BuildWorkDTO.class);
        Log.info(buildWorkDTO);

        try {
            CreateTaskDTO taskAdjustReqour = reqourAdjustAdapter
                    .generateRexTask(
                            ownUrl,
                            buildWorkDTO.getCorrelationId(),
                            startRequest,
                            buildWorkDTO.toReqourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repositoryDriverSetupAdapter.generateRexTask(
                    ownUrl,
                    buildWorkDTO.getCorrelationId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSetupDTO());

            Request cleanBuildEnvOnFailureForEnvironmentDriver = getCleanBuildEnvOnFailure(
                    buildWorkDTO,
                    environmentDriverCreateAdapter.getRexTaskName(buildWorkDTO.getCorrelationId()));

            CreateTaskDTO taskCreateEnv = environmentDriverCreateAdapter.generateRexTaskRetryItself(
                    ownUrl,
                    buildWorkDTO.getCorrelationId(),
                    startRequest,
                    buildWorkDTO.toEnvironmentDriverCreateDTO(),
                    cleanBuildEnvOnFailureForEnvironmentDriver);

            Request cleanBuildEnvOnFailureForBuildDriver = getCleanBuildEnvOnFailure(
                    buildWorkDTO,
                    buildDriverAdapter.getRexTaskName(buildWorkDTO.getCorrelationId()));
            CreateTaskDTO taskBuild = buildDriverAdapter
                    .generateRexTask(
                            ownUrl,
                            buildWorkDTO.getCorrelationId(),
                            startRequest,
                            buildWorkDTO.toBuildDriverDTO(),
                            taskRepoSetup.name,
                            cleanBuildEnvOnFailureForBuildDriver);

            CreateTaskDTO taskCompleteEnv = environmentDriverCompleteAdapter.generateRexTask(
                    ownUrl,
                    buildWorkDTO.getCorrelationId(),
                    startRequest,
                    buildWorkDTO.toEnvironmentDriverCompleteDTO());

            CreateTaskDTO taskRepoSeal = repositoryDriverSealAdapter.generateRexTask(
                    ownUrl,
                    buildWorkDTO.getCorrelationId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSealDTO());
            CreateTaskDTO taskRepoPromote = repositoryDriverPromoteAdapter.generateRexTask(
                    ownUrl,
                    buildWorkDTO.getCorrelationId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverPromoteDTO());

            List<CreateTaskDTO> tasks = List.of(
                    taskAdjustReqour,
                    taskRepoSetup,
                    taskCreateEnv,
                    taskBuild,
                    taskCompleteEnv,
                    taskRepoSeal,
                    taskRepoPromote);
            Map<String, CreateTaskDTO> vertices = getVertices(tasks);

            EdgeDTO adjustReqourToRepoSetup = EdgeDTO.builder()
                    .source(taskRepoSetup.name)
                    .target(taskAdjustReqour.name)
                    .build();
            EdgeDTO repoSetupToCreateEnv = EdgeDTO.builder()
                    .source(taskCreateEnv.name)
                    .target(taskRepoSetup.name)
                    .build();
            EdgeDTO createEnvToBuild = EdgeDTO.builder().source(taskBuild.name).target(taskCreateEnv.name).build();
            EdgeDTO adjustReqourToBuild = EdgeDTO.builder()
                    .source(taskBuild.name)
                    .target(taskAdjustReqour.name)
                    .build();
            EdgeDTO buildToCompleteEnv = EdgeDTO.builder().source(taskCompleteEnv.name).target(taskBuild.name).build();

            // WARN: NCL-9060: dependency tasks like reqour adjust are deleted if the taskCompleteEnv has no dependents.
            // Adding that edge artifically so that the dependency tasks are not deleted prematurely
            EdgeDTO completeToRepoSealEnv = EdgeDTO.builder()
                    .source(taskRepoSeal.name)
                    .target(taskCompleteEnv.name)
                    .build();
            EdgeDTO buildToRepoSeal = EdgeDTO.builder().source(taskRepoSeal.name).target(taskBuild.name).build();
            EdgeDTO repoSealToRepoPromote = EdgeDTO.builder()
                    .source(taskRepoPromote.name)
                    .target(taskRepoSeal.name)
                    .build();

            Set<EdgeDTO> edges = Set.of(
                    adjustReqourToRepoSetup,
                    repoSetupToCreateEnv,
                    createEnvToBuild,
                    adjustReqourToBuild,
                    buildToCompleteEnv,
                    completeToRepoSealEnv,
                    buildToRepoSeal,
                    repoSealToRepoPromote);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    buildWorkDTO.getCorrelationId(),
                    rexQueueName,
                    configurationDTO,
                    edges,
                    vertices);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);
            taskEndpoint.start(graphRequest);

            return new CorrelationId(buildWorkDTO.getCorrelationId());

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }

    private Request getCleanBuildEnvOnFailure(BuildWorkDTO buildWorkDTO, String rexTaskName) {
        BuildWorkflowClearEnvironmentDTO buildWorkflowClearEnvironmentDTO = BuildWorkflowClearEnvironmentDTO
                .builder()
                .environmentDriverUrl(buildWorkDTO.getEnvironmentDriverUrl())
                .correlationId(buildWorkDTO.getCorrelationId())
                .rexTaskName(rexTaskName)
                .build();

        return Request.builder()
                .uri(URI.create(this.ownUrl + WorkflowEndpoint.BUILD_CLEAR_ENVIRONMENT))
                .method(Request.Method.POST)
                .headers(TaskHelper.getHTTPHeaders())
                .attachment(buildWorkflowClearEnvironmentDTO)
                .build();
    }

    @Override
    public Response rexNotification(NotificationRequest notificationRequest) {

        if (!NotificationHelper.isFromRunningToFinal(notificationRequest)) {
            // we only care about states from running to final
            return Response.ok().build();
        }

        Log.infof(
                "[%s] -> [%s] :: %s",
                notificationRequest.getBefore(),
                notificationRequest.getAfter(),
                notificationRequest.getTask().getName());

        String correlationId = notificationRequest.getTask().getCorrelationID();
        Set<TaskDTO> tasks = taskEndpoint.byCorrelation(correlationId);

        if (NotificationHelper.areAllRexTasksInFinalState(tasks)) {

            Log.infof("Right now I should be sending a notification to the caller");
            tasks.forEach(taskDTO -> Log.infof("Task: %s, state: %s", taskDTO.getName(), taskDTO.getState()));

            // we set the notification attachment to be the StartRequest in submitWorkflow method
            StartRequest request = objectMapper.convertValue(notificationRequest.getAttachment(), StartRequest.class);
            if (request == null) {
                Log.info("No start request in the notification message");
            } else {
                Log.info("Sending request to rex callback");
                ProcessStageUtils
                        .logProcessStageBegin(ProcessStage.FINALIZING_BUILD.name(), "Submitting final result to Orch");
                BuildResult buildResult = generateBuildResult(request, tasks, correlationId);
                if (buildResult.hasFailed()) {
                    cleanupEnvironmentIfNecessary(tasks, correlationId);
                }
                sendRexCallback(request, buildResult);
            }
        }
        return Response.ok().build();
    }

    /**
     * In case of failure, see if we need to cleanup any remaining environment pod running
     * 
     * @param tasks
     * @param correlationId
     */
    private void cleanupEnvironmentIfNecessary(Set<TaskDTO> tasks, String correlationId) {

        Optional<TaskDTO> buildData = findTask(tasks, buildDriverAdapter.getRexTaskName(correlationId));
        Optional<TaskDTO> environmentData = findTask(
                tasks,
                environmentDriverCreateAdapter.getRexTaskName(correlationId));

        if (buildData.isEmpty() || environmentData.isEmpty()) {
            return;
        }

        TaskDTO buildTask = buildData.get();
        // That's a good sign that the environment pod might still be running
        if (STATE_FAILED.contains(buildTask.getState()) || STATE_FAILED.contains(environmentData.get().getState())) {
            try {
                BuildWorkflowClearEnvironmentDTO dto = objectMapper.convertValue(
                        buildTask.getRemoteRollback().getAttachment(),
                        BuildWorkflowClearEnvironmentDTO.class);
                Log.infof(
                        "Trying to cleanup the environment for correlation: %s due to failed build or environment driver step",
                        correlationId);
                clearEnvironment(dto);
            } catch (Exception e) {
                Log.errorf(
                        "Tried to cleanup the environment due to failed build or environment driver but an exception happened. Giving up. Correlation: %s",
                        correlationId,
                        e);
            }
        }
    }

    /**
     * Try your best to clear the environment. If you cannot, that's ok. Any failure here shouldn't stop the build
     *
     * @param dto
     */
    public void clearEnvironment(BuildWorkflowClearEnvironmentDTO dto) {

        Log.infof("Clearing environment from workflow, possibly due to retries: %s", dto.getCorrelationId());
        try {
            environmentDriverCompleteAdapter
                    .clearEnvironment(dto.getEnvironmentDriverUrl(), dto.getCorrelationId(), false);
        } catch (RuntimeException e) {
            Log.warnf("Clearing environment from workflow, for correlation: %s failed", dto.getCorrelationId());
        }
    }

    private static Map<String, CreateTaskDTO> getVertices(List<CreateTaskDTO> tasks) {
        Map<String, CreateTaskDTO> vertices = new HashMap<>();
        for (CreateTaskDTO task : tasks) {
            vertices.put(task.name, task);
        }
        return vertices;
    }

    private BuildResult generateBuildResult(StartRequest request, Set<TaskDTO> tasks, String correlationId) {

        Optional<AdjustResponse> reqourResult = getReqourResult(tasks, correlationId);
        Optional<RepourResult> repourResult = toRepourResult(reqourResult);

        Optional<CompletionStatus> repoCreateResponse = getRepositoryCreateResponse(tasks, correlationId);

        Optional<EnvironmentDriverResult> environmentDriverResult = getEnvironmentDriverResult(tasks, correlationId);

        Optional<BuildCompleted> buildCompleted = getBuildCompleted(tasks, correlationId);
        Optional<BuildDriverResult> buildDriverResult = getBuildDriverResult(buildCompleted);

        Optional<CompletionStatus> repoSealResponse = getRepositorySealResponse(tasks, correlationId);

        Optional<RepositoryManagerResult> repoResult = getRepositoryManagerResult(tasks, correlationId);

        OverallStatus overallStatus = determineCompletionStatus(
                repoResult,
                buildCompleted,
                repourResult,
                environmentDriverResult,
                repoCreateResponse,
                repoSealResponse);

        // BuildExecutionConfiguration needed for legacy reasons
        // PNC-Orch just extracts the reqour data in buildExecutionConfiguration
        BuildExecutionConfiguration buildExecutionConfiguration = getBuildExecutionConfiguration(reqourResult);
        BuildResult buildResult = new BuildResult(
                overallStatus.completionStatus,
                overallStatus.processException,
                Optional.ofNullable(buildExecutionConfiguration),
                buildDriverResult,
                repoResult,
                environmentDriverResult,
                repourResult);

        Log.infof("Build result: %s", buildResult);

        return buildResult;
    }

    private Optional<CompletionStatus> getRepositoryCreateResponse(Set<TaskDTO> tasks, String correlationId) {
        return getCompletionResponse(tasks, repositoryDriverSetupAdapter.getRexTaskName(correlationId));
    }

    private Optional<CompletionStatus> getRepositorySealResponse(Set<TaskDTO> tasks, String correlationId) {
        return getCompletionResponse(tasks, repositoryDriverSealAdapter.getRexTaskName(correlationId));
    }

    private Optional<CompletionStatus> getCompletionResponse(Set<TaskDTO> tasks, String taskName) {
        Optional<TaskDTO> task = findTask(tasks, taskName);
        if (task.isEmpty()) {
            Log.info(taskName + " is empty");
            return Optional.empty();
        }

        TaskDTO driverTask = task.get();
        if (driverTask.getState() == State.SUCCESSFUL) {
            // everything is good!
            return Optional.of(CompletionStatus.SUCCESS);
        }
        if (STATE_FAILED.contains(driverTask.getState())) {
            return Optional.of(CompletionStatus.SYSTEM_ERROR);
        } else {
            return Optional.empty();
        }
    }

    private Optional<BuildDriverResult> getBuildDriverResult(Optional<BuildCompleted> buildCompleted) {
        BuildDriverResult buildDriverResult = null;

        if (buildCompleted.isPresent()) {
            buildDriverResult = new BuildDriverResult() {
                @Override
                public BuildStatus getBuildStatus() {
                    return BuildStatus.valueOf(buildCompleted.get().getBuildStatus().name());
                }

                @Override
                public Optional<String> getOutputChecksum() {
                    return Optional.empty();
                }
            };
        }
        return Optional.ofNullable(buildDriverResult);
    }

    private Optional<EnvironmentDriverResult> getEnvironmentDriverResult(Set<TaskDTO> tasks, String correlationId) {
        Optional<TaskDTO> task = findTask(tasks, environmentDriverCreateAdapter.getRexTaskName(correlationId));

        if (task.isEmpty()) {
            Log.info("environment driver task is empty");
            return Optional.empty();
        }

        TaskDTO environmentDriverTask = task.get();
        if (environmentDriverTask.getState() == State.SUCCESSFUL) {
            // everything is good!
            return Optional.empty();
        }
        if (STATE_FAILED.contains(environmentDriverTask.getState())) {
            // if we are here, the environment driver failed
            return Optional
                    .of(EnvironmentDriverResult.builder().completionStatus(CompletionStatus.SYSTEM_ERROR).build());
        }

        // step didn't run at all
        return Optional.empty();
    }

    private static BuildExecutionConfiguration getBuildExecutionConfiguration(Optional<AdjustResponse> reqourResult) {
        BuildExecutionConfiguration buildExecutionConfiguration = null;
        if (reqourResult.isPresent()) {
            AdjustResponse reqourResultGet = reqourResult.get();
            if (reqourResultGet.getInternalUrl() != null) {
                buildExecutionConfiguration = new BuildExecutionConfigurationSimplifiedDTO(
                        reqourResultGet.getInternalUrl().getReadonlyUrl(),
                        reqourResultGet.getDownstreamCommit(),
                        reqourResultGet.getTag(),
                        reqourResultGet.getUpstreamCommit(),
                        reqourResultGet.isRefRevisionInternal(),
                        // THis is a hack to get orch to parse build execution configuraiton properly. Oh well!
                        Map.of("id", "0"));
            }
        }
        return buildExecutionConfiguration;
    }

    private static class OverallStatus {
        CompletionStatus completionStatus;
        Optional<ProcessException> processException = Optional.empty();

        void set(CompletionStatus completionStatus, String systemErrorMessage) {
            this.completionStatus = completionStatus;
            if (completionStatus.isFailed()) {
                this.processException = Optional.of(new ProcessException(systemErrorMessage));
            }
        }

        void set(CompletionStatus completionStatus) {
            this.completionStatus = completionStatus;
            this.processException = Optional.empty();
        }
    }

    private OverallStatus determineCompletionStatus(
            Optional<RepositoryManagerResult> repoManagerResult,
            Optional<BuildCompleted> buildCompleted,
            Optional<RepourResult> repourResult,
            Optional<EnvironmentDriverResult> environmentDriverResult,
            Optional<CompletionStatus> repoCreateResponse,
            Optional<CompletionStatus> repoSealResponse) {

        OverallStatus overallStatus = new OverallStatus();

        // let's assume everything is ok, then we go through the results
        overallStatus.set(CompletionStatus.SUCCESS);

        // debug
        if (repoManagerResult.isEmpty()) {
            Log.warn("repository result is empty");
        }
        if (buildCompleted.isEmpty()) {
            Log.warn("build result is empty");
        }
        if (repourResult.isEmpty()) {
            Log.warn("repour result is empty");
        }

        if (repoManagerResult.isEmpty() || repourResult.isEmpty() || buildCompleted.isEmpty()) {
            overallStatus.set(CompletionStatus.FAILED);
        }

        if (repoManagerResult.isPresent() && repoManagerResult.get().getCompletionStatus().isFailed()) {
            overallStatus.set(
                    repoManagerResult.get().getCompletionStatus(),
                    "Failed at repository-result step: " + repoManagerResult.get());
        }

        if (repoSealResponse.isPresent() && repoSealResponse.get().isFailed()) {
            overallStatus.set(repoSealResponse.get(), "Failed at repository-seal step");
        }

        if (buildCompleted.isPresent() && !buildCompleted.get().getBuildStatus().isSuccess()) {
            overallStatus.set(
                    CompletionStatus.valueOf(buildCompleted.get().getBuildStatus().name()),
                    "Failed at build-driver step: " + buildCompleted.get());
        }

        if (environmentDriverResult.isPresent() && environmentDriverResult.get().getCompletionStatus().isFailed()) {
            overallStatus.set(
                    environmentDriverResult.get().getCompletionStatus(),
                    "Failed at environment-driver step: " + environmentDriverResult.get());
        }

        if (repoCreateResponse.isPresent() && repoCreateResponse.get().isFailed()) {
            overallStatus
                    .set(repoCreateResponse.get(), "Failed at repository-create step: " + repoCreateResponse.get());
        }

        if (repourResult.isPresent() && repourResult.get().getCompletionStatus().isFailed()) {
            overallStatus.set(repourResult.get().getCompletionStatus(), "Failed at reqour step: " + repourResult.get());
        }

        return overallStatus;
    }

    private void sendRexCallback(StartRequest startRequest, BuildResult buildResult) {
        Request callback;
        if (buildResult.getCompletionStatus().isFailed()) {
            callback = startRequest.getNegativeCallback();
        } else {
            callback = startRequest.getPositiveCallback();
        }
        Log.infof("Final build callback sent to: %s", callback.getUri().toString());

        Request toSend = Request.builder()
                .method(callback.getMethod())
                .uri(callback.getUri())
                .headers(callback.getHeaders())
                .attachment(buildResult)
                .build();
        genericClient.send(toSend);
    }

    private Optional<RepourResult> toRepourResult(Optional<AdjustResponse> response) {

        if (response.isEmpty()) {
            return Optional.empty();
        }
        AdjustResponse adjustResponse = response.get();

        RepourResult repourResult;
        if (adjustResponse.getCallback().getStatus().isSuccess()) {
            repourResult = RepourResult.builder()
                    .completionStatus(CompletionStatus.valueOf(adjustResponse.getCallback().getStatus().name()))
                    .executionRootName(
                            adjustResponse.getManipulatorResult().getVersioningState().getExecutionRootName())
                    .executionRootVersion(
                            adjustResponse.getManipulatorResult().getVersioningState().getExecutionRootVersion())
                    .build();
        } else {
            repourResult = RepourResult.builder()
                    .completionStatus(CompletionStatus.valueOf(adjustResponse.getCallback().getStatus().name()))
                    .build();
        }

        return Optional.of(repourResult);

    }

    private Optional<AdjustResponse> getReqourResult(Set<TaskDTO> tasks, String correlationId) {
        Optional<TaskDTO> task = findTask(tasks, reqourAdjustAdapter.getRexTaskName(correlationId));

        if (task.isEmpty()) {
            Log.info("reqour task is empty");
            return Optional.empty();
        }

        TaskDTO reqourTask = task.get();
        List<ServerResponseDTO> responses = reqourTask.getServerResponses();

        if (responses.isEmpty()) {
            Log.info("repour response task is empty");
            return Optional.empty();
        }

        ServerResponseDTO finalResponse = responses.get(responses.size() - 1);
        AdjustResponse response = objectMapper.convertValue(finalResponse.getBody(), AdjustResponse.class);

        if (response == null) {
            ReqourCallback callback = ReqourCallback.builder()
                    .status(ResultStatus.FAILED)
                    .build();
            response = AdjustResponse.builder()
                    .callback(callback)
                    .build();
        }

        return Optional.ofNullable(response);
    }

    private Optional<BuildCompleted> getBuildCompleted(Set<TaskDTO> tasks, String correlationId) {
        Optional<TaskDTO> task = findTask(tasks, buildDriverAdapter.getRexTaskName(correlationId));
        if (task.isEmpty()) {
            Log.info("build driver task is empty");
            return Optional.empty();
        }

        TaskDTO buildTask = task.get();
        List<ServerResponseDTO> responses = buildTask.getServerResponses();

        if (responses.isEmpty()) {
            Log.info("build driver response task is empty");
            return Optional.empty();
        }

        ServerResponseDTO finalResponse = responses.get(responses.size() - 1);
        BuildCompleted response = objectMapper.convertValue(finalResponse.getBody(), BuildCompleted.class);

        if (response == null && STATE_FAILED.contains(buildTask.getState())) {
            Log.info("build completed response task is empty and task failed");
            response = BuildCompleted.builder().buildStatus(ResultStatus.FAILED).build();
        }

        return Optional.ofNullable(response);
    }

    private Optional<RepositoryManagerResult> getRepositoryManagerResult(Set<TaskDTO> tasks, String correlationId) {

        Optional<TaskDTO> task = findTask(tasks, repositoryDriverPromoteAdapter.getRexTaskName(correlationId));

        if (task.isEmpty()) {
            Log.infof(
                    "Repo promote task is supposed to be: %s",
                    repositoryDriverPromoteAdapter.getRexTaskName(correlationId));
            for (TaskDTO taskTemp : tasks) {
                Log.infof("Present: task: %s", taskTemp.getName());
            }
            Log.info("repository manager task is empty");
            return Optional.empty();
        }

        TaskDTO repoTask = task.get();
        List<ServerResponseDTO> responses = repoTask.getServerResponses();

        if (responses.isEmpty()) {
            Log.info("repository manager response task is empty");
            return Optional.empty();
        }

        ServerResponseDTO finalResponse = responses.get(responses.size() - 1);
        RepositoryPromoteResult response = objectMapper
                .convertValue(finalResponse.getBody(), RepositoryPromoteResult.class);

        if (response == null) {
            Log.info("repository manager server response task is empty");
            return Optional.empty();
        }
        RepositoryManagerResult result = new RepositoryManagerResult() {
            @Override
            public List<Artifact> getBuiltArtifacts() {
                return convertFromRepositoryArtifacts(response.getBuiltArtifacts());
            }

            @Override
            public List<Artifact> getDependencies() {
                return convertFromRepositoryArtifacts(response.getDependencies());
            }

            @Override
            public String getBuildContentId() {
                return response.getBuildContentId();
            }

            @Override
            public CompletionStatus getCompletionStatus() {
                return CompletionStatus.valueOf(response.getStatus().name());
            }
        };

        return Optional.of(result);

    }

    private Optional<TaskDTO> findTask(Set<TaskDTO> tasks, String name) {
        return tasks.stream().filter(task -> task.getName().equals(name)).findFirst();
    }

    private List<Artifact> convertFromRepositoryArtifacts(List<RepositoryArtifact> artifacts) {
        if (artifacts == null) {
            return Collections.emptyList();
        }
        return artifacts.stream()
                .map(
                        ra -> Artifact.builder()
                                .identifier(ra.getIdentifier())
                                .purl(ra.getPurl())
                                .artifactQuality(convertArtifactQuality(ra.getArtifactQuality()))
                                .buildCategory(convertBuildCategory(ra.getBuildCategory()))
                                .md5(ra.getMd5())
                                .sha1(ra.getSha1())
                                .sha256(ra.getSha256())
                                .filename(ra.getFilename())
                                .deployPath(ra.getDeployPath())
                                .importDate(ra.getImportDate() == null ? null : Date.from(ra.getImportDate()))
                                .originUrl(ra.getOriginUrl())
                                .size(ra.getSize())
                                .targetRepository(convertTargetRepository(ra.getTargetRepository()))
                                .build())
                .collect(Collectors.toList());
    }

    private org.jboss.pnc.enums.ArtifactQuality convertArtifactQuality(ArtifactQuality quality) {
        return quality == null ? null : org.jboss.pnc.enums.ArtifactQuality.valueOf(quality.name());
    }

    private TargetRepository convertTargetRepository(
            org.jboss.pnc.api.repositorydriver.dto.TargetRepository targetRepository) {
        if (targetRepository == null) {
            return null;
        }
        return TargetRepository.newBuilder()
                .temporaryRepo(targetRepository.getTemporaryRepo())
                .identifier(targetRepository.getIdentifier())
                .repositoryType(convertRepositoryType(targetRepository.getRepositoryType()))
                .repositoryPath(targetRepository.getRepositoryPath())
                .build();
    }

    private RepositoryType convertRepositoryType(org.jboss.pnc.api.enums.RepositoryType repositoryType) {
        if (repositoryType == null) {
            return null;
        }
        return RepositoryType.valueOf(repositoryType.name());
    }

    private org.jboss.pnc.enums.BuildCategory convertBuildCategory(BuildCategory buildCategory) {
        if (buildCategory == null) {
            return null;
        }
        return org.jboss.pnc.enums.BuildCategory.valueOf(buildCategory.name());
    }
}
