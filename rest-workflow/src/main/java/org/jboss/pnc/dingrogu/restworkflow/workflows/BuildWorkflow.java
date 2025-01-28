package org.jboss.pnc.dingrogu.restworkflow.workflows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
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
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverPromoteAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverSealAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverSetupAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepourAdjustAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourAdjustAdapter;
import org.jboss.pnc.dingrogu.restadapter.client.GenericClient;
import org.jboss.pnc.enums.RepositoryType;
import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.model.TargetRepository;
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
import org.jboss.pnc.spi.coordinator.CompletionStatus;
import org.jboss.pnc.spi.repositorymanager.RepositoryManagerResult;
import org.jboss.pnc.spi.repour.RepourResult;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build process workflow implementation
 */
@ApplicationScoped
public class BuildWorkflow implements Workflow<BuildWorkDTO> {

    @Inject
    RepourAdjustAdapter repour;

    @Inject
    ReqourAdjustAdapter reqour;

    @Inject
    RepositoryDriverSetupAdapter repoSetup;

    @Inject
    RepositoryDriverSealAdapter repoSeal;

    @Inject
    RepositoryDriverPromoteAdapter repoPromote;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GenericClient genericClient;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Override
    public CorrelationId submitWorkflow(BuildWorkDTO buildWorkDTO) throws WorkflowSubmissionException {
        CorrelationId correlationId;
        if (buildWorkDTO.getCorrelationId() == null) {
            correlationId = CorrelationId.generateUnique();
        } else {
            correlationId = new CorrelationId(buildWorkDTO.getCorrelationId());
        }

        try {
            CreateTaskDTO taskAlign = repour
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO, buildWorkDTO.toRepourAdjustDTO());
            CreateTaskDTO taskAlignReqour = reqour
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO, buildWorkDTO.toReqourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repoSetup.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toRepositoryDriverSetupDTO());
            CreateTaskDTO taskRepoSeal = repoSeal.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toRepositoryDriverSealDTO());
            CreateTaskDTO taskRepoPromote = repoPromote.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    buildWorkDTO,
                    buildWorkDTO.toRepositoryDriverPromoteDTO());

            List<CreateTaskDTO> tasks = List.of(taskAlignReqour, taskRepoSetup, taskRepoSeal, taskRepoPromote);
            Map<String, CreateTaskDTO> vertices = getVertices(tasks);

            EdgeDTO alignToRepoSetup = EdgeDTO.builder()
                    .source(taskRepoSetup.name)
                    .target(taskAlignReqour.name)
                    .build();

            // Temporary: testing
            EdgeDTO repoSetupToRepoSeal = EdgeDTO.builder()
                    .source(taskRepoSeal.name)
                    .target(taskRepoSetup.name)
                    .build();
            EdgeDTO repoSealToRepoPromote = EdgeDTO.builder()
                    .source(taskRepoPromote.name)
                    .target(taskRepoSeal.name)
                    .build();

            Set<EdgeDTO> edges = Set.of(alignToRepoSetup, repoSetupToRepoSeal, repoSealToRepoPromote);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    configurationDTO,
                    edges,
                    vertices);
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
            CreateTaskDTO taskAlignReqour = reqour
                    .generateRexTask(ownUrl, correlationId.getId(), startRequest, buildWorkDTO.toReqourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repoSetup.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSetupDTO());
            CreateTaskDTO taskRepoSeal = repoSeal.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverSealDTO());
            CreateTaskDTO taskRepoPromote = repoPromote.generateRexTask(
                    ownUrl,
                    correlationId.getId(),
                    startRequest,
                    buildWorkDTO.toRepositoryDriverPromoteDTO());

            List<CreateTaskDTO> tasks = List.of(taskAlignReqour, taskRepoSetup, taskRepoSeal, taskRepoPromote);
            Map<String, CreateTaskDTO> vertices = getVertices(tasks);

            EdgeDTO alignToRepoSetup = EdgeDTO.builder()
                    .source(taskRepoSetup.name)
                    .target(taskAlignReqour.name)
                    .build();

            // Temporary: testing
            EdgeDTO repoSetupToRepoSeal = EdgeDTO.builder()
                    .source(taskRepoSeal.name)
                    .target(taskRepoSetup.name)
                    .build();
            EdgeDTO repoSealToRepoPromote = EdgeDTO.builder()
                    .source(taskRepoPromote.name)
                    .target(taskRepoSeal.name)
                    .build();

            Set<EdgeDTO> edges = Set.of(alignToRepoSetup, repoSetupToRepoSeal, repoSealToRepoPromote);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    configurationDTO,
                    edges,
                    vertices);
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
                BuildResult buildResult = generateBuildResult(
                        tasks,
                        objectMapper.convertValue(request.getPayload(), BuildWorkDTO.class));
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

    private BuildResult generateBuildResult(Set<TaskDTO> tasks, BuildWorkDTO buildWorkDTO) {
        Optional<RepositoryManagerResult> repoResult = getRepositoryManagerResult(
                tasks,
                buildWorkDTO.getCorrelationId());
        Optional<RepourResult> repourResult = getReqourResult(tasks, buildWorkDTO.getCorrelationId());
        CompletionStatus completionStatus = determineCompletionStatus(repoResult, repourResult);
        BuildResult buildResult = new BuildResult(
                completionStatus,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                repoResult,
                Optional.empty(),
                repourResult);

        return buildResult;
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

    private Optional<RepourResult> getReqourResult(Set<TaskDTO> tasks, String correlationId) {
        Optional<TaskDTO> task = findTask(tasks, reqour.getRexTaskName(correlationId));

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

        if (response == null) {
            Log.info("adjustresponse response task is empty");
            return Optional.empty();
        }

        RepourResult repourResult;
        if (response.getCallback().getStatus().isSuccess()) {
            repourResult = RepourResult.builder()
                    .completionStatus(CompletionStatus.valueOf(response.getCallback().getStatus().name()))
                    .executionRootName(response.getManipulatorResult().getVersioningState().getExecutionRootName())
                    .executionRootVersion(
                            response.getManipulatorResult().getVersioningState().getExecutionRootVersion())
                    .build();
        } else {
            repourResult = RepourResult.builder()
                    .completionStatus(CompletionStatus.valueOf(response.getCallback().getStatus().name()))
                    .build();
        }

        return Optional.of(repourResult);
    }

    private Optional<RepositoryManagerResult> getRepositoryManagerResult(Set<TaskDTO> tasks, String correlationId) {

        Optional<TaskDTO> task = findTask(tasks, repoPromote.getRexTaskName(correlationId));

        if (task.isEmpty()) {
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
