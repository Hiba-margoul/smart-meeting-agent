import { Component, inject, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ReportService } from '../services/EditReport/edit-report.service';
import { Report } from '../model/Report';
import { CommonModule, Location } from '@angular/common'; // Ajout de Location

@Component({
  selector: 'app-edit-report',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './edit-report.component.html',
  styleUrl: './edit-report.component.css'
})
export class EditReportComponent implements OnInit {
  reportForm: FormGroup;
  meetId: string | null = null;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  // Injection via constructeur (ou inject() moderne)
  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private location: Location // Pour le bouton retour
  ) {
    this.reportForm = this.fb.group({
      id: [''],
      meetId: [''],
      templateId: [''],
      title: ['', Validators.required],
      sections: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.meetId = this.route.snapshot.paramMap.get('meetId');
    if (this.meetId) {
      this.loadReport(this.meetId);
    }
  }

  get sections(): FormArray {
    return this.reportForm.get('sections') as FormArray;
  }

  loadReport(meetId: string) {
    this.isLoading = true;
    this.reportService.getReportByMeetId(meetId).subscribe({
      next: (report) => {
        this.reportForm.patchValue({
          id: report.id,
          meetId: report.meetId,
          templateId: report.templateId,
          title: report.title
        });

        this.sections.clear();
        if (report.sections) {
          report.sections.forEach(section => {
            const sectionGroup = this.fb.group({
              code: [section.code],
              title: [section.title, Validators.required],
              content: [section.content, Validators.required]
            });
            this.sections.push(sectionGroup);
          });
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement rapport', err);
        this.errorMessage = "Impossible de charger le rapport.";
        this.isLoading = false;
      }
    });
  }

  onSubmit() {
    if (this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const reportData: Report = this.reportForm.value;

    this.reportService.saveOrUpdateReport(reportData).subscribe({
      next: (savedReport) => {
        this.successMessage = "Rapport enregistré avec succès !";
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/dashboard_manager']), 1500);
      },
      error: (err) => {
        console.error('Erreur sauvegarde', err);
        this.errorMessage = "Erreur lors de l'enregistrement.";
        this.isLoading = false;
      }
    });
  }

  // Fonction standardisée pour le bouton retour
  goBack() {
    this.location.back();
  }
}
