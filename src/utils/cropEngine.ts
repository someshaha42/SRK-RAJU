import {
  CropExportOutput,
  MediaInspectionResult,
  NormalizedCropRect,
  PhotoCropFallbackConfig,
  VideoCropFallbackConfig,
} from '../types/framecut';

export function computeCropPixels(
  rect: NormalizedCropRect,
  displayWidth: number,
  displayHeight: number,
  evenAlign: boolean,
): { left: number; top: number; width: number; height: number } {
  const align = (v: number) => (evenAlign ? Math.max(2, Math.round(v / 2) * 2) : Math.max(1, Math.round(v)));
  const left = Math.max(0, Math.min(displayWidth - 2, Math.round(rect.left * displayWidth)));
  const top = Math.max(0, Math.min(displayHeight - 2, Math.round(rect.top * displayHeight)));
  const width = Math.min(displayWidth - left, align((rect.right - rect.left) * displayWidth));
  const height = Math.min(displayHeight - top, align((rect.bottom - rect.top) * displayHeight));
  return { left, top, width, height };
}

export async function inspectMediaFile(file: File): Promise<MediaInspectionResult> {
  const url = URL.createObjectURL(file);
  const isVideo = file.type.startsWith('video/') || /\.(mp4|mov|mkv|webm|avi|3gp)$/i.test(file.name);
  const ext = (file.name.split('.').pop() || '').toUpperCase() || (isVideo ? 'MP4' : 'PNG');

  if (isVideo) {
    return new Promise((resolve, reject) => {
      const video = document.createElement('video');
      video.preload = 'metadata';
      video.src = url;
      video.onloadedmetadata = () => {
        const w = Math.max(2, video.videoWidth || 1280);
        const h = Math.max(2, video.videoHeight || 720);
        const durationMs = Math.round((video.duration || 0) * 1000);
        const bitrateBps =
          durationMs > 0 ? Math.round((file.size * 8 * 1000) / durationMs) : 12_000_000;
        resolve({
          id: `media_${Date.now()}`,
          url,
          file,
          kind: 'VIDEO',
          displayName: file.name,
          mimeType: file.type || 'video/mp4',
          containerLabel: ext,
          codecLabel: ext === 'WEBM' ? 'VP9' : 'H.264/HEVC',
          audioCodecLabel: 'AAC/Opus',
          rawWidth: w,
          rawHeight: h,
          rotationDegrees: 0,
          displayWidth: w,
          displayHeight: h,
          durationMs,
          frameRate: 30,
          bitrateBps,
          fileSizeBytes: file.size,
        });
      };
      video.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error(`Unable to inspect video file "${file.name}".`));
      };
    });
  }

  return new Promise((resolve, reject) => {
    const img = new Image();
    img.src = url;
    img.onload = () => {
      resolve({
        id: `media_${Date.now()}`,
        url,
        file,
        kind: 'PHOTO',
        displayName: file.name,
        mimeType: file.type || 'image/png',
        containerLabel: ext === 'JPG' ? 'JPEG' : ext,
        codecLabel: ext === 'JPG' ? 'JPEG' : ext,
        audioCodecLabel: null,
        rawWidth: img.naturalWidth || 1080,
        rawHeight: img.naturalHeight || 1080,
        rotationDegrees: 0,
        displayWidth: img.naturalWidth || 1080,
        displayHeight: img.naturalHeight || 1080,
        durationMs: 0,
        frameRate: null,
        bitrateBps: null,
        fileSizeBytes: file.size,
      });
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error(`Unable to decode image file "${file.name}".`));
    };
  });
}

export async function createSamplePhotoFile(): Promise<File> {
  const canvas = document.createElement('canvas');
  canvas.width = 1600;
  canvas.height = 1000;
  const ctx = canvas.getContext('2d')!;

  const grad = ctx.createLinearGradient(0, 0, 1600, 1000);
  grad.addColorStop(0, '#0D0F14');
  grad.addColorStop(0.55, '#1E2330');
  grad.addColorStop(1, '#3F1216');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, 1600, 1000);

  ctx.strokeStyle = 'rgba(220, 38, 38, 0.45)';
  ctx.lineWidth = 4;
  ctx.strokeRect(80, 80, 1440, 840);

  ctx.fillStyle = '#DC2626';
  ctx.beginPath();
  ctx.arc(800, 440, 120, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = '#F3F4F6';
  ctx.font = '700 64px "Space Grotesk", sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('FrameCut Sample Frame', 800, 640);
  ctx.fillStyle = '#9CA3AF';
  ctx.font = '500 28px "JetBrains Mono", monospace';
  ctx.fillText('1600 × 1000 · BT.709 Studio Reference', 800, 700);

  const blob = await new Promise<Blob>((res) => canvas.toBlob((b) => res(b!), 'image/png'));
  return new File([blob], 'framecut_sample_photo.png', { type: 'image/png' });
}

export async function exportCroppedPhoto(
  inspection: MediaInspectionResult,
  cropRect: NormalizedCropRect,
  config: PhotoCropFallbackConfig,
  fullFrameCopy: boolean,
): Promise<CropExportOutput> {
  if (fullFrameCopy && inspection.file) {
    const url = URL.createObjectURL(inspection.file);
    return {
      url,
      displayName: inspection.displayName,
      mimeType: inspection.mimeType,
      outputWidth: inspection.displayWidth,
      outputHeight: inspection.displayHeight,
      sizeBytes: inspection.fileSizeBytes,
      usedOriginalStreamCopy: true,
    };
  }

  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const el = new Image();
    el.src = inspection.url;
    el.onload = () => resolve(el);
    el.onerror = () => reject(new Error('Failed to load image for cropping.'));
  });

  const px = computeCropPixels(cropRect, inspection.displayWidth, inspection.displayHeight, false);
  const scale = Math.max(0.25, Math.min(1, config.scalePercent / 100));
  const outW = Math.max(1, Math.round(px.width * scale));
  const outH = Math.max(1, Math.round(px.height * scale));

  const canvas = document.createElement('canvas');
  canvas.width = outW;
  canvas.height = outH;
  const ctx = canvas.getContext('2d')!;
  ctx.drawImage(img, px.left, px.top, px.width, px.height, 0, 0, outW, outH);

  let targetMime = inspection.mimeType || 'image/png';
  let ext = 'png';
  if (config.outputFormat === 'JPEG') {
    targetMime = 'image/jpeg';
    ext = 'jpg';
  } else if (config.outputFormat === 'PNG') {
    targetMime = 'image/png';
    ext = 'png';
  } else if (config.outputFormat === 'WEBP') {
    targetMime = 'image/webp';
    ext = 'webp';
  } else if (targetMime.includes('jpeg') || targetMime.includes('jpg')) {
    ext = 'jpg';
  } else if (targetMime.includes('webp')) {
    ext = 'webp';
  }

  const quality = Math.max(0.25, Math.min(1, config.qualityPercent / 100));
  const blob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('Image encoding failed.'))),
      targetMime,
      quality,
    );
  });

  const baseName = inspection.displayName.replace(/\.[^.]+$/, '');
  const displayName = `${baseName}_cropped.${ext}`;
  const url = URL.createObjectURL(blob);
  return {
    url,
    displayName,
    mimeType: blob.type || targetMime,
    outputWidth: outW,
    outputHeight: outH,
    sizeBytes: blob.size,
    usedOriginalStreamCopy: false,
  };
}

export async function exportCroppedVideo(
  inspection: MediaInspectionResult,
  cropRect: NormalizedCropRect,
  config: VideoCropFallbackConfig,
  fullFrameCopy: boolean,
  onProgress: (fraction: number, statusText: string) => void,
  signal: AbortSignal,
): Promise<CropExportOutput> {
  if (fullFrameCopy && inspection.file) {
    onProgress(1, 'Copying original stream…');
    const url = URL.createObjectURL(inspection.file);
    return {
      url,
      displayName: inspection.displayName,
      mimeType: inspection.mimeType,
      outputWidth: inspection.displayWidth,
      outputHeight: inspection.displayHeight,
      sizeBytes: inspection.fileSizeBytes,
      usedOriginalStreamCopy: true,
    };
  }

  const px = computeCropPixels(cropRect, inspection.displayWidth, inspection.displayHeight, true);
  let outW = px.width;
  let outH = px.height;
  const maxLongEdgeMap: Record<string, number> = {
    ORIGINAL: Number.MAX_SAFE_INTEGER,
    UHD_4K: 3840,
    QHD_2K: 2560,
    FHD_1080P: 1920,
    HD_720P: 1280,
  };
  const cap = maxLongEdgeMap[config.resolutionPreset] ?? Number.MAX_SAFE_INTEGER;
  const longEdge = Math.max(outW, outH);
  if (longEdge > cap) {
    const ratio = cap / longEdge;
    outW = Math.max(2, Math.round((outW * ratio) / 2) * 2);
    outH = Math.max(2, Math.round((outH * ratio) / 2) * 2);
  }

  const fps =
    config.fpsPreset === 'FPS_60'
      ? 60
      : config.fpsPreset === 'FPS_30'
        ? 30
        : config.fpsPreset === 'FPS_24'
          ? 24
          : inspection.frameRate ?? 30;

  const video = document.createElement('video');
  video.src = inspection.url;
  video.muted = true;
  video.playsInline = true;
  await new Promise<void>((res, rej) => {
    video.onloadeddata = () => res();
    video.onerror = () => rej(new Error('Failed to load video stream for cropping.'));
  });

  const canvas = document.createElement('canvas');
  canvas.width = outW;
  canvas.height = outH;
  const ctx = canvas.getContext('2d')!;
  const stream = canvas.captureStream(fps);
  const mime = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
    ? 'video/webm;codecs=vp9'
    : 'video/webm';
  const recorder = new MediaRecorder(stream, {
    mimeType: mime,
    videoBitsPerSecond: Math.min(config.bitrateMbps * 1_000_000, 20_000_000),
  });
  const chunks: BlobPart[] = [];
  recorder.ondataavailable = (e) => {
    if (e.data.size > 0) chunks.push(e.data);
  };

  const durationSec = Math.max(0.5, video.duration || (inspection.durationMs / 1000) || 2);
  onProgress(0.05, 'Re-encoding cropped video frames…');
  recorder.start(150);
  video.currentTime = 0;
  await video.play();

  return new Promise((resolve, reject) => {
    let finished = false;
    const cleanup = () => {
      stream.getTracks().forEach((t) => t.stop());
      video.pause();
      video.src = '';
    };

    const onAbort = () => {
      if (finished) return;
      finished = true;
      if (recorder.state !== 'inactive') recorder.stop();
      cleanup();
      reject(new Error('Export cancelled by user.'));
    };
    signal.addEventListener('abort', onAbort, { once: true });

    recorder.onstop = () => {
      if (finished) return;
      finished = true;
      signal.removeEventListener('abort', onAbort);
      cleanup();
      const blob = new Blob(chunks, { type: mime });
      const baseName = inspection.displayName.replace(/\.[^.]+$/, '');
      const ext = mime.includes('mp4') ? 'mp4' : 'webm';
      resolve({
        url: URL.createObjectURL(blob),
        displayName: `${baseName}_cropped.${ext}`,
        mimeType: blob.type || mime,
        outputWidth: outW,
        outputHeight: outH,
        sizeBytes: blob.size,
        usedOriginalStreamCopy: false,
      });
    };

    const tick = () => {
      if (finished || signal.aborted) return;
      ctx.drawImage(video, px.left, px.top, px.width, px.height, 0, 0, outW, outH);
      const frac = Math.min(0.98, Math.max(0.05, video.currentTime / durationSec));
      onProgress(frac, `Cropping & encoding… ${Math.round(frac * 100)}%`);
      if (video.ended || video.currentTime >= durationSec - 0.03) {
        onProgress(1, 'Finalizing cropped media…');
        if (recorder.state !== 'inactive') recorder.stop();
        return;
      }
      requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  });
}
