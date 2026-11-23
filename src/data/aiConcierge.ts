import { BudgetRebalancer, RebalanceProposal } from '../domain/budgetRebalancer';
import { GeoMatchEngine, ScoredVendor } from '../domain/geoMatchEngine';
import { BudgetAllocation, DefaultWeddingCatalog, VendorProfile, Wedding } from '../domain/models';

export interface ChatTurn {
  id: string;
  fromUser: boolean;
  text: string;
  impactSummary?: string;
  proposal?: RebalanceProposal;
  proposalApplied?: boolean;
  recommendedVendors?: ScoredVendor[];
}

const CATEGORY_KEYWORDS: Record<string, string[]> = {
  Venue: ['venue', 'estate', 'garden', 'canyon', 'lodge', 'hall', 'barn', 'location', 'lugar', 'salón', 'jardin'],
  Catering: ['catering', 'food', 'dinner', 'menu', 'cake', 'banquet', 'bar', 'comida', 'cena', 'banquete'],
  Photography: ['photo', 'photographer', 'photography', 'video', 'film', 'camera', 'editorial', 'foto', 'fotografia'],
  Florals: ['floral', 'flower', 'bouquet', 'decor', 'centerpiece', 'arch', 'flores', 'ramo', 'decoracion'],
  Music: ['music', 'dj', 'band', 'quartet', 'strings', 'sound', 'musica', 'banda'],
  Attire: ['dress', 'attire', 'gown', 'suit', 'tux', 'hair', 'makeup', 'vestido', 'traje'],
  Stationery: ['stationery', 'invite', 'invitation', 'envelope', 'paper', 'card', 'invitacion', 'papeleria'],
  Coordination: ['coordinator', 'coordination', 'planner', 'logistics', 'timeline', 'coordinacion', 'planeacion']
};

export function detectCategoriesFromPrompt(prompt: string, priorities: string[]): string[] {
  const lower = prompt.toLowerCase();
  const matched = Object.entries(CATEGORY_KEYWORDS)
    .filter(([, keys]) => keys.some((k) => lower.includes(k)))
    .map(([cat]) => cat);
  return matched.length > 0 ? matched : priorities.slice(0, 2);
}

export function generateConciergeReply(
  userPrompt: string,
  wedding: Wedding | null,
  allocations: BudgetAllocation[],
  allVendors: VendorProfile[]
): Omit<ChatTurn, 'id' | 'fromUser'> {
  const priorities = wedding?.priorityOrder ?? DefaultWeddingCatalog.CATEGORIES;
  const detectedCategories = detectCategoriesFromPrompt(userPrompt, priorities);
  const isSavingPrompt = /save|ahorr|reduc|cut|lower|trim|bajar|econom/i.test(userPrompt);
  const boostPercent = isSavingPrompt ? -0.15 : 0.16;

  const proposal = BudgetRebalancer.rebalanceForCategories(
    wedding?.totalBudget ?? 35000,
    priorities,
    allocations,
    detectedCategories,
    boostPercent
  );

  const proposedAllocations = allocations.map((alloc) => {
    const ideal =
      proposal.ranges.find((r) => r.category === alloc.category)?.suggestedIdeal ??
      alloc.allocatedAmount;
    return { ...alloc, allocatedAmount: ideal };
  });

  const scoredVendors = allVendors
    .map((v) => GeoMatchEngine.scoreVendor(v, wedding, proposedAllocations))
    .filter(
      (sv) =>
        detectedCategories.length === 0 ||
        detectedCategories.some((c) => c.toLowerCase() === sv.vendor.category.toLowerCase())
    )
    .sort((a, b) => b.affinityScore - a.affinityScore)
    .slice(0, 3);

  const focusLabel = detectedCategories.join(' & ');
  const coupleNames =
    wedding && wedding.brideName ? `${wedding.brideName} & ${wedding.partnerName}` : 'your wedding';
  const city = wedding?.city ?? 'Provo, UT';

  const topChanges = proposal.ranges
    .filter((r) => Math.abs(r.delta) >= 50)
    .sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))
    .slice(0, 3)
    .map((r) => {
      const sign = r.delta > 0 ? '+' : '';
      return `${r.category} (${sign}${BudgetRebalancer.formatCurrency(r.delta)})`;
    });

  const message = isSavingPrompt
    ? `For ${coupleNames} in ${city}, I recalculated a streamlined plan trimming ${focusLabel} while strictly honoring your locked categories and keeping total allocations at ${BudgetRebalancer.formatCurrency(proposal.totalProposed)} / ${BudgetRebalancer.formatCurrency(proposal.totalBudget)}.`
    : `I optimized your ${BudgetRebalancer.formatCurrency(proposal.totalBudget)} wedding plan for ${coupleNames} in ${city} to prioritize ${focusLabel} without exceeding your budget. ${
        scoredVendors.length > 0
          ? `I also matched ${scoredVendors.length} verified Utah County vendor${scoredVendors.length > 1 ? 's' : ''} aligned with your ${wedding?.styleTags.join(' & ') ?? 'Classic'} style.`
          : 'Review the exact verified ranges below and apply them in one click.'
      }`;

  const impactSummary =
    topChanges.length > 0
      ? `Key shifts: ${topChanges.join(' • ')}. Committed deposits and locked categories remain 100% protected (Budget Health ${proposal.healthScore}/100).`
      : `Your current allocations are already well balanced around your priority ranking (Budget Health ${proposal.healthScore}/100).`;

  return {
    text: message,
    impactSummary,
    proposal,
    proposalApplied: false,
    recommendedVendors: scoredVendors
  };
}
