#!/usr/bin/env node

'use strict';

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const root = path.resolve(__dirname, '..');
const workspaceRepositories = ['red-backend', 'red-web', 'red-android', 'red-database', 'red-infra'];
const repositories = fs.existsSync(path.join(root, 'red-backend'))
  ? workspaceRepositories
  : ['.'];
const ignoredDirectories = new Set([
  '.git', '.gradle', '.idea', '.kotlin', '.vite', 'build', 'coverage', 'dist', 'node_modules'
]);
const ignoredExtensions = new Set([
  '.aab', '.apk', '.class', '.gif', '.ico', '.jpeg', '.jpg', '.keystore', '.pdf', '.png',
  '.p12', '.pfx', '.so', '.webp', '.zip'
]);

// Expressions are assembled to keep this scanner from detecting its own source.
const rules = [
  ['aws-access-key-id', new RegExp(`(?:AK${'IA'}|AS${'IA'})[A-Z0-9]{16}`, 'g')],
  ['aws-secret-assignment', new RegExp(`(?:AWS_SECRET_ACCESS_${'KEY'}|aws_secret_access_${'key'})\\s*[:=]\\s*[^\\s$<{][^\\s]*`, 'g')],
  ['aws-session-token-assignment', new RegExp(`AWS_SESSION_${'TOKEN'}\\s*[:=]\\s*[^\\s$<{][^\\s]*`, 'g')],
  ['private-key', new RegExp(`BEGIN (?:RSA |EC |OPENSSH )?PRIVATE ${'KEY'}`, 'g')],
  ['authenticated-mongodb-uri', new RegExp(`mongodb(?:\\+srv)?:\\/\\/[^\\s/:@]+:[^\\s/@]+@`, 'g')]
];

const findings = [];

function report(rule, file, line, detail = '') {
  findings.push({ rule, file: path.relative(root, file), line, detail });
}

function scanFile(file) {
  if (!fs.existsSync(file)) return;
  const relativeParts = path.relative(root, file).split(path.sep);
  if (relativeParts.some((part) => ignoredDirectories.has(part))) return;
  const extension = path.extname(file).toLowerCase();
  if (ignoredExtensions.has(extension)) return;

  let content;
  try {
    content = fs.readFileSync(file, 'utf8');
  } catch {
    return;
  }
  if (content.includes('\u0000')) return;

  content.split(/\r?\n/).forEach((line, index) => {
    rules.forEach(([name, expression]) => {
      expression.lastIndex = 0;
      if (expression.test(line)) report(name, file, index + 1);
    });
  });
}

function trackedFiles(repository) {
  try {
    return execFileSync('git', ['-C', repository, 'ls-files'], {
      cwd: root,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore']
    }).split(/\r?\n/).filter(Boolean);
  } catch {
    return [];
  }
}

function isPublicClientEnvironment(file) {
  try {
    return fs.readFileSync(file, 'utf8').split(/\r?\n/).every((line) => {
      const trimmed = line.trim();
      return !trimmed || trimmed.startsWith('#') || trimmed.startsWith('VITE_');
    });
  } catch {
    return false;
  }
}

for (const repository of repositories) {
  const absolute = path.join(root, repository);
  if (!fs.existsSync(absolute)) continue;

  for (const relative of trackedFiles(repository)) {
    const file = path.join(absolute, relative);
    if (!fs.existsSync(file)) continue;
    scanFile(file);
    const base = path.basename(relative);
    const forbiddenEnv = base === '.env'
      || (/^\.env\./.test(base) && !/^\.env\.(example|test)$/.test(base));
    if (forbiddenEnv && !isPublicClientEnvironment(file)) {
      report('tracked-env-file', file, 1, 'tracked by Git');
    }
  }
}

if (findings.length) {
  console.error(`Secret scan failed with ${findings.length} finding(s). Values are intentionally suppressed.`);
  findings
    .sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line)
    .forEach(({ rule, file, line, detail }) => {
      console.error(`- ${rule}: ${file}:${line}${detail ? ` (${detail})` : ''}`);
    });
  process.exit(1);
}

console.log('Secret scan passed. No configured secret pattern or forbidden tracked env file was found.');
