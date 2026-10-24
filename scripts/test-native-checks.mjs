import test from 'node:test';
import assert from 'node:assert/strict';
import { cpSync, mkdtempSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, basename } from 'node:path';
import { spawnSync } from 'node:child_process';

const source = process.env.KODLY_JAVA_FIXTURE;
test('long generated project names pass native style and formatting checks', { skip: !process.env.KODLY_LONG_NAME_FIXTURE, timeout: 120_000 }, t => {
  const root = mkdtempSync(join(tmpdir(), 'recipe-long-name-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  cpSync(process.env.KODLY_LONG_NAME_FIXTURE, root, { recursive: true, filter: path => !['build', '.gradle'].includes(basename(path)) });
  assert.match(readFileSync(join(root, 'settings.gradle'), 'utf8'), /clean-stack-final-api-20261021/);
  const result = spawnSync('sh', ['gradlew', '--no-daemon', 'checkstyleMain', 'checkstyleTest', 'spotlessCheck'], {
    cwd: root, encoding: 'utf8', timeout: 90_000,
  });
  assert.equal(result.error, undefined);
  assert.equal(result.status, 0, result.stdout + result.stderr);
});

test('Mongo-backed application starts and migrates without configuration warnings', { skip: !process.env.KODLY_MONGO_FIXTURE, timeout: 300_000 }, t => {
  const root = mkdtempSync(join(tmpdir(), 'recipe-mongo-startup-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  cpSync(process.env.KODLY_MONGO_FIXTURE, root, { recursive: true, filter: path => !['build', '.gradle'].includes(basename(path)) });
  const result = spawnSync('sh', ['gradlew', '--no-daemon', 'integrationTest'], {
    cwd: root, encoding: 'utf8', timeout: 240_000,
  });
  assert.equal(result.error, undefined);
  const output = result.stdout + result.stderr;
  assert.equal(result.status, 0, output);
  const reports = join(root, 'build/test-results/integrationTest');
  const results = readdirSync(reports).filter(file => file.endsWith('.xml'));
  assert.ok(results.length >= 2, 'Application and Mongo integration suites must run');
  for (const file of results) {
    const xml = readFileSync(join(reports, file), 'utf8');
    assert.match(xml, /failures="0"/);
    const warnings = xml.split('\n').filter(line => /\bWARN\b|WARNING:|VM warning:|deprecated API/.test(line));
    assert.deepEqual(warnings, [], `Unexpected startup warnings in ${file}`);
  }
});

test('unit and integration tests support inline mocking without JVM agent warnings', { skip: !source, timeout: 300_000 }, t => {
  const root = mkdtempSync(join(tmpdir(), 'recipe-java-mocking-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  cpSync(source, root, { recursive: true, filter: path => !['build', '.gradle'].includes(basename(path)) });
  const directory = join(root, 'src/test/java/qualification');
  mkdirSync(directory, { recursive: true });
  for (const [name, tag] of [['InlineMockTest', ''], ['InlineMockIntegrationTest', '@org.junit.jupiter.api.Tag("integration")']]) {
    writeFileSync(join(directory, `${name}.java`), `package qualification;
${tag}
class ${name} {
  static final class Value { String read() { return "original"; } }
  @org.junit.jupiter.api.Test void supportsFinalClasses() {
    Value value = org.mockito.Mockito.mock(Value.class);
    org.mockito.Mockito.when(value.read()).thenReturn("mocked");
    org.junit.jupiter.api.Assertions.assertEquals("mocked", value.read());
  }
}
`);
  }
  const result = spawnSync('sh', ['gradlew', '--no-daemon', 'test', 'integrationTest', '--tests', 'qualification.*'], {
    cwd: root, encoding: 'utf8', timeout: 240_000,
  });
  assert.equal(result.error, undefined);
  const output = result.stdout + result.stderr;
  assert.equal(result.status, 0, output);
  assert.doesNotMatch(output, /self-attaching|agent has been loaded dynamically|Sharing is only supported|Dynamic loading of agents/i);
});

test('a generated repository rejects copied Java and style violations without Kodly tooling', { skip: !source, timeout: 300_000 }, t => {
  const root = mkdtempSync(join(tmpdir(), 'recipe-java-native-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  cpSync(source, root, { recursive: true, filter: path => !['build', '.gradle'].includes(basename(path)) });
  const directory = join(root, 'src/main/java/qualification');
  mkdirSync(directory, { recursive: true });
  const body = `  int calculate(int input) {\n${Array.from({ length: 30 }, (_, i) => `    int result${i} = input + ${i};`).join('\n')}\n    return ${Array.from({ length: 30 }, (_, i) => `result${i}`).join(' + ')};\n  }\n`;
  for (const name of ['First', 'Second']) writeFileSync(join(directory, `${name}.java`), `package qualification;\nclass ${name} {\n${body}}\n`);
  const run = tasks => {
    const result = spawnSync('sh', ['gradlew', '--no-daemon', ...tasks], { cwd: root, encoding: 'utf8', timeout: 90_000 });
    assert.equal(result.error, undefined);
    return { status: result.status, output: result.stdout + result.stderr };
  };
  const duplicated = run(['checkDuplicates']);
  assert.notEqual(duplicated.status, 0);
  assert.match(duplicated.output, /Found a .*duplicate|duplication|duplicate of/i);
  rmSync(directory, { recursive: true });
  mkdirSync(directory);
  writeFileSync(join(directory, 'Style.java'), 'package qualification;\npublic class Style{public int BadName(){return 1;}}\n');
  const style = run(['checkstyleMain']);
  assert.notEqual(style.status, 0);
  assert.match(style.output, /Checkstyle|checkstyle/);
  assert.notEqual(run(['spotlessCheck']).status, 0);
  rmSync(directory, { recursive: true });
  const tests = join(root, 'src/test/java/qualification');
  mkdirSync(tests, { recursive: true });
  writeFileSync(join(tests, 'DeliberateFailureTest.java'), `package qualification;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
class DeliberateFailureTest { @Test void failureMustStopTheBuild() { fail("qualification failure"); } }
`);
  const failedTest = run(['test']);
  assert.notEqual(failedTest.status, 0);
  assert.match(failedTest.output, /DeliberateFailureTest.*FAILED|failing tests/);
  rmSync(tests, { recursive: true });
  const restored = run(['checkDuplicates', 'checkstyleMain', 'spotlessCheck', 'test']);
  assert.equal(restored.status, 0, restored.output);
});
