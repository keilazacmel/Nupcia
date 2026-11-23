import { BudgetRebalancer } from './budgetRebalancer';
import {
  BudgetAllocation,
  Guest,
  ServiceRequest,
  StationeryDesign,
  VendorProfile,
  Wedding
} from './models';

export interface ScoredVendor {
  vendor: VendorProfile;
  distanceKm: number;
  distanceMiles: number;
  coverageRadiusMiles: number;
  coversWeddingLocation: boolean;
  withinCategoryBudget: boolean;
  affinityScore: number;
  categoryBudgetAllocated: number;
}

export interface ReadinessReport {
  score: number;
  budgetHealth: number;
  bookedHighPriorityCount: number;
  confirmedGuestsCount: number;
  seatedGuestsCount: number;
  nextBestActionTitle: string;
  nextBestActionSubtitle: string;
  nextBestActionTab: number; // 0=Home, 1=AI, 2=Map, 3=Event
}

const EARTH_RADIUS_KM = 6371.0;

function toRadians(deg: number): number {
  return (deg * Math.PI) / 180;
}

export const GeoMatchEngine = {
  distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const dLat = toRadians(lat2 - lat1);
    const dLng = toRadians(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLng / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_KM * c;
  },

  scoreVendor(
    vendor: VendorProfile,
    wedding: Wedding | null,
    allocations: BudgetAllocation[]
  ): ScoredVendor {
    const wLat = wedding?.lat ?? 40.2338;
    const wLng = wedding?.lng ?? -111.6585;
    const dist = this.distanceKm(wLat, wLng, vendor.lat, vendor.lng);
    const covers = dist <= vendor.coverageRadiusKm;

    const matchedAlloc = allocations.find(
      (a) => a.category.toLowerCase() === vendor.category.toLowerCase()
    );
    const catAlloc = matchedAlloc?.allocatedAmount ?? (wedding?.totalBudget ?? 35000) * 0.12;
    const withinBudget = vendor.minPrice <= catAlloc * 1.1;

    const weddingStyles = new Set((wedding?.styleTags ?? []).map((s) => s.toLowerCase()));
    const vendorStyles = new Set(vendor.styleTags.map((s) => s.toLowerCase()));
    let styleScore: number;
    if (weddingStyles.size === 0) {
      styleScore = 24;
    } else {
      let overlap = 0;
      weddingStyles.forEach((s) => {
        if (vendorStyles.has(s)) overlap++;
      });
      styleScore = Math.min(35, (overlap / Math.max(1, weddingStyles.size)) * 35);
    }

    const budgetScore =
      vendor.minPrice <= catAlloc && vendor.maxPrice >= catAlloc * 0.5
        ? 30
        : vendor.minPrice <= catAlloc * 1.15
          ? 20
          : 8;

    const geoScore =
      covers && dist <= vendor.coverageRadiusKm * 0.5 ? 20 : covers ? 15 : 3;

    const ratingScore = (Math.min(5, Math.max(0, vendor.ratingAvg)) / 5) * 15;
    const total = Math.min(99, Math.max(25, Math.round(styleScore + budgetScore + geoScore + ratingScore)));

    return {
      vendor,
      distanceKm: dist,
      distanceMiles: dist * 0.621371,
      coverageRadiusMiles: Math.max(1, Math.round(vendor.coverageRadiusKm * 0.621371)),
      coversWeddingLocation: covers,
      withinCategoryBudget: withinBudget,
      affinityScore: total,
      categoryBudgetAllocated: catAlloc
    };
  },

  computeReadiness(
    wedding: Wedding | null,
    allocations: BudgetAllocation[],
    requests: ServiceRequest[],
    guests: Guest[],
    stationery: StationeryDesign[]
  ): ReadinessReport {
    const budgetHealth = BudgetRebalancer.computeBudgetHealthScore(
      wedding?.totalBudget ?? 0,
      allocations
    );
    const topPriorities = wedding?.priorityOrder.slice(0, 3) ?? [];
    const bookedCats = new Set(
      requests
        .filter((r) => r.status === 'ACCEPTED' || r.status === 'COMPLETED')
        .map((r) => r.category.toLowerCase())
    );
    const bookedTop = topPriorities.filter((top) => bookedCats.has(top.toLowerCase())).length;

    const confirmedGuests = guests
      .filter((g) => g.rsvpStatus === 'CONFIRMED')
      .reduce((acc, g) => acc + g.partySize, 0);
    const seatedGuests = guests
      .filter((g) => g.rsvpStatus === 'CONFIRMED' && g.tableNumber > 0)
      .reduce((acc, g) => acc + g.partySize, 0);
    const hasApprovedProof = stationery.some((s) => s.proofApproved);

    const budgetPts = Math.round(budgetHealth * 0.25);
    const vendorPts = Math.round((bookedTop / Math.max(1, topPriorities.length)) * 30);
    const guestPts = guests.length > 0 ? 20 : 5;
    const seatingPts =
      confirmedGuests > 0 ? Math.round((seatedGuests / confirmedGuests) * 15) : 5;
    const stationeryPts = hasApprovedProof ? 10 : stationery.length > 0 ? 5 : 0;

    const totalScore = Math.min(
      100,
      Math.max(10, budgetPts + vendorPts + guestPts + seatingPts + stationeryPts)
    );

    let title: string;
    let subtitle: string;
    let tab: number;

    if (bookedTop < topPriorities.length) {
      const missing =
        topPriorities.find((top) => !bookedCats.has(top.toLowerCase())) ?? 'your #1 priority';
      title = `Book your ${missing} vendor`;
      subtitle =
        'This is one of your top priorities. Explore Utah County matches on the map or ask the AI Concierge.';
      tab = 2;
    } else if (guests.length === 0) {
      title = 'Add your first households & guests';
      subtitle =
        'Add guests to collect mailing addresses, track RSVPs, and unlock your Seating Chart.';
      tab = 3;
    } else if (confirmedGuests > seatedGuests) {
      title = `Assign tables for ${confirmedGuests - seatedGuests} confirmed guests`;
      subtitle = 'Organize your reception seating chart and review dietary notes.';
      tab = 3;
    } else if (!hasApprovedProof) {
      title = 'Approve your wedding stationery proof';
      subtitle = 'Customize your coordinated suite and verify print bleed safe zones.';
      tab = 3;
    } else {
      title = 'Fine-tune your final balance with AI Concierge';
      subtitle = 'Your wedding plan is in great shape. Explore ways to optimize your remaining budget.';
      tab = 1;
    }

    return {
      score: totalScore,
      budgetHealth,
      bookedHighPriorityCount: bookedTop,
      confirmedGuestsCount: confirmedGuests,
      seatedGuestsCount: seatedGuests,
      nextBestActionTitle: title,
      nextBestActionSubtitle: subtitle,
      nextBestActionTab: tab
    };
  }
};
