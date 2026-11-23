import {
  Home,
  LogOut,
  Map,
  Moon,
  PartyPopper,
  RotateCcw,
  Sparkles,
  Store,
  Sun
} from 'lucide-react';
import { AiBudgetChatScreen } from './components/AiBudgetChatScreen';
import { AuthScreen, NupciaCrest } from './components/AuthScreen';
import { CoupleHomeScreen } from './components/CoupleHomeScreen';
import { EventHubScreen } from './components/EventHubScreen';
import { GeospatialMapScreen } from './components/GeospatialMapScreen';
import { OnboardingScreen } from './components/OnboardingScreen';
import { VendorPortalScreen } from './components/VendorPortalScreen';
import { useNupciaStore } from './state/useNupciaStore';

const COUPLE_TABS = [
  { label: 'Home', icon: Home },
  { label: 'AI Concierge', icon: Sparkles },
  { label: 'Utah Map', icon: Map },
  { label: 'Event Hub', icon: PartyPopper }
];

export default function App() {
  const store = useNupciaStore();

  // 1. Welcome / Auth Screen (with prominent Demo button)
  if (!store.profile) {
    return (
      <>
        <AuthScreen
          onEnterCoupleDemo={() => store.enterDemoMode('COUPLE')}
          onEnterVendorDemo={() => store.enterDemoMode('VENDOR')}
          onStartOnboarding={() => store.startFreshOnboarding()}
        />
        {store.toast && <div className="toast">{store.toast}</div>}
      </>
    );
  }

  // 2. Bridal Onboarding Wizard
  if (store.profile.workspaceMode === 'COUPLE' && !store.profile.onboardingDone) {
    return (
      <>
        <OnboardingScreen
          onSwitchToVendor={() => store.switchWorkspace('VENDOR')}
          onLoadQuickDemo={() => store.enterDemoMode('COUPLE')}
          onComplete={store.completeOnboarding}
        />
        {store.toast && <div className="toast">{store.toast}</div>}
      </>
    );
  }

  // 3. Vendor B2B Portal Shell
  if (store.profile.workspaceMode === 'VENDOR') {
    return (
      <div className="app-shell">
        <aside className="side-rail">
          <div className="rail-brand">
            <NupciaCrest size={42} />
            <div>
              <div className="rail-brand-title">NUPCIA</div>
              <div className="rail-brand-sub">Vendor Portal</div>
            </div>
          </div>

          <button type="button" className="nav-item active">
            <Store size={18} /> Vendor Studio
          </button>
          <button
            type="button"
            className="nav-item"
            onClick={() => store.switchWorkspace('COUPLE')}
          >
            <Home size={18} /> Switch to Couple Mode
          </button>

          <div className="rail-footer">
            <button type="button" className="nav-item" onClick={store.toggleDarkMode}>
              {store.darkMode ? <Sun size={18} /> : <Moon size={18} />}
              {store.darkMode ? 'Light Atelier' : 'Candlelit Dark'}
            </button>
            <button
              type="button"
              className="nav-item"
              onClick={() => store.enterDemoMode('VENDOR')}
            >
              <RotateCcw size={18} /> Reset Demo Data
            </button>
            <button type="button" className="nav-item" onClick={store.signOut}>
              <LogOut size={18} /> Welcome Screen
            </button>
          </div>
        </aside>

        <main className="main-stage">
          {store.profile.isDemo && (
            <DemoRibbon
              mode="VENDOR"
              onSwitchMode={() => store.switchWorkspace('COUPLE')}
              onReset={() => store.enterDemoMode('VENDOR')}
            />
          )}
          <div className="content-container">
            <VendorPortalScreen
              vendorProfile={store.myVendorProfile}
              catalogItems={store.myCatalogItems}
              requests={store.requests}
              tasks={store.vendorTasks}
              calendarBlocks={store.vendorCalendarBlocks}
              onAcceptRequest={store.acceptRequestAsVendor}
              onCounterOffer={store.counterOfferAsVendor}
              onTransitionRequest={store.transitionRequestStatus}
              onCycleTask={store.cycleVendorTaskStatus}
              onSaveProfile={store.saveVendorProfile}
              onSaveCatalogItem={store.saveCatalogPackage}
              onAddManualBlock={store.addManualBlock}
              onDeleteCalendarBlock={store.deleteCalendarBlock}
              onSwitchToCouple={() => store.switchWorkspace('COUPLE')}
              onSignOut={store.signOut}
            />
          </div>
        </main>
        {store.toast && <div className="toast">{store.toast}</div>}
      </div>
    );
  }

  // 4. Couple Portal Shell (Sidebar on Desktop, Bottom Bar on Mobile)
  return (
    <div className="app-shell">
      <aside className="side-rail">
        <div className="rail-brand">
          <NupciaCrest size={42} />
          <div>
            <div className="rail-brand-title">NUPCIA</div>
            <div className="rail-brand-sub">Utah County Studio</div>
          </div>
        </div>

        {COUPLE_TABS.map((tab, idx) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.label}
              type="button"
              className={`nav-item ${store.selectedCoupleTab === idx ? 'active' : ''}`}
              onClick={() => store.setSelectedCoupleTab(idx)}
            >
              <Icon size={18} /> {tab.label}
            </button>
          );
        })}

        <div className="rail-footer">
          <button
            type="button"
            className="nav-item"
            onClick={() => store.switchWorkspace('VENDOR')}
          >
            <Store size={18} /> Vendor Portal
          </button>
          <button type="button" className="nav-item" onClick={store.toggleDarkMode}>
            {store.darkMode ? <Sun size={18} /> : <Moon size={18} />}
            {store.darkMode ? 'Light Atelier' : 'Candlelit Dark'}
          </button>
          <button
            type="button"
            className="nav-item"
            onClick={() => store.enterDemoMode('COUPLE')}
          >
            <RotateCcw size={18} /> Reset Demo
          </button>
          <button type="button" className="nav-item" onClick={store.signOut}>
            <LogOut size={18} /> Welcome Screen
          </button>
        </div>
      </aside>

      <main className="main-stage">
        {store.profile.isDemo && (
          <DemoRibbon
            mode="COUPLE"
            onSwitchMode={() => store.switchWorkspace('VENDOR')}
            onReset={() => store.enterDemoMode('COUPLE')}
          />
        )}

        <div className="content-container">
          {store.selectedCoupleTab === 0 && (
            <CoupleHomeScreen
              wedding={store.wedding}
              allocations={store.allocations}
              requests={store.requests}
              readiness={store.readinessReport}
              onNavigateTab={store.setSelectedCoupleTab}
              onUpdateAllocation={store.updateAllocation}
              onRequestAction={store.transitionRequestStatus}
              onSwitchToVendor={() => store.switchWorkspace('VENDOR')}
              onSignOut={store.signOut}
            />
          )}
          {store.selectedCoupleTab === 1 && (
            <AiBudgetChatScreen
              turns={store.chatTurns}
              isThinking={store.isAiThinking}
              onSendPrompt={store.sendAiPrompt}
              onApplyProposal={store.applyProposalFromChat}
              onViewVendorOnMap={store.openVendorOnMap}
              onSendInquiry={store.sendInquiryToVendor}
            />
          )}
          {store.selectedCoupleTab === 2 && (
            <GeospatialMapScreen
              wedding={store.wedding}
              scoredVendors={store.scoredVendors}
              highlightedVendorId={store.highlightedVendorId}
              onClearHighlight={store.clearHighlightedVendor}
              onSendInquiry={store.sendInquiryToVendor}
              onLoadDemoVendors={store.loadDemoVendors}
              onSwitchToVendorPortal={() => store.switchWorkspace('VENDOR')}
            />
          )}
          {store.selectedCoupleTab === 3 && (
            <EventHubScreen
              wedding={store.wedding}
              guests={store.guests}
              registryItems={store.registryItems}
              stationeryDesigns={store.stationeryDesigns}
              onSaveGuest={store.saveGuest}
              onDeleteGuest={store.deleteGuest}
              onAddRegistryItem={store.addRegistryItem}
              onAddContribution={store.addContribution}
              onToggleThankYou={store.toggleThankYou}
              onSaveStationery={store.saveStationery}
              onSaveWebsite={store.saveWeddingWebsite}
              onCopyLink={store.showToast}
            />
          )}
        </div>
      </main>

      {/* Mobile Bottom Navigation */}
      <nav className="mobile-nav">
        {COUPLE_TABS.map((tab, idx) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.label}
              type="button"
              className={`mobile-nav-btn ${store.selectedCoupleTab === idx ? 'active' : ''}`}
              onClick={() => store.setSelectedCoupleTab(idx)}
            >
              <Icon size={19} />
              <span>{tab.label.split(' ')[0]}</span>
            </button>
          );
        })}
      </nav>

      {store.toast && <div className="toast">{store.toast}</div>}
    </div>
  );
}

function DemoRibbon({
  mode,
  onSwitchMode,
  onReset
}: {
  mode: 'COUPLE' | 'VENDOR';
  onSwitchMode: () => void;
  onReset: () => void;
}) {
  return (
    <div className="demo-ribbon">
      <div className="demo-ribbon-left">
        <Sparkles size={15} />
        <span>
          Modo Demo Interactivo — {mode === 'COUPLE' ? 'Emma & Liam (Provo, UT)' : 'Portal Proveedor B2B'}
        </span>
      </div>
      <div className="demo-ribbon-actions">
        <button type="button" className="btn btn-outline btn-sm" onClick={onSwitchMode}>
          {mode === 'COUPLE' ? 'Ver Portal Proveedor' : 'Ver Portal Pareja'}
        </button>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onReset}>
          <RotateCcw size={13} /> Reiniciar Demo
        </button>
      </div>
    </div>
  );
}
