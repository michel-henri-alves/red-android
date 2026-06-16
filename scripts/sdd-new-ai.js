const fs = require('fs');
const { spawn } = require('child_process');
const https = require('https');
const path = require('path');
const readline = require('readline/promises');

const root = path.resolve(__dirname, '..');
const featuresDir = path.join(root, 'docs/features');
const specsDir = path.join(root, 'docs/specs');
const projectType = 'mobile';
const defaultVerification = ['npm run sdd:check', 'npm run contracts:check', 'npm run test', 'npm run lint', 'npm run build'];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function readDirSafe(dir) {
  if (!fs.existsSync(dir)) return [];
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
  return value.split(/\n|;/).map((item) => item.trim()).filter(Boolean);
}

function splitFiles(value) {
  return value.split(/,|\n|;/).map((item) => item.trim()).filter(Boolean);
}

function bulletList(items, fallback) {
  const values = Array.isArray(items) && items.length ? items : [fallback];
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
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) return JSON.parse(trimmed);

  const match = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (match) return JSON.parse(match[1]);

  const start = trimmed.indexOf('{');
  const end = trimmed.lastIndexOf('}');
  if (start >= 0 && end > start) return JSON.parse(trimmed.slice(start, end + 1));

  throw new Error('AI response did not return JSON.');
}

function normalizeUsage(usage) {
  return {
    inputTokens: usage?.input_tokens || usage?.prompt_tokens || usage?.inputTokens || 0,
    outputTokens: usage?.output_tokens || usage?.completion_tokens || usage?.outputTokens || 0,
    totalTokens: usage?.total_tokens || usage?.totalTokens || 0
  };
}

function addUsage(total, usage) {
  const normalized = normalizeUsage(usage);
  total.inputTokens += normalized.inputTokens;
  total.outputTokens += normalized.outputTokens;
  total.totalTokens += normalized.totalTokens || normalized.inputTokens + normalized.outputTokens;
}

async function askRequired(rl, question) {
  while (true) {
    const answer = (await rl.question(question)).trim();
    if (answer) return answer;
    console.log('Please provide a value.');
  }
}

async function askOptional(rl, question, fallback = '') {
  const answer = (await rl.question(question)).trim();
  return answer || fallback;
}

function openAiCall(model, systemPrompt, payload) {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    throw new Error('OPENAI_API_KEY is required for OpenAI provider.');
  }

  const body = JSON.stringify({
    model,
    temperature: 0.2,
    input: [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: JSON.stringify(payload, null, 2) }
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
          reject(new Error(`OpenAI request failed (${response.statusCode}): ${responseBody}`));
          return;
        }

        try {
          const parsed = JSON.parse(responseBody);
          const text = parsed.output_text || parsed.output
            ?.flatMap((item) => item.content || [])
            ?.map((content) => content.text)
            ?.filter(Boolean)
            ?.join('\n');

          if (!text) throw new Error('OpenAI response did not include output text.');
          resolve({ data: extractJson(text), usage: parsed.usage });
        } catch (error) {
          reject(error);
        }
      });
    });

    request.on('error', reject);
    request.end(body);
  });
}

function customAiCall(command, payload) {
  return new Promise((resolve, reject) => {
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
        reject(new Error(`Custom AI command failed with exit code ${code}: ${stderr}`));
        return;
      }

      const parsed = extractJson(stdout);
      resolve(parsed.data ? parsed : { data: parsed, usage: parsed.usage });
    });

    child.stdin.end(JSON.stringify(payload, null, 2));
  });
}

async function aiCall(config, task, payload) {
  const systemPrompt = [
    'You are an expert software product engineer helping create concise SDD feature docs.',
    `Project type: ${projectType}.`,
    'If user input is not English, translate it to concise technical English in final docs.',
    'If user input is already English, refine it without unnecessary rewriting.',
    'Preserve code symbols, file paths, endpoint paths, commands, env vars, REQ ids, companyId, smartCode, tenant, role names, and domain names.',
    'Return JSON only.'
  ].join(' ');

  const requestPayload = { task, projectType, ...payload };

  if (config.provider === 'custom') {
    return customAiCall(config.command, requestPayload);
  }

  return openAiCall(config.model, systemPrompt, requestPayload);
}

async function askAiConfig(rl) {
  const provider = await askOptional(rl, 'LLM provider (openai/custom) [openai]: ', process.env.SDD_AI_PROVIDER || 'openai');
  if (provider === 'custom') {
    const command = await askOptional(rl, 'Custom AI command [SDD_AI_COMMAND]: ', process.env.SDD_AI_COMMAND || '');
    if (!command) throw new Error('Custom provider requires SDD_AI_COMMAND or an entered command.');
    return { provider, command, model: 'custom' };
  }

  const model = await askOptional(rl, 'OpenAI model [gpt-4o-mini]: ', process.env.SDD_AI_MODEL || 'gpt-4o-mini');
  return { provider: 'openai', model };
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    console.log('Usage: npm run sdd:new:ai');
    return;
  }

  ensureDir(featuresDir);

  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  const usage = { inputTokens: 0, outputTokens: 0, totalTokens: 0 };
  const calls = [];

  try {
    console.log('SDD AI-assisted feature wizard');
    console.log('This wizard uses AI to ask follow-up questions and refine final docs.\n');

    const config = await askAiConfig(rl);
    const domains = listDomains();
    if (domains.length) console.log(`Known domains: ${domains.join(', ')}\n`);

    const title = await askRequired(rl, 'Feature title: ');
    const domain = await askOptional(rl, 'Primary domain (optional): ');
    const problem = await askRequired(rl, 'Problem this solves: ');
    const scope = await askRequired(rl, 'Scope/change summary: ');
    const knownFiles = await askOptional(rl, 'Known files/routes/APIs/hooks, comma-separated (optional): ');
    const constraints = await askOptional(rl, 'Constraints, risks, or out-of-scope notes (optional): ');

    console.log('\nAsking AI for follow-up questions...');
    const questionsResult = await aiCall(config, 'followup_questions', {
      expectedJson: {
        questions: [
          { id: 'q1', question: 'question in the user input language', required: false }
        ]
      },
      baseAnswers: { title, domain, problem, scope, knownFiles: splitFiles(knownFiles), constraints }
    });
    addUsage(usage, questionsResult.usage);
    calls.push({ name: 'followup_questions', usage: normalizeUsage(questionsResult.usage) });

    const followupAnswers = {};
    const questions = Array.isArray(questionsResult.data.questions) ? questionsResult.data.questions.slice(0, 8) : [];
    for (const question of questions) {
      const answer = question.required
        ? await askRequired(rl, `${question.question}: `)
        : await askOptional(rl, `${question.question}: `);
      followupAnswers[question.id || question.question] = answer;
    }

    const rawAnswers = {
      title,
      domain,
      problem,
      scope,
      knownFiles: splitFiles(knownFiles),
      constraints,
      followupAnswers,
      defaultVerification
    };

    console.log('\nAsking AI to create refined SDD content...');
    const docsResult = await aiCall(config, 'generate_sdd_docs', {
      expectedJson: {
        title: 'English feature title',
        domain: 'domain',
        problem: 'English problem statement',
        scope: 'English scope statement',
        workflow: 'English workflow statement',
        apiContracts: 'English API/data contract notes',
        requirements: ['English requirement'],
        acceptance: ['English acceptance criterion'],
        files: ['path/to/file'],
        outOfScope: ['English out-of-scope item'],
        risks: ['English risk'],
        verificationCommands: defaultVerification,
        detectedInputLanguage: 'pt-BR',
        translatedToEnglish: true
      },
      rawAnswers
    });
    addUsage(usage, docsResult.usage);
    calls.push({ name: 'generate_sdd_docs', usage: normalizeUsage(docsResult.usage) });

    const answers = docsResult.data;
    const slug = slugify(answers.title || title);
    if (!slug) throw new Error('Could not create a slug from the title.');

    const featureId = `${nextFeatureNumber()}-${slug}`;
    const featureDir = path.join(featuresDir, featureId);
    if (fs.existsSync(featureDir)) throw new Error(`Feature directory already exists: docs/features/${featureId}`);

    console.log(`\nAbout to create: docs/features/${featureId}/`);
    const confirm = await askOptional(rl, 'Create files? (Y/n): ', 'Y');
    if (/^n(o)?$/i.test(confirm)) {
      console.log('Canceled. No files were created.');
      return;
    }

    ensureDir(featureDir);
    const prefix = reqPrefix(answers.domain, slug);
    const titleHeading = (answers.title || title).replace(/^./, (char) => char.toUpperCase());
    const requirements = answers.requirements || [];
    const verificationCommands = answers.verificationCommands?.length ? answers.verificationCommands : defaultVerification;

    const spec = `# ${titleHeading} Spec

## Problem
${answers.problem}

## Scope
${answers.scope}

## Domain
${answers.domain || 'Not specified'}

## User Workflow
${answers.workflow || 'No user workflow change described yet.'}

## API/Data Contracts
${answers.apiContracts || 'No API/data contract assumptions identified yet.'}

## Requirements
${numberedReqs(prefix, requirements)}

## UI States
- Loading: Define if the feature uses async data.
- Error: Define user-facing feedback for failures.
- Empty: Define behavior when there is no data.
- Success: Define the expected completed state.

## Acceptance Criteria
${bulletList(answers.acceptance || [], 'Acceptance criteria not defined yet.')}

## Out Of Scope
${bulletList(answers.outOfScope || [], 'No explicit out-of-scope items yet.')}
`;

    const plan = `# ${titleHeading} Plan

## Files
${bulletList(answers.files || [], 'Files to be identified during implementation.')}

## Context Bundle
- \`docs/sdd/constitution.md\`
- \`docs/features/${featureId}/spec.md\`
- \`docs/features/${featureId}/tasks.md\`
${answers.domain ? `- \`docs/specs/${answers.domain}.spec.md\`\n- \`docs/tasks/${answers.domain}.tasks.md\`` : '- Domain spec/task files to be identified if needed.'}

## Verification
${verificationCommands.map((command) => `- \`${command}\``).join('\n')}

## Risks
${bulletList(answers.risks || [], 'No known risks yet.')}
`;

    const tasks = `# ${titleHeading} Tasks

${taskList(prefix, requirements, verificationCommands)}
`;

    const report = `# AI Wizard Report

## LLM
- Provider: ${config.provider}
- Model: ${config.model}

## Language
- Detected input language: ${answers.detectedInputLanguage || 'unknown'}
- Translated to English: ${answers.translatedToEnglish ? 'yes' : 'no'}

## Token Usage
- Input tokens: ${usage.inputTokens || 'unknown'}
- Output tokens: ${usage.outputTokens || 'unknown'}
- Total tokens: ${usage.totalTokens || 'unknown'}

## Calls
${calls.map((call) => `- ${call.name}: input ${call.usage.inputTokens || 'unknown'}, output ${call.usage.outputTokens || 'unknown'}, total ${call.usage.totalTokens || 'unknown'}`).join('\n')}
`;

    fs.writeFileSync(path.join(featureDir, 'spec.md'), spec, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'plan.md'), plan, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'tasks.md'), tasks, 'utf8');
    fs.writeFileSync(path.join(featureDir, 'ai-report.md'), report, 'utf8');

    console.log('\nCreated:');
    console.log(`- docs/features/${featureId}/spec.md`);
    console.log(`- docs/features/${featureId}/plan.md`);
    console.log(`- docs/features/${featureId}/tasks.md`);
    console.log(`- docs/features/${featureId}/ai-report.md`);
    console.log(`\nToken usage: ${usage.totalTokens || 'unknown'} total tokens`);
    console.log('\nNext:');
    console.log('- npm run sdd:check');
    console.log('- npm run contracts:check');
    console.log('- npm run test');
    console.log('- npm run lint');
    console.log('- npm run build');
    console.log(`- ai/adapters/codex.sh ${featureId}`);
  } finally {
    rl.close();
  }
}

main().catch((error) => {
  console.error(`SDD AI wizard failed: ${error.message}`);
  process.exit(1);
});
