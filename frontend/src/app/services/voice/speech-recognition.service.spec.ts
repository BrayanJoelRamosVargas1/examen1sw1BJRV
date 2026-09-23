import { TestBed } from '@angular/core/testing';
import { SpeechRecognitionService } from './speech-recognition.service';
import { NgZone } from '@angular/core';

describe('SpeechRecognitionService', () => {
  let service: SpeechRecognitionService;
  let mockRecognition: any;

  beforeEach(() => {
    mockRecognition = {
      start: jasmine.createSpy('start'),
      stop: jasmine.createSpy('stop')
    };

    (window as any).SpeechRecognition = function() {
      return mockRecognition;
    };

    TestBed.configureTestingModule({
      providers: [
        SpeechRecognitionService,
        { provide: NgZone, useValue: new NgZone({ enableLongStackTrace: false }) }
      ]
    });
    service = TestBed.inject(SpeechRecognitionService);
  });

  afterEach(() => {
    delete (window as any).SpeechRecognition;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should support speech recognition if window API exists', () => {
    expect(service.isSupported).toBeTrue();
  });

  it('should call start on underlying recognition API', () => {
    service.startListening();
    expect(mockRecognition.start).toHaveBeenCalled();
    expect(service.isCurrentlyListening).toBeTrue();
  });

  it('should not call start twice if already listening', () => {
    service.startListening();
    service.startListening();
    expect(mockRecognition.start).toHaveBeenCalledTimes(1);
  });

  it('should call stop on underlying recognition API', () => {
    service.startListening();
    service.stopListening();
    expect(mockRecognition.stop).toHaveBeenCalled();
    expect(service.isCurrentlyListening).toBeFalse();
  });

  it('should emit transcript on result', (done) => {
    service.getResults().subscribe(res => {
      expect(res.transcript).toBe('crear clase Cliente');
      expect(res.isFinal).toBeTrue();
      done();
    });

    // Simulate result event
    mockRecognition.onresult({
      resultIndex: 0,
      results: [
        Object.assign([{ transcript: 'crear clase Cliente' }], { isFinal: true })
      ]
    });
  });

  it('should emit error on error event', (done) => {
    service.getErrors().subscribe(err => {
      expect(err).toBe('not-allowed');
      expect(service.isCurrentlyListening).toBeFalse();
      done();
    });

    service.startListening();
    mockRecognition.onerror({ error: 'not-allowed' });
  });

});
