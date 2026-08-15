const fs = require('fs');
const path = require('path');
const readline = require('readline/promises');

const root = process.cwd();
const featuresDir = path.join(root, 'docs/features');
const specsDir = path.join(root, 'docs/specs');
const wizardSchemaPath = path.join(root, 'docs/sdd/wizards/feature-types.json');

function usage() {
  console.log(`Usage: npm run sdd:new:interactive

Creates a new docs/features/<id>-<slug>/ folder with:
- spec.md
- plan.md
- tasks.md

This mode is schema-driven and includes feature-type presets, conditional prompts,
preview, and answer editing before files are written.`);
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function readDirSafe(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true });
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function listDomains() {
  return readDirSafe(specsDir)
    .filter((entry) => entry.isFile() && entry.name.endsWith('.spec.md'))
    .map((entry) => entry.name.replace('.spec.md', ''))
    .sort();
}

function nextFeatureNumber() {
  const maxNumber = readDirSafe(featuresDir)
    .filter((entry) => entry.isDirectory())
    .map((entry) => {
      const match = entry.name.match(/^(\d{4})-/);
      return match ? Number(match[1]) : 0;
    })
    .reduce((max, number) => Math.max(max, number), 0);

  return String(maxNumber + 1).padStart(4, '0');
}

function slugify(value) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 70);
}

function reqPrefix(domain, slug) {
  const base = (domain || slug)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .split('-')
    .filter(Boolean)
    .slice(0, 3)
    .join('-');

  return `REQ-${base || 'FEATURE'}`;
}

function splitList(value) {
  return String(value || '')
    .split(/\n|;/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function splitFiles(value) {
  return String(value || '')
    .split(/,|\n|;/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatList(items, fallback) {
  const values = Array.isArray(items) && items.length ? items : [fallback];
  return values.map((item) => `- ${item}`).join('\n');
}

function formatChecks(items, selectedIds) {
  return items
    .map((item) => `- ${selectedIds.includes(item.id) ? 'yes' : 'no'}: ${item.label}`)
    .join('\n');
}

function numberedReqs(prefix, requirements) {
  return requirements
    .map((requirement, index) => `- ${prefix}-${String(index + 1).padStart(3, '0')}: ${requirement}`)
    .join('\n');
}

function taskList(prefix, requirements, verificationCommands) {
  const tasks = [];
  let taskNumber = 1;

  requirements.forEach((requirement, index) => {
    const reqId = `${prefix}-${String(index + 1).padStart(3, '0')}`;
    const testTaskId = `T${String(taskNumber++).padStart(3, '0')}`;
    const implementationTaskId = `T${String(taskNumber++).padStart(3, '0')}`;

    tasks.push(
      `- [ ] ${testTaskId} - ${reqId} Add or update focused verification before implementation.`,
      `  - Agent: \`test-engineer\``,
      `  - Depends on: none`,
      `  - Verification: ${verificationCommands[0] ? `\`${verificationCommands[0]}\`` : 'focused test command or documented exception'}`,
      `- [ ] ${implementationTaskId} - ${reqId} Implement: ${requirement}`,
      `  - Agent: \`implementation-engineer\``,
      `  - Depends on: ${testTaskId}`,
      `  - Verification: focused verification from ${testTaskId}`
    );
  });

  verificationCommands.forEach((command) => {
    const taskId = `T${String(taskNumber++).padStart(3, '0')}`;
    tasks.push(
      `- [ ] ${taskId} - ${prefix}-001 Run \`${command}\`.`,
      `  - Agent: \`implementation-engineer\``,
      `  - Depends on: previous implementation tasks`,
      `  - Verification: \`${command}\` exits with status 0`
    );
  });

  return tasks.join('\n');
}

function compact(value) {
  if (Array.isArray(value)) return value.filter(Boolean);
  return value || '';
}

async function ask(rl, question, fallback = '') {
  const suffix = fallback ? ` [${fallback}]` : '';
  const answer = (await rl.question(`${question}${suffix}: `)).trim();
  return answer || fallback;
}

async function askRequired(rl, question, fallback = '') {
  while (true) {
    const answer = await ask(rl, question, fallback);
    if (answer) return answer;
    console.log('Please provide a value.');
  }
}

async function chooseOne(rl, title, options) {
  console.log(`\n${title}`);
  options.forEach((option, index) => {
    console.log(`${index + 1}. ${option.label}${option.description ? ` - ${option.description}` : ''}`);
  });

  while (true) {
    const answer = await ask(rl, 'Choose one', '1');
    const index = Number(answer) - 1;
    if (Number.isInteger(index) && options[index]) return options[index];
    console.log('Choose a valid number.');
  }
}

async function chooseDomain(rl, domains) {
  if (!domains.length) return ask(rl, 'Primary domain (optional)');

  console.log('\nKnown domains');
  domains.forEach((domain, index) => console.log(`${index + 1}. ${domain}`));
  console.log('0. Enter another domain or leave blank');

  while (true) {
    const answer = await ask(rl, 'Primary domain', '0');
    if (answer === '0') return ask(rl, 'Domain name (optional)');
    const index = Number(answer) - 1;
    if (Number.isInteger(index) && domains[index]) return domains[index];
    if (!Number.isNaN(Number(answer))) {
      console.log('Choose a valid number.');
      continue;
    }
    return answer;
  }
}

async function chooseMany(rl, title, options, defaultIds = []) {
  if (!options.length) return [];

  console.log(`\n${title}`);
  options.forEach((option, index) => {
    const checked = defaultIds.includes(option.id) ? '*' : ' ';
    console.log(`${index + 1}. [${checked}] ${option.label}`);
  });
  console.log('Use comma-separated numbers. Leave blank for defaults. Use 0 for none.');

  while (true) {
    const answer = await ask(rl, 'Select');
    if (!answer) return defaultIds;
    if (answer === '0') return [];

    const indexes = answer.split(',').map((item) => Number(item.trim()) - 1);
    if (indexes.every((index) => Number.isInteger(index) && options[index])) {
      return indexes.map((index) => options[index].id);
    }
    console.log('Choose valid comma-separated numbers.');
  }
}

function promptToValue(prompt, raw) {
  if (prompt.kind === 'list') return splitList(raw);
  if (prompt.kind === 'files') return splitFiles(raw);
  return raw.trim();
}

async function askPrompt(rl, prompt, currentValue) {
  const fallback = Array.isArray(currentValue) ? currentValue.join('; ') : currentValue || prompt.default || '';
  const raw = prompt.required
    ? await askRequired(rl, prompt.question, fallback)
    : await ask(rl, prompt.question, fallback);
  return promptToValue(prompt, raw);
}

function allPrompts(schema, featureType) {
  return [...(schema.commonPrompts || []), ...(featureType.prompts || [])];
}

function normalizeAnswers(schema, featureType, answers) {
  const values = { ...answers };
  const prompts = allPrompts(schema, featureType);
  for (const prompt of prompts) {
    if (prompt.default !== undefined && (values[prompt.id] === undefined || values[prompt.id] === '')) {
      values[prompt.id] = prompt.kind === 'list' || prompt.kind === 'files'
        ? promptToValue(prompt, prompt.default)
        : prompt.default;
    }
  }

  values.requirements = compact(values.requirements);
  values.acceptance = compact(values.acceptance);
  values.files = compact(values.files);
  values.outOfScope = compact(values.outOfScope);
  values.risks = compact(values.risks);
  values.verificationCommands = compact(values.verificationCommands);
  return values;
}

function buildDocs(schema, featureType, featureId, answers) {
  const slug = slugify(answers.title);
  const prefix = reqPrefix(answers.domain, slug);
  const titleHeading = answers.title.replace(/^./, (char) => char.toUpperCase());
  const impactOptions = schema.impactOptions || [];
  const selectedImpacts = answers.impacts || [];
  const requirements = answers.requirements.length ? answers.requirements : ['Define the testable requirement before implementation.'];
  const verificationCommands = answers.verificationCommands.length ? answers.verificationCommands : schema.defaultVerification;

  const spec = `# ${titleHeading} Spec

## Feature Type
${featureType.label}

## Problem
${answers.problem}

## Scope
${answers.scope}

## Domain
${answers.domain || 'Not specified'}

## Impact Classification
${formatChecks(impactOptions, selectedImpacts)}

## User Workflow
${answers.workflow || 'No user workflow change described yet.'}

## API/Data Contracts
${answers.apiContracts || 'No API/data contract assumptions identified yet.'}

## Data Impact
${answers.dataImpact || 'No data model, index, migration, or backfill impact identified yet.'}

## UI States
${answers.uiStates || 'Loading, error, empty, and success states should be defined when relevant.'}

## Requirements
${numberedReqs(prefix, requirements)}

## Acceptance Criteria
${formatList(answers.acceptance, 'Acceptance criteria not defined yet.')}

## Out Of Scope
${formatList(answers.outOfScope, 'No explicit out-of-scope items yet.')}
`;

  const plan = `# ${titleHeading} Plan

## Files
${formatList(answers.files, 'Files to be identified during implementation.')}

## Context Bundle
- \`docs/sdd/constitution.md\`
- \`docs/features/${featureId}/spec.md\`
- \`docs/features/${featureId}/tasks.md\`
${answers.domain ? `- \`docs/specs/${answers.domain}.spec.md\`\n- \`docs/tasks/${answers.domain}.tasks.md\`` : '- Domain spec/task files to be identified if needed.'}

## Verification
${verificationCommands.map((command) => `- \`${command}\``).join('\n')}

## Risks
${formatList(answers.risks, 'No known risks yet.')}
`;

  const tasks = `# ${titleHeading} Tasks

${taskList(prefix, requirements, verificationCommands)}
`;

  return { spec, plan, tasks };
}

function printPreview(featureId, docs) {
  console.log(`\nPreview: docs/features/${featureId}/`);
  for (const [name, content] of Object.entries(docs)) {
    const fileName = `${name}.md`;
    const lines = content.trimEnd().split('\n');
    console.log(`\n--- ${fileName} (${lines.length} lines) ---`);
    console.log(lines.slice(0, 28).join('\n'));
    if (lines.length > 28) console.log(`... ${lines.length - 28} more lines`);
  }
}

function printAnswerSummary(schema, featureType, answers) {
  const prompts = allPrompts(schema, featureType);
  console.log('\nEditable answers');
  console.log('0. Continue');
  prompts.forEach((prompt, index) => {
    const value = Array.isArray(answers[prompt.id]) ? answers[prompt.id].join('; ') : answers[prompt.id];
    console.log(`${index + 1}. ${prompt.id}: ${value || '(empty)'}`);
  });
}

async function editLoop(rl, schema, featureType, answers) {
  const prompts = allPrompts(schema, featureType);
  while (true) {
    printAnswerSummary(schema, featureType, answers);
    const choice = await ask(rl, 'Edit a field number, or continue', '0');
    if (choice === '0') return answers;

    const index = Number(choice) - 1;
    const prompt = prompts[index];
    if (!prompt) {
      console.log('Choose a valid number.');
      continue;
    }

    answers[prompt.id] = await askPrompt(rl, prompt, answers[prompt.id]);
  }
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    usage();
    return;
  }

  if (!fs.existsSync(wizardSchemaPath)) {
    throw new Error(`Missing wizard schema: ${path.relative(root, wizardSchemaPath)}`);
  }

  ensureDir(featuresDir);

  const schema = readJson(wizardSchemaPath);
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

  try {
    console.log(`${schema.projectName || 'Project'} interactive SDD feature wizard`);
    console.log('Answer briefly. Lists accept semicolons; file lists accept commas or semicolons.');

    const featureType = await chooseOne(rl, 'Feature type', schema.featureTypes || []);
    const domains = listDomains();
    const answers = {
      featureType: featureType.id,
      title: await askRequired(rl, 'Feature title'),
      domain: await chooseDomain(rl, domains),
      impacts: await chooseMany(rl, 'Impacts', schema.impactOptions || [], featureType.defaultImpacts || [])
    };

    for (const prompt of allPrompts(schema, featureType)) {
      if (answers[prompt.id] !== undefined) continue;
      answers[prompt.id] = await askPrompt(rl, prompt);
    }

    const normalized = normalizeAnswers(schema, featureType, answers);
    await editLoop(rl, schema, featureType, normalized);

    const slug = slugify(normalized.title);
    if (!slug) throw new Error('Could not create a slug from the title.');

    const featureId = `${nextFeatureNumber()}-${slug}`;
    const featureDir = path.join(featuresDir, featureId);
    if (fs.existsSync(featureDir)) throw new Error(`Feature directory already exists: docs/features/${featureId}`);

    const docs = buildDocs(schema, featureType, featureId, normalizeAnswers(schema, featureType, normalized));
    printPreview(featureId, docs);

    const confirm = await ask(rl, '\nCreate these files? (Y/n)', 'Y');
    if (/^n(o)?$/i.test(confirm)) {
      console.log('Canceled. No files were created.');
      return;
    }

    ensureDir(featureDir);
    fs.writeFileSync(path.join(featureDir, 'spec.md'), docs.spec, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'plan.md'), docs.plan, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'tasks.md'), docs.tasks, 'utf8');

    console.log('\nCreated:');
    console.log(`- docs/features/${featureId}/spec.md`);
    console.log(`- docs/features/${featureId}/plan.md`);
    console.log(`- docs/features/${featureId}/tasks.md`);
    console.log('\nNext:');
    console.log('- npm run sdd:check');
    console.log(`- ai/adapters/codex.sh ${featureId}`);
  } finally {
    rl.close();
  }
}

main().catch((error) => {
  console.error(`Interactive SDD wizard failed: ${error.message}`);
  process.exit(1);
});
