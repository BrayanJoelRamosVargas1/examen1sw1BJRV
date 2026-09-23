import { Injectable, NgZone } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface SpeechResult {
  transcript: string;
  isFinal: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SpeechRecognitionService {
  private recognition: any;
  private resultSubject = new Subject<SpeechResult>();
  private errorSubject = new Subject<string>();
  private isListening = false;

  constructor(private zone: NgZone) {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = false;
      this.recognition.interimResults = true;
      this.recognition.lang = 'es-ES';

      this.recognition.onresult = (event: any) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }

        this.zone.run(() => {
          if (finalTranscript) {
            this.resultSubject.next({ transcript: finalTranscript.trim(), isFinal: true });
          } else if (interimTranscript) {
            this.resultSubject.next({ transcript: interimTranscript.trim(), isFinal: false });
          }
        });
      };

      this.recognition.onerror = (event: any) => {
        this.zone.run(() => {
          this.isListening = false;
          this.errorSubject.next(event.error);
        });
      };

      this.recognition.onend = () => {
        this.zone.run(() => {
          this.isListening = false;
        });
      };
    }
  }

  public get isSupported(): boolean {
    return !!this.recognition;
  }

  public get isCurrentlyListening(): boolean {
    return this.isListening;
  }

  public getResults(): Observable<SpeechResult> {
    return this.resultSubject.asObservable();
  }

  public getErrors(): Observable<string> {
    return this.errorSubject.asObservable();
  }

  public startListening(): void {
    if (!this.recognition) {
      this.errorSubject.next('not-supported');
      return;
    }
    if (this.isListening) return;
    
    try {
      this.recognition.start();
      this.isListening = true;
    } catch (e: any) {
      this.errorSubject.next('start-failed');
    }
  }

  public stopListening(): void {
    if (!this.recognition || !this.isListening) return;
    try {
      this.recognition.stop();
    } catch (e) {}
    this.isListening = false;
  }
}
