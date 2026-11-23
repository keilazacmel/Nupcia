import { useMemo, useState } from 'react';
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Check, Heart, Play, Store } from 'lucide-react';
import { BudgetRebalancer } from '../domain/budgetRebalancer';
import { DefaultWeddingCatalog } from '../domain/models';

interface OnboardingScreenProps {
  onSwitchToVendor: () => void;
  onLoadQuickDemo: () => void;
  onComplete: (params: {
    brideName: string;
    partnerName: string;
    weddingDate: string;
    city: string;
    lat: number;
    lng: number;
    totalBudget: number;
    estimatedGuests: number;
    styleTags: string[];
    colorPalette: string[];
    ceremonyType: string;
    visionExpectations: string;
    priorityOrder: string[];
  }) => void;
}

export function OnboardingScreen({
  onSwitchToVendor,
  onLoadQuickDemo,
  onComplete
}: OnboardingScreenProps) {
  const [step, setStep] = useState(0);
  const [brideName, setBrideName] = useState('Emma');
  const [partnerName, setPartnerName] = useState('Liam');
  const [weddingDate, setWeddingDate] = useState('2027-06-19');
  const [selectedCity, setSelectedCity] = useState(DefaultWeddingCatalog.CITIES[0]);
  const [budgetText, setBudgetText] = useState('35000');
  const [guestsText, setGuestsText] = useState('140');

  const [selectedStyles, setSelectedStyles] = useState<string[]>(['Garden', 'Classic']);
  const [selectedPaletteName, setSelectedPaletteName] = useState(
    Object.keys(DefaultWeddingCatalog.COLOR_PALETTES)[0]
  );
  const [selectedCeremony, setSelectedCeremony] = useState(
    DefaultWeddingCatalog.CEREMONY_TYPES[0]
  );
  const [visionExpectations, setVisionExpectations] = useState(
    'A luminous mountain-view garden celebration with seasonal dining and timeless editorial photography.'
  );
  const [priorityOrder, setPriorityOrder] = useState<string[]>([
    ...DefaultWeddingCatalog.CATEGORIES
  ]);

  const parsedBudget = Math.min(50000000, Math.max(1000, Number(budgetText) || 35000));
  const previewAllocations = useMemo(
    () => BudgetRebalancer.computeInitialAllocations(parsedBudget, priorityOrder),
    [parsedBudget, priorityOrder]
  );
  const matchedSuite = useMemo(
    () => DefaultWeddingCatalog.suiteForStyles(selectedStyles),
    [selectedStyles]
  );

  const toggleStyle = (tag: string) => {
    setSelectedStyles((prev) => {
      if (prev.includes(tag)) {
        return prev.length > 1 ? prev.filter((t) => t !== tag) : prev;
      }
      return prev.length < 4 ? [...prev, tag] : prev;
    });
  };

  const movePriority = (idx: number, dir: -1 | 1) => {
    const nextIdx = idx + dir;
    if (nextIdx < 0 || nextIdx >= priorityOrder.length) return;
    const copy = [...priorityOrder];
    const [item] = copy.splice(idx, 1);
    copy.splice(nextIdx, 0, item);
    setPriorityOrder(copy);
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', padding: '24px 16px 40px' }}>
      <div style={{ maxWidth: 680, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 18 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ fontWeight: 700, fontSize: 13.5, color: 'var(--primary)' }}>
            Step {step + 1} of 3
          </span>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="button" className="btn btn-tonal btn-sm" onClick={onLoadQuickDemo}>
              <Play size={14} /> Instant Demo
            </button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={onSwitchToVendor}>
              <Store size={15} /> I&apos;m a Vendor
            </button>
          </div>
        </div>

        <div className="progress-track">
          <div className="progress-fill" style={{ width: `${((step + 1) / 3) * 100}%` }} />
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          {step === 0 && (
            <>
              <div>
                <h2 className="font-serif" style={{ fontSize: 30, fontWeight: 700 }}>
                  Let&apos;s start with your story
                </h2>
                <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>
                  These details calibrate your smart wedding budget, Utah County vendor map, and
                  wedding website.
                </p>
              </div>

              <div className="grid-2">
                <div className="field">
                  <label>Your name</label>
                  <input className="input" value={brideName} onChange={(e) => setBrideName(e.target.value)} />
                </div>
                <div className="field">
                  <label>Partner&apos;s name</label>
                  <input className="input" value={partnerName} onChange={(e) => setPartnerName(e.target.value)} />
                </div>
              </div>

              <div className="grid-2">
                <div className="field">
                  <label>Wedding date</label>
                  <input
                    className="input"
                    type="date"
                    value={weddingDate}
                    onChange={(e) => setWeddingDate(e.target.value)}
                  />
                </div>
                <div className="field">
                  <label>Estimated guests</label>
                  <input
                    className="input"
                    type="number"
                    min={2}
                    max={5000}
                    value={guestsText}
                    onChange={(e) => setGuestsText(e.target.value)}
                  />
                </div>
              </div>

              <div className="field">
                <label>Estimated total budget (USD)</label>
                <input
                  className="input"
                  type="number"
                  min={1000}
                  step={500}
                  value={budgetText}
                  onChange={(e) => setBudgetText(e.target.value)}
                />
              </div>

              <div className="field">
                <label>Utah County wedding location</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 4 }}>
                  {DefaultWeddingCatalog.CITIES.map((city) => (
                    <button
                      key={city.name}
                      type="button"
                      className={`chip ${selectedCity.name === city.name ? 'selected' : ''}`}
                      onClick={() => setSelectedCity(city)}
                    >
                      {city.name}
                    </button>
                  ))}
                </div>
              </div>
            </>
          )}

          {step === 1 && (
            <>
              <div>
                <h2 className="font-serif" style={{ fontSize: 30, fontWeight: 700 }}>
                  Aesthetic vision &amp; expectations
                </h2>
                <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>
                  We tailor your stationery suite, wedding website, and vendor affinity scores to
                  match your style.
                </p>
              </div>

              <div className="field">
                <label>Wedding styles you love (up to 4)</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 4 }}>
                  {DefaultWeddingCatalog.STYLE_TAGS.map((tag) => {
                    const active = selectedStyles.includes(tag);
                    return (
                      <button
                        key={tag}
                        type="button"
                        className={`chip ${active ? 'selected' : ''}`}
                        onClick={() => toggleStyle(tag)}
                      >
                        {active && <Check size={14} />}
                        {tag}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="field">
                <label>Color palette</label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
                  {Object.entries(DefaultWeddingCatalog.COLOR_PALETTES).map(([name, hexes]) => {
                    const active = selectedPaletteName === name;
                    return (
                      <button
                        key={name}
                        type="button"
                        onClick={() => setSelectedPaletteName(name)}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '12px 16px',
                          borderRadius: 14,
                          border: active ? '2px solid var(--primary)' : '1px solid var(--outline-variant)',
                          background: active ? 'var(--primary-container)' : 'var(--surface)',
                          cursor: 'pointer'
                        }}
                      >
                        <span style={{ fontWeight: 600, fontSize: 14 }}>{name}</span>
                        <div style={{ display: 'flex', gap: 6 }}>
                          {hexes.map((hex) => (
                            <span
                              key={hex}
                              style={{
                                width: 22,
                                height: 22,
                                borderRadius: '50%',
                                background: hex,
                                border: '1px solid var(--outline)'
                              }}
                            />
                          ))}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="field">
                <label>Ceremony setting</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 4 }}>
                  {DefaultWeddingCatalog.CEREMONY_TYPES.map((ct) => (
                    <button
                      key={ct}
                      type="button"
                      className={`chip ${selectedCeremony === ct ? 'selected' : ''}`}
                      onClick={() => setSelectedCeremony(ct)}
                    >
                      {ct}
                    </button>
                  ))}
                </div>
              </div>

              <div className="field">
                <label>What do you dream of remembering most?</label>
                <textarea
                  className="textarea"
                  value={visionExpectations}
                  onChange={(e) => setVisionExpectations(e.target.value)}
                />
              </div>
            </>
          )}

          {step === 2 && (
            <>
              <div>
                <h2 className="font-serif" style={{ fontSize: 30, fontWeight: 700 }}>
                  Priority Ranking
                </h2>
                <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>
                  Rank categories from highest to lowest importance. Our engine recalculates your
                  allocations in real time without exceeding{' '}
                  <strong>{BudgetRebalancer.formatCurrency(parsedBudget)}</strong>.
                </p>
              </div>

              <div
                style={{
                  background: 'var(--tertiary-container)',
                  color: 'var(--on-tertiary-container)',
                  padding: '14px 16px',
                  borderRadius: 16,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12
                }}
              >
                <Heart size={20} color="var(--primary)" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: 14 }}>
                    Matched suite: {matchedSuite.name}
                  </div>
                  <div style={{ fontSize: 12.5, opacity: 0.85 }}>{matchedSuite.subtitle}</div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {priorityOrder.map((cat, idx) => {
                  const amount =
                    previewAllocations.find((a) => a.category === cat)?.allocatedAmount ?? 0;
                  return (
                    <div
                      key={cat}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '10px 14px',
                        borderRadius: 14,
                        border: '1px solid var(--outline-variant)',
                        background: 'var(--bg)'
                      }}
                    >
                      <span
                        style={{
                          width: 30,
                          height: 30,
                          borderRadius: '50%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 700,
                          fontSize: 13,
                          background: idx < 2 ? 'var(--primary)' : 'var(--surface-variant)',
                          color: idx < 2 ? 'var(--on-primary)' : 'var(--text-muted)'
                        }}
                      >
                        {idx + 1}
                      </span>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, fontSize: 14 }}>{cat}</div>
                        <div style={{ fontSize: 12.5, color: 'var(--primary)', fontWeight: 600 }}>
                          Suggested: {BudgetRebalancer.formatCurrency(amount)}
                        </div>
                      </div>
                      <button
                        type="button"
                        className="icon-btn"
                        disabled={idx === 0}
                        onClick={() => movePriority(idx, -1)}
                        aria-label="Move up"
                      >
                        <ArrowUp size={17} />
                      </button>
                      <button
                        type="button"
                        className="icon-btn"
                        disabled={idx === priorityOrder.length - 1}
                        onClick={() => movePriority(idx, 1)}
                        aria-label="Move down"
                      >
                        <ArrowDown size={17} />
                      </button>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>

        <div style={{ display: 'flex', gap: 12 }}>
          {step > 0 && (
            <button
              type="button"
              className="btn btn-outline"
              style={{ flex: 1, height: 50 }}
              onClick={() => setStep((s) => s - 1)}
            >
              <ArrowLeft size={17} /> Back
            </button>
          )}
          <button
            type="button"
            className="btn btn-primary"
            style={{ flex: 1.4, height: 50 }}
            onClick={() => {
              if (step < 2) {
                setStep((s) => s + 1);
              } else {
                onComplete({
                  brideName,
                  partnerName,
                  weddingDate,
                  city: selectedCity.name,
                  lat: selectedCity.lat,
                  lng: selectedCity.lng,
                  totalBudget: parsedBudget,
                  estimatedGuests: Math.min(5000, Math.max(2, Number(guestsText) || 140)),
                  styleTags: selectedStyles,
                  colorPalette:
                    DefaultWeddingCatalog.COLOR_PALETTES[selectedPaletteName] ?? [
                      '#7B2D3B',
                      '#FAF6F1',
                      '#536B5E'
                    ],
                  ceremonyType: selectedCeremony,
                  visionExpectations,
                  priorityOrder
                });
              }
            }}
          >
            {step < 2 ? (
              <>
                Continue <ArrowRight size={17} />
              </>
            ) : (
              <>
                Create My Wedding Plan <Check size={17} />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
