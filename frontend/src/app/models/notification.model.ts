export interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  message: string;
  referenceId?: number;
  referenceType?: string;
  isRead: boolean;
  createdAt: string;
  organizationId: number;
}

