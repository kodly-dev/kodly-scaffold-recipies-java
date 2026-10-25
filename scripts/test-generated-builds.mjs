import test from 'node:test';
import assert from 'node:assert/strict';
import { cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

assert.ok(process.env.KODLY_SCAFFOLDER_MODULE,
  'Set KODLY_SCAFFOLDER_MODULE to the installed scaffolder dist/index.js.');
const { scaffoldInit, scaffold } = await import(pathToFileURL(resolve(process.env.KODLY_SCAFFOLDER_MODULE)).href);
const source = fileURLToPath(new URL('../', import.meta.url));

for (const recipes of [['mongodb'], ['mongodb', 'sql', 'redis-cache']]) {
  const name = recipes.join('-');
  test(`generated ${name} passes the repository-owned build and quality checks`,
    { timeout: 600_000 }, async t => {
      const root = mkdtempSync(join(tmpdir(), `kodly-generated-${name}-`));
      t.after(() => rmSync(root, { recursive: true, force: true }));
      await scaffoldInit({ group: 'dev.kodly.qualification', projectName: 'generated-check',
        targetDir: root, templateDir: join(source, 'skeleton') });
      for (const recipe of recipes) {
        const recipeDir = join(source, 'recipes', recipe);
        const manifest = JSON.parse(readFileSync(join(recipeDir, 'manifest.json'), 'utf8'));
        await scaffold({ recipe: `${recipe}@${manifest.version}`, workspaceDir: root, recipeDir });
      }
      // Unit tests, Checkstyle (including copied recipe tests), Spotless and CPD all run.
      // Live-provider integration already has a separate CI step with disposable services.
      const args = ['gradlew', 'build', '-x', 'integrationTest', '--no-daemon', '--no-watch-fs',
        '--max-workers=2', '--continue'];
      const result = spawnSync('sh', args, { cwd: root, encoding: 'utf8',
        env: { ...process.env, CI: 'true' }, timeout: 540_000, maxBuffer: 8 * 1024 * 1024 });
      const output = (result.stdout ?? '') + (result.stderr ?? '');
      if (process.env.KODLY_GENERATED_REPORT_DIR) {
        const destination = resolve(process.env.KODLY_GENERATED_REPORT_DIR, name);
        mkdirSync(destination, { recursive: true });
        writeFileSync(join(destination, 'build.log'), output);
        for (const directory of ['reports/checkstyle', 'test-results/test']) {
          const report = join(root, 'build', directory);
          if (existsSync(report)) cpSync(report, join(destination, directory), { recursive: true });
        }
      }
      assert.equal(result.error, undefined, output);
      assert.equal(result.status, 0, output);
      for (const task of ['checkstyleMain', 'checkstyleTest', 'spotlessCheck', 'checkDuplicates', 'test'])
        assert.ok(output.includes(`> Task :${task}`), `Missing generated-project check: ${task}`);
    });
}
