const fs = require('fs');
const { spawn } = require('child_process');
const https = require('https');
const path = require('path');
const readline = require('readline/promises');

const root = path.resolve(__dirname, '..');
const featuresDir = path.join(root, 'docs/features');
const specsDir = path.join(root, 'docs/specs');

function usage() {
  console.log(`Usage: npm run sdd:new
       npm run sdd:new:translate

Creates a new docs/features/<id>-<slug>/ folder with:
- spec.md
- plan.md
- tasks.md

Default mode is local and deterministic. Translate mode makes one optional translation/refinement call.`);
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function readDirSafe(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }

  return fs.readdirSync(dir, { withFileTypes: true });
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
  return value
    .split(/\n|;/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function splitFiles(value) {
  return value
    .split(/,|\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function bulletList(items, fallback) {
  const values = items.length ? items : [fallback];
  return values.map((item) => `- ${item}`).join('\n');
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

function extractJson(text) {
  const trimmed = text.trim();
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    return JSON.parse(trimmed);
  }

  const match = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (match) {
    return JSON.parse(match[1]);
  }

  const start = trimmed.indexOf('{');
  const end = trimmed.lastIndexOf('}');
  if (start >= 0 && end > start) {
    return JSON.parse(trimmed.slice(start, end + 1));
  }

  throw new Error('Translator did not return JSON.');
}

function runTranslateCommand(payload) {
  return new Promise((resolve, reject) => {
    const command = process.env.SDD_TRANSLATE_COMMAND;
    const child = spawn(command, {
      shell: true,
      stdio: ['pipe', 'pipe', 'pipe'],
      cwd: root
    });

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
      stdout += chunk;
    });

    child.stderr.on('data', (chunk) => {
      stderr += chunk;
    });

    child.on('error', reject);
    child.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(`SDD_TRANSLATE_COMMAND failed with exit code ${code}: ${stderr}`));
        return;
      }

      try {
        resolve(extractJson(stdout));
      } catch (error) {
        reject(error);
      }
    });

    child.stdin.end(JSON.stringify(payload, null, 2));
  });
}

function openAiRequest(payload) {
  const apiKey = process.env.OPENAI_API_KEY;
  const model = process.env.SDD_TRANSLATE_MODEL || 'gpt-4o-mini';

  if (!apiKey) {
    throw new Error('Translate mode requires OPENAI_API_KEY or SDD_TRANSLATE_COMMAND.');
  }

  const systemPrompt = [
    'You translate and refine software SDD planning answers into concise technical English.',
    'Return JSON only, with the exact same keys and value shapes as the input.',
    'Preserve code symbols, file paths, endpoint paths, commands, env vars, REQ ids, companyId, smartCode, tenant, role names, and domain names.',
    'Do not add requirements that are not implied by the input.',
    'Keep text short, professional, implementation-ready, and unambiguous.'
  ].join(' ');

  const body = JSON.stringify({
    model,
    temperature: 0.1,
    input: [
      {
        role: 'system',
        content: systemPrompt
      },
      {
        role: 'user',
        content: JSON.stringify(payload, null, 2)
      }
    ]
  });

  return new Promise((resolve, reject) => {
    const request = https.request({
      hostname: 'api.openai.com',
      path: '/v1/responses',
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body)
      }
    }, (response) => {
      let responseBody = '';

      response.on('data', (chunk) => {
        responseBody += chunk;
      });

      response.on('end', () => {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`OpenAI translation failed (${response.statusCode}): ${responseBody}`));
          return;
        }

        try {
          const parsed = JSON.parse(responseBody);
          const text = parsed.output_text || parsed.output
            ?.flatMap((item) => item.content || [])
            ?.map((content) => content.text)
            ?.filter(Boolean)
            ?.join('\n');

          if (!text) {
            throw new Error('OpenAI response did not include output text.');
          }

          resolve(extractJson(text));
        } catch (error) {
          reject(error);
        }
      });
    });

    request.on('error', reject);
    request.end(body);
  });
}

async function translateAnswers(payload) {
  if (process.env.SDD_TRANSLATE_COMMAND) {
    return runTranslateCommand(payload);
  }

  return openAiRequest(payload);
}

async function askRequired(rl, question) {
  while (true) {
    const answer = (await rl.question(question)).trim();
    if (answer) {
      return answer;
    }
    console.log('Please provide a value.');
  }
}

async function askOptional(rl, question, fallback = '') {
  const answer = (await rl.question(question)).trim();
  return answer || fallback;
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    usage();
    return;
  }

  const translate = process.argv.includes('--translate');

  ensureDir(featuresDir);

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  try {
    console.log(translate ? 'SDD new feature wizard with translation' : 'SDD new feature wizard');
    console.log('Answer briefly. Use semicolons for multiple items when prompted.\n');

    const domains = listDomains();
    if (domains.length) {
      console.log(`Known domains: ${domains.join(', ')}\n`);
    }

    const title = await askRequired(rl, 'Feature title: ');
    const domain = await askOptional(rl, 'Primary domain (optional): ');
    const problem = await askRequired(rl, 'Problem this solves: ');
    const scope = await askRequired(rl, 'Scope/change summary: ');
    const workflow = await askOptional(rl, 'User workflow impacted (optional): ', 'No user workflow change described yet.');
    const apiContracts = await askOptional(rl, 'API/data contract assumptions (optional): ', 'No API/data contract assumptions identified yet.');
    const requirementsInput = await askRequired(rl, 'Requirements, separated by semicolon: ');
    const acceptanceInput = await askRequired(rl, 'Acceptance criteria, separated by semicolon: ');
    const filesInput = await askOptional(rl, 'Expected files, comma-separated (optional): ');
    const outOfScopeInput = await askOptional(rl, 'Out of scope, separated by semicolon (optional): ', 'No explicit out-of-scope items yet.');
    const risksInput = await askOptional(rl, 'Risks, separated by semicolon (optional): ', 'No known risks yet.');
    const verificationInput = await askOptional(
      rl,
      'Verification commands, separated by semicolon (default: npm run sdd:check; npm run contracts:check; npm run test; npm run lint; npm run build): ',
      'npm run sdd:check; npm run contracts:check; npm run test; npm run lint; npm run build'
    );

    const rawAnswers = {
      title,
      domain,
      problem,
      scope,
      workflow,
      apiContracts,
      requirements: splitList(requirementsInput),
      acceptance: splitList(acceptanceInput),
      files: splitFiles(filesInput),
      outOfScope: splitList(outOfScopeInput),
      risks: splitList(risksInput),
      verificationCommands: splitList(verificationInput)
    };

    const answers = translate ? await translateAnswers(rawAnswers) : rawAnswers;
    if (translate) {
      console.log('\nTranslated and refined answers for English SDD docs.');
    }

    const slug = slugify(answers.title || title);
    if (!slug) {
      throw new Error('Could not create a slug from the title.');
    }

    const featureId = `${nextFeatureNumber()}-${slug}`;
    const featureDir = path.join(featuresDir, featureId);
    if (fs.existsSync(featureDir)) {
      throw new Error(`Feature directory already exists: docs/features/${featureId}`);
    }

    const prefix = reqPrefix(answers.domain, slug);
    const requirements = answers.requirements || [];
    const acceptance = answers.acceptance || [];
    const files = answers.files || [];
    const outOfScope = answers.outOfScope || [];
    const risks = answers.risks || [];
    const verificationCommands = answers.verificationCommands || [];
    const titleHeading = (answers.title || title).replace(/^./, (char) => char.toUpperCase());

    console.log(`\nAbout to create: docs/features/${featureId}/`);
    const confirm = await askOptional(rl, 'Create files? (Y/n): ', 'Y');
    if (/^n(o)?$/i.test(confirm)) {
      console.log('Canceled. No files were created.');
      return;
    }

    ensureDir(featureDir);

    const spec = `# ${titleHeading} Spec

## Problem
${answers.problem}

## Scope
${answers.scope}

## Domain
${answers.domain || 'Not specified'}

## User Workflow
${answers.workflow}

## API/Data Contracts
${answers.apiContracts}

## Requirements
${numberedReqs(prefix, requirements)}

## UI States
- Loading: Define if the feature uses async data.
- Error: Define user-facing feedback for failures.
- Empty: Define behavior when there is no data.
- Success: Define the expected completed state.

## Acceptance Criteria
${bulletList(acceptance, 'Acceptance criteria not defined yet.')}

## Out Of Scope
${bulletList(outOfScope, 'No explicit out-of-scope items yet.')}
`;

    const plan = `# ${titleHeading} Plan

## Files
${bulletList(files, 'Files to be identified during implementation.')}

## Context Bundle
- \`docs/sdd/constitution.md\`
- \`docs/features/${featureId}/spec.md\`
- \`docs/features/${featureId}/tasks.md\`
${answers.domain ? `- \`docs/specs/${answers.domain}.spec.md\`\n- \`docs/tasks/${answers.domain}.tasks.md\`` : '- Domain spec/task files to be identified if needed.'}

## Verification
${verificationCommands.map((command) => `- \`${command}\``).join('\n')}

## Risks
${bulletList(risks, 'No known risks yet.')}
`;

    const tasks = `# ${titleHeading} Tasks

${taskList(prefix, requirements, verificationCommands)}
`;

    fs.writeFileSync(path.join(featureDir, 'spec.md'), spec, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'plan.md'), plan, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'tasks.md'), tasks, 'utf8');

    console.log('\nCreated:');
    console.log(`- docs/features/${featureId}/spec.md`);
    console.log(`- docs/features/${featureId}/plan.md`);
    console.log(`- docs/features/${featureId}/tasks.md`);
    console.log('\nNext:');
    console.log(`- npm run sdd:check`);
    console.log(`- npm run contracts:check`);
    console.log(`- npm run test`);
    console.log(`- npm run lint`);
    console.log(`- npm run build`);
    console.log(`- ai/adapters/codex.sh `);
  } finally {
    rl.close();
  }
}

main().catch((error) => {
  console.error(`SDD wizard failed: ${error.message}`);
  process.exit(1);
});
