import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('demo@test.com');
  const [password, setPassword] = useState('password');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      const msg = err?.response?.data?.error || err?.message || 'Login failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#0f172a',
        color: '#f9fafb'
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 420,
          background: '#020617',
          borderRadius: 16,
          padding: '32px 28px',
          boxShadow: '0 24px 80px rgba(15,23,42,0.8)',
          border: '1px solid rgba(148,163,184,0.35)'
        }}
      >
        <h1 style={{ fontSize: 28, marginBottom: 8 }}>Serverless Portal</h1>
        <p style={{ marginBottom: 24, color: '#9ca3af' }}>
          Sign in with your demo account to explore the AWS serverless portal.
        </p>

        {error && (
          <div
            style={{
              background: 'rgba(248,113,113,0.12)',
              border: '1px solid rgba(248,113,113,0.5)',
              color: '#fecaca',
              padding: '10px 12px',
              borderRadius: 8,
              marginBottom: 16,
              fontSize: 14
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 14 }}>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '10px 12px',
                borderRadius: 8,
                border: '1px solid rgba(148,163,184,0.5)',
                background: '#020617',
                color: '#f9fafb'
              }}
            />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 14 }}>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '10px 12px',
                borderRadius: 8,
                border: '1px solid rgba(148,163,184,0.5)',
                background: '#020617',
                color: '#f9fafb'
              }}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '11px 14px',
              borderRadius: 999,
              border: 'none',
              background: loading ? '#4b5563' : 'linear-gradient(90deg,#6366f1,#22c55e)',
              color: '#f9fafb',
              fontWeight: 600,
              cursor: loading ? 'default' : 'pointer'
            }}
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p style={{ marginTop: 18, fontSize: 12, color: '#6b7280' }}>
          Demo credentials: <strong>demo@test.com</strong> / <strong>password</strong>
        </p>
      </div>
    </div>
  );
};

