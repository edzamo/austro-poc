import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs';
import { CreditEvaluationService } from '../credit-evaluation.service';
import { CreditEvaluation } from '../credit-evaluation.model';
import { EvaluationListComponent } from '../evaluation-list/evaluation-list.component';

type FormState = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-credit-evaluation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EvaluationListComponent],
  templateUrl: './credit-evaluation-form.component.html',
  styleUrl: './credit-evaluation-form.component.scss'
})
export class CreditEvaluationFormComponent implements OnInit {

  form: FormGroup;
  state = signal<FormState>('idle');
  lastResult = signal<CreditEvaluation | null>(null);
  errorMessage = signal<string>('');
  evaluations = signal<CreditEvaluation[]>([]);
  loadingHistory = signal(false);

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: CreditEvaluationService
  ) {
    this.form = this.fb.group({
      cedula:          ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      requestedAmount: [null, [Validators.required, Validators.min(1)]],
      years:           [null, [Validators.required, Validators.min(1), Validators.max(30)]],
      salary:          [null, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.loadHistory();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.state.set('loading');
    this.lastResult.set(null);
    this.errorMessage.set('');

    this.service.submit(this.form.value)
      .pipe(finalize(() => {
        if (this.state() === 'loading') this.state.set('idle');
      }))
      .subscribe({
        next: (result) => {
          this.lastResult.set(result);
          this.state.set('success');
          this.loadHistory();
        },
        error: (err) => {
          this.state.set('error');
          this.errorMessage.set(
            err?.error?.message ?? 'Error en el sistema. Por favor intente más tarde.'
          );
        }
      });
  }

  private loadHistory(): void {
    this.loadingHistory.set(true);
    this.service.findAll()
      .pipe(finalize(() => this.loadingHistory.set(false)))
      .subscribe({
        next: (data) => this.evaluations.set(data),
        error: () => this.evaluations.set([])
      });
  }

  get isLoading(): boolean { return this.state() === 'loading'; }
  get isApproved(): boolean { return this.lastResult()?.finalStatus === 'APROBADO'; }
  get isRejected(): boolean { return this.lastResult()?.finalStatus === 'RECHAZADO'; }

  fieldInvalid(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!(control?.invalid && control?.touched);
  }
}
