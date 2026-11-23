const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const COUPLE_UID = "couple_alice_101";
const VENDOR_UID = "vendor_marco_202";
const OTHER_UID = "other_user_303";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (offsetMs = 5000) => new Date(Date.now() - offsetMs);

function validUserPayload(uid, mode = "COUPLE") {
  const ts = pastDate(10000);
  return {
    userId: uid,
    displayName: "Camila Mendoza",
    workspaceMode: mode,
    onboardingDone: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWeddingPayload(uid) {
  const ts = pastDate(10000);
  return {
    ownerId: uid,
    brideName: "Camila",
    partnerName: "Sebastián",
    weddingDate: "2027-06-19",
    city: "Ciudad de México",
    lat: 19.4326,
    lng: -99.1332,
    totalBudget: 350000.0,
    estimatedGuests: 150,
    slug: "camila-y-sebastian",
    selectedSuiteId: "botanical_ivory",
    styleTags: ["Jardín", "Clásico"],
    colorPalette: ["#7B2D3B", "#FAF6F1", "#536B5E"],
    ceremonyType: "Jardín al aire libre",
    visionExpectations: "Una velada íntima con velas, gastronomía de autor y fotografía editorial.",
    priorityOrder: ["Lugar", "Banquete", "Fotografía", "Flores", "Música", "Vestido", "Papelería", "Otros"],
    websiteHeadline: "Nos casamos en junio",
    websiteStory: "Nuestra historia comenzó hace cinco años...",
    websiteVenueName: "Hacienda San Gabriel",
    websiteSchedule: "16:30 Ceremonia • 18:00 Cóctel • 19:30 Cena",
    websiteProtected: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validVendorPayload(ownerId, published = true) {
  const ts = pastDate(10000);
  return {
    ownerId,
    businessName: "Atelier Floral Magnolia",
    category: "Flores",
    description: "Diseño floral botánico y montajes editoriales para bodas.",
    city: "Ciudad de México",
    lat: 19.4194,
    lng: -99.1620,
    coverageRadiusKm: 120.0,
    minPrice: 28000.0,
    maxPrice: 75000.0,
    styleTags: ["Jardín", "Romántico"],
    ratingAvg: 4.9,
    completedEventsCount: 14,
    published,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validServiceRequestPayload(coupleId, vendorId, vendorOwnerId) {
  const ts = pastDate(10000);
  return {
    coupleId,
    vendorId,
    vendorOwnerId,
    coupleName: "Camila y Sebastián",
    vendorName: "Atelier Floral Magnolia",
    category: "Flores",
    catalogItemTitle: "Montaje Floral Integral",
    eventDate: "2027-06-19",
    proposedBudget: 42000.0,
    counterOfferBudget: 0.0,
    notes: "Nos encantó su estilo botánico para ceremonia en jardín.",
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(COUPLE_UID).get());
  await assertFails(unauthDb.collection("weddings").doc(COUPLE_UID).get());
  await assertFails(unauthDb.collection("vendors").where("published", "==", true).get());
  await assertFails(unauthDb.collection("serviceRequests").where("coupleId", "==", COUPLE_UID).get());
  await assertFails(unauthDb.collection("users").doc(COUPLE_UID).set(validUserPayload(COUPLE_UID)));
});

test("User profile & wedding: owner create/update succeed; shadow fields, future timestamps & cross-user access fail", async () => {
  const coupleDb = testEnv.authenticatedContext(COUPLE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertSucceeds(coupleDb.collection("users").doc(COUPLE_UID).set(validUserPayload(COUPLE_UID)));
  await assertSucceeds(
    coupleDb.collection("users").doc(COUPLE_UID).update({
      onboardingDone: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    coupleDb.collection("users").doc(COUPLE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    coupleDb.collection("users").doc(COUPLE_UID).update({
      displayName: "Future Hack",
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Reject cross-user read/write
  await assertFails(otherDb.collection("users").doc(COUPLE_UID).get());
  await assertFails(otherDb.collection("users").doc(COUPLE_UID).update({ displayName: "Hijack", updatedAt: pastDate(1000) }));

  // Wedding create & website partial update
  await assertSucceeds(coupleDb.collection("weddings").doc(COUPLE_UID).set(validWeddingPayload(COUPLE_UID)));
  await assertSucceeds(
    coupleDb.collection("weddings").doc(COUPLE_UID).update({
      websiteHeadline: "¡Celebra con nosotros!",
      websiteProtected: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(otherDb.collection("weddings").doc(COUPLE_UID).get());
});

test("Owner-scoped subcollections (budget, guests, registry, stationery, vendorTasks, calendarBlocks)", async () => {
  const coupleDb = testEnv.authenticatedContext(COUPLE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ts = pastDate(8000);

  // Budget allocation
  await assertSucceeds(
    coupleDb.collection("users").doc(COUPLE_UID).collection("budgetAllocations").doc("cat_flores").set({
      category: "Flores",
      allocatedAmount: 45000.0,
      spentAmount: 0.0,
      priorityRank: 2,
      locked: false,
      notes: "Prioridad alta",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(coupleDb.collection("users").doc(COUPLE_UID).collection("budgetAllocations").get());
  await assertFails(otherDb.collection("users").doc(COUPLE_UID).collection("budgetAllocations").get());

  // Guest (PII isolated to owner)
  await assertSucceeds(
    coupleDb.collection("users").doc(COUPLE_UID).collection("guests").doc("guest_1").set({
      householdName: "Familia López",
      fullName: "Sofía López",
      partySize: 2,
      email: "sofia@example.com",
      phone: "+52 55 1234 5678",
      postalAddress: "Av. Reforma 222, CDMX",
      rsvpStatus: "CONFIRMED",
      mealChoice: "Salmón en salsa cítrica",
      dietaryNotes: "Sin nueces",
      tableNumber: 3,
      inviteToken: "NUP-4829",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(coupleDb.collection("users").doc(COUPLE_UID).collection("guests").get());
  await assertFails(otherDb.collection("users").doc(COUPLE_UID).collection("guests").doc("guest_1").get());

  // Reject @DocumentId 'id' field in payload
  await assertFails(
    coupleDb.collection("users").doc(COUPLE_UID).collection("guests").doc("guest_bad").set({
      id: "guest_bad",
      householdName: "Bad",
      fullName: "Bad Guest",
      partySize: 1,
      email: "",
      phone: "",
      postalAddress: "",
      rsvpStatus: "PENDING",
      mealChoice: "",
      dietaryNotes: "",
      tableNumber: 0,
      inviteToken: "NUP-0001",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Registry item
  await assertSucceeds(
    coupleDb.collection("users").doc(COUPLE_UID).collection("registryItems").doc("reg_1").set({
      itemType: "CASH_FUND",
      title: "Luna de Miel en Toscana",
      description: "Cena en viñedo",
      targetAmount: 25000.0,
      collectedAmount: 8000.0,
      contributorCount: 2,
      lastContributorName: "Tía Elena",
      thankYouSent: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Stationery design
  await assertSucceeds(
    coupleDb.collection("users").doc(COUPLE_UID).collection("stationeryDesigns").doc("stat_1").set({
      suiteId: "botanical_ivory",
      productType: "INVITATION",
      headline: "Camila & Sebastián",
      subheadline: "19 de Junio, 2027",
      bodyText: "Te invitamos a celebrar nuestro amor en Hacienda San Gabriel.",
      bgColorHex: "#FAF6F1",
      accentColorHex: "#7B2D3B",
      fontStyle: "EDITORIAL_SERIF",
      bleedValid: true,
      proofApproved: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Marketplace vendors & catalog: self-get before creation succeeds; cross-user probing fails; published queries succeed; only owner mutates", async () => {
  const vendorDb = testEnv.authenticatedContext(VENDOR_UID).firestore();
  const coupleDb = testEnv.authenticatedContext(COUPLE_UID).firestore();
  const ts = pastDate(8000);

  // Regression test: owner reading non-existent vendors/{uid} (observeMyVendorProfile & saveVendorProfile pre-check) MUST succeed
  const emptySnap = await assertSucceeds(vendorDb.collection("vendors").doc(VENDOR_UID).get());
  if (emptySnap.exists) throw new Error("Expected non-existent vendor doc before creation");

  // Cross-user probing of non-existent vendor doc MUST fail
  await assertFails(coupleDb.collection("vendors").doc(VENDOR_UID).get());

  // Owner creates unpublished profile, reads it, then publishes it
  await assertSucceeds(vendorDb.collection("vendors").doc(VENDOR_UID).set(validVendorPayload(VENDOR_UID, false)));
  await assertSucceeds(vendorDb.collection("vendors").doc(VENDOR_UID).get());
  // Couple cannot read unpublished vendor profile
  await assertFails(coupleDb.collection("vendors").doc(VENDOR_UID).get());

  await assertSucceeds(
    vendorDb.collection("vendors").doc(VENDOR_UID).update({
      published: true,
      updatedAt: pastDate(1000),
    })
  );
  // Couple can read published vendor profile
  await assertSucceeds(coupleDb.collection("vendors").doc(VENDOR_UID).get());

  // Owner can also check non-existent catalog item under their own vendor doc
  await assertSucceeds(vendorDb.collection("vendors").doc(VENDOR_UID).collection("catalogItems").doc("pkg_1").get());
  await assertFails(coupleDb.collection("vendors").doc(VENDOR_UID).collection("catalogItems").doc("pkg_1").get());

  await assertSucceeds(
    vendorDb.collection("vendors").doc(VENDOR_UID).collection("catalogItems").doc("pkg_1").set({
      vendorOwnerId: VENDOR_UID,
      title: "Montaje Floral Integral",
      description: "Arco floral, ramo y 12 centros de mesa.",
      basePrice: 42000.0,
      pricingUnit: "Por evento",
      active: true,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Couple queries published vendors (matches Kotlin repository)
  await assertSucceeds(coupleDb.collection("vendors").where("published", "==", true).get());
  await assertSucceeds(
    coupleDb.collection("vendors").doc(VENDOR_UID).collection("catalogItems").where("published", "==", true).get()
  );

  // Unfiltered list on /vendors fails
  await assertFails(coupleDb.collection("vendors").get());

  // Couple cannot modify vendor profile
  await assertFails(
    coupleDb.collection("vendors").doc(VENDOR_UID).update({
      businessName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );
});

test("ServiceRequests lifecycle: create, counteroffer, accept, complete, terminal lock & isolation", async () => {
  const vendorDb = testEnv.authenticatedContext(VENDOR_UID).firestore();
  const coupleDb = testEnv.authenticatedContext(COUPLE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(vendorDb.collection("vendors").doc(VENDOR_UID).set(validVendorPayload(VENDOR_UID, true)));

  const reqRefCouple = coupleDb.collection("serviceRequests").doc("req_1");
  await assertSucceeds(reqRefCouple.set(validServiceRequestPayload(COUPLE_UID, VENDOR_UID, VENDOR_UID)));

  // Queries aligned with Kotlin repositories
  await assertSucceeds(coupleDb.collection("serviceRequests").where("coupleId", "==", COUPLE_UID).get());
  await assertSucceeds(vendorDb.collection("serviceRequests").where("vendorOwnerId", "==", VENDOR_UID).get());

  // Unfiltered query & third-party read rejected
  await assertFails(otherDb.collection("serviceRequests").get());
  await assertFails(otherDb.collection("serviceRequests").doc("req_1").get());

  // Vendor cannot skip straight from PENDING to COMPLETED
  await assertFails(
    vendorDb.collection("serviceRequests").doc("req_1").update({
      status: "COMPLETED",
      updatedAt: pastDate(1000),
    })
  );

  // Vendor accepts + atomically creates vendorTask and calendarBlock
  await assertSucceeds(
    vendorDb.collection("serviceRequests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    vendorDb.collection("users").doc(VENDOR_UID).collection("vendorTasks").doc("task_req1_1").set({
      serviceRequestId: "req_1",
      coupleName: "Camila y Sebastián",
      category: "Flores",
      title: "Confirmación de contrato y anticipo",
      description: "Enviar contrato digital y confirmar reserva.",
      dueDate: "2026-11-25",
      eventDate: "2027-06-19",
      status: "PENDING",
      milestone: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    vendorDb.collection("users").doc(VENDOR_UID).collection("calendarBlocks").doc("blk_req1").set({
      serviceRequestId: "req_1",
      title: "Boda Camila y Sebastián",
      startDate: "2027-06-19",
      endDate: "2027-06-19",
      blockType: "BOOKED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Vendor completes event -> terminal lock prevents further edits
  await assertSucceeds(
    vendorDb.collection("serviceRequests").doc("req_1").update({
      status: "COMPLETED",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    coupleDb.collection("serviceRequests").doc("req_1").update({
      status: "CANCELLED",
      updatedAt: pastDate(200),
    })
  );
});
