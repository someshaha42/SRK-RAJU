import {
  getEffectiveCanvasSize,
  RecordedVideoItem,
  RecordingSettings,
  toPixelRegion,
} from '../types/framecut';

export interface ActiveRecordingSession {
  pause: () => void;
  resume: () => void;
  stop: () => Promise<RecordedVideoItem>;
  resolutionLabel: string;
  audioLabel: string;
  usedFallbackCanvas: boolean;
  warningMessage: string | null;
}

function pickSupportedMimeType(): string {
  const candidates = [
    'video/webm;codecs=vp9,opus',
    'video/webm;codecs=vp8,opus',
    'video/webm;codecs=h264,opus',
    'video/webm',
    'video/mp4',
  ];
  for (const type of candidates) {
    if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(type)) {
      return type;
    }
  }
  return '';
}

function createStudioCanvasStream(
  width: number,
  height: number,
  fps: number,
  settings: RecordingSettings,
): { stream: MediaStream; cleanup: () => void } {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d')!;
  const start = performance.now();
  let stopped = false;

  const drawFrame = () => {
    if (stopped) return;
    const elapsedSec = (performance.now() - start) / 1000;

    // Deep studio background gradient
    const grad = ctx.createLinearGradient(0, 0, width, height);
    grad.addColorStop(0, '#0D0F14');
    grad.addColorStop(0.5, '#151821');
    grad.addColorStop(1, '#1A1F2E');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);

    // Subtle grid
    ctx.strokeStyle = 'rgba(45, 52, 70, 0.35)';
    ctx.lineWidth = 1;
    const step = 64;
    for (let x = 0; x < width; x += step) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += step) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // Animated telemetry rings
    const cx = width / 2;
    const cy = height / 2;
    const baseR = Math.min(width, height) * 0.22;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(elapsedSec * 0.6);
    ctx.strokeStyle = '#DC2626';
    ctx.lineWidth = 4;
    ctx.setLineDash([18, 12]);
    ctx.beginPath();
    ctx.arc(0, 0, baseR, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();

    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(-elapsedSec * 0.9);
    ctx.strokeStyle = '#60A5FA';
    ctx.lineWidth = 2.5;
    ctx.setLineDash([10, 14]);
    ctx.beginPath();
    ctx.arc(0, 0, baseR * 0.72, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();

    // Header badge
    ctx.fillStyle = '#F3F4F6';
    ctx.font = `700 ${Math.round(Math.min(width, height) * 0.042)}px "Space Grotesk", sans-serif`;
    ctx.textAlign = 'center';
    ctx.fillText('FRAMECUT STUDIO CAPTURE', cx, cy - 12);

    ctx.fillStyle = '#9CA3AF';
    ctx.font = `500 ${Math.round(Math.min(width, height) * 0.024)}px "JetBrains Mono", monospace`;
    ctx.fillText(
      `${width}×${height} • ${fps} FPS • ${settings.bitrateMode} • ${settings.colorDepth === 'BIT_10' ? '10-Bit' : '8-Bit'}`,
      cx,
      cy + 26,
    );

    ctx.fillStyle = '#10B981';
    ctx.fillText(`T+${elapsedSec.toFixed(2)}s`, cx, cy + 62);

    // Region border visualization if REGION mode and SHOW animation
    if (settings.recordingMode === 'REGION' && settings.regionAnimation === 'SHOW') {
      ctx.save();
      ctx.strokeStyle = '#DC2626';
      ctx.lineWidth = 4;
      ctx.setLineDash([16, 10]);
      ctx.lineDashOffset = -(elapsedSec * 40);
      ctx.strokeRect(8, 8, width - 16, height - 16);
      ctx.restore();
    }

    requestAnimationFrame(drawFrame);
  };

  requestAnimationFrame(drawFrame);
  const stream = canvas.captureStream(fps);
  return {
    stream,
    cleanup: () => {
      stopped = true;
      stream.getTracks().forEach((t) => t.stop());
    },
  };
}

export async function startScreenRecording(
  settings: RecordingSettings,
): Promise<ActiveRecordingSession> {
  const { width: canvasW, height: canvasH } = getEffectiveCanvasSize(settings.orientationMode);
  const pixelRegion = toPixelRegion(settings.selectedRegion, canvasW, canvasH);
  const targetW = settings.recordingMode === 'REGION' ? pixelRegion.width : canvasW;
  const targetH = settings.recordingMode === 'REGION' ? pixelRegion.height : canvasH;
  const fps = settings.customVideoFps ?? (settings.videoFps === 'FPS_60' ? 60 : 30);
  const bitrateBps =
    (settings.customVideoBitrateMbps ??
      parseInt(settings.videoBitrate.replace('MBPS_', ''), 10)) * 1_000_000;

  let displayStream: MediaStream | null = null;
  let micStream: MediaStream | null = null;
  let usedFallbackCanvas = false;
  let warningMessage: string | null = null;

  // Attempt native browser display capture first
  if (typeof navigator !== 'undefined' && navigator.mediaDevices?.getDisplayMedia) {
    try {
      displayStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          frameRate: { ideal: fps },
        },
        audio: settings.audioSource === 'INTERNAL',
      });
    } catch {
      usedFallbackCanvas = true;
      warningMessage =
        'Display capture permission was declined or unavailable in this browser frame — recorded with FrameCut Studio Canvas.';
    }
  } else {
    usedFallbackCanvas = true;
    warningMessage =
      'Browser display capture API is unavailable in this environment — recorded with FrameCut Studio Canvas.';
  }

  if (settings.audioSource === 'MICROPHONE' && navigator.mediaDevices?.getUserMedia) {
    try {
      micStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch {
      warningMessage =
        (warningMessage ? `${warningMessage} ` : '') +
        'Microphone access unavailable; recording video track only.';
    }
  }

  let outputStream: MediaStream;
  let cleanupExtra: () => void = () => {};

  if (usedFallbackCanvas || !displayStream) {
    const studio = createStudioCanvasStream(targetW, targetH, fps, settings);
    outputStream = studio.stream;
    cleanupExtra = studio.cleanup;
  } else if (settings.recordingMode === 'REGION') {
    // Crop the captured display stream to the selected normalized region via an offscreen canvas
    const videoEl = document.createElement('video');
    videoEl.srcObject = displayStream;
    videoEl.muted = true;
    await videoEl.play().catch(() => {});

    const cropCanvas = document.createElement('canvas');
    cropCanvas.width = targetW;
    cropCanvas.height = targetH;
    const cropCtx = cropCanvas.getContext('2d')!;
    let running = true;

    const pump = () => {
      if (!running) return;
      if (videoEl.videoWidth > 0 && videoEl.videoHeight > 0) {
        const sx = Math.round(settings.selectedRegion.left * videoEl.videoWidth);
        const sy = Math.round(settings.selectedRegion.top * videoEl.videoHeight);
        const sw = Math.max(2, Math.round((settings.selectedRegion.right - settings.selectedRegion.left) * videoEl.videoWidth));
        const sh = Math.max(2, Math.round((settings.selectedRegion.bottom - settings.selectedRegion.top) * videoEl.videoHeight));
        cropCtx.drawImage(videoEl, sx, sy, sw, sh, 0, 0, targetW, targetH);
      }
      requestAnimationFrame(pump);
    };
    requestAnimationFrame(pump);

    const canvasStream = cropCanvas.captureStream(fps);
    const tracks = [...canvasStream.getVideoTracks()];
    if (settings.audioSource === 'INTERNAL') {
      tracks.push(...displayStream.getAudioTracks());
    }
    outputStream = new MediaStream(tracks);
    cleanupExtra = () => {
      running = false;
      videoEl.srcObject = null;
      canvasStream.getTracks().forEach((t) => t.stop());
    };
  } else {
    outputStream = displayStream;
  }

  if (micStream) {
    micStream.getAudioTracks().forEach((t) => outputStream.addTrack(t));
  }

  const mimeType = pickSupportedMimeType();
  const chunks: BlobPart[] = [];
  const startedAt = Date.now();
  let pausedAt: number | null = null;
  let accumulatedPausedMs = 0;

  const recorder = new MediaRecorder(outputStream, {
    ...(mimeType ? { mimeType } : {}),
    videoBitsPerSecond: Math.min(bitrateBps, 25_000_000),
  });

  recorder.ondataavailable = (ev) => {
    if (ev.data && ev.data.size > 0) {
      chunks.push(ev.data);
    }
  };

  recorder.start(250);

  const audioLabel =
    settings.audioSource === 'INTERNAL' ? 'Internal Audio' : 'Microphone';
  const resolutionLabel =
    settings.recordingMode === 'REGION'
      ? `Region (${targetW}×${targetH})`
      : `Full Screen (${targetW}×${targetH})`;

  return {
    resolutionLabel,
    audioLabel,
    usedFallbackCanvas,
    warningMessage,
    pause: () => {
      if (recorder.state === 'recording') {
        recorder.pause();
        pausedAt = Date.now();
      }
    },
    resume: () => {
      if (recorder.state === 'paused') {
        if (pausedAt !== null) {
          accumulatedPausedMs += Date.now() - pausedAt;
          pausedAt = null;
        }
        recorder.resume();
      }
    },
    stop: () =>
      new Promise<RecordedVideoItem>((resolve) => {
        const finalize = () => {
          const finalPausedMs =
            pausedAt !== null ? accumulatedPausedMs + (Date.now() - pausedAt) : accumulatedPausedMs;
          const durationMs = Math.max(500, Date.now() - startedAt - finalPausedMs);
          const actualMime = recorder.mimeType || mimeType || 'video/webm';
          const ext = actualMime.includes('mp4') ? 'mp4' : 'webm';
          const stamp = new Date()
            .toISOString()
            .replace(/[:.]/g, '-')
            .slice(0, 19);
          const displayName = `FrameCut_${stamp}.${ext}`;
          const blob = new Blob(chunks, { type: actualMime });
          const url = URL.createObjectURL(blob);

          cleanupExtra();
          displayStream?.getTracks().forEach((t) => t.stop());
          micStream?.getTracks().forEach((t) => t.stop());

          resolve({
            id: `rec_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
            displayName,
            mimeType: actualMime,
            width: targetW,
            height: targetH,
            durationMs,
            sizeBytes: blob.size,
            dateAddedSec: Math.floor(Date.now() / 1000),
            url,
            blob,
            modeLabel: resolutionLabel,
          });
        };

        if (recorder.state === 'inactive') {
          finalize();
          return;
        }
        recorder.onstop = finalize;
        recorder.stop();
      }),
  };
}
