import {
  DEFAULT_RECORDING_SETTINGS,
  normalizeRegion,
  RecordedVideoItem,
  RecordingSettings,
} from '../types/framecut';

const SETTINGS_STORAGE_KEY = 'framecut_recording_settings_v2';
const DB_NAME = 'framecut_recordings_db';
const DB_VERSION = 1;
const STORE_NAME = 'recordings';

export function loadRecordingSettings(): RecordingSettings {
  try {
    const raw = window.localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (!raw) return DEFAULT_RECORDING_SETTINGS;
    const parsed = JSON.parse(raw) as Partial<RecordingSettings>;
    return {
      ...DEFAULT_RECORDING_SETTINGS,
      ...parsed,
      selectedRegion: normalizeRegion(parsed.selectedRegion ?? DEFAULT_RECORDING_SETTINGS.selectedRegion),
    };
  } catch {
    return DEFAULT_RECORDING_SETTINGS;
  }
}

export function saveRecordingSettings(settings: RecordingSettings): void {
  try {
    window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
  } catch {
    // Ignore quota errors in restricted environments
  }
}

interface StoredRecordingRecord {
  id: string;
  displayName: string;
  mimeType: string;
  width: number;
  height: number;
  durationMs: number;
  sizeBytes: number;
  dateAddedSec: number;
  blob: Blob;
  modeLabel?: string;
}

function openRecordingsDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB unavailable'));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error ?? new Error('Failed to open IndexedDB'));
  });
}

export async function loadSavedRecordings(): Promise<RecordedVideoItem[]> {
  try {
    const db = await openRecordingsDb();
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.getAll();
      req.onsuccess = () => {
        const rows = (req.result as StoredRecordingRecord[]) ?? [];
        rows.sort((a, b) => b.dateAddedSec - a.dateAddedSec);
        resolve(
          rows.map((r) => ({
            id: r.id,
            displayName: r.displayName,
            mimeType: r.mimeType,
            width: r.width,
            height: r.height,
            durationMs: r.durationMs,
            sizeBytes: r.sizeBytes,
            dateAddedSec: r.dateAddedSec,
            blob: r.blob,
            url: URL.createObjectURL(r.blob),
            modeLabel: r.modeLabel,
          })),
        );
      };
      req.onerror = () => reject(req.error);
    });
  } catch {
    return [];
  }
}

export async function saveRecordingToDb(item: RecordedVideoItem): Promise<void> {
  if (!item.blob) return;
  try {
    const db = await openRecordingsDb();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const record: StoredRecordingRecord = {
        id: item.id,
        displayName: item.displayName,
        mimeType: item.mimeType,
        width: item.width,
        height: item.height,
        durationMs: item.durationMs,
        sizeBytes: item.sizeBytes,
        dateAddedSec: item.dateAddedSec,
        blob: item.blob!,
        modeLabel: item.modeLabel,
      };
      const req = store.put(record);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
  } catch {
    // Non-fatal in-memory fallback already works for active session
  }
}

export async function deleteRecordingFromDb(id: string): Promise<void> {
  try {
    const db = await openRecordingsDb();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.delete(id);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
  } catch {
    // Ignore
  }
}
