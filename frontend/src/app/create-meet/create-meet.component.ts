import { Component, OnInit } from '@angular/core'; // N'oubliez pas OnInit
import { User } from '../model/user.model';
import { NewMeetService } from '../services/NewMeet/new-meet.service';
import { MeetRequest } from '../model/MeetRequest';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule,Location } from '@angular/common';
import { ReportTemplate } from '../model/ReportTemplate';

@Component({
  selector: 'app-create-meet',
  standalone: true, // J'assume que vous êtes en standalone vu les imports
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './create-meet.component.html',
  styleUrl: './create-meet.component.css'
})
export class CreateMeetComponent implements OnInit {
  users: User[] = [];
  templates: ReportTemplate[] = [];
  form!: FormGroup;
  errorMessage: string = '';

  constructor(
    private newMeetService: NewMeetService,
    private route: Router,
    private fb: FormBuilder,
    private location: Location
  ) {}

  ngOnInit() {
    // 1. Initialisation du formulaire
    this.form = this.fb.group({
      title: ['', [Validators.required]],
      userInvitedsSelected: [[], Validators.required],
      // CORRECTION : Ajout du champ pour la template
      reportTemplateId: ['']
    });

    // 2. Chargement des données
    this.loadUsers();
    this.loadTemplates();
  }

  loadUsers() {
    this.newMeetService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
      },
      error: (err) => console.error("Erreur chargement users", err)
    });
  }

  loadTemplates() {
    this.newMeetService.getAllTemplate().subscribe({
      next: (data) => {
        this.templates = data;
      },
      error: (err) => console.error("Erreur chargement templates", err)
    });
  }

  createMeet() {
    if (this.form.invalid) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires.';
      this.form.markAllAsTouched(); // Affiche toutes les erreurs visuelles
      return;
    }

    // Récupération des valeurs du formulaire
    const { title, userInvitedsSelected, reportTemplateId } = this.form.value;

    // Construction de l'objet à envoyer
    const meet: MeetRequest = {
      title: title,
      invitedUserIds: userInvitedsSelected,
      reportTemplateId: reportTemplateId // CORRECTION : On envoie l'ID de la template
    };

    this.newMeetService.createMeet(meet).subscribe({
      next: (res) => {
        console.log("Meeting créé avec succès");
        if(meet.reportTemplateId){
          this.route.navigate(['/dashboard_manager']);
        }
        else{
          this.route.navigate(['/create_template',res.id]);
        }


      },
      error: (err) => {
        console.error("Échec de la création", err);
        this.errorMessage = err.error?.message || 'Erreur lors de la création du meeting.';
      }
    });
  }
  onCheckboxChange(e: any) {
    const userInvitedsControl = this.form.get('userInvitedsSelected');
    const currentList: string[] = userInvitedsControl?.value || [];
    const email = e.target.value;

    if (e.target.checked) {
      // Ajouter l'email s'il n'est pas déjà présent
      if (!currentList.includes(email)) {
        currentList.push(email);
      }
    } else {
      // Retirer l'email si on décoche
      const index = currentList.indexOf(email);
      if (index > -1) {
        currentList.splice(index, 1);
      }
    }

    // Mettre à jour le formulaire
    userInvitedsControl?.setValue(currentList);
    userInvitedsControl?.markAsTouched();
  }
  goBack() {
    this.location.back();
  }
  goToTemplate(){
    this.route.navigate(['/create_template']);
  }
}
