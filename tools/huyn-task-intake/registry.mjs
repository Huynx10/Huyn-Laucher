// Trusted, bounded task intake. User comments are data, never shell instructions.
export const REPO = 'Huynx10/Huyn-Laucher';
export const OWNER = 'Huynx10';
export const ISSUE = 29;
const shaRe = /^[a-f0-9]{40}$/;
const TASKS = [
  {
    id: 'f2-qa', command: '/huyn f2-qa', mode: 'QA_ONLY',
    scope: 'drawer', branch: 'fix/f2-app-over-app-20261106',
    paths: [
      'app/src/main/java/com/example/ui/components/AppDrawer.kt',
      'app/src/main/java/com/example/ui/components/DrawerFolderMergePolicy.kt',
      'app/src/test/java/com/example/ui/components/DrawerFolderMergePolicyTest.kt'
    ]
  },
  {
    id: 'f3-readonly', command: '/huyn f3-readonly', mode: 'READ_ONLY',
    scope: 'tile-geometry', branch: 'main',
    paths: [
      'app/src/main/java/com/example/ui/components/TileView.kt',
      'app/src/main/java/com/example/ui/components/LumiaStartGrid.kt'
    ]
  }
];
export const registry = Object.freeze(TASKS.map(t => Object.freeze({...t, paths: Object.freeze(t.paths)})));

export function assertDisjointScopes(tasks = registry) {
  const seen = new Map();
  for (const task of tasks) {
    if (!/^[a-z0-9-]+$/.test(task.id) || !/^[a-z0-9-]+$/.test(task.scope))
      throw new Error('invalid_task_or_scope');
    for (const path of task.paths) {
      if (seen.has(path) && seen.get(path) !== task.scope)
        throw new Error('overlapping_write_scope:' + path);
      seen.set(path, task.scope);
    }
  }
  return true;
}

export function routeEvent({eventName, repository, actor, ref, payload}) {
  if (eventName !== 'issue_comment' || ref !== 'refs/heads/main' || repository !== REPO ||
      payload?.repository?.full_name !== REPO || actor !== OWNER ||
      payload?.sender?.login !== OWNER || payload?.action !== 'created' ||
      payload?.issue?.number !== ISSUE || payload?.issue?.pull_request ||
      payload?.comment?.user?.login !== OWNER ||
      !Number.isSafeInteger(payload?.comment?.id) || payload.comment.id <= 0)
    return {authorized:false, reason:'untrusted_or_out_of_scope'};
  const task = registry.find(t => t.command === payload.comment.body);
  if (!task) return {authorized:false, reason:'unregistered_command'};
  return {authorized:true, task, eventId:payload.comment.id};
}

export function verifyCanonicalHead(task, refData) {
  if (!registry.some(t => t.id === task?.id) || refData?.ref !== 'refs/heads/' + task.branch ||
      refData?.object?.type !== 'commit' || !shaRe.test(refData?.object?.sha ?? ''))
    throw new Error('noncanonical_or_invalid_ref');
  return refData.object.sha;
}

export function claimRef(task, sha) {
  if (!registry.some(t => t.id === task?.id) || !shaRe.test(sha ?? ''))
    throw new Error('invalid_claim_identity');
  return 'refs/heads/ci/huyn-task-claims/' + task.id + '/' + sha;
}
