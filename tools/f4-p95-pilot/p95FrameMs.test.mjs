import { test } from 'node:test';
import assert from 'node:assert/strict';
import { p95FrameMs } from './p95FrameMs.mjs';
test('empty samples', () => assert.equal(p95FrameMs([]), null));
test('single sample', () => assert.equal(p95FrameMs([16]), 16));
test('p95 for twenty frames uses nearest rank', () => assert.equal(p95FrameMs([99,4,12,7,5,23,8,9,1,6,11,2,3,10,13,14,15,16,17,18]), 23));
test('invalid samples', () => { for (const v of [null, [-1], [NaN], [Infinity], [1,'2']]) assert.equal(p95FrameMs(v),null); });
test('does not mutate input', () => { const a=[16,1,9]; p95FrameMs(a); assert.deepEqual(a,[16,1,9]); });
test('rejects sparse samples', () => {
  assert.equal(p95FrameMs(new Array(3)), null);
  assert.equal(p95FrameMs([1, , 3]), null);
});
