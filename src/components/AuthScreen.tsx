import { MapPin, Play, Sparkles, Store, Wand2 } from 'lucide-react';

interface AuthScreenProps {
  onEnterCoupleDemo: () => void;
  onEnterVendorDemo: () => void;
  onStartOnboarding: () => void;
}

export function NupciaCrest({ size = 96 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 112 112" aria-label="Nupcia monogram crest">
      <circle cx="56" cy="56" r="52" fill="var(--primary)" fillOpacity="0.12" />
      <circle
        cx="43"
        cy="60"
        r="24"
        fill="none"
        stroke="var(--primary)"
        strokeWidth="4.2"
      />
      <circle
        cx="69"
        cy="60"
        r="24"
        fill="none"
        stroke="var(--tertiary)"
        strokeWidth="4.2"
      />
      <circle cx="56" cy="30" r="5.5" fill="var(--primary)" />
    </svg>
  );
}

export function AuthScreen({
  onEnterCoupleDemo,
  onEnterVendorDemo,
  onStartOnboarding
}: AuthScreenProps) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '32px 20px',
        background:
          'radial-gradient(circle at top, var(--primary-container) 0%, var(--bg) 52%, var(--bg) 100%)'
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 560,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 24
        }}
      >
        {/* Editorial Monogram Header */}
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
          <NupciaCrest size={108} />
          <h1
            className="font-serif"
            style={{
              fontSize: 48,
              fontWeight: 700,
              letterSpacing: '0.1em',
              color: 'var(--primary)',
              lineHeight: 1.05
            }}
          >
            NUPCIA
          </h1>
          <p
            style={{
              fontSize: 13.5,
              fontWeight: 700,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: 'var(--tertiary)'
            }}
          >
            Utah County Wedding Planning Studio
          </p>
          <p style={{ color: 'var(--text-muted)', fontSize: 15, maxWidth: 440, lineHeight: 1.55 }}>
            Editorial wedding planning with a deterministic AI budget concierge, Utah County
            geospatial vendor map, B2B vendor portal, RSVP seating chart, cash registry, and
            stationery studio.
          </p>
        </div>

        {/* Feature Highlights */}
        <div className="card" style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 18 }}>
          <FeatureRow
            icon={<Sparkles size={20} />}
            title="AI Concierge & Dynamic Budget"
            subtitle="Priority-weighted budget rebalancing with real local Utah County vendor matches."
          />
          <FeatureRow
            icon={<MapPin size={20} />}
            title="Utah County Map & Affinity Match"
            subtitle="Discover venues and vendors across Provo, Orem, Lehi, and Provo Canyon."
          />
          <FeatureRow
            icon={<Store size={20} />}
            title="Vendor Portal + RSVP & Stationery"
            subtitle="Accepted bookings auto-create tasks, seating charts, cash funds, and print proofs."
          />
        </div>

        {/* Action Buttons — Prominent Demo Button */}
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 12 }}>
          <button
            type="button"
            className="btn btn-demo"
            style={{ width: '100%', height: 56, fontSize: 16 }}
            onClick={onEnterCoupleDemo}
          >
            <Play size={18} fill="currentColor" />
            Demo — Probar Nupcia Ahora (Emma &amp; Liam)
          </button>

          <div className="grid-2">
            <button
              type="button"
              className="btn btn-primary"
              style={{ height: 48 }}
              onClick={onStartOnboarding}
            >
              <Wand2 size={17} />
              Crear mi boda (Onboarding)
            </button>
            <button
              type="button"
              className="btn btn-outline"
              style={{ height: 48 }}
              onClick={onEnterVendorDemo}
            >
              <Store size={17} />
              Demo Portal Proveedor
            </button>
          </div>

          <p style={{ textAlign: 'center', fontSize: 12.5, color: 'var(--text-muted)', marginTop: 4 }}>
            Interactive Web Edition • Pre-loaded Utah County demo data • Instant couple &amp; vendor switching
          </p>
        </div>
      </div>
    </div>
  );
}

function FeatureRow({
  icon,
  title,
  subtitle
}: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
      <div
        style={{
          width: 42,
          height: 42,
          borderRadius: '50%',
          background: 'var(--primary-container)',
          color: 'var(--primary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0
        }}
      >
        {icon}
      </div>
      <div>
        <div style={{ fontWeight: 700, fontSize: 14.5 }}>{title}</div>
        <div style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 2, lineHeight: 1.45 }}>
          {subtitle}
        </div>
      </div>
    </div>
  );
}
