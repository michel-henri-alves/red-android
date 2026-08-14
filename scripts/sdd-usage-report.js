#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const root = process.cwd();
const featuresDir = path.join(root, 'docs', 'features');

function walk(dir) {
  if (!fs.existsSync(dir)) return [];

  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(fullPath);
    return [fullPath];
  });
}

function parseUsage(content) {
  const match = content.match(
    /- Uso Codex: (\d+) turn\(s\), (\d+) input tokens, (\d+) cached input tokens, (\d+) input tokens nao-cacheados, (\d+) output tokens, (\d+) reasoning output tokens/
  );

  if (!match) return null;

  return {
    turns: Number(match[1]),
    input: Number(match[2]),
    cached: Number(match[3]),
    uncachedInput: Number(match[4]),
    output: Number(match[5]),
    reasoning: Number(match[6]),
  };
}

function parseField(content, name) {
  const match = content.match(new RegExp(`- ${name}: (.+)`));
  return match ? match[1].trim() : '';
}

const rows = walk(featuresDir)
  .filter((file) => file.endsWith('-execution-log.md'))
  .map((file) => {
    const content = fs.readFileSync(file, 'utf8');
    const usage = parseUsage(content);
    if (!usage) return null;

    return {
      file,
      feature: parseField(content, 'Feature') || path.relative(featuresDir, file).split(path.sep)[0],
      action: parseField(content, 'Acao') || 'run',
      task: parseField(content, 'Tarefa') || 'none',
      status: parseField(content, 'Status') || '',
      ...usage,
    };
  })
  .filter(Boolean);

if (rows.length === 0) {
  console.log('No Codex usage logs found under docs/features/*/runs.');
  process.exit(0);
}

const totals = rows.reduce(
  (acc, row) => {
    for (const key of ['turns', 'input', 'cached', 'uncachedInput', 'output', 'reasoning']) {
      acc[key] += row[key];
    }
    return acc;
  },
  { turns: 0, input: 0, cached: 0, uncachedInput: 0, output: 0, reasoning: 0 }
);

console.table(
  rows.map((row) => ({
    feature: row.feature,
    action: row.action,
    task: row.task,
    status: row.status,
    turns: row.turns,
    input: row.input,
    cached: row.cached,
    uncached_input: row.uncachedInput,
    output: row.output,
    reasoning: row.reasoning,
  }))
);

console.log('Totals');
console.table([
  {
    runs: rows.length,
    turns: totals.turns,
    input: totals.input,
    cached: totals.cached,
    uncached_input: totals.uncachedInput,
    output: totals.output,
    reasoning: totals.reasoning,
  },
]);
