import { test } from 'node:test';
import assert from 'node:assert/strict';
import { expectedSource, verifyCandidate, verifyTranscript, selectCanonicalFinderHead } from './worker.mjs';
const source = 'import android.content.Intent\nif (activity?.exported != true || activity.packageName != SAMSUNG_FINDER_PACKAGE) return false\ncontext.startActivity(intent)';
test('only exact explicit copied intent accepted', () => { const s=expectedSource(source); assert.match(s,/Intent\(intent\).setComponent/); verifyCandidate(source,s); assert.throws(()=>verifyCandidate(source,s+'\nmalicious')); });
test('unsupported baseline fails closed',()=>assert.throws(()=>expectedSource('different')));
test('idempotent after hardening',()=>{const s=expectedSource(source);assert.equal(expectedSource(s),s);});
test('tool calls and model failures rejected',()=>{for(const item of ['command_execution','mcp_tool_call','file_change'])assert.throws(()=>verifyTranscript(JSON.stringify({type:'item.completed',item:{type:item}})));assert.throws(()=>verifyTranscript('{"type":"turn.failed"}'));});
test('plain model output accepted',()=>verifyTranscript('{"type":"item.completed","item":{"type":"agent_message"}}'));

test('canonical live branch ref wins stale PR metadata but never authorizes a different branch', () => {
  const pr={state:'open',head:{sha:'1'.repeat(40),ref:'fix/finder-public-intent-20261030',repo:{full_name:'Huynx10/Huyn-Laucher'}},base:{ref:'main'}};
  const branchRef={ref:'refs/heads/fix/finder-public-intent-20261030',object:{type:'commit',sha:'2'.repeat(40)}};
  const accepted=selectCanonicalFinderHead(pr,branchRef);
  assert.equal(accepted.sha,'2'.repeat(40));
  assert.equal(accepted.metadataLag,true);
  assert.throws(()=>selectCanonicalFinderHead(pr,{...branchRef,ref:'refs/heads/fix/other'}));
  assert.throws(()=>selectCanonicalFinderHead({...pr,state:'closed'},branchRef));
  assert.throws(()=>selectCanonicalFinderHead({...pr,head:{...pr.head,repo:{full_name:'attacker/fork'}}},branchRef));
  assert.throws(()=>selectCanonicalFinderHead(pr,{...branchRef,object:{type:'tag',sha:'2'.repeat(40)}}));
});
