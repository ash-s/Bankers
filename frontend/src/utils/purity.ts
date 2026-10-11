// Purity presets per material (matched case-insensitively against the Material select).
const PURITY_BY_MATERIAL: Record<string, string[]> = {
  Gold: ['24K', '22K', '18K'],
  Silver: ['92.5%', '80%', '70%'],
  Platinum: ['950', '900', '850'],
};

export function purityOptions(material?: string): { value: string; label: string }[] {
  if (!material) return [];
  const match = Object.keys(PURITY_BY_MATERIAL).find(
    (k) => k.toLowerCase() === material.toLowerCase(),
  );
  // Unknown/custom materials fall back to every preset so the field is never left empty.
  const list = match ? PURITY_BY_MATERIAL[match] : Object.values(PURITY_BY_MATERIAL).flat();
  return [...new Set(list)].map((v) => ({ value: v, label: v }));
}