import { type ReactNode } from 'react';

interface ModalProps {
  title: string;
  children: ReactNode;
  onClose: () => void;
  large?: boolean;
}

export default function Modal({ title, children, onClose, large }: ModalProps) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={`modal ${large ? 'large' : ''}`} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h3 style={{ margin: 0 }}>{title}</h3>
          <button className="btn btn-sm btn-outline" onClick={onClose}>✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
