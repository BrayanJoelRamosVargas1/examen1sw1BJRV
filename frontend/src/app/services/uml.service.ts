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

export interface UmlModelResponse {
  projectId: string;
  version: number;
  classes: UmlClassDto[];
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
}
