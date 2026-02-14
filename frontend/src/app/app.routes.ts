import { Routes } from '@angular/router';
import {LoginComponent} from './login/login.component';
import {IndexComponent} from './index/index.component';
import {AuthGuard} from './guards/auth.guard';
import {DashboardComponent} from './dashboard/dashboard.component';
import {CreateMeetComponent} from './create-meet/create-meet.component';
import {DashboardManagerComponent} from './dashboard-manager/dashboard-manager.component';
import {RoomMeetComponent} from './room-meet/room-meet.component';
import {CreateTemplateComponent} from './create-template/create-template.component';
import {EditReportComponent} from './edit-report/edit-report.component';

export const routes: Routes = [
  { path: '', component: LoginComponent },

  {
    path: 'dashboard',
    component: DashboardComponent,

  },

  {
    path: 'dashboard_manager',
    component: DashboardManagerComponent,

  },
  {path :'create_meet',
    component : CreateMeetComponent,

  }
,
  {
    path : 'join_meet/:id',
    component : RoomMeetComponent
  },
  {
    path : 'create_template/:id',
    component : CreateTemplateComponent
  },
  {
    path :'edit_report/:meetId',
    component : EditReportComponent
  },
  { path: '**', redirectTo: '' }
];
