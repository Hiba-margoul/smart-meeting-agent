export interface ReportSectionContent {
  code: string;
  title: string;
  content: string;
}

export interface Report {
  id?: string;
  meetId: string;
  templateId?: string;
  title: string;
  createdAt?: Date;
  sections: ReportSectionContent[];
}
