import {ReportSection} from './ReportSection';

export interface ReportTemplate {
  id?: string;
  name: string;
  description: string;
  sections: ReportSection[];
  meetIds: string[];
}
