import { useState } from 'react';
import {
  CheckCircle2,
  Edit3,
  Lock,
  LockOpen,
  LogOut,
  Sparkles,
  Store,
  TrendingUp
} from 'lucide-react';
import { BudgetRebalancer } from '../domain/budgetRebalancer';
import { ReadinessReport } from '../domain/geoMatchEngine';
import { BudgetAllocation, ServiceRequest, ServiceRequestStatus, Wedding } from '../domain/models';

interface CoupleHomeScreenProps {
  wedding: Wedding | null;
  allocations: BudgetAllocation[];
  requests: ServiceRequest[];
  readiness: ReadinessReport;
  onNavigateTab: (tab: number) => void;
  onUpdateAllocation: (id: string, allocated: number, spent: number, locked: boolean) => void;
  onRequestAction: (req: ServiceRequest, status: ServiceRequestStatus) => void;
  onSwitchToVendor: () => void;
  onSignOut: () => void;
}

export function CoupleHomeScreen({
  wedding,
  allocations,
  requests,
  readiness,
  onNavigateTab,
  onUpdateAllocation,
  onRequestAction,
  onSwitchToVendor,
  onSignOut
}: CoupleHomeScreenProps) {
  const [editing, setEditing] = useState<BudgetAllocation | null>(null);
  const [allocInput, setAllocInput] = useState('');
  const [spentInput, setSpentInput] = useState('');
  const [lockedInput, setLockedInput] = useState(false);

  const totalBudget = wedding?.totalBudget ?? 35000;
  const totalAllocated = allocations.reduce((acc, a) => acc + a.allocatedAmount, 0);
  const totalSpent = allocations.reduce((acc, a) => acc + a.spentAmount, 0);

  const openEdit = (alloc: BudgetAllocation) => {
    setEditing(alloc);
    setAllocInput(String(Math.round(alloc.allocatedAmount)));
    setSpentInput(String(Math.round(alloc.spentAmount)));
    setLockedInput(alloc.locked);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Editorial Couple Hero Card */}
      <div className="hero-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
          <span
            style={{
              background: 'rgba(255,255,255,0.16)',
              padding: '5px 13px',
              borderRadius: 999,
              fontSize: 12.5,
              fontWeight: 600
            }}
          >
            {wedding?.weddingDate ?? 'Date TBD'} • {wedding?.city ?? 'Provo, UT'}
          </span>
          <div style={{ display: 'flex', gap: 4 }}>
            <button
              type="button"
              className="icon-btn"
              title="Switch to Vendor Portal"
              onClick={onSwitchToVendor}
            >
              <Store size={19} />
            </button>
            <button type="button" className="icon-btn" title="Exit to Welcome" onClick={onSignOut}>
              <LogOut size={19} />
            </button>
          </div>
        </div>

        <h1 className="font-serif" style={{ fontSize: 40, fontWeight: 700, marginTop: 12, lineHeight: 1.1 }}>
          {wedding?.brideName ?? 'Emma'} &amp; {wedding?.partnerName ?? 'Liam'}
        </h1>

        {wedding && (
          <p style={{ fontSize: 13.5, opacity: 0.88, marginTop: 6 }}>
            Style: {wedding.styleTags.join(' • ')} | {wedding.estimatedGuests} guests |{' '}
            {wedding.ceremonyType}
          </p>
        )}

        <div className="grid-3" style={{ marginTop: 18 }}>
          <MetricPill label="Total Budget" value={BudgetRebalancer.formatCurrency(totalBudget)} />
          <MetricPill label="Committed" value={BudgetRebalancer.formatCurrency(totalSpent)} />
          <MetricPill label="Readiness Index" value={`${readiness.score}%`} />
        </div>
      </div>

      {/* Next Best Action + Budget Health */}
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <TrendingUp size={20} color="var(--secondary)" />
            <span style={{ fontWeight: 700, fontSize: 16 }}>Next Best Action</span>
          </div>
          <span className="badge badge-secondary">Budget health {readiness.budgetHealth}/100</span>
        </div>

        <div style={{ fontWeight: 700, fontSize: 15, color: 'var(--primary)' }}>
          {readiness.nextBestActionTitle}
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: 13.5, lineHeight: 1.5 }}>
          {readiness.nextBestActionSubtitle}
        </p>

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 4 }}>
          <button
            type="button"
            className="btn btn-tonal btn-sm"
            onClick={() => onNavigateTab(readiness.nextBestActionTab)}
          >
            Go now
          </button>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={() => onNavigateTab(1)}
          >
            <Sparkles size={15} /> Optimize with AI
          </button>
        </div>
      </div>

      {/* Active Vendor Inquiries */}
      {requests.length > 0 && (
        <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
            Vendor Inquiries ({requests.length})
          </h2>
          <div className="grid-2">
            {requests.slice(0, 6).map((req) => (
              <div key={req.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: 15 }}>{req.vendorName}</div>
                    <div style={{ fontSize: 12.5, color: 'var(--text-muted)', marginTop: 2 }}>
                      {req.category} • {req.catalogItemTitle} • {req.eventDate}
                    </div>
                  </div>
                  <StatusBadge status={req.status} />
                </div>

                <div style={{ fontSize: 13.5, fontWeight: 700, color: 'var(--primary)' }}>
                  {req.status === 'COUNTERED' && req.counterOfferBudget > 0
                    ? `Proposed: ${BudgetRebalancer.formatCurrency(req.proposedBudget)} → Counteroffer: ${BudgetRebalancer.formatCurrency(req.counterOfferBudget)}`
                    : `Proposed budget: ${BudgetRebalancer.formatCurrency(req.proposedBudget)}`}
                </div>

                {req.notes && (
                  <p style={{ fontSize: 12.5, color: 'var(--text-muted)', lineHeight: 1.45 }}>
                    {req.notes}
                  </p>
                )}

                {(req.status === 'COUNTERED' || req.status === 'PENDING') && (
                  <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                    {req.status === 'COUNTERED' && (
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => onRequestAction(req, 'ACCEPTED')}
                      >
                        <CheckCircle2 size={15} /> Accept counteroffer
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={() => onRequestAction(req, 'CANCELLED')}
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Priority-Weighted Budget Plan */}
      <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
          <div>
            <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Priority Budget Plan
            </h2>
            <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              Allocated {BudgetRebalancer.formatCurrency(totalAllocated)} of{' '}
              {BudgetRebalancer.formatCurrency(totalBudget)}
            </p>
          </div>
          <button type="button" className="btn btn-tonal btn-sm" onClick={() => onNavigateTab(1)}>
            <Sparkles size={15} /> AI Rebalance
          </button>
        </div>

        <div className="grid-2">
          {allocations.map((alloc) => {
            const pct =
              alloc.allocatedAmount > 0
                ? Math.min(100, (alloc.spentAmount / alloc.allocatedAmount) * 100)
                : 0;
            const isOver = alloc.spentAmount > alloc.allocatedAmount && alloc.allocatedAmount > 0;
            return (
              <div key={alloc.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span
                    style={{
                      width: 30,
                      height: 30,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 12.5,
                      fontWeight: 700,
                      background:
                        alloc.priorityRank <= 2 ? 'var(--primary-container)' : 'var(--surface-variant)',
                      color: alloc.priorityRank <= 2 ? 'var(--primary)' : 'var(--text-muted)'
                    }}
                  >
                    #{alloc.priorityRank}
                  </span>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700, fontSize: 14.5 }}>{alloc.category}</div>
                    <div
                      style={{
                        fontSize: 12.5,
                        color: isOver ? 'var(--error)' : 'var(--text-muted)'
                      }}
                    >
                      {BudgetRebalancer.formatCurrency(alloc.spentAmount)} spent /{' '}
                      {BudgetRebalancer.formatCurrency(alloc.allocatedAmount)}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="icon-btn"
                    title={alloc.locked ? 'Unlock during AI rebalance' : 'Lock during AI rebalance'}
                    onClick={() =>
                      onUpdateAllocation(
                        alloc.id,
                        alloc.allocatedAmount,
                        alloc.spentAmount,
                        !alloc.locked
                      )
                    }
                    style={{ color: alloc.locked ? 'var(--tertiary)' : 'var(--text-muted)' }}
                  >
                    {alloc.locked ? <Lock size={17} /> : <LockOpen size={17} />}
                  </button>
                  <button
                    type="button"
                    className="icon-btn"
                    title="Edit budget category"
                    onClick={() => openEdit(alloc)}
                  >
                    <Edit3 size={17} />
                  </button>
                </div>
                <div className="progress-track">
                  <div
                    className={`progress-fill ${isOver ? 'over' : ''}`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Edit Allocation Modal */}
      {editing && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Edit {editing.category}
            </h3>
            <div className="field">
              <label>Allocated budget (USD)</label>
              <input
                className="input"
                type="number"
                value={allocInput}
                onChange={(e) => setAllocInput(e.target.value)}
              />
            </div>
            <div className="field">
              <label>Committed / spent amount (USD)</label>
              <input
                className="input"
                type="number"
                value={spentInput}
                onChange={(e) => setSpentInput(e.target.value)}
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 13.5 }}>
              <span>Lock amount during AI rebalance</span>
              <input
                type="checkbox"
                checked={lockedInput}
                onChange={(e) => setLockedInput(e.target.checked)}
                style={{ width: 18, height: 18, accentColor: 'var(--primary)' }}
              />
            </label>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 6 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onUpdateAllocation(
                    editing.id,
                    Number(allocInput) || editing.allocatedAmount,
                    Number(spentInput) || 0,
                    lockedInput
                  );
                  setEditing(null);
                }}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function MetricPill({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ background: 'rgba(255,255,255,0.14)', borderRadius: 16, padding: '12px 14px' }}>
      <div style={{ fontSize: 11.5, opacity: 0.82 }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, marginTop: 2 }}>{value}</div>
    </div>
  );
}

export function StatusBadge({ status }: { status: ServiceRequestStatus }) {
  const map: Record<ServiceRequestStatus, { label: string; cls: string }> = {
    ACCEPTED: { label: 'Accepted', cls: 'badge-secondary' },
    COUNTERED: { label: 'Counteroffer', cls: 'badge-tertiary' },
    COMPLETED: { label: 'Completed', cls: 'badge-secondary' },
    REJECTED: { label: 'Declined', cls: 'badge-error' },
    CANCELLED: { label: 'Cancelled', cls: 'badge-muted' },
    PENDING: { label: 'Pending', cls: 'badge-primary' }
  };
  const info = map[status];
  return <span className={`badge ${info.cls}`}>{info.label}</span>;
}
