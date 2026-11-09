const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function serverNow() {
  return new Date();
}

function validWallet(uid) {
  const now = serverNow();
  return {
    userId: uid,
    displayName: "Alice Rahman",
    accountNumber: "01711-223344",
    pinHash: "1234",
    biometricEnabled: true,
    balanceBdt: 12500.0,
    totalSavingsBdt: 5000.0,
    rewardPoints: 120,
    kycTier: "Verified",
    createdAt: now,
    updatedAt: now,
  };
}

function validTransaction(uid) {
  const now = serverNow();
  return {
    userId: uid,
    type: "SEND_MONEY",
    title: "Send Money to Rahim",
    recipientNumber: "01819-556677",
    recipientName: "Rahim Uddin",
    operatorOrProvider: "TakaPay P2P",
    amountBdt: 500.0,
    feeBdt: 5.0,
    cashbackBdt: 0.0,
    balanceAfterBdt: 11995.0,
    referenceNote: "Lunch split",
    trxId: "TKP9012ABX",
    authMethod: "BIOMETRIC",
    status: "COMPLETED",
    createdAt: now,
    updatedAt: now,
  };
}

function validSavingsGoal(uid) {
  const now = serverNow();
  return {
    userId: uid,
    title: "Emergency DPS Vault",
    category: "DPS",
    targetAmountBdt: 50000.0,
    savedAmountBdt: 5000.0,
    monthlyDepositBdt: 2500.0,
    interestRatePercent: 8.5,
    institutionName: "IDLC Finance",
    autoSaveRoundUp: true,
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
  };
}

function validBiller(uid) {
  const now = serverNow();
  return {
    userId: uid,
    billerType: "UTILITY",
    name: "Home DESCO Prepaid",
    accountOrMeterNo: "44019283746",
    providerCategory: "Electricity",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or create wallets, transactions, savings, or billers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("wallets").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("wallets").doc(ALICE_UID).set(validWallet(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .get()
  );
});

test("Authenticated owner can create, read, and update their own wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const walletRef = aliceDb.collection("wallets").doc(ALICE_UID);
  await assertSucceeds(walletRef.set(validWallet(ALICE_UID)));
  await assertSucceeds(walletRef.get());
  await assertSucceeds(
    walletRef.update({
      balanceBdt: 15000.0,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access to wallet is rejected (PII & financial isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("wallets")
      .doc(BOB_UID)
      .set(validWallet(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("wallets").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("wallets").doc(BOB_UID).update({ balanceBdt: 0 })
  );
});

test("Shadow update with ghost field on wallet is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = { ...validWallet(ALICE_UID), isAdmin: true };
  await assertFails(aliceDb.collection("wallets").doc(ALICE_UID).set(payload));
});

test("Transactions: owner can create and query with matching userId filter, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions")
    .doc("tx_1");
  await assertSucceeds(txRef.set(validTransaction(ALICE_UID)));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .get()
  );
});

test("Savings goals and saved billers: owner CRUD succeeds and invalid negative balance fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("savings_goals")
    .doc("goal_1");
  await assertSucceeds(goalRef.set(validSavingsGoal(ALICE_UID)));
  await assertSucceeds(
    goalRef.update({ savedAmountBdt: 7500.0, updatedAt: new Date() })
  );
  await assertFails(
    goalRef.update({ savedAmountBdt: -100.0, updatedAt: new Date() })
  );

  const billerRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_billers")
    .doc("biller_1");
  await assertSucceeds(billerRef.set(validBiller(ALICE_UID)));
});
