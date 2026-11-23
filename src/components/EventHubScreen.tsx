import { useState } from 'react';
import {
  CheckCircle2,
  Gift,
  Globe,
  Mail,
  Plus,
  Share2,
  Trash2,
  Users,
  Verified
} from 'lucide-react';
import { BudgetRebalancer } from '../domain/budgetRebalancer';
import {
  DefaultWeddingCatalog,
  Guest,
  RegistryItem,
  RegistryItemType,
  RsvpStatus,
  StationeryDesign,
  StationeryProductType,
  Wedding
} from '../domain/models';

interface EventHubScreenProps {
  wedding: Wedding | null;
  guests: Guest[];
  registryItems: RegistryItem[];
  stationeryDesigns: StationeryDesign[];
  onSaveGuest: (guest: {
    id?: string;
    householdName: string;
    fullName: string;
    partySize: number;
    email: string;
    phone: string;
    postalAddress: string;
    rsvpStatus: RsvpStatus;
    mealChoice: string;
    dietaryNotes: string;
    tableNumber: number;
  }) => void;
  onDeleteGuest: (id: string) => void;
  onAddRegistryItem: (type: RegistryItemType, title: string, desc: string, target: number) => void;
  onAddContribution: (itemId: string, contributorName: string, amount: number) => void;
  onToggleThankYou: (itemId: string, sent: boolean) => void;
  onSaveStationery: (design: StationeryDesign) => void;
  onSaveWebsite: (params: {
    selectedSuiteId: string;
    websiteHeadline: string;
    websiteStory: string;
    websiteVenueName: string;
    websiteSchedule: string;
    websiteProtected: boolean;
    slug: string;
  }) => void;
  onCopyLink: (msg: string) => void;
}

export function EventHubScreen(props: EventHubScreenProps) {
  const [subTab, setSubTab] = useState(0);

  return (
    <div>
      <div className="subtabs">
        <button type="button" className={`subtab ${subTab === 0 ? 'active' : ''}`} onClick={() => setSubTab(0)}>
          <Users size={16} /> Guests &amp; Seating
        </button>
        <button type="button" className={`subtab ${subTab === 1 ? 'active' : ''}`} onClick={() => setSubTab(1)}>
          <Gift size={16} /> Registry &amp; Funds
        </button>
        <button type="button" className={`subtab ${subTab === 2 ? 'active' : ''}`} onClick={() => setSubTab(2)}>
          <Mail size={16} /> Stationery Studio
        </button>
        <button type="button" className={`subtab ${subTab === 3 ? 'active' : ''}`} onClick={() => setSubTab(3)}>
          <Globe size={16} /> Wedding Website
        </button>
      </div>

      {subTab === 0 && (
        <GuestsTab
          wedding={props.wedding}
          guests={props.guests}
          onSaveGuest={props.onSaveGuest}
          onDeleteGuest={props.onDeleteGuest}
          onCopyLink={props.onCopyLink}
        />
      )}
      {subTab === 1 && (
        <RegistryTab
          items={props.registryItems}
          onAddItem={props.onAddRegistryItem}
          onAddContribution={props.onAddContribution}
          onToggleThankYou={props.onToggleThankYou}
        />
      )}
      {subTab === 2 && (
        <StationeryTab
          wedding={props.wedding}
          guests={props.guests}
          designs={props.stationeryDesigns}
          onSaveDesign={props.onSaveStationery}
        />
      )}
      {subTab === 3 && (
        <WebsiteTab wedding={props.wedding} onSaveWebsite={props.onSaveWebsite} />
      )}
    </div>
  );
}

/* --- Sub-tab 1: Guests & Seating Chart --- */
function GuestsTab({
  wedding,
  guests,
  onSaveGuest,
  onDeleteGuest,
  onCopyLink
}: {
  wedding: Wedding | null;
  guests: Guest[];
  onSaveGuest: EventHubScreenProps['onSaveGuest'];
  onDeleteGuest: (id: string) => void;
  onCopyLink: (msg: string) => void;
}) {
  const [showSeating, setShowSeating] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<Guest | null>(null);

  const [household, setHousehold] = useState('');
  const [fullName, setFullName] = useState('');
  const [partySize, setPartySize] = useState('2');
  const [tableNum, setTableNum] = useState('1');
  const [address, setAddress] = useState('');
  const [meal, setMeal] = useState('Main Course');
  const [diet, setDiet] = useState('');
  const [email, setEmail] = useState('');

  const openEditor = (guest?: Guest) => {
    setEditing(guest ?? null);
    setHousehold(guest?.householdName ?? '');
    setFullName(guest?.fullName ?? '');
    setPartySize(String(guest?.partySize ?? 2));
    setTableNum(String(guest?.tableNumber ?? 1));
    setAddress(guest?.postalAddress ?? '');
    setMeal(guest?.mealChoice ?? 'Main Course');
    setDiet(guest?.dietaryNotes ?? '');
    setEmail(guest?.email ?? '');
    setEditorOpen(true);
  };

  const confirmedTotal = guests
    .filter((g) => g.rsvpStatus === 'CONFIRMED')
    .reduce((acc, g) => acc + g.partySize, 0);
  const pendingTotal = guests
    .filter((g) => g.rsvpStatus === 'PENDING')
    .reduce((acc, g) => acc + g.partySize, 0);
  const withAddress = guests.filter((g) => g.postalAddress.trim()).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div className="grid-3">
        <div className="card" style={{ textAlign: 'center', background: 'var(--secondary-container)', color: 'var(--on-secondary-container)' }}>
          <div style={{ fontSize: 28, fontWeight: 700 }}>{confirmedTotal}</div>
          <div style={{ fontSize: 12.5, fontWeight: 600 }}>Confirmed Seats</div>
        </div>
        <div className="card" style={{ textAlign: 'center', background: 'var(--tertiary-container)', color: 'var(--on-tertiary-container)' }}>
          <div style={{ fontSize: 28, fontWeight: 700 }}>{pendingTotal}</div>
          <div style={{ fontSize: 12.5, fontWeight: 600 }}>Pending Seats</div>
        </div>
        <div className="card" style={{ textAlign: 'center', background: 'var(--primary-container)', color: 'var(--on-primary-container)' }}>
          <div style={{ fontSize: 28, fontWeight: 700 }}>
            {withAddress}/{guests.length}
          </div>
          <div style={{ fontSize: 12.5, fontWeight: 600 }}>Mailing Addresses</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <button type="button" className="btn btn-primary" onClick={() => openEditor()}>
          <Plus size={16} /> Add household
        </button>
        <button type="button" className="btn btn-tonal" onClick={() => setShowSeating((s) => !s)}>
          {showSeating ? 'View RSVP List' : 'Seating Chart'}
        </button>
      </div>

      {showSeating ? (
        <div className="grid-2">
          {[0, 1, 2, 3, 4].map((tNum) => {
            const seated = guests.filter((g) => g.rsvpStatus !== 'DECLINED' && g.tableNumber === tNum);
            const seatCount = seated.reduce((a, g) => a + g.partySize, 0);
            if (tNum > 2 && seated.length === 0) return null;
            return (
              <div key={tNum} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong style={{ color: 'var(--primary)' }}>
                    {tNum === 0 ? 'Unassigned Table' : `Table #${tNum}`}
                  </strong>
                  <span
                    className={`badge ${seatCount > 8 && tNum > 0 ? 'badge-error' : 'badge-secondary'}`}
                  >
                    {tNum === 0 ? `${seatCount} guests` : `${seatCount} / 8 seats`}
                  </span>
                </div>
                {seated.length === 0 ? (
                  <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No guests assigned here.</p>
                ) : (
                  seated.map((g) => (
                    <div
                      key={g.id}
                      onClick={() => openEditor(g)}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: 13,
                        padding: '6px 8px',
                        borderRadius: 10,
                        background: 'var(--bg)',
                        cursor: 'pointer'
                      }}
                    >
                      <span>
                        {g.fullName} ({g.partySize}) • {g.mealChoice}
                      </span>
                      {g.dietaryNotes && (
                        <span style={{ color: 'var(--tertiary)', fontWeight: 600 }}>
                          {g.dietaryNotes}
                        </span>
                      )}
                    </div>
                  ))
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="grid-2">
          {guests.map((guest) => (
            <div key={guest.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                <div style={{ cursor: 'pointer' }} onClick={() => openEditor(guest)}>
                  <div style={{ fontWeight: 700, fontSize: 15 }}>
                    {guest.householdName} — {guest.fullName}
                  </div>
                  <div style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>
                    {guest.partySize} seats • Table {guest.tableNumber > 0 ? `#${guest.tableNumber}` : 'Unassigned'} •{' '}
                    {guest.mealChoice}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 2 }}>
                  <button
                    type="button"
                    className="icon-btn"
                    title="Copy RSVP link"
                    onClick={() => {
                      const link = `https://nupcia-amber.vercel.app/${wedding?.slug ?? 'our-wedding'}/rsvp?token=${guest.inviteToken}`;
                      navigator.clipboard?.writeText(link).catch(() => {});
                      onCopyLink(`RSVP link copied for ${guest.fullName}`);
                    }}
                  >
                    <Share2 size={16} />
                  </button>
                  <button
                    type="button"
                    className="icon-btn"
                    title="Delete guest"
                    onClick={() => onDeleteGuest(guest.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              {guest.postalAddress && (
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  Address: {guest.postalAddress}
                </div>
              )}

              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                {(['CONFIRMED', 'PENDING', 'DECLINED'] as RsvpStatus[]).map((st) => (
                  <button
                    key={st}
                    type="button"
                    className={`chip ${guest.rsvpStatus === st ? 'selected' : ''}`}
                    style={{ fontSize: 11.5, padding: '5px 11px' }}
                    onClick={() => onSaveGuest({ ...guest, rsvpStatus: st })}
                  >
                    {st === 'CONFIRMED' ? 'Confirmed' : st === 'PENDING' ? 'Pending' : 'Declined'}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {editorOpen && (
        <div className="modal-backdrop" onClick={() => setEditorOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              {editing ? 'Edit Guest' : 'New Household / Guest'}
            </h3>
            <div className="field">
              <label>Family / Household</label>
              <input className="input" value={household} onChange={(e) => setHousehold(e.target.value)} />
            </div>
            <div className="field">
              <label>Primary Guest Name</label>
              <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} />
            </div>
            <div className="grid-2">
              <div className="field">
                <label>Seats</label>
                <input className="input" type="number" min={1} max={20} value={partySize} onChange={(e) => setPartySize(e.target.value)} />
              </div>
              <div className="field">
                <label>Table #</label>
                <input className="input" type="number" min={0} max={100} value={tableNum} onChange={(e) => setTableNum(e.target.value)} />
              </div>
            </div>
            <div className="field">
              <label>Mailing Address (for envelopes)</label>
              <input className="input" value={address} onChange={(e) => setAddress(e.target.value)} />
            </div>
            <div className="grid-2">
              <div className="field">
                <label>Meal Choice</label>
                <input className="input" value={meal} onChange={(e) => setMeal(e.target.value)} />
              </div>
              <div className="field">
                <label>Dietary Notes</label>
                <input className="input" value={diet} onChange={(e) => setDiet(e.target.value)} />
              </div>
            </div>
            <div className="field">
              <label>Email</label>
              <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setEditorOpen(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onSaveGuest({
                    id: editing?.id,
                    householdName: household,
                    fullName,
                    partySize: Math.max(1, Number(partySize) || 1),
                    email,
                    phone: editing?.phone ?? '',
                    postalAddress: address,
                    rsvpStatus: editing?.rsvpStatus ?? 'PENDING',
                    mealChoice: meal,
                    dietaryNotes: diet,
                    tableNumber: Math.max(0, Number(tableNum) || 0)
                  });
                  setEditorOpen(false);
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

/* --- Sub-tab 2: Registry & Cash Funds --- */
function RegistryTab({
  items,
  onAddItem,
  onAddContribution,
  onToggleThankYou
}: {
  items: RegistryItem[];
  onAddItem: EventHubScreenProps['onAddRegistryItem'];
  onAddContribution: EventHubScreenProps['onAddContribution'];
  onToggleThankYou: EventHubScreenProps['onToggleThankYou'];
}) {
  const [showCreate, setShowCreate] = useState(false);
  const [isCashFund, setIsCashFund] = useState(true);
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [target, setTarget] = useState('1500');

  const [contribItem, setContribItem] = useState<RegistryItem | null>(null);
  const [guestName, setGuestName] = useState('');
  const [amount, setAmount] = useState('200');

  const totalCollected = items.reduce((a, i) => a + i.collectedAmount, 0);
  const totalTarget = items.reduce((a, i) => a + i.targetAmount, 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div
        className="card"
        style={{
          background: 'var(--tertiary-container)',
          color: 'var(--on-tertiary-container)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12
        }}
      >
        <div>
          <div style={{ fontSize: 12.5, fontWeight: 700 }}>Registry &amp; Cash Funds Raised</div>
          <div className="font-serif" style={{ fontSize: 36, fontWeight: 700, color: 'var(--primary)' }}>
            {BudgetRebalancer.formatCurrency(totalCollected)}
          </div>
          <div style={{ fontSize: 13 }}>Total goal: {BudgetRebalancer.formatCurrency(totalTarget)}</div>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setShowCreate(true)}>
          <Plus size={16} /> New fund
        </button>
      </div>

      <div className="grid-2">
        {items.map((item) => {
          const pct = item.targetAmount > 0 ? Math.min(100, (item.collectedAmount / item.targetAmount) * 100) : 0;
          return (
            <div key={item.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 15.5 }}>{item.title}</div>
                  <div style={{ fontSize: 12, color: 'var(--primary)', fontWeight: 600 }}>
                    {item.itemType === 'CASH_FUND' ? 'Cash fund' : 'Physical gift'} •{' '}
                    {item.contributorCount} gifts
                  </div>
                </div>
                <strong style={{ fontSize: 13.5 }}>
                  {BudgetRebalancer.formatCurrency(item.collectedAmount)} /{' '}
                  {BudgetRebalancer.formatCurrency(item.targetAmount)}
                </strong>
              </div>
              {item.description && (
                <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>{item.description}</p>
              )}
              <div className="progress-track">
                <div className="progress-fill" style={{ width: `${pct}%` }} />
              </div>
              {item.lastContributorName && (
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  Latest gift: {item.lastContributorName}
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
                <button
                  type="button"
                  className="btn btn-tonal btn-sm"
                  onClick={() => {
                    setContribItem(item);
                    setGuestName('');
                    setAmount('200');
                  }}
                >
                  Log gift
                </button>
                <button
                  type="button"
                  className={`chip ${item.thankYouSent ? 'selected' : ''}`}
                  style={{ fontSize: 12 }}
                  onClick={() => onToggleThankYou(item.id, !item.thankYouSent)}
                >
                  {item.thankYouSent ? 'Thanked ✓' : 'Send thank-you'}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {showCreate && (
        <div className="modal-backdrop" onClick={() => setShowCreate(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Add to Gift Registry
            </h3>
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="button" className={`chip ${isCashFund ? 'selected' : ''}`} onClick={() => setIsCashFund(true)}>
                Cash fund
              </button>
              <button type="button" className={`chip ${!isCashFund ? 'selected' : ''}`} onClick={() => setIsCashFund(false)}>
                Physical gift
              </button>
            </div>
            <div className="field">
              <label>Title</label>
              <input className="input" placeholder="e.g. Honeymoon Fund" value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="field">
              <label>Description</label>
              <input className="input" value={desc} onChange={(e) => setDesc(e.target.value)} />
            </div>
            <div className="field">
              <label>Goal (USD)</label>
              <input className="input" type="number" value={target} onChange={(e) => setTarget(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setShowCreate(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onAddItem(isCashFund ? 'CASH_FUND' : 'PRODUCT', title, desc, Number(target) || 500);
                  setShowCreate(false);
                  setTitle('');
                  setDesc('');
                }}
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}

      {contribItem && (
        <div className="modal-backdrop" onClick={() => setContribItem(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif" style={{ fontSize: 24, fontWeight: 700 }}>
              Log Gift to {contribItem.title}
            </h3>
            <div className="field">
              <label>Guest name</label>
              <input className="input" value={guestName} onChange={(e) => setGuestName(e.target.value)} />
            </div>
            <div className="field">
              <label>Amount (USD)</label>
              <input className="input" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setContribItem(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onAddContribution(contribItem.id, guestName, Number(amount) || 100);
                  setContribItem(null);
                }}
              >
                Save gift
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* --- Sub-tab 3: Stationery Studio --- */
function StationeryTab({
  wedding,
  guests,
  designs,
  onSaveDesign
}: {
  wedding: Wedding | null;
  guests: Guest[];
  designs: StationeryDesign[];
  onSaveDesign: (d: StationeryDesign) => void;
}) {
  const existing = designs[0];
  const [productType, setProductType] = useState<StationeryProductType>(
    existing?.productType ?? 'INVITATION'
  );
  const [suite, setSuite] = useState(
    DefaultWeddingCatalog.SUITES.find((s) => s.id === (existing?.suiteId ?? wedding?.selectedSuiteId)) ??
      DefaultWeddingCatalog.SUITES[0]
  );
  const [headline, setHeadline] = useState(
    existing?.headline || `${wedding?.brideName ?? 'Emma'} & ${wedding?.partnerName ?? 'Liam'}`
  );
  const [subheadline, setSubheadline] = useState(
    existing?.subheadline || `${wedding?.weddingDate ?? '2027-06-19'} • ${wedding?.city ?? 'Provo, UT'}`
  );
  const [bodyText, setBodyText] = useState(
    existing?.bodyText ||
      'Together with our families, we joyfully invite you to celebrate our wedding in Utah County.'
  );
  const [showBleed, setShowBleed] = useState(true);
  const [guestIdx, setGuestIdx] = useState(0);

  const previewGuest = guests.length > 0 ? guests[guestIdx % guests.length] : null;

  const save = (approved: boolean) => {
    onSaveDesign({
      id: existing?.id ?? 'inv_main',
      suiteId: suite.id,
      productType,
      headline,
      subheadline,
      bodyText,
      bgColorHex: suite.bgHex,
      accentColorHex: suite.accentHex,
      fontStyle: 'EDITORIAL_SERIF',
      bleedValid: true,
      proofApproved: approved
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {(
          [
            ['INVITATION', 'Invitation'],
            ['SAVE_THE_DATE', 'Save the Date'],
            ['MENU', 'Menu'],
            ['ENVELOPE', 'Envelope']
          ] as Array<[StationeryProductType, string]>
        ).map(([code, label]) => (
          <button
            key={code}
            type="button"
            className={`chip ${productType === code ? 'selected' : ''}`}
            onClick={() => setProductType(code)}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Live Print Proof Preview */}
      <div
        style={{
          position: 'relative',
          minHeight: 300,
          borderRadius: 20,
          background: suite.bgHex,
          border: '1px solid var(--outline)',
          padding: 40,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          boxShadow: 'var(--shadow-md)'
        }}
      >
        {/* Ornamental Frame */}
        <div
          style={{
            position: 'absolute',
            inset: 18,
            border: `1.5px solid ${suite.accentHex}`,
            opacity: 0.55,
            borderRadius: 10,
            pointerEvents: 'none'
          }}
        />
        {/* Bleed Safe-Zone Guide */}
        {showBleed && (
          <div
            style={{
              position: 'absolute',
              inset: 8,
              border: '1.5px dashed rgba(211, 47, 47, 0.65)',
              borderRadius: 14,
              pointerEvents: 'none'
            }}
          />
        )}

        <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.16em', color: suite.accentHex }}>
          {productType === 'SAVE_THE_DATE'
            ? 'SAVE THE DATE'
            : productType === 'MENU'
              ? 'RECEPTION MENU'
              : productType === 'ENVELOPE'
                ? 'MAILING RECIPIENT'
                : 'WEDDING INVITATION'}
        </div>
        <h2
          className="font-serif"
          style={{ fontSize: 36, fontWeight: 700, color: suite.accentHex, marginTop: 10 }}
        >
          {productType === 'ENVELOPE' && previewGuest ? previewGuest.householdName : headline}
        </h2>
        <div style={{ fontSize: 14, fontWeight: 600, color: '#3E3436', marginTop: 6 }}>
          {productType === 'ENVELOPE' && previewGuest ? previewGuest.fullName : subheadline}
        </div>
        <p style={{ fontSize: 13.5, color: '#4F4345', maxWidth: 420, marginTop: 10, lineHeight: 1.5 }}>
          {productType === 'ENVELOPE' && previewGuest
            ? previewGuest.postalAddress || 'Mailing address pending collection'
            : bodyText}
        </p>
        {previewGuest && productType !== 'ENVELOPE' && (
          <div style={{ fontSize: 11.5, fontWeight: 600, color: suite.accentHex, marginTop: 14 }}>
            Personalized for: {previewGuest.householdName} ({previewGuest.partySize} seats)
          </div>
        )}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13.5 }}>
          <input
            type="checkbox"
            checked={showBleed}
            onChange={(e) => setShowBleed(e.target.checked)}
            style={{ width: 17, height: 17, accentColor: 'var(--primary)' }}
          />
          Show print bleed safe-zone guide (3mm)
        </label>
        {guests.length > 0 && (
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => setGuestIdx((i) => i + 1)}
          >
            Next household ({(guestIdx % guests.length) + 1}/{guests.length})
          </button>
        )}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {DefaultWeddingCatalog.SUITES.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`chip ${suite.id === s.id ? 'selected' : ''}`}
              onClick={() => setSuite(s)}
            >
              {s.name}
            </button>
          ))}
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Main headline</label>
            <input className="input" value={headline} onChange={(e) => setHeadline(e.target.value)} />
          </div>
          <div className="field">
            <label>Date &amp; location</label>
            <input className="input" value={subheadline} onChange={(e) => setSubheadline(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Invitation message</label>
          <textarea className="textarea" value={bodyText} onChange={(e) => setBodyText(e.target.value)} />
        </div>

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-outline" onClick={() => save(false)}>
            Save Draft
          </button>
          <button type="button" className="btn btn-primary" onClick={() => save(true)}>
            <Verified size={16} /> Approve Print Proof
          </button>
        </div>
      </div>
    </div>
  );
}

/* --- Sub-tab 4: Wedding Website Builder --- */
function WebsiteTab({
  wedding,
  onSaveWebsite
}: {
  wedding: Wedding | null;
  onSaveWebsite: EventHubScreenProps['onSaveWebsite'];
}) {
  const [suiteId, setSuiteId] = useState(wedding?.selectedSuiteId ?? 'botanical_ivory');
  const [headline, setHeadline] = useState(wedding?.websiteHeadline ?? 'Emma & Liam');
  const [story, setStory] = useState(wedding?.websiteStory ?? '');
  const [venueName, setVenueName] = useState(wedding?.websiteVenueName ?? '');
  const [schedule, setSchedule] = useState(wedding?.websiteSchedule ?? '');
  const [slug, setSlug] = useState(wedding?.slug ?? 'emma-and-liam');
  const [protectedSite, setProtectedSite] = useState(wedding?.websiteProtected ?? false);

  const suite =
    DefaultWeddingCatalog.SUITES.find((s) => s.id === suiteId) ?? DefaultWeddingCatalog.SUITES[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Live Mini Preview */}
      <div
        style={{
          background: suite.bgHex,
          border: `1.5px solid ${suite.accentHex}`,
          borderRadius: 22,
          padding: 32,
          textAlign: 'center',
          color: '#23191B'
        }}
      >
        <div style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.12em', color: suite.accentHex }}>
          nupcia-amber.vercel.app/{slug || 'our-wedding'} {protectedSite ? '🔒' : ''}
        </div>
        <h2 className="font-serif" style={{ fontSize: 38, fontWeight: 700, color: suite.accentHex, marginTop: 8 }}>
          {headline}
        </h2>
        <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>
          {wedding?.weddingDate ?? '2027-06-19'} • {venueName}
        </div>
        <p style={{ maxWidth: 520, margin: '12px auto 0', fontSize: 14, lineHeight: 1.55, color: '#4F4345' }}>
          {story}
        </p>
        <div
          style={{
            marginTop: 16,
            display: 'inline-block',
            padding: '8px 16px',
            borderRadius: 999,
            background: suite.accentHex,
            color: '#FFFDF9',
            fontSize: 12.5,
            fontWeight: 600
          }}
        >
          {schedule}
        </div>
      </div>

      {/* Editor */}
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {DefaultWeddingCatalog.SUITES.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`chip ${suiteId === s.id ? 'selected' : ''}`}
              onClick={() => setSuiteId(s.id)}
            >
              {s.name}
            </button>
          ))}
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Website headline</label>
            <input className="input" value={headline} onChange={(e) => setHeadline(e.target.value)} />
          </div>
          <div className="field">
            <label>Custom URL slug</label>
            <input className="input" value={slug} onChange={(e) => setSlug(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Venue name</label>
          <input className="input" value={venueName} onChange={(e) => setVenueName(e.target.value)} />
        </div>
        <div className="field">
          <label>Couple story</label>
          <textarea className="textarea" value={story} onChange={(e) => setStory(e.target.value)} />
        </div>
        <div className="field">
          <label>Day-of schedule</label>
          <input className="input" value={schedule} onChange={(e) => setSchedule(e.target.value)} />
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13.5 }}>
          <input
            type="checkbox"
            checked={protectedSite}
            onChange={(e) => setProtectedSite(e.target.checked)}
            style={{ width: 17, height: 17, accentColor: 'var(--primary)' }}
          />
          Protect wedding website with guest passcode
        </label>

        <div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() =>
              onSaveWebsite({
                selectedSuiteId: suiteId,
                websiteHeadline: headline,
                websiteStory: story,
                websiteVenueName: venueName,
                websiteSchedule: schedule,
                websiteProtected: protectedSite,
                slug
              })
            }
          >
            <CheckCircle2 size={16} /> Publish Wedding Website
          </button>
        </div>
      </div>
    </div>
  );
}
