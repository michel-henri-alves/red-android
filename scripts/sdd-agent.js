const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

const agents = {
  'spec-reviewer': 'ai/agents/sdd-spec-reviewer.md',
  planner: 'ai/agents/sdd-planner.md',
  implementation: 'ai/agents/implementation-engineer.md',
  test: 'ai/agents/test-engineer.md',
  'backend-contract': 'ai/agents/backend-contract-reviewer.md',
  'mobile-ux': 'ai/agents/mobile-ux-regression-reviewer.md',
  security: 'ai/agents/security-tenant-isolation-reviewer.md',
  integration: 'ai/agents/cross-project-integrator.md',
  'code-reviewer': 'ai/agents/code-reviewer.md',
  'performance-cost': 'ai/agents/performance-cost-reviewer.md',
  'release-gate': 'ai/agents/release-gate-reviewer.md'
};

function usage() {
  console.log(`Usage: npm run sdd:agent -- <feature-id> <agent>

Agents:
  ${Object.keys(agents).join('\n  ')}

Creates a prompt/report file under docs/features/<feature-id>/runs/.`);
}

function timestamp() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function exists(relativePath) {
  return fs.existsSync(path.join(root, relativePath));
}

function requireFile(relativePath) {
  if (!exists(relativePath)) {
    throw new Error(`Required file not found: ${relativePath}`);
  }
}

function main() {
  const [, , feature, agentName] = process.argv;

  if (!feature || !agentName || feature === '-h' || feature === '--help') {
    usage();
    process.exit(feature ? 0 : 1);
  }

  const agentPath = agents[agentName];
  if (!agentPath) {
    throw new Error(`Unknown agent: ${agentName}`);
  }

  const featureDir = `docs/features/${feature}`;
  const specPath = `${featureDir}/spec.md`;
  const planPath = `${featureDir}/plan.md`;
  const tasksPath = `${featureDir}/tasks.md`;

  [
    'docs/sdd/constitution.md',
    'docs/sdd/workflow.md',
    'docs/sdd/context-map.md',
    'docs/sdd/agents.md',
    'docs/sdd/skills.md',
    agentPath,
    specPath,
    planPath,
    tasksPath
  ].forEach(requireFile);

  const runsDir = path.join(root, featureDir, 'runs');
  fs.mkdirSync(runsDir, { recursive: true });

  const reportPath = path.join(runsDir, `${timestamp()}-${agentName}.md`);
  const body = [
    `# SDD Agent Run - ${feature}`,
    '',
    `- Agent: ${agentName}`,
    `- Agent file: ${agentPath}`,
    `- Created at UTC: ${new Date().toISOString()}`,
    '',
    '## Context Bundle',
    '- `docs/sdd/constitution.md`',
    '- `docs/sdd/workflow.md`',
    '- `docs/sdd/context-map.md`',
    '- `docs/sdd/agents.md`',
    '- `docs/sdd/skills.md`',
    `- \`${agentPath}\``,
    `- \`${specPath}\``,
    `- \`${planPath}\``,
    `- \`${tasksPath}\``,
    '',
    '## Prompt',
    '```text',
    'Use the context below and execute the requested SDD agent role.',
    '',
    '## SDD Constitution',
    read('docs/sdd/constitution.md'),
    '',
    '## SDD Workflow',
    read('docs/sdd/workflow.md'),
    '',
    '## SDD Context Map',
    read('docs/sdd/context-map.md'),
    '',
    '## SDD Agents Guide',
    read('docs/sdd/agents.md'),
    '',
    '## SDD Skills Guide',
    read('docs/sdd/skills.md'),
    '',
    '## Active Agent',
    read(agentPath),
    '',
    '## Feature Specification',
    read(specPath),
    '',
    '## Feature Plan',
    read(planPath),
    '',
    '## Feature Tasks',
    read(tasksPath),
    '```',
    '',
    '## Agent Output',
    '_Paste or summarize the agent result here when the agent is executed by an external provider._'
  ].join('\n');

  fs.writeFileSync(reportPath, body);
  console.log(`SDD agent prompt: ${path.relative(root, reportPath)}`);
}

try {
  main();
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
