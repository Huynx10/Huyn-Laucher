// Huyn Launcher event policy. Treat event payloads as untrusted data, not instructions.
export const SOURCE_REPO = 'Huynx10/Huyn-Laucher';
export const OWNER = 'Huynx10';
export const F1_PR = 41;
export const F1_ISSUE = 29;
const shaPattern = /^[a-f0-9]{40}$/i;
const testBranch = 'refs/heads/ci/huyn-event-orchestrator-20261105';
const allowedWorkflowName = 'Lumia Launcher Android CI';

export function gateEvent({ eventName, repository, actor, ref, payload }) {
  if (repository !== SOURCE_REPO || !payload || payload.repository?.full_name !== SOURCE_REPO)
    return { authorized: false, reason: 'repository_mismatch' };
  if (eventName === 'push') {
    if (actor !== OWNER || ref !== testBranch || payload.ref !== testBranch)
      return { authorized: false, reason: 'pilot_push_not_authorized' };
    return { authorized: true, kind: 'PILOT_ONLY', runLocal: false, sourceSha: null, pr: null,
      eventKey: 'pilot:' + String(payload.after ?? 'unknown') };
  }
  if (ref !== 'refs/heads/main') return { authorized: false, reason: 'not_default_branch' };
  if (eventName === 'workflow_run') {
    const run = payload.workflow_run;
    const linked = run?.pull_requests;
    const number = Array.isArray(linked) && linked.length === 1 ? linked[0]?.number : null;
    if (payload.action !== 'completed' || run?.name !== allowedWorkflowName ||
        run.conclusion !== 'success' || run.event !== 'pull_request' ||
        number !== F1_PR || run.head_repository?.full_name !== SOURCE_REPO ||
        !shaPattern.test(run.head_sha ?? '') || !Number.isInteger(run.id))
      return { authorized: false, reason: 'ci_not_verified_or_out_of_scope' };
    // The hosted gate MUST fetch live PR #41 HEAD and compare exact source SHA.
    return { authorized: true, kind: 'CI_F1', runLocal: true, sourceSha: run.head_sha.toLowerCase(),
      pr: F1_PR, eventKey: 'ci:' + run.id + ':' + (run.run_attempt ?? 1) + ':' + run.head_sha.toLowerCase() };
  }
  if (eventName === 'issue_comment') {
    const c = payload.comment;
    if (actor !== OWNER || payload.sender?.login !== OWNER || payload.action !== 'created' ||
        payload.issue?.number !== F1_ISSUE || payload.issue?.pull_request ||
        c?.user?.login !== OWNER || c?.body !== '/huyn f1-readonly' || !Number.isInteger(c.id))
      return { authorized: false, reason: 'untrusted_or_out_of_scope_comment' };
    return { authorized: true, kind: 'OWNER_F1_PROBE', runLocal: true, sourceSha: null,
      pr: F1_PR, eventKey: 'owner:' + c.id };
  }
  if (eventName === 'workflow_dispatch') {
    if (actor !== OWNER || payload.sender?.login !== OWNER || payload.inputs?.task !== 'f1-readonly')
      return { authorized: false, reason: 'untrusted_manual_request' };
    return { authorized: true, kind: 'OWNER_F1_PROBE', runLocal: true, sourceSha: null,
      pr: F1_PR, eventKey: 'manual:' + String(payload.sender.id ?? OWNER) };
  }
  return { authorized: false, reason: 'unsupported_event' };
}

export function verifyLivePr({ pr, authorizedEvent }) {
  if (!authorizedEvent?.authorized || !authorizedEvent.pr || pr?.number !== authorizedEvent.pr ||
      pr.state !== 'open' || pr.head?.repo?.full_name !== SOURCE_REPO ||
      pr.base?.ref !== 'main' || !shaPattern.test(pr.head?.sha ?? ''))
    return { verified: false, reason: 'live_pr_mismatch' };
  const sourceSha = pr.head.sha.toLowerCase();
  if (authorizedEvent.sourceSha !== null && authorizedEvent.sourceSha !== sourceSha)
    return { verified: false, reason: 'stale_ci_sha', currentHead: sourceSha };
  return { verified: true, sourceSha, pr: authorizedEvent.pr, kind: authorizedEvent.kind,
    eventKey: authorizedEvent.eventKey, requiresQA: true, requiresWriterLease: true };
}
