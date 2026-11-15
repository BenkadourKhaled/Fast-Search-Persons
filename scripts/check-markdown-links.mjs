#!/usr/bin/env node
/**
 * Vérifie les liens relatifs et les images des fichiers Markdown du monorepo.
 *
 * Usage :
 *   node scripts/check-markdown-links.mjs [racine]
 *
 * Les liens http(s), mailto, tel, data et les ancres pures sont ignorés.
 * Le script sort avec le code 1 si un lien relatif est manquant.
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const root = resolve(process.argv[2] ?? join(scriptDirectory, '..'));
const skipDirectories = new Set([
  '.git',
  '.github', // les templates YAML ne sont pas des documents Markdown
  'node_modules',
  'target',
  'dist',
  'dist-ssr',
  'data',
  'coverage',
  '.vite',
  'chrome-profile',
]);
const skipFiles = new Set(['package-lock.json']);

const markdownFiles = [];

function walk(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!skipDirectories.has(entry.name)) walk(join(directory, entry.name));
      continue;
    }
    if (skipFiles.has(entry.name)) continue;
    if (extname(entry.name).toLowerCase() === '.md') {
      markdownFiles.push(join(directory, entry.name));
    }
  }
}

function stripCodeFences(text) {
  return text.replace(/```[\s\S]*?```/g, '');
}

function isExternal(target) {
  return (
    /^(?:https?:|mailto:|tel:|data:|javascript:)/i.test(target) ||
    target.startsWith('#')
  );
}

function cleanTarget(raw) {
  let target = raw.trim();
  if (target.startsWith('<') && target.endsWith('>')) {
    target = target.slice(1, -1);
  }
  return target.split('#')[0].split('?')[0];
}

function checkFile(file) {
  const text = stripCodeFences(readFileSync(file, 'utf8'));
  const targets = [];
  const markdownLink = /\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;
  const htmlLink = /(?:src|href)="([^"]+)"/g;

  for (const regex of [markdownLink, htmlLink]) {
    let match;
    while ((match = regex.exec(text)) !== null) {
      targets.push(match[1]);
    }
  }

  const missing = [];
  for (const raw of targets) {
    if (isExternal(raw)) continue;
    const target = cleanTarget(raw);
    if (!target) continue;

    let decoded;
    try {
      decoded = decodeURIComponent(target);
    } catch {
      decoded = target;
    }

    const absolute = resolve(dirname(file), decoded);
    if (!statSafeExists(absolute)) {
      missing.push({ raw, absolute });
    }
  }
  return missing;
}

function statSafeExists(path) {
  try {
    statSync(path);
    return true;
  } catch {
    return false;
  }
}

walk(root);

let missingCount = 0;
let linkCount = 0;

for (const file of markdownFiles) {
  const missing = checkFile(file);
  if (missing.length > 0) {
    for (const item of missing) {
      console.error(
        `MANQUANT ${relative(root, file)} -> ${item.raw} (${relative(root, item.absolute)})`
      );
      missingCount += 1;
    }
  }
  linkCount += 1;
}

if (missingCount > 0) {
  console.error(
    `\n${missingCount} lien(s) relatif(s) manquant(s) dans ${markdownFiles.length} fichier(s) Markdown.`
  );
  process.exit(1);
}

console.log(
  `Liens Markdown : OK (${markdownFiles.length} fichiers analysés, aucun lien relatif manquant).`
);
