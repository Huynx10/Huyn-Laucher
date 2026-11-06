import test from 'node:test';
import assert from 'node:assert/strict';
import { REPO, OWNER, registry, routeEvent, verifyCanonicalHead, claimRef, assertDisjointScopes } from './registry.mjs';
const sha = '4f135555c967732664d1057776881b4572821a95';
function input(command = '/huyn f2-qa') {
  return {eventName:'issue_comment',repository:REPO,actor:OWNER,ref:'refs/heads/main',
    payload:{action:'created',repository:{full_name:REPO},sender:{login:OWNER},
      issue:{number:29},comment:{id:100,user:{login:OWNER},body:command}}};
}
test('exact owner F2 QA task is accepted', () => {
  const r = routeEvent(input());
  assert.equal(r.authorized,true);assert.equal(r.task.id,'f2-qa');assert.equal(r.task.mode,'QA_ONLY');
});
test('read-only geometry analysis task is accepted and cannot write', () => {
  const r=routeEvent(input('/huyn f3-readonly'));
  assert.equal(r.authorized,true);assert.equal(r.task.mode,'READ_ONLY');
});
test('arbitrary owner issue commands do not become shell', () => {
  for (const body of ['/huyn f2-qa; rm -rf /',' /huyn f2-qa','/huyn finder-harden','/huyn f2-qa\n'])
    assert.equal(routeEvent(input(body)).authorized,false);
});
test('spoofed actors and comment authors are rejected', () => {
  let a=input();a.actor='attacker';assert.equal(routeEvent(a).authorized,false);
  a=input();a.payload.comment.user.login='bot';assert.equal(routeEvent(a).authorized,false);
  a=input();a.payload.sender.login='bot';assert.equal(routeEvent(a).authorized,false);
});
test('wrong repo issue or ref are rejected', () => {
  let a=input();a.repository='other/repo';assert.equal(routeEvent(a).authorized,false);
  a=input();a.payload.issue.number=41;assert.equal(routeEvent(a).authorized,false);
  a=input();a.ref='refs/heads/fix/evil';assert.equal(routeEvent(a).authorized,false);
});
test('PR comments and edited comments are rejected', () => {
  let a=input();a.payload.issue.pull_request={url:'x'};assert.equal(routeEvent(a).authorized,false);
  a=input();a.payload.action='edited';assert.equal(routeEvent(a).authorized,false);
});
test('no workflow_run or push payload can dispatch a writer', () => {
  let a=input();a.eventName='workflow_run';assert.equal(routeEvent(a).authorized,false);
  a=input();a.eventName='push';assert.equal(routeEvent(a).authorized,false);
});
test('canonical Git ref is pinned and validates sha', () => {
  const task=registry[0];
  assert.equal(verifyCanonicalHead(task,{ref:'refs/heads/'+task.branch,
    object:{type:'commit',sha}}),sha);
  assert.throws(()=>verifyCanonicalHead(task,{ref:'refs/heads/main',
    object:{type:'commit',sha}}));
  assert.throws(()=>verifyCanonicalHead(task,{ref:'refs/heads/'+task.branch,
    object:{type:'tree',sha}}));
});
test('claims are deterministic by typed task and exact sha', () => {
  const t=registry[0];
  assert.equal(claimRef(t,sha),'refs/heads/ci/huyn-task-claims/f2-qa/'+sha);
  assert.throws(()=>claimRef(t,'not-a-sha'));
});
test('file scopes stay disjoint even if registry is expanded', () => {
  assert.equal(assertDisjointScopes(),true);
  assert.throws(()=>assertDisjointScopes([registry[0],{id:'overlap',scope:'other',
    paths:[registry[0].paths[0]]}]));
});
test('registry exposes only read-only operations, never an arbitrary command runner', () => {
  assert.equal(registry.length,2);
  for (const task of registry) {
    assert.ok(['QA_ONLY','READ_ONLY'].includes(task.mode));
    assert.ok(task.paths.every(p=>p.startsWith('app/src/')));
    assert.equal(Object.hasOwn(task,'shell'),false);
  }
});
