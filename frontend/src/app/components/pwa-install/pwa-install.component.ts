import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ToastService} from '../../services/toast.service';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

@Component({
  selector: 'app-pwa-install',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="showInstallPrompt" class="pwa-install-banner">
      <div class="install-content">
        <div class="install-icon">📱</div>
        <div class="install-text">
          <strong>Install VDS EMS</strong>
          <p>Install this app on your device for a better experience</p>
        </div>
        <div class="install-actions">
          <button (click)="dismissInstallPrompt()" class="btn-dismiss">Not now</button>
          <button (click)="installApp()" class="btn-install">Install</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .pwa-install-banner {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 1rem;
      box-shadow: 0 -4px 6px rgba(0, 0, 0, 0.1);
      z-index: 1000;
      animation: slideUp 0.3s ease-out;
    }

    @keyframes slideUp {
      from {
        transform: translateY(100%);
      }
      to {
        transform: translateY(0);
      }
    }

    .install-content {
      display: flex;
      align-items: center;
      gap: 1rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .install-icon {
      font-size: 2rem;
      flex-shrink: 0;
    }

    .install-text {
      flex: 1;
    }

    .install-text strong {
      display: block;
      font-size: 1.1rem;
      margin-bottom: 0.25rem;
    }

    .install-text p {
      margin: 0;
      font-size: 0.9rem;
      opacity: 0.9;
    }

    .install-actions {
      display: flex;
      gap: 0.5rem;
      flex-shrink: 0;
    }

    .btn-dismiss,
    .btn-install {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 0.5rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-dismiss {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }

    .btn-dismiss:hover {
      background: rgba(255, 255, 255, 0.3);
    }

    .btn-install {
      background: white;
      color: #667eea;
    }

    .btn-install:hover {
      background: #f0f0f0;
      transform: translateY(-1px);
    }

    @media (max-width: 768px) {
      .install-content {
        flex-direction: column;
        text-align: center;
      }

      .install-actions {
        width: 100%;
      }

      .btn-dismiss,
      .btn-install {
        flex: 1;
      }
    }
  `]
})
export class PwaInstallComponent implements OnInit, OnDestroy {
  showInstallPrompt = false;
  private deferredPrompt: BeforeInstallPromptEvent | null = null;
  private installPromptDismissed = false;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    // Check if already installed
    if (this.isInstalled()) {
      return;
    }

    // Check if user previously dismissed the prompt
    const dismissed = localStorage.getItem('pwa-install-dismissed');
    if (dismissed) {
      const dismissedTime = parseInt(dismissed, 10);
      const daysSinceDismissed = (Date.now() - dismissedTime) / (1000 * 60 * 60 * 24);
      // Show again after 7 days
      if (daysSinceDismissed < 7) {
        return;
      }
    }

    // Listen for the beforeinstallprompt event
    window.addEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt.bind(this));
  }

  ngOnDestroy(): void {
    window.removeEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt.bind(this));
  }

  private handleBeforeInstallPrompt(event: Event): void {
    // Prevent the default browser install prompt
    event.preventDefault();
    
    // Store the event for later use
    this.deferredPrompt = event as BeforeInstallPromptEvent;
    
    // Show our custom install prompt
    this.showInstallPrompt = true;
  }

  async installApp(): Promise<void> {
    if (!this.deferredPrompt) {
      // Fallback: show instructions for manual installation
      this.showManualInstallInstructions();
      return;
    }

    // Show the browser's install prompt
    this.deferredPrompt.prompt();

    // Wait for the user's response
    const {outcome} = await this.deferredPrompt.userChoice;

    if (outcome === 'accepted') {
      this.toastService.success('App installation started!');
    } else {
      this.toastService.info('Installation cancelled');
    }

    // Clear the deferred prompt
    this.deferredPrompt = null;
    this.showInstallPrompt = false;
  }

  dismissInstallPrompt(): void {
    this.showInstallPrompt = false;
    this.installPromptDismissed = true;
    // Remember dismissal for 7 days
    localStorage.setItem('pwa-install-dismissed', Date.now().toString());
  }

  private isInstalled(): boolean {
    // Check if running in standalone mode (installed PWA)
    if (window.matchMedia('(display-mode: standalone)').matches) {
      return true;
    }

    // Check if running from home screen (iOS)
    if ((window.navigator as any).standalone === true) {
      return true;
    }

    return false;
  }

  private showManualInstallInstructions(): void {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    const isAndroid = /Android/.test(navigator.userAgent);

    let instructions = '';

    if (isIOS) {
      instructions = 'To install: Tap the Share button, then "Add to Home Screen"';
    } else if (isAndroid) {
      instructions = 'To install: Tap the menu (⋮) and select "Install app" or "Add to Home screen"';
    } else {
      instructions = 'To install: Look for the install icon (⊕) in your browser\'s address bar';
    }

    this.toastService.info(instructions);
  }
}

