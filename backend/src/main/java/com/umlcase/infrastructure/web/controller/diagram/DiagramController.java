package com.umlcase.infrastructure.web.controller.diagram;

import com.umlcase.application.command.diagram.SaveNodeViewCommand;
import com.umlcase.application.port.in.diagram.GetDiagramLayoutUseCase;
import com.umlcase.application.port.in.diagram.SaveNodeViewUseCase;
import com.umlcase.application.query.diagram.GetDiagramLayoutQuery;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import com.umlcase.infrastructure.web.dto.diagram.GetDiagramLayoutResponse;
import com.umlcase.infrastructure.web.dto.diagram.SaveNodeViewRequest;
import com.umlcase.infrastructure.web.dto.diagram.SaveNodeViewResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/diagram")
public class DiagramController {

    private final GetDiagramLayoutUseCase getDiagramLayoutUseCase;
    private final SaveNodeViewUseCase saveNodeViewUseCase;

    public DiagramController(GetDiagramLayoutUseCase getDiagramLayoutUseCase, SaveNodeViewUseCase saveNodeViewUseCase) {
        this.getDiagramLayoutUseCase = getDiagramLayoutUseCase;
        this.saveNodeViewUseCase = saveNodeViewUseCase;
    }

    @GetMapping
    public ResponseEntity<GetDiagramLayoutResponse> getDiagramLayout(@PathVariable String projectId) {
        UmlDiagramLayout layout = getDiagramLayoutUseCase.execute(new GetDiagramLayoutQuery(projectId));

        GetDiagramLayoutResponse response = GetDiagramLayoutResponse.builder()
                .projectId(layout.getProjectId())
                .layoutVersion(layout.getVersion())
                .nodeViews(layout.getNodeViews().stream()
                        .map(nv -> GetDiagramLayoutResponse.NodeViewDto.builder()
                                .classId(nv.getClassId())
                                .x(nv.getX())
                                .y(nv.getY())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(response);
    }

    @PutMapping("/nodes/{classId}")
    public ResponseEntity<SaveNodeViewResponse> saveNodeView(
            @PathVariable String projectId,
            @PathVariable String classId,
            @RequestBody SaveNodeViewRequest request) {

        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .commandId(request.getCommandId())
                .participantId(request.getParticipantId())
                .projectId(projectId)
                .classId(classId)
                .expectedLayoutVersion(request.getExpectedLayoutVersion())
                .x(request.getX())
                .y(request.getY())
                .build();

        long newVersion = saveNodeViewUseCase.execute(command);

        SaveNodeViewResponse response = SaveNodeViewResponse.builder()
                .commandId(command.getCommandId())
                .classId(classId)
                .x(command.getX())
                .y(command.getY())
                .layoutVersion(newVersion)
                .build();

        return ResponseEntity.ok(response);
    }
}
