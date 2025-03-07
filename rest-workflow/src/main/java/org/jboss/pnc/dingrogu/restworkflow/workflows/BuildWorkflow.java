package org.jboss.pnc.dingrogu.restworkflow.workflows;

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
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.ArtifactQuality;
import org.jboss.pnc.api.enums.BuildCategory;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryArtifact;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteResult;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildExecutionConfigurationSimplifiedDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
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

    @Override
    public CorrelationId submitWorkflow(BuildWorkDTO buildWorkDTO) throws WorkflowSubmissionException {
        CorrelationId correlationId;
        if (buildWorkDTO.getCorrelationId() == null) {
            correlationId = CorrelationId.generateUnique();
        } else {
            correlationId = new CorrelationId(buildWorkDTO.getCorrelationId());
        }

        try {
            CreateTaskDTO taskAdjustReqour = reqourAdjustAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO, buildWorkDTO.toReqourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repositoryDriverSetupAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toRepositoryDriverSetupDTO());

            CreateTaskDTO taskCreateEnv = environmentDriverCreateAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toEnvironmentDriverCreateDTO());
            CreateTaskDTO taskBuild = buildDriverAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO, buildWorkDTO.toBuildDriverDTO());
            CreateTaskDTO taskCompleteEnv = environmentDriverCompleteAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toEnvironmentDriverCompleteDTO());

            CreateTaskDTO taskRepoSeal = repositoryDriverSealAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toRepositoryDriverSealDTO());
            CreateTaskDTO taskRepoPromote = repositoryDriverPromoteAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
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
                    buildToRepoSeal,
                    repoSealToRepoPromote);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    rexQueueName,
                    configurationDTO,
                    edges,
                    vertices);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);
            taskEndpoint.start(graphRequest);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
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
        CorrelationId correlationId = CorrelationId.generateUnique();

        try {
            CreateTaskDTO taskAdjustReqour = reqourAdjustAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), startRequest, buildWorkDTO.toReqourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repositoryDriverSetupAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSetupDTO());

            CreateTaskDTO taskCreateEnv = environmentDriverCreateAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toEnvironmentDriverCreateDTO());
            CreateTaskDTO taskBuild = buildDriverAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), startRequest, buildWorkDTO.toBuildDriverDTO());
            CreateTaskDTO taskCompleteEnv = environmentDriverCompleteAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toEnvironmentDriverCompleteDTO());

            CreateTaskDTO taskRepoSeal = repositoryDriverSealAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSealDTO());
            CreateTaskDTO taskRepoPromote = repositoryDriverPromoteAdapter.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
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
                    buildToRepoSeal,
                    repoSealToRepoPromote);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    rexQueueName,
                    configurationDTO,
                    edges,
                    vertices);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);
            taskEndpoint.start(graphRequest);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
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
                BuildResult buildResult = generateBuildResult(request, tasks, correlationId);
                sendRexCallback(request, buildResult);
            }
        }
        return Response.ok().build();
    }

    private static Map<String, CreateTaskDTO> getVertices(List<CreateTaskDTO> tasks) {
        Map<String, CreateTaskDTO> vertices = new HashMap<>();
        for (CreateTaskDTO task : tasks) {
            vertices.put(task.name, task);
        }
        return vertices;
    }

    private BuildResult generateBuildResult(StartRequest request, Set<TaskDTO> tasks, String correlationId) {

        Optional<RepositoryManagerResult> repoResult = getRepositoryManagerResult(tasks, correlationId);
        Optional<AdjustResponse> reqourResult = getReqourResult(tasks, correlationId);
        Optional<RepourResult> repourResult = toRepourResult(reqourResult);
        CompletionStatus completionStatus = determineCompletionStatus(repoResult, repourResult);
        BuildDriverResult buildDriverResult = null;
        if (completionStatus.isFailed()) {
            buildDriverResult = new BuildDriverResult() {
                @Override
                public BuildStatus getBuildStatus() {
                    return BuildStatus.FAILED;
                }

                @Override
                public Optional<String> getOutputChecksum() {
                    return Optional.empty();
                }
            };
        }
        // BuildExecutionConfiguration needed for legacy reasons
        // PNC-Orch just extracts the reqour data in buildExecutionConfiguration
        BuildExecutionConfiguration buildExecutionConfiguration = getBuildExecutionConfiguration(reqourResult);
        BuildResult buildResult = new BuildResult(
                completionStatus,
                Optional.empty(),
                Optional.ofNullable(buildExecutionConfiguration),
                Optional.ofNullable(buildDriverResult),
                repoResult,
                Optional.empty(),
                repourResult);

        Log.infof("Build result: %s", buildResult);

        return buildResult;
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

    private CompletionStatus determineCompletionStatus(
            Optional<RepositoryManagerResult> repoResult,
            Optional<RepourResult> repourResult) {
        // debug
        if (repoResult.isEmpty()) {
            Log.warn("repository result is empty");
        }
        if (repourResult.isEmpty()) {
            Log.warn("repour result is empty");
        }
        if (repoResult.isEmpty() || repourResult.isEmpty()) {
            return CompletionStatus.FAILED;
        }
        if (repourResult.get().getCompletionStatus().isFailed()) {
            return repourResult.get().getCompletionStatus();
        }
        if (repoResult.get().getCompletionStatus().isFailed()) {
            return repoResult.get().getCompletionStatus();
        }

        // if we are here, everything succeeded!
        return CompletionStatus.SUCCESS;
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
            Log.info("repour task is empty");
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

    private List<Artifact> convertFromRepositoryArtifacts(List<RepositoryArtifact> builtArtifacts) {
        if (builtArtifacts == null) {
            return Collections.emptyList();
        }
        return builtArtifacts.stream()
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
                                .importDate(Date.from(ra.getImportDate()))
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
