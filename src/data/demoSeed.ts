import { BudgetRebalancer } from '../domain/budgetRebalancer';
import {
  BudgetAllocation,
  DefaultWeddingCatalog,
  Guest,
  RegistryItem,
  ServiceRequest,
  StationeryDesign,
  UserProfile,
  VendorCalendarBlock,
  VendorCatalogItem,
  VendorProfile,
  VendorTask,
  Wedding
} from '../domain/models';

export const DEMO_USER_ID = 'demo_user_utah';

export const DEMO_VENDORS: VendorProfile[] = [
  {
    id: 'demo_venue_utah',
    ownerId: DEMO_USER_ID,
    businessName: 'Timpanogos Garden Estate',
    category: 'Venue',
    description:
      'Manicured foothills gardens, open-air pavilion, and mountain views in Provo Canyon.',
    city: 'Sundance / Provo Canyon',
    lat: 40.3785,
    lng: -111.596,
    coverageRadiusKm: 65,
    minPrice: 4500,
    maxPrice: 11000,
    styleTags: ['Garden', 'Classic', 'Mountain'],
    ratingAvg: 4.9,
    completedEventsCount: 28,
    published: true
  },
  {
    id: 'demo_photo_utah',
    ownerId: DEMO_USER_ID,
    businessName: 'Wasatch Light & Film Co.',
    category: 'Photography',
    description:
      'Editorial documentary & film wedding photography with heirloom linen albums.',
    city: 'Provo, UT',
    lat: 40.2444,
    lng: -111.6485,
    coverageRadiusKm: 120,
    minPrice: 2200,
    maxPrice: 4800,
    styleTags: ['Editorial', 'Minimalist', 'Modern'],
    ratingAvg: 5.0,
    completedEventsCount: 41,
    published: true
  },
  {
    id: 'demo_floral_utah',
    ownerId: DEMO_USER_ID,
    businessName: 'Wild Sage Floral Studio',
    category: 'Florals',
    description:
      'Organic floral arches, seasonal Utah blooms, and candlelit reception tablescapes.',
    city: 'Lehi, UT',
    lat: 40.388,
    lng: -111.832,
    coverageRadiusKm: 80,
    minPrice: 1800,
    maxPrice: 4500,
    styleTags: ['Garden', 'Bohemian', 'Romantic'],
    ratingAvg: 4.8,
    completedEventsCount: 19,
    published: true
  },
  {
    id: 'demo_catering_utah',
    ownerId: DEMO_USER_ID,
    businessName: 'Canyon Table Catering',
    category: 'Catering',
    description:
      'Locally sourced seasonal plated dinners, mocktail craft bars, and artisan grazing tables.',
    city: 'American Fork, UT',
    lat: 40.369,
    lng: -111.785,
    coverageRadiusKm: 90,
    minPrice: 5000,
    maxPrice: 12500,
    styleTags: ['Classic', 'Modern', 'Editorial'],
    ratingAvg: 4.9,
    completedEventsCount: 34,
    published: true
  },
  {
    id: 'demo_music_utah',
    ownerId: DEMO_USER_ID,
    businessName: 'Valley Strings & DJ Collective',
    category: 'Music',
    description:
      'Live string quartet for ceremonies plus modern reception DJ and sound production.',
    city: 'Springville, UT',
    lat: 40.172,
    lng: -111.615,
    coverageRadiusKm: 100,
    minPrice: 1400,
    maxPrice: 3200,
    styleTags: ['Modern', 'Classic', 'Romantic'],
    ratingAvg: 4.8,
    completedEventsCount: 23,
    published: true
  }
];

export interface DemoSnapshot {
  profile: UserProfile;
  wedding: Wedding;
  allocations: BudgetAllocation[];
  guests: Guest[];
  registryItems: RegistryItem[];
  stationeryDesigns: StationeryDesign[];
  vendors: VendorProfile[];
  myVendorProfile: VendorProfile;
  myCatalogItems: VendorCatalogItem[];
  requests: ServiceRequest[];
  vendorTasks: VendorTask[];
  vendorCalendarBlocks: VendorCalendarBlock[];
}

export function createDemoSnapshot(mode: 'COUPLE' | 'VENDOR' = 'COUPLE'): DemoSnapshot {
  const priorityOrder = [...DefaultWeddingCatalog.CATEGORIES];
  const baseAllocations = BudgetRebalancer.computeInitialAllocations(35000, priorityOrder);
  const allocations: BudgetAllocation[] = baseAllocations.map((a) => {
    if (a.category === 'Venue') {
      return { ...a, spentAmount: 7800, locked: true, notes: 'High Priority • Deposit secured' };
    }
    if (a.category === 'Photography') {
      return { ...a, spentAmount: 1200, notes: 'High Priority • Retainer held' };
    }
    if (a.category === 'Florals') {
      return { ...a, spentAmount: 600, notes: 'Medium Priority • Moodboard approved' };
    }
    return a;
  });

  const wedding: Wedding = {
    id: DEMO_USER_ID,
    brideName: 'Emma',
    partnerName: 'Liam',
    weddingDate: '2027-06-19',
    city: 'Provo, UT',
    lat: 40.2338,
    lng: -111.6585,
    totalBudget: 35000,
    estimatedGuests: 140,
    slug: 'emma-and-liam',
    selectedSuiteId: 'botanical_ivory',
    styleTags: ['Garden', 'Classic', 'Editorial'],
    colorPalette: ['#7B2D3B', '#FAF6F1', '#536B5E'],
    ceremonyType: 'Outdoor Garden',
    visionExpectations:
      'A luminous mountain-view garden celebration with seasonal dining and timeless editorial photography.',
    priorityOrder,
    websiteHeadline: 'Emma & Liam',
    websiteStory:
      'We met on a golden autumn hike near Bridal Veil Falls and cannot wait to celebrate our wedding surrounded by family and friends in Utah County.',
    websiteVenueName: 'Timpanogos Garden Estate, Provo Canyon',
    websiteSchedule: '4:30 PM Ceremony • 6:00 PM Cocktail Hour • 7:30 PM Dinner & Dancing',
    websiteProtected: false
  };

  const guests: Guest[] = [
    {
      id: 'g_1',
      householdName: 'The Carter Family',
      fullName: 'Hannah & David Carter',
      partySize: 4,
      email: 'hannah.carter@example.com',
      phone: '(801) 555-0142',
      postalAddress: '482 E Center St, Provo, UT 84606',
      rsvpStatus: 'CONFIRMED',
      mealChoice: 'Herb-Crusted Salmon',
      dietaryNotes: '1 Gluten-free',
      tableNumber: 1,
      inviteToken: 'NUP-CART01'
    },
    {
      id: 'g_2',
      householdName: 'Sorensen Household',
      fullName: 'Claire & Nathan Sorensen',
      partySize: 2,
      email: 'claire.sorensen@example.com',
      phone: '(801) 555-0188',
      postalAddress: '1120 N Canyon Rd, Orem, UT 84097',
      rsvpStatus: 'CONFIRMED',
      mealChoice: 'Braised Short Rib',
      dietaryNotes: '',
      tableNumber: 1,
      inviteToken: 'NUP-SORE02'
    },
    {
      id: 'g_3',
      householdName: 'Bennett Family',
      fullName: 'Olivia & Marcus Bennett',
      partySize: 3,
      email: 'olivia.bennett@example.com',
      phone: '(801) 555-0194',
      postalAddress: '310 W Main St, Lehi, UT 84043',
      rsvpStatus: 'CONFIRMED',
      mealChoice: 'Wild Mushroom Risotto',
      dietaryNotes: 'Vegetarian',
      tableNumber: 2,
      inviteToken: 'NUP-BENN03'
    },
    {
      id: 'g_4',
      householdName: 'Jensen Household',
      fullName: 'Sofia Jensen & Guest',
      partySize: 2,
      email: 'sofia.jensen@example.com',
      phone: '(801) 555-0115',
      postalAddress: '745 S Main St, Springville, UT 84663',
      rsvpStatus: 'PENDING',
      mealChoice: 'Main Course',
      dietaryNotes: 'Nut allergy',
      tableNumber: 0,
      inviteToken: 'NUP-JENS04'
    }
  ];

  const registryItems: RegistryItem[] = [
    {
      id: 'reg_1',
      itemType: 'CASH_FUND',
      title: 'Amalfi Coast Honeymoon Fund',
      description: 'Cliffside stays, sunset boat excursions, and Italian espresso mornings.',
      targetAmount: 4500,
      collectedAmount: 2850,
      contributorCount: 11,
      lastContributorName: 'The Carter Family',
      thankYouSent: true
    },
    {
      id: 'reg_2',
      itemType: 'CASH_FUND',
      title: 'Utah Valley Home Down Payment',
      description: 'Helping us build our first sunlit home together near the Wasatch foothills.',
      targetAmount: 6000,
      collectedAmount: 1900,
      contributorCount: 6,
      lastContributorName: 'Claire & Nathan Sorensen',
      thankYouSent: false
    },
    {
      id: 'reg_3',
      itemType: 'PRODUCT',
      title: 'Heirloom Linen Dinnerware Set',
      description: '12-piece stoneware & silk-ivory table linen collection for Sunday dinners.',
      targetAmount: 680,
      collectedAmount: 680,
      contributorCount: 2,
      lastContributorName: 'Olivia & Marcus Bennett',
      thankYouSent: true
    }
  ];

  const stationeryDesigns: StationeryDesign[] = [
    {
      id: 'inv_main',
      suiteId: 'botanical_ivory',
      productType: 'INVITATION',
      headline: 'Emma & Liam',
      subheadline: 'June 19, 2027 • Provo Canyon, UT',
      bodyText:
        'Together with their families, joyfully invite you to celebrate their marriage at Timpanogos Garden Estate.',
      bgColorHex: '#FAF6F1',
      accentColorHex: '#4B6858',
      fontStyle: 'EDITORIAL_SERIF',
      bleedValid: true,
      proofApproved: false
    }
  ];

  const myVendorProfile: VendorProfile = { ...DEMO_VENDORS[1] };

  const myCatalogItems: VendorCatalogItem[] = [
    {
      id: 'cat_pkg_1',
      vendorOwnerId: DEMO_USER_ID,
      title: 'Heirloom Editorial Full-Day Coverage',
      description: '10 hours dual-photographer coverage, 35mm film rolls, online gallery & linen album.',
      basePrice: 3800,
      pricingUnit: 'Per event',
      active: true,
      published: true
    },
    {
      id: 'cat_pkg_2',
      vendorOwnerId: DEMO_USER_ID,
      title: 'Intimate Canyon Elopement',
      description: '5 hours documentary coverage in Provo Canyon + sunset bridal portraits.',
      basePrice: 2200,
      pricingUnit: 'Per event',
      active: true,
      published: true
    }
  ];

  const requests: ServiceRequest[] = [
    {
      id: 'req_1',
      coupleId: DEMO_USER_ID,
      vendorId: 'demo_venue_utah',
      vendorOwnerId: DEMO_USER_ID,
      coupleName: 'Emma & Liam',
      vendorName: 'Timpanogos Garden Estate',
      category: 'Venue',
      catalogItemTitle: 'Full Estate Garden & Pavilion Package',
      eventDate: '2027-06-19',
      proposedBudget: 7800,
      counterOfferBudget: 0,
      notes: 'Includes ceremony lawn, mountain-view pavilion, and bridal suite access.',
      status: 'ACCEPTED'
    },
    {
      id: 'req_2',
      coupleId: DEMO_USER_ID,
      vendorId: 'demo_photo_utah',
      vendorOwnerId: DEMO_USER_ID,
      coupleName: 'Emma & Liam',
      vendorName: 'Wasatch Light & Film Co.',
      category: 'Photography',
      catalogItemTitle: 'Heirloom Editorial Full-Day Coverage',
      eventDate: '2027-06-19',
      proposedBudget: 3500,
      counterOfferBudget: 3800,
      notes: 'Includes 2 rolls of medium-format film and custom linen album!',
      status: 'COUNTERED'
    },
    {
      id: 'req_3',
      coupleId: DEMO_USER_ID,
      vendorId: 'demo_floral_utah',
      vendorOwnerId: DEMO_USER_ID,
      coupleName: 'Emma & Liam',
      vendorName: 'Wild Sage Floral Studio',
      category: 'Florals',
      catalogItemTitle: 'Organic Garden Floral Arch & Tablescapes',
      eventDate: '2027-06-19',
      proposedBudget: 3200,
      counterOfferBudget: 0,
      notes: 'We love eucalyptus sage, ivory garden roses, and bordeaux ranunculus accents.',
      status: 'PENDING'
    }
  ];

  const vendorTasks: VendorTask[] = [
    {
      id: 'task_req_1_1',
      serviceRequestId: 'req_1',
      coupleName: 'Emma & Liam',
      category: 'Venue',
      title: 'Contract & Deposit Confirmation',
      description: 'Send formal proposal for Full Estate Garden & Pavilion Package and confirm booking deposit.',
      dueDate: 'Immediate (+3 days)',
      eventDate: '2027-06-19',
      status: 'COMPLETED',
      milestone: true
    },
    {
      id: 'task_req_1_2',
      serviceRequestId: 'req_1',
      coupleName: 'Emma & Liam',
      category: 'Venue',
      title: 'Creative Alignment Briefing',
      description: 'Review aesthetic style, color palette, and floor plan with Emma & Liam.',
      dueDate: '45 days before event',
      eventDate: '2027-06-19',
      status: 'IN_PROGRESS',
      milestone: true
    },
    {
      id: 'task_req_1_3',
      serviceRequestId: 'req_1',
      coupleName: 'Emma & Liam',
      category: 'Venue',
      title: 'Final Logistics & Venue Access',
      description: 'Confirm vendor load-in times, lighting setup schedule, and ceremony rain plan.',
      dueDate: '10 days before event',
      eventDate: '2027-06-19',
      status: 'PENDING',
      milestone: true
    },
    {
      id: 'task_req_1_4',
      serviceRequestId: 'req_1',
      coupleName: 'Emma & Liam',
      category: 'Venue',
      title: 'Event Execution & Post-Event Review',
      description: 'Deliver services on 2027-06-19 and invite couple review.',
      dueDate: '2027-06-19',
      eventDate: '2027-06-19',
      status: 'PENDING',
      milestone: true
    }
  ];

  const vendorCalendarBlocks: VendorCalendarBlock[] = [
    {
      id: 'blk_req_1',
      serviceRequestId: 'req_1',
      title: 'Emma & Liam Wedding (Venue)',
      startDate: '2027-06-19',
      endDate: '2027-06-19',
      blockType: 'BOOKED'
    },
    {
      id: 'blk_manual_1',
      serviceRequestId: '',
      title: 'Editorial Styled Shoot — Sundance',
      startDate: '2027-07-04',
      endDate: '2027-07-04',
      blockType: 'MANUAL_BLOCK'
    }
  ];

  return {
    profile: {
      id: DEMO_USER_ID,
      displayName: 'Emma (Demo)',
      workspaceMode: mode,
      onboardingDone: true,
      isDemo: true
    },
    wedding,
    allocations,
    guests,
    registryItems,
    stationeryDesigns,
    vendors: DEMO_VENDORS,
    myVendorProfile,
    myCatalogItems,
    requests,
    vendorTasks,
    vendorCalendarBlocks
  };
}
