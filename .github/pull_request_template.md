<!--
===================================================================================
IMPORTANT: Remove this comment before submitting your PR. Set your PR title before submitting!
===================================================================================

PR Title Format: <emoji> <type>: <Description>

Type Emojis:
  🚀 feat/feature    - New feature or enhancement
  🐛 fix/bugfix      - Non-breaking fix for an issue
  🔥 hotfix          - Critical bug fix for production
  📝 docs            - Documentation only changes
  💄 style           - Code style/formatting changes (whitespace, formatting, etc.)
  ♻️  refactor       - Code refactoring without functionality change
  ⚡ perf            - Performance improvement
  ✅ test/tests      - Adding or updating tests
  🔧 build/ci        - Build system or CI/CD changes
  🧹 chore           - Maintenance tasks, dependency updates
  🔄 revert          - Reverting previous changes

Examples:
  ✅ 🚀 feat: Add user authentication
  ✅ 🐛 fix: Resolve login button issue
  ✅ 📝 docs: Update API documentation
  ✅ 🔥 hotfix: Fix critical data corruption issue
  ✅ ♻️  refactor: Restructure backend services
  ✅ ⚡ perf: Optimize database queries

Note: Scopes are optional. You can use them if needed:
  ✅ 🚀 feat(backend): Add user authentication
  ✅ 🐛 fix(frontend): Resolve login button issue

===================================================================================
-->

## PR Title
<!--
Construct your PR title here, then copy it to the actual PR title field above.
Use the emoji that matches your change type from the list above.
Important: Remove PR Title section from PR body before submitting.
-->

**Your PR Title:**
```
<emoji> <type>: <Description>
```

**Example:**
```
🚀 feat: Add user authentication system
```

## Summary
<!--
Provide a clear and concise summary of what this PR does.
Requirements:
- Minimum 50 characters for meaningful context
- PR description must be at least 100 characters total
- Describe the problem being solved and the solution approach
-->



## Changes Made
<!-- List the specific changes made in this PR. Be detailed and specific. -->
-
-
-

## Pre-Submission Checklist

### Code Quality
- [ ] Code follows project coding conventions
- [ ] Self-review completed
- [ ] Code is well-commented (especially complex logic)
- [ ] No new warnings or errors
- [ ] Code formatted: `make format`
- [ ] Linting passed: `make lint`

### Testing Requirements
- [ ] Tests added/updated for new functionality
- [ ] All tests pass locally: `make test`
- [ ] Test coverage ≥80% for new code
- [ ] Integration tests added (if applicable)

### Documentation
- [ ] Code documentation updated (docstrings, comments)
- [ ] API documentation updated (if API changes)
- [ ] README updated (if setup/usage changes)
- [ ] **Design Documentation updated on Wiki** (if architectural changes)
  - [ ] Revision history incremented
  - [ ] System architecture diagram updated (if applicable)
  - [ ] Class diagrams updated (frontend/backend, if applicable)
  - [ ] ER diagram updated (if database changes)
  - [ ] Testing plan updated (Iteration 3+)
  - Wiki: [Design Documentation](https://github.com/snuhcs-course/swpp-2025-project-team-10/wiki/Design-Documentation)

### Dependencies & Breaking Changes
- [ ] No new dependencies (or justified below)
- [ ] Dependencies pinned to specific versions
- [ ] No breaking changes (or documented below)
- [ ] Dependent changes merged and published

### SWPP Course Requirements
- [ ] Design Documentation updated for major changes
- [ ] Testing plan updated (Iteration 3+)
- [ ] User acceptance test stories unchanged (after Iteration 3)

## Commit Messages
<!--
Ensure all commit messages follow Conventional Commits format:
  <type>(<scope>): <description>

Examples:
  ✅ feat(backend): add user authentication
  ✅ fix(frontend): resolve login button issue
  ✅ docs: update API documentation
  ✅ test: add unit tests for auth service

Valid types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
-->

## Related Issues
<!-- Link related issues. Use "Closes #123" to auto-close issues when PR merges -->

Closes #

## Screenshots / Demo
<!-- Add screenshots, GIFs, or video links for UI changes -->

## Breaking Changes
<!-- List any breaking changes and migration steps. Remove this section if N/A -->

## Additional Context
<!-- Any additional information, concerns, or discussion points for reviewers -->

---

## For Reviewers

### Review Focus Areas
<!-- Author: Highlight specific areas that need careful review -->

### Review Checklist
- [ ] Code quality and conventions followed
- [ ] Tests are comprehensive and pass
- [ ] Documentation is clear and complete
- [ ] No security vulnerabilities
- [ ] Performance impact considered
- [ ] Design Documentation updated (if architectural changes)
- [ ] Changes align with project architecture
- [ ] Breaking changes properly documented

### Reviewer Comments
<!-- Reviewers: Add your feedback here -->

