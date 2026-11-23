import { useState } from 'react';
import {
  BarChart3,
  Calendar,
  CheckCircle2,
  ClipboardCheck,
  Heart,
  LogOut,
  Package,
  Plus,
  Trash2
} from 'lucide-react';
import { BudgetRebalancer } from '../domain/budgetRebalancer';
import {
  DefaultWeddingCatalog,
  ServiceRequest,
  ServiceRequestStatus,
  VendorCalendarBlock,
  VendorCatalogItem,
  VendorProfile,
  VendorTask
} from '../domain/models';
import { StatusBadge } from './CoupleHomeScreen';

interface VendorPortalScreenProps {
  vendorProfile: VendorProfile | null;
  catalogItems: VendorCatalogItem[];
  requests: ServiceRequest[];
  tasks: VendorTask[];
  calendarBlocks: VendorCalendarBlock[];
  onAcceptRequest: (req: ServiceRequest) => void;
  onCounterOffer: (reqId: string, amount: number, notes: string) => void;
  onTransitionRequest: (req: ServiceRequest, status: ServiceRequestStatus) => void;
  onCycleTask: (task: VendorTask) => void;
  onSaveProfile: (
    profile: Omit<VendorProfile, 'id' | 'ownerId' | 'ratingAvg' | 'completedEventsCount'>
  ) => void;
  onSaveCatalogItem: (item: {
    id?: string;
    title: string;
    description: string;
    basePrice: number;
    pricingUnit: string;
    active: boolean;
  }) => void;
  onAddManualBlock: (title: string, date: string) => void;
  onDeleteCalendarBlock: (blockId: string) => void;
  onSwitchToCouple: () => void;
  onSignOut: () => void;
}

export function VendorPortalScreen(props: VendorPortalScreenProps) {
  const [tab, setTab] = useState(0);
  const pendingCount = props.requests.filter((r) => r.status === 'PENDING').length;
  const activeTaskCount = props.tasks.filter(
    (t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED'
  ).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* Vendor Header Banner */}
      <div className="hero-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <span style={{ fontSize: 12, fontWeight: 700, opacity: 0.85, letterSpacing: '0.08em' }}>
            B2B VENDOR PORTAL
          </span>
          <h1 className="font-serif" style={{ fontSize: 34, fontWeight: 700, marginTop: 4 }}>
            {props.vendorProfile?.businessName || 'Wasatch Bridal Studio'}
          </h1>
          <p style={{ fontSize: 13.5, opacity: 0.88, marginTop: 4 }}>
            {pendingCount} pending inquiries • {activeTaskCount} active tasks
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="button"
            className="btn btn-sm"
            style={{ background: 'rgba(255,255,255,0.18)', color: '#FFFBF8' }}
            onClick={props.onSwitchToCouple}
          >
            <Heart size={15} /> Switch to Couple Mode
          </button>
          <button type="button" className="icon-btn" onClick={props.onSignOut} title="Exit">
            <LogOut size={19} />
          </button>
        </div>
      </div>

      <div className="subtabs">
        <button type="button" className={`subtab ${tab === 0 ? 'active' : ''}`} onClick={() => setTab(0)}>
          <ClipboardCheck size={16} /> Inquiries &amp; Tasks
        </button>
        <button type="button" className={`subtab ${tab === 1 ? 'active' : ''}`} onClick={() => setTab(1)}>
          <Package size={16} /> Catalog &amp; Profile
        </button>
        <button type="button" className={`subtab ${tab === 2 ? 'active' : ''}`} onClick={() => setTab(2)}>
          <Calendar size={16} /> Unified Calendar
        </button>
        <button type="button" className={`subtab ${tab === 3 ? 'active' : ''}`} onClick={() => setTab(3)}>
          <BarChart3 size={16} /> Analytics
        </button>
      </div>

      {tab === 0 && (
        <InquiriesAndTasksTab
          requests={props.requests}
          tasks={props.tasks}
          onAccept={props.onAcceptRequest}
          onCounterOffer={props.onCounterOffer}
          onTransition={props.onTransitionRequest}
          onCycleTask={props.onCycleTask}
        />
      )}
      {tab === 1 && (
        <CatalogTab
          profile={props.vendorProfile}
          catalogItems={props.catalogItems}
          onSaveProfile={props.onSaveProfile}
          onSaveCatalogItem={props.onSaveCatalogItem}
        />
      )}
      {tab === 2 && (
        <CalendarTab
          blocks={props.calendarBlocks}
          tasks={props.tasks}
          onAddBlock={props.onAddManualBlock}
          onDeleteBlock={props.onDeleteCalendarBlock}
        />
      )}
      {tab === 3 && (
        <AnalyticsTab
          profile={props.vendorProfile}
          requests={props.requests}
          tasks={props.tasks}
        />
      )}
    </div>
  );
}

function InquiriesAndTasksTab({
  requests,
  tasks,
  onAccept,
  onCounterOffer,
  onTransition,
  onCycleTask
}: {
  requests: ServiceRequest[];
  tasks: VendorTask[];
  onAccept: (r: ServiceRequest) => void;
  onCounterOffer: (id: string, amount: number, notes: string) => void;
  onTransition: (r: ServiceRequest, st: ServiceRequestStatus) => void;
  onCycleTask: (t: VendorTask) => void;
}) {
  const [counterTarget, setCounterTarget] = useState<ServiceRequest | null>(null);
  const [counterAmount, setCounterAmount] = useState('');
  const [counterNotes, setCounterNotes] = useState('');
  const activeTasks = tasks.filter((t) => t.status !== 'CANCELLED');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div>
        <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
          Incoming Inquiries ({requests.length})
        </h2>
        <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
          Accepting an inquiry automatically blocks the event date on your calendar and creates 4
          operational milestone tasks.
        </p>
      </div>

      <div className="grid-2">
        {requests.map((req) => (
          <div key={req.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 15.5 }}>{req.coupleName}</div>
                <div style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>
                  {req.catalogItemTitle} • Date: {req.eventDate}
                </div>
              </div>
              <StatusBadge status={req.status} />
            </div>

            <div style={{ fontSize: 13.5, fontWeight: 700, color: 'var(--primary)' }}>
              Proposed: {BudgetRebalancer.formatCurrency(req.proposedBudget)}
              {req.counterOfferBudget > 0 &&
                ` • Counteroffer: ${BudgetRebalancer.formatCurrency(req.counterOfferBudget)}`}
            </div>
            {req.notes && <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>{req.notes}</p>}

            {(req.status === 'PENDING' || req.status === 'COUNTERED') && (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 4 }}>
                <button type="button" className="btn btn-primary btn-sm" onClick={() => onAccept(req)}>
                  <CheckCircle2 size={15} /> Accept
                </button>
                {req.status === 'PENDING' && (
                  <button
                    type="button"
                    className="btn btn-tonal btn-sm"
                    onClick={() => {
                      setCounterTarget(req);
                      setCounterAmount(String(Math.round(req.proposedBudget)));
                      setCounterNotes('We can include extended event coverage for this adjustment.');
                    }}
                  >
                    Counteroffer
                  </button>
                )}
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => onTransition(req, 'REJECTED')}>
                  Decline
                </button>
              </div>
            )}

            {req.status === 'ACCEPTED' && (
              <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                <button type="button" className="btn btn-primary btn-sm" onClick={() => onTransition(req, 'COMPLETED')}>
                  Mark Event Completed
                </button>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => onTransition(req, 'CANCELLED')}>
                  Cancel
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      <div>
        <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
          Operational Task Board ({activeTasks.length})
        </h2>
        <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
          Click any task card to advance its status: Pending → In Progress → Done.
        </p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {activeTasks.map((task) => (
          <div
            key={task.id}
            className="card"
            onClick={() => onCycleTask(task)}
            style={{
              cursor: 'pointer',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 12
            }}
          >
            <div>
              <div style={{ fontWeight: 700, fontSize: 14.5 }}>{task.title}</div>
              <div style={{ fontSize: 12, color: 'var(--primary)', fontWeight: 600 }}>
                {task.coupleName} • Due: {task.dueDate}
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 2 }}>
                {task.description}
              </div>
            </div>
            <span
              className={`badge ${
                task.status === 'COMPLETED'
                  ? 'badge-secondary'
                  : task.status === 'IN_PROGRESS'
                    ? 'badge-tertiary'
                    : 'badge-muted'
              }`}
            >
              {task.status === 'COMPLETED'
                ? 'Done ✓'
                : task.status === 'IN_PROGRESS'
                  ? 'In Progress'
                  : 'Pending'}
            </span>
          </div>
        ))}
      </div>

      {counterTarget && (
        <div className="modal-backdrop" onClick={() => setCounterTarget(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Counteroffer for {counterTarget.coupleName}
            </h3>
            <div className="field">
              <label>Suggested amount (USD)</label>
              <input className="input" type="number" value={counterAmount} onChange={(e) => setCounterAmount(e.target.value)} />
            </div>
            <div className="field">
              <label>Message</label>
              <textarea className="textarea" value={counterNotes} onChange={(e) => setCounterNotes(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setCounterTarget(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onCounterOffer(
                    counterTarget.id,
                    Number(counterAmount) || counterTarget.proposedBudget,
                    counterNotes
                  );
                  setCounterTarget(null);
                }}
              >
                Send
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function CatalogTab({
  profile,
  catalogItems,
  onSaveProfile,
  onSaveCatalogItem
}: {
  profile: VendorProfile | null;
  catalogItems: VendorCatalogItem[];
  onSaveProfile: VendorPortalScreenProps['onSaveProfile'];
  onSaveCatalogItem: VendorPortalScreenProps['onSaveCatalogItem'];
}) {
  const [businessName, setBusinessName] = useState(profile?.businessName ?? 'Wasatch Bridal Studio');
  const [category, setCategory] = useState(profile?.category ?? 'Photography');
  const [description, setDescription] = useState(
    profile?.description ??
      'Editorial Utah County wedding coverage with full digital gallery and heirloom album.'
  );
  const [city, setCity] = useState(
    DefaultWeddingCatalog.CITIES.find((c) => c.name === profile?.city) ??
      DefaultWeddingCatalog.CITIES[0]
  );
  const [minPrice, setMinPrice] = useState(String(Math.round(profile?.minPrice ?? 2200)));
  const [maxPrice, setMaxPrice] = useState(String(Math.round(profile?.maxPrice ?? 5500)));
  const [radiusMi, setRadiusMi] = useState(
    String(Math.round((profile?.coverageRadiusKm ?? 48) * 0.621371))
  );
  const [styles, setStyles] = useState<string[]>(profile?.styleTags ?? ['Editorial', 'Classic']);
  const [published, setPublished] = useState(profile?.published ?? true);

  const [showPkgModal, setShowPkgModal] = useState(false);
  const [pkgTitle, setPkgTitle] = useState('');
  const [pkgDesc, setPkgDesc] = useState('');
  const [pkgPrice, setPkgPrice] = useState('3200');
  const [pkgUnit, setPkgUnit] = useState('Per event');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
          Public Marketplace Profile
        </h2>
        <div className="field">
          <label>Business name</label>
          <input className="input" value={businessName} onChange={(e) => setBusinessName(e.target.value)} />
        </div>
        <div className="field">
          <label>Service description</label>
          <textarea className="textarea" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="field">
          <label>Category</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {DefaultWeddingCatalog.CATEGORIES.map((cat) => (
              <button key={cat} type="button" className={`chip ${category === cat ? 'selected' : ''}`} onClick={() => setCategory(cat)}>
                {cat}
              </button>
            ))}
          </div>
        </div>
        <div className="field">
          <label>Utah County Base Location</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {DefaultWeddingCatalog.CITIES.map((c) => (
              <button key={c.name} type="button" className={`chip ${city.name === c.name ? 'selected' : ''}`} onClick={() => setCity(c)}>
                {c.name}
              </button>
            ))}
          </div>
        </div>
        <div className="grid-3">
          <div className="field">
            <label>Min price (USD)</label>
            <input className="input" type="number" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} />
          </div>
          <div className="field">
            <label>Max price (USD)</label>
            <input className="input" type="number" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} />
          </div>
          <div className="field">
            <label>Radius (miles)</label>
            <input className="input" type="number" value={radiusMi} onChange={(e) => setRadiusMi(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Matching Wedding Styles</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {DefaultWeddingCatalog.STYLE_TAGS.map((tag) => {
              const active = styles.includes(tag);
              return (
                <button
                  key={tag}
                  type="button"
                  className={`chip ${active ? 'selected' : ''}`}
                  onClick={() =>
                    setStyles((prev) =>
                      prev.includes(tag)
                        ? prev.length > 1
                          ? prev.filter((t) => t !== tag)
                          : prev
                        : prev.length < 4
                          ? [...prev, tag]
                          : prev
                    )
                  }
                >
                  {tag}
                </button>
              );
            })}
          </div>
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13.5 }}>
          <input
            type="checkbox"
            checked={published}
            onChange={(e) => setPublished(e.target.checked)}
            style={{ width: 17, height: 17, accentColor: 'var(--primary)' }}
          />
          Visible on Utah County map &amp; AI Assistant
        </label>
        <div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() =>
              onSaveProfile({
                businessName,
                category,
                description,
                city: city.name,
                lat: city.lat,
                lng: city.lng,
                coverageRadiusKm: Math.max(5, (Number(radiusMi) || 30) * 1.60934),
                minPrice: Number(minPrice) || 2200,
                maxPrice: Number(maxPrice) || 5500,
                styleTags: styles,
                published
              })
            }
          >
            Save &amp; publish profile
          </button>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
          Service Packages ({catalogItems.length})
        </h2>
        <button type="button" className="btn btn-tonal btn-sm" onClick={() => setShowPkgModal(true)}>
          <Plus size={15} /> New package
        </button>
      </div>

      <div className="grid-2">
        {catalogItems.map((item) => (
          <div key={item.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
            <div>
              <div style={{ fontWeight: 700, fontSize: 15 }}>{item.title}</div>
              <div style={{ fontSize: 13, color: 'var(--primary)', fontWeight: 600 }}>
                {BudgetRebalancer.formatCurrency(item.basePrice)} • {item.pricingUnit}
              </div>
              <p style={{ fontSize: 12.5, color: 'var(--text-muted)', marginTop: 4 }}>{item.description}</p>
            </div>
            <input
              type="checkbox"
              checked={item.active}
              onChange={(e) => onSaveCatalogItem({ ...item, active: e.target.checked })}
              style={{ width: 19, height: 19, accentColor: 'var(--primary)' }}
            />
          </div>
        ))}
      </div>

      {showPkgModal && (
        <div className="modal-backdrop" onClick={() => setShowPkgModal(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              New Catalog Package
            </h3>
            <div className="field">
              <label>Package title</label>
              <input className="input" value={pkgTitle} onChange={(e) => setPkgTitle(e.target.value)} />
            </div>
            <div className="field">
              <label>Includes…</label>
              <textarea className="textarea" value={pkgDesc} onChange={(e) => setPkgDesc(e.target.value)} />
            </div>
            <div className="grid-2">
              <div className="field">
                <label>Base price (USD)</label>
                <input className="input" type="number" value={pkgPrice} onChange={(e) => setPkgPrice(e.target.value)} />
              </div>
              <div className="field">
                <label>Unit</label>
                <input className="input" value={pkgUnit} onChange={(e) => setPkgUnit(e.target.value)} />
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setShowPkgModal(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onSaveCatalogItem({
                    title: pkgTitle,
                    description: pkgDesc,
                    basePrice: Number(pkgPrice) || 1800,
                    pricingUnit: pkgUnit,
                    active: true
                  });
                  setShowPkgModal(false);
                  setPkgTitle('');
                  setPkgDesc('');
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

function CalendarTab({
  blocks,
  tasks,
  onAddBlock,
  onDeleteBlock
}: {
  blocks: VendorCalendarBlock[];
  tasks: VendorTask[];
  onAddBlock: (title: string, date: string) => void;
  onDeleteBlock: (id: string) => void;
}) {
  const [showAdd, setShowAdd] = useState(false);
  const [title, setTitle] = useState('Blocked date (private event)');
  const [date, setDate] = useState('2027-07-10');

  const upcomingTasks = tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
            Unified Calendar
          </h2>
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
            Booked weddings &amp; manual date blocks
          </p>
        </div>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setShowAdd(true)}>
          <Plus size={15} /> Block date
        </button>
      </div>

      <div className="grid-2">
        {blocks.map((b) => (
          <div key={b.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14.5 }}>{b.title}</div>
              <div
                style={{
                  fontSize: 12.5,
                  fontWeight: 600,
                  color: b.blockType === 'BOOKED' ? 'var(--secondary)' : 'var(--tertiary)'
                }}
              >
                {b.startDate} • {b.blockType === 'BOOKED' ? 'Booked wedding' : 'Manual block'}
              </div>
            </div>
            {b.blockType === 'MANUAL_BLOCK' && (
              <button type="button" className="icon-btn" onClick={() => onDeleteBlock(b.id)}>
                <Trash2 size={16} />
              </button>
            )}
          </div>
        ))}
      </div>

      {upcomingTasks.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <h3 style={{ fontWeight: 700, fontSize: 16 }}>Upcoming Task Deadlines</h3>
          {upcomingTasks.map((t) => (
            <div key={t.id} className="card-subtle" style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 13.5 }}>{t.title}</div>
                <div style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>{t.coupleName}</div>
              </div>
              <span style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--primary)' }}>
                {t.dueDate}
              </span>
            </div>
          ))}
        </div>
      )}

      {showAdd && (
        <div className="modal-backdrop" onClick={() => setShowAdd(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Manual Calendar Block
            </h3>
            <div className="field">
              <label>Reason</label>
              <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="field">
              <label>Date</label>
              <input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setShowAdd(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onAddBlock(title, date);
                  setShowAdd(false);
                }}
              >
                Block
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AnalyticsTab({
  profile,
  requests,
  tasks
}: {
  profile: VendorProfile | null;
  requests: ServiceRequest[];
  tasks: VendorTask[];
}) {
  const completedRequests = requests.filter((r) => r.status === 'COMPLETED');
  const acceptedOrCompleted = requests.filter(
    (r) => r.status === 'ACCEPTED' || r.status === 'COMPLETED'
  );
  const conversionRate =
    requests.length > 0 ? Math.round((acceptedOrCompleted.length / requests.length) * 100) : 0;

  const fulfilledRevenue = completedRequests.reduce(
    (acc, r) => acc + (r.counterOfferBudget > 0 ? r.counterOfferBudget : r.proposedBudget),
    0
  );
  const pipelineRevenue = requests
    .filter((r) => r.status === 'ACCEPTED')
    .reduce(
      (acc, r) => acc + (r.counterOfferBudget > 0 ? r.counterOfferBudget : r.proposedBudget),
      0
    );

  const bars = [
    { label: 'Pending', value: requests.filter((r) => r.status === 'PENDING').length, color: 'var(--tertiary)' },
    { label: 'Accepted', value: requests.filter((r) => r.status === 'ACCEPTED').length, color: 'var(--primary)' },
    { label: 'Completed', value: completedRequests.length, color: 'var(--secondary)' },
    { label: 'Tasks Done', value: tasks.filter((t) => t.status === 'COMPLETED').length, color: 'var(--primary)' }
  ];
  const maxVal = Math.max(1, ...bars.map((b) => b.value));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <h2 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
          Completed Event Metrics
        </h2>
        <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
          Counts strictly events marked COMPLETED (Business Rule #3).
        </p>
      </div>

      <div className="grid-3">
        <div className="card" style={{ textAlign: 'center' }}>
          <div className="font-serif" style={{ fontSize: 34, fontWeight: 700, color: 'var(--primary)' }}>
            {completedRequests.length}
          </div>
          <div style={{ fontSize: 12.5, color: 'var(--text-muted)', fontWeight: 600 }}>Completed</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div className="font-serif" style={{ fontSize: 34, fontWeight: 700, color: 'var(--secondary)' }}>
            {conversionRate}%
          </div>
          <div style={{ fontSize: 12.5, color: 'var(--text-muted)', fontWeight: 600 }}>Conversion</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div className="font-serif" style={{ fontSize: 34, fontWeight: 700, color: 'var(--tertiary)' }}>
            {profile?.ratingAvg ?? 4.9} ★
          </div>
          <div style={{ fontSize: 12.5, color: 'var(--text-muted)', fontWeight: 600 }}>Rating</div>
        </div>
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Completed revenue</div>
            <div className="font-serif" style={{ fontSize: 30, fontWeight: 700, color: 'var(--secondary)' }}>
              {BudgetRebalancer.formatCurrency(fulfilledRevenue)}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Booked pipeline</div>
            <div className="font-serif" style={{ fontSize: 30, fontWeight: 700, color: 'var(--primary)' }}>
              {BudgetRebalancer.formatCurrency(pipelineRevenue)}
            </div>
          </div>
        </div>

        {/* SVG Pipeline Bar Chart */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14, alignItems: 'end', height: 150, paddingTop: 12 }}>
          {bars.map((bar) => (
            <div key={bar.label} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, height: '100%', justifyContent: 'flex-end' }}>
              <div
                style={{
                  width: '56%',
                  height: `${Math.max(12, (bar.value / maxVal) * 100)}%`,
                  background: bar.color,
                  borderRadius: '10px 10px 4px 4px',
                  transition: 'height 0.25s ease'
                }}
              />
              <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', textAlign: 'center' }}>
                {bar.label} ({bar.value})
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
