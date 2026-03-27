import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';

const API_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? '/api';

interface ContentItem {
  contentId: string;
  title: string;
  body: string;
  createdAt: string;
}

interface ContentResponse {
  items: ContentItem[];
  count: number;
}

export const Dashboard: React.FC = () => {
  const { user, token, logout } = useAuth();
  const [items, setItems] = useState<ContentItem[]>([]);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadContent = async () => {
    if (!API_URL) return;
    setError(null);
    try {
      const res = await axios.get<ContentResponse>(`${API_URL}/content`, {
        headers: {
          Authorization: token ? `Bearer ${token}` : ''
        }
      });
      setItems(res.data.items || []);
    } catch (e: any) {
      setError(e?.response?.data?.error || e?.message || 'Failed to load content');
    }
  };

  useEffect(() => {
    void loadContent();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!API_URL) return;
    if (!title || !body) return;
    setLoading(true);
    setError(null);
    try {
      await axios.post(
        `${API_URL}/content`,
        { title, body },
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: token ? `Bearer ${token}` : ''
          }
        }
      );
      setTitle('');
      setBody('');
      await loadContent();
    } catch (e: any) {
      setError(e?.response?.data?.error || e?.message || 'Failed to create content');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#020617',
        color: '#e5e7eb',
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      <header
        style={{
          padding: '16px 24px',
          borderBottom: '1px solid rgba(148,163,184,0.35)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}
      >
        <div>
          <h1 style={{ fontSize: 22, marginBottom: 4 }}>Serverless Portal Dashboard</h1>
          <p style={{ fontSize: 13, color: '#9ca3af' }}>
            Backed by AWS Lambda + DynamoDB via LocalStack.
          </p>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 14 }}>{user?.email}</div>
          <div style={{ fontSize: 12, color: '#9ca3af' }}>Role: {user?.role}</div>
          <button
            onClick={logout}
            style={{
              marginTop: 8,
              padding: '6px 12px',
              borderRadius: 999,
              border: '1px solid rgba(148,163,184,0.6)',
              background: 'transparent',
              color: '#e5e7eb',
              fontSize: 12,
              cursor: 'pointer'
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <main style={{ padding: 24, maxWidth: 960, width: '100%', margin: '0 auto', flex: 1 }}>
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

        <section
          style={{
            marginBottom: 24,
            padding: 20,
            borderRadius: 16,
            border: '1px solid rgba(148,163,184,0.35)',
            background:
              'radial-gradient(circle at top left, rgba(56,189,248,0.12), transparent 55%), #020617'
          }}
        >
          <h2 style={{ fontSize: 18, marginBottom: 12 }}>Create Content</h2>
          <form onSubmit={handleCreate}>
            <div style={{ marginBottom: 10 }}>
              <input
                type="text"
                placeholder="Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                style={{
                  width: '100%',
                  padding: '9px 11px',
                  borderRadius: 8,
                  border: '1px solid rgba(148,163,184,0.5)',
                  background: '#020617',
                  color: '#f9fafb'
                }}
              />
            </div>
            <div style={{ marginBottom: 10 }}>
              <textarea
                placeholder="Body"
                value={body}
                onChange={(e) => setBody(e.target.value)}
                rows={3}
                style={{
                  width: '100%',
                  padding: '9px 11px',
                  borderRadius: 8,
                  border: '1px solid rgba(148,163,184,0.5)',
                  background: '#020617',
                  color: '#f9fafb',
                  resize: 'vertical'
                }}
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              style={{
                padding: '8px 14px',
                borderRadius: 999,
                border: 'none',
                background: loading ? '#4b5563' : 'linear-gradient(90deg,#22c55e,#06b6d4)',
                color: '#f9fafb',
                fontSize: 14,
                fontWeight: 600,
                cursor: loading ? 'default' : 'pointer'
              }}
            >
              {loading ? 'Saving…' : 'Add item'}
            </button>
          </form>
        </section>

        <section
          style={{
            padding: 20,
            borderRadius: 16,
            border: '1px solid rgba(148,163,184,0.35)',
            background:
              'radial-gradient(circle at top right, rgba(129,140,248,0.16), transparent 55%), #020617'
          }}
        >
          <h2 style={{ fontSize: 18, marginBottom: 12 }}>Your Content</h2>
          {items.length === 0 ? (
            <p style={{ color: '#9ca3af', fontSize: 14 }}>No content yet. Create your first item.</p>
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {items.map((item) => (
                <div
                  key={item.contentId}
                  style={{
                    borderRadius: 12,
                    padding: 14,
                    border: '1px solid rgba(148,163,184,0.35)',
                    background: 'rgba(15,23,42,0.9)'
                  }}
                >
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>{item.title}</div>
                  <div style={{ fontSize: 14, color: '#d1d5db', marginBottom: 6 }}>{item.body}</div>
                  <div style={{ fontSize: 11, color: '#6b7280' }}>
                    Created at: {new Date(item.createdAt).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

