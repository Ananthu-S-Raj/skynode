import Icon from './Icon';

export default function Feedback({ children, kind = 'error', onRetry }) {
  if (!children) return null;
  return (
    <div
      className={`feedback feedback-${kind}`}
      role={kind === 'error' ? 'alert' : 'status'}
    >
      <Icon name={kind === 'success' ? 'check' : 'info'} />
      <div>
        {children}
        {onRetry && (
          <button className="text-button" onClick={onRetry} type="button">
            Try again <Icon name="refresh" size={15} />
          </button>
        )}
      </div>
    </div>
  );
}

export function Loading({ children = 'Loading…' }) {
  return (
    <div className="loading" role="status">
      <span className="spinner" />
      {children}
    </div>
  );
}

export function EmptyState({ icon = 'search', title, children, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} size={28} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}
