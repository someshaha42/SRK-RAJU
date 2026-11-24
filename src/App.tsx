import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AppRoute,
  getEffectiveCanvasSize,
  NormalizedRegion,
  RecordedVideoItem,
  RecordingSessionState,
  RecordingSettings,
  toPixelRegion,
} from './types/framecut';
import {
  deleteRecordingFromDb,
  loadRecordingSettings,
  loadSavedRecordings,
  saveRecordingSettings,
  saveRecordingToDb,
} from './utils/storage';
import { ActiveRecordingSession, startScreenRecording } from './utils/recorder';
import { MainScreen } from './components/MainScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { RecordingsScreen } from './components/RecordingsScreen';
import { CropToolScreen } from './components/CropToolScreen';
import { RegionSelectorOverlay } from './components/RegionSelectorOverlay';

export function App() {
  const [route, setRoute] = useState<AppRoute>('main');
  const [showRegionEditor, setShowRegionEditor] = useState(false);

  const [settings, setSettings] = useState<RecordingSettings>(() => loadRecordingSettings());
  const [recordings, setRecordings] = useState<RecordedVideoItem[]>([]);
  const [isLoadingRecordings, setIsLoadingRecordings] = useState(true);

  const [session, setSession] = useState<RecordingSessionState>({
    phase: 'IDLE',
    elapsedMillis: 0,
    activeResolutionLabel: '',
    activeAudioLabel: '',
    lastSavedName: null,
    errorMessage: null,
    warningMessage: null,
  });

  const activeSessionRef = useRef<ActiveRecordingSession | null>(null);
  const timerIntervalRef = useRef<number | null>(null);
  const elapsedBeforePauseRef = useRef<number>(0);
  const segmentStartMsRef = useRef<number>(0);

  const isDarkTheme = settings.darkThemeOverride ?? true;

  useEffect(() => {
    document.documentElement.setAttribute(
      'data-theme',
      isDarkTheme ? 'studio_dark' : 'studio_light',
    );
  }, [isDarkTheme]);

  useEffect(() => {
    saveRecordingSettings(settings);
  }, [settings]);

  useEffect(() => {
    let active = true;
    loadSavedRecordings()
      .then((items) => {
        if (active) setRecordings(items);
      })
      .finally(() => {
        if (active) setIsLoadingRecordings(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    return () => {
      if (timerIntervalRef.current !== null) {
        window.clearInterval(timerIntervalRef.current);
      }
    };
  }, []);

  const canvasSize = useMemo(
    () => getEffectiveCanvasSize(settings.orientationMode),
    [settings.orientationMode],
  );

  const selectedPixelRegion = useMemo(
    () => toPixelRegion(settings.selectedRegion, canvasSize.width, canvasSize.height),
    [settings.selectedRegion, canvasSize.width, canvasSize.height],
  );

  const startTimerTick = () => {
    if (timerIntervalRef.current !== null) {
      window.clearInterval(timerIntervalRef.current);
    }
    segmentStartMsRef.current = Date.now();
    timerIntervalRef.current = window.setInterval(() => {
      const currentElapsed =
        elapsedBeforePauseRef.current + (Date.now() - segmentStartMsRef.current);
      setSession((prev) => ({ ...prev, elapsedMillis: currentElapsed }));
    }, 100);
  };

  const stopTimerTick = () => {
    if (timerIntervalRef.current !== null) {
      window.clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
  };

  const handleStartRecord = async () => {
    if (session.phase !== 'IDLE') return;
    try {
      const activeRec = await startScreenRecording(settings);
      activeSessionRef.current = activeRec;
      elapsedBeforePauseRef.current = 0;
      setSession({
        phase: 'RECORDING',
        elapsedMillis: 0,
        activeResolutionLabel: activeRec.resolutionLabel,
        activeAudioLabel: activeRec.audioLabel,
        lastSavedName: null,
        errorMessage: null,
        warningMessage: activeRec.warningMessage,
      });
      startTimerTick();
    } catch (err) {
      setSession((prev) => ({
        ...prev,
        phase: 'IDLE',
        errorMessage:
          err instanceof Error ? err.message : 'Unable to start screen recording.',
      }));
    }
  };

  const handlePauseResume = () => {
    const activeRec = activeSessionRef.current;
    if (!activeRec) return;
    if (session.phase === 'RECORDING') {
      elapsedBeforePauseRef.current += Date.now() - segmentStartMsRef.current;
      stopTimerTick();
      activeRec.pause();
      setSession((prev) => ({
        ...prev,
        phase: 'PAUSED',
        elapsedMillis: elapsedBeforePauseRef.current,
      }));
    } else if (session.phase === 'PAUSED') {
      activeRec.resume();
      setSession((prev) => ({ ...prev, phase: 'RECORDING' }));
      startTimerTick();
    }
  };

  const handleStopRecord = async () => {
    const activeRec = activeSessionRef.current;
    if (!activeRec) return;
    stopTimerTick();
    setSession((prev) => ({ ...prev, phase: 'FINALIZING' }));
    try {
      const savedItem = await activeRec.stop();
      activeSessionRef.current = null;
      await saveRecordingToDb(savedItem);
      setRecordings((prev) => [savedItem, ...prev]);
      setSession((prev) => ({
        ...prev,
        phase: 'IDLE',
        elapsedMillis: 0,
        lastSavedName: savedItem.displayName,
      }));
    } catch (err) {
      activeSessionRef.current = null;
      setSession((prev) => ({
        ...prev,
        phase: 'IDLE',
        elapsedMillis: 0,
        errorMessage: err instanceof Error ? err.message : 'Failed to finalize recording.',
      }));
    }
  };

  const handleDeleteRecording = (item: RecordedVideoItem) => {
    URL.revokeObjectURL(item.url);
    setRecordings((prev) => prev.filter((r) => r.id !== item.id));
    void deleteRecordingFromDb(item.id);
  };

  const isIdle = session.phase === 'IDLE';

  return (
    <>
      {route === 'main' && (
        <MainScreen
          settings={settings}
          session={session}
          recordings={recordings}
          selectedPixelRegion={selectedPixelRegion}
          canvasWidthPx={canvasSize.width}
          canvasHeightPx={canvasSize.height}
          isDarkTheme={isDarkTheme}
          onToggleTheme={() =>
            setSettings((s) => ({ ...s, darkThemeOverride: !(s.darkThemeOverride ?? true) }))
          }
          onOpenCropTool={() => setRoute('crop')}
          onOpenRecordings={() => setRoute('recordings')}
          onOpenSettings={() => setRoute('settings')}
          onStartRecordClick={() => void handleStartRecord()}
          onPauseResumeClick={handlePauseResume}
          onStopClick={() => void handleStopRecord()}
          onSelectMode={(recordingMode) => setSettings((s) => ({ ...s, recordingMode }))}
          onSelectOrientationMode={(orientationMode) =>
            setSettings((s) => ({ ...s, orientationMode }))
          }
          onSelectAudioSource={(audioSource) => setSettings((s) => ({ ...s, audioSource }))}
          onSelectBitrate={(videoBitrate) =>
            setSettings((s) => ({ ...s, videoBitrate, customVideoBitrateMbps: null }))
          }
          onSaveCustomBitrateMbps={(mbps) =>
            setSettings((s) => ({ ...s, customVideoBitrateMbps: mbps }))
          }
          onResetCustomBitrate={() =>
            setSettings((s) => ({ ...s, customVideoBitrateMbps: null }))
          }
          onSelectRegionAnimation={(regionAnimation) =>
            setSettings((s) => ({ ...s, regionAnimation }))
          }
          onEditRegion={() => setShowRegionEditor(true)}
          onDismissMessages={() =>
            setSession((prev) => ({ ...prev, errorMessage: null, warningMessage: null }))
          }
        />
      )}

      {route === 'settings' && (
        <SettingsScreen
          settings={settings}
          isIdle={isIdle}
          onBack={() => setRoute('main')}
          onSelectBitrateMode={(bitrateMode) => setSettings((s) => ({ ...s, bitrateMode }))}
          onSelectColorDepth={(colorDepth) => setSettings((s) => ({ ...s, colorDepth }))}
          onSelectVideoFps={(videoFps) =>
            setSettings((s) => ({ ...s, videoFps, customVideoFps: null }))
          }
          onSelectGopInterval={(gopInterval) =>
            setSettings((s) => ({ ...s, gopInterval, customGopSeconds: null }))
          }
          onSelectColorRange={(colorRange) => setSettings((s) => ({ ...s, colorRange }))}
          onSelectAudioFormat={(audioFormat) => setSettings((s) => ({ ...s, audioFormat }))}
          onSaveCustomFps={(fps) => setSettings((s) => ({ ...s, customVideoFps: fps }))}
          onResetCustomFps={() => setSettings((s) => ({ ...s, customVideoFps: null }))}
          onSaveCustomGopSeconds={(sec) => setSettings((s) => ({ ...s, customGopSeconds: sec }))}
          onResetCustomGop={() => setSettings((s) => ({ ...s, customGopSeconds: null }))}
          onSaveCustomAudio={(sampleRateHz, bitrateBps) =>
            setSettings((s) => ({
              ...s,
              customAudioSampleRateHz: sampleRateHz,
              customAudioBitrateBps: bitrateBps,
            }))
          }
          onResetCustomAudio={() =>
            setSettings((s) => ({
              ...s,
              customAudioSampleRateHz: null,
              customAudioBitrateBps: null,
            }))
          }
        />
      )}

      {route === 'recordings' && (
        <RecordingsScreen
          recordings={recordings}
          isLoading={isLoadingRecordings}
          onBack={() => setRoute('main')}
          onDelete={handleDeleteRecording}
        />
      )}

      {route === 'crop' && <CropToolScreen onBack={() => setRoute('main')} />}

      {showRegionEditor && (
        <RegionSelectorOverlay
          initialRegion={settings.selectedRegion}
          screenWidthPx={canvasSize.width}
          screenHeightPx={canvasSize.height}
          regionAnimation={settings.regionAnimation}
          onConfirm={(region: NormalizedRegion) => {
            setSettings((s) => ({
              ...s,
              recordingMode: 'REGION',
              selectedRegion: region,
            }));
            setShowRegionEditor(false);
          }}
          onCancel={() => setShowRegionEditor(false)}
        />
      )}
    </>
  );
}

export default App;
