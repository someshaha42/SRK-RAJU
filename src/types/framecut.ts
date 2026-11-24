export type AppRoute = 'main' | 'settings' | 'recordings' | 'crop';

export type RecordingMode = 'FULL_SCREEN' | 'REGION';
export type OrientationMode = 'PORTRAIT' | 'LANDSCAPE';
export type AudioSourceOption = 'INTERNAL' | 'MICROPHONE';
export type VideoBitrateOption =
  | 'MBPS_100'
  | 'MBPS_150'
  | 'MBPS_200'
  | 'MBPS_250'
  | 'MBPS_300'
  | 'MBPS_350';
export type RegionAnimationOption = 'SHOW' | 'HIDE';
export type BitrateModeOption = 'VBR' | 'CQ';
export type ColorDepthOption = 'BIT_8' | 'BIT_10';
export type VideoFpsOption = 'FPS_30' | 'FPS_60';
export type GopIntervalOption = 'SEC_1' | 'SEC_0_5';
export type ColorRangeOption = 'LIMITED' | 'FULL';
export type AudioFormatOption = 'MONO_AAC' | 'MONO_MP3';

export type RecordingPhase = 'IDLE' | 'RECORDING' | 'PAUSED' | 'FINALIZING';

export interface NormalizedRegion {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export interface PixelRegion {
  left: number;
  top: number;
  right: number;
  bottom: number;
  width: number;
  height: number;
}

export interface RecordingSettings {
  recordingMode: RecordingMode;
  orientationMode: OrientationMode;
  audioSource: AudioSourceOption;
  videoBitrate: VideoBitrateOption;
  regionAnimation: RegionAnimationOption;
  selectedRegion: NormalizedRegion;
  bitrateMode: BitrateModeOption;
  colorDepth: ColorDepthOption;
  videoFps: VideoFpsOption;
  gopInterval: GopIntervalOption;
  colorRange: ColorRangeOption;
  audioFormat: AudioFormatOption;
  darkThemeOverride: boolean | null;
  customVideoBitrateMbps: number | null;
  customVideoFps: number | null;
  customGopSeconds: number | null;
  customAudioSampleRateHz: number | null;
  customAudioBitrateBps: number | null;
}

export interface RecordedVideoItem {
  id: string;
  displayName: string;
  mimeType: string;
  width: number;
  height: number;
  durationMs: number;
  sizeBytes: number;
  dateAddedSec: number;
  url: string;
  blob?: Blob;
  modeLabel?: string;
}

export interface RecordingSessionState {
  phase: RecordingPhase;
  elapsedMillis: number;
  activeResolutionLabel: string;
  activeAudioLabel: string;
  lastSavedName: string | null;
  errorMessage: string | null;
  warningMessage: string | null;
}

export const BITRATE_OPTIONS: { id: VideoBitrateOption; mbps: number; label: string }[] = [
  { id: 'MBPS_100', mbps: 100, label: '100 Mbps' },
  { id: 'MBPS_150', mbps: 150, label: '150 Mbps' },
  { id: 'MBPS_200', mbps: 200, label: '200 Mbps' },
  { id: 'MBPS_250', mbps: 250, label: '250 Mbps' },
  { id: 'MBPS_300', mbps: 300, label: '300 Mbps' },
  { id: 'MBPS_350', mbps: 350, label: '350 Mbps' },
];

export const DEFAULT_REGION: NormalizedRegion = {
  left: 0.1,
  top: 0.2,
  right: 0.9,
  bottom: 0.7,
};

export const DEFAULT_RECORDING_SETTINGS: RecordingSettings = {
  recordingMode: 'FULL_SCREEN',
  orientationMode: 'PORTRAIT',
  audioSource: 'INTERNAL',
  videoBitrate: 'MBPS_150',
  regionAnimation: 'SHOW',
  selectedRegion: DEFAULT_REGION,
  bitrateMode: 'VBR',
  colorDepth: 'BIT_8',
  videoFps: 'FPS_30',
  gopInterval: 'SEC_1',
  colorRange: 'LIMITED',
  audioFormat: 'MONO_AAC',
  darkThemeOverride: true,
  customVideoBitrateMbps: null,
  customVideoFps: null,
  customGopSeconds: null,
  customAudioSampleRateHz: null,
  customAudioBitrateBps: null,
};

export function normalizeRegion(region: NormalizedRegion): NormalizedRegion {
  const minSpan = 0.12;
  const left = Math.max(0, Math.min(region.left, 1 - minSpan));
  const top = Math.max(0, Math.min(region.top, 1 - minSpan));
  const right = Math.max(left + minSpan, Math.min(region.right, 1));
  const bottom = Math.max(top + minSpan, Math.min(region.bottom, 1));
  return { left, top, right, bottom };
}

export function toPixelRegion(
  region: NormalizedRegion,
  canvasWidthPx: number,
  canvasHeightPx: number,
): PixelRegion {
  const norm = normalizeRegion(region);
  const align2 = (v: number) => Math.max(2, Math.round(v / 2) * 2);
  const left = Math.max(0, Math.min(canvasWidthPx - 2, Math.round(norm.left * canvasWidthPx)));
  const top = Math.max(0, Math.min(canvasHeightPx - 2, Math.round(norm.top * canvasHeightPx)));
  const rawW = Math.round((norm.right - norm.left) * canvasWidthPx);
  const rawH = Math.round((norm.bottom - norm.top) * canvasHeightPx);
  const width = Math.min(canvasWidthPx - left, align2(Math.max(16, rawW)));
  const height = Math.min(canvasHeightPx - top, align2(Math.max(16, rawH)));
  return {
    left,
    top,
    right: left + width,
    bottom: top + height,
    width,
    height,
  };
}

export function getEffectiveCanvasSize(orientation: OrientationMode): { width: number; height: number } {
  return orientation === 'PORTRAIT'
    ? { width: 1080, height: 1920 }
    : { width: 1920, height: 1080 };
}

export function formatElapsedTimer(elapsedMs: number): string {
  const totalSeconds = Math.max(0, Math.floor(elapsedMs / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`);
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

export function formatDurationShort(ms: number): string {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const mins = Math.floor(totalSec / 60);
  const secs = totalSec % 60;
  return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
}

export function formatFileSize(bytes: number): string {
  if (bytes <= 0) return '0 KB';
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  const mb = kb / 1024;
  if (mb < 1024) return `${mb.toFixed(1)} MB`;
  const gb = mb / 1024;
  return `${gb.toFixed(2)} GB`;
}

export function formatRecordingDate(epochSec: number): string {
  const d = new Date(epochSec * 1000);
  return d.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

// Crop Tool Models
export type CropMediaCategory = 'ALL' | 'PHOTO' | 'VIDEO';
export type CropMediaKind = 'PHOTO' | 'VIDEO';
export type CropAspectRatioPreset = 'FREE' | 'RATIO_1_1' | 'RATIO_4_3' | 'RATIO_16_9' | 'RATIO_9_16';
export type PhotoOutputFormatPreset = 'ORIGINAL' | 'JPEG' | 'PNG' | 'WEBP';
export type VideoOutputFormatPreset = 'ORIGINAL' | 'MP4_H264' | 'MP4_HEVC' | 'WEBM';
export type VideoCropResolutionPreset = 'ORIGINAL' | 'UHD_4K' | 'QHD_2K' | 'FHD_1080P' | 'HD_720P';
export type VideoCropFpsPreset = 'ORIGINAL' | 'FPS_60' | 'FPS_30' | 'FPS_24';

export interface NormalizedCropRect {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export const FULL_FRAME_CROP: NormalizedCropRect = { left: 0, top: 0, right: 1, bottom: 1 };
export const DEFAULT_CROP_RECT: NormalizedCropRect = { left: 0.08, top: 0.08, right: 0.92, bottom: 0.92 };

export function isFullFrameCrop(rect: NormalizedCropRect, tolerance = 0.008): boolean {
  return (
    rect.left <= tolerance &&
    rect.top <= tolerance &&
    rect.right >= 1 - tolerance &&
    rect.bottom >= 1 - tolerance
  );
}

export interface MediaInspectionResult {
  id: string;
  url: string;
  file?: File;
  kind: CropMediaKind;
  displayName: string;
  mimeType: string;
  containerLabel: string;
  codecLabel: string;
  audioCodecLabel: string | null;
  rawWidth: number;
  rawHeight: number;
  rotationDegrees: number;
  displayWidth: number;
  displayHeight: number;
  durationMs: number;
  frameRate: number | null;
  bitrateBps: number | null;
  fileSizeBytes: number;
}

export interface VideoCropFallbackConfig {
  resolutionPreset: VideoCropResolutionPreset;
  fpsPreset: VideoCropFpsPreset;
  outputFormat: VideoOutputFormatPreset;
  bitrateMbps: number;
}

export interface PhotoCropFallbackConfig {
  preserveExifAndColor: boolean;
  qualityPercent: number;
  scalePercent: number;
  outputFormat: PhotoOutputFormatPreset;
}

export interface CropExportOutput {
  url: string;
  displayName: string;
  mimeType: string;
  outputWidth: number;
  outputHeight: number;
  sizeBytes: number;
  usedOriginalStreamCopy: boolean;
}
