const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, deleteDoc, serverTimestamp, Timestamp } = require('firebase/firestore');

let env;
const thisYear = new Date().getUTCFullYear();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-friday',
    firestore: {
      host: '127.0.0.1', port: 8089,
      rules: fs.readFileSync(path.join(__dirname, '..', '..', 'firestore.rules'), 'utf8'),
    },
  });
});
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

const alice = (verified = true) =>
  env.authenticatedContext('alice', { email: 'alice@example.com', email_verified: verified }).firestore();
const bob = () =>
  env.authenticatedContext('bob', { email: 'bob@example.com', email_verified: true }).firestore();
const anon = () => env.unauthenticatedContext().firestore();

const profile = (over = {}) => ({
  name: 'Alice', yearOfBirth: thisYear - 30, email: 'alice@example.com', emailVerified: true,
  createdAt: serverTimestamp(), ...over,
});

test('a signed-in user can create and read their own profile', async () => {
  const db = alice();
  await assertSucceeds(setDoc(doc(db, 'users/alice'), profile()));
  await assertSucceeds(getDoc(doc(db, 'users/alice')));
});

test('nobody can read or write another user profile', async () => {
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), profile()));
  await assertFails(getDoc(doc(bob(), 'users/alice')));
  await assertFails(setDoc(doc(bob(), 'users/alice'), profile()));
  await assertFails(deleteDoc(doc(bob(), 'users/alice')));
  await assertFails(setDoc(doc(alice(), 'users/bob'), profile({ email: 'bob@example.com' })));
});

test('a signed-out visitor can do nothing', async () => {
  await assertFails(getDoc(doc(anon(), 'users/alice')));
  await assertFails(setDoc(doc(anon(), 'users/alice'), profile()));
});

test('a profile under 18 is refused, and so is a year that could still be 17', async () => {
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ yearOfBirth: thisYear - 10 })));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ yearOfBirth: thisYear - 18 })));
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), profile({ yearOfBirth: thisYear - 19 })));
});

test('the email must match the sign-in and be verified', async () => {
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ email: 'other@example.com' })));
  await assertFails(setDoc(doc(alice(false), 'users/alice'), profile()));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ emailVerified: false })));
});

test('unknown fields, a bad name and a wrong year type are refused', async () => {
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ plan: 'pro' })));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ name: '   ' })));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ name: 'x'.repeat(41) })));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ yearOfBirth: '1990' })));
});

test('createdAt cannot be faked on create or changed on update', async () => {
  const fake = Timestamp.fromDate(new Date('2020-01-01'));
  await assertFails(setDoc(doc(alice(), 'users/alice'), profile({ createdAt: fake })));
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), profile()));
  const snap = await getDoc(doc(alice(), 'users/alice'));
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), { ...snap.data(), name: 'Alice K' }));
  await assertFails(setDoc(doc(alice(), 'users/alice'), { ...snap.data(), createdAt: fake }));
});

test('the owner can delete their profile', async () => {
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), profile()));
  await assertSucceeds(deleteDoc(doc(alice(), 'users/alice')));
  const snap = await getDoc(doc(alice(), 'users/alice'));
  assert.strictEqual(snap.exists(), false);
});

test('entitlements can be read by the owner and never written from the app', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'entitlements/alice'), { plan: 'trial' });
  });
  await assertSucceeds(getDoc(doc(alice(), 'entitlements/alice')));
  await assertFails(getDoc(doc(bob(), 'entitlements/alice')));
  await assertFails(setDoc(doc(alice(), 'entitlements/alice'), { plan: 'pro' }));
  await assertFails(deleteDoc(doc(alice(), 'entitlements/alice')));
});

test('every other path is closed', async () => {
  await assertFails(setDoc(doc(alice(), 'misc/x'), { a: 1 }));
  await assertFails(getDoc(doc(alice(), 'misc/x')));
});
