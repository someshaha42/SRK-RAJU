import React, { useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Crop,
  Download,
  FolderOpen,
  Maximize2,
  Move,
  Images,
  RefreshCw,
  Sliders,
  Sparkles,
  Verified,
  X,
} from 'lucide-react';
import {
  CropAspectRatioPreset,
  CropExportOutput,
  CropMediaCategory,
  DEFAULT_CROP_RECT,
  formatFileSize,
  FULL_FRAME_CROP,
  isFullFrameCrop,
  MediaInspectionResult,
  NormalizedCropRect,
  PhotoCropFallbackConfig,
  PhotoOutputFormatPreset,
  VideoCropFallbackConfig,
  VideoCropFpsPreset,
  VideoCropResolutionPreset,
  VideoOutputFormatPreset,
} from '../types/framecut';
import {
  computeCropPixels,
  createSamplePhotoFile,
  exportCroppedPhoto,
  exportCroppedVideo,
  inspectMediaFile,
} from '../utils/cropEngine';
import { RegionBorderCanvas } from './RegionBorderCanvas';

interface CropToolScreenProps {
  onBack: () => void;
}

const ASPECT_OPTIONS: { id: CropAspectRatioPreset; label: string; ratio: number | null }[] = [
  { id: 'FREE', label: 'Free', ratio: null },
  { id: 'RATIO_1_1', label: '1:1', ratio: 1 },
  { id: 'RATIO_4_3', label: '4:3', ratio: 4 / 3 },
  { id: 'RATIO_16_9', label: '16:9', ratio: 16 / 9 },
  { id: 'RATIO_9_16', label: '9:16', ratio: 9 / 16 },
];

type DragHandle = 'move' | 'nw' | 'ne' | 'sw' | 'se' | 'n' | 's' | 'w' | 'e';

type ExportFlowState =
  | { kind: 'IDLE' }
  | { kind: 'AWAITING_CONSENT' }
  | { kind: 'CONFIGURING' }
  | { kind: 'EXPORTING'; progress: number; statusText: string }
  | { kind: 'COMPLETED'; output: CropExportOutput }
  | { kind: 'FAILED'; message: string };

export const CropToolScreen: React.FC<CropToolScreenProps> = ({ onBack }) => {
  const [category, setCategory] = useState<CropMediaCategory>('ALL');
  const [isLoadingMedia, setIsLoadingMedia] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [inspection, setInspection] = useState<MediaInspectionResult | null>(null);
  const [cropRect, setCropRect] = useState<NormalizedCropRect>(DEFAULT_CROP_RECT);
  const [aspectPreset, setAspectPreset] = useState<CropAspectRatioPreset>('FREE');
  const [exportState, setExportState] = useState<ExportFlowState>({ kind: 'IDLE' });

  const [videoConfig, setVideoConfig] = useState<VideoCropFallbackConfig>({
    resolutionPreset: 'ORIGINAL',
    fpsPreset: 'ORIGINAL',
    outputFormat: 'ORIGINAL',
    bitrateMbps: 12,
  });
  const [photoConfig, setPhotoConfig] = useState<PhotoCropFallbackConfig>({
    preserveExifAndColor: true,
    qualityPercent: 100,
    scalePercent: 100,
    outputFormat: 'ORIGINAL',
  });

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const editorBoxRef = useRef<HTMLDivElement | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const isBusy = isLoadingMedia || exportState.kind === 'EXPORTING';
  const isFullFrame = isFullFrameCrop(cropRect);

  const liveCropPixels = useMemo(() => {
    if (!inspection) return { left: 0, top: 0, width: 0, height: 0 };
    return computeCropPixels(
      cropRect,
      inspection.displayWidth,
      inspection.displayHeight,
      inspection.kind === 'VIDEO',
    );
  }, [inspection, cropRect]);

  const handleOpenPicker = (cat: CropMediaCategory) => {
    setCategory(cat);
    if (fileInputRef.current) {
      fileInputRef.current.accept =
        cat === 'PHOTO' ? 'image/*' : cat === 'VIDEO' ? 'video/*' : 'image/*,video/*';
      fileInputRef.current.click();
    }
  };

  const loadFile = async (file: File) => {
    setIsLoadingMedia(true);
    setLoadError(null);
    setExportState({ kind: 'IDLE' });
    try {
      const res = await inspectMediaFile(file);
      setInspection(res);
      setCropRect(DEFAULT_CROP_RECT);
      setAspectPreset('FREE');
      const suggestedMbps = res.bitrateBps
        ? Math.max(2, Math.min(50, Math.round(res.bitrateBps / 1_000_000)))
        : 12;
      setVideoConfig({
        resolutionPreset: 'ORIGINAL',
        fpsPreset: 'ORIGINAL',
        outputFormat: 'ORIGINAL',
        bitrateMbps: suggestedMbps,
      });
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Unable to load media file.');
    } finally {
      setIsLoadingMedia(false);
    }
  };

  const handleLoadSample = async () => {
    const file = await createSamplePhotoFile();
    await loadFile(file);
  };

  const applyAspect = (presetId: CropAspectRatioPreset) => {
    setAspectPreset(presetId);
    const opt = ASPECT_OPTIONS.find((o) => o.id === presetId);
    if (!opt?.ratio || !inspection) return;
    const w = inspection.displayWidth;
    const h = inspection.displayHeight;
    const cx = (cropRect.left + cropRect.right) / 2;
    const cy = (cropRect.top + cropRect.bottom) / 2;
    let targetW = (cropRect.right - cropRect.left) * w;
    let targetH = targetW / opt.ratio;
    if (targetH > h * 0.85) {
      targetH = h * 0.85;
      targetW = targetH * opt.ratio;
    }
    if (targetW > w * 0.9) {
      targetW = w * 0.9;
      targetH = targetW / opt.ratio;
    }
    const hw = targetW / w / 2;
    const hh = targetH / h / 2;
    const clampedCx = Math.max(hw, Math.min(1 - hw, cx));
    const clampedCy = Math.max(hh, Math.min(1 - hh, cy));
    setCropRect({
      left: Math.max(0, clampedCx - hw),
      top: Math.max(0, clampedCy - hh),
      right: Math.min(1, clampedCx + hw),
      bottom: Math.min(1, clampedCy + hh),
    });
  };

  const startDrag = (kind: DragHandle, e: React.PointerEvent) => {
    if (isBusy) return;
    e.preventDefault();
    e.stopPropagation();
    const box = editorBoxRef.current?.getBoundingClientRect();
    if (!box || box.width <= 0 || box.height <= 0) return;

    const startX = e.clientX;
    const startY = e.clientY;
    const startRect = { ...cropRect };
    const minFrac = 0.08;
    if (kind !== 'move') setAspectPreset('FREE');

    const onMove = (ev: PointerEvent) => {
      const dx = (ev.clientX - startX) / box.width;
      const dy = (ev.clientY - startY) / box.height;
      if (kind === 'move') {
        const wf = startRect.right - startRect.left;
        const hf = startRect.bottom - startRect.top;
        const nl = Math.max(0, Math.min(1 - wf, startRect.left + dx));
        const nt = Math.max(0, Math.min(1 - hf, startRect.top + dy));
        setCropRect({ left: nl, top: nt, right: nl + wf, bottom: nt + hf });
        return;
      }
      let { left, top, right, bottom } = startRect;
      if (kind.includes('w')) left = Math.max(0, Math.min(right - minFrac, startRect.left + dx));
      if (kind.includes('e')) right = Math.min(1, Math.max(left + minFrac, startRect.right + dx));
      if (kind.includes('n')) top = Math.max(0, Math.min(bottom - minFrac, startRect.top + dy));
      if (kind.includes('s')) bottom = Math.min(1, Math.max(top + minFrac, startRect.bottom + dy));
      setCropRect({ left, top, right, bottom });
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const runExport = async () => {
    if (!inspection) return;
    const ctrl = new AbortController();
    abortControllerRef.current = ctrl;
    setExportState({ kind: 'EXPORTING', progress: 0.1, statusText: 'Preparing crop export…' });
    try {
      const output =
        inspection.kind === 'PHOTO'
          ? await exportCroppedPhoto(inspection, cropRect, photoConfig, isFullFrame)
          : await exportCroppedVideo(
              inspection,
              cropRect,
              videoConfig,
              isFullFrame,
              (progress, statusText) => {
                setExportState({ kind: 'EXPORTING', progress, statusText });
              },
              ctrl.signal,
            );
      setExportState({ kind: 'COMPLETED', output });
    } catch (err) {
      setExportState({
        kind: 'FAILED',
        message: err instanceof Error ? err.message : 'Crop export failed.',
      });
    }
  };

  const handleRequestExport = () => {
    if (!inspection) return;
    if (isFullFrame) {
      void runExport();
    } else {
      setExportState({ kind: 'AWAITING_CONSENT' });
    }
  };

  const downloadOutput = (output: CropExportOutput) => {
    const a = document.createElement('a');
    a.href = output.url;
    a.download = output.displayName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  const editorHandles: { kind: DragHandle; x: number; y: number; cursor: string }[] = [
    { kind: 'nw', x: cropRect.left, y: cropRect.top, cursor: 'nwse-resize' },
    { kind: 'ne', x: cropRect.right, y: cropRect.top, cursor: 'nesw-resize' },
    { kind: 'sw', x: cropRect.left, y: cropRect.bottom, cursor: 'nesw-resize' },
    { kind: 'se', x: cropRect.right, y: cropRect.bottom, cursor: 'nwse-resize' },
    { kind: 'n', x: (cropRect.left + cropRect.right) / 2, y: cropRect.top, cursor: 'ns-resize' },
    { kind: 's', x: (cropRect.left + cropRect.right) / 2, y: cropRect.bottom, cursor: 'ns-resize' },
    { kind: 'w', x: cropRect.left, y: (cropRect.top + cropRect.bottom) / 2, cursor: 'ew-resize' },
    { kind: 'e', x: cropRect.right, y: (cropRect.top + cropRect.bottom) / 2, cursor: 'ew-resize' },
  ];

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--fc-bg)' }}>
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) void loadFile(f);
          e.target.value = '';
        }}
      />

      <header
        className="sticky top-0 z-30 border-b backdrop-blur-md"
        style={{
          backgroundColor: 'color-mix(in srgb, var(--fc-bg) 90%, transparent)',
          borderColor: 'var(--fc-outline-variant)',
        }}
      >
        <div className="mx-auto flex h-16 max-w-[900px] items-center gap-3 px-4 sm:px-6">
          <button
            type="button"
            onClick={onBack}
            title="Navigate back"
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="font-display text-lg font-semibold leading-tight">Media Crop Tool</h1>
            <p className="font-mono-tnum text-xs" style={{ color: 'var(--fc-on-surface-variant)' }}>
              Universal Photo &amp; Video Crop
            </p>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[900px] space-y-4 px-4 pb-8 pt-2 sm:px-6">
        {/* Category Tabs */}
        <div
          className="grid grid-cols-3 border-b"
          style={{ borderColor: 'var(--fc-outline-variant)' }}
        >
          {(['ALL', 'PHOTO', 'VIDEO'] as CropMediaCategory[]).map((cat) => {
            const active = category === cat;
            return (
              <button
                key={cat}
                type="button"
                disabled={isBusy}
                onClick={() => handleOpenPicker(cat)}
                className="relative py-3 text-xs font-semibold tracking-wide transition-colors cursor-pointer"
                style={{
                  color: active ? 'var(--fc-primary)' : 'var(--fc-on-surface-variant)',
                }}
              >
                {cat}
                {active && (
                  <span
                    className="absolute bottom-0 left-4 right-4 h-0.5 rounded-full"
                    style={{ backgroundColor: 'var(--fc-primary)' }}
                  />
                )}
              </button>
            );
          })}
        </div>

        {/* Picker Bar */}
        <div className="flex flex-wrap gap-2.5">
          <button
            type="button"
            disabled={isBusy}
            onClick={() => handleOpenPicker(category)}
            className="flex flex-1 min-w-[180px] items-center justify-center gap-2 rounded-2xl border px-4 py-3 text-sm font-semibold transition-opacity hover:opacity-90 cursor-pointer"
            style={{
              borderColor: 'var(--fc-outline)',
              backgroundColor: 'var(--fc-surface)',
            }}
          >
            <Images className="h-4 w-4" />
            <span>{inspection ? `Pick ${category}` : 'Pick Media from Gallery'}</span>
          </button>
          <button
            type="button"
            disabled={isBusy}
            onClick={() => handleOpenPicker('ALL')}
            className="flex flex-1 min-w-[160px] items-center justify-center gap-2 rounded-2xl border px-4 py-3 text-sm font-semibold transition-opacity hover:opacity-90 cursor-pointer"
            style={{
              borderColor: 'var(--fc-outline)',
              backgroundColor: 'var(--fc-surface)',
            }}
          >
            <FolderOpen className="h-4 w-4" />
            <span>Browse Any File</span>
          </button>
          <button
            type="button"
            disabled={isBusy}
            onClick={() => void handleLoadSample()}
            className="flex items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-semibold transition-opacity hover:opacity-90 cursor-pointer"
            style={{
              backgroundColor: 'var(--fc-secondary-container)',
              color: 'var(--fc-on-secondary-container)',
            }}
          >
            <Sparkles className="h-4 w-4" />
            <span>Sample Frame</span>
          </button>
        </div>

        {loadError && (
          <div
            className="flex items-center gap-2.5 rounded-2xl px-4 py-3 text-sm"
            style={{
              backgroundColor: 'var(--fc-error-container)',
              color: 'var(--fc-on-error-container)',
            }}
          >
            <AlertTriangle className="h-4 w-4 shrink-0" />
            <span>{loadError}</span>
          </div>
        )}

        {!inspection ? (
          <div
            className="flex flex-col items-center gap-4 rounded-3xl border p-8 text-center"
            style={{
              backgroundColor: 'var(--fc-surface)',
              borderColor: 'var(--fc-outline-variant)',
            }}
          >
            <div
              className="flex h-[72px] w-[72px] items-center justify-center rounded-2xl"
              style={{
                backgroundColor: 'var(--fc-primary-container)',
                color: 'var(--fc-on-primary-container)',
              }}
            >
              <Crop className="h-8 w-8" />
            </div>
            <h2 className="font-display text-lg font-semibold">
              Select {category} Media to Crop
            </h2>
            <p
              className="max-w-lg text-xs leading-relaxed"
              style={{ color: 'var(--fc-on-surface-variant)' }}
            >
              Supports any photo or video format (JPEG, PNG, WebP, MP4, MOV, WebM). Full-frame
              exports keep 1:1 original bitstreams; sub-region crops request confirmation and save a
              new _cropped file.
            </p>
            <div className="flex flex-wrap justify-center gap-2.5 pt-1">
              <button
                type="button"
                onClick={() => handleOpenPicker(category)}
                className="flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold text-white cursor-pointer"
                style={{ backgroundColor: 'var(--fc-primary)' }}
              >
                <Images className="h-4 w-4" />
                <span>Pick Media from Gallery</span>
              </button>
              <button
                type="button"
                onClick={() => void handleLoadSample()}
                className="flex items-center gap-2 rounded-xl border px-5 py-2.5 text-sm font-semibold cursor-pointer"
                style={{
                  borderColor: 'var(--fc-outline)',
                  backgroundColor: 'var(--fc-surface-variant)',
                }}
              >
                <Sparkles className="h-4 w-4" />
                <span>Load Sample Photo</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            {/* Media Inspection Card */}
            <div
              className="rounded-2xl border p-4 space-y-2.5"
              style={{
                backgroundColor: 'var(--fc-surface)',
                borderColor: 'var(--fc-outline-variant)',
              }}
            >
              <div className="flex items-center justify-between gap-3">
                <div className="truncate text-sm font-semibold">{inspection.displayName}</div>
                <div
                  className="font-mono-tnum text-xs"
                  style={{ color: 'var(--fc-on-surface-variant)' }}
                >
                  {formatFileSize(inspection.fileSizeBytes)}
                </div>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {[
                  `${inspection.containerLabel} · ${inspection.codecLabel}`,
                  `Src ${inspection.displayWidth}×${inspection.displayHeight}`,
                  `Crop ${liveCropPixels.width}×${liveCropPixels.height}`,
                  inspection.frameRate ? `${inspection.frameRate} FPS` : null,
                ]
                  .filter(Boolean)
                  .map((pill) => (
                    <span
                      key={pill}
                      className="rounded-lg px-2.5 py-1 font-mono-tnum text-[11px]"
                      style={{ backgroundColor: 'var(--fc-surface-variant)' }}
                    >
                      {pill}
                    </span>
                  ))}
              </div>
              <div
                className="flex items-center gap-1.5 text-xs"
                style={{ color: 'var(--fc-on-surface-variant)' }}
              >
                <Verified
                  className="h-4 w-4 shrink-0"
                  style={{ color: isFullFrame ? 'var(--fc-primary)' : 'var(--fc-ready-accent)' }}
                />
                <span>
                  {isFullFrame
                    ? 'Full Frame: True 1:1 original stream copy (no re-encoding)'
                    : 'Sub-region crop: Will ask CANCEL / CONTINUE before re-encoding'}
                </span>
              </div>
            </div>

            {/* Interactive Crop Canvas */}
            <div
              className="flex justify-center rounded-2xl border p-3"
              style={{
                backgroundColor: '#0A0C10',
                borderColor: 'var(--fc-outline-variant)',
              }}
            >
              <div
                ref={editorBoxRef}
                className="relative overflow-hidden rounded-lg select-none"
                style={{
                  aspectRatio: `${inspection.displayWidth / Math.max(1, inspection.displayHeight)}`,
                  maxHeight: '420px',
                  maxWidth: '100%',
                }}
              >
                {inspection.kind === 'PHOTO' ? (
                  <img
                    src={inspection.url}
                    alt={inspection.displayName}
                    className="block h-full w-full object-contain pointer-events-none"
                  />
                ) : (
                  <video
                    src={inspection.url}
                    controls
                    muted
                    className="block h-full w-full object-contain"
                  />
                )}

                <div className="pointer-events-none absolute inset-0">
                  <RegionBorderCanvas
                    region={cropRect}
                    animate={false}
                    showBorder={true}
                    strokeWidth={2.5}
                    scrimColor="rgba(0,0,0,0.55)"
                  />
                </div>

                {/* Move interior */}
                <div
                  onPointerDown={(e) => startDrag('move', e)}
                  style={{
                    left: `${cropRect.left * 100}%`,
                    top: `${cropRect.top * 100}%`,
                    width: `${(cropRect.right - cropRect.left) * 100}%`,
                    height: `${(cropRect.bottom - cropRect.top) * 100}%`,
                  }}
                  className="absolute flex cursor-move items-center justify-center touch-none"
                >
                  <div
                    className="flex h-8 w-8 items-center justify-center rounded-full"
                    style={{ backgroundColor: 'rgba(13,15,20,0.65)', color: '#fff' }}
                  >
                    <Move className="h-3.5 w-3.5" />
                  </div>
                </div>

                {/* 8 Handles */}
                {editorHandles.map((h) => (
                  <button
                    key={h.kind}
                    type="button"
                    aria-label={`Resize ${h.kind}`}
                    onPointerDown={(e) => startDrag(h.kind, e)}
                    style={{
                      left: `${h.x * 100}%`,
                      top: `${h.y * 100}%`,
                      cursor: h.cursor,
                      transform: 'translate(-50%, -50%)',
                    }}
                    className="absolute flex h-9 w-9 items-center justify-center touch-none"
                  >
                    <span
                      className="block h-3.5 w-3.5 rounded-full border-2 border-white shadow"
                      style={{ backgroundColor: 'var(--fc-record-accent)' }}
                    />
                  </button>
                ))}
              </div>
            </div>

            {/* Aspect Ratio & Bounds Row */}
            <div className="flex flex-wrap items-center gap-2">
              {ASPECT_OPTIONS.map((opt) => {
                const selected = aspectPreset === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    disabled={isBusy}
                    onClick={() => applyAspect(opt.id)}
                    className="rounded-xl border px-3 py-1.5 text-xs font-medium cursor-pointer"
                    style={{
                      backgroundColor: selected
                        ? 'var(--fc-secondary-container)'
                        : 'var(--fc-surface)',
                      borderColor: selected ? 'var(--fc-secondary)' : 'var(--fc-outline-variant)',
                      color: selected ? 'var(--fc-on-secondary-container)' : 'var(--fc-on-surface)',
                    }}
                  >
                    {opt.label}
                  </button>
                );
              })}
              <button
                type="button"
                disabled={isBusy}
                onClick={() => {
                  setAspectPreset('FREE');
                  setCropRect(DEFAULT_CROP_RECT);
                }}
                className="flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-medium cursor-pointer"
                style={{
                  backgroundColor: 'var(--fc-surface)',
                  borderColor: 'var(--fc-outline-variant)',
                }}
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Reset</span>
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() => {
                  setAspectPreset('FREE');
                  setCropRect(FULL_FRAME_CROP);
                }}
                className="flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-medium cursor-pointer"
                style={{
                  backgroundColor: isFullFrame
                    ? 'var(--fc-secondary-container)'
                    : 'var(--fc-surface)',
                  borderColor: isFullFrame ? 'var(--fc-secondary)' : 'var(--fc-outline-variant)',
                }}
              >
                <Maximize2 className="h-3.5 w-3.5" />
                <span>Full Frame</span>
              </button>
            </div>

            {/* Export Flow Section */}
            {exportState.kind === 'CONFIGURING' && (
              <div
                className="rounded-3xl border p-5 space-y-4"
                style={{
                  backgroundColor: 'var(--fc-surface)',
                  borderColor: 'var(--fc-outline)',
                }}
              >
                <h3 className="font-display text-base font-semibold">
                  {inspection.kind === 'VIDEO'
                    ? 'Video Crop Export Configuration'
                    : 'Photo Crop Export Configuration'}
                </h3>

                {inspection.kind === 'VIDEO' ? (
                  <div className="space-y-3 text-xs">
                    <div>
                      <div className="mb-1.5 font-semibold">Output File Format</div>
                      <div className="flex flex-wrap gap-1.5">
                        {(
                          [
                            ['ORIGINAL', `Original (${inspection.containerLabel})`],
                            ['MP4_H264', 'MP4 (H.264)'],
                            ['WEBM', 'WebM (VP9)'],
                          ] as [VideoOutputFormatPreset, string][]
                        ).map(([id, label]) => (
                          <button
                            key={id}
                            type="button"
                            onClick={() => setVideoConfig((c) => ({ ...c, outputFormat: id }))}
                            className="rounded-xl border px-3 py-1.5 font-medium cursor-pointer"
                            style={{
                              backgroundColor:
                                videoConfig.outputFormat === id
                                  ? 'var(--fc-secondary-container)'
                                  : 'var(--fc-surface-variant)',
                              borderColor:
                                videoConfig.outputFormat === id
                                  ? 'var(--fc-secondary)'
                                  : 'var(--fc-outline-variant)',
                            }}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-1.5 font-semibold">Resolution</div>
                      <div className="flex flex-wrap gap-1.5">
                        {(
                          [
                            ['ORIGINAL', `Original (${liveCropPixels.width}×${liveCropPixels.height})`],
                            ['FHD_1080P', '1080P'],
                            ['HD_720P', '720P'],
                          ] as [VideoCropResolutionPreset, string][]
                        ).map(([id, label]) => (
                          <button
                            key={id}
                            type="button"
                            onClick={() => setVideoConfig((c) => ({ ...c, resolutionPreset: id }))}
                            className="rounded-xl border px-3 py-1.5 font-medium cursor-pointer"
                            style={{
                              backgroundColor:
                                videoConfig.resolutionPreset === id
                                  ? 'var(--fc-secondary-container)'
                                  : 'var(--fc-surface-variant)',
                              borderColor:
                                videoConfig.resolutionPreset === id
                                  ? 'var(--fc-secondary)'
                                  : 'var(--fc-outline-variant)',
                            }}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-1.5 font-semibold">Frame Rate</div>
                      <div className="flex flex-wrap gap-1.5">
                        {(
                          [
                            ['ORIGINAL', 'Original'],
                            ['FPS_60', '60 FPS'],
                            ['FPS_30', '30 FPS'],
                            ['FPS_24', '24 FPS'],
                          ] as [VideoCropFpsPreset, string][]
                        ).map(([id, label]) => (
                          <button
                            key={id}
                            type="button"
                            onClick={() => setVideoConfig((c) => ({ ...c, fpsPreset: id }))}
                            className="rounded-xl border px-3 py-1.5 font-medium cursor-pointer"
                            style={{
                              backgroundColor:
                                videoConfig.fpsPreset === id
                                  ? 'var(--fc-secondary-container)'
                                  : 'var(--fc-surface-variant)',
                              borderColor:
                                videoConfig.fpsPreset === id
                                  ? 'var(--fc-secondary)'
                                  : 'var(--fc-outline-variant)',
                            }}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-1 flex justify-between font-semibold">
                        <span>Target Bitrate: {videoConfig.bitrateMbps} Mbps</span>
                      </div>
                      <input
                        type="range"
                        min={2}
                        max={30}
                        value={videoConfig.bitrateMbps}
                        onChange={(e) =>
                          setVideoConfig((c) => ({
                            ...c,
                            bitrateMbps: parseInt(e.target.value, 10),
                          }))
                        }
                        className="w-full"
                      />
                    </div>
                  </div>
                ) : (
                  <div className="space-y-3 text-xs">
                    <div>
                      <div className="mb-1.5 font-semibold">Output File Format</div>
                      <div className="flex flex-wrap gap-1.5">
                        {(
                          [
                            ['ORIGINAL', `Original (${inspection.containerLabel})`],
                            ['JPEG', 'JPEG'],
                            ['PNG', 'PNG (Lossless)'],
                            ['WEBP', 'WebP'],
                          ] as [PhotoOutputFormatPreset, string][]
                        ).map(([id, label]) => (
                          <button
                            key={id}
                            type="button"
                            onClick={() => setPhotoConfig((c) => ({ ...c, outputFormat: id }))}
                            className="rounded-xl border px-3 py-1.5 font-medium cursor-pointer"
                            style={{
                              backgroundColor:
                                photoConfig.outputFormat === id
                                  ? 'var(--fc-secondary-container)'
                                  : 'var(--fc-surface-variant)',
                              borderColor:
                                photoConfig.outputFormat === id
                                  ? 'var(--fc-secondary)'
                                  : 'var(--fc-outline-variant)',
                            }}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-1.5 font-semibold">
                        Output Size: {photoConfig.scalePercent}%
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {[100, 75, 50, 25].map((pct) => (
                          <button
                            key={pct}
                            type="button"
                            onClick={() => setPhotoConfig((c) => ({ ...c, scalePercent: pct }))}
                            className="rounded-xl border px-3 py-1.5 font-medium cursor-pointer"
                            style={{
                              backgroundColor:
                                photoConfig.scalePercent === pct
                                  ? 'var(--fc-secondary-container)'
                                  : 'var(--fc-surface-variant)',
                              borderColor:
                                photoConfig.scalePercent === pct
                                  ? 'var(--fc-secondary)'
                                  : 'var(--fc-outline-variant)',
                            }}
                          >
                            {pct === 100 ? 'Original (100%)' : `${pct}%`}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-1 font-semibold">
                        Encoding Quality: {photoConfig.qualityPercent}%
                      </div>
                      <input
                        type="range"
                        min={25}
                        max={100}
                        value={photoConfig.qualityPercent}
                        onChange={(e) =>
                          setPhotoConfig((c) => ({
                            ...c,
                            qualityPercent: parseInt(e.target.value, 10),
                          }))
                        }
                        className="w-full"
                      />
                    </div>
                  </div>
                )}

                <div className="flex gap-2.5 pt-2">
                  <button
                    type="button"
                    onClick={() => setExportState({ kind: 'IDLE' })}
                    className="flex-1 rounded-xl border py-2.5 text-sm font-semibold cursor-pointer"
                    style={{ borderColor: 'var(--fc-outline)' }}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={() => void runExport()}
                    className="flex-[1.4] rounded-xl py-2.5 text-sm font-semibold text-white cursor-pointer"
                    style={{ backgroundColor: 'var(--fc-primary)' }}
                  >
                    {inspection.kind === 'VIDEO' ? 'Start Re-Encode Export' : 'Start Crop Export'}
                  </button>
                </div>
              </div>
            )}

            {exportState.kind === 'EXPORTING' && (
              <div
                className="rounded-3xl border p-5 space-y-3"
                style={{
                  backgroundColor: 'var(--fc-surface)',
                  borderColor: 'var(--fc-outline)',
                }}
              >
                <div className="flex items-center justify-between text-sm font-semibold">
                  <span>{exportState.statusText}</span>
                  <span>{Math.round(exportState.progress * 100)}%</span>
                </div>
                <div
                  className="h-2 w-full overflow-hidden rounded-full"
                  style={{ backgroundColor: 'var(--fc-surface-variant)' }}
                >
                  <div
                    className="h-full transition-all"
                    style={{
                      width: `${Math.round(exportState.progress * 100)}%`,
                      backgroundColor: 'var(--fc-primary)',
                    }}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => abortControllerRef.current?.abort()}
                  className="flex w-full items-center justify-center gap-2 rounded-xl border py-2.5 text-sm font-semibold cursor-pointer"
                  style={{ borderColor: 'var(--fc-outline)' }}
                >
                  <X className="h-4 w-4" />
                  <span>Cancel Export</span>
                </button>
              </div>
            )}

            {exportState.kind === 'COMPLETED' && (
              <div
                className="rounded-3xl p-5 space-y-3"
                style={{
                  backgroundColor: 'var(--fc-secondary-container)',
                  color: 'var(--fc-on-secondary-container)',
                }}
              >
                <div className="flex items-center gap-2 text-base font-semibold">
                  <CheckCircle2 className="h-5 w-5" />
                  <span>
                    {exportState.output.usedOriginalStreamCopy
                      ? 'Saved 1:1 Original Stream Copy'
                      : 'Saved New Cropped Copy'}
                  </span>
                </div>
                <div className="font-mono-tnum text-xs opacity-90">
                  {exportState.output.displayName} · {exportState.output.outputWidth}×
                  {exportState.output.outputHeight} · {formatFileSize(exportState.output.sizeBytes)}
                </div>
                <div className="flex gap-2.5">
                  <button
                    type="button"
                    onClick={() => setExportState({ kind: 'IDLE' })}
                    className="flex-1 rounded-xl border py-2.5 text-sm font-semibold cursor-pointer"
                    style={{ borderColor: 'currentColor' }}
                  >
                    Done
                  </button>
                  <button
                    type="button"
                    onClick={() => downloadOutput(exportState.output)}
                    className="flex-[1.3] flex items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-semibold cursor-pointer"
                    style={{
                      backgroundColor: 'var(--fc-secondary)',
                      color: 'var(--fc-on-secondary)',
                    }}
                  >
                    <Download className="h-4 w-4" />
                    <span>Download Cropped File</span>
                  </button>
                </div>
              </div>
            )}

            {exportState.kind === 'FAILED' && (
              <div
                className="flex items-center justify-between gap-3 rounded-2xl px-4 py-3 text-sm"
                style={{
                  backgroundColor: 'var(--fc-error-container)',
                  color: 'var(--fc-on-error-container)',
                }}
              >
                <span>{exportState.message}</span>
                <button
                  type="button"
                  onClick={() => setExportState({ kind: 'IDLE' })}
                  className="font-semibold cursor-pointer"
                >
                  Dismiss
                </button>
              </div>
            )}

            {(exportState.kind === 'IDLE' || exportState.kind === 'AWAITING_CONSENT') && (
              <button
                type="button"
                disabled={isBusy}
                onClick={handleRequestExport}
                className="flex h-14 w-full items-center justify-center gap-2 rounded-2xl text-base font-semibold text-white cursor-pointer"
                style={{ backgroundColor: 'var(--fc-primary)' }}
              >
                <Crop className="h-5 w-5" />
                <span>
                  {isFullFrame ? 'Export 1:1 Original Stream Copy' : 'Export Cropped Media'}
                </span>
              </button>
            )}
          </div>
        )}
      </main>

      {/* Sub-region Consent Dialog */}
      {exportState.kind === 'AWAITING_CONSENT' && inspection && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ backgroundColor: 'rgba(0,0,0,0.68)' }}
        >
          <div
            className="w-full max-w-md rounded-3xl border p-6 shadow-2xl space-y-4"
            style={{
              backgroundColor: 'var(--fc-surface)',
              borderColor: 'var(--fc-outline)',
            }}
          >
            <div className="flex items-center gap-2.5">
              <Sliders className="h-5 w-5" style={{ color: 'var(--fc-primary)' }} />
              <h2 className="font-display text-lg font-semibold">Re-Encoding Required for Crop</h2>
            </div>
            <p className="text-sm" style={{ color: 'var(--fc-on-surface-variant)' }}>
              Sub-region cropping changes pixel dimensions ({liveCropPixels.width}×
              {liveCropPixels.height}), so a fresh cropped copy will be generated.
            </p>
            <div
              className="rounded-xl p-3 font-mono-tnum text-xs space-y-1"
              style={{
                backgroundColor: 'var(--fc-surface-variant)',
                color: 'var(--fc-on-surface-variant)',
              }}
            >
              <div>• Source: {inspection.displayWidth}×{inspection.displayHeight} ({inspection.containerLabel})</div>
              <div>• Target Crop: {liveCropPixels.width}×{liveCropPixels.height} px</div>
              <div>• Original file is never modified or overwritten</div>
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setExportState({ kind: 'IDLE' })}
                className="rounded-xl px-4 py-2 text-xs font-semibold tracking-wider cursor-pointer"
                style={{ color: 'var(--fc-on-surface-variant)' }}
              >
                CANCEL
              </button>
              <button
                type="button"
                onClick={() => setExportState({ kind: 'CONFIGURING' })}
                className="rounded-xl px-4 py-2 text-xs font-semibold tracking-wider text-white cursor-pointer"
                style={{ backgroundColor: 'var(--fc-primary)' }}
              >
                CONTINUE
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
