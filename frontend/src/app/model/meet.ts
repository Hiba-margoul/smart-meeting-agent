export interface Meet {
  id: string;
  title: string;
  liveKitRoomName: string;
  hostId: string;
  status: 'PLANNED' | 'ACTIVE' | 'FINISHED' | 'CANCELLED';
  createdAt: string;
  startedAt: string;
  endedAt: string;
  invitedUserIds: string[];
  reportGenerated: boolean;
  reportId?: string;
  reportTemplateId ?: string;
}
