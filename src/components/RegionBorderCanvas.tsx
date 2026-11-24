import React, { useEffect, useRef } from 'react';
import { NormalizedRegion } from '../types/framecut';

interface RegionBorderCanvasProps {
  region: NormalizedRegion;
  animate: boolean;
  showBorder: boolean;
  strokeWidth?: number;
  scrimColor?: string;
  neutralGuideColor?: string;
  className?: string;
}

export const RegionBorderCanvas: React.FC<RegionBorderCanvasProps> = ({
  region,
  animate,
  showBorder,
  strokeWidth = 2.5,
  scrimColor = 'rgba(0,0,0,0.45)',
  neutralGuideColor = 'rgba(255,255,255,0.45)',
  className = '',
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let rafId = 0;
    const startTime = performance.now();

    const render = (now: number) => {
      const dpr = window.devicePixelRatio || 1;
      const rect = canvas.getBoundingClientRect();
      const targetW = Math.max(1, Math.round(rect.width * dpr));
      const targetH = Math.max(1, Math.round(rect.height * dpr));
      if (canvas.width !== targetW || canvas.height !== targetH) {
        canvas.width = targetW;
        canvas.height = targetH;
      }

      const w = rect.width;
      const h = rect.height;
      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, w, h);

      const l = Math.max(0, Math.min(w, region.left * w));
      const t = Math.max(0, Math.min(h, region.top * h));
      const r = Math.max(l, Math.min(w, region.right * w));
      const b = Math.max(t, Math.min(h, region.bottom * h));

      // 4 surrounding scrim rects matching RegionBorderCanvas.kt
      if (scrimColor && scrimColor !== 'transparent') {
        ctx.fillStyle = scrimColor;
        ctx.fillRect(0, 0, w, t);
        ctx.fillRect(0, b, w, Math.max(0, h - b));
        ctx.fillRect(0, t, l, Math.max(0, b - t));
        ctx.fillRect(r, t, Math.max(0, w - r), Math.max(0, b - t));
      }

      const rw = Math.max(1, r - l);
      const rh = Math.max(1, b - t);

      if (showBorder) {
        const dashLen = 10;
        const gapLen = 6;
        const period = dashLen + gapLen;
        const elapsed = animate ? ((now - startTime) % 900) / 900 : 0;
        const dashOffset = -(elapsed * period);

        ctx.setLineDash([]);
        ctx.lineWidth = strokeWidth + 2;
        ctx.strokeStyle = 'rgba(0,0,0,0.45)';
        ctx.strokeRect(l, t, rw, rh);

        ctx.setLineDash([dashLen, gapLen]);
        ctx.lineDashOffset = dashOffset;
        ctx.lineWidth = strokeWidth;
        ctx.strokeStyle = getComputedStyle(document.documentElement)
          .getPropertyValue('--fc-record-accent')
          .trim() || '#DC2626';
        ctx.strokeRect(l, t, rw, rh);
      } else if (neutralGuideColor && neutralGuideColor !== 'transparent') {
        ctx.setLineDash([]);
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = neutralGuideColor;
        ctx.strokeRect(l, t, rw, rh);
      }

      ctx.restore();

      if (showBorder && animate) {
        rafId = requestAnimationFrame(render);
      }
    };

    rafId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(rafId);
  }, [region, animate, showBorder, strokeWidth, scrimColor, neutralGuideColor]);

  return <canvas ref={canvasRef} className={`block w-full h-full pointer-events-none ${className}`} />;
};
