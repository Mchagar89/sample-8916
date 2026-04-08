# CLAUDE.md Best Practices

Summarized from official docs, the Claude Code creator (Boris Cherny), and the community.

---

## Core Philosophy

- **CLAUDE.md is a living document** — treat it like code. Review it when things go wrong, prune it regularly, and test changes by observing whether Claude's behavior actually shifts.
- **Every correction is a contribution** — when Claude does something wrong, end your correction with *"Update your CLAUDE.md so you don't make that mistake again."* Claude is very good at writing rules for itself.
- **Check it into git** — the whole team should contribute. The file compounds in value over time.
- The Claude Code team (Boris Cherny) shares a single CLAUDE.md for the Claude Code repo, with team members contributing multiple times a week.

---

## Size & Focus

- **Target: under 200 lines** — 60 lines is ideal. Bloated CLAUDE.md files cause Claude to ignore your actual instructions.
- There is roughly a **150–200 instruction budget** before compliance drops off, and the system prompt already uses ~50 of those.
- **For each line, ask**: *"Would removing this cause Claude to make mistakes?"* If not, cut it.
- **Only include what applies broadly** — for domain knowledge or workflows that are only sometimes relevant, use skills instead.
- Avoid context overload: the more inapplicable information you include, the more likely Claude will ignore your instructions.

---

## What to Include

### Essential Sections
1. **Project Overview** — tech stack, what the app does, key constraints
2. **Architecture** — high-level map of the codebase, especially important in monorepos
3. **Commands** — build, test, lint, run. Prevents Claude from guessing or using wrong commands
4. **Code Style & Conventions** — language-specific standards, formatting rules
5. **Terminology** — domain-specific terms help Claude navigate and edit the correct files
6. **Workflow Rules** — how Claude should approach work in this project (e.g. "always run tests before committing", "use bun not npm")

### Optional but Valuable
- **File references over code snippets** — use `file:line` pointers instead of inline code; snippets go stale fast
- **Known pitfalls** — things Claude tends to get wrong in this specific codebase
- **Environment setup notes** — non-obvious toolchain requirements

---

## Structure Tips

- Use **tags/headers** to section rules — as files grow longer, this prevents Claude from skimming past important instructions
- For **monorepos**: use multiple CLAUDE.md files — one at root, and additional ones in subdirectories for scope-specific rules
- Use **`.claude/rules/`** for topic-specific files (e.g. `testing.md`, `security.md`, `code-style.md`) instead of stuffing everything into one file
- Path-scoped rules (via YAML frontmatter in `.claude/rules/`) only load when Claude reads matching files — great for reducing noise

---

## What NOT to Include

- Generic best practices ("write unit tests", "don't commit secrets") — Claude already knows these
- Code snippets that will go stale — use file references instead
- Things already obvious from reading the code
- Anything only relevant to one specific task — use skills or slash commands for those

---

## Getting Started

```bash
# Auto-generate a starter CLAUDE.md from your project
/init
```

Then refine iteratively. After long sessions, look for moments where you had to re-explain or correct Claude — those are exactly the things that belong in CLAUDE.md.

---

## Advanced Patterns

- **Slash commands** (`.claude/commands/`) — for every "inner loop" workflow done many times a day (e.g. `/commit-push-pr`). Saves repeated prompting and Claude can use them too.
- **Skills** (`.claude/skills/`) — for more complex reusable workflows with their own instructions and supporting files. Think of skills as better-structured, invocable CLAUDE.md sections.
- **Plan mode** (Shift+Tab twice) — use as a read-only exploration mode before making changes. Lets Claude reason through problems without touching files.
- **Parallel sessions** — Boris runs 10–15 Claude sessions at a time for different tasks.

---

## Sources

- [Best Practices for Claude Code — Official Docs](https://code.claude.com/docs/en/best-practices)
- [Writing a good CLAUDE.md — HumanLayer Blog](https://www.humanlayer.dev/blog/writing-a-good-claude-md)
- [Using CLAUDE.MD files — Anthropic Blog](https://claude.com/blog/using-claude-md-files)
- [Boris Cherny (Claude Code creator) on X](https://x.com/bcherny/status/2007179832300581177)
- [How Boris Uses Claude Code](https://howborisusesclaudecode.com/)
- [CLAUDE.md Best Practices — UX Planet](https://uxplanet.org/claude-md-best-practices-1ef4f861ce7c)
- [50 Claude Code Tips — Builder.io](https://www.builder.io/blog/claude-code-tips-best-practices)
- [Claude Code Best Practices — Ran the Builder](https://ranthebuilder.cloud/blog/claude-code-best-practices-lessons-from-real-projects/)
- [I Rewrote My CLAUDE.md From Scratch — Medium](https://medium.com/all-about-claude/i-rewrote-my-claude-md-from-scratch-heres-the-10-section-structure-that-actually-works-c691f5beeef5)
- [awesome-claude-code — GitHub](https://github.com/hesreallyhim/awesome-claude-code)
- [claude-code-tips — ykdojo/GitHub](https://github.com/ykdojo/claude-code-tips)
- [Boris Cherny 15 tips thread](https://github.com/shanraisshan/claude-code-best-practice/blob/main/tips/claude-boris-15-tips-30-mar-26.md)
