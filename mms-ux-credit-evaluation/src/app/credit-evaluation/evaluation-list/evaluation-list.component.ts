import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';
import { CreditEvaluation } from '../credit-evaluation.model';

@Component({
  selector: 'app-evaluation-list',
  standalone: true,
  imports: [CommonModule, DatePipe, CurrencyPipe],
  templateUrl: './evaluation-list.component.html',
  styleUrl: './evaluation-list.component.scss'
})
export class EvaluationListComponent {
  @Input() evaluations: CreditEvaluation[] = [];
  @Input() loading = false;
}
