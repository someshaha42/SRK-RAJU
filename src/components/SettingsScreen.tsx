import React from 'react';
import {
  ArrowLeft,
  AudioLines,
  Cpu,
  FileVideo,
  Gauge,
  KeyRound,
  Lock,
  Palette,
  Sliders,
  Sparkles,
  SunMedium,
} from 'lucide-react';
import {
  AudioFormatOption,
  BitrateModeOption,
  ColorDepthOption,
  ColorRangeOption,
  GopIntervalOption,
  RecordingSettings,
  VideoFpsOption,
} from '../types/framecut';
import { CustomNumericInputBox } from './CustomNumericInputBox';

interface SettingsScreenProps {
  settings: RecordingSettings;
  isIdle: boolean;
  onBack: () => void;
  onSelectBitrateMode: (mode: BitrateModeOption) => void;
  onSelectColorDepth: (depth: ColorDepthOption) => void;
  onSelectVideoFps: (fps: VideoFpsOption) => void;
  onSelectGopInterval: (gop: GopIntervalOption) => void;
  onSelectColorRange: (range: ColorRangeOption) => void;
  onSelectAudioFormat: (fmt: AudioFormatOption) => void;
  onSaveCustomFps: (fps: number) => void;
  onResetCustomFps: () => void;
  onSaveCustomGopSeconds: (sec: number) => void;
  onResetCustomGop: () => void;
  onSaveCustomAudio: (sampleRateHz: number | null, bitrateBps: number | null) => void;
  onResetCustomAudio: () => void;
}

const PipelineCard: React.FC<{
  title: string;
  hint: string;
  children: React.ReactNode;
}> = ({ title, hint, children }) => (
  <section
    className="rounded-[20px] border p-4 space-y-3"
    style={{
      backgroundColor: 'var(--fc-surface)',
      borderColor: 'var(--fc-outline-variant)',
    }}
  >
    <div>
      <h2 className="font-display text-sm font-semibold">{title}</h2>
      <p className="text-xs" style={{ color: 'var(--fc-on-surface-variant)' }}>
        {hint}
      </p>
    </div>
    {children}
  </section>
);

const PipelineOptionTile: React.FC<{
  title: string;
  subtitle: string;
  icon: React.ReactNode;
  selected: boolean;
  enabled: boolean;
  onClick: () => void;
}> = ({ title, subtitle, icon, selected, enabled, onClick }) => (
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
    <span
      className="flex h-8 w-8 items-center justify-center rounded-lg"
      style={{
        backgroundColor: selected ? 'var(--fc-secondary)' : 'var(--fc-surface)',
        color: selected ? 'var(--fc-on-secondary)' : 'var(--fc-on-surface-variant)',
      }}
    >
      {icon}
    </span>
    <div>
      <div className="text-sm font-semibold leading-tight">{title}</div>
      <div
        className="mt-0.5 text-xs leading-snug"
        style={{
          color: selected ? 'var(--fc-on-secondary-container)' : 'var(--fc-on-surface-variant)',
        }}
      >
        {subtitle}
      </div>
    </div>
  </button>
);

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  isIdle,
  onBack,
  onSelectBitrateMode,
  onSelectColorDepth,
  onSelectVideoFps,
  onSelectGopInterval,
  onSelectColorRange,
  onSelectAudioFormat,
  onSaveCustomFps,
  onResetCustomFps,
  onSaveCustomGopSeconds,
  onResetCustomGop,
  onSaveCustomAudio,
  onResetCustomAudio,
}) => {
  const customKhz = settings.customAudioSampleRateHz
    ? (settings.customAudioSampleRateHz / 1000).toString()
    : '';
  const customKbps = settings.customAudioBitrateBps
    ? Math.round(settings.customAudioBitrateBps / 1000).toString()
    : '';

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--fc-bg)' }}>
      <header
        className="sticky top-0 z-30 border-b backdrop-blur-md"
        style={{
          backgroundColor: 'color-mix(in srgb, var(--fc-bg) 90%, transparent)',
          borderColor: 'var(--fc-outline-variant)',
        }}
      >
        <div className="mx-auto flex h-16 max-w-[1120px] items-center gap-3 px-4 sm:px-6">
          <button
            type="button"
            onClick={onBack}
            title="Navigate back"
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="font-display text-lg font-semibold leading-tight">Encoding Settings</h1>
            <p className="font-mono-tnum text-xs" style={{ color: 'var(--fc-on-surface-variant)' }}>
              H.265/HEVC Hardware Surface Pipeline
            </p>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1120px] space-y-4 px-4 py-4 sm:px-6 sm:py-6">
        {!isIdle && (
          <div
            className="flex items-center gap-3 rounded-2xl px-4 py-3 text-sm"
            style={{
              backgroundColor: 'var(--fc-paused-container)',
              color: 'var(--fc-on-surface)',
            }}
          >
            <Lock className="h-4 w-4 shrink-0" style={{ color: 'var(--fc-paused-accent)' }} />
            <span>Stop active recording to change pipeline settings.</span>
          </div>
        )}

        <div
          className={`grid grid-cols-1 gap-3.5 md:grid-cols-2 ${
            isIdle ? '' : 'opacity-55 pointer-events-none'
          }`}
        >
          <PipelineCard
            title="Bitrate Mode"
            hint="Controls bitrate allocation behavior"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="VBR"
                subtitle="Balanced adaptive quality"
                icon={<Gauge className="h-4 w-4" />}
                selected={settings.bitrateMode === 'VBR'}
                enabled={isIdle}
                onClick={() => onSelectBitrateMode('VBR')}
              />
              <PipelineOptionTile
                title="CQ"
                subtitle="Constant target quality"
                icon={<Sliders className="h-4 w-4" />}
                selected={settings.bitrateMode === 'CQ'}
                enabled={isIdle}
                onClick={() => onSelectBitrateMode('CQ')}
              />
            </div>
          </PipelineCard>

          <PipelineCard
            title="Color Depth"
            hint="Selects hardware HEVC bit precision"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="8-Bit"
                subtitle="Standard smooth playback"
                icon={<Palette className="h-4 w-4" />}
                selected={settings.colorDepth === 'BIT_8'}
                enabled={isIdle}
                onClick={() => onSelectColorDepth('BIT_8')}
              />
              <PipelineOptionTile
                title="10-Bit"
                subtitle="Richer gradient depth"
                icon={<Sparkles className="h-4 w-4" />}
                selected={settings.colorDepth === 'BIT_10'}
                enabled={isIdle}
                onClick={() => onSelectColorDepth('BIT_10')}
              />
            </div>
          </PipelineCard>

          <PipelineCard
            title="Frame Rate (FPS)"
            hint="Sets capture smoothness per second"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="30 FPS"
                subtitle="Saves battery & heat"
                icon={<Gauge className="h-4 w-4" />}
                selected={settings.customVideoFps === null && settings.videoFps === 'FPS_30'}
                enabled={isIdle}
                onClick={() => onSelectVideoFps('FPS_30')}
              />
              <PipelineOptionTile
                title="60 FPS"
                subtitle="Fluid motion capture"
                icon={<Gauge className="h-4 w-4" />}
                selected={settings.customVideoFps === null && settings.videoFps === 'FPS_60'}
                enabled={isIdle}
                onClick={() => onSelectVideoFps('FPS_60')}
              />
            </div>
            <CustomNumericInputBox
              label="Custom FPS"
              placeholder="Enter target FPS"
              suffixText="FPS"
              savedValueText={settings.customVideoFps?.toString() ?? ''}
              activeBadgeText={
                settings.customVideoFps ? `Custom FPS: ${settings.customVideoFps} FPS` : null
              }
              allowDecimal={false}
              enabled={isIdle}
              parseAndSave={(raw) => {
                const fps = parseInt(raw.trim(), 10);
                if (!Number.isFinite(fps) || fps <= 0) return false;
                onSaveCustomFps(fps);
                return true;
              }}
              onReset={onResetCustomFps}
            />
          </PipelineCard>

          <PipelineCard
            title="Color Standard"
            hint="Fixed hardware display color gamut"
          >
            <PipelineOptionTile
              title="BT.709 (SDR)"
              subtitle="Fixed accurate SDR color"
              icon={<Palette className="h-4 w-4" />}
              selected={true}
              enabled={true}
              onClick={() => {}}
            />
          </PipelineCard>

          <PipelineCard
            title="Output Container"
            hint="Active MP4 muxing container format"
          >
            <PipelineOptionTile
              title="Standard MP4"
              subtitle="Native MediaMuxer / WebM"
              icon={<FileVideo className="h-4 w-4" />}
              selected={true}
              enabled={true}
              onClick={() => {}}
            />
          </PipelineCard>

          <PipelineCard
            title="Rendering Pipeline"
            hint="Fixed zero-copy surface callback pipeline"
          >
            <PipelineOptionTile
              title="Asynchronous Rendering"
              subtitle="Direct Surface + Async Callback"
              icon={<Cpu className="h-4 w-4" />}
              selected={true}
              enabled={true}
              onClick={() => {}}
            />
          </PipelineCard>

          <PipelineCard
            title="Keyframe Interval (GOP)"
            hint="Controls seeking & edit precision"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="1 Second"
                subtitle="Smaller file size"
                icon={<KeyRound className="h-4 w-4" />}
                selected={settings.customGopSeconds === null && settings.gopInterval === 'SEC_1'}
                enabled={isIdle}
                onClick={() => onSelectGopInterval('SEC_1')}
              />
              <PipelineOptionTile
                title="0.5 Second"
                subtitle="Faster video scrubbing"
                icon={<KeyRound className="h-4 w-4" />}
                selected={settings.customGopSeconds === null && settings.gopInterval === 'SEC_0_5'}
                enabled={isIdle}
                onClick={() => onSelectGopInterval('SEC_0_5')}
              />
            </div>
            <CustomNumericInputBox
              label="Custom Keyframe Interval (sec)"
              placeholder="≤ 1s supports decimal (e.g. 0.5), > 1s seconds"
              suffixText="s"
              savedValueText={settings.customGopSeconds?.toString() ?? ''}
              activeBadgeText={
                settings.customGopSeconds ? `Custom GOP: ${settings.customGopSeconds} s` : null
              }
              allowDecimal={true}
              supportingHint="≤ 1s supports decimal (e.g. 0.5), > 1s seconds"
              enabled={isIdle}
              parseAndSave={(raw) => {
                const val = parseFloat(raw.trim());
                if (!Number.isFinite(val) || val <= 0) return false;
                onSaveCustomGopSeconds(val);
                return true;
              }}
              onReset={onResetCustomGop}
            />
          </PipelineCard>

          <PipelineCard
            title="Color Range"
            hint="Sets luminance & contrast span"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="Limited (16–235)"
                subtitle="Standard safe compatibility"
                icon={<SunMedium className="h-4 w-4" />}
                selected={settings.colorRange === 'LIMITED'}
                enabled={isIdle}
                onClick={() => onSelectColorRange('LIMITED')}
              />
              <PipelineOptionTile
                title="Full (0–255)"
                subtitle="Maximum tonal contrast"
                icon={<SunMedium className="h-4 w-4" />}
                selected={settings.colorRange === 'FULL'}
                enabled={isIdle}
                onClick={() => onSelectColorRange('FULL')}
              />
            </div>
          </PipelineCard>

          <PipelineCard
            title="Audio Format"
            hint="Selects mono 48 kHz audio encoder"
          >
            <div className="flex gap-2.5">
              <PipelineOptionTile
                title="Mono AAC"
                subtitle="48 kHz · 128 kbps"
                icon={<AudioLines className="h-4 w-4" />}
                selected={settings.audioFormat === 'MONO_AAC'}
                enabled={isIdle}
                onClick={() => onSelectAudioFormat('MONO_AAC')}
              />
              <PipelineOptionTile
                title="Mono MP3"
                subtitle="48 kHz · 128 kbps"
                icon={<AudioLines className="h-4 w-4" />}
                selected={settings.audioFormat === 'MONO_MP3'}
                enabled={isIdle}
                onClick={() => onSelectAudioFormat('MONO_MP3')}
              />
            </div>

            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <CustomNumericInputBox
                label="Sample Rate (kHz)"
                placeholder="e.g. 48"
                suffixText="kHz"
                savedValueText={customKhz}
                activeBadgeText={
                  settings.customAudioSampleRateHz
                    ? `${Math.round(settings.customAudioSampleRateHz / 1000)} kHz`
                    : null
                }
                allowDecimal={false}
                enabled={isIdle}
                parseAndSave={(raw) => {
                  const khz = parseInt(raw.trim(), 10);
                  if (!Number.isFinite(khz) || khz < 8) return false;
                  onSaveCustomAudio(khz * 1000, settings.customAudioBitrateBps);
                  return true;
                }}
                onReset={onResetCustomAudio}
              />
              <CustomNumericInputBox
                label="Audio Bitrate (kbps)"
                placeholder="e.g. 128"
                suffixText="kbps"
                savedValueText={customKbps}
                activeBadgeText={
                  settings.customAudioBitrateBps
                    ? `${Math.round(settings.customAudioBitrateBps / 1000)} kbps`
                    : null
                }
                allowDecimal={false}
                enabled={isIdle}
                parseAndSave={(raw) => {
                  const kbps = parseInt(raw.trim(), 10);
                  if (!Number.isFinite(kbps) || kbps < 16) return false;
                  onSaveCustomAudio(settings.customAudioSampleRateHz, kbps * 1000);
                  return true;
                }}
                onReset={onResetCustomAudio}
              />
            </div>
          </PipelineCard>

          <PipelineCard
            title="Chroma Subsampling"
            hint="Active hardware surface color sampling"
          >
            <PipelineOptionTile
              title="4:2:0"
              subtitle="Hardware native YUV420/P010"
              icon={<Palette className="h-4 w-4" />}
              selected={true}
              enabled={true}
              onClick={() => {}}
            />
          </PipelineCard>
        </div>
      </main>
    </div>
  );
};
