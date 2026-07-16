import { appendFile, readFile } from 'node:fs/promises'

const report = process.argv[2]
const title = process.argv[3] ?? 'Web client'

let markdown
try {
  const coverage = JSON.parse(await readFile(report, 'utf8')).total
  const metrics = [
    ['Statements', coverage.statements],
    ['Branches', coverage.branches],
    ['Functions', coverage.functions],
    ['Lines', coverage.lines],
  ]
  const rows = metrics
    .map(([name, metric]) => `| ${name} | ${metric.covered} | ${metric.total} | ${metric.pct.toFixed(2)}% |`)
    .join('\n')
  markdown = `## ${title} coverage\n\n| Metric | Covered | Total | Coverage |\n|---|---:|---:|---:|\n${rows}\n`
} catch (error) {
  markdown = `## ${title} coverage\n\nCoverage report was not generated.\n`
  console.error(error instanceof Error ? error.message : error)
}

if (process.env.GITHUB_STEP_SUMMARY) {
  await appendFile(process.env.GITHUB_STEP_SUMMARY, markdown)
} else {
  process.stdout.write(markdown)
}
