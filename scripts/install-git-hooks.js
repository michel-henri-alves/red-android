const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const hooksDir = path.join(root, '.git', 'hooks');
const sourceDir = path.join(root, 'scripts', 'hooks');

function installHook(name) {
  const source = path.join(sourceDir, name);
  const target = path.join(hooksDir, name);

  if (!fs.existsSync(source)) {
    throw new Error(`Missing hook template: scripts/hooks/${name}`);
  }

  fs.mkdirSync(hooksDir, { recursive: true });
  fs.copyFileSync(source, target);
  fs.chmodSync(target, 0o755);
  console.log(`Installed .git/hooks/${name}`);
}

['pre-commit', 'pre-push'].forEach(installHook);
