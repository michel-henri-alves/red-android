const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

const requiredFiles = [
  'docs/sdd/constitution.md',
  'docs/sdd/agents.md',
  'docs/sdd/context-map.md',
  'docs/sdd/mcp.md',
  'docs/sdd/operation-manual.md',
  'docs/sdd/quality-gates.md',
  'docs/sdd/skills.md',
  'docs/sdd/workflow.md',
  'ai/context/mobile.md',
  'ai/prompts/implement-from-spec.md',
  'package.json',
  'app/build.gradle.kts'
];

const requiredAgents = [
  'ai/agents/sdd-spec-reviewer.md',
  'ai/agents/sdd-planner.md',
  'ai/agents/android-architecture-engineer.md',
  'ai/agents/kotlin-software-engineer.md',
  'ai/agents/implementation-engineer.md',
  'ai/agents/test-engineer.md',
  'ai/agents/backend-contract-reviewer.md',
  'ai/agents/mobile-ux-regression-reviewer.md',
  'ai/agents/security-tenant-isolation-reviewer.md',
  'ai/agents/cross-project-integrator.md',
  'ai/agents/code-reviewer.md',
  'ai/agents/performance-cost-reviewer.md',
  'ai/agents/release-gate-reviewer.md'
];

const requiredSkills = [
  'ai/skills/red-android-compose-state/SKILL.md',
  'ai/skills/red-android-backend-contract/SKILL.md',
  'ai/skills/red-android-camera-barcode/SKILL.md',
  'ai/skills/red-android-auth-tenant-security/SKILL.md',
  'ai/skills/red-android-testing-quality/SKILL.md',
  'ai/skills/red-cross-project-contract-change/SKILL.md',
  'ai/skills/red-sdd-feature-closure/SKILL.md'
];

const failures = [];
const warnings = [];

const exists = (relativePath) => fs.existsSync(path.join(root, relativePath));
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8');
const fail = (message) => failures.push(message);
const warn = (message) => warnings.push(message);

function requireFile(relativePath) {
  if (!exists(relativePath)) fail(`Missing required file: ${relativePath}`);
}

function listMarkdownFiles(relativeDir) {
  const absoluteDir = path.join(root, relativeDir);
  if (!fs.existsSync(absoluteDir)) return [];
  return fs.readdirSync(absoluteDir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.md'))
    .map((entry) => path.join(relativeDir, entry.name));
}

function extractSection(content, sectionName) {
  const sectionPattern = new RegExp(`^##\\s+${sectionName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`, 'im');
  const sectionMatch = content.match(sectionPattern);
  if (!sectionMatch || sectionMatch.index === undefined) return null;
  const start = sectionMatch.index + sectionMatch[0].length;
  const rest = content.slice(start);
  const nextSectionMatch = rest.match(/^##\s+/m);
  const end = nextSectionMatch && nextSectionMatch.index !== undefined ? start + nextSectionMatch.index : content.length;
  return content.slice(start, end).trim();
}

function extractReqIds(content) {
  return [...content.matchAll(/\bREQ-[A-Z0-9-]+-\d{3}\b/g)].map((match) => match[0]);
}

function extractTaskIds(content) {
  return [...content.matchAll(/\bT\d{3}\b/g)].map((match) => match[0]);
}

function unique(values) {
  return [...new Set(values)];
}

function hasClarificationMarker(content) {
  return /\[(?:NEEDS CLARIFICATION|PRECISA ESCLARECER):[^\]]+\]/i.test(content);
}

function hasTaskMetadata(content) {
  return /Agent:/i.test(content) && /Depends on:/i.test(content) && /Verification:/i.test(content);
}

function hasIncompleteTasks(content) {
  return /^\s*-\s*\[\s\]\s*T\d{3}\b/m.test(content);
}

function isCriticalFeature(specContent) {
  const criticality = extractSection(specContent, 'Criticality') || '';
  return /\bcritical\b/i.test(criticality);
}

function isYes(value) {
  return ['yes', 'sim', 'true'].includes((value || '').trim().toLowerCase());
}

function parseImpactClassification(content) {
  const section = extractSection(content, 'Impact Classification');
  if (!section) return null;

  const fields = {};
  [...section.matchAll(/^\s*-\s*([^:\n]+):\s*(.+?)\s*$/gm)].forEach((match) => {
    fields[match[1].trim().toLowerCase()] = match[2].trim().toLowerCase();
  });

  return { section, fields };
}

function checkPackageScripts() {
  const packageJson = JSON.parse(read('package.json'));
  const scripts = packageJson.scripts || {};
  [
    'build',
    'lint',
    'test',
    'test:coverage',
    'static:analysis',
    'quality:check',
    'contracts:check',
    'sdd:new',
    'sdd:new:translate',
    'sdd:new:ai',
    'sdd:agent',
    'sdd:agents',
    'sdd:check',
    'sdd:estimate',
    'sdd:run',
    'hooks:install'
  ].forEach((scriptName) => {
    if (!scripts[scriptName]) fail(`package.json is missing script: ${scriptName}`);
  });
}

function checkTemplateDocs() {
  [
    'docs/features/_template/spec.md',
    'docs/features/_template/plan.md',
    'docs/features/_template/tasks.md'
  ].forEach(requireFile);

  if (exists('docs/features/_template/plan.md')) {
    const plan = read('docs/features/_template/plan.md');
    ['npm run sdd:check', 'npm run contracts:check', 'npm run test', 'npm run lint', 'npm run build'].forEach((command) => {
      if (!plan.includes(command)) fail(`Feature plan template is missing verification command: ${command}`);
    });
    ['Definition Of Done', 'Gate Checks', 'Canonical Documentation'].forEach((section) => {
      if (!plan.includes(section)) fail(`Feature plan template is missing section: ${section}`);
    });
  }
}

function checkAgents() {
  requiredAgents.forEach((agentPath) => {
    requireFile(agentPath);
    if (exists(agentPath) && !read(agentPath).includes('## Objective')) {
      fail(`Agent file is missing an Objective section: ${agentPath}`);
    }
  });
}

function checkSkills() {
  requiredSkills.forEach((skillPath) => {
    requireFile(skillPath);
    if (!exists(skillPath)) return;
    const skill = read(skillPath);
    if (!/^---\n[\s\S]*?\n---/m.test(skill)) fail(`Skill file is missing YAML frontmatter: ${skillPath}`);
    ['name:', 'description:', '## Use When', '## Workflow'].forEach((requiredText) => {
      if (!skill.includes(requiredText)) fail(`Skill file ${skillPath} is missing required text: ${requiredText}`);
    });
  });

  if (exists('docs/sdd/skills.md')) {
    const skillsGuide = read('docs/sdd/skills.md');
    requiredSkills.forEach((skillPath) => {
      const skillName = path.basename(path.dirname(skillPath));
      if (!skillsGuide.includes(skillName)) fail(`docs/sdd/skills.md does not reference required skill: ${skillName}`);
    });
  }
}

function checkFeatureImpact(featureDir, spec, plan, tasks) {
  const classification = parseImpactClassification(spec);
  if (!classification) {
    warn(`Feature ${featureDir} spec.md has no Impact Classification section`);
    return;
  }

  [
    'impact',
    'creates new domain/workflow',
    'changes domain model',
    'changes public api contract',
    'changes durable architecture/project memory'
  ].forEach((field) => {
    if (!classification.fields[field]) fail(`Feature ${featureDir} Impact Classification is missing field: ${field}`);
  });

  const isHighImpact = classification.fields.impact === 'high'
    || isYes(classification.fields['creates new domain/workflow'])
    || isYes(classification.fields['changes domain model'])
    || isYes(classification.fields['changes public api contract'])
    || isYes(classification.fields['changes durable architecture/project memory']);

  if (!isHighImpact) return;

  const combined = `${classification.section}\n${plan}\n${tasks}`;
  if (!/docs\/(?:specs|tasks|memory)\/[^`\s]+\.md/.test(combined)) {
    fail(`Feature ${featureDir} is high impact but does not reference canonical docs/specs, docs/tasks, or docs/memory updates`);
  }

  if (!hasIncompleteTasks(tasks)) {
    [...combined.matchAll(/`(docs\/(?:specs|tasks|memory)\/[^`]+\.md)`/g)].forEach((match) => {
      if (!exists(match[1])) fail(`Feature ${featureDir} is complete but referenced canonical doc does not exist: ${match[1]}`);
    });
  }
}

function checkFeatureDocs() {
  const featureRoot = path.join(root, 'docs/features');
  if (!fs.existsSync(featureRoot)) {
    fail('Missing docs/features directory');
    return;
  }

  const featureDirs = fs.readdirSync(featureRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && !entry.name.startsWith('_'))
    .map((entry) => entry.name);

  if (!featureDirs.length) warn('No feature folders found in docs/features');

  featureDirs.forEach((featureDir) => {
    const specPath = `docs/features/${featureDir}/spec.md`;
    const planPath = `docs/features/${featureDir}/plan.md`;
    const tasksPath = `docs/features/${featureDir}/tasks.md`;

    [specPath, planPath, tasksPath].forEach(requireFile);

    if (exists(specPath)) {
      const spec = read(specPath);
      const reqIds = unique(extractReqIds(spec));
      if (!reqIds.length) fail(`Feature ${featureDir} spec.md has no REQ-* requirement ids`);
      if (hasClarificationMarker(spec)) fail(`Feature ${featureDir} has unresolved clarification marker in spec.md`);

      if (exists(tasksPath)) {
        const tasks = read(tasksPath);
        const taskReqIds = unique(extractReqIds(tasks));
        reqIds.forEach((reqId) => {
          if (!taskReqIds.includes(reqId)) fail(`Feature ${featureDir} tasks.md does not reference ${reqId}`);
        });
      }

      if (exists(planPath) && exists(tasksPath)) {
        checkFeatureImpact(featureDir, spec, read(planPath), read(tasksPath));
      }

      if (isCriticalFeature(spec) && exists(planPath)) {
        const plan = read(planPath);
        if (!/app\/src\/(?:test|androidTest)\/[^`\s]+\.kt/.test(plan)) {
          fail(`Critical feature ${featureDir} plan.md must list focused Android test files`);
        }
      }
    }

    if (exists(tasksPath)) {
      const tasks = read(tasksPath);
      if (hasClarificationMarker(tasks)) fail(`Feature ${featureDir} has unresolved clarification marker in tasks.md`);
      if (!extractTaskIds(tasks).length) warn(`Feature ${featureDir} tasks.md has no Txxx task ids`);
      if (extractTaskIds(tasks).length && !hasTaskMetadata(tasks)) {
        fail(`Feature ${featureDir} tasks.md uses Txxx ids but is missing Agent, Depends on, or Verification metadata`);
      }
    }

    if (exists(planPath)) {
      const plan = read(planPath);
      if (hasClarificationMarker(plan)) fail(`Feature ${featureDir} has unresolved clarification marker in plan.md`);
      ['npm run sdd:check', 'npm run contracts:check', 'npm run test', 'npm run lint', 'npm run build'].forEach((command) => {
        if (!plan.includes(command)) fail(`Feature ${featureDir} plan.md is missing verification command: ${command}`);
      });
    }
  });
}

function checkDomainSpecs() {
  const specFiles = listMarkdownFiles('docs/specs').filter((file) => file.endsWith('.spec.md'));
  const taskNames = new Set(
    listMarkdownFiles('docs/tasks').map((file) => path.basename(file).replace(/\.tasks\.md$/, ''))
  );

  specFiles.forEach((specFile) => {
    const domainName = path.basename(specFile).replace(/\.spec\.md$/, '');
    if (!taskNames.has(domainName)) fail(`Domain spec ${specFile} has no matching docs/tasks/${domainName}.tasks.md`);
  });
}

function checkAndroidGeneratedArtifacts() {
  const forbiddenDirs = ['.gradle', 'app/build', 'app/debug', 'app/release'];
  forbiddenDirs.forEach((relativePath) => {
    if (exists(relativePath)) {
      warn(`Generated/local directory exists and should not be modified by SDD work: ${relativePath}`);
    }
  });
}

requiredFiles.forEach(requireFile);
if (exists('package.json')) checkPackageScripts();
checkTemplateDocs();
checkAgents();
checkSkills();
checkFeatureDocs();
checkDomainSpecs();
checkAndroidGeneratedArtifacts();

if (warnings.length) {
  console.log('SDD warnings:');
  warnings.forEach((message) => console.log(`- ${message}`));
}

if (failures.length) {
  console.error('SDD check failed:');
  failures.forEach((message) => console.error(`- ${message}`));
  process.exit(1);
}

console.log('SDD check passed.');
