import React, { useMemo, useRef, useState } from 'react';
import { Check, Move, RotateCcw, X } from 'lucide-react';
import {
  NormalizedRegion,
  normalizeRegion,
  RegionAnimationOption,
  toPixelRegion,
} from '../types/framecut';
import { RegionBorderCanvas } from './RegionBorderCanvas';

interface RegionSelectorOverlayProps {
  initialRegion: NormalizedRegion;
  screenWidthPx: number;
  screenHeightPx: number;
  regionAnimation: RegionAnimationOption;
  onConfirm: (region: NormalizedRegion) => void;
  onCancel: () => void;
}

type AspectPreset = { id: string; label: string; ratio: number | null };

const ASPECT_PRESETS: AspectPreset[] = [
  { id: 'free', label: 'Free', ratio: null },
  { id: '16_9', label: '16:9', ratio: 16 / 9 },
  { id: '9_16', label: '9:16', ratio: 9 / 16 },
  { id: '4_3', label: '4:3', ratio: 4 / 3 },
  { id: '1_1', label: '1:1', ratio: 1 },
];

type DragKind =
  | 'move'
  | 'nw'
  | 'ne'
  | 'sw'
  | 'se'
  | 'n'
  | 's'
  | 'w'
  | 'e';

export const RegionSelectorOverlay: React.FC<RegionSelectorOverlayProps> = ({
  initialRegion,
  screenWidthPx,
  screenHeightPx,
  regionAnimation,
  onConfirm,
  onCancel,
}) => {
  const [draft, setDraft] = useState<NormalizedRegion>(() => normalizeRegion(initialRegion));
  const [selectedPreset, setSelectedPreset] = useState<string>('free');
  const containerRef = useRef<HTMLDivElement | null>(null);

  const pixelRegion = useMemo(
    () => toPixelRegion(draft, screenWidthPx, screenHeightPx),
    [draft, screenWidthPx, screenHeightPx],
  );

  const hudAtTop = draft.bottom > 0.72 && draft.top > 0.25;
  const showRedBorder = regionAnimation === 'SHOW';

  const applyAspectPreset = (preset: AspectPreset) => {
    setSelectedPreset(preset.id);
    if (!preset.ratio) return;
    const rect = containerRef.current?.getBoundingClientRect();
    const cw = rect?.width || screenWidthPx;
    const ch = rect?.height || screenHeightPx;
    const centerX = (draft.left + draft.right) / 2;
    const centerY = (draft.top + draft.bottom) / 2;
    let targetW = (draft.right - draft.left) * cw;
    let targetH = targetW / preset.ratio;
    if (targetH > ch * 0.78) {
      targetH = ch * 0.78;
      targetW = targetH * preset.ratio;
    }
    if (targetW > cw * 0.9) {
      targetW = cw * 0.9;
      targetH = targetW / preset.ratio;
    }
    const halfW = targetW / cw / 2;
    const halfH = targetH / ch / 2;
    const cx = Math.max(halfW, Math.min(1 - halfW, centerX));
    const cy = Math.max(halfH, Math.min(1 - halfH, centerY));
    setDraft(
      normalizeRegion({
        left: cx - halfW,
        top: cy - halfH,
        right: cx + halfW,
        bottom: cy + halfH,
      }),
    );
  };

  const startDrag = (kind: DragKind, e: React.PointerEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const box = containerRef.current?.getBoundingClientRect();
    if (!box || box.width <= 0 || box.height <= 0) return;

    const startX = e.clientX;
    const startY = e.clientY;
    const startRegion = { ...draft };
    const minSpan = 0.12;

    if (kind !== 'move') {
      setSelectedPreset('free');
    }

    const onMove = (ev: PointerEvent) => {
      const dx = (ev.clientX - startX) / box.width;
      const dy = (ev.clientY - startY) / box.height;

      if (kind === 'move') {
        const wFrac = startRegion.right - startRegion.left;
        const hFrac = startRegion.bottom - startRegion.top;
        const nextLeft = Math.max(0, Math.min(1 - wFrac, startRegion.left + dx));
        const nextTop = Math.max(0, Math.min(1 - hFrac, startRegion.top + dy));
        setDraft({
          left: nextLeft,
          top: nextTop,
          right: nextLeft + wFrac,
          bottom: nextTop + hFrac,
        });
        return;
      }

      let { left, top, right, bottom } = startRegion;
      if (kind.includes('w')) left = Math.max(0, Math.min(right - minSpan, startRegion.left + dx));
      if (kind.includes('e')) right = Math.min(1, Math.max(left + minSpan, startRegion.right + dx));
      if (kind.includes('n')) top = Math.max(0, Math.min(bottom - minSpan, startRegion.top + dy));
      if (kind.includes('s')) bottom = Math.min(1, Math.max(top + minSpan, startRegion.bottom + dy));
      setDraft(normalizeRegion({ left, top, right, bottom }));
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handles: { kind: DragKind; x: number; y: number; cursor: string; label: string }[] = [
    { kind: 'nw', x: draft.left, y: draft.top, cursor: 'nwse-resize', label: 'Top left corner' },
    { kind: 'ne', x: draft.right, y: draft.top, cursor: 'nesw-resize', label: 'Top right corner' },
    { kind: 'sw', x: draft.left, y: draft.bottom, cursor: 'nesw-resize', label: 'Bottom left corner' },
    { kind: 'se', x: draft.right, y: draft.bottom, cursor: 'nwse-resize', label: 'Bottom right corner' },
    { kind: 'n', x: (draft.left + draft.right) / 2, y: draft.top, cursor: 'ns-resize', label: 'Top edge' },
    { kind: 's', x: (draft.left + draft.right) / 2, y: draft.bottom, cursor: 'ns-resize', label: 'Bottom edge' },
    { kind: 'w', x: draft.left, y: (draft.top + draft.bottom) / 2, cursor: 'ew-resize', label: 'Left edge' },
    { kind: 'e', x: draft.right, y: (draft.top + draft.bottom) / 2, cursor: 'ew-resize', label: 'Right edge' },
  ];

  const aspectRatio = screenWidthPx / Math.max(1, screenHeightPx);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(10, 12, 16, 0.94)' }}
    >
      {/* Interactive Screen Canvas */}
      <div
        ref={containerRef}
        className="relative overflow-hidden rounded-xl border"
        style={{
          aspectRatio: `${aspectRatio}`,
          height: aspectRatio < 1 ? 'min(82vh, 720px)' : 'auto',
          width: aspectRatio >= 1 ? 'min(92vw, 960px)' : 'auto',
          maxWidth: '94vw',
          maxHeight: '85vh',
          borderColor: 'rgba(255,255,255,0.2)',
          background:
            'radial-gradient(circle at 30% 25%, rgba(37,99,235,0.18), transparent 55%), radial-gradient(circle at 75% 70%, rgba(220,38,38,0.16), transparent 55%), #12151E',
        }}
      >
        <RegionBorderCanvas
          region={draft}
          animate={showRedBorder}
          showBorder={showRedBorder}
          strokeWidth={3}
          scrimColor="rgba(0,0,0,0.55)"
          neutralGuideColor="rgba(255,255,255,0.45)"
        />

        {/* Draggable Interior Box */}
        <div
          onPointerDown={(e) => startDrag('move', e)}
          style={{
            left: `${draft.left * 100}%`,
            top: `${draft.top * 100}%`,
            width: `${(draft.right - draft.left) * 100}%`,
            height: `${(draft.bottom - draft.top) * 100}%`,
          }}
          className="absolute flex cursor-move items-center justify-center select-none touch-none"
        >
          <div
            className="flex h-10 w-10 items-center justify-center rounded-full shadow-lg"
            style={{ backgroundColor: 'rgba(13, 15, 20, 0.72)', color: '#FFFFFF' }}
          >
            <Move className="h-4 w-4" />
          </div>
        </div>

        {/* 8 Resize Handles */}
        {handles.map((h) => (
          <button
            key={h.kind}
            type="button"
            aria-label={h.label}
            onPointerDown={(e) => startDrag(h.kind, e)}
            style={{
              left: `${h.x * 100}%`,
              top: `${h.y * 100}%`,
              cursor: h.cursor,
              transform: 'translate(-50%, -50%)',
            }}
            className="absolute flex h-11 w-11 items-center justify-center touch-none focus:outline-none"
          >
            <span
              className="block h-4 w-4 rounded-full border-2 border-white shadow"
              style={{ backgroundColor: 'var(--fc-record-accent)' }}
            />
          </button>
        ))}
      </div>

      {/* Floating Control HUD */}
      <div
        className={`fixed left-1/2 z-50 w-[min(92vw,440px)] -translate-x-1/2 rounded-2xl border p-4 shadow-2xl backdrop-blur-md ${
          hudAtTop ? 'top-4' : 'bottom-4'
        }`}
        style={{
          backgroundColor: 'rgba(21, 24, 33, 0.94)',
          borderColor: 'var(--fc-outline)',
          color: '#F3F4F6',
        }}
      >
        <div className="mb-3 flex items-center justify-between">
          <div>
            <div className="font-display text-base font-semibold">
              {pixelRegion.width} × {pixelRegion.height} px
            </div>
            <div className="font-mono-tnum text-xs text-gray-400">
              Origin ({pixelRegion.left}, {pixelRegion.top})
            </div>
          </div>
          <div className="flex items-center gap-1">
            <button
              type="button"
              title="Reset"
              onClick={() => {
                setSelectedPreset('free');
                setDraft({ left: 0.1, top: 0.2, right: 0.9, bottom: 0.7 });
              }}
              className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              <RotateCcw className="h-4 w-4" />
            </button>
            <button
              type="button"
              title="Cancel"
              onClick={onCancel}
              className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="mb-3 flex flex-wrap gap-1.5">
          {ASPECT_PRESETS.map((preset) => {
            const active = selectedPreset === preset.id;
            return (
              <button
                key={preset.id}
                type="button"
                onClick={() => applyAspectPreset(preset)}
                className="rounded-xl border px-3 py-1.5 text-xs font-medium font-mono-tnum transition-colors cursor-pointer"
                style={{
                  backgroundColor: active ? '#3F1216' : '#1E2330',
                  borderColor: active ? '#DC2626' : '#2D3446',
                  color: active ? '#FFDAD6' : '#E5E7EB',
                }}
              >
                {preset.label}
              </button>
            );
          })}
        </div>

        <button
          type="button"
          onClick={() => onConfirm(normalizeRegion(draft))}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-xl text-sm font-semibold text-white transition-opacity hover:opacity-95 cursor-pointer"
          style={{ backgroundColor: '#DC2626' }}
        >
          <Check className="h-4 w-4" />
          <span>Confirm Region</span>
        </button>
      </div>
    </div>
  );
};
