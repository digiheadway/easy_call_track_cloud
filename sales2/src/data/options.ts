// === Interface ===
export interface Option {
  value: string;
  label: string;
  apiValue?: string;
  searchable?: boolean;
}

// === Helper to create uniform options ===
const createOption = (value: string): Option => ({
  value,
  label: value,
  apiValue: value,
});

// === Reusable Persons ===
const personOptions: Option[] = [
  "Yogesh",
  "Mohit",
  "Sharvan",
  "Lather Uncle",
  "Suresh",
  "Raju",
  "Rinku",
  "Pardeep",
  "Narender",
  "Other",
  "Deepak",
  "Komal",
  "Gulshan",
].map(createOption);

// === Reusable Tags ===
const commonTags: Option[] = [
  "Meeting Done",
  "Multiple Site Visit Done",
  "First Sell then buy",
  "Seller Also",
  "Must Remember",
  "Serious Buyer",
  "Trident",
  "Godrej",
  "Sigma",
  "Nysa Residential",
  "Palm Drive",
  "M3M",
  "Crown",
  "Dealer",
  "Affiliate",
  "Primary Sale Investor",
  "Urgent Sale Investor",
  "Property Dealer",
  "Need Loan",
  "VIP",
  "Investor",
  "May Be Investor",
  "Repeat",
  "International",
  "Very Less Chances",
  "Very Chances",
  "Can Extend Budget",
  "Special Case",
  "Premium",
  "Cash Buyer",
  "First Time Buyer",
  "VIP Client",
  "to delete",
].map(createOption);

// === Stage Options ===
export const stageOptions: Option[] = [
  "Init - Fresh",
  "Init - Not Connected Yet",
  "Init - General Enquiry",
  "Mid - Waiting for Match",
  "Mid - Options Ready",
  "Mat - Selected Options",
  "Mat - Awaiting Decision",
  "Mat - Exploring Other Options",
  "Mat - May Finalize",
  "Deal - In Negotiation",
  "Deal - Finalized",
  "Deal - Token Received",
  "Deal - Registry Pending",
  "Deal - Deal Closed",
  "Deal - Commission Pending",
  "Neg - Low Bids",
  "Neg - Budget Issue",
  "Neg - Unrealistic",
  "Neg - Req Closed",
  "Neg - Invalid",
  "Neg - Lost",
  "Neg - Useless",
  "Other",
].map(createOption);

// === Priority Options === (kept as is because apiValue is numeric string)
export const priorityOptions: Option[] = [
  { value: "Super High", label: "Super High", apiValue: "6" },
  { value: "High", label: "High", apiValue: "5" },
  { value: "Focus", label: "Focus", apiValue: "4" },
  { value: "General", label: "General", apiValue: "3" },
  { value: "Low", label: "Low", apiValue: "2" },
  { value: "Avoid", label: "Avoid", apiValue: "1" },
  { value: "Never Attend", label: "Never Attend", apiValue: "0" },
];

// === Simple Uniform Options ===
export const sourceOptions = [
"Organic Social Media",
"Youtube",
"Referral",
"GMB Others",
"Website Others",
"Our GMB",
"Website",
"M3M",
"Trident",
"Dealer Network",
"Social Media",
"Cold Call",
"Olx",
"Offline Meet",
"Property DB",
"Shoping",
"Other",
"Groups",
"Ads Display",
"Ads Search",
"Many Chats",
"Listings",
"Physical World",
"Holdings",
"Other Organic Display",
"Other Organic Search"

].map(createOption);

export const mediumOptions = ["Post", "Story", "Reel", "Direct", "Other"].map(
  createOption
);
export const placementOptions = ["Main Page", "Story", "Search", "Other"].map(
  createOption
);
export const nextActionOptions = [
  "Call Back",
  "Schedule Visit",
  "Follow Up",
  "Meeting",
  "Other",
].map(createOption);
export const purposeOptions = [
  "Self Use",
  "Investment",
  "Rental",
  "Resale",
  "Bussiness",
  "Other",
].map(createOption);
export const todoTypeOptions = [
  "Follow Up",
  "Meeting",
  "Find Match",
  "Schedule Site Visit",
  "Todo",
  "Activity",
  "Other",
].map(createOption);

// === Changed to uniform .map(createOption) ===
export const purchaseTimelineOptions: Option[] = [
  "ASAP",
  "1 Month",
  "3 Months",
  "6 Months",
  "1 Year",
].map(createOption);

export const intentOptions: Option[] = [
  "Zero",
  "Very Low",
  "Cold",
  "Warm",
  "Hot",
].map(createOption);

export const todoStatusOptions: Option[] = [
  "Pending",
  "Completed",
  "Cancelled",
  "Overdue",
].map(createOption);

export const segmentOptions: Option[] = [
  "Panipat",
  "Panipat Projects",
  "Panipat Investors",
  "Rohtak",
  "Sonipat",
  "M3M",
].map(createOption);

// === Tag Grouped Options ===
export const tagOptions = {
  preferredLocation: [
    "Z - Modal Town",
    "Z - Nangal Kheri",
    "Z - Sector 13 17",
    "Z - Sector 24",
    "Z - Tehsil Camp",
    "Z - Toll",
    "Projects",
    "Asandh Road",
    "Babail Naka",
    "Babail Road",
    "Court Ki Piche",
    "Devi Mandir",
    "Ekta Vihar",
    "Eldeco",
    "GT Road",
    "Insaar Bazar",
    "Jattal Road",
    "Kabdi Road",
    "Kutani Road",
    "M3M",
    "Mukhija",
    "New HB Colony",
    "Nehar Paar",
    "NFL",
    "Old Bus Stand",
    "Palm Drive",
    "RK Puram",
    "Sector 11-12",
    "Sector 18",
    "Sector 24",
    "Sector 25",
    "Sewah",
    "Sondhapur Chowk",
    "TDI Sector 23",
    "TDI Toll",
    "Tehsil Camp",
    "Ugra Khedi",
    "Ujha Road",
    "Virat Nagar",
    "Shanti nagar",
    "8 Marla",
    "Vikash Nagar",
    "Any Good Location in Panipat",
    "Anywhere in Panipat",
    "Punjabi Area",
    "Posh Area",
  ].map(createOption),

  preferredSize: [
    "50-70 Gaj",
    "80-100 Gaj",
    "100-150 Gaj",
    "150-200 Gaj",
    "200-250 Gaj",
    "250-300 Gaj",
    "300-400 Gaj",
    "400-500 Gaj",
    "500+ Gaj",
    "1-5 Kille",
    "5-10 Kille",
    "10+ Kille",
  ].map(createOption),

  propertyType: [
    "Plot Residential",
    "Shop",
    "House",
    "Labour Quater",
    "Colony",
    "Flats",
    "Agriculture Land",
    "Free Zone Land",
    "Godown",
    "Factory",
    "Big Commercial",
    "Plot Industrial",
    "Other",
  ].map(createOption),

  tags: commonTags,

  assignedTo: personOptions,

  participants: personOptions,
};

// === Helpers ===
export function getOptionByValue(
  options: Option[],
  value: string
): Option | undefined {
  return options.find((option) => option.value === value);
}

export function getOptionByApiValue(
  options: Option[],
  apiValue: string
): Option | undefined {
  return options.find((option) => option.apiValue === apiValue);
}

export function getOptionLabel(options: Option[], value: string): string {
  return getOptionByValue(options, value)?.label || value;
}

export function getApiValue(options: Option[], value: string): string {
  return getOptionByValue(options, value)?.apiValue || value;
}

export function toDropdownOptions(options: Option[]) {
  return options.map(({ value, label }) => ({ value, label }));
}

// === Master Dropdown Set ===
export const dropdownOptions = {
  stage: toDropdownOptions(stageOptions),
  priority: toDropdownOptions(priorityOptions),
  source: toDropdownOptions(sourceOptions),
  medium: toDropdownOptions(mediumOptions),
  placement: toDropdownOptions(placementOptions),
  nextAction: toDropdownOptions(nextActionOptions),
  segment: toDropdownOptions(segmentOptions),
  purpose: toDropdownOptions(purposeOptions),
  purchaseTimeline: toDropdownOptions(purchaseTimelineOptions),
  intent: toDropdownOptions(intentOptions),
  todoType: toDropdownOptions(todoTypeOptions),
  todoStatus: toDropdownOptions(todoStatusOptions),
};
