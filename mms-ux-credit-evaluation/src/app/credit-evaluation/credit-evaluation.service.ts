import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreditEvaluation, CreditEvaluationCommand } from './credit-evaluation.model';

@Injectable({ providedIn: 'root' })
export class CreditEvaluationService {

  private readonly apiUrl = `${environment.apiBaseUrl}/v1/credit-evaluations`;

  constructor(private readonly http: HttpClient) {}

  submit(command: CreditEvaluationCommand): Observable<CreditEvaluation> {
    return this.http.post<CreditEvaluation>(this.apiUrl, command);
  }

  findAll(): Observable<CreditEvaluation[]> {
    return this.http.get<CreditEvaluation[]>(this.apiUrl);
  }
}
