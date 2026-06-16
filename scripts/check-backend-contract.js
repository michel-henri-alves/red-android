const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const backendOpenApi = path.resolve(root, '..', 'red-backend', 'docs', 'contracts', 'openapi.json');
const sourceRoot = path.join(root, 'app', 'src', 'main', 'java');

const warnings = [];
const failures = [];

function walk(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(fullPath);
    return [fullPath];
  });
}

function read(file) {
  return fs.readFileSync(file, 'utf8');
}

if (!fs.existsSync(backendOpenApi)) {
  warnings.push(`Backend OpenAPI not found: ${path.relative(root, backendOpenApi)}`);
} else {
  try {
    JSON.parse(read(backendOpenApi));
  } catch (error) {
    failures.push(`Backend OpenAPI is not valid JSON: ${error.message}`);
  }
}

const kotlinFiles = walk(sourceRoot).filter((file) => file.endsWith('.kt'));
const retrofitFiles = kotlinFiles.filter((file) => /retrofit2\.http|@GET|@POST|@PUT|@PATCH|@DELETE/.test(read(file)));

if (kotlinFiles.some((file) => /Retrofit\.Builder|retrofit2/.test(read(file))) && retrofitFiles.length === 0) {
  warnings.push('Retrofit dependency appears to be used, but no Retrofit service interface annotations were found.');
}

retrofitFiles.forEach((file) => {
  const content = read(file);
  if (/Authorization|Bearer|companyId|tenant/i.test(content) && !/Header|Interceptor|Request/.test(content)) {
    warnings.push(`Review auth/tenant header construction in ${path.relative(root, file)}`);
  }
});

if (warnings.length) {
  console.log('Contract warnings:');
  warnings.forEach((message) => console.log(`- ${message}`));
}

if (failures.length) {
  console.error('Contract check failed:');
  failures.forEach((message) => console.error(`- ${message}`));
  process.exit(1);
}

console.log('Backend contract check passed.');
