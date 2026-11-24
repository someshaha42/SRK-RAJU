import React, { useState } from 'react';
import { ArrowLeft, Download, Film, Play, Share2, Trash2, Video, X } from 'lucide-react';
import {
  formatDurationShort,
  formatFileSize,
  formatRecordingDate,
  RecordedVideoItem,
} from '../types/framecut';

interface RecordingsScreenProps {
  recordings: RecordedVideoItem[];
  isLoading: boolean;
  onBack: () => void;
  onDelete: (item: RecordedVideoItem) => void;
}

export const RecordingsScreen: React.FC<RecordingsScreenProps> = ({
  recordings,
  isLoading,
  onBack,
  onDelete,
}) => {
  const [playingItem, setPlayingItem] = useState<RecordedVideoItem | null>(null);
  const [pendingDelete, setPendingDelete] = useState<RecordedVideoItem | null>(null);

  const handleShareOrDownload = async (item: RecordedVideoItem) => {
    if (navigator.share && item.blob) {
      try {
        const file = new File([item.blob], item.displayName, { type: item.mimeType });
        if (!navigator.canShare || navigator.canShare({ files: [file] })) {
          await navigator.share({
            title: item.displayName,
            files: [file],
          });
          return;
        }
      } catch {
        // Fallback to direct download below
      }
    }
    const a = document.createElement('a');
    a.href = item.url;
    a.download = item.displayName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--fc-bg)' }}>
      <header
        className="sticky top-0 z-30 border-b backdrop-blur-md"
        style={{
          backgroundColor: 'color-mix(in srgb, var(--fc-bg) 90%, transparent)',
          borderColor: 'var(--fc-outline-variant)',
        }}
      >
        <div className="mx-auto flex h-16 max-w-[1240px] items-center gap-3 px-4 sm:px-6">
          <button
            type="button"
            onClick={onBack}
            title="Navigate back"
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="font-display text-lg font-semibold">Recorded Videos</h1>
        </div>
      </header>

      <main className="mx-auto max-w-[1240px] px-4 py-5 sm:px-7">
        {isLoading && recordings.length === 0 ? (
          <div className="flex min-h-[55vh] items-center justify-center">
            <div
              className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
              style={{ borderColor: 'var(--fc-secondary)', borderTopColor: 'transparent' }}
            />
          </div>
        ) : recordings.length === 0 ? (
          <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
            <div
              className="mb-4 flex h-[72px] w-[72px] items-center justify-center rounded-2xl"
              style={{
                backgroundColor: 'var(--fc-surface-variant)',
                color: 'var(--fc-on-surface-variant)',
              }}
            >
              <Film className="h-8 w-8" />
            </div>
            <h2 className="font-display text-base font-semibold">No recordings yet</h2>
            <p
              className="mt-1.5 max-w-sm text-sm"
              style={{ color: 'var(--fc-on-surface-variant)' }}
            >
              Captured videos will appear here ready to play, download, or share.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
            {recordings.map((item) => {
              const details = [
                item.width > 0 && item.height > 0 ? `${item.width}×${item.height}` : null,
                item.durationMs > 0 ? formatDurationShort(item.durationMs) : null,
                formatFileSize(item.sizeBytes),
                item.dateAddedSec > 0 ? formatRecordingDate(item.dateAddedSec) : null,
              ]
                .filter(Boolean)
                .join(' • ');

              return (
                <div
                  key={item.id}
                  onClick={() => setPlayingItem(item)}
                  className="flex cursor-pointer items-center gap-3 rounded-2xl border p-3.5 transition-colors hover:border-[var(--fc-outline)]"
                  style={{
                    backgroundColor: 'var(--fc-surface)',
                    borderColor: 'var(--fc-outline-variant)',
                  }}
                >
                  <div
                    className="flex h-[52px] w-[52px] shrink-0 items-center justify-center rounded-xl"
                    style={{
                      backgroundColor: 'var(--fc-primary-container)',
                      color: 'var(--fc-primary)',
                    }}
                  >
                    <Video className="h-5 w-5" />
                  </div>

                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="truncate text-sm font-semibold">{item.displayName}</div>
                    <div
                      className="truncate font-mono-tnum text-[11px]"
                      style={{ color: 'var(--fc-on-surface-variant)' }}
                    >
                      {details}
                    </div>
                  </div>

                  <div
                    className="flex items-center gap-0.5"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      type="button"
                      title="Play"
                      onClick={() => setPlayingItem(item)}
                      className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
                    >
                      <Play className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      title="Download / Share"
                      onClick={() => handleShareOrDownload(item)}
                      className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
                    >
                      <Share2 className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      title="Delete"
                      onClick={() => setPendingDelete(item)}
                      className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Video Player Modal */}
      {playingItem && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          style={{ backgroundColor: 'rgba(0, 0, 0, 0.78)' }}
          onClick={() => setPlayingItem(null)}
        >
          <div
            className="flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border shadow-2xl"
            style={{
              backgroundColor: 'var(--fc-surface)',
              borderColor: 'var(--fc-outline)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              className="flex items-center justify-between border-b px-5 py-3.5"
              style={{ borderColor: 'var(--fc-outline-variant)' }}
            >
              <div className="min-w-0 flex-1 pr-3">
                <div className="truncate text-sm font-semibold">{playingItem.displayName}</div>
              </div>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  title="Download video"
                  onClick={() => handleShareOrDownload(playingItem)}
                  className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
                >
                  <Download className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  title="Close"
                  onClick={() => setPlayingItem(null)}
                  className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-white/10 cursor-pointer"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
            </div>
            <div className="flex flex-1 items-center justify-center bg-black p-3">
              <video
                src={playingItem.url}
                controls
                autoPlay
                className="max-h-[72vh] w-full rounded-xl"
              />
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {pendingDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ backgroundColor: 'rgba(0,0,0,0.65)' }}
        >
          <div
            className="w-full max-w-sm rounded-3xl border p-6 shadow-2xl"
            style={{
              backgroundColor: 'var(--fc-surface)',
              borderColor: 'var(--fc-outline)',
            }}
          >
            <h2 className="font-display text-lg font-semibold">Delete recording?</h2>
            <p className="mt-2 text-sm" style={{ color: 'var(--fc-on-surface-variant)' }}>
              Remove {pendingDelete.displayName} permanently from browser storage?
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setPendingDelete(null)}
                className="rounded-xl px-4 py-2 text-sm font-semibold hover:opacity-80 cursor-pointer"
                style={{ color: 'var(--fc-on-surface-variant)' }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onDelete(pendingDelete);
                  if (playingItem?.id === pendingDelete.id) setPlayingItem(null);
                  setPendingDelete(null);
                }}
                className="rounded-xl px-4 py-2 text-sm font-semibold text-white hover:opacity-90 cursor-pointer"
                style={{ backgroundColor: 'var(--fc-record-accent)' }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
