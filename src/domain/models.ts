export type WorkspaceMode = 'COUPLE' | 'VENDOR';
export type RsvpStatus = 'PENDING' | 'CONFIRMED' | 'DECLINED';
export type RegistryItemType = 'PRODUCT' | 'CASH_FUND';
export type StationeryProductType = 'INVITATION' | 'SAVE_THE_DATE' | 'MENU' | 'ENVELOPE';
export type ServiceRequestStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'COUNTERED'
  | 'COMPLETED'
  | 'CANCELLED';
export type VendorTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type CalendarBlockType = 'BOOKED' | 'MANUAL_BLOCK';

export interface UserProfile {
  id: string;
  displayName: string;
  workspaceMode: WorkspaceMode;
  onboardingDone: boolean;
  isDemo: boolean;
}

export interface Wedding {
  id: string;
  brideName: string;
  partnerName: string;
  weddingDate: string;
  city: string;
  lat: number;
  lng: number;
  totalBudget: number;
  estimatedGuests: number;
  slug: string;
  selectedSuiteId: string;
  styleTags: string[];
  colorPalette: string[];
  ceremonyType: string;
  visionExpectations: string;
  priorityOrder: string[];
  websiteHeadline: string;
  websiteStory: string;
  websiteVenueName: string;
  websiteSchedule: string;
  websiteProtected: boolean;
}

export interface BudgetAllocation {
  id: string;
  category: string;
  allocatedAmount: number;
  spentAmount: number;
  priorityRank: number;
  locked: boolean;
  notes: string;
}

export interface Guest {
  id: string;
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
  inviteToken: string;
}

export interface RegistryItem {
  id: string;
  itemType: RegistryItemType;
  title: string;
  description: string;
  targetAmount: number;
  collectedAmount: number;
  contributorCount: number;
  lastContributorName: string;
  thankYouSent: boolean;
}

export interface StationeryDesign {
  id: string;
  suiteId: string;
  productType: StationeryProductType;
  headline: string;
  subheadline: string;
  bodyText: string;
  bgColorHex: string;
  accentColorHex: string;
  fontStyle: string;
  bleedValid: boolean;
  proofApproved: boolean;
}

export interface VendorProfile {
  id: string;
  ownerId: string;
  businessName: string;
  category: string;
  description: string;
  city: string;
  lat: number;
  lng: number;
  coverageRadiusKm: number;
  minPrice: number;
  maxPrice: number;
  styleTags: string[];
  ratingAvg: number;
  completedEventsCount: number;
  published: boolean;
}

export interface VendorCatalogItem {
  id: string;
  vendorOwnerId: string;
  title: string;
  description: string;
  basePrice: number;
  pricingUnit: string;
  active: boolean;
  published: boolean;
}

export interface ServiceRequest {
  id: string;
  coupleId: string;
  vendorId: string;
  vendorOwnerId: string;
  coupleName: string;
  vendorName: string;
  category: string;
  catalogItemTitle: string;
  eventDate: string;
  proposedBudget: number;
  counterOfferBudget: number;
  notes: string;
  status: ServiceRequestStatus;
}

export interface VendorTask {
  id: string;
  serviceRequestId: string;
  coupleName: string;
  category: string;
  title: string;
  description: string;
  dueDate: string;
  eventDate: string;
  status: VendorTaskStatus;
  milestone: boolean;
}

export interface VendorCalendarBlock {
  id: string;
  serviceRequestId: string;
  title: string;
  startDate: string;
  endDate: string;
  blockType: CalendarBlockType;
}

export interface CityOption {
  name: string;
  lat: number;
  lng: number;
}

export interface StationerySuite {
  id: string;
  name: string;
  styleTag: string;
  bgHex: string;
  accentHex: string;
  subtitle: string;
}

export const DefaultWeddingCatalog = {
  CATEGORIES: [
    'Venue',
    'Catering',
    'Photography',
    'Florals',
    'Music',
    'Attire',
    'Stationery',
    'Coordination'
  ],

  STYLE_TAGS: [
    'Classic',
    'Bohemian',
    'Modern',
    'Garden',
    'Minimalist',
    'Mountain',
    'Romantic',
    'Editorial'
  ],

  CEREMONY_TYPES: [
    'Outdoor Garden',
    'Mountain Lodge / Canyon',
    'Historic Estate',
    'Modern Ballroom',
    'Intimate Ceremony'
  ],

  COLOR_PALETTES: {
    'Bordeaux & Ivory': ['#7B2D3B', '#FAF6F1', '#536B5E'],
    'Eucalyptus & Champagne': ['#4B6858', '#F7F3EB', '#B68D40'],
    'Terracotta & Sand': ['#A4573E', '#F8EFE6', '#6D594D'],
    'Midnight & Gold': ['#24293E', '#F4F1EA', '#C59B43']
  } as Record<string, string[]>,

  CITIES: [
    { name: 'Provo, UT', lat: 40.2338, lng: -111.6585 },
    { name: 'Orem, UT', lat: 40.2969, lng: -111.6946 },
    { name: 'Lehi, UT', lat: 40.3916, lng: -111.8508 },
    { name: 'American Fork, UT', lat: 40.3769, lng: -111.7958 },
    { name: 'Sundance / Provo Canyon', lat: 40.3934, lng: -111.5888 },
    { name: 'Alpine, UT', lat: 40.4533, lng: -111.778 },
    { name: 'Pleasant Grove, UT', lat: 40.3641, lng: -111.7385 },
    { name: 'Highland, UT', lat: 40.4272, lng: -111.7966 },
    { name: 'Springville, UT', lat: 40.1652, lng: -111.6108 },
    { name: 'Spanish Fork, UT', lat: 40.115, lng: -111.6549 }
  ] as CityOption[],

  SUITES: [
    {
      id: 'botanical_ivory',
      name: 'Botanical Ivory',
      styleTag: 'Garden',
      bgHex: '#FAF6F1',
      accentHex: '#4B6858',
      subtitle: 'Fine foliage & classic serif'
    },
    {
      id: 'bordeaux_couture',
      name: 'Bordeaux Couture',
      styleTag: 'Classic',
      bgHex: '#FDF7F4',
      accentHex: '#7B2D3B',
      subtitle: 'Timeless editorial elegance'
    },
    {
      id: 'terracotta_boho',
      name: 'Terracotta Sun',
      styleTag: 'Bohemian',
      bgHex: '#F9F0E7',
      accentHex: '#A4573E',
      subtitle: 'Warm organic desert tones'
    },
    {
      id: 'champagne_minimal',
      name: 'Champagne Line',
      styleTag: 'Minimalist',
      bgHex: '#FFFFFF',
      accentHex: '#9C762F',
      subtitle: 'Clean gold architectural lines'
    },
    {
      id: 'alpine_linen',
      name: 'Alpine Linen',
      styleTag: 'Mountain',
      bgHex: '#F4F8F9',
      accentHex: '#3B6275',
      subtitle: 'Crisp Wasatch mountain air'
    }
  ] as StationerySuite[],

  suiteForStyles(styles: string[]): StationerySuite {
    const found = this.SUITES.find((suite) =>
      styles.some((s) => s.toLowerCase() === suite.styleTag.toLowerCase())
    );
    return found ?? this.SUITES[0];
  }
};
