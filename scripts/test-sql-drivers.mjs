import test from 'node:test';
import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

assert.ok(process.env.KODLY_SCAFFOLDER_MODULE,
  'Set KODLY_SCAFFOLDER_MODULE to the built scaffolder dist/index.js to qualify generated SQL projects.');
const { scaffoldInit, scaffold } = await import(pathToFileURL(resolve(process.env.KODLY_SCAFFOLDER_MODULE)).href);
const source = fileURLToPath(new URL('../', import.meta.url));
const manifest = JSON.parse(readFileSync(join(source, 'recipes/sql/manifest.json'), 'utf8'));

for (const dialect of ['postgresql', 'mysql']) {
  test(`generated ${dialect} project passes every bound datasource/Flyway guard with only its own driver`,
    { timeout: 300_000 }, async t => {
      const root = mkdtempSync(join(tmpdir(), `recipe-${dialect}-`));
      t.after(() => rmSync(root, { recursive: true, force: true }));
      await scaffoldInit({ group: 'dev.kodly.qualification', projectName: `check-${dialect}`,
        targetDir: root, templateDir: join(source, 'skeleton') });
      await scaffold({ recipe: `sql@${manifest.version}`, workspaceDir: root,
        recipeDir: join(source, 'recipes/sql') });

      const build = join(root, 'build.gradle');
      const config = join(root, 'src/main/resources/application.yml');
      if (dialect === 'mysql') {
        // Follow docs/SQL.md: replace the production dependencies, without a test-only PostgreSQL driver.
        const original = readFileSync(build, 'utf8');
        assert.match(original, /runtimeOnly 'org.postgresql:postgresql'/);
        assert.match(original, /runtimeOnly 'org.flywaydb:flyway-database-postgresql'/);
        writeFileSync(build, original
          .replace("runtimeOnly 'org.postgresql:postgresql'", "runtimeOnly 'com.mysql:mysql-connector-j'")
          .replace("runtimeOnly 'org.flywaydb:flyway-database-postgresql'", "runtimeOnly 'org.flywaydb:flyway-mysql'"));
        writeFileSync(config, readFileSync(config, 'utf8')
          .replaceAll('jdbc:postgresql://', 'jdbc:mysql://').replaceAll(':5432/', ':3306/')
          .replaceAll('org.postgresql.Driver', 'com.mysql.cj.jdbc.Driver'));
      }

      const args = ['gradlew', '--no-daemon', '--no-build-cache', '--rerun-tasks', '--max-workers=2',
        'test', '--tests', '*SqlBoundDataSourceTest', '--tests', '*SqlJdbcUrlIsolationTest',
        'dependencies', '--configuration', 'testRuntimeClasspath'];
      if (process.env.KODLY_GRADLE_OFFLINE === 'true') args.splice(1, 0, '--offline');
      const result = spawnSync('sh', args, { cwd: root, encoding: 'utf8',
        env: { ...process.env, CI: 'true' }, timeout: 240_000, maxBuffer: 8 * 1024 * 1024 });
      const output = (result.stdout ?? '') + (result.stderr ?? '');
      const reports = join(root, 'build/test-results/test');
      const suites = existsSync(reports) ? readdirSync(reports).filter(file => file.endsWith('.xml')) : [];

      if (process.env.KODLY_SQL_REPORT_DIR) {
        const evidence = resolve(process.env.KODLY_SQL_REPORT_DIR, dialect);
        mkdirSync(evidence, { recursive: true });
        writeFileSync(join(evidence, 'gradle.log'), output + (result.error ? String(result.error) : ''));
        writeFileSync(join(evidence, 'command.json'), JSON.stringify({ command: ['sh', ...args], status: result.status }, null, 2) + '\n');
        copyFileSync(build, join(evidence, 'build.gradle'));
        copyFileSync(config, join(evidence, 'application.yml'));
        for (const suite of suites) copyFileSync(join(reports, suite), join(evidence, suite));
      }

      assert.equal(result.error, undefined, output);
      assert.equal(result.status, 0, output);
      assert.equal(suites.length, 2, 'Both the bound datasource/Flyway and driver-routing suites must run');
      const boundSuite = suites.find(file => file.endsWith('.SqlBoundDataSourceTest.xml'));
      const routingSuite = suites.find(file => file.endsWith('.SqlJdbcUrlIsolationTest.xml'));
      assert.ok(boundSuite, 'Missing bound datasource suite');
      assert.ok(routingSuite, 'Missing driver-routing suite');
      const xml = readFileSync(join(reports, boundSuite), 'utf8');
      assert.match(xml, /tests="21"/);
      for (const attribute of ['failures', 'errors', 'skipped']) assert.match(xml, new RegExp(`${attribute}="0"`));
      assert.match(xml, /name="separateFlywayConnectionUsesTheTestProviderBeforeMigrationsRun\(\)"/);
      const routingXml = readFileSync(join(reports, routingSuite), 'utf8');
      assert.match(routingXml, /tests="110"/);
      for (const attribute of ['failures', 'errors', 'skipped']) assert.match(routingXml, new RegExp(`${attribute}="0"`));
      assert.match(routingXml, /actualDriverDatabaseOverrideIsRefusedBeforeBeansInitialize/);
      assert.match(routingXml, /#\?PGDBNAME=application/);
      assert.match(routingXml, /spelling = &quot;application%00&quot;/);
      assert.match(routingXml, /spelling = &quot;%1Fapplication&quot;/);
      const [own, other] = dialect === 'mysql' ? ['com.mysql:mysql-connector-j', 'org.postgresql:postgresql']
        : ['org.postgresql:postgresql', 'com.mysql:mysql-connector-j'];
      assert.ok(output.includes(own), `Missing ${dialect} runtime driver`);
      assert.ok(!output.includes(other), `Unexpected additional driver: ${other}`);
    });
}
