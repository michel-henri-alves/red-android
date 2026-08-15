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
    const openApi = JSON.parse(read(backendOpenApi));
    const sales = openApi.components?.schemas?.Sales;
    const postSalesSchema = openApi.paths?.['/sales']?.post?.requestBody?.content?.['application/json']?.schema;

    if (postSalesSchema?.$ref !== '#/components/schemas/Sales') {
      failures.push('POST /sales must reference the Sales request schema.');
    }
    if (!sales) {
      failures.push('Backend OpenAPI is missing components.schemas.Sales.');
    } else {
      const expectedTypes = {
        items: 'array',
        paymentMethod: 'array',
        amountPaid: 'array',
        discount: 'number',
        change: 'number',
        vendor: 'string',
        realizedAt: 'string'
      };

      Object.entries(expectedTypes).forEach(([field, type]) => {
        if (sales.properties?.[field]?.type !== type) {
          failures.push(`Sales.${field} must be OpenAPI type ${type}.`);
        }
      });
      if (sales.properties?.items?.items?.type !== 'object') {
        failures.push('Sales.items entries must be objects.');
      }
      if (sales.properties?.amountPaid?.items?.type !== 'number') {
        failures.push('Sales.amountPaid entries must be numbers.');
      }
      ['items', 'paymentMethod', 'amountPaid'].forEach((field) => {
        if (!sales.required?.includes(field)) {
          failures.push(`Sales.${field} must be required.`);
        }
      });
    }
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

const salesRequestFile = path.join(sourceRoot, 'com', 'm4', 'red_android', 'data', 'models', 'SalesRequest.kt');
if (!fs.existsSync(salesRequestFile)) {
  failures.push('Android SalesRequest DTO is missing.');
} else {
  const salesRequest = read(salesRequestFile);
  if (!/val amountPaid:\s*List<Double>/.test(salesRequest)) {
    failures.push('Android amountPaid must be a non-null List<Double>.');
  }
  if (/data class SalesRequest[\s\S]*?\bval (code|companyId):/.test(salesRequest)) {
    failures.push('Android SalesRequest must not send local code or tenant fields.');
  }
}

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
