const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const agentsDir = path.join(root, 'ai/agents');
const skillsDir = path.join(root, 'ai/skills');

const recommendedOrder = [
  'sdd-spec-reviewer.md',
  'sdd-planner.md',
  'implementation-engineer.md',
  'test-engineer.md',
  'backend-contract-reviewer.md',
  'cross-project-integrator.md',
  'mobile-ux-regression-reviewer.md',
  'security-tenant-isolation-reviewer.md',
  'code-reviewer.md',
  'performance-cost-reviewer.md',
  'release-gate-reviewer.md'
];

function readAgentTitle(fileName) {
  const content = fs.readFileSync(path.join(agentsDir, fileName), 'utf8');
  const title = content.match(/^#\s+(.+)$/m)?.[1] || fileName;
  const objective = content.match(/^## Objective\n([\s\S]*?)(?:\n## |\n$)/m)?.[1]
    ?.trim()
    ?.replace(/\s+/g, ' ');

  return { title, objective };
}

function readSkill(dirName) {
  const relativePath = `ai/skills/${dirName}/SKILL.md`;
  const content = fs.readFileSync(path.join(root, relativePath), 'utf8');
  const name = content.match(/^name:\s*(.+)$/m)?.[1]?.trim() || dirName;
  const description = content.match(/^description:\s*(.+)$/m)?.[1]?.trim() || '';

  return { name, description, relativePath };
}

function main() {
  if (!fs.existsSync(agentsDir)) {
    console.error('Missing ai/agents directory.');
    process.exit(1);
  }

  console.log('SDD agents for red-android');
  console.log('');
  console.log('Recommended sequence:');

  recommendedOrder.forEach((fileName, index) => {
    const relativePath = `ai/agents/${fileName}`;
    const absolutePath = path.join(root, relativePath);

    if (!fs.existsSync(absolutePath)) {
      console.log(`${index + 1}. Missing: ${relativePath}`);
      return;
    }

    const agent = readAgentTitle(fileName);
    console.log(`${index + 1}. ${agent.title}`);
    console.log(`   File: ${relativePath}`);
    if (agent.objective) {
      console.log(`   Objective: ${agent.objective}`);
    }
  });

  console.log('');
  console.log('Orchestration guide: docs/sdd/agents.md');

  if (fs.existsSync(skillsDir)) {
    const skillDirs = fs.readdirSync(skillsDir, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => entry.name)
      .filter((dirName) => fs.existsSync(path.join(skillsDir, dirName, 'SKILL.md')))
      .sort();

    if (skillDirs.length) {
      console.log('');
      console.log('Project skills:');
      skillDirs.forEach((dirName) => {
        const skill = readSkill(dirName);
        console.log(`- ${skill.name}`);
        console.log(`  File: ${skill.relativePath}`);
        if (skill.description) {
          console.log(`  Description: ${skill.description}`);
        }
      });
      console.log('');
      console.log('Skill selection guide: docs/sdd/skills.md');
    }
  }
}

main();
