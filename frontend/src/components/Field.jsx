import { useId } from 'react';
import Icon from './Icon';

export default function Field({
  label,
  icon,
  options,
  hint,
  className = '',
  endControl,
  ...props
}) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {props.required && (
          <span className="required" aria-hidden="true">
            {' '}
            *
          </span>
        )}
      </label>
      <div className={`input-shell${icon ? ' with-icon' : ''}${endControl ? ' with-end-control' : ''}`}>
        {icon && <Icon name={icon} size={19} />}
        {options ? (
          <select id={id} {...props}>
            {options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        ) : (
          <input id={id} {...props} />
        )}
        {endControl}
      </div>
      {hint && <small>{hint}</small>}
    </div>
  );
}
