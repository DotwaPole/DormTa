/**
 * DormTa! — Mock dorm listings for Cebu City
 * Structure compatible with Firebase Firestore
 * 
 */

let DORMS = [];


/** School options for dropdowns and explorer (fullName for badge display) */
const SCHOOLS = [
  { 
    id: "SWU", 
    name: "SWU", 
    fullName: "SWU Southwestern University", 
    keywords: ["Aznar", "Urgelio", "Sambag"],
    coords: [10.30662, 123.89035] 
  },
];

/** Budget ranges (label, min, max in pesos) */
const BUDGET_RANGES = [
  { id: "under3", label: "Under ₱3,000", min: 0, max: 2999 },
  { id: "3k5k", label: "₱3,000 – ₱5,000", min: 3000, max: 5000 },
  { id: "5k7k", label: "₱5,000 – ₱7,500", min: 5001, max: 7500 },
  { id: "7k10k", label: "₱7,500 – ₱10,000", min: 7501, max: 10000 },
  { id: "over10k", label: "Over ₱10,000", min: 10001, max: Infinity },
];

/**
 * Calculates the Haversine distance between two points in km
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function getDormsBySchool(schoolId) {
  if (!schoolId) return DORMS;
  return DORMS.filter((d) => d.school === schoolId);
}

function getDormsByBudget(dorms, budgetId) {
  if (!budgetId) return dorms;
  const range = BUDGET_RANGES.find((r) => r.id === budgetId);
  if (!range) return dorms;
  return dorms.filter((d) => {
    const price = Number(d.price) || 0;
    return price >= range.min && price <= range.max;
  });
}

function getDormById(id) {
  const target = String(id);
  // Check Firebase populated state first
  if (window.DORMS_STATE && window.DORMS_STATE.length > 0) {
    const found = window.DORMS_STATE.find((d) => String(d.id) === target);
    if (found) return found;
  }
  // Fallback to mock data
  return DORMS.find((d) => String(d.id) === target);
}

function updateDormsData(newData) {
  DORMS = newData;
}

window.updateDormsData = updateDormsData;
window.calculateDistance = calculateDistance; // Export to window
window.calculateDistance = calculateDistance; // Export to window
