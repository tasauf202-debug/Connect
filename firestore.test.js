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
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsNow() {
  return new Date(Date.now() - 1000);
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
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("broadcasts").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("app_settings").doc("moderation").get());
});

test("PII Isolation: Bob cannot read Alice's private user document, nor list users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isBanned: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Privilege Escalation Guard: Regular user cannot self-ban-bypass, add ghost field, or become admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Cannot create self as banned=true or with ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isBanned: true,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isBanned: false,
      isVerified: true, // cannot self-verify
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isBanned: false,
      ghostField: "hack", // ghost field
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isBanned: false,
      isVerified: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Regular user cannot update own isVerified to true
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: tsNow(),
    })
  );

  // Cannot write to admins collection
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      grantedBy: ALICE_UID,
      createdAt: tsNow(),
    })
  );

  // Cannot create public profile with isAdminBadge = true or isVerified = true
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      isDiscoverable: true,
      isAdminBadge: true,
      isVerified: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      isDiscoverable: true,
      isAdminBadge: false,
      isVerified: true,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Cannot spoof authorVerified on a post when not verified
  await assertFails(
    aliceDb.collection("posts").doc("post_spoof").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorVerified: true,
      content: "Fake verified post",
      category: "status",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );
});

test("Email Spoofing Test: Unverified admin email is rejected for admin operations", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: "tasauf202@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedDb.collection("broadcasts").doc("b1").set({
      title: "Fake",
      message: "Spoofed broadcast",
      priority: "urgent",
      isActive: true,
      createdBy: "spoof_uid",
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  await assertFails(
    spoofedDb.collection("app_settings").doc("moderation").set({
      allowNewPosts: false,
      allowNewChats: false,
      autoFilterEnabled: true,
      maintenanceNotice: "Hacked",
      updatedBy: "spoof_uid",
      updatedAt: tsNow(),
    })
  );
});

test("Verified Bootstrapped Admin can publish broadcasts, update moderation, and ban users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
      isBanned: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "tasauf202@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("broadcasts").doc("b_welcome").set({
      title: "স্বাগতম!",
      message: "নতুন ফিচার চালু হয়েছে।",
      priority: "important",
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  await assertSucceeds(
    adminDb.collection("app_settings").doc("moderation").set({
      allowNewPosts: true,
      allowNewChats: true,
      autoFilterEnabled: false,
      maintenanceNotice: "",
      updatedBy: ADMIN_UID,
      updatedAt: tsNow(),
    })
  );

  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      isBanned: true,
      updatedAt: tsNow(),
    })
  );

  // Admin can grant verified blue tick to Bob across users and public_profiles
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("public_profiles").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      isDiscoverable: true,
      isAdminBadge: false,
      isVerified: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    });
  });

  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      isVerified: true,
      isBanned: false,
      updatedAt: tsNow(),
    })
  );
  await assertSucceeds(
    adminDb.collection("public_profiles").doc(BOB_UID).update({
      isVerified: true,
      updatedAt: tsNow(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertSucceeds(
    bobDb.collection("posts").doc("post_verified_bob").set({
      authorId: BOB_UID,
      authorName: "Bob",
      authorVerified: true,
      content: "এখন আমি ভেরিফাইড!",
      category: "status",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );
});

test("Feed Posts, Likes atomicity, Comments, and Query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Unfiltered query on posts fails (rules are not filters)
  await assertFails(aliceDb.collection("posts").get());

  // Filtered public posts query succeeds
  await assertSucceeds(aliceDb.collection("posts").where("visibility", "==", "public").get());

  // Alice creates a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "প্রথম পোস্ট: সবার জন্য উন্মুক্ত ফিড।",
      category: "status",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Bob cannot update post likesCount without creating his like doc atomically
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: tsNow(),
    })
  );

  // Bob can atomically batch-like post_1
  const batch = bobDb.batch();
  batch.set(bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID), {
    userId: BOB_UID,
    createdAt: tsNow(),
  });
  batch.update(bobDb.collection("posts").doc("post_1"), {
    likesCount: 1,
    updatedAt: tsNow(),
  });
  await assertSucceeds(batch.commit());

  // Bob cannot edit Alice's post content
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      content: "Tampered",
      updatedAt: tsNow(),
    })
  );

  // Bob can comment on post_1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "দারুণ পোস্ট!",
      createdAt: tsNow(),
    })
  );
});

test("Chat Rooms & Messages enforce participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_999", { email: "charlie@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").set({
      title: "Alice & Bob",
      chatType: "private",
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      createdBy: ALICE_UID,
      lastMessageText: "হ্যালো!",
      lastSenderId: ALICE_UID,
      lastSenderName: "Alice",
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Bob can query chats he participates in and send a message
  await assertSucceeds(
    bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_1").collection("messages").doc("m_1").set({
      chatId: "chat_1",
      senderId: BOB_UID,
      senderName: "Bob",
      text: "কেমন আছো?",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: tsNow(),
    })
  );

  // Charlie cannot read chat_1 or write messages into it
  await assertFails(charlieDb.collection("chats").doc("chat_1").get());
  await assertFails(
    charlieDb.collection("chats").doc("chat_1").collection("messages").doc("m_2").set({
      chatId: "chat_1",
      senderId: "charlie_999",
      senderName: "Charlie",
      text: "Intruder",
      participantIds: [ALICE_UID, BOB_UID, "charlie_999"],
      createdAt: tsNow(),
    })
  );
});

test("Profile Visibility: public, friends, private modes enforce privacy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      isDiscoverable: true,
      profileVisibility: "public",
      isAdminBadge: false,
      isVerified: false,
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Invalid visibility enum is rejected
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      profileVisibility: "everyone",
      updatedAt: tsNow(),
    })
  );

  // Friends mode succeeds and Bob can read profile doc by ID
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      profileVisibility: "friends",
      isDiscoverable: false,
      updatedAt: tsNow(),
    })
  );
  await assertSucceeds(bobDb.collection("public_profiles").doc(ALICE_UID).get());

  // Private mode hides profile from Bob while Alice retains access
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      profileVisibility: "private",
      isDiscoverable: false,
      updatedAt: tsNow(),
    })
  );
  await assertFails(bobDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).get());
});
