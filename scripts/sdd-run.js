const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const root = path.resolve(__dirname, '..');
const defaultCommands = [
  'npm run sdd:check',
  'npm run contracts:check',
  'npm run test',
  'npm run lint',
  'npm run build'
];
const fullOutputModes = new Set(['full', 'verbose']);
const outputMode = process.env.SDD_RUN_OUTPUT || 'summary';
const maxOutputChars = Number(process.env.SDD_RUN_MAX_OUTPUT_CHARS || 12000);
const headChars = Number(process.env.SDD_RUN_HEAD_CHARS || 4000);
const tailChars = Number(process.env.SDD_RUN_TAIL_CHARS || 4000);

function usage() {
  console.log(`Usage: npm run sdd:run -- <feature-id> [command...]

Runs verification commands and writes:
docs/features/<feature-id>/runs/<timestamp>.md

Output is summarized by default to keep SDD evidence compact.
Use SDD_RUN_OUTPUT=full for complete stdout/stderr.

Examples:
  npm run sdd:run -- 0002-user-initial-password-change
  npm run sdd:run -- 0002-user-initial-password-change "npm run sdd:check" "npm run test"
  SDD_RUN_OUTPUT=full npm run sdd:run -- 0002-user-initial-password-change`);
}

function timestamp() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

function ensureFeature(feature) {
  const featureDir = path.join(root, 'docs/features', feature);
  if (!fs.existsSync(featureDir)) {
    throw new Error(`Feature folder not found: docs/features/${feature}`);
  }
  return featureDir;
}

function runCommand(command) {
  const startedAt = new Date();
  const result = spawnSync(command, {
    cwd: root,
    shell: true,
    encoding: 'utf8'
  });
  const endedAt = new Date();

  return {
    command,
    status: result.status,
    startedAt: startedAt.toISOString(),
    endedAt: endedAt.toISOString(),
    durationMs: endedAt.getTime() - startedAt.getTime(),
    stdout: result.stdout || '',
    stderr: result.stderr || ''
  };
}

function fenced(value) {
  const trimmed = value.trim();
  return trimmed ? `\n\`\`\`text\n${trimmed}\n\`\`\`\n` : '\n_None._\n';
}

function summarizeOutput(value) {
  const trimmed = value.trim();
  if (!trimmed) {
    return { text: '', truncated: false, originalChars: 0, originalLines: 0 };
  }

  const originalChars = trimmed.length;
  const originalLines = trimmed.split(/\r?\n/).length;

  if (fullOutputModes.has(outputMode) || originalChars <= maxOutputChars) {
    return { text: trimmed, truncated: false, originalChars, originalLines };
  }

  const head = trimmed.slice(0, headChars).trimEnd();
  const tail = trimmed.slice(-tailChars).trimStart();
  const omittedChars = Math.max(originalChars - head.length - tail.length, 0);
  const text = [
    head,
    '',
    `[output truncated: ${omittedChars} chars omitted; set SDD_RUN_OUTPUT=full to record complete output]`,
    '',
    tail
  ].join('\n');

  return { text, truncated: true, originalChars, originalLines };
}

function outputSection(title, value) {
  const summary = summarizeOutput(value);
  const metadata = [
    `#### ${title}`,
    '',
    `- Original size: ${summary.originalChars} chars, ${summary.originalLines} lines`,
    `- Truncated: ${summary.truncated ? 'yes' : 'no'}`,
    fenced(summary.text)
  ];

  return metadata;
}

function writeReport(featureDir, feature, results) {
  const runsDir = path.join(featureDir, 'runs');
  fs.mkdirSync(runsDir, { recursive: true });

  const success = results.every((result) => result.status === 0);
  const reportPath = path.join(runsDir, `${timestamp()}.md`);
  const body = [
    `# SDD Run - ${feature}`,
    '',
    `- Status: ${success ? 'passed' : 'failed'}`,
    `- Created at UTC: ${new Date().toISOString()}`,
    `- Project: ${root}`,
    `- Output mode: ${fullOutputModes.has(outputMode) ? 'full' : 'summary'}`,
    '',
    '## Commands',
    '',
    ...results.flatMap((result) => [
      `### ${result.command}`,
      '',
      `- Status: ${result.status}`,
      `- Started at UTC: ${result.startedAt}`,
      `- Duration: ${result.durationMs}ms`,
      '',
      ...outputSection('stdout', result.stdout),
      ...outputSection('stderr', result.stderr)
    ])
  ].join('\n');

  fs.writeFileSync(reportPath, body);
  return { reportPath, success };
}

function main() {
  const [, , feature, ...commands] = process.argv;

  if (!feature || feature === '-h' || feature === '--help') {
    usage();
    process.exit(feature ? 0 : 1);
  }

  const featureDir = ensureFeature(feature);
  const commandsToRun = commands.length ? commands : defaultCommands;
  const results = commandsToRun.map(runCommand);
  const { reportPath, success } = writeReport(featureDir, feature, results);

  console.log(`SDD run report: ${path.relative(root, reportPath)}`);

  if (!success) {
    process.exit(1);
  }
}

try {
  main();
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
