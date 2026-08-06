import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./credit-evaluation/credit-evaluation-form/credit-evaluation-form.component')
        .then(m => m.CreditEvaluationFormComponent)
  }
];
