import React, { useEffect, useState } from 'react';

interface CustomNumericInputBoxProps {
  label: string;
  placeholder: string;
  suffixText: string;
  savedValueText: string;
  activeBadgeText: string | null;
  allowDecimal?: boolean;
  enabled: boolean;
  supportingHint?: string;
  parseAndSave: (raw: string) => boolean;
  onReset: () => void;
}

export const CustomNumericInputBox: React.FC<CustomNumericInputBoxProps> = ({
  label,
  placeholder,
  suffixText,
  savedValueText,
  activeBadgeText,
  allowDecimal = false,
  enabled,
  supportingHint,
  parseAndSave,
  onReset,
}) => {
  const [draft, setDraft] = useState(savedValueText);
  const [expanded, setExpanded] = useState(false);
  const [errorText, setErrorText] = useState<string | null>(null);

  useEffect(() => {
    setDraft(savedValueText);
  }, [savedValueText]);

  const handleChange = (val: string) => {
    const cleaned = allowDecimal
      ? val.replace(/[^0-9.]/g, '')
      : val.replace(/[^0-9]/g, '');
    setDraft(cleaned);
    setErrorText(null);
    setExpanded(true);
  };

  const handleSave = () => {
    const ok = parseAndSave(draft);
    if (!ok) {
      setErrorText(
        allowDecimal ? 'Enter a valid positive number (e.g. 0.5 or 2)' : 'Enter a valid positive integer',
      );
      return;
    }
    setErrorText(null);
    setExpanded(false);
  };

  const handleReset = () => {
    onReset();
    setDraft('');
    setErrorText(null);
    setExpanded(false);
  };

  return (
    <div className="mt-2 space-y-2">
      {activeBadgeText && (
        <div
          className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium"
          style={{
            backgroundColor: 'var(--fc-secondary-container)',
            color: 'var(--fc-on-secondary-container)',
          }}
        >
          <span>{activeBadgeText}</span>
        </div>
      )}

      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-medium" style={{ color: 'var(--fc-on-surface-variant)' }}>
          {label}
        </label>
        <div
          className="flex items-center rounded-xl border px-3 py-2 transition-colors"
          style={{
            backgroundColor: 'var(--fc-surface-variant)',
            borderColor: errorText
              ? 'var(--fc-error)'
              : expanded
                ? 'var(--fc-secondary)'
                : 'var(--fc-outline)',
          }}
        >
          <input
            type="text"
            inputMode={allowDecimal ? 'decimal' : 'numeric'}
            value={draft}
            disabled={!enabled}
            placeholder={placeholder}
            onFocus={() => setExpanded(true)}
            onChange={(e) => handleChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSave();
            }}
            className="w-full bg-transparent text-sm outline-none font-mono-tnum"
            style={{ color: 'var(--fc-on-surface)' }}
          />
          <span
            className="ml-2 shrink-0 text-xs font-medium font-mono-tnum"
            style={{ color: 'var(--fc-on-surface-variant)' }}
          >
            {suffixText}
          </span>
        </div>
        {supportingHint && !errorText && (
          <p className="text-[11px]" style={{ color: 'var(--fc-on-surface-variant)' }}>
            {supportingHint}
          </p>
        )}
        {errorText && (
          <p className="text-[11px]" style={{ color: 'var(--fc-error)' }}>
            {errorText}
          </p>
        )}
      </div>

      {(expanded || savedValueText.length > 0) && enabled && (
        <div className="flex items-center justify-end gap-2">
          {savedValueText.length > 0 && (
            <button
              type="button"
              onClick={handleReset}
              className="rounded-lg px-3 py-1.5 text-xs font-semibold transition-opacity hover:opacity-80 cursor-pointer"
              style={{
                backgroundColor: 'var(--fc-surface-variant)',
                color: 'var(--fc-on-surface)',
              }}
            >
              Reset
            </button>
          )}
          <button
            type="button"
            onClick={handleSave}
            className="rounded-lg px-3.5 py-1.5 text-xs font-semibold transition-opacity hover:opacity-90 cursor-pointer"
            style={{
              backgroundColor: 'var(--fc-secondary)',
              color: 'var(--fc-on-secondary)',
            }}
          >
            Save
          </button>
        </div>
      )}
    </div>
  );
};
