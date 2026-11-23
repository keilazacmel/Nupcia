import { useCallback, useEffect, useMemo, useState } from 'react';
import { ChatTurn, generateConciergeReply } from '../data/aiConcierge';
import { createDemoSnapshot, DEMO_USER_ID, DEMO_VENDORS } from '../data/demoSeed';
import { BudgetRebalancer, RebalanceProposal } from '../domain/budgetRebalancer';
import { GeoMatchEngine } from '../domain/geoMatchEngine';
import {
  BudgetAllocation,
  DefaultWeddingCatalog,
  Guest,
  RegistryItem,
  RegistryItemType,
  RsvpStatus,
  ServiceRequest,
  ServiceRequestStatus,
  StationeryDesign,
  StationeryProductType,
  UserProfile,
  VendorCalendarBlock,
  VendorCatalogItem,
  VendorProfile,
  VendorTask,
  Wedding,
  WorkspaceMode
} from '../domain/models';

const STORAGE_KEY = 'nupcia_web_state_v1';

interface PersistedState {
  profile: UserProfile | null;
  wedding: Wedding | null;
  allocations: BudgetAllocation[];
  guests: Guest[];
  registryItems: RegistryItem[];
  stationeryDesigns: StationeryDesign[];
  vendors: VendorProfile[];
  myVendorProfile: VendorProfile | null;
  myCatalogItems: VendorCatalogItem[];
  requests: ServiceRequest[];
  vendorTasks: VendorTask[];
  vendorCalendarBlocks: VendorCalendarBlock[];
  chatTurns: ChatTurn[];
}

const WELCOME_CHAT: ChatTurn = {
  id: 'welcome',
  fromUser: false,
  text: "Hi! I'm your Nupcia Editorial Wedding & Budget Concierge. I know your vision, priority ranking, and real Utah County vendors. Which category would you like to optimize or quote today?"
};

const EMPTY_STATE: PersistedState = {
  profile: null,
  wedding: null,
  allocations: [],
  guests: [],
  registryItems: [],
  stationeryDesigns: [],
  vendors: DEMO_VENDORS,
  myVendorProfile: null,
  myCatalogItems: [],
  requests: [],
  vendorTasks: [],
  vendorCalendarBlocks: [],
  chatTurns: [WELCOME_CHAT]
};

function loadInitialState(): PersistedState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return EMPTY_STATE;
    const parsed = JSON.parse(raw) as Partial<PersistedState>;
    return {
      ...EMPTY_STATE,
      ...parsed,
      chatTurns: parsed.chatTurns?.length ? parsed.chatTurns : [WELCOME_CHAT]
    };
  } catch {
    return EMPTY_STATE;
  }
}

export function useNupciaStore() {
  const [state, setState] = useState<PersistedState>(loadInitialState);
  const [selectedCoupleTab, setSelectedCoupleTab] = useState(0);
  const [highlightedVendorId, setHighlightedVendorId] = useState<string | null>(null);
  const [isAiThinking, setIsAiThinking] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [darkMode, setDarkMode] = useState(false);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Ignore storage quota errors
    }
  }, [state]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3400);
    return () => clearTimeout(timer);
  }, [toast]);

  const scoredVendors = useMemo(() => {
    return state.vendors
      .filter((v) => v.published)
      .map((v) => GeoMatchEngine.scoreVendor(v, state.wedding, state.allocations))
      .sort((a, b) => b.affinityScore - a.affinityScore);
  }, [state.vendors, state.wedding, state.allocations]);

  const readinessReport = useMemo(() => {
    return GeoMatchEngine.computeReadiness(
      state.wedding,
      state.allocations,
      state.requests,
      state.guests,
      state.stationeryDesigns
    );
  }, [
    state.wedding,
    state.allocations,
    state.requests,
    state.guests,
    state.stationeryDesigns
  ]);

  // --- Auth & Demo Mode ---

  const enterDemoMode = useCallback(
    (mode: WorkspaceMode = 'COUPLE') => {
      const snap = createDemoSnapshot(mode);
      setState({
        ...snap,
        chatTurns: [WELCOME_CHAT]
      });
      setSelectedCoupleTab(0);
      showToast(
        mode === 'VENDOR'
          ? 'Demo Vendor Portal loaded with live Utah County inquiries & tasks'
          : 'Demo Couple Studio loaded for Emma & Liam (Provo, UT)'
      );
    },
    [showToast]
  );

  const startFreshOnboarding = useCallback(
    (displayName = 'Nupcia Bride') => {
      setState({
        ...EMPTY_STATE,
        profile: {
          id: DEMO_USER_ID,
          displayName,
          workspaceMode: 'COUPLE',
          onboardingDone: false,
          isDemo: false
        }
      });
      setSelectedCoupleTab(0);
    },
    []
  );

  const signOut = useCallback(() => {
    setState(EMPTY_STATE);
    localStorage.removeItem(STORAGE_KEY);
    setSelectedCoupleTab(0);
    showToast('Returned to Nupcia welcome screen');
  }, [showToast]);

  const switchWorkspace = useCallback(
    (mode: WorkspaceMode) => {
      setState((prev) => {
        if (!prev.profile) return prev;
        return {
          ...prev,
          profile: {
            ...prev.profile,
            workspaceMode: mode,
            onboardingDone: mode === 'VENDOR' ? true : prev.profile.onboardingDone
          }
        };
      });
      showToast(mode === 'VENDOR' ? 'Vendor Portal activated' : 'Couple Portal activated');
    },
    [showToast]
  );

  const completeOnboarding = useCallback(
    (params: {
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
    }) => {
      const safeBride = params.brideName.trim() || 'Bride';
      const safePartner = params.partnerName.trim() || 'Partner';
      const matchedSuite = DefaultWeddingCatalog.suiteForStyles(params.styleTags);
      const slug =
        `${safeBride.toLowerCase()}-and-${safePartner.toLowerCase()}`
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-|-$/g, '') || 'our-wedding';

      const allocations = BudgetRebalancer.computeInitialAllocations(
        params.totalBudget,
        params.priorityOrder
      );

      const wedding: Wedding = {
        id: DEMO_USER_ID,
        brideName: safeBride,
        partnerName: safePartner,
        weddingDate: params.weddingDate || '2027-06-19',
        city: params.city || 'Provo, UT',
        lat: params.lat,
        lng: params.lng,
        totalBudget: params.totalBudget,
        estimatedGuests: params.estimatedGuests,
        slug,
        selectedSuiteId: matchedSuite.id,
        styleTags: params.styleTags,
        colorPalette: params.colorPalette,
        ceremonyType: params.ceremonyType,
        visionExpectations: params.visionExpectations,
        priorityOrder: params.priorityOrder,
        websiteHeadline: `${safeBride} & ${safePartner}`,
        websiteStory: `We would love to celebrate the beginning of our next chapter with you in ${params.city}.`,
        websiteVenueName: `To be announced in ${params.city}`,
        websiteSchedule: '4:30 PM Ceremony • 6:00 PM Cocktail Hour • 7:30 PM Dinner & Dancing',
        websiteProtected: false
      };

      const starterInvitation: StationeryDesign = {
        id: 'inv_main',
        suiteId: matchedSuite.id,
        productType: 'INVITATION',
        headline: `${safeBride} & ${safePartner}`,
        subheadline: `${wedding.weddingDate} • ${wedding.city}`,
        bodyText:
          'Together with our families, we joyfully invite you to celebrate our wedding. Please RSVP on our wedding website.',
        bgColorHex: matchedSuite.bgHex,
        accentColorHex: matchedSuite.accentHex,
        fontStyle: 'EDITORIAL_SERIF',
        bleedValid: true,
        proofApproved: false
      };

      setState((prev) => ({
        ...prev,
        profile: {
          id: DEMO_USER_ID,
          displayName: safeBride,
          workspaceMode: 'COUPLE',
          onboardingDone: true,
          isDemo: prev.profile?.isDemo ?? false
        },
        wedding,
        allocations,
        stationeryDesigns: [starterInvitation]
      }));
      setSelectedCoupleTab(0);
      showToast('Your wedding plan & smart budget are ready!');
    },
    [showToast]
  );

  // --- Budget ---

  const updateAllocation = useCallback(
    (id: string, allocatedAmount: number, spentAmount: number, locked: boolean) => {
      setState((prev) => ({
        ...prev,
        allocations: prev.allocations.map((a) =>
          a.id === id ? { ...a, allocatedAmount, spentAmount, locked } : a
        )
      }));
      showToast('Budget category updated');
    },
    [showToast]
  );

  // --- AI Concierge ---

  const sendAiPrompt = useCallback(
    (prompt: string) => {
      const clean = prompt.trim();
      if (!clean || isAiThinking) return;

      const userTurn: ChatTurn = {
        id: `u_${Date.now()}`,
        fromUser: true,
        text: clean
      };
      setState((prev) => ({ ...prev, chatTurns: [...prev.chatTurns, userTurn] }));
      setIsAiThinking(true);

      setTimeout(() => {
        setState((prev) => {
          const reply = generateConciergeReply(
            clean,
            prev.wedding,
            prev.allocations,
            prev.vendors
          );
          return {
            ...prev,
            chatTurns: [
              ...prev.chatTurns,
              { id: `ai_${Date.now()}`, fromUser: false, ...reply }
            ]
          };
        });
        setIsAiThinking(false);
      }, 420);
    },
    [isAiThinking]
  );

  const applyProposalFromChat = useCallback(
    (turnId: string, proposal: RebalanceProposal) => {
      const idealMap = new Map(proposal.ranges.map((r) => [r.category, r.suggestedIdeal]));
      setState((prev) => ({
        ...prev,
        allocations: prev.allocations.map((a) => ({
          ...a,
          allocatedAmount: idealMap.get(a.category) ?? a.allocatedAmount
        })),
        chatTurns: prev.chatTurns.map((t) =>
          t.id === turnId ? { ...t, proposalApplied: true } : t
        )
      }));
      showToast('Budget rebalanced according to your priorities');
    },
    [showToast]
  );

  // --- Map & Vendors ---

  const openVendorOnMap = useCallback((vendorId: string) => {
    setHighlightedVendorId(vendorId);
    setSelectedCoupleTab(2);
  }, []);

  const loadDemoVendors = useCallback(() => {
    setState((prev) => {
      const existingIds = new Set(prev.vendors.map((v) => v.id));
      const merged = [...prev.vendors, ...DEMO_VENDORS.filter((d) => !existingIds.has(d.id))];
      return { ...prev, vendors: merged };
    });
    showToast('5 Utah County vendors loaded on the map');
  }, [showToast]);

  const sendInquiryToVendor = useCallback(
    (vendor: VendorProfile, catalogItemTitle: string, proposedBudget: number, notes: string) => {
      setState((prev) => {
        const coupleName = prev.wedding
          ? `${prev.wedding.brideName} & ${prev.wedding.partnerName}`
          : 'Nupcia Couple';
        const newReq: ServiceRequest = {
          id: `req_${Date.now()}`,
          coupleId: DEMO_USER_ID,
          vendorId: vendor.id,
          vendorOwnerId: vendor.ownerId,
          coupleName,
          vendorName: vendor.businessName,
          category: vendor.category,
          catalogItemTitle: catalogItemTitle.trim() || `Full ${vendor.category} Package`,
          eventDate: prev.wedding?.weddingDate ?? '2027-06-19',
          proposedBudget,
          counterOfferBudget: 0,
          notes,
          status: 'PENDING'
        };
        return { ...prev, requests: [newReq, ...prev.requests] };
      });
      showToast(`Inquiry sent to ${vendor.businessName}`);
    },
    [showToast]
  );

  // --- Vendor B2B Portal ---

  const acceptRequestAsVendor = useCallback(
    (request: ServiceRequest) => {
      setState((prev) => {
        const updatedRequests = prev.requests.map((r) =>
          r.id === request.id ? { ...r, status: 'ACCEPTED' as ServiceRequestStatus } : r
        );
        const blockId = `blk_${request.id}`;
        const hasBlock = prev.vendorCalendarBlocks.some((b) => b.id === blockId);
        const newBlocks: VendorCalendarBlock[] = hasBlock
          ? prev.vendorCalendarBlocks
          : [
              ...prev.vendorCalendarBlocks,
              {
                id: blockId,
                serviceRequestId: request.id,
                title: `${request.coupleName} Wedding (${request.category})`,
                startDate: request.eventDate,
                endDate: request.eventDate,
                blockType: 'BOOKED'
              }
            ];

        const milestones: Array<[string, string, string]> = [
          [
            'Contract & Deposit Confirmation',
            `Send formal proposal for ${request.catalogItemTitle} and confirm booking deposit.`,
            'Immediate (+3 days)'
          ],
          [
            'Creative Alignment Briefing',
            `Review aesthetic style, color palette, and vision with ${request.coupleName}.`,
            '45 days before event'
          ],
          [
            'Final Logistics & Venue Access',
            'Confirm venue load-in times, setup schedule, and technical needs.',
            '10 days before event'
          ],
          [
            'Event Execution & Post-Event Review',
            `Deliver services on ${request.eventDate} and invite couple review.`,
            request.eventDate
          ]
        ];

        const existingTaskIds = new Set(prev.vendorTasks.map((t) => t.id));
        const generated: VendorTask[] = milestones
          .map(([title, description, dueDate], idx) => ({
            id: `task_${request.id}_${idx + 1}`,
            serviceRequestId: request.id,
            coupleName: request.coupleName,
            category: request.category,
            title,
            description,
            dueDate,
            eventDate: request.eventDate,
            status: 'PENDING' as const,
            milestone: true
          }))
          .filter((t) => !existingTaskIds.has(t.id));

        return {
          ...prev,
          requests: updatedRequests,
          vendorCalendarBlocks: newBlocks,
          vendorTasks: [...generated, ...prev.vendorTasks]
        };
      });
      showToast('Inquiry accepted: date locked & 4 operational tasks created');
    },
    [showToast]
  );

  const counterOfferAsVendor = useCallback(
    (requestId: string, counterAmount: number, notes: string) => {
      setState((prev) => ({
        ...prev,
        requests: prev.requests.map((r) =>
          r.id === requestId
            ? { ...r, status: 'COUNTERED', counterOfferBudget: counterAmount, notes }
            : r
        )
      }));
      showToast('Counteroffer sent to couple');
    },
    [showToast]
  );

  const transitionRequestStatus = useCallback(
    (request: ServiceRequest, newStatus: ServiceRequestStatus) => {
      setState((prev) => {
        const updatedRequests = prev.requests.map((r) =>
          r.id === request.id ? { ...r, status: newStatus } : r
        );
        let updatedTasks = prev.vendorTasks;
        let updatedBlocks = prev.vendorCalendarBlocks;
        let updatedProfile = prev.myVendorProfile;

        if (newStatus === 'CANCELLED') {
          updatedTasks = prev.vendorTasks.map((t) =>
            t.serviceRequestId === request.id && t.status !== 'COMPLETED'
              ? { ...t, status: 'CANCELLED' }
              : t
          );
          updatedBlocks = prev.vendorCalendarBlocks.filter(
            (b) => b.serviceRequestId !== request.id
          );
        }

        if (newStatus === 'COMPLETED' && updatedProfile) {
          updatedProfile = {
            ...updatedProfile,
            completedEventsCount: updatedProfile.completedEventsCount + 1
          };
        }

        return {
          ...prev,
          requests: updatedRequests,
          vendorTasks: updatedTasks,
          vendorCalendarBlocks: updatedBlocks,
          myVendorProfile: updatedProfile
        };
      });
      const labels: Partial<Record<ServiceRequestStatus, string>> = {
        COMPLETED: 'Event marked completed and added to your analytics!',
        REJECTED: 'Inquiry declined',
        CANCELLED: 'Booking cancelled and calendar released',
        ACCEPTED: 'Counteroffer accepted'
      };
      showToast(labels[newStatus] ?? 'Status updated');
    },
    [showToast]
  );

  const cycleVendorTaskStatus = useCallback((task: VendorTask) => {
    const next =
      task.status === 'PENDING'
        ? 'IN_PROGRESS'
        : task.status === 'IN_PROGRESS'
          ? 'COMPLETED'
          : 'PENDING';
    setState((prev) => ({
      ...prev,
      vendorTasks: prev.vendorTasks.map((t) => (t.id === task.id ? { ...t, status: next } : t))
    }));
  }, []);

  const saveVendorProfile = useCallback(
    (profile: Omit<VendorProfile, 'id' | 'ownerId' | 'ratingAvg' | 'completedEventsCount'>) => {
      setState((prev) => {
        const nextProfile: VendorProfile = {
          id: prev.myVendorProfile?.id ?? 'my_vendor_utah',
          ownerId: DEMO_USER_ID,
          ratingAvg: prev.myVendorProfile?.ratingAvg ?? 4.9,
          completedEventsCount: prev.myVendorProfile?.completedEventsCount ?? 12,
          ...profile
        };
        const exists = prev.vendors.some((v) => v.id === nextProfile.id);
        const vendors = exists
          ? prev.vendors.map((v) => (v.id === nextProfile.id ? nextProfile : v))
          : [nextProfile, ...prev.vendors];
        return { ...prev, myVendorProfile: nextProfile, vendors };
      });
      showToast('Vendor profile saved & published on the Utah County map');
    },
    [showToast]
  );

  const saveCatalogPackage = useCallback(
    (item: {
      id?: string;
      title: string;
      description: string;
      basePrice: number;
      pricingUnit: string;
      active: boolean;
    }) => {
      setState((prev) => {
        if (item.id) {
          return {
            ...prev,
            myCatalogItems: prev.myCatalogItems.map((c) =>
              c.id === item.id ? { ...c, ...item, published: item.active } : c
            )
          };
        }
        const created: VendorCatalogItem = {
          id: `pkg_${Date.now()}`,
          vendorOwnerId: DEMO_USER_ID,
          title: item.title.trim() || 'Service Package',
          description: item.description,
          basePrice: item.basePrice,
          pricingUnit: item.pricingUnit || 'Per event',
          active: item.active,
          published: item.active
        };
        return { ...prev, myCatalogItems: [created, ...prev.myCatalogItems] };
      });
      showToast('Service package saved to your catalog');
    },
    [showToast]
  );

  const addManualBlock = useCallback(
    (title: string, date: string) => {
      setState((prev) => ({
        ...prev,
        vendorCalendarBlocks: [
          ...prev.vendorCalendarBlocks,
          {
            id: `blk_manual_${Date.now()}`,
            serviceRequestId: '',
            title: title.trim() || 'Blocked Date',
            startDate: date || '2027-07-10',
            endDate: date || '2027-07-10',
            blockType: 'MANUAL_BLOCK'
          }
        ]
      }));
      showToast('Date blocked on your calendar');
    },
    [showToast]
  );

  const deleteCalendarBlock = useCallback(
    (blockId: string) => {
      setState((prev) => ({
        ...prev,
        vendorCalendarBlocks: prev.vendorCalendarBlocks.filter((b) => b.id !== blockId)
      }));
      showToast('Calendar block removed');
    },
    [showToast]
  );

  // --- Event Hub: Guests, Registry, Stationery, Website ---

  const saveGuest = useCallback(
    (guest: {
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
    }) => {
      setState((prev) => {
        if (guest.id) {
          return {
            ...prev,
            guests: prev.guests.map((g) => (g.id === guest.id ? { ...g, ...guest } : g))
          };
        }
        const created: Guest = {
          ...guest,
          id: `g_${Date.now()}`,
          householdName: guest.householdName.trim() || 'Guest Household',
          fullName: guest.fullName.trim() || guest.householdName.trim() || 'Guest',
          inviteToken: `NUP-${Math.random().toString(36).slice(2, 8).toUpperCase()}`
        };
        return { ...prev, guests: [...prev.guests, created] };
      });
      showToast('Guest saved');
    },
    [showToast]
  );

  const deleteGuest = useCallback(
    (guestId: string) => {
      setState((prev) => ({
        ...prev,
        guests: prev.guests.filter((g) => g.id !== guestId)
      }));
      showToast('Guest removed');
    },
    [showToast]
  );

  const addRegistryItem = useCallback(
    (itemType: RegistryItemType, title: string, description: string, targetAmount: number) => {
      setState((prev) => ({
        ...prev,
        registryItems: [
          {
            id: `reg_${Date.now()}`,
            itemType,
            title: title.trim() || 'Wedding Fund',
            description,
            targetAmount,
            collectedAmount: 0,
            contributorCount: 0,
            lastContributorName: '',
            thankYouSent: false
          },
          ...prev.registryItems
        ]
      }));
      showToast('Added to your wedding registry');
    },
    [showToast]
  );

  const addContribution = useCallback(
    (itemId: string, contributorName: string, amount: number) => {
      const safeName = contributorName.trim() || 'Guest';
      setState((prev) => ({
        ...prev,
        registryItems: prev.registryItems.map((i) =>
          i.id === itemId
            ? {
                ...i,
                collectedAmount: i.collectedAmount + Math.max(1, amount),
                contributorCount: i.contributorCount + 1,
                lastContributorName: safeName
              }
            : i
        )
      }));
      showToast(`Contribution recorded from ${safeName}`);
    },
    [showToast]
  );

  const toggleThankYou = useCallback((itemId: string, thankYouSent: boolean) => {
    setState((prev) => ({
      ...prev,
      registryItems: prev.registryItems.map((i) =>
        i.id === itemId ? { ...i, thankYouSent } : i
      )
    }));
  }, []);

  const saveStationery = useCallback(
    (design: StationeryDesign) => {
      setState((prev) => {
        const exists = prev.stationeryDesigns.some((d) => d.id === design.id);
        return {
          ...prev,
          stationeryDesigns: exists
            ? prev.stationeryDesigns.map((d) => (d.id === design.id ? design : d))
            : [design, ...prev.stationeryDesigns]
        };
      });
      showToast(
        design.proofApproved
          ? 'Print proof approved and ready for press'
          : 'Stationery draft saved'
      );
    },
    [showToast]
  );

  const saveWeddingWebsite = useCallback(
    (params: {
      selectedSuiteId: string;
      websiteHeadline: string;
      websiteStory: string;
      websiteVenueName: string;
      websiteSchedule: string;
      websiteProtected: boolean;
      slug: string;
    }) => {
      setState((prev) => {
        if (!prev.wedding) return prev;
        return {
          ...prev,
          wedding: {
            ...prev.wedding,
            ...params,
            slug: params.slug.trim() || 'our-wedding'
          }
        };
      });
      showToast('Wedding website updated');
    },
    [showToast]
  );

  return {
    ...state,
    scoredVendors,
    readinessReport,
    selectedCoupleTab,
    setSelectedCoupleTab,
    highlightedVendorId,
    clearHighlightedVendor: () => setHighlightedVendorId(null),
    isAiThinking,
    toast,
    darkMode,
    toggleDarkMode: () => setDarkMode((d) => !d),
    enterDemoMode,
    startFreshOnboarding,
    signOut,
    switchWorkspace,
    completeOnboarding,
    updateAllocation,
    sendAiPrompt,
    applyProposalFromChat,
    openVendorOnMap,
    loadDemoVendors,
    sendInquiryToVendor,
    acceptRequestAsVendor,
    counterOfferAsVendor,
    transitionRequestStatus,
    cycleVendorTaskStatus,
    saveVendorProfile,
    saveCatalogPackage,
    addManualBlock,
    deleteCalendarBlock,
    saveGuest,
    deleteGuest,
    addRegistryItem,
    addContribution,
    toggleThankYou,
    saveStationery,
    saveWeddingWebsite,
    showToast
  };
}
