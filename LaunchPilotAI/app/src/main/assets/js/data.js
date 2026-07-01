/*
 * LaunchPilot AI — India Business Knowledge Base
 * All data below is India-specific. Costs/timeframes are approximate, general-knowledge
 * estimates meant for early planning — always confirm current figures on the official
 * portal linked with each item before relying on them.
 */
window.KB = (function () {

  var STATES = [
    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa",
    "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
    "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
    "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
    "Uttar Pradesh", "Uttarakhand", "West Bengal",
    "Andaman and Nicobar Islands", "Chandigarh",
    "Dadra and Nagar Haveli and Daman and Diu", "Delhi (NCT)", "Jammu and Kashmir",
    "Ladakh", "Lakshadweep", "Puducherry"
  ];

  var INDUSTRIES = [
    { id: "clothing", label: "Clothing & Fashion" },
    { id: "food", label: "Food & Beverage" },
    { id: "cosmetics", label: "Cosmetics & Personal Care" },
    { id: "electronics", label: "Electronics & Gadgets" },
    { id: "furniture", label: "Furniture & Home Decor" },
    { id: "jewelry", label: "Jewelry & Accessories" },
    { id: "leather", label: "Leather Goods & Footwear" },
    { id: "ceramics", label: "Ceramics & Pottery" },
    { id: "sportsgoods", label: "Sports Goods" },
    { id: "automobile", label: "Automobile & Auto Parts" },
    { id: "hometextiles", label: "Home Textiles (Bedsheets, Linen)" },
    { id: "handicrafts", label: "Handicrafts" },
    { id: "software", label: "Software / App / SaaS" },
    { id: "ecommerce", label: "E-commerce / Retail (Reselling)" },
    { id: "services", label: "Services / Consulting" },
    { id: "education", label: "Education / Coaching" },
    { id: "other", label: "Other" }
  ];

  var BUDGET_RANGES = [
    { id: "under50k", label: "Under ₹50,000", min: 0, max: 50000 },
    { id: "50k-2l", label: "₹50,000 – ₹2 Lakh", min: 50000, max: 200000 },
    { id: "2l-5l", label: "₹2 Lakh – ₹5 Lakh", min: 200000, max: 500000 },
    { id: "5l-10l", label: "₹5 Lakh – ₹10 Lakh", min: 500000, max: 1000000 },
    { id: "10l-25l", label: "₹10 Lakh – ₹25 Lakh", min: 1000000, max: 2500000 },
    { id: "above25l", label: "Above ₹25 Lakh", min: 2500000, max: 10000000 }
  ];

  var STRUCTURES = [
    { id: "individual", label: "Sole Proprietorship (Individual)" },
    { id: "partnership", label: "Partnership Firm" },
    { id: "llp", label: "LLP (Limited Liability Partnership)" },
    { id: "opc", label: "One Person Company (OPC)" },
    { id: "pvt", label: "Private Limited Company" }
  ];

  // ---------------------------------------------------------------------
  // Checklist categories & items
  // Each item: id, title, why, cost, time, documents[], whereToApply,
  // officialUrl, steps[], tags[]  (tags drive whether it's included for a
  // given business profile — see app.js -> generateChecklist)
  // tags: "always", "food", "export", "offlineStore", "manufacturing",
  //       "structure:individual" etc., "hiring"
  // ---------------------------------------------------------------------
  var CHECKLIST_CATEGORIES = [
    {
      id: "legal", name: "Legal & Compliance", icon: "⚖️",
      items: [
        {
          id: "reg-individual", tags: ["structure:individual"],
          title: "Register as a Sole Proprietorship",
          why: "The simplest, cheapest way to start a business in India as an individual. Your GST certificate or Udyam registration itself acts as proof of a proprietorship — no separate incorporation needed.",
          cost: "₹0 – ₹2,000 (mostly government fees for GST/Udyam)",
          time: "1 – 7 days",
          documents: ["PAN card", "Aadhaar card", "Address proof", "Bank account"],
          whereToApply: "No separate registrar — established via GST/Udyam/Shop Act registration",
          officialUrl: "https://udyamregistration.gov.in/",
          steps: [
            "Decide a business name (check it isn't trademarked by someone else)",
            "Open a current bank account in the business name",
            "Apply for Udyam (MSME) registration using Aadhaar",
            "Apply for GST registration if turnover will cross the threshold or you sell B2B"
          ]
        },
        {
          id: "reg-partnership", tags: ["structure:partnership"],
          title: "Register a Partnership Firm",
          why: "Formalizes the agreement between partners and gives the firm legal standing to open bank accounts, sign contracts, and sue/be sued in the firm's name.",
          cost: "₹2,000 – ₹10,000 (stamp duty + registration, varies by state)",
          time: "7 – 15 days",
          documents: ["Partnership deed", "PAN of all partners", "Address proof", "Proof of business address"],
          whereToApply: "Registrar of Firms (state government) where the business is located",
          officialUrl: "https://www.india.gov.in/topics/law-justice",
          steps: [
            "Draft a Partnership Deed with a lawyer (profit sharing, roles, capital)",
            "Get the deed notarized / registered with the Registrar of Firms",
            "Apply for PAN of the firm",
            "Open a current bank account and complete GST/Udyam registration"
          ]
        },
        {
          id: "reg-llp", tags: ["structure:llp"],
          title: "Register an LLP (Limited Liability Partnership)",
          why: "Gives partners limited liability protection (personal assets are safe) while keeping compliance lighter than a Private Limited Company.",
          cost: "₹4,000 – ₹10,000 (government + professional fees)",
          time: "7 – 15 days",
          documents: ["DSC (Digital Signature) for partners", "DIN/DPIN", "LLP Agreement", "Address proof", "PAN of partners"],
          whereToApply: "Ministry of Corporate Affairs (MCA) — LLP registration via the MCA portal",
          officialUrl: "https://www.mca.gov.in/",
          steps: [
            "Obtain Digital Signature Certificates (DSC) for all designated partners",
            "Reserve an LLP name via the RUN-LLP service on the MCA portal",
            "File incorporation form FiLLiP with the LLP Agreement",
            "Get the Certificate of Incorporation and apply for PAN/TAN"
          ]
        },
        {
          id: "reg-opc", tags: ["structure:opc"],
          title: "Register a One Person Company (OPC)",
          why: "Lets a single founder get a company structure with limited liability and a separate legal identity, without needing a co-founder.",
          cost: "₹7,000 – ₹15,000 (government + professional fees)",
          time: "10 – 15 days",
          documents: ["DSC", "DIN", "Nominee consent (Form INC-3)", "Address proof", "PAN & Aadhaar"],
          whereToApply: "Ministry of Corporate Affairs (MCA) portal",
          officialUrl: "https://www.mca.gov.in/",
          steps: [
            "Get a Digital Signature Certificate (DSC) for the sole member/director",
            "Reserve a company name via SPICe+ Part A",
            "File SPICe+ Part B with MOA, AOA and nominee details",
            "Receive Certificate of Incorporation, PAN and TAN"
          ]
        },
        {
          id: "reg-pvt", tags: ["structure:pvt"],
          title: "Register a Private Limited Company",
          why: "Preferred structure for startups planning to raise funding, hire a team, or scale quickly — gives limited liability and a credible legal identity for investors and large B2B clients.",
          cost: "₹6,000 – ₹20,000 (government + professional fees, varies by state & authorised capital)",
          time: "10 – 20 days",
          documents: ["DSC for directors", "DIN", "MOA & AOA", "Registered office proof", "PAN & Aadhaar of directors"],
          whereToApply: "Ministry of Corporate Affairs (MCA) via the SPICe+ integrated form",
          officialUrl: "https://www.mca.gov.in/",
          steps: [
            "Get Digital Signature Certificates (DSC) for all directors",
            "Reserve the company name via SPICe+ Part A",
            "File SPICe+ Part B with MOA/AOA, registered office and director details",
            "Receive Certificate of Incorporation, PAN, TAN and open a current account"
          ]
        },
        {
          id: "gst-registration", tags: ["always"],
          title: "GST Registration",
          why: "Mandatory once turnover crosses ₹40 lakh (goods) / ₹20 lakh (services) in most states, and required by most marketplaces, B2B buyers and courier partners even below that. Lets you legally collect and claim GST.",
          cost: "₹0 (government fee is free; ₹500–2,000 if you use a CA/agent)",
          time: "3 – 7 working days",
          documents: ["PAN", "Aadhaar", "Business address proof", "Bank account details", "Photograph"],
          whereToApply: "GST portal",
          officialUrl: "https://www.gst.gov.in/",
          steps: [
            "Create an account on the GST portal with PAN & mobile/email OTP",
            "Fill Part A & Part B of the GST REG-01 form",
            "Upload address proof, bank details and photograph",
            "Receive ARN, then GSTIN after verification"
          ]
        },
        {
          id: "udyam-msme", tags: ["always"],
          title: "MSME (Udyam) Registration",
          why: "Free government registration that unlocks collateral-free loans, subsidies, delayed-payment protection from buyers, and priority in government tenders.",
          cost: "₹0 (100% free, government portal)",
          time: "Same day (instant certificate)",
          documents: ["Aadhaar of proprietor/partner/director", "PAN of business", "Bank account details"],
          whereToApply: "Udyam Registration Portal (Ministry of MSME)",
          officialUrl: "https://udyamregistration.gov.in/",
          steps: [
            "Visit the Udyam Registration portal",
            "Enter Aadhaar number and validate with OTP",
            "Fill business details, PAN and investment/turnover figures",
            "Submit to get your Udyam Registration Certificate instantly"
          ]
        },
        {
          id: "trademark", tags: ["always"],
          title: "Trademark Registration (Brand Name & Logo)",
          why: "Protects your brand name/logo from being copied and gives you legal ownership nationwide — very important before you spend on branding, packaging and marketing.",
          cost: "₹4,500 (individual/MSME, per class, government fee) + ₹2,000–10,000 professional fee if using an agent",
          time: "Filing: same day. Full registration: 8 – 24 months (contestable if opposed)",
          documents: ["Brand name/logo", "Applicant ID proof", "Udyam certificate (for MSME discount)", "Power of Attorney (if using an agent)"],
          whereToApply: "Office of the Controller General of Patents, Designs & Trademarks (IP India)",
          officialUrl: "https://ipindia.gov.in/",
          steps: [
            "Run a trademark search on the IP India portal to check availability",
            "File the TM-A application under the correct class (e.g. Class 25 for apparel)",
            "Respond to any Examination Report objections if raised",
            "Once published & unopposed, get the Registration Certificate"
          ]
        },
        {
          id: "brand-name-domain-check", tags: ["always"],
          title: "Brand Name & Domain Availability Check",
          why: "Avoid falling in love with a name that's already trademarked or has no matching domain/social handles available — do this before you invest in branding.",
          cost: "₹0 (just your time) + domain cost later (₹300–1,500/yr)",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "IP India public search + any domain registrar (GoDaddy, Namecheap, BigRock)",
          officialUrl: "https://ipindia.gov.in/",
          steps: [
            "Search the name on the IP India Trademark Public Search tool",
            "Check .com / .in domain availability",
            "Check Instagram/Facebook/YouTube handle availability",
            "Shortlist 2-3 backup names in case your first choice is taken"
          ]
        },
        {
          id: "shops-establishment", tags: ["offlineStore"],
          title: "Shops & Establishment Registration",
          why: "Legally required if you operate from a physical shop, office, or warehouse — regulates working hours, employee conditions and is often needed to open a bank account or apply for other licenses.",
          cost: "₹500 – ₹5,000 (varies by state and number of employees)",
          time: "3 – 10 days",
          documents: ["Address proof of premises", "PAN & Aadhaar", "Rent agreement (if rented)", "Photographs of premises"],
          whereToApply: "State Labour Department / local municipal corporation (varies by state)",
          officialUrl: "https://www.india.gov.in/topics/law-justice",
          steps: [
            "Check your state's Shops & Establishment portal (many states now do this online)",
            "Submit premises details, employee count and working hours",
            "Pay the applicable fee",
            "Receive registration certificate — display it at your premises"
          ]
        },
        {
          id: "fssai", tags: ["food"],
          title: "FSSAI License / Registration",
          why: "Mandatory for anyone manufacturing, packaging, or selling food products in India — the tier (Basic/State/Central) depends on your turnover and scale.",
          cost: "₹100/year (Basic, turnover < ₹12L) to ₹7,500/year (Central license)",
          time: "7 – 30 days depending on tier",
          documents: ["ID & address proof", "Passport photo", "Premises proof", "Food safety management plan (for State/Central)"],
          whereToApply: "FSSAI (Food Safety and Standards Authority of India)",
          officialUrl: "https://fssai.gov.in/",
          steps: [
            "Determine your license tier based on annual turnover",
            "Register on the FoSCoS portal",
            "Upload required documents and pay the fee",
            "Receive your FSSAI license/registration number — print it on packaging"
          ]
        },
        {
          id: "iec", tags: ["export"],
          title: "Import Export Code (IEC)",
          why: "Mandatory 10-digit code needed to legally import or export goods/services from India — required by customs and banks for any cross-border shipment or payment.",
          cost: "₹500 (government fee, one-time, lifetime validity)",
          time: "1 – 2 working days",
          documents: ["PAN of business/individual", "Bank account & cancelled cheque", "Address proof"],
          whereToApply: "Directorate General of Foreign Trade (DGFT)",
          officialUrl: "https://www.dgft.gov.in/",
          steps: [
            "Register on the DGFT portal with PAN",
            "Fill the online IEC application (ANF 2A)",
            "Upload bank certificate/cancelled cheque and address proof",
            "Pay the fee and download your IEC certificate"
          ]
        },
        {
          id: "startup-india-recognition", tags: ["always"],
          title: "Startup India (DPIIT) Recognition",
          why: "Free recognition that unlocks tax exemptions, easier compliance, access to the Seed Fund Scheme, and priority in government tenders — worth applying for even for very early-stage businesses.",
          cost: "₹0 (free)",
          time: "Same day to a few days",
          documents: ["Certificate of incorporation/registration", "Business description", "PAN"],
          whereToApply: "Startup India portal (DPIIT, Ministry of Commerce & Industry)",
          officialUrl: "https://www.startupindia.gov.in/",
          steps: [
            "Create a profile on the Startup India portal",
            "Fill in entity details and a short write-up of your innovation/business",
            "Submit for DPIIT recognition",
            "Use your recognition number to apply for tax benefits & the Seed Fund"
          ]
        },
        {
          id: "gem-registration", tags: ["always"],
          title: "GeM Registration (optional — for government sales)",
          why: "If you want to sell to government departments/PSUs, GeM (Government e-Marketplace) is the only channel — optional, but a large, steady demand source once you're established.",
          cost: "₹0 (free registration)",
          time: "1 – 3 days",
          documents: ["PAN", "Udyam/GST certificate", "Bank account"],
          whereToApply: "Government e-Marketplace (GeM)",
          officialUrl: "https://gem.gov.in/",
          steps: [
            "Register as a seller on GeM with PAN and business details",
            "Complete seller KYC and add bank account",
            "List your products/services with pricing",
            "Respond to government bids/direct orders as they come"
          ]
        }
      ]
    },
    {
      id: "finance", name: "Finance & Accounting", icon: "💰",
      items: [
        {
          id: "bank-account", tags: ["always"],
          title: "Open a Current Business Bank Account",
          why: "Keeps business money separate from personal funds, required by most payment gateways/marketplaces, and builds a clean financial trail for loans later.",
          cost: "₹0 – ₹5,000 (minimum balance requirement varies by bank)",
          time: "1 – 3 days",
          documents: ["Business registration proof (GST/Udyam/COI)", "PAN", "Address proof", "Photographs"],
          whereToApply: "Any bank of your choice (compare current account plans)",
          officialUrl: "https://www.rbi.org.in/",
          steps: [
            "Compare current account fees/minimum balance across 2-3 banks",
            "Carry business registration proof, PAN and address proof",
            "Complete KYC and initial deposit",
            "Set up net banking & UPI for the business account"
          ]
        },
        {
          id: "accounting-software", tags: ["always"],
          title: "Set Up Accounting / Bookkeeping",
          why: "Clean books make GST filing, loan applications and tax season painless — start tracking from day one instead of reconstructing records later.",
          cost: "₹0 (spreadsheet) to ₹1,500–6,000/year (Vyapar/Zoho Books/Tally)",
          time: "1 – 2 days to set up",
          documents: [],
          whereToApply: "Vyapar, Zoho Books, Tally, or a local Chartered Accountant",
          officialUrl: "https://www.icai.org/",
          steps: [
            "Pick a tool sized to your volume (spreadsheet, Vyapar, Zoho Books, or Tally)",
            "Set up your chart of accounts (sales, purchases, expenses)",
            "Connect your bank feed or import statements",
            "Reconcile monthly and keep receipts digitized"
          ]
        },
        {
          id: "upi-business", tags: ["always"],
          title: "Set Up Business UPI / Bharat QR",
          why: "Lets customers pay you instantly in offline and online settings — practically expected by Indian customers today.",
          cost: "₹0 (free to set up, standard UPI has no merchant fee for most volumes)",
          time: "Same day",
          documents: ["Business bank account", "PAN"],
          whereToApply: "Your bank's UPI merchant service or apps like Razorpay/PhonePe Business/Paytm for Business",
          officialUrl: "https://www.npci.org.in/what-we-do/upi/product-overview",
          steps: [
            "Choose a UPI merchant app linked to your business account",
            "Complete merchant KYC",
            "Generate your UPI QR code",
            "Display it at your store/on invoices"
          ]
        },
        {
          id: "payment-gateway", tags: ["always"],
          title: "Integrate a Payment Gateway",
          why: "Needed to accept card, UPI, net-banking and wallet payments on your website — essential for any online sales.",
          cost: "₹0 setup + ~2% transaction fee (Razorpay/PayU/Cashfree, varies by plan)",
          time: "1 – 5 days (KYC approval)",
          documents: ["Business PAN", "GST/Udyam certificate", "Bank account", "Website URL"],
          whereToApply: "Razorpay, PayU, Cashfree, Instamojo (India-focused gateways)",
          officialUrl: "https://razorpay.com/",
          steps: [
            "Sign up and complete KYC with business documents",
            "Get API keys/plugin for your website platform",
            "Test a sandbox transaction",
            "Go live and reconcile settlements with your accounting"
          ]
        },
        {
          id: "invoice-system", tags: ["always"],
          title: "GST-Compliant Invoice Template",
          why: "GST law requires specific fields (GSTIN, HSN/SAC code, tax breakup) on every invoice — get this right from your very first sale.",
          cost: "₹0 (template) to ₹1,000/year (invoicing software)",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "Use the in-app Documents & Templates tool, or invoicing software (Zoho Invoice/Vyapar)",
          officialUrl: "https://www.gst.gov.in/",
          steps: [
            "Download the GST invoice template from Documents & Templates",
            "Fill in your GSTIN, HSN/SAC codes and tax rates",
            "Number invoices sequentially per financial year",
            "Store copies for at least 6 years as required by law"
          ]
        },
        {
          id: "pricing-calculator", tags: ["always"],
          title: "Work Out Product Pricing & Margins",
          why: "Underpricing is the #1 reason new Indian D2C brands run out of cash — factor in COGS, packaging, shipping, platform fees, marketing and your margin before you launch.",
          cost: "₹0",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "Use the in-app Cost Estimator tool",
          officialUrl: "",
          steps: [
            "List cost per unit: raw material + manufacturing + packaging",
            "Add logistics, marketplace commission & payment gateway fees",
            "Add your target margin",
            "Compare the resulting price against competitors"
          ]
        },
        {
          id: "break-even-calculator", tags: ["always"],
          title: "Calculate Your Break-Even Point",
          why: "Tells you exactly how many units/sales you need each month to stop losing money — critical for realistic planning.",
          cost: "₹0",
          time: "30 – 60 minutes",
          documents: [],
          whereToApply: "Use the in-app Cost Estimator tool",
          officialUrl: "",
          steps: [
            "Add up your fixed monthly costs (rent, salaries, subscriptions)",
            "Note your contribution margin per unit (price minus variable cost)",
            "Divide fixed costs by contribution margin per unit",
            "Track actual sales against this number every month"
          ]
        },
        {
          id: "gst-filing-setup", tags: ["always"],
          title: "Plan Your GST Return Filing",
          why: "Missing GST return deadlines attracts late fees and interest — decide upfront whether you'll file yourself or hire a CA, and mark the recurring dates.",
          cost: "₹0 (self-filing) to ₹1,000–5,000/month (CA-assisted)",
          time: "Recurring — monthly or quarterly",
          documents: ["Sales & purchase invoices", "GSTIN login"],
          whereToApply: "GST portal, or via a Chartered Accountant",
          officialUrl: "https://www.gst.gov.in/",
          steps: [
            "Choose the QRMP scheme if turnover is under ₹5 crore (quarterly filing)",
            "Set calendar reminders for GSTR-1 and GSTR-3B due dates",
            "Reconcile invoices with your books each period",
            "File on time to avoid late fees and interest"
          ]
        }
      ]
    },
    {
      id: "manufacturing", name: "Manufacturing & Suppliers", icon: "🏭",
      items: [
        {
          id: "find-manufacturer", tags: ["manufacturing"],
          title: "Find & Shortlist Manufacturers",
          why: "Your manufacturer determines quality, cost and lead time — shortlist at least 3-5 before committing, and always order samples first.",
          cost: "Varies by MOQ & product (see Manufacturer Finder tool)",
          time: "1 – 3 weeks to shortlist & finalize",
          documents: ["Product spec sheet", "Reference images/tech pack"],
          whereToApply: "Use the in-app Manufacturer Finder tool for hub cities + directories",
          officialUrl: "",
          steps: [
            "Identify the right manufacturing hub for your product category",
            "Shortlist 3-5 manufacturers via IndiaMART/TradeIndia/trade associations",
            "Compare MOQ, pricing, lead time and certifications",
            "Visit in person or request video verification before large orders"
          ]
        },
        {
          id: "sample-production", tags: ["manufacturing"],
          title: "Get Samples Made & Approved",
          why: "Never place a bulk order without approving a physical sample first — catches quality, fit and finish issues before you commit to hundreds of units.",
          cost: "₹500 – ₹5,000 per sample (varies by product)",
          time: "1 – 3 weeks",
          documents: ["Tech pack / design specs"],
          whereToApply: "Directly with your shortlisted manufacturer",
          officialUrl: "",
          steps: [
            "Share detailed specs (measurements, fabric/material, color) with the manufacturer",
            "Request 1-2 sample pieces before bulk production",
            "Test the sample yourself and get 3rd-party feedback",
            "Approve in writing before the bulk run begins"
          ]
        },
        {
          id: "quality-check", tags: ["manufacturing"],
          title: "Set Quality Control Standards",
          why: "Defines what 'acceptable' looks like before mass production starts, reducing returns and bad reviews later.",
          cost: "₹0 – ₹10,000 (self-check vs. hiring a 3rd-party QC agency)",
          time: "Ongoing per batch",
          documents: ["QC checklist"],
          whereToApply: "Self-managed, or 3rd-party QC agencies (searchable on IndiaMART)",
          officialUrl: "",
          steps: [
            "Write a simple QC checklist (stitching, color match, packaging, labeling)",
            "Do random-sample inspection on every batch",
            "Keep photographic records of approved batches",
            "Have a clear rejection/rework process agreed with your manufacturer"
          ]
        },
        {
          id: "packaging-compliance", tags: ["manufacturing"],
          title: "Finalize Packaging & Mandatory Labeling",
          why: "Indian law (Legal Metrology Act) requires specific declarations on pre-packaged goods — MRP, net quantity, manufacturer details, and country of origin.",
          cost: "₹2 – ₹20 per unit depending on packaging complexity",
          time: "1 – 2 weeks",
          documents: ["Legal Metrology declaration details"],
          whereToApply: "Legal Metrology Division (Ministry of Consumer Affairs) for compliance rules",
          officialUrl: "https://consumeraffairs.nic.in/",
          steps: [
            "Confirm mandatory declarations: MRP, net quantity, mfg details, customer care",
            "Design packaging around your brand + these mandatory fields",
            "Get a few physical proofs printed before a full print run",
            "Finalize with your packaging supplier"
          ]
        },
        {
          id: "raw-material-sourcing", tags: ["manufacturing"],
          title: "Source Raw Materials / Components",
          why: "Sourcing directly (fabric, components, ingredients) instead of through your manufacturer can cut costs significantly at scale.",
          cost: "Varies widely by material",
          time: "Ongoing",
          documents: [],
          whereToApply: "Use the in-app Supplier Finder tool",
          officialUrl: "",
          steps: [
            "Identify the specific raw material hub for your product",
            "Get quotes from 3+ suppliers",
            "Negotiate MOQ and payment terms (advance vs. credit)",
            "Test one batch before committing to a bulk contract"
          ]
        },
        {
          id: "moq-negotiation", tags: ["manufacturing"],
          title: "Negotiate MOQ & Pricing",
          why: "MOQ (Minimum Order Quantity) and per-unit pricing are almost always negotiable, especially if you commit to repeat orders.",
          cost: "₹0 (just negotiation)",
          time: "Few days to a couple of weeks",
          documents: [],
          whereToApply: "Directly with your manufacturer",
          officialUrl: "",
          steps: [
            "Ask for pricing at 2-3 different order quantities",
            "Negotiate a lower MOQ for your first trial order",
            "Clarify payment terms (advance %, balance on delivery)",
            "Get everything in a written purchase order"
          ]
        },
        {
          id: "courier-tie-up", tags: ["manufacturing", "ecommerce"],
          title: "Tie Up With a Courier / Logistics Partner",
          why: "Reliable shipping with COD support, easy returns and good tracking directly affects customer trust and repeat orders.",
          cost: "₹30 – ₹150 per shipment depending on weight/zone/COD",
          time: "1 – 3 days to onboard",
          documents: ["GST certificate", "Bank account", "Address proof"],
          whereToApply: "Shiprocket, Delhivery, Ecom Express, Blue Dart, India Post",
          officialUrl: "https://www.shiprocket.in/",
          steps: [
            "Compare 2-3 courier aggregators for your typical shipment weight/zones",
            "Register and complete KYC",
            "Test with a few sample shipments",
            "Set up COD remittance and return/RTO handling"
          ]
        },
        {
          id: "barcode-registration", tags: ["manufacturing", "ecommerce"],
          title: "Get a GS1 Barcode for Your Products",
          why: "Required by most large retailers and many marketplaces for inventory tracking — also looks more professional at retail.",
          cost: "₹4,000 – ₹30,000/year depending on number of barcodes (GS1 India membership)",
          time: "1 – 2 weeks",
          documents: ["Business registration proof", "Product list"],
          whereToApply: "GS1 India",
          officialUrl: "https://www.gs1india.org/",
          steps: [
            "Apply for GS1 India membership",
            "Get your unique GS1 company prefix",
            "Generate barcodes for each product/SKU",
            "Print barcodes on packaging or labels"
          ]
        }
      ]
    },
    {
      id: "branding", name: "Branding & Packaging", icon: "🎨",
      items: [
        {
          id: "logo-design", tags: ["always"],
          title: "Design Your Logo",
          why: "Your logo is the first visual touchpoint across packaging, website and social media — invest early since it's used everywhere.",
          cost: "₹0 (DIY/AI tools) to ₹15,000+ (professional designer)",
          time: "1 – 7 days",
          documents: [],
          whereToApply: "Use the in-app Logo Maker tool, Fiverr/Behance freelancers, or a design agency",
          officialUrl: "",
          steps: [
            "Define 2-3 words describing your brand personality",
            "Generate/sketch a few concepts",
            "Test readability at small sizes (favicon, packaging tag)",
            "Finalize with vector (.AI/.SVG) + PNG exports"
          ]
        },
        {
          id: "brand-guidelines", tags: ["always"],
          title: "Create Brand Guidelines",
          why: "A simple one-pager (colors, fonts, tone of voice) keeps everything you make — packaging, ads, website — visually consistent.",
          cost: "₹0",
          time: "2 – 4 hours",
          documents: [],
          whereToApply: "DIY using Canva or the in-app Document Generator",
          officialUrl: "",
          steps: [
            "Lock your primary + secondary brand colors (hex codes)",
            "Pick 1-2 fonts for headings/body",
            "Write 3 words describing your brand tone",
            "Save as a one-page PDF for future reference"
          ]
        },
        {
          id: "packaging-look", tags: ["manufacturing"],
          title: "Design Packaging Look & Feel",
          why: "Great unboxing experience drives repeat purchase and word-of-mouth — especially important for D2C brands sold online.",
          cost: "₹2,000 – ₹20,000 (design) + per-unit printing cost",
          time: "1 – 3 weeks",
          documents: [],
          whereToApply: "Freelance packaging designers, or your manufacturer's in-house design team",
          officialUrl: "",
          steps: [
            "Decide packaging type (box, poly bag, pouch, bottle etc.)",
            "Design around your brand guidelines + mandatory labeling",
            "Get a physical sample/proof printed",
            "Finalize and place the bulk print order"
          ]
        },
        {
          id: "product-photography", tags: ["always"],
          title: "Product Photography / Mockups",
          why: "Good product photos are the single biggest driver of online conversion — especially on Instagram and marketplaces.",
          cost: "₹2,000 – ₹15,000 (professional shoot) or ₹0 with AI mockup tools for early testing",
          time: "1 – 3 days",
          documents: [],
          whereToApply: "Use the in-app Mockup Generator, or hire a local product photographer",
          officialUrl: "",
          steps: [
            "Shoot on a plain/lifestyle background matching your brand",
            "Capture multiple angles + a size-reference/lifestyle shot",
            "Edit for consistent lighting/color across the catalog",
            "Export web-optimized sizes for your website/marketplace listings"
          ]
        },
        {
          id: "tagline-messaging", tags: ["always"],
          title: "Write Your Brand Tagline & Messaging",
          why: "A clear one-line pitch helps customers instantly understand what you sell and why it's different.",
          cost: "₹0",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "Use the in-app AI Business Mentor for suggestions",
          officialUrl: "",
          steps: [
            "Write down your product's #1 benefit for the customer",
            "Draft 3-5 tagline options",
            "Test them with 5-10 people in your target audience",
            "Pick the clearest, most memorable one"
          ]
        },
        {
          id: "social-handles", tags: ["always"],
          title: "Reserve Social Media Handles",
          why: "Secure the same username across Instagram, Facebook, YouTube etc. before someone else takes it — consistency builds trust.",
          cost: "₹0 (free)",
          time: "30 – 60 minutes",
          documents: [],
          whereToApply: "Instagram, Facebook, YouTube, X, Pinterest",
          officialUrl: "",
          steps: [
            "Check your brand name's availability across platforms",
            "Register accounts even if you won't post immediately",
            "Add a bio, logo and link to your website",
            "Turn on business/creator account features"
          ]
        }
      ]
    },
    {
      id: "website", name: "Website & Tech", icon: "💻",
      items: [
        {
          id: "domain-purchase", tags: ["always"],
          title: "Buy a Domain Name",
          why: "A professional domain (yourbrand.com / .in) makes you look established and is needed for a website and business email.",
          cost: "₹300 – ₹1,500/year",
          time: "10 minutes",
          documents: [],
          whereToApply: "GoDaddy, BigRock, Namecheap, Hostinger",
          officialUrl: "",
          steps: [
            "Search your brand name across .com/.in/.co.in",
            "Buy for at least 1-2 years (renew reminders)",
            "Enable WHOIS privacy protection",
            "Point DNS to your website/hosting once ready"
          ]
        },
        {
          id: "website-builder", tags: ["always"],
          title: "Build Your Website / Online Store",
          why: "Even if you sell mainly via marketplaces or Instagram, your own website builds credibility and is where you keep 100% of the margin (no commission).",
          cost: "₹0 (basic no-code) to ₹50,000+ (custom-built)",
          time: "3 days – 4 weeks",
          documents: [],
          whereToApply: "Use the in-app Website Builder tool, or Shopify/WooCommerce/Wix",
          officialUrl: "",
          steps: [
            "Choose a platform based on budget & tech comfort (Shopify for D2C, WooCommerce for control)",
            "Pick a theme and customize it with your branding",
            "Add product listings, pricing and policies",
            "Test checkout end-to-end before going live"
          ]
        },
        {
          id: "hosting-ssl", tags: ["always"],
          title: "Set Up Hosting & SSL",
          why: "SSL (https://) is required for customer trust and for payment gateways to work — most modern website builders include this automatically.",
          cost: "Often bundled free with website builders; ₹200–2,000/month for self-hosted",
          time: "Same day",
          documents: [],
          whereToApply: "Bundled with your website platform, or Hostinger/AWS/Vercel for custom sites",
          officialUrl: "",
          steps: [
            "Confirm your platform includes free SSL (most do by default)",
            "Point your domain's DNS to the hosting provider",
            "Verify https:// loads correctly with no warnings",
            "Set up automatic backups if self-hosting"
          ]
        },
        {
          id: "product-catalog", tags: ["always"],
          title: "Upload Your Product Catalog",
          why: "Clear titles, descriptions, pricing and photos directly affect conversion — this is your digital shopfront.",
          cost: "₹0 (your time)",
          time: "1 – 5 days depending on catalog size",
          documents: ["Product photos", "Descriptions", "Pricing", "SKU/inventory count"],
          whereToApply: "Your website + marketplace seller panels",
          officialUrl: "",
          steps: [
            "Write clear, benefit-led product titles and descriptions",
            "Upload consistent, high-quality photos per product",
            "Set accurate pricing and stock counts",
            "Organize into categories/collections for easy browsing"
          ]
        },
        {
          id: "app-analytics", tags: ["always"],
          title: "Set Up Analytics & Tracking",
          why: "You can't improve what you don't measure — know where visitors come from and where they drop off before checkout.",
          cost: "₹0 (Google Analytics & Meta Pixel are free)",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "Google Analytics, Meta Business Suite (Pixel)",
          officialUrl: "https://analytics.google.com/",
          steps: [
            "Create a Google Analytics 4 property for your site",
            "Add the tracking snippet to your website",
            "Install the Meta Pixel for ad tracking/retargeting",
            "Set up conversion goals (purchase, add-to-cart)"
          ]
        },
        {
          id: "marketplace-listing", tags: ["ecommerce", "manufacturing"],
          title: "List on Marketplaces",
          why: "Amazon, Flipkart, Myntra etc. bring built-in traffic and trust — a fast way to get your first sales while you build your own brand's audience.",
          cost: "₹0 registration; 5–25% commission per sale depending on category/platform",
          time: "3 – 10 days for seller approval",
          documents: ["GSTIN", "PAN", "Bank account", "Product catalog"],
          whereToApply: "Amazon Seller Central, Flipkart Seller Hub, Myntra Partner, Meesho",
          officialUrl: "https://sell.amazon.in/",
          steps: [
            "Choose marketplaces matching your product category & audience",
            "Register as a seller with GSTIN and bank details",
            "Upload catalog with category-compliant images/specs",
            "Set competitive pricing accounting for commission + shipping"
          ]
        }
      ]
    },
    {
      id: "marketing", name: "Marketing & Sales", icon: "📣",
      items: [
        {
          id: "marketing-plan", tags: ["always"],
          title: "Create a Marketing Plan",
          why: "A simple plan (who, where, what message, how much budget) stops marketing from becoming random, unmeasured spending.",
          cost: "₹0",
          time: "2 – 4 hours",
          documents: [],
          whereToApply: "Use the in-app AI Business Mentor + Document Generator",
          officialUrl: "",
          steps: [
            "Define your ideal customer in one sentence",
            "Pick 2 primary marketing channels to start (don't spread thin)",
            "Set a monthly budget and expected outcome",
            "Review and adjust every 2-4 weeks based on results"
          ]
        },
        {
          id: "social-content", tags: ["always"],
          title: "Plan a Social Media Content Calendar",
          why: "Consistent posting (even 3x/week) builds an audience over time and gives you a channel to launch new products to for free.",
          cost: "₹0 (organic) or with a content creator/agency",
          time: "Ongoing — 2 – 3 hours/week",
          documents: [],
          whereToApply: "Instagram, YouTube Shorts, Pinterest — plan with Canva/Notion",
          officialUrl: "",
          steps: [
            "Pick 3 content pillars (e.g. product, behind-the-scenes, customer stories)",
            "Plan a week ahead using a simple calendar",
            "Batch-shoot content when possible to save time",
            "Track which posts drive the most engagement/sales"
          ]
        },
        {
          id: "influencer-outreach", tags: ["always"],
          title: "Influencer / Micro-Influencer Outreach",
          why: "Micro-influencers (5k-100k followers) in India often deliver better ROI than large influencers for new D2C brands due to lower cost and higher trust.",
          cost: "₹500 – ₹10,000 per micro-influencer (or product barter for smaller ones)",
          time: "1 – 3 weeks to set up first collaborations",
          documents: [],
          whereToApply: "Direct Instagram outreach, or platforms like Winkl, Kofluence",
          officialUrl: "",
          steps: [
            "Shortlist 10-20 micro-influencers matching your niche & audience",
            "Send a personalized outreach message with product samples",
            "Track using unique discount codes/links",
            "Double down on the collaborations that convert"
          ]
        },
        {
          id: "paid-ads", tags: ["always"],
          title: "Set Up Meta / Google Ads",
          why: "Paid ads let you get in front of customers immediately instead of waiting for organic reach to build — start small and scale what works.",
          cost: "Start with ₹300–500/day test budget",
          time: "1 – 2 days to launch first campaign",
          documents: ["Business registration (for ad account verification, if requested)"],
          whereToApply: "Meta Ads Manager, Google Ads",
          officialUrl: "https://www.facebook.com/business/ads",
          steps: [
            "Install Meta Pixel / Google tag before spending on ads",
            "Start with a small test budget across 2-3 audiences/creatives",
            "Let it run 3-5 days before judging performance",
            "Scale budget on what's working, kill what isn't"
          ]
        },
        {
          id: "launch-offer", tags: ["always"],
          title: "Plan a Launch Offer",
          why: "A time-bound launch discount or bundle creates urgency and gives your first customers a reason to buy now instead of 'later'.",
          cost: "Cost of the discount margin given up",
          time: "1 – 2 days to plan",
          documents: [],
          whereToApply: "Your website + social channels",
          officialUrl: "",
          steps: [
            "Decide the offer (% off, bundle, free shipping)",
            "Set a clear start/end date to create urgency",
            "Promote across all channels simultaneously on launch day",
            "Track redemption to measure launch success"
          ]
        },
        {
          id: "sales-channels", tags: ["always"],
          title: "Decide Your Sales Channels",
          why: "D2C website, marketplaces, WhatsApp/Instagram DMs, and offline retail each have different economics — decide your primary channel before spreading effort thin.",
          cost: "₹0 (decision) — costs vary by channel chosen",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "",
          officialUrl: "",
          steps: [
            "List every channel available to your product category",
            "Compare margin, effort and control for each",
            "Pick 1 primary + 1 secondary channel to start",
            "Add more channels only once the first is running smoothly"
          ]
        }
      ]
    },
    {
      id: "hiring", name: "Hire & Team", icon: "🤝",
      items: [
        {
          id: "hiring-plan", tags: ["always"],
          title: "Decide What Roles You Actually Need",
          why: "Many solo founders over-hire early — figure out what can be outsourced/automated vs. what truly needs a full-time hire.",
          cost: "₹0 (planning)",
          time: "1 – 2 hours",
          documents: [],
          whereToApply: "",
          officialUrl: "",
          steps: [
            "List every task currently eating your time",
            "Mark each as 'automate', 'outsource' or 'hire'",
            "Prioritize the single highest-leverage hire/freelancer first",
            "Revisit this list every quarter as you grow"
          ]
        },
        {
          id: "offer-letter-template", tags: ["hiring"],
          title: "Prepare an Offer Letter Template",
          why: "A clear, written offer avoids disputes later and looks professional even for your very first hire.",
          cost: "₹0",
          time: "30 – 60 minutes",
          documents: [],
          whereToApply: "Use the in-app Document Generator",
          officialUrl: "",
          steps: [
            "Use the Employee Offer Letter template from Documents & Templates",
            "Fill in role, compensation, start date and reporting manager",
            "Add probation period and notice period clauses",
            "Get it signed by both parties before day one"
          ]
        },
        {
          id: "freelancer-vendors", tags: ["always"],
          title: "Onboard Freelancers / Agencies",
          why: "For design, ads, logistics and content, freelancers/agencies often get you moving faster and cheaper than a full-time hire in the early stage.",
          cost: "Varies widely by skill and scope",
          time: "3 – 7 days to onboard",
          documents: ["Scope of work / vendor agreement"],
          whereToApply: "Fiverr, Upwork, local agencies, referrals",
          officialUrl: "",
          steps: [
            "Write a clear scope of work with deliverables and deadlines",
            "Get 2-3 quotes before committing",
            "Use the in-app Vendor Agreement template",
            "Start with a small paid trial task before a larger commitment"
          ]
        },
        {
          id: "labour-compliance", tags: ["hiring"],
          title: "Labour Law Compliance (PF/ESI)",
          why: "PF (Provident Fund) becomes mandatory once you cross 20 employees, and ESI once you cross 10 in most states — plan for it as your team grows.",
          cost: "12% of basic wage each from employer & employee (PF); ~4% combined (ESI)",
          time: "Ongoing once applicable",
          documents: ["Employee details", "Establishment registration"],
          whereToApply: "Employees' Provident Fund Organisation (EPFO) / Employees' State Insurance Corporation (ESIC)",
          officialUrl: "https://www.epfindia.gov.in/",
          steps: [
            "Track your headcount against the PF (20) and ESI (10) thresholds",
            "Register on the EPFO/ESIC portal before you cross the threshold",
            "Set up monthly payroll deductions and employer contributions",
            "File monthly returns on time to avoid penalties"
          ]
        }
      ]
    },
    {
      id: "launch", name: "Launch & Beyond", icon: "🚀",
      items: [
        {
          id: "soft-launch", tags: ["always"],
          title: "Plan a Soft Launch",
          why: "Testing with friends, family or a small beta group surfaces bugs, pricing objections and packaging issues before your public launch.",
          cost: "₹0 – ₹5,000 (samples/discounts given to beta testers)",
          time: "1 – 2 weeks",
          documents: [],
          whereToApply: "",
          officialUrl: "",
          steps: [
            "Invite 10-30 people from your network to buy/try at a discount",
            "Actively collect feedback on product, pricing and delivery",
            "Fix any issues found before the public launch",
            "Ask happy beta testers for reviews/testimonials"
          ]
        },
        {
          id: "official-launch", tags: ["always"],
          title: "Official Launch Day Plan",
          why: "A coordinated launch (all channels live at once) creates more buzz than a quiet rollout — plan the exact day and hour.",
          cost: "Depends on launch marketing spend",
          time: "1 day (with 1-2 weeks of prior planning)",
          documents: [],
          whereToApply: "",
          officialUrl: "",
          steps: [
            "Pick a launch date and work backward on a checklist",
            "Prepare all content/ads/emails in advance, scheduled to go live together",
            "Brief anyone helping (support, shipping) on the expected volume",
            "Monitor the website/checkout closely on launch day"
          ]
        },
        {
          id: "feedback-loop", tags: ["always"],
          title: "Set Up a Customer Feedback Loop",
          why: "Early customer feedback is the fastest, cheapest way to improve your product-market fit — build a simple habit of asking and listening.",
          cost: "₹0",
          time: "Ongoing",
          documents: [],
          whereToApply: "WhatsApp, post-purchase email/SMS, review requests",
          officialUrl: "",
          steps: [
            "Send a simple feedback request 3-5 days after delivery",
            "Make it a single question wherever possible (e.g. rating + comment)",
            "Act publicly on recurring feedback themes",
            "Thank and reward customers who leave detailed reviews"
          ]
        },
        {
          id: "post-launch-review", tags: ["always"],
          title: "30-Day Post-Launch Review",
          why: "A structured look at what worked and what didn't in your first month prevents you from repeating the same costly mistakes.",
          cost: "₹0",
          time: "2 – 3 hours",
          documents: [],
          whereToApply: "",
          officialUrl: "",
          steps: [
            "Review sales, ad performance and customer feedback from month 1",
            "Identify your best-performing channel/product",
            "Cut or fix what clearly isn't working",
            "Set goals for month 2 based on what you learned"
          ]
        },
        {
          id: "scale-planning", tags: ["always"],
          title: "Plan Your Next Product / City / Expansion",
          why: "Once the first product/city is stable, a deliberate expansion plan (not a random one) compounds your early traction.",
          cost: "Varies",
          time: "Ongoing",
          documents: [],
          whereToApply: "Use the in-app Expansion Planner (AI Business Mentor)",
          officialUrl: "",
          steps: [
            "Identify your best-selling product/highest-demand city",
            "Decide the next logical expansion (new product line, new city, new channel)",
            "Re-run the Cost Estimator for the new scope",
            "Set a timeline and revisit your checklist for anything new required"
          ]
        }
      ]
    }
  ];

  // ---------------------------------------------------------------------
  // Manufacturing hubs — factual Indian industrial clusters (city names are
  // well-established public knowledge; contact/company-level directory data
  // is intentionally NOT fabricated — the app links out to real directories
  // like IndiaMART/TradeIndia/GeM instead).
  // ---------------------------------------------------------------------
  var MANUFACTURING_HUBS = {
    clothing: {
      label: "Apparel & Clothing",
      hubs: [
        { city: "Tiruppur, Tamil Nadu", note: "India's knitwear capital — best for T-shirts, hoodies & knitwear at scale" },
        { city: "Ludhiana, Punjab", note: "Woollens, sweaters & hosiery" },
        { city: "Delhi (Gandhi Nagar / Karol Bagh)", note: "Wholesale garments & fast turnaround fashion" },
        { city: "Ahmedabad, Gujarat", note: "Textiles, denim & bulk fabric" },
        { city: "Surat, Gujarat", note: "Synthetic & silk fabric manufacturing" }
      ],
      moq: "Typically 50 – 500 pieces per style for small manufacturers"
    },
    hometextiles: {
      label: "Home Textiles",
      hubs: [
        { city: "Panipat, Haryana", note: "Largest home-textile hub in India — bedsheets, blankets, carpets" },
        { city: "Karur, Tamil Nadu", note: "Bed linen & home textile exports" },
        { city: "Solapur, Maharashtra", note: "Towels & bedsheets" }
      ],
      moq: "Typically 100 – 1000 units depending on product"
    },
    leather: {
      label: "Leather Goods & Footwear",
      hubs: [
        { city: "Kanpur, Uttar Pradesh", note: "Leather goods & footwear manufacturing" },
        { city: "Agra, Uttar Pradesh", note: "Leather footwear" },
        { city: "Ambur/Vellore, Tamil Nadu", note: "Leather tanning & export-grade manufacturing" }
      ],
      moq: "Typically 100 – 500 pairs/pieces"
    },
    ceramics: {
      label: "Ceramics & Pottery",
      hubs: [
        { city: "Khurja, Uttar Pradesh", note: "Traditional pottery & ceramics" },
        { city: "Morbi, Gujarat", note: "India's largest ceramic tiles & sanitaryware cluster" }
      ],
      moq: "Varies widely — confirm directly with the manufacturer"
    },
    jewelry: {
      label: "Jewelry & Accessories",
      hubs: [
        { city: "Jaipur, Rajasthan", note: "Gemstones, kundan & handcrafted jewelry" },
        { city: "Surat, Gujarat", note: "Diamond cutting & polishing" },
        { city: "Mumbai (Zaveri Bazaar), Maharashtra", note: "Gold & diamond jewelry trading hub" }
      ],
      moq: "Varies — many jewelry manufacturers accept small trial orders"
    },
    furniture: {
      label: "Furniture & Home Decor",
      hubs: [
        { city: "Jodhpur, Rajasthan", note: "Wooden & handcrafted furniture, strong export base" },
        { city: "Saharanpur, Uttar Pradesh", note: "Wood carving & furniture" },
        { city: "Chennai, Tamil Nadu", note: "Furniture manufacturing & exports" }
      ],
      moq: "Often unit-based rather than bulk MOQ — confirm with manufacturer"
    },
    electronics: {
      label: "Electronics & Gadgets",
      hubs: [
        { city: "Noida / Delhi NCR", note: "Electronics assembly & components" },
        { city: "Bengaluru, Karnataka", note: "Electronics hardware & design" },
        { city: "Pune, Maharashtra", note: "Electronics manufacturing" }
      ],
      moq: "Typically higher MOQs (500+ units) — factor in tooling/mold costs for custom products"
    },
    cosmetics: {
      label: "Cosmetics & Personal Care",
      hubs: [
        { city: "Baddi, Himachal Pradesh", note: "Large cosmetics & pharma manufacturing hub with tax incentives" },
        { city: "Vapi, Gujarat", note: "Cosmetics & personal care manufacturing" },
        { city: "Mumbai, Maharashtra", note: "Cosmetics formulation & contract manufacturing" }
      ],
      moq: "Typically 500 – 5,000 units for private-label contract manufacturing"
    },
    sportsgoods: {
      label: "Sports Goods",
      hubs: [
        { city: "Jalandhar, Punjab", note: "India's best-known sports goods manufacturing hub" },
        { city: "Meerut, Uttar Pradesh", note: "Sports goods & cricket equipment" }
      ],
      moq: "Typically 100 – 1,000 units"
    },
    automobile: {
      label: "Automobile & Auto Parts",
      hubs: [
        { city: "Chennai, Tamil Nadu", note: "Known as the 'Detroit of India' — auto & auto component manufacturing" },
        { city: "Pune, Maharashtra", note: "Automobile manufacturing & ancillary industries" },
        { city: "Gurugram/Manesar, Haryana", note: "Auto components manufacturing" }
      ],
      moq: "Highly component-specific — confirm directly with manufacturer"
    },
    food: {
      label: "Food & Beverage",
      hubs: [
        { city: "Anand, Gujarat", note: "Dairy processing hub (home of the Amul cooperative model)" },
        { city: "Ahmedabad/Indore", note: "Packaged snacks & namkeen manufacturing" },
        { city: "Kochi/Guntur", note: "Spices processing & export" }
      ],
      moq: "Varies widely by product — FSSAI licensing is mandatory regardless of scale"
    },
    handicrafts: {
      label: "Handicrafts",
      hubs: [
        { city: "Jaipur, Rajasthan", note: "Block printing, handicrafts & home decor" },
        { city: "Moradabad, Uttar Pradesh", note: "Metal handicrafts — known as 'Brass City'" },
        { city: "Varanasi, Uttar Pradesh", note: "Handloom, silk & Banarasi weaving" }
      ],
      moq: "Often low MOQ, well suited to small/independent brands"
    }
  };

  var WHOLESALE_MARKETS = {
    general: ["Delhi (Sadar Bazar, Chandni Chowk)", "Mumbai (Crawford Market)", "Kolkata (Burrabazar)"],
    clothing: ["Surat, Gujarat (Textile Market)", "Delhi (Gandhi Nagar, Karol Bagh, Chandni Chowk)", "Mumbai (Mangaldas Market)", "Kolkata (Burrabazar)"],
    hometextiles: ["Panipat, Haryana", "Surat, Gujarat", "Mumbai (Mulji Jetha Market)", "Erode, Tamil Nadu"],
    jewelry: ["Mumbai (Zaveri Bazaar)", "Delhi (Dariba Kalan)", "Jaipur (Johari Bazaar)"],
    electronics: ["Delhi (Nehru Place, Bhagirath Palace)", "Mumbai (Lamington Road)"],
    ceramics: ["Morbi, Gujarat", "Khurja, Uttar Pradesh"]
  };

  var SUPPLIER_CATEGORIES = [
    { name: "Corrugated Boxes & Cartons", where: "IndiaMART, TradeIndia, local packaging clusters in your city" },
    { name: "Poly Bags & Courier Bags", where: "IndiaMART, TradeIndia" },
    { name: "Woven/Printed Labels & Tags", where: "Tiruppur & Delhi label makers, IndiaMART" },
    { name: "Buttons, Zippers & Trims", where: "Delhi (Sadar Bazar), Mumbai, IndiaMART" },
    { name: "Fabric Sourcing", where: "Surat (synthetic), Ludhiana (wool), Erode/Tiruppur (cotton knit)" },
    { name: "Printing (Screen / DTF / Sublimation)", where: "Local printing vendors, IndiaMART, Delhi/Tiruppur printing units" },
    { name: "Embroidery Units", where: "Tiruppur, Ludhiana, local embroidery job workers" }
  ];

  // ---------------------------------------------------------------------
  // Funding & Support — real, well-known Indian government schemes.
  // Figures are general-knowledge, approximate and subject to change —
  // always confirm on the official portal linked.
  // ---------------------------------------------------------------------
  var FUNDING_SCHEMES = [
    {
      id: "pmegp", type: "loans", name: "PMEGP Loan Scheme",
      summary: "Loan up to ₹50 lakh for manufacturing units and ₹20 lakh for service businesses, with 15-35% government subsidy depending on category and location.",
      url: "https://www.kviconline.gov.in/pmegpeportal/pmegphome/index.jsp"
    },
    {
      id: "mudra", type: "loans", name: "Mudra Loan (Shishu / Kishor / Tarun)",
      summary: "Collateral-free loans up to ₹10 lakh for micro/small businesses, split into Shishu (up to ₹50,000), Kishor (up to ₹5 lakh) and Tarun (up to ₹10 lakh).",
      url: "https://www.mudra.org.in/"
    },
    {
      id: "startup-seed-fund", type: "grants", name: "Startup India Seed Fund Scheme",
      summary: "Funding up to ₹20 lakh for proof of concept/prototype and up to ₹50 lakh for market entry, for DPIIT-recognized early-stage startups.",
      url: "https://www.startupindia.gov.in/"
    },
    {
      id: "standup-india", type: "loans", name: "Stand-Up India",
      summary: "Loans between ₹10 lakh – ₹1 crore for at least one woman entrepreneur and one SC/ST entrepreneur per bank branch, for greenfield enterprises.",
      url: "https://www.standupmitra.in/"
    },
    {
      id: "cgtmse", type: "schemes", name: "CGTMSE Credit Guarantee",
      summary: "Collateral-free credit guarantee cover for MSME loans up to ₹2 crore, making banks more willing to lend without collateral.",
      url: "https://www.cgtmse.in/"
    },
    {
      id: "msme-schemes", type: "schemes", name: "MSME Ministry Schemes (via Udyam)",
      summary: "Udyam-registered businesses get access to subsidized interest rates, priority sector lending, ISO certification reimbursement and delayed-payment protection.",
      url: "https://udyamregistration.gov.in/"
    },
    {
      id: "state-startup-policy", type: "schemes", name: "State Startup Policy Incentives",
      summary: "Most Indian states run their own startup policy with seed grants, subsidized incubation space and SGST reimbursement — check your state's Industries/IT Department portal.",
      url: "https://www.startupindia.gov.in/content/sih/en/reports.html"
    },
    {
      id: "sidbi", type: "loans", name: "SIDBI MSME Financing",
      summary: "Direct and indirect financing for MSMEs including working capital and term loans through the Small Industries Development Bank of India.",
      url: "https://www.sidbi.in/"
    },
    {
      id: "angel-investors-india", type: "investors", name: "Angel Networks & Incubators (India)",
      summary: "Indian Angel Network, Venture Catalysts, LetsVenture and government-recognized incubators/accelerators fund early-stage startups in exchange for equity.",
      url: "https://www.startupindia.gov.in/content/sih/en/website/find-an-incubator.html"
    },
    {
      id: "crowdfunding-india", type: "investors", name: "Crowdfunding Platforms",
      summary: "Platforms like Ketto and Wishberry let you raise smaller amounts directly from backers/customers, well suited to consumer product launches.",
      url: ""
    }
  ];

  // ---------------------------------------------------------------------
  // Country-specific engine — India-only per this app's scope
  // ---------------------------------------------------------------------
  var COUNTRY_INDIA = {
    name: "India",
    portals: [
      { label: "GST Portal", url: "https://www.gst.gov.in/" },
      { label: "Ministry of Corporate Affairs (MCA)", url: "https://www.mca.gov.in/" },
      { label: "Udyam (MSME) Registration", url: "https://udyamregistration.gov.in/" },
      { label: "Startup India", url: "https://www.startupindia.gov.in/" },
      { label: "DGFT (Import/Export)", url: "https://www.dgft.gov.in/" },
      { label: "GeM (Government e-Marketplace)", url: "https://gem.gov.in/" },
      { label: "BIS (Bureau of Indian Standards)", url: "https://www.bis.gov.in/" },
      { label: "FSSAI (Food Licensing)", url: "https://fssai.gov.in/" },
      { label: "IP India (Trademark/Patent)", url: "https://ipindia.gov.in/" }
    ]
  };

  // ---------------------------------------------------------------------
  // Cost Estimator — percentage-based split templates
  // ---------------------------------------------------------------------
  var COST_TEMPLATES = {
    manufacturing: [
      { key: "registration", label: "Business Registration & Legal", pct: 0.06 },
      { key: "samples", label: "Samples & Prototyping", pct: 0.09 },
      { key: "production", label: "Manufacturing / First Production Run", pct: 0.35 },
      { key: "packaging", label: "Packaging", pct: 0.12 },
      { key: "branding", label: "Branding & Website", pct: 0.15 },
      { key: "marketing", label: "Marketing (Initial)", pct: 0.18 },
      { key: "misc", label: "Miscellaneous / Contingency", pct: 0.05 }
    ],
    trading: [
      { key: "registration", label: "Business Registration & Legal", pct: 0.05 },
      { key: "inventory", label: "Initial Inventory Purchase", pct: 0.45 },
      { key: "packaging", label: "Packaging", pct: 0.08 },
      { key: "branding", label: "Branding & Website", pct: 0.14 },
      { key: "marketing", label: "Marketing (Initial)", pct: 0.20 },
      { key: "misc", label: "Miscellaneous / Contingency", pct: 0.08 }
    ],
    service: [
      { key: "registration", label: "Business Registration & Legal", pct: 0.08 },
      { key: "tools", label: "Tools & Software Subscriptions", pct: 0.12 },
      { key: "branding", label: "Branding & Website", pct: 0.25 },
      { key: "marketing", label: "Marketing (Initial)", pct: 0.35 },
      { key: "misc", label: "Miscellaneous / Contingency", pct: 0.20 }
    ]
  };

  // ---------------------------------------------------------------------
  // Document templates — plain text, filled in with the user's profile
  // ---------------------------------------------------------------------
  var DOCUMENT_TEMPLATES = [
    { id: "business-plan", category: "popular", title: "Business Plan Outline", format: "Text / Google Docs" },
    { id: "pitch-deck", category: "popular", title: "Pitch Deck Outline", format: "Text / Google Slides" },
    { id: "gst-invoice", category: "finance", title: "GST Invoice Template", format: "Text / Excel" },
    { id: "purchase-order", category: "popular", title: "Purchase Order Template", format: "Text / Word" },
    { id: "nda", category: "legal", title: "Non-Disclosure Agreement (NDA)", format: "Text / Google Docs" },
    { id: "manufacturer-agreement", category: "legal", title: "Manufacturer Agreement", format: "Text / Google Docs" },
    { id: "offer-letter", category: "hr", title: "Employee Offer Letter", format: "Text / Google Docs" },
    { id: "vendor-agreement", category: "legal", title: "Vendor Agreement", format: "Text / Google Docs" },
    { id: "return-policy", category: "legal", title: "Return & Refund Policy", format: "Text / Website" },
    { id: "privacy-policy", category: "legal", title: "Privacy Policy", format: "Text / Website" },
    { id: "terms", category: "legal", title: "Terms & Conditions", format: "Text / Website" }
  ];

  // ---------------------------------------------------------------------
  // Research Hub — curated official resources (no fabricated articles)
  // ---------------------------------------------------------------------
  var RESEARCH_RESOURCES = [
    { title: "Startup India — Learning & Development Hub", url: "https://www.startupindia.gov.in/", tag: "Official" },
    { title: "GST Registration & Filing Guide", url: "https://www.gst.gov.in/", tag: "Official" },
    { title: "Udyam (MSME) Registration Guide", url: "https://udyamregistration.gov.in/", tag: "Official" },
    { title: "Trademark Search & Filing (IP India)", url: "https://ipindia.gov.in/", tag: "Official" },
    { title: "FSSAI Food License Guide", url: "https://fssai.gov.in/", tag: "Official" },
    { title: "Import/Export Code (DGFT)", url: "https://www.dgft.gov.in/", tag: "Official" },
    { title: "GeM — Sell to Government", url: "https://gem.gov.in/", tag: "Official" },
    { title: "GS1 India — Barcoding Guide", url: "https://www.gs1india.org/", tag: "Official" }
  ];

  return {
    STATES: STATES,
    INDUSTRIES: INDUSTRIES,
    BUDGET_RANGES: BUDGET_RANGES,
    STRUCTURES: STRUCTURES,
    CHECKLIST_CATEGORIES: CHECKLIST_CATEGORIES,
    MANUFACTURING_HUBS: MANUFACTURING_HUBS,
    WHOLESALE_MARKETS: WHOLESALE_MARKETS,
    SUPPLIER_CATEGORIES: SUPPLIER_CATEGORIES,
    FUNDING_SCHEMES: FUNDING_SCHEMES,
    COUNTRY_INDIA: COUNTRY_INDIA,
    COST_TEMPLATES: COST_TEMPLATES,
    DOCUMENT_TEMPLATES: DOCUMENT_TEMPLATES,
    RESEARCH_RESOURCES: RESEARCH_RESOURCES
  };
})();
