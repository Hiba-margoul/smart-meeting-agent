import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common'; // Ajout important pour le HTML

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule // Nécessaire pour *ngIf
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  form!: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false; // Pour l'état de chargement du bouton

  constructor(
    private authService: AuthService,
    private route: Router,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  login() {
    if (this.form.invalid) {
      this.errorMessage = 'Veuillez remplir tous les champs correctement.';
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = ''; // Reset erreur

    const { email, password } = this.form.value;

    this.authService.login(email, password).subscribe({
      next: (res) => {
        console.log("User connecté avec succès");
        const role = localStorage.getItem("role");

        // Simulation d'un petit délai pour l'UX (optionnel)
        setTimeout(() => {
          this.isLoading = false;
          if (role === "MANAGER") {
            this.route.navigate(['/dashboard_manager']);
          } else {
            this.route.navigate(['/dashboard']);
          }
        }, 500);
      },
      error: (err) => {
        console.log("Login échoué", err.error);
        this.isLoading = false;
        // Gestion plus fine du message d'erreur si possible
        this.errorMessage = typeof err.error === 'string' ? err.error : 'Email ou mot de passe incorrect.';
      }
    });
  }
}
