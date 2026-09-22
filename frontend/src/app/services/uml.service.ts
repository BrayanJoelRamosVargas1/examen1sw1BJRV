import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = 'http://localhost:8080/api';

export interface UmlClassDto {
  id: string;
  name: string;
  attributes?: any[]; // or define UmlAttributeDto if needed
  operations?: any[];
}

export interface UmlRelationshipDto {
  id: string;
  type: string;
  sourceClassId: string;
  targetClassId: string;
  sourceMultiplicity: string;
  targetMultiplicity: string;
}

export interface UmlModelResponse {
  projectId: string;
  version: number;
  classes: UmlClassDto[];
  relationships?: UmlRelationshipDto[];
}

export interface RemoveOperationRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
}

export interface RemoveOperationResponse {
  commandId: string;
  classId: string;
  operationId: string;
  modelVersion: number;
}

export interface CreateClassRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  name: string;
}

export interface CreateClassResponse {
  commandId: string;
  classId: string;
  className: string;
  modelVersion: number;
}

export interface RenameClassRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  newName: string;
}

export interface RenameClassResponse {
  commandId: string;
  classId: string;
  newName: string;
  modelVersion: number;
}

export interface RemoveAttributeRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
}

export interface RemoveAttributeResponse {
  commandId: string;
  classId: string;
  attributeId: string;
  modelVersion: number;
}

export interface AddAttributeResponse {
  commandId: string;
  classId: string;
  attribute: {
    id: string;
    name: string;
    type: string;
    visibility: string;
    orderIndex: number;
  };
  modelVersion: number;
}

export interface AddOperationRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  name: string;
  returnType: string;
  visibility: string;
  parameters: {
    name: string;
    type: string;
  }[];
}

export interface AddOperationResponse {
  commandId: string;
  classId: string;
  operationId: string;
  name: string;
  returnType: string;
  visibility: string;
  orderIndex: number;
  parameters: {
    id: string;
    name: string;
    type: string;
    orderIndex: number;
  }[];
  modelVersion: number;
}

export interface UpdateOperationRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  name: string;
  returnType: string;
  visibility: string;
  parameters: {
    id: string | null;
    name: string;
    type: string;
  }[];
}

export interface UpdateOperationResponse {
  commandId: string;
  classId: string;
  operationId: string;
  name: string;
  returnType: string;
  visibility: string;
  orderIndex: number;
  parameters: {
    id: string;
    name: string;
    type: string;
    orderIndex: number;
  }[];
  modelVersion: number;
}

export interface AddRelationshipRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  type: string;
  sourceClassId: string;
  targetClassId: string;
  sourceMultiplicity?: string;
  targetMultiplicity?: string;
}

export interface AddRelationshipResponse {
  commandId: string;
  relationshipId: string;
  modelVersion: number;
  relationship: UmlRelationshipDto;
}

export interface UpdateRelationshipRequest {
  commandId: string;
  participantId: string;
  expectedVersion: number;
  type: string;
  sourceMultiplicity?: string;
  targetMultiplicity?: string;
}

export interface UpdateRelationshipResponse {
  relationshipId: string;
  type: string;
  sourceMultiplicity?: string;
  targetMultiplicity?: string;
  modelVersion: number;
}

export interface SaveNodeViewResponse {
  commandId: string;
  classId: string;
  x: number;
  y: number;
  layoutVersion: number;
}

export interface NodeViewDto {
  classId: string;
  x: number;
  y: number;
}

export interface GetDiagramLayoutResponse {
  projectId: string;
  layoutVersion: number;
  nodeViews: NodeViewDto[];
}

@Injectable({ providedIn: 'root' })
export class UmlService {
  constructor(private http: HttpClient) {}

  getModel(projectId: string): Observable<UmlModelResponse> {
    return this.http.get<UmlModelResponse>(
      `${API}/projects/${projectId}/model`
    );
  }

  createClass(
    projectId: string,
    request: CreateClassRequest
  ): Observable<CreateClassResponse> {
    return this.http.post<CreateClassResponse>(
      `${API}/projects/${projectId}/classes`,
      request
    );
  }

  renameClass(
    projectId: string,
    classId: string,
    request: RenameClassRequest
  ): Observable<RenameClassResponse> {
    return this.http.patch<RenameClassResponse>(
      `${API}/projects/${projectId}/classes/${classId}`,
      request
    );
  }

  addAttribute(
    projectId: string,
    classId: string,
    request: any
  ): Observable<AddAttributeResponse> {
    return this.http.post<AddAttributeResponse>(
      `${API}/projects/${projectId}/classes/${classId}/attributes`,
      request
    );
  }

  updateAttribute(
    projectId: string,
    classId: string,
    attributeId: string,
    request: any
  ): Observable<any> {
    return this.http.put<any>(
      `${API}/projects/${projectId}/classes/${classId}/attributes/${attributeId}`,
      request
    );
  }

  removeAttribute(
    projectId: string,
    classId: string,
    attributeId: string,
    request: RemoveAttributeRequest
  ): Observable<RemoveAttributeResponse> {
    return this.http.delete<RemoveAttributeResponse>(
      `${API}/projects/${projectId}/classes/${classId}/attributes/${attributeId}`,
      { body: request }
    );
  }

  addOperation(
    projectId: string,
    classId: string,
    request: AddOperationRequest
  ): Observable<AddOperationResponse> {
    return this.http.post<AddOperationResponse>(
      `${API}/projects/${projectId}/classes/${classId}/operations`,
      request
    );
  }

  updateOperation(
    projectId: string,
    classId: string,
    operationId: string,
    request: UpdateOperationRequest
  ): Observable<UpdateOperationResponse> {
    return this.http.put<UpdateOperationResponse>(
      `${API}/projects/${projectId}/classes/${classId}/operations/${operationId}`,
      request
    );
  }

  deleteOperation(
    projectId: string,
    classId: string,
    operationId: string,
    request: RemoveOperationRequest
  ): Observable<RemoveOperationResponse> {
    return this.http.request<RemoveOperationResponse>('delete', `${API}/projects/${projectId}/classes/${classId}/operations/${operationId}`, {
      body: request
    });
  }

  addRelationship(
    projectId: string,
    request: AddRelationshipRequest
  ): Observable<AddRelationshipResponse> {
    return this.http.post<AddRelationshipResponse>(
      `${API}/projects/${projectId}/relationships`,
      request
    );
  }

  updateRelationship(
    projectId: string,
    relationshipId: string,
    request: UpdateRelationshipRequest
  ): Observable<UpdateRelationshipResponse> {
    return this.http.put<UpdateRelationshipResponse>(
      `${API}/projects/${projectId}/relationships/${relationshipId}`,
      request
    );
  }

  // DIAGRAM LAYOUT

  getDiagram(projectId: string): Observable<GetDiagramLayoutResponse> {
    return this.http.get<GetDiagramLayoutResponse>(`${API}/projects/${projectId}/diagram`);
  }

  saveNodeView(
    projectId: string,
    classId: string,
    commandId: string,
    participantId: string,
    expectedLayoutVersion: number,
    x: number,
    y: number
  ): Observable<SaveNodeViewResponse> {
    return this.http.put<SaveNodeViewResponse>(`${API}/projects/${projectId}/diagram/nodes/${classId}`, {
      commandId,
      participantId,
      expectedLayoutVersion,
      x,
      y
    });
  }
}
