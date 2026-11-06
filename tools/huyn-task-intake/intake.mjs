import {readFileSync,writeFileSync,appendFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {REPO,routeEvent,verifyCanonicalHead,claimRef} from './registry.mjs';
function gh(args) {
  return JSON.parse(execFileSync('gh',['api',...args],{encoding:'utf8',
    env:process.env,timeout:25000}));
}
const event=JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH,'utf8'));
const routed=routeEvent({eventName:process.env.GITHUB_EVENT_NAME,
  repository:process.env.GITHUB_REPOSITORY,actor:process.env.GITHUB_ACTOR,
  ref:process.env.GITHUB_REF,payload:event});
const output=process.env.GITHUB_OUTPUT;
const summary=process.env.GITHUB_STEP_SUMMARY;
if (!routed.authorized) {
  appendFileSync(output,'approved=false\n');
  appendFileSync(summary,'No registered owner command; hosted gate correctly ignored event.\n');
  process.exit(0);
}
const task=routed.task;
const refData=gh(['repos/'+REPO+'/git/ref/heads/'+task.branch]);
const sha=verifyCanonicalHead(task,refData);
const claim=claimRef(task,sha);
let claimed=true;
try {
  gh(['-X','POST','repos/'+REPO+'/git/refs','-f','ref='+claim,'-f','sha='+sha]);
} catch (e) {
  const existing=gh(['repos/'+REPO+'/git/ref/heads/'+claim.replace('refs/heads/','')]);
  if (existing.ref!==claim || existing.object?.sha!==sha) throw e;
  claimed=false;
}
const again=verifyCanonicalHead(task,gh(['repos/'+REPO+'/git/ref/heads/'+task.branch]));
if (again !== sha) throw new Error('canonical_branch_changed_during_claim');
const envelope={schema:1,eventId:routed.eventId,taskId:task.id,mode:task.mode,
  scope:task.scope,sourceBranch:task.branch,sourceSha:sha,allowPaths:task.paths,
  claimRef:claim,claimed,writerEnabled:false};
writeFileSync('task-envelope.json',JSON.stringify(envelope,null,2)+'\n',{mode:0o600});
appendFileSync(output,'approved='+String(claimed)+'\ntask='+task.id+
  '\nsource_sha='+sha+'\n');
appendFileSync(summary,claimed?'Registered durable claim for '+task.id+' at '+sha+'\n':
  'Duplicate verified claim: no task rerun.\n');
