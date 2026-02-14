import { Component, inject } from '@angular/core';
import { CommonModule, Location } from "@angular/common"; // Ajout de Location
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NewTemplateService } from '../services/createTemplatesService/new-template.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ReportTemplate } from '../model/ReportTemplate';

@Component({
  selector: 'app-create-template',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
  ],
  templateUrl: './create-template.component.html',
  styleUrl: './create-template.component.css' // Correction nom fichier si nécessaire
})
export class CreateTemplateComponent {
  private fb = inject(FormBuilder);
  private templateService = inject(NewTemplateService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private location = inject(Location); // Injection de Location

  templateForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    sections: this.fb.array([])
  });

  get sections(): FormArray {
    return this.templateForm.get('sections') as FormArray;
  }

  ngOnInit() {
    // On ajoute une section par défaut pour éviter un écran vide
    if (this.sections.length === 0) {
      this.addSection();
    }
  }

  addSection() {
    const sectionGroup = this.fb.group({
      code: ['', Validators.required],
      title: ['', Validators.required],
      enabled: [true],
      order: [this.sections.length + 1],
      guidance: ['']
    });

    this.sections.push(sectionGroup);
  }

  removeSection(index: number) {
    this.sections.removeAt(index);
  }

  onSubmit() {
    if (!this.templateForm.valid) {
      // Astuce pour afficher les erreurs si l'utilisateur clique sans remplir
      this.templateForm.markAllAsTouched();
      return;
    }

    const meetId = this.route.snapshot.paramMap.get('id');

    const template: ReportTemplate = {
      name: this.templateForm.value.name,
      description: this.templateForm.value.description,
      sections: this.templateForm.value.sections,
      meetIds: meetId ? [meetId] : []
    };

    this.templateService.createTemplate(template).subscribe({
      next: (response) => {
        console.log('Template créé avec succès !', response);
        this.router.navigate(['/dashboard_manager']);
      },
      error: (err) => {
        console.error('Erreur lors de la création', err);
        // Vous pouvez gérer un message d'erreur global ici si vous voulez
      }
    });
  }

  goBack() {
    this.location.back();
  }
}
