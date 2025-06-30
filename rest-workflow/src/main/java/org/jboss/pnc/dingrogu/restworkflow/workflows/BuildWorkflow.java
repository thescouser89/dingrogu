package org.jboss.pnc.dingrogu.restworkflow.workflows;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.builddriver.dto.BuildCompleted;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteResult;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.api.reqour.dto.ReqourCallback;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.EnvironmentDriverCreateDTO;
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
import org.jboss.pnc.dingrogu.restworkflow.workflows.helpers.ConverterHelper;
import org.jboss.pnc.dingrogu.restworkflow.workflows.helpers.OverallStatus;
import org.jboss.pnc.dingrogu.restworkflow.workflows.helpers.TaskResponse;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Origin;
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
import org.jboss.pnc.spi.environment.EnvironmentDriverResult;
import org.jboss.pnc.spi.executor.BuildExecutionConfiguration;
import org.jboss.pnc.spi.repositorymanager.RepositoryManagerResult;
import org.jboss.pnc.spi.repour.RepourResult;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private static final Set<State> STATE_FAILED = Set
            .of(State.FAILED, State.START_FAILED, State.STOP_FAILED, State.ROLLBACK_FAILED);

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
                    // set default value for heartbeat delay and interval to all the tasks
                    .heartbeatInitialDelay(Duration.ofMinutes(2))
                    .heartbeatInterval(Duration.ofSeconds(30))
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

            String buildId = MDC.get(MDCHeaderKeys.BUILD_ID.getMdcKey());
            Log.infof("Right now I should be sending a notification to the caller for buildid: %s", buildId);
            tasks.forEach(taskDTO -> {
                Log.infof("Task: %s, state: %s", taskDTO.getName(), taskDTO.getState());
                if (STATE_FAILED.contains(taskDTO.getState())) {
                    try {
                        Log.infof(objectMapper.writeValueAsString(taskDTO));
                    } catch (JsonProcessingException e) {
                        // do nothing
                    }
                }
            });

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
            if (isDebugEnabled(environmentData.get())) {
                Log.infof("Debug enabled for the pod. Not deleting it");
                return;
            }
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

        TaskResponse<AdjustResponse> reqourResult = getReqourResult(tasks, correlationId);
        TaskResponse<RepourResult> repourResult = toRepourResult(reqourResult);

        TaskResponse<CompletionStatus> repoCreateResponse = getRepositoryCreateResponse(tasks, correlationId);

        TaskResponse<EnvironmentDriverResult> environmentDriverResult = getEnvironmentDriverResult(
                tasks,
                correlationId);

        TaskResponse<BuildCompleted> buildCompleted = getBuildCompleted(tasks, correlationId);
        TaskResponse<BuildDriverResult> buildDriverResult = getBuildDriverResult(buildCompleted);

        TaskResponse<CompletionStatus> repoSealResponse = getRepositorySealResponse(tasks, correlationId);

        TaskResponse<RepositoryManagerResult> repoResult = getRepositoryManagerResult(tasks, correlationId);

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
                buildDriverResult.getDTO(),
                repoResult.getDTO(),
                environmentDriverResult.getDTO(),
                repourResult.getDTO());

        try {
            Log.infof("Build result: %s", objectMapper.writeValueAsString(buildResult));
        } catch (JsonProcessingException e) {
            // do nothing
        }

        return buildResult;
    }

    private static BuildExecutionConfiguration getBuildExecutionConfiguration(
            TaskResponse<AdjustResponse> reqourResult) {
        BuildExecutionConfiguration buildExecutionConfiguration = null;
        if (reqourResult.getDTO().isPresent()) {
            AdjustResponse reqourResultGet = reqourResult.getDTO().get();
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

    private OverallStatus determineCompletionStatus(
            TaskResponse<RepositoryManagerResult> repoManagerResult,
            TaskResponse<BuildCompleted> buildCompleted,
            TaskResponse<RepourResult> repourResult,
            TaskResponse<EnvironmentDriverResult> environmentDriverResult,
            TaskResponse<CompletionStatus> repoCreateResponse,
            TaskResponse<CompletionStatus> repoSealResponse) {

        OverallStatus overallStatus = new OverallStatus();

        // let's assume everything is ok, then we go through the results
        overallStatus.set(CompletionStatus.SUCCESS);

        // debug
        if (repoManagerResult.getDTO().isEmpty()) {
            Log.warn("repository result is empty");
        }
        if (buildCompleted.getDTO().isEmpty()) {
            Log.warn("build result is empty");
        }
        if (repourResult.getDTO().isEmpty()) {
            Log.warn("repour result is empty");
        }

        if (repoManagerResult.getDTO().isEmpty() || repourResult.getDTO().isEmpty()
                || buildCompleted.getDTO().isEmpty()) {
            overallStatus.set(CompletionStatus.FAILED);
        }

        if (repoManagerResult.getDTO().isPresent()
                && repoManagerResult.getDTO().get().getCompletionStatus().isFailed()) {

            overallStatus.set(
                    repoManagerResult.getDTO().get().getCompletionStatus(),
                    repoManagerResult.addToErrorMessage("Failed at repository-result step"));
        }

        if (repoSealResponse.getDTO().isPresent() && repoSealResponse.getDTO().get().isFailed()) {
            overallStatus.set(
                    repoSealResponse.getDTO().get(),
                    repoSealResponse.addToErrorMessage("Failed at repository-seal step"));
        }

        if (buildCompleted.getDTO().isPresent() && !buildCompleted.getDTO().get().getBuildStatus().isSuccess()) {
            overallStatus.set(
                    CompletionStatus.valueOf(buildCompleted.getDTO().get().getBuildStatus().name()),
                    buildCompleted.addToErrorMessage("Failed at build-driver step"));
        }

        if (environmentDriverResult.getDTO().isPresent()
                && environmentDriverResult.getDTO().get().getCompletionStatus() != null
                && environmentDriverResult.getDTO().get().getCompletionStatus().isFailed()) {
            overallStatus.set(
                    environmentDriverResult.getDTO().get().getCompletionStatus(),
                    environmentDriverResult.addToErrorMessage("Failed at environment-driver step"));
        }

        if (repoCreateResponse.getDTO().isPresent() && repoCreateResponse.getDTO().get().isFailed()) {
            overallStatus
                    .set(
                            repoCreateResponse.getDTO().get(),
                            repoCreateResponse.addToErrorMessage("Failed at repository-create step"));
        }

        if (repourResult.getDTO().isPresent() && repourResult.getDTO().get().getCompletionStatus().isFailed()) {
            overallStatus.set(
                    repourResult.getDTO().get().getCompletionStatus(),
                    repourResult.addToErrorMessage("Failed at reqour step"));
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

    private TaskResponse<CompletionStatus> getRepositoryCreateResponse(Set<TaskDTO> tasks, String correlationId) {
        return getCompletionResponse(tasks, repositoryDriverSetupAdapter.getRexTaskName(correlationId));
    }

    private TaskResponse<CompletionStatus> getRepositorySealResponse(Set<TaskDTO> tasks, String correlationId) {
        return getCompletionResponse(tasks, repositoryDriverSealAdapter.getRexTaskName(correlationId));
    }

    private TaskResponse<CompletionStatus> getCompletionResponse(Set<TaskDTO> tasks, String taskName) {

        Optional<TaskDTO> optionalTask = findTask(tasks, taskName);

        if (optionalTask.isEmpty()) {
            String errorMessage = taskName + " is empty";
            Log.error(errorMessage);
            return new TaskResponse<>(CompletionStatus.SYSTEM_ERROR, errorMessage);
        }

        TaskDTO task = optionalTask.get();
        if (task.getState() == State.SUCCESSFUL) {
            // everything is good!
            return new TaskResponse<>(CompletionStatus.SUCCESS);
        }
        if (STATE_FAILED.contains(task.getState())) {
            return new TaskResponse<>(
                    CompletionStatus.SYSTEM_ERROR,
                    taskName + " failed with status: " + task.getState());
        } else {
            return new TaskResponse<>(null, null);
        }
    }

    private TaskResponse<BuildDriverResult> getBuildDriverResult(TaskResponse<BuildCompleted> buildCompleted) {

        BuildDriverResult buildDriverResult = null;

        if (buildCompleted.getDTO().isPresent()) {
            buildDriverResult = new BuildDriverResult() {
                @Override
                public BuildStatus getBuildStatus() {
                    return BuildStatus.valueOf(buildCompleted.getDTO().get().getBuildStatus().name());
                }

                @Override
                public Optional<String> getOutputChecksum() {
                    return Optional.empty();
                }
            };
        }
        return new TaskResponse<>(buildDriverResult, buildCompleted.errorMessage);
    }

    private TaskResponse<EnvironmentDriverResult> getEnvironmentDriverResult(Set<TaskDTO> tasks, String correlationId) {

        EnvironmentDriverResult failedResponse = EnvironmentDriverResult.builder()
                .completionStatus(CompletionStatus.SYSTEM_ERROR)
                .build();
        return getTaskResult(
                tasks,
                environmentDriverCreateAdapter.getRexTaskName(correlationId),
                failedResponse,
                EnvironmentDriverResult.class);
    }

    private TaskResponse<RepourResult> toRepourResult(TaskResponse<AdjustResponse> response) {

        if (response.getDTO().isEmpty()) {
            return new TaskResponse<>(null, response.errorMessage);
        }

        AdjustResponse adjustResponse = response.getDTO().get();

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

        return new TaskResponse<>(repourResult, response.errorMessage);
    }

    private TaskResponse<AdjustResponse> getReqourResult(Set<TaskDTO> tasks, String correlationId) {

        ReqourCallback failedCallback = ReqourCallback.builder()
                .status(ResultStatus.FAILED)
                .build();

        AdjustResponse failedResponse = AdjustResponse.builder()
                .callback(failedCallback)
                .build();

        return getTaskResult(
                tasks,
                reqourAdjustAdapter.getRexTaskName(correlationId),
                failedResponse,
                AdjustResponse.class);
    }

    private TaskResponse<BuildCompleted> getBuildCompleted(Set<TaskDTO> tasks, String correlationId) {
        BuildCompleted failedResponse = BuildCompleted.builder().buildStatus(ResultStatus.SYSTEM_ERROR).build();
        return getTaskResult(
                tasks,
                buildDriverAdapter.getRexTaskName(correlationId),
                failedResponse,
                BuildCompleted.class,
                "Builder pod has failed to start multiple times.");
    }

    private TaskResponse<RepositoryManagerResult> getRepositoryManagerResult(Set<TaskDTO> tasks, String correlationId) {

        RepositoryPromoteResult failedResponse = RepositoryPromoteResult.builder()
                .buildContentId("")
                .builtArtifacts(Collections.emptyList())
                .dependencies(Collections.emptyList())
                .status(ResultStatus.SYSTEM_ERROR)
                .build();

        TaskResponse<RepositoryPromoteResult> response = getTaskResult(
                tasks,
                repositoryDriverPromoteAdapter.getRexTaskName(correlationId),
                failedResponse,
                RepositoryPromoteResult.class);

        // PNC-Orch wants RepositoryManagerResult, not RepositoryPromoteResult. We need to convert
        RepositoryManagerResult result = null;

        if (response.getDTO().isPresent()) {
            result = new RepositoryManagerResult() {
                @Override
                public List<Artifact> getBuiltArtifacts() {
                    return ConverterHelper.convertFromRepositoryArtifacts(response.getDTO().get().getBuiltArtifacts());
                }

                @Override
                public List<Artifact> getDependencies() {
                    return ConverterHelper.convertFromRepositoryArtifacts(response.getDTO().get().getDependencies());
                }

                @Override
                public String getBuildContentId() {
                    return response.getDTO().get().getBuildContentId();
                }

                @Override
                public CompletionStatus getCompletionStatus() {
                    return CompletionStatus.valueOf(response.getDTO().get().getStatus().name());
                }
            };
        }

        return new TaskResponse<>(result, response.errorMessage);
    }

    /**
     * Helper method to return a task response and set an appropriate error message on failure
     *
     * @param tasks
     * @param rexTaskName
     * @param failedResponse
     * @param clazz
     * @return
     * @param <T>
     */
    private <T> TaskResponse<T> getTaskResult(
            Set<TaskDTO> tasks,
            String rexTaskName,
            T failedResponse,
            Class<T> clazz) {
        return getTaskResult(tasks, rexTaskName, failedResponse, clazz, "Task retried multiple times.");
    }

    /**
     * Helper method to return a task response and set an appropriate error message on failure. You can specify a custom
     * error message when the task has been retried multiple times and still failed.
     * 
     * @param tasks
     * @param rexTaskName
     * @param failedResponse
     * @param clazz
     * @param multipleRollbackErrorMessage
     * @return
     * @param <T>
     */
    private <T> TaskResponse<T> getTaskResult(
            Set<TaskDTO> tasks,
            String rexTaskName,
            T failedResponse,
            Class<T> clazz,
            String multipleRollbackErrorMessage) {

        Optional<TaskDTO> optionalTask = findTask(tasks, rexTaskName);

        if (optionalTask.isEmpty()) {
            return new TaskResponse<>(failedResponse, rexTaskName + " task is not present");
        }

        TaskDTO task = optionalTask.get();

        // get responses from the caller only
        List<ServerResponseDTO> responses = task.getServerResponses()
                .stream()
                .filter(
                        response -> response.getOrigin().equals(Origin.REMOTE_ENTITY)
                                && response.getState().equals(State.UP))
                .toList();

        int rollbackCounter = 0;
        List<ServerResponseDTO> allResponses = task.getServerResponses();
        if (allResponses != null && !allResponses.isEmpty()) {
            rollbackCounter = allResponses.get(allResponses.size() - 1).rollbackCounter.intValue();
        }

        if (responses.isEmpty()) {
            if (STATE_FAILED.contains(task.getState())) {
                String errorMessage = rexTaskName + " response task is empty:: rollbackCounter=" + rollbackCounter;
                if (rollbackCounter > 1) {
                    multipleRollbackErrorMessage = multipleRollbackErrorMessage == null ? " Retried multiple times"
                            : multipleRollbackErrorMessage;
                    errorMessage += "\n" + multipleRollbackErrorMessage;
                }
                Log.error(errorMessage);
                return new TaskResponse<>(failedResponse, errorMessage);
            } else {
                return new TaskResponse<>(null, null);
            }
        }

        ServerResponseDTO finalResponse = responses.get(responses.size() - 1);
        try {
            T response = objectMapper.convertValue(finalResponse.getBody(), clazz);
            if (response == null && STATE_FAILED.contains(task.getState())) {
                return new TaskResponse<>(
                        failedResponse,
                        rexTaskName + " didn't start properly: " + task.getState() + " :: " + finalResponse);
            }
            return new TaskResponse<>(response);
        } catch (IllegalArgumentException e) {
            return new TaskResponse<>(
                    failedResponse,
                    rexTaskName + " response task cannot be parsed to " + clazz.getName() + ": "
                            + finalResponse.getBody());
        }
    }

    /**
     * Helper method to find the right task
     * 
     * @param tasks
     * @param name
     * @return
     */
    private Optional<TaskDTO> findTask(Set<TaskDTO> tasks, String name) {
        return tasks.stream().filter(task -> task.getName().equals(name)).findFirst();
    }

    private boolean isDebugEnabled(TaskDTO taskDTO) {
        try {
            EnvironmentDriverCreateDTO request = objectMapper
                    .convertValue(taskDTO.getRemoteStart().getAttachment(), EnvironmentDriverCreateDTO.class);
            return request.isDebugEnabled();
        } catch (IllegalArgumentException e) {
            Log.error(e.getMessage());
            return false;
        }
    }
}
