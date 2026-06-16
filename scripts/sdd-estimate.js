const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

const modelProfiles = {
  codex: {
    label: 'Codex / OpenAI default',
    charsPerToken: 3.8,
    tokensPerWord: 1.35,
    messageOverheadTokens: 220,
    fileOverheadTokens: 12
  },
  openai: {
    label: 'OpenAI chat model',
    charsPerToken: 3.8,
    tokensPerWord: 1.35,
    messageOverheadTokens: 220,
    fileOverheadTokens: 12
  },
  claude: {
    label: 'Claude chat model',
    charsPerToken: 3.6,
    tokensPerWord: 1.3,
    messageOverheadTokens: 180,
    fileOverheadTokens: 10
  },
  copilot: {
    label: 'GitHub Copilot Chat',
    charsPerToken: 3.9,
    tokensPerWord: 1.33,
    messageOverheadTokens: 220,
    fileOverheadTokens: 12
  }
};

const actionConfig = {
  implement: {
    aliases: ['implement', 'implementation', 'spec'],
    promptFile: 'ai/prompts/implement-from-spec.md',
    agentFile: 'ai/agents/implementation-engineer.md',
    description: 'Implement the feature described by the SDD files.'
  },
  test: {
    aliases: ['test', 'tests', 'unit-tests', 'unit'],
    promptFile: 'ai/prompts/test-generator.md',
    agentFile: 'ai/agents/test-engineer.md',
    description: 'Generate comprehensive tests for the feature described by the SDD files.'
  },
  refactor: {
    aliases: ['refactor', 'refactoring'],
    promptFile: 'ai/prompts/refactor.md',
    agentFile: '',
    description: 'Refactor the feature implementation using the SDD files as context.'
  }
};

function usage() {
  console.log(`Usage: node scripts/sdd-estimate.js <feature> [action] [delta-file] [--model codex|openai|claude|copilot] [--mode current|legacy] [--json]

Examples:
  node scripts/sdd-estimate.js 0001-pos-sdd-context
  node scripts/sdd-estimate.js pos implement --model claude
  node scripts/sdd-estimate.js pos --mode legacy
  node scripts/sdd-estimate.js customer implement docs/specs/customer.delta.md --json`);
}

function parseArgs(argv) {
  const positional = [];
  const options = {
    model: 'codex',
    mode: 'current',
    json: false
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];

    if (arg === '-h' || arg === '--help') {
      return { help: true };
    }

    if (arg === '--json') {
      options.json = true;
      continue;
    }

    if (arg === '--model') {
      options.model = argv[index + 1];
      index += 1;
      continue;
    }

    if (arg === '--mode') {
      options.mode = argv[index + 1];
      index += 1;
      continue;
    }

    if (arg.startsWith('--model=')) {
      options.model = arg.slice('--model='.length);
      continue;
    }

    if (arg.startsWith('--mode=')) {
      options.mode = arg.slice('--mode='.length);
      continue;
    }

    positional.push(arg);
  }

  return {
    feature: positional[0],
    action: positional[1] || 'implement',
    deltaFile: positional[2] || '',
    ...options
  };
}

function resolveAction(actionName) {
  const normalized = actionName.toLowerCase();
  const key = Object.keys(actionConfig).find((name) => actionConfig[name].aliases.includes(normalized));

  if (!key) {
    throw new Error(`Unknown action: ${actionName}`);
  }

  return actionConfig[key];
}

function exists(relativePath) {
  return fs.existsSync(path.join(root, relativePath));
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function requireFile(relativePath) {
  if (!exists(relativePath)) {
    throw new Error(`Required file not found: ${relativePath}`);
  }
}

function countWords(text) {
  const matches = text.trim().match(/[^\s]+/g);
  return matches ? matches.length : 0;
}

function countLines(text) {
  if (!text) {
    return 0;
  }

  return text.split(/\r?\n/).length;
}

function estimateTokens(text, profile, fileCount) {
  const chars = text.length;
  const words = countWords(text);
  const charEstimate = chars / profile.charsPerToken;
  const wordEstimate = words * profile.tokensPerWord;
  const structureOverhead = profile.messageOverheadTokens + (fileCount * profile.fileOverheadTokens);

  return Math.ceil(Math.max(charEstimate, wordEstimate) + structureOverhead);
}

function buildSection(title, relativePath) {
  const content = read(relativePath);

  return {
    title,
    relativePath,
    content: `## ${title}\n${content}\n`
  };
}

function buildBundle(feature, action, deltaFile, mode) {
  const constitutionFile = 'docs/sdd/constitution.md';
  const workflowFile = 'docs/sdd/workflow.md';
  const contextMapFile = 'docs/sdd/context-map.md';
  const skillsFile = 'docs/sdd/skills.md';
  const contextFile = 'ai/context/mobile.md';
  const featureDir = `docs/features/${feature}`;
  const featureSpecFile = `${featureDir}/spec.md`;
  const featurePlanFile = `${featureDir}/plan.md`;
  const featureTasksFile = `${featureDir}/tasks.md`;
  const domainSpecFile = `docs/specs/${feature}.spec.md`;
  const domainTasksFile = `docs/tasks/${feature}.tasks.md`;

  let promptFile = action.promptFile;
  const agentFile = action.agentFile;

  if (mode === 'legacy') {
    [promptFile, domainSpecFile, domainTasksFile, contextFile].forEach(requireFile);

    return [
      buildSection('AI Prompt', promptFile),
      buildSection('Domain Specification', domainSpecFile),
      buildSection('Domain Tasks', domainTasksFile),
      buildSection('Mobile Context', contextFile)
    ];
  }

  [promptFile, constitutionFile, workflowFile, contextMapFile, skillsFile].forEach(requireFile);
  if (agentFile) {
    requireFile(agentFile);
  }

  const sections = [
    {
      title: 'Action',
      relativePath: '(adapter action)',
      content: `${action.description}\n`
    },
    buildSection('SDD Constitution', constitutionFile),
    buildSection('SDD Workflow', workflowFile),
    buildSection('SDD Context Map', contextMapFile),
    buildSection('SDD Skills Guide', skillsFile)
  ];

  if (exists(featureDir)) {
    promptFile = exists('ai/prompts/sdd-execute.md')
      ? 'ai/prompts/sdd-execute.md'
      : promptFile;

    [promptFile, featureSpecFile, featurePlanFile, featureTasksFile].forEach(requireFile);

    sections.push(buildSection('AI Prompt', promptFile));
    if (agentFile) {
      sections.push(buildSection('SDD Agent', agentFile));
    }
    sections.push(buildSection('Feature Specification', featureSpecFile));
    sections.push(buildSection('Feature Plan', featurePlanFile));
    sections.push(buildSection('Feature Tasks', featureTasksFile));
  } else {
    [domainSpecFile, domainTasksFile, contextFile].forEach(requireFile);

    sections.push(buildSection('AI Prompt', promptFile));
    if (agentFile) {
      sections.push(buildSection('SDD Agent', agentFile));
    }
    sections.push(buildSection('Domain Specification', domainSpecFile));
    sections.push(buildSection('Domain Tasks', domainTasksFile));
    sections.push(buildSection('Mobile Context', contextFile));
  }

  if (deltaFile) {
    requireFile(deltaFile);
    sections.push({
      title: 'SDD Delta',
      relativePath: deltaFile,
      content: `## SDD Delta\nApply only the changes described in this delta file. Use the main specification as baseline context and avoid unrelated refactors.\n\n${read(deltaFile)}\n`
    });
  }

  return sections;
}

function summarizeSections(sections, profile) {
  return sections.map((section) => ({
    section: section.title,
    file: section.relativePath,
    chars: section.content.length,
    words: countWords(section.content),
    lines: countLines(section.content),
    estimatedTokens: estimateTokens(section.content, {
      ...profile,
      messageOverheadTokens: 0
    }, section.relativePath === '(adapter action)' ? 0 : 1)
  }));
}

function printTextReport(result) {
  console.log(`SDD token estimate for ${result.feature}`);
  console.log(`Model profile: ${result.model} (${result.profile.label})`);
  console.log(`Mode: ${result.mode}`);
  console.log(`Action: ${result.action}`);
  console.log('');
  console.log('| Section | File | Words | Chars | Estimated tokens |');
  console.log('| --- | --- | ---: | ---: | ---: |');
  result.sections.forEach((section) => {
    console.log(`| ${section.section} | \`${section.file}\` | ${section.words} | ${section.chars} | ${section.estimatedTokens} |`);
  });
  console.log('| **Total** |  | **' + result.totals.words + '** | **' + result.totals.chars + '** | **' + result.totals.estimatedTokens + '** |');
  console.log('');
  console.log('Notes: token counts are estimates based on characters, words, model profile, adapter message overhead, and per-file framing overhead.');
}

function main() {
  const args = parseArgs(process.argv.slice(2));

  if (args.help) {
    usage();
    return;
  }

  if (!args.feature) {
    usage();
    process.exit(1);
  }

  if (!modelProfiles[args.model]) {
    throw new Error(`Unknown model profile: ${args.model}`);
  }

  if (!['current', 'legacy'].includes(args.mode)) {
    throw new Error(`Unknown mode: ${args.mode}`);
  }

  const action = resolveAction(args.action);
  const profile = modelProfiles[args.model];
  const sections = buildBundle(args.feature, action, args.deltaFile, args.mode);
  const body = sections.map((section) => section.content).join('\n');
  const sectionSummaries = summarizeSections(sections, profile);
  const result = {
    feature: args.feature,
    action: args.action,
    mode: args.mode,
    model: args.model,
    profile,
    totals: {
      chars: body.length,
      words: countWords(body),
      lines: countLines(body),
      files: sections.filter((section) => section.relativePath !== '(adapter action)').length,
      estimatedTokens: estimateTokens(body, profile, sections.length - 1)
    },
    sections: sectionSummaries
  };

  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
    return;
  }

  printTextReport(result);
}

try {
  main();
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
