import { test } from 'node:test';
import assert from 'node:assert/strict';
import { gateEvent, verifyLivePr } from './policy.mjs';

const HEAD = '463ddd020118dced0124668457090a4101b2576d';
const repository = { full_name: 'Huynx10/Huyn-Laucher' };
const mainRef = 'refs/heads/main';
const run = { path: '.github/workflows/android-ci.yml', workflow_id: 393632398, id: 40996632375, run_attempt: 1, name: 'Lumia Launcher Android CI',
  conclusion: 'success', event: 'pull_request', head_sha: HEAD,
  head_repository: repository, pull_requests: [{ number: 41 }] };
const ci = { eventName: 'workflow_run', repository: repository.full_name, actor: 'Huynx10',
  ref: mainRef, payload: { repository, action: 'completed', workflow_run: run } };
const live = { number: 41, state: 'open', draft: true, head: { sha: HEAD, repo: repository },
  base: { ref: 'main' } };
const ownerComment = {
  eventName: 'issue_comment', repository: repository.full_name, actor: 'Huynx10', ref: mainRef,
  payload: { repository, sender: { login: 'Huynx10' }, action: 'created', issue: { number: 29 },
    comment: { id: 1234, user: { login: 'Huynx10' }, body: '/huyn f1-readonly' } }
};

test('exact completed PR #41 CI requires independent live SHA check', () => {
  const e = gateEvent(ci);
  assert.equal(e.kind, 'CI_F1');
  assert.equal(e.sourceSha, HEAD);
  assert.equal(e.runLocal, true);
  assert.equal(verifyLivePr({ pr: live, authorizedEvent: e }).verified, true);
  assert.equal(verifyLivePr({ pr: live, authorizedEvent: e }).requiresQA, true);
});
test('red, cancelled and early CI runs cannot dispatch', () => {
  for (const conclusion of ['failure', 'cancelled', null]) {
    assert.equal(gateEvent({ ...ci, payload: { ...ci.payload, workflow_run: { ...run, conclusion } } }).authorized, false);
  }
  assert.equal(gateEvent({ ...ci, payload: { ...ci.payload, action: 'requested' } }).authorized, false);
});
test('only the targeted PR may pass, never generic CI', () => {
  for (const number of [39, 40, 42]) {
    const e = { ...ci, payload: { ...ci.payload, workflow_run: { ...run, pull_requests: [{ number }] } } };
    assert.equal(gateEvent(e).authorized, false);
  }
  assert.equal(gateEvent({ ...ci, payload: { ...ci.payload, workflow_run: { ...run, pull_requests: [] } } }).authorized, false);
});
test('reject cross-repo and upstream CI from forks', () => {
  assert.equal(gateEvent({ ...ci, repository: 'other/repo' }).authorized, false);
  assert.equal(gateEvent({ ...ci, payload: { ...ci.payload, workflow_run: { ...run, head_repository: { full_name: 'other/repo' } } } }).authorized, false);
});
test('reject stale CI and live PR modifications', () => {
  const e = gateEvent(ci);
  assert.equal(verifyLivePr({ pr: { ...live, head: { ...live.head, sha: '0'.repeat(40) } }, authorizedEvent: e }).reason, 'stale_ci_sha');
  assert.equal(verifyLivePr({ pr: { ...live, base: { ref: 'develop' } }, authorizedEvent: e }).verified, false);
  assert.equal(verifyLivePr({ pr: { ...live, state: 'closed' }, authorizedEvent: e }).verified, false);
});
test('owner issue #29 exact bounded command accepted', () => {
  const e = gateEvent(ownerComment);
  assert.equal(e.kind, 'OWNER_F1_PROBE');
  assert.equal(e.eventKey, 'owner:1234');
  assert.equal(e.sourceSha, null);
  assert.equal(verifyLivePr({ pr: live, authorizedEvent: e }).sourceSha, HEAD);
});
test('deny arbitrary text and injected suffix commands', () => {
  for (const body of ['/huyn f1-readonly extra', '/huyn f1-readonly\nexec', '/huyn f1', ' /huyn f1-readonly']) {
    assert.equal(gateEvent({ ...ownerComment, payload: { ...ownerComment.payload, comment: { ...ownerComment.payload.comment, body } } }).authorized, false);
  }
});
test('deny forged issue, PR comments, edited comments and actor spoofing', () => {
  assert.equal(gateEvent({ ...ownerComment, actor: 'outsider' }).authorized, false);
  assert.equal(gateEvent({ ...ownerComment, payload: { ...ownerComment.payload, issue: { number: 30 } } }).authorized, false);
  assert.equal(gateEvent({ ...ownerComment, payload: { ...ownerComment.payload, issue: { number: 29, pull_request: {} } } }).authorized, false);
  assert.equal(gateEvent({ ...ownerComment, payload: { ...ownerComment.payload, action: 'edited' } }).authorized, false);
  assert.equal(gateEvent({ ...ownerComment, payload: { ...ownerComment.payload, comment: { ...ownerComment.payload.comment, user: { login: 'outsider' } } } }).authorized, false);
});
test('workflow_dispatch requires owner and one explicitly permitted task', () => {
  const input = { runId: '123', eventName: 'workflow_dispatch', repository: repository.full_name, ref: mainRef, actor: 'Huynx10',
    payload: { repository, sender: { login: 'Huynx10' }, inputs: { task: 'f1-readonly' } } };
  assert.equal(gateEvent(input).authorized, true);
  assert.equal(gateEvent({ ...input, payload: { ...input.payload, inputs: { task: 'write-code' } } }).authorized, false);
});
test('push is pilot-only and never triggers local worker', () => {
  const ref = 'refs/heads/ci/huyn-event-orchestrator-20261105';
  const input = { eventName: 'push', repository: repository.full_name, actor: 'Huynx10',
    ref, payload: { repository, ref, after: HEAD } };
  assert.equal(gateEvent(input).kind, 'PILOT_ONLY');
  assert.equal(gateEvent(input).runLocal, false);
  assert.equal(gateEvent({ ...input, actor: 'outsider' }).authorized, false);
});
test('unexpected events and nondefault ref are rejected', () => {
  assert.equal(gateEvent({ ...ci, eventName: 'issues' }).authorized, false);
  assert.equal(gateEvent({ ...ci, ref: 'refs/heads/feature' }).authorized, false);
});
test('no workflow_run event can authorize code writes or skip QA lease', () => {
  const result = verifyLivePr({ pr: live, authorizedEvent: gateEvent(ci) });
  assert.equal(result.requiresQA, true);
  assert.equal(result.requiresWriterLease, true);
  assert.equal('mayWrite' in result, false);
});

test('workflow display name alone is not trusted', () => {
  for (const patch of [{ path: '.github/workflows/other.yml' }, { workflow_id: 1 }, { path: undefined }, { workflow_id: undefined }]) {
    assert.equal(gateEvent({ ...ci, payload: { ...ci.payload, workflow_run: { ...run, ...patch } } }).authorized, false);
  }
});

 test('manual invocation uses run ID, rejects missing ID, reruns preserve identity', () => {
  const base = { runId: '123', eventName: 'workflow_dispatch', repository: repository.full_name, ref: mainRef, actor: 'Huynx10', payload: { repository, sender: { login: 'Huynx10' }, inputs: { task: 'f1-readonly' } } };
  assert.equal(gateEvent(base).eventKey, 'manual:123');
  assert.notEqual(gateEvent(base).eventKey, gateEvent({ ...base, runId: '124' }).eventKey);
  assert.equal(gateEvent({ ...base, runId: undefined }).authorized, false);
  assert.equal(gateEvent(ci).eventKey, gateEvent({ ...ci, payload: { ...ci.payload, workflow_run: { ...run, run_attempt: 2 } } }).eventKey);
});
