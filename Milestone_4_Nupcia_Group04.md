# **Milestone 4**
# **Innovation Project Meeting — Nupcia Prototype Plan**

**Date:** 11/21/2026  
**Group:** 04  
**Project Name:** **Nupcia** — Editorial Wedding Planning, AI Dynamic Budget & Geospatial Vendor Marketplace (Android MVP)

## Attendees & Team Roles

| Team Member | Primary Role | Core Responsibilities |
| :--- | :--- | :--- |
| **Keila Zacarias** | Product Lead & Core Prototype Developer | System architecture, Firebase backend setup (Firestore, Google Sign-In, App Check, Gemini AI Logic), deterministic budget engine, atomic vendor workflow, and security rules testing. |
| **Domenica Coello** | UX/UI Designer & Co-Developer | Editorial visual identity (color palette, typography, custom app icon), bridal onboarding UX, geospatial map & Event Hub interfaces, usability walkthroughs, and demo data curation. |
| **Daniela Garcia** | Presentation Strategist & QA Lead | 8-minute pitch outline, end-to-end QA script (Couple ↔ Vendor journey), speaker transition cues, live demo coordination, and rehearsal timekeeping. |
| **Aden Aldrich** | Visual Storytelling & Slide Deck Lead | Slide deck creation using Nupcia's brand system, high-resolution emulator screen captures, diagram visuals (law-firm analogy, system flow, analytics), and audience Q&A prep. |

---

# **Agenda Items**

1. **Design for America —** Reviewed key user-centered design takeaways from this week's assignment, focusing on reducing decision fatigue and budget anxiety for engaged couples while eliminating fragmented admin work for wedding vendors.
2. **Brainstorm Prototype Ideas —** Evaluated static mockups (PowerPoint/Figma) versus a functional mobile app. Decided to go beyond static wireframes and build a **working Android MVP prototype (`Nupcia`)** that demonstrates real AI recommendations, real cloud persistence, an interactive map, and live Couple-to-Vendor workflows.
3. **Choose a Prototype Idea —** Selected **Nupcia**, an integrated wedding ecosystem modeled after a full-service law firm's case-matching approach, combining intelligent budget optimization, geospatial vendor matching, a B2B vendor operations portal, and guest/stationery event tools.
4. **Make a Plan to Create the Prototype —** Defined required tools/stack, modular architecture, individual task ownership, SMART milestones, and a day-by-day timetable from build kickoff through final presentation readiness.
5. **Looking Ahead — Prototype Presentation —** Designed an organized 8-minute executive pitch (within the 5–10 minute window) including a live interactive walkthrough, creative slide deck plan, speaker assignments, backup contingency plan, and full alignment with the 35-point presentation rubric.

---

# **Notes**

## 1. Prototype Idea — Detailed Concept & Architecture

### Core Concept ("The Law Firm Model for Weddings")
Planning a wedding usually forces couples to juggle spreadsheets, disconnected vendor directories, group chats, guest lists, and stationery tools—often resulting in budget overruns and mismatched providers. Meanwhile, independent wedding vendors lose time manually tracking inquiries, calendar conflicts, and client deliverables.

**Nupcia** solves both sides of the market by working like a **modern full-service law firm**: just as a law firm evaluates your case, priorities, and budget to match you with the exact specialist attorney, **Nupcia** captures a couple's aesthetic vision, non-negotiable priorities, event location, and total budget—and dynamically pairs them with verified vendors, exact budget ranges, and a unified planning workspace.

### Prototype Format & Technical Foundation
Rather than a static slide mockup, Group 04 developed a **functional Android MVP application** built with **Kotlin, Jetpack Compose, and Material Design 3** featuring an *Editorial Bridal Atelier* aesthetic (Silk Ivory `#FAF6F1`, Deep Bordeaux `#7B2D3B`, Eucalyptus Sage `#4B6858`, Burnished Champagne `#9C762F`, paired with `Cormorant Garamond` display serif and `Plus Jakarta Sans` UI typography, plus a custom interlocking-rings adaptive launcher icon).

### Core Modules Implemented in the Prototype

1. **Multi-Role Authentication & Bridal Vision Onboarding (Module A)**
   - **Google Sign-In via Jetpack Credential Manager:** Secure one-tap authentication (`GetSignInWithGoogleOption` + silent auto sign-in) with strict per-user data isolation in Cloud Firestore.
   - **Seamless Dual-Role Workspace:** Supports both **Couple** and **Vendor** portals, with an in-app workspace switcher so users (and our live demo audience) can experience both sides of the marketplace seamlessly.
   - **3-Step Bridal Onboarding Quiz:** Captures names, wedding date, geocoded city, total budget, estimated guest count, style tags (*Jardín, Clásico, Editorial, Bohemio, Minimalista, Moderno*), color palette, ceremony type, open-ended vision expectations, and an interactive **Priority Ranking** list.
   - **Instant Visual & Financial Personalization:** Automatically matches a coordinated stationery/website suite to the bride's style and generates initial priority-weighted budget allocations in real time.

2. **AI Recommendation Chatbot with Deterministic Dynamic Budget Rebalancing (Module B)**
   - **Firebase AI Logic (`gemini-3.8-flash`) + Firebase App Check:** Contextual wedding concierge that reads live wedding data, spent amounts, priority rankings, and real published vendors from Firestore.
   - **Zero-Hallucination Deterministic Math Engine (`BudgetRebalancer`):** Financial arithmetic is never left to the LLM. Before prompting Gemini, our deterministic Kotlin engine recalculates exact `min`, `ideal`, and `max` ranges by amplifying high-priority categories, compressing flexible unlocked categories, protecting already-spent funds, and guaranteeing `sum(allocations) <= totalBudget`.
   - **Actionable Structured Cards:** Gemini returns structured JSON (`Schema.obj`) explaining financial trade-offs and renders interactive in-chat cards with one-tap buttons: **"Apply Budget Adjustment"**, **"View on Map"**, and **"Send Inquiry"**.

3. **Interactive Geospatial Map & Affinity Matching Engine (Module C)**
   - **Custom Pan/Zoom Geospatial Map Canvas:** Displays the couple's geocoded wedding venue at the center with concentric distance rings, interactive vendor pins color-coded by budget fit, and tap-to-inspect details.
   - **Haversine Distance & Coverage Validation (`GeoMatchEngine`):** Calculates exact kilometer distance from the venue to each vendor and checks whether the wedding falls within the vendor's `coverageRadiusKm`.
   - **0–100% Wedding Affinity Score:** Ranks vendors by combining aesthetic style overlap (35%), budget alignment (30%), geospatial coverage (20%), and vendor rating (15%), alongside category, budget, and coverage filters.

4. **B2B Vendor Operational Portal (Module D)**
   - **Public Business Profile & Service Catalog CRUD:** Vendors configure their category, base city/coordinates, service radius, price range, style tags, visibility toggle, and individual service packages.
   - **Atomic Request-to-Tasks & Calendar Automation (`WriteBatch`):** Vendors can **Accept**, **Counteroffer**, **Reject**, or **Mark Completed** incoming couple requests. Accepting a request executes a single atomic Firestore batch that:
     1. Updates the request status to `ACCEPTED`.
     2. Locks the event date as `BOOKED` on the vendor's unified calendar.
     3. Automatically generates **4 milestone operational tasks** (*Contract & Deposit Confirmation*, *Creative Alignment Briefing*, *Final Logistics & Venue Access*, *Event Execution & Post-Event Review*) on the vendor's interactive task board (`Pending → In Progress → Completed`).
     - If an accepted request is cancelled, pending tasks are automatically cancelled and the calendar date is released.
   - **Client Management & Completed Events Analytics:** Tracks active/past couple portfolios and computes conversion rate, accumulated revenue, pipeline revenue, and a native Compose bar chart—strictly counting only `COMPLETED` events toward "Completed Events" metrics.

5. **Event Hub & Visual Stationery Editor (Module E — Zola + The Knot Paper Inspired)**
   - **Guests, Households & Interactive Seating Chart:** Manages households, party sizes, RSVP status chips, meal choices, dietary allergy notes, shareable RSVP/address-collection invite tokens, and a table-by-table Seating Chart with 8-seat capacity alerts.
   - **Gift Registry & Cash Funds:** Supports physical gifts and cash funds (*Honeymoon, Home*) with live progress bars, contribution logging, and a Thank-You status tracker.
   - **Visual Canvas Stationery Editor:** Real-time Compose Canvas designer for *Invitations, Save the Dates, Menus, and Envelopes* with coordinated suite presets, toggleable **print bleed safe-zone guides**, live merge preview of real guest household names & postal addresses, and **Print Proof Approval**.
   - **Customizable Wedding Website:** Editable headline, couple story, venue details, event schedule, privacy toggle, and native Android share sheet for the custom wedding slug.

6. **Key Product & Engineering Optimizations Added**
   - **Wedding Readiness Index (0–100%) & "Next Best Action" Card:** Aggregates financial health, high-priority vendor bookings, RSVP progress, seating completion, and proof approval into one clear recommendation on the Home dashboard.
   - **Selective Budget Locking:** Couples can lock finalized categories so AI rebalancing only redistributes remaining unlocked funds.
   - **Zero-Trust Security & Adaptive UI:** Enforces strict Firestore security rules validated by automated local emulator tests (5/5 passing) and adapts responsively between mobile `NavigationBar` and tablet `NavigationRail`.

---

## 2. Plan to Create the Prototype

### Tools, Technologies & Supplies Required

| Category | Tools / Resources Used | Purpose in Prototype |
| :--- | :--- | :--- |
| **Mobile Frontend** | Kotlin, Jetpack Compose, Material Design 3 | Native Android UI, custom Canvas map, stationery editor, and analytics chart. |
| **Typography & Branding** | `Cormorant Garamond`, `Plus Jakarta Sans` (local `.ttf`), custom XML/PNG adaptive launcher icon | Cohesive luxury editorial visual identity across all screens and DPIs. |
| **Cloud Backend & Auth** | Firebase Firestore Enterprise, Firebase Auth, Jetpack Credential Manager (Google Sign-In) | Real-time multi-role data sync, secure OAuth login, and per-user data isolation. |
| **Generative AI** | Firebase AI Logic (`gemini-3.8-flash`), Firebase App Check | Contextual wedding concierge with structured JSON output and secure attestation. |
| **Verification & QA** | Node.js `node:test`, `@firebase/rules-unit-testing`, Firestore Local Emulator, Gradle Build | Automated verification of zero-trust Firestore rules and clean APK compilation. |
| **Presentation & Demo** | Android Streaming Emulator, Google Slides / Canva, Screen Recorder | Live interactive demonstration, slide deck visuals, and recorded backup video. |

### Creation Plan, Assigned Tasks & Timetable

| Phase | Task Description | Assigned Owner(s) | Deliverable | Target Date | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Phase 1: Concept & Design** | Finalize "law-firm matching" concept, define user stories for Couple & Vendor roles, and design editorial branding (palette, typography, app icon). | **All Members** (Lead: **Domenica**) | Product spec, brand kit, & adaptive app icon | Nov 18, 2026 | Completed |
| **Phase 2: Backend & Security** | Provision Firebase (Firestore, Google Sign-In, App Check, AI Logic), define data schema (`firebase-blueprint.json`), author zero-trust `firestore.rules`, and pass 5/5 emulator security tests. | **Keila** | Provisioned backend, verified & deployed security rules | Nov 19, 2026 | Completed |
| **Phase 3: Core Engines & Onboarding** | Implement Google Sign-In flow, 3-step Bridal Onboarding Quiz, deterministic `BudgetRebalancer`, and Haversine `GeoMatchEngine`. | **Keila** & **Domenica** | Working auth, onboarding, priority budget, and scoring engines | Nov 19, 2026 | Completed |
| **Phase 4: AI Chat, Map & Vendor Portal** | Build Gemini 3.8 Flash AI Chat with 1-tap budget apply, interactive Geospatial Map canvas, B2B Vendor Portal with atomic Request→Tasks/Calendar `WriteBatch`, and Event Hub (RSVP, Registry, Stationery, Website). | **Keila** & **Domenica** | Compiled, fully functional Nupcia Android MVP APK | Nov 20, 2026 | Completed |
| **Phase 5: QA & Demo Script** | Execute end-to-end Couple ↔ Vendor test script, populate clean demo scenario (5 local vendors + sample guests), and record a 2-minute fallback video in case of classroom Wi-Fi issues. | **Daniela** & **Keila** | QA sign-off checklist & backup demo recording | Nov 22, 2026 | In Progress |
| **Phase 6: Slide Deck & Visuals** | Capture crisp screenshots from the Android emulator, build the 9-slide presentation deck matching Nupcia's color palette, and integrate diagrams. | **Aden** & **Domenica** | Final 9-slide pitch deck ready for rehearsal | Nov 23, 2026 | In Progress |
| **Phase 7: Full Team Rehearsal** | Conduct two timed 8-minute dry runs (verifying eye contact, pacing, speaker handoffs, and live emulator demo transitions) and prepare Q&A answers. | **All Members** (Lead: **Daniela**) | Timed rehearsal log & presentation readiness sign-off | Nov 24–25, 2026 | Scheduled |
| **Phase 8: Class Pitch** | Deliver the 8-minute professional Prototype Presentation to the class and collect structured peer/instructor feedback for Phase 2 iteration. | **All Members** | Final class presentation & feedback log | Nov 26, 2026 | Scheduled |

---

## 3. Prototype Presentation Plan

### Presentation Format & Objectives
- **Total Target Time:** **8 minutes** (strictly within the 5–10 minute requirement) + 2 minutes for audience Q&A.
- **Tone & Delivery Style:** Executive startup pitch presented as if pitching to product stakeholders—confident posture, clear voice projection, eye contact, smooth handoffs between all 4 presenters, and a **live interactive walkthrough** of the real Android app on screen.

### Detailed Explanation of How the Prototype Works (Demo Architecture)
During the presentation, we will explain and demonstrate the **complete connected loop** inside Nupcia:
1. **Onboarding & Calibration:** The bride signs in with Google, enters her wedding city, budget ($350,000), styles (*Jardín, Clásico*), and drags/orders her top priorities (*Banquete, Fotografía, Flores*). Nupcia immediately computes a weighted budget and assigns a matching *Marfil Botánico* design suite.
2. **Dashboard & Readiness Optimizer:** The Couple Home screen shows the live **Readiness Score**, financial health meter, and **Next Best Action** prompt, plus individual category locks.
3. **AI Dynamic Rebalancing:** In the **Asistente IA** tab, the bride types *"Busco fotógrafo y flores dentro de mi presupuesto"*. Our deterministic engine recalculates exact ranges without exceeding $350,000, Gemini 3.8 Flash explains the trade-offs in natural language, and one tap on **"Aplicar este ajuste"** updates Firestore in real time.
4. **Geospatial Discovery & Inquiry:** Tapping **"Ver en mapa"** opens the interactive Geospatial Map showing distance in km, coverage radius validation, and a 90%+ Affinity Score. The couple taps **"Solicitar"** to submit a quote request.
5. **B2B Vendor Portal (Live Role Switch):** Switching to the **Portal de Proveedor**, the audience sees the incoming request appear immediately. Tapping **"Aceptar"** triggers an atomic batch that locks the wedding date on the calendar and auto-generates 4 milestone tasks. Marking an event **"Cumplido"** updates the **Completed Events Analytics** chart in real time.
6. **Event Hub & Stationery Proof:** Finally, we showcase the Guest/Seating Chart capacity view, Cash Funds progress, and the Canvas **Stationery Editor** merging a real guest household's postal address onto an envelope with bleed guides enabled.

### Timed Presentation Outline & Creative Slide Plan (9 Slides — 8 Minutes)

| Slide # | Slide Title | Creative Visual & Layout Plan | Key Talking Points & Demo Integration | Speaker | Time |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **1** | **Nupcia: The Law Firm Model for Weddings** | Full-bleed Silk Ivory canvas with Bordeaux/Gold interlocking rings emblem, bold `Cormorant Garamond` title, and clean group subtitle. | Hook the audience with the "law firm" analogy: matching couples to the right specialists based on their case, vision, and budget. Introduce the team and why we built Nupcia. | **Aden** | 0:45 |
| **2** | **The Problem: Fragmented Planning & Vendor Chaos** | Split-screen infographic (Couple side: spreadsheets, budget stress, endless directories vs. Vendor side: missed leads, manual tasks, double-booked dates). | Quantify the pain points discovered during Design for America research—why existing tools leave either the couple or the vendor behind. | **Aden** | 0:45 |
| **3** | **Meet Nupcia: One Connected Two-Sided Ecosystem** | Clean architectural hub diagram connecting 5 color-coded pillars (Onboarding, AI Budget, Geo Map, Vendor B2B, Event Hub) over a cloud sync core. | High-level overview of the working Android MVP we actually built (Kotlin, Jetpack Compose, Firebase, Gemini 3.8 Flash). | **Daniela** | 0:45 |
| **4** | **Personalized Bridal Onboarding & Readiness Engine** | High-res phone mockup of the 3-Step Bridal Quiz + Priority Ranking screen paired with the Readiness Score hero card. | Explain how onboarding captures aesthetic vision and priorities, pre-selects coordinated stationery suites, and calculates weighted budgets + the Readiness "Next Best Action." | **Domenica** | 1:00 |
| **5** | **AI Dynamic Budget Concierge (Deterministic + Gemini)** | Visual flow diagram: *User Prompt → Deterministic Kotlin Math Engine (No Overruns) → Gemini 3.8 Flash Explanation → 1-Tap Action Cards*. | Explain the core innovation: separating deterministic budget math from conversational AI so recommendations are accurate, locked-category safe, and directly actionable. | **Keila** | 1:00 |
| **6** | **Live Demo Part 1: Couple Journey (Onboarding → AI → Map → Request)** | Minimalist header slide ("LIVE PROTOTYPE DEMO — COUPLE PORTAL") framing the live Android emulator projection. | **Live Demo:** Show Readiness Score, ask Gemini to rebalance Photography & Florals, apply the budget in 1 tap, jump to the Geospatial Map (Haversine distance & Affinity %), and send a real vendor quote request. | **Keila** & **Domenica** | 1:45 |
| **7** | **Live Demo Part 2: Vendor Portal & Atomic Task Automation** | Minimalist header slide ("LIVE PROTOTYPE DEMO — VENDOR B2B PORTAL") framing the live emulator projection. | **Live Demo:** Switch roles to Vendor Portal, accept the live request to show the **instant calendar lock + 4 auto-generated operational tasks**, check off a task, mark completed, and show the live **Completed Events Analytics** bar chart. | **Daniela** & **Keila** | 1:15 |
| **8** | **Event Hub: RSVP, Seating, Cash Funds & Stationery Studio** | 2x2 visual showcase grid featuring emulator screenshots of the Seating Chart, Registry Cash Funds, Stationery Canvas with bleed guides + guest address merge, and Wedding Website. | Highlight how couple data flows automatically into guest address collection, table seating capacity alerts, and print-ready stationery proofs. | **Domenica** | 0:30 |
| **9** | **Roadmap, Optimizations & Feedback Ask** | Clean 3-column Phase 2 roadmap card (Real Payment Gateway, Live Map Tiles, Public Guest Web Portal) + bold closing call for class feedback. | Summarize key technical optimizations (zero-trust Firestore rules, adaptive layouts, affinity engine) and invite specific class feedback on UX and priority features. | **Aden** | 0:15 |

*Total Planned Presentation Time: **8 minutes 00 seconds** (leaves comfortable buffer inside the 5–10 minute window).*

---

# **Action Items (SMART Method)**

| # | SMART Action Item | Owner | Measurable Success Metric | Target Date | Status |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **1** | **Finalize & verify the Android MVP prototype (`Nupcia`)** including Firebase provisioning, deterministic budget rebalancing, Gemini 3.8 Flash chat, geospatial map, atomic vendor request-to-task automation, and zero-trust Firestore rules. | **Keila Zacarias** | Clean Gradle APK build (`0` errors), `5/5` passing Firestore emulator security tests, and rules deployed live. | 11/21/2026 | Completed |
| **2** | **Polish UX flows, seed demo showcase scenario, and capture high-resolution emulator visuals** covering Onboarding, Couple Dashboard, AI Chat, Map, Event Hub (Stationery + Seating), and Vendor Portal. | **Domenica Coello** | 10+ crisp emulator screenshots exported in 1080p and 5 local demo vendors + 4 sample guest households verified in-app. | 11/22/2026 | In Progress |
| **3** | **Finalize the 8-minute Presentation Outline, live demo cue sheet, and backup walkthrough recording**, and run end-to-end QA across both Couple and Vendor roles to eliminate any demo dead-ends. | **Daniela Garcia** | Complete timed script (≤ 8:00), 100% pass rate on the 6-step demo QA checklist, and a 2-minute backup screen recording saved locally. | 11/22/2026 | In Progress |
| **4** | **Design and complete the 9-slide creative Pitch Deck** aligned with Nupcia's Silk Ivory & Deep Bordeaux visual identity, integrating Domenica's screenshots, the law-firm analogy visual, and the system architecture diagram. | **Aden Aldrich** | 9 cohesive, low-text, high-readability slides reviewed and approved by all 4 group members. | 11/23/2026 | In Progress |
| **5** | **Conduct two full-team timed dress rehearsals** via Zoom/in-person to practice confident delivery, eye contact, speaker transitions, live emulator screen-sharing, and Q&A responses. | **All Members** | Two consecutive rehearsals completed between 7:30 and 8:30 minutes with zero technical hiccups. | 11/25/2026 | Scheduled |

---

# **Collaboration & Presentation Readiness Checklist**

- [x] **Clear Role Division:** Every group member owns a distinct track (Core Engineering, UX/Visuals, Presentation Strategy/QA, Slide Deck Storytelling) and has an assigned speaking segment in the live pitch.
- [x] **Working Draft Prototype Ready:** Real Android APK compiled and connected to live Firebase Firestore, Google Sign-In, and Gemini 3.8 Flash.
- [ ] **Demo Contingency Ready:** Pre-recorded backup video of the live emulator walkthrough embedded in Slides 6–7 in case of venue Wi-Fi failure.
- [ ] **Rubric Cross-Check Complete:** Confirmed all 7 presentation criteria (compelling intro, complete feature explanation, realistic live demo, clear preparation, confident delivery, creative clear slides, professional organization) are explicitly covered.

---

# **Next Meeting Agenda Items (Pre-Presentation Rehearsal — 11/24/2026)**

1. **Slide Deck Visual Audit:** Review all 9 slides for typography contrast, minimal text density, and alignment with Nupcia's editorial color palette.
2. **Timed Full-Team Dry Run #1 & #2:** Run the complete 8-minute presentation with live Android emulator screen sharing and stopwatch timing.
3. **Contingency Check:** Verify the pre-recorded demo backup plays smoothly and test Google Sign-In + Gemini response speed on classroom Wi-Fi.
4. **Feedback Collection Plan:** Prepare 3 targeted questions to ask the class after our pitch to prioritize Phase 2 improvements (e.g., Stripe Connect cash funds, interactive Mapbox/Google Maps tiles, and public guest RSVP web links).

---

# **Reference: Prototype Presentation Grading Rubric (35 Points)**

| CRITERIA | RATINGS | POINTS |
| :--- | :--- | :---: |
| The students introduce what they have created and why they created it in an appealing, professional manner. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The students clearly explain how all aspects of their prototype works. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The students demonstrate a realistic model or draft of their innovation. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The students are well prepared and have put significant effort into their prototype and presentation. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The students who speak during the presentation appear confident, maintain good eye contact, use effective body language and hand gestures, and use a strong clear voice. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The presentation uses slides that are creative, clear and easy to read and enhance the overall presentation. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| The overall presentation is professional, organized, and well-prepared. | *Leave blank. Instructors will fill this in with their comments when the activity is being graded* | 5 |
| **TOTAL POINTS** | | **35** |
