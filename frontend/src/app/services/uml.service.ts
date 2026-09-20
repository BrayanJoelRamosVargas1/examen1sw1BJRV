import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = 'http://localhost:8080/api';

export interface UmlClassDto {
  id: string;
  name: string;
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
}
