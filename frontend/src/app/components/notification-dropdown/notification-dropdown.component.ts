import { Component, OnInit, OnDestroy, HostListener, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/notification.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-dropdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-dropdown.component.html',
  styleUrls: ['./notification-dropdown.component.css']
})
export class NotificationDropdownComponent implements OnInit, OnDestroy {
  @ViewChild('dropdown', { static: false }) dropdownElement?: ElementRef;
  @ViewChild('bellButton', { static: false }) bellButtonElement?: ElementRef;
  
  isOpen = false;
  notifications: Notification[] = [];
  unreadCount = 0;
  private subscriptions: Subscription[] = [];

  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.loadUnreadCount();

    // Subscribe to unread count changes
    const countSub = this.notificationService.unreadCount$.subscribe(count => {
      this.unreadCount = count;
    });
    this.subscriptions.push(countSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    // Remove body click listener
    document.removeEventListener('click', this.handleDocumentClick);
    document.removeEventListener('touchstart', this.handleDocumentClick);
  }

  @HostListener('document:click', ['$event'])
  @HostListener('document:touchstart', ['$event'])
  handleDocumentClick = (event: Event): void => {
    if (!this.isOpen) return;
    
    const target = event.target as HTMLElement;
    const clickedInside = this.elementRef.nativeElement.contains(target);
    
    if (!clickedInside) {
      this.closeDropdown();
    }
  }

  toggleDropdown(event?: Event): void {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }
    
    const wasOpen = this.isOpen;
    this.isOpen = !this.isOpen;
    
    if (this.isOpen) {
      this.loadNotifications();
      // Prevent body scroll on mobile
      if (window.innerWidth <= 768) {
        document.body.classList.add('dropdown-open');
      }
      // Add listeners when opening
      setTimeout(() => {
        document.addEventListener('click', this.handleDocumentClick, true);
        document.addEventListener('touchstart', this.handleDocumentClick, true);
      }, 0);
    } else {
      // Remove listeners when closing
      document.removeEventListener('click', this.handleDocumentClick, true);
      document.removeEventListener('touchstart', this.handleDocumentClick, true);
      // Restore body scroll
      if (window.innerWidth <= 768) {
        document.body.classList.remove('dropdown-open');
      }
    }
  }

  closeDropdown(): void {
    if (this.isOpen) {
      this.isOpen = false;
      // Remove listeners
      document.removeEventListener('click', this.handleDocumentClick, true);
      document.removeEventListener('touchstart', this.handleDocumentClick, true);
      // Restore body scroll
      if (window.innerWidth <= 768) {
        document.body.classList.remove('dropdown-open');
      }
    }
  }

  loadNotifications(): void {
    this.notificationService.getRecentNotifications().subscribe({
      next: (notifications) => {
        this.notifications = notifications;
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe();
  }

  markAsRead(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          this.handleNotificationClick(notification);
        },
        error: (error) => {
          console.error('Error marking notification as read:', error);
        }
      });
    } else {
      this.handleNotificationClick(notification);
    }
  }

  handleNotificationClick(notification: Notification): void {
    this.closeDropdown();

    // Navigate based on notification type
    if (notification.referenceType === 'LEAVE' && notification.referenceId) {
      this.router.navigate(['/leaves']);
    } else if (notification.referenceType === 'DOCUMENT' && notification.referenceId) {
      // Navigate to the specific document detail page
      this.router.navigate(['/documents', notification.referenceId]);
    }
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.unreadCount = 0;
      },
      error: (error) => {
        console.error('Error marking all as read:', error);
      }
    });
  }

  deleteNotification(event: Event, id: number): void {
    event.stopPropagation();
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== id);
      },
      error: (error) => {
        console.error('Error deleting notification:', error);
      }
    });
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'LEAVE_REQUEST':
        return '🏖️';
      case 'LEAVE_APPROVED':
        return '✅';
      case 'LEAVE_REJECTED':
        return '❌';
      case 'DOCUMENT_EXPIRED':
        return '🚨';
      case 'DOCUMENT_NEAR_EXPIRY':
        return '⚠️';
      default:
        return '🔔';
    }
  }

  getTimeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
    return date.toLocaleDateString();
  }
}

