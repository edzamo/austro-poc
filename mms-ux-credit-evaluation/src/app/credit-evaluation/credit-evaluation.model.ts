export type EvaluationStatus = 'APROBADO' | 'RECHAZADO';

export interface CreditEvaluation {
  readonly id: number;
  readonly cedula: string;
  readonly requestedAmount: number;
  readonly years: number;
  readonly salary: number;
  readonly finalStatus: EvaluationStatus;
  readonly evaluationDate: string;
}

export interface CreditEvaluationCommand {
  cedula: string;
  requestedAmount: number;
  years: number;
  salary: number;
}
