import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, mkdtempSync, rmSync, existsSync, readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
const yaml = readFileSync(new URL('../../.github/workflows/huyn-event-gate.yml', import.meta.url), 'utf8');
const block = yaml.split('# BEGIN PERSISTENT READINESS CLAIM (tested directly from this workflow)\n')[1].split('# END PERSISTENT READINESS CLAIM')[0].replace(/^          /gm, '');
function execute(dir, key = 'owner:123', tail = 'echo executed >> "$RUNNER_TOOL_CACHE/executions"', sha = 'a'.repeat(40)) {
  return new Promise((resolve, reject) => {
    const child = spawn('bash', ['-c', 'set -euo pipefail\n' + block + '\n' + tail], { env: { ...process.env, RUNNER_TOOL_CACHE: dir, APPROVED_KEY: key, APPROVED_SHA: sha }, stdio: 'ignore' });
    child.on('error', reject); child.on('exit', code => resolve(code));
  });
}
function fixture(t) { const dir = mkdtempSync(join(tmpdir(), 'huyn-claim-test-')); t.after(() => rmSync(dir, { recursive: true, force: true })); return dir; }
test('concurrent copies claim exactly once; completed rerun does not execute', async t => {
  const dir = fixture(t);
  const codes = await Promise.all(Array.from({ length: 8 }, () => execute(dir)));
  assert.ok(codes.every(code => code === 0));
  assert.equal(readFileSync(join(dir, 'executions'), 'utf8'), 'executed\n');
  assert.equal(await execute(dir), 0);
  assert.equal(readFileSync(join(dir, 'executions'), 'utf8'), 'executed\n');
});
test('interrupted claim stays blocked; no automatic expiry/retry', async t => {
  const dir = fixture(t);
  assert.equal(await execute(dir, 'owner:123', 'exit 7'), 7);
  assert.equal(await execute(dir), 0);
  assert.equal(existsSync(join(dir, 'executions')), false);
  assert.equal(readdirSync(join(dir, 'huyn-readiness-claims-v1')).length, 1);
});
test('distinct manual invocation and changed SHA have separate claims', async t => {
  const dir = fixture(t);
  await execute(dir, 'manual:1'); await execute(dir, 'manual:2');
  await execute(dir, 'manual:2', undefined, 'b'.repeat(40));
  assert.equal(readFileSync(join(dir, 'executions'), 'utf8').trim().split('\n').length, 3);
});
test('invalid identity fails closed before execution', async t => {
  const dir = fixture(t);
  assert.notEqual(await execute(dir, '../../bad'), 0);
  assert.notEqual(await execute(dir, 'owner:123', undefined, 'bad'), 0);
  assert.equal(existsSync(join(dir, 'executions')), false);
});
