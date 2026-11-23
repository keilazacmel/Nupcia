import { useEffect, useMemo, useState } from 'react';
import { Compass, Crosshair, MapPin, Minus, Plus, Star, Store } from 'lucide-react';
import { BudgetRebalancer } from '../domain/budgetRebalancer';
import { ScoredVendor } from '../domain/geoMatchEngine';
import { DefaultWeddingCatalog, VendorProfile, Wedding } from '../domain/models';

interface GeospatialMapScreenProps {
  wedding: Wedding | null;
  scoredVendors: ScoredVendor[];
  highlightedVendorId: string | null;
  onClearHighlight: () => void;
  onSendInquiry: (vendor: VendorProfile, pkgTitle: string, budget: number, notes: string) => void;
  onLoadDemoVendors: () => void;
  onSwitchToVendorPortal: () => void;
}

const UTAH_LAKE_COORDS: Array<[number, number]> = [
  [40.355, -111.86],
  [40.35, -111.79],
  [40.29, -111.745],
  [40.225, -111.735],
  [40.165, -111.74],
  [40.095, -111.795],
  [40.075, -111.86],
  [40.145, -111.895],
  [40.245, -111.89]
];

const WASATCH_RIDGE_COORDS: Array<[number, number]> = [
  [40.48, -111.61],
  [40.4, -111.57],
  [40.33, -111.6],
  [40.25, -111.58],
  [40.17, -111.56],
  [40.08, -111.59],
  [40.08, -111.35],
  [40.48, -111.35]
];

const I15_CORRIDOR: Array<[number, number]> = [
  [40.115, -111.6549],
  [40.1652, -111.6108],
  [40.2338, -111.6585],
  [40.2969, -111.6946],
  [40.3641, -111.7385],
  [40.3769, -111.7958],
  [40.3916, -111.8508]
];

const CANYON_ROAD: Array<[number, number]> = [
  [40.2969, -111.6946],
  [40.335, -111.635],
  [40.3934, -111.5888]
];

export function GeospatialMapScreen({
  wedding,
  scoredVendors,
  highlightedVendorId,
  onClearHighlight,
  onSendInquiry,
  onLoadDemoVendors,
  onSwitchToVendorPortal
}: GeospatialMapScreenProps) {
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [onlyInBudget, setOnlyInBudget] = useState(false);
  const [onlyInRadius, setOnlyInRadius] = useState(false);
  const [selectedVendor, setSelectedVendor] = useState<ScoredVendor | null>(null);
  const [inquiryTarget, setInquiryTarget] = useState<ScoredVendor | null>(null);
  const [zoom, setZoom] = useState(1);

  const [pkgTitle, setPkgTitle] = useState('');
  const [budgetText, setBudgetText] = useState('');
  const [notes, setNotes] = useState('');

  const filtered = useMemo(
    () =>
      scoredVendors.filter((sv) => {
        const catOk =
          selectedCategory === 'All' ||
          sv.vendor.category.toLowerCase() === selectedCategory.toLowerCase();
        const budgetOk = !onlyInBudget || sv.withinCategoryBudget;
        const radiusOk = !onlyInRadius || sv.coversWeddingLocation;
        return catOk && budgetOk && radiusOk;
      }),
    [scoredVendors, selectedCategory, onlyInBudget, onlyInRadius]
  );

  useEffect(() => {
    if (highlightedVendorId) {
      const match = scoredVendors.find((sv) => sv.vendor.id === highlightedVendorId) ?? null;
      setSelectedVendor(match);
      onClearHighlight();
    }
  }, [highlightedVendorId, scoredVendors, onClearHighlight]);

  const openInquiry = (sv: ScoredVendor) => {
    setInquiryTarget(sv);
    setPkgTitle(`Full ${sv.vendor.category} Package`);
    const clamped = Math.min(
      Math.max(sv.vendor.minPrice, sv.categoryBudgetAllocated),
      Math.max(sv.vendor.minPrice, sv.vendor.maxPrice)
    );
    setBudgetText(String(Math.round(clamped)));
    setNotes('We love your work! Are you available on our wedding date in Utah County?');
  };

  const centerLat = wedding?.lat ?? 40.2338;
  const centerLng = wedding?.lng ?? -111.6585;

  const width = 820;
  const height = 340;
  const cx = width / 2;
  const cy = height / 2 + 24;
  const scale = 1080 * zoom;

  const toXY = (lat: number, lng: number): [number, number] => [
    cx + (lng - centerLng) * scale,
    cy - (lat - centerLat) * scale
  ];

  const toPoints = (coords: Array<[number, number]>) =>
    coords.map(([lat, lng]) => toXY(lat, lng).join(',')).join(' ');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Filter Header */}
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div>
          <h1 className="font-serif" style={{ fontSize: 28, fontWeight: 700 }}>
            Utah County Vendor Map
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
            Wedding venue: {wedding?.city ?? 'Provo, UT'} ({centerLat.toFixed(3)}, {centerLng.toFixed(3)})
          </p>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          <button
            type="button"
            className={`chip ${onlyInBudget ? 'selected' : ''}`}
            onClick={() => setOnlyInBudget((v) => !v)}
          >
            In budget
          </button>
          <button
            type="button"
            className={`chip ${onlyInRadius ? 'selected' : ''}`}
            onClick={() => setOnlyInRadius((v) => !v)}
          >
            Covers venue
          </button>
          {['All', ...DefaultWeddingCatalog.CATEGORIES.slice(0, 6)].map((cat) => (
            <button
              key={cat}
              type="button"
              className={`chip ${selectedCategory === cat ? 'selected' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* High-Contrast Wasatch Slate-Teal Cartographic Map Canvas */}
      <div
        style={{
          position: 'relative',
          borderRadius: 22,
          overflow: 'hidden',
          border: '1.5px solid rgba(156, 118, 47, 0.55)',
          background: 'linear-gradient(180deg, var(--map-top) 0%, var(--map-bottom) 100%)',
          boxShadow: 'var(--shadow-md)'
        }}
      >
        <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height: 320, display: 'block' }}>
          {/* Topographic Grid */}
          {Array.from({ length: 16 }).map((_, i) => (
            <line
              key={`gx-${i}`}
              x1={i * 54}
              y1={0}
              x2={i * 54}
              y2={height}
              stroke="#4C7285"
              strokeOpacity="0.22"
              strokeWidth="1"
            />
          ))}
          {Array.from({ length: 8 }).map((_, i) => (
            <line
              key={`gy-${i}`}
              x1={0}
              y1={i * 48}
              x2={width}
              y2={i * 48}
              stroke="#4C7285"
              strokeOpacity="0.22"
              strokeWidth="1"
            />
          ))}

          {/* Utah Lake */}
          <polygon
            points={toPoints(UTAH_LAKE_COORDS)}
            fill="#234E66"
            stroke="#42809E"
            strokeWidth="2"
            strokeLinejoin="round"
          />

          {/* Wasatch Front / Mt. Timpanogos Ridge */}
          <polygon
            points={toPoints(WASATCH_RIDGE_COORDS)}
            fill="#2D4C5C"
            fillOpacity="0.58"
            stroke="#6A94A8"
            strokeOpacity="0.4"
            strokeDasharray="10 6"
            strokeWidth="1.5"
          />

          {/* I-15 Valley Corridor & US-189 Provo Canyon */}
          <polyline
            points={toPoints(I15_CORRIDOR)}
            fill="none"
            stroke="#6A94A8"
            strokeOpacity="0.7"
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <polyline
            points={toPoints(CANYON_ROAD)}
            fill="none"
            stroke="#6A94A8"
            strokeOpacity="0.55"
            strokeWidth="1.8"
            strokeLinecap="round"
          />

          {/* Distance Rings (5 mi & 12 mi) */}
          {[0.0725 * scale, 0.174 * scale].map((r, idx) => (
            <circle
              key={idx}
              cx={cx}
              cy={cy}
              r={r}
              fill="none"
              stroke="#F8F4EF"
              strokeOpacity="0.26"
              strokeDasharray="8 6"
              strokeWidth="1.4"
            />
          ))}

          {/* Wedding Venue Pin */}
          <g>
            <circle cx={cx} cy={cy} r={22} fill="#FF6B7A" fillOpacity="0.24" />
            <circle cx={cx} cy={cy} r={11} fill="#F8F4EF" />
            <circle cx={cx} cy={cy} r={8} fill="#FF6B7A" />
            <circle cx={cx} cy={cy} r={2.8} fill="#F8F4EF" />
          </g>

          {/* Vendor Pins */}
          {filtered.map((sv) => {
            const [px, py] = toXY(sv.vendor.lat, sv.vendor.lng);
            const isSelected = selectedVendor?.vendor.id === sv.vendor.id;
            const pinColor = sv.withinCategoryBudget ? '#6EE7B7' : '#F6C667';
            return (
              <g
                key={sv.vendor.id}
                onClick={() => setSelectedVendor(sv)}
                style={{ cursor: 'pointer' }}
              >
                {isSelected && <circle cx={px} cy={py} r={28} fill={pinColor} fillOpacity="0.28" />}
                <circle cx={px} cy={py} r={isSelected ? 14 : 11} fill="#142530" />
                <circle cx={px} cy={py} r={isSelected ? 12 : 9.5} fill="#F8F4EF" />
                <circle cx={px} cy={py} r={isSelected ? 9 : 7} fill={pinColor} />
                <text
                  x={px}
                  y={py - 16}
                  textAnchor="middle"
                  fill="#F8F4EF"
                  fontSize="11"
                  fontWeight="600"
                  style={{ pointerEvents: 'none', textShadow: '0 1px 4px rgba(0,0,0,0.85)' }}
                >
                  {sv.vendor.businessName.split(' ').slice(0, 2).join(' ')}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Legend (Top-Left) */}
        <div
          style={{
            position: 'absolute',
            top: 12,
            left: 12,
            background: 'var(--map-scrim)',
            color: 'var(--map-text)',
            padding: '6px 12px',
            borderRadius: 12,
            display: 'flex',
            gap: 12,
            fontSize: 11.5,
            fontWeight: 600
          }}
        >
          <LegendDot color="#FF6B7A" label="Venue" />
          <LegendDot color="#6EE7B7" label="In budget" />
          <LegendDot color="#F6C667" label="Premium" />
        </div>

        {/* Region Badge (Top-Right) */}
        <div
          style={{
            position: 'absolute',
            top: 12,
            right: 12,
            background: 'var(--map-scrim)',
            color: 'var(--map-text)',
            padding: '6px 12px',
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            gap: 5,
            fontSize: 11.5,
            fontWeight: 700
          }}
        >
          <MapPin size={13} color="#6EE7B7" />
          Utah County, UT
        </div>

        {/* Selected Vendor Quick Callout (Bottom-Left) */}
        {selectedVendor && (
          <button
            type="button"
            onClick={() => openInquiry(selectedVendor)}
            style={{
              position: 'absolute',
              bottom: 12,
              left: 12,
              background: 'var(--map-scrim)',
              color: 'var(--map-text)',
              border: '1px solid rgba(255,255,255,0.16)',
              borderRadius: 14,
              padding: '9px 14px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              cursor: 'pointer',
              textAlign: 'left'
            }}
          >
            <span
              style={{
                width: 10,
                height: 10,
                borderRadius: '50%',
                background: selectedVendor.withinCategoryBudget ? '#6EE7B7' : '#F6C667'
              }}
            />
            <div>
              <div style={{ fontWeight: 700, fontSize: 13 }}>{selectedVendor.vendor.businessName}</div>
              <div style={{ fontSize: 11.5, color: '#6EE7B7' }}>
                {selectedVendor.vendor.city} • {selectedVendor.distanceMiles.toFixed(1)} mi •{' '}
                {selectedVendor.affinityScore}% match — Click to inquire
              </div>
            </div>
          </button>
        )}

        {/* Zoom & Recenter Controls (Bottom-Right) */}
        <div
          style={{
            position: 'absolute',
            bottom: 12,
            right: 12,
            display: 'flex',
            flexDirection: 'column',
            gap: 6
          }}
        >
          <button
            type="button"
            className="icon-btn"
            style={{ background: 'var(--map-scrim)', color: '#F8F4EF' }}
            onClick={() => setZoom((z) => Math.min(2.2, +(z + 0.25).toFixed(2)))}
            aria-label="Zoom in"
          >
            <Plus size={17} />
          </button>
          <button
            type="button"
            className="icon-btn"
            style={{ background: 'var(--map-scrim)', color: '#F8F4EF' }}
            onClick={() => setZoom((z) => Math.max(0.65, +(z - 0.25).toFixed(2)))}
            aria-label="Zoom out"
          >
            <Minus size={17} />
          </button>
          <button
            type="button"
            className="icon-btn"
            style={{ background: 'var(--map-scrim)', color: '#FF6B7A' }}
            onClick={() => setZoom(1)}
            aria-label="Recenter map"
          >
            <Crosshair size={17} />
          </button>
        </div>
      </div>

      {/* Matching Vendors List */}
      {filtered.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
          <Compass size={36} color="var(--primary)" />
          <h3 style={{ fontSize: 17, fontWeight: 700 }}>No vendors match these filters yet</h3>
          <p style={{ fontSize: 13.5, color: 'var(--text-muted)', maxWidth: 420 }}>
            Adjust your filters, publish your business in the Vendor Portal, or reload the 5 Utah
            County sample vendors.
          </p>
          <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
            <button type="button" className="btn btn-primary btn-sm" onClick={onLoadDemoVendors}>
              Load 5 Utah vendors
            </button>
            <button type="button" className="btn btn-outline btn-sm" onClick={onSwitchToVendorPortal}>
              <Store size={15} /> Vendor Portal
            </button>
          </div>
        </div>
      ) : (
        <div className="grid-2">
          {filtered.map((sv) => {
            const v = sv.vendor;
            const active = selectedVendor?.vendor.id === v.id;
            return (
              <div
                key={v.id}
                className="card"
                onClick={() => setSelectedVendor(sv)}
                style={{
                  cursor: 'pointer',
                  borderColor: active ? 'var(--primary)' : undefined,
                  background: active ? 'var(--primary-container)' : undefined,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: 15.5 }}>{v.businessName}</div>
                    <div style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>
                      {v.category} • {v.city} • {sv.distanceMiles.toFixed(1)} mi (
                      {sv.coverageRadiusMiles} mi radius)
                    </div>
                  </div>
                  <span className="badge badge-secondary">{sv.affinityScore}% match</span>
                </div>

                <p style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.45 }}>
                  {v.description}
                </p>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, marginTop: 'auto', paddingTop: 6 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12.5 }}>
                    <Star size={14} color="var(--tertiary)" fill="var(--tertiary)" />
                    <span>
                      {v.ratingAvg} ({v.completedEventsCount})
                    </span>
                    <span>•</span>
                    <strong style={{ color: 'var(--primary)' }}>
                      {BudgetRebalancer.formatCurrency(v.minPrice)}–
                      {BudgetRebalancer.formatCurrency(v.maxPrice)}
                    </strong>
                  </div>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      openInquiry(sv);
                    }}
                  >
                    Inquire
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Send Inquiry Modal */}
      {inquiryTarget && (
        <div className="modal-backdrop" onClick={() => setInquiryTarget(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Inquire with {inquiryTarget.vendor.businessName}
            </h3>
            <p style={{ fontSize: 13, color: 'var(--primary)', fontWeight: 600 }}>
              Your allocated budget for {inquiryTarget.vendor.category}:{' '}
              {BudgetRebalancer.formatCurrency(inquiryTarget.categoryBudgetAllocated)}
            </p>
            <div className="field">
              <label>Desired service / package</label>
              <input className="input" value={pkgTitle} onChange={(e) => setPkgTitle(e.target.value)} />
            </div>
            <div className="field">
              <label>Proposed budget (USD)</label>
              <input
                className="input"
                type="number"
                value={budgetText}
                onChange={(e) => setBudgetText(e.target.value)}
              />
            </div>
            <div className="field">
              <label>Notes for vendor</label>
              <textarea className="textarea" value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setInquiryTarget(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onSendInquiry(
                    inquiryTarget.vendor,
                    pkgTitle,
                    Number(budgetText) || inquiryTarget.vendor.minPrice,
                    notes
                  );
                  setInquiryTarget(null);
                }}
              >
                Send inquiry
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function LegendDot({ color, label }: { color: string; label: string }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
      <span style={{ width: 8, height: 8, borderRadius: '50%', background: color }} />
      {label}
    </span>
  );
}
