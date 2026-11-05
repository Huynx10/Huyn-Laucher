import { test } from 'node:test';
import assert from 'node:assert/strict';
import { expectedSource, verifyCandidate, verifyTranscript } from './worker.mjs';
const source = 'import android.content.Intent\nif (activity?.exported != true || activity.packageName != SAMSUNG_FINDER_PACKAGE) return false\ncontext.startActivity(intent)';
test('only exact explicit copied intent accepted', () => { const s=expectedSource(source); assert.match(s,/Intent\(intent\).setComponent/); verifyCandidate(source,s); assert.throws(()=>verifyCandidate(source,s+'\nmalicious')); });
test('unsupported baseline fails closed',()=>assert.throws(()=>expectedSource('different')));
test('idempotent after hardening',()=>{const s=expectedSource(source);assert.equal(expectedSource(s),s);});
test('tool calls and model failures rejected',()=>{for(const item of ['command_execution','mcp_tool_call','file_change'])assert.throws(()=>verifyTranscript(JSON.stringify({type:'item.completed',item:{type:item}})));assert.throws(()=>verifyTranscript('{"type":"turn.failed"}'));});
test('plain model output accepted',()=>verifyTranscript('{"type":"item.completed","item":{"type":"agent_message"}}'));
