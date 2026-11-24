import React from 'react';
import {
  Crop,
  Film,
  Fullscreen,
  Mic,
  Moon,
  Pause,
  Play,
  Radio,
  RectangleHorizontal,
  Settings,
  Square,
  Sun,
  Video,
  Volume2,
  Eye,
  EyeOff,
  SlidersHorizontal,
  Smartphone,
  Monitor,
  Layers,
} from 'lucide-react';
import {
  AudioSourceOption,
  BITRATE_OPTIONS,
  formatElapsedTimer,
  OrientationMode,
  PixelRegion,
  RecordedVideoItem,
  RecordingMode,
  RecordingSessionState,
  RecordingSettings,
  RegionAnimationOption,
  VideoBitrateOption,
} from '../types/framecut';
import { RegionBorderCanvas } from './RegionBorderCanvas';
import { CustomNumericInputBox } from './CustomNumericInputBox';

interface MainScreenProps {
  settings: RecordingSettings;
  session: RecordingSessionState;
  recordings: RecordedVideoItem[];
  selectedPixelRegion: PixelRegion;
  canvasWidthPx: number;
  canvasHeightPx: number;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onOpenCropTool: () => void;
  onOpenRecordings: () => void;
  onOpenSettings: () => void;
  onStartRecordClick: () => void;
  onPauseResumeClick: () => void;
  onStopClick: () => void;
  onSelectMode: (mode: RecordingMode) => void;
  onSelectOrientationMode: (mode: OrientationMode) => void;
  onSelectAudioSource: (source: AudioSourceOption) => void;
  onSelectBitrate: (bitrate: VideoBitrateOption) => void;
  onSaveCustomBitrateMbps: (mbps: number) => void;
  onResetCustomBitrate: () => void;
  onSelectRegionAnimation: (anim: RegionAnimationOption) => void;
  onEditRegion: () => void;
  onDismissMessages: () => void;
}

interface OptionTileProps {
  title: string;
  subtitle: string;
  icon: React.ReactNode;
  selected: boolean;
  enabled: boolean;
  onClick: () => void;
}

const OptionTile: React.FC<OptionTileProps> = ({
  title,
  subtitle,
  icon,
  selected,
  enabled,
  onClick,
}) => (
  <button
    type="button"
    disabled={!enabled}
    onClick={onClick}
    className="flex flex-1 flex-col items-start gap-2 rounded-2xl border p-3.5 text-left transition-all cursor-pointer disabled:cursor-not-allowed"
    style={{
      backgroundColor: selected ? 'var(--fc-secondary-container)' : 'var(--fc-surface-variant)',
      borderColor: selected ? 'var(--fc-secondary)' : 'var(--fc-outline-variant)',
      borderWidth: selected ? '1.5px' : '1px',
      color: selected ? 'var(--fc-on-secondary-container)' : 'var(--fc-on-surface)',
    }}
  >
    <div className="flex w-full items-center justify-between">
      <span
        className="flex h-8 w-8 items-center justify-center rounded-lg"
        style={{
          backgroundColor: selected ? 'var(--fc-secondary)' : 'var(--fc-surface)',
          color: selected ? 'var(--fc-on-secondary)' : 'var(--fc-on-surface-variant)',
        }}
      >
        {icon}
      </span>
      {selected && (
        <span
          className="h-2 w-2 rounded-full"
          style={{ backgroundColor: 'var(--fc-secondary)' }}
        />
      )}
    </div>
    <div>
      <div className="text-sm font-semibold leading-tight">{title}</div>
      <div
        className="mt-0.5 text-xs leading-snug"
        style={{
          color: selected ? 'var(--fc-on-secondary-container)' : 'var(--fc-on-surface-variant)',
          opacity: selected ? 0.85 : 1,
        }}
      >
        {subtitle}
      </div>
    </div>
  </button>
);

const SectionCard: React.FC<{ title: string; children: React.ReactNode }> = ({
  title,
  children,
}) => (
  <section
    className="rounded-[20px] border p-4 space-y-3"
    style={{
      backgroundColor: 'var(--fc-surface)',
      borderColor: 'var(--fc-outline-variant)',
    }}
  >
    <h2 className="font-display text-sm font-semibold tracking-tight">{title}</h2>
    {children}
  </section>
);

export const MainScreen: React.FC<MainScreenProps> = ({
  settings,
  session,
  recordings,
  selectedPixelRegion,
  canvasWidthPx,
  canvasHeightPx,
  isDarkTheme,
  onToggleTheme,
  onOpenCropTool,
  onOpenRecordings,
  onOpenSettings,
  onStartRecordClick,
  onPauseResumeClick,
  onStopClick,
  onSelectMode,
  onSelectOrientationMode,
  onSelectAudioSource,
  onSelectBitrate,
  onSaveCustomBitrateMbps,
  onResetCustomBitrate,
  onSelectRegionAnimation,
  onEditRegion,
  onDismissMessages,
}) => {
  const isIdle = session.phase === 'IDLE';
  const bannerText = session.errorMessage ?? session.warningMessage;
  const isError = session.errorMessage !== null;

  const statusMeta = (() => {
    switch (session.phase) {
      case 'RECORDING':
        return {
          label: 'Recording',
          accent: 'var(--fc-record-accent)',
          container: 'var(--fc-record-container)',
        };
      case 'PAUSED':
        return {
          label: 'Paused',
          accent: 'var(--fc-paused-accent)',
          container: 'var(--fc-paused-container)',
        };
      case 'FINALIZING':
        return {
          label: 'Finalizing MP4…',
          accent: 'var(--fc-secondary)',
          container: 'var(--fc-secondary-container)',
        };
      default:
        return {
          label: 'Ready to record',
          accent: 'var(--fc-ready-accent)',
          container: 'var(--fc-ready-container)',
        };
    }
  })();

  const orientationLabel = settings.orientationMode === 'PORTRAIT' ? 'Portrait' : 'Landscape';
  const subtitleSummary = isIdle
    ? `${
        settings.recordingMode === 'FULL_SCREEN'
          ? `Full Screen (${canvasWidthPx}×${canvasHeightPx})`
          : `Region / Area (${selectedPixelRegion.width}×${selectedPixelRegion.height})`
      } • ${orientationLabel} • ${
        settings.audioSource === 'INTERNAL' ? 'Internal Audio' : 'Microphone'
      }`
    : [session.activeResolutionLabel, orientationLabel, session.activeAudioLabel]
        .filter(Boolean)
        .join(' • ');

  const previewAspect = Math.max(0.45, Math.min(2.2, canvasWidthPx / Math.max(1, canvasHeightPx)));
  const showAnim = settings.regionAnimation === 'SHOW';

  const consoleSection = (
    <div className="space-y-3.5">
      {bannerText && (
        <div
          className="flex items-center justify-between gap-3 rounded-2xl px-4 py-2.5 text-xs"
          style={{
            backgroundColor: isError ? 'var(--fc-error-container)' : 'var(--fc-paused-container)',
            color: isError ? 'var(--fc-on-error-container)' : 'var(--fc-on-surface)',
          }}
        >
          <span className="flex-1 leading-relaxed">{bannerText}</span>
          <button
            type="button"
            onClick={onDismissMessages}
            className="shrink-0 font-semibold px-2 py-1 rounded-lg hover:opacity-80 cursor-pointer"
          >
            Close
          </button>
        </div>
      )}

      {/* Recording Console Card */}
      <div
        className="overflow-hidden rounded-[24px] border transition-colors"
        style={{
          backgroundColor: 'var(--fc-surface)',
          borderColor: 'var(--fc-outline)',
        }}
      >
        <div
          className="flex flex-col items-center gap-4 p-6 text-center"
          style={{
            background: `linear-gradient(180deg, color-mix(in srgb, ${statusMeta.container} 55%, transparent), var(--fc-surface))`,
          }}
        >
          {/* Status Pill */}
          <div
            className="inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-sm font-semibold"
            style={{
              backgroundColor: statusMeta.container,
              color: statusMeta.accent,
            }}
          >
            <span
              className={`h-2 w-2 rounded-full ${
                session.phase === 'RECORDING' ? 'animate-pulse' : ''
              }`}
              style={{ backgroundColor: statusMeta.accent }}
            />
            <span>{statusMeta.label}</span>
          </div>

          {/* Timer */}
          <div className="font-mono-tnum text-[40px] font-semibold leading-none tracking-tight">
            {formatElapsedTimer(session.elapsedMillis)}
          </div>

          {/* Active pipeline summary */}
          <div
            className="font-mono-tnum text-xs"
            style={{ color: 'var(--fc-on-surface-variant)' }}
          >
            {subtitleSummary}
          </div>

          {/* Action Buttons */}
          {session.phase === 'IDLE' && (
            <button
              type="button"
              onClick={onStartRecordClick}
              className="flex h-14 w-full items-center justify-center gap-2.5 rounded-2xl text-base font-semibold transition-transform active:scale-[0.99] cursor-pointer font-display"
              style={{
                backgroundColor: 'var(--fc-record-accent)',
                color: 'var(--fc-on-primary)',
              }}
            >
              <Radio className="h-5 w-5" />
              <span>Record</span>
            </button>
          )}

          {(session.phase === 'RECORDING' || session.phase === 'PAUSED') && (
            <div className="flex w-full gap-3">
              <button
                type="button"
                onClick={onPauseResumeClick}
                className="flex h-[54px] flex-1 items-center justify-center gap-2 rounded-2xl text-sm font-semibold transition-opacity hover:opacity-90 cursor-pointer"
                style={{
                  backgroundColor: 'var(--fc-secondary-container)',
                  color: 'var(--fc-on-secondary-container)',
                }}
              >
                {session.phase === 'PAUSED' ? (
                  <>
                    <Play className="h-4 w-4" />
                    <span>Resume</span>
                  </>
                ) : (
                  <>
                    <Pause className="h-4 w-4" />
                    <span>Pause</span>
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={onStopClick}
                className="flex h-[54px] flex-1 items-center justify-center gap-2 rounded-2xl text-sm font-semibold transition-opacity hover:opacity-95 cursor-pointer"
                style={{
                  backgroundColor: 'var(--fc-record-accent)',
                  color: 'var(--fc-on-primary)',
                }}
              >
                <Square className="h-4 w-4 fill-current" />
                <span>Stop</span>
              </button>
            </div>
          )}

          {session.phase === 'FINALIZING' && (
            <button
              type="button"
              disabled
              className="flex h-14 w-full items-center justify-center rounded-2xl text-sm font-semibold opacity-70"
              style={{
                backgroundColor: 'var(--fc-surface-variant)',
                color: 'var(--fc-on-surface-variant)',
              }}
            >
              Finalizing MP4…
            </button>
          )}
        </div>
      </div>

      {/* Saved Video Quick Banner */}
      {isIdle && session.lastSavedName && (
        <button
          type="button"
          onClick={onOpenRecordings}
          className="flex w-full items-center justify-between gap-3 rounded-2xl px-4 py-3 text-left transition-opacity hover:opacity-90 cursor-pointer"
          style={{
            backgroundColor: 'var(--fc-ready-container)',
            color: 'var(--fc-on-surface)',
          }}
        >
          <div className="min-w-0 flex-1">
            <div className="text-xs font-semibold" style={{ color: 'var(--fc-ready-accent)' }}>
              Saved to FrameCut Library
            </div>
            <div className="truncate text-xs opacity-90">{session.lastSavedName}</div>
          </div>
          <span className="text-xs font-semibold" style={{ color: 'var(--fc-ready-accent)' }}>
            Play
          </span>
        </button>
      )}

      {/* Recordings Library Button */}
      <button
        type="button"
        onClick={onOpenRecordings}
        className="flex h-[52px] w-full items-center justify-center gap-2.5 rounded-2xl border text-sm font-semibold transition-colors hover:opacity-90 cursor-pointer"
        style={{
          backgroundColor: 'transparent',
          borderColor: 'var(--fc-outline)',
          color: 'var(--fc-on-surface)',
        }}
      >
        <Film className="h-4 w-4" />
        <span>
          {recordings.length === 0 ? 'Recorded Videos' : `Recorded Videos (${recordings.length})`}
        </span>
      </button>
    </div>
  );

  const settingsGridCards = (
    <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-2">
      <div className={`space-y-3.5 ${isIdle ? '' : 'opacity-55 pointer-events-none'}`}>
        <SectionCard title="Recording Mode">
          <div className="flex gap-2.5">
            <OptionTile
              title="Full Screen"
              subtitle="Entire display capture"
              icon={<Fullscreen className="h-4 w-4" />}
              selected={settings.recordingMode === 'FULL_SCREEN'}
              enabled={isIdle}
              onClick={() => onSelectMode('FULL_SCREEN')}
            />
            <OptionTile
              title="Region / Area"
              subtitle="Hardware-sized region capture"
              icon={<RectangleHorizontal className="h-4 w-4" />}
              selected={settings.recordingMode === 'REGION'}
              enabled={isIdle}
              onClick={() => onSelectMode('REGION')}
            />
          </div>
        </SectionCard>

        {settings.recordingMode === 'REGION' && (
          <SectionCard title="Selected Region">
            <div className="flex items-center gap-3.5">
              <div
                className="relative w-[74px] shrink-0 overflow-hidden rounded-xl border"
                style={{
                  aspectRatio: `${previewAspect}`,
                  backgroundColor: 'var(--fc-surface-variant)',
                  borderColor: 'var(--fc-outline)',
                }}
              >
                <RegionBorderCanvas
                  region={settings.selectedRegion}
                  animate={showAnim && session.phase === 'RECORDING'}
                  showBorder={showAnim}
                  strokeWidth={2}
                  scrimColor="rgba(0,0,0,0.45)"
                />
              </div>
              <div className="min-w-0 flex-1 space-y-1.5">
                <div className="font-display text-base font-semibold">
                  {selectedPixelRegion.width} × {selectedPixelRegion.height} px
                </div>
                <div
                  className="font-mono-tnum text-[11px]"
                  style={{ color: 'var(--fc-on-surface-variant)' }}
                >
                  Position ({selectedPixelRegion.left}, {selectedPixelRegion.top})
                </div>
                <button
                  type="button"
                  disabled={!isIdle}
                  onClick={onEditRegion}
                  className="flex w-full items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold transition-opacity hover:opacity-90 cursor-pointer"
                  style={{
                    backgroundColor: 'var(--fc-secondary-container)',
                    color: 'var(--fc-on-secondary-container)',
                  }}
                >
                  <SlidersHorizontal className="h-3.5 w-3.5" />
                  <span>Adjust Region</span>
                </button>
              </div>
            </div>

            <div
              className="flex items-center justify-between gap-2 rounded-xl px-3 py-2"
              style={{ backgroundColor: 'var(--fc-surface-variant)' }}
            >
              <div className="flex items-center gap-2 text-xs">
                <Layers className="h-3.5 w-3.5" style={{ color: 'var(--fc-ready-accent)' }} />
                <div>
                  <div className="font-medium">Floating border over capture</div>
                  <div className="text-[11px]" style={{ color: 'var(--fc-on-surface-variant)' }}>
                    Active during region recording preview
                  </div>
                </div>
              </div>
            </div>
          </SectionCard>
        )}

        <SectionCard title="Orientation">
          <div className="flex gap-2.5">
            <OptionTile
              title="Portrait"
              subtitle="Vertical for Full Screen & Region"
              icon={<Smartphone className="h-4 w-4" />}
              selected={settings.orientationMode === 'PORTRAIT'}
              enabled={isIdle}
              onClick={() => onSelectOrientationMode('PORTRAIT')}
            />
            <OptionTile
              title="Landscape"
              subtitle="Horizontal for Full Screen & Region"
              icon={<Monitor className="h-4 w-4" />}
              selected={settings.orientationMode === 'LANDSCAPE'}
              enabled={isIdle}
              onClick={() => onSelectOrientationMode('LANDSCAPE')}
            />
          </div>
        </SectionCard>
      </div>

      <div className="space-y-3.5">
        <div className={`space-y-3.5 ${isIdle ? '' : 'opacity-55 pointer-events-none'}`}>
          <SectionCard title="Audio Source">
            <div className="flex gap-2.5">
              <OptionTile
                title="Internal Audio"
                subtitle="Device media & app playback"
                icon={<Volume2 className="h-4 w-4" />}
                selected={settings.audioSource === 'INTERNAL'}
                enabled={isIdle}
                onClick={() => onSelectAudioSource('INTERNAL')}
              />
              <OptionTile
                title="Microphone"
                subtitle="External mic prioritized automatically"
                icon={<Mic className="h-4 w-4" />}
                selected={settings.audioSource === 'MICROPHONE'}
                enabled={isIdle}
                onClick={() => onSelectAudioSource('MICROPHONE')}
              />
            </div>
          </SectionCard>

          <SectionCard title="Video Bitrate">
            <div className="flex flex-wrap gap-2">
              {BITRATE_OPTIONS.map((opt) => {
                const selected =
                  settings.customVideoBitrateMbps === null && settings.videoBitrate === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    disabled={!isIdle}
                    onClick={() => onSelectBitrate(opt.id)}
                    className="rounded-xl border px-3 py-2 text-xs font-medium font-mono-tnum transition-colors cursor-pointer"
                    style={{
                      backgroundColor: selected
                        ? 'var(--fc-secondary-container)'
                        : 'var(--fc-surface-variant)',
                      borderColor: selected ? 'var(--fc-secondary)' : 'var(--fc-outline-variant)',
                      color: selected ? 'var(--fc-on-secondary-container)' : 'var(--fc-on-surface)',
                    }}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>

            <CustomNumericInputBox
              label="Custom Bitrate (Mbps)"
              placeholder="Enter any Mbps value"
              suffixText="Mbps"
              savedValueText={settings.customVideoBitrateMbps?.toString() ?? ''}
              activeBadgeText={
                settings.customVideoBitrateMbps
                  ? `Custom Bitrate: ${settings.customVideoBitrateMbps} Mbps`
                  : null
              }
              allowDecimal={false}
              enabled={isIdle}
              parseAndSave={(raw) => {
                const parsed = parseInt(raw.trim(), 10);
                if (!Number.isFinite(parsed) || parsed <= 0) return false;
                onSaveCustomBitrateMbps(parsed);
                return true;
              }}
              onReset={onResetCustomBitrate}
            />
          </SectionCard>
        </div>

        <SectionCard title="Region Border Animation">
          <div className="flex gap-2.5">
            <OptionTile
              title="Show"
              subtitle="Animated red capture frame"
              icon={<Eye className="h-4 w-4" />}
              selected={settings.regionAnimation === 'SHOW'}
              enabled={true}
              onClick={() => onSelectRegionAnimation('SHOW')}
            />
            <OptionTile
              title="Hide"
              subtitle="Clean borderless capture"
              icon={<EyeOff className="h-4 w-4" />}
              selected={settings.regionAnimation === 'HIDE'}
              enabled={true}
              onClick={() => onSelectRegionAnimation('HIDE')}
            />
          </div>
        </SectionCard>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--fc-bg)' }}>
      {/* Top App Bar */}
      <header
        className="sticky top-0 z-30 border-b backdrop-blur-md"
        style={{
          backgroundColor: 'color-mix(in srgb, var(--fc-bg) 90%, transparent)',
          borderColor: 'var(--fc-outline-variant)',
        }}
      >
        <div className="mx-auto flex h-16 max-w-[1280px] items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-2.5">
            <div
              className="flex h-[30px] w-[30px] items-center justify-center rounded-lg"
              style={{
                backgroundColor: 'var(--fc-record-container)',
                color: 'var(--fc-record-accent)',
              }}
            >
              <Video className="h-4 w-4" />
            </div>
            <h1 className="font-display text-2xl font-bold tracking-tight">FrameCut</h1>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={onToggleTheme}
              title={isDarkTheme ? 'Switch to light theme' : 'Switch to dark theme'}
              className="flex h-11 w-11 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              {isDarkTheme ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </button>
            <button
              type="button"
              onClick={onOpenCropTool}
              title="Open photo and video crop tool"
              className="flex h-11 w-11 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              <Crop className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={onOpenRecordings}
              title="Open recorded videos"
              className="relative flex h-11 w-11 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              <Film className="h-5 w-5" />
              {recordings.length > 0 && (
                <span
                  className="absolute right-1.5 top-1.5 flex h-4 min-w-[16px] items-center justify-center rounded-full px-1 text-[10px] font-bold text-white"
                  style={{ backgroundColor: 'var(--fc-record-accent)' }}
                >
                  {recordings.length}
                </span>
              )}
            </button>
            <button
              type="button"
              onClick={onOpenSettings}
              title="Open encoding settings"
              className="flex h-11 w-11 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              <Settings className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Responsive Two-Pane / Single-Pane Layout */}
      <main className="mx-auto max-w-[1280px] px-4 py-4 sm:px-6 sm:py-6">
        <div className="grid grid-cols-1 gap-5 md:grid-cols-12">
          <div className="md:col-span-5 lg:col-span-4">{consoleSection}</div>
          <div className="md:col-span-7 lg:col-span-8">{settingsGridCards}</div>
        </div>
      </main>
    </div>
  );
};
