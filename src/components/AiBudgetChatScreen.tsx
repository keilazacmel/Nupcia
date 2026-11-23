import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, MapPin, Send, Sparkles, Star } from 'lucide-react';
import { ChatTurn } from '../data/aiConcierge';
import { BudgetRebalancer, RebalanceProposal } from '../domain/budgetRebalancer';
import { VendorProfile } from '../domain/models';

interface AiBudgetChatScreenProps {
  turns: ChatTurn[];
  isThinking: boolean;
  onSendPrompt: (prompt: string) => void;
  onApplyProposal: (turnId: string, proposal: RebalanceProposal) => void;
  onViewVendorOnMap: (vendorId: string) => void;
  onSendInquiry: (vendor: VendorProfile, pkgTitle: string, budget: number, notes: string) => void;
}

const QUICK_PROMPTS = [
  'Find photography & florals in Utah County within budget',
  'Prioritize venue and catering without going over total',
  'How can we save on music and stationery?'
];

export function AiBudgetChatScreen({
  turns,
  isThinking,
  onSendPrompt,
  onApplyProposal,
  onViewVendorOnMap,
  onSendInquiry
}: AiBudgetChatScreenProps) {
  const [inputText, setInputText] = useState('');
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [turns.length, isThinking]);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || isThinking) return;
    onSendPrompt(inputText);
    setInputText('');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sparkles size={20} color="var(--primary)" />
            <h1 className="font-serif" style={{ fontSize: 28, fontWeight: 700 }}>
              AI Budget Concierge
            </h1>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: 13.5, marginTop: 2 }}>
            Deterministic priority rebalancing + real Utah County vendor affinity matching
          </p>
        </div>
        <span className="badge badge-tertiary">Zero-Hallucination Math Engine</span>
      </div>

      {/* Chat Messages */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {turns.map((turn) => (
          <div
            key={turn.id}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: turn.fromUser ? 'flex-end' : 'flex-start',
              gap: 10
            }}
          >
            <div
              style={{
                maxWidth: turn.fromUser ? '80%' : '94%',
                padding: '14px 18px',
                borderRadius: turn.fromUser ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                background: turn.fromUser ? 'var(--primary)' : 'var(--surface)',
                color: turn.fromUser ? 'var(--on-primary)' : 'var(--text)',
                border: turn.fromUser ? 'none' : '1px solid var(--outline-variant)',
                boxShadow: 'var(--shadow-sm)',
                lineHeight: 1.55,
                fontSize: 14.5
              }}
            >
              <div>{turn.text}</div>
              {turn.impactSummary && (
                <div
                  style={{
                    marginTop: 10,
                    padding: '10px 12px',
                    borderRadius: 12,
                    background: 'var(--tertiary-container)',
                    color: 'var(--on-tertiary-container)',
                    fontSize: 12.5,
                    fontWeight: 500
                  }}
                >
                  <strong>Financial impact:</strong> {turn.impactSummary}
                </div>
              )}
            </div>

            {/* Deterministic Budget Rebalance Proposal Card */}
            {turn.proposal && (
              <div className="card" style={{ width: '100%', maxWidth: 640, display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
                  <div style={{ fontWeight: 700, fontSize: 15, color: 'var(--primary)' }}>
                    Exact Budget Rebalance Proposal
                  </div>
                  <span className="badge badge-secondary">
                    Verified total: {BudgetRebalancer.formatCurrency(turn.proposal.totalProposed)} /{' '}
                    {BudgetRebalancer.formatCurrency(turn.proposal.totalBudget)}
                  </span>
                </div>

                <div style={{ borderTop: '1px solid var(--outline-variant)', paddingTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {turn.proposal.ranges.slice(0, 6).map((r) => {
                    const sign = r.delta > 0 ? '+' : '';
                    return (
                      <div
                        key={r.category}
                        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}
                      >
                        <div>
                          <div style={{ fontWeight: 700, fontSize: 13.5 }}>
                            #{r.priorityRank} {r.category} {r.locked ? '🔒' : ''}
                          </div>
                          <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                            Range {BudgetRebalancer.formatCurrency(r.suggestedMin)} –{' '}
                            {BudgetRebalancer.formatCurrency(r.suggestedMax)}
                          </div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontWeight: 700, fontSize: 14.5 }}>
                            {BudgetRebalancer.formatCurrency(r.suggestedIdeal)}
                          </div>
                          {r.delta !== 0 && (
                            <div
                              style={{
                                fontSize: 12,
                                fontWeight: 700,
                                color: r.delta > 0 ? 'var(--secondary)' : 'var(--tertiary)'
                              }}
                            >
                              {sign}
                              {BudgetRebalancer.formatCurrency(r.delta)}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ width: '100%', marginTop: 4 }}
                  disabled={turn.proposalApplied}
                  onClick={() => onApplyProposal(turn.id, turn.proposal!)}
                >
                  <CheckCircle2 size={17} />
                  {turn.proposalApplied
                    ? 'Budget adjustment applied'
                    : 'Apply this budget adjustment'}
                </button>
              </div>
            )}

            {/* Matched Vendor Recommendation Cards */}
            {turn.recommendedVendors && turn.recommendedVendors.length > 0 && (
              <div style={{ width: '100%', maxWidth: 640, display: 'flex', flexDirection: 'column', gap: 10 }}>
                {turn.recommendedVendors.map((sv) => (
                  <div key={sv.vendor.id} className="card-subtle" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                      <div style={{ fontWeight: 700, fontSize: 14.5 }}>{sv.vendor.businessName}</div>
                      <span className="badge badge-secondary">{sv.affinityScore}% match</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-muted)' }}>
                      <span>
                        {sv.vendor.category} • {BudgetRebalancer.formatCurrency(sv.vendor.minPrice)}–
                        {BudgetRebalancer.formatCurrency(sv.vendor.maxPrice)}
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3, color: 'var(--tertiary)', fontWeight: 700 }}>
                        <Star size={13} fill="currentColor" /> {sv.vendor.ratingAvg}
                      </span>
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {sv.distanceMiles.toFixed(1)} mi from venue •{' '}
                      {sv.coversWeddingLocation ? 'Covers your area' : 'Outside usual radius'}
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 2 }}>
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        style={{ flex: 1 }}
                        onClick={() => onViewVendorOnMap(sv.vendor.id)}
                      >
                        <MapPin size={15} /> View on map
                      </button>
                      <button
                        type="button"
                        className="btn btn-tonal btn-sm"
                        style={{ flex: 1 }}
                        onClick={() =>
                          onSendInquiry(
                            sv.vendor,
                            `${sv.vendor.category} Inquiry (AI Match)`,
                            Math.max(sv.categoryBudgetAllocated, sv.vendor.minPrice),
                            `Recommended by Nupcia AI Concierge with ${sv.affinityScore}% wedding affinity.`
                          )
                        }
                      >
                        Send inquiry
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}

        {isThinking && (
          <div className="card" style={{ maxWidth: 420, fontSize: 13.5, color: 'var(--text-muted)' }}>
            Calculating exact budget rebalance &amp; matching Utah County vendors…
          </div>
        )}
        <div ref={endRef} />
      </div>

      {/* Quick Prompts */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {QUICK_PROMPTS.map((prompt) => (
          <button
            key={prompt}
            type="button"
            className="chip"
            disabled={isThinking}
            onClick={() => onSendPrompt(prompt)}
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Composer */}
      <form
        onSubmit={submit}
        className="card"
        style={{ display: 'flex', gap: 10, padding: 12, alignItems: 'center' }}
      >
        <input
          className="input"
          placeholder="e.g. Find editorial photography & florals in Utah County…"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
        />
        <button
          type="submit"
          className="btn btn-primary"
          disabled={!inputText.trim() || isThinking}
          style={{ height: 44, paddingInline: 18 }}
        >
          <Send size={16} /> Send
        </button>
      </form>
    </div>
  );
}
