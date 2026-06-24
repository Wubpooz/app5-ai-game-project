import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'secondary',
  className = '',
  ...props
}) => {
  const variantClass = {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    ghost: 'btn-ghost',
    danger: 'btn-danger',
  }[variant];

  return (
    <button className={`btn ${variantClass} ${className}`} {...props}>
      {children}
    </button>
  );
};
export default Button;
