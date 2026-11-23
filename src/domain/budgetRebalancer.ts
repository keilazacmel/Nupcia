import { BudgetAllocation, DefaultWeddingCatalog } from './models';

export interface CategoryBudgetRange {
  category: string;
  priorityRank: number;
  currentAllocated: number;
  suggestedMin: number;
  suggestedIdeal: number;
  suggestedMax: number;
  delta: number;
  locked: boolean;
}

export interface RebalanceProposal {
  totalBudget: number;
  totalSpent: number;
  totalProposed: number;
  remainingUnallocated: number;
  focusCategories: string[];
  ranges: CategoryBudgetRange[];
  healthScore: number;
  warnings: string[];
}

const BASE_SHARE_WEIGHTS: Record<string, number> = {
  Venue: 0.24,
  Catering: 0.26,
  Photography: 0.13,
  Florals: 0.11,
  Music: 0.09,
  Attire: 0.08,
  Stationery: 0.04,
  Coordination: 0.05
};

export const BudgetRebalancer = {
  roundToFifty(value: number): number {
    return Math.round(value / 50) * 50;
  },

  formatCurrency(amount: number): string {
    const rounded = Math.round(amount);
    return '$' + rounded.toLocaleString('en-US');
  },

  computeInitialAllocations(totalBudget: number, priorityOrder: string[]): BudgetAllocation[] {
    const categories = priorityOrder.length > 0 ? priorityOrder : DefaultWeddingCatalog.CATEGORIES;
    const n = Math.max(1, categories.length);
    const rawWeights = categories.map((cat, index) => {
      const base = BASE_SHARE_WEIGHTS[cat] ?? 1 / n;
      const ratio = n > 1 ? index / (n - 1) : 0;
      const multiplier = 1.42 - 0.74 * ratio;
      return { cat, weight: base * multiplier };
    });
    const weightSum = Math.max(
      0.0001,
      rawWeights.reduce((acc, r) => acc + r.weight, 0)
    );

    let runningTotal = 0;
    return rawWeights.map(({ cat, weight }, index) => {
      let amount: number;
      if (index === rawWeights.length - 1) {
        amount = Math.max(0, this.roundToFifty(totalBudget - runningTotal));
      } else {
        amount = this.roundToFifty(totalBudget * (weight / weightSum));
        runningTotal += amount;
      }
      const rankLabel =
        index <= 1 ? 'High Priority' : index <= 4 ? 'Medium Priority' : 'Flexible Priority';
      return {
        id: `cat_${index + 1}`,
        category: cat,
        allocatedAmount: amount,
        spentAmount: 0,
        priorityRank: index + 1,
        locked: false,
        notes: rankLabel
      };
    });
  },

  rebalanceForCategories(
    totalBudget: number,
    priorityOrder: string[],
    currentAllocations: BudgetAllocation[],
    focusCategories: string[],
    boostPercent = 0.16
  ): RebalanceProposal {
    const safeTotal = Math.max(0, totalBudget);
    const ordered =
      currentAllocations.length > 0
        ? [...currentAllocations].sort((a, b) => a.priorityRank - b.priorityRank)
        : this.computeInitialAllocations(safeTotal, priorityOrder);

    const normalizedFocus = focusCategories.map((c) => c.trim()).filter(Boolean);
    const lockedSum = ordered
      .filter((a) => a.locked)
      .reduce((acc, a) => acc + Math.max(a.allocatedAmount, a.spentAmount), 0);
    const unlocked = ordered.filter((a) => !a.locked);
    const availableForUnlocked = Math.max(0, safeTotal - lockedSum);

    const warnings: string[] = [];
    const totalSpent = ordered.reduce((acc, a) => acc + a.spentAmount, 0);
    if (totalSpent > safeTotal) {
      warnings.push(
        `Committed spending exceeds your total budget by ${this.formatCurrency(totalSpent - safeTotal)}.`
      );
    }

    const minFloorsSum = unlocked.reduce((acc, a) => acc + a.spentAmount, 0);
    const redistributableHeadroom = Math.max(0, availableForUnlocked - minFloorsSum);
    const n = Math.max(1, ordered.length);

    const desiredWeights: Record<string, number> = {};
    let weightTotal = 0;
    for (const alloc of unlocked) {
      const rankIdx = Math.min(n - 1, Math.max(0, alloc.priorityRank - 1));
      const base = BASE_SHARE_WEIGHTS[alloc.category] ?? 1 / n;
      const priorityFactor = 1.45 - 0.75 * (rankIdx / Math.max(1, n - 1));
      const isFocus = normalizedFocus.some(
        (f) => f.toLowerCase() === alloc.category.toLowerCase()
      );
      const focusFactor = isFocus ? 1 + boostPercent : 1;
      const w = base * priorityFactor * focusFactor;
      desiredWeights[alloc.category] = w;
      weightTotal += w;
    }
    weightTotal = Math.max(0.0001, weightTotal);

    let allocatedSum = lockedSum;
    const proposalsMap: Record<string, number> = {};

    ordered
      .filter((a) => a.locked)
      .forEach((a) => {
        proposalsMap[a.category] = Math.max(a.allocatedAmount, a.spentAmount);
      });

    unlocked.forEach((alloc, idx) => {
      const floor = alloc.spentAmount;
      const share =
        idx === unlocked.length - 1
          ? Math.max(0, safeTotal - allocatedSum - floor)
          : this.roundToFifty(
              redistributableHeadroom * ((desiredWeights[alloc.category] ?? 0) / weightTotal)
            );
      const proposed = Math.min(
        Math.max(floor, floor + share),
        Math.max(floor, safeTotal - allocatedSum)
      );
      const finalAmount = Math.max(floor, this.roundToFifty(proposed));
      proposalsMap[alloc.category] = finalAmount;
      allocatedSum += finalAmount;
    });

    const sumNow = Object.values(proposalsMap).reduce((a, b) => a + b, 0);
    if (sumNow > safeTotal && unlocked.length > 0) {
      const excess = sumNow - safeTotal;
      let donor = unlocked[0];
      let bestSlack = (proposalsMap[donor.category] ?? 0) - donor.spentAmount;
      for (const u of unlocked) {
        const slack = (proposalsMap[u.category] ?? 0) - u.spentAmount;
        if (slack > bestSlack) {
          bestSlack = slack;
          donor = u;
        }
      }
      proposalsMap[donor.category] = Math.max(
        donor.spentAmount,
        (proposalsMap[donor.category] ?? 0) - excess
      );
    }

    const ranges: CategoryBudgetRange[] = ordered.map((alloc) => {
      const ideal = proposalsMap[alloc.category] ?? alloc.allocatedAmount;
      const minVal = Math.max(alloc.spentAmount, this.roundToFifty(ideal * 0.85));
      const maxVal = Math.min(safeTotal, this.roundToFifty(ideal * 1.15));
      return {
        category: alloc.category,
        priorityRank: alloc.priorityRank,
        currentAllocated: alloc.allocatedAmount,
        suggestedMin: minVal,
        suggestedIdeal: ideal,
        suggestedMax: maxVal,
        delta: ideal - alloc.allocatedAmount,
        locked: alloc.locked
      };
    });

    const finalTotal = ranges.reduce((acc, r) => acc + r.suggestedIdeal, 0);
    const health = this.computeBudgetHealthScore(safeTotal, ordered);
    if (finalTotal > safeTotal) {
      warnings.push('Locked categories prevent reducing the total budget further.');
    }

    return {
      totalBudget: safeTotal,
      totalSpent,
      totalProposed: finalTotal,
      remainingUnallocated: Math.max(0, safeTotal - finalTotal),
      focusCategories: normalizedFocus,
      ranges,
      healthScore: health,
      warnings
    };
  },

  computeBudgetHealthScore(totalBudget: number, allocations: BudgetAllocation[]): number {
    if (totalBudget <= 0 || allocations.length === 0) return 65;
    const allocated = allocations.reduce((acc, a) => acc + a.allocatedAmount, 0);
    const spent = allocations.reduce((acc, a) => acc + a.spentAmount, 0);
    let score = 100;
    if (allocated > totalBudget * 1.02) score -= 25;
    if (spent > totalBudget) score -= 35;
    const overCategories = allocations.filter(
      (a) => a.spentAmount > a.allocatedAmount && a.allocatedAmount > 0
    ).length;
    score -= overCategories * 10;
    if (allocated < totalBudget * 0.8) score -= 8;
    return Math.min(100, Math.max(10, score));
  }
};
