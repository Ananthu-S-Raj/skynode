import { useEffect, useRef } from 'react';
import Icon from './Icon';

export default function ConfirmationDialog({
  title,
  children,
  confirmLabel = 'Confirm',
  busy,
  onConfirm,
  onCancel,
}) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => {
      if (dialog.open) dialog.close();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className="confirmation-dialog"
      aria-labelledby="confirmation-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onCancel();
      }}
    >
      <div className="dialog-symbol">
        <Icon name="info" size={26} />
      </div>
      <h2 id="confirmation-title">{title}</h2>
      <div className="dialog-body">{children}</div>
      <div className="dialog-actions">
        <button
          type="button"
          className="button button-secondary"
          autoFocus
          disabled={busy}
          onClick={onCancel}
        >
          Keep it
        </button>
        <button
          type="button"
          className="button button-danger"
          disabled={busy}
          onClick={onConfirm}
        >
          {busy ? 'Please wait…' : confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
