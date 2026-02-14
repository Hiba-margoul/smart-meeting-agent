import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { Meet } from '../model/meet';
import { DashboardService } from '../services/DashboardService/dashboard.service';
import { Router } from '@angular/router';
import { DatePipe, NgClass, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SafeUrlPipe } from '../safe-url.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { AuthService } from '../auth/auth.service';
import { EventSourcePolyfill } from 'event-source-polyfill';

@Component({
  selector: 'app-dashboard-manager',
  standalone: true, // J'ai ajouté standalone: true car vous utilisez imports []
  imports: [
    DatePipe,
    NgForOf,
    NgClass,
    FormsModule,
    NgIf,
    SafeUrlPipe
  ],
  templateUrl: './dashboard-manager.component.html',
  styleUrl: './dashboard-manager.component.css'
})
export class DashboardManagerComponent implements OnInit, OnDestroy {
  meets: Meet[] = [];
  filtredMeet: Meet[] = [];
  statusFilter: "ALL" | "PLANNED" | "ACTIVE" | "FINISHED" = "ALL";
  searchTerm: string = '';
  startDateFilter: string = ''; // format yyyy-MM-dd
  endDateFilter: string = '';
  pdfUrl: string | null = null;

  // Pour stocker le meeting sélectionné
  selectedMeet: any = null;
  pdfPanelOpen = false;

  // Pagination
  paginatedMeets: Meet[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 5;
  totalPages: number = 0;
  pagesArray: number[] = [];

  stats = {
    total: 0,
    active: 0,
    finished: 0,
    planned: 0
  };

  private eventSource: EventSourcePolyfill | undefined;

  constructor(
    private dashboardService: DashboardService,
    private loginServie: AuthService,
    private route: Router,
    private sanitizer: DomSanitizer,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // 1. Charger les meets existants via HTTP classique
    this.dashboardService.getMeet().subscribe({
      next: data => {
        this.meets = data;
        this.applyFilters();
        this.calculateStats();
      },
      error: err => console.error('Erreur chargement initial meets', err)
    });

    // 2. Lancer la connexion temps réel (SSE)
    this.setupSse();
  }

  setupSse() {
    const token = localStorage.getItem("id_token");

    if (!token) {
      console.error("Impossible d'établir la connexion SSE : aucun token disponible.");
      return;
    }

    console.log("Tentative de connexion SSE...");

    // Configuration de la source avec le Token JWT
    this.eventSource = new EventSourcePolyfill('http://localhost:8089/meeting/stream', {
      headers: {
        'Authorization': 'Bearer ' + token
      },
      heartbeatTimeout: 120000, // 2 minutes avant de considérer la connexion morte localement
      withCredentials: true     // Important pour CORS
    });

    // --- Gestionnaire 1 : Connexion ouverte ---
    this.eventSource.onopen = (event: any) => {
      console.log('✅ Connexion SSE établie avec succès !');
    };

    // --- Gestionnaire 2 : Réception des messages standards ---
    this.eventSource.addEventListener('message', (event: any) => {
      // 1. Filtrer les PINGS (Heartbeat) pour ne pas planter le JSON.parse
      if (!event.data || event.data === 'ping') {
        console.log('💓 Heartbeat (message) reçu');
        return;
      }

      this.zone.run(() => {
        try {
          console.log("⚡ Update reçue :", event.data);
          const updatedMeet: Meet = JSON.parse(event.data);

          this.handleMeetUpdate(updatedMeet);

        } catch (e) {
          console.error('Erreur parsing SSE JSON', e, event.data);
        }
      });
    });

    // --- Gestionnaire 3 : Réception explicite de l'événement "heartbeat" ---
    // (Si vous avez utilisé .event("heartbeat") dans le Java)
    this.eventSource.addEventListener('heartbeat', (event: any) => {
      console.log('💓 Heartbeat (event) reçu - Connexion vivante');
      // Rien à faire, cela empêche juste le navigateur de fermer la connexion
    });

    // --- Gestionnaire 4 : Erreurs ---
    this.eventSource.onerror = (error: any) => {
      console.error('❌ Erreur SSE :', error);

      // Si l'erreur est fatale (ex: 401 Unauthorized), on ferme
      if (error && error.status === 401) {
        console.error("Session expirée, fermeture du stream.");
        this.eventSource?.close();
      }
    };
  }

  // Logique extraite pour mettre à jour la liste proprement
  handleMeetUpdate(updatedMeet: Meet) {
    const index = this.meets.findIndex(m => m.id === updatedMeet.id);

    if (index > -1) {
      // Mise à jour existante
      // On crée une nouvelle référence de tableau pour qu'Angular détecte le changement
      const newMeets = [...this.meets];
      newMeets[index] = updatedMeet;
      this.meets = newMeets;
    } else {
      // Nouveau meeting : ajout au début
      this.meets = [updatedMeet, ...this.meets];
    }

    this.calculateStats();
    this.applyFilters();
    this.cdr.detectChanges(); // Force la mise à jour visuelle
  }

  calculateStats() {
    if (!this.meets) return;
    this.stats.total = this.meets.length;
    this.stats.active = this.meets.filter(m => m.status === 'ACTIVE').length;
    this.stats.finished = this.meets.filter(m => m.status === 'FINISHED').length;
    this.stats.planned = this.meets.filter(m => m.status === 'PLANNED').length;
  }

  applyFilters(): void {
    if (!this.meets) return;

    this.filtredMeet = this.meets
      .filter(meet =>
        this.statusFilter === 'ALL' || (meet.status === this.statusFilter)
      )
      .filter(meet =>
        meet.title.toLowerCase().includes(this.searchTerm.toLowerCase())
      )
      .filter(meet => !this.startDateFilter || new Date(meet.startedAt) >= new Date(this.startDateFilter!))
      .filter(meet => !this.endDateFilter || new Date(meet.endedAt) <= new Date(this.endDateFilter!));

    // Réinitialiser à la page 1 après un filtrage
    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination() {
    this.totalPages = Math.ceil(this.filtredMeet.length / this.itemsPerPage);
    // Créer un tableau [1, 2, 3...] pour l'affichage
    this.pagesArray = Array.from({ length: this.totalPages }, (_, i) => i + 1);

    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedMeets = this.filtredMeet.slice(startIndex, endIndex);
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
    }
  }

  changeStatusFilter(status: "ALL" | "PLANNED" | "ACTIVE" | "FINISHED") {
    this.statusFilter = status;
    this.applyFilters();
  }

  onSearch(term: string) {
    this.searchTerm = term;
    this.applyFilters();
  }

  onStartDateChange(date: string) {
    this.startDateFilter = date;
    this.applyFilters();
  }

  onEndDateChange(date: string) {
    this.endDateFilter = date;
    this.applyFilters();
  }

  createMeet(){
    this.route.navigate(['/create_meet']);
  }

  joinMeet(meetId: string){
    this.route.navigate(['/join_meet', meetId]);
  }

  openPdfPanel(meet: Meet) {
    this.selectedMeet = meet;
    this.pdfPanelOpen = true;
    this.pdfUrl = null;

    this.dashboardService.getReportPdf(meet.id).subscribe({
      next: (data: Blob) => {
        const file = new Blob([data], { type: 'application/pdf' });
        const objectUrl = URL.createObjectURL(file);
        this.pdfUrl = objectUrl;
      },
      error: (err) => {
        console.error('Erreur téléchargement PDF', err);
      }
    });
  }

  downloadPdf() {
    if (this.pdfUrl) {
      const link = document.createElement('a');
      link.href = this.pdfUrl;
      link.download = `Rapport-${this.selectedMeet?.title || 'meeting'}.pdf`;
      link.click();
    }
  }

  closePdfPanel() {
    this.pdfPanelOpen = false;
    this.selectedMeet = null;
    this.pdfUrl = null;
  }

  editReport() {
    if (this.selectedMeet) {
      this.route.navigate(['/edit_report', this.selectedMeet.id]);
    }
  }

  logout(){
    this.loginServie.logout();
  }

  ngOnDestroy() {
    if (this.eventSource) {
      this.eventSource.close();
      console.log("Connexion SSE fermée proprement.");
    }
  }
}
